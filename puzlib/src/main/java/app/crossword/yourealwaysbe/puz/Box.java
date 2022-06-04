package app.crossword.yourealwaysbe.puz;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Box implements Serializable {
    public static final char BLANK = ' ';
    private static final int NOCOLOR = -1;

    private String responder;
    private boolean cheated;
    private boolean circled;
    private char response = BLANK;
    private char solution;
    private String clueNumber;
    // for each clue this box is a part of, the index of the cell it is
    // the clue word
    private Map<ClueID, Integer> cluePositions = new HashMap<>();

    private boolean barTop = false;
    private boolean barBottom = false;
    private boolean barLeft = false;
    private boolean barRight = false;
    // 3x3 grid of small text marks
    private String[][] marks = null;

    // 24-bit representation 0x00rrggbb
    private int color = NOCOLOR;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        Box other = (Box) obj;

        if (!cluePositions.equals(other.cluePositions)) {
            return false;
        }

        if (isCheated() != other.isCheated()) {
            return false;
        }

        if (!Objects.equals(getClueNumber(), other.getClueNumber())) {
            return false;
        }

        if (isCircled() != other.isCircled()) {
            return false;
        }

        if (getResponder() == null) {
            if (other.getResponder() != null) {
                return false;
            }
        } else if (!responder.equals(other.responder)) {
            return false;
        }

        if (getResponse() != other.getResponse()) {
            return false;
        }

        if (getSolution() != other.getSolution()) {
            return false;
        }

        if (isBarredTop() != other.isBarredTop())
            return false;

        if (isBarredBottom() != other.isBarredBottom())
            return false;

        if (isBarredLeft() != other.isBarredLeft())
            return false;

        if (isBarredRight() != other.isBarredRight())
            return false;

        if (getColor() != other.getColor())
            return false;

        // Annoying Arrays.equals doesn't do arrays of arrays..
        String[][] marks = getMarks();
        String[][] otherMarks = other.getMarks();
        if (marks != null || otherMarks != null) {
            if (marks == null || otherMarks == null)
                return false;
            if (marks.length != otherMarks.length)
                return false;
            for (int row = 0; row < marks.length; row++) {
                if (!Arrays.equals(marks[row], otherMarks[row]))
                    return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + cluePositions.hashCode();
        result = (prime * result) + (isCheated() ? 1231 : 1237);
        result = (prime * result) + Objects.hash(getClueNumber());
        result = (prime * result) + (isCircled() ? 1231 : 1237);
        result = (prime * result) + (isBarredTop() ? 1231 : 1237);
        result = (prime * result) + (isBarredBottom() ? 1231 : 1237);
        result = (prime * result) + (isBarredLeft() ? 1231 : 1237);
        result = (prime * result) + (isBarredRight() ? 1231 : 1237);
        result = (prime * result) +
            ((getResponder() == null) ? 0 : getResponder().hashCode());
        result = (prime * result) + getResponse();
        result = (prime * result) + getSolution();
        result = (prime * result) + getColor();
        // ignore marks, too awkward and probably empty

        return result;
    }

    @Override
    public String toString() {
        String number = getClueNumber();
        if (number != null)
            return number + getSolution() + " ";
        else
            return getSolution() + " ";
    }

    /**
     * @param responder the responder to set
     */
    public void setResponder(String responder) {
        this.responder = responder;
    }

    /**
     * @return if start of clue in list name with box number
     */
    public boolean isStartOf(ClueID clueID) {
        Integer position = cluePositions.get(clueID);
        return position != null && position == 0;
    }

    /**
     * @return the cheated
     */
    public boolean isCheated() {
        return cheated;
    }

    /**
     * @param cheated the cheated to set
     */
    public void setCheated(boolean cheated) {
        this.cheated = cheated;
    }

    /**
     * @return if the box is circled
     */
    public boolean isCircled() {
	return circled;
    }

    /**
     * @param circled the circled to set
     */
    public void setCircled(boolean circled) {
	this.circled = circled;
    }

    /**
     * @return the response
     */
    public char getResponse() {
        return response;
    }

    /**
     * @param response the response to set
     */
    public void setResponse(char response) {
        this.response = response;
    }

    /**
     * True if box has solution (i.e. not '\0')
     */
    public boolean hasSolution() {
        return getSolution() != '\0';
    }

    /**
     * @return the solution
     */
    public char getSolution() {
        return solution;
    }

    /**
     * @param solution the solution to set
     */
    public void setSolution(char solution) {
        this.solution = solution;
    }

    /**
     * True if there is a clue number in the box
     */
    public boolean hasClueNumber() {
        return getClueNumber() != null;
    }

    /**
     * @return the clueNumber, or null for no clue
     */
    public String getClueNumber() {
        return clueNumber;
    }

    /**
     * @param clueNumber the clueNumber to set
     */
    public void setClueNumber(String clueNumber) {
        this.clueNumber = clueNumber;
    }

    /**
     * @return the responder
     */
    public String getResponder() {
        return responder;
    }

    /**
     * @return if the current box is blank
     */
    public boolean isBlank() { return getResponse() == BLANK; }

    public void setBlank() { setResponse(BLANK); }

    /**
     * @returns true if box is part of the clue
     */
    public boolean isPartOf(ClueID clueId) {
        return cluePositions.containsKey(clueId);
    }

    public boolean isPartOf(Clue clue) {
        if (clue == null)
            return false;
        return isPartOf(clue.getClueID());
    }

    /**
     * The clue ids that have this box in their zones
     */
    public Set<ClueID> getIsPartOfClues() {
        return cluePositions.keySet();
    }

    /**
     * Get a clue that this box is part of from the specified list
     *
     * If there are more than one clues from the same list, either will
     * be returned.
     *
     * Null returned if no clue
     */
    public ClueID getIsPartOfClue(String listName) {
        if (listName == null)
            return null;

        for (ClueID cid : getIsPartOfClues()) {
            if (listName.equals(cid.getListName()))
                return cid;
        }

        return null;
    }

    /**
     * @param position if part of a clue, the position in the
     * word
     */
    public void setCluePosition(ClueID clueId, int position) {
        cluePositions.put(clueId, position);
    }

    /**
     * Get position of box in clue
     *
     * @return postion or -1 if not in clue
     */
    public int getCluePosition(ClueID clueId) {
        Integer pos = cluePositions.get(clueId);
        return (pos == null) ? -1 : pos;
    }

    public boolean isBarredTop() { return barTop; }
    public boolean isBarredBottom() { return barBottom; }
    public boolean isBarredLeft() { return barLeft; }
    public boolean isBarredRight() { return barRight; }

    /**
     * 3x3 array of text marks to put in box, can have null entries
     */
    public String[][] getMarks() { return marks; }
    public boolean hasMarks() { return marks != null; }

    /**
     * 3x3 array of text marks to put in box
     *
     * row x col, can have null entries
     */
    public void setMarks(String[][] marks) {
        if (marks != null) {
            if (marks.length != 3) {
                throw new IllegalArgumentException("Marks array must be 3x3.");
            }
            for (int row = 0; row < marks.length; row++) {
                if (marks[row] == null || marks[row].length != 3) {
                    throw new IllegalArgumentException(
                        "Marks array must be 3x3."
                    );
                }
            }
        }
        this.marks = marks;
    }

    /**
     * True if box has any bars
     */
    public boolean isBarred() {
        return isBarredTop() || isBarredBottom()
            || isBarredLeft() || isBarredRight();
    }

    public void setBarredTop(boolean barTop) {
        this.barTop = barTop;
    }

    public void setBarredBottom(boolean barBottom) {
        this.barBottom = barBottom;
    }

    public void setBarredLeft(boolean barLeft) {
        this.barLeft = barLeft;
    }

    public void setBarredRight(boolean barRight) {
        this.barRight = barRight;
    }

    public boolean hasColor() { return color != NOCOLOR; }

    /**
     * 24-bit 0x00rrggbb when has color
     */
    public int getColor() { return color; }

    /**
     * Set as 24-bit 0x00rrggbb
     */
    public void setColor(int color) {
        this.color = color;
    }
}
