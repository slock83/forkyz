package app.crossword.yourealwaysbe.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;

import androidx.core.content.ContextCompat;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Note;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Puzzle.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.view.ScrollingImageView.Point;

import java.util.logging.Logger;


public class PlayboardRenderer {
    private static final float BASE_BOX_SIZE_INCHES = 0.25F;
    private static final Logger LOG = Logger.getLogger(PlayboardRenderer.class.getCanonicalName());
    @SuppressLint("NewApi")
    private static final Typeface TYPEFACE_SEMI_BOLD_SANS = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                ? Typeface.create(Typeface.SANS_SERIF, 600, false) // semi-bold if available
                : Typeface.create("sans-serif", Typeface.BOLD); // or fallback to bold
    private final Paint blackBox = new Paint();
    private final Paint blackCircle = new Paint();
    private final Paint blackLine = new Paint();
    private final Paint cheated = new Paint();
    private final Paint currentLetterBox = new Paint();
    private final Paint currentLetterHighlight = new Paint();
    private final Paint currentWordHighlight = new Paint();
    private final Paint letterText = new Paint();
    private final Paint numberText = new Paint();
    private final Paint noteText = new Paint();
    private final Paint red = new Paint();
    private final Paint redHighlight = new Paint();
    private final Paint white = new Paint();
    private final Paint flag = new Paint();
    private Bitmap bitmap;
    private Playboard board;
    private float dpi;
    private float scale = 1.0F;
    private boolean hintHighlight;
    private int widthPixels;

    // colors are gotten from context
    public PlayboardRenderer(Playboard board,
                             float dpi, int widthPixels, boolean hintHighlight,
                             Context context) {
        this.dpi = dpi;
        this.widthPixels = widthPixels;
        this.board = board;
        this.hintHighlight = hintHighlight;

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

    public float getDeviceMaxScale(){
        float retValue;
        LOG.info("Board "+board.getPuzzle().getWidth() +" widthPixels "+widthPixels);
        // inches * pixels per inch * units
        retValue = 2.2F;
        float puzzleBaseSizeInInches
            = board.getPuzzle().getWidth() * BASE_BOX_SIZE_INCHES;
        //leave a 1/16th in gutter on the puzzle.
        float fitToScreen =  (dpi * (puzzleBaseSizeInInches + 0.0625F)) / dpi;

        if(retValue < fitToScreen){
            retValue = fitToScreen;
        }

        LOG.warning("getDeviceMaxScale "+retValue);
        return retValue;
    }

    public float getDeviceMinScale(){
        //inches * (pixels / pixels per inch);
        float retValue = 0.9F * ((dpi * BASE_BOX_SIZE_INCHES) / dpi);
        LOG.warning("getDeviceMinScale "+retValue);
        return retValue;
    }

    public void setScale(float scale) {
        if (scale > getDeviceMaxScale()) {
            scale = getDeviceMaxScale();
        } else if (scale < getDeviceMinScale()) {
            scale = getDeviceMinScale();
        } else if (String.valueOf(scale).equals("NaN")) {
            scale = 1.0f;
        }
        this.bitmap = null;
        this.scale = scale;
    }

    public float getScale()
    {
        return this.scale;
    }

    public Bitmap draw(Word reset,
                       boolean displayScratchAcross, boolean displayScratchDown) {
        try {
            Puzzle puz = this.board.getPuzzle();
            Box[][] boxes = this.board.getBoxes();
            int width = puz.getWidth();
            int height = puz.getHeight();
            boolean renderAll = reset == null;

            if (scale > getDeviceMaxScale()) {
                scale = getDeviceMaxScale();
            } else if (scale < getDeviceMinScale()) {
                scale = getDeviceMinScale();
            } else if (Float.isNaN(scale)) {
                scale = 1.0F;
            }

            int boxSize = (int) (BASE_BOX_SIZE_INCHES * dpi * scale);

            if (bitmap == null) {
                LOG.warning("New bitmap box size "+boxSize);
                bitmap = Bitmap.createBitmap(
                    width * boxSize, height * boxSize, Bitmap.Config.RGB_565
                );
                bitmap.eraseColor(Color.BLACK);
                renderAll = true;
            }

            Canvas canvas = new Canvas(bitmap);

            // board data

            Word currentWord = this.board.getCurrentWord();

            for (int row = 0; row < height; row++) {
                for (int col = 0; col < width; col++) {
                    if (!renderAll) {
                        if (!currentWord.checkInWord(row, col) && !reset.checkInWord(row, col)) {
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
                        currentWord, this.board.getHighlightLetter(),
                        displayScratchAcross, displayScratchDown,
                        true
                    );
                }
            }

            return bitmap;
        } catch (OutOfMemoryError e) {
            return bitmap;
        }
    }

    public Bitmap drawWord(boolean displayScratchAcross, boolean displayScratchDown) {
        Position[] word = this.board.getCurrentWordPositions();
        Box[] boxes = this.board.getCurrentWordBoxes();
        int boxSize = (int) (BASE_BOX_SIZE_INCHES * this.dpi * scale) ;
        Bitmap bitmap = Bitmap.createBitmap(word.length * boxSize, boxSize, Bitmap.Config.RGB_565);
        bitmap.eraseColor(Color.BLACK);

        Canvas canvas = new Canvas(bitmap);

        for (int i = 0; i < word.length; i++) {
            int x = i * boxSize;
            int y = 0;
            this.drawBox(
                canvas,
                x, y,
                word[i].getRow(), word[i].getCol(),
                boxSize,
                boxes[i],
                null,
                this.board.getHighlightLetter(),
                displayScratchAcross, displayScratchDown,
                false
            );
        }

        return bitmap;
    }

    public Bitmap drawBoxes(Box[] boxes,
                            Position highlight,
                            boolean displayScratchAcross,
                            boolean displayScratchDown) {
        if (boxes == null || boxes.length == 0) {
            return null;
        }

        int boxSize = (int) (BASE_BOX_SIZE_INCHES * this.dpi * scale);
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
                         displayScratchAcross, displayScratchDown,
                         false);
        }

        return bitmap;
    }


