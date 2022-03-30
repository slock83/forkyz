
package app.crossword.yourealwaysbe.puz;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import app.crossword.yourealwaysbe.util.CollectionUtils;

class MutableClueList implements ClueList {
    private NavigableMap<String, Clue> clueMap = new TreeMap<>(
        new Comparator<String>() {
            @Override
            public int compare(String num1, String num2) {
                if (num1 == null && num2 == null)
                    return 0;
                if (num1 == null)
                    return -1;
                if (num2 == null)
                    return 1;

                // compare as integers if both integers
                try {
                    return Integer.valueOf(num1)
                        .compareTo(Integer.valueOf(num2));
                } catch (NumberFormatException e) {
                    return num1.compareTo(num2);
                }
            }
        }
    );
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
    public String getFirstClueNumber() {
        return clueMap.firstEntry().getKey();
    }

    @Override
    public String getLastClueNumber() {
        return clueMap.lastEntry().getKey();
    }

    @Override
    public String getNextClueNumber(String number, boolean wrap) {
        String next = clueMap.higherKey(number);
        if (next == null)
            return wrap ? clueMap.firstEntry().getKey() : null;
        else
            return next;
    }

    @Override
    public String getPreviousClueNumber(String number, boolean wrap) {
        String previous = clueMap.lowerKey(number);
        if (previous == null)
            return wrap ? clueMap.lastEntry().getKey() : null;
        else
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

}
