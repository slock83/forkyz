package app.crossword.yourealwaysbe;

import java.util.Collections;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.DialogFragment;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.Note;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.Zone;
import app.crossword.yourealwaysbe.util.KeyboardManager;
import app.crossword.yourealwaysbe.view.BoardEditText.BoardEditFilter;
import app.crossword.yourealwaysbe.view.BoardEditText;
import app.crossword.yourealwaysbe.view.BoardEditView.BoardClickListener;
import app.crossword.yourealwaysbe.view.BoardWordEditView;
import app.crossword.yourealwaysbe.view.ForkyzKeyboard;
import app.crossword.yourealwaysbe.view.ScrollingImageView.Point;
import app.crossword.yourealwaysbe.view.ScrollingImageView;

public class NotesActivity extends PuzzleActivity {
    private static final Logger LOG = Logger.getLogger(
        NotesActivity.class.getCanonicalName()
    );

    /**
     * Start with intent extras Clue ID to note for, omit if puzzle
     * notes.
     */
    public static final String CLUE_NOTE_LISTNAME = "clueNoteListName";
    public static final String CLUE_NOTE_INDEX = "clueNoteIndex";

    private static final String TRANSFER_RESPONSE_REQUEST_KEY
        = "transferResponseRequest";

    private enum TransferResponseRequest {
        SCRATCH_TO_BOARD,
        ANAGRAM_SOL_TO_BOARD,
        BOARD_TO_SCRATCH,
        BOARD_TO_ANAGRAM_SOL
    }

    protected KeyboardManager keyboardManager;
    private TextView clueLine;
    private TextView boardViewLabel;
    private BoardWordEditView boardView;
    private EditText notesBox;
    private BoardEditText scratchView;
    private BoardEditText anagramSourceView;
    private BoardEditText anagramSolView;
    private CheckBox flagClue;

    private Random rand = new Random();

