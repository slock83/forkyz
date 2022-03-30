
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
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleBuilder;

/**
 * Read a character stream of JSON data in the format used by the
 * Guardian.
 */
public class GuardianJSONIO implements PuzzleParser {
    private static final Logger LOG
        = Logger.getLogger(GuardianJSONIO.class.getCanonicalName());

    @Override
    public Puzzle parseInput(InputStream is) throws Exception {
        return readPuzzle(is);
    }

    public static Puzzle readPuzzle(InputStream is) throws IOException {
        try {
            JSONObject json = new JSONObject(new JSONTokener(is));
            return readPuzzleFromJSON(json);
        } catch (JSONException e) {
            LOG.severe("Could not read Guardian JSON: " + e);
            return null;
        }
    }

    public static Puzzle readPuzzle(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            return readPuzzleFromJSON(json);
        } catch (JSONException e) {
            LOG.severe("Could not read Guardian JSON: " + e);
            return null;
        }
    }

    /**
     * Read puzzle from Guardian JSON format
     *
     * Does not set source or support url (this method may be moved to
     * puzlib/io at some point).
     */
    private static Puzzle readPuzzleFromJSON(
        JSONObject json
    ) throws JSONException {
        PuzzleBuilder builder = new PuzzleBuilder(getBoxes(json))
            .setTitle(json.optString("name"));

        JSONObject creator = json.optJSONObject("creator");
        if (creator != null)
            builder.setAuthor(creator.optString("name"));

        if (json.has("date")) {
            long epochMillis = json.getLong("date");
            builder.setDate(
                LocalDate.ofEpochDay(epochMillis / (1000 * 60 * 60 * 24))
            );
        }

        addClues(json, builder);

        return builder.getPuzzle();
    }

    private static Box[][] getBoxes(JSONObject json) throws JSONException {

        JSONObject dimensions = json.getJSONObject("dimensions");
        int numRows = dimensions.getInt("rows");
        int numCols = dimensions.getInt("cols");

        Box[][] boxes = new Box[numRows][numCols];

        JSONArray entries = json.getJSONArray("entries");
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);

            JSONObject position = entry.getJSONObject("position");
            int x = position.getInt("x");
            int y = position.getInt("y");

            if (x < 0 || x >= numCols || y < 0 || y >= numRows)
                continue;

            int num = entry.getInt("number");
            String clueSol = entry.getString("solution");
            String direction = entry.getString("direction");

            int dx = 0;
            int dy = 0;
            if (direction.equals("across"))
                dx = 1;
            else
                dy = 1;

            int boxX = x;
            int boxY = y;
            for (int j = 0; j < clueSol.length(); j++) {
                if (boxX >= numCols || boxY >= numRows)
                    break;

                if (boxes[boxY][boxX] == null)
                    boxes[boxY][boxX] = new Box();
                boxes[boxY][boxX].setSolution(clueSol.charAt(j));

                boxX += dx;
                boxY += dy;
            }

            boxes[y][x].setClueNumber(String.valueOf(num));
        }

        return boxes;
    }

    private static void addClues(JSONObject json, PuzzleBuilder builder)
            throws JSONException {
        JSONArray entries = json.getJSONArray("entries");
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);

            String num = String.valueOf(entry.getInt("number"));
            boolean across = entry.getString("direction").equals("across");
            String hint = entry.getString("clue");

            if (across)
                builder.addAcrossClue(num, hint);
            else
                builder.addDownClue(num, hint);
        }
    }


}