    public Position findBox(Point p) {
        int boxSize = (int) (BASE_BOX_SIZE_INCHES * dpi * scale);

        if (boxSize == 0) {
            boxSize = (int) (BASE_BOX_SIZE_INCHES * dpi * 0.25F);
        }

        int col = p.x / boxSize;
        int row = p.y / boxSize;

        return new Position(row, col);
    }

    public int findBoxNoScale(Point p) {
        int boxSize =  (int) (BASE_BOX_SIZE_INCHES * dpi);
        LOG.info("DPI "+dpi+" scale "+ scale +" box size "+boxSize);
        return p.x / boxSize;
    }

    public Point findPointBottomRight(Position p) {
        int boxSize = (int) (BASE_BOX_SIZE_INCHES * dpi * scale);
        int x = (p.getCol() * boxSize) + boxSize;
        int y = (p.getRow() * boxSize) + boxSize;

        return new Point(x, y);
    }

    public Point findPointBottomRight(Word word) {
        Position p = word.start;

        int acrossLen = word.across ? word.length : 1;
        int downLen = word.across ? 1 : word.length;

        int boxSize = (int) (BASE_BOX_SIZE_INCHES * dpi * scale);
        int x = (p.getCol() * boxSize) + acrossLen * boxSize;
        int y = (p.getRow() * boxSize) + downLen * boxSize;

        return new Point(x, y);
    }

    public Point findPointTopLeft(Position p) {
        int boxSize = (int) (BASE_BOX_SIZE_INCHES  * dpi * scale);
        int x = p.getCol() * boxSize;
        int y = p.getRow() * boxSize;

        return new Point(x, y);
    }

    public Point findPointTopLeft(Word word) {
        return findPointTopLeft(word.start);
    }

    public float fitTo(int shortDimension) {
        this.bitmap = null;
        // (pixels / boxes) / (pixels per inch / inches)
        Puzzle puz = this.board.getPuzzle();
        int numBoxes = Math.max(puz.getWidth(), puz.getHeight());
        return fitTo(shortDimension, numBoxes);
    }

    public float fitTo(int shortDimension, int numBoxes) {
        this.bitmap = null;
        double newScale = (double) shortDimension / (double) numBoxes / ((double) dpi * (double) BASE_BOX_SIZE_INCHES);
        LOG.warning("fitTo "+shortDimension+" dpi"+ dpi +" == "+newScale);
        if(newScale < getDeviceMinScale()){
            newScale = getDeviceMinScale();
        }
        this.scale = (float) newScale;
        return this.scale;
    }

    public float zoomIn() {
        this.bitmap = null;
        this.scale = scale * 1.25F;
        if(scale > this.getDeviceMaxScale()){
            this.scale = this.getDeviceMaxScale();
        }
        return scale;
    }

