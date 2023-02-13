
package app.crossword.yourealwaysbe.io;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.ClueList;
import app.crossword.yourealwaysbe.puz.Puzzle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KeesingXMLIOTest {

    public static InputStream getTestPuzzle1InputStream() {
        return KeesingXMLIOTest.class.getResourceAsStream("/keesing.xml");
    }

    public static void assertIsTestPuzzle1(Puzzle puz) {
        String ACROSS_CLUES = "Across";
        String DOWN_CLUES = "Down";

        assertEquals("Test Crossword", puz.getTitle());

        Box[][] boxes = puz.getBoxes();

        assertEquals(10, boxes.length);
        assertEquals(11, boxes[0].length);
        assertEquals("1", boxes[0][0].getClueNumber());
        assertTrue(boxes[0][0].isStartOf(new ClueID(ACROSS_CLUES, 0)));
        assertTrue(boxes[0][0].isStartOf(new ClueID(DOWN_CLUES, 0)));
        assertTrue(boxes[0][6].isStartOf(new ClueID(DOWN_CLUES, 3)));
        assertFalse(boxes[0][2].isStartOf(new ClueID(DOWN_CLUES, 3)));

        assertEquals(boxes[0][0].getSolution(), "T");
        assertEquals(boxes[2][7].getSolution(), "Y");
        assertEquals(boxes[9][9].getSolution(), "S");
        assertEquals(boxes[3][6].getSolution(), "N");
        assertNull(boxes[0][5]);
        assertNull(boxes[4][2]);

        ClueList acrossClues = puz.getClues(ACROSS_CLUES);
        ClueList downClues = puz.getClues(DOWN_CLUES);

        assertEquals(
            acrossClues.getClueByNumber("1").getHint(),
            "Test clue 1"
        );
        assertEquals(
            acrossClues.getClueByNumber("13").getHint(),
            "Test clue 13"
        );
        assertEquals(
            downClues.getClueByNumber("26").getHint(),
            "Test clue 26"
        );
        assertEquals(
            downClues.getClueByNumber("14").getHint(),
            "Test clue 14"
        );
    }

    @Test
    public void testKeesingXML() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamUtils.copyStream(getTestPuzzle1InputStream(), baos);
        Puzzle puz = KeesingXMLIO.readPuzzle(
            new ByteArrayInputStream(baos.toByteArray())
        );
        assertIsTestPuzzle1(puz);
    }
}
