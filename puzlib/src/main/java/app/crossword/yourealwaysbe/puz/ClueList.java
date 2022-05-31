
package app.crossword.yourealwaysbe.puz;

import java.util.Collection;

public interface ClueList extends Iterable<Clue> {
    /**
     * Get clue by clue number (not index)
     */
    public Clue getClue(String number);

    /**
     * Get clue with number using index
     */
    public Clue getUnnumberedClue(int index);

    /**
     * How many clues without numbers
     */
    public int sizeUnnumbered();

    /**
     * Get clues, iterator will go in clue order
     */
    public Collection<Clue> getClues();

    public boolean hasClue(String number);

    public int size();

    /**
     * Get first clue in list
     *
     * Specify if must have zone
     *
     * @return null if no clue
     */
    public String getFirstClueNumber(boolean hasZone);

    /**
     * Get first clue in list
     *
     * Specify if must have zone.
     *
     * @return null if no clue
     */
    public String getLastClueNumber(boolean hasZone);

    /**
     * Get the next clue after the given clue number
     *
     * Wraps back to beginning or returns null if no next number
     */
    public String getNextClueNumber(
        String number, boolean hasZone, boolean wrap
    );

    /**
     * Get the clue before the given clue number
     *
     * Wraps back to end or null if no previous
     */
    public String getPreviousClueNumber(
        String number, boolean hasZone, boolean wrap
    );

    /**
     * Returns index of clue in clue list
     */
    public int getClueIndex(String number);
}
