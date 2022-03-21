
package app.crossword.yourealwaysbe.puz;

import java.io.Serializable;
import java.util.Objects;

public class Clue implements Serializable {
    public static final String ACROSS = "Across";
    public static final String DOWN = "Down";

    // to support non-standard clue lists without numbers
    private static final int NO_NUMBER = -1;

    private int number = NO_NUMBER;
    private String listName;
    private String hint;

    /**
     * Construct a clue
     *
     * Use listName Clue.ACROSS/DOWN for standard directions, else
     * direction can be custom
     */
    public Clue(int number, String listName, String hint) {
        this.number = number;
        this.listName = listName;
        this.hint = hint;
    }

    /**
     * Construct a numberless clue
     */
    public Clue(String listName, String hint) {
        this(NO_NUMBER, listName, hint);
    }

    public boolean hasNumber() { return getNumber() != NO_NUMBER; }
    public int getNumber() { return number; }
    public String getListName() { return listName; }
    public boolean isAcross() { return ACROSS.equals(listName); }
    public boolean isDown() { return DOWN.equals(listName); }
    public String getHint() { return hint; }

    public void setListName(String listName) {
        this.listName = listName;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Clue))
            return false;

        final Clue other = (Clue) obj;

        if (this.getNumber() != other.getNumber())
            return false;

        if (!Objects.equals(this.getListName(), other.getListName()))
            return false;

        if (!Objects.equals(this.getHint(), other.getHint()))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getNumber(), getHint());
    }

    @Override
    public String toString() {
        return getNumber() + ":" + getListName() + ". " + getHint();
    }
}


