
package app.crossword.yourealwaysbe.puz;

import java.io.Serializable;
import java.util.Arrays;

public class Position implements Serializable {
    private int row;
    private int col;

    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }

    public void setRow(int row) { this.row = row; }
    public void setCol(int col) { this.col = col; }

    @Override
    public boolean equals(Object o) {
        if ((o == null) || (o.getClass() != this.getClass())) {
            return false;
        }

        Position p = (Position) o;

        return ((p.col == this.col) && (p.row == this.row));
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new int[] {row, col});
    }

    @Override
    public String toString() {
        return "(" + this.row+ ", " + this.col + ")";
    }
}

