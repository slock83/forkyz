
package app.crossword.yourealwaysbe.puz;

import java.util.Objects;

/**
 * Identifies a clue on the board
 */
public class ClueID implements Comparable<ClueID> {
    public static final String ACROSS = "Across";
    public static final String DOWN = "Down";

    private String clueNumber;
    private String listName;

    public ClueID(String clueNumber, String listName) {
        this.clueNumber = clueNumber;
        this.listName = listName;
    }

    public boolean hasClueNumber() { return getClueNumber() != null; }
    public String getClueNumber() { return clueNumber; }
    public String getListName() { return listName; }

    // TODO: will become deprecated when generic lists are fully
    // supported
    public boolean isAcross() {
        return ACROSS.equals(listName);
    }

    // TODO: will become deprecated when generic lists are fully
    // supported
    public boolean isDown() {
        return DOWN.equals(listName);
    }

    public boolean equals(Object o) {
        if (o instanceof ClueID) {
            ClueID other = (ClueID) o;
            return Objects.equals(clueNumber, other.clueNumber)
                && Objects.equals(listName, other.listName);
        }
        return false;
    }

    @Override
    public int compareTo(ClueID other) {
        // this feels overbuilt...

        if (other == null)
            return 1;

        String num = getClueNumber();
        String otherNum = other.getClueNumber();

        if (num == null && otherNum != null)
            return -1;
        if (otherNum == null)
            return 1;

        // try comparing as numbers first
        int numberCompare = 0;
        try {
            numberCompare
                = Integer.valueOf(num).compareTo(Integer.valueOf(otherNum));
        } catch (NumberFormatException e) {
            numberCompare = num.compareTo(otherNum);
        }

        if (numberCompare != 0)
            return numberCompare;

        String list = getListName();
        String otherList = other.getListName();

        if (list == null && otherList != null)
            return -1;
        else if (list != null && otherList == null)
            return 1;
        else
            return list.compareTo(otherList);
    }

    public int hashCode() {
        return Objects.hash(clueNumber, listName);
    }

    public String toString() {
        return clueNumber + "(" + listName + ")";
    }
}

