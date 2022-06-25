package app.crossword.yourealwaysbe.puz;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import app.crossword.yourealwaysbe.util.CollectionUtils;

import java.util.logging.Logger;

/**
 * Represent a puzzle
 *
 * String should use HTML for formatting (including new lines), except
 * Note objects which do not support it.
 */
public class Puzzle implements Serializable{
    private static final Logger LOG = Logger.getLogger("app.crossword.yourealwaysbe");

    private String author;
    private String copyright;
    private String notes;
    private String title;
    private Map<String, MutableClueList> clueLists = new HashMap<>();
    private LocalDate pubdate = LocalDate.now();
    private String source;
    private String sourceUrl;
    private String shareUrl;
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
    private ClueID currentclueID;

    private Map<ClueID, Note> clueNotes = new HashMap<>();
    private Note playerNote;

    private LinkedList<ClueID> historyList = new LinkedList<>();

    private Set<ClueID> flaggedClues = new HashSet<>();
    private List<PuzImage> images = new LinkedList<>();

    // Temporary fields used for unscrambling.
    public int[] unscrambleKey;
    public byte[] unscrambleTmp;
    public byte[] unscrambleBuf;

    public void addClue(Clue clue) {
        String listName = clue.getClueID().getListName();
        if (!clueLists.containsKey(listName))
            clueLists.put(listName, new MutableClueList());
        clueLists.get(listName).addClue(clue);
        addClueToBoxes(clue);
    }

    public ClueList getClues(String listName) {
        if (listName != null)
            return clueLists.get(listName);
        else
            return null;
    }

    public Iterable<Clue> getAllClues() {
        Collection<Clue> cids = Collections.emptySet();
        for (String listName : getClueListNames()) {
            cids = CollectionUtils.join(
                cids,
                getClues(listName).getClues()
            );
        }
        return cids;
    }

    public Set<String> getClueListNames() {
        return clueLists.keySet();
    }

    public boolean hasClue(ClueID clueID) {
        if (clueID == null)
            return false;

        ClueList clues = getClues(clueID.getListName());

        if (clues == null)
            return false;

        if (!clues.hasClueByIndex(clueID.getIndex()))
            return false;

        return true;
    }

    /**
     * Get clue by id
     *
     * @return clue or null
     */
    public Clue getClue(ClueID clueID) {
        if (clueID == null)
            return null;

        ClueList clues = getClues(clueID.getListName());

        return (clues == null)
            ? null
            : clues.getClueByIndex(clueID.getIndex());
    }

