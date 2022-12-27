package app.crossword.yourealwaysbe.net;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import app.crossword.yourealwaysbe.BrowseActivity;
import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.io.BrainsOnlyIO;
import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.io.JPZIO;
import app.crossword.yourealwaysbe.io.PrzekrojIO;
import app.crossword.yourealwaysbe.util.files.FileHandler;
import app.crossword.yourealwaysbe.versions.AndroidVersionUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Downloaders {
    private static final Logger LOG
        = Logger.getLogger("app.crossword.yourealwaysbe");
    private static final int NUM_DOWNLOAD_THREADS = 3;

    public static final String PREF_AUTO_DOWNLOADERS = "autoDownloaders";
    public static final String PREF_DOWNLOAD_TIMEOUT = "downloadTimeout";
    private static final String DEFAULT_TIMEOUT_MILLIS = "30000";

    private Context context;
    private NotificationManager notificationManager;
    private boolean suppressSummaryMessages;
    private boolean suppressIndividualMessages;
    private SharedPreferences prefs;

    public Downloaders(SharedPreferences prefs,
                       NotificationManager notificationManager,
                       Context context) {
        this(prefs, notificationManager, context, true);
    }

    // Set isInteractive to true if this class can ask for user interaction when needed (e.g. to
    // refresh NYT credentials), false if otherwise.
    public Downloaders(SharedPreferences prefs,
                       NotificationManager notificationManager,
                       Context context,
                       boolean challengeForCredentials) {
        this.prefs = prefs;
        this.notificationManager = notificationManager;
        this.context = context;
        this.suppressSummaryMessages
            = prefs.getBoolean("supressSummaryMessages", false);
        this.suppressIndividualMessages
            = prefs.getBoolean("supressMessages", false);
    }

    public List<Downloader> getDownloaders() {
        return getDownloaders(prefs, context);
    }

    public List<Downloader> getDownloaders(LocalDate date) {
        List<Downloader> retVal = new LinkedList<Downloader>();
        for (Downloader d : getDownloaders()) {
            if (d.isAvailable(date))
                retVal.add(d);
        }
        return retVal;
    }

    public void download(LocalDate date) {
        download(date, getDownloaders(date));
    }

    // Downloads the latest puzzles newer/equal to than the given date for the given set of
    // downloaders.
    //
    // If downloaders is null, then the full list of downloaders will be used.
    public void downloadLatestInRange(
        LocalDate from, LocalDate to, List<Downloader> downloaders
    ) {
        if (downloaders == null) {
            downloaders = getDownloaders();
        }

        HashMap<Downloader, LocalDate> puzzlesToDownload
            = new HashMap<Downloader, LocalDate>();
        for (Downloader d : downloaders) {
            LocalDate latestDate = d.getLatestDate(to);
            if (latestDate != null && !latestDate.isBefore(from)) {
                LOG.info(
                    "Will try to download puzzle " + d + " @ " + latestDate
                );
                puzzlesToDownload.put(d, latestDate);
            }
        }

        if (!puzzlesToDownload.isEmpty()) {
            download(puzzlesToDownload);
        }
    }

    public void download(LocalDate date, List<Downloader> downloaders) {
        if ((downloaders == null) || (downloaders.size() == 0)) {
            downloaders = getDownloaders(date);
        }

        HashMap<Downloader, LocalDate> puzzlesToDownload = new HashMap<Downloader, LocalDate>();
        for (Downloader d : downloaders) {
            puzzlesToDownload.put(d, date);
        }

        download(puzzlesToDownload);
    }

    public static boolean isDLOnStartup(Context context) {
        SharedPreferences prefs
            = PreferenceManager.getDefaultSharedPreferences(
                context
            );
        return prefs.getBoolean("dlOnStartup", false);
    }

    public List<Downloader> getAutoDownloaders() {
        return getAutoDownloaders(context);
    }

    public static List<Downloader> getAutoDownloaders(Context context) {
        SharedPreferences prefs
            = PreferenceManager.getDefaultSharedPreferences(context);

        List<Downloader> available = getDownloaders(prefs, context);

        Set<String> autoDownloaders = prefs.getStringSet(
            PREF_AUTO_DOWNLOADERS, Collections.emptySet()
        );

        List<Downloader> downloaders = new LinkedList<>();
        for (Downloader downloader : available) {
            if (autoDownloaders.contains(downloader.getInternalName()))
                downloaders.add(downloader);
        }

        return downloaders;
    }

    public static List<Downloader> getDownloaders(Context context) {
        SharedPreferences prefs
            = PreferenceManager.getDefaultSharedPreferences(context);
        return getDownloaders(prefs, context);
    }

    /**
     * Handle introduction of second selection of auto downloaders
     */
    public static void migrateAutoDownloaders(Context context) {
        SharedPreferences prefs
            = PreferenceManager.getDefaultSharedPreferences(context);

        Set<String> autoDownloaders
            = prefs.getStringSet(PREF_AUTO_DOWNLOADERS, null);

        if (autoDownloaders == null) {
            autoDownloaders = new HashSet<>();

            List<Downloader> downloaders = getDownloaders(prefs, context);

            for (Downloader downloader : downloaders) {
                autoDownloaders.add(downloader.getInternalName());
            }

            prefs.edit()
                .putStringSet(PREF_AUTO_DOWNLOADERS, autoDownloaders)
                .apply();
        }
    }

    private void download(Map<Downloader, LocalDate> puzzlesToDownload) {
        boolean hasConnection =
            AndroidVersionUtils.Factory.getInstance()
                .hasNetworkConnection(context);

        if (!hasConnection) {
            notifyNoConnection();
            return;
        }

        FileHandler fileHandler
            = ForkyzApplication.getInstance().getFileHandler();

        NotificationCompat.Builder not =
                new NotificationCompat.Builder(context, ForkyzApplication.PUZZLE_DOWNLOAD_CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.stat_sys_download)
                        .setContentTitle(context.getString(
                            R.string.puzzles_downloading
                        ))
                        .setWhen(System.currentTimeMillis());

        int nextNotificationId = 1;
        Set<String> fileNames = fileHandler.getPuzzleNames();

        ExecutorService downloadExecutor
            = Executors.newFixedThreadPool(NUM_DOWNLOAD_THREADS);
        final AtomicBoolean somethingDownloaded = new AtomicBoolean(false);
        final AtomicBoolean somethingFailed = new AtomicBoolean(false);

        for (
            Map.Entry<Downloader, LocalDate> puzzle
                : puzzlesToDownload.entrySet()
        ) {
            int notificationId = nextNotificationId++;
            Downloader downloader = puzzle.getKey();
            LocalDate date = puzzle.getValue();

            downloadExecutor.submit(() -> {
                Downloader.DownloadResult result = downloadPuzzle(
                    downloader,
                    date,
                    not,
                    notificationId,
                    fileNames
                );
                if (result.isSuccess())
                    somethingDownloaded.set(true);
                else if (result.isFailed())
                    somethingFailed.set(true);
            });
        }

        downloadExecutor.shutdown();

        try {
            downloadExecutor.awaitTermination(
                Long.MAX_VALUE, TimeUnit.MILLISECONDS
            );
        } catch (InterruptedException e) {
            // Oh well
        }

        if (this.notificationManager != null) {
            this.notificationManager.cancel(0);
        }

        if (!this.suppressSummaryMessages) {
            this.postDownloadedGeneral(
                somethingDownloaded.get(), somethingFailed.get()
            );
        }
    }

    /**
     * Download and save the puzzle from the downloader
     *
     * Only saves if we don't already have it.
     */
    private Downloader.DownloadResult downloadPuzzle(
        Downloader d,
        LocalDate date,
        NotificationCompat.Builder not,
        int notificationId,
        Set<String> existingFileNames
    ) {
        FileHandler fileHandler
            = ForkyzApplication.getInstance().getFileHandler();

        LOG.info("Downloading " + d.toString());

        // failed unless proven otherwise!
        Downloader.DownloadResult result = Downloader.DownloadResult.FAILED;

        try {
            String contentText = context.getString(
                R.string.puzzles_downloading_from, d.getName()
            );
            not.setContentText(contentText)
                .setContentIntent(getContentIntent());

            boolean notify = !this.suppressIndividualMessages
                && this.notificationManager != null;

            if (notify) {
                this.notificationManager.notify(0, not.build());
            }

            result = d.download(date, existingFileNames);

            if (result.isSuccess()) {
                String fileName = result.getFileName();
                if (!existingFileNames.contains(fileName)) {
                    fileHandler.saveNewPuzzle(
                        result.getPuzzle(), fileName
                    );
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to download "+d.getName(), e);
        }

        if (!this.suppressIndividualMessages) {
            this.postDownloadedNotification(
                notificationId, d.getName(), result
            );
        }

        return result;
    }

    private void postDownloadedGeneral(
        Boolean somethingDownloaded, Boolean somethingFailed
    ) {
        int messageId;
        if (somethingDownloaded && somethingFailed)
            messageId = R.string.puzzles_downloaded_some;
        else if (somethingDownloaded)
            messageId = R.string.puzzles_downloaded_all;
        else if (somethingFailed)
            messageId = R.string.puzzles_downloaded_none;
        else // nothing downloaded or failed
            return;

        Notification not = new NotificationCompat.Builder(
            context, ForkyzApplication.PUZZLE_DOWNLOAD_CHANNEL_ID
        ).setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(context.getString(messageId))
            .setContentIntent(getContentIntent())
            .setWhen(System.currentTimeMillis())
            .build();

        if (this.notificationManager != null) {
            this.notificationManager.notify(0, not);
        }
    }

    private void postDownloadedNotification(
        int i, String name, Downloader.DownloadResult result
    ) {
        // don't notify unless success or failure
        // notifications about existing puzzles would be annoying
        if (!(result.isSuccess() || result.isFailed()))
            return;

        int messageId = result.isSuccess()
            ? R.string.puzzle_downloaded
            : R.string.puzzle_download_failed;

        Notification not
            = new NotificationCompat.Builder(
            context, ForkyzApplication.PUZZLE_DOWNLOAD_CHANNEL_ID
        ).setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentIntent(getContentIntent())
            .setContentTitle(context.getString(messageId, name))
            .setWhen(System.currentTimeMillis())
            .build();

        if (this.notificationManager != null) {
            this.notificationManager.notify(i, not);
        }
    }

    // package access for testing
    static List<Downloader> getDownloaders(
        SharedPreferences prefs, Context context
    ) {
        List<Downloader> downloaders = new LinkedList<>();

        if (prefs.getBoolean("downloadGuardianDailyCryptic", true)) {
            downloaders.add(new GuardianDailyCrypticDownloader(
                "guardian", context.getString(R.string.guardian_daily)
            ));
        }

        if (prefs.getBoolean("downloadHamAbend", true)) {
            downloaders.add(new RaetselZentraleSchwedenDownloader(
                "hamabend",
                context.getString(R.string.hamburger_abendblatt_daily),
                "hhab",
                Downloader.DATE_DAILY,
                Duration.ofHours(1), // midnight Germany (verified)
                "https://www.abendblatt.de/plus",
                "'https://www.abendblatt.de/ratgeber/wissen/"
                    + "article106560367/Spielen-Sie-hier-taeglich"
                    + "-das-kostenlose-Kreuzwortraetsel.html'"
            ));
        }

        if (prefs.getBoolean("downloadIndependentDailyCryptic", true)) {
            downloaders.add(new AbstractDateDownloader(
                "independent",
                context.getString(R.string.independent_daily),
                Downloader.DATE_DAILY,
                Duration.ZERO, // midnight UK (actually further in advance)
                "https://www.independent.co.uk/donations",
                new JPZIO(),
                "'https://ams.cdn.arkadiumhosted.com/assets/gamesfeed/"
                    + "independent/daily-crossword/c_'yyMMdd'.xml'",
                "'https://puzzles.independent.co.uk/games/"
                    + "cryptic-crossword-independent'"
            ));
        }

        if (prefs.getBoolean("downloadJonesin", true)) {
            downloaders.add(new AbstractDateDownloader(
                "jonesin",
                context.getString(R.string.jonesin_crosswords),
                Downloader.DATE_THURSDAY,
                Duration.ofDays(-2), // by experiment
                "https://crosswordnexus.com/jonesin/",
                new IO(),
                "'https://herbach.dnsalias.com/Jonesin/jz'yyMMdd'.puz'",
                "'https://crosswordnexus.com/solve/?"
                    + "puzzle=/downloads/jonesin/jonesin'yyMMdd'.puz'"
            ));
        }

        if (prefs.getBoolean("downloadJoseph", true)) {
            downloaders.add(new KingDigitalDownloader(
                "Joseph",
                "Joseph",
                context.getString(R.string.joseph_crossword),
                Downloader.DATE_NO_SUNDAY,
                Duration.ofDays(-7), // more than a week by experiment
                "https://puzzles.kingdigital.com",
                "'https://www.arkadium.com/games/"
                    + "joseph-crossword-kingsfeatures/'"
            ));
        }

        if (prefs.getBoolean("downloadLeParisien", true)) {
            downloaders.add(new LeParisienDownloader(
                "leparisien", context.getString(R.string.le_parisien_daily)
            ));
        }

        if (prefs.getBoolean("downloadNewsday", true)) {
            downloaders.add(new AbstractDateDownloader(
                "newsday",
                context.getString(R.string.newsday),
                Downloader.DATE_DAILY,
                Duration.ofHours(5), // midnight US? (by experiment)
                // i can't browse this site for a more specific URL
                // (GDPR)
                "https://www.newsday.com",
                new BrainsOnlyIO(),
                "'https://brainsonly.com/servlets-newsday-crossword/"
                    + "newsdaycrossword?date='yyMMdd",
                "'https://www.newsday.com'"
            ));
        }

        if (prefs.getBoolean("downloadPremier", true)) {
            downloaders.add(new KingDigitalDownloader(
                "Premier",
                "Premier",
                context.getString(R.string.premier_crossword),
                Downloader.DATE_SUNDAY,
                Duration.ofDays(-7), // more than a week by experiment
                "https://puzzles.kingdigital.com",
                "'https://www.arkadium.com/games/"
                    + "premier-crossword-kingsfeatures/'"
            ));
        }

        if (prefs.getBoolean("downloadSheffer", true)) {
            downloaders.add(new KingDigitalDownloader(
                "Sheffer",
                "Sheffer",
                context.getString(R.string.sheffer_crossword),
                Downloader.DATE_NO_SUNDAY,
                Duration.ofDays(-7), // more than a week by experiment
                "https://puzzles.kingdigital.com",
                "'https://www.arkadium.com/games/"
                    + "sheffer-crossword-kingsfeatures/'"
            ));
        }

        if (prefs.getBoolean("downloadUniversal", true)) {
            downloaders.add(new UclickDownloader(
                "universal",
                "fcx",
                context.getString(R.string.universal_crossword),
                context.getString(R.string.uclick_copyright),
                "http://www.uclick.com/client/spi/fcx/",
                Downloader.DATE_DAILY,
                Duration.ofMinutes(6 * 60 + 30), // 06:30 by experiment
                null
            ));
        }

        if (prefs.getBoolean("downloadUSAToday", true)) {
            downloaders.add(new UclickDownloader(
                "usatoday",
                "usaon",
                context.getString(R.string.usa_today),
                context.getString(R.string.usa_today),
                "https://subscribe.usatoday.com",
                Downloader.DATE_DAILY,
                Duration.ofMinutes(6 * 60 + 30), // 06:30 by experiment
                "'https://games.usatoday.com/en/games/uclick-crossword'"
            ));
        }

        if (prefs.getBoolean("downloadWaPoSunday", true)) {
            downloaders.add(new AbstractDateDownloader(
                "waposunday",
                context.getString(R.string.washington_post_sunday),
                Downloader.DATE_SUNDAY,
                Duration.ofHours(-1), // by experiment
                "https://subscribe.wsj.com",
                new IO(),
                "'https://herbach.dnsalias.com/Wapo/wp'yyMMdd'.puz'",
                "'https://subscribe.washingtonpost.com'"
            ));
        }

        if (prefs.getBoolean("downloadWsj", true)) {
            downloaders.add(new AbstractDateDownloader(
                "wsj",
                context.getString(R.string.wall_street_journal),
                Downloader.DATE_NO_SUNDAY,
                Duration.ofHours(-3), // by experiment
                "https://subscribe.wsj.com",
                new IO(),
                "'https://herbach.dnsalias.com/wsj/wsj'yyMMdd'.puz'",
                "'https://www.wsj.com/news/puzzle'"
            ));
        }

        addCustomDownloaders(prefs, context, downloaders);

        if (prefs.getBoolean("scrapeCru", false)) {
            downloaders.add(new PageScraper.Puz(
                // certificate doesn't seem to work for me
                // "https://theworld.com/~wij/puzzles/cru/index.html",
                "https://archive.nytimes.com/www.nytimes.com/premium/xword/cryptic-archive.html",
                "crypticcru",
                context.getString(R.string.cru_puzzle_workshop),
                "https://archive.nytimes.com/www.nytimes.com/premium/xword/cryptic-archive.html"
            ));
        }

        if (prefs.getBoolean("scrapeKegler", false)) {
            downloaders.add(new PageScraper.Puz(
                "https://kegler.gitlab.io/Block_style/index.html",
                "keglar",
                context.getString(R.string.keglars_cryptics),
                "https://kegler.gitlab.io/"
            ));
        }

        if (prefs.getBoolean("scrapePrivateEye", false)) {
            downloaders.add(new PageScraper.Puz(
                "https://www.private-eye.co.uk/pictures/crossword/download/",
                "privateeye",
                context.getString(R.string.private_eye),
                "https://shop.private-eye.co.uk",
                true // download from end of page
            ));
        }

        if (prefs.getBoolean("scrapePrzekroj", false)) {
            downloaders.add(new PageScraper(
                ".*krzyzowki/\\d+",
                new PrzekrojIO(),
                "https://przekroj.pl/rozrywka/krzyzowki/",
                "przekroj",
                context.getString(R.string.przekroj),
                "https://przekroj.pl/shop/kiosk",
                true, // share file url
                false // read top down
            ));
        }

        // Set timeout before returning
        int timeout = Integer.valueOf(
            prefs.getString(PREF_DOWNLOAD_TIMEOUT, DEFAULT_TIMEOUT_MILLIS)
        );
        for (Downloader downloader : downloaders)
            downloader.setTimeout(timeout);

        return downloaders;
    }

    private static void addCustomDownloaders(
        SharedPreferences prefs, Context context, List<Downloader> downloaders
    ) {
        if (prefs.getBoolean("downloadCustomDaily", true)) {
            String title = prefs.getString("customDailyTitle", "");
            if (title == null || title.trim().isEmpty())
                title = context.getString(R.string.custom_daily_title);

            String urlDateFormatPattern
                = prefs.getString("customDailyUrl", "");

            downloaders.add(
                new CustomDailyDownloader("custom", title, urlDateFormatPattern)
            );
        }
    }

    private void notifyNoConnection() {
        if (suppressSummaryMessages)
            return;

        Notification not = new NotificationCompat.Builder(
            context, ForkyzApplication.PUZZLE_DOWNLOAD_CHANNEL_ID
        ).setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(
                context.getString(R.string.puzzle_download_no_connection)
            )
            .setContentIntent(getContentIntent())
            .setWhen(System.currentTimeMillis())
            .build();

        if (this.notificationManager != null) {
            this.notificationManager.notify(0, not);
        }
    }

    /**
     * Make a content intent for a notification
     */
    private PendingIntent getContentIntent() {
        Intent notificationIntent = new Intent(context, BrowseActivity.class);
        return PendingIntent.getActivity(
            context, 0, notificationIntent,
            AndroidVersionUtils
                .Factory.getInstance().immutablePendingIntentFlag()
        );
    }
}
