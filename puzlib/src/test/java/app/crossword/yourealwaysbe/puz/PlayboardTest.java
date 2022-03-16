/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package app.crossword.yourealwaysbe.puz;

import java.io.DataInputStream;

import junit.framework.TestCase;

import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.io.IOTest;
import app.crossword.yourealwaysbe.puz.Puzzle.Position;

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
         Puzzle puz = IO.loadNative(new DataInputStream(IOTest.class.getResourceAsStream("/test.puz")));

         Playboard board = new Playboard(puz);
         board.setHighlightLetter(new Position(5, 5));
         System.out.println("ON: "+board.getBoxes()[board.getHighlightLetter().getRow()][board.getHighlightLetter().getCol()].getSolution());
         board.moveUp(false);


         System.out.println("ON: "+board.getBoxes()[board.getHighlightLetter().getRow()][board.getHighlightLetter().getCol()].getSolution());
         assertEquals(4, board.getHighlightLetter().getRow());
         board.moveUp(false);


         System.out.println("ON: "+board.getBoxes()[board.getHighlightLetter().getRow()][board.getHighlightLetter().getCol()].getSolution());
         assertEquals(3, board.getHighlightLetter().getRow());
         board.moveUp(false);

         System.out.println("ON: "+board.getBoxes()[board.getHighlightLetter().getRow()][board.getHighlightLetter().getCol()].getSolution());
         assertEquals(2, board.getHighlightLetter().getRow());
         board.moveUp(false);

         System.out.println("ON: "+board.getBoxes()[board.getHighlightLetter().getRow()][board.getHighlightLetter().getCol()].getSolution());
         assertEquals(2, board.getHighlightLetter().getRow());
         board.moveUp(false);

         System.out.println("ON: "+board.getBoxes()[board.getHighlightLetter().getRow()][board.getHighlightLetter().getCol()].getSolution());
         assertEquals(2, board.getHighlightLetter().getRow());
         board.moveUp(false);


         System.out.println("----------");
         board.setHighlightLetter(new Position(4,4));

         System.out.println("ON: "+board.getBoxes()[board.getHighlightLetter().getRow()][board.getHighlightLetter().getCol()].getSolution());
         assertEquals(4, board.getHighlightLetter().getRow());
         board.moveUp(false);

         System.out.println("ON: "+board.getBoxes()[board.getHighlightLetter().getRow()][board.getHighlightLetter().getCol()].getSolution());
         assertEquals(2, board.getHighlightLetter().getRow());
         board.moveUp(false);

         System.out.println("ON: "+board.getBoxes()[board.getHighlightLetter().getRow()][board.getHighlightLetter().getCol()].getSolution());
         assertEquals(1, board.getHighlightLetter().getRow());
         board.moveUp(false);

         System.out.println("ON: "+board.getBoxes()[board.getHighlightLetter().getRow()][board.getHighlightLetter().getCol()].getSolution());
         assertEquals(0, board.getHighlightLetter().getRow());
         board.moveUp(false);

         System.out.println("ON: "+board.getBoxes()[board.getHighlightLetter().getRow()][board.getHighlightLetter().getCol()].getSolution());
         assertEquals(0, board.getHighlightLetter().getRow());
         board.moveUp(false);

         System.out.println("ON: "+board.getBoxes()[board.getHighlightLetter().getRow()][board.getHighlightLetter().getCol()].getSolution());
         assertEquals(0, board.getHighlightLetter().getRow());
         board.moveUp(false);



    }

}