    public int getNumberOfClues() {
        int count = 0;
        for (String listName : getClueListNames())
            count += getClues(listName).size();
        return count;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAuthor() {
        return author;
    }

    /**
     * Return null if index out of range or not a box
     */
    public Box checkedGetBox(int row, int col) {
        if (row < 0 || row >= boxes.length)
            return null;
        if (col < 0 || col >= boxes[row].length)
            return null;
        return boxes[row][col];
    }

    /**
     * Return null if index out of range or not a box
     */
    public Box checkedGetBox(Position p) {
        if (p != null)
            return checkedGetBox(p.getRow(), p.getCol());
        else
            return null;
    }

    /**
     * Set boxes for puzzle
     *
     * Also sets height and width and fills in "is part of clue" info in
     * boxes.
     *
     * @param boxes boxes in row, col order, null means black square.
     * Must be a true grid.
     * @throws IllegalArgumentException if the boxes are not a grid.
     */
    public void setBoxes(Box[][] boxes) {
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

        addCluesToBoxes();
    }

    public Box[][] getBoxes() {
        return boxes;
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
     * Left to right, top to bottom, clue list order by name (across
     * before down in particular).
     *
     * Caution: will not find clues whose IDs don't neatly match the
     * numbers on the board.
     */
    public Iterable<ClueID> getBoardClueIDs() {
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
                    private int listIndex = 0;
                    private List<String> listNames = new ArrayList<>(
                        Puzzle.this.clueLists.keySet()
                    );

                    {
                        Collections.sort(listNames);
                        moveToNext();
                    }

                    @Override
                    public boolean hasNext() {
                        return row < height;
                    }

                    @Override
                    public ClueID next() {
                        String number = boxes[row][col].getClueNumber();
                        ClueList curList = getCurrentClueList();
                        // not be null by moveToNext
                        Clue curClue = curList.getClueByNumber(number);
                        ClueID result = curClue.getClueID();
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
                            if (box != null && box.hasClueNumber()) {
                                String number = box.getClueNumber();
                                ClueList clues = getCurrentClueList();
                                if (clues.hasClueByNumber(number))
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
                        if (listIndex < listNames.size() - 1) {
                            listIndex += 1;
                        } else {
                            listIndex = 0;
                            col = (col + 1) % width;
                            if (col == 0)
                                row += 1;
                        }
                    }

                    private String getCurrentListName() {
                        return listNames.get(listIndex);
                    }

                    private ClueList getCurrentClueList() {
                        return Puzzle.this.getClues(getCurrentListName());
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

    public int getPercentComplete() {
        int total = 0;
        int correct = 0;

        for (int x = 0; x < boxes.length; x++) {
            for (int y = 0; y < boxes[x].length; y++) {
                if (boxes[x][y] != null) {
                    total++;

                    if (Objects.equals(
                        boxes[x][y].getResponse(), boxes[x][y].getSolution()
                    )) {
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

    /**
     * The actual URL of the puzzle
     */
    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    /**
     * A URL where the puzzle creator can be supported in some way
     *
     * Either by visits or money
     */
    public void setSupportUrl(String supportUrl) {
        this.supportUrl = supportUrl;
    }

    public String getSupportUrl() {
        return supportUrl;
    }

    /**
     * The URL to use if you want to share the puzzle with someone
     *
     * E.g. the puzzle page of the site it came from (as opposed to the
     * backend .puz link
     */
    public void setShareUrl(String shareUrl) {
        this.shareUrl = shareUrl;
    }

    public String getShareUrl() {
        return shareUrl;
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
    public void setCurrentClueID(ClueID clueID) {
        this.currentclueID = clueID;
    }

    /**
     * Get currently selected clue id or null
     */
    public ClueID getCurrentClueID() {
        return currentclueID;
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
    public Note getNote(ClueID clueID) {
        return clueNotes.get(clueID);
    }

    public Note getNote(Clue clue) {
        return clue == null ? null : getNote(clue.getClueID());
    }

    /**
     * Set note for a clue only if clue exists in puzzle
     */
    public void setNote(ClueID clueID, Note note) {
        if (!hasClue(clueID))
            return;
        clueNotes.put(clueID, note);
    }

    public void setNote(Clue clue, Note note) {
        if (clue != null)
            setNote(clue.getClueID(), note);
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

        if (!clueLists.equals(other.clueLists)) {
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

        for (int x = 0; x < b1.length; x++) {
            for (int y = 0; y < b1[x].length; y++) {
                if (!((b1[x][y] == b2[x][y]) || b1[x][y].equals(b2[x][y]))) {
                    return false;
                }
            }
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

        if (!clueNotes.equals(other.clueNotes)) {
            return false;
        }

        if (!flaggedClues.equals(other.flaggedClues))
            return false;

        if (!Objects.equals(playerNote, other.playerNote))
            return false;

        if (!Objects.equals(images, other.images))
            return false;

        if (!Objects.equals(sourceUrl, other.sourceUrl))
            return false;

        if (!Objects.equals(supportUrl, other.supportUrl))
            return false;

        if (!Objects.equals(shareUrl, other.shareUrl))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + clueLists.hashCode();
        result = (prime * result) + ((author == null) ? 0 : author.hashCode());
        result = (prime * result) + Arrays.hashCode(boxes);
        result = (prime * result) +
            ((copyright == null) ? 0 : copyright.hashCode());
        result = (prime * result) + height;
        result = (prime * result) + ((notes == null) ? 0 : notes.hashCode());
        result = (prime * result) + ((title == null) ? 0 : title.hashCode());
        result = (prime * result) + width;
        result = (prime *result) + clueNotes.hashCode();
        result = (prime * result) + flaggedClues.hashCode();
        result = (prime * result) + Objects.hashCode(playerNote);
        result = (prime * result) + Objects.hashCode(images);
        result = (prime * result)
            + Objects.hash(sourceUrl, supportUrl, shareUrl);

        return result;
    }

    @Override
    public String toString() {
        return "Puzzle " + boxes.length + " x " + boxes[0].length + " " +
        this.title;
    }

    public void updateHistory(ClueID clueID) {
        if (clueID == null)
            return;

        if (hasClue(clueID)) {
            // if a new item, not equal to most recent
            if (historyList.isEmpty() ||
                !clueID.equals(historyList.getFirst())) {
                historyList.remove(clueID);
                historyList.addFirst(clueID);
            }
        }
    }

    public void setHistory(List<ClueID> newHistory) {
        historyList.clear();
        for (ClueID item : newHistory) {
            if (hasClue(item))
                historyList.add(item);
        }
    }

    public List<ClueID> getHistory() {
        return historyList;
    }

    /**
     * Flag or unflag clue
     */
    public void flagClue(ClueID clueID, boolean flag) {
        if (clueID == null)
            return;

        if (flag)
            flaggedClues.add(clueID);
        else
            flaggedClues.remove(clueID);
    }

    public void flagClue(Clue clue, boolean flag) {
        if (clue != null)
            flagClue(clue.getClueID(), flag);
    }

    public boolean isFlagged(ClueID clueID) {
        return flaggedClues.contains(clueID);
    }

    public boolean isFlagged(Clue clue) {
        return (clue == null) ? false : isFlagged(clue.getClueID());
    }

    public Collection<ClueID> getFlaggedClues() {
        return flaggedClues;
    }

    public void addImage(PuzImage image) {
        images.add(image);
    }

    public List<PuzImage> getImages() {
        return images;
    }

    /**
     * Goes through existing clues and makes sure info in boxes
     *
     * E.g. clue starts are marked in the boxes, and
     * ispartofacross/down.
     */
    private void addCluesToBoxes() {
        for (String listName : getClueListNames())
            for (Clue clue : getClues(listName))
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
        Zone zone = (clue == null) ? null : clue.getZone();
        if (zone == null)
            return;

        String number = clue.getClueNumber();

        for (int offset = 0; offset < zone.size(); offset++) {
            Position pos = zone.getPosition(offset);
            Box box = checkedGetBox(pos);
            if (box == null) {
                throw new IllegalArgumentException(
                    "Clue " + number + " " + clue.getClueID().getListName()
                    + " zone has a null box at position " + pos
                );
            }
            box.setCluePosition(clue.getClueID(), offset);
        }
    }
}
