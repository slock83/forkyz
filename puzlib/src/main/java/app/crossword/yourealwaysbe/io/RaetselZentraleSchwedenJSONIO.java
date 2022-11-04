
package app.crossword.yourealwaysbe.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleBuilder;
import app.crossword.yourealwaysbe.puz.Zone;

/**
 * Format used by RaetselZentrale for Schwedenrätsel
 *
 * Swedish crosswords are the ones where the clues themselves appear in
 * the grid, with arrows pointing to the box where the answer starts.
 */
public class RaetselZentraleSchwedenJSONIO implements PuzzleParser {
    private static final Logger LOG
        = Logger.getLogger(RaetselZentraleSchwedenJSONIO.class.getCanonicalName());

    private static final String ACROSS_LIST = "Hinüber";
    private static final String DOWN_LIST = "Hinunter";
    private static final String WINWORD_LIST = "Lösungswort";

    private static final String WINWORD_HINT = "Lösungswort";

    private static final String ARROW_DOWN_RIGHT = "1";
    private static final String ARROW_UP_RIGHT = "2";
    private static final String ARROW_RIGHT = "3";
    private static final String ARROW_RIGHT_DOWN = "4";
    private static final String ARROW_LEFT_DOWN = "5";
    private static final String ARROW_DOWN = "6";
    private static final String MULTI_CLUE_CELL = ":";
    private static final String EMPTY_CELL = "+";

    public static class RZSFormatException extends Exception {
        public RZSFormatException(String msg) { super(msg); }
    }

    @Override
    public Puzzle parseInput(InputStream is) throws Exception {
        return readPuzzle(is);
    }

    public static Puzzle readPuzzle(InputStream is) throws IOException {
        try {
            JSONObject json = new JSONObject(new JSONTokener(is));
            return readPuzzleFromJSON(json);
        } catch (JSONException | RZSFormatException e) {
            LOG.severe("Could not read RaetselZentrale JSON: " + e);
            return null;
        }
    }

    public static Puzzle readPuzzle(String jsonString) throws RZSFormatException {
        try {
            JSONObject json = new JSONObject(jsonString);
            return readPuzzleFromJSON(json);
        } catch (JSONException e) {
            LOG.severe("Could not read Guardian JSON: " + e);
            return null;
        }
    }

    /**
     * Read puzzle from RaetselZentrale JSON format
     *
     * Does not include author, date, source.
     */
    private static Puzzle readPuzzleFromJSON(
        JSONObject json
    ) throws JSONException, RZSFormatException {
        String type = json.getString("type");
        if (!type.toLowerCase().equals("sweden")) {
            throw new RZSFormatException(
                "Unsupported puzzle type " + type + "."
            );
        }

        if (json.optBoolean("encrypted", false)) {
            throw new RZSFormatException(
                "Encrypted puzzles not supported."
            );
        }

        JSONObject payload = json.getJSONObject("payload");

        PuzzleBuilder builder = new PuzzleBuilder(getBoxes(payload))
            .setTitle(json.getString("name"));

        addClues(payload, builder);
        addWinWord(payload, builder);

        return builder.getPuzzle();
    }

    private static Box[][] getBoxes(JSONObject json) throws RZSFormatException {
        int numRows = json.getInt("height");
        int numCols = json.getInt("width");

        Box[][] boxes = new Box[numRows][numCols];

        JSONArray grid = json.getJSONArray("grid");
        JSONObject solution = json.getJSONObject("solution");

        addGridToBoxes(grid, boxes);

        return boxes;
    }

    private static void addGridToBoxes(JSONArray grid, Box[][] boxes) {
        for (int row = 0; row < grid.length(); row++) {
            JSONArray cellRow = grid.getJSONArray(row);
            for (int col = 0; col < cellRow.length(); col++) {
                String cell = cellRow.getString(col);

                if (!isBlankCell(cell)) {
                    boxes[row][col] = new Box();
                    boxes[row][col].setSolution(cell);
                }

                // clue numbers added with clues
            }
        }
    }

    /**
     * Takes payload json, adds clues to builder
     */
    private static void addClues(JSONObject json, PuzzleBuilder builder)
            throws RZSFormatException {
        Map<Position, List<ClueInfo>> clueInfos = getClueInfos(json);

        int nextClueNumber = 1;
        Position curPos = new Position(0, 0);

        for (int row = 0; row < builder.getHeight(); row++) {
            curPos.setRow(row);
            for (int col = 0; col < builder.getWidth(); col++) {
                curPos.setCol(col);
                List<ClueInfo> posClueInfos = clueInfos.get(curPos);
                if (posClueInfos != null) {
                    for (ClueInfo clueInfo : posClueInfos) {
                        Box box = builder.getBox(curPos);
                        if (box == null) {
                            throw new RZSFormatException(
                                "Clue has position on a null square "
                                + curPos
                            );
                        }

                        String clueNumber;
                        if (box.hasClueNumber()) {
                            clueNumber = box.getClueNumber();
                        } else {
                            clueNumber = String.valueOf(nextClueNumber);
                            nextClueNumber += 1;
                            builder.setBoxClueNumber(
                                row, col, clueNumber
                            );
                        }

                        if (clueInfo.getIsAcross()) {
                            builder.addAcrossClue(
                                ACROSS_LIST, clueNumber, clueInfo.getHint()
                            );
                        } else {
                            builder.addDownClue(
                                DOWN_LIST, clueNumber, clueInfo.getHint()
                            );
                        }
                    }
                }
            }
        }
    }

