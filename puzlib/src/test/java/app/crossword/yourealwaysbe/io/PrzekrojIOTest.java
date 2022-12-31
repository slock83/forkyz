
package app.crossword.yourealwaysbe.io;

import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueList;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.PuzImage;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.Zone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class PrzekrojIOTest {

    public static InputStream getTestPuzzle1InputStream() {
        return JPZIOTest.class.getResourceAsStream("/przekroj.json");
    }

    public static void assertIsTestPuzzle1(Puzzle puz) throws Exception {
        assertEquals(puz.getWidth(), 19);
        assertEquals(puz.getHeight(), 17);

        Box[][] boxes = puz.getBoxes();

        assertNull(boxes[0][0]);
        assertEquals(boxes[0][5].getClueNumber(), "1");
        assertEquals(boxes[1][6], null);
        assertEquals(boxes[6][12].getClueNumber(), "17");

        assertEquals(boxes[0][5].getSolution(), "A");
        assertEquals(boxes[5][7].getSolution(), "B");

        ClueList acrossClues = puz.getClues("Poziomo");
        ClueList downClues = puz.getClues("Pionowo");

        assertEquals(acrossClues.getClueByNumber("1").getHint(), "Test clue 1");
        assertEquals(
            acrossClues.getClueByNumber("10").getHint(),
            "Test clue 10 żó"
        );
        assertEquals(downClues.getClueByNumber("1").getHint(), "Test clue 1d");
        assertEquals(downClues.getClueByNumber("2").getHint(), "Test clue 2d");

        Clue clue = acrossClues.getClueByNumber("6");
        Zone clueZone = clue.getZone();
        assertEquals(clueZone.getPosition(0), new Position(2, 7));
        assertEquals(clueZone.getPosition(4), new Position(2, 11));

        List<PuzImage> images = puz.getImages();
        assertEquals(
            images.get(0),
            new PuzImage("myimage.jpg", 0, 2, 3, 3)
        );
    }

    @Test
    public void testPuzzle1() throws Exception {
        try (InputStream is = getTestPuzzle1InputStream()) {
            Puzzle puz = PrzekrojIO.readPuzzle(is);
            assertIsTestPuzzle1(puz);
        }
    }
}

