package app.crossword.yourealwaysbe.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Base64;

import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.Note;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.PuzImage;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.Zone;
import app.crossword.yourealwaysbe.versions.AndroidVersionUtils;
import app.crossword.yourealwaysbe.view.ScrollingImageView.Point;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

public class PlayboardRenderer {
    // for calculating max scale with no puzzle
    private static final int DEFAULT_PUZZLE_WIDTH = 15;
    private static final float BASE_BOX_SIZE_INCHES = 0.25F;
    private static final Logger LOG = Logger.getLogger(PlayboardRenderer.class.getCanonicalName());
    private static final Typeface TYPEFACE_SEMI_BOLD_SANS =
        AndroidVersionUtils.Factory.getInstance().getSemiBoldTypeface();
    private static final float DESCENT_FUDGE_FACTOR = 1.3F;

    private final Paint blackBox = new Paint();
    private final Paint blackCircle = new Paint();
    private final Paint blackLine = new Paint();
    private final Paint cheated = new Paint();
    private final Paint currentLetterBox = new Paint();
    private final Paint currentLetterHighlight = new Paint();
    private final Paint currentWordHighlight = new Paint();
    private final TextPaint letterText = new TextPaint();
    private final TextPaint numberText = new TextPaint();
    private final TextPaint noteText = new TextPaint();
    private final TextPaint miniNoteText = new TextPaint();
    private final Paint red = new Paint();
    private final TextPaint redHighlight = new TextPaint();
    private final TextPaint white = new TextPaint();
    private final Paint flag = new Paint();
    private Bitmap bitmap;
    private Playboard board;
    private float dpi;
    private float scale = 1.0F;
    private float maxScale;
    private float minScale;
    private boolean hintHighlight;
    private int widthPixels;

    private final static AndroidVersionUtils versionUtils
        = AndroidVersionUtils.Factory.getInstance();

    // colors are gotten from context
    public PlayboardRenderer(Playboard board,
                             float dpi, int widthPixels, boolean hintHighlight,
                             Context context) {
        this.dpi = dpi;
        this.widthPixels = widthPixels;
        this.board = board;
        this.hintHighlight = hintHighlight;
        this.maxScale = getDeviceMaxScale();
        this.minScale = getDeviceMinScale();

        int blankColor = ContextCompat.getColor(context, R.color.blankColor);
        int boxColor = ContextCompat.getColor(context, R.color.boxColor);
        int currentWordHighlightColor
            = ContextCompat.getColor(context, R.color.currentWordHighlightColor);
        int currentLetterHighlightColor
            = ContextCompat.getColor(context, R.color.currentLetterHighlightColor);
        int errorColor
            = ContextCompat.getColor(context, R.color.errorColor);
        int errorHighlightColor
            = ContextCompat.getColor(context, R.color.errorHighlightColor);
        int cheatedColor
            = ContextCompat.getColor(context, R.color.cheatedColor);
        int boardLetterColor
            = ContextCompat.getColor(context, R.color.boardLetterColor);
        int boardNoteColor
            = ContextCompat.getColor(context, R.color.boardNoteColor);
        int flagColor = ContextCompat.getColor(context, R.color.flagColor);

        blackLine.setColor(blankColor);
        blackLine.setStrokeWidth(2.0F);

        numberText.setTextAlign(Align.LEFT);
        numberText.setColor(boardLetterColor);
        numberText.setAntiAlias(true);
        numberText.setTypeface(Typeface.MONOSPACE);

        noteText.setTextAlign(Align.CENTER);
        noteText.setColor(boardNoteColor);
        noteText.setAntiAlias(true);
        noteText.setTypeface(TYPEFACE_SEMI_BOLD_SANS);

        miniNoteText.setTextAlign(Align.CENTER);
        miniNoteText.setColor(boardNoteColor);
        miniNoteText.setAntiAlias(true);
        miniNoteText.setTypeface(TYPEFACE_SEMI_BOLD_SANS);

        letterText.setTextAlign(Align.CENTER);
        letterText.setColor(boardLetterColor);
        letterText.setAntiAlias(true);
        letterText.setTypeface(Typeface.SANS_SERIF);

        blackBox.setColor(blankColor);

        blackCircle.setColor(boardLetterColor);
        blackCircle.setAntiAlias(true);
        blackCircle.setStyle(Style.STROKE);

        currentWordHighlight.setColor(currentWordHighlightColor);
        currentLetterHighlight.setColor(currentLetterHighlightColor);
        currentLetterBox.setColor(boxColor);
        currentLetterBox.setStrokeWidth(2.0F);

        white.setTextAlign(Align.CENTER);
        white.setColor(boxColor);
        white.setAntiAlias(true);
        white.setTypeface(Typeface.SANS_SERIF);

        red.setTextAlign(Align.CENTER);
        red.setColor(errorColor);
        red.setAntiAlias(true);
        red.setTypeface(Typeface.SANS_SERIF);

        redHighlight.setTextAlign(Align.CENTER);
        redHighlight.setColor(errorHighlightColor);
        redHighlight.setAntiAlias(true);
        redHighlight.setTypeface(Typeface.SANS_SERIF);

        cheated.setColor(cheatedColor);

        flag.setColor(flagColor);
    }

    public float getMaxScale() {
        return maxScale;
    }

