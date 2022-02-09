package app.crossword.yourealwaysbe.puz;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import app.crossword.yourealwaysbe.util.WeakSet;

public class Playboard implements Serializable {
    private static final Logger LOG = Logger.getLogger(Playboard.class.getCanonicalName());

    private Map<Integer, Position> acrossWordStarts = new HashMap<Integer, Position>();
    private Map<Integer, Position> downWordStarts = new HashMap<Integer, Position>();
    private Map<Position, Integer> acrossWordRanges = new HashMap<>();
    private Map<Position, Integer> downWordRanges = new HashMap<>();
    private MovementStrategy movementStrategy = MovementStrategy.MOVE_NEXT_ON_AXIS;
    private Position highlightLetter = new Position(0, 0);
    private Puzzle puzzle;
    private String responder;
    private boolean across = true;
    private boolean showErrorsGrid;
    private boolean showErrorsCursor;
    private boolean skipCompletedLetters;
    private boolean preserveCorrectLettersInShowErrors;
    private boolean dontDeleteCrossing;
    private Set<PlayboardListener> listeners = WeakSet.buildSet();
    private int notificationDisabledDepth = 0;
    private Word previousWord = null;

    public Playboard(Puzzle puzzle,
                     MovementStrategy movementStrategy,
                     boolean preserveCorrectLettersInShowErrors,
                     boolean dontDeleteCrossing){
        this(puzzle, movementStrategy);
        this.preserveCorrectLettersInShowErrors = preserveCorrectLettersInShowErrors;
        this.dontDeleteCrossing = dontDeleteCrossing;
    }

    public Playboard(Puzzle puzzle, MovementStrategy movementStrategy) {
        this(puzzle);
        this.movementStrategy = movementStrategy;
    }

    public Playboard(Puzzle puzzle) {
        this.puzzle = puzzle;
        this.highlightLetter = this.puzzle.getPosition();
        if (this.highlightLetter == null)
            this.highlightLetter = new Position(0, 0);
        this.across = this.puzzle.getAcross();
        Box[][] boxes = puzzle.getBoxes();

        for (int row = 0; row < puzzle.getHeight(); row++) {
            for (int col = 0; col < puzzle.getWidth(); col++) {
                if (puzzle.isStartAcross(row, col)) {
                    acrossWordStarts.put(
                        boxes[row][col].getClueNumber(),
                        new Position(col, row)
                    );
                }
                if (puzzle.isStartDown(row, col)) {
                    downWordStarts.put(
                        boxes[row][col].getClueNumber(),
                        new Position(col, row)
                    );
                }
            }
        }

        ensureClueSelected();
        updateHistory();
        // there will be no listeners at this point, but the call also
        // does a bit of bookkeeping / setup for the next use
        notifyChange();
    }

    public void setPreserveCorrectLettersInShowErrors(boolean value){
        this.preserveCorrectLettersInShowErrors = value;
    }

    /**
     * Whether to delete characters from crossing words
     */
    public void setDontDeleteCrossing(boolean value){
        this.dontDeleteCrossing = value;
    }

    public void setAcross(boolean across) {
        boolean changed = (this.across != across);
        this.across = across;
        if (this.puzzle != null) {
            this.puzzle.setAcross(across);
        }
        if (changed) {
            notifyChange();
        }
    }

    public boolean isAcross() {
        return across;
    }

    public Box[][] getBoxes() {
        return puzzle.getBoxes();
    }

    /**
     * Returns null if no clue for current position
     */
    public Clue getClue() {
        int number = getClueNumber();
        return (number > -1)
            ? this.puzzle.getClues(this.isAcross()).getClue(number)
            : null;
    }

    /**
     * Clue number for current position or -1 if none
     */
    public int getClueNumber() {
        Position start = this.getCurrentWordStart();
        Box box = puzzle.checkedGetBox(start.down, start.across);
        return box == null ? -1 : box.getClueNumber();
    }

    /**
     * Returns -1 if no current clue
     */
    public int getCurrentClueIndex() {
        Clue clue = getClue();
        if (clue != null) {
            return puzzle.getClues(clue.getIsAcross())
                .getClueIndex(clue.getNumber());
        } else {
            return -1;
        }
    }

