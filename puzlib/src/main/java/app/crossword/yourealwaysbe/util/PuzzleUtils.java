
package app.crossword.yourealwaysbe.util;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueList;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.Zone;

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

    /**
     * Return first across list name found or null
     */
    public static String getAcrossListName(Puzzle puz) {
        for (String listName : puz.getClueListNames()) {
            ClueList list = puz.getClues(listName);
            if (isAcrossList(puz, list))
                return listName;
        }
        return null;
    }

    /**
     * Returns first across list found or null
     */
    public static ClueList getAcrossList(Puzzle puz) {
        String list = getAcrossListName(puz);
        if (list == null)
            return null;
        else
            return puz.getClues(list);
    }

    /**
     * Check all clues follow "across rules"
     *
     * I.e. start at the right number, then go right until the box is no
     * longer joined to the right.
     */
    public static boolean isAcrossList(Puzzle puz, ClueList clues) {
        for (Clue clue : clues) {
            if (!clue.hasZone())
                return false;

            Zone zone = clue.getZone();
            if (zone.isEmpty())
                return false;

            Position start = zone.getPosition(0);
            Box box = puz.checkedGetBox(start);

            if (box == null)
                return false;
            if (!box.hasClueNumber())
                return false;
            if (!box.getClueNumber().equals(clue.getClueNumber()))
                return false;

            for (int i = 1; i < zone.size(); i++) {
                Position prev = zone.getPosition(i - 1);
                Position cur = zone.getPosition(i);

                if (prev.getRow() != cur.getRow())
                    return false;
                if (prev.getCol() + 1 != cur.getCol())
                    return false;

                box = puz.checkedGetBox(cur);
                if (box == null)
                    return false;

                boolean joined = PuzzleUtils.joinedRight(
                    puz, prev.getRow(),  prev.getCol()
                );

                if (!joined)
                    return false;
            }

            Position end = zone.getPosition(zone.size() - 1);
            boolean joined = PuzzleUtils.joinedRight(
                puz, end.getRow(), end.getCol()
            );
            if (joined)
                return false;
        }
        return true;
    }

    /**
     * Return first down list name found or null
     */
    public static String getDownListName(Puzzle puz) {
        for (String listName : puz.getClueListNames()) {
            ClueList list = puz.getClues(listName);
            if (isDownList(puz, list))
                return listName;
        }
        return null;
    }

    /**
     * Returns first down list found or null
     */
    public static ClueList getDownList(Puzzle puz) {
        String list = getDownListName(puz);
        if (list == null)
            return null;
        else
            return puz.getClues(list);
    }

    public static boolean isDownList(Puzzle puz, ClueList clues) {
        for (Clue clue : clues) {
            if (!clue.hasZone())
                return false;

            Zone zone = clue.getZone();
            if (zone.isEmpty())
                return false;

            Position start = zone.getPosition(0);
            Box box = puz.checkedGetBox(start);

            if (box == null)
                return false;
            if (!box.hasClueNumber())
                return false;
            if (!box.getClueNumber().equals(clue.getClueNumber()))
                return false;

            for (int i = 1; i < zone.size(); i++) {
                Position prev = zone.getPosition(i - 1);
                Position cur = zone.getPosition(i);

                if (prev.getRow() + 1 != cur.getRow())
                    return false;
                if (prev.getCol() != cur.getCol())
                    return false;

                box = puz.checkedGetBox(cur);
                if (box == null)
                    return false;

                boolean joined = PuzzleUtils.joinedBottom(
                    puz, prev.getRow(),  prev.getCol()
                );

                if (!joined)
                    return false;
            }

            Position end = zone.getPosition(zone.size() - 1);
            boolean joined = PuzzleUtils.joinedBottom(
                puz, end.getRow(), end.getCol()
            );
            if (joined)
                return false;
        }
        return true;
    }

    /**
     * True if any clue has a non-empty zone
     */
    public static boolean isZonesList(ClueList clues) {
        for (Clue clue : clues) {
            if (clue.hasZone() && !clue.getZone().isEmpty())
                return true;
        }
        return false;
    }
}
