
package app.crossword.yourealwaysbe.puz;

import java.io.Serializable;
import java.util.Objects;

/**
 * An image to be on the board
 *
 * Specified by grid start position and width/height in cells. The URL can
 * either be a url to a file, or a data url in base64.
 */
public class PuzImage implements Serializable {
    private String url;
    private int row;
    private int col;
    private int width;
    private int height;
    private Object tag;

    public PuzImage(String url, int row, int col, int width, int height) {
        this.url = url;
        this.row = row;
        this.col = col;
        this.width = width;
        this.height = height;
    }

    public String getURL() { return url; }
    public int getRow() { return row; }
    public int getCol() { return col; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public void setURL(String url) {
        this.url = url;
    }

    /**
     * Add an object to the image
     *
     * Designed to optimise renders that might want to keep a copy of
     * their bitmap object somewhere convenient.
     */
    public void setTag(Object tag) {
        this.tag = tag;
    }

    public Object getTag() {
        return tag;
    }

    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (!(o instanceof PuzImage))
            return false;

        PuzImage other = (PuzImage) o;

        return Objects.equals(getURL(), other.getURL())
            && Objects.equals(getRow(), other.getRow())
            && Objects.equals(getCol(), other.getCol())
            && Objects.equals(getWidth(), other.getWidth())
            && Objects.equals(getHeight(), other.getHeight());
    }

    public int hashCode() {
        return Objects.hash(url, row, col, width, height);
    }
}
