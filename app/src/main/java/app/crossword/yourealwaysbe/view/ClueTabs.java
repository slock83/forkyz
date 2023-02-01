package app.crossword.yourealwaysbe.view;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.ClueList;
import app.crossword.yourealwaysbe.puz.Playboard.PlayboardChanges;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.util.WeakSet;
import app.crossword.yourealwaysbe.view.BoardEditView.BoardClickListener;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import androidx.core.text.HtmlCompat;
import androidx.core.view.GestureDetectorCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

public class ClueTabs extends LinearLayout
                      implements Playboard.PlayboardListener {
    private static final Logger LOG = Logger.getLogger("app.crossword.yourealwaysbe");

    public static enum PageType {
        CLUES, HISTORY;
    }

    private ViewPager2 viewPager;
    private Playboard board;
    private boolean listening = false;
    private Set<ClueTabsListener> listeners = WeakSet.buildSet();
    private boolean forceSnap = false;
    private List<String> listNames = new ArrayList<>();
    private boolean showWords = false;

    public static interface ClueTabsListener {
        /**
         * When the user clicks a clue
         *
         * @param clue the clue clicked
         * @param view the view calling
         */
        default void onClueTabsClick(Clue clue,
                                     ClueTabs view) { }

        /**
         * When the user long-presses a clue
         *
         * @param clue the clue clicked
         * @param view the view calling
         */
        default void onClueTabsLongClick(Clue clue,
                                         ClueTabs view) { }

        /**
         * When the user swipes up on the tab bar
         *
         * @param view the view calling
         */
        default void onClueTabsBarSwipeUp(ClueTabs view) { }

        /**
         * When the user swipes down on the tab bar
         *
         * @param view the view calling
         */
        default void onClueTabsBarSwipeDown(ClueTabs view) { }

        /**
         * When the user swipes down on the tab bar
         *
         * @param view the view calling
         */
        default void onClueTabsBarLongclick(ClueTabs view) { }

        /**
         * When the user changes the page being viewed
         *
         * @param view the view calling
         */
        default void onClueTabsPageChange(ClueTabs view, int pageNumber) { }
    }

    public ClueTabs(Context context, AttributeSet as) {
        super(context, as);
        LayoutInflater.from(context).inflate(R.layout.clue_tabs, this);
    }

    /**
     * Show words beneath each clue in list
     */
    public void setShowWords(boolean showWords) {
        boolean changed = this.showWords != showWords;
        this.showWords = showWords;
        if (changed)
            refresh();
    }

    /**
     * Does nothing if the same board is already set
     */
    public void setBoard(Playboard board) {
        if (board == null)
            return;

        // same board, nothing to do (avoid rebuilding adapters and
        // losing position)
        if (board == this.board)
            return;

        // ignore old board if there was one
        unlistenBoard();
        listNames.clear();

        this.board = board;
        Puzzle puz = board.getPuzzle();

        if (puz == null)
            return;

        listNames.addAll(puz.getClueListNames());
        Collections.sort(listNames);

        TabLayout tabLayout = findViewById(R.id.clueTabsTabLayout);
        viewPager = findViewById(R.id.clueTabsPager);

        final ClueTabsPagerAdapter adapter = new ClueTabsPagerAdapter();

        viewPager.setAdapter(adapter);

        viewPager.registerOnPageChangeCallback(
            new ViewPager2.OnPageChangeCallback() {
                public void onPageSelected(int position) {
                    ClueTabs.this.notifyListenersPageChanged(position);
                }
            }
        );

        new TabLayoutMediator(tabLayout, viewPager,
            (tab, position) -> { tab.setText(adapter.getPageTitle(position)); }
        ).attach();

        setTabLayoutOnTouchListener();

        LinearLayout tabStrip = (LinearLayout) tabLayout.getChildAt(0);
        for (int i = 0; i < tabStrip.getChildCount(); i++) {
            tabStrip.getChildAt(i).setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    ClueTabs.this.notifyListenersTabsBarLongClick();
                    return true;
                }
            });
        }
    }

    public void setPage(int pageNumber) {
        if (viewPager != null) {
            viewPager.setCurrentItem(pageNumber, false);
        }
    }

    public PageType getCurrentPageType() {
        if (viewPager != null) {
            int curPage = viewPager.getCurrentItem();
            ClueTabsPagerAdapter adapter
                = (ClueTabsPagerAdapter) viewPager.getAdapter();
            return adapter.getPageType(curPage);
        }
        return null;
    }

    /**
     * Name of current list or null if oob or history
     */
    public String getCurrentPageListName() {
        if (viewPager != null) {
            int curPage = viewPager.getCurrentItem();
            ClueTabsPagerAdapter adapter
                = (ClueTabsPagerAdapter) viewPager.getAdapter();
            return adapter.getPageListName(curPage);
        }
        return null;
    }

    public void nextPage() {
        if (viewPager != null) {
            int curPage = viewPager.getCurrentItem();
            int numPages = viewPager.getAdapter().getItemCount();
            viewPager.setCurrentItem((curPage + 1) % numPages, false);
        }
    }

    public void prevPage() {
        if (viewPager != null) {
            int curPage = viewPager.getCurrentItem();
            int numPages = viewPager.getAdapter().getItemCount();
            int nextPage = curPage == 0 ? numPages - 1 : curPage - 1;
            viewPager.setCurrentItem(nextPage, false);
        }
    }

    /**
     * Always snap to clue if true, else follow prefs
     */
    public void setForceSnap(boolean forceSnap) {
        this.forceSnap = forceSnap;
    }

    /**
     * Refresh view to match current board state
     */
    @SuppressLint("NotifyDataSetChanged")
    public void refresh() {
        // make sure up to date with board
        if (viewPager != null)
            viewPager.getAdapter().notifyDataSetChanged();
    }

    public void addListener(ClueTabsListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ClueTabsListener listener) {
        listeners.remove(listener);
    }

    public void listenBoard() {
        if (board != null && !listening) {
            board.addListener(this);
            listening = true;
        }
    }

    public void unlistenBoard() {
        if (board != null && listening) {
            board.removeListener(this);
            listening = false;
        }
    }

    public void onResume() {

    }

    public void onPause() {

    }

    public void onPlayboardChange(PlayboardChanges changes) {
        if (viewPager == null || !isSnapToClue())
            return;

        ClueID cid = board.getClueID();
        String listName = (cid == null) ? null : cid.getListName();

        if (listName == null)
            return;

        int listIndex = listNames.indexOf(listName);
        if (listIndex < 0)
            return;

        viewPager.setCurrentItem(listIndex);
    }

    private boolean isSnapToClue() {
        SharedPreferences prefs
            = PreferenceManager.getDefaultSharedPreferences(
                getContext()
            );

        return forceSnap || prefs.getBoolean("snapClue", false);
    }

    private boolean isShowCount() {
        SharedPreferences prefs
            = PreferenceManager.getDefaultSharedPreferences(getContext());
        return prefs.getBoolean("showCount", false);
    }

    private void notifyListenersClueClick(Clue clue) {
        for (ClueTabsListener listener : listeners)
            listener.onClueTabsClick(clue, this);
    }

    private void notifyListenersClueLongClick(Clue clue) {
        for (ClueTabsListener listener : listeners)
            listener.onClueTabsLongClick(clue, this);
    }

    private void notifyListenersTabsBarSwipeUp() {
        for (ClueTabsListener listener : listeners)
            listener.onClueTabsBarSwipeUp(this);
    }

    private void notifyListenersTabsBarSwipeDown() {
        for (ClueTabsListener listener : listeners)
            listener.onClueTabsBarSwipeDown(this);
    }

    private void notifyListenersTabsBarLongClick() {
        for (ClueTabsListener listener : listeners)
            listener.onClueTabsBarSwipeDown(this);
    }

    private void notifyListenersPageChanged(int pageNumber) {
        for (ClueTabsListener listener : listeners)
            listener.onClueTabsPageChange(this, pageNumber);
    }

    private class ClueTabsPagerAdapter extends RecyclerView.Adapter<ClueListHolder> {
        @Override
        public ClueListHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View page = LayoutInflater.from(parent.getContext())
                                      .inflate(R.layout.clue_tabs_page,
                                               parent,
                                               false);
            return new ClueListHolder(page);
        }

        @Override
        public void onBindViewHolder(ClueListHolder holder, int position) {
            holder.setContents(getPageType(position), getPageTitle(position));
        }

        public PageType getPageType(int position) {
            if (position == listNames.size())
                return PageType.HISTORY;
            else
                return PageType.CLUES;
        }

        public String getPageTitle(int position) {
            Context context = getContext();
            if (position < 0 || position > listNames.size())
                return null;
            else if (position == listNames.size())
                return context.getString(R.string.clue_tab_history);
            else
                return listNames.get(position);
        }

        /**
         * Name of current list or null if history or oob
         */
        public String getPageListName(int position) {
            if (position < 0 || position >= listNames.size())
                return null;
            else
                return listNames.get(position);
        }

        @Override
        public int getItemCount() {
            return listNames.size() + 1;
        }
    }

    private class ClueListHolder
            extends RecyclerView.ViewHolder {

        private RecyclerView clueList;
        private ClueListAdapter clueListAdapter;
        private LinearLayoutManager layoutManager;
        private PageType pageType;
        private String listName;

        // used to follow moved clue in history list when selected
        private RecyclerView.AdapterDataObserver historyObserver
            = new RecyclerView.AdapterDataObserver() {
                public void onItemRangeInserted(int start, int count) {
                    boolean atStart
                        = layoutManager.findFirstVisibleItemPosition() == 0;
                    if (start == 0 && atStart && clueList != null)
                        layoutManager.scrollToPositionWithOffset(0, 0);
                }

                public void onItemRangeMoved(int from, int to, int count) {
                    boolean atStart
                        = layoutManager.findFirstVisibleItemPosition() == 0;
                    if (to == 0 && atStart && clueList != null)
                        layoutManager.scrollToPositionWithOffset(0, 0);
                }
            };

        public ClueListHolder(View view) {
            super(view);
            Context context = itemView.getContext();
            clueList = view.findViewById(R.id.tabClueList);

            layoutManager = new LinearLayoutManager(context);
            clueList.setLayoutManager(layoutManager);
            clueList.setItemAnimator(new DefaultItemAnimator());
            clueList.addItemDecoration(
                new DividerItemDecoration(context,
                                          DividerItemDecoration.VERTICAL)
            );
        }

        /**
         * List name only used when pageType is CLUES
         */
        @SuppressLint("NotifyDataSetChanged")
        public void setContents(PageType pageType, String listName) {

            Playboard board = ClueTabs.this.board;
            Puzzle puz = board.getPuzzle();

            boolean changed = (
                this.pageType != pageType
                || !Objects.equals(this.listName, listName)
            );

            if (board != null && changed) {
                // remove old observer if there was one
                if (clueListAdapter != null) {
                    if (this.pageType == PageType.HISTORY) {
                        clueListAdapter.unregisterAdapterDataObserver(
                            historyObserver
                        );
                    }
                }

                switch (pageType) {
                case CLUES:
                    if (puz != null) {
                        clueListAdapter = new PuzzleListAdapter(
                            listName, puz.getClues(listName)
                        );
                    } else {
                        // easier to create an empty history list that
                        // puzzle clue list
                        clueListAdapter = new HistoryListAdapter(
                            new LinkedList<>()
                        );
                    }
                    break;

                case HISTORY:
                    if (puz != null) {
                        clueListAdapter
                            = new HistoryListAdapter(puz.getHistory());
                        clueListAdapter.registerAdapterDataObserver(
                            historyObserver
                        );
                    } else {
                        clueListAdapter
                            = new HistoryListAdapter(new LinkedList<>());
                    }
                    break;
                }

                clueList.setAdapter(clueListAdapter);
                this.pageType = pageType;
                this.listName = listName;
            }

            clueListAdapter.notifyDataSetChanged();

            if (board != null) {
                if (isSnapToClue()) {
                    switch (pageType) {
                    case CLUES:
                        int position = board.getCurrentClueIndex();
                        layoutManager.scrollToPositionWithOffset(position, 0);
                        break;
                    case HISTORY:
                        layoutManager.scrollToPositionWithOffset(0, 0);
                        break;
                    }
                }
            }
        }
    }

    private abstract class ClueListAdapter
            extends RecyclerView.Adapter<ClueViewHolder>
            implements Playboard.PlayboardListener {

        boolean showDirection;

        public ClueListAdapter(boolean showDirection) {
            this.showDirection = showDirection;

            if (ClueTabs.this.board != null)
                ClueTabs.this.board.addListener(this);
        }

        @Override
        public ClueViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View clueView = LayoutInflater.from(parent.getContext())
                                          .inflate(R.layout.clue_list_item,
                                                   parent,
                                                   false);
            return new ClueViewHolder(clueView, showDirection);
        }

        @Override
        public void onViewRecycled(ClueViewHolder holder) {
            holder.setClue(null);
        }
    }

    private class PuzzleListAdapter extends ClueListAdapter {
        private ClueList clueList;
        private List<Clue> rawClueList;
        private String listName;

        public PuzzleListAdapter(String listName, ClueList clueList) {
            super(false);
            this.listName = listName;
            this.clueList = clueList;
            this.rawClueList = new ArrayList<Clue>(clueList.getClues());
        }

        @Override
        public void onBindViewHolder(ClueViewHolder holder, int position) {
            Clue clue = rawClueList.get(position);
            holder.setClue(clue);
        }

        @Override
        public int getItemCount() {
            return clueList.size();
        }

        @Override
        public void onPlayboardChange(PlayboardChanges changes) {
            // do nothing
        }
    }

    public class HistoryListAdapter
           extends ClueListAdapter {

        private List<ClueID> historyList;

        public HistoryListAdapter(List<ClueID> historyList) {
            super(true);
            this.historyList = historyList;
        }

        @Override
        public void onBindViewHolder(ClueViewHolder holder, int position) {
            ClueID item = historyList.get(position);
            Playboard board = ClueTabs.this.board;
            if (board != null) {
                Puzzle puz = board.getPuzzle();
                Clue clue = puz.getClue(item);
                if (puz != null && clue != null) {
                    holder.setClue(clue);
                }
            }
        }

        @Override
        public int getItemCount() {
            return historyList.size();
        }

        @Override
        public void onPlayboardChange(PlayboardChanges changes) {
            if (changes.isHistoryChange()) {
                int lastIndex = changes.getLastHistoryIndex();
                if (lastIndex < 0)
                    notifyItemInserted(0);
                else if (lastIndex != 0)
                    notifyItemMoved(lastIndex, 0);
            }
        }
    }

    private class ClueViewHolder
            extends RecyclerView.ViewHolder
            implements Playboard.PlayboardListener {

        private final float MAX_WORD_SCALE = 0.9F;

        private CheckedTextView clueView;
        private View flagView;
        private BoardWordEditView boardView;
        private Clue clue;
        private boolean showDirection;

        public ClueViewHolder(View view, boolean showDirection) {
            super(view);
            this.clueView = view.findViewById(R.id.clue_text_view);
            this.flagView = view.findViewById(R.id.clue_flag_view);
            this.boardView = view.findViewById(R.id.miniboard);
            this.showDirection = showDirection;

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ClueTabs.this.notifyListenersClueClick(clue);
                }
            });

            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    ClueTabs.this.notifyListenersClueLongClick(clue);
                    return true;
                }
            });

            // assume gone unless proven otherwise
            this.boardView.setVisibility(View.GONE);
            this.boardView.setMaxScale(MAX_WORD_SCALE);
            // potential memory leak as click listener has a strong
            // reference in the board view, but the board view lives as
            // long as this holder afaik
            this.boardView.addBoardClickListener(new BoardClickListener() {
                @Override
                public void onClick(Position position, Word previousWord) {
                    ClueTabs.this.notifyListenersClueClick(clue);
                }

                @Override
                public void onLongClick(Position position) {
                    ClueTabs.this.notifyListenersClueLongClick(clue);
                }
            });
        }

        /**
         * Set to null to "deactivate" view from board listening
         */
        public void setClue(Clue clue) {
            this.clue = clue;

            if (clue == null) {
                boardView.detach();
            } else {
                Playboard board = ClueTabs.this.board;

                clueView.setText(HtmlCompat.fromHtml(getClueText(clue), 0));

                if (board != null)
                    board.addListener(this);

                setStyle();

                if (showWords && board != null) {
                    Word word = board.getClueWord(clue.getClueID());
                    // suppress board render until we set the word later
                    boardView.setBoard(board, true);
                    boardView.setWord(word, Collections.<String>emptySet());
                    boardView.setVisibility(word == null ? View.GONE : View.VISIBLE);
                } else {
                    boardView.setVisibility(View.GONE);
                }
            }
        }

        @Override
        public void onPlayboardChange(PlayboardChanges changes) {
            Playboard board = ClueTabs.this.board;
            if (board == null)
                return;

            ClueID thisClueID = (clue == null) ? null : clue.getClueID();
            if (thisClueID == null)
                return;

            Word currentWord = changes.getCurrentWord();
            Word previousWord = changes.getPreviousWord();

            ClueID curClueID
                = (currentWord == null) ? null : currentWord.getClueID();
            ClueID prevClueID
                = (previousWord == null) ? null : previousWord.getClueID();

            boolean refresh =
                Objects.equals(thisClueID, curClueID)
                || Objects.equals(thisClueID, prevClueID);

            if (refresh)
                setStyle();
        }

        private void setStyle() {
            Playboard board = ClueTabs.this.board;
            if (board == null)
                return;

            ColorStateList colors = clueView.getTextColors();
            int alpha
                = ClueTabs.this.getContext().getResources().getInteger(
                    board.isFilledClueID(clue.getClueID())
                    ? R.integer.filled_clue_alpha
                    : R.integer.unfilled_clue_alpha
                );
            clueView.setTextColor(colors.withAlpha(alpha));

            ClueID cid = board.getClueID();
            boolean selected = Objects.equals(clue.getClueID(), cid);
            clueView.setChecked(selected);

            Puzzle puz = board.getPuzzle();
            if (puz != null && puz.isFlagged(clue)) {
                flagView.setVisibility(View.VISIBLE);
            } else {
                flagView.setVisibility(View.INVISIBLE);
            }
        }

        private String getClueText(Clue clue) {
            String listName = getShortListName(clue);
            String displayNum = clue.getDisplayNumber();
            String hint = clue.getHint();
            boolean hasCount = clue.hasZone();
            int count = hasCount ? clue.getZone().size() : -1;

            if (showDirection) {
                if (hasCount && isShowCount()) {
                    if (displayNum != null) {
                        return ClueTabs.this.getContext().getString(
                            R.string.clue_format_short_with_count,
                            displayNum, listName, hint, count
                        );
                    } else {
                        return ClueTabs.this.getContext().getString(
                            R.string.clue_format_short_no_num_with_count,
                            listName, hint, count
                        );
                    }
                } else {
                    if (displayNum != null) {
                        return ClueTabs.this.getContext().getString(
                            R.string.clue_format_short,
                            displayNum, listName, hint
                        );
                    } else {
                        return ClueTabs.this.getContext().getString(
                            R.string.clue_format_short_no_num,
                            listName, hint
                        );
                    }
                }
            } else {
                if (hasCount && isShowCount()) {
                    if (displayNum != null) {
                        return ClueTabs.this.getContext().getString(
                            R.string.clue_format_short_no_dir_with_count,
                            displayNum, hint, count
                        );
                    } else {
                        return ClueTabs.this.getContext().getString(
                            R.string.clue_format_short_no_num_no_dir_with_count,
                            hint, count
                        );
                    }
                } else {
                    if (displayNum != null) {
                        return ClueTabs.this.getContext().getString(
                            R.string.clue_format_short_no_dir,
                            displayNum, hint
                        );
                    } else {
                        return ClueTabs.this.getContext().getString(
                            R.string.clue_format_short_no_num_no_dir,
                            hint
                        );
                    }
                }
            }
        }

        private String getShortListName(Clue clue) {
            String listName = clue.getClueID().getListName();
            if (listName == null || listName.isEmpty())
                return "";
            else
                return listName.substring(0,1)
                    .toLowerCase(Locale.getDefault());
        }
    }

    // suppress because the swipe detector does not consume clicks
    @SuppressWarnings("ClickableViewAccessibility")
    private void setTabLayoutOnTouchListener() {
        TabLayout tabLayout = findViewById(R.id.clueTabsTabLayout);

        OnGestureListener tabSwipeListener
            = new SimpleOnGestureListener() {
                // as recommended by the docs
                // https://developer.android.com/training/gestures/detector
                public boolean onDown(MotionEvent e) {
                    return true;
                }

                public boolean onFling(MotionEvent e1,
                                       MotionEvent e2,
                                       float velocityX,
                                       float velocityY) {
                    if (Math.abs(velocityY) < Math.abs(velocityX))
                        return false;

                    if (velocityY > 0)
                        ClueTabs.this.notifyListenersTabsBarSwipeDown();
                    else
                        ClueTabs.this.notifyListenersTabsBarSwipeUp();

                    return true;
                }
            };

        GestureDetectorCompat tabSwipeDetector = new GestureDetectorCompat(
            tabLayout.getContext(), tabSwipeListener
        );

        tabLayout.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent e) {
                return tabSwipeDetector.onTouchEvent(e);
            }
        });
    }
}