    public Note getNote() {
        Clue c = this.getClue();
        if (c != null)
            return this.puzzle.getNote(c.getNumber(), this.across);
        else
            return null;
    }

    public Box getCurrentBox() {
        return getCurrentBoxOffset(0, 0);
    }

    /**
     * Get the box to the left in the direction of the clue
     *
     * I.e. if across, get box one north, if down, get box one east
     */
    public Box getAdjacentBoxLeft() {
        Position curPos = getHighlightLetter();
        if (across) {
            if (puzzle.joinedTop(curPos.down, curPos.across)) {
                return puzzle.checkedGetBox(curPos.down - 1, curPos.across);
            }
        } else {
            if (puzzle.joinedRight(curPos.down, curPos.across))
                return puzzle.checkedGetBox(curPos.down, curPos.across + 1);
        }
        return null;
    }

    /**
     * Get the box to the right in the direction of the clue
     *
     * I.e. if across, get box one south, if down, get box one west
     */
    public Box getAdjacentBoxRight() {
        Position curPos = getHighlightLetter();
        if (across) {
            if (puzzle.joinedBottom(curPos.down, curPos.across)) {
                return puzzle.checkedGetBox(curPos.down + 1, curPos.across);
            }
        } else {
            if (puzzle.joinedLeft(curPos.down, curPos.across))
                return puzzle.checkedGetBox(curPos.down, curPos.across - 1);
        }
        return null;
    }

    /**
     * Get the box at the given offset from current box
     *
     * Null if no box
     */
    private Box getCurrentBoxOffset(int offsetAcross, int offsetDown) {
        Position currentPos = getHighlightLetter();
        int offAcross = currentPos.across + offsetAcross;
        int offDown = currentPos.down + offsetDown;
        return puzzle.checkedGetBox(offDown, offAcross);
    }

    public Word getCurrentWord() {
        Word w = new Word();
        w.start = this.getCurrentWordStart();
        w.across = this.isAcross();
        w.number = this.getClueNumber();
        w.length = this.getWordRange();

        return w;
    }

    public Box[] getCurrentWordBoxes() {
        Word currentWord = this.getCurrentWord();
        Box[] result = new Box[currentWord.length];
        Box[][] boxes = getBoxes();

        int across = currentWord.start.across;
        int down = currentWord.start.down;

        for (int i = 0; i < result.length; i++) {
            int newAcross = across;
            int newDown = down;

            if (currentWord.across) {
                newAcross += i;
            } else {
                newDown += i;
            }

            result[i] = boxes[newDown][newAcross];
        }

        return result;
    }

    public String getCurrentWordResponse() {
        Box[] boxes = getCurrentWordBoxes();
        char[] letters = new char[boxes.length];

        for (int i = 0; i < boxes.length; i++) {
            letters[i] = boxes[i].getResponse();
        }
        return new String(letters);
    }

    public Position[] getCurrentWordPositions() {
        Word currentWord = this.getCurrentWord();
        Position[] result = new Position[currentWord.length];
        int across = currentWord.start.across;
        int down = currentWord.start.down;

        for (int i = 0; i < result.length; i++) {
            int newAcross = across;
            int newDown = down;

            if (currentWord.across) {
                newAcross += i;
            } else {
                newDown += i;
            }

            result[i] = new Position(newAcross, newDown);
        }

        return result;
    }

    /**
     * Finds start of highlighted word
     *
     * If the current box is not part of a clue in the current playboard
     * direction, then returns current highlighted letter
     */
    public Position getCurrentWordStart() {
        int row = this.highlightLetter.down;
        int col = this.highlightLetter.across;

        if (this.isAcross()) {
            if (puzzle.isPartOfAcross(row, col)) {
                while (!puzzle.isStartAcross(row, col)) {
                    col -= 1;
                }
            }
        } else {
            if (puzzle.isPartOfDown(row, col)) {
                while (!puzzle.isStartDown(row, col)) {
                    row -= 1;
                }
            }
        }

        return new Position(col, row);
    }