    public float zoomOut() {
        this.bitmap = null;
        this.scale = scale / 1.25F;
        if(scale < this.getDeviceMinScale()){
            scale = this.getDeviceMinScale();
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
        this.scale = getDeviceMaxScale();

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

        int clueNumber = box.getClueNumber();
        String number = drawClueNumber(box)
            ? context.getString(R.string.cur_box_number, clueNumber)
            : context.getString(R.string.cur_box_no_number);

        String acrossClueInfo;
        if (box.isPartOfAcross()) {
            acrossClueInfo = context.getString(
                R.string.cur_box_across_clue_info,
                box.getPartOfAcrossClueNumber()
            );
        } else {
            acrossClueInfo = context.getString(
                R.string.cur_box_no_across_clue_info
            );
        }

        String downClueInfo;
        if (box.isPartOfDown()) {
            downClueInfo = context.getString(
                R.string.cur_box_down_clue_info,
                box.getPartOfDownClueNumber()
            );
        } else {
            downClueInfo = context.getString(
                R.string.cur_box_no_down_clue_info
            );
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
            response, acrossClueInfo, downClueInfo, number,
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
     */
    private void drawBox(Canvas canvas,
                         int x, int y,
                         int row, int col,
                         int boxSize,
                         Box box,
                         Word currentWord,
                         Position highlight,
                         boolean displayScratchAcross, boolean displayScratchDown,
                         boolean fullBoard) {
        int numberTextSize = boxSize / 4;
        int miniNoteTextSize = boxSize / 2;
        int noteTextSize = Math.round(boxSize * 0.6F);
        int letterTextSize = Math.round(boxSize * 0.7F);
        int barSize = boxSize / 12;
        int numberOffset = barSize;

        // scale paints
        numberText.setTextSize(numberTextSize);
        letterText.setTextSize(letterTextSize);
        red.setTextSize(letterTextSize);
        redHighlight.setTextSize(letterTextSize);
        white.setTextSize(letterTextSize);

        boolean inCurrentWord = (currentWord != null) && currentWord.checkInWord(row, col);
        boolean isHighlighted
            = (highlight.getCol() == col) && (highlight.getRow() == row);

        Paint thisLetter;

        Paint boxColor = (((highlight.getCol() == col) && (highlight.getRow() == row)) && (currentWord != null))
                ? this.currentLetterBox : this.blackLine;

        // Draw left
        if ((col != (highlight.getCol() + 1)) || (row != highlight.getRow())) {
            canvas.drawLine(x, y, x, y + boxSize, boxColor);
        }
        // Draw top
        if ((row != (highlight.getRow() + 1)) || (col != highlight.getCol())) {
            canvas.drawLine(x, y, x + boxSize, y, boxColor);
        }
        // Draw right
        if ((col != (highlight.getCol() - 1)) || (row != highlight.getRow())) {
            canvas.drawLine(x + boxSize, y, x + boxSize, y + boxSize, boxColor);
        }
        // Draw bottom
        if ((row != (highlight.getRow() - 1)) || (col != highlight.getCol())) {
            canvas.drawLine(x, y + boxSize, x + boxSize, y + boxSize, boxColor);
        }

        Rect r = new Rect(x + 1, y + 1, (x + boxSize) - 1, (y + boxSize) - 1);

        if (box == null) {
            canvas.drawRect(r, this.blackBox);
        } else {
            boolean highlightError = highlightError(box, isHighlighted);

            if (highlightError)
                box.setCheated(true);

            // Background colors
            if (isHighlighted && !highlightError) {
                canvas.drawRect(r, this.currentLetterHighlight);
            } else if (isHighlighted && highlightError) {
                canvas.drawRect(r, this.redHighlight);
            } else if ((currentWord != null) && currentWord.checkInWord(row, col)) {
                canvas.drawRect(r, this.currentWordHighlight);
            } else if (highlightError) {
                canvas.drawRect(r, this.red);
            } else if (this.hintHighlight && box.isCheated()) {
                canvas.drawRect(r, this.cheated);
            } else {
                if (!box.hasColor()) {
                    canvas.drawRect(r, this.white);
                } else {
                    Paint paint = getRelativePaint(this.white, box.getColor());
                    canvas.drawRect(r, paint);
                }
            }

            // Bars before clue numbers to avoid obfuscating
            if (fullBoard) {
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

            if (drawClueNumber(box)) {
                int clueNumber = box.getClueNumber();
                canvas.drawText(
                    Integer.toString(clueNumber),
                    x + numberOffset,
                    y + numberTextSize + numberOffset,
                    this.numberText
                );

                Puzzle puz = board.getPuzzle();

                if (fullBoard) {
                    if (box.isDown()) {
                        if (puz.isFlagged(clueNumber, false)) {
                            int numDigits = Math.abs(clueNumber == 0
                                ? 1
                                : (int) Math.floor(Math.log10(clueNumber)) + 1
                            );
                            int numWidth = numDigits * numberTextSize / 2;
                            Rect bar = new Rect(
                                x + numberOffset + numWidth + barSize,
                                y + 1 * barSize,
                                x + boxSize - barSize,
                                y + 2 * barSize
                            );
                            canvas.drawRect(bar, this.flag);
                        }
                    }

                    if (box.isAcross()) {
                        if (puz.isFlagged(clueNumber, true)) {
                            Rect bar = new Rect(
                                x + 1 * barSize,
                                y + barSize + numberOffset + numberTextSize,
                                x + 2 * barSize,
                                y + boxSize - barSize
                            );
                            canvas.drawRect(bar, this.flag);
                        }
                    }
                }
            }

            // Draw circle
            if (box.isCircled()) {
                canvas.drawCircle(x + (boxSize / 2) + 0.5F, y + (boxSize / 2) + 0.5F, (boxSize / 2) - 1.5F, blackCircle);
            }

            thisLetter = this.letterText;
            String letterString = box.isBlank() ? null : Character.toString(box.getResponse());
            String noteStringAcross = null;
            String noteStringDown = null;

            if (highlightError) {
                if (isHighlighted) {
                    thisLetter = this.white;
                } else if (inCurrentWord) {
                    thisLetter = this.redHighlight;
                }
            }

            if (box.isBlank()) {
                if (displayScratchAcross && box.isPartOfAcross()) {
                    int clueNumber = box.getPartOfAcrossClueNumber();
                    Note note = board.getPuzzle().getNote(clueNumber, true);
                    if (note != null) {
                        String scratch = note.getScratch();
                        int pos = box.getAcrossPosition();
                        if (scratch != null && pos < scratch.length()) {
                            char noteChar = scratch.charAt(pos);
                            if (noteChar != ' ') noteStringAcross = Character.toString(noteChar);
                        }
                    }
                }
                if (displayScratchDown && box.isPartOfDown()) {
                    int clueNumber = box.getPartOfDownClueNumber();
                    Note note = board.getPuzzle().getNote(clueNumber, false);
                    if (note != null) {
                        String scratch = note.getScratch();
                        int pos = box.getDownPosition();
                        if (scratch != null && pos < scratch.length()) {
                            char noteChar = scratch.charAt(pos);
                            if (noteChar != ' ') noteStringDown = Character.toString(noteChar);
                        }
                    }
                }
            }


            if (letterString != null) {
                // Full size letter in normal font
                canvas.drawText(letterString,
                        x + (boxSize / 2),
                        y + (int)(boxSize / 2 - thisLetter.ascent() * 0.6),
                        thisLetter);
            } else {
                float[] mWidth = new float[1];
                letterText.getTextWidths("M", mWidth);
                float letterTextHalfWidth = mWidth[0] / 2;

                if (noteStringAcross != null && noteStringDown != null) {
                    if (noteStringAcross.equals(noteStringDown)) {
                        // Same scratch letter in both directions - align letter with across and
                        // down answers
                        noteText.setTextSize(noteTextSize);
                        canvas.drawText(noteStringAcross,
                                x + (int)(boxSize - letterTextHalfWidth),
                                y + (boxSize * 9 / 10),
                                noteText);
                    } else {
                        // Conflicting scratch letters - display both letters side by side
                        noteText.setTextSize(miniNoteTextSize);
                        canvas.drawText(noteStringAcross,
                                x + (int)(boxSize * 0.05 + letterTextHalfWidth),
                                y + (boxSize * 9 / 10),
                                noteText);
                        canvas.drawText(noteStringDown,
                                x + (int)(boxSize - letterTextHalfWidth),
                                y + (boxSize * 1 / 10) - noteText.ascent(),
                                noteText);
                    }
                } else if (noteStringAcross != null) {
                    // Across scratch letter only - display in bottom left
                    noteText.setTextSize(noteTextSize);
                    canvas.drawText(noteStringAcross,
                            x + (boxSize / 2),
                            y + (boxSize * 9 / 10),
                            noteText);
                } else if (noteStringDown != null) {
                    // Down scratch letter only - display in bottom right
                    noteText.setTextSize(noteTextSize);
                    canvas.drawText(noteStringDown,
                            x + (int)(boxSize - letterTextHalfWidth),
                            y + (boxSize - noteText.ascent())  / 2,
                            noteText);
                }
            }
        }
    }

    private boolean highlightError(Box box, boolean hasCursor) {
        boolean showErrors = this.board.isShowErrorsGrid()
            || (this.board.isShowErrorsCursor() && hasCursor);

        return showErrors
            && !box.isBlank()
            && box.hasSolution()
            && box.getSolution() != box.getResponse();
    }

    private boolean drawClueNumber(Box box) {
        return box.isAcross() || box.isDown();
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
}
