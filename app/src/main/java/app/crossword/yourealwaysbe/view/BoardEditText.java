package app.crossword.yourealwaysbe.view;

import java.util.logging.Logger;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import androidx.preference.PreferenceManager;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Position;

public class BoardEditText extends ScrollingImageView {
    private static final Logger LOG = Logger.getLogger(BoardEditText.class.getCanonicalName());

    public interface BoardEditFilter {
        /**
         * @param oldChar the character being deleted
         * @param pos the position of the box being deleted from
         * @return true if the deletion is allowed to occur
         */
        public boolean delete(char oldChar, int pos);

        /**
         * @param oldChar the character that used to be in the box
         * @param newChar the character to replace it with
         * @param pos the position of the box
         * @return the actual character to replace the old one with or null char
         * if the replacement is not allowed
         */
        public char filter(char oldChar, char newChar, int pos);
    }

    private Position selection = new Position(0, -1);
    private Box[] boxes;
    private PlayboardRenderer renderer;
    // surely a better way...
    static final String ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private SharedPreferences prefs;

    // we have our own onTap for input, but the activity containing the widget
    // might also need to know about on taps, so override setContextMenuListener
    // to intercept
    private ClickListener ctxListener;
    private BoardEditFilter[] filters;
    private CharSequence contentDescriptionBase;

    /**
     * Call setRenderer to set the same renderer as used by the activity using
     * the boardedittext widget.  Else, falls back onto
     * ForkyzApplication.RENDERER
     */
    public BoardEditText(Context context, AttributeSet as) {
        super(context, as);

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();

        // We are not giving the renderer a board, so be careful some
        // method calls may NPE
        this.renderer = new PlayboardRenderer(
            null,
            metrics.densityDpi,
            metrics.widthPixels,
            false,
            context
        );

        setAllowOverScroll(false);

        super.setContextMenuListener(new ClickListener() {
            public void onContextMenu(Point e) {
                if (ctxListener != null) {
                    ctxListener.onContextMenu(e);
                }
            }

            public void onTap(Point e) {
                BoardEditText.this.requestFocus();

                int box = renderer.findBox(e).getCol();
                if (boxes != null && box < boxes.length) {
                    selection.setCol(box);
                }
                BoardEditText.this.render();

                if (ctxListener != null) {
                    ctxListener.onTap(e);
                }
            }
        });

        prefs = PreferenceManager.getDefaultSharedPreferences(context);

        contentDescriptionBase = getContentDescription();
    }

