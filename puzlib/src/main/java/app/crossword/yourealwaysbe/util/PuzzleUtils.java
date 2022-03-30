
package app.crossword.yourealwaysbe.util;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Puzzle;

public class PuzzleUtils {

    public static boolean joinedTop(Puzzle puzzle, int row, int col) {
        Box boxCur = puzzle.checkedGetBox(row, col);
        Box boxAbove = puzzle.checkedGetBox(row - 1, col);

        if (boxAbove == null || boxCur == null)
            return false;

        return !(boxCur.isBarredTop() || boxAbove.isBarredBottom());
    }

    public static boolean joinedBottom(Puzzle puzzle, int row, int col) {
        Box boxCur = puzzle.checkedGetBox(row, col);
        Box boxBelow = puzzle.checkedGetBox(row + 1, col);

        if (boxBelow == null || boxCur == null)
            return false;

        return !(boxCur.isBarredBottom() || boxBelow.isBarredTop());
    }

    public static boolean joinedLeft(Puzzle puzzle, int row, int col) {
        Box boxCur = puzzle.checkedGetBox(row, col);
        Box boxLeft = puzzle.checkedGetBox(row, col - 1);

        if (boxLeft == null || boxCur == null)
            return false;

        return !(boxCur.isBarredLeft() || boxLeft.isBarredRight());
    }

    public static boolean joinedRight(Puzzle puzzle, int row, int col) {
        Box boxCur = puzzle.checkedGetBox(row, col);
        Box boxRight = puzzle.checkedGetBox(row, col + 1);

        if (boxRight == null || boxCur == null)
            return false;

        return !(boxCur.isBarredRight() || boxRight.isBarredLeft());
    }
}
