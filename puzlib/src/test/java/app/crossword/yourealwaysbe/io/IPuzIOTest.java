package app.crossword.yourealwaysbe.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueList;
import app.crossword.yourealwaysbe.puz.Note;
import app.crossword.yourealwaysbe.puz.Puzzle.ClueNumDir;
import app.crossword.yourealwaysbe.puz.Puzzle.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;

public class IPuzIOTest extends TestCase {

    public IPuzIOTest(String testName) {
        super(testName);
    }

    public static InputStream getTestPuzzle1InputStream() {
        return IPuzIOTest.class.getResourceAsStream("/test.ipuz");
    }

    public static InputStream getTestPuzzle2InputStream() {
        return IPuzIOTest.class.getResourceAsStream("/barred-test.ipuz");
    }

    public static InputStream getTestPuzzleExtrasInputStream() {
        return IPuzIOTest.class.getResourceAsStream("/extras.ipuz");
    }

    public static void assertIsTestPuzzle1(Puzzle puz) throws Exception {
        assertEquals(puz.getTitle(), "Test &amp; puzzle");
        assertEquals(puz.getAuthor(), "Test author");
        assertEquals(puz.getCopyright(), "Test copyright");
        assertEquals(puz.getSourceUrl(), "https://testurl.com");
        assertEquals(puz.getSource(), "Test publisher");
        assertEquals(puz.getDate(), LocalDate.of(2003,2,1));

        assertEquals(puz.getWidth(), 3);
        assertEquals(puz.getHeight(), 2);

        Box[][] boxes = puz.getBoxes();

        assertEquals(boxes[0][0].getClueNumber(), 1);
        assertEquals(boxes[0][1].getClueNumber(), 2);
        assertFalse(boxes[0][1].isCircled());
        assertEquals(boxes[0][2], null);
        assertEquals(boxes[1][0].getClueNumber(), 3);
        assertEquals(boxes[1][0].getResponse(), 'A');
        assertTrue(boxes[1][0].isCircled());

        assertTrue(boxes[0][0].isBlank());
        assertEquals(boxes[0][1].getResponse(), 'B');
        assertEquals(boxes[1][1].getResponse(), 'C');
        assertTrue(boxes[1][2].isBlank());

        assertEquals(boxes[0][0].getSolution(), 'A');
        assertEquals(boxes[0][1].getSolution(), 'B');
        assertEquals(boxes[1][0].getSolution(), 'A');
        assertEquals(boxes[1][1].getSolution(), 'C');
        assertEquals(boxes[1][2].getSolution(), 'D');

        ClueList acrossClues = puz.getClues(true);
        ClueList downClues = puz.getClues(false);

        assertEquals(acrossClues.getClue(1).getHint(), "Test clue 1");
        assertEquals(acrossClues.getClue(3).getHint(), "Test clue 2 (clues 3/2)");
        assertEquals(downClues.getClue(1).getHint(), "Test clue 3");
        assertEquals(
            downClues.getClue(2).getHint(),
            "Test clue 4 (cont. 1 Across/1 Down) "
                + "(ref. 1&2 Across) (clues 2/1/3) (3-2-1)"
        );
    }

    public static void assertIsTestPuzzle2(Puzzle puz) throws Exception {
        Box[][] boxes = puz.getBoxes();

        assertTrue(boxes[1][1].isBarredTop());
        assertFalse(boxes[0][2].isBarredBottom());
        assertTrue(boxes[3][4].isBarredLeft());
        assertFalse(boxes[3][4].isBarredRight());

        assertEquals(boxes[8][3].getSolution(), 'V');
        assertEquals(boxes[10][1].getSolution(), 'R');
        assertEquals(boxes[1][10].getSolution(), 'W');

        assertTrue(boxes[1][2].isCircled());
        assertFalse(boxes[2][1].isCircled());

        assertEquals(boxes[0][7].getPartOfAcrossClueNumber(), 5);
        assertEquals(boxes[1][7].getPartOfDownClueNumber(), 6);

        ClueList acrossClues = puz.getClues(true);
        ClueList downClues = puz.getClues(false);

        assertEquals(acrossClues.getClue(5).getHint(), "Clue 5");
        assertEquals(downClues.getClue(2).getHint(), "Clue 2d");
    }

