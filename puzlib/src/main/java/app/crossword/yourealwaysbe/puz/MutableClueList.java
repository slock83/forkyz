
package app.crossword.yourealwaysbe.puz;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class MutableClueList implements ClueList {
    List<Clue> clues = new ArrayList<>();
    Map<String, Clue> numberedClueMap = new HashMap<>();

    /**
     * Add a clue to the list
     *
     * Clues must be added in contiguous index order.
     */
    public void addClue(Clue clue) {
        int index = clue.getClueID().getIndex();
        if (index < size()) {
            throw new IllegalArgumentException(
                "Clue has same index as existing clue: " + clue
            );
        }
        if (index > size()) {
            throw new IllegalArgumentException(
                "Clue index leaves gaps in clue list: " + clue
            );
        }

        clues.add(clue);
        if (clue.hasClueNumber()) {
            numberedClueMap.put(clue.getClueNumber(), clue);
        }
    }

    @Override
    public Iterator<Clue> iterator() {
        return clues.iterator();
    }

    @Override
    public Clue getClueByIndex(int index) {
        if (0 <= index && index < size())
            return clues.get(index);
        return null;
    }

    @Override
    public Clue getClueByNumber(String number) {
        return numberedClueMap.get(number);
    }

    @Override
    public Collection<Clue> getClues() {
        return clues;
    }

    @Override
    public boolean hasClueByNumber(String number) {
        return numberedClueMap.containsKey(number);
    }

    @Override
    public boolean hasClueByIndex(int index) {
        return 0 <= index && index < size();
    }

    @Override
    public int size() {
        return clues.size();
    }

    @Override
    public int getFirstZonedIndex() {
        return findClueIndex(0, 1, false, true, false);
    }

    @Override
    public int getLastZonedIndex() {
        return findClueIndex(size() - 1, -1, false, true, false);
    }

    @Override
    public int getNextZonedIndex(int startIndex, boolean wrap) {
        return findClueIndex(startIndex + 1, 1, false, true, wrap);
    }

    @Override
    public int getPreviousZonedIndex(int startIndex, boolean wrap) {
        return findClueIndex(startIndex - 1, -1, false, true, wrap);
    }

    @Override
    public int getClueIndex(String number) {
        Clue clue = getClueByNumber(number);
        return (clue ==  null) ? -1 : clue.getClueID().getIndex();
    }

    @Override
    public int hashCode() {
        return clues.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MutableClueList))
            return false;
        MutableClueList other = (MutableClueList) o;
        return getClues().equals(other.getClues());
    }

    @Override
    public String toString() {
        return clues.toString();
    }

    /**
     * Search for next clue index
     *
     * Starts at startIndex (inclusive, possibly wrapped), moves by
     * delta. Can specify if clue is required to have a number or zone
     * and whether to wrap around list.
     *
     * Returns -1 if nothing found.
     */
    private int findClueIndex(
        int startIndex, int delta,
        boolean hasNumber, boolean hasZone, boolean wrap
    ) {
        int len = size();

        if (len == 0)
            return -1;

        startIndex = wrapIf(startIndex, len, wrap);

        if (startIndex < 0 || startIndex >= len)
            return -1;

        int index = startIndex;

        do {
            Clue clue = getClueByIndex(index);
            boolean good = (!hasNumber || clue.hasClueNumber())
                && (!hasZone || clue.hasZone());
            if (good)
                return index;
            index = wrapIf(index + delta, len, wrap);
        } while (index != startIndex && 0 <= index && index < len);

        return -1;
    }

    /**
     * Wrap value to 0..base-1 if wrap is true
     */
    private int wrapIf(int val, int base, boolean wrap) {
        return wrap ? ((val % base) + base) % base : val;
    }
}