    public void setCurrentWord(String response) {
        Box[] boxes = getCurrentWordBoxes();
        int length = Math.min(boxes.length, response.length());
        for (int i = 0; i < length; i++) {
            boxes[i].setResponse(response.charAt(i));
        }
        notifyChange();
    }

    public void setCurrentWord(Box[] response) {
        Box[] boxes = getCurrentWordBoxes();
        int length = Math.min(boxes.length, response.length);
        for (int i = 0; i < length; i++) {
            boxes[i].setResponse(response[i].getResponse());
        }
        notifyChange();
    }

    public Word setHighlightLetter(Position highlightLetter) {
        Word w = this.getCurrentWord();
        int col = highlightLetter.across;
        int row = highlightLetter.down;

        pushNotificationDisabled();

        if (highlightLetter.equals(this.highlightLetter)) {
            toggleDirection();
        } else {
            Box box = puzzle.checkedGetBox(row, col);
            if (box != null) {
                this.highlightLetter = highlightLetter;

                if (this.puzzle != null) {
                    this.puzzle.setPosition(highlightLetter);
                }

                if ((isAcross() && !box.isPartOfAcross()) ||
                    (!isAcross() && !box.isPartOfDown())) {
                    toggleDirection();
                }
            }
        }

        popNotificationDisabled();

        notifyChange();

        return w;
    }

    /**
     * Returns true if the position is part of a word (not blank cell)
     */
    public boolean isInWord(Position p) {
        int col = p.across;
        int row = p.down;
        return puzzle.checkedGetBox(row, col) != null;
    }

    public Position getHighlightLetter() {
        return highlightLetter;
    }

    public void setMovementStrategy(MovementStrategy movementStrategy) {
        this.movementStrategy = movementStrategy;
    }

    public Puzzle getPuzzle() {
        return this.puzzle;
    }

    /**
     * @param responder the responder to set
     */
    public void setResponder(String responder) {
        this.responder = responder;
    }

    /**
     * @return the responder
     */
    public String getResponder() {
        return responder;
    }

    /**
     * Show errors across the whole grid
     */
    public void setShowErrorsGrid(boolean showErrors) {
        boolean changed = (this.showErrorsGrid != showErrors);
        this.showErrorsGrid = showErrors;
        if (changed)
            notifyChange();
    }

    /**
     * Show errors under the cursor only
     */
    public void setShowErrorsCursor(boolean showErrorsCursor) {
        boolean changed = (this.showErrorsCursor != showErrorsCursor);
        this.showErrorsCursor = showErrorsCursor;
        if (changed)
            notifyChange();
    }

    /**
     * Toggle show errors across the grid
     */
    public void toggleShowErrorsGrid() {
        this.showErrorsGrid = !this.showErrorsGrid;
        notifyChange(true);
    }

    /**
     * Toggle show errors across under cursor
     */
    public void toggleShowErrorsCursor() {
        this.showErrorsCursor = !this.showErrorsCursor;
        notifyChange(true);
    }

    /**
     * Is showing errors across the whole grid
     */
    public boolean isShowErrorsGrid() {
        return this.showErrorsGrid;
    }

    /**
     * Is showing errors across under cursor
     */
    public boolean isShowErrorsCursor() {
        return this.showErrorsCursor;
    }

    /**
     * Is showing errors either or cursor or grid
     */
    public boolean isShowErrors() {
        return isShowErrorsGrid() || isShowErrorsCursor();
    }

    public void setSkipCompletedLetters(boolean skipCompletedLetters) {
        this.skipCompletedLetters = skipCompletedLetters;
    }

    public boolean isSkipCompletedLetters() {
        return skipCompletedLetters;
    }

    public boolean isFilledClueNum(int number, boolean isAcross) {
        Position start = (isAcross ?
                          this.acrossWordStarts.get(number) :
                          this.downWordStarts.get(number));

        if(start == null)
            return false;

        int range = this.getWordRange(start, isAcross);
        int across = start.across;
        int down = start.down;
        Box[][] boxes = getBoxes();

        for (int i = 0; i < range; i++) {
            int newAcross = isAcross ? across + i : across;
            int newDown = isAcross ? down : down + i;

            if (boxes[newDown][newAcross].isBlank())
                return false;
        }

        return true;
    }

