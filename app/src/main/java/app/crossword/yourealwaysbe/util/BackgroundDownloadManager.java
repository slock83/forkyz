
package app.crossword.yourealwaysbe.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.NetworkType;
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

    private static final String DOWNLOAD_WORK_NAME_HOURLY
        = "backgroundDownloadHourly";
    private static final String DOWNLOAD_WORK_NAME_DAY_PREFIX
        = "backgroundDownloadDay";

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

    public static void updateBackgroundDownloads() {
        cancelBackgroundDownloads();

        SharedPreferences prefs = getPrefs();

        boolean hourly = prefs.getBoolean(PREF_DOWNLOAD_HOURLY, false);
        if (hourly)
            scheduleBackgroundDownload(DOWNLOAD_WORK_NAME_HOURLY, 0, 1);

        Set<String> days = prefs.getStringSet(
            PREF_DOWNLOAD_DAYS, Collections.emptySet()
        );
        int downloadTime
            = Integer.valueOf(prefs.getString(PREF_DOWNLOAD_DAYS_TIME, "8"));

        for (String dayString : days) {
            int day = Integer.valueOf(dayString);
            String workName = getDayOfWeekWorkName(day);
            long delay = getDelay(day, downloadTime);
            scheduleBackgroundDownload(workName, delay, 7*24);
        }
    }

    public static boolean checkBackgroundDownload() {
        if (ForkyzApplication.getInstance().isMissingWritePermission())
            return false;

        SharedPreferences prefs = getPrefs();

        boolean isPending = prefs.getBoolean(PREF_DOWNLOAD_PENDING, false);

        clearBackgroundDownload();

        return isPending;
    }

    public static void clearBackgroundDownload() {
        SharedPreferences prefs = getPrefs();

        prefs.edit()
            .putBoolean(PREF_DOWNLOAD_PENDING, false)
            .apply();
    }

    /**
     * Set the download period to 1 hour
     */
    public static void setHourlyBackgroundDownloadPeriod() {
        clearPreferences();

        getPrefs().edit()
            .putBoolean(PREF_DOWNLOAD_HOURLY, true)
            .apply();

        updateBackgroundDownloads();
    }

    private static void clearPreferences() {
        SharedPreferences.Editor edit = getPrefs().edit();
        for (String pref : CONFIG_PREFERENCES) {
            edit.remove(pref);
        }
        edit.apply();
    }

    /**
     * Schedule download every period hours with ms delay
     */
    private static void scheduleBackgroundDownload(
        String workName, long delayMillis, int periodHours
    ) {
        Constraints constraints = getConstraints();

        PeriodicWorkRequest request
            = new PeriodicWorkRequest.Builder(
                DownloadWorker.class, periodHours, TimeUnit.HOURS
            ).setConstraints(constraints)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build();

        getWorkManager()
            .enqueueUniquePeriodicWork(
                workName, ExistingPeriodicWorkPolicy.REPLACE, request
            );
    }

    private static void cancelBackgroundDownloads() {
        WorkManager manager = getWorkManager();

        manager.cancelUniqueWork(DOWNLOAD_WORK_NAME_HOURLY);

        for (DayOfWeek day : DayOfWeek.values()) {
            manager.cancelUniqueWork(getDayOfWeekWorkName(day.getValue()));
        }
    }

    private static String getDayOfWeekWorkName(int dayOfWeek) {
        return DOWNLOAD_WORK_NAME_DAY_PREFIX + dayOfWeek;
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

        if (nextDownload.isBefore(LocalDateTime.now()))
            nextDownload = nextDownload.plusDays(7);

        return ChronoUnit.MILLIS.between(now, nextDownload);
    }

    private static SharedPreferences getPrefs() {
        Context context = ForkyzApplication.getInstance();
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    private static WorkManager getWorkManager() {
        return WorkManager.getInstance(ForkyzApplication.getInstance());
    }


    public static class DownloadWorker extends Worker {
        public DownloadWorker(Context context, WorkerParameters params) {
            super(context, params);
        }

        @Override
        public ListenableWorker.Result doWork() {
            ForkyzApplication app = ForkyzApplication.getInstance();

            NotificationManager nm =
                (NotificationManager)
                    app.getSystemService(Context.NOTIFICATION_SERVICE);

            if (app.isMissingWritePermission()) {
                LOGGER.info("Skipping download, no write permission");
                return ListenableWorker.Result.failure();
            }

            LOGGER.info("Downloading most recent puzzles");

            SharedPreferences prefs = BackgroundDownloadManager.getPrefs();

            final Downloaders dls = new Downloaders(prefs, nm, app, false);
            dls.downloadLatestIfNewerThanDate(LocalDate.now(), null);

            // This is used to tell BrowseActivity that puzzles may have
            // been updated while paused.
            prefs.edit()
                .putBoolean(PREF_DOWNLOAD_PENDING, true)
                .apply();

            return ListenableWorker.Result.success();
        }
    }
}