    public static void assertIsTestPuzzleExtras(Puzzle puz) throws Exception {
        Box[][] boxes = puz.getBoxes();

        assertEquals(boxes[0][6].getClueNumber(), 0);
        assertEquals(boxes[0][8].getClueNumber(), 5);
        assertEquals(boxes[6][0].getClueNumber(), 0);
        assertEquals(boxes[7][0].getClueNumber(), 25);

        assertFalse(boxes[2][0].hasColor());
        assertFalse(boxes[8][10].hasColor());
        assertTrue(boxes[6][0].hasColor());
        assertTrue(boxes[10][6].hasColor());
        int grey = Integer.valueOf("DCDCDC", 16);
        assertEquals(boxes[6][0].getColor(), grey);
        assertEquals(boxes[10][6].getColor(), grey);

        Set<String> extraLists = puz.getExtraClueListNames();
        assertEquals(extraLists.size(), 1);
        assertTrue(extraLists.contains("OddOnes"));

        List<Clue> oddClues = puz.getExtraClues("OddOnes");

        assertEquals(oddClues.get(5).getHint(), "Odd sixth");
        assertEquals(oddClues.get(0).getHint(), "Odd first");
    }


    /**
     * Test HTML in various parts of puzzle
     */
    public static InputStream getTestPuzzleHTMLInputStream() {
        return JPZIOTest.class.getResourceAsStream("/html.ipuz");
    }

    public static void assertIsTestPuzzleHTML(Puzzle puz) throws Exception {
        assertEquals(puz.getTitle(), "<b>Test</b> &amp; puzzle<br>For testing");
        assertEquals(
            puz.getAuthor(), "Test author<br><b>For<sup>Test</sup></b>"
        );
        assertEquals(
            puz.getSource(), "Test &nbsp;&nbsp;publisher<br>test<i>test</i>"
        );

        ClueList acrossClues = puz.getClues(true);

        assertEquals(
            acrossClues.getClue(1).getHint(),
            "Test <b>clue</b> 1<br>A clue&excl;"
        );
    }

