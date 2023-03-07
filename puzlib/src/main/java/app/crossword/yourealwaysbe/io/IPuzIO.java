
package app.crossword.yourealwaysbe.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.ClueList;
import app.crossword.yourealwaysbe.puz.Note;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.PuzImage;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleBuilder;
import app.crossword.yourealwaysbe.puz.Zone;
import app.crossword.yourealwaysbe.util.PuzzleUtils;

import static app.crossword.yourealwaysbe.util.HtmlUtil.htmlString;
import static app.crossword.yourealwaysbe.util.HtmlUtil.unHtmlString;

/**
 * Read IPuz from a stream.
 *
 * Throws an exception if the puzzle is not in IPuz format, or is in a format
 * not supported.
 *
 * Many fields are ignored. A rough estimate has been made as to whether
 * a puzzle is playable if that field is ignored. If it may be, then the
 * puzzle will load without it. If it's probably a problem, it will
 * raise an IPuzFormatException.
 *
 * Currently checksums of solutions are not supported. The puzzle will
 * still be playable. The Puzzle class has a checksum field that is not
 * used, and corresponds to Across Lite checksums.
 *
 * Note: an error in the first version of this code means dates were
 * written in British (dd/mm/yyyy) format, not US (mm/dd/yyyy). The
 * IO_VERSION checks were added to still read old files correctly.
 *
 * http://www.ipuz.org/
 */
public class IPuzIO implements PuzzleParser {
    private static final Logger LOG
        = Logger.getLogger(IPuzIO.class.getCanonicalName());

    // version 1 was not tagged in file
    private static final int IO_VERSION = 3;

    private static final Charset WRITE_CHARSET = Charset.forName("UTF-8");

    private static final String FIELD_VERSION = "version";
    private static final String FIELD_KIND = "kind";

    private static final String FIELD_AUTHOR = "author";
    private static final String FIELD_COPYRIGHT = "copyright";
    private static final String FIELD_DATE = "date";
    private static final String FIELD_INTRO = "intro";
    private static final String FIELD_EXPLANATION = "explanation";
    private static final String FIELD_NOTES = "notes";
    private static final String FIELD_PUBLISHER = "publisher";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_URL = "url";

    private static final String FIELD_DIMENSIONS = "dimensions";
    private static final String FIELD_WIDTH = "width";
    private static final String FIELD_HEIGHT = "height";

    private static final String FIELD_PUZZLE = "puzzle";
    private static final String FIELD_SAVED = "saved";
    private static final String FIELD_SOLUTION = "solution";
    private static final String FIELD_SHOW_ENUMERATIONS = "showenumerations";
    private static final String FIELD_CLUES = "clues";

    private static final String FIELD_CELL = "cell";
    private static final String FIELD_VALUE = "value";
    private static final String FIELD_STYLE = "style";
    private static final String FIELD_SHAPE_BG = "shapebg";
    private static final String FIELD_COLOR = "color";
    private static final int HEX_CODE_LEN = 6;
    private static final String HEX_COLOR_FORMAT = "%0" + HEX_CODE_LEN + "X";
    private static final String FIELD_BARRED = "barred";
    private static final String FIELD_DASHED = "dashed";

    private static final String SHAPE_BG_CIRCLE = "circle";
    private static final char BARRED_TOP = 'T';
    private static final char BARRED_BOTTOM = 'B';
    private static final char BARRED_LEFT = 'L';
    private static final char BARRED_RIGHT = 'R';

    private static final char DASHED_TOP = 'T';
    private static final char DASHED_BOTTOM = 'B';
    private static final char DASHED_LEFT = 'L';
    private static final char DASHED_RIGHT = 'R';
    private static final String FIELD_MARK = "mark";
    private static final String FIELD_MARK_TOP_LEFT = "TL";
    private static final String FIELD_MARK_TOP = "T";
    private static final String FIELD_MARK_TOP_RIGHT = "TR";
    private static final String FIELD_MARK_LEFT = "L";
    private static final String FIELD_MARK_CENTER = "C";
    private static final String FIELD_MARK_RIGHT = "R";
    private static final String FIELD_MARK_BOTTOM_LEFT = "BL";
    private static final String FIELD_MARK_BOTTOM = "B";
    private static final String FIELD_MARK_BOTTOM_RIGHT = "BR";

    private static final String FIELD_CLUES_ACROSS = "Across";
    private static final String FIELD_CLUES_DOWN = "Down";

    private static final String FIELD_CLUE_NUMBER = "number";
    private static final String FIELD_CLUE_LABEL = "label";
    private static final String FIELD_CLUE_INDEX = "index";
    private static final String FIELD_CLUE_NUMBERS = "numbers";
    private static final String FIELD_CLUE_HINT = "clue";
    private static final String FIELD_CLUE_CONTINUED = "continued";
    private static final String FIELD_CLUE_REFERENCES = "references";
    private static final String FIELD_CLUE_DIRECTION = "direction";
    private static final String FIELD_CLUE_CELLS = "cells";

    private static final String FIELD_ENUMERATION = "enumeration";

    private static final String FIELD_BLOCK = "block";
    private static final String FIELD_EMPTY = "empty";

    private static final String DEFAULT_BLOCK = "#";
    private static final String DEFAULT_EMPTY_READ = "0";
    private static final int DEFAULT_EMPTY_WRITE = 0;

    private static final String WRITE_VERSION = "http://ipuz.org/v2";
    private static final String WRITE_KIND = "http://ipuz.org/crossword#1";
    private static final String WRITE_KIND_ACROSTIC
        = "http://ipuz.org/acrostic#1";
    private static final String ACROSTIC_PREFIX = "http://ipuz.org/acrostic";

    private static final String[] SUPPORTED_VERSIONS = {
        "http://ipuz.org/v1",
        WRITE_VERSION
    };
    private static final String[] SUPPORTED_KIND_PREFIXES = {
        WRITE_KIND,
        "http://ipuz.org/crossword",
        ACROSTIC_PREFIX
    };

    private static final String EXT_NAMESPACE = "app.crossword.yourealwaysbe";

    private static final String FIELD_EXT_SUPPORT_URL
        = getQualifiedExtensionName("supporturl");
    private static final String FIELD_EXT_SHARE_URL
        = getQualifiedExtensionName("shareurl");
    private static final String FIELD_EXT_PLAY_DATA
        = getQualifiedExtensionName("playdata");
    private static final String FIELD_EXT_IO_VERSION
        = getQualifiedExtensionName("ioversion");
    private static final String FIELD_EXT_IMAGES
        = getQualifiedExtensionName("images");
    private static final String FIELD_EXT_IMAGE_URL
        = getQualifiedExtensionName("url");
    private static final String FIELD_EXT_IMAGE_ROW
        = getQualifiedExtensionName("row");
    private static final String FIELD_EXT_IMAGE_COL
        = getQualifiedExtensionName("col");
    private static final String FIELD_EXT_IMAGE_WIDTH
        = getQualifiedExtensionName("width");
    private static final String FIELD_EXT_IMAGE_HEIGHT
        = getQualifiedExtensionName("height");
    private static final String FIELD_EXT_PINNED_CLUE_ID
        = getQualifiedExtensionName("pinnedClueID");

    private static final String FIELD_VOLATILE = "volatile";
    private static final String FIELD_IS_VOLATILE = "*";
    private static final String FIELD_IS_NOT_VOLATILE = "";

    private static final String[] VOLATILE_EXTENSIONS = {
        FIELD_EXT_PLAY_DATA
    };

    private static final String[] NON_VOLATILE_EXTENSIONS = {
        FIELD_EXT_SUPPORT_URL,
        FIELD_EXT_SHARE_URL,
        FIELD_EXT_IO_VERSION,
        FIELD_EXT_IMAGES
    };

    private static final String FIELD_BOX_EXTRAS = "boxextras";
    private static final String FIELD_BOX_CHEATED = "cheated";
    private static final String FIELD_BOX_RESPONDER = "responder";
    private static final String FIELD_COMPLETION_TIME = "completiontime";
    private static final String FIELD_PCNT_COMPLETE = "percentcomplete";
    private static final String FIELD_PCNT_FILLED = "percentfilled";
    private static final String FIELD_UPDATABLE = "updatable";
    private static final String FIELD_POSITION = "position";
    private static final String FIELD_POSITION_ROW = "row";
    private static final String FIELD_POSITION_COL = "col";
    private static final String FIELD_POSITION_ACROSS = "across";
    private static final String FIELD_POSITION_CLUEID = "clueid";
    private static final String FIELD_CLUE_HISTORY = "cluehistory";
    private static final String FIELD_CLUE_ACROSS = "across";
    private static final String FIELD_CLUE_LISTNAME = "listname";
    private static final String FIELD_CLUE_NOTES = "cluenotes";
    private static final String FIELD_PLAYER_NOTE = "playernote";
    private static final String FIELD_CLUE_NOTE_CLUE = "clue";
    private static final String FIELD_NOTE_SCRATCH = "scratch";
    private static final String FIELD_NOTE_TEXT = "text";
    private static final String FIELD_NOTE_ANAGRAM_SRC = "anagramsource";
    private static final String FIELD_NOTE_ANAGRAM_SOL
        = "anagramsolution";
    private static final String FIELD_FLAGGED_CLUES = "flaggedclues";

