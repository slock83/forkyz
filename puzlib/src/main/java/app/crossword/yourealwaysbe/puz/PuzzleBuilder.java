
package app.crossword.yourealwaysbe.puz;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.crossword.yourealwaysbe.util.PuzzleUtils;

public class PuzzleBuilder {
    private final Puzzle puzzle = new Puzzle();
    private Map<String, Position> numberPositions;

    /**
     * Convenience class for partial clues
     */
    public static class NumHint {
        private String num, hint;

        public NumHint(String num, String hint) {
            this.num = num;
            this.hint = hint;
        }

        public String getNum() { return num; }
        public String getHint() { return hint; }
    }

    public PuzzleBuilder(Box[][] boxes) {
        puzzle.setBoxes(boxes);
    }

    public PuzzleBuilder(Box[] boxesList, int width, int height) {
        int i = 0;
        Box[][] boxes = new Box[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boxes[y][x] = boxesList[i++];
            }
        }
        puzzle.setBoxes(boxes);
    }

    /**
     * Provides access to puzzle methods directly
     */
    public Puzzle getPuzzle() { return puzzle; }
    public int getWidth() { return puzzle.getWidth(); }
    public int getHeight() { return puzzle.getHeight(); }
    public Iterable<ClueID> getBoardClueIDs() {
        return puzzle.getBoardClueIDs();
    }

    public Box getBox(int row, int col) {
        return puzzle.checkedGetBox(row, col);
    }

    public Box getBox(Position position) {
        return puzzle.checkedGetBox(position);
    }

    public PuzzleBuilder addClue(Clue clue) {
        puzzle.addClue(clue);
        return this;
    }

    public PuzzleBuilder setSolutionChecksum(short checkSum) {
        puzzle.setSolutionChecksum(checkSum);
        return this;
    }

    public PuzzleBuilder setScrambled(boolean scrambled) {
        puzzle.setScrambled(scrambled);
        return this;
    }

    public PuzzleBuilder setTitle(String title) {
        puzzle.setTitle(title);
        return this;
    }

    public PuzzleBuilder setAuthor(String author) {
        puzzle.setAuthor(author);
        return this;
    }

    public PuzzleBuilder setCopyright(String copyright) {
        puzzle.setCopyright(copyright);
        return this;
    }

    public PuzzleBuilder setNotes(String notes) {
        puzzle.setNotes(notes);
        return this;
    }

    public PuzzleBuilder setPlayerNote(Note playerNote) {
        puzzle.setPlayerNote(playerNote);
        return this;
    }

    public PuzzleBuilder setNote(ClueID cid, Note note) {
        puzzle.setNote(cid, note);
        return this;
    }

    public PuzzleBuilder setDate(LocalDate date) {
        puzzle.setDate(date);
        return this;
    }

    public PuzzleBuilder setSource(String source) {
        puzzle.setSource(source);
        return this;
    }

    public PuzzleBuilder setSourceUrl(String sourceUrl) {
        puzzle.setSourceUrl(sourceUrl);
        return this;
    }

    public PuzzleBuilder setSupportUrl(String supportUrl) {
        puzzle.setSupportUrl(supportUrl);
        return this;
    }

    public PuzzleBuilder setTime(long time) {
        puzzle.setTime(time);
        return this;
    }

    public PuzzleBuilder setUpdatable(boolean updateable) {
        puzzle.setUpdatable(updateable);
        return this;
    }

    public PuzzleBuilder flagClue(ClueID clueId, boolean flag) {
        puzzle.flagClue(clueId, flag);
        return this;
    }

    public PuzzleBuilder setPosition(Position position) {
        puzzle.setPosition(position);
        return this;
    }

    public PuzzleBuilder setCurrentClueID(ClueID clueId) {
        puzzle.setCurrentClueID(clueId);
        return this;
    }

    public PuzzleBuilder setHistory(List<ClueID> history) {
        puzzle.setHistory(history);
        return this;
    }

    /**
     * Number puzzle boxes according to standard system
     *
     * @throws IllegalArgumentException if mismatch with existing
     * numbers
     */
    public PuzzleBuilder autoNumberBoxes() throws IllegalArgumentException {
        int clueCount = 1;

        for (int row = 0; row < puzzle.getHeight(); row++) {
            for (int col = 0; col < puzzle.getWidth(); col++) {
                Box box = puzzle.checkedGetBox(row, col);

                if (box == null) {
                    continue;
                }

                boolean isStart = isStartClue(row, col, true)
                    || isStartClue(row, col, false);

                String boxNumber = box.getClueNumber();
                String autoNumber = String.valueOf(clueCount);

                if (isStart) {
                    if (boxNumber != null && !boxNumber.equals(autoNumber)) {
                        throw new IllegalArgumentException(
                            "Box clue number " + boxNumber
                                + " does not match expected "
                                + clueCount
                        );
                    }

                    box.setClueNumber(autoNumber);
                    clueCount++;
                } else {
                    if (boxNumber != null) {
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

        return this;
    }

    public PuzzleBuilder addAcrossClue(String listName, NumHint numHint) {
        return addAcrossClue(listName, numHint.getNum(), numHint.getHint());
    }

    public PuzzleBuilder addAcrossClue(
        String listName, String number, String hint
    ) {
        Position start = getNumberPositions().get(number);
        if (start == null) {
            throw new IllegalArgumentException(
                "Can't add clue " + number + " to board: "
                + "the number is not in the boxes."
            );
        }

        Zone zone = new Zone();

        int row = start.getRow();
        int col = start.getCol();

        int off = -1;
        do {
            off += 1;
            zone.addPosition(new Position(row, col + off));
        } while (joinedRight(row, col + off));

        puzzle.addClue(new Clue(number, listName, hint, zone));

        return this;
    }

    public PuzzleBuilder addDownClue(String listName, NumHint numHint) {
        return addDownClue(listName, numHint.getNum(), numHint.getHint());
    }

    public PuzzleBuilder addDownClue(
        String listName, String number, String hint
    ) {
        Position start = getNumberPositions().get(number);
        if (start == null) {
            throw new IllegalArgumentException(
                "Can't add clue " + number + " to board: "
                + "the number is not in the boxes."
            );
        }

        Zone zone = new Zone();

        int row = start.getRow();
        int col = start.getCol();

        int off = -1;
        do {
            off += 1;
            zone.addPosition(new Position(row + off, col));
        } while (joinedBottom(row + off, col));

        puzzle.addClue(new Clue(number, listName, hint, zone));

        return this;
    }

    /**
     * True if box is start of clue in standard numbering system
     *
     * Regardless of whether there is actually a clue in the puzzle
     */
    public boolean isStartClue(int row, int col, boolean across) {
        if (across) {
            return !joinedLeft(row, col) && joinedRight(row, col);
        } else {
            return !joinedTop(row, col) && joinedBottom(row, col);
        }
    }

    private Map<String, Position> getNumberPositions() {
        if (numberPositions == null) {
            numberPositions = new HashMap<>();
            Box[][] boxes = puzzle.getBoxes();

            for (int row = 0; row < puzzle.getHeight(); row++) {
                for (int col = 0; col < puzzle.getWidth(); col++) {
                    Box box = boxes[row][col];
                    if (box != null && box.hasClueNumber()) {
                        numberPositions.put(
                            box.getClueNumber(), new Position(row, col)
                        );
                    }
                }
            }
        }

        return numberPositions;
    }

    private boolean joinedTop(int row, int col) {
        return PuzzleUtils.joinedTop(puzzle, row, col);
    }

    private boolean joinedBottom(int row, int col) {
        return PuzzleUtils.joinedBottom(puzzle, row, col);
    }

    private boolean joinedLeft(int row, int col) {
        return PuzzleUtils.joinedLeft(puzzle, row, col);
    }

    private boolean joinedRight(int row, int col) {
        return PuzzleUtils.joinedRight(puzzle, row, col);
    }
}