/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package app.crossword.yourealwaysbe.puz;

import java.io.DataInputStream;
import java.io.IOException;

import junit.framework.TestCase;

import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.io.IOTest;

/**
 *
 * @author kebernet
 */
public class PlayboardTest extends TestCase {

    public PlayboardTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testMoveUp() throws Exception {
         Puzzle puz = loadTestPuz();

         Playboard board = new Playboard(puz);
         moveToPosition(board, 5, 5);
         board.moveUp(false);

         assertAtRow(board, 4);
         board.moveUp(false);

         assertAtRow(board, 3);
         board.moveUp(false);

         assertAtRow(board, 2);
         board.moveUp(false);

         assertAtRow(board, 2);
         board.moveUp(false);

         assertAtRow(board, 2);
         board.moveUp(false);

         moveToPosition(board, 4, 4);

         assertAtRow(board, 4);
         board.moveUp(false);

         assertAtRow(board, 2);
         board.moveUp(false);

         assertAtRow(board, 1);
         board.moveUp(false);

         assertAtRow(board, 0);
         board.moveUp(false);

         assertAtRow(board, 0);
         board.moveUp(false);

         assertAtRow(board, 0);
         board.moveUp(false);
    }

    public void testDeleteLetter() throws Exception {
         Puzzle puz = loadTestPuz();

         Playboard board = new Playboard(puz);
         board.setDontDeleteCrossing(false);

         // Create
         //
         //  A
         // ABCDE
         //    A
         //
         // then delete from E back, row should be empty

         moveToPosition(board, 1, 0);
         board.playLetter('A');
         board.playLetter('B');
         board.playLetter('C');
         board.playLetter('D');
         board.playLetter('E');

         moveToPosition(board, 0, 1);
         board.playLetter('A');

         moveToPosition(board, 2, 3);
         board.playLetter('A');

         moveToPosition(board, 1, 4);
         for (int i = 0; i < 5; i++)
             board.deleteLetter();

         assertBoxBlank(puz, 1, 0);
         assertBoxBlank(puz, 1, 1);
         assertBoxBlank(puz, 1, 2);
         assertBoxBlank(puz, 1, 3);
         assertBoxBlank(puz, 1, 4);
         assertBoxLetter(puz, 0, 1, 'A');
         assertBoxLetter(puz, 2, 3, 'A');
    }

    public void testDeleteLetterCrossing() throws Exception {
         Puzzle puz = loadTestPuz();

         Playboard board = new Playboard(puz);
         board.setDontDeleteCrossing(true);

         // Create
         //
         //  A
         // ABCDE
         //    A
         //
         // then delete from E back, B and D should remain

         moveToPosition(board, 1, 0);
         board.playLetter('A');
         board.playLetter('B');
         board.playLetter('C');
         board.playLetter('D');
         board.playLetter('E');

         moveToPosition(board, 0, 1);
         board.playLetter('A');

         moveToPosition(board, 2, 3);
         board.playLetter('A');

         moveToPosition(board, 1, 4);
         for (int i = 0; i < 5; i++)
             board.deleteLetter();

         assertBoxBlank(puz, 1, 0);
         assertBoxLetter(puz, 1, 1, 'B');
         assertBoxBlank(puz, 1, 2);
         assertBoxLetter(puz, 1, 3, 'D');
         assertBoxBlank(puz, 1, 4);
    }

