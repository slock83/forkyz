
package app.crossword.yourealwaysbe.io;

import java.io.InputStream;
import java.time.LocalDate;

import junit.framework.TestCase;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.ClueList;
import app.crossword.yourealwaysbe.puz.Puzzle;

public class AmuseLabsJSONIOTest extends TestCase {

    public AmuseLabsJSONIOTest(String testName) {
        super(testName);
    }

    public static InputStream getTestPuzzle1InputStream() {
        return JPZIOTest.class.getResourceAsStream("/amuselabs.json");
    }

    public static void assertIsTestPuzzle1(Puzzle puz) throws Exception {
        assertEquals(puz.getTitle(), "Test Amuse Labs");
        assertEquals(puz.getAuthor(), "Test Author");
        assertEquals(puz.getCopyright(), "Test Copyright");
        assertEquals(puz.getDate(), LocalDate.of(2021,8,4));

        assertEquals(puz.getWidth(), 15);
        assertEquals(puz.getHeight(), 15);

        Box[][] boxes = puz.getBoxes();

        assertEquals(boxes[0][0].getClueNumber(), "1");
        assertEquals(boxes[0][1].getClueNumber(), "2");
        assertEquals(boxes[0][4], null);
        assertEquals(boxes[5][5].getClueNumber(), "28");
        assertEquals(boxes[5][7], null);

        assertEquals(boxes[0][0].getSolution(), 'A');
        assertEquals(boxes[5][3].getSolution(), 'B');

        assertTrue(boxes[10][3].isCircled());
        assertTrue(boxes[7][6].isCircled());
        assertFalse(boxes[3][7].isCircled());
        assertFalse(boxes[5][9].isCircled());

        ClueList acrossClues = puz.getClues(ClueID.ACROSS);
        ClueList downClues = puz.getClues(ClueID.DOWN);

        assertEquals(acrossClues.getClue("1").getHint(), "Clue 1a");
        assertEquals(acrossClues.getClue("21").getHint(), "Clue 21a");
        assertEquals(downClues.getClue("1").getHint(), "Clue 1d");
        assertEquals(downClues.getClue("2").getHint(), "Clue 2d");
    }

    public void testPuzzle1() throws Exception {
        try (InputStream is = getTestPuzzle1InputStream()) {
            Puzzle puz = AmuseLabsJSONIO.readPuzzle(is);
            assertIsTestPuzzle1(puz);
        }
    }
}

