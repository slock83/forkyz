package app.crossword.yourealwaysbe;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.inputmethodservice.KeyboardView;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import androidx.core.content.ContextCompat;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.text.SpannableString;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.MovementStrategy;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Playboard.Clue;
import app.crossword.yourealwaysbe.puz.Playboard.Position;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.view.ClueTabs;
import app.crossword.yourealwaysbe.view.PlayboardRenderer;
import app.crossword.yourealwaysbe.view.ScrollingImageView;
import app.crossword.yourealwaysbe.view.ScrollingImageView.ClickListener;
import app.crossword.yourealwaysbe.view.ScrollingImageView.Point;
import app.crossword.yourealwaysbe.view.ScrollingImageView.ScaleListener;
import app.crossword.yourealwaysbe.view.SeparatedListAdapter;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;


public class PlayActivity extends ForkyzActivity {
    private static final Logger LOG = Logger.getLogger("app.crossword.yourealwaysbe");
    private static final int INFO_DIALOG = 0;
    private static final int REVEAL_PUZZLE_DIALOG = 2;
    private static final int HISTORY_LEN = 5;
    static final String ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String SHOW_TIMER = "showTimer";
    public static final String SCALE = "scale";

    @SuppressWarnings("rawtypes")
    private AdapterView across;
    @SuppressWarnings("rawtypes")
    private AdapterView down;
    private AlertDialog revealPuzzleDialog;
    private ListView allClues;
    private ClueListAdapter acrossAdapter;
    private ClueListAdapter downAdapter;
    private SeparatedListAdapter allCluesAdapter;
    private RecyclerView historyView;
    private HistoryListAdapter historyAdapter;
    //private LinkedList<HistoryItem> historyList;
    private ClueTabs clueTabs;
    private Configuration configuration;
    private Dialog dialog;
    private File baseFile;
    private Handler handler = new Handler();
    private ImaginaryTimer timer;
    private KeyboardManager keyboardManager;
    private MovementStrategy movement = null;
    private Puzzle puz;
    private ScrollingImageView boardView;
    private TextView clue;
    private boolean fitToScreen;
    private boolean runTimer = false;
    private Runnable updateTimeTask = new Runnable() {
        public void run() {
            if (timer != null) {
                getWindow().setTitle(timer.time());
                //noinspection deprecation
                getWindow().setFeatureInt(Window.FEATURE_PROGRESS,
                        puz.getPercentComplete() * 100);
            }

            if (runTimer) {
                handler.postDelayed(this, 1000);
            }
        }
    };

    private boolean showCount = false;
    private boolean showErrors = false;
    private long lastKey;
    private long lastTap = 0;
    private long resumedOn;
    private int screenWidthInInches;
    private Runnable fitToScreenTask = new Runnable() {
        @Override
        public void run() {
            fitToScreen();
        }
    };


    private Playboard getBoard(){
        return ForkyzApplication.getInstance().getBoard();
    }

    private PlayboardRenderer getRenderer(){
        return ForkyzApplication.getInstance().getRenderer();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.configuration = newConfig;

        keyboardManager.onConfigurationChanged(newConfig);

        this.runTimer = prefs.getBoolean("runTimer", false);

        if (runTimer) {
            this.handler.post(this.updateTimeTask);
        }

        metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        this.screenWidthInInches = (metrics.widthPixels > metrics.heightPixels ? metrics.widthPixels : metrics.heightPixels) / Math.round(160 * metrics.density);
        LOG.info("Configuration Changed "+this.screenWidthInInches+" ");
        if(this.screenWidthInInches >= 7){
            this.handler.post(this.fitToScreenTask);
        }
    }

    DisplayMetrics metrics;

