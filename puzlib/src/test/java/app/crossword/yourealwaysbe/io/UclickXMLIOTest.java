package app.crossword.yourealwaysbe.io;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.ClueList;
import app.crossword.yourealwaysbe.puz.Puzzle;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UclickXMLIOTest {
    private static final String TITLE = "12/15/09 LET'S BE HONEST";
    private static final String AUTHOR = "by Billie Truitt, edited by Stanley Newman";

    public static InputStream getTestPuzzle1InputStream() {
        return UclickXMLIOTest.class.getResourceAsStream(
            "/crnet091215-data.xml"
        );
    }

    public static void assertIsTestPuzzle1(Puzzle puz) {
        assertEquals(TITLE, puz.getTitle());
        assertEquals(AUTHOR, puz.getAuthor());

        Box[][] boxes = puz.getBoxes();

        assertEquals(15, boxes.length);
        assertEquals(15, boxes[0].length);
        assertEquals("1", boxes[0][0].getClueNumber());
        assertEquals(true, boxes[0][0].isStartOf(new ClueID("Across", 0)));
        assertEquals(true, boxes[0][0].isStartOf(new ClueID("Down", 0)));
        assertEquals(false, boxes[0][3].isStartOf(new ClueID("Across", 1)));

        assertEquals(boxes[0][0].getSolution(), "G");
        assertEquals(boxes[5][14], null);
        assertEquals(boxes[14][14].getSolution(), "S");
        assertEquals(boxes[14][5].getSolution(), "L");
        assertEquals(boxes[3][6].getSolution(), "N");


        ClueList acrossClues = puz.getClues("Across");
        ClueList downClues = puz.getClues("Down");

        assertEquals(
            acrossClues.getClueByNumber("1").getHint(),
            "Film legend Greta"
        );
        assertEquals(
            acrossClues.getClueByNumber("50").getHint(),
            "Distress signal"
        );
        assertEquals(
            downClues.getClueByNumber("5").getHint(),
            "Rampaging"
        );
        assertEquals(
            downClues.getClueByNumber("6").getHint(),
            "Get even for"
        );
        assertEquals(
            downClues.getClueByNumber("7").getHint(),
            "Nickname for an NCO"
        );
        assertEquals(
            downClues.getClueByNumber("13").getHint(),
            "Covered with rocks"
        );
    }

    @Test
    public void testRead() throws IOException {
        try (InputStream is = getTestPuzzle1InputStream()) {
            Puzzle puz = UclickXMLIO.parsePuzzle(is);
            assertIsTestPuzzle1(puz);
        }
    }
}