    public void testIPuz() throws Exception {
        try (InputStream is = getTestPuzzle1InputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);
            assertIsTestPuzzle1(puz);
        }
    }

    public void testIPuzWriteRead() throws Exception {
        try (InputStream is = getTestPuzzle1InputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IPuzIO.writePuzzle(puz, baos);
            baos.close();

            ByteArrayInputStream bais
                = new ByteArrayInputStream(baos.toByteArray());

            Puzzle puz2 = IPuzIO.readPuzzle(bais);

            assertEquals(puz, puz2);
        }
    }

    public void testIPuzReadPlayWriteRead() throws Exception {
        try (InputStream is = getTestPuzzle1InputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);

            puz.setSupportUrl("http://test.url");
            puz.setTime(1234L);
            puz.setPosition(new Position(2, 1));
            puz.setAcross(false);

            puz.updateHistory(3, true);
            puz.updateHistory(1, false);

            puz.setNote(
                1,
                new Note("test1", "test2", "test3", "test4"),
                true
            );
            puz.setNote(
                2,
                new Note("test5", "test6\nnew line", "test7", "test8"),
                false
            );
            puz.flagClue(3, true, true);
            puz.flagClue(1, false, true);

            puz.setPlayerNote(
                new Note("scratch", "a note", "anagsrc", "anagsol")
            );

            Box[][] boxes = puz.getBoxes();

            boxes[0][1].setResponse('X');
            boxes[1][2].setResponse('Y');
            boxes[0][1].setResponder("Test");
            boxes[1][0].setCheated(true);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IPuzIO.writePuzzle(puz, baos);
            baos.close();

            ByteArrayInputStream bais
                = new ByteArrayInputStream(baos.toByteArray());

            Puzzle puz2 = IPuzIO.readPuzzle(bais);

            Box[][] boxes2 = puz2.getBoxes();

            assertEquals(puz2.getSupportUrl(), "http://test.url");
            assertEquals(puz2.getTime(), 1234L);
            assertEquals(puz.getPosition(), puz2.getPosition());
            assertFalse(puz.getAcross());
            assertEquals(puz.getHistory().get(0), new ClueNumDir(1, false));
            assertEquals(puz.getHistory().get(1), new ClueNumDir(3, true));
            assertEquals(puz.getNote(1, true).getText(), "test2");
            assertEquals(puz.getNote(2, false).getText(), "test6\nnew line");
            assertEquals(puz.getNote(2, false).getAnagramSource(), "test7");
            assertEquals(boxes2[0][1].getResponse(), 'X');
            assertEquals(boxes2[1][2].getResponse(), 'Y');
            assertEquals(boxes2[0][1].getResponder(), "Test");
            assertFalse(boxes2[0][1].isCheated());
            assertTrue(boxes2[1][0].isCheated());
            assertTrue(puz.isFlagged(1, false));
            assertTrue(puz.isFlagged(3, true));
            assertFalse(puz.isFlagged(1, true));

            assertEquals(puz, puz2);
        }
    }

    public void testIPuzHTML() throws Exception {
        try (InputStream is = getTestPuzzleHTMLInputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);
            assertIsTestPuzzleHTML(puz);
        }
    }

    public void testIPuzWriteReadHTML() throws Exception {
        try (InputStream is = getTestPuzzleHTMLInputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IPuzIO.writePuzzle(puz, baos);
            baos.close();

            ByteArrayInputStream bais
                = new ByteArrayInputStream(baos.toByteArray());

            Puzzle puz2 = IPuzIO.readPuzzle(bais);

            assertEquals(puz, puz2);
        }
    }

    public void testIPuzBarred() throws Exception {
        try (InputStream is = getTestPuzzle2InputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);
            assertIsTestPuzzle2(puz);
        }
    }

    public void testIPuzReadPlayWriteReadBarred() throws Exception {
        try (InputStream is = getTestPuzzle2InputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);

            puz.setSupportUrl("http://test.url");
            puz.setTime(1234L);
            puz.setPosition(new Position(1, 2));
            puz.setAcross(false);

            puz.updateHistory(3, false);
            puz.updateHistory(1, true);

            puz.setNote(
                1,
                new Note("test1", "test2", "test3", "test4"),
                true
            );
            puz.setNote(
                2,
                new Note("test5", "test6\nnew line", "test7", "test8"),
                false
            );

            Box[][] boxes = puz.getBoxes();

            boxes[0][1].setResponse('X');
            boxes[1][2].setResponse('Y');
            boxes[0][1].setResponder("Test");
            boxes[1][0].setCheated(true);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IPuzIO.writePuzzle(puz, baos);
            baos.close();

            ByteArrayInputStream bais
                = new ByteArrayInputStream(baos.toByteArray());

            Puzzle puz2 = IPuzIO.readPuzzle(bais);

            Box[][] boxes2 = puz2.getBoxes();

            assertEquals(puz2.getSupportUrl(), "http://test.url");
            assertEquals(puz2.getTime(), 1234L);
            assertEquals(puz.getPosition(), puz2.getPosition());
            assertFalse(puz.getAcross());
            assertEquals(puz.getHistory().get(0), new ClueNumDir(1, true));
            assertEquals(puz.getHistory().get(1), new ClueNumDir(3, false));
            assertEquals(puz.getNote(1, true).getText(), "test2");
            assertEquals(puz.getNote(2, false).getText(), "test6\nnew line");
            assertEquals(puz.getNote(2, false).getAnagramSource(), "test7");
            assertEquals(boxes2[0][1].getResponse(), 'X');
            assertEquals(boxes2[1][2].getResponse(), 'Y');
            assertEquals(boxes2[0][1].getResponder(), "Test");
            assertFalse(boxes2[0][1].isCheated());
            assertTrue(boxes2[1][0].isCheated());

            assertEquals(puz, puz2);
        }
    }

    public void testIPuzExtras() throws Exception {
        try (InputStream is = getTestPuzzleExtrasInputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);
            assertIsTestPuzzleExtras(puz);
        }
    }

    public void testIPuzWriteReadExtras() throws Exception {
        try (InputStream is = getTestPuzzleExtrasInputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IPuzIO.writePuzzle(puz, baos);
            baos.close();

            ByteArrayInputStream bais
                = new ByteArrayInputStream(baos.toByteArray());

            Puzzle puz2 = IPuzIO.readPuzzle(bais);

            assertEquals(puz, puz2);
        }
    }
}