    public float getMinScale() {
        return minScale;
    }

    public void setMaxScale(float maxScale) {
        this.maxScale = maxScale;
    }

    public void setMinScale(float minScale) {
        this.minScale = minScale;
    }

    public float getDeviceMaxScale(){
        float retValue;
        // inches * pixels per inch * units
        retValue = 2.2F;
        Puzzle puz = (board == null) ? null : board.getPuzzle();
        int width = (puz == null) ? DEFAULT_PUZZLE_WIDTH : puz.getWidth();
        float puzzleBaseSizeInInches = width * BASE_BOX_SIZE_INCHES;
        //leave a 1/16th in gutter on the puzzle.
        float fitToScreen =  (dpi * (puzzleBaseSizeInInches + 0.0625F)) / dpi;

        if(retValue < fitToScreen){
            retValue = fitToScreen;
        }

        return retValue;
    }

    public float getDeviceMinScale(){
        //inches * (pixels / pixels per inch);
        float retValue = 0.9F * ((dpi * BASE_BOX_SIZE_INCHES) / dpi);
        return retValue;
    }

    public void setScale(float scale) {
        float maxScale = getMaxScale();
        float minScale = getMinScale();

        if (scale > maxScale) {
            scale = maxScale;
        } else if (scale < minScale) {
            scale = minScale;
        } else if (Float.isNaN(scale)) {
            scale = 1.0f;
        }
        this.bitmap = null;
        this.scale = scale;
    }

    public float getScale() {
        return this.scale;
    }

    /**
     * Draw the board or just refresh it
     *
     * Refreshes current word and reset word if not null
     *
     * @param suppressNotesLists as in drawBox
     */
    public Bitmap draw(Word reset, Set<String> suppressNotesLists) {
        try {
            Puzzle puz = this.board.getPuzzle();
            Box[][] boxes = this.board.getBoxes();
            int width = puz.getWidth();
            int height = puz.getHeight();

            boolean newBitmap = initialiseBitmap();
            boolean renderAll = reset == null || newBitmap;

            Canvas canvas = new Canvas(bitmap);

            drawBoardBoxes(canvas, reset, renderAll, suppressNotesLists);
            drawPinnedClue(canvas, reset, renderAll, suppressNotesLists);
            if (renderAll)
                drawImages(canvas);

            return bitmap;
        } catch (OutOfMemoryError e) {
            return bitmap;
        }
    }

    /**
     * Draw current word
     *
     * @param suppressNotesLists as in drawBox
     */
    public Bitmap drawWord(Set<String> suppressNotesLists) {
        return drawWord(this.board.getCurrentWord(), suppressNotesLists);
    }

    /**
     * Draw word suppressing no notes
     */
    public Bitmap drawWord(Word word) {
        // call draw word with empty list
        return drawWord(word, Collections.<String>emptySet());
    }

    /**
     * Draw given word
     */
    public Bitmap drawWord(Word word, Set<String> suppressNotesLists) {
        Box[] boxes = board.getWordBoxes(word);
        Zone zone = word.getZone();
        int boxSize = getBoxSize();
        Position highlight = board.getHighlightLetter();

        Bitmap bitmap = Bitmap.createBitmap(
            boxes.length * boxSize, boxSize, Bitmap.Config.RGB_565
        );
        bitmap.eraseColor(Color.BLACK);

        Canvas canvas = new Canvas(bitmap);

        for (int i = 0; i < boxes.length; i++) {
            int x = i * boxSize;
            int y = 0;
            Position pos = zone.getPosition(i);
            this.drawBox(
                canvas,
                x, y,
                pos.getRow(), pos.getCol(),
                boxSize,
                boxes[i],
                null, highlight,
                suppressNotesLists,
                false
            );
        }

        // draw highlight outline again as it will have been overpainted
        if (highlight != null) {
            int idx = zone.indexOf(highlight);
            if (idx > -1) {
                int x = idx * boxSize;
                drawBoxOutline(canvas, x, 0, boxSize, currentLetterBox);
            }
        }

        return bitmap;
    }

    /**
     * Draw the boxes
     *
     * @param suppressNotesLists as in drawBox
     */
    public Bitmap drawBoxes(
        Box[] boxes,
        Position highlight,
        Set<String> suppressNotesLists
    ) {
        if (boxes == null || boxes.length == 0) {
            return null;
        }

        int boxSize = getBoxSize();
        Bitmap bitmap = Bitmap.createBitmap(boxes.length * boxSize,
                                            boxSize,
                                            Bitmap.Config.RGB_565);
        bitmap.eraseColor(Color.BLACK);

        Canvas canvas = new Canvas(bitmap);

        for (int i = 0; i < boxes.length; i++) {
            int x = i * boxSize;
            int y = 0;
            this.drawBox(canvas,
                         x, y,
                         0, i,
                         boxSize,
                         boxes[i],
                         null,
                         highlight,
                         suppressNotesLists,
                         false);
        }

        if (highlight != null) {
            int col = highlight.getCol();
            if (col >= 0 && col < boxes.length) {
                drawBoxOutline(
                    canvas, col * boxSize, 0, boxSize, currentLetterBox
                );
            }
        }

        return bitmap;
    }

