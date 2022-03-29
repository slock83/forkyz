package app.crossword.yourealwaysbe.puz;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import java.util.logging.Logger;

/**
 * Represent a puzzle
 *
 * String should use HTML for formatting (including new lines), except
 * Note objects which do not support it.
 */
public class Puzzle implements Serializable{
    private static final Logger LOG = Logger.getLogger("app.crossword.yourealwaysbe");

    @FunctionalInterface
    public static interface ClueIDConsumer {
        public void accept(int number, boolean across) throws Exception;
    }

    private String author;
    private String copyright;
    private String notes;
    private String title;
    private MutableClueList acrossClues = new MutableClueList();
    private MutableClueList downClues = new MutableClueList();
    private Map<String, List<Clue>> extraClues = new HashMap<>();
    private LocalDate pubdate = LocalDate.now();
    private String source;
    private String sourceUrl = "";
    private String supportUrl;
    private Box[][] boxes;
    private boolean updatable;
    private int height;
    private int width;
    private long playedTime;
    private boolean scrambled;
    public short solutionChecksum;

    // current play position data needed for saving state...
    private Position position;
    private boolean across = true;

    private SortedMap<Integer, Note> acrossNotes = new TreeMap<>();
    private SortedMap<Integer, Note> downNotes = new TreeMap<>();
    private Note playerNote;

    private LinkedList<ClueID> historyList = new LinkedList<>();

    private Set<ClueID> flaggedClues = new HashSet<>();

    private Map<Integer, Position> wordStarts
        = new HashMap<Integer, Position>();

    // Temporary fields used for unscrambling.
    public int[] unscrambleKey;
    public byte[] unscrambleTmp;
    public byte[] unscrambleBuf;

    /**
     * Add a clue
     *
     * List name Clue.ACROSS/DOWN added to board, other clues put into
     * "extra" clue lists
     */
    public void addClue(Clue clue) {
        if (clue.isAcross()) {
            acrossClues.addClue(clue);
            addClueToBoxes(clue);
        } else if (clue.isDown()) {
            downClues.addClue(clue);
            addClueToBoxes(clue);
        } else {
            String listName = clue.getListName();
            if (!extraClues.containsKey(listName))
                extraClues.put(listName, new ArrayList<Clue>());
            extraClues.get(listName).add(clue);
        }
    }

    /**
     * Get across/down clue lists
     *
     * Note: no reason to assume there is a numbered clue for every
     * numbered position on the board. This is not always the case when
     * clues span multiple entries.
     */
    public ClueList getClues(boolean across) {
        return across ? this.acrossClues : this.downClues;
    }

    /**
     * Clues that are in a non-standard list (not across/down)
     *
     * @return null if list does not exist
     */
    public List<Clue> getExtraClues(String listName) {
        return extraClues.get(listName);
    }