    public Box[] getWordBoxes(int number, boolean isAcross) {
        Position start = isAcross ? this.acrossWordStarts.get(number) : this.downWordStarts.get(number);
        if(start == null) {
            return new Box[0];
        }
        int range = this.getWordRange(start, isAcross);
        int across = start.across;
        int down = start.down;
        Box[][] boxes = getBoxes();
        Box[] result = new Box[range];

        for (int i = 0; i < result.length; i++) {
            int newAcross = across;
            int newDown = down;

            if (isAcross) {
                newAcross += i;
            } else {
                newDown += i;
            }

            result[i] = boxes[newDown][newAcross];
        }

        return result;
    }

    public int getWordRange(int number, boolean isAcross) {
        Position start = isAcross
            ? acrossWordStarts.get(number)
            : downWordStarts.get(number);

        if(start == null)
            return 0;
        else
            return getWordRange(start, isAcross);
    }

    public int getWordRange(Position start, boolean across) {
        Map<Position, Integer> rangeMap =
            across ? acrossWordRanges : downWordRanges;

        Integer range = rangeMap.get(start);

        if (range == null) {
            int row = start.down;
            int col = start.across;

            if (across) {
                while (puzzle.joinedRight(row, col)) {
                    col += 1;
                }
                range = col - start.across + 1;
            } else {
                while (puzzle.joinedBottom(row, col)) {
                    row += 1;
                }
                range = row - start.down + 1;
            }

            rangeMap.put(start, range);
        }

        return range;
    }

    public int getWordRange() {
        return getWordRange(this.getCurrentWordStart(), this.isAcross());
    }

    /**
     * Handler for the backspace key.  Uses the following algorithm:
     * -If current box is empty, move back one character.  If not, stay still.
     * -Delete the letter in the current box.
     */
    public Word deleteLetter() {
        Box currentBox = this.getCurrentBox();
        Word wordToReturn = this.getCurrentWord();

        pushNotificationDisabled();


        if (currentBox.isBlank() || isDontDeleteCurrent()) {
            wordToReturn = this.previousLetter();
            currentBox = getBoxes()[this.highlightLetter.down][this.highlightLetter.across];
        }


        if (!isDontDeleteCurrent()) {
            currentBox.setBlank();
        }

        popNotificationDisabled();

        notifyChange();

        return wordToReturn;
    }

    public void deleteScratchLetter() {
        Box currentBox = this.getCurrentBox();

        pushNotificationDisabled();

        if (currentBox.isBlank()) {
            Note note = this.getNote();
            if (note != null) {
                int pos = this.across ? currentBox.getAcrossPosition() : currentBox.getDownPosition();
                String response = this.getCurrentWordResponse();
                if (pos >= 0 && pos < response.length())
                    note.deleteScratchLetterAt(pos);
            }
        }

        this.previousLetter();

        popNotificationDisabled();
        notifyChange();
    }

    /**
     * Returns true if the current letter should not be deleted
     *
     * E.g. because it is correct and show errors is show
     */
    private boolean isDontDeleteCurrent() {
        Box currentBox = getCurrentBox();

        // Prohibit deleting correct letters
        boolean skipCorrect
            = (preserveCorrectLettersInShowErrors &&
               currentBox.getResponse() == currentBox.getSolution() &&
               this.isShowErrors());

        // Don't delete letters from crossing words
        Box adjacentLeft = getAdjacentBoxLeft();
        Box adjacentRight = getAdjacentBoxRight();
        boolean skipAdjacent
            = (dontDeleteCrossing &&
               ((adjacentLeft != null && !adjacentLeft.isBlank()) ||
                (adjacentRight != null && !adjacentRight.isBlank())));

        return skipCorrect || skipAdjacent;
    }

    /**
     * Ignored if clueNumber does not exist
     */
    public void jumpToClue(int clueNumber, boolean across) {
        try {
            pushNotificationDisabled();

            Position pos = null;

            if (across) {
                pos = this.acrossWordStarts.get(clueNumber);
            } else {
                pos = this.downWordStarts.get(clueNumber);
            }

            if (pos != null) {
                this.setHighlightLetter(pos);
                this.setAcross(across);
            }

            popNotificationDisabled();

            if (pos != null)
                notifyChange();
        } catch (Exception e) {
        }
    }