    /**
     * Called when the activity is first created.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        this.screenWidthInInches = (metrics.widthPixels > metrics.heightPixels ? metrics.widthPixels : metrics.heightPixels) / Math.round(160 * metrics.density);
        this.configuration = getBaseContext().getResources().getConfiguration();

        try {
            if (!prefs.getBoolean(SHOW_TIMER, false)) {
                if (ForkyzApplication.isLandscape(metrics)) {
                    if (ForkyzApplication.isMiniTabletish(metrics)) {
                        utils.hideWindowTitle(this);
                    }
                }

            } else {
                supportRequestWindowFeature(Window.FEATURE_PROGRESS);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        utils.holographic(this);
        utils.finishOnHomeButton(this);

        this.showErrors = this.prefs.getBoolean("showErrors", false);
        setDefaultKeyMode(Activity.DEFAULT_KEYS_DISABLE);

        MovementStrategy movement = this.getMovementStrategy();

        if (prefs.getBoolean("fullScreen", false)) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        try {
            Uri u = this.getIntent().getData();

            if (u != null) {
                if (u.getScheme().equals("file")) {
                    baseFile = new File(u.getPath());
                    puz = IO.load(baseFile);
                }
            }

            if (puz == null || puz.getBoxes() == null) {
                throw new IOException();
            }

            ForkyzApplication.getInstance().setBoard(new Playboard(puz, movement, prefs.getBoolean("preserveCorrectLettersInShowErrors", false)));
            ForkyzApplication.getInstance().setRenderer(new PlayboardRenderer(getBoard(), metrics.densityDpi, metrics.widthPixels,
                    !prefs.getBoolean("supressHints", false),
                    ContextCompat.getColor(this, R.color.boxColor), ContextCompat.getColor(this, R.color.blankColor),
                    ContextCompat.getColor(this, R.color.errorColor)));

            float scale = prefs.getFloat(SCALE, 1.0F);

            if (scale > getRenderer().getDeviceMaxScale()) {
                scale = getRenderer().getDeviceMaxScale();
            } else if (scale < getRenderer().getDeviceMinScale()) {
                scale = getRenderer().getDeviceMinScale();
            } else if (Float.isNaN(scale)) {
                scale = 1F;
            }
            prefs.edit().putFloat(SCALE, scale).apply();

            getRenderer().setScale(scale);
            getBoard().setSkipCompletedLetters(this.prefs.getBoolean("skipFilled",
                    false));

            if (puz.getPercentComplete() != 100) {
                this.timer = new ImaginaryTimer(puz.getTime());
                this.timer.start();
                this.runTimer = prefs.getBoolean(SHOW_TIMER, false);

                if (runTimer) {
                    this.handler.post(this.updateTimeTask);
                }
            }

            setContentView(R.layout.play);

            keyboardManager
                = new KeyboardManager(this, (KeyboardView) findViewById(R.id.playKeyboard));

            this.clue = this.findViewById(R.id.clueLine);
            if (clue != null && clue.getVisibility() != View.GONE) {
                ConstraintSet set = new ConstraintSet();
                set.setVisibility(clue.getId(), ConstraintSet.GONE);
                set.applyTo(this.findViewById(R.id.playConstraintLayout));

                View custom = utils.onActionBarCustom(this, R.layout.clue_line_only);
                if (custom != null) {
                    clue = custom.findViewById(R.id.clueLine);
                }
            }
            if(this.clue != null) {
                this.clue.setClickable(true);
                this.clue.setOnClickListener(new OnClickListener() {
                    public void onClick(View arg0) {
                        Intent i = new Intent(PlayActivity.this,
                                ClueListActivity.class);
                        i.setData(Uri.fromFile(baseFile));
                        PlayActivity.this.startActivityForResult(i, 0);
                    }
                });
            }

            boardView = (ScrollingImageView) this.findViewById(R.id.board);
            this.boardView.setCurrentScale(scale);
            this.boardView.setFocusable(true);
            this.registerForContextMenu(boardView);
            boardView.setContextMenuListener(new ClickListener() {
                public void onContextMenu(final Point e) {
                    handler.post(new Runnable() {
                        public void run() {
                            try {
                                Position p = getRenderer().findBox(e);
                                Word w = getBoard().setHighlightLetter(p);
                                boolean displayScratch = prefs.getBoolean("displayScratch", false);
                                getRenderer().draw(w,
                                                   displayScratch,
                                                   displayScratch);

                                launchNotes();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                }

                public void onTap(Point e) {
                    try {
                        if (prefs.getBoolean("doubleTap", false)
                                && ((System.currentTimeMillis() - lastTap) < 300)) {
                            if (fitToScreen) {
                                getRenderer().setScale(prefs.getFloat(SCALE, 1F));
                                boardView.setCurrentScale(1F);
                                getBoard().setHighlightLetter(getRenderer().findBox(e));
                                render();
                            } else {
                                int w = boardView.getWidth();
                                int h = boardView.getHeight();
                                float scale = getRenderer().fitTo((w < h) ? w : h);
                                boardView.setCurrentScale(scale);
                                render(true);
                                boardView.scrollTo(0, 0);
                            }

                            fitToScreen = !fitToScreen;
                        } else {
                            Position p = getRenderer().findBox(e);
                            if (getBoard().isInWord(p)) {
                                Word old = getBoard().setHighlightLetter(p);
                                PlayActivity.this.render(old);
                            }
                        }

                        lastTap = System.currentTimeMillis();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
        } catch (IOException e) {
            System.err.println(this.getIntent().getData());
            e.printStackTrace();

            String filename = null;

            try {
                filename = this.baseFile.getName();
            } catch (Exception ee) {
                e.printStackTrace();
            }

            Toast t = Toast
                    .makeText(
                            this,
                            "Unable to read file" +
                                    (filename != null ?
                                            (" \n" + filename)
                                    : "")
                            , Toast.LENGTH_SHORT);
            t.show();
            this.finish();

            return;
        }

        revealPuzzleDialog = new AlertDialog.Builder(this).create();
        revealPuzzleDialog.setTitle("Reveal Entire Puzzle");
        revealPuzzleDialog.setMessage("Are you sure?");

        revealPuzzleDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        getBoard().revealPuzzle();
                        render();
                    }
                });
        revealPuzzleDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        this.boardView.setFocusable(true);
        this.boardView.setScaleListener(new ScaleListener() {
            TimerTask t;
            Timer renderTimer = new Timer();

            public void onScale(float newScale, final Point center) {
                //fitToScreen = false;

                if (t != null) {
                    t.cancel();
                }

                renderTimer.purge();
                t = new TimerTask() {
                    @Override
                    public void run() {
                        handler.post(new Runnable() {
                            public void run() {
                                int w = boardView.getImageView().getWidth();
                                int h = boardView.getImageView().getHeight();
                                float scale = getRenderer().fitTo((w < h) ? w : h);
                                prefs.edit()
                                        .putFloat(SCALE,
                                                scale)
                                        .apply();
                                getBoard().setHighlightLetter(getRenderer().findBox(center));

                                render(true);
                            }
                        });
                    }
                };
                renderTimer.schedule(t, 500);
                lastTap = System.currentTimeMillis();
            }
        });

        if (puz.isUpdatable()) {
            this.showErrors = false;
        }

        if (getBoard().isShowErrors() != this.showErrors) {
            getBoard().toggleShowErrors();
        }

        this.clueTabs = this.findViewById(R.id.playClueTab);
        this.clueTabs.setBoard(getBoard());
        // this.historyList = new LinkedList<HistoryItem>();
        // this.historyAdapter = new HistoryListAdapter(this.historyList);
        // this.historyView = (RecyclerView) this.findViewById(R.id.historyList);
        // if (this.historyView != null) {
        //     RecyclerView.LayoutManager layoutManager
        //         = new LinearLayoutManager(getApplicationContext());
        //     this.historyView.setLayoutManager(layoutManager);
        //     this.historyView.setItemAnimator(new DefaultItemAnimator());
        //     this.historyView.setAdapter(this.historyAdapter);
        //     this.historyView.addItemDecoration(
        //         new DividerItemDecoration(getApplicationContext(),
        //                                   DividerItemDecoration.VERTICAL)
        //     );
        // }

        this.render(true);

        this.across = this.findViewById(R.id.acrossList);
        this.down = this.findViewById(R.id.downList);
        boolean isGal = false;
        if ((this.across == null) && (this.down == null)) {
            this.across = this.findViewById(R.id.acrossListGal);
            this.down = this.findViewById(R.id.downListGal);
            isGal = true;
        }

        if ((across != null) && (down != null)) {
            across.setAdapter(this.acrossAdapter = new ClueListAdapter(this,
                    getBoard().getAcrossClues(), true));
            down.setAdapter(this.downAdapter = new ClueListAdapter(this, getBoard()
                    .getDownClues(), false));
            if (!isGal) {
                across.setOnItemClickListener(new OnItemClickListener() {
                    public void onItemClick(AdapterView<?> arg0, View arg1,
                                            int arg2, long arg3) {
                        arg0.setSelected(true);
                        getBoard().jumpTo(arg2, true);
                        render();
                    }
                });
                across.setOnItemLongClickListener(new OnItemLongClickListener() {
                    public boolean onItemLongClick(AdapterView<?> parent,
                                                   View view,
                                                   int pos,
                                                   long id) {
                        parent.setSelected(true);
                        getBoard().jumpTo(pos, true);
                        render();
                        launchNotes();
                        return true;
                    }
                });
            }

            across.setOnItemSelectedListener(new OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> arg0, View arg1,
                                           int arg2, long arg3) {
                    if (!getBoard().isAcross()
                            || (getBoard().getCurrentClueIndex() != arg2)) {
                        getBoard().jumpTo(arg2, true);
                        render();
                    }
                }

                public void onNothingSelected(AdapterView<?> view) {
                }
            });

            if (!isGal) {
                down.setOnItemClickListener(new OnItemClickListener() {
                    public void onItemClick(AdapterView<?> arg0, View arg1,
                                            final int arg2, long arg3) {
                        getBoard().jumpTo(arg2, false);
                        render();
                    }
                });
                down.setOnItemLongClickListener(new OnItemLongClickListener() {
                    public boolean onItemLongClick(AdapterView<?> parent,
                                                   View view,
                                                   int pos,
                                                   long id) {
                        parent.setSelected(true);
                        getBoard().jumpTo(pos, true);
                        render();
                        launchNotes();
                        return true;
                    }
                });
            }

            down.setOnItemSelectedListener(new OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> arg0, View arg1,
                                           int arg2, long arg3) {
                    if (getBoard().isAcross()
                            || (getBoard().getCurrentClueIndex() != arg2)) {
                        getBoard().jumpTo(arg2, false);
                        render();
                    }
                }

                public void onNothingSelected(AdapterView<?> arg0) {
                }
            });
            down.scrollTo(0, 0);
            across.scrollTo(0, 0);

            // MATT TODO: re-enable this somehow if its needed
            //down.setOnKeyListener(onKeyListener);
            //across.setOnKeyListener(onKeyListener);
            down.setFocusable(false);
            across.setFocusable(false);
        }
        this.allClues = (ListView) this.findViewById(R.id.allClues);
        if (this.allClues != null) {
            this.allCluesAdapter = new SeparatedListAdapter(this);
            this.allCluesAdapter.addSection(
                    "Across",
                    this.acrossAdapter = new ClueListAdapter(this, getBoard()
                            .getAcrossClues(), true));
            this.allCluesAdapter.addSection(
                    "Down",
                    this.downAdapter = new ClueListAdapter(this, getBoard()
                            .getDownClues(), false));
            allClues.setAdapter(this.allCluesAdapter);

            allClues.setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent,
                                        View view,
                                        int clickIndex,
                                        long id) {
                    onItemClickSelect(parent, view, clickIndex, id);
                }
            });
            allClues.setOnItemLongClickListener(new OnItemLongClickListener() {
                public boolean onItemLongClick(AdapterView<?> parent,
                                               View view,
                                               int pos,
                                               long id) {
                    onItemClickSelect(parent, view, pos, id);
                    launchNotes();
                    return true;
                }
            });
            allClues.setOnItemSelectedListener(new OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> arg0, View arg1,
                                           int clickIndex, long arg3) {
                    boolean across = clickIndex <= getBoard().getAcrossClues().length + 1;
                    int index = clickIndex - 1;
                    if (index > getBoard().getAcrossClues().length) {
                        index = index - getBoard().getAcrossClues().length - 1;
                    }
                    if (!getBoard().isAcross() == across && getBoard().getCurrentClueIndex() != index) {
                        arg0.setSelected(true);
                        getBoard().jumpTo(index, across);
                        render();
                    }
                }

                public void onNothingSelected(AdapterView<?> view) {
                }
            });
        }

        if (!prefs.getBoolean(SHOW_TIMER, false)) {
            if (ForkyzApplication.isLandscape(metrics)) {
                if (ForkyzApplication.isMiniTabletish(metrics) && allClues != null) {
                    utils.hideActionBar(this);
                }
            }
        }

        this.setClueSize(prefs.getInt("clueSize", 12));
        setTitle(neverNull(puz.getTitle()) + " - " + neverNull(puz.getAuthor())
             + " -	" + neverNull(puz.getCopyright()));
        this.showCount = prefs.getBoolean("showCount", false);
        if (this.prefs.getBoolean("fitToScreen", false) || (ForkyzApplication.isLandscape(metrics)) && (ForkyzApplication.isTabletish(metrics) || ForkyzApplication.isMiniTabletish(metrics))) {
            this.handler.postDelayed(new Runnable() {

                public void run() {
                    boardView.scrollTo(0, 0);

                    int v = (boardView.getWidth() < boardView.getHeight()) ? boardView
                            .getWidth() : boardView.getHeight();
                    if (v == 0) {
                        handler.postDelayed(this, 100);
                    }
                    float newScale = getRenderer().fitTo(v);
                    boardView.setCurrentScale(newScale);

                    prefs.edit().putFloat(SCALE, newScale).apply();
                    render();
                }

            }, 100);

        }

    }

    private static String neverNull(String val) {
        return val == null ? "" : val.trim();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Clues").setIcon(android.R.drawable.ic_menu_agenda);
        menu.add("Notes").setIcon(android.R.drawable.ic_menu_agenda);

        Menu zoom = menu.addSubMenu("Zoom");
        zoom.add(createSpannableForMenu("Zoom In")).setTitleCondensed("Zoom In");

        if (getRenderer() != null && getRenderer().getScale() < getRenderer().getDeviceMaxScale())
            zoom.add(createSpannableForMenu("Zoom In Max")).setTitleCondensed("Zoom In Max");

        zoom.add(createSpannableForMenu("Zoom Out")).setTitleCondensed("Zoom Out");
        zoom.add(createSpannableForMenu("Fit to Screen")).setTitleCondensed("Fit to Screen");
        zoom.add(createSpannableForMenu("Zoom Reset")).setTitleCondensed("Zoom Reset");

        Menu clueSize = menu.addSubMenu("Clue Text Size");
        clueSize.add(createSpannableForMenu("Small")).setTitleCondensed("Small");
        clueSize.add(createSpannableForMenu("Medium")).setTitleCondensed("Medium");
        clueSize.add(createSpannableForMenu("Large")).setTitleCondensed("Large");

        if (puz != null && !puz.isUpdatable()) {
            MenuItem showItem = menu.add(
                    this.showErrors ? "Hide Errors" : "Show Errors").setIcon(
                    android.R.drawable.ic_menu_view);
            if (ForkyzApplication.isTabletish(metrics)) {
                utils.onActionBarWithText(showItem);
            }

            SubMenu reveal = menu.addSubMenu("Reveal").setIcon(
                    android.R.drawable.ic_menu_view);
            reveal.add(createSpannableForMenu("Letter")).setTitleCondensed("Letter");
            reveal.add(createSpannableForMenu("Word")).setTitleCondensed("Word");
            reveal.add(createSpannableForMenu("Errors")).setTitleCondensed("Errors");
            reveal.add(createSpannableForMenu("Puzzle")).setTitleCondensed("Puzzle");
            if (ForkyzApplication.isTabletish(metrics)) {
                utils.onActionBarWithText(reveal);
            }
        } else {
            menu.add("Show Errors").setEnabled(false)
                    .setIcon(android.R.drawable.ic_menu_view);
            menu.add("Reveal").setIcon(android.R.drawable.ic_menu_view)
                    .setEnabled(false);
        }

        menu.add("Info").setIcon(android.R.drawable.ic_menu_info_details);
        menu.add("Help").setIcon(android.R.drawable.ic_menu_help);
        menu.add("Settings").setIcon(android.R.drawable.ic_menu_preferences);

        return true;
    }

    private SpannableString createSpannableForMenu(String value){
        SpannableString s = new SpannableString(value);
        s.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.textColorPrimary)), 0, s.length(), 0);
        return s;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return keyCode == KeyEvent.KEYCODE_ENTER;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Word previous;

        if ((System.currentTimeMillis() - this.resumedOn) < 500) {
            return true;
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_SEARCH:
                System.out.println("Next clue.");
                getBoard().setMovementStrategy(MovementStrategy.MOVE_NEXT_CLUE);
                previous = getBoard().nextWord();
                getBoard().setMovementStrategy(this.getMovementStrategy());
                this.render(previous);

                return true;

            case KeyEvent.KEYCODE_BACK:
                this.finish();

                return true;

            case KeyEvent.KEYCODE_MENU:
                return false;

            case KeyEvent.KEYCODE_DPAD_DOWN:

                if ((System.currentTimeMillis() - lastKey) > 50) {
                    previous = getBoard().moveDown();
                    this.render(previous);
                }


                lastKey = System.currentTimeMillis();

                return true;
            case KeyEvent.KEYCODE_DPAD_UP:

                if ((System.currentTimeMillis() - lastKey) > 50) {
                    previous = getBoard().moveUp();
                    this.render(previous);
                }

                lastKey = System.currentTimeMillis();

                return true;

            case KeyEvent.KEYCODE_DPAD_LEFT:

                if ((System.currentTimeMillis() - lastKey) > 50) {
                    previous = getBoard().moveLeft();
                    this.render(previous);
                }

                lastKey = System.currentTimeMillis();

                return true;

            case KeyEvent.KEYCODE_DPAD_RIGHT:

                if ((System.currentTimeMillis() - lastKey) > 50) {
                    previous = getBoard().moveRight();
                    this.render(previous);
                }

                lastKey = System.currentTimeMillis();

                return true;

            case KeyEvent.KEYCODE_DPAD_CENTER:
                previous = getBoard().toggleDirection();
                this.render(previous);

                return true;

            case KeyEvent.KEYCODE_SPACE:

                if ((System.currentTimeMillis() - lastKey) > 150) {
                    if (prefs.getBoolean("spaceChangesDirection", true)) {
                        previous = getBoard().toggleDirection();
                        this.render(previous);
                    } else {
                        previous = getBoard().playLetter(' ');
                        this.render(previous);
                    }
                }

                lastKey = System.currentTimeMillis();

                return true;

            case KeyEvent.KEYCODE_ENTER:

                if ((System.currentTimeMillis() - lastKey) > 150) {
                    if (prefs.getBoolean("enterChangesDirection", true)) {
                        previous = getBoard().toggleDirection();
                        this.render(previous);
                    } else {
                        previous = getBoard().nextWord();
                        this.render(previous);
                    }
                }

                lastKey = System.currentTimeMillis();

                return true;

            case KeyEvent.KEYCODE_DEL:

                if ((System.currentTimeMillis() - lastKey) > 150) {
                    previous = getBoard().deleteLetter();
                    this.render(previous);
                }

                lastKey = System.currentTimeMillis();

                return true;
        }

        char c = Character
                .toUpperCase(((this.configuration.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) || keyboardManager.getUseNativeKeyboard()) ? event
                        .getDisplayLabel() : ((char) keyCode));

        if (ALPHA.indexOf(c) != -1) {
            previous = getBoard().playLetter(c);
            this.render(previous);

            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return onOptionsItemSelected(item);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        System.out.println(item.getTitle());
        if (item.getTitle() == null) {
            finish();
            return true;
        }
        if (item.getTitle().toString().equals("Letter")) {
            getBoard().revealLetter();
            this.render();

            return true;
        } else if (item.getTitle().toString().equals("Word")) {
            getBoard().revealWord();
            this.render();

            return true;
        } else if (item.getTitle().toString().equals("Errors")) {
            getBoard().revealErrors();
            this.render();

            return true;
        } else if (item.getTitle().toString().equals("Puzzle")) {
            this.showDialog(REVEAL_PUZZLE_DIALOG);

            return true;
        } else if (item.getTitle().toString().equals("Show Errors")
                || item.getTitle().toString().equals("Hide Errors")) {
            getBoard().toggleShowErrors();
            item.setTitle(getBoard().isShowErrors() ? "Hide Errors" : "Show Errors");
            this.prefs.edit().putBoolean("showErrors", getBoard().isShowErrors())
                    .apply();
            this.render();

            return true;
        } else if (item.getTitle().toString().equals("Settings")) {
            Intent i = new Intent(this, PreferencesActivity.class);
            this.startActivity(i);

            return true;
        } else if (item.getTitle().toString().equals("Zoom In")) {
            this.boardView.scrollTo(0, 0);

            float newScale = getRenderer().zoomIn();
            this.prefs.edit().putFloat(SCALE, newScale).apply();
            this.fitToScreen = false;
            boardView.setCurrentScale(newScale);
            this.render();

            return true;
        } else if (item.getTitle().toString().equals("Zoom In Max")) {
            this.boardView.scrollTo(0, 0);

            float newScale = getRenderer().zoomInMax();
            this.prefs.edit().putFloat(SCALE, newScale).apply();
            this.fitToScreen = false;
            boardView.setCurrentScale(newScale);
            this.render();

            return true;
        } else if (item.getTitle().toString().equals("Zoom Out")) {
            this.boardView.scrollTo(0, 0);

            float newScale = getRenderer().zoomOut();
            this.prefs.edit().putFloat(SCALE, newScale).apply();
            this.fitToScreen = false;
            boardView.setCurrentScale(newScale);
            this.render();

            return true;
        } else if (item.getTitle().toString().equals("Fit to Screen")) {
            fitToScreen();

            return true;
        } else if (item.getTitle().toString().equals("Zoom Reset")) {
            float newScale = getRenderer().zoomReset();
            boardView.setCurrentScale(newScale);
            this.prefs.edit().putFloat(SCALE, newScale).apply();
            this.render();
            this.boardView.scrollTo(0, 0);

            return true;
        } else if (item.getTitle().toString().equals("Info")) {
            if (dialog != null) {
                TextView view = (TextView) dialog
                        .findViewById(R.id.puzzle_info_time);

                if (timer != null) {
                    this.timer.stop();
                    view.setText("Elapsed Time: " + this.timer.time());
                    this.timer.start();
                } else {
                    view.setText("Elapsed Time: "
                            + new ImaginaryTimer(puz.getTime()).time());
                }
            }

            this.showDialog(INFO_DIALOG);

            return true;
        } else if (item.getTitle().toString().equals("Clues")) {
            Intent i = new Intent(PlayActivity.this, ClueListActivity.class);
            i.setData(Uri.fromFile(baseFile));
            PlayActivity.this.startActivityForResult(i, 0);

            return true;
        } else if (item.getTitle().toString().equals("Notes")) {
            launchNotes();
            return true;
        } else if (item.getTitle().toString().equals("Help")) {
            Intent i = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("file:///android_asset/playscreen.html"), this,
                    HTMLActivity.class);
            this.startActivity(i);
        } else if (item.getTitle().toString().equals("Small")) {
            this.setClueSize(12);
        } else if (item.getTitle().toString().equals("Medium")) {
            this.setClueSize(14);
        } else if (item.getTitle().toString().equals("Large")) {
            this.setClueSize(16);
        }

        return false;
    }

    private void fitToScreen() {
        this.boardView.scrollTo(0, 0);

        int v = (this.boardView.getWidth() < this.boardView.getHeight()) ? this.boardView
                .getWidth() : this.boardView.getHeight();
        float newScale = getRenderer().fitTo(v);
        this.prefs.edit().putFloat(SCALE, newScale).apply();
        boardView.setCurrentScale(newScale);
        this.render();
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        this.render();
    }

    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case INFO_DIALOG:

                // This is weird. I don't know why a rotate resets the dialog.
                // Whatevs.
                return createInfoDialog();

            case REVEAL_PUZZLE_DIALOG:
                return revealPuzzleDialog;

            default:
                return null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        keyboardManager.onPause();

        try {
            if ((puz != null) && (baseFile != null)) {
                if ((puz.getPercentComplete() != 100) && (this.timer != null)) {
                    this.timer.stop();
                    puz.setTime(timer.getElapsed());
                    this.timer = null;
                }

                IO.save(puz, baseFile);
            }
        } catch (IOException ioe) {
            LOG.log(Level.SEVERE, null, ioe);
        }

        this.timer = null;
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        if (this.timer != null) {
            this.timer.start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.resumedOn = System.currentTimeMillis();
        getBoard().setSkipCompletedLetters(this.prefs
                .getBoolean("skipFilled", false));
        getBoard().setMovementStrategy(this.getMovementStrategy());

        keyboardManager.onResume((KeyboardView) findViewById(R.id.playKeyboard));

        this.showCount = prefs.getBoolean("showCount", false);
        this.onConfigurationChanged(this.configuration);

        if (puz.getPercentComplete() != 100) {
            timer = new ImaginaryTimer(this.puz.getTime());
            timer.start();
        }

        this.runTimer = prefs.getBoolean(SHOW_TIMER, false);

        if (runTimer) {
            this.handler.post(this.updateTimeTask);
        }

        render();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (this.timer != null) {
            this.timer.stop();
        }

        if (keyboardManager != null)
            keyboardManager.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (keyboardManager != null)
            keyboardManager.onDestroy();
    }

    private void setClueSize(int dps) {
        this.clue.setTextSize(TypedValue.COMPLEX_UNIT_SP, dps);

        if ((acrossAdapter != null) && (downAdapter != null)) {
            acrossAdapter.textSize = dps;
            acrossAdapter.notifyDataSetInvalidated();
            downAdapter.textSize = dps;
            downAdapter.notifyDataSetInvalidated();
        }

        if (prefs.getInt("clueSize", 12) != dps) {
            this.prefs.edit().putInt("clueSize", dps).apply();
        }
    }

    private MovementStrategy getMovementStrategy() {
        if (movement != null) {
            return movement;
        } else {
            String stratName = this.prefs.getString("movementStrategy",
                    "MOVE_NEXT_ON_AXIS");
            switch (stratName) {
                case "MOVE_NEXT_ON_AXIS":
                    movement = MovementStrategy.MOVE_NEXT_ON_AXIS;
                    break;
                case "STOP_ON_END":
                    movement = MovementStrategy.STOP_ON_END;
                    break;
                case "MOVE_NEXT_CLUE":
                    movement = MovementStrategy.MOVE_NEXT_CLUE;
                    break;
                case "MOVE_PARALLEL_WORD":
                    movement = MovementStrategy.MOVE_PARALLEL_WORD;
                    break;
            }

            return movement;
        }
    }

    private Dialog createInfoDialog() {
        if (dialog == null) {
            dialog = new Dialog(this);
        }

        dialog.setTitle("Puzzle Info");
        dialog.setContentView(R.layout.puzzle_info_dialog);

        TextView view = dialog.findViewById(R.id.puzzle_info_title);
        view.setText(this.puz.getTitle());
        view = dialog.findViewById(R.id.puzzle_info_author);
        view.setText(this.puz.getAuthor());
        view = dialog.findViewById(R.id.puzzle_info_copyright);
        view.setText(this.puz.getCopyright());
        view = dialog.findViewById(R.id.puzzle_info_time);

        if (timer != null) {
            this.timer.stop();
            view.setText("Elapsed Time: " + this.timer.time());
            this.timer.start();
        } else {
            view.setText("Elapsed Time: "
                    + new ImaginaryTimer(puz.getTime()).time());
        }

        ProgressBar progress = (ProgressBar) dialog
                .findViewById(R.id.puzzle_info_progress);
        progress.setProgress(this.puz.getPercentComplete());

        view = dialog.findViewById(R.id.puzzle_info_filename);
        view.setText(Uri.fromFile(baseFile).toString());

        addNotes(dialog);

        return dialog;
    }

    private void addNotes(Dialog infoDialog) {
        TextView view = dialog.findViewById(R.id.puzzle_info_notes);

        String puzNotes = this.puz.getNotes();
        if (puzNotes == null)
            puzNotes = "";

        final String notes = puzNotes;

        String[] split = notes.split("(?i:(?m:^\\s*Across:?\\s*$|^\\s*\\d))", 2);

        final String text = (split.length > 1) ? split[0].trim() : null;

        String tapShow = getString(R.string.tap_to_show_full_notes);
        if (text != null && text.length() > 0)
            view.setText(text + "\n(" + tapShow + ")");
        else
            view.setText("(" + tapShow + ")");

        view.setOnClickListener(new OnClickListener() {
            private boolean showAll = true;
            private String tapShow = getString(R.string.tap_to_show_full_notes);
            private String tapHide = getString(R.string.tap_to_hide_full_notes);

            public void onClick(View view) {
                TextView tv = (TextView) view;

                if (showAll)
                    tv.setText(notes + "\n(" + tapHide + ")");
                else if (text == null || text.length() == 0)
                    tv.setText("(" + tapShow + ")");
                else
                    tv.setText(text + "\n(" + tapShow + ")");

                showAll = !showAll;
            }
        });
    }

    private void render() {
        render(null);
    }

    private void render(boolean rescale) {
        this.render(null, rescale);
    }

    private void render(Word previous) {
        this.render(previous, true);
    }

    private void render(Word previous, boolean rescale) {
        // only show keyboard if double click a word
        // hide if it's a new word
        Position newPos = getBoard().getHighlightLetter();
        if ((previous != null) &&
            previous.checkInWord(newPos.across, newPos.down)) {
            keyboardManager.render();
        } else {
            keyboardManager.hideKeyboard();
        }

        Clue c = getBoard().getClue();
        getBoard().toggleDirection();
        getBoard().toggleDirection();

        if (c.hint == null) {
            getBoard().toggleDirection();
            c = getBoard().getClue();
        }

        boolean displayScratch = this.prefs.getBoolean("displayScratch", false);
        this.boardView.setBitmap(getRenderer().draw(previous,
                                                    displayScratch, displayScratch),
                                 rescale);
        this.boardView.requestFocus();
        /*
		 * If we jumped to a new word, ensure the first letter is visible.
		 * Otherwise, insure that the current letter is visible. Only necessary
		 * if the cursor is currently off screen.
		 */
        if (rescale && this.prefs.getBoolean("ensureVisible", true)) {
            Point topLeft;
            Point bottomRight;
            Point cursorTopLeft;
            Point cursorBottomRight;
            cursorTopLeft = getRenderer().findPointTopLeft(getBoard()
                    .getHighlightLetter());
            cursorBottomRight = getRenderer().findPointBottomRight(getBoard()
                    .getHighlightLetter());

            if ((previous != null) && previous.equals(getBoard().getCurrentWord())) {
                topLeft = cursorTopLeft;
                bottomRight = cursorBottomRight;
            } else {
                topLeft = getRenderer()
                        .findPointTopLeft(getBoard().getCurrentWordStart());
                bottomRight = getRenderer().findPointBottomRight(getBoard()
                        .getCurrentWordStart());
            }

            int tlDistance = cursorTopLeft.distance(topLeft);
            int brDistance = cursorBottomRight.distance(bottomRight);

            if (!this.boardView.isVisible(topLeft) && (tlDistance < brDistance)) {
                this.boardView.ensureVisible(topLeft);
            }

            if (!this.boardView.isVisible(bottomRight)
                    && (brDistance < tlDistance)) {
                this.boardView.ensureVisible(bottomRight);
            }

            // ensure the cursor is always on the screen.
            this.boardView.ensureVisible(cursorBottomRight);
            this.boardView.ensureVisible(cursorTopLeft);

        }

