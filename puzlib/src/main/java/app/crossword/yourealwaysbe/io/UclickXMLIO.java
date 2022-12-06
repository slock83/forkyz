package app.crossword.yourealwaysbe.io;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleBuilder;
import app.crossword.yourealwaysbe.puz.PuzzleBuilder.BasicClue;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Converts a puzzle from the XML format used by uclick syndicated puzzles
 * to the Across Lite .puz format.  The format is:
 *
 * <crossword>
 *   <Title v="[Title]" />
 *   <Author v="[Author]" />
 *   <Width v="[Width]" />
 *   <Height v="[Height]" />
 *   <AllAnswer v="[Grid]" />
 *   <Copyright = "[Copyright]" />
 *   <across>
 *       <a[i] a="[Answer]" c="[Clue]" n="[GridIndex]" cn="[ClueNumber]" />
 *   </across>
 *   <down>
 *       <d[j] ... />
 *   </down>
 * </crossword>
 *
 * [Grid] contains all of the letters in the solution, reading left-to-right,
 * top-to-bottom, with - for black squares. [i] is an incrementing number for
 * each across clue, starting at 1. [GridIndex] is the offset into [Grid] at
 * which the clue starts.  [Clue] text is HTML escaped.
 */
public class UclickXMLIO implements PuzzleParser {
    private static String CHARSET_NAME = "utf8";
    private static final String ACROSS_LIST = "Across";
    private static final String DOWN_LIST = "Down";

    private static class UclickXMLParser extends DefaultHandler {
        private String title;
        private String author;
        private String copyright;
        private Box[] boxList;
        private List<BasicClue> acrossClues = new ArrayList<>();
        private List<BasicClue> downClues = new ArrayList<>();

        private boolean inAcross = false;
        private boolean inDown = false;
        private int maxClueNum = -1;
        private int width = 0;
        private int height = 0;

        public boolean isSuccessfulRead() {
            return boxList != null
                && acrossClues.size() > 0
                && downClues.size() > 0
                && maxClueNum > -1
                && width > 0
                && height > 0;
        }

        public String getTitle() { return title; }
        public String getAuthor() { return author; }
        public String getCopyright() { return copyright; }
        public Box[] getBoxList() { return boxList; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public List<BasicClue> getAcrossClues() { return acrossClues; }
        public List<BasicClue> getDownClues() { return downClues; }

        @Override
        public void startElement(String nsURI, String strippedName,
                String tagName, Attributes attributes) throws SAXException {
            strippedName = strippedName.trim();
            String name = strippedName.length() == 0 ? tagName.trim() : strippedName;
            //System.out.println("Start" + name);
            if (inAcross) {
                String clueNum = attributes.getValue("cn");
                int clueNumInt = Integer.parseInt(clueNum);
                if (clueNumInt > maxClueNum) {
                    maxClueNum = clueNumInt;
                }
                acrossClues.add(new BasicClue(
                    clueNum,
                    decodeValue(attributes.getValue("c"))
                ));
            } else if (inDown) {
                String clueNum = attributes.getValue("cn");
                int clueNumInt = Integer.parseInt(clueNum);
                if (clueNumInt > maxClueNum) {
                    maxClueNum = clueNumInt;
                }
                downClues.add(new BasicClue(
                    clueNum,
                    decodeValue(attributes.getValue("c"))
                ));
            } else if (name.equalsIgnoreCase("title")) {
                title = decodeValue(attributes.getValue("v"));
            } else if (name.equalsIgnoreCase("author")) {
                author = decodeValue(attributes.getValue("v"));
            } else if (name.equalsIgnoreCase("copyright")) {
                copyright = decodeValue(attributes.getValue("v"));
            } else if (name.equalsIgnoreCase("width")) {
                width = Integer.parseInt(attributes.getValue("v"));
            } else if (name.equalsIgnoreCase("height")) {
                height = Integer.parseInt(attributes.getValue("v"));
            } else if (name.equalsIgnoreCase("allanswer")) {
                String rawGrid = attributes.getValue("v");
                boxList = new Box[height*width];
                for (int i = 0; i < rawGrid.length(); i++) {
                    char sol = rawGrid.charAt(i);
                    if (sol != '-') {
                        boxList[i] = new Box();
                        boxList[i].setSolution(sol);
                        boxList[i].setBlank();
                    }
                }
            } else if (name.equalsIgnoreCase("across")) {
                inAcross = true;
            } else if (name.equalsIgnoreCase("down")) {
                inDown = true;
            }
        }

        @Override
        public void endElement(String nsURI, String strippedName,
                String tagName) throws SAXException {
            //System.out.println("EndElement " +nsURI+" : "+tagName);
            strippedName = strippedName.trim();
            String name = strippedName.length() == 0 ? tagName.trim() : strippedName;
            //System.out.println("End : "+name);

            if (name.equalsIgnoreCase("across")) {
                inAcross = false;
            } else if (name.equalsIgnoreCase("down")) {
                inDown = false;
            }
        }

        private String decodeValue(String value) {
            try {
                return URLDecoder.decode(value, CHARSET_NAME);
            } catch (UnsupportedEncodingException e) {
                return value;
            }
        }
    }

    @Override
    public Puzzle parseInput(InputStream is) {
        return parsePuzzle(is);
    }

    public static Puzzle parsePuzzle(InputStream is) {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            SAXParser parser = factory.newSAXParser();
            XMLReader xr = parser.getXMLReader();
            UclickXMLParser handler = new UclickXMLParser();
            xr.setContentHandler(handler);
            xr.parse(new InputSource(is));

            if (!handler.isSuccessfulRead())
                return null;

            PuzzleBuilder builder = new PuzzleBuilder(
                handler.getBoxList(), handler.getWidth(), handler.getHeight()
            );
            builder.autoNumberBoxes()
                .setTitle(handler.getTitle())
                .setAuthor(handler.getAuthor())
                .setCopyright(handler.getCopyright())
                .setNotes("");

            for (BasicClue basicClue : handler.getAcrossClues())
                builder.addAcrossClue(ACROSS_LIST, basicClue);

            for (BasicClue basicClue : handler.getDownClues())
                builder.addDownClue(DOWN_LIST, basicClue);

            return builder.getPuzzle();
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean convertUclickPuzzle(InputStream is, DataOutputStream os,
            String copyright, LocalDate d) {
        Puzzle puz = parsePuzzle(is);
        puz.setDate(d);
        if (copyright != null)
            puz.setCopyright(copyright);

        try {
            IO.saveNative(puz, os);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Unable to save puzzle: " + e.getMessage());
            return false;
        }
    }
}