    public Word moveDown() {
        return this.moveDown(false);
    }

    public Position moveDown(Position original, boolean skipCompleted) {
        Position next = new Position(original.across, original.down + 1);

        if (next.down >= puzzle.getHeight())
            return original;

        Box value = puzzle.checkedGetBox(next.down, next.across);

        if ((value == null) || skipCurrentBox(value, skipCompleted)) {
            next = moveDown(next, skipCompleted);
        }

        return next;
    }

    public Word moveDown(boolean skipCompleted) {
        Word w = this.getCurrentWord();
        Position newPos = this.moveDown(this.getHighlightLetter(), skipCompleted);
        this.setHighlightLetter(newPos);
        return w;
    }

    public Position moveLeft(Position original, boolean skipCompleted) {
        Position next = new Position(original.across - 1, original.down);

        if (next.across < 0)
            return original;

        Box value = puzzle.checkedGetBox(next.down, next.across);

        if ((value == null) || skipCurrentBox(value, skipCompleted)) {
            next = moveLeft(next, skipCompleted);
        }

        return next;
    }

    public Word moveLeft(boolean skipCompleted) {
        Word w = this.getCurrentWord();
        Position newPos = this.moveLeft(this.getHighlightLetter(), skipCompleted);
        this.setHighlightLetter(newPos);
        return w;
    }

    public Word moveLeft() {
        return moveLeft(false);
    }

    public Word moveRight() {
        return moveRight(false);
    }

    public Position moveRight(Position original, boolean skipCompleted) {
        Position next = new Position(original.across + 1, original.down);

        if (next.across >= puzzle.getWidth())
            return original;

        Box value = puzzle.checkedGetBox(next.down, next.across);

        if ((value == null) || skipCurrentBox(value, skipCompleted)) {
            next = moveRight(next, skipCompleted);
        }

        return next;
    }

    public Word moveRight(boolean skipCompleted) {
        Word w = this.getCurrentWord();
        Position newPos = this.moveRight(this.getHighlightLetter(), skipCompleted);
        this.setHighlightLetter(newPos);
        return w;
    }

    public Position moveUp(Position original, boolean skipCompleted) {
        Position next = new Position(original.across, original.down - 1);

        if (next.down < 0)
            return original;

        Box value = puzzle.checkedGetBox(next.down, next.across);

        if ((value == null) || skipCurrentBox(value, skipCompleted)) {
            next = moveUp(next, skipCompleted);
        }

        return next;
    }

    public Word moveUp() {
        return moveUp(false);
    }

    public Word moveUp(boolean skipCompleted) {
        Word w = this.getCurrentWord();
        Position newPos = this.moveUp(this.getHighlightLetter(), skipCompleted);
        this.setHighlightLetter(newPos);
        return w;
    }

    public Word nextLetter(boolean skipCompletedLetters) {
        return this.movementStrategy.move(this, skipCompletedLetters);
    }

    public Word nextLetter() {
        return nextLetter(this.skipCompletedLetters);
    }

    /**
     * Jump to next word, regardless of movement strategy
     */
    public Word nextWord() {
        Word previous = this.getCurrentWord();

        Position p = this.getHighlightLetter();

        int newAcross = p.across;
        int newDown = p.down;

        if (previous.across) {
            newAcross = (previous.start.across + previous.length) - 1;
        } else {
            newDown = (previous.start.down + previous.length) - 1;
        }

        Position newPos = new Position(newAcross, newDown);

        if (!newPos.equals(p)) {
            pushNotificationDisabled();
            this.setHighlightLetter(newPos);
            popNotificationDisabled();
        }

        MovementStrategy.MOVE_NEXT_CLUE.move(this, false);

        return previous;
    }

