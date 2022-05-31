
package app.crossword.yourealwaysbe.puz;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

import app.crossword.yourealwaysbe.util.ClueNumberComparator;
import app.crossword.yourealwaysbe.util.CollectionUtils;

class MutableClueList implements ClueList {
    private NavigableMap<String, Clue> clueMap
        = new TreeMap<>(new ClueNumberComparator());
    private List<Clue> unnumberedClues = new ArrayList<>();

    // access through invalidateIndexCache and getIndexCache
    private Map<String, Integer> indexMap;

    public void addClue(Clue clue) {
        if (clue.hasClueNumber()) {
            clueMap.put(clue.getClueNumber(), clue);
            invalidateIndexCache();
        } else {
            unnumberedClues.add(clue);
        }
    }

    @Override
    public Iterator<Clue> iterator() {
        return CollectionUtils.join(
            clueMap.values(), unnumberedClues
        ).iterator();
    }

    @Override
    public Clue getClue(String number) {
        return clueMap.get(number);
    }

    @Override
    public Collection<Clue> getClues() {
        return CollectionUtils.join(
            clueMap.values(),
            unnumberedClues
        );
    }

    @Override
    public boolean hasClue(String number) {
        return clueMap.containsKey(number);
    }

    @Override
    public int size() {
        return clueMap.size() + unnumberedClues.size();
    }

    @Override
    public String getFirstClueNumber(boolean hasZone) {
        if (clueMap.isEmpty())
            return null;
        String number = clueMap.firstEntry().getKey();
        if (hasZone)
            number = findClueNumberWithZone(number, true, false);
        return number;
    }

    @Override
    public String getLastClueNumber(boolean hasZone) {
        if (clueMap.isEmpty())
            return null;
        String number = clueMap.lastEntry().getKey();
        if (hasZone)
            number = findClueNumberWithZone(number, false, false);
        return number;
    }

    @Override
    public String getNextClueNumber(
        String number, boolean hasZone, boolean wrap
    ) {
        String next = clueMap.higherKey(number);
        if (next == null)
            next = wrap ? clueMap.firstEntry().getKey() : null;
        if (hasZone)
            next = findClueNumberWithZone(next, true, wrap);
        return next;
    }

    @Override
    public String getPreviousClueNumber(
        String number, boolean hasZone, boolean wrap
    ) {
        String previous = clueMap.lowerKey(number);
        if (previous == null)
            previous = wrap ? clueMap.lastEntry().getKey() : null;
        if (hasZone)
            previous = findClueNumberWithZone(previous, false, wrap);
        return previous;
    }

    @Override
    public int getClueIndex(String number) {
        Integer idx = getIndexCache().get(number);
        return (idx == null) ? -1 : idx;
    }

    @Override
    public Clue getUnnumberedClue(int index) {
        if (index < 0 || index >= unnumberedClues.size())
            return null;
        return unnumberedClues.get(index);
    }

    @Override
    public int sizeUnnumbered() {
        return unnumberedClues.size();
    }

    @Override
    public int hashCode() {
        return clueMap.hashCode() + unnumberedClues.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MutableClueList))
            return false;

        MutableClueList other = (MutableClueList) o;

        return clueMap.equals(other.clueMap)
            && unnumberedClues.equals(other.unnumberedClues);
    }

    @Override
    public String toString() {
        return clueMap.toString()
            + " / " + unnumberedClues.toString();
    }

    private void invalidateIndexCache() {
        indexMap = null;
    }

    private Map<String, Integer> getIndexCache() {
        if (indexMap == null) {
            indexMap = new HashMap<>();
            int idx = 0;
            for (Clue clue : getClues()) {
                indexMap.put(clue.getClueNumber(), idx);
                idx += 1;
            }
        }
        return indexMap;
    }

    /**
     * Find clue with zone (inclusive)
     *
     * Starting with startNumber, find a clue that has a (non-empty) zone.
     * Search forwards or backwards, choose if wrap.
     *
     * @return null if none to jump to
     */
    private String findClueNumberWithZone(
        String startNumber, boolean forwards, boolean wrap
    ) {
        String nextNumber = startNumber;
        Clue nextClue = getClue(startNumber);
        while (!nextClue.hasZone()) {
            nextNumber = forwards
                ? getNextClueNumber(nextNumber, false, wrap)
                : getPreviousClueNumber(nextNumber, false, wrap);

            if (Objects.equals(startNumber, nextNumber))
                return null;

            nextClue = getClue(nextNumber);
        }
        return nextClue.getClueNumber();
    }
}