    @Override
    public void onFocusChanged(
        boolean gainFocus, int direction, Rect previouslyFocusedRect
    ) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (!gainFocus) {
            selection.setCol(-1);
            render();
        } else if (boxes != null) {
            if (selection.getCol() < 0
                    || selection.getCol() >= boxes.length) {
                selection.setCol(0);
                render();
            }
        }
    }

    @Override
    public void setContextMenuListener(ClickListener l) {
        this.ctxListener = l;
    }

    public void setFilters(BoardEditFilter[] filters) {
        this.filters = filters;
    }

    public void setLength(int len) {
        if (boxes == null || len != boxes.length) {
            Box[] newBoxes = new Box[len];

            int overlap = 0;
            if (boxes != null) {
                overlap = Math.min(len, boxes.length);
                for (int i = 0; i < overlap; i++) {
                    newBoxes[i] = boxes[i];
                }
            }

            for (int i = overlap; i < len; ++i) {
                newBoxes[i] = new Box();
            }

            boxes = newBoxes;

            render();
        }
    }

    public void setRenderer(PlayboardRenderer renderer) {
        this.renderer = renderer;
        render();
    }

    public int getLength() {
        return (boxes == null) ? 0 : boxes.length;
    }

    public Box[] getBoxes() {
        return boxes;
    }

    public boolean isBlank(int pos) {
        if (boxes != null && 0 <= pos && pos < boxes.length) {
            return boxes[pos].isBlank();
        } else {
            return true;
        }
    }

    public char getResponse(int pos) {
        if (boxes != null && 0 <= pos && pos < boxes.length) {
            return boxes[pos].getResponse();
        } else {
            return '\0';
        }
    }

    public void setResponse(int pos, char c) {
        if (boxes != null && 0 <= pos && pos < boxes.length) {
            boxes[pos].setResponse(c);
            render();
        }
    }

    public void setFromString(String text) {
        if (text == null) {
            boxes = null;
        } else {
            boxes = new Box[text.length()];
            for (int i = 0; i < text.length(); i++) {
                boxes[i] = new Box();
                boxes[i].setResponse(text.charAt(i));
            }
        }
        render();
    }

    public void clear() {
        if (boxes != null) {
            for (Box box : boxes)
                box.setBlank();
            render();
        }
    }

    public String toString() {
        if (boxes == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < boxes.length; i++) {
            sb.append(boxes[i].getResponse());
        }

        return sb.toString();
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_MENU:
            return false;

        case KeyEvent.KEYCODE_DPAD_LEFT: {
                int col = selection.getCol();
                if (col > 0) {
                    selection.setCol(col - 1);
                    this.render();
                }
                return true;
            }

        case KeyEvent.KEYCODE_DPAD_RIGHT: {
                int col = selection.getCol();
                if (boxes != null && col < boxes.length - 1) {
                    selection.setCol(col + 1);
                    this.render();
                }

                return true;
            }

        case KeyEvent.KEYCODE_DEL:
            if (boxes != null) {
                int col = selection.getCol();
                if (boxes[col].isBlank() && col > 0)
                    selection.setCol(col - 1);
                if (canDelete(selection))
                    boxes[selection.getCol()].setBlank();
                this.render();
            }
            return true;

        case KeyEvent.KEYCODE_SPACE:
            if (boxes != null && canDelete(selection)) {
                int col = selection.getCol();
                boxes[col].setBlank();

                if (col < boxes.length - 1) {
                    selection.setCol(col + 1);
                }

                this.render();
            }
            return true;
        }

        char c = Character.toUpperCase(event.getDisplayLabel());

        if (boxes != null && ALPHA.indexOf(c) != -1) {
            c = filterReplacement(c, selection);

            if (c != '\0') {
                int col = selection.getCol();

                boxes[col].setResponse(c);

                if (col < boxes.length - 1) {
                    col += 1;
                    int nextPos = col;

                    while (getBoard().isSkipCompletedLetters() &&
                           boxes[col].getResponse() != ' ' &&
                           col < boxes.length - 1) {
                        col += 1;
                    }

                    selection.setCol(col);

                    if (boxes[col].getResponse() != ' ')
                        selection.setCol(nextPos);
                }

                this.render();
            }

            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    private void render() {
        boolean displayScratch = prefs.getBoolean("displayScratch", false);
        setBitmap(renderer.drawBoxes(boxes, selection, displayScratch, displayScratch));
        setContentDescription(
            renderer.getContentDescription(
                contentDescriptionBase, boxes, selection.getCol(), true
            )
        );
    }

    private boolean canDelete(Position pos) {
        if (filters == null)
            return true;

        if (boxes == null || pos.getCol() < 0 || pos.getCol() >= boxes.length)
            return false;

        char oldChar = boxes[pos.getCol()].getResponse();

        for (BoardEditFilter filter : filters) {
            if (filter != null && !filter.delete(oldChar, pos.getCol())) {
                return false;
            }
        }

        return true;
    }

    private char filterReplacement(char newChar, Position pos) {
        if (filters == null)
            return newChar;

        if (boxes == null || pos.getCol() < 0 || pos.getCol() >= boxes.length)
            return '\0';

        char oldChar = boxes[pos.getCol()].getResponse();

        for (BoardEditFilter filter : filters) {
            if (filter != null) {
                newChar = filter.filter(oldChar, newChar, pos.getCol());
            }
        }

        return newChar;
    }

    private Playboard getBoard(){
        return ForkyzApplication.getInstance().getBoard();
    }
}
