package app.crossword.yourealwaysbe.net;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.core.app.NotificationCompat;

import app.crossword.yourealwaysbe.BrowseActivity;
import app.crossword.yourealwaysbe.PlayActivity;
import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.io.BrainsOnlyIO;
import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.io.JPZIO;
import app.crossword.yourealwaysbe.io.PrzekrojIO;
import app.crossword.yourealwaysbe.util.files.FileHandler;
import app.crossword.yourealwaysbe.versions.AndroidVersionUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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

    public List<Downloader> getDownloaders(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        List<Downloader> retVal = new LinkedList<Downloader>();

        for (Downloader d : getDownloadersFromPrefs()) {
            // TODO: Downloader.getGoodThrough() should account for the day of week.
            if (Arrays.binarySearch(d.getDownloadDates(), dayOfWeek) >= 0) {
                LocalDate dGoodFrom = d.getGoodFrom();
                boolean isGoodFrom
                    = date.isEqual(dGoodFrom) || date.isAfter(dGoodFrom);
                LocalDate dGoodThrough = d.getGoodThrough();
                boolean isGoodThrough
                    = date.isBefore(dGoodThrough) || date.isEqual(dGoodThrough);

                if(isGoodFrom && isGoodThrough) {
                    retVal.add(d);
                }
            }
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
    public void downloadLatestIfNewerThanDate(LocalDate oldestDate, List<Downloader> downloaders) {
        if (downloaders == null) {
            downloaders = new ArrayList<Downloader>();
        }

        if (downloaders.size() == 0) {
            downloaders.addAll(getDownloadersFromPrefs());
        }

        HashMap<Downloader, LocalDate> puzzlesToDownload = new HashMap<Downloader, LocalDate>();
        for (Downloader d : downloaders) {
            LocalDate goodThrough = d.getGoodThrough();
            DayOfWeek goodThroughDayOfWeek = goodThrough.getDayOfWeek();
            boolean isDay
                = Arrays.binarySearch(
                    d.getDownloadDates(), goodThroughDayOfWeek
                ) >= 0;
            boolean isGoodThrough
                = goodThrough.isEqual(oldestDate)
                    || goodThrough.isAfter(oldestDate);
            if (isDay && isGoodThrough) {
                LOG.info("Will try to download puzzle " + d + " @ " + goodThrough);
                puzzlesToDownload.put(d, goodThrough);
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

    private void download(Map<Downloader, LocalDate> puzzlesToDownload) {
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
            Intent notificationIntent = new Intent(context, PlayActivity.class);
            PendingIntent contentIntent = PendingIntent.getActivity(
                context, 0, notificationIntent,
                AndroidVersionUtils
                    .Factory.getInstance().immutablePendingIntentFlag()
            );

            not.setContentText(contentText)
                .setContentIntent(contentIntent);

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

        Intent notificationIntent = new Intent(Intent.ACTION_EDIT, null,
                context, BrowseActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(
            context, 0, notificationIntent,
            AndroidVersionUtils
                .Factory.getInstance().immutablePendingIntentFlag()
        );

        Notification not = new NotificationCompat.Builder(
            context, ForkyzApplication.PUZZLE_DOWNLOAD_CHANNEL_ID
        ).setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(context.getString(messageId))
            .setContentIntent(contentIntent)
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

        Intent notificationIntent = new Intent(
            Intent.ACTION_EDIT, null, context, BrowseActivity.class
        );
        PendingIntent contentIntent = PendingIntent.getActivity(
            context, 0, notificationIntent,
            AndroidVersionUtils
                .Factory.getInstance().immutablePendingIntentFlag()
        );

        Notification not
            = new NotificationCompat.Builder(
            context, ForkyzApplication.PUZZLE_DOWNLOAD_CHANNEL_ID
        ).setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentIntent(contentIntent)
            .setContentTitle(context.getString(messageId, name))
            .setWhen(System.currentTimeMillis())
            .build();

        if (this.notificationManager != null) {
            this.notificationManager.notify(i, not);
        }
    }

    private List<Downloader> getDownloadersFromPrefs() {
        List<Downloader> downloaders = new LinkedList<>();

        if (prefs.getBoolean("downloadGuardianDailyCryptic", true)) {
            downloaders.add(new GuardianDailyCrypticDownloader());
        }

        if (prefs.getBoolean("downloadHamAbend", true)) {
            downloaders.add(new RaetselZentraleSchwedenDownloader(
                context.getString(R.string.hamburger_abendblatt_daily),
                "hhab",
                Downloader.DATE_DAILY,
                "https://www.abendblatt.de/plus",
                "'https://www.abendblatt.de/ratgeber/wissen/"
                    + "article106560367/Spielen-Sie-hier-taeglich"
                    + "-das-kostenlose-Kreuzwortraetsel.html'"
            ));
        }

        if (prefs.getBoolean("downloadIndependentDailyCryptic", true)) {
            downloaders.add(new AbstractDateDownloader(
                context.getString(R.string.independent_daily),
                Downloader.DATE_DAILY,
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
                context.getString(R.string.jonesin_crosswords),
                Downloader.DATE_THURSDAY,
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
                context.getString(R.string.joseph_crossword),
                Downloader.DATE_NO_SUNDAY,
                "https://puzzles.kingdigital.com",
                "'https://www.arkadium.com/games/"
                    + "joseph-crossword-kingsfeatures/'"
            ));
        }

        if (prefs.getBoolean("downloadLeParisien", true)) {
            downloaders.add(new LeParisienDownloader());
        }

        if (prefs.getBoolean("downloadNewsday", true)) {
            downloaders.add(new AbstractDateDownloader(
                context.getString(R.string.newsday),
                Downloader.DATE_DAILY,
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
                context.getString(R.string.premier_crossword),
                Downloader.DATE_SUNDAY,
                "https://puzzles.kingdigital.com",
                "'https://www.arkadium.com/games/"
                    + "premier-crossword-kingsfeatures/'"
            ));
        }

        if (prefs.getBoolean("downloadSheffer", true)) {
            downloaders.add(new KingDigitalDownloader(
                "Sheffer",
                context.getString(R.string.sheffer_crossword),
                Downloader.DATE_NO_SUNDAY,
                "https://puzzles.kingdigital.com",
                "'https://www.arkadium.com/games/"
                    + "sheffer-crossword-kingsfeatures/'"
            ));
        }

        if (prefs.getBoolean("downloadUniversal", true)) {
            downloaders.add(new UclickDownloader(
                "fcx",
                context.getString(R.string.universal_crossword),
                context.getString(R.string.uclick_copyright),
                "http://www.uclick.com/client/spi/fcx/",
                Downloader.DATE_DAILY,
                null
            ));
        }

        if (prefs.getBoolean("downloadUSAToday", true)) {
            downloaders.add(new UclickDownloader(
                "usaon",
                context.getString(R.string.usa_today),
                context.getString(R.string.usa_today),
                "https://subscribe.usatoday.com",
                Downloader.DATE_NO_SUNDAY,
                "'https://games.usatoday.com/en/games/uclick-crossword'"
            ));
        }

        if (prefs.getBoolean("downloadWaPoSunday", true)) {
            downloaders.add(new AbstractDateDownloader(
                context.getString(R.string.washington_post_sunday),
                Downloader.DATE_SUNDAY,
                "https://subscribe.wsj.com",
                new IO(),
                "'https://herbach.dnsalias.com/Wapo/wp'yyMMdd'.puz'",
                "'https://subscribe.washingtonpost.com'"
            ));
        }

        if (prefs.getBoolean("downloadWsj", true)) {
            downloaders.add(new AbstractDateDownloader(
                context.getString(R.string.wall_street_journal),
                Downloader.DATE_NO_SUNDAY,
                "https://subscribe.wsj.com",
                new IO(),
                "'https://herbach.dnsalias.com/wsj/wsj'yyMMdd'.puz'",
                "'https://www.wsj.com/news/puzzle'"
            ));
        }

        addCustomDownloaders(downloaders);

        if (prefs.getBoolean("scrapeCru", false)) {
            downloaders.add(new PageScraper.Puz(
                // certificate doesn't seem to work for me
                // "https://theworld.com/~wij/puzzles/cru/index.html",
                "https://archive.nytimes.com/www.nytimes.com/premium/xword/cryptic-archive.html",
                context.getString(R.string.cru_puzzle_workshop),
                "https://archive.nytimes.com/www.nytimes.com/premium/xword/cryptic-archive.html"
            ));
        }

        if (prefs.getBoolean("scrapeKegler", false)) {
            downloaders.add(new PageScraper.Puz(
                "https://kegler.gitlab.io/Block_style/index.html",
                context.getString(R.string.keglars_cryptics),
                "https://kegler.gitlab.io/"
            ));
        }

        if (prefs.getBoolean("scrapePrivateEye", false)) {
            downloaders.add(new PageScraper.Puz(
                "https://www.private-eye.co.uk/pictures/crossword/download/",
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
                context.getString(R.string.przekroj),
                "https://przekroj.pl/shop/kiosk",
                true, // share file url
                false // read top down
            ));
        }

        return downloaders;
    }

    private void addCustomDownloaders(List<Downloader> downloaders) {
        if (prefs.getBoolean("downloadCustomDaily", true)) {
            String title = prefs.getString("customDailyTitle", "");
            String urlDateFormatPattern
                = prefs.getString("customDailyUrl", "");

            downloaders.add(
                new CustomDailyDownloader(title, urlDateFormatPattern)
            );
        }
    }
}
