package app.crossword.yourealwaysbe.versions;

import java.util.function.Consumer;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree;
import androidx.fragment.app.Fragment;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.util.BackgroundDownloadManager;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class LollipopUtil extends KitKatUtil {

    private static final String PREF_LEGACY_BACKGROUND_DOWNLOAD
        = "backgroundDownload";

    @Override
    public void migrateLegacyBackgroundDownloads(SharedPreferences prefs) {
        boolean legacyEnabled
            = prefs.getBoolean(PREF_LEGACY_BACKGROUND_DOWNLOAD, false);

        if (legacyEnabled) {
            // clear old
            prefs.edit()
                .remove(PREF_LEGACY_BACKGROUND_DOWNLOAD)
                .apply();

            ForkyzApplication app = ForkyzApplication.getInstance();
            JobScheduler scheduler = (JobScheduler)
                app.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            scheduler.cancelAll();

            // start new
            BackgroundDownloadManager.setHourlyBackgroundDownloadPeriod();
        }
    }

    @Override
    public void finishAndRemoveTask(Activity activity) {
        activity.finishAndRemoveTask();
    }

    /**
     * SAF support for Forkyz rather than Android
     *
     * Android had it in KitKat, but didn't have the document tree access we
     * need until now.
     */
    @Override
    public boolean isSAFSupported() {
        return true;
    }

    @Override
    public ActivityResultLauncher<Uri> registerForSAFUriResult(
        Fragment fragment, Consumer<Uri> uriConsumer
    ) {
        return fragment.registerForActivityResult(
            new OpenDocumentTree(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    uriConsumer.accept(uri);
                }
            }
        );
    }
}
