package app.crossword.yourealwaysbe.view;

import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import androidx.preference.PreferenceManager;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.util.BoxInputConnection;
import app.crossword.yourealwaysbe.util.KeyboardManager;
import app.crossword.yourealwaysbe.versions.AndroidVersionUtils;

public class BoardEditText
        extends ScrollingImageView
        implements BoxInputConnection.BoxInputListener,
            KeyboardManager.ManageableView {

    private static final Logger LOG
        = Logger.getLogger(BoardEditText.class.getCanonicalName());

    private BoxInputConnection currentInputConnection = null;
    private boolean nativeInput = false;

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
                    int width = getContentWidth();
                    float scale = renderer.fitWidthTo(width, boxes.length);
                    if (scale > 1) {
                        renderer.setScale(1.0F);
                    }
                    render();
                    return true;
                }
            }
        );
    }

    @Override
    protected void onTap(Point point) {
        BoardEditText.this.requestFocus();

        int box = findPosition(point);
        if (box >= 0) {
            setSelectedCol(box);
            BoardEditText.this.render();
            updateInputConnection();
        }

        super.onTap(point);
    }

    @Override
    public void onNewResponse(String response) {
        if (response == null || response.isEmpty())
            return;
        onNewResponse(response.charAt(0));
    }

    @Override
    public void onDeleteResponse() {
        if (boxes != null) {
            int col = getSelectedCol();
            if (boxes[col].isBlank() && col > 0)
                setSelectedCol(col - 1);
            if (canDelete(selection))
                boxes[getSelectedCol()].setBlank();
            this.render();
            updateInputConnection();
        }
    }

    @Override
    public void onFocusChanged(
        boolean gainFocus, int direction, Rect previouslyFocusedRect
    ) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (!gainFocus) {
            setSelectedCol(-1);
            render();
        } else if (boxes != null) {
            if (getSelectedCol() < 0
                    || getSelectedCol() >= boxes.length) {
                setSelectedCol(0);
                updateInputConnection();
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
        setResponseNoInputConnectionUpdate(pos, c);
        updateInputConnection();
    }

    public void setResponseNoInputConnectionUpdate(int pos, char c) {
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
        updateInputConnection();
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
    public void setNativeInput(boolean nativeInput) {
        this.nativeInput = nativeInput;
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return nativeInput;
    }

    // Set input type to be raw keys if a keyboard is used
    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        currentInputConnection = new BoxInputConnection(
            this,
            getSelectedResponse(),
            this
        );
        currentInputConnection.setOutAttrs(outAttrs);
        return currentInputConnection;
    }

    @Override
    public InputConnection onCreateForkyzInputConnection(EditorInfo outAttrs) {
        BaseInputConnection fic = new BaseInputConnection(this, false);
        outAttrs.inputType = InputType.TYPE_NULL;
        return fic;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_LEFT:
        case KeyEvent.KEYCODE_DPAD_RIGHT:
        case KeyEvent.KEYCODE_DEL:
        case KeyEvent.KEYCODE_SPACE:
            return true;

        default:
            char c = Character.toUpperCase(event.getDisplayLabel());
            if (boxes != null && isAcceptableCharacterResponse(c))
                return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_MENU:
            return false;

        case KeyEvent.KEYCODE_DPAD_LEFT: {
                int col = getSelectedCol();
                if (col > 0) {
                    setSelectedCol(col - 1);
                    this.render();
                }
                return true;
            }

        case KeyEvent.KEYCODE_DPAD_RIGHT: {
                int col = getSelectedCol();
                if (boxes != null && col < boxes.length - 1) {
                    setSelectedCol(col + 1);
                    this.render();
                }

                return true;
            }

        case KeyEvent.KEYCODE_DEL:
            onDeleteResponse();
            return true;

        // space handled as any char
        }

        char c = Character.toUpperCase(event.getDisplayLabel());

        if (boxes != null && isAcceptableCharacterResponse(c)) {
            onNewResponse(c);
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    public int getNumNonBlank() {
        int count = 0;
        int len = getLength();
        for (int i = 0; i < len; i++) {
            if (!isBlank(i))
                count += 1;
        }
        return count;
    }

    /**
     * If this character in the string is a blank box
     */
    public static boolean isBlank(char c) {
        return Box.BLANK.equals(String.valueOf(c));
    }

    private void onNewResponse(char c) {
        if (!isAcceptableCharacterResponse(c))
            return;

        c = filterReplacement(c, selection);

        if (c != '\0') {
            int col = getSelectedCol();

            // we'll update later if the selection doesn't change
            setResponseNoInputConnectionUpdate(col, c);

            if (col < boxes.length - 1) {
                col += 1;
                int nextPos = col;

                while (getBoard().isSkipCompletedLetters() &&
                       !boxes[col].isBlank() &&
                       col < boxes.length - 1) {
                    col += 1;
                }

                if (boxes[col].isBlank())
                    setSelectedCol(col);
                else
                    setSelectedCol(nextPos);
            } else {
                updateInputConnection();
            }

            this.render();
        } else {
            // Needs updating else thinks there's a character in the
            // buffer, when it was refused
            updateInputConnection();
        }
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
                contentDescriptionBase, boxes, getSelectedCol(), true
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

    private int getSelectedCol() {
        return selection.getCol();
    }

    private void setSelectedCol(int col) {
        selection.setCol(col);
        updateInputConnection();
    }

    private String getSelectedResponse() {
        int col = getSelectedCol();
        if (0 <= col && col < boxes.length)
            return boxes[col].getResponse();
        else
            return Box.BLANK;
    }

    private void updateInputConnection() {
        if (currentInputConnection != null) {
            String response = getSelectedResponse();
            currentInputConnection.setResponse(response);
        }
    }

    private boolean isAcceptableCharacterResponse(char c) {
        return AndroidVersionUtils.Factory
            .getInstance()
            .isAcceptableCharacterResponse(c);
    }

    /**
     * Width of the usable area inside the view (sans padding)
     */
    private int getContentWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }
}