    private static final DateTimeFormatter DATE_FORMATTER_V1
        = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.US);
    private static final DateTimeFormatter DATE_FORMATTER
        = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US);
    // for puzzles that forget to double digits
    private static final DateTimeFormatter DATE_FORMATTER_SINGLES
        = DateTimeFormatter.ofPattern("M/d/yyyy");

    private static final String FIELD_CLUES_CLUES = "Clues";
    private static final String FIELD_CLUES_ZONES = "Zones";
    private static final Set<String> FIELD_CLUES_DIRECTIONS
        = new HashSet<>(Arrays.asList(new String[] {
            "Across",
            "Down",
            "Diagonal",
            "Diagonal Up",
            "Diagonal Down Left",
            "Diagonal Up Left",
            FIELD_CLUES_ZONES,
            FIELD_CLUES_CLUES
        }));

    private static final String NULL_CLUE = "-";

    // before we started supporting any list name, we used across/down
    // and a boolean to tell the difference.
    private static final String OLD_ACROSS_LIST_NAME = "Across";
    private static final String OLD_DOWN_LIST_NAME = "Down";

    private static final String ACROSTIC_BOARD_CLUE_LIST = "Quote";
    private static final String ACROSTIC_BOARD_CLUE_HINT = "Quote";

    /**
     * An unfancy exception indicating error while parsing
     */
    public static class IPuzFormatException extends Exception {
        public IPuzFormatException(String msg) { super(msg); }
    }

    @Override
    public Puzzle parseInput(InputStream is) throws Exception {
        return readPuzzle(is);
    }

    public static Puzzle readPuzzle(InputStream is) throws IOException {
        try {
            JSONObject json = new JSONObject(new JSONTokener(is));

            checkIPuzVersion(json);
            Puzzle.Kind kind = getPuzKind(json);

            PuzzleBuilder builder = new PuzzleBuilder(readBoxes(json));

            if (kind != null)
                builder.setKind(kind);

            readMetaData(json, builder);
            readClues(json, builder);
            readExtensions(json, builder);

            if (Puzzle.Kind.ACROSTIC.equals(kind))
                ensureBoardClue(builder);

            return builder.getPuzzle();
        } catch (IPuzFormatException | JSONException e) {
            LOG.severe("Could not read IPuz file: " + e);
            e.printStackTrace();
            return null;
        }
    }

    private static void checkIPuzVersion(JSONObject puzJson)
            throws IPuzFormatException {
        String version = puzJson.getString(FIELD_VERSION);
        for (String supportedVersion : SUPPORTED_VERSIONS) {
            if (supportedVersion.equalsIgnoreCase(version))
                return;
        }
        throw new IPuzFormatException(
            "Unsupported IPuz version: " + version
        );
    }

    /**
     * Checks recognized ipuz kind
     *
     * @return Puzzle.Kind the ipuz kind matches
     */
    private static Puzzle.Kind getPuzKind(JSONObject puzJson)
            throws IPuzFormatException {
        Puzzle.Kind puzKind = null;

        // Either acrostic or crossword. If we find acrostic, we're
        // done. Otherwise, assume crossword unless acrostic comes up
        // later

        JSONArray kinds = puzJson.getJSONArray(FIELD_KIND);
        for (int i = 0; i < kinds.length(); i++) {
            String kind = kinds.getString(i).toLowerCase();

            if (kind.startsWith(ACROSTIC_PREFIX))
                return Puzzle.Kind.ACROSTIC;

            for (String supportedKindPrefix : SUPPORTED_KIND_PREFIXES) {
                if (kind.startsWith(supportedKindPrefix)) {
                    puzKind = Puzzle.Kind.CROSSWORD;
                }
            }
        }

        if (puzKind == null)
            throw new IPuzFormatException("No supported IPuz kind: " + kinds);

        return puzKind;
    }

    /**
     * Read puzzle info from puzJson into puz
     *
     * Meta-data stuff, like title, copyright, etc.
     */
    private static void readMetaData(
        JSONObject puzJson, PuzzleBuilder builder
    ) throws IPuzFormatException {
        builder.setTitle(optStringNull(puzJson, FIELD_TITLE))
            .setAuthor(optStringNull(puzJson, FIELD_AUTHOR))
            .setCopyright(optStringNull(puzJson, FIELD_COPYRIGHT))
            .setIntroMessage(optStringNull(puzJson, FIELD_INTRO))
            .setNotes(optStringNull(puzJson, FIELD_NOTES))
            .setCompletionMessage(optStringNull(puzJson, FIELD_EXPLANATION))
            .setSourceUrl(optStringNull(puzJson, FIELD_URL))
            .setSource(optStringNull(puzJson, FIELD_PUBLISHER))
            .setDate(parseDate(puzJson));
    }

    /**
     * Parse date field (depends on IO version!)
     *
     * I put day/month the wrong way around in initial version :/
     *
     * @return null if no date
     */
    private static LocalDate parseDate(JSONObject puzJson)
            throws IPuzFormatException {
        String date = optStringNull(puzJson, FIELD_DATE);
        if (date == null)
            return null;

        try {
            if (getIOVersion(puzJson) == 1)
                return LocalDate.parse(date, DATE_FORMATTER_V1);
            else
                return LocalDate.parse(date, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(date, DATE_FORMATTER_SINGLES);
            } catch (DateTimeParseException e2) {
                throw new IPuzFormatException("Unrecognised date " + date);
            }
        }
    }

    /**
     * The Forkyz IO version
     *
     * 0 if no Forkyz data specified, otherwise 1 or above
     */
    private static int getIOVersion(JSONObject puzJson) {
        if (puzJson.has(FIELD_EXT_IO_VERSION))
            return puzJson.getInt(FIELD_EXT_IO_VERSION);
        else
            return puzJson.has(FIELD_EXT_PLAY_DATA) ? 1 : 0;
    }

    /**
     * Get optional field from JSON
     *
     * Strips any HTML elements from it.
     */
    private static String unHtmlOptString(JSONObject json, String field) {
        String value = optStringNull(json, field);

        if (value == null)
            return null;

        return unHtmlString(value);
    }

    /**
     * Read fully populated Box objects from JSON
     *
     * Will always return some boxes, or throw a JSON exception
     */
    private static Box[][] readBoxes(JSONObject puzJson)
            throws IPuzFormatException {
        JSONObject dimensions = puzJson.getJSONObject(FIELD_DIMENSIONS);

        int width = dimensions.getInt(FIELD_WIDTH);
        int height = dimensions.getInt(FIELD_HEIGHT);

        Box[][] boxes = new Box[height][width];

        readPuzzleCells(puzJson, boxes);
        readSaved(puzJson, boxes);
        readSolution(puzJson, boxes);

        return boxes;
    }

    /**
     * Populate boxes array following cells in JSON
     *
     * I.e. no box, block, empty, or clue number, possibly with styles
     * etc.
     */
    private static void readPuzzleCells(JSONObject puzJson, Box[][] boxes)
            throws IPuzFormatException {
        JSONArray cells = puzJson.getJSONArray(FIELD_PUZZLE);

        if (cells.length() < boxes.length) {
            throw new IPuzFormatException(
                "Number of cell rows doesn't match boxes dimensions"
            );
        }

        String block = getBlockString(puzJson);
        String empty = getEmptyCellString(puzJson);

        for (int row = 0; row < boxes.length; row++) {
            JSONArray rowCells = cells.getJSONArray(row);

            if (rowCells.length() < boxes[row].length) {
                throw new IPuzFormatException(
                    "Number of cell columns doesn't match boxes dimension"
                );
            }

            for (int col = 0; col < boxes[row].length; col++) {
                boxes[row][col]
                    = getBoxFromObj(rowCells.get(col), block, empty);
            }
        }
    }

    private static String getBlockString(JSONObject puzJson) {
        return puzJson.optString(FIELD_BLOCK, DEFAULT_BLOCK);
    }

    private static String getEmptyCellString(JSONObject puzJson) {
        return puzJson.optString(FIELD_EMPTY, DEFAULT_EMPTY_READ);
    }

    /**
     * Turn the (JSON) object into a box
     *
     * If null or block value, then blank. If empty then empty box, else
     * box with clue number and maybe decoration.
     *
     * @param cell the object in the JSON for the cell (could be a
     * number, string, or JSON object
     * @param block the string for a block
     * @param empty the string for an empty cell
     */
    private static Box getBoxFromObj(Object cell, String block, String empty)
            throws IPuzFormatException {
        if (cell == null || JSONObject.NULL.equals(cell)) {
            return null;
        } else if (cell instanceof JSONObject) {
            JSONObject json = (JSONObject) cell;

            // unsure if ipuz allows cell field to be missing, but
            // reasonable to assume empty cell if so
            Object cellObj = json.opt(FIELD_CELL);
            if (cellObj == null)
                cellObj = empty;

            Box box = getBoxFromObj(cellObj, block, empty);

            String initVal = optStringNull(json, FIELD_VALUE);
            if (initVal != null) {
                if (initVal.length() != 1) {
                    throw new IPuzFormatException(
                        "Cannot represent init values of more "
                        + "than one character: '"
                        + initVal
                        + "'"
                    );
                }
                box.setResponse(initVal.charAt(0));
            }

            JSONObject style = json.optJSONObject(FIELD_STYLE);

            if (style != null) {
                if (SHAPE_BG_CIRCLE.equals(style.optString(FIELD_SHAPE_BG))) {
                    box.setCircled(true);
                }
                String color = style.optString(FIELD_COLOR);
                // not supporting numbered "unique" colors for now
                if (color != null && color.length() == HEX_CODE_LEN) {
                    box.setColor(hexToColor(color));
                }

                getBarredFromStyleObj(style, box);
                getDashedFromStyleObj(style, box);
                getMarksFromStyleObj(style, box);
            }

            return box;
        } else if (cell.toString().equals(block.toString())) {
            return null;
        } else if (cell.toString().equals(empty.toString())) {
            return new Box();
        } else {
            try {
                Box box = new Box();
                box.setClueNumber(cell.toString());
                return box;
            } catch (NumberFormatException e) {
                throw new IPuzFormatException(
                    "Unrecognised cell in puzzle: " + cell
                );
            }
        }
    }

    private static void getBarredFromStyleObj(JSONObject style, Box box) {
        String barred = optStringNull(style, FIELD_BARRED);
        if (barred != null) {
            barred = barred.toUpperCase();
            for (int i = 0; i < barred.length(); i++) {
                char c = barred.charAt(i);
                switch(c) {
                case BARRED_TOP:
                    box.setBarredTop(true);
                    break;
                case BARRED_BOTTOM:
                    box.setBarredBottom(true);
                    break;
                case BARRED_LEFT:
                    box.setBarredLeft(true);
                    break;
                case BARRED_RIGHT:
                    box.setBarredRight(true);
                    break;
                default:
                    // do nothing
                }
            }
        }
    }

    private static void getDashedFromStyleObj(JSONObject style, Box box) {
        String dashed = optStringNull(style, FIELD_DASHED);
        if (dashed != null) {
            dashed = dashed.toUpperCase();
            for (int i = 0; i < dashed.length(); i++) {
                char c = dashed.charAt(i);
                switch(c) {
                    case DASHED_TOP:
                        box.setDashedTop(true);
                        break;
                    case DASHED_BOTTOM:
                        box.setDashedBottom(true);
                        break;
                    case DASHED_LEFT:
                        box.setDashedLeft(true);
                        break;
                    case DASHED_RIGHT:
                        box.setDashedRight(true);
                        break;
                    default:
                        // do nothing
                }
            }
        }
    }

    private static void getMarksFromStyleObj(JSONObject style, Box box) {
        JSONObject markObj = style.optJSONObject(FIELD_MARK);
        if (markObj == null)
            return;

        String[][] marks = new String[3][3];
        marks[0][0] = optStringNull(markObj, FIELD_MARK_TOP_LEFT);
        marks[0][1] = optStringNull(markObj, FIELD_MARK_TOP);
        marks[0][2] = optStringNull(markObj, FIELD_MARK_TOP_RIGHT);
        marks[1][0] = optStringNull(markObj, FIELD_MARK_LEFT);
        marks[1][1] = optStringNull(markObj, FIELD_MARK_CENTER);
        marks[1][2] = optStringNull(markObj, FIELD_MARK_RIGHT);
        marks[2][0] = optStringNull(markObj, FIELD_MARK_BOTTOM_LEFT);
        marks[2][1] = optStringNull(markObj, FIELD_MARK_BOTTOM);
        marks[2][2] = optStringNull(markObj, FIELD_MARK_BOTTOM_RIGHT);

        box.setMarks(marks);
    }

    /**
     * Populate boxes array with response data from JSON
     */
    private static void readSaved(JSONObject puzJson, Box[][] boxes)
            throws IPuzFormatException {
        String block = getBlockString(puzJson);
        String empty = getEmptyCellString(puzJson);
        JSONArray saved = puzJson.optJSONArray(FIELD_SAVED);
        if (saved != null && saved.length() > 0)
            readValues(saved, boxes, false, block, empty);
    }

    /**
     * Populate boxes array with puzzle solution data from JSON
     */
    private static void readSolution(JSONObject puzJson, Box[][] boxes)
            throws IPuzFormatException {
        String block = getBlockString(puzJson);
        String empty = getEmptyCellString(puzJson);
        JSONArray solution = puzJson.optJSONArray(FIELD_SOLUTION);
        if (solution != null && solution.length() > 0)
            readValues(solution, boxes, true, block, empty);
    }

    /**
     * Reads a value array and loads into boxes saved/solution
     *
     * @param cells the array of arrays of CrosswordValues
     * @param boxes the boxes to read data into
     * @param isSolution whether puzzles solution is being read, else
     * saved user responses will be read
     */
    private static void readValues(
        JSONArray cells, Box[][] boxes, boolean isSolution,
        String block, String empty
    ) throws IPuzFormatException {
        int height = Math.min(cells.length(), boxes.length);

        for (int row = 0; row < height; row++) {
            JSONArray rowCells = cells.getJSONArray(row);

            int width = Math.min(rowCells.length(), boxes[row].length);

            for (int col = 0; col < width; col++) {
                String value = getCrosswordValueFromObj(
                    rowCells.get(col), block, empty
                );

                if (value !=  null) {
                    if (isSolution)
                        boxes[row][col].setSolution(value);
                    else
                        boxes[row][col].setResponse(value);
                }
            }
        }
    }

    /**
     * Fill in the saved data for the box from the object in the JSON
     *
     * @param cell the object in the JSON saved array
     * @param block the representation for a block
     * @param empty the representation of an empty cell
     * @return value of response if given, Box.BLANK if empty, null if
     * block or omitted
     */
    private static String getCrosswordValueFromObj(
        Object cell, String block, String empty
    ) throws IPuzFormatException {
        if (cell == null || JSONObject.NULL.equals(cell)) {
            return null;
        } else if (cell instanceof JSONArray) {
            JSONArray values = (JSONArray) cell;
            if (values.length() != 1) {
                throw new IPuzFormatException(
                    "Multiple cell values not supported: " + values
                );
            }
            return getCrosswordValueFromObj(values.get(0), block, empty);
        } else if (cell instanceof JSONObject) {
            JSONObject json = (JSONObject) cell;
            String value = optStringNull(json, FIELD_VALUE);
            if (value == null)
                return null;
            else if (value.isEmpty())
                return Box.BLANK;
            else
                return value;
        } else {
            String value = cell.toString();
            if (block.equals(value))
                return null;
            else if (empty.equals(value) || value.isEmpty())
                return Box.BLANK;
            else
                return value;
        }
    }

    /**
     * Read clues into puz
     */
    private static void readClues(JSONObject puzJson, PuzzleBuilder builder)
            throws IPuzFormatException {
        // default to true as it is safest
        boolean showEnumerations = puzJson.optBoolean(
            FIELD_SHOW_ENUMERATIONS, true
        );

        int coordBase = getCoordBase(puzJson);

        JSONObject clues = puzJson.getJSONObject(FIELD_CLUES);

        JSONArray names = clues.names();
        for (int i = 0; i < names.length(); i++) {
            String listName = names.getString(i);
            addClues(
                clues.getJSONArray(listName),
                names.getString(i),
                showEnumerations,
                coordBase,
                builder
            );
        }
    }

    /**
     * Determine if cell indexes 0- or 1-based
     *
     * 0-based matches Puzzaz implementation and first crossword in
     * ipuz. 1-based matches the examples in the ipuz spec (but crashes
     * Puzzaz).
     *
     * Use 0-based by default. 1-based if on Forkyz ipuz io 2 or below,
     * or if some coordinate is too high using 0-based numbering.
     *
     * @param puzJson json object of puzzle
     */
    private static int getCoordBase(JSONObject puzJson) {
        int ioVersion = getIOVersion(puzJson);
        if (ioVersion > 0 && ioVersion <= 2) {
            return 1;
        } else if (ioVersion >= 3) {
            return 0;
        } else { // search cells
            JSONObject dimensions = puzJson.getJSONObject(FIELD_DIMENSIONS);
            int width = dimensions.getInt(FIELD_WIDTH);
            int height = dimensions.getInt(FIELD_HEIGHT);

            JSONObject clues = puzJson.getJSONObject(FIELD_CLUES);
            JSONArray names = clues.names();
            for (int i = 0; i < names.length(); i++) {
                String listName = names.getString(i);
                JSONArray clueList = clues.getJSONArray(listName);
                for (int j = 0; j < clueList.length(); j++) {
                    Object clueObj = clueList.get(j);
                    if (clueObj instanceof JSONObject) {
                        JSONObject clueJson = (JSONObject) clueObj;
                        JSONArray cells
                            = clueJson.optJSONArray(FIELD_CLUE_CELLS);
                        if (cells != null) {
                            for (int k = 0; k < cells.length(); k++) {
                                JSONArray cell = cells.getJSONArray(k);
                                int row = cell.getInt(1);
                                if (row >= height)
                                    return 1;
                                int col = cell.getInt(0);
                                if (col >= width)
                                    return 1;
                            }
                        }
                    }
                }
            }
            return 0;
        }
    }

    /**
     * Transfer clues from json to puzzle
     *
     * Adds enumeration text to hint if showEnumerations is true
     *
     * @param coordBase 0 if 0-based, 1 if 1-based
     */
    private static void addClues(
        JSONArray jsonClues, String listName,
        boolean showEnumerations, int coordBase,
        PuzzleBuilder builder
    ) throws IPuzFormatException {
        String[] splitName = listName.split(":");
        String dirName = splitName[0];
        String displayName = splitName.length > 1 ? splitName[1] : dirName;

        try {
            for (int i = 0; i < jsonClues.length(); i++) {
                Object clueObj = jsonClues.get(i);
                // all clues not across until proven otherwise by list name
                IPuzClue ipc = getClue(clueObj, showEnumerations, coordBase);
                // TODO: support zoning of more than just across/down
                if (ipc != null) {
                    if (FIELD_CLUES_ACROSS.equals(dirName)) {
                        builder.addAcrossClue(
                            displayName,
                            ipc.getClueNumber(),
                            ipc.getLabel(),
                            ipc.getHint()
                        );
                    } else if (FIELD_CLUES_DOWN.equals(dirName)) {
                        builder.addDownClue(
                            displayName,
                            ipc.getClueNumber(),
                            ipc.getLabel(),
                            ipc.getHint()
                        );
                    } else {
                        builder.addClue(new Clue(
                            displayName,
                            i,
                            ipc.getClueNumber(),
                            ipc.getLabel(),
                            ipc.getHint(),
                            ipc.getZone()
                        ));
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            throw new IPuzFormatException(e.getMessage());
        }
    }

    /**
     * Convert a JSON object clue into a Clue
     *
     * Adds enumeration to hint if showEnumerations is true
     *
     * @param coordBase 0 if 0-based indexing, 1 if 1-based
     */
    private static IPuzClue getClue(
        Object clueObj, boolean showEnumerations, int coordBase
    ) throws IPuzFormatException {
        if (clueObj instanceof String) {
            return new IPuzClue((String) clueObj);
        } else if (clueObj instanceof JSONArray) {
            JSONArray clueArray = (JSONArray) clueObj;
            if (clueArray.length() != 2) {
                throw new IPuzFormatException(
                    "Unexpected clue array length: " + clueArray.length()
                );
            }
            Object clueNumObj = clueArray.get(0);
            String hint = clueArray.getString(1);

            return buildClue(clueNumObj, null, hint, null, null);
        } else if (clueObj instanceof JSONObject) {
            JSONObject clueJson = (JSONObject) clueObj;

            // get clue number, sometimes nested
            Object clueNumObj = clueJson.opt(FIELD_CLUE_NUMBER);
            if (getClueNumber(clueNumObj) == null)
                clueNumObj = clueJson.opt(FIELD_CLUE_NUMBERS);

            String label = optStringNull(clueJson, FIELD_CLUE_LABEL);

            // build hint, bake in additional info
            StringBuilder hint = new StringBuilder();

            hint.append(clueJson.getString(FIELD_CLUE_HINT));

            JSONArray conts = clueJson.optJSONArray(FIELD_CLUE_CONTINUED);
            if (conts != null && conts.length() > 0) {
                addCrossRefList(hint, conts, "cont.");
            }

            JSONArray refs = clueJson.optJSONArray(FIELD_CLUE_REFERENCES);
            if (refs != null && refs.length() > 0) {
                addCrossRefList(hint, refs, "ref.");
            }

            String enumeration = showEnumerations
                ? optStringNull(clueJson, FIELD_ENUMERATION)
                : null;

            Zone zone = getClueZone(clueJson, coordBase);

            return buildClue(
                clueNumObj, label, hint.toString(), enumeration, zone
            );
        } else {
            throw new IPuzFormatException(
                "Unsupported clue format " + clueObj.getClass() + ": " + clueObj
            );
        }
    }

    /**
     * Read zone info from clue json
     *
     * @param coordBase 0 if 0-based, 1 if 1-based
     * @return null if no info
     */
    private static Zone getClueZone(JSONObject clueJson, int coordBase) {
        if (clueJson == null)
            return null;

        JSONArray cells = clueJson.optJSONArray(FIELD_CLUE_CELLS);
        if (cells == null)
            return null;

        Zone zone = new Zone();

        for (int i = 0; i < cells.length(); i++) {
            JSONArray cell = cells.getJSONArray(i);
            zone.addPosition(new Position(
                cell.getInt(1) - coordBase, cell.getInt(0) - coordBase
            ));
        }

        return zone;
    }

    /**
     * Add cross references to hint
     *
     * @param hint hint being built
     * @param refs list of references
     * @param description text to identify what reference list is (e.g.
     * "cont." or "ref.")
     */
    private static void addCrossRefList(
        StringBuilder hint, JSONArray refs, String description
    ) {
        hint.append(" (");
        hint.append(description);
        hint.append(" ");
        for (int i = 0; i < refs.length(); i++) {
            if (i > 0)
                hint.append("/");
            JSONObject ref = refs.getJSONObject(i);
            hint.append(getComplexClueNumString(
                ref.get(FIELD_CLUE_NUMBER))
            );
            hint.append(" ");;
            hint.append(ref.getString(FIELD_CLUE_DIRECTION));
        }
        hint.append(")");
    }

    /**
     * Build a Clue object from info
     *
     * In particular handles the different clue number formats
     *
     * @param clueNumObj the ClueNum object in JSON
     * @param label a label to display instead of clue number (null if
     * not used)
     * @param hint the clue hint
     * @param enumeration null or empty if no enumeration to be shown in clue
     * @param zone the zone of the clue or null
     */
    private static IPuzClue buildClue(
        Object clueNumObj, String label, String hint, String enumeration, Zone zone
    ) throws IPuzFormatException {
        String number = getClueNumber(clueNumObj);

        if (isComplexClueNumber(clueNumObj)) {
            String numString = getComplexClueNumString(clueNumObj);
            if (numString != null && numString.length() > 0)
                hint += " (clues " + numString + ")";
        }

        if (enumeration != null && enumeration.length() > 0) {
            hint += " (" + enumeration + ")";
        }

        return new IPuzClue(number, label, hint, zone);
    }

    /**
     * Check if clueNumObj is something other than a simple number
     *
     * Will accept a JSONArray of ClueNums not just a single ClueNum
     */
    private static boolean isComplexClueNumber(Object clueNumObj)
            throws IPuzFormatException {
        if (clueNumObj == null)
            return false;

        if (clueNumObj instanceof Number)
            return false;

        if (clueNumObj instanceof String)
            return false;

        if (clueNumObj instanceof JSONArray) {
            return true;
        }

        throw new IPuzFormatException(
            "Unrecognised clue number format: " + clueNumObj.getClass()
        );
    }

    /**
     * Return basic number from clueNumObj
     *
     * If a number, return its int value. If a string, return first
     * integer in the string. If a JSONArray return the first item in
     * the array from which a number can be extracted.
     *
     * Else return null (including if null passed).
     */
    private static String getClueNumber(Object clueNumObj) {
        if (clueNumObj instanceof Number)
            return String.valueOf(((Number) clueNumObj).intValue());

        if (clueNumObj instanceof String)
            return (String) clueNumObj;

        if (clueNumObj instanceof JSONArray) {
            JSONArray clueNums = (JSONArray) clueNumObj;

            for (Object subNumObj : clueNums) {
                String subNum = getClueNumber(subNumObj);
                if (subNum != null)
                    return subNum;
            }
        }

        return null;
    }

    /**
     * Extract string from a complex clue number representation
     *
     * E.g. "1/2" from ["1", "2"]
     *
     * Returns null if nothing useful could be extracted.
     */
    private static String getComplexClueNumString(Object clueNumObj) {
        if (clueNumObj instanceof Number)
            return String.valueOf(((Number) clueNumObj).intValue());

        if (clueNumObj instanceof String)
            return (String) clueNumObj;

        if (clueNumObj instanceof JSONArray) {
            JSONArray objs = (JSONArray) clueNumObj;

            StringBuilder builder = new StringBuilder();

            for (int i = 0; i < objs.length(); i++) {
                if (i > 0)
                    builder.append("/");
                builder.append(getComplexClueNumString(objs.get(i)));
            }

            return builder.toString();
        }

        return null;
    }

    /**
     * Read non-standard IPuz fields into puzzle
     */
    private static void readExtensions(
        JSONObject puzJson, PuzzleBuilder builder
    ) throws IPuzFormatException {
        String supportUrl = optStringNull(puzJson, FIELD_EXT_SUPPORT_URL);
        if (supportUrl != null)
            builder.setSupportUrl(supportUrl);

        String shareUrl = optStringNull(puzJson, FIELD_EXT_SHARE_URL);
        if (shareUrl != null)
            builder.setShareUrl(shareUrl);

        readImages(puzJson, builder);

        ClueID pinnedClueID = decodeClueID(
            puzJson.optJSONObject(FIELD_EXT_PINNED_CLUE_ID), builder
        );
        if (pinnedClueID != null)
            builder.setPinnedClueID(pinnedClueID);

        JSONObject playData = puzJson.optJSONObject(FIELD_EXT_PLAY_DATA);
        if (playData != null && !JSONObject.NULL.equals(playData))
            readPlayData(playData, builder);
    }

    private static void readImages(JSONObject puzJson, PuzzleBuilder builder) {
        JSONArray images = puzJson.optJSONArray(FIELD_EXT_IMAGES);
        if (images == null)
            return;

        for (int i = 0; i < images.length(); i++) {
            JSONObject image = images.getJSONObject(i);
            String url = image.getString(FIELD_EXT_IMAGE_URL);
            int row = image.getInt(FIELD_EXT_IMAGE_ROW);
            int col = image.getInt(FIELD_EXT_IMAGE_COL);
            int width = image.getInt(FIELD_EXT_IMAGE_WIDTH);
            int height = image.getInt(FIELD_EXT_IMAGE_HEIGHT);

            builder.addImage(new PuzImage(url, row, col, width, height));
        }
    }

    /**
     * Read play data extension from playData object to puz
     *
     * @param playData the playData field of the puz json
     */
    private static void readPlayData(
        JSONObject playData, PuzzleBuilder builder
    ) throws IPuzFormatException {
        readBoxExtras(playData, builder);
        readPosition(playData, builder);
        readClueHistory(playData, builder);
        readClueNotes(playData, builder);
        readPlayerNote(playData, builder);
        readFlaggedClues(playData, builder);

        if (playData.has(FIELD_COMPLETION_TIME))
            builder.setTime(playData.getLong(FIELD_COMPLETION_TIME));

        if (playData.has(FIELD_UPDATABLE))
            builder.setUpdatable(playData.getBoolean(FIELD_UPDATABLE));
    }

    /**
     * Read non-standard info about boxes (e.g. is cheated)
     *
     * Assumes boxes have been set on puz
     */
    private static void readBoxExtras(
        JSONObject playData, PuzzleBuilder builder
    ) {
        if (!playData.has(FIELD_BOX_EXTRAS))
            return;

        JSONArray cellsJson = playData.getJSONArray(FIELD_BOX_EXTRAS);

        int numRows = Math.min(cellsJson.length(), builder.getHeight());

        for (int row = 0; row < numRows; row++) {
            JSONArray rowJson = cellsJson.getJSONArray(row);

            int numCols = Math.min(rowJson.length(), builder.getWidth());

            for (int col = 0; col < numCols; col++) {
                Box box = builder.getBox(row, col);
                if (box != null) {
                    JSONObject boxJson = rowJson.getJSONObject(col);

                    if (boxJson.has(FIELD_BOX_CHEATED)) {
                        box.setCheated(boxJson.getBoolean(FIELD_BOX_CHEATED));
                    }
                    if (boxJson.has(FIELD_BOX_RESPONDER)) {
                        box.setResponder(
                            boxJson.getString(FIELD_BOX_RESPONDER)
                        );
                    }
                }
            }
        }
    }

    /**
     * Read the position from playData
     *
     * Assumes builder already provides a puzzle with boxes and clues set up.
     */
    private static void readPosition(
        JSONObject playData, PuzzleBuilder builder
    ) throws IPuzFormatException {
        if (!playData.has(FIELD_POSITION))
            return;

        JSONObject positionJson = playData.getJSONObject(FIELD_POSITION);

        if (
            positionJson.has(FIELD_POSITION_ROW)
            && positionJson.has(FIELD_POSITION_COL)
        ) {
            int row = positionJson.optInt(FIELD_POSITION_ROW, -1);
            int col = positionJson.optInt(FIELD_POSITION_COL, -1);
            Position pos = new Position(row, col);

            if (
                0 <= row && row <= builder.getHeight()
                && 0 <= col && col <= builder.getWidth()
            ) {
                builder.setPosition(pos);
            }

            // old style: has an across boolean for across/down only
            // new style: has current clue id
            if (positionJson.has(FIELD_POSITION_ACROSS)) {
                // old style
                String list
                    = positionJson.getBoolean(FIELD_POSITION_ACROSS)
                    ? OLD_ACROSS_LIST_NAME
                    : OLD_DOWN_LIST_NAME;
                Box box = builder.getBox(pos);
                ClueID cid
                    = (box == null) ? null : box.getIsPartOfClue(list);
                builder.setCurrentClueID(cid);
            } else if (positionJson.has(FIELD_POSITION_CLUEID)) {
                JSONObject cidJson
                    = positionJson.getJSONObject(FIELD_POSITION_CLUEID);
                builder.setCurrentClueID(decodeClueID(cidJson, builder));
            }
        }
    }

    /**
     * Reads clue history from playData
     *
     * Assumes builder returns a puzzle with clue lists set up.
     */
    private static void readClueHistory(
        JSONObject playData, PuzzleBuilder builder
    ) throws IPuzFormatException {
        if (!playData.has(FIELD_CLUE_HISTORY))
            return;

        JSONArray historyJson = playData.getJSONArray(FIELD_CLUE_HISTORY);

        LinkedList<ClueID> history = new LinkedList<>();

        for (int i = 0; i < historyJson.length(); i++) {
            JSONObject itemJson = historyJson.getJSONObject(i);
            ClueID cid = decodeClueID(itemJson, builder);
            if (cid != null)
                history.add(cid);
        }

        builder.setHistory(history);
    }

    /**
     * Read notes from playData
     *
     * Assumes builder returns a puzzle with clue lists set up
     */
    private static void readClueNotes(
        JSONObject playData, PuzzleBuilder builder
    ) throws IPuzFormatException {
        if (!playData.has(FIELD_CLUE_NOTES))
            return;

        JSONArray notesJson = playData.getJSONArray(FIELD_CLUE_NOTES);

        for (int i = 0; i < notesJson.length(); i++) {
            JSONObject noteJson = notesJson.getJSONObject(i);
            JSONObject cndJson = noteJson.optJSONObject(FIELD_CLUE_NOTE_CLUE);
            ClueID cid = decodeClueID(cndJson, builder);

            if (cid != null) {
                String scratch
                    = noteJson.optString(FIELD_NOTE_SCRATCH, null);
                String text
                    = unHtmlOptString(noteJson, FIELD_NOTE_TEXT);
                String anagramSrc
                    = noteJson.optString(FIELD_NOTE_ANAGRAM_SRC, null);
                String anagramSol
                    = noteJson.optString(FIELD_NOTE_ANAGRAM_SOL, null);

                if (scratch != null
                        || text != null
                        || anagramSrc != null
                        || anagramSol != null) {
                    builder.setNote(
                        cid,
                        new Note(scratch, text, anagramSrc, anagramSol)
                    );
                }
            }
        }
    }

    /**
     * Read player note
     */
    private static void readPlayerNote(
        JSONObject playData, PuzzleBuilder builder
    ) {
        if (!playData.has(FIELD_PLAYER_NOTE))
            return;

        JSONObject noteJson = playData.getJSONObject(FIELD_PLAYER_NOTE);

        String scratch
            = noteJson.optString(FIELD_NOTE_SCRATCH, null);
        String text
            = unHtmlOptString(noteJson, FIELD_NOTE_TEXT);
        String anagramSrc
            = noteJson.optString(FIELD_NOTE_ANAGRAM_SRC, null);
        String anagramSol
            = noteJson.optString(FIELD_NOTE_ANAGRAM_SOL, null);

        if (scratch != null
                || text != null
                || anagramSrc != null
                || anagramSol != null) {
            builder.setPlayerNote(
                new Note(scratch, text, anagramSrc, anagramSol)
            );
        }
    }

    /**
     * Read which clues are flagged
     *
     * Assumes builder returns a puzzle whose clue lists have been set
     * up
     */
    private static void readFlaggedClues(
        JSONObject playData, PuzzleBuilder builder
    ) throws IPuzFormatException {
        if (!playData.has(FIELD_FLAGGED_CLUES))
            return;

        JSONArray flagsJson = playData.getJSONArray(FIELD_FLAGGED_CLUES);

        for (int i = 0; i < flagsJson.length(); i++) {
            JSONObject cndJson = flagsJson.getJSONObject(i);
            ClueID cnd = decodeClueID(cndJson, builder);
            if (cnd != null)
                builder.flagClue(cnd, true);
        }
    }

    /**
     * Read a JSON representation of ClueID to ClueID
     *
     * Assumes (for legacy clue ids) that builder already returns a
     * puzzle with clue lists set up.
     *
     * ClueIDs version 1: list name and across/down boolean
     * ClueIDs version 2: list name and clue number
     * ClueIDs version 3: list name and index in clue list
     *
     * Version 2 was created to handle clue lists apart from
     * across/down. Extended to Version 3 to better handle unnumbered clues.
     *
     * @return null if not right
     */
    private static ClueID decodeClueID(JSONObject cid, PuzzleBuilder builder)
            throws IPuzFormatException {
        if (cid == null)
            return null;
        if (JSONObject.NULL.equals(cid))
            return null;

        // Version 3
        if (cid.has(FIELD_CLUE_LISTNAME) && cid.has(FIELD_CLUE_INDEX)) {
            String listName = cid.getString(FIELD_CLUE_LISTNAME);
            int index = cid.getInt(FIELD_CLUE_INDEX);
            return new ClueID(listName, index);
        // Version 2
        } else if (cid.has(FIELD_CLUE_LISTNAME) && cid.has(FIELD_CLUE_NUMBER)) {
            String number = cid.optString(FIELD_CLUE_NUMBER);
            if (number == null)
                number = String.valueOf(cid.getInt(FIELD_CLUE_NUMBER));
            String listName = cid.getString(FIELD_CLUE_LISTNAME);
            return getClueIDFromListNum(listName, number, builder);
        // Version 1
        } else if (cid.has(FIELD_CLUE_NUMBER) && cid.has(FIELD_CLUE_ACROSS)) {
            boolean across = cid.getBoolean(FIELD_CLUE_ACROSS);
            String listName = across
                ? OLD_ACROSS_LIST_NAME
                : OLD_DOWN_LIST_NAME;
            String number = cid.optString(FIELD_CLUE_NUMBER);
            if (number == null)
                number = String.valueOf(cid.getInt(FIELD_CLUE_NUMBER));
            return getClueIDFromListNum(listName, number, builder);
        } else {
            throw new IPuzFormatException(
                "Could not decode ClueID from " + cid
            );
        }
    }

    /**
     * Find the clue ID from listname and clue number
     *
     * Requires buidler to return a puzzle with clue lists already set
     * up.
     */
    private static ClueID getClueIDFromListNum(
        String listName, String number, PuzzleBuilder builder
    ) throws IPuzFormatException {
        Puzzle puz = builder.getPuzzle();

        ClueList clues = puz.getClues(listName);
        if (clues == null) {
            throw new IPuzFormatException(
                "Clue ID with non-existent list name: " + listName
            );
        }

        int index = clues.getClueIndex(number);
        if (index < 0) {
            throw new IPuzFormatException(
                "Clue ID with non-existent number " + number
                + " in list " + listName
            );
        }

        return new ClueID(listName, index);
    }

    /**
     * For acrostics, ensure there's a clue for selecting whole board
     *
     * Else the user has to pick box by box, which is annoying. Add one
     * if needed.
     */
    private static void ensureBoardClue(PuzzleBuilder builder) {
        Puzzle puz = builder.getPuzzle();

        Zone boardZone = getCluedBoardZone(puz);
        if (!hasClueCoveringZone(puz, boardZone)) {
            int index = builder.getNextClueIndex(ACROSTIC_BOARD_CLUE_LIST);
            builder.addClue(new Clue(
                ACROSTIC_BOARD_CLUE_LIST,
                index,
                null,
                ACROSTIC_BOARD_CLUE_HINT,
                boardZone
            ));
        }
    }

    /**
     * True if there's a clue in puz with all position in zone
     */
    private static boolean hasClueCoveringZone(Puzzle puz, Zone zone) {
        for (Clue clue : puz.getAllClues()) {
            if (zoneCoversZone(clue.getZone(), zone))
                return true;
        }
        return false;
    }

    /**
     * True if all positions of coveredZone are in coveringZone
     *
     * Assumes coveredZone doesn't contain duplicates. Positions don't
     * have to be in same order.
     */
    private static boolean zoneCoversZone(
        Zone coveringZone, Zone coveredZone
    ) {
        if (coveringZone == coveredZone)
            return true;
        if (coveredZone == null)
            return true;
        if (coveringZone == null)
            return coveredZone.isEmpty();

        // this optimisation won't work if coveredZone contain duplicate
        // elements... (never in the way we use this)
        if (coveringZone.size() < coveredZone.size())
            return false;

        Set<Position> coveringPositions = new HashSet<>(coveringZone.size());
        for (Position pos : coveringZone)
            coveringPositions.add(pos);

        for (Position pos : coveredZone) {
            if (!coveringPositions.contains(pos))
                return false;
        }

        return true;
    }

    /**
     * Make a zone containing all puzzle cells appearing in a clue
     */
    private static Zone getCluedBoardZone(Puzzle puz) {
        Set<Position> cluedPositions = new HashSet<>();
        for (Clue clue : puz.getAllClues())
            for (Position pos : clue.getZone())
                cluedPositions.add(pos);

        Zone boardZone = new Zone();
        for (int row = 0; row < puz.getHeight(); row++) {
            for (int col = 0; col < puz.getWidth(); col++) {
                Box box = puz.checkedGetBox(row, col);
                if (box != null) {
                    Position pos = new Position(row, col);
                    if (cluedPositions.contains(pos))
                        boardZone.addPosition(pos);
                }
            }
        }

        return boardZone;
    }

    public static void writePuzzle(Puzzle puz, OutputStream os)
            throws IOException {
        writePuzzle(puz, os, false);
    }

    /**
     * Write puzzle to os using WRITE_CHARSET
     */
    public static void writePuzzle(
        Puzzle puz, OutputStream os, boolean omitPlayState
    ) throws IOException {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(
                new OutputStreamWriter(os, WRITE_CHARSET)
            );

            FormatableJSONWriter jsonWriter = new FormatableJSONWriter(writer);

            jsonWriter.object();
            jsonWriter.newLine();

            writeIPuzHeader(puz, jsonWriter);
            writeMetaData(puz, jsonWriter);
            writeBoxes(puz, jsonWriter, omitPlayState);
            writeClues(puz, jsonWriter);
            writeExtensions(puz, jsonWriter, omitPlayState);

            jsonWriter.endObject();
            jsonWriter.newLine();
        } finally {
            // don't close original output stream, it's the caller's job
            if (writer != null)
                writer.flush();
        };
    }

    /**
     * Write IPuz version and kind
     */
    private static void writeIPuzHeader(Puzzle puz, FormatableJSONWriter writer)
            throws IOException {
        writer.keyValueNonNull(FIELD_VERSION, WRITE_VERSION)
            .key(FIELD_KIND)
            .array();

        switch (puz.getKind()) {
        case CROSSWORD:
            writer.value(WRITE_KIND);
            break;
        case ACROSTIC:
            writer.value(WRITE_KIND_ACROSTIC);
            break;
        }

        writer.endArray();
        writer.newLine();
    }

    /**
     * Add basic puzzle metadata (title, author, etc).
     */
    private static void writeMetaData(Puzzle puz, FormatableJSONWriter writer)
            throws IOException {
        writer
            .keyValueNonNull(FIELD_TITLE, puz.getTitle())
            .keyValueNonNull(FIELD_AUTHOR, puz.getAuthor())
            .keyValueNonNull(FIELD_COPYRIGHT, puz.getCopyright())
            .keyValueNonNull(FIELD_INTRO, puz.getIntroMessage())
            .keyValueNonNull(FIELD_NOTES, puz.getNotes())
            .keyValueNonNull(FIELD_EXPLANATION, puz.getCompletionMessage())
            .keyValueNonNull(FIELD_URL, puz.getSourceUrl())
            .keyValueNonNull(FIELD_PUBLISHER, puz.getSource());

        LocalDate date = puz.getDate();
        if (date != null)
            writer.keyValueNonNull(FIELD_DATE, DATE_FORMATTER.format(date));
    }

    /**
     * Read all IPuz supported box information to json
     */
    private static void writeBoxes(
        Puzzle puz, FormatableJSONWriter writer, boolean omitPlayState
    ) throws IOException {
        writeDimensions(puz, writer);
        writePuzzleCells(puz, writer);
        if (!omitPlayState)
            writeSaved(puz, writer);
        writeSolution(puz, writer);
    }

    /**
     * Add puzzle dimensions to json
     */
    private static void writeDimensions(Puzzle puz, FormatableJSONWriter writer)
            throws IOException {
        writer.key(FIELD_DIMENSIONS)
            .object()
                .key(FIELD_WIDTH).value(puz.getWidth())
                .key(FIELD_HEIGHT).value(puz.getHeight())
            .endObject();
        writer.newLine();
    }

    /**
     * Add the puzzle field to json
     */
    private static void writePuzzleCells(Puzzle puz, FormatableJSONWriter writer)
            throws IOException {
        writer.key(FIELD_PUZZLE)
            .array();
        writer.newLine();

        Box[][] boxes = puz.getBoxes();

        for (int row = 0; row < boxes.length; row++) {
            writer.indent(1)
                .array();

            for (int col = 0; col < boxes[row].length; col++) {
                Box box = boxes[row][col];

                if (box == null) {
                    writer.value(DEFAULT_BLOCK);
                } else {
                    String clueNumber = box.getClueNumber();
                    if (isCellWithStyle(box)) {
                        writer.object()
                            .key(FIELD_STYLE);

                        writeCellStyleObj(box, writer);

                        if (clueNumber != null)
                            writer.key(FIELD_CELL).value(clueNumber);
                        else
                            writer.key(FIELD_CELL).value(DEFAULT_EMPTY_WRITE);

                        writer.endObject();
                    } else if (clueNumber != null) {
                        writer.value(clueNumber);
                    } else {
                        writer.value(DEFAULT_EMPTY_WRITE);
                    }
                }
            }

            writer.endArray();
            writer.newLine();
        }

        writer.endArray();
        writer.newLine();
    }

    private static boolean isCellWithStyle(Box box) {
        return box.isCircled()
            || box.isBarred()
            || box.isDashed()
            || box.hasColor()
            || box.hasMarks();
    }

    private static void writeCellStyleObj(
        Box box, FormatableJSONWriter writer
    ) {
        writer.object();

        if (box.hasColor()) {
            writer.key(FIELD_COLOR)
                .value(colorToHex(box.getColor()));
        }

        if (box.isCircled()) {
            writer.key(FIELD_SHAPE_BG).value(SHAPE_BG_CIRCLE);
        }

        writeBarredField(box, writer);
        writeDashedField(box, writer);
        writeMarkField(box, writer);

        writer.endObject();
    }

    private static void writeBarredField(Box box, FormatableJSONWriter writer) {
        if (box.isBarred()) {
            String barred = "";
            if (box.isBarredTop())
                barred += BARRED_TOP;
            if (box.isBarredRight())
                barred += BARRED_RIGHT;
            if (box.isBarredBottom())
                barred += BARRED_BOTTOM;
            if (box.isBarredLeft())
                barred += BARRED_LEFT;

            writer.key(FIELD_BARRED).value(barred.toString());
        }
    }

    private static void writeDashedField(Box box, FormatableJSONWriter writer) {
        if (box.isDashed()) {
            String dashed = "";
            if (box.isDashedTop())
                dashed += DASHED_TOP;
            if (box.isDashedRight())
                dashed += DASHED_RIGHT;
            if (box.isDashedBottom())
                dashed += DASHED_BOTTOM;
            if (box.isDashedLeft())
                dashed += DASHED_LEFT;

            writer.key(FIELD_DASHED).value(dashed.toString());
        }
    }

    private static void writeMarkField(Box box, FormatableJSONWriter writer) {
        if (box.hasMarks()) {
            writer.key(FIELD_MARK)
                .object();

            String[][] marks = box.getMarks();
            writeMark(FIELD_MARK_TOP_LEFT, marks[0][0], writer);
            writeMark(FIELD_MARK_TOP, marks[0][1], writer);
            writeMark(FIELD_MARK_TOP_RIGHT, marks[0][2], writer);
            writeMark(FIELD_MARK_LEFT, marks[1][0], writer);
            writeMark(FIELD_MARK_CENTER, marks[1][1], writer);
            writeMark(FIELD_MARK_RIGHT, marks[1][2], writer);
            writeMark(FIELD_MARK_BOTTOM_LEFT, marks[2][0], writer);
            writeMark(FIELD_MARK_BOTTOM, marks[2][1], writer);
            writeMark(FIELD_MARK_BOTTOM_RIGHT, marks[2][2], writer);

            writer.endObject();
        }
    }

    private static void writeMark(
        String markField, String text, FormatableJSONWriter writer
    ) {
        if (text == null)
            return;
        writer.key(markField).value(text);
    }

    /**
     * Add the saved field to json
     */
    private static void writeSaved(Puzzle puz, FormatableJSONWriter writer)
            throws IOException {
        writer.key(FIELD_SAVED)
            .array();
        writer.newLine();

        Box[][] boxes = puz.getBoxes();

        for (int row = 0; row < boxes.length; row++) {
            writer.indent(1)
                .array();

            for (int col = 0; col < boxes[row].length; col++) {
                Box box = boxes[row][col];

                if (box == null)
                    writer.value(DEFAULT_BLOCK);
                else if (box.isBlank())
                    writer.value(DEFAULT_EMPTY_WRITE);
                else
                    writer.value(String.valueOf(box.getResponse()));
            }

            writer.endArray();
            writer.newLine();
        }

        writer.endArray();
        writer.newLine();
    }

    /**
     * Add the solution field to json if the puzzle has one
     */
    private static void writeSolution(Puzzle puz, FormatableJSONWriter writer)
            throws IOException {
        if (!puz.hasSolution())
            return;

        writer.key(FIELD_SOLUTION)
            .array();
        writer.newLine();

        Box[][] boxes = puz.getBoxes();

        for (int row = 0; row < boxes.length; row++) {
            writer.indent(1)
                .array();

            for (int col = 0; col < boxes[row].length; col++) {
                Box box = boxes[row][col];

                if (box == null) {
                    writer.value(DEFAULT_BLOCK);
                } else if (box.hasSolution()) {
                    writer.value(String.valueOf(box.getSolution()));
                } else {
                    writer.value(JSONObject.NULL);
                }
            }

            writer.endArray();
            writer.newLine();
        }

        writer.endArray();
        writer.newLine();
    }

    /**
     * Add the clues lists to the json
     */
    private static void writeClues(Puzzle puz, FormatableJSONWriter writer)
            throws IOException {
        writer.key(FIELD_CLUES)
            .object();
        writer.newLine();

        // Sort so guaranteed output order
        List<String> listNames = new ArrayList<>(puz.getClueListNames());
        Collections.sort(listNames);

        for (String listName : listNames) {
            ClueList clues = puz.getClues(listName);
            String direction = null;

            // check if a standard direction or "zones", else default to
            // "clues" list

            if (PuzzleUtils.isAcrossList(puz, clues)) {
                direction = FIELD_CLUES_ACROSS;
            } else if (PuzzleUtils.isDownList(puz, clues)) {
                direction = FIELD_CLUES_DOWN;
            } else if (PuzzleUtils.isZonesList(clues)) {
                direction = FIELD_CLUES_ZONES;
            }
            // TODO: other directions

            if (direction != null) {
                String fullListName = listName;
                if (!direction.equals(fullListName))
                    fullListName = direction + ":" + listName;
                boolean zones = FIELD_CLUES_ZONES.equals(direction);
                writeClueList(fullListName, clues, zones, writer);
            } else {
                // assume unrecognised fields are all Clues:xxx
                String fullListName = listName;
                if (!FIELD_CLUES_CLUES.equals(listName))
                    fullListName = FIELD_CLUES_CLUES + ":" + listName;
                writeClueList(fullListName, clues, false, writer);
            }
        }

        writer.endObject();
        writer.newLine();
    }

    /**
     * Convert a clues list into a json array and return it
     */
    private static void writeClueList(
        String fieldName,
        Iterable<Clue> clues,
        boolean withZones,
        FormatableJSONWriter writer
    ) throws IOException {
        writer.indent(1)
            .key(fieldName)
            .array();
        writer.newLine();

        for (Clue clue : clues) {
            String hint = clue.getHint();
            if (hint == null)
                hint = NULL_CLUE;

            writer.indent(2);

            if (withZones || clue.hasLabel()) {
                writer.object();
                writer.newLine();

                if (clue.hasClueNumber()) {
                    writer.indent(3)
                        .key(FIELD_CLUE_NUMBER)
                        .value(clue.getClueNumber());
                    writer.newLine();
                }

                if (clue.hasLabel()) {
                    writer.indent(3)
                        .key(FIELD_CLUE_LABEL)
                        .value(clue.getLabel());
                    writer.newLine();
                }

                writer.indent(3)
                    .key(FIELD_CLUE_HINT)
                    .value(clue.getHint());
                writer.newLine();

                if (clue.hasZone()) {
                    writer.indent(3)
                        .key(FIELD_CLUE_CELLS)
                        .array();
                    Zone zone = clue.getZone();
                    for (Position pos : zone) {
                        writer.array()
                            .value(pos.getCol())
                            .value(pos.getRow())
                            .endArray();
                    }
                    writer.endArray();
                    writer.newLine();
                }
                writer.indent(2)
                    .endObject();
            } else {
                if (clue.hasClueNumber()) {
                    writer.array()
                        .value(clue.getClueNumber())
                        .value(hint)
                        .endArray();
                } else {
                    writer.value(hint);
                }
            }
            writer.newLine();
        }

        writer.indent(1)
            .endArray();
        writer.newLine();
    }

    /**
     * Write Puzzle features not natively supported by IPuz
     */
    private static void writeExtensions(
        Puzzle puz, FormatableJSONWriter writer, boolean omitPlayState
    ) throws IOException {
        writeExtensionVolatility(writer);

        writer.keyValueNonNull(FIELD_EXT_SUPPORT_URL, puz.getSupportUrl());
        writer.keyValueNonNull(FIELD_EXT_SHARE_URL, puz.getShareUrl());

        if (puz.hasPinnedClueID()) {
            writer.key(FIELD_EXT_PINNED_CLUE_ID);
            writeClueID(puz.getPinnedClueID(), writer);
            writer.newLine();
        }

        writer.keyValueNonNull(FIELD_EXT_IO_VERSION, IO_VERSION);

        writeImages(puz, writer);

        if (!omitPlayState) {
            writer.key(FIELD_EXT_PLAY_DATA)
                .object();
            writer.newLine();

            writeBoxExtras(puz, writer);
            writePosition(puz, writer);
            writeClueHistory(puz, writer);
            writeClueNotes(puz, writer);
            writePlayerNote(puz, writer);
            writeFlaggedClues(puz, writer);

            writer.keyValueNonNull(1, FIELD_COMPLETION_TIME, puz.getTime())
                .keyValueNonNull(1, FIELD_PCNT_FILLED, puz.getPercentFilled())
                .keyValueNonNull(1, FIELD_PCNT_COMPLETE, puz.getPercentComplete())
                .keyValueNonNull(1, FIELD_UPDATABLE, puz.isUpdatable());

            writer.endObject();
            writer.newLine();
        }
    }

    private static void writeImages(
        Puzzle puz, FormatableJSONWriter writer
    ) throws IOException {
        List<PuzImage> images = puz.getImages();
        if (images.size() > 0) {
            writer.key(FIELD_EXT_IMAGES)
                .array();
            writer.newLine();

            for (PuzImage image : images) {
                writer.object();
                writer.keyValueNonNull(1, FIELD_EXT_IMAGE_URL, image.getURL())
                    .keyValueNonNull(1, FIELD_EXT_IMAGE_ROW, image.getRow())
                    .keyValueNonNull(1, FIELD_EXT_IMAGE_COL, image.getCol())
                    .keyValueNonNull(1, FIELD_EXT_IMAGE_WIDTH, image.getWidth())
                    .keyValueNonNull(
                        1, FIELD_EXT_IMAGE_HEIGHT, image.getHeight()
                    )
                    .endObject();
                writer.newLine();
            }

            writer.endArray();
            writer.newLine();
        }
    }

    /**
     * Add Note objects about individual clues to json
     */
    private static void writeClueNotes(Puzzle puz, FormatableJSONWriter writer)
            throws IOException {
        writer.indent(1)
            .key(FIELD_CLUE_NOTES)
            .array();
        writer.newLine();

        // Sort so guaranteed output order
        List<String> listNames = new ArrayList<>(puz.getClueListNames());
        Collections.sort(listNames);

        for (String listName : listNames) {
            for (Clue clue : puz.getClues(listName)) {
                ClueID cid = clue.getClueID();
                Note note = puz.getNote(cid);
                if (note != null && !note.isEmpty()) {
                    writer.indent(2)
                        .object();
                    writer.newLine()
                        .indent(3)
                        .key(FIELD_CLUE_NOTE_CLUE);
                    writeClueID(cid, writer);
                    writer.newLine()
                        .keyValueNonNull(
                            3,
                            FIELD_NOTE_SCRATCH,
                            note.getCompressedScratch()
                        ).keyValueNonNull(
                            3,
                            FIELD_NOTE_TEXT,
                            htmlString(note.getText())
                        ).keyValueNonNull(
                            3,
                            FIELD_NOTE_ANAGRAM_SRC,
                            note.getCompressedAnagramSource()
                        ).keyValueNonNull(
                            3,
                            FIELD_NOTE_ANAGRAM_SOL,
                            note.getCompressedAnagramSolution()
                        );
                    writer.indent(2)
                        .endObject();
                    writer.newLine();
                }
            }
        }

        writer.indent(1)
            .endArray();
        writer.newLine();
    }

/**
     * Add player Note object
     */
    private static void writePlayerNote(Puzzle puz, FormatableJSONWriter writer)
            throws IOException {
        Note note = puz.getPlayerNote();
        if (note != null && !note.isEmpty()) {
            writer.indent(1)
                .key(FIELD_PLAYER_NOTE)
                .object();
            writer.newLine()
                .keyValueNonNull(
                    2,
                    FIELD_NOTE_SCRATCH,
                    note.getCompressedScratch()
                ).keyValueNonNull(
                    2,
                    FIELD_NOTE_TEXT,
                    htmlString(note.getText())
                ).keyValueNonNull(
                    2,
                    FIELD_NOTE_ANAGRAM_SRC,
                    note.getCompressedAnagramSource()
                ).keyValueNonNull(
                    2,
                    FIELD_NOTE_ANAGRAM_SOL,
                    note.getCompressedAnagramSolution()
                );
            writer.indent(1)
                .endObject();
            writer.newLine();
        }
    }

    private static void writeFlaggedClues(
        Puzzle puz, FormatableJSONWriter writer
    ) throws IOException {
        writer.indent(1)
            .key(FIELD_FLAGGED_CLUES)
            .array();
        writer.newLine();

        // guarantee writing order
        List<ClueID> flagged = new ArrayList<>(puz.getFlaggedClues());
        Collections.sort(flagged);

        for (ClueID cnd : flagged) {
            writer.indent(2);
            writeClueID(cnd, writer);
            writer.newLine();
        }

        writer.indent(1)
            .endArray();
        writer.newLine();
    }

    /**
     * Write a ClueID on one line as a JSONObject
     */
    private static void writeClueID(
        ClueID cid, FormatableJSONWriter writer
    ) throws IOException {
        if (cid != null) {
            writer.object()
                .key(FIELD_CLUE_LISTNAME).value(cid.getListName())
                .key(FIELD_CLUE_INDEX).value(cid.getIndex())
                .endObject();
        }
    }

    /**
     * Write clue history list
     */
    private static void writeClueHistory(Puzzle puz, FormatableJSONWriter writer)
            throws IOException {
        List<ClueID> history = puz.getHistory();
        if (history.isEmpty())
            return;

        writer.indent(1)
            .key(FIELD_CLUE_HISTORY)
            .array();
        writer.newLine();

        for (ClueID item : puz.getHistory()) {
            writer.indent(2);
            writeClueID(item, writer);
            writer.newLine();
        }

        writer.indent(1)
            .endArray();
        writer.newLine();
    }

    /**
     * Write current highlight position
     */
    private static void writePosition(Puzzle puz, FormatableJSONWriter writer)
            throws IOException {
        Position pos = puz.getPosition();
        if (pos == null)
            return;

        writer.indent(1)
            .key(FIELD_POSITION)
            .object()
            .key(FIELD_POSITION_ROW).value(pos.getRow())
            .key(FIELD_POSITION_COL).value(pos.getCol());
        ClueID cid = puz.getCurrentClueID();
        if (cid != null) {
            writer.key(FIELD_POSITION_CLUEID);
            writeClueID(cid, writer);
        }
        writer.endObject();
        writer.newLine();
    }

    /**
     * Write additional info about boxes (cheated, responder) if any
     */
    private static void writeBoxExtras(Puzzle puz, FormatableJSONWriter writer)
            throws IOException {
        if (!puz.hasCheated() && !puz.hasResponders())
            return;

        Box[][] boxes = puz.getBoxes();
        if (boxes == null)
            return;

        writer.indent(1)
            .key(FIELD_BOX_EXTRAS)
            .array();
        writer.newLine();

        for (int row = 0; row < boxes.length; row++) {
            writer.indent(2)
                .array();
            for (int col = 0; col < boxes[row].length; col++) {
                writer.object();

                Box box = boxes[row][col];
                if (box != null) {
                    if (box.isCheated())
                        writer.key(FIELD_BOX_CHEATED).value(true);
                    String responder = box.getResponder();
                    if (responder != null)
                        writer.key(FIELD_BOX_RESPONDER).value(responder);
                }

                writer.endObject();
            }

            writer.endArray();
            writer.newLine();
        }

        writer.indent(1)
            .endArray();
        writer.newLine();
    }

    /**
     * Write volatility info about our extensions to json
     */
    private static void writeExtensionVolatility(FormatableJSONWriter writer)
            throws IOException {
        writer.key(FIELD_VOLATILE)
            .object();
        writer.newLine();

        for (String field : VOLATILE_EXTENSIONS)
            writer.keyValueNonNull(1, field, FIELD_IS_VOLATILE);

        for (String field : NON_VOLATILE_EXTENSIONS)
            writer.keyValueNonNull(1, field, FIELD_IS_NOT_VOLATILE);

        writer.endObject();
        writer.newLine();
    }

    /**
     * Returns fully qualified name of an extension field
     */
    private static String getQualifiedExtensionName(String fieldName) {
        return EXT_NAMESPACE + ":" + fieldName;
    }

    /**
     * Convert dddddd to 0x00dddddd
     */
    private static int hexToColor(String hex) {
        return Integer.valueOf(hex, 16);
    }

    /**
     * Convert a color to dddddd
     */
    private static String colorToHex(int color) {
        // ignore alpha channel
        return String.format(
            HEX_COLOR_FORMAT, color & 0x00ffffff
        );
    }

    /**
     * Reads field from json, returns null if not there or empty val
     */
    private static String optStringNull(JSONObject json, String field) {
        String value = json.optString(field);
        if (value == null || value.isEmpty())
            return null;
        return value;
    }

    /**
     * Extend JSONWriter with a write method to add custom formatting
     */
    private static class FormatableJSONWriter extends JSONWriter {
        public FormatableJSONWriter(Appendable writer) {
            super(writer);
        }

        /**
         * Writes the field if it is not null with trailing new line
         * @return self for chaining
         */
        public FormatableJSONWriter keyValueNonNull(String field, Object value)
                throws IOException {
            keyValueNonNull(0, field, value);
            return this;
        }

        /**
         * Writes the field if not null with trailing new line and indent
         * @return self for chaining
         */
        public FormatableJSONWriter keyValueNonNull(
            int indentSteps, String field, Object value
        ) throws IOException {
            if (value != null) {
                indent(indentSteps);
                key(field);
                value(value);
                newLine();
            }
            return this;
        }

        public FormatableJSONWriter newLine() throws IOException {
            writer.append("\n");
            return this;
        }

        public FormatableJSONWriter indent(int count) throws IOException {
            for (int i = 0; i < count; i++)
                writer.append("\t");
            return this;
        }
    }

    private static class IPuzClue {
        private String number;
        private String label;
        private String hint;
        private Zone zone;

        public IPuzClue(String number, String label, String hint, Zone zone) {
            this.number = number;
            this.label = label;
            this.hint = hint;
            this.zone = zone;
        }

        public IPuzClue(String number, String hint) {
            this(number, null, hint, null);
        }

        public IPuzClue(String hint) {
            this(null, null, hint, null);
        }

        public String getClueNumber() { return number; }
        public String getLabel() { return label; }
        public String getHint() { return hint; }
        public Zone getZone() { return zone; }
    }
}