    public void testMoveNextOnAxis() throws Exception {
        Puzzle puz = loadTestPuz();
        Playboard board = new Playboard(puz);
        board.setMovementStrategy(MovementStrategy.MOVE_NEXT_ON_AXIS);

        // Across
        for (int i = 0; i < 4; i++)
            board.playLetter('A');
        assertPosition(board, 0, 4);
        board.playLetter('A');
        assertPosition(board, 0, 6);
        for (int i = 0; i < 10; i++)
            board.playLetter('A');
        assertPosition(board, 0, 14);

        // Down
        moveToPosition(board, 0, 6);
        board.toggleSelection();

        for (int i = 0; i < 3; i++)
            board.playLetter('A');
        assertPosition(board, 3, 6);
        board.playLetter('A');
        assertPosition(board, 5, 6);
        for (int i = 0; i < 10; i++)
            board.playLetter('A');
        assertPosition(board, 14, 6);

        // Down / Back
        moveToPosition(board, 5, 6);
        board.deleteLetter();
        board.deleteLetter();
        assertPosition(board, 3, 6);
        for (int i = 0; i < 5; i++)
            board.deleteLetter();

        // Across / Back
        moveToPosition(board, 0, 6);
        board.toggleSelection();
        board.deleteLetter();
        assertPosition(board, 0, 4);
        for (int i = 0; i < 6; i++)
            board.deleteLetter();
        assertPosition(board, 0, 0);
    }

    public void testMoveStopEnd() throws Exception {
        Puzzle puz = loadTestPuz();
        Playboard board = new Playboard(puz);
        board.setMovementStrategy(MovementStrategy.STOP_ON_END);

        // Across
        for (int i = 0; i < 4; i++)
            board.playLetter('A');
        assertPosition(board, 0, 4);
        board.playLetter('A');
        assertPosition(board, 0, 4);

        // Down
        moveToPosition(board, 0, 6);
        board.toggleSelection();

        for (int i = 0; i < 3; i++)
            board.playLetter('A');
        assertPosition(board, 3, 6);
        board.playLetter('A');
        assertPosition(board, 3, 6);

        // Down / Back
        moveToPosition(board, 5, 6);
        board.deleteLetter();
        board.deleteLetter();
        assertPosition(board, 5, 6);

        // Across / Back
        moveToPosition(board, 0, 6);
        board.toggleSelection();
        board.deleteLetter();
        assertPosition(board, 0, 6);
    }

    public void testMoveNextClue() throws Exception {
        Puzzle puz = loadTestPuz();
        Playboard board = new Playboard(puz);
        board.setMovementStrategy(MovementStrategy.MOVE_NEXT_CLUE);

        // Across
        for (int i = 0; i < 4; i++)
            board.playLetter('A');
        assertPosition(board, 0, 4);
        board.playLetter('A');
        assertPosition(board, 0, 6);
        for (int i = 0; i < 8; i++)
            board.playLetter('A');
        assertPosition(board, 1, 0);

        // Down
        moveToPosition(board, 0, 6);
        board.toggleSelection();

        for (int i = 0; i < 3; i++)
            board.playLetter('A');
        assertPosition(board, 3, 6);
        board.playLetter('A');
        assertPosition(board, 0, 7);

        // Down / Back
        board.deleteLetter();
        board.deleteLetter();
        assertPosition(board, 3, 6);

        // Across / Back
        moveToPosition(board, 0, 6);
        board.toggleSelection();
        board.deleteLetter();
        board.deleteLetter();
        assertPosition(board, 0, 4);
        for (int i = 0; i < 5; i++)
            board.deleteLetter();
        // wrap to down clues
        assertPosition(board, 14, 10);
        for (int i = 0; i < 3; i++)
            board.deleteLetter();
        assertPosition(board, 14, 14);

        // Wrap across
        board.toggleSelection();
        board.playLetter('A');
        assertPosition(board, 0, 0);

        // Wrap down
        board.toggleSelection();
        board.deleteLetter();
        // wrap to across clues
        assertPosition(board, 14, 10);
        board.playLetter('A');
        assertPosition(board, 0, 0);
    }