    /**
     * Board position of the point
     *
     * Not checked if in bounds, if outsize main board, check with
     * getUnpinnedPosition to see if it's a position in the pinned
     * display
     */
    public Position findPosition(Point p) {
        int boxSize = getBoxSize();

        int col = p.x / boxSize;
        int row = p.y / boxSize;

        return new Position(row, col);
    }

    /**
     * Convert a position to true position on board
     *
     * If position not on board but in the display of the pinned clue,
     * return the cell on the main board corresponding to this cell on
     * the pinned clue.
     *
     * Else return null;
     */
    public Position getUnpinnedPosition(Position pos) {
        Zone pinnedZone = getPinnedZone();
        if (pinnedZone == null)
            return null;

        if (pos.getRow() != getPinnedRow())
            return null;

        int col = pos.getCol() - getPinnedCol();

        if (col >= 0 && col < pinnedZone.size())
            return pinnedZone.getPosition(col);
        else
            return null;
    }

    public int findBoxNoScale(Point p) {
        int boxSize =  (int) (BASE_BOX_SIZE_INCHES * dpi);
        LOG.info("DPI "+dpi+" scale "+ scale +" box size "+boxSize);
        return p.x / boxSize;
    }

    public Point findPointBottomRight(Position p) {
        int boxSize = getBoxSize();
        int x = (p.getCol() * boxSize) + boxSize;
        int y = (p.getRow() * boxSize) + boxSize;

        return new Point(x, y);
    }

    public Point findPointBottomRight(Word word) {
        Zone zone = word.getZone();

        if (zone == null || zone.isEmpty())
            return null;

        // for now assume that last box is bottom right
        Position p = zone.getPosition(zone.size() - 1);

        int boxSize = getBoxSize();
        int x = (p.getCol() * boxSize) + boxSize;
        int y = (p.getRow() * boxSize) + boxSize;

        return new Point(x, y);
    }

    public Point findPointTopLeft(Position p) {
        int boxSize = getBoxSize();
        int x = p.getCol() * boxSize;
        int y = p.getRow() * boxSize;

        return new Point(x, y);
    }

    public Point findPointTopLeft(Word word) {
        // for now, assume first zone position is top left
        Zone zone = word.getZone();
        if (zone == null || zone.isEmpty())
            return null;
        return findPointTopLeft(zone.getPosition(0));
    }

    public float fitTo(int width, int height) {
        return fitTo(width, height, getFullWidth(), getFullHeight());
    }

    public float fitTo(
        int width, int height, int numBoxesWidth, int numBoxesHeight
    ) {
        this.bitmap = null;
        float newScaleWidth = calculateScale(width, numBoxesWidth);
        float newScaleHeight = calculateScale(height, numBoxesHeight);
        setScale(Math.min(newScaleWidth, newScaleHeight));
        return getScale();
    }

    public float fitWidthTo(int width, int numBoxes) {
        this.bitmap = null;
        setScale(calculateScale(width, numBoxes));
        return getScale();
    }

    public float zoomIn() {
        this.bitmap = null;
        this.scale = scale * 1.25F;
        if(scale > this.getMaxScale()){
            this.scale = this.getMaxScale();
        }
        return scale;
    }

    public float zoomOut() {
        this.bitmap = null;
        this.scale = scale / 1.25F;
        if(scale < this.getMinScale()){
            scale = this.getMinScale();
        }
        return scale;
    }

    public float zoomReset() {
        this.bitmap = null;
        this.scale = 1.0F;
        return scale;
    }

    public float zoomInMax() {
        this.bitmap = null;
        this.scale = getMaxScale();

        return scale;
    }

    /**
     * Dynamic content description describing currently selected box on board
     *
     * @param baseDescription short description of what the board is
     * displaying
     */
    public String getContentDescription(CharSequence baseDescription) {
        Box curBox = board.getCurrentBox();
        return getContentDescription(baseDescription, curBox, true);
    }

    /**
     * Dynamic content description describing first box in word
     *
     * @param baseDescription short description of what the board is
     * displaying
     * @return description or a null description if no first box
     */
    public String getContentDescription(
        Word word, CharSequence baseDescription
    ) {
        if (word == null || word.getLength() <= 0) {
            Context context = ForkyzApplication.getInstance();
            return context.getString(
                R.string.cur_box_none_selected, baseDescription
            );
        }

        Position firstBoxPos = word.getZone().getPosition(0);
        Position curPos = board.getHighlightLetter();
        boolean selected = firstBoxPos.equals(curPos);
        Box firstBox = board.getPuzzle().checkedGetBox(firstBoxPos);

        return getContentDescription(baseDescription, firstBox, selected);
    }

    /**
     * Dynamic content description describing currently selected box
     *
     * @param baseDescription short description of what the board is
     * displaying
     * @param boxes
     * @param index which of the boxes to get description for, will return a
     * "no selection" string if index is out of range
     * @param hasCursor true if the box has the cursor
     */
    public String getContentDescription(
        CharSequence baseDescription, Box[] boxes, int index, boolean hasCursor
    ) {
        if (index < 0 || index >= boxes.length) {
            Context context = ForkyzApplication.getInstance();
            return context.getString(
                R.string.cur_box_none_selected, baseDescription
            );
        } else {
            Box curBox = boxes[index];
            return getContentDescription(baseDescription, curBox, hasCursor);
        }
    }

