package app.crossword.yourealwaysbe;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.view.ActionMode;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.leinardi.android.speeddial.SpeedDialActionItem;
import com.leinardi.android.speeddial.SpeedDialView;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.net.Downloader;
import app.crossword.yourealwaysbe.net.Downloaders;
import app.crossword.yourealwaysbe.util.BackgroundDownloadManager;
import app.crossword.yourealwaysbe.util.MigrationHelper;
import app.crossword.yourealwaysbe.util.files.Accessor;
import app.crossword.yourealwaysbe.util.files.DirHandle;
import app.crossword.yourealwaysbe.util.files.PuzHandle;
import app.crossword.yourealwaysbe.util.files.PuzMetaFile;
import app.crossword.yourealwaysbe.view.CircleProgressBar;
import app.crossword.yourealwaysbe.view.recycler.RemovableRecyclerViewAdapter;
import app.crossword.yourealwaysbe.view.recycler.SeparatedRecyclerViewAdapter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class BrowseActivity extends ForkyzActivity {

    /**
     * Request other browsers close
     *
     * An intent to broadcast from BrowseActivity to tell all other
     * BrowseActivities to close down (avoid multiple instances on same
     * file system)
     *
     * launchMode singleTask is not really what we want here as it
     * prevents the user from returning to a puzzle from the home screen
     * (annoying)
     */
    private static final String BROWSER_CLOSE_ACTION
        = "app.crossword.yourealwaysbe.BROWSER_CLOSE_ACTION";
    /**
     * The task ID of the BrowseActivity requesting the close
     *
     * To avoid closing self
     */
    private static final String BROWSER_CLOSE_TASK_ID
        = "app.crossword.yourealwaysbe.BROWSER_CLOSE_TASK_ID";

    private static final int REQUEST_WRITE_STORAGE = 1002;

    // allow import of all docs (parser will take care of detecting if it's a
    // puzzle that's recognised)
    private static final String IMPORT_MIME_TYPE =  "*/*";

    private static final Logger LOGGER
        = Logger.getLogger(BrowseActivity.class.getCanonicalName());

    /**
     * See note for BROWSER_CLOSE_ACTION
     */
    private BroadcastReceiver closeActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BROWSER_CLOSE_ACTION)) {
                int myTaskId = BrowseActivity.this.getTaskId();
                int otherTaskId
                    = intent.getIntExtra(BROWSER_CLOSE_TASK_ID, myTaskId);

                if (myTaskId != otherTaskId)
                    utils.finishAndRemoveTask(BrowseActivity.this);
            }
        }
    };

    /**
     * When POST_NOTIFICATIONS permission needed
     */
    private ActivityResultLauncher<String> notificationPermissionLauncher
        = registerForActivityResult(new RequestPermission(), isGranted -> {
            if (!isGranted) {
                DialogFragment dialog = new NotificationPermissionDialog();
                dialog.show(
                    getSupportFragmentManager(), "NotificationPermissionDialog"
                );
            }
        });

    /**
     * When WRITE_EXTERNAL_STORAGE permission needed
     */
    private ActivityResultLauncher<String> writeStorageLauncher
        = registerForActivityResult(new RequestPermission(), isGranted -> {
            hasWritePermissions = isGranted;
        });

    private BrowseActivityViewModel model;

    private DirHandle archiveFolder
        = getFileHandler().getArchiveDirectory();
    private DirHandle crosswordsFolder
        = getFileHandler().getCrosswordsDirectory();

    private Accessor accessor = Accessor.DATE_DESC;
    private SeparatedRecyclerViewAdapter<FileViewHolder, FileAdapter>
        currentAdapter = null;
    private Handler handler = new Handler(Looper.getMainLooper());
    private RecyclerView puzzleList;
    private NotificationManagerCompat nm;
    private boolean hasWritePermissions;
    private SpeedDialView buttonAdd;
    private Set<PuzMetaFile> selected = new HashSet<>();
    private MenuItem viewCrosswordsArchiveMenuItem;
    private View pleaseWaitView;
    private Uri pendingImport;

    ActivityResultLauncher<String> getImportURI =
        utils.registerForUriContentsResult(
            this,
            uris -> { onImportURIs(uris); }
        );

    private ActionMode actionMode;
    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            actionMode = mode;

            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.browse_action_bar_menu, menu);

            if (model.getIsViewArchive()) {
                menu.findItem(R.id.browse_action_archive)
                    .setVisible(false);
            } else {
                menu.findItem(R.id.browse_action_unarchive)
                    .setVisible(false);
            }

            for (int i = 0; i < menu.size(); i++) {
                utils.onActionBarWithText(menu.getItem(i));
            }

            setSpeedDialVisibility(View.GONE);

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(
            ActionMode actionMode, MenuItem menuItem
        ) {
            int id = menuItem.getItemId();

            Set<PuzMetaFile> toAction = new HashSet<>(selected);

            if (id == R.id.browse_action_delete) {
                model.deletePuzzles(toAction);
            } else if (id == R.id.browse_action_archive) {
                model.movePuzzles(toAction, archiveFolder);
            } else if (id == R.id.browse_action_unarchive) {
                model.movePuzzles(toAction, crosswordsFolder);
            }

            actionMode.finish();

            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            clearSelection();
            setSpeedDialVisibility(View.VISIBLE);
            actionMode = null;
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // for parity with onKeyUp
        switch (keyCode) {
        case KeyEvent.KEYCODE_ESCAPE:
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_ESCAPE:
            finish();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.browse_menu, menu);

        MenuItem item = menu.findItem(R.id.browse_menu_app_theme);
        if (item != null) item.setIcon(getNightModeIcon());

        viewCrosswordsArchiveMenuItem
            = menu.findItem(R.id.browse_menu_archives);

        setViewCrosswordsOrArchiveUI();

        return true;
    }

    private void nextNightMode() {
        nightMode.next();
        if(nightMode.isNightMode()){
            AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_YES
            );
        } else {
            AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_NO
            );
        }
    }

    private int getNightModeIcon() {
        switch (nightMode.getCurrentMode()) {
        case DAY: return R.drawable.day_mode;
        case NIGHT: return R.drawable.night_mode;
        case SYSTEM: return R.drawable.system_daynight_mode;
        }
        return R.drawable.day_mode;
    }

    private void setListItemColor(View v, boolean selected){
        v.setSelected(selected);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.browse_menu_app_theme) {
            nextNightMode();
            item.setIcon(getNightModeIcon());
            return true;
        } else if (id == R.id.browse_menu_settings) {
            Intent settingsIntent = new Intent(this, PreferencesActivity.class);
            this.startActivity(settingsIntent);
            return true;
        } else if (id == R.id.browse_menu_archives) {
            startLoadPuzzleList(!model.getIsViewArchive());
            return true;
        } else if (id == R.id.browse_menu_cleanup) {
            model.cleanUpPuzzles();
            return true;
        } else if (id == R.id.browse_menu_help) {
            Intent helpIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("file:///android_asset/filescreen.html"), this,
                    HTMLActivity.class);
            this.startActivity(helpIntent);
            return true;
        } else if (id == R.id.browse_menu_sort_source) {
            this.accessor = Accessor.SOURCE;
            prefs.edit()
                 .putInt("sort", 2)
                 .apply();
            this.loadPuzzleAdapter();
            return true;
        } else if (id == R.id.browse_menu_sort_date_asc) {
            this.accessor = Accessor.DATE_ASC;
            prefs.edit()
                 .putInt("sort", 1)
                 .apply();
            this.loadPuzzleAdapter();
            return true;
        } else if (id == R.id.browse_menu_sort_date_desc) {
            this.accessor = Accessor.DATE_DESC;
            prefs.edit()
                 .putInt("sort", 0)
                 .apply();
            this.loadPuzzleAdapter();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ask others to close down
        sendBroadcast(
            new Intent(BROWSER_CLOSE_ACTION).putExtra(
                BROWSER_CLOSE_TASK_ID, getTaskId()
            )
        );

        // listen for others closing us down
        registerReceiver(
            closeActionReceiver, new IntentFilter(BROWSER_CLOSE_ACTION)
        );

        // Bring up to date
        MigrationHelper.applyMigrations(this, prefs);

        // Now create!

        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);
        this.setContentView(R.layout.browse);
        this.puzzleList = (RecyclerView) this.findViewById(R.id.puzzleList);
        this.puzzleList.setLayoutManager(new LinearLayoutManager(this));
        ItemTouchHelper helper = new ItemTouchHelper(
            new ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.START | ItemTouchHelper.END
            ) {
                @Override
                public int getSwipeDirs(
                    RecyclerView recyclerView,
                    RecyclerView.ViewHolder viewHolder
                ) {
                    if (!(viewHolder instanceof FileViewHolder)
                            || prefs.getBoolean("disableSwipe", false)
                            || !selected.isEmpty()) {
                        return 0; // Don't swipe the headers.
                    }
                    return super.getSwipeDirs(recyclerView, viewHolder);
                }

                @Override
                public boolean onMove(
                    RecyclerView recyclerView,
                    RecyclerView.ViewHolder viewHolder,
                    RecyclerView.ViewHolder viewHolder1
                ) {
                    return false;
                }

                @Override
                public void onSwiped(
                    RecyclerView.ViewHolder viewHolder, int direction
                ) {
                    if(!selected.isEmpty())
                        return;
                    if(!(viewHolder instanceof FileViewHolder))
                        return;

                    PuzMetaFile puzMeta
                        = ((FileViewHolder) viewHolder).getPuzMetaFile();

                    boolean delete = "DELETE".equals(
                        prefs.getString("swipeAction", "DELETE")
                    );
                    if (delete) {
                        model.deletePuzzle(puzMeta);
                    } else {
                        if (model.getIsViewArchive()) {
                            model.movePuzzle(puzMeta, crosswordsFolder);
                        } else {
                            model.movePuzzle(puzMeta, archiveFolder);
                        }
                    }
                }
            });
        helper.attachToRecyclerView(this.puzzleList);
        upgradePreferences();
        this.nm = NotificationManagerCompat.from(this);

        switch (prefs.getInt("sort", 0)) {
        case 2:
            this.accessor = Accessor.SOURCE;
            break;
        case 1:
            this.accessor = Accessor.DATE_ASC;
            break;
        default:
            this.accessor = Accessor.DATE_DESC;
        }
        buttonAdd = findViewById(R.id.speed_dial_add);
        setupSpeedDial();

        SwipeRefreshLayout swipePuzzleReloadView
            = findViewById(R.id.swipeContainer);

        model = new ViewModelProvider(this).get(BrowseActivityViewModel.class);
        model.getPuzzleFiles().observe(this, (v) -> {
            BrowseActivity.this.setViewCrosswordsOrArchiveUI();
            BrowseActivity.this.loadPuzzleAdapter();
            swipePuzzleReloadView.setRefreshing(false);
        });

        pleaseWaitView = findViewById(R.id.please_wait_notice);
        model.getIsUIBusy().observe(this, (isBusy) -> {
            if (isBusy)
                showPleaseWait();
            else
                hidePleaseWait();
        });

        model.getPuzzleLoadEvents().observe(this, (v) -> {
            Intent i = new Intent(BrowseActivity.this, PlayActivity.class);
            BrowseActivity.this.startActivity(i);
        });

        swipePuzzleReloadView.setOnRefreshListener(
             new SwipeRefreshLayout.OnRefreshListener() {
                 @Override
                 public void onRefresh() {
                     startLoadPuzzleList();
                 }
             }
         );

        setViewCrosswordsOrArchiveUI();
        // populated properly inside onResume or with puzzle list
        // observer
        setPuzzleListAdapter(buildEmptyList(), false);

        // If this was started by a file open
        Intent intent = getIntent();
        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action)) {
            // loaded by onResume
            setPendingImport(intent.getData());
        }
    }

    private void setViewCrosswordsOrArchiveUI() {
        boolean viewArchive = model.getIsViewArchive();
        if (viewCrosswordsArchiveMenuItem != null) {
            viewCrosswordsArchiveMenuItem.setTitle(viewArchive
                ? BrowseActivity.this.getString(R.string.title_view_crosswords)
                : BrowseActivity.this.getString(R.string.title_view_archives)
            );
        }
        this.setTitle(viewArchive
            ? BrowseActivity.this.getString(R.string.title_view_archives)
            : BrowseActivity.this.getString(R.string.title_view_crosswords)
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // A background update will commonly happen when the user turns
        // on the preference for the first time, so check here to ensure
        // the UI is re-rendered when they exit the settings dialog.
        if (
            model.getPuzzleFiles().getValue() == null
            || BackgroundDownloadManager.checkBackgroundDownloadPendingFlag()
        ) {
            if (hasPendingImport()) {
                Uri importUri = getPendingImport();
                clearPendingImport();
                onImportURI(importUri, true);

                // won't be triggered by import if archive is shown
                if (model.getIsViewArchive())
                    startLoadPuzzleList();
            } else {
                startLoadPuzzleList();
            }
        } else {
            refreshLastAccessedPuzzle();
        }

        // previous game ended for now
        ForkyzApplication.getInstance().clearBoard();

        autoDownloadIfEnabled();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // permissions checks in onStart to avoid multiple calls via
        // onResume

        if (ForkyzApplication.getInstance().isMissingWritePermission()) {
            boolean showRationale
                = ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                );

            if (showRationale) {
                DialogFragment dialog = new StoragePermissionDialog();
                dialog.show(
                    getSupportFragmentManager(), "StoragePermissionDialog"
                );
            } else {
                requestWritePermission();
            }

            return;
        } else {
            hasWritePermissions = true;
        }

        checkAutoDownloadNotificationPermissions();
    }

    private void refreshLastAccessedPuzzle() {
        final PuzHandle lastAccessed
            = ForkyzApplication.getInstance().getPuzHandle();
        if (lastAccessed == null)
            return;
        model.refreshPuzzleMeta(lastAccessed);
    }

    private SeparatedRecyclerViewAdapter<FileViewHolder, FileAdapter>
    buildEmptyList() {
        return new SeparatedRecyclerViewAdapter<>(
            R.layout.puzzle_list_header,
            FileViewHolder.class
        );
    }

    private SeparatedRecyclerViewAdapter<FileViewHolder, FileAdapter>
    buildList(
        List<MutableLiveData<PuzMetaFile>> puzFiles, Accessor accessor
    ) {
        try {
            Collections.sort(
                puzFiles,
                (pm1, pm2) -> {
                    return accessor.compare(pm1.getValue(), pm2.getValue());
                }
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        SeparatedRecyclerViewAdapter<FileViewHolder, FileAdapter> adapter
            = new SeparatedRecyclerViewAdapter<>(
                R.layout.puzzle_list_header,
                FileViewHolder.class
            );
        String lastHeader = null;
        ArrayList<MutableLiveData<PuzMetaFile>> current = new ArrayList<>();

        for (MutableLiveData<PuzMetaFile> pmData: puzFiles) {
            PuzMetaFile puzMeta = pmData.getValue();

            String check = accessor.getLabel(puzMeta);

            if (!((lastHeader == null) || lastHeader.equals(check))) {
                FileAdapter fa = new FileAdapter(current);
                adapter.addSection(lastHeader, fa);
                current = new ArrayList<>();
            }

            lastHeader = check;
            current.add(pmData);
        }

        if (lastHeader != null) {
            FileAdapter fa = new FileAdapter(current);
            adapter.addSection(lastHeader, fa);
        }

        return adapter;
    }

    private void checkAutoDownloadNotificationPermissions() {
        Downloaders dls = new Downloaders(this, prefs, nm);
        boolean downloading = dls.isDLOnStartup()
            || BackgroundDownloadManager.isBackgroundDownloadEnabled();

        if (dls.isNotificationPermissionNeeded() && downloading)
            checkRequestNotificationPermissions();
    }

    private void autoDownloadIfEnabled() {
        if (!hasWritePermissions) return;

        long lastDL = prefs.getLong("dlLast", 0);
        Downloaders dls = new Downloaders(this, prefs, nm);

        if (dls.isDLOnStartup() &&
                ((System.currentTimeMillis() - (long) (12 * 60 * 60 * 1000)) > lastDL)) {
            model.download(LocalDate.now(), dls.getAutoDownloaders());
            prefs.edit()
                    .putLong("dlLast", System.currentTimeMillis())
                    .apply();
        }
    }

    private void startLoadPuzzleList() {
        startLoadPuzzleList(model.getIsViewArchive());
    }

    private void startLoadPuzzleList(boolean archive) {
        if (!hasWritePermissions) return;

        BackgroundDownloadManager.clearBackgroundDownloadPendingFlag();

        model.startLoadFiles(archive);
    }

    private void loadPuzzleAdapter() {
        cleanUpCurrentAdapter();
        List<MutableLiveData<PuzMetaFile>> puzList
            = model.getPuzzleFiles().getValue();
        if (puzList != null) {
            setPuzzleListAdapter(buildList(puzList, accessor), true);
        } else {
            setPuzzleListAdapter(buildEmptyList(), true);
        }
    }

    /**
     * Before changing adapter, clear up the old one
     */
    private void cleanUpCurrentAdapter() {
        if (currentAdapter != null) {
            for (FileAdapter adapter : currentAdapter.sectionAdapters()) {
                adapter.cleanUpForRemoval();
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void upgradePreferences() {
        // do nothing now no keyboard
    }

    public void onItemClick(final View v, final PuzMetaFile puzMeta) {
        if (!selected.isEmpty()) {
            updateSelection(v, puzMeta);
        } else {
            if (puzMeta == null)
                return;
            model.loadPuzzle(puzMeta);
        }
    }

    public void onItemLongClick(View v, PuzMetaFile puzMeta) {
        if (actionMode == null) {
            startSupportActionMode(actionModeCallback);
        }
        updateSelection(v, puzMeta);
    }

    private void updateSelection(View v, PuzMetaFile puzMeta) {
        if (selected.contains(puzMeta)) {
            setListItemColor(v, false);
            selected.remove(puzMeta);
        } else {
            setListItemColor(v, true);
            selected.add(puzMeta);
        }
        if (selected.isEmpty() && actionMode != null) {
            actionMode.finish();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void clearSelection() {
        selected.clear();
        currentAdapter.notifyDataSetChanged();
    }

    private boolean hasCurrentPuzzleListAdapter() {
        return currentAdapter != null;
    }

    /**
     * Set the puzzle list adapter
     * @param showEmptyMsgs give feedback to user when no files (used to
     * avoid doing so during loading)
     */
    private void setPuzzleListAdapter(
        SeparatedRecyclerViewAdapter<FileViewHolder, FileAdapter> adapter,
        boolean showEmptyMsgs
    ) {
        currentAdapter = adapter;
        puzzleList.setAdapter(adapter);

        TextView emptyMsg = findViewById(R.id.empty_listing_msg);
        TextView storageMsg = findViewById(R.id.internal_storage_msg);

        if (adapter.isEmpty() && showEmptyMsgs) {
            if (model.getIsViewArchive()) {
                emptyMsg.setText(R.string.no_puzzles);
            } else {
                emptyMsg.setText(
                    R.string.no_puzzles_download_or_configure_storage
                );
            }
            emptyMsg.setVisibility(View.VISIBLE);

            if (ForkyzApplication.getInstance().isInternalStorage())
                storageMsg.setVisibility(View.VISIBLE);
            else
                storageMsg.setVisibility(View.GONE);
        } else {
            emptyMsg.setVisibility(View.GONE);
            storageMsg.setVisibility(View.GONE);
        }

        showSpeedDial();
    }

    private void showPleaseWait() {
        pleaseWaitView.setVisibility(View.VISIBLE);
        setSpeedDialVisibility(View.GONE);
    }

    private void hidePleaseWait() {
        pleaseWaitView.setVisibility(View.GONE);
        setSpeedDialVisibility(View.VISIBLE);
    }

    private void setSpeedDialVisibility(int visibility) {
        buttonAdd.setVisibility(visibility);
    }

    /**
     * Unhide the FAB if hidden
     *
     * Distinct from setSpeedDialVisibility in that it uses the standard
     * scroll show/hide feature, rather than setting visibility directly
     */
    private void showSpeedDial() {
        buttonAdd.show();
    }

    private void setupSpeedDial() {
        buttonAdd.inflate(R.menu.speed_dial_browse_menu);

        buttonAdd.setOnActionSelectedListener(
            new SpeedDialView.OnActionSelectedListener() {
                @Override
                public boolean onActionSelected(
                    SpeedDialActionItem actionItem
                ) {
                    int id = actionItem.getId();
                    if (id == R.id.speed_dial_download) {
                        buttonAdd.close();
                        showDownloadDialog();
                        return true;
                    } else if (id == R.id.speed_dial_import) {
                        getImportURI.launch(IMPORT_MIME_TYPE);
                    } else if (id == R.id.speed_dial_online_sources) {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(
                            Uri.parse(getString(R.string.online_sources_url))
                        );
                        startActivity(i);
                    }
                    return false;
                }
            }
        );

        setSpeedDialVisibility(View.VISIBLE);
    }

    /**
     * Import from URIs, does not force reload
     */
    private void onImportURIs(List<Uri> uris) {
        if (uris != null)
            model.importURIs(uris, false);
    }

    /**
     * Import from URI, force reload of puz list if asked
     */
    private void onImportURI(Uri uri, boolean forceReload) {
        if (uri != null)
            model.importURI(uri, forceReload);
    }

    private boolean hasPendingImport() {
        return pendingImport != null;
    }

    private Uri getPendingImport() {
        return pendingImport;
    }

    private void clearPendingImport() {
        pendingImport = null;
    }

    private void setPendingImport(Uri uri) {
        pendingImport = uri;
    }

    private void showDownloadDialog() {
        DialogFragment dialog = new DownloadDialog();
        checkAndWarnNetworkState();
        checkRequestNotificationPermissions();
        dialog.show(getSupportFragmentManager(), "DownloadDialog");
    }

    private void checkAndWarnNetworkState() {
        if (!utils.hasNetworkConnection(this)) {
            Toast t = Toast.makeText(
                this,
                R.string.download_but_no_active_network,
                Toast.LENGTH_LONG
            );
            t.show();
        }
    }

    /**
     * Request notification permissions if needed
     *
     * E.g. not if settings block them. Doesn't ask twice in an
     * activities life
     */
    private void checkRequestNotificationPermissions() {
        Downloaders dls = new Downloaders(this, prefs, nm);
        if (!dls.isNotificationPermissionNeeded())
            return;

        if (nm.areNotificationsEnabled())
            return;

        boolean showRationale
            = utils.shouldShowRequestNotificationPermissionRationale(this);

        if (showRationale) {
            Toast t = Toast.makeText(
                this,
                R.string.notifications_request_rationale,
                Toast.LENGTH_LONG
            );
            t.show();
        }

        utils.requestPostNotifications(notificationPermissionLauncher);
    }

    private void requestWritePermission() {
        writeStorageLauncher.launch(
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        );
    }

    private class FileAdapter
            extends RemovableRecyclerViewAdapter<FileViewHolder> {
        final DateTimeFormatter df
            = DateTimeFormatter.ofPattern("EEEE\n MMM dd, yyyy");
        final ArrayList<MutableLiveData<PuzMetaFile>> objects;
        final Map<MutableLiveData<PuzMetaFile>, Observer<PuzMetaFile>>
            objectObservers = new HashMap<>();

        public FileAdapter(ArrayList<MutableLiveData<PuzMetaFile>> objects) {
            this.objects = objects;
            for (MutableLiveData<PuzMetaFile> pmData : objects) {
                addObserver(pmData);
            }
        }

        @Override
        public FileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.puzzle_list_item, parent, false);
            return new FileViewHolder(view);
        }

        @Override
        public void onBindViewHolder(FileViewHolder holder, int position) {
            View view = holder.itemView;
            MutableLiveData<PuzMetaFile> pmData = objects.get(position);
            PuzMetaFile pm = pmData.getValue();

            holder.setPuzMetaFile(pm);

            view.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    BrowseActivity.this.onItemClick(view, pm);
                }
            });

            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    BrowseActivity.this.onItemLongClick(view, pm);
                    return true;
                }
            });

            TextView date = (TextView) view.findViewById(R.id.puzzle_date);

            date.setText(df.format(pm.getDate()));

            if (accessor == Accessor.SOURCE) {
                date.setVisibility(View.VISIBLE);
            } else {
                date.setVisibility(View.GONE);
            }

            String title = pm.getTitle();
            String caption = pm.getCaption();
            String author = pm.getAuthor();

            TextView titleView = (TextView) view.findViewById(R.id.puzzle_name);

            titleView.setText(smartHtml(title));

            CircleProgressBar bar
                = (CircleProgressBar) view.findViewById(R.id.puzzle_progress);

            bar.setPercentFilled(pm.getFilled());
            bar.setComplete(pm.getComplete() == 100);

            TextView captionView
                = (TextView) view.findViewById(R.id.puzzle_caption);

            // add author if not already in title or caption
            // case insensitive trick:
            // https://www.baeldung.com/java-case-insensitive-string-matching
            String quotedAuthor = Pattern.quote(author);
            boolean addAuthor
                = author.length() > 0
                    && !title.matches("(?i).*" + quotedAuthor + ".*")
                    && !caption.matches("(?i).*" + quotedAuthor + ".*");

            if (addAuthor) {
                captionView.setText(smartHtml(
                    view.getContext().getString(
                        R.string.puzzle_caption_with_author, caption, author
                    )
                ));
            } else {
                captionView.setText(smartHtml(caption));
            }

            setListItemColor(view, selected.contains(pm));
        }

        @Override
        public int getItemCount() {
            return objects.size();
        }

        @Override
        public void remove(int position) {
            objects.remove(position);
        }

        /**
         * Call when adapter is about to be replaced
         *
         * Removes observers from all live data.
         */
        public void cleanUpForRemoval() {
            for (MutableLiveData<PuzMetaFile> pmData : objects)
                removeObserver(pmData);
        }

        /**
         * Only one observer per pmData, removes old if exists
         */
        private void addObserver(MutableLiveData<PuzMetaFile> pmData) {
            if (objectObservers.containsKey(pmData))
                removeObserver(pmData);

            Observer<PuzMetaFile> observer = (v) -> {
                // need to search each time since position may change
                // throughout lifecycle
                int idx = objects.indexOf(pmData);
                if (v == null) {
                    objects.remove(idx);
                    removeObserver(pmData);
                    FileAdapter.this.notifyItemRemoved(idx);
                } else {
                    FileAdapter.this.notifyItemChanged(idx);
                }
            };

            pmData.observe(BrowseActivity.this, observer);
            objectObservers.put(pmData, observer);
        }

        private void removeObserver(MutableLiveData<PuzMetaFile> pmData) {
            Observer<PuzMetaFile> observer = objectObservers.get(pmData);
            if (observer != null) {
                pmData.removeObserver(observer);
                objectObservers.remove(pmData);
            }
        }
    }

    private class FileViewHolder extends RecyclerView.ViewHolder {
        private PuzMetaFile puzMetaFile;

        public FileViewHolder(View itemView) {
            super(itemView);
        }

        public void setPuzMetaFile(PuzMetaFile puzMetaFile) {
            this.puzMetaFile = puzMetaFile;
        }

        public PuzMetaFile getPuzMetaFile() {
            return puzMetaFile;
        }
    }

    public static class DownloadDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            DownloadPickerDialogBuilder.OnDownloadSelectedListener
                downloadButtonListener
                    = new DownloadPickerDialogBuilder
                        .OnDownloadSelectedListener() {
                public void onDownloadSelected(
                    LocalDate d,
                    List<Downloader> downloaders
                ) {
                    BrowseActivityViewModel model
                        = new ViewModelProvider(getActivity())
                            .get(BrowseActivityViewModel.class);

                    model.download(d, downloaders);
                }
            };

            LocalDate d = LocalDate.now();
            BrowseActivity activity = (BrowseActivity) getActivity();

            DownloadPickerDialogBuilder dpd
                = new DownloadPickerDialogBuilder(
                    activity,
                    downloadButtonListener,
                    d.getYear(),
                    d.getMonthValue(),
                    d.getDayOfMonth(),
                    new Downloaders(activity, activity.prefs, activity.nm)
            );

            return dpd.getInstance();
        }
    }

    public static class NotificationPermissionDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            MaterialAlertDialogBuilder builder
                = new MaterialAlertDialogBuilder(getActivity());

            builder.setTitle(getString(R.string.disable_notifications))
                .setMessage(getString(R.string.notifications_denied_msg))
                .setPositiveButton(
                    R.string.disable_notifications_button,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(
                            DialogInterface dialogInterface, int i
                        ) {
                            Context context = getActivity();
                            SharedPreferences prefs
                                = PreferenceManager
                                    .getDefaultSharedPreferences(context);
                            Downloaders dls = new Downloaders(context, prefs);
                            dls.disableNotificationsInPrefs();
                        }
                    }
                ).setNegativeButton(
                    R.string.android_app_settings_button,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(
                            DialogInterface dialogInterface, int i
                        ) {
                            String appPackage = getActivity().getPackageName();
                            Intent intent = new Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:" + appPackage)
                            );
                            intent.addCategory(Intent.CATEGORY_DEFAULT);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    }
                );

            return builder.create();
        }
    }

    public class StoragePermissionDialog extends DialogFragment {
        public static final String RESULT_CODE_KEY = "resultCode";

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            MaterialAlertDialogBuilder builder
                = new MaterialAlertDialogBuilder(getActivity());

            builder.setTitle(R.string.allow_permissions)
                .setMessage(R.string.please_allow_storage)
                .setPositiveButton(
                    android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(
                            DialogInterface dialogInterface, int i
                        ) {
                            requestWritePermission();
                        }
                    }
                );

            return builder.create();
        }
    }
}
