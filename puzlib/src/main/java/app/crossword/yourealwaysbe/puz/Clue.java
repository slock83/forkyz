
package app.crossword.yourealwaysbe.puz;

import java.io.Serializable;
import java.util.Objects;

/**
 * A clue on the board
 *
 * Must have a listName and index, number is optional.
 */
public class Clue implements Serializable {
    private ClueID clueID;
    // because sometimes numbers are alphabetic
    private String number;
    private String hint;
    private Zone zone;

    public Clue(
        String listName, int index, String number, String hint, Zone zone
    ) {
        this.clueID = new ClueID(listName, index);
        this.number = number;
        if (listName == null || index < 0) {
            throw new IllegalArgumentException(
                "Clues must have a list name and index in the list"
            );
        }
        this.hint = hint;
        this.zone = (zone == null) ? new Zone() : zone;
    }

    /**
     * Construct a numberless clue with no zone
     */
    public Clue(String listName, int index, String hint) {
        this(listName, index, null, hint, null);
    }

    public ClueID getClueID() { return clueID; }
    public boolean hasClueNumber() { return number != null; }
    public String getClueNumber() { return number; }
    public String getHint() { return hint; }
    public boolean hasZone() { return zone.size() != 0; }
    public Zone getZone() { return zone; }

    @Override
    public String toString() {
        return getClueID() + " / " + getClueNumber() + " / "  + getHint();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (!(o instanceof Clue))
            return false;

        Clue other = (Clue) o;

        return getClueID().equals(other.getClueID())
            && Objects.equals(getClueNumber(), other.getClueNumber())
            && Objects.equals(getHint(), other.getHint())
            && Objects.equals(getZone(), other.getZone());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            getClueID(), getClueNumber(), getHint(), getZone()
        );
    }
}