    public Word playLetter(char letter) {
        Box b = puzzle.checkedGetBox(
            this.highlightLetter.down, this.highlightLetter.across
        );

        if (b == null) {
            return null;
        }

        if (preserveCorrectLettersInShowErrors && b.getResponse() == b.getSolution() && isShowErrors()) {
            // Prohibit replacing correct letters
            return this.getCurrentWord();
        } else {
            pushNotificationDisabled();
            b.setResponse(letter);
            b.setResponder(this.responder);
            Word next = this.nextLetter();
            popNotificationDisabled();

            notifyChange();

            return next;
        }
    }

    public void playScratchLetter(char letter) {
        Box b = puzzle.checkedGetBox(
            this.highlightLetter.down, this.highlightLetter.across
        );

        if (b == null) {
            return;
        }

        pushNotificationDisabled();

        Note note = this.getNote();
        String response = this.getCurrentWordResponse();

        // Create a note for this clue if we don't already have one
        if (note == null) {
            note = new Note(response.length());
            Clue c = this.getClue();
            this.puzzle.setNote(c.getNumber(), note, this.across);
        }

        // Update the scratch text
        int pos = this.across ? b.getAcrossPosition() : b.getDownPosition();
        if (pos >= 0 && pos < response.length())
            note.setScratchLetter(pos, letter);

        this.nextLetter();
        popNotificationDisabled();

        notifyChange();
    }

    public Word previousLetter() {
        return this.movementStrategy.back(this);
    }

    /**
     * Moves to start of previous word regardless of movement strategy
     */
    public Word previousWord() {
        Word previous = this.getCurrentWord();

        Position p = this.getHighlightLetter();

        int newAcross = p.across;
        int newDown = p.down;

        pushNotificationDisabled();

        if (previous.across && p.across != previous.start.across) {
            this.setHighlightLetter(
                new Position(previous.start.across, p.down)
            );
        } else if (!previous.across && p.down != previous.start.down) {
            this.setHighlightLetter(
                new Position(p.across, previous.start.down)
            );
        }

        MovementStrategy.MOVE_NEXT_CLUE.back(this);

        popNotificationDisabled();

        Word current = this.getCurrentWord();
        this.setHighlightLetter(new Position(current.start.across, current.start.down));

        return previous;
    }

    public Position revealLetter() {
        Box b = puzzle.checkedGetBox(
            this.highlightLetter.down, this.highlightLetter.across
        );

        if ((b != null) && (b.getSolution() != b.getResponse())) {
            b.setCheated(true);
            b.setResponse(b.getSolution());

            notifyChange();

            return this.highlightLetter;
        }

        return null;
    }

    /**
     * Reveals the correct answers for any "red" squares on the board.
     *
     * This covers hidden and visible incorrect responses, as well as squares that are marked as
     * "cheated" from previously erased incorrect responses.
     *
     * @return
     */
    public List<Position> revealErrors() {
        ArrayList<Position> changes = new ArrayList<Position>();
        Box[][] boxes = getBoxes();

        for (int row = 0; row < puzzle.getHeight(); row++) {
            for (int col = 0; col < puzzle.getWidth(); col++) {
                Box b = boxes[row][col];
                if (b == null) { continue; }

                if (b.isCheated() ||
                        (!b.isBlank() && (b.getSolution() != b.getResponse()))) {
                    b.setCheated(true);
                    b.setResponse(b.getSolution());
                    changes.add(new Position(col, row));
                }
            }
        }

        notifyChange(true);

        return changes;
    }

    public List<Position> revealPuzzle() {
        ArrayList<Position> changes = new ArrayList<Position>();
        Box[][] boxes = getBoxes();

        for (int row = 0; row < puzzle.getHeight(); row++) {
            for (int col = 0; col < puzzle.getWidth(); col++) {
                Box b = boxes[row][col];

                if ((b != null) && (b.getSolution() != b.getResponse())) {
                    b.setCheated(true);
                    b.setResponse(b.getSolution());
                    changes.add(new Position(col, row));
                }
            }
        }

        notifyChange(true);

        return changes;
    }

    public List<Position> revealWord() {
        ArrayList<Position> changes = new ArrayList<Position>();
        Position oldHighlight = this.highlightLetter;
        Word w = this.getCurrentWord();

        pushNotificationDisabled();

        if (!oldHighlight.equals(w.start))
            this.setHighlightLetter(w.start);

        for (int i = 0; i < w.length; i++) {
            Position p = revealLetter();

            if (p != null) {
                changes.add(p);
            }

            nextLetter(false);
        }

        popNotificationDisabled();

        this.setHighlightLetter(oldHighlight);

        return changes;
    }

