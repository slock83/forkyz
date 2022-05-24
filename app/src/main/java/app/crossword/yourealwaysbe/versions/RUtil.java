package app.crossword.yourealwaysbe.versions;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

@TargetApi(Build.VERSION_CODES.R)
public class RUtil extends PieUtil {
    @Override
    public void setFullScreen(Window window) {
        WindowInsetsController insetsController
            = window.getInsetsController();
        if (insetsController != null) {
            insetsController.hide(WindowInsets.Type.statusBars());
        }
    }
}
