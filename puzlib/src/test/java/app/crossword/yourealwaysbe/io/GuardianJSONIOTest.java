
package app.crossword.yourealwaysbe.io;

import java.io.InputStream;
import java.time.LocalDate;

import junit.framework.TestCase;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueList;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.Zone;

public class GuardianJSONIOTest extends TestCase {

    public GuardianJSONIOTest(String testName) {
        super(testName);
    }

    public static InputStream getTestPuzzle1InputStream() {
        return JPZIOTest.class.getResourceAsStream("/guardian.json");
    }

    public static void assertIsTestPuzzle1(Puzzle puz) throws Exception {
        assertEquals(puz.getTitle(), "Test Crossword");
        assertEquals(puz.getAuthor(), "Test Author");
        assertEquals(puz.getDate(), LocalDate.of(2021,8,12));

        assertEquals(puz.getWidth(), 15);
        assertEquals(puz.getHeight(), 15);

        Box[][] boxes = puz.getBoxes();

        assertEquals(boxes[0][0].getClueNumber(), "1");
        assertEquals(boxes[0][2].getClueNumber(), "2");
        assertEquals(boxes[0][7], null);
        assertEquals(boxes[0][8].getClueNumber(), "5");

        assertEquals(boxes[0][0].getSolution(), "A");
        assertEquals(boxes[0][6].getSolution(), "B");

        ClueList acrossClues = puz.getClues("Across");
        ClueList downClues = puz.getClues("Down");

        assertEquals(acrossClues.getClueByNumber("1").getHint(), "Test clue 1");
        assertEquals(
            acrossClues.getClueByNumber("10").getHint(), "Test clue 10"
        );
        assertEquals(downClues.getClueByNumber("1").getHint(), "Test clue 1d");
        assertEquals(downClues.getClueByNumber("2").getHint(), "Test clue 2d");

        Clue fiveAcross = acrossClues.getClueByNumber("5, 10");
        Zone fiveAcrossZone = fiveAcross.getZone();
        assertEquals(fiveAcrossZone.getPosition(6), new Position(0, 14));
        assertEquals(fiveAcrossZone.getPosition(7), new Position(2, 6));

        Clue tenAcross = acrossClues.getClueByNumber("10");
        Zone tenAcrossZone = tenAcross.getZone();
        assertTrue(tenAcrossZone == null || tenAcrossZone.isEmpty());
    }

    public void testPuzzle1() throws Exception {
        try (InputStream is = getTestPuzzle1InputStream()) {
            Puzzle puz = GuardianJSONIO.readPuzzle(is);
            assertIsTestPuzzle1(puz);
        }
    }
}

