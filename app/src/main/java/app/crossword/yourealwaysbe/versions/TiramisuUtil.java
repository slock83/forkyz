
package app.crossword.yourealwaysbe.versions;

import java.io.Serializable;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.core.app.ActivityCompat;

@TargetApi(Build.VERSION_CODES.TIRAMISU)
public class TiramisuUtil extends RUtil {
    @Override
    public <T extends Serializable>
    T getSerializable(Bundle bundle, String key, Class<T> klass) {
        return bundle.getSerializable(key, klass);
    }

    @Override
    public void requestPostNotifications(
        ActivityResultLauncher<String> launcher
    ) {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    @Override
    public boolean shouldShowRequestNotificationPermissionRationale(
        Activity activity
    ) {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity, Manifest.permission.POST_NOTIFICATIONS
        );
    }
}
