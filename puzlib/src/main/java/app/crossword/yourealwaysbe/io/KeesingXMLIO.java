
package app.crossword.yourealwaysbe.io;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Converts a puzzle from the XML format used by web.keesing.com
 *
 * This is not necessarily a complete implementation, but works for the
 * sources tested.
 *
 * The (supported) XML format is:
 *
 * <puzzle
 *  type="Crossword"
 *  width="[n]"
 *  height="[n]"
 *  ...
 * >
 *   <title>
 *   <grid>
 *     <cells>
 *        <cell
 *             x="[x]" y="[y]"
 *             visible="[1/0]"
 *             content="[solution letter]"
 *             giveaway="[1/0]" (display solution)
 *             fillable="[1/0]" (blocks have 0)
 *        />
 *        ...
 *     </cells>
 *   </grid>
 *   <wordgroups>
 *     <wordgroup index="[n]" kind="[horizontal|vertical|??]">
 *     <header>[listname]</header>
 *     <words>
 *       <word
 *         length="[n]"
 *         content="[solution]"
 *         giveaway="[0/1]"
 *         number="0" (i think this means infer number)
 *       />
 *         <cells>
 *           <cell x="[x]" y="[y]"/>
 *           ...
 *         </cells>
 *         <puzzleword>[solution]</puzzleword>
 *         <clue>[hint]</clue>
 *       </word>
 *       ...
 *     </words>
 *   </wordgroups>
 * </puzzle>
 */
public class KeesingXMLIO implements PuzzleParser {
    private static final Logger LOG = Logger.getLogger(
        KeesingXMLIO.class.getCanonicalName()
    );

    public static class KeesingXMLParserException extends Exception {
        public KeesingXMLParserException(String msg) {
            super(msg);
        }
    }

    // time to make a superclass?
    private static class ClueInfo {
        private String listName;
        private String clueNumber;
        private String hint;
        private Zone zone;

        public ClueInfo(
            String listName, String clueNumber, String hint, Zone zone
        ) {
            this.listName = listName;
            this.clueNumber = clueNumber;
            this.hint = hint;
            this.zone = zone;
        }

        public String getListName() { return listName; }
        public String getClueNumber() { return clueNumber; }
        public String getHint() { return hint; }
        public Zone getZone() { return zone; }

        public void setClueNumber(String clueNumber) {
            this.clueNumber = clueNumber;
        }
    }

    private static class KeesingXMLParser extends DefaultHandler {
        private String type;
        private String title = "";
        private int width;
        private int height;
        private Box[][] boxes;
        private List<ClueInfo> clues = new LinkedList<>();
        private StringBuilder charBuffer = new StringBuilder();

        // sanity checks
        private boolean hasGridEle = false;
        private boolean hasWordGroupsEle = false;