    /**
     * Dynamic content description for the given box
     *
     * @param baseDescription short description of the box
     * @param box the box to describe
     * @param hasCursor if the current box has the cursor
     */
    public String getContentDescription(
        CharSequence baseDescription, Box box, boolean hasCursor
    ) {
        Context context = ForkyzApplication.getInstance();

        String response = box.isBlank()
            ? context.getString(R.string.cur_box_blank)
            : String.valueOf(box.getResponse());

        String clueNumber = box.getClueNumber();
        String number = box.hasClueNumber()
            ? context.getString(R.string.cur_box_number, clueNumber)
            : context.getString(R.string.cur_box_no_number);

        Puzzle puz = board == null ? null : board.getPuzzle();

        String clueInfo = "";
        if (puz != null) {
            for (ClueID cid : box.getIsPartOfClues()) {
                Clue clue = puz.getClue(cid);

                String partOfClueNumber = clue.getClueNumber();
                if (partOfClueNumber == null)
                    partOfClueNumber = "";

                clueInfo += context.getString(
                    R.string.cur_box_clue_info,
                    cid.getListName(),
                    partOfClueNumber,
                    cid.getIndex()
                );
            }
        }

        String circled = context.getString(
            box.isCircled()
                ? R.string.cur_box_circled
                : R.string.cur_box_not_circled
        );

        String barTop = context.getString(
            box.isBarredTop()
                ? R.string.cur_box_bar_top
                : R.string.cur_box_no_bar_top
        );

        String barRight = context.getString(
            box.isBarredRight()
                ? R.string.cur_box_bar_right
                : R.string.cur_box_no_bar_right
        );

        String barBottom = context.getString(
            box.isBarredBottom()
                ? R.string.cur_box_bar_bottom
                : R.string.cur_box_no_bar_bottom
        );

        String barLeft = context.getString(
            box.isBarredLeft()
                ? R.string.cur_box_bar_left
                : R.string.cur_box_no_bar_left
        );

        String error = context.getString(
            highlightError(box, hasCursor)
                ? R.string.cur_box_error
                : R.string.cur_box_no_error
        );

       String contentDesc = context.getString(
            R.string.cur_box_desc,
            baseDescription,
            response, clueInfo, number,
            circled, barTop, barRight, barBottom, barLeft,
            error
        );

        return contentDesc;
    }

    /**
     * Draw an individual box
     *
     * @param fullBoard whether to draw details that only make sense when the
     * full board can be seen.
     * @param suppressNotesLists set of lists to not draw notes from.
     * Empty set means draw notes from all lists, null means don't draw
     * any notes.
     */
    private void drawBox(Canvas canvas,
                         int x, int y,
                         int row, int col,
                         int boxSize,
                         Box box,
                         Word currentWord,
                         Position highlight,
                         Set<String> suppressNotesLists,
                         boolean fullBoard) {
        int numberTextSize = boxSize / 4;
        int miniNoteTextSize = boxSize / 2;
        int noteTextSize = Math.round(boxSize * 0.6F);
        int letterTextSize = Math.round(boxSize * 0.7F);
        int barSize = boxSize / 12;
        int numberOffset = barSize;
        int textOffset = boxSize / 30;

        // scale paints
        numberText.setTextSize(numberTextSize);
        letterText.setTextSize(letterTextSize);
        red.setTextSize(letterTextSize);
        redHighlight.setTextSize(letterTextSize);
        white.setTextSize(letterTextSize);
        noteText.setTextSize(noteTextSize);
        miniNoteText.setTextSize(miniNoteTextSize);

        boolean isHighlighted
            = highlight.getCol() == col
                && highlight.getRow() == row;

        Paint outlineColor = isHighlighted ? currentLetterBox : blackLine;
        drawBoxOutline(canvas, x, y, boxSize, outlineColor);

        Rect r = new Rect(x + 1, y + 1, (x + boxSize) - 1, (y + boxSize) - 1);

        if (box == null) {
            canvas.drawRect(r, this.blackBox);
        } else {
            if (highlightError(box, isHighlighted))
                box.setCheated(true);

            drawBoxBackground(canvas, box, row, col, r, highlight, currentWord);

            // Bars before clue numbers to avoid obfuscating
            if (fullBoard)
                drawBoxBars(canvas, x, y, box, boxSize, barSize);

            drawBoxMarks(
                canvas, x, y, box, boxSize, numberOffset, numberText
            );
            if (fullBoard) {
                drawBoxFlags(
                    canvas, x, y, box,
                    boxSize, barSize, numberOffset, numberTextSize
                );
            }

            drawBoxCircle(canvas, x, y, box, boxSize);

            if (box.isBlank()) {
                if (suppressNotesLists != null) {
                    drawBoxNotes(
                        canvas, x, y, box,
                        boxSize, textOffset,
                        noteText, miniNoteText,
                        suppressNotesLists
                    );
                }
            } else {
                drawBoxLetter(
                    canvas, x, y,
                    box, row, col,
                    boxSize, textOffset, isHighlighted, currentWord
                );
            }
        }
    }

