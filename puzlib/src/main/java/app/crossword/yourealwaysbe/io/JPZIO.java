package app.crossword.yourealwaysbe.io;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleBuilder;
import app.crossword.yourealwaysbe.puz.Zone;
import app.crossword.yourealwaysbe.util.HtmlUtil;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Converts a puzzle from the JPZ Crossword Compiler XML format.
 *
 * This is not necessarily a complete implementation, but works for the
 * sources tested.
 *
 * Converts to the Across Lite .puz format.
 *
 * The (supported) XML format is:
 *
 * <crossword-compiler>
 *   <rectangular-puzzle>
 *     <metadata>
 *       <title>[Title]</title>
 *       <creator>[Author]</creator>
 *       <copyright>[Copyright]</copyright>
 *       <description>[Description]</description>
 *     </metadata>
 *     <crossword>
 *       <grid width="[width]" height="[height]">
 *         <cell x="[x]" y="[y]" solution="[letter]" ?number="[number]"/>
 *         <cell x="[x]" y="[y]" type="block" .../>
 *         ...
 *       </grid>
 *       <clues ordering="normal">
 *         <title><b>Across [or] Down</b></title>
 *         <clue number="[number]" format="[length]" citation="[explanation]">
 *           [clue]
 *         </clue>
 *         <clue number="[number]" is-link="[ordering num]">
 *           [clue]
 *         </clue>
*       </clues>
 *     </crossword>
 *   </rectangular-puzzle>
 * </crossword-compiler>
 */
public class JPZIO implements PuzzleParser {
    private static final Logger LOG
        = Logger.getLogger("app.crossword.yourealwaysbe");

    private static class ClueInfo extends ClueID {
        private String clueNumber;
        private String hint;
        private String zoneID;
        private String citation;

        public ClueInfo(
            String listName, int index,
            String clueNumber, String hint, String zoneID,
            String citation
        ) {
            super(listName, index);
            this.clueNumber = clueNumber;
            this.hint = hint;
            this.zoneID = zoneID;
            this.citation = citation;
        }

        public String getClueNumber() { return clueNumber; }
        public String getHint() { return hint; }
        public String getZoneID() { return zoneID; }
        public String getCitation() { return citation; }
    }

    private static class JPZXMLParser extends DefaultHandler {
        private String title = "";
        private String creator = "";
        private String copyright = "";
        private String description = "";
        private int width;
        private int height;
        private Box[][] boxes;
        private List<ClueInfo> clues = new LinkedList<>();
        private Map<String, Zone> zoneMap = new HashMap<>();
        private StringBuilder charBuffer = new StringBuilder();

        // sanity checks
        private boolean hasRectangularPuzzleEle = false;
        private boolean hasGridEle = false;
        private boolean hasCluesEle = false;

