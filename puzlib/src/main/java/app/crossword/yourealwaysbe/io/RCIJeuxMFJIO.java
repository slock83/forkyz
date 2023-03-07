
package app.crossword.yourealwaysbe.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.json.JSONArray;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleBuilder;

/**
 * Format used by RCI Jeux for Mots Fleches puzzles
 *
 * Which are the same as the Swedish crosswords for RaetselZentrale:
 * the grid, with arrows pointing to the box where the answer starts.
 *
 *
 * The basic crossword structure will be playable, but perhaps a
 * completion game is missing.
 */
public class RCIJeuxMFJIO implements PuzzleParser {
    private static final Logger LOG
        = Logger.getLogger(RCIJeuxMFJIO.class.getCanonicalName());

    private static final String ACROSS_LIST = "Horiz.";
    private static final String DOWN_LIST = "Vert.";
    private static final char JOIN_DASH = '–';

    public static class MFJFormatException extends Exception {
        public MFJFormatException(String msg) { super(msg); }
    }

    @Override
    public Puzzle parseInput(InputStream is) throws Exception {
        return readPuzzle(is);
    }

    public static Puzzle readPuzzle(InputStream is) throws IOException {
        try {
            String hjson = getHJSON(is);
            if (hjson == null)
                return null;

            return readPuzzleFromHJSON(
                JsonValue.readHjson(hjson).asObject()
            );
        } catch (MFJFormatException e) {
            LOG.severe("Could not read RCIJeux MFJ: " + e);
            return null;
        }
    }

