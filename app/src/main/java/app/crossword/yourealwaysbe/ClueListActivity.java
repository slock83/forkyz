package app.crossword.yourealwaysbe;

import java.util.Objects;
import java.util.logging.Logger;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.Zone;
import app.crossword.yourealwaysbe.util.KeyboardManager;
import app.crossword.yourealwaysbe.util.VoiceCommands.VoiceCommand;
import app.crossword.yourealwaysbe.view.BoardEditView.BoardClickListener;
import app.crossword.yourealwaysbe.view.BoardWordEditView;
import app.crossword.yourealwaysbe.view.ClueTabs;
import app.crossword.yourealwaysbe.view.ForkyzKeyboard;

public class ClueListActivity extends PuzzleActivity
                              implements ClueTabs.ClueTabsListener {
    private static final Logger LOG = Logger.getLogger(
        ClueListActivity.class.getCanonicalName()
    );

    private static final String PREF_SHOW_WORDS = "showWordsInClueList";

    private KeyboardManager keyboardManager;
    private BoardWordEditView boardView;
    private ClueTabs clueTabs;
    private View voiceButtonContainer;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.clue_list_menu_clue_notes) {
            launchClueNotes(getBoard().getClueID());
            return true;
        } else if (id == R.id.clue_list_menu_puzzle_notes) {
            launchPuzzleNotes();
            return true;
        } else if (id == R.id.clue_list_menu_show_words) {
            toggleShowWords();
            return true;
        }
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
            LOG.info(
                "ClueListActivity resumed but no Puzzle selected, "
                    + "finishing."
            );
            finish();
            return;
        }

        setContentView(R.layout.clue_list);

        this.boardView = this.findViewById(R.id.miniboard);
        this.boardView.setAllowOverScroll(false);

        this.boardView.addBoardClickListener(new BoardClickListener() {
            @Override
            public void onClick(Position position, Word previousWord) {
                displayKeyboard();
            }

            @Override
            public void onLongClick(Position position) {
                launchClueNotes(getBoard().getClueID());
            }
        });

        this.clueTabs = this.findViewById(R.id.clueListClueTabs);

        ForkyzKeyboard keyboard = (ForkyzKeyboard) findViewById(R.id.keyboard);
        keyboardManager = new KeyboardManager(this, keyboard, boardView);

        this.voiceButtonContainer
            = this.findViewById(R.id.voiceButtonContainer);
        this.findViewById(R.id.voiceButton).setOnClickListener(view -> {
            launchVoiceInput();
        });

        setupVoiceCommands();
    }

    @Override
    public void onResume() {
        super.onResume();

        Playboard board = getBoard();
        Puzzle puz = getPuzzle();

        if (board == null || puz == null) {
            LOG.info(
                "ClueListActivity resumed but no Puzzle selected, "
                    + "finishing."
            );
            finish();
            return;
        }

        boardView.setBoard(board);

        setupShowWordsMode();

        clueTabs.setBoard(board);
        clueTabs.addListener(this);
        clueTabs.listenBoard();
        clueTabs.refresh();

        keyboardManager.onResume();

        voiceButtonContainer.setVisibility(
            isButtonActivatesVoicePref() ? View.VISIBLE : View.GONE
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.clue_list_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean result = super.onPrepareOptionsMenu(menu);

        if (result) {
            menu.findItem(R.id.clue_list_menu_show_words)
                .setChecked(isShowWordsInClueList());
        }

        return result;
    }

    @Override
    public void onClueTabsClick(Clue clue, ClueTabs view) {
        Playboard board = getBoard();
        if (board == null)
            return;

        if (clue.hasZone()) {
            Word old = board.getCurrentWord();
            if (!Objects.equals(clue.getClueID(), board.getClueID()))
                board.jumpToClue(clue);
            displayKeyboard(old);
        }
    }

    @Override
    public void onClueTabsLongClick(Clue clue, ClueTabs view) {
        Playboard board = getBoard();
        if (board == null)
            return;
        if (!Objects.equals(clue.getClueID(), board.getClueID()))
            board.jumpToClue(clue);
        launchClueNotes(clue);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // for parity with onKeyUp
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
        case KeyEvent.KEYCODE_ESCAPE:
        case KeyEvent.KEYCODE_DPAD_UP:
        case KeyEvent.KEYCODE_DPAD_DOWN:
        case KeyEvent.KEYCODE_DPAD_LEFT:
        case KeyEvent.KEYCODE_DPAD_RIGHT:
        case KeyEvent.KEYCODE_DEL:
        case KeyEvent.KEYCODE_SPACE:
            return true;
        case KeyEvent.KEYCODE_VOLUME_DOWN:
            return isVolumeDownActivatesVoicePref();
        }

        char c = Character.toUpperCase(event.getDisplayLabel());
        if (Character.isLetterOrDigit(c))
            return true;

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
        case KeyEvent.KEYCODE_ESCAPE:
            onBackKey();
            return true;

        case KeyEvent.KEYCODE_DPAD_LEFT:
            onLeftKey();
            return true;

        case KeyEvent.KEYCODE_DPAD_RIGHT:
            onRightKey();
            return true;

        case KeyEvent.KEYCODE_DPAD_UP:
            onUpKey();
            return true;

        case KeyEvent.KEYCODE_DPAD_DOWN:
            onDownKey();
            return true;

        case KeyEvent.KEYCODE_DEL:
            onDeleteKey();
            return true;

        case KeyEvent.KEYCODE_SPACE:
            onSpaceKey();
            return true;

        case KeyEvent.KEYCODE_VOLUME_DOWN:
            if (isVolumeDownActivatesVoicePref()) {
                launchVoiceInput();
                return true;
            }
        }

        Playboard board = getBoard();
        char c = Character.toUpperCase(event.getDisplayLabel());

        if (board != null && Character.isLetterOrDigit(c)) {
            Word w = board.getCurrentWord();
            Position last = getCurrentLastPosition();

            board.playLetter(c);

            Position p = board.getHighlightLetter();
            int row = p.getRow();
            int col = p.getCol();

            if (!board.getCurrentWord().equals(w)
                    || (board.getBoxes()[row][col] == null)) {
                board.setHighlightLetter(last);
            }

            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();

        keyboardManager.onPause();

        if (clueTabs != null) {
            clueTabs.removeListener(this);
            clueTabs.unlistenBoard();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        keyboardManager.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        keyboardManager.onDestroy();
    }

    private void displayKeyboard() {
        keyboardManager.showKeyboard(boardView);
    }

    private void displayKeyboard(Word previousWord) {
        // only show keyboard if double click a word
        // hide if it's a new word
        Playboard board = getBoard();
        if (board != null) {
            Position newPos = board.getHighlightLetter();
            if ((previousWord != null) &&
                previousWord.checkInWord(newPos.getRow(), newPos.getCol())) {
                keyboardManager.showKeyboard(boardView);
            } else {
                keyboardManager.hideKeyboard();
            }
        }
    }

    /**
     * Find a clue to select and select it
     *
     * Searches for the first clue in the current page, else starts
     * flipping through pages in the given direction.
     *
     * Does nothing if no clue found.
     */
    private void selectFirstSelectableClue(boolean searchForwards) {
        boolean selectedClue = false;
        String startPage = clueTabs.getCurrentPageListName();
        do {
           selectedClue = selectFirstClue();
           if (!selectedClue) {
               if (searchForwards)
                   clueTabs.nextPage();
               else
                   clueTabs.prevPage();
            }
        } while (
            !selectedClue
            && !Objects.equals(startPage, clueTabs.getCurrentPageListName())
        );
    }

    /**
     * Selects first clue in cur list
     *
     * Returns false if no selectable clue
     */
    private boolean selectFirstClue() {
        Playboard board = getBoard();
        Puzzle puz = getPuzzle();
        switch (clueTabs.getCurrentPageType()) {
        case CLUES:
            String listName = clueTabs.getCurrentPageListName();
            int firstClue = puz.getClues(listName).getFirstZonedIndex();
            if (firstClue < 0) {
                return false;
            } else {
                board.jumpToClue(new ClueID(listName, firstClue));
                return true;
            }
        case HISTORY:
            for (ClueID cid : puz.getHistory()) {
                Clue clue = puz.getClue(cid);
                if (clue.hasZone()) {
                    board.jumpToClue(clue);
                    return true;
                }
            }
            return false;
        default:
            return false;
        }
    }

    private boolean isShowWordsInClueList() {
        return prefs.getBoolean(PREF_SHOW_WORDS, false);
    }

    private void toggleShowWords() {
        boolean showWords = !isShowWordsInClueList();

        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREF_SHOW_WORDS, showWords);
        editor.apply();

        setupShowWordsMode();
        invalidateOptionsMenu();
    }

    private void setupShowWordsMode() {
        if (isShowWordsInClueList()) {
            boardView.setIncognitoMode(true);
            clueTabs.setShowWords(true);
        } else {
            boardView.setIncognitoMode(false);
            clueTabs.setShowWords(false);
        }
    }

    private void setupVoiceCommands() {
        registerVoiceCommandAnswer();
        registerVoiceCommandLetter();
        registerVoiceCommandClear();
        registerVoiceCommandNumber();

        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_left),
            args -> { onLeftKey(); }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_right),
            args -> { onRightKey(); }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_up),
            args -> { onUpKey(); }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_down),
            args -> { onDownKey(); }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_back),
            args -> { onBackKey(); }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_next),
            args -> {
                clueTabs.nextPage();
                selectFirstClue();
            }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_previous),
            args -> {
                clueTabs.prevPage();
                selectFirstClue();
            }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_notes),
            args -> {
                Playboard board = getBoard();
                if (board != null)
                    launchClueNotes(board.getClueID());
            }
        ));
    }

    private void onLeftKey() {
        Playboard board = getBoard();
        if (board == null)
            return;

        Position first = getCurrentFirstPosition();

        if (!board.getHighlightLetter().equals(first)) {
            board.moveZoneBack(false);
        } else {
            clueTabs.prevPage();
            selectFirstSelectableClue(false);
        }
    }

    private void onRightKey() {
        Playboard board = getBoard();
        if (board == null)
            return;

        Position last = getCurrentLastPosition();

        if (!board.getHighlightLetter().equals(last)) {
            board.moveZoneForward(false);
        } else {
            clueTabs.nextPage();
            selectFirstSelectableClue(true);
        }
    }

    private void onUpKey() {
        Playboard board = getBoard();
        Puzzle puz = getPuzzle();
        if (board == null || puz == null)
            return;

        ClueID cid = board.getClueID();
        String curList = cid == null ? null : cid.getListName();
        int curClueIndex = cid == null ? -1 : cid.getIndex();

        if (curList != null && curClueIndex >= 0) {
            int prev = puz.getClues(curList)
                .getPreviousZonedIndex(curClueIndex, true);
            clueTabs.setForceSnap(true);
            board.jumpToClue(new ClueID(curList, prev));
            clueTabs.setForceSnap(false);
        } else {
            selectFirstSelectableClue(false);
        }
    }

    private void onDownKey() {
        Playboard board = getBoard();
        Puzzle puz = getPuzzle();
        if (board == null || puz == null)
            return;

        ClueID cid = board.getClueID();
        String curList = cid == null ? null : cid.getListName();
        int curClueIndex = cid == null ? -1 : cid.getIndex();

        if (curList != null && curClueIndex >= 0) {
            int next = puz.getClues(curList)
                .getNextZonedIndex(curClueIndex, true);
            clueTabs.setForceSnap(true);
            board.jumpToClue(new ClueID(curList, next));
            clueTabs.setForceSnap(false);
        } else {
            selectFirstSelectableClue(true);
        }
    }

    private void onDeleteKey() {
        Playboard board = getBoard();
        if (board == null)
            return;

        Word w = board.getCurrentWord();
        Position first = getCurrentFirstPosition();

        board.deleteLetter();

        Position p = board.getHighlightLetter();
        if (!w.checkInWord(p) && first != null) {
            board.setHighlightLetter(first);
        }
    }

    private void onSpaceKey() {
        Playboard board = getBoard();
        if (board == null)
            return;

        if(prefs.getBoolean("spaceChangesDirection", true))
            return;

        Word w = board.getCurrentWord();
        Position last = getCurrentLastPosition();

        board.playLetter(' ');

        Position curr = board.getHighlightLetter();
        int row = curr.getRow();
        int col = curr.getCol();

        if (!board.getCurrentWord().equals(w)
                || (board.getBoxes()[row][col] == null)) {
            board.setHighlightLetter(last);
        }
    }

    private void onBackKey() {
        if (!keyboardManager.handleBackKey())
            this.finish();
    }

    /**
     * First position of current word or null
     */
    private Position getCurrentFirstPosition() {
        Playboard board = getBoard();
        if (board == null)
            return null;

        Word w = board.getCurrentWord();
        Zone zone = (w == null) ? null : w.getZone();
        Position first = null;
        if (zone != null && !zone.isEmpty())
            first = zone.getPosition(0);
        return first;
    }

    /**
     * Last position of current word or null
     */
    private Position getCurrentLastPosition() {
        Playboard board = getBoard();
        if (board == null)
            return null;

        Word w = board.getCurrentWord();
        Zone zone = (w == null) ? null : w.getZone();
        Position last = null;
        if (zone != null && !zone.isEmpty())
            last = zone.getPosition(zone.size() - 1);
        return last;
    }
}
