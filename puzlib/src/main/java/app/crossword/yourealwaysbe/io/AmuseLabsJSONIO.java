
package app.crossword.yourealwaysbe.io;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.Puzzle;

/**
 * Read a character stream of JSON data in the format used by the
 * Amuse Labs.
 */
public class AmuseLabsJSONIO implements PuzzleParser {
    private static final Logger LOG
        = Logger.getLogger(AmuseLabsJSONIO.class.getCanonicalName());

    /**
     * An unfancy exception indicating error while parsing
     */
    public static class AmuseLabsFormatException extends Exception {
        public AmuseLabsFormatException(String msg) { super(msg); }
    }

    @Override
    public Puzzle parseInput(InputStream is) throws Exception {
        return readPuzzle(is);
    }

    public static Puzzle readPuzzle(InputStream is) throws IOException {
        try {
            JSONObject json = new JSONObject(new JSONTokener(is));
            return readPuzzleFromJSON(json);
        } catch (AmuseLabsFormatException | JSONException e) {
            LOG.severe("Could not read Amuse Labs JSON: " + e);
            return null;
        }
    }

    public static Puzzle readPuzzle(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            return readPuzzleFromJSON(json);
        } catch (AmuseLabsFormatException | JSONException e) {
            LOG.severe("Could not read Amuse Labs JSON: " + e);
            return null;
        }
    }

    /**
     * Read puzzle from Amuse Labs JSON format
     */
    private static Puzzle readPuzzleFromJSON(
        JSONObject json
    ) throws JSONException, AmuseLabsFormatException {
        Puzzle puz = new Puzzle();

        puz.setTitle(json.optString("title"));
        puz.setAuthor(json.optString("author"));
        puz.setCopyright(json.optString("copyright"));
        puz.setSource(json.optString("publisher"));

        if (json.has("publishTime")) {
            long epochMillis = json.getLong("publishTime");
            puz.setDate(
                LocalDate.ofEpochDay(epochMillis / (1000 * 60 * 60 * 24))
            );
        }

        try {
            puz.setBoxes(getBoxes(json), false);
        } catch (IllegalArgumentException e) {
            throw new AmuseLabsFormatException("Could not set grid boxes from data file: " + e.getMessage());
        }

        addClues(json, puz);

        return puz;
    }

    private static Box[][] getBoxes(JSONObject json)
            throws JSONException, AmuseLabsFormatException {

        int numRows = json.getInt("h");
        int numCols = json.getInt("w");

        Box[][] boxes = new Box[numRows][numCols];

        JSONArray cols = json.getJSONArray("box");
        for (int col = 0; col < cols.length(); col++) {
            JSONArray rows = cols.getJSONArray(col);
            for (int row = 0; row < rows.length(); row++) {
                String entryString = rows.getString(row);

                if (entryString.length() != 1) {
                    throw new AmuseLabsFormatException(
                        "Don't know what to do with box contents "
                            + "that is not a single character: "
                            + entryString
                    );
                }

                char entry = entryString.charAt(0);

                if (entry != 0) {
                    boxes[row][col] = new Box();
                    boxes[row][col].setSolution(entry);
                }
            }
        }

        cols = json.optJSONArray("clueNums");
        if (cols != null) {
            for (int col = 0; col < cols.length(); col++) {
                JSONArray rows = cols.getJSONArray(col);
                for (int row = 0; row < rows.length(); row++) {
                    int clueNum = rows.getInt(row);

                    if (clueNum > 0) {
                        if (boxes[row][col] == null) {
                            boxes[row][col] = new Box();
                        }
                        boxes[row][col].setClueNumber(clueNum);
                    }
                }
            }
        }

        JSONArray cellInfos = json.optJSONArray("cellInfos");
        if (cellInfos != null) {
            for (int i = 0; i < cellInfos.length(); i++) {
                JSONObject cellInfo = cellInfos.getJSONObject(i);

                int row = cellInfo.getInt("y");
                int col = cellInfo.getInt("x");
                boolean circled = cellInfo.optBoolean("isCircled");

                if (circled) {
                    if (boxes[row][col] == null) {
                        boxes[row][col] = new Box();
                    }
                    boxes[row][col].setCircled(true);
                }
            }
        }

        return boxes;
    }

    private static void addClues(JSONObject json, Puzzle puz)
            throws JSONException {
        JSONArray entries = json.getJSONArray("placedWords");

        if (entries == null)
            return;

        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);

            int num = entry.getInt("clueNum");
            if (num > 0) {
                String listName = entry.getBoolean("acrossNotDown")
                    ? Clue.ACROSS
                    : Clue.DOWN;
                String clue = entry.getJSONObject("clue").getString("clue");

                puz.addClue(new Clue(num, listName, clue));
            }
        }
    }
}
