
package app.crossword.yourealwaysbe.puz;

import java.io.Serializable;
import java.util.Objects;

public class Clue implements Serializable {
    // to support non-standard clue lists without numbers
    private static final int NO_NUMBER = -1;

    private int number = NO_NUMBER;
    private boolean isAcross;
    private String hint;

    public Clue(int number, boolean isAcross, String hint) {
        this.number = number;
        this.isAcross = isAcross;
        this.hint = hint;
    }

    public Clue(boolean isAcross, String hint) {
        this.number = number;
        this.isAcross = isAcross;
        this.hint = hint;
    }

    public boolean hasNumber() { return getNumber() != NO_NUMBER; }
    public int getNumber() { return number; }
    public boolean getIsAcross() { return isAcross; }
    public String getHint() { return hint; }

    public void setIsAcross(boolean isAcross) {
        this.isAcross = isAcross;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Clue))
            return false;

        final Clue other = (Clue) obj;

        if (this.getNumber() != other.getNumber())
            return false;

        if (this.getIsAcross() != other.getIsAcross())
            return false;

        if (!Objects.equals(this.getHint(), other.getHint()))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return getNumber();
    }

    @Override
    public String toString() {
        String dir = getIsAcross() ? "a" : "d";
        return getNumber() + dir + ". " + getHint();
    }
}


