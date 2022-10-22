
package app.crossword.yourealwaysbe.view;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
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
 *
 * Use setWord to specify which word to draw, else current word will be
 * drawn.
 */
public class BoardWordEditView extends BoardEditView {
    private Word word;
    private Set<Position> wordPositions;
    private boolean incognitoMode;
    private int originalHeight;
    private Set<String> suppressNotesList = Collections.<String>emptySet();

    public BoardWordEditView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setAllowOverScroll(false);
        setAllowZoom(false);
        setMaxScale(1.0f);
    }

    /**
     * Set incognito mode
     *
     * This is a bit of a hack for having a more or less invisible input
     * view at the top of the clues list if the board is being show in
     * the clues items.
     */
    public void setIncognitoMode(boolean incognitoMode) {
        if (this.incognitoMode != incognitoMode) {
            ViewGroup.LayoutParams params = getLayoutParams();
            if (incognitoMode) {
                originalHeight = params.height;
                params.height = 1;
            } else {
                params.height = originalHeight;
            }
            setLayoutParams(params);

            this.incognitoMode = incognitoMode;
        }
    }

    @Override
    public void setBoard(Playboard board) {
        super.setBoard(board);
        setMaxScale(1.0f);
    }

    /**
     * Convenience to set board and suppress notes list efficiently
     */
    public void setBoard(Playboard board, Set<String> suppressNotesList) {
        this.suppressNotesList = suppressNotesList;
        setBoard(board);
    }

    /**
     * Set word to draw or null to draw current word
     */
    public void setWord(Word word, Set<String> suppressNotesList) {
        this.suppressNotesList = suppressNotesList;
        this.word = word;
        onRenderWordChanged();
        // need to rescale with new word
        renderToView();
    }

    @Override
    public void onPlayboardChange(
        boolean wholeBoard, Word currentWord, Word previousWord
    ) {
        if (isRendering()) {
            // if rendering current word, which is changing, refit
            // else redraw if data indicates the rendered word is
            // affected
            if (
                isRenderingCurrentWord()
                && !Objects.equals(currentWord, previousWord)
            ) {
                renderToView();
                onRenderWordChanged();
            } else if (redrawNeeded(wholeBoard, currentWord, previousWord)) {
                render();
            }
        }
        super.onPlayboardChange(wholeBoard, currentWord, previousWord);
    }

    @Override
    public Position findPosition(Point point) {
        PlayboardRenderer renderer = getRenderer();
        if (renderer == null)
            return null;

        Zone zone = getRenderWordZone();
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
        if (!isRendering())
            return;

        PlayboardRenderer renderer = getRenderer();
        if (renderer == null)
            return;

        Word renderWord = getRenderWord();
        setBitmap(
            renderer.drawWord(renderWord, getSuppressNotesList()),
            rescale
        );
        setContentDescription(
            renderer.getContentDescription(
                renderWord,
                getContentDescriptionBase()
            )
        );
    }

    @Override
    protected void onSizeChanged(
        int newWidth, int newHeight, int oldWidth, int oldHeight
    ) {
        super.onSizeChanged(newWidth, newHeight, oldWidth, oldHeight);

        if (!isRendering())
            return;

        // do after layout pass (has no effect during pass)
        getViewTreeObserver().addOnPreDrawListener(
            new ViewTreeObserver.OnPreDrawListener() {
                public boolean onPreDraw() {
                    getViewTreeObserver().removeOnPreDrawListener(this);
                    renderToView();
                    return true;
                }
            }
        );
    }

    @Override
    protected void onClick(Position newPosition) {
        if (!isRendering())
            return;

        Playboard board = getBoard();
        if (board == null)
            return;

        Position curPosition = board.getHighlightLetter();
        ClueID curCID = board.getClueID();

        Word renderWord = getRenderWord();
        ClueID renderCID = renderWord.getClueID();

        boolean samePosition
            = Objects.equals(curPosition, newPosition)
                && Objects.equals(curCID, renderCID);

        if (samePosition) {
            // don't set highlight letter to avoid toggle of direction
            Word currentWord = board.getCurrentWord();
            notifyClick(newPosition, currentWord);
        } else {
            Word previousWord
                = board.setHighlightLetter(newPosition, renderCID);
            notifyClick(newPosition, previousWord);
        }
    }

    @Override
    protected void onVisibilityChanged (View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == View.VISIBLE)
            renderToView();
    }

    @Override
    protected Set<String> getSuppressNotesList() {
        return suppressNotesList;
    }

    private boolean isRenderingCurrentWord() {
        return word == null;
    }

    private Word getRenderWord() {
        Word renderWord = word;
        if (isRenderingCurrentWord()) {
            Playboard board = getBoard();
            renderWord = (board == null) ? null : board.getCurrentWord();
        }
        return renderWord;
    }

    private Zone getRenderWordZone() {
        Word renderWord = getRenderWord();
        return (renderWord == null) ? null : renderWord.getZone();
    }

    private float fitToView(boolean noRender) {
        PlayboardRenderer renderer = getRenderer();
        if (renderer == null)
            return -1;

        scrollTo(0, 0);

        int width = getContentWidth();
        Zone zone = getRenderWordZone();
        if (zone == null)
            return -1;

        int numBoxes = zone.size();

        float scale = renderer.fitWidthTo(width, numBoxes);

        setCurrentScale(scale, noRender);

        return scale;
    }

    /**
     * Width of the usable area inside the view (sans padding)
     */
    private int getContentWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    /**
     * Indicates whether rendering
     *
     * Not rendering if in incognito mode, or width is zero, or not
     * visible
     */
    private boolean isRendering() {
        return !incognitoMode && getWidth() != 0 && getVisibility() == VISIBLE;
    }

    /**
     * Render after making sure fits view size
     */
    private void renderToView() {
        // don't render during fitToView because we want to
        // guarantee rendering (and fitToView won't render
        // if the scale doesn't change).
        fitToView(true);
        render(true);
    }

    private boolean redrawNeeded(
        boolean wholeBoard, Word currentWord, Word previousWord
    ) {
        if (wholeBoard)
            return true;

        Set<Position> positions = getRenderWordPositions();

        if (positions.isEmpty())
            return false;

        return wordOverlaps(positions, currentWord)
            || wordOverlaps(positions, previousWord);
    }

    /**
     * Get (cached) positions of rendering word
     *
     * Call onRenderWordChanged when rendering word changes to clear
     * cache.
     */
    private Set<Position> getRenderWordPositions() {
        if (wordPositions == null) {
            Zone zone = getRenderWordZone();

            if (zone == null || zone.isEmpty()) {
                wordPositions = Collections.<Position>emptySet();
            } else {
                wordPositions = new HashSet<>(zone.size());
                for (Position position : zone) {
                    wordPositions.add(position);
                }
            }
        }
        return wordPositions;
    }

    /**
     * E.g. clears wordPositions cached
     */
    private void onRenderWordChanged() {
        this.wordPositions = null;
    }

    private boolean wordOverlaps(Set<Position> positions, Word word) {
        if (word == null)
            return false;

        Zone zone = word.getZone();

        if (zone == null)
            return false;

        for (Position position : zone) {
            if (positions.contains(position))
                return true;
        }

        return false;
    }
}
