
package app.crossword.yourealwaysbe.view;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;

import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Zone;

/**
 * A live view of a word on the playboard
 *
 * Renders the playboard on change and implements input connection with
 * soft-input.
 */
public class BoardWordEditView extends BoardEditView {
    public BoardWordEditView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setAllowOverScroll(false);
        setAllowZoom(false);
    }

    @Override
    public void onPlayboardChange(
        boolean wholeBoard, Word currentWord, Word previousWord
    ) {
        if (!Objects.equals(currentWord, previousWord)) {
            fitToView(true);
            render(true);
        } else {
            render();
        }
        super.onPlayboardChange(wholeBoard, currentWord, previousWord);
    }

    @Override
    public Position findPosition(Point point) {
        PlayboardRenderer renderer = getRenderer();
        if (renderer == null)
            return null;

        Zone zone = getCurrentZone();
        if (zone == null)
            return null;

        int box = renderer.findPosition(point).getCol();
        return zone.getPosition(box);
    }

    @Override
    public float fitToView() {
        return fitToView(false);
    }

    @Override
    public void render(Word previous, boolean rescale) {
        // don't draw until we get onSizeChanged
        if (getWidth() == 0)
            return;

        PlayboardRenderer renderer = getRenderer();
        if (renderer == null)
            return;

        setBitmap(renderer.drawWord(getSuppressNotesList()), rescale);
        setContentDescription(
            renderer.getContentDescription(getContentDescriptionBase())
        );
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
                    // don't render during fitToView because we want to
                    // guarantee rendering (and fitToView won't render
                    // if the scale doesn't change).
                    fitToView(true);
                    render(true);
                    return false;
                }
            }
        );
    }

    @Override
    protected void onClick(Position newPosition) {
        Playboard board = getBoard();
        if (board == null)
            return;

        Position currentPosition = board.getHighlightLetter();

        if (Objects.equals(currentPosition, newPosition)) {
            // don't set highlight letter to avoid toggle of direction
            Word currentWord = board.getCurrentWord();
            notifyClick(newPosition, currentWord);
        } else {
            Word previousWord = board.setHighlightLetter(newPosition);
            notifyClick(newPosition, previousWord);
        }
    }

    @Override
    protected Set<String> getSuppressNotesList() {
        boolean displayScratch = getPrefs().getBoolean("displayScratch", false);
        if (!displayScratch)
            return null;

        ClueID cid = getBoard().getClueID();
        if (cid == null)
            return Collections.emptySet();

        String list = cid.getListName();
        if (list == null)
            return Collections.emptySet();
        else
            return Collections.singleton(list);
    }

    private Zone getCurrentZone() {
        Playboard board = getBoard();
        Word current = (board == null) ? null : board.getCurrentWord();
        return (current == null) ? null : current.getZone();
    }

    private float fitToView(boolean noRender) {
        PlayboardRenderer renderer = getRenderer();
        if (renderer == null)
            return -1;

        scrollTo(0, 0);

        int width = getWidth();
        Zone zone = getCurrentZone();
        if (zone == null)
            return -1;

        int numBoxes = zone.size();

        float scale = renderer.fitWidthTo(width, numBoxes);
        if (scale > 1) {
            scale = 1.0F;
            renderer.setScale(scale);
        }

        setCurrentScale(scale, noRender);

        return scale;
    }
}