    /**
     * Get list of names of all non-standard clue lists
     */
    public Set<String> getExtraClueListNames() {
        return extraClues.keySet();
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAuthor() {
        return author;
    }

    /**
     * Get start of clue position
     *
     * @return null if no such clue
     */
    public Position getWordStart(int clueNum, boolean across) {
        Position start = wordStarts.get(clueNum);

        if (start == null)
            return null;

        Box box = boxes[start.getRow()][start.getCol()];

        if (box == null)
            return null;

        if (across && box.isAcross())
            return start;

        if (!across && box.isDown())
            return start;

        return null;
    }

    private static Box checkedGetBox(Box[][] boxes, int row, int col) {
        if (row < 0 || row >= boxes.length)
            return null;
        if (col < 0 || col >= boxes[row].length)
            return null;

        return boxes[row][col];
    }

    private static boolean joinedTop(Box[][] boxes, int row, int col) {
        Box boxCur = checkedGetBox(boxes, row, col);
        Box boxAbove = checkedGetBox(boxes, row - 1, col);

        if (boxAbove == null || boxCur == null)
            return false;

        return !(boxCur.isBarredTop() || boxAbove.isBarredBottom());
    }

    private static boolean joinedBottom(Box[][] boxes, int row, int col) {
        Box boxCur = checkedGetBox(boxes, row, col);
        Box boxBelow = checkedGetBox(boxes, row + 1, col);

        if (boxBelow == null || boxCur == null)
            return false;

        return !(boxCur.isBarredBottom() || boxBelow.isBarredTop());
    }

    private static boolean joinedLeft(Box[][] boxes, int row, int col) {
        Box boxCur = checkedGetBox(boxes, row, col);
        Box boxLeft = checkedGetBox(boxes, row, col - 1);

        if (boxLeft == null || boxCur == null)
            return false;

        return !(boxCur.isBarredLeft() || boxLeft.isBarredRight());
    }

    private static boolean joinedRight(Box[][] boxes, int row, int col) {
        Box boxCur = checkedGetBox(boxes, row, col);
        Box boxRight = checkedGetBox(boxes, row, col + 1);

        if (boxRight == null || boxCur == null)
            return false;

        return !(boxCur.isBarredRight() || boxRight.isBarredLeft());
    }

    /**
     * Test if box at position is same clue as the box above
     */
    public boolean joinedTop(int row, int col) {
        return joinedTop(boxes, row, col);
    }

    /**
     * Test if box at pos is same clue as the box below
     */
    public boolean joinedBottom(int row, int col) {
        return joinedBottom(boxes, row, col);
    }

    /**
     * Test if box at pos is same clue as the box to left
     */
    public boolean joinedLeft(int row, int col) {
        return joinedLeft(boxes, row, col);
    }

    /**
     * Test if box at pos is same clue as the box to right
     */
    public boolean joinedRight(int row, int col) {
        return joinedRight(boxes, row, col);
    }

    /**
     * If the box at the position is the start of an across clue
     */
    public boolean isStartAcross(int row, int col) {
        Box b = checkedGetBox(boxes, row, col);
        return b != null && b.isAcross();
    }

    /**
     * If the box at the position is the start of a down clue
     */
    public boolean isStartDown(int row, int col) {
        Box b = checkedGetBox(boxes, row, col);
        return b != null && b.isDown();
    }

    /**
     * If the box at the position is the part of an across clue
     */
    public boolean isPartOfAcross(int row, int col) {
        Box b = checkedGetBox(boxes, row, col);
        return b != null && b.isPartOfAcross();
    }

    /**
     * If the box at the position is part of a down clue
     */
    public boolean isPartOfDown(int row, int col) {
        Box b = checkedGetBox(boxes, row, col);
        return b != null && b.isPartOfDown();
    }

    /**
     * Return null if index out of range or not a box
     */
    public Box checkedGetBox(int row, int col) {
        return checkedGetBox(boxes, row, col);
    }

    /**
     * Return null if index out of range or not a box
     */
    public Box checkedGetBox(Position p) {
        return checkedGetBox(boxes, p.getRow(), p.getCol());
    }

    /**
     * Set boxes for puzzle
     *
     * "Standard" crossword numbering rules: read left to right, top to
     * bottom. The start of an across clue is when a box is not joined
     * to something on the left, but is joined to something on the
     * right. Similarly for down clues.
     *
     * Also sets height and width and fills in "is part of clue" info in
     * boxes. This includes whether the numbered cells are the start of
     * an across or down clue.
     *
     * @param boxes boxes in row, col order, null means black square.
     * Must be a true grid.
     * @param autoNumber assign numbers to boxes using standard method
     * rather than any existing numbering
     * @throws IllegalArgumentException if the boxes are not a grid, or
     * contain autoNumber true but any existing numbering inconsistent
     * with the "standard" crossword numbering system.
     */
    public void setBoxes(Box[][] boxes, boolean autoNumber) {
        this.boxes = boxes;

        this.height = boxes.length;
        this.width = height > 0 ? boxes[0].length : 0;

        // check is grid
        for (int row = 0; row < boxes.length; row++) {
            if (boxes[row].length != width) {
                throw new IllegalArgumentException(
                    "Boxes do not form a grid"
                );
            }
        }

        if (autoNumber)
            autoNumberBoxes(boxes);

        loadWordStarts();
        addCluesToBoxes();
    }

    public Box[][] getBoxes() {
        return boxes;
    }

    /**
     * Assumes height and width has been set
     *
     * See setBoxes for more details, uses autoNumber=true
     */
    public void setBoxesFromList(Box[] boxesList, int width, int height) {
        int i = 0;
        Box[][] boxes = new Box[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boxes[y][x] = boxesList[i++];
            }
        }

        setBoxes(boxes, true);
    }

