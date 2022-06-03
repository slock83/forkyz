
package app.crossword.yourealwaysbe.puz;

import java.util.Collection;

public interface ClueList extends Iterable<Clue> {
    public Clue getClueByIndex(int index);

    public Clue getClueByNumber(String number);

    /**
     * Get clues, iterator will go in clue order
     */
    public Collection<Clue> getClues();

    public boolean hasClueByNumber(String number);

    public boolean hasClueByIndex(int index);

    public int size();

    /**
     * Index of first clue with a zone, or -1
     */
    public int getFirstZonedIndex();

    /**
     * Index of last clue with a zone, or -1
     */
    public int getLastZonedIndex();

    /**
     * First index after startIndex with zone
     *
     * Non-inclusive, specify whether to wrap around
     */
    public int getNextZonedIndex(int startIndex, boolean wrap);

    /**
     * First index before startIndex with zone
     *
     * Non-inclusive, specify whether to wrap around
     */
    public int getPreviousZonedIndex(int startIndex, boolean wrap);

    /**
     * Returns index of clue in clue list
     */
    public int getClueIndex(String number);
}