        this.clue
                .setText("("
                        + (getBoard().isAcross() ? "across" : "down")
                        + ") "
                        + c.number
                        + ". "
                        + c.hint
                        + (this.showCount ? ("  ["
                        + getBoard().getCurrentWord().length + "]") : ""));

        updateHistory(c, getBoard().isAcross());

        if (this.allClues != null) {
            if (getBoard().isAcross()) {
                ClueListAdapter cla = (ClueListAdapter) this.allCluesAdapter.sections
                        .get(0);
                cla.setActiveDirection(getBoard().isAcross());
                cla.setHighlightClue(c);
                this.allCluesAdapter.notifyDataSetChanged();
                this.allClues.setSelectionFromTop(cla.indexOf(c) + 1,
                        (this.allClues.getHeight() / 2) - 50);
            } else {
                ClueListAdapter cla = (ClueListAdapter) this.allCluesAdapter.sections
                        .get(1);
                cla.setActiveDirection(!getBoard().isAcross());
                cla.setHighlightClue(c);
                this.allCluesAdapter.notifyDataSetChanged();
                this.allClues.setSelectionFromTop(
                        cla.indexOf(c) + getBoard().getAcrossClues().length + 2,
                        (this.allClues.getHeight() / 2) - 50);
            }
        }

        if (this.down != null) {
            this.downAdapter.setHighlightClue(c);
            this.downAdapter.setActiveDirection(!getBoard().isAcross());
            this.downAdapter.notifyDataSetChanged();

            if (!getBoard().isAcross() && !c.equals(this.down.getSelectedItem())) {
                if (this.down instanceof ListView) {
                    ((ListView) this.down).setSelectionFromTop(
                            this.downAdapter.indexOf(c),
                            (down.getHeight() / 2) - 50);
                } else {
                    // Gallery
                    this.down.setSelection(this.downAdapter.indexOf(c));
                }
            }
        }