    public void testMoveParallel() throws Exception {
        Puzzle puz = loadTestPuz();
        Playboard board = new Playboard(puz);
        board.setMovementStrategy(MovementStrategy.MOVE_PARALLEL_WORD);

        // Across
        for (int i = 0; i < 4; i++)
            board.playLetter('A');
        assertPosition(board, 0, 4);
        board.playLetter('A');
        assertPosition(board, 1, 0);
        for (int i = 0; i < 100; i++)
            board.playLetter('A');
        assertPosition(board, 14, 3);

        // Down
        moveToPosition(board, 0, 4);
        board.toggleSelection();

        for (int i = 0; i < 2; i++)
            board.playLetter('A');
        assertPosition(board, 2, 4);
        board.playLetter('A');
        assertPosition(board, 0, 6);
        for (int i = 0; i < 100; i++)
            board.playLetter('A');
        assertPosition(board, 3, 14);

        // Down / Back
        for (int i = 0; i < 5; i++)
            board.deleteLetter();
        assertPosition(board, 3, 13);
        for (int i = 0; i < 100; i++)
            board.deleteLetter();
        assertPosition(board, 0, 0);

        // Across / Back
        moveToPosition(board, 14, 3);
        board.toggleSelection();
        for (int i = 0; i < 5; i++)
            board.deleteLetter();
        assertPosition(board, 13, 3);
        for (int i = 0; i < 100; i++)
            board.deleteLetter();
        assertPosition(board, 0, 0);
    }

    public void testPlayFullMoveAxis() throws Exception {
        checkNoMoveFullGrid(MovementStrategy.MOVE_NEXT_ON_AXIS);
    }

    public void testPlayFullMoveStopEnd() throws Exception {
        checkNoMoveFullGrid(MovementStrategy.STOP_ON_END);
    }

    public void testPlayFullMoveNextClue() throws Exception {
        checkNoMoveFullGrid(MovementStrategy.MOVE_NEXT_CLUE);
    }

    public void testPlayFullMoveParallelWord() throws Exception {
        checkNoMoveFullGrid(MovementStrategy.MOVE_PARALLEL_WORD);
    }

    private void checkNoMoveFullGrid(
        MovementStrategy moveStrat
    ) throws Exception {
        // pick a box this far from the edges to test
        final int CHECK_OFFSET = 5;

        Puzzle puz = loadTestPuz();
        int width = puz.getWidth();
        int height = puz.getHeight();
        Box[][] boxes = puz.getBoxes();

        Playboard board = new Playboard(puz);
        board.setMovementStrategy(moveStrat);
        board.setSkipCompletedLetters(true);

        Position checkPos = null;

        // fill grid, find checkPos
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                Box box = boxes[row][col];
                if (box != null) {
                    if (
                        checkPos == null
                        && row > CHECK_OFFSET && col > CHECK_OFFSET
                    ) {
                        checkPos = new Position(row, col);
                        box.setBlank();
                    } else {
                        box.setResponse('A');
                    }
                }
            }
        }

        board.setHighlightLetter(checkPos);
        board.playLetter('A');

        assertEquals(checkPos, board.getHighlightLetter());
    }

    private void assertBoxBlank(Puzzle puz, int row, int col) throws Exception {
        assertTrue(puz.checkedGetBox(row, col).isBlank());
    }

    private void assertBoxLetter(
        Puzzle puz, int row, int col, char letter
    ) throws Exception {
        assertEquals(puz.checkedGetBox(row, col).getResponse(), letter);
    }

    private void assertPosition(
        Playboard board, int row, int col
    ) throws Exception {
        assertEquals(board.getHighlightLetter(), new Position(row, col));
    }

    private void assertAtRow(Playboard board, int row) throws Exception {
        assertEquals(board.getHighlightLetter().getRow(), row);
    }

    private void moveToPosition(Playboard board, int row, int col) {
        Position pos = new Position(row, col);
        if (!pos.equals(board.getHighlightLetter()))
            board.setHighlightLetter(pos);
    }

    private Puzzle loadTestPuz() throws IOException {
        return IO.loadNative(
            new DataInputStream(
                IOTest.class.getResourceAsStream("/test.puz")
            )
        );
    }
}
