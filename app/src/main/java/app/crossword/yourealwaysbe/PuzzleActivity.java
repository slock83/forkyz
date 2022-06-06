package app.crossword.yourealwaysbe;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.util.files.PuzHandle;
import app.crossword.yourealwaysbe.view.SpecialEntryDialog;

import java.util.logging.Logger;
import java.util.regex.Pattern;

public abstract class PuzzleActivity
        extends ForkyzActivity
        implements Playboard.PlayboardListener {

    private static final Logger LOG = Logger.getLogger("app.crossword.yourealwaysbe");

    public static final String SHOW_TIMER = "showTimer";
    public static final String PRESERVE_CORRECT
        = "preserveCorrectLettersInShowErrors";
    public static final String DONT_DELETE_CROSSING = "dontDeleteCrossing";

    private ImaginaryTimer timer;
    private Handler handler = new Handler(Looper.getMainLooper());

    private Runnable updateTimeTask = new Runnable() {
        public void run() {
            PuzzleActivity.this.onTimerUpdate();
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        startTimer();
    }

    private void startTimer() {
        Puzzle puz = getPuzzle();

        if (puz != null && puz.getPercentComplete() != 100) {
            ImaginaryTimer timer = new ImaginaryTimer(puz.getTime());
            setTimer(timer);
            timer.start();

            if (prefs.getBoolean(SHOW_TIMER, false)) {
                handler.post(updateTimeTask);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (prefs.getBoolean(SHOW_TIMER, false)) {
            handler.post(updateTimeTask);
        }

        Playboard board = getBoard();
        if (board != null) {
            boolean preserveCorrect = prefs.getBoolean(PRESERVE_CORRECT, true);
            board.setPreserveCorrectLettersInShowErrors(preserveCorrect);
            boolean noDelCrossing = prefs.getBoolean(DONT_DELETE_CROSSING, false);
            board.setDontDeleteCrossing(noDelCrossing);
        }
    }

    public void onPlayboardChange(
        boolean wholeBoard, Word currentWord, Word previousWord
    ) {
        Puzzle puz = getPuzzle();
        ImaginaryTimer timer = getTimer();

        if (puz != null &&
            puz.getPercentComplete() == 100 &&
            timer != null) {

            timer.stop();
            puz.setTime(timer.getElapsed());
            setTimer(null);
            Intent i = new Intent(PuzzleActivity.this,
                                  PuzzleFinishedActivity.class);
            this.startActivity(i);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.puzzle_menu_special_entry) {
            specialEntry();
            return true;
        } else if (id == R.id.puzzle_menu_share_clue) {
            shareClue(false);
        } else if (id == R.id.puzzle_menu_share_clue_response) {
            shareClue(true);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();

        Puzzle puz = getPuzzle();
        ImaginaryTimer timer = getTimer();

        if ((puz != null)) {
            if ((timer != null) && (puz.getPercentComplete() != 100)) {
                timer.stop();
                puz.setTime(timer.getElapsed());
                setTimer(null);
            }

            saveBoard();
        }

        Playboard board = getBoard();
        if (board != null)
            board.removeListener(this);
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        ImaginaryTimer timer = getTimer();
        if (timer != null)
            timer.start();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Puzzle puz = getPuzzle();
        if (puz != null && puz.getPercentComplete() != 100) {
            ImaginaryTimer timer = new ImaginaryTimer(puz.getTime());
            setTimer(timer);
            timer.start();
        }

        if (prefs.getBoolean(SHOW_TIMER, false)) {
            handler.post(updateTimeTask);
        }

        Playboard board = getBoard();
        if (board != null)
            board.addListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        ImaginaryTimer timer = getTimer();
        if (timer != null) {
            timer.stop();
        }
    }

    /**
     * Override if you want to update your UI based on the timer
     *
     * But still call super. Only called if the showTimer pref is true
     */
    protected void onTimerUpdate() {
        if (prefs.getBoolean(SHOW_TIMER, false)) {
            handler.postDelayed(updateTimeTask, 1000);
        }
    }

    protected Playboard getBoard(){
        return ForkyzApplication.getInstance().getBoard();
    }

    protected Puzzle getPuzzle() {
        Playboard board = getBoard();
        return (board == null) ? null : getBoard().getPuzzle();
    }

    protected void setTimer(ImaginaryTimer timer) {
        this.timer = timer;
    }

    protected ImaginaryTimer getTimer() {
        return timer;
    }

    protected PuzHandle getPuzHandle() {
        return ForkyzApplication.getInstance().getPuzHandle();
    }

    protected void saveBoard() {
        ForkyzApplication.getInstance().saveBoard();
    }

    protected String getLongClueText(Clue clue) {
        boolean showCount = prefs.getBoolean("showCount", false);

        if (clue == null)
            return getString(R.string.unknown_hint);

        int wordLen = clue.hasZone() ? clue.getZone().size() : -1;

        if (showCount && wordLen >= 0) {
            if (clue.hasClueNumber()) {
                return getString(
                    R.string.clue_format_long_with_count,
                    clue.getClueID().getListName(),
                    clue.getClueNumber(),
                    clue.getHint(),
                    wordLen
                );
            } else {
                return getString(
                    R.string.clue_format_long_no_num_with_count,
                    clue.getClueID().getListName(),
                    clue.getHint(),
                    wordLen
                );
            }
        } else {
            if (clue.hasClueNumber()) {
                return getString(
                    R.string.clue_format_long,
                    clue.getClueID().getListName(),
                    clue.getClueNumber(),
                    clue.getHint()
                );
            } else {
                return getString(
                    R.string.clue_format_long_no_num,
                    clue.getClueID().getListName(),
                    clue.getHint()
                );
            }
        }
    }

    protected void launchClueNotes(ClueID cid) {
        if (cid != null) {
            Intent i = new Intent(this, NotesActivity.class);
            i.putExtra(NotesActivity.CLUE_NOTE_LISTNAME, cid.getListName());
            i.putExtra(NotesActivity.CLUE_NOTE_INDEX, cid.getIndex());
            this.startActivity(i);
        } else {
            launchPuzzleNotes();
        }
    }

    protected void launchClueNotes(Clue clue) {
        if (clue != null)
            launchClueNotes(clue.getClueID());
        else
            launchPuzzleNotes();
    }

    protected void launchPuzzleNotes() {
        Intent i = new Intent(this, NotesActivity.class);
        this.startActivity(i);
    }

    protected void specialEntry() {
        SpecialEntryDialog dialog
            = new SpecialEntryDialog(getBoard());
        dialog.show(getSupportFragmentManager(), "SpecialEntryDialog");
    }

    protected void shareClue(boolean withResponse) {
        Playboard board = getBoard();
        Clue clue = (board == null) ? null : board.getClue();
        if (clue == null)
            return;

        Puzzle puz = board.getPuzzle();
        String source = (puz == null) ? null : puz.getSource();
        String title = (puz == null) ? null : puz.getTitle();
        String author = (puz == null) ? null : puz.getAuthor();
        Box[] response = board.getCurrentWordBoxes();

        String shareMessage = getShareMessage(
            clue, response, source, title, author, withResponse
        );

        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(
            sendIntent, getString(R.string.share_clue)
        ));
    }

    private String getShareMessage(
        Clue clue, Box[] response,
        String source, String title, String author,
        boolean withResponse
    ) {
        String clueText = getShareClueText(clue);
        String responseText = withResponse
            ? getShareResponseText(response)
            : null;

        if (source == null)
            source = "";
        if (title == null)
            title = "";
        if (author != null) {
            // add author if not already in title or caption
            // case insensitive trick:
            // https://www.baeldung.com/java-case-insensitive-string-matching
            String quotedAuthor = Pattern.quote(author);
            boolean removeAuthor
                = author.isEmpty()
                    || title.matches("(?i).*" + quotedAuthor + ".*")
                    || source.matches("(?i).*" + quotedAuthor + ".*");

            if (removeAuthor)
                author = null;
        }

        if (withResponse) {
            if (author != null) {
                return getString(
                    R.string.share_clue_response_text_author,
                    clueText,
                    responseText,
                    source, title, author
                );
            } else {
                return getString(
                    R.string.share_clue_response_text_no_author,
                    clueText,
                    responseText,
                    source, title
                );
            }
        } else {
            if (author != null) {
                return getString(
                    R.string.share_clue_text_author,
                    clueText,
                    source, title, author
                );
            } else {
                return getString(
                    R.string.share_clue_text_no_author,
                    clueText,
                    source, title
                );
            }
        }
    }

    private String getShareResponseText(Box[] boxes) {
        StringBuilder responseText = new StringBuilder();
        if (boxes != null) {
            for (Box box : boxes) {
                if (box.isBlank()) {
                    responseText.append(
                        getString(R.string.share_clue_blank_box)
                    );
                } else {
                    responseText.append(box.getResponse());
                }
            }
        }
        return responseText.toString();
    }

    protected String getShareClueText(Clue clue) {
        boolean showCount = prefs.getBoolean("showCount", false);

        if (clue == null)
            return getString(R.string.unknown_hint);

        int wordLen = clue.hasZone() ? clue.getZone().size() : -1;

        if (showCount && wordLen >= 0) {
            return getString(
                R.string.clue_format_short_no_num_no_dir_with_count,
                clue.getHint(),
                wordLen
            );
        } else {
            return clue.getHint();
        }
    }

}
