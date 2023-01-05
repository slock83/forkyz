
package app.crossword.yourealwaysbe.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.net.Downloaders;

/**
 * Schedule background downloads using Android WorkManager
 */
public class BackgroundDownloadManager {
    private static final Logger LOGGER = Logger.getLogger(
        BackgroundDownloadManager.class.getCanonicalName()
    );

    private static final String PREF_DOWNLOAD_UNMETERED
        = "backgroundDownloadRequireUnmetered";
    private static final String PREF_DOWNLOAD_ROAMING
        = "backgroundDownloadAllowRoaming";
    private static final String PREF_DOWNLOAD_CHARGING
        = "backgroundDownloadRequireCharging";

    private static final String PREF_DOWNLOAD_HOURLY
        = "backgroundDownloadHourly";
    private static final String PREF_DOWNLOAD_DAYS
        = "backgroundDownloadDays";
    private static final String PREF_DOWNLOAD_DAYS_TIME
        = "backgroundDownloadDaysTime";

    private static final String DOWNLOAD_WORK_NAME
        = "backgroundDownload";

    private static final String PREF_DOWNLOAD_PENDING = "backgroundDlPending";

    private static final Set<String> CONFIG_PREFERENCES = new HashSet<>(
        Arrays.asList(
            PREF_DOWNLOAD_UNMETERED,
            PREF_DOWNLOAD_ROAMING,
            PREF_DOWNLOAD_CHARGING,
            PREF_DOWNLOAD_HOURLY,
            PREF_DOWNLOAD_DAYS,
            PREF_DOWNLOAD_DAYS_TIME
        )
    );

    /**
     * For preference activity to detect if config changes
     */
    public static boolean isBackgroundDownloadConfigPref(String pref) {
        return CONFIG_PREFERENCES.contains(pref);
    }

    public static boolean isBackgroundDownloadEnabled() {
        return getPrefs().getBoolean(PREF_DOWNLOAD_HOURLY, false)
            || getNextDailyDownloadDelay() >= 0;
    }

    public static void updateBackgroundDownloads() {
        cancelBackgroundDownloads();

        SharedPreferences prefs = getPrefs();

        boolean hourly = prefs.getBoolean(PREF_DOWNLOAD_HOURLY, false);
        if (hourly)
            scheduleHourlyDownloads();
        else
            scheduleNextDailyDownload();
    }

    public static void scheduleHourlyDownloads() {
        LOGGER.info("Scheduling hourly downloads");

        PeriodicWorkRequest request
            = new PeriodicWorkRequest.Builder(
                HourlyDownloadWorker.class, 1, TimeUnit.HOURS
            ).setConstraints(getConstraints())
            .build();

        getWorkManager()
            .enqueueUniquePeriodicWork(
                DOWNLOAD_WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                request
            );
    }

    public static void scheduleNextDailyDownload() {
        long nextDelay = getNextDailyDownloadDelay();
        if (nextDelay < 0)
            return;

        LOGGER.info("Scheduling next daily download in " + nextDelay + "ms");

        OneTimeWorkRequest request
            = new OneTimeWorkRequest.Builder(DailyDownloadWorker.class)
                .setConstraints(getConstraints())
                .setInitialDelay(nextDelay, TimeUnit.MILLISECONDS)
                .build();

        getWorkManager()
            .enqueueUniqueWork(
                DOWNLOAD_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            );
    }

    public static boolean checkBackgroundDownloadPendingFlag() {
        if (ForkyzApplication.getInstance().isMissingWritePermission())
            return false;

        SharedPreferences prefs = getPrefs();

        boolean isPending = prefs.getBoolean(PREF_DOWNLOAD_PENDING, false);

        clearBackgroundDownloadPendingFlag();

        return isPending;
    }

    public static void clearBackgroundDownloadPendingFlag() {
        SharedPreferences prefs = getPrefs();

        prefs.edit()
            .putBoolean(PREF_DOWNLOAD_PENDING, false)
            .apply();
    }

    /**
     * Set the download period to 1 hour
     */
    public static void setHourlyBackgroundDownloadPeriod() {
        getPrefs().edit()
            .putBoolean(PREF_DOWNLOAD_HOURLY, true)
            .apply();

        updateBackgroundDownloads();
    }