    private void drawBoxOutline(
        Canvas canvas, int x, int y, int boxSize, Paint color
    ) {
        // Draw left, top, right, bottom
        canvas.drawLine(x, y, x, y + boxSize, color);
        canvas.drawLine(x, y, x + boxSize, y, color);
        canvas.drawLine(x + boxSize, y, x + boxSize, y + boxSize, color);
        canvas.drawLine(x, y + boxSize, x + boxSize, y + boxSize, color);
    }

    private void drawBoxBackground(
        Canvas canvas, Box box, int row, int col,
        Rect boxRect, Position highlight, Word currentWord
    ) {
        // doesn't depend on current word (for BoxEditText)
        boolean isHighlighted
            = highlight.getCol() == col
                && highlight.getRow() == row;
        boolean highlightError = highlightError(box, isHighlighted);

        if (isHighlighted && !highlightError) {
            canvas.drawRect(boxRect, this.currentLetterHighlight);
        } else if (isHighlighted && highlightError) {
            canvas.drawRect(boxRect, this.redHighlight);
        } else if ((currentWord != null) && currentWord.checkInWord(row, col)) {
            canvas.drawRect(boxRect, this.currentWordHighlight);
        } else if (highlightError) {
            canvas.drawRect(boxRect, this.red);
        } else if (this.hintHighlight && box.isCheated()) {
            canvas.drawRect(boxRect, this.cheated);
        } else {
            if (!box.hasColor()) {
                canvas.drawRect(boxRect, this.white);
            } else {
                Paint paint = getRelativePaint(this.white, box.getColor());
                canvas.drawRect(boxRect, paint);
            }
        }
    }

    private void drawBoxBars(
        Canvas canvas, int x, int y, Box box,
        int boxSize, int barSize
    ) {
        if (box.isBarredLeft()) {
            Rect bar = new Rect(x, y, x + barSize, y + boxSize);
            canvas.drawRect(bar, this.blackBox);
        }

        if (box.isBarredTop()) {
            Rect bar = new Rect(x, y, x + boxSize, y + barSize);
            canvas.drawRect(bar, this.blackBox);
        }

        if (box.isBarredRight()) {
            Rect bar = new Rect(
                x + boxSize - barSize, y,
                x + boxSize, y + boxSize
            );
            canvas.drawRect(bar, this.blackBox);
        }

        if (box.isBarredBottom()) {
            Rect bar = new Rect(
                x, y + boxSize - barSize,
                x + boxSize, y + boxSize
            );
            canvas.drawRect(bar, this.blackBox);
        }
    }

    private void drawBoxMarks(
        Canvas canvas, int x, int y, Box box,
        int boxSize, int numberOffset, TextPaint numberText
    ) {
        if (box.hasClueNumber()) {
            String clueNumber = box.getClueNumber();
            drawHtmlText(
                canvas,
                clueNumber,
                x + numberOffset,
                y + numberOffset / 2,
                boxSize,
                numberText
            );
        }

        if (box.hasMarks()) {
            int markHeight = getTotalHeight(numberText);

            // 3x3 by guarantee of Box
            String[][] marks = box.getMarks();
            for (int row = 0; row < 3; row++) {
                int markY;
                switch (row) {
                case 1: // middle
                    markY = boxSize / 2 - markHeight / 2 - numberOffset / 2;
                    break;
                case 2: // bottom
                    markY = boxSize - numberOffset - markHeight;
                    break;
                default: // top
                    markY = numberOffset / 2;
                }

                for (int col = 0; col < 3; col++) {
                    if (marks[row][col] != null) {
                        int fullWidth = boxSize - 2 * numberOffset;
                        Layout.Alignment align;
                        switch (col) {
                        case 1: // centre
                            align = Layout.Alignment.ALIGN_CENTER;
                            break;
                        case 2: // right
                            align = Layout.Alignment.ALIGN_OPPOSITE;
                            break;
                        default: // left
                            align = Layout.Alignment.ALIGN_NORMAL;
                        }

                        drawHtmlText(
                            canvas,
                            marks[row][col],
                            x + numberOffset,
                            y + markY,
                            fullWidth,
                            align,
                            numberText
                        );
                    }
                }
            }
        }
    }

    private void drawBoxFlags(
        Canvas canvas, int x, int y, Box box,
        int boxSize, int barSize, int numberOffset, int numberTextSize
    ) {
        Puzzle puz = board.getPuzzle();

        boolean flagAcross = false;
        boolean flagDown = false;

        for (ClueID cid : box.getIsPartOfClues()) {
            if (box.isStartOf(cid) && puz.isFlagged(cid)) {
                if (isClueProbablyAcross(cid))
                    flagAcross = true;
                else
                    flagDown = true;

            }
        }

        if (flagDown) {
            String clueNumber = box.getClueNumber();
            int numDigits = clueNumber == null ? 0 : clueNumber.length();
            int numWidth = numDigits * numberTextSize / 2;
            Rect bar = new Rect(
                x + numberOffset + numWidth + barSize,
                y + 1 * barSize,
                x + boxSize - barSize,
                y + 2 * barSize
            );
            canvas.drawRect(bar, this.flag);
        }

        if (flagAcross) {
            Rect bar = new Rect(
                x + 1 * barSize,
                y + barSize + numberOffset + numberTextSize,
                x + 2 * barSize,
                y + boxSize - barSize
            );
            canvas.drawRect(bar, this.flag);
        }
    }