        if (this.across != null) {
            this.acrossAdapter.setHighlightClue(c);
            this.acrossAdapter.setActiveDirection(getBoard().isAcross());
            this.acrossAdapter.notifyDataSetChanged();

            if (getBoard().isAcross() && !c.equals(this.across.getSelectedItem())) {
                if (across instanceof ListView) {
                    ((ListView) this.across).setSelectionFromTop(
                            this.acrossAdapter.indexOf(c),
                            (across.getHeight() / 2) - 50);
                } else {
                    // Gallery view
                    this.across.setSelection(this.acrossAdapter.indexOf(c));
                }
            }
        }

        if ((puz.getPercentComplete() == 100) && (timer != null)) {
            timer.stop();
            puz.setTime(timer.getElapsed());
            this.timer = null;
            Intent i = new Intent(PlayActivity.this,
                    PuzzleFinishedActivity.class);
            this.startActivity(i);

        }
        this.boardView.requestFocus();
    }

    private void updateHistory(Clue currentClue, boolean across) {
        // if (historyList == null)
        //     return;

        // HistoryItem item = new HistoryItem(currentClue, across);
        // // if a new item, not equal to most recent
        // if (historyList.isEmpty() ||
        //     !item.equals(historyList.getFirst())) {
        //     historyList.remove(item);
        //     historyList.addFirst(item);
        //     while (historyList.size() > HISTORY_LEN)
        //         historyList.removeLast();
        //     historyAdapter.notifyDataSetChanged();
        // }
    }

    /**
     * For across/down/allClues onItemClick and onItemLongClick
     */
    private void onItemClickSelect(AdapterView<?> parent,
                                   View view,
                                   int clickIndex,
                                   long id) {
        boolean across = clickIndex <= getBoard().getAcrossClues().length + 1;
        int index = clickIndex - 1;
        if (index > getBoard().getAcrossClues().length) {
            index = index - getBoard().getAcrossClues().length - 1;
        }
        parent.setSelected(true);
        getBoard().jumpTo(index, across);
        render();
    }

    private void launchNotes() {
        Intent i = new Intent(this, NotesActivity.class);
        this.startActivityForResult(i, 0);
    }
}