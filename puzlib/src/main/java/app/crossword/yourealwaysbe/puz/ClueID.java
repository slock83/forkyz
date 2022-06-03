
package app.crossword.yourealwaysbe.puz;

import java.util.Objects;

public class ClueID implements Comparable<ClueID> {
    private String listName;
    private int index;

    public ClueID(String listName, int index) {
        this.listName = listName;
        this.index = index;
    }

    public String getListName() { return listName; }
    public int getIndex() { return index; }

    public boolean equals(Object o) {
        if (o instanceof ClueID) {
            ClueID other = (ClueID) o;
            return index == other.getIndex()
                && Objects.equals(listName, other.getListName());
        }
        return false;
    }

    @Override
    public int compareTo(ClueID other) {
        if (other == null)
            return 1;

        String list = getListName();
        String otherList = other.getListName();

        if (list == null && otherList != null)
            return -1;

        if (list != null && otherList == null)
            return 1;

        int listCompare = list.compareTo(otherList);

        if (listCompare != 0)
            return listCompare;

        return Integer.compare(getIndex(), other.getIndex());
    }

    public int hashCode() {
        return Objects.hash(getListName(), getIndex());
    }

    public String toString() {
        return getListName() + "[" + getIndex() + "]";
    }
}