    private void drawBoxCircle(
        Canvas canvas, int x, int y, Box box, int boxSize
    ) {
        // Draw circle
        if (box.isCircled()) {
            canvas.drawCircle(x + (boxSize / 2) + 0.5F, y + (boxSize / 2) + 0.5F, (boxSize / 2) - 1.5F, blackCircle);
        }
    }

    private void drawBoxLetter(
        Canvas canvas, int x, int y,
        Box box, int row, int col,
        int boxSize, int textOffset, boolean isHighlighted, Word currentWord
    ) {
        TextPaint thisLetter = this.letterText;
        String letterString = box.isBlank()
            ? null
            : box.getResponse();

        if (letterString == null)
            return;

        if (highlightError(box, isHighlighted)) {
            boolean inCurrentWord =
                (currentWord != null) && currentWord.checkInWord(row, col);

            if (isHighlighted) {
                thisLetter = this.white;
            } else if (inCurrentWord) {
                thisLetter = this.redHighlight;
            }
        }

        float letterTextSize = letterText.getTextSize();

        if (letterString.length() > 1) {
            letterText.setTextSize(getIdealTextSize(
                letterString, letterText, boxSize
            ));
        }

        int yoffset = boxSize - textOffset - getTotalHeight(letterText);
        drawText(
            canvas,
            letterString,
            x + (boxSize / 2),
            y + yoffset,
            boxSize,
            thisLetter
        );

        thisLetter.setTextSize(letterTextSize);
    }

    private void drawBoxNotes(
        Canvas canvas, int x, int y, Box box,
        int boxSize, int textOffset,
        TextPaint noteText, TextPaint miniNoteText,
        Set<String> suppressNotesLists
    ) {
        String noteStringAcross = null;
        String noteStringDown = null;

        for (ClueID cid : box.getIsPartOfClues()) {
            if (suppressNotesLists.contains(cid.getListName()))
                continue;

            Note note = board.getPuzzle().getNote(cid);
            if (note == null)
                continue;

            String scratch = note.getScratch();
            if (scratch == null)
                continue;

            int pos = box.getCluePosition(cid);
            if (pos < 0 || pos >= scratch.length())
                continue;

            char noteChar = scratch.charAt(pos);
            if (noteChar == ' ')
                continue;

            if (isClueProbablyAcross(cid)) {
                noteStringAcross =
                    Character.toString(noteChar);
            } else {
                noteStringDown =
                    Character.toString(noteChar);
            }
        }

        float[] mWidth = new float[1];
        letterText.getTextWidths("M", mWidth);
        float letterTextHalfWidth = mWidth[0] / 2;

        if (noteStringAcross != null && noteStringDown != null) {
            if (noteStringAcross.equals(noteStringDown)) {
                // Same scratch letter in both directions
                // Align letter with across and down answers
                int noteTextHeight = getTotalHeight(noteText);
                drawText(
                    canvas,
                    noteStringAcross,
                    x + (int)(boxSize - letterTextHalfWidth),
                    y + boxSize - noteTextHeight - textOffset,
                    boxSize,
                    noteText
                );
            } else {
                // Conflicting scratch letters
                // Display both letters side by side
                int noteTextHeight = getTotalHeight(miniNoteText);
                drawText(
                    canvas,
                    noteStringAcross,
                    x + (int)(boxSize * 0.05 + letterTextHalfWidth),
                    y + boxSize - noteTextHeight - textOffset,
                    boxSize,
                    miniNoteText
                );
                int yoffset =
                    boxSize
                    - noteTextHeight
                    + (int) miniNoteText.ascent();
                drawText(
                    canvas,
                    noteStringDown,
                    x + (int)(boxSize - letterTextHalfWidth),
                    y + yoffset,
                    boxSize,
                    miniNoteText
                );
            }
        } else if (noteStringAcross != null) {
            // Across scratch letter only - display in bottom left
            int noteTextHeight = getTotalHeight(noteText);
            drawText(
                canvas,
                noteStringAcross,
                x + (boxSize / 2),
                y + boxSize - noteTextHeight - textOffset,
                boxSize,
                noteText
            );
        } else if (noteStringDown != null) {
            // Down scratch letter only - display in bottom left
            int noteTextHeight = getTotalHeight(noteText);
            drawText(
                canvas,
                noteStringDown,
                x + (int)(boxSize - letterTextHalfWidth),
                y + boxSize - noteTextHeight - textOffset,
                boxSize,
                noteText
            );
        }
    }

    /**
     * Estimate general direction of clue
     *
     * Bias towards across if unsure
     */
    private boolean isClueProbablyAcross(ClueID cid) {
        Puzzle puz = board == null ? null : board.getPuzzle();
        if (puz == null)
            return true;

        Clue clue = puz.getClue(cid);
        Zone zone = (clue == null) ? null : clue.getZone();
        if (zone == null || zone.size() <= 1)
            return true;

        Position pos0 = zone.getPosition(0);
        Position pos1 = zone.getPosition(1);

        return pos1.getCol() > pos0.getCol();
    }

    private boolean highlightError(Box box, boolean hasCursor) {
        boolean showErrors = board != null && (
            this.board.isShowErrorsGrid()
            || (this.board.isShowErrorsCursor() && hasCursor)
        );

        return showErrors
            && !box.isBlank()
            && box.hasSolution()
            && !Objects.equals(box.getSolution(), box.getResponse());
    }