    private int numAnagramLetters = 0;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // just here to note the only menu item is handled by parent
        return super.onOptionsItemSelected(item);
    }

    /**
     * Create the activity
     *
     * This only sets up the UI widgets. The set up for the current
     * puzzle/board is done in onResume as these are held by the
     * application and may change while paused!
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        utils.holographic(this);
        utils.finishOnHomeButton(this);

        Playboard board = getBoard();
        Puzzle puz = getPuzzle();

        if (board == null || puz == null) {
            LOG.info("NotesActivity resumed but no Puzzle selected, finishing.");
            finish();
            return;
        }

        setContentView(R.layout.notes);

        clueLine = (TextView) this.findViewById(R.id.clueLine);
        if (clueLine != null && clueLine.getVisibility() != View.GONE) {
            clueLine.setVisibility(View.GONE);
            clueLine
                = (TextView) utils
                    .onActionBarCustom(this, R.layout.clue_line_only)
                    .findViewById(R.id.clueLine);
        }

        int clueTextSize
            = getResources().getInteger(R.integer.clue_text_size);
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
            clueLine, 5, clueTextSize, 1, TypedValue.COMPLEX_UNIT_SP
        );

        boardViewLabel = this.findViewById(R.id.boardLab);
        boardView = this.findViewById(R.id.miniboard);
        boardView.setAllowOverScroll(false);
        boardView.addBoardClickListener(new BoardClickListener() {
            @Override
            public void onClick(Position position, Word previousWord) {
                NotesActivity.this.keyboardManager.showKeyboard(boardView);
            }

            @Override
            public void onLongClick(Position position) {
                View focused = getWindow().getCurrentFocus();
                int id = focused.getId();
                if (id == R.id.scratchMiniboard) {
                    NotesActivity.this.executeTransferResponseRequest(
                        TransferResponseRequest.BOARD_TO_SCRATCH, true
                    );
                } else if (
                    id == R.id.anagramSolution || id == R.id.anagramSource
                ) {
                    NotesActivity.this.executeTransferResponseRequest(
                        TransferResponseRequest.BOARD_TO_ANAGRAM_SOL, true
                    );
                }
            }
        });
        this.boardView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP)
                    return NotesActivity.this.onMiniboardKeyUp(keyCode, event);
                else
                    return false;
            }
        });

        notesBox = (EditText) this.findViewById(R.id.notesBox);

        notesBox.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                NotesActivity.this.moveScratchToNote();
                return true;
            }
        });
        notesBox.setOnFocusChangeListener(
            new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean gainFocus) {
                    NotesActivity.this.onNotesBoxFocusChanged(gainFocus);
                }
            }
        );
        if (isPuzzleNotes()) {
            notesBox.setHint(getString(R.string.general_puzzle_notes));
        }

        scratchView = (BoardEditText) this.findViewById(R.id.scratchMiniboard);
        scratchView.setContextMenuListener(
            new ScrollingImageView.ClickListener() {
                @Override
                public void onTap(Point point) {
                    NotesActivity
                        .this
                        .keyboardManager
                        .showKeyboard(scratchView);
                }

                @Override
                public void onContextMenu(Point point) {
                    if (isClueOnBoard()) {
                        executeTransferResponseRequest(
                            TransferResponseRequest.SCRATCH_TO_BOARD, true
                        );
                    }
                }
            }
        );

        anagramSourceView = (BoardEditText) this.findViewById(R.id.anagramSource);
        anagramSolView = (BoardEditText) this.findViewById(R.id.anagramSolution);

        anagramSourceView.setContextMenuListener(
            new ScrollingImageView.ClickListener() {
                @Override
                public void onTap(Point point) {
                    NotesActivity
                        .this
                        .keyboardManager
                        .showKeyboard(anagramSourceView);
                }

                @Override
                public void onContextMenu(Point point) {
                    // reshuffle squares
                    int len = anagramSourceView.getLength();
                    for (int i = 0; i < len; i++) {
                        int j = rand.nextInt(len);
                        char ci = anagramSourceView.getResponse(i);
                        char cj = anagramSourceView.getResponse(j);
                        anagramSourceView.setResponse(i, cj);
                        anagramSourceView.setResponse(j, ci);
                    }
                }
            }
        );

        anagramSolView.setContextMenuListener(
            new ScrollingImageView.ClickListener() {
                @Override
                public void onTap(Point point) {
                    NotesActivity.this.keyboardManager.showKeyboard(anagramSolView);
                }

                @Override
                public void onContextMenu(Point point) {
                    if (isClueOnBoard()) {
                        executeTransferResponseRequest(
                            TransferResponseRequest.ANAGRAM_SOL_TO_BOARD, true
                        );
                    }
                }
            }
        );

        flagClue = (CheckBox) findViewById(R.id.flagClue);

        ForkyzKeyboard keyboardView
            = (ForkyzKeyboard) findViewById(R.id.keyboard);
        keyboardManager = new KeyboardManager(this, keyboardView, boardView);
        keyboardManager.showKeyboard(boardView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.notes_menu, menu);
        return true;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // for parity with onKeyUp
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
        case KeyEvent.KEYCODE_ESCAPE:
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
        case KeyEvent.KEYCODE_ESCAPE:
            if (!keyboardManager.handleBackKey())
                this.finish();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    public void onPause() {
        Puzzle puz = getPuzzle();

        EditText notesBox = (EditText) this.findViewById(R.id.notesBox);
        String text = notesBox.getText().toString();

        String scratch = scratchView.toString();
        String anagramSource = anagramSourceView.toString();
        String anagramSolution = anagramSolView.toString();

        Note note = new Note(scratch, text, anagramSource, anagramSolution);

        if (isPuzzleNotes()) {
            puz.setPlayerNote(note);
        } else {
            Clue clue = getNotesClue();
            puz.setNote(clue, note);
            puz.flagClue(clue, flagClue.isChecked());
        }

        super.onPause();

        keyboardManager.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (keyboardManager != null)
            keyboardManager.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (keyboardManager != null)
            keyboardManager.onDestroy();
    }

    private boolean onMiniboardKeyUp(int keyCode, KeyEvent event) {
        Word w = getBoard().getCurrentWord();
        Zone zone = (w == null) ? null : w.getZone();
        Position first = null;
        Position last = null;

        if (zone != null && !zone.isEmpty()) {
            first = zone.getPosition(0);
            last = zone.getPosition(zone.size() - 1);
        }

        switch (keyCode) {
        case KeyEvent.KEYCODE_MENU:
            return false;

        case KeyEvent.KEYCODE_DPAD_LEFT:
            getBoard().moveZoneBack(false);
            return true;

        case KeyEvent.KEYCODE_DPAD_RIGHT:
            getBoard().moveZoneForward(false);
            return true;

        case KeyEvent.KEYCODE_DEL:
            w = getBoard().getCurrentWord();

            getBoard().deleteLetter();

            Position p = getBoard().getHighlightLetter();

            if (!w.checkInWord(p) && first != null) {
                getBoard().setHighlightLetter(first);
            }

            return true;

        // space handled as any char
        }

        char c = Character.toUpperCase(event.getDisplayLabel());

        if (utils.isAcceptableCharacterResponse(c)) {
            getBoard().playLetter(c);

            Position p = getBoard().getHighlightLetter();
            int row = p.getRow();
            int col = p.getCol();

            if (!getBoard().getCurrentWord().equals(w)
                    || (getBoard().getBoxes()[row][col] == null)) {
                getBoard().setHighlightLetter(last);
            }

            return true;
        }

        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();

        Playboard board = getBoard();
        Puzzle puz = getPuzzle();

        if (board == null || puz == null) {
            LOG.info(
                "NotesActivity resumed but no Puzzle selected, "
                    + "finishing."
            );
            finish();
            // finish doesn't finish right away
            return;
        }

        Clue clue = getNotesClue();

        final int curWordLen = (clue == null || !clue.hasZone())
            ? Math.max(puz.getWidth(), puz.getHeight())
            : clue.getZone().size();

        if (curWordLen < 0) {
            LOG.info("NotesActivity needs a non-empty word");
            finish();
            return;
        }

        Note note;

        if (isPuzzleNotes()) {
            clueLine.setText(getString(R.string.player_notes));
            note = puz.getPlayerNote();
            boardViewLabel.setVisibility(View.GONE);
            boardView.setVisibility(View.GONE);
            flagClue.setVisibility(View.GONE);
        } else {
            boardView.setBoard(board);

            clueLine.setText(smartHtml(getLongClueText(clue)));
            note = puz.getNote(clue);
            flagClue.setChecked(puz.isFlagged(clue));
            int clueVisibility = isClueOnBoard() ? View.VISIBLE : View.GONE;
            boardViewLabel.setVisibility(clueVisibility);
            boardView.setVisibility(clueVisibility);
            flagClue.setVisibility(View.VISIBLE);
        }

        // set up and erase any previous data
        notesBox.setText("");

        // set lengths after fully set up
        scratchView.clear();
        anagramSourceView.clear();
        anagramSolView.clear();

        numAnagramLetters = 0;

        BoardEditFilter sourceFilter = new BoardEditFilter() {
            public boolean delete(char oldChar, int pos) {
                if (!BoardEditText.isBlank(oldChar)) {
                    numAnagramLetters -= 1;
                }
                return true;
            }

            public char filter(char oldChar, char newChar, int pos) {
                if (BoardEditText.isBlank(newChar)) {
                    if (!BoardEditText.isBlank(oldChar))
                        numAnagramLetters -= 1;
                    return newChar;
                } else {
                    if (!BoardEditText.isBlank(oldChar)) {
                        return newChar;
                    } else if (numAnagramLetters < curWordLen) {
                        numAnagramLetters++;
                        return newChar;
                    } else {
                        return '\0';
                    }
                }
            }
        };

        anagramSourceView.setFilters(new BoardEditFilter[]{sourceFilter});

        BoardEditFilter solFilter = new BoardEditFilter() {
            public boolean delete(char oldChar, int pos) {
                if (!BoardEditText.isBlank(oldChar)) {
                    for (int i = 0; i < curWordLen; i++) {
                        if (anagramSourceView.isBlank(i)) {
                            anagramSourceView.setResponse(i, oldChar);
                            return true;
                        }
                    }
                }
                return true;
            }

            public char filter(char oldChar, char newChar, int pos) {
                boolean changed
                    = NotesActivity.this.preAnagramSolResponse(pos, newChar);
                return changed ? newChar : '\0';
            }
        };

        anagramSolView.setFilters(new BoardEditFilter[]{solFilter});

        if (note != null) {
            notesBox.setText(note.getText());

            scratchView.setFromString(note.getScratch());

            String src = note.getAnagramSource();
            if (src != null) {
                anagramSourceView.setFromString(src);
                numAnagramLetters += anagramSourceView.getNumNonBlank();
            }

            String sol = note.getAnagramSolution();
            if (sol != null) {
                anagramSolView.setFromString(sol);
                numAnagramLetters +=  anagramSolView.getNumNonBlank();
            }
        }

        anagramSolView.setLength(curWordLen);
        scratchView.setLength(curWordLen);
        anagramSourceView.setLength(curWordLen);

        keyboardManager.onResume();
    }

    private Set<String> getSuppressNotesList() {
        boolean displayScratch = prefs.getBoolean("displayScratch", false);
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

    private void moveScratchToNote() {
        EditText notesBox = (EditText) this.findViewById(R.id.notesBox);
        String notesText = notesBox.getText().toString();

        String scratchText = scratchView.toString().trim();

        if (scratchText.length() > 0) {
            if (notesText.length() > 0)
                notesText += "\n";
            notesText += scratchText;

            scratchView.clear();
            notesBox.setText(notesText);
        }
    }

    private void copyBoxesToAnagramSol(Box[] boxes) {
        for (int i = 0; i < boxes.length; i++) {
            if (!boxes[i].isBlank()) {
                String newResponse = boxes[i].getResponse();
                if (newResponse != null && !newResponse.isEmpty()) {
                    char newChar = newResponse.charAt(0);
                    boolean allowed = preAnagramSolResponse(i, newChar);
                    if (allowed)
                        anagramSolView.setResponse(i, newChar);
                }
            }
        }
    }

    private boolean hasConflict(Box[] source,
                                Box[] dest,
                                boolean copyBlanks) {
        int length = Math.min(source.length, dest.length);
        for (int i = 0; i < length; i++) {
            if (
                (copyBlanks || !source[i].isBlank()) &&
                !dest[i].isBlank() &&
                !Objects.equals(
                    source[i].getResponse(), dest[i].getResponse()
                )
            ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Copies non-blank characters from response to the view
     */
    private void overlayBoxesOnBoardView(Box[] boxes,
                                         BoardEditText view) {
        int length = Math.min(boxes.length, view.getLength());
        for (int i = 0; i < length; i++) {
            if (!boxes[i].isBlank()) {
                view.setResponse(i, boxes[i].getResponse());
            }
        }
    }

    /**
     * Make arrangements for anagram letter to be played
     *
     * Changes source/sol boxes by moving required letters around.
     *
     * @return true if play of letter can proceed
     */
    private boolean preAnagramSolResponse(int pos, char newChar) {
        char oldChar = anagramSolView.getResponse(pos);
        int sourceLen = anagramSourceView.getLength();
        for (int i = 0; i < sourceLen; i++) {
            if (anagramSourceView.getResponse(i) == newChar) {
                anagramSourceView.setResponse(i, oldChar);
                return true;
            }
        }
        // if failed to find it in the source view, see if we can
        // find one to swap it with one in the solution
        int solLen = anagramSolView.getLength();
        for (int i = 0; i < solLen; i++) {
            if (anagramSolView.getResponse(i) == newChar) {
                anagramSolView.setResponse(i, oldChar);
                return true;
            }
        }
        return false;
    }

    /**
     * Transfer one board view to another
     *
     * Somewhat inelegant as a dialog confirmation has to be robust
     * against activity recreation, so it all gets funnelled through
     * here with little external state.
     *
     * The alternative is to copy-paste the dialog construction several
     * times. I'm not sure which is better.
     */
    private void executeTransferResponseRequest(
        TransferResponseRequest request,
        boolean confirmOverwrite
    ) {
        Playboard board = getBoard();
        if (board == null)
            return;

        boolean conflict = false;
        Box[] curWordBoxes = board.getCurrentWordBoxes();

        if (confirmOverwrite) {
            switch (request) {
            case SCRATCH_TO_BOARD:
                conflict = hasConflict(
                    scratchView.getBoxes(), curWordBoxes, true
                );
                break;
            case ANAGRAM_SOL_TO_BOARD:
                conflict = hasConflict(
                    anagramSolView.getBoxes(), curWordBoxes, true
                );
                break;
            case BOARD_TO_SCRATCH:
                conflict = hasConflict(
                    curWordBoxes, scratchView.getBoxes(), false
                );
                break;
            case BOARD_TO_ANAGRAM_SOL:
                conflict = false;
            }
        }

        if (conflict) {
            confirmAndExecuteTransferRequest(request);
        } else {
            switch (request) {
            case SCRATCH_TO_BOARD:
                board.setCurrentWord(scratchView.getBoxes());
                break;
            case ANAGRAM_SOL_TO_BOARD:
                board.setCurrentWord(anagramSolView.getBoxes());
                break;
            case BOARD_TO_SCRATCH:
                overlayBoxesOnBoardView(curWordBoxes, scratchView);
                break;
            case BOARD_TO_ANAGRAM_SOL:
                copyBoxesToAnagramSol(curWordBoxes);
                break;
            }
        }
    }

    private void confirmAndExecuteTransferRequest(
        TransferResponseRequest request
    ) {
        DialogFragment dialog = new TransferResponseRequestDialog();
        Bundle args = new Bundle();
        args.putSerializable(TRANSFER_RESPONSE_REQUEST_KEY, request);
        dialog.setArguments(args);
        dialog.show(
            getSupportFragmentManager(), "TransferResponseRequestDialog"
        );
    }

    public static class TransferResponseRequestDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final NotesActivity activity = (NotesActivity) getActivity();

            Bundle args = getArguments();
            TransferResponseRequest request
                = (TransferResponseRequest)
                    args.getSerializable(TRANSFER_RESPONSE_REQUEST_KEY);

            AlertDialog.Builder builder
                = new AlertDialog.Builder(activity);

            builder.setTitle(R.string.copy_conflict)
                .setMessage(R.string.transfer_overwrite_warning)
                .setPositiveButton(R.string.yes,
                                      new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        NotesActivity activity = ((NotesActivity) getActivity());
                        activity.executeTransferResponseRequest(request, false);
                    }
                })
                .setNegativeButton(R.string.no,
                                          new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

            return builder.create();
        }
    }

    /**
     * Force hide in-app keyboard if focus gained, hide soft keyboard if
     * focus lost
     */
    private void onNotesBoxFocusChanged(boolean gainFocus) {
        keyboardManager.onFocusNativeView(notesBox, gainFocus);
    }

    /**
     * Read clue id from intent, or null if not valid or not present
     *
     * Will return null if isPuzzleNotes
     */
    private Clue getNotesClue() {
        Playboard board = getBoard();
        Puzzle puz = getPuzzle();
        if (puz == null)
            return null;

        Intent intent = getIntent();
        String listName = intent.getStringExtra(CLUE_NOTE_LISTNAME);
        int index = intent.getIntExtra(CLUE_NOTE_INDEX, -1);

        if (listName == null || index < 0)
            return null;

        ClueID cid = new ClueID(listName, index);

        return puz.getClue(cid);
    }

    private boolean isPuzzleNotes() {
        return getNotesClue() == null;
    }

    /**
     * If the clue is on the board
     *
     * Returns false if isPuzzleNotes
     */
    private boolean isClueOnBoard() {
        Clue clue = getNotesClue();
        return clue != null && clue.hasZone();
    }
}