    /**
     * Takes payload json, adds winword to builder
     */
    private static void addWinWord(JSONObject json, PuzzleBuilder builder)
            throws RZSFormatException {
        Box[][] boxes = builder.getPuzzle().getBoxes();

        JSONObject solution = json.optJSONObject("solution");
        if (solution == null)
            return;

        JSONArray positions = solution.getJSONArray("matrix");
        Zone zone = new Zone();
        for (int i = 0; i < positions.length(); i++) {
            JSONArray position = positions.getJSONArray(i);
            int row = position.getInt(1) - 1;
            int col = position.getInt(0) - 1;
            zone.addPosition(new Position(row, col));

            String[][] marks = new String[3][3];
            marks[2][2] = String.valueOf(i + 1);
            boxes[row][col].setMarks(marks);
        }

        builder.addClue(new Clue(WINWORD_LIST, 0, null, WINWORD_HINT, zone));
    }

    private static Map<Position, List<ClueInfo>> getClueInfos(JSONObject json)
            throws RZSFormatException {
        Map<Position, List<ClueInfo>> clueInfos = new HashMap<>();

        JSONArray clues = json.getJSONArray("words");
        for (int i = 0; i < clues.length(); i++) {
            JSONObject clue = clues.getJSONObject(i);

            JSONObject position = clue.getJSONObject("position");
            int row = position.getInt("row") - 1;
            int col = position.getInt("col") - 1;


            // -+ to split words across a line
            // just + to split not during a word
            String hint = clue.getString("question")
                .replace("-+", "")
                .replace("+", "");

            String arrowType = clue.getString("arrowtype");

            Position trueStart = getTrueStart(row, col, arrowType);
            boolean isAcross = getIsAcross(arrowType);

            if (!clueInfos.containsKey(trueStart))
                clueInfos.put(trueStart, new LinkedList<ClueInfo>());

            clueInfos.get(trueStart)
                .add(new ClueInfo(trueStart, isAcross, hint));

        }

        return clueInfos;
    }

    /**
     * Where the word actually starts given the clue position
     */
    private static Position getTrueStart(int row, int col, String arrowType)
            throws RZSFormatException {
        if (ARROW_DOWN_RIGHT.equals(arrowType))
            return new Position(row + 1, col);
        if (ARROW_UP_RIGHT.equals(arrowType))
            return new Position(row - 1, col);
        if (ARROW_RIGHT.equals(arrowType))
            return new Position(row, col + 1);
        if (ARROW_RIGHT_DOWN.equals(arrowType))
            return new Position(row, col + 1);
        if (ARROW_LEFT_DOWN.equals(arrowType))
            return new Position(row, col - 1);
        if (ARROW_DOWN.equals(arrowType))
            return new Position(row + 1, col);

        throw new RZSFormatException("Unexpected arrow type: " + arrowType);
    }

    /**
     * Whether arrow says clue is across (or down)
     */
    private static boolean getIsAcross(String arrowType)
            throws RZSFormatException {
        if (ARROW_DOWN_RIGHT.equals(arrowType))
            return true;
        if (ARROW_UP_RIGHT.equals(arrowType))
            return true;
        if (ARROW_RIGHT.equals(arrowType))
            return true;
        if (ARROW_RIGHT_DOWN.equals(arrowType))
            return false;
        if (ARROW_LEFT_DOWN.equals(arrowType))
            return false;
        if (ARROW_DOWN.equals(arrowType))
            return false;

        throw new RZSFormatException("Unexpected arrow type: " + arrowType);
    }

    private static boolean isBlankCell(String cellValue) {
        if (cellValue == null)
            return true;
        if (ARROW_DOWN_RIGHT.equals(cellValue))
            return true;
        if (ARROW_UP_RIGHT.equals(cellValue))
            return true;
        if (ARROW_RIGHT.equals(cellValue))
            return true;
        if (ARROW_RIGHT_DOWN.equals(cellValue))
            return true;
        if (ARROW_LEFT_DOWN.equals(cellValue))
            return true;
        if (ARROW_DOWN.equals(cellValue))
            return true;
        if (MULTI_CLUE_CELL.equals(cellValue))
            return true;
        if (EMPTY_CELL.equals(cellValue))
            return true;
        return false;
    }

    private static class ClueInfo {
        private Position pos;
        private boolean isAcross;
        private String hint;

        public ClueInfo(Position pos, boolean isAcross, String hint) {
            this.pos = pos;
            this.isAcross = isAcross;
            this.hint = hint;
        }

        public Position getPosition() { return pos; }
        public boolean getIsAcross() { return isAcross; }
        public String getHint() { return hint; }
    }
}
