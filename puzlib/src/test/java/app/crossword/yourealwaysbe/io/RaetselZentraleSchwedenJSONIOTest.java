
package app.crossword.yourealwaysbe.io;

import java.io.InputStream;

import junit.framework.TestCase;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueList;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.Zone;

public class RaetselZentraleSchwedenJSONIOTest extends TestCase {

    public RaetselZentraleSchwedenJSONIOTest(String testName) {
        super(testName);
    }

    public static InputStream getTestPuzzle1InputStream() {
        return JPZIOTest.class.getResourceAsStream("/raetselzentrale.json");
    }

    public static void assertIsTestPuzzle1(Puzzle puz) throws Exception {
        assertEquals(puz.getTitle(), "Test Puzzle");

        assertEquals(puz.getWidth(), 16);
        assertEquals(puz.getHeight(), 18);

        Box[][] boxes = puz.getBoxes();

        assertNull(boxes[0][0]);
        assertEquals(boxes[0][4].getClueNumber(), "1");
        assertEquals(boxes[0][7].getClueNumber(), "2");
        assertEquals(boxes[4][13].getClueNumber(), "19");

        assertEquals(boxes[0][4].getSolution(), "A");
        assertEquals(boxes[4][6].getSolution(), "I");

        assertEquals(boxes[1][4].getMarks()[2][2], "2");
        assertEquals(boxes[5][5].getMarks()[2][2], "9");

        assertNull(boxes[17][3]);
        assertFalse(boxes[17][4].hasClueNumber());
        assertEquals(boxes[17][4].getMarks()[2][2], "1");
        assertEquals(boxes[17][12].getMarks()[2][2], "9");
        assertNull(boxes[17][13]);
        assertEquals(boxes[17][6].getSolution(), "B");

        ClueList acrossClues = puz.getClues("Hinüber");
        ClueList downClues = puz.getClues("Hinunter");

        assertEquals(acrossClues.getClueByNumber("6").getHint(), "Test clue one");
        assertEquals(
            acrossClues.getClueByNumber("10").getHint(), "Test clue 10"
        );
        assertEquals(downClues.getClueByNumber("1").getHint(), "Test clue 1d");
        assertEquals(downClues.getClueByNumber("2").getHint(), "Test clue 2d");

        Clue sixAcross = acrossClues.getClueByNumber("6");
        Zone sixAcrossZone = sixAcross.getZone();
        assertEquals(sixAcrossZone.getPosition(0), new Position(1, 0));
        assertEquals(sixAcrossZone.getPosition(4), new Position(1, 4));
        assertEquals(sixAcrossZone.size(), 5);

        Clue anAcross = acrossClues.getClueByNumber("14");
        Zone anAcrossZone = anAcross.getZone();
        assertEquals(anAcrossZone.size(), 7);
    }

    public void testPuzzle1() throws Exception {
        try (InputStream is = getTestPuzzle1InputStream()) {
            Puzzle puz = RaetselZentraleSchwedenJSONIO.readPuzzle(is);
            assertIsTestPuzzle1(puz);
        }
    }
}

