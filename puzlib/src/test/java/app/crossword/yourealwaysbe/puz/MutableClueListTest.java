package app.crossword.yourealwaysbe.puz;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MutableClueListTest {

    @Test
    public void testEmptyList() {
        ClueList cl = new MutableClueList();
        assertNull(cl.getClueByIndex(1));
        assertNull(cl.getClueByNumber("1"));
        assertEquals(0, cl.getClues().size());
        assertEquals(-1, cl.getFirstZonedIndex());
        assertEquals(-1, cl.getLastZonedIndex());
        assertEquals(-1, cl.getClueIndex("1"));
        assertEquals(-1, cl.getNextZonedIndex(0, true));
        assertEquals(-1, cl.getPreviousZonedIndex(1, true));
    }

    @Test
    public void testSingletonList() {
        MutableClueList cl = new MutableClueList();

        Clue clue1 = new Clue(
            "TestList", 0, "1", "The first clue", new Zone()
        );
        cl.addClue(clue1);

        assertEquals(clue1, cl.getClueByIndex(0));
        assertNull(cl.getClueByIndex(1));
        assertEquals(clue1, cl.getClueByNumber("1"));
        assertNull(cl.getClueByNumber("2"));
        assertEquals(1, cl.getClues().size());
        assertEquals(-1, cl.getFirstZonedIndex());
        assertEquals(-1, cl.getLastZonedIndex());
        assertEquals(0, cl.getClueIndex("1"));
        assertEquals(-1, cl.getClueIndex("2"));
        assertEquals(-1, cl.getNextZonedIndex(0, false));
        assertEquals(-1, cl.getPreviousZonedIndex(0, false));
    }

    @Test
    public void testSingletonZonedList() {
        MutableClueList cl = new MutableClueList();

        Zone zone1 = new Zone();
        zone1.addPosition(new Position(0, 0));
        Clue clue1 = new Clue(
            "TestList", 0, "1", "The first clue", zone1
        );
        cl.addClue(clue1);

        assertEquals(0, cl.getFirstZonedIndex());
        assertEquals(0, cl.getLastZonedIndex());
        assertEquals(-1, cl.getNextZonedIndex(0, false));
        assertEquals(-1, cl.getPreviousZonedIndex(0, false));
        assertEquals(0, cl.getNextZonedIndex(0, true));
        assertEquals(0, cl.getPreviousZonedIndex(0, true));
    }


    @Test
    public void testMultiList() {
        MutableClueList cl = new MutableClueList();

        Clue clue1 = new Clue(
            "TestList", 0, "1", "The first clue", new Zone()
        );
        Clue clue2 = new Clue(
            "TestList", 1, "2", "The second clue", new Zone()
        );
        Clue clue3 = new Clue(
            "TestList", 2, "3", "The third clue", new Zone()
        );
        cl.addClue(clue1);
        cl.addClue(clue2);
        cl.addClue(clue3);

        assertEquals(clue2, cl.getClueByIndex(1));
        assertNull(cl.getClueByIndex(3));
        assertEquals(clue1, cl.getClueByNumber("1"));
        assertNull(cl.getClueByNumber("4"));
        assertEquals(3, cl.getClues().size());
        assertEquals(-1, cl.getFirstZonedIndex());
        assertEquals(-1, cl.getLastZonedIndex());
        assertEquals(1, cl.getClueIndex("2"));
        assertEquals(-1, cl.getClueIndex("0"));
        assertEquals(-1, cl.getNextZonedIndex(0, false));
        assertEquals(-1, cl.getPreviousZonedIndex(3, false));
    }

    @Test
    public void testHasZone() {
        MutableClueList cl = new MutableClueList();

        Clue clue1 = new Clue(
            "TestList", 0, "1", "The first clue", new Zone()
        );
        Zone zone2 = new Zone();
        zone2.addPosition(new Position(0, 0));
        Clue clue2 = new Clue(
            "TestList", 1, "2", "The second clue", zone2
        );
        Clue clue3 = new Clue(
            "TestList", 2, "3", "The third clue", new Zone()
        );
        cl.addClue(clue1);
        cl.addClue(clue2);
        cl.addClue(clue3);

        assertEquals(1, cl.getFirstZonedIndex());
        assertEquals(1, cl.getLastZonedIndex());
        assertEquals(1, cl.getNextZonedIndex(0, false));
        assertEquals(-1, cl.getNextZonedIndex(1, false));
        assertEquals(1, cl.getNextZonedIndex(1, true));
        assertEquals(1, cl.getPreviousZonedIndex(2, false));
        assertEquals(-1, cl.getPreviousZonedIndex(1, false));
        assertEquals(1, cl.getPreviousZonedIndex(1, true));
    }

    @Test
    public void testTwoZones() {
        MutableClueList cl = new MutableClueList();

        Clue clue1 = new Clue(
            "TestList", 0, "1", "The first clue", new Zone()
        );
        Zone zone2 = new Zone();
        zone2.addPosition(new Position(0, 0));
        Clue clue2 = new Clue(
            "TestList", 1, "2", "The second clue", zone2
        );
        Clue clue3 = new Clue(
            "TestList", 2, "3", "The third clue", zone2
        );
        cl.addClue(clue1);
        cl.addClue(clue2);
        cl.addClue(clue3);

        assertEquals(1, cl.getFirstZonedIndex());
        assertEquals(2, cl.getLastZonedIndex());
        assertEquals(1, cl.getNextZonedIndex(0, false));
        assertEquals(2, cl.getNextZonedIndex(1, false));
        assertEquals(1, cl.getPreviousZonedIndex(2, false));
        assertEquals(-1, cl.getPreviousZonedIndex(1, false));
        assertEquals(2, cl.getPreviousZonedIndex(1, true));
    }

    @Test
    public void testBadIndex() {
        boolean addedClue1 = false;
        boolean addedClue2 = false;
        try {
            MutableClueList cl = new MutableClueList();

            Clue clue1 = new Clue(
                "TestList", 0, "1", "The first clue", new Zone()
            );
            Clue clue2 = new Clue(
                "TestList", 2, "2", "The second clue", new Zone()
            );

            cl.addClue(clue1);
            addedClue1 = true;
            cl.addClue(clue2);
            addedClue2 = true;
        } catch (IllegalArgumentException e) {
            // do nothing
        }
        assertTrue(addedClue1);
        assertFalse(addedClue2);
    }

}