    public Box[] getBoxesList() {
        Box[] result = new Box[boxes.length * boxes[0].length];
        int i = 0;

        for (int x = 0; x < boxes.length; x++) {
            for (int y = 0; y < boxes[x].length; y++) {
                result[i++] = boxes[x][y];
            }
        }

        return result;
    }

    /**
     * Iterate over clue starts in board order
     *
     * Left to right, top to bottom, across before down
     */
    public Iterable<ClueID> getClueIDs() {
        return new Iterable<ClueID>() {
            public Iterator<ClueID> iterator() {
                return new Iterator<ClueID>() {
                    private final int width = getWidth();
                    private final int height = getHeight();
                    private final Box[][] boxes = getBoxes();

                    // next position (0, 0, across) -> (0, 0, down) -> (0, 1,
                    // across) -> ...
                    private int row = 0;
                    private int col = 0;
                    private boolean across = true;

                    { moveToNext(); }

                    @Override
                    public boolean hasNext() {
                        return row < height;
                    }

                    @Override
                    public ClueID next() {
                        int number = boxes[row][col].getClueNumber();
                        ClueID result = new ClueID(number, across);
                        moveOneStep();
                        moveToNext();
                        return result;
                    }

                    /**
                     * Find next clue/dir position including current position
                     */
                    private void moveToNext() {
                        while (row < height) {
                            Box box = boxes[row][col];
                            if (box != null && box.getClueNumber() > 0) {
                                if (across && box.isAcross())
                                    return;
                                else if (!across && box.isDown())
                                    return;
                            }
                            moveOneStep();
                        }
                    }

                    /**
                     * Move to next position, one step, not to next clue/dir
                     * position
                     */
                    private void moveOneStep() {
                        if (across) {
                            across = false;
                        } else {
                            across = true;
                            col = (col + 1) % width;
                            if (col == 0)
                                row += 1;
                        }
                    }
                };
            }
        };
    }

    /**
     * Initialize the temporary unscramble buffers.  Returns the scrambled solution.
     */
    public byte[] initializeUnscrambleData() {
        unscrambleKey = new int[4];
        unscrambleTmp = new byte[9];

        byte[] solution = getSolutionDown();
        unscrambleBuf = new byte[solution.length];

        return solution;
    }