        public String getTitle() { return title; }
        public String getCreator() { return creator; }
        public String getCopyright() { return copyright; }
        public String getDescription() { return description; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public Box[][] getBoxes() { return boxes; }
        public List<ClueInfo> getClues() { return clues; }
        public Map<String, Zone> getZoneMap() { return zoneMap; }

        /**
         * Best assessment of whether read succeeded (i.e. was a JPZ
         * file)
         */
        public boolean isSuccessfulRead() {
            return hasRectangularPuzzleEle
                && hasGridEle
                && hasCluesEle
                && getWidth() > 0
                && getHeight() > 0
                && (getClues().size() > 0);
        }

        // Use several handlers to maintain three different modes:
        // outerXML, inGrid, and inClues

        private DefaultHandler outerXML = new DefaultHandler() {
            @Override
            public void startElement(String nsURI,
                                     String strippedName,
                                     String tagName,
                                     Attributes attributes) throws SAXException {
                strippedName = strippedName.trim();
                String name = strippedName.length() == 0
                    ? tagName.trim() : strippedName;

                if (name.equalsIgnoreCase("title")
                        || name.equalsIgnoreCase("creator")
                        || name.equalsIgnoreCase("copyright")
                        || name.equalsIgnoreCase("description")) {
                    charBuffer.delete(0, charBuffer.length());
                } else {
                    charBuffer.append("<" + tagName + ">");
                }
            }

            public void characters(char[] ch, int start, int length)
                    throws SAXException {
                charBuffer.append(ch, start, length);
            }

            @Override
            public void endElement(String nsURI,
                                   String strippedName,
                                   String tagName) throws SAXException {
                strippedName = strippedName.trim();
                String name = strippedName.length() == 0
                    ? tagName.trim() : strippedName;

                String charData = charBuffer.toString().trim();

                if (name.equalsIgnoreCase("title")) {
                    title = charData;
                } else if (name.equalsIgnoreCase("creator")) {
                    creator = charData;
                } else if (name.equalsIgnoreCase("copyright")) {
                    copyright = charData;
                } else if (name.equalsIgnoreCase("description")) {
                    description = charData;
                } else {
                    charBuffer.append("</" + tagName + ">");
                }
            }
        };

        private DefaultHandler inGrid = new DefaultHandler() {
            @Override
            public void startElement(String nsURI,
                                     String strippedName,
                                     String tagName,
                                     Attributes attributes) throws SAXException {
                strippedName = strippedName.trim();
                String name = strippedName.length() == 0
                    ? tagName.trim() : strippedName;

                try {
                    if (name.equalsIgnoreCase("grid")) {
                        JPZXMLParser.this.width
                            = Integer.parseInt(attributes.getValue("width"));
                        JPZXMLParser.this.height
                            = Integer.parseInt(attributes.getValue("height"));
                        JPZXMLParser.this.boxes = new Box[height][width];
                    } else if (name.equalsIgnoreCase("cell")) {
                        parseCell(attributes);
                    }
                } catch (NumberFormatException e) {
                    LOG.severe("Could not read JPZ XML cell data: " + e);
                }
            }

            private void parseCell(Attributes attributes) {
                int x = Integer.parseInt(attributes.getValue("x")) - 1;
                int y = Integer.parseInt(attributes.getValue("y")) - 1;

                String cellType = attributes.getValue("type");
                boolean isCell
                    = cellType == null || !"block".equalsIgnoreCase(cellType);

                if (
                    isCell
                    && 0 <= x && x < JPZXMLParser.this.getWidth()
                    && 0 <= y && y < JPZXMLParser.this.getHeight()
                ) {
                    Box box = new Box();

                    String solution = attributes.getValue("solution");
                    if (solution != null && solution.length() > 0)
                        box.setSolution(solution.charAt(0));

                    String response = attributes.getValue("solve-state");
                    if (response != null && response.length() > 0)
                        box.setResponse(response.charAt(0));
                    else
                        box.setBlank();

                    String number = attributes.getValue("number");
                    if (number != null) {
                        box.setClueNumber(number);
                    }

                    String shape
                        = attributes.getValue("background-shape");
                    if ("circle".equalsIgnoreCase(shape)) {
                        box.setCircled(true);
                    }

                    String color
                        = attributes.getValue("background-color");
                    // if is hex color
                    if (color != null
                            && color.startsWith("#")
                            && color.length() == 7) {
                        try {
                            box.setColor(Integer.valueOf(
                                color.substring(1), 16
                            ));
                        } catch (NumberFormatException e) {
                            // oh well, we tried
                        }
                    }

                    String topBar = attributes.getValue("top-bar");
                    box.setBarredTop("true".equalsIgnoreCase(topBar));
                    String bottomBar = attributes.getValue("bottom-bar");
                    box.setBarredBottom("true".equalsIgnoreCase(bottomBar));
                    String leftBar = attributes.getValue("left-bar");
                    box.setBarredLeft("true".equalsIgnoreCase(leftBar));
                    String rightBar = attributes.getValue("right-bar");
                    box.setBarredRight("true".equalsIgnoreCase(rightBar));

                    JPZXMLParser.this.boxes[y][x] = box;
                }
            }
        };

        private DefaultHandler inClues = new DefaultHandler() {
            private String inClueNum = null;
            private String inClueFormat = "";
            private String inListName = "No List";
            private String inClueZoneID = null;
            private String inClueCitation = null;
            private int inClueIndex = -1;

            private StringBuilder charBuffer = new StringBuilder();

            @Override
            public void startElement(String nsURI,
                                     String strippedName,
                                     String tagName,
                                     Attributes attributes) throws SAXException {
                strippedName = strippedName.trim();
                String name = strippedName.length() == 0
                    ? tagName.trim()
                    : strippedName;

                try {
                    if (name.equalsIgnoreCase("title")) {
                        charBuffer.delete(0, charBuffer.length());
                    } else if (name.equalsIgnoreCase("clue")) {
                        charBuffer.delete(0, charBuffer.length());

                        inClueNum = attributes.getValue("number");
                        inClueIndex += 1;

                        String link = attributes.getValue("is-link");
                        if (link == null) {
                            inClueFormat = attributes.getValue("format");
                            if (inClueFormat == null)
                                inClueFormat = "";

                            inClueCitation = attributes.getValue("citation");
                            inClueZoneID = attributes.getValue("word");

                            // clue appears in characters between start
                            // and end
                        }
                    } else {
                        charBuffer.append("<" + tagName + ">");
                    }
                } catch (NumberFormatException e) {
                    LOG.severe("Could not read JPZ XML cell data: " + e);
                }
            }

            @Override
            public void characters(char[] ch, int start, int length)
                    throws SAXException {
                charBuffer.append(ch, start, length);
            }

            @Override
            public void endElement(String nsURI,
                                   String strippedName,
                                   String tagName) throws SAXException {
                strippedName = strippedName.trim();
                String name = strippedName.length() == 0
                    ? tagName.trim()
                    : strippedName;

                if (name.equalsIgnoreCase("title")) {
                    inListName = HtmlUtil.unHtmlString(charBuffer.toString());
                    inClueIndex = -1;
                } else if (name.equalsIgnoreCase("clue")) {
                    String fullClue = charBuffer.toString();

                    if (inClueFormat.length() > 0) {
                        fullClue = String.format(
                            "%s (%s)", fullClue, inClueFormat
                        );
                    }

                    clues.add(
                        new ClueInfo(
                            inListName, inClueIndex, inClueNum,
                            fullClue, inClueZoneID,
                            inClueCitation
                        )
                    );

                    inClueNum = null;
                    inClueFormat = "";
                    inClueZoneID = null;
                    inClueCitation = null;
                } else {
                    charBuffer.append("</" + tagName + ">");
                }
            }
        };

        private DefaultHandler inWord = new DefaultHandler() {
            private String zoneID;
            private Zone zone;

            @Override
            public void startElement(String nsURI,
                                     String strippedName,
                                     String tagName,
                                     Attributes attributes) throws SAXException {
                strippedName = strippedName.trim();
                String name = strippedName.length() == 0
                    ? tagName.trim() : strippedName;

                if (name.equalsIgnoreCase("word")) {
                    zoneID = attributes.getValue("id");
                    zone = new Zone();

                    String x = attributes.getValue("x");
                    String y = attributes.getValue("y");
                    if (x != null && y != null)
                        parseCells(x, y);
                } else if (name.equalsIgnoreCase("cells")) {
                    parseCells(
                        attributes.getValue("x"),
                        attributes.getValue("y")
                    );
                }
            }

            @Override
            public void endElement(String nsURI,
                                   String strippedName,
                                   String tagName) throws SAXException {
                strippedName = strippedName.trim();
                String name = strippedName.length() == 0
                    ? tagName.trim()
                    : strippedName;

                if (name.equalsIgnoreCase("word")) {
                    if (zoneID != null)
                        zoneMap.put(zoneID, zone);
                    zoneID = null;
                    zone = null;
                }
            }

            /**
             * Parse cells data into zone
             *
             * E.g. x="1-3" y = "2" is (2, 1), (2, 2), (2, 3);
             */
            private void parseCells(String x, String y) {
                String[] xs = x.split("-");
                int xstart = Integer.valueOf(xs[0]) - 1;
                int xend = (xs.length > 1)
                    ? Integer.valueOf(xs[1]) - 1
                    : xstart;

                String[] ys = y.split("-");
                int ystart = Integer.valueOf(ys[0]) - 1;
                int yend = (ys.length > 1)
                    ? Integer.valueOf(ys[1]) - 1
                    : ystart;

                for (int row = ystart; row <= yend; row++)
                    for (int col = xstart; col <= xend; col++)
                        zone.addPosition(new Position(row, col));
            }
        };

        private DefaultHandler state = outerXML;

        @Override
        public void startElement(String nsURI,
                                 String strippedName,
                                 String tagName,
                                 Attributes attributes) throws SAXException {
            strippedName = strippedName.trim();
            String name = strippedName.length() == 0 ? tagName.trim() : strippedName;

            if (name.equalsIgnoreCase("rectangular-puzzle")) {
                hasRectangularPuzzleEle = true;
            } else if (name.equalsIgnoreCase("grid")) {
                hasGridEle = true;
                state = inGrid;
            } else if (name.equalsIgnoreCase("clues")) {
                hasCluesEle = true;
                state = inClues;
            } else if (name.equalsIgnoreCase("word")) {
                state = inWord;
            }

            state.startElement(nsURI, name, tagName, attributes);
        }

        @Override
        public void characters(char[] ch, int start, int length)
                throws SAXException {
            state.characters(ch, start, length);
        }

        @Override
        public void endElement(String nsURI,
                               String strippedName,
                               String tagName) throws SAXException {
            strippedName = strippedName.trim();
            String name = strippedName.length() == 0 ? tagName.trim() : strippedName;

            state.endElement(nsURI, strippedName, tagName);

            if (name.equalsIgnoreCase("grid")) {
                state = outerXML;
            } else if (name.equalsIgnoreCase("clues")) {
                state = outerXML;
            } else if (name.equalsIgnoreCase("word")) {
                state = outerXML;
            }
        }
    }

