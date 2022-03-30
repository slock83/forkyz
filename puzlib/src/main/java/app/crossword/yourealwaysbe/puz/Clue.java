
package app.crossword.yourealwaysbe.puz;

import java.io.Serializable;

public class Clue extends ClueID implements Serializable {
    // because sometimes numbers are alphabetic
    private String hint;
    private Zone zone;

    /**
     * Construct a clue
     *
     * Use listName Clue.ACROSS/DOWN for standard directions, else
     * direction can be custom
     */
    public Clue(
        String number, String listName, String hint, Zone zone
    ) {
        super(number, listName);
        this.hint = hint;
        this.zone = (zone == null) ? new Zone() : zone;
    }

    /**
     * Construct a numberless clue with no zone
     */
    public Clue(String listName, String hint) {
        this(null, listName, hint, null);
    }

    public String getHint() { return hint; }
    public boolean hasZone() { return zone.size() != 0; }
    public Zone getZone() { return zone; }

    @Override
    public String toString() {
        return super.toString() + " " + getHint();
    }
}


