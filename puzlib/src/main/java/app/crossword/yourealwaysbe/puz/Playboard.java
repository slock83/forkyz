package app.crossword.yourealwaysbe.puz;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

import app.crossword.yourealwaysbe.util.WeakSet;

public class Playboard implements Serializable {
    private static final Logger LOG = Logger.getLogger(Playboard.class.getCanonicalName());

    private MovementStrategy movementStrategy = MovementStrategy.MOVE_NEXT_ON_AXIS;
    private Puzzle puzzle;
    private String responder;
    private List<String> sortedClueListNames = new ArrayList<>();
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
        if (puzzle == null) {
            throw new IllegalArgumentException(
                "Cannot initialise a playboard with a null puzzle."
            );
        }

        this.puzzle = puzzle;

        sortedClueListNames.addAll(puzzle.getClueListNames());
        Collections.sort(this.sortedClueListNames);

        if (puzzle.getPosition() == null)
            selectFirstPosition();

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

    public Box[][] getBoxes() {
        return puzzle.getBoxes();
    }

    /**
     * Returns null if no clue for current position
     */
    public Clue getClue() {
        ClueID clueID = getClueID();
        if (clueID == null)
            return null;
        return puzzle.getClue(clueID);
    }

    public ClueID getClueID() {
        return puzzle.getCurrentClueID();
    }

    /**
     * Gets the currently selected clue list from the puzzle
     *
     * Or null if none selected
     */
    public ClueList getCurrentClueList() {
        ClueID clueID = getClueID();
        return (clueID == null) ? null : puzzle.getClues(clueID.getListName());
    }

    /**
     * Clue number for current position or null if none
     */
    public String getClueNumber() {
        ClueID cid = getClueID();
        if (cid == null)
            return null;
        Clue clue = puzzle.getClue(cid);
        if (clue == null)
            return null;
        return clue.getClueNumber();
    }

    /**
     * Returns -1 if no current clue selected
     */
    public int getCurrentClueIndex() {
        Clue clue = getClue();
        return clue == null ? -1 : clue.getClueID().getIndex();
    }

    public Note getNote() {
        Clue c = this.getClue();
        return (c == null) ? null : this.puzzle.getNote(c.getClueID());
    }

    public Box getCurrentBox() {
        return getCurrentBoxOffset(0, 0);
    }

    /**
     * Get the box at the given offset from current box
     *
     * Null if no box
     */
    private Box getCurrentBoxOffset(int offsetAcross, int offsetDown) {
        Position currentPos = getHighlightLetter();
        int offAcross = currentPos.getCol() + offsetAcross;
        int offDown = currentPos.getRow() + offsetDown;
        return puzzle.checkedGetBox(offDown, offAcross);
    }

    public Word getCurrentWord() {
        Position pos = getHighlightLetter();
        Clue clue = getClue();

        if (clue == null || !clue.hasZone()) {
            Zone zone = new Zone();
            zone.addPosition(pos);
            return new Word(zone);
        } else {
            return new Word(clue.getZone(), clue.getClueID());
        }
    }

