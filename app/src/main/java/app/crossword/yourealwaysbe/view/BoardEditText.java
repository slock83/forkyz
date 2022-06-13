package app.crossword.yourealwaysbe.view;

import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.ViewTreeObserver;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import androidx.preference.PreferenceManager;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Position;

public class BoardEditText extends ScrollingImageView {
    private static final Logger LOG
        = Logger.getLogger(BoardEditText.class.getCanonicalName());

    private Handler handler = new Handler(Looper.getMainLooper());

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

    private SharedPreferences prefs;

    private BoardEditFilter[] filters;
    private CharSequence contentDescriptionBase;

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
        setAllowZoom(false);

        prefs = PreferenceManager.getDefaultSharedPreferences(context);

        contentDescriptionBase = getContentDescription();
    }

    @Override
    protected void onSizeChanged(
        int newWidth, int newHeight, int oldWidth, int oldHeight
    ) {
        super.onSizeChanged(newWidth, newHeight, oldWidth, oldHeight);
        // do after layout pass (has no effect during pass)
        getViewTreeObserver().addOnPreDrawListener(
            new ViewTreeObserver.OnPreDrawListener() {
                public boolean onPreDraw() {
                    getViewTreeObserver().removeOnPreDrawListener(this);
                    scrollTo(0, 0);
                    int width = getWidth();
                    float scale = renderer.fitWidthTo(width, boxes.length);
                    if (scale > 1) {
                        renderer.setScale(1.0F);
                    }
                    render();
                    return false;
                }
            }
        );
    }

    @Override
    protected void onTap(Point point) {
        BoardEditText.this.requestFocus();

        int box = findPosition(point);
        if (box >= 0) {
            selection.setCol(box);
            BoardEditText.this.render();
        }

        super.onTap(point);
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
            String response = boxes[pos].getResponse();
            if (response == null || response.isEmpty())
                return '\0';
            else
                return response.charAt(0);
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

    public void setResponse(int pos, String c) {
        if (c != null && !c.isEmpty())
            setResponse(pos, c.charAt(0));
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

    @Override
    public boolean onCheckIsTextEditor() {
        // todo will change i think
        return false;
    }

    // Set input type to be raw keys if a keyboard is used
    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        BaseInputConnection fic = new BaseInputConnection(this, false);
        outAttrs.inputType = InputType.TYPE_NULL;
        return fic;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_LEFT:
        case KeyEvent.KEYCODE_DPAD_RIGHT:
        case KeyEvent.KEYCODE_DEL:
        case KeyEvent.KEYCODE_SPACE:
            return true;

        default:
            char c = Character.toUpperCase(event.getDisplayLabel());
            if (boxes != null && Character.isLetterOrDigit(c))
                return true;
        }

        return super.onKeyUp(keyCode, event);
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

        if (boxes != null && Character.isLetterOrDigit(c)) {
            c = filterReplacement(c, selection);

            if (c != '\0') {
                int col = selection.getCol();

                boxes[col].setResponse(c);

                if (col < boxes.length - 1) {
                    col += 1;
                    int nextPos = col;

                    while (getBoard().isSkipCompletedLetters() &&
                           !boxes[col].isBlank() &&
                           col < boxes.length - 1) {
                        col += 1;
                    }

                    selection.setCol(col);

                    if (!boxes[col].isBlank())
                        selection.setCol(nextPos);
                }

                this.render();
            }

            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    private void render() {
        if (getWidth() == 0)
            return;

        boolean displayScratch = prefs.getBoolean("displayScratch", false);
        Set<String> suppressNotesList
            = displayScratch
            ? Collections.emptySet()
            : null;

        setBitmap(renderer.drawBoxes(boxes, selection, suppressNotesList));
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

        char oldChar = getResponse(pos.getCol());

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

        char oldChar = getResponse(pos.getCol());

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

    private int findPosition(Point point) {
        if (boxes == null)
            return -1;

        Position position = renderer.findPosition(point);
        if (position == null)
            return -1;

        int box = position.getCol();

        if (0 <= box && box < boxes.length)
            return box;
        else
            return -1;
    }
}
