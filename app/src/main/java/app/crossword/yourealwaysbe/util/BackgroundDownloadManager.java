
package app.crossword.yourealwaysbe.util;

import java.time.LocalDate;
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

    private static final String PREF_DOWNLOAD_PENDING = "backgroundDlPending";
    private static final String PREF_DOWNLOAD_PERIOD
        = "backgroundDownloadPeriod";
    private static final String DOWNLOAD_WORK_NAME = "backgroundDownload";

    public static void updateBackgroundDownloads(Context context) {
        SharedPreferences prefs = getPrefs(context);

        int period = Integer.valueOf(
            prefs.getString(PREF_DOWNLOAD_PERIOD, "0")
        );

        if (period > 0)
            scheduleBackgroundDownload(context, period);
        else
            cancelBackgroundDownload(context);
    }

    public static boolean checkBackgroundDownload(Context context) {
        if (ForkyzApplication.getInstance().isMissingWritePermission())
            return false;

        SharedPreferences prefs = getPrefs(context);

        boolean isPending = prefs.getBoolean(PREF_DOWNLOAD_PENDING, false);

        clearBackgroundDownload(context);

        return isPending;
    }

    public static void clearBackgroundDownload(Context context) {
        SharedPreferences prefs = getPrefs(context);

        prefs.edit()
            .putBoolean(PREF_DOWNLOAD_PENDING, false)
            .apply();
    }

    /**
     * Get download period from preferences
     *
     * @return period in hours, 0 if no background download
     */
    public static int getBackgroundDownloadPeriod(Context context) {
        SharedPreferences prefs = getPrefs(context);
        return prefs.getInt(PREF_DOWNLOAD_PERIOD, 0);
    }

    /**
     * Set the download period and update work schedule
     *
     * If this changes the work period, then call
     * updateBackgroundDownloads.
     *
     * @param hours time period in hours, 0 means no auto download
     */
    public static void setBackgroundDownloadPeriod(Context context, int period) {
        int prevPeriod = getBackgroundDownloadPeriod(context);

        if (prevPeriod != period) {
            SharedPreferences prefs = getPrefs(context);

            prefs.edit()
                .putString(PREF_DOWNLOAD_PERIOD, String.valueOf(period))
                .apply();

            updateBackgroundDownloads(context);
        }
    }

    /**
     * Schedule download every period hours
     */
    private static void scheduleBackgroundDownload(
        Context context, int period
    ) {
        Constraints constraints = getConstraints(context);

        PeriodicWorkRequest request
            = new PeriodicWorkRequest.Builder(
                DownloadWorker.class, period, TimeUnit.HOURS
            ).setConstraints(constraints)
            .build();

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                DOWNLOAD_WORK_NAME, ExistingPeriodicWorkPolicy.REPLACE, request
            );
    }

    private static void cancelBackgroundDownload(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(DOWNLOAD_WORK_NAME);
    }

    private static Constraints getConstraints(Context context) {
        SharedPreferences prefs
            = PreferenceManager.getDefaultSharedPreferences(context);

        boolean requireUnmetered
            = prefs.getBoolean("backgroundDownloadRequireUnmetered", true);
        boolean allowRoaming
            = prefs.getBoolean("backgroundDownloadAllowRoaming", false);
        boolean requireCharging
            = prefs.getBoolean("backgroundDownloadRequireCharging", false);

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

    public static class DownloadWorker extends Worker {
        private Context context;

        public DownloadWorker(Context context, WorkerParameters params) {
            super(context, params);
            this.context = context;
        }

        @Override
        public ListenableWorker.Result doWork() {
            NotificationManager nm =
                (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (ForkyzApplication.getInstance().isMissingWritePermission()) {
                LOGGER.info("Skipping download, no write permission");
                return ListenableWorker.Result.failure();
            }

            LOGGER.info("Downloading most recent puzzles");

            SharedPreferences prefs
                = PreferenceManager.getDefaultSharedPreferences(context);

            final Downloaders dls = new Downloaders(prefs, nm, context, false);
            dls.downloadLatestIfNewerThanDate(LocalDate.now(), null);

            // This is used to tell BrowseActivity that puzzles may have
            // been updated while paused.
            prefs.edit()
                .putBoolean(PREF_DOWNLOAD_PENDING, true)
                .apply();

            return ListenableWorker.Result.success();
        }
    }

    private static SharedPreferences getPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