    private byte[] getSolutionDown() {
        StringBuilder ans = new StringBuilder();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (boxes[y][x] != null) {
                    ans.append(boxes[y][x].getSolution());
                }
            }
        }
        return ans.toString().getBytes();
    }

    public void setUnscrambledSolution(byte[] solution) {
        int i = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (boxes[y][x] != null) {
                    boxes[y][x].setSolution((char) solution[i++]);
                }
            }
        }
        setScrambled(false);
        setUpdatable(false);
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    public String getCopyright() {
        return copyright;
    }

    public void setDate(LocalDate date) {
        this.pubdate = date;
    }

    public LocalDate getDate() {
        return pubdate;
    }

    /**
     * @return the height
     */
    public int getHeight() {
        return height;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getNotes() {
        return notes;
    }

    public int getNumberOfClues() {
        return this.acrossClues.size() + this.downClues.size();
    }

    public int getPercentComplete() {
        int total = 0;
        int correct = 0;

        for (int x = 0; x < boxes.length; x++) {
            for (int y = 0; y < boxes[x].length; y++) {
                if (boxes[x][y] != null) {
                    total++;

                    if (boxes[x][y].getResponse() == boxes[x][y].getSolution()) {
                        correct++;
                    }
                }
            }
        }
        if(total == 0){
            return 0;
        }
        return (correct * 100) / (total);
    }

    public int getPercentFilled() {
        int total = 0;
        int filled = 0;

        for (int x = 0; x < boxes.length; x++) {
            for (int y = 0; y < boxes[x].length; y++) {
                if (boxes[x][y] != null) {
                    total++;

                    if (!boxes[x][y].isBlank()) {
                        filled++;
                    }
                }
            }
        }

        return (filled * 100) / (total);
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSource() {
        return source;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSupportUrl(String supportUrl) {
        this.supportUrl = supportUrl;
    }

    public String getSupportUrl() {
        return supportUrl;
    }

    public void setTime(long time) {
        this.playedTime = time;
    }

    public long getTime() {
        return this.playedTime;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setUpdatable(boolean updatable) {
        this.updatable = updatable;
    }

    public boolean isUpdatable() {
        return updatable;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public Position getPosition() {
        return position;
    }

    /**
     * Set whether current position is across
     */
    public void setAcross(boolean across) {
        this.across = across;
    }

    /**
     * Get whether current position is across
     */
    public boolean getAcross() {
        return across;
    }

    public void setScrambled(boolean scrambled) {
        this.scrambled = scrambled;
    }

    public boolean isScrambled() {
        return scrambled;
    }

    public void setSolutionChecksum(short checksum) {
        this.solutionChecksum = checksum;
    }

    public short getSolutionChecksum() {
        return solutionChecksum;
    }

    /**
     * @return the width
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns null if no note
     */
    public Note getNote(int clueNum, boolean isAcross) {
        if (isAcross)
            return acrossNotes.get(clueNum);
        else
            return downNotes.get(clueNum);
    }

    /**
     * Set note for a clue only if clue exists in puzzle
     */
    public void setNote(int clueNum, Note note, boolean isAcross) {
        if (!getClues(isAcross).hasClue(clueNum))
            return;

        if (isAcross)
            acrossNotes.put(clueNum, note);
        else
            downNotes.put(clueNum, note);
    }

    public Note getPlayerNote() {
        return playerNote;
    }

    public void setPlayerNote(Note note) {
        this.playerNote = note;
    }

    /**
     * Returns true if some box has a solution set
     */
    public boolean hasSolution() {
        if (boxes == null)
            return false;

        for (int row = 0; row < boxes.length; row++) {
            for (int col = 0; col < boxes[row].length; col++) {
                Box box = boxes[row][col];
                if (box != null && box.hasSolution())
                    return true;
            }
        }

        return false;
    }

    /**
     * Returns true if some box has cheated
     */
    public boolean hasCheated() {
        if (boxes == null)
            return false;

        for (int row = 0; row < boxes.length; row++) {
            for (int col = 0; col < boxes[row].length; col++) {
                Box box = boxes[row][col];
                if (box != null && box.isCheated())
                    return true;
            }
        }

        return false;
    }

    /**
     * Returns true if some box has a responder set
     */
    public boolean hasResponders() {
        if (boxes == null)
            return false;

        for (int row = 0; row < boxes.length; row++) {
            for (int col = 0; col < boxes[row].length; col++) {
                Box box = boxes[row][col];
                if (box != null && box.getResponder() != null)
                    return true;
            }
        }

        return false;
    }

    /**
     * Returns true if some box is circled
     */
    public boolean hasCircled() {
        if (boxes == null)
            return false;

        for (int row = 0; row < boxes.length; row++) {
            for (int col = 0; col < boxes[row].length; col++) {
                Box box = boxes[row][col];
                if (box != null && box.isCircled())
                    return true;
            }
        }

        return false;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (Puzzle.class != obj.getClass()) {
            return false;
        }

        Puzzle other = (Puzzle) obj;

        if (!acrossClues.equals(other.acrossClues)) {
            return false;
        }

        if (!downClues.equals(other.downClues)) {
            return false;
        }

        if (!extraClues.equals(other.extraClues)) {
            return false;
        }

        if (author == null) {
            if (other.author != null) {
                return false;
            }
        } else if (!author.equals(other.author)) {
            return false;
        }

        Box[][] b1 = boxes;
        Box[][] b2 = other.boxes;
        boolean boxEq = true;

        for (int x = 0; x < b1.length; x++) {
            for (int y = 0; y < b1[x].length; y++) {
                boxEq = boxEq
                    ? ((b1[x][y] == b2[x][y]) || b1[x][y].equals(b2[x][y]))
                    : boxEq;
            }
        }

        if (!boxEq) {
            return false;
        }

        if (copyright == null) {
            if (other.copyright != null) {
                return false;
            }
        } else if (!copyright.equals(other.copyright)) {
            return false;
        }

        if (height != other.height) {
            return false;
        }

        if (notes == null) {
            if (other.notes != null) {
                return false;
            }
        } else if (!notes.equals(other.notes)) {
            return false;
        }

        if (title == null) {
            if (other.title != null) {
                return false;
            }
        } else if (!title.equals(other.title)) {
            return false;
        }

        if (width != other.width) {
            return false;
        }

        if (scrambled != other.scrambled) {
            return false;
        }

        if (solutionChecksum != other.solutionChecksum) {
            return false;
        }

        if (!acrossNotes.equals(other.acrossNotes)) {
            return false;
        }

        if (!downNotes.equals(other.downNotes)) {
            return false;
        }

        if (!flaggedClues.equals(other.flaggedClues))
            return false;

        if (!Objects.equals(playerNote, other.playerNote))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + acrossClues.hashCode();
        result = (prime * result) + downClues.hashCode();
        result = (prime * result) + ((author == null) ? 0 : author.hashCode());
        result = (prime * result) + Arrays.hashCode(boxes);
        result = (prime * result) +
            ((copyright == null) ? 0 : copyright.hashCode());
        result = (prime * result) + height;
        result = (prime * result) + ((notes == null) ? 0 : notes.hashCode());
        result = (prime * result) + ((title == null) ? 0 : title.hashCode());
        result = (prime * result) + width;
        result = (prime *result) + acrossNotes.hashCode();
        result = (prime *result) + downNotes.hashCode();
        result = (prime * result) + flaggedClues.hashCode();
        result = (prime * result) + extraClues.hashCode();
        result = (prime * result) + Objects.hashCode(playerNote);

        return result;
    }

    @Override
    public String toString() {
        return "Puzzle " + boxes.length + " x " + boxes[0].length + " " +
        this.title;
    }

    public void updateHistory(int clueNumber, boolean across) {
        if (getClues(across).hasClue(clueNumber)) {
            ClueID item = new ClueID(clueNumber, across);
            // if a new item, not equal to most recent
            if (historyList.isEmpty() ||
                !item.equals(historyList.getFirst())) {
                historyList.remove(item);
                historyList.addFirst(item);
            }
        }
    }

    public void setHistory(List<ClueID> newHistory) {
        historyList.clear();
        for (ClueID item : newHistory) {
            int number = item.getClueNumber();
            if (getClues(item.getAcross()).hasClue(number))
                historyList.add(item);
        }
    }

    public List<ClueID> getHistory() {
        return historyList;
    }

    /**
     * Flag or unflag clue
     */
    public void flagClue(ClueID clueNumDir, boolean flag) {
        if (flag)
            flaggedClues.add(clueNumDir);
        else
            flaggedClues.remove(clueNumDir);
    }

    /**
     * Flag or unflag clue
     *
     * Only works for across/down clues
     */
    public void flagClue(Clue clue, boolean flag) {
        if (clue == null)
            return;

        if (clue.isAcross())
            flagClue(clue.getNumber(), true, flag);
        else if (clue.isDown())
            flagClue(clue.getNumber(), false, flag);
    }

    /**
     * Flag or unflag clue
     *
     * Only works for across/down clues
     */
    public void flagClue(int number, boolean across, boolean flag) {
        flagClue(new ClueID(number, across), flag);
    }

    public boolean isFlagged(ClueID clueNumDir) {
        return flaggedClues.contains(clueNumDir);
    }

    /**
     * Always false for non across/down clues
     */
    public boolean isFlagged(Clue clue) {
        if (clue == null)
            return false;
        if (clue.isAcross())
            return isFlagged(new ClueID(clue.getNumber(), true));
        if (clue.isDown())
            return isFlagged(new ClueID(clue.getNumber(), false));
        return false;
    }

    /**
     * Dir assumed down if across is false
     */
    public boolean isFlagged(int number, boolean across) {
        return isFlagged(new ClueID(number, across));
    }

    public Iterable<ClueID> getFlaggedClues() {
        return flaggedClues;
    }

    /**
     * Returns true if the clue can have its own note
     *
     * Currently only supported for across/down clues
     */
    public boolean isNotableClue(Clue clue) {
        if (clue == null)
            return false;
        return clue.isAcross() || clue.isDown();
    }

    /**
     * Number boxes according to standard system
     *
     * @throws IllegalArgumentException if mismatch with existing
     * numbers
     */
    private static void autoNumberBoxes(Box[][] boxes)
            throws IllegalArgumentException {

        int clueCount = 1;

        for (int row = 0; row < boxes.length; row++) {
            for (int col = 0; col < boxes[row].length; col++) {
                if (boxes[row][col] == null) {
                    continue;
                }

                boolean isStart = isStartClue(boxes, row, col, true)
                    || isStartClue(boxes, row, col, false);

                int boxNumber = boxes[row][col].getClueNumber();

                if (isStart) {
                    if (boxNumber > 0 && boxNumber != clueCount) {
                        throw new IllegalArgumentException(
                            "Box clue number " + boxNumber
                                + " does not match expected "
                                + clueCount
                        );
                    }

                    boxes[row][col].setClueNumber(clueCount);
                    clueCount++;
                } else {
                    if (boxNumber > 0) {
                        throw new IllegalArgumentException(
                            "Box at row " + row
                                + " and col " + col
                                + " numbered " + boxNumber
                                + " expected not to be numbered"
                        );
                    }
                }
            }
        }
    }

    /**
     * True if box is start of clue in standard numbering system
     *
     * Regardless of whether there is actually a clue in the puzzle
     */
    public static boolean isStartClue(
        Box[][] boxes, int row, int col, boolean across
    ) {
        if (across) {
            return !joinedLeft(boxes, row, col)
                && joinedRight(boxes, row, col);
        } else {
            return !joinedTop(boxes, row, col)
                && joinedBottom(boxes, row, col);
        }
    }

    /**
     * Go through boxes and add numbered positions to wordStarts
     */
    private void loadWordStarts() {
        for (int row = 0; row < boxes.length; row++) {
            for (int col = 0; col < boxes[row].length; col++) {
                Box box = boxes[row][col];
                if (box != null) {
                    int clueNum = box.getClueNumber();
                    if (clueNum > 0)
                        wordStarts.put(clueNum, new Position(row, col));
                }
            }
        }
    }

    /**
     * Goes through existing clues and makes sure info in boxes
     *
     * E.g. clue starts are marked in the boxes, and
     * ispartofacross/down. Assumes wordStarts loaded.
     */
    private void addCluesToBoxes() {
        for (Clue clue : getClues(true))
            addClueToBoxes(clue);
        for (Clue clue : getClues(false))
            addClueToBoxes(clue);
    }

    /**
     * Marks clue on current boxes
     *
     * E.g. set box as start of the clue or part of the clue.
     *
     * Only supports across/down clues
     */
    private void addClueToBoxes(Clue clue) {
        int number = clue.getNumber();

        Position start = wordStarts.get(number);

        // ignore if not in the boxes
        if (start == null)
            return;

        int row = start.getRow();
        int col = start.getCol();

        Box box = boxes[row][col];

        if (box == null)
            return; // should not happen

        // set start of across/down
        // then mark boxes in word as part of clue
        if (clue.isAcross()) {
            box.setAcross(true);

            int off = -1;
            do {
                off += 1;
                Box midBox = boxes[row][col + off];
                midBox.setPartOfAcrossClueNumber(number);
                midBox.setAcrossPosition(off);
            } while (joinedRight(boxes, row, col + off));
        } else if (clue.isDown()) {
            box.setDown(true);

            int off = -1;
            do {
                off += 1;
                Box midBox = boxes[row + off][col];
                midBox.setPartOfDownClueNumber(number);
                midBox.setDownPosition(off);
            } while (joinedBottom(boxes, row + off, col));
        }
    }
}