    public Box[] getCurrentWordBoxes() {
        Word word = getCurrentWord();
        Zone zone = (word == null) ? null : word.getZone();
        if (zone == null)
            return null;

        Box[] result = new Box[zone.size()];
        Box[][] boxes = getBoxes();

        for (int i = 0; i < zone.size(); i++) {
            Position pos = zone.getPosition(i);
            result[i] = boxes[pos.getRow()][pos.getCol()];
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

        if (highlightLetter == null)
            return w;

        pushNotificationDisabled();

        Position currentHighlight = getHighlightLetter();

        if (highlightLetter.equals(currentHighlight)) {
            toggleSelection();
        } else {
            Box box = puzzle.checkedGetBox(highlightLetter);
            if (box != null) {
                puzzle.setPosition(highlightLetter);

                // toggle if not part of current clue
                Zone zone = getZone(getClueID());
                if (zone == null || !zone.hasPosition(highlightLetter)) {
                    toggleSelection();
                }
            }
        }

        popNotificationDisabled();

        notifyChange();

        return w;
    }

    /**
     * Returns true if the position is part of a word
     *
     * Words may be single cells that are not part of any clue
     */
    public boolean isInWord(Position p) {
        return puzzle.checkedGetBox(p) != null;
    }

    public Position getHighlightLetter() {
        return puzzle.getPosition();
    }

    public void setMovementStrategy(MovementStrategy movementStrategy) {
        this.movementStrategy = movementStrategy;
    }

    public Puzzle getPuzzle() {
        return puzzle;
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

    /**
     * If the clue has been filled in
     *
     * Always false if clue has a null or empty zone
     */
    public boolean isFilledClueID(ClueID clueID) {
        Zone zone = getZone(clueID);

        if(zone == null || zone.isEmpty())
            return false;

        Box[][] boxes = getBoxes();

        for (Position pos : zone) {
            if (boxes[pos.getRow()][pos.getCol()].isBlank())
                return false;
        }

        return true;
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
            Position highlightLetter = getHighlightLetter();
            int row = highlightLetter.getRow();
            int col = highlightLetter.getCol();
            currentBox = getBoxes()[row][col];
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
                ClueID cid = getClueID();
                int cluePos = currentBox.getCluePosition(cid);
                String response = this.getCurrentWordResponse();
                if (cluePos >= 0 && cluePos < response.length())
                    note.deleteScratchLetterAt(cluePos);
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

        boolean skipAdjacent = dontDeleteCrossing
            && currentBoxHasFilledAdjacent();

        return skipCorrect || skipAdjacent;
    }

    private boolean currentBoxHasFilledAdjacent() {
        ClueID currentCID = getClueID();
        if (currentCID == null)
            return false;

        Position currentPos = getHighlightLetter();
        Box currentBox = getCurrentBox();
        Set<ClueID> currentBoxClues = currentBox.getIsPartOfClues();

        for (ClueID otherCID : currentBoxClues) {
            if (currentCID.equals(otherCID))
                continue;

            Zone zone = puzzle.getClue(otherCID).getZone();
            int curPos = zone.indexOf(currentPos);
            Position posBefore = (curPos > 0)
                ? zone.getPosition(curPos - 1)
                : null;

            Box boxBefore = puzzle.checkedGetBox(posBefore);
            if (boxBefore != null && !boxBefore.isBlank())
                return true;

            Position posAfter = (curPos < zone.size() - 1)
                ? zone.getPosition(curPos + 1)
                : null;

            Box boxAfter = puzzle.checkedGetBox(posAfter);
            if (boxAfter != null && !boxAfter.isBlank())
                return true;
        }

        return false;
    }

    /**
     * Ignored if clue not on board
     */
    public void jumpToClue(ClueID clueID) {
        if (clueID == null)
            return;

        pushNotificationDisabled();

        Position pos = getClueStart(clueID);

        if (pos != null) {
            setHighlightLetter(pos);
            puzzle.setCurrentClueID(clueID);
        }

        popNotificationDisabled();

        if (pos != null)
            notifyChange();
    }

    public void jumpToClue(Clue clue) {
        if (clue != null)
            jumpToClue(clue.getClueID());
    }

    /**
     * Ignored if clue not on board
     */
    public void jumpToClueEnd(ClueID clueID) {
        if (clueID == null)
            return;

        pushNotificationDisabled();

        Position pos = getClueEnd(clueID);

        if (pos != null) {
            setHighlightLetter(pos);
            puzzle.setCurrentClueID(clueID);
        }

        popNotificationDisabled();

        if (pos != null)
            notifyChange();
    }

    public void jumpToClueEnd(Clue clue) {
        if (clue != null)
            jumpToClueEnd(clue.getClueID());
    }

    /**
     * Move in direction, or not if no position found
     */
    public Word moveDown() {
        return this.moveDown(false);
    }

    /**
     * Move in direction, or not if no position found
     */
    public Word moveDown(boolean skipCompleted) {
        return moveDelta(skipCompleted, 1, 0);
    }

    /**
     * Move in direction, or not if no position found
     */
    public Position findNextDown(Position original, boolean skipCompleted) {
        return findNextDelta(original, skipCompleted, 1, 0);
    }

    /**
     * Move in direction, or not if no position found
     */
    public Word moveLeft() {
        return moveLeft(false);
    }

    /**
     * Move in direction, or not if no position found
     */
    public Position findNextLeft(Position original, boolean skipCompleted) {
        return findNextDelta(original, skipCompleted, 0, -1);
    }

    /**
     * Move in direction, or not if no position found
     */
    public Word moveLeft(boolean skipCompleted) {
        return moveDelta(skipCompleted, 0, -1);
    }

    /**
     * Move in direction, or not if no position found
     */
    public Word moveRight() {
        return moveRight(false);
    }

    /**
     * Move in direction, or not if no position found
     */
    public Position findNextRight(Position original, boolean skipCompleted) {
        return findNextDelta(original, skipCompleted, 0, 1);
    }

    /**
     * Move in direction, or not if no position found
     */
    public Word moveRight(boolean skipCompleted) {
        return moveDelta(skipCompleted, 0, 1);
    }

    /**
     * Move in direction, or not if no position found
     */
    public Position findNextUp(Position original, boolean skipCompleted) {
        return findNextDelta(original, skipCompleted, -1, 0);
    }

    /**
     * Move in direction, or not if no position found
     */
    public Word moveUp() {
        return moveUp(false);
    }

    /**
     * Move in direction, or not if no position found
     */
    public Word moveUp(boolean skipCompleted) {
        return moveDelta(skipCompleted, -1, 0);
    }

    /**
     * Finds next box in direction
     *
     * @return next box, or null
     */
    public Position findNextDelta(
        Position original, boolean skipCompleted, int drow, int dcol
    ) {
        Position next = new Position(
            original.getRow() + drow, original.getCol() + dcol
        );

        if (
            next.getCol() < 0
            || next.getRow() < 0
            || next.getCol() >= puzzle.getWidth()
            || next.getRow() >= puzzle.getHeight()
        ) {
            return null;
        }

        Box value = puzzle.checkedGetBox(next);

        if ((value == null) || skipBox(value, skipCompleted)) {
            next = findNextDelta(next, skipCompleted, drow, dcol);
        }

        return next;
    }

    /**
     * Move in direction, or not if no position found
     */
    public Word moveDelta(boolean skipCompleted, int drow, int dcol) {
        Word w = getCurrentWord();
        Position oldPos = getHighlightLetter();
        Position newPos = findNextDelta(oldPos, skipCompleted, drow, dcol);
        if (newPos != null)
            setHighlightLetter(newPos);
        return w;
    }

    /**
     * Move to the next position in the current word zone
     *
     * Returns new position, or null if run out
     */
    public Position findNextZone(Position original, boolean skipCompleted) {
        return findZoneDelta(original, skipCompleted, 1);
    }

    /**
     * Move to the next position in zone
     *
     * Does not move if a new position can't be found
     */
    public Word moveZoneForward(boolean skipCompleted) {
        return moveZone(skipCompleted, 1);
    }

    /**
     * Move to the previous position in the current word zone
     *
     * Returns new position, or null if run out
     */
    public Position findPrevZone(Position original, boolean skipCompleted) {
        return findZoneDelta(original, skipCompleted, -1);
    }

    /**
     * Move to the prev position in zone
     *
     * Does not move if a new position can't be found
     */
    public Word moveZoneBack(boolean skipCompleted) {
        return moveZone(skipCompleted, -1);
    }

    /**
     * Find next position in the current word zone by delta steps
     *
     * Returns new position, or null if none found
     */
    public Position findZoneDelta(
        Position original, boolean skipCompleted, int delta
    ) {
        Word word = getCurrentWord();
        Zone zone = word.getZone();

        if (zone == null)
            return null;

        int nextIdx = zone.indexOf(original) + delta;
        if (nextIdx < 0 || nextIdx >= zone.size()) {
            return null;
        } else {
            Position next = zone.getPosition(nextIdx);
            Box box = puzzle.checkedGetBox(next);
            if ((box == null) || skipBox(box, skipCompleted)) {
                next = findZoneDelta(next, skipCompleted, delta);
            }
            return next;
        }
    }

    /**
     * Moves by delta along the zone
     *
     * Does not move if no new position found
     */
    public Word moveZone(boolean skipCompleted, int delta) {
        Word w = this.getCurrentWord();
        Position oldPos = getHighlightLetter();
        Position newPos = findZoneDelta(oldPos, skipCompleted, delta);
        if (newPos != null)
            setHighlightLetter(newPos);
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
     *
     * Still skips completed letters if setting chosen
     */
    public Word nextWord() {
        Word previous = this.getCurrentWord();
        Position curPos = this.getHighlightLetter();
        Position newPos = getClueEnd(getClueID());

        if (!Objects.equals(newPos, curPos)) {
            pushNotificationDisabled();
            setHighlightLetter(newPos);
            popNotificationDisabled();
        }

        MovementStrategy.MOVE_NEXT_CLUE.move(this, skipCompletedLetters);

        return previous;
    }

    public Word playLetter(char letter) {
        Box b = puzzle.checkedGetBox(getHighlightLetter());

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
        Box box = puzzle.checkedGetBox(getHighlightLetter());
        if (box == null)
            return;

        pushNotificationDisabled();

        ClueID cid = getClueID();
        Note note = getNote();
        String response = getCurrentWordResponse();

        // Create a note for this clue if we don't already have one
        if (note == null) {
            note = new Note(response.length());
            this.puzzle.setNote(cid, note);
        }

        // Update the scratch text
        int pos = box.getCluePosition(cid);
        if (pos >= 0 && pos < response.length())
            note.setScratchLetter(pos, letter);

        nextLetter();
        popNotificationDisabled();

        notifyChange();
    }

    public Word previousLetter() {
        return movementStrategy.back(this);
    }

    /**
     * Moves to start of previous word regardless of movement strategy
     */
    public Word previousWord() {
        Word previous = getCurrentWord();

        Position curPos = getHighlightLetter();
        Position newPos = getClueStart(getClueID());

        pushNotificationDisabled();

        if (!Objects.equals(curPos, newPos))
            this.setHighlightLetter(newPos);

        MovementStrategy.MOVE_NEXT_CLUE.back(this);

        popNotificationDisabled();

        setHighlightLetter(getClueStart(getClueID()));

        return previous;
    }

    public Position revealLetter() {
        Position highlightLetter = getHighlightLetter();
        Box b = puzzle.checkedGetBox(highlightLetter);

        if ((b != null) && (b.getSolution() != b.getResponse())) {
            b.setCheated(true);
            b.setResponse(b.getSolution());
            notifyChange();
            return highlightLetter;
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
                    changes.add(new Position(row, col));
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
                    changes.add(new Position(row, col));
                }
            }
        }

        notifyChange(true);

        return changes;
    }

    public List<Position> revealWord() {
        ArrayList<Position> changes = new ArrayList<Position>();
        Position curPos = getHighlightLetter();
        ClueID cid = getClueID();
        Position startPos = getClueStart(cid);
        Zone zone = getZone(cid);

        pushNotificationDisabled();

        if (!Objects.equals(curPos, startPos))
            setHighlightLetter(startPos);

        for (int i = 0; i < zone.size(); i++) {
            Position p = revealLetter();
            if (p != null)
                changes.add(p);
            nextLetter(false);
        }

        popNotificationDisabled();

        setHighlightLetter(curPos);

        return changes;
    }

    public boolean skipPosition(Position p, boolean skipCompleted) {
        Box box = puzzle.checkedGetBox(p);
        return (box == null) ? false : skipBox(box, skipCompleted);
    }

    public boolean skipBox(Box b, boolean skipCompleted) {
        return skipCompleted && !b.isBlank() &&
        (!this.isShowErrors() || (b.getResponse() == b.getSolution()));
    }

    public Word toggleSelection() {
        // TODO: this feels a bit inefficient -- to create and sort a
        // list every time, but also it's not expected to be a list of
        // more than 2 in most situations.
        Word w = this.getCurrentWord();

        Box box = puzzle.checkedGetBox(getHighlightLetter());
        if (box == null)
            return w;

        List<ClueID> boxClues = new ArrayList<>(box.getIsPartOfClues());

        boolean changed = false;

        if (boxClues.isEmpty()) {
            changed = w.getClueID() != null;
            puzzle.setCurrentClueID(null);
        } else {
            Collections.sort(boxClues);

            int curPos = boxClues.indexOf(getClueID());

            // if in current clue, toggle, else try to stay in same list
            if (curPos >= 0) {
                int nextPos = (curPos + 1) % boxClues.size();
                puzzle.setCurrentClueID(boxClues.get(nextPos));
                changed = (nextPos != curPos);
            } else {
                ClueID curClue = getClueID();
                ClueID newClue = null;
                if (curClue != null)
                    newClue = box.getIsPartOfClue(curClue.getListName());
                if (newClue == null)
                    newClue = boxClues.get(0);
                puzzle.setCurrentClueID(newClue);
                changed = true;
            }
        }

        if (changed)
            notifyChange();

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

    private void updateHistory() {
        ClueID cid = getClueID();
        if (cid == null)
            return;
        puzzle.updateHistory(cid);
    }

    /**
     * Find the first clue with a non-empty zone in board order, or all
     * back to first clue with zone found
     *
     * Requires sortedClueListNames
     */
    private void selectFirstPosition() {
        for (ClueID cid : puzzle.getBoardClueIDs()) {
            Clue clue = puzzle.getClue(cid);
            if (clue.hasZone()) {
                puzzle.setPosition(clue.getZone().getPosition(0));
                puzzle.setCurrentClueID(cid);
                return;
            }
        }
        // try all clues
        for (Clue clue : puzzle.getAllClues()) {
            if (clue.hasZone()) {
                puzzle.setPosition(clue.getZone().getPosition(0));
                puzzle.setCurrentClueID(clue.getClueID());
                return;
            }
        }
        // fall back to first cell in grid
        int width = puzzle.getWidth();
        int height = puzzle.getHeight();
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (puzzle.checkedGetBox(row, col) != null) {
                    puzzle.setPosition(new Position(row, col));
                    return;
                }
            }
        }
        throw new IllegalArgumentException(
            "Can't handled grids with no cells"
        );
    }