        public String getTitle() { return title; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public Box[][] getBoxes() { return boxes; }
        public List<ClueInfo> getClues() { return clues; }

        /**
         * Best assessment of whether read succeeded
         */
        public boolean isSuccessfulRead() {
            return "Crossword".equalsIgnoreCase(type)
                && hasGridEle
                && hasWordGroupsEle
                && getWidth() > 0
                && getHeight() > 0
                && getClues().size() > 0;
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

                if (name.equalsIgnoreCase("puzzle")) {
                    charBuffer.delete(0, charBuffer.length());

                    type = attributes.getValue("type");
                    width = Integer.parseInt(attributes.getValue("width"));
                    height = Integer.parseInt(attributes.getValue("height"));
                    boxes = new Box[height][width];
                } else if (name.equalsIgnoreCase("title")) {
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

                if (name.equalsIgnoreCase("cell")) {
                    parseCell(attributes);
                }
            }

            private void parseCell(Attributes attributes) {
                int x = Integer.parseInt(attributes.getValue("x"));
                int y = Integer.parseInt(attributes.getValue("y"));

                if (
                    isCell(attributes)
                    && 0 <= x && x < getWidth()
                    && 0 <= y && y < getHeight()
                ) {
                    Box box = new Box();

                    String solution = attributes.getValue("content");
                    if (solution != null && solution.length() > 0) {
                        box.setSolution(solution);
                        if ("1".equals(attributes.getValue("giveaway")))
                            box.setResponse(solution);
                    }

                    boxes[y][x] = box;
                }
            }

            private boolean isCell(Attributes attributes) {
                String content = attributes.getValue("content");
                String fillable = attributes.getValue("fillable");

                boolean block =
                    (content == null || content.isEmpty())
                    && "0".equals(fillable);

                return !block;
            }
        };

        private DefaultHandler inClues = new DefaultHandler() {
            private String inListName = null;
            private String inClueNumber = null;
            private Zone inClueZone = null;

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
                    if (name.equalsIgnoreCase("wordgroup")) {
                        charBuffer.delete(0, charBuffer.length());
                    } else if (name.equalsIgnoreCase("header")) {
                        charBuffer.delete(0, charBuffer.length());
                    } else if (name.equalsIgnoreCase("word")) {
                        charBuffer.delete(0, charBuffer.length());

                        inClueNumber = attributes.getValue("number");
                        if ("0".equals(inClueNumber))
                            inClueNumber = null;

                        inClueZone = new Zone();
                    } else if (name.equalsIgnoreCase("cell")) {
                        if (inClueZone != null) {
                            int x = Integer.parseInt(attributes.getValue("x"));
                            int y = Integer.parseInt(attributes.getValue("y"));
                            inClueZone.addPosition(new Position(y, x));
                        }
                    } else if (name.equalsIgnoreCase("clue")) {
                        charBuffer.delete(0, charBuffer.length());
                    } else {
                        charBuffer.append("<" + tagName + ">");
                    }
                } catch (NumberFormatException e) {
                    LOG.severe("Could not read Keesing XML cell data: " + e);
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

                if (name.equalsIgnoreCase("header")) {
                    inListName = HtmlUtil.unHtmlString(charBuffer.toString());
                } else if (name.equalsIgnoreCase("clue")) {
                    String clue = charBuffer.toString();

                    clues.add(new ClueInfo(
                        inListName, inClueNumber, clue, inClueZone
                    ));

                    inClueNumber = null;
                    inClueZone = null;
                } else if (name.equalsIgnoreCase("wordgroup")) {
                    inListName = null;
                } else {
                    charBuffer.append("</" + tagName + ">");
                }
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

            if (name.equalsIgnoreCase("grid")) {
                hasGridEle = true;
                state = inGrid;
            } else if (name.equalsIgnoreCase("wordgroups")) {
                hasWordGroupsEle = true;
                state = inClues;
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
            String name = strippedName.length() == 0
                ? tagName.trim()
                : strippedName;

            state.endElement(nsURI, strippedName, tagName);

            if (name.equalsIgnoreCase("grid")) {
                state = outerXML;
            } else if (name.equalsIgnoreCase("wordgroups")) {
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
        KeesingXMLParser handler = new KeesingXMLParser();
        xr.setContentHandler(handler);
        xr.parse(new InputSource(is));

        if (!handler.isSuccessfulRead())
            return null;

        PuzzleBuilder builder = new PuzzleBuilder(handler.getBoxes());
        builder.setTitle(handler.getTitle())
            .autoNumberBoxes();


        Map<String, List<ClueInfo>> clues = getSortedClues(
            handler.getClues(), builder.getPuzzle()
        );
        for (String listName : clues.keySet()) {
            for (ClueInfo clue : clues.get(listName)) {
                int index = builder.getNextClueIndex(listName);
                builder.addClue(new Clue(
                    listName, index, clue.getClueNumber(), null,
                    clue.getHint(), clue.getZone()
                ));
            }
        }

        return builder.getPuzzle();
    }

    public static boolean convertPuzzle(
        InputStream is, DataOutputStream os, LocalDate d
    ) {
        try {
            Puzzle puz = readPuzzle(is);
            puz.setDate(d);
            IO.saveNative(puz, os);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            LOG.severe("Unable to convert Keesing XML file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Fill in clue numbers and sort by them
     *
     * @return map from list name to clue info with clue number
     */
    private static Map<String, List<ClueInfo>> getSortedClues(
        List<ClueInfo> clues, Puzzle puz
    ) throws KeesingXMLParserException {
        Map<String, List<ClueInfo>> sortedClues = new HashMap<>();

        for (int i = 0; i < clues.size(); i++) {
            ClueInfo clue = clues.get(i);

            String listName = clue.getListName();
            if (listName == null)
                listName = "No list";

            if (!sortedClues.containsKey(listName))
                sortedClues.put(listName, new ArrayList<>());

            List<ClueInfo> clueList = sortedClues.get(listName);

            Zone zone = clue.getZone();
            if (zone != null && !zone.isEmpty()) {
                Box start
                    = puz.checkedGetBox(zone.getPosition(0));
                if (start == null) {
                    throw new KeesingXMLParserException(
                        "Clue starts at null box " + zone.getPosition(0)
                    );
                }
                clue.setClueNumber(start.getClueNumber());
            }

            clueList.add(clue);
        }

        for (List<ClueInfo> list : sortedClues.values()) {
            sortClueList(list);
        }

        return sortedClues;
    }

    private static void sortClueList(List<ClueInfo> clues) {
        Collections.sort(clues, new Comparator<ClueInfo> () {
            public int compare(ClueInfo ci1, ClueInfo ci2) {
                String num1 = ci1.getClueNumber();
                String num2 = ci2.getClueNumber();

                if (num1 == null && num2 == null)
                    return 0;
                else if (num1 == null)
                    return -1;
                else if (num2 == null)
                    return 1;

                try {
                    return Integer.compare(
                        Integer.parseInt(num1),
                        Integer.parseInt(num2)
                    );
                } catch (NumberFormatException e) {
                    return num1.compareTo(num2);
                }
            }
        });
    }
}
