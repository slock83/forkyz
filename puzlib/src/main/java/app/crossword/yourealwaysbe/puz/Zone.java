
package app.crossword.yourealwaysbe.puz;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Representation of a word on the grid
 *
 * Normally where an answer to a clue goes, a sequence of boxes, usually
 * in a line across or down, but can be any sequence.
 */
public class Zone implements Iterable<Position> {
    private final List<Position> positions = new ArrayList<>();

    public boolean hasPosition(Position pos) {
        return positions.contains(pos);
    }

    public int size() {
        return positions.size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Get position at index, or null
     */
    public Position getPosition(int idx) {
        if (idx >= 0 && idx < positions.size())
            return positions.get(idx);
        else
            return null;
    }

    /**
     * Index of position or -1
     */
    public int indexOf(Position pos) {
        return positions.indexOf(pos);
    }

    public void addPosition(Position pos) {
        if (pos != null)
            positions.add(pos);
    }

    public void appendZone(Zone zone) {
        for (Position pos : zone)
            addPosition(pos);
    }

    @Override
    public Iterator<Position> iterator() {
        return positions.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof Zone))
            return false;

        Zone other = (Zone) o;

        return positions.equals(other.positions);
    }

    @Override
    public int hashCode() {
        return positions.hashCode();
    }

    @Override
    public String toString() {
        return positions.toString();
    }
}