    /**
     * Return a new paint based on color
     *
     * For use when "inverting" a color to appear on the board. Relative
     * vs. a pure white background is the pure color. Vs. a pure black
     * background in the inverted color. Somewhere in between is
     * somewhere in between.
     *
     * @param base the standard background color
     * @param color 24-bit 0x00rrggbb "pure" color
     */
    private Paint getRelativePaint(Paint base, int pureColor) {
        int baseCol = base.getColor();

        // the android color library is compatible with 0x00rrggbb
        int mixedR = mixColors(Color.red(baseCol), Color.red(pureColor));
        int mixedG = mixColors(Color.green(baseCol), Color.green(pureColor));
        int mixedB = mixColors(Color.blue(baseCol), Color.blue(pureColor));

        Paint mixedPaint = new Paint(base);
        mixedPaint.setColor(Color.rgb(mixedR, mixedG, mixedB));

        return mixedPaint;
    }

    /**
     * Tint a 0-255 pure color against a base
     *
     * See getRelativePaint
     */
    private int mixColors(int base, int pure) {
        double baseBias = base / 255.0;
        return (int)(
            (baseBias * pure) + ((1- baseBias) * (255 - pure))
        );
    }

    private static void drawText(
        Canvas canvas,
        CharSequence text,
        int x, int  y, int width,
        TextPaint style
    ) {
        drawText(
            canvas, text, x, y, width, Layout.Alignment.ALIGN_NORMAL, style
        );
    }

    private static void drawText(
        Canvas canvas,
        CharSequence text,
        int x, int  y, int width, Layout.Alignment align,
        TextPaint style
    ) {
        // with some help from:
        // https://stackoverflow.com/a/41870464
        StaticLayout staticLayout
            = versionUtils.getStaticLayout(text, style, width, align);
        canvas.save();
        canvas.translate(x, y);
        staticLayout.draw(canvas);
        canvas.restore();
    }

    /**
     * Calculate text size to avoid overflow
     *
     * See how much space it would be, recommend a smaller version if
     * needed.
     */
    private static int getIdealTextSize(
        CharSequence text, TextPaint style, int width
    ) {
        float desiredWidth = StaticLayout.getDesiredWidth(text, style);
        float styleSize = style.getTextSize();
        if (desiredWidth > width) {

            return (int) ((width / desiredWidth) * styleSize);
        } else {
            return (int) styleSize;
        }
    }

    private static void drawHtmlText(
        Canvas canvas, String text, int x, int y, int width, TextPaint style
    ) {
        drawText(canvas, HtmlCompat.fromHtml(text, 0), x, y, width, style);
    }

    private static void drawHtmlText(
        Canvas canvas, String text,
        int x, int y, int width, Layout.Alignment align,
        TextPaint style
    ) {
        drawText(
            canvas, HtmlCompat.fromHtml(text, 0), x, y, width, align, style
        );
    }

    private float calculateScale(int numPixels, int numBoxes) {
        double density = (double) dpi * (double) BASE_BOX_SIZE_INCHES;
        return (float) ((double) numPixels / (double) numBoxes / density);
    }

    private int getTotalHeight(TextPaint style) {
        return (int) Math.ceil(
            - style.ascent()
            + DESCENT_FUDGE_FACTOR * style.descent()
        );
    }

    private void drawImages(Canvas canvas) {
        Puzzle puz = (board == null) ? null : board.getPuzzle();
        if (puz == null)
            return;

        int boxSize = getBoxSize();

        for (PuzImage image : puz.getImages()) {
            Object tag = image.getTag();
            if (tag == null || !(tag instanceof Bitmap))
                tagImageWithBitmap(image);

            tag = image.getTag();
            if (tag instanceof Bitmap) {
                Bitmap bmp = (Bitmap) tag;
                int startx = image.getCol() * boxSize;
                int starty = image.getRow() * boxSize;
                int endx = startx + image.getWidth() * boxSize;
                int endy = starty + image.getHeight() * boxSize;

                canvas.drawBitmap(
                    bmp,
                    new Rect(0, 0, bmp.getWidth(), bmp.getHeight()),
                    new Rect(startx, starty, endx, endy),
                    null
                );
            }
        }
    }

    private void tagImageWithBitmap(PuzImage image) {
        String url = image.getURL();
        if (url.substring(0, 5).equalsIgnoreCase("data:")) {
            int start = url.indexOf(",") + 1;
            if (start > 0) {
                byte[] data = Base64.decode(
                    url.substring(start), Base64.DEFAULT
                );
                Bitmap imgBmp
                    = BitmapFactory.decodeByteArray(data, 0, data.length);
                image.setTag(imgBmp);
            }
        }
    }