    /**
     * Number of millis to next daily download
     *
     * @return -1 if none to schedule
     */
    private static long getNextDailyDownloadDelay() {
        SharedPreferences prefs = getPrefs();

        Set<String> days = prefs.getStringSet(
            PREF_DOWNLOAD_DAYS, Collections.emptySet()
        );
        int downloadTime
            = Integer.valueOf(prefs.getString(PREF_DOWNLOAD_DAYS_TIME, "8"));

        long nextDownloadDelay = -1;

        for (String dayString : days) {
            int day = Integer.valueOf(dayString);
            long delay = getDelay(day, downloadTime);
            if (nextDownloadDelay < 0 || delay < nextDownloadDelay)
                nextDownloadDelay = delay;
        }

        return nextDownloadDelay;
    }

    private static void cancelBackgroundDownloads() {
        getWorkManager().cancelUniqueWork(DOWNLOAD_WORK_NAME);
    }

    private static Constraints getConstraints() {
        SharedPreferences prefs = getPrefs();

        boolean requireUnmetered
            = prefs.getBoolean(PREF_DOWNLOAD_UNMETERED, true);
        boolean allowRoaming
            = prefs.getBoolean(PREF_DOWNLOAD_ROAMING, false);
        boolean requireCharging
            = prefs.getBoolean(PREF_DOWNLOAD_CHARGING, false);

        Constraints.Builder constraintsBuilder = new Constraints.Builder();

        if (requireUnmetered)
            constraintsBuilder.setRequiredNetworkType(NetworkType.UNMETERED);
        else if (!allowRoaming)
            constraintsBuilder.setRequiredNetworkType(NetworkType.NOT_ROAMING);
        else
            constraintsBuilder.setRequiredNetworkType(NetworkType.CONNECTED);

        constraintsBuilder.setRequiresCharging(requireCharging);

        return constraintsBuilder.build();
    }

    /**
     * Get num millis to next day/time
     *
     * @param dayOfWeek 1-7 like java DayOfWeek
     * @param hourOfDay 0-23
     */
    private static long getDelay(int dayOfWeek, int hourOfDay) {
        // start from now and adjust
        LocalDateTime now = LocalDateTime.now();
        int nowDayOfWeek = now.getDayOfWeek().getValue();

        LocalDateTime nextDownload
            = now.plusDays(dayOfWeek - nowDayOfWeek)
                .withHour(hourOfDay)
                .truncatedTo(ChronoUnit.HOURS);

        if (!nextDownload.isAfter(LocalDateTime.now()))
            nextDownload = nextDownload.plusDays(7);

        return ChronoUnit.MILLIS.between(now, nextDownload);
    }

    private static SharedPreferences getPrefs() {
        Context context = ForkyzApplication.getInstance();
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static WorkManager getWorkManager() {
        return WorkManager.getInstance(ForkyzApplication.getInstance());
    }

    private static abstract class BaseDownloadWorker extends Worker {
        public BaseDownloadWorker(Context context, WorkerParameters params) {
            super(context, params);
        }

        protected void doDownload() {
            ForkyzApplication app = ForkyzApplication.getInstance();

            NotificationManagerCompat nm = NotificationManagerCompat.from(app);

            if (app.isMissingWritePermission()) {
                LOGGER.info("Skipping download, no write permission");
                return;
            }

            LOGGER.info("Downloading most recent puzzles");

            SharedPreferences prefs = BackgroundDownloadManager.getPrefs();

            final Downloaders dls = new Downloaders(app, prefs, nm);
            LocalDate now = LocalDate.now();
            dls.downloadLatestInRange(now, now, dls.getAutoDownloaders());

            // This is used to tell BrowseActivity that puzzles may have
            // been updated while paused.
            prefs.edit()
                .putBoolean(PREF_DOWNLOAD_PENDING, true)
                .apply();

            return;
        }
    }

    public static class HourlyDownloadWorker extends BaseDownloadWorker {
        public HourlyDownloadWorker(Context context, WorkerParameters params) {
            super(context, params);
        }

        @Override
        public ListenableWorker.Result doWork() {
            doDownload();
            return ListenableWorker.Result.success();
        }
    }

    public static class DailyDownloadWorker extends BaseDownloadWorker {
        public DailyDownloadWorker(Context context, WorkerParameters params) {
            super(context, params);
        }

        @Override
        public ListenableWorker.Result doWork() {
            // do this first in case doDownload goes horribly wrong
            scheduleNextDailyDownload();
            doDownload();
            return ListenableWorker.Result.success();
        }
    }
}
