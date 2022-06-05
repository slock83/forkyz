package app.crossword.yourealwaysbe.io;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import junit.framework.TestCase;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.ClueList;
import app.crossword.yourealwaysbe.puz.Puzzle;

public class JPZIOTest extends TestCase {

    public JPZIOTest(String testName) {
        super(testName);
    }

    public static InputStream getTestPuzzle1InputStream() {
        return JPZIOTest.class.getResourceAsStream("/lat_puzzle_111128.xml");
    }

    public static void assertIsTestPuzzle1(Puzzle puz) {
        String ACROSS_CLUES = "Across";
        String DOWN_CLUES = "Down";

        assertEquals("LA Times, Mon, Nov 28, 2011", puz.getTitle());
        assertEquals("Jeff Chen / Ed. Rich Norris", puz.getAuthor());
        assertEquals("© 2011 Tribune Media Services, Inc.", puz.getCopyright());
        assertEquals(
            "Test"
                + "<h1>Down</h1>"
                + "<p>22: Shower Heads v7</p>"
                + "<p>61: I'm NOT going to ATTEND it / I'm going to SKIP it</p>",
            puz.getNotes()
        );

        Box[][] boxes = puz.getBoxes();

        assertEquals(15, boxes.length);
        assertEquals(15, boxes[0].length);
        assertEquals("1", boxes[0][0].getClueNumber());
        assertEquals(true, boxes[0][0].isStartOf(new ClueID(ACROSS_CLUES, 0)));
        assertEquals(true, boxes[0][0].isStartOf(new ClueID(DOWN_CLUES, 0)));
        assertEquals(false, boxes[0][3].isStartOf(new ClueID(ACROSS_CLUES, 1)));

        assertEquals(boxes[0][0].getSolution(), 'C');
        assertEquals(boxes[5][14].getSolution(), 'Y');
        assertEquals(boxes[14][14].getSolution(), 'S');
        assertEquals(boxes[14][5], null);
        assertEquals(boxes[3][6].getSolution(), 'N');

        assertTrue(boxes[2][2].isBarredTop());
        assertFalse(boxes[3][2].isBarredTop());
        assertTrue(boxes[5][2].isBarredRight());
        assertFalse(boxes[5][2].isBarredLeft());
        assertTrue(boxes[6][2].isBarredBottom());
        assertFalse(boxes[6][2].isBarredLeft());
        assertTrue(boxes[7][2].isBarredLeft());
        assertFalse(boxes[7][2].isBarredRight());

        ClueList acrossClues = puz.getClues(ACROSS_CLUES);
        ClueList downClues = puz.getClues(DOWN_CLUES);

        assertEquals(
            acrossClues.getClueByNumber("1").getHint(),
            "Baby bovine (4)"
        );
        assertEquals(
            acrossClues.getClueByNumber("5").getHint(),
            "At the drop of __ (4)"
        );
        assertEquals(
            acrossClues.getClueByNumber("13/18").getHint(),
            "Ice cream-and-cookies brand (4)"
        );
        assertEquals(
            acrossClues.getClueByNumber("23").getHint(),
            "Stat start"
        );
        assertFalse(acrossClues.getClueByNumber("18").hasZone());
        assertEquals(
            downClues.getClueByNumber("6").getHint(),
            "Schmooze, as with the A-list (6)"
        );
        assertEquals(
            downClues.getClueByNumber("7").getHint(),
            "Work like __ (4)"
        );
    }

    public void testJPZ() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamUtils.copyStream(getTestPuzzle1InputStream(), baos);
        Puzzle puz = JPZIO.readPuzzle(
            new ByteArrayInputStream(baos.toByteArray())
        );
        assertIsTestPuzzle1(puz);
    }
}
