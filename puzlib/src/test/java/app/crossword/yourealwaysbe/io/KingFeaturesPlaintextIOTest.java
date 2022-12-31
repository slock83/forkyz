package app.crossword.yourealwaysbe.io;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.ClueList;
import app.crossword.yourealwaysbe.puz.Puzzle;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class KingFeaturesPlaintextIOTest {

    public static InputStream getTestPuzzle1InputStream() {
        return KingFeaturesPlaintextIOTest.class.getResourceAsStream(
            "/premiere-20100704.txt"
        );
    }

    public static void assertIsTestPuzzle1(Puzzle puz) {
        Box[][] boxes = puz.getBoxes();

        assertEquals(21, boxes.length);
        assertEquals(21, boxes[0].length);
        assertEquals("1", boxes[0][0].getClueNumber());
        assertEquals(true, boxes[0][0].isStartOf(new ClueID("Across", 0)));
        assertEquals(true, boxes[0][0].isStartOf(new ClueID("Down", 0)));
        assertEquals(false, boxes[0][3].isStartOf(new ClueID("Across", 1)));

        assertEquals(boxes[0][0].getSolution(), "F");
        assertEquals(boxes[5][14].getSolution(), "E");
        assertEquals(boxes[14][14].getSolution(), "E");
        assertEquals(boxes[14][5].getSolution(), "R");
        assertEquals(boxes[1][7], null);

        ClueList acrossClues = puz.getClues("Across");
        ClueList downClues = puz.getClues("Down");

        assertEquals(
            acrossClues.getClueByNumber("1").getHint(),
            "Murals on plaster"
        );
        assertEquals(
            acrossClues.getClueByNumber("8").getHint(),
            "Glucose-level regulator"
        );
        assertEquals(
            acrossClues.getClueByNumber("23").getHint(),
            "Cocky retort to a bully"
        );
        assertEquals(
            downClues.getClueByNumber("5").getHint(),
            "One preserving fruit, e.g."
        );
        assertEquals(
            downClues.getClueByNumber("7").getHint(),
            "In stitches"
        );
        assertEquals(
            downClues.getClueByNumber("14").getHint(),
            "Napoleonic marshal Michel"
        );
    }

    @Test
    public void testRead() throws IOException {
        try (InputStream is = getTestPuzzle1InputStream()) {
            Puzzle puz = KingFeaturesPlaintextIO.parsePuzzle(is);
            assertIsTestPuzzle1(puz);
        }
    }
}