    /**
     * Refresh the pinned clue (or draw)
     *
     * Refresh parts in current or reset word, unless renderAll.
     *
     * @param canvas to draw on (assumed large enough)
     * @param boxSize size of box in pixels
     * @param reset a word (not current) to refresh
     * @param renderAll whether to refresh no matter what
     */
    private void drawPinnedClue(
        Canvas canvas, Word reset, boolean renderAll,
        Set<String> suppressNotesLists
    ) {
        Puzzle puz = this.board.getPuzzle();
        Box[][] boxes = this.board.getBoxes();
        int boxSize = getBoxSize();
        Position highlight = board.getHighlightLetter();

        if (!puz.hasPinnedClueID())
            return;

        Zone pinnedZone = getPinnedZone();
        if (pinnedZone == null)
            return;

        Word currentWord = this.board.getCurrentWord();

        int pinnedRow = getPinnedRow();
        int pinnedCol = getPinnedCol();

        int y =  pinnedRow * boxSize;

        for (int i = 0; i < pinnedZone.size(); i++) {
            Position pos = pinnedZone.getPosition(i);
            if (!renderAll) {
                boolean inCur = currentWord.checkInWord(pos);
                boolean inReset
                    = reset != null && reset.checkInWord(pos);
                if (!inCur && !inReset)
                    continue;
            }

            int x = (pinnedCol + i) * boxSize;
            int row = pos.getRow();
            int col = pos.getCol();

            this.drawBox(
                canvas,
                x, y, row, col,
                boxSize,
                boxes[row][col],
                currentWord, highlight,
                suppressNotesLists,
                true
            );
        }

        // draw highlight outline again as it will have been overpainted
        if (highlight != null) {
            int idx = pinnedZone.indexOf(highlight);
            if (idx > -1) {
                int x = (pinnedCol + idx) * boxSize;
                drawBoxOutline(canvas, x, y, boxSize, currentLetterBox);
            }
        }
    }

    /**
     * Row on which pinned word is rendered
     *
     * Or -1 if nothing pinned
     */
    private int getPinnedRow() {
        Puzzle puz = this.board.getPuzzle();
        return puz.hasPinnedClueID() ? puz.getHeight() + 1 : -1;
    }

    /**
     * Col of first box of pinned word
     *
     * Or -1 if nothing pinned
     */
    private int getPinnedCol() {
        Zone pinnedZone = getPinnedZone();
        return pinnedZone == null
            ? -1
            : (getFullWidth() - pinnedZone.size()) / 2;
    }

    /**
     * Make sure bitmap field has a bitmap
     *
     * @return true if a new (blank) bitmap created, else old one used
     */
    private boolean initialiseBitmap() {
        if (bitmap != null)
            return false;

        int boxSize = getBoxSize();
        int width = getFullWidth();
        int height = getFullHeight();

        bitmap = Bitmap.createBitmap(
            width * boxSize, height * boxSize, Bitmap.Config.RGB_565
        );
        bitmap.eraseColor(Color.BLACK);

        return true;
    }

    private int getFullWidth() {
        Puzzle puz = this.board.getPuzzle();
        int width = puz.getWidth();

        if (puz.hasPinnedClueID()) {
            Zone pinnedZone = getPinnedZone();
            if (pinnedZone != null)
                width = Math.max(width, pinnedZone.size());
        }

        return width;
    }

    private int getFullHeight() {
        Puzzle puz = this.board.getPuzzle();
        int height = puz.getHeight();

        if (puz.hasPinnedClueID())
            height += 2;

        return height;
    }

    /**
     * Refresh board (current word) on canvas or draw all
     *
     * @param canvas canvas to draw on
     * @param boxSize the size of a box
     * @param word (that isn't current) that also needs refreshing
     * @param renderAll whether to just draw the whole board anyway
     * @param suppressNotesLists the notes lists not to draw (null means
     * draw none, empty means draw all)
     */
    private void drawBoardBoxes(
        Canvas canvas,
        Word reset, boolean renderAll, Set<String> suppressNotesLists
    ) {
        Puzzle puz = board.getPuzzle();
        Box[][] boxes = board.getBoxes();
        int boxSize = getBoxSize();
        int width = puz.getWidth();
        int height = puz.getHeight();
        Position highlight = board.getHighlightLetter();
        Word currentWord = this.board.getCurrentWord();

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (!renderAll) {
                    boolean inCur = currentWord.checkInWord(row, col);
                    boolean inReset
                        = reset != null && reset.checkInWord(row, col);
                    if (!inCur && !inReset) {
                        continue;
                    }
                }

                int x = col * boxSize;
                int y = row * boxSize;
                this.drawBox(
                    canvas,
                    x, y, row, col,
                    boxSize,
                    boxes[row][col],
                    currentWord, highlight,
                    suppressNotesLists,
                    true
                );
            }
        }

        // draw highlight outline again as it will have been overpainted
        if (highlight != null) {
            int curX = highlight.getCol() * boxSize;
            int curY = highlight.getRow() * boxSize;
            drawBoxOutline(canvas, curX, curY, boxSize, currentLetterBox);
        }
    }

    private Zone getPinnedZone() {
        Puzzle puz = this.board.getPuzzle();
        Clue pinnedClue = puz.getClue(puz.getPinnedClueID());
        return pinnedClue == null
            ? null
            : pinnedClue.getZone();
    }

    /**
     * The size of a box in pixels according to current scale
     */
    private int getBoxSize() {
        int boxSize = (int) (BASE_BOX_SIZE_INCHES * dpi * scale);
        if (boxSize == 0) {
            boxSize = (int) (BASE_BOX_SIZE_INCHES * dpi * 0.25F);
        }
        return boxSize;
    }
}

