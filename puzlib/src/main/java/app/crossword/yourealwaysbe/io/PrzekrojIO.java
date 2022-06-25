
package app.crossword.yourealwaysbe.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.PuzImage;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleBuilder;

/**
 * Read crosswords from https://przekroj.pl
 *
 * First interpret io stream as JSON, if that fails, try it as an HTML file and
 * try to extract JSON from it.
 *
 * Does not set title/author/date (not in JSON)
 */
public class PrzekrojIO implements PuzzleParser {
    private static final Logger LOG
        = Logger.getLogger(PrzekrojIO.class.getCanonicalName());

    private static final String ACROSS_LIST = "Poziomo";
    private static final String DOWN_LIST = "Pionowo";

    /**
     * An unfancy exception indicating error while parsing
     */
    public static class PrzFormatException extends Exception {
        public PrzFormatException(String msg) { super(msg); }
    }

    @Override
    public Puzzle parseInput(InputStream is) throws Exception {
        return readPuzzle(is);
    }

    public static Puzzle readPuzzle(InputStream is) throws IOException {
        try {
            ByteArrayInputStream copy = StreamUtils.copyInputStream(is);

            Puzzle puz = null;
            try {
                puz = readPuzzleFromJSON(
                    new JSONObject(new JSONTokener(copy))
                );
            } catch (JSONException e) {
                // fall through
            }

            if (puz == null) {
                copy.reset();
                Document doc = Jsoup.parse(copy, null, "");
                String cwJson = doc.select(".crossword")
                    .attr("data-json")
                    .replace("&quot;", "\"");
                String title = doc.select("article").attr("data-title");

                puz = readPuzzleFromJSON(
                    new JSONObject(new JSONTokener(cwJson))
                );
                if (title != null)
                    puz.setTitle(title);
            }

            return puz;
        } catch (IOException | JSONException | PrzFormatException e) {
            LOG.info("Could not read Przekroj crossword: " + e);
            return null;
        }
    }

    private static Puzzle readPuzzleFromJSON(JSONObject json)
            throws PrzFormatException {
        Box[][] boxes = readBoxes(json);
        PuzzleBuilder builder = new PuzzleBuilder(boxes);
        addClues(json, builder);
        addImages(json, builder);

        return builder.getPuzzle();
    }

    private static Box[][] readBoxes(JSONObject json)
            throws PrzFormatException {
        int width = json.getInt("x");
        int height = json.getInt("y");

        Box[][] boxes = new Box[height][width];

        JSONArray questions = json.getJSONArray("questions");
        for (int i = 0; i < questions.length(); i++) {
            JSONObject question = questions.getJSONObject(i);
            int row = question.getInt("y") - 1;
            int col = question.getInt("x") - 1;
            String number = question.getString("n");

            if (boxes[row][col] == null) {
                boxes[row][col] = new Box();
            } else {
                Box box = boxes[row][col];
                if (box.hasClueNumber()) {
                    if (!Objects.equals(number, box.getClueNumber())) {
                        throw new PrzFormatException(
                            "Clue number mismatch at position " + row + " " + col
                        );
                    }
                }
            }

            boxes[row][col].setClueNumber(number);

            String dir = question.getString("d");
            boolean across = "horizontal".equals(dir.toLowerCase());
            int drow = across ? 0 : 1;
            int dcol = across ? 1 : 0;

            String answer = question.getString("a").toUpperCase();

            for (int j = 0; j < answer.length(); j++) {
                if (boxes[row][col] == null)
                    boxes[row][col] = new Box();
                boxes[row][col].setSolution(String.valueOf(answer.charAt(j)));

                row += drow;
                col += dcol;
            }
        }

        return boxes;
    }

    private static void addClues(JSONObject json, PuzzleBuilder builder) {
        JSONArray questions = json.getJSONArray("questions");
        for (int i = 0; i < questions.length(); i++) {
            JSONObject question = questions.getJSONObject(i);
            String number = question.getString("n");
            String hint = question.getString("q");
            String dir = question.getString("d");
            boolean across = "horizontal".equals(dir.toLowerCase());

            if (across)
                builder.addAcrossClue(ACROSS_LIST, number, hint);
            else
                builder.addDownClue(DOWN_LIST, number, hint);
        }
    }

    private static void addImages(JSONObject json, PuzzleBuilder builder) {
        JSONArray images = json.optJSONArray("images");
        if (images == null)
            return;

        for (int i = 0; i < images.length(); i++) {
            JSONObject image = images.getJSONObject(i);
            String url = image.getString("src");
            int row = image.getInt("y") - 1;
            int col = image.getInt("x") - 1;
            int width = image.getInt("w");
            int height = image.getInt("h");

            builder.addImage(new PuzImage(url, row, col, width, height));
        }
    }
}