    @Override
    public Puzzle parseInput(InputStream is) throws Exception {
        return readPuzzle(is);
    }

    public static Puzzle readPuzzle(InputStream is) throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        XMLReader xr = parser.getXMLReader();
        JPZXMLParser handler = new JPZXMLParser();
        xr.setContentHandler(handler);
        xr.parse(new InputSource(is));

        if (!handler.isSuccessfulRead())
            return null;

        // TODO: move away from this and use JPZ words to build puzzle
        // directly
        PuzzleBuilder builder = new PuzzleBuilder(handler.getBoxes());
        builder.setTitle(handler.getTitle());
        builder.setAuthor(handler.getCreator());
        builder.setCopyright(handler.getCopyright());

        setClues(builder, handler);
        setNote(builder, handler);

        return builder.getPuzzle();
    }

    public static boolean convertPuzzle(InputStream is,
                                        DataOutputStream os,
                                        LocalDate d) {
        try {
            Puzzle puz = readPuzzle(is);
            puz.setDate(d);
            IO.saveNative(puz, os);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            LOG.severe("Unable to convert JPZ file: " + e.getMessage());
            return false;
        }
    }

    private static void setClues(PuzzleBuilder builder, JPZXMLParser handler) {
        Map<String, Zone> zones = handler.getZoneMap();

        List<ClueInfo> clues = handler.getClues();
        for (int i = 0; i < clues.size(); i++) {
            ClueInfo clue = clues.get(i);
            builder.addClue(new Clue(
                clue.getListName(),
                clue.getIndex(),
                clue.getClueNumber(),
                clue.getHint(),
                zones.get(clue.getZoneID())
            ));
        }
    }

    private static void setNote(PuzzleBuilder builder, JPZXMLParser handler) {
        StringBuilder notes = new StringBuilder();

        String description = handler.getDescription();
        if (description != null)
            notes.append(description);

        // sort lists into order then construct citations text
        Map<String, StringBuilder> listNotes = new HashMap<>();

        for (ClueInfo ci : handler.getClues()) {
            String number = ci.getClueNumber();
            String listName = ci.getListName();
            String citation = ci.getCitation();

            if (citation != null) {
                if (!listNotes.containsKey(listName))
                    listNotes.put(listName, new StringBuilder());

                listNotes.get(listName).append(
                    String.format("<p>%s: %s</p>", number, citation)
                );
            }
        }

        List<String> listNames = new ArrayList<>(listNotes.keySet());
        Collections.sort(listNames);

        for (String listName : listNames) {
            notes.append("<h1>" + listName + "</h1>");
            notes.append(listNotes.get(listName).toString());
        }

        builder.setNotes(notes.toString());
    }
}
