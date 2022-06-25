
package app.crossword.yourealwaysbe.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleBuilder;
import app.crossword.yourealwaysbe.puz.Zone;
import app.crossword.yourealwaysbe.util.PuzzleUtils;

/**
 * Read a character stream of JSON data in the format used by the
 * Guardian.
 */
public class GuardianJSONIO implements PuzzleParser {
    private static final Logger LOG
        = Logger.getLogger(GuardianJSONIO.class.getCanonicalName());

    private static final String ACROSS_LIST = "Across";
    private static final String DOWN_LIST = "Down";

    @Override
    public Puzzle parseInput(InputStream is) throws Exception {
        return readPuzzle(is);
    }

    public static Puzzle readPuzzle(InputStream is) throws IOException {
        ByteArrayInputStream copy = StreamUtils.copyInputStream(is);
        Puzzle puz = readFromJSON(copy);
        if (puz == null)
            puz = readFromHTML(copy);
        return puz;
    }

    /**
     * Returns null if failed
     */
    public static Puzzle readFromJSON(InputStream is) {
        try {
            JSONObject json = new JSONObject(new JSONTokener(is));
            return readPuzzleFromJSON(json);
        } catch (JSONException e) {
            return null;
        }
    }

    public static Puzzle readFromHTML(InputStream is) {
        try {
            Document doc = Jsoup.parse(is, null, "");
            String cwJson = doc.select(".js-crossword")
                               .attr("data-crossword-data");
            if (!cwJson.isEmpty()) {
                return readPuzzleFromJSON(
                    new JSONObject(new JSONTokener(cwJson))
                );
            }
        } catch (IOException e) {
            // pass through
        }
        return null;
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

            String num = String.valueOf(entry.getInt("number"));
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

            boxes[y][x].setClueNumber(num);
        }

        return boxes;
    }

    private static void addClues(JSONObject json, PuzzleBuilder builder)
            throws JSONException {
        Map<String, Zone> zones = getPuzzleZones(json, builder);

        JSONArray entries = json.getJSONArray("entries");
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);
            addClue(entry, zones, builder);
        }
    }

    private static void addClue(
        JSONObject entry,
        Map<String, Zone> zones,
        PuzzleBuilder builder
    ) {
        String id = entry.getString("id");
        String num = entry.getString("humanNumber");
        boolean across = entry.getString("direction").equals("across");
        String hint = entry.getString("clue");

        JSONArray group = entry.getJSONArray("group");
        Zone zone = new Zone();

        if (group.length() > 0 && id.equals(group.getString(0))) {
            for (int i = 0; i < group.length(); i++)
                zone.appendZone(zones.get(group.getString(i)));
        }

        String listName = across ? ACROSS_LIST : DOWN_LIST;
        int index = builder.getNextClueIndex(listName);

        builder.addClue(new Clue(listName, index, num, hint, zone));
    }

    /**
     * Gets map from clue id to zone on board
     */
    private static Map<String, Zone> getPuzzleZones(
        JSONObject json, PuzzleBuilder builder
    ) {
        Map<String, Zone> zones = new HashMap<>();

        JSONArray entries = json.getJSONArray("entries");
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);

            String id = entry.getString("id");

            JSONObject position = entry.getJSONObject("position");
            int x = position.getInt("x");
            int y = position.getInt("y");
            boolean across = entry.getString("direction").equals("across");

            Puzzle puz = builder.getPuzzle();
            Position start = new Position(y, x);

            Zone zone = across
                ? PuzzleUtils.getAcrossZone(puz, start)
                : PuzzleUtils.getDownZone(puz, start);

            zones.put(id, zone);
        }

        return zones;
    }
}