    /**
     * Extract the HJSON part from the stream
     *
     * Format is usually
     *
     * var gameData = {
     *
     * };
     *
     * just get the { ... } HJSON bit, return as a string, ignore
     * anything outside of the outer { }, doesn't have to be "var
     * gameData"
     */
    private static String getHJSON(InputStream is) throws IOException {
        try (
            BufferedReader reader
                = new BufferedReader(new InputStreamReader(is))
        ) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            String wholeFile = sb.toString();
            int start = wholeFile.indexOf('{');
            int end = wholeFile.lastIndexOf('}');

            if (start < 0 || end < 0)
                return null;
            else
                return wholeFile.substring(start, end + 1);
        }
    }

    /**
     * Read puzzle from RaetselZentrale JSON format
     *
     * Does not include author, date, source.
     */
    private static Puzzle readPuzzleFromHJSON(
        JsonObject hjson
    ) throws MFJFormatException {
        String title = hjson.getString("titre", null);
        if (title == null)
            throw new MFJFormatException("No titre field in HJSON.");

        PuzzleBuilder builder = new PuzzleBuilder(getBoxes(hjson))
            .setTitle(title);

        addClues(hjson, builder);

        return builder.getPuzzle();
    }

    private static Box[][] getBoxes(
        JsonObject hjson
    ) throws MFJFormatException {
        int numRows = hjson.getInt("nbcaseshauteur", -1);
        int numCols = hjson.getInt("nbcaseslargeur", -1);

        if (numRows < 0 || numCols < 0) {
            throw new MFJFormatException(
                "Impossible grid size " + numRows + "x" + numCols + "."
            );
        }

        Box[][] boxes = new Box[numRows][numCols];
        JsonArray rows = asArray(hjson.get("grille"));
        JsonArray vdashes = asArray(hjson.get("spountzV"));
        JsonArray hdashes = asArray(hjson.get("spountzH"));

        for (int row = 0; row < numRows; row++) {
            String cols = asString(rows.get(row));
            for (int col = 0; col < numCols; col++) {
                char cell = cols.charAt(col);
                if (Character.isUpperCase(cell)) {
                    boxes[row][col] = new Box();
                    boxes[row][col].setSolution(String.valueOf(cell));
                    // clue numbers set with clues
                }
            }
        }

        for (JsonValue vdash: vdashes) {
            JsonArray vdash_arr = vdash.asArray();
            boxes[vdash_arr.get(1).asInt()-1][vdash_arr.get(0).asInt()-1].setDashedRight(true);
        }

        for (JsonValue hdash: hdashes) {
            JsonArray hdash_arr = hdash.asArray();
            boxes[hdash_arr.get(1).asInt()-1][hdash_arr.get(0).asInt()-1].setDashedBottom(true);
        }

        return boxes;
    }

    // this is exactly as RaetselZentrale, merge?
    private static void addClues(JsonObject hjson, PuzzleBuilder builder)
            throws MFJFormatException {
        Map<Position, List<ClueInfo>> clueInfos = getClueInfos(hjson);

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
                            throw new MFJFormatException(
                                "Clue has position on a null square "
                                + curPos + clueInfo.hint
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

    private static Map<Position, List<ClueInfo>> getClueInfos(JsonObject hjson)
            throws MFJFormatException {
        Map<Position, List<ClueInfo>> clueInfos = new HashMap<>();

        JsonArray rows = asArray(hjson.get("grille"));
        JsonArray clues = asArray(hjson.get("definitions"));

        int clueIdx = 0;

        for (int row = 0; row < rows.size(); row++) {
            String cols = asString(rows.get(row));
            for (int col = 0; col < cols.length(); col++) {
                char cell = cols.charAt(col);
                if (Character.isLowerCase(cell)) {
                    Arrow arrow = getArrowFromCell(cell);
                    if (arrow.getUpper() == 'z')
                        continue;

                    Position position1 = arrow.getPosition1(row, col);
                    boolean isAcross1 = arrow.getIsAcross1();
                    String hint1 = getHint(asArray(clues.get(clueIdx)));
                    clueIdx += 1;
                    addClueInfo(clueInfos, position1, isAcross1, hint1);

                    if (arrow.getHasTwoClues()) {
                        Position position2 = arrow.getPosition2(row, col);
                        boolean isAcross2 = arrow.getIsAcross2();
                        String hint2 = getHint(asArray(clues.get(clueIdx)));
                        clueIdx += 1;
                        addClueInfo(clueInfos, position2, isAcross2, hint2);
                    }
                }
            }
        }

        return clueInfos;
    }

    private static void addClueInfo(
        Map<Position, List<ClueInfo>> clueInfos,
        Position position, boolean isAcross, String hint
    ) {
        if (!clueInfos.containsKey(position))
            clueInfos.put(position, new LinkedList<ClueInfo>());
        clueInfos.get(position).add(
            new ClueInfo(position, isAcross, hint)
        );
    }

    private static Arrow getArrowFromCell(char cell) throws MFJFormatException {
        for (Arrow arrow : Arrow.values()) {
            if (arrow.getLower() <= cell && cell <= arrow.getUpper())
                return arrow;
        }
        throw new MFJFormatException(
            "Unrecognised arrow type " + cell
        );
    }

    private static String getHint(
        JsonArray clueParts
    ) throws MFJFormatException {
        StringBuilder hint = new StringBuilder();

        for (int i = 0; i < clueParts.size(); i++) {
            hint.append(asString(clueParts.get(i)));
            int length = hint.length();
            if (hint.charAt(length - 1) == JOIN_DASH)
                hint.setLength(length - 1);
            else if (i < clueParts.size() - 1)
                hint.append(" ");
        }

        return hint.toString();
    }

    private static JsonArray asArray(JsonValue val) throws MFJFormatException {
        if (val == null || !val.isArray()) {
            throw new MFJFormatException(
                "Expected " + val + " to be an array."
            );
        }
        return val.asArray();
    }

    private static String asString(JsonValue val) throws MFJFormatException {
        if (val == null || !val.isString()) {
            throw new MFJFormatException(
                "Expect " + val + " to be a string."
            );
        }
        return val.asString();
    }

    private static enum Arrow {
        RIGHT('a', 'a', 0, 1, true),
        DOWN('b', 'b', 1, 0, false),
        RIGHT_DOWN('c', 'c', 0, 1, false),
        LEFT_DOWN('d', 'd', 1, 0, true), // guessed, not seen
        RIGHT_AND_DOWN('e', 'i', 0, 1, true, 1, 0, false),
        RIGHT_DOWN_AND_DOWN('j', 'n', 0, 1, false, 1, 0, false),
        RIGHT_AND_DOWN_RIGHT('o', 's', 0, 1, true, 1, 0, true),
        RIGHT_DOWN_AND_DOWN_RIGHT('t', 'x', 0, 1, false, 1, 0, true),
        NONE('z', 'z', 0, 0, false);

        private final char lower;
        private final char upper;
        // start position of first/second clue associated with arrow
        private int drow1, dcol1;
        private int drow2, dcol2;
        // whether first/second clue is across or down
        private boolean isAcross1;
        private boolean isAcross2;
        // if arrow actually has two clues
        private boolean hasTwoClues;

        Arrow(
            char lower, char upper,
            int drow1, int dcol1, boolean isAcross1
        ) {
            this(lower, upper, drow1, dcol1, isAcross1, 0, 0, false, false);
        }

        Arrow(
            char lower, char upper,
            int drow1, int dcol1, boolean isAcross1,
            int drow2, int dcol2, boolean isAcross2
        ) {
            this(
                lower, upper,
                drow1, dcol1, isAcross1,
                drow2, dcol2, isAcross2,
                true
            );
        }

        private Arrow(
            char lower, char upper,
            int drow1, int dcol1, boolean isAcross1,
            int drow2, int dcol2, boolean isAcross2,
            boolean hasTwoClues
        ) {
            this.lower = lower;
            this.upper = upper;
            this.drow1 = drow1;
            this.dcol1 = dcol1;
            this.isAcross1 = isAcross1;
            this.drow2 = drow2;
            this.dcol2 = dcol2;
            this.isAcross2 = isAcross2;
            this.hasTwoClues = hasTwoClues;
        }

        public char getLower() { return lower; }
        public char getUpper() { return upper; }

        /**
         * Get position of first clue associated to this arrow
         *
         * Given the starting point row/col
         */
        public Position getPosition1(int row, int col) {
            return new Position(row + drow1, col + dcol1);
        }

        public boolean getIsAcross1() { return isAcross1; }

        public Position getPosition2(int row, int col) {
            if (!getHasTwoClues()) {
                throw new IllegalArgumentException(
                    this + " only has one position"
                );
            }
            return new Position(row + drow2, col + dcol2);
        }

        public boolean getIsAcross2() {
            if (!getHasTwoClues()) {
                throw new IllegalArgumentException(
                    this + " only has one position"
                );
            }
            return isAcross2;
        }

        public boolean getHasTwoClues() { return hasTwoClues; }
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