    public boolean skipCurrentBox(Box b, boolean skipCompleted) {
        return skipCompleted && !b.isBlank() &&
        (!this.isShowErrors() || (b.getResponse() == b.getSolution()));
    }

    public Word toggleDirection() {
        Word w = this.getCurrentWord();
        Position cur = getHighlightLetter();
        int row = cur.down;
        int col = cur.across;
        Box box = puzzle.checkedGetBox(row, col);

        if ((across && box.isPartOfDown()) ||
            (!across && box.isPartOfAcross())) {
            this.setAcross(!this.isAcross());
        }

        return w;
    }

    public void addListener(PlayboardListener listener) {
        listeners.add(listener);
    }

    public void removeListener(PlayboardListener listener) {
        listeners.remove(listener);
    }

    private void notifyChange() { notifyChange(false); }

    private void notifyChange(boolean wholeBoard) {
        if (notificationDisabledDepth == 0) {
            updateHistory();

            Word currentWord = getCurrentWord();
            for (PlayboardListener listener : listeners) {
                listener.onPlayboardChange(
                    wholeBoard, currentWord, previousWord
                );
            }
            previousWord = currentWord;
        }
    }

    private void pushNotificationDisabled() {
        notificationDisabledDepth += 1;
    }

    private void popNotificationDisabled() {
        if (notificationDisabledDepth > 0)
            notificationDisabledDepth -= 1;
    }

    public static class Position implements Serializable {
        public int across;
        public int down;

        protected Position(){

        }

        public Position(int across, int down) {
            this.down = down;
            this.across = across;
        }

        @Override
        public boolean equals(Object o) {
            if ((o == null) || (o.getClass() != this.getClass())) {
                return false;
            }

            Position p = (Position) o;

            return ((p.down == this.down) && (p.across == this.across));
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(new int[] {across, down});
        }

        @Override
        public String toString() {
            return "[" + this.across + " x " + this.down + "]";
        }
    }

    public static class Word implements Serializable {
        public Position start;
        public boolean across;
        public int number;
        public int length;

        public boolean checkInWord(int across, int down) {
            int ranging = this.across ? across : down;
            boolean offRanging = this.across ? (down == start.down) : (across == start.across);

            int startPos = this.across ? start.across : start.down;

            return (offRanging && (startPos <= ranging) && ((startPos + length) > ranging));
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }

            if (o.getClass() != Word.class) {
                return false;
            }

            Word check = (Word) o;

            return check.start.equals(this.start) && (check.across == this.across) && (check.length == this.length);
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = (29 * hash) + ((this.start != null) ? this.start.hashCode() : 0);
            hash = (29 * hash) + (this.across ? 1 : 0);
            hash = (29 * hash) + this.length;

            return hash;
        }
    }

    private void updateHistory() {
        if (puzzle != null) {
            Clue c = getClue();
            if (c != null)
                puzzle.updateHistory(c.getNumber(), c.getIsAcross());
        }
    }

    private void ensureClueSelected() {
        if (getCurrentBox() == null)
            this.moveRight(false);

        Box current = getCurrentBox();
        if (isAcross() && !current.isPartOfAcross())
            setAcross(false);
        else if (!isAcross() && !current.isPartOfDown())
            setAcross(true);
    }

    /**
     * Playboard listeners will be updated when the highlighted letter
     * changes or the contents of a box changes.
     *
     * TODO: what about notes in scratch?
     */
    public interface PlayboardListener {
        /**
         * Notify that something has changed on the board
         *
         * currentWord and previousWord are the selected words since the
         * last notification. These will be where changes are.
         *
         * @param wholeBoard true if change affects whole board
         * @param currentWord the currently selected word
         * @param previousWord the word selected in the last
         * notification (may be null)
         */
        public void onPlayboardChange(
            boolean wholeBoard, Word currentWord, Word previousWord
        );
    }
}
