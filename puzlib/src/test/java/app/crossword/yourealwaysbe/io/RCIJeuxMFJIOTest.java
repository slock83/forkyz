
package app.crossword.yourealwaysbe.io;

import java.io.InputStream;

import org.junit.jupiter.api.Test;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueList;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.Zone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class RCIJeuxMFJIOTest {

    public static InputStream getTestPuzzle1InputStream() {
        return JPZIOTest.class.getResourceAsStream("/rcijeux-motsfleches.mfj");
    }

    public static void assertIsTestPuzzle1(Puzzle puz) throws Exception {
        assertEquals(puz.getTitle(), "Test Puzzle RCI");

        assertEquals(puz.getWidth(), 14);
        assertEquals(puz.getHeight(), 9);

        Box[][] boxes = puz.getBoxes();

        assertNull(boxes[0][0]);
        assertNull(boxes[6][10]);
        assertEquals(boxes[0][1].getSolution(), "A");
        assertEquals(boxes[2][4].getSolution(), "B");
        assertEquals(boxes[4][12].getSolution(), "C");

        ClueList acrossClues = puz.getClues("Horiz.");
        ClueList downClues = puz.getClues("Vert.");

        Clue[] clues = new Clue[] {
            downClues.getClueByNumber(boxes[0][1].getClueNumber()),
            acrossClues.getClueByNumber(boxes[1][0].getClueNumber()),
            acrossClues.getClueByNumber(boxes[2][3].getClueNumber()),
            downClues.getClueByNumber(boxes[3][2].getClueNumber()),
            acrossClues.getClueByNumber(boxes[7][10].getClueNumber())
        };

        for (int i = 0; i < clues.length; i++) {
            assertEquals(clues[i].getHint(), "Test clue " + (i + 1));
        }

        Zone zone3 = clues[2].getZone();
        assertEquals(zone3.size(), 6);
        assertEquals(zone3.getPosition(0), new Position(2, 3));
        assertEquals(zone3.getPosition(5), new Position(2, 8));

        Zone zone1 = clues[0].getZone();
        assertEquals(zone1.size(), 9);
        assertEquals(zone1.getPosition(0), new Position(0, 1));
        assertEquals(zone1.getPosition(8), new Position(8, 1));
    }

    @Test
    public void testPuzzle1() throws Exception {
        try (InputStream is = getTestPuzzle1InputStream()) {
            Puzzle puz = RCIJeuxMFJIO.readPuzzle(is);
            assertIsTestPuzzle1(puz);
        }
    }
}

