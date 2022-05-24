package app.crossword.yourealwaysbe.versions;

import android.annotation.TargetApi;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.preference.PreferenceManager;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.util.BackgroundDownloadManager;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class LollipopUtil extends JellyBeanMR1Util {

    private static final String PREF_LEGACY_BACKGROUND_DOWNLOAD
        = "backgroundDownload";

    @Override
    public void migrateLegacyBackgroundDownloads() {
        ForkyzApplication app = ForkyzApplication.getInstance();

        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(app);

        boolean legacyEnabled
            = prefs.getBoolean(PREF_LEGACY_BACKGROUND_DOWNLOAD, false);

        if (legacyEnabled) {
            // clear old
            prefs.edit()
                .remove(PREF_LEGACY_BACKGROUND_DOWNLOAD)
                .apply();

            JobScheduler scheduler =
                (JobScheduler)
                    app.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            scheduler.cancelAll();

            // start new
            BackgroundDownloadManager.setHourlyBackgroundDownloadPeriod();
        }
    }
}