    /**
     * Returns the zone of the clue, or null
     */
    private Zone getZone(ClueID clueID) {
        Clue clue = puzzle.getClue(clueID);
        return (clue == null) ? null : clue.getZone();
    }

    /**
     * Get start of clue position
     *
     * @return null if no such clue or not on board
     */
    private Position getClueStart(ClueID clueID) {
        Clue clue = puzzle.getClue(clueID);
        Zone zone = clue == null ? null : clue.getZone();

        if (zone == null || zone.isEmpty())
            return null;
        else
            return zone.getPosition(0);
    }

    /**
     * Get end of clue position
     *
     * @return null if no such clue or not on board
     */
    private Position getClueEnd(ClueID clueID) {
        Clue clue = puzzle.getClue(clueID);
        Zone zone = clue == null ? null : clue.getZone();

        if (zone == null || zone.isEmpty())
            return null;
        else
            return zone.getPosition(zone.size() - 1);
    }

    /**
     * A word on the grid
     *
     * A Zone and possibly the clue it goes with (null if no clue).
     */
    public static class Word implements Serializable {
        private final Zone zone;
        private final ClueID clueID;

        public Word(Zone zone, ClueID clueID) {
            this.zone = zone;
            this.clueID = clueID;
        }

        public Word(Zone zone) {
            this.zone = zone;
            this.clueID = null;
        }

        public Zone getZone() { return zone; }
        public ClueID getClueID() { return clueID; }

        public boolean checkInWord(Position pos) {
            return zone.hasPosition(pos);
        }

        public boolean checkInWord(int row, int col) {
            return zone.hasPosition(new Position(row, col));
        }

        /**
         * Length of word
         *
         * @return len or -1 if no zone
         */
        public int getLength() {
            return (zone == null) ? -1 : zone.size();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;

            if (o == null)
                return false;

            if (!(o instanceof Word))
                return false;

            Word check = (Word) o;

            return Objects.equals(zone, check.zone)
                && Objects.equals(clueID, check.clueID);
        }

        @Override
        public int hashCode() {
            return Objects.hash(zone, clueID);
        }
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
