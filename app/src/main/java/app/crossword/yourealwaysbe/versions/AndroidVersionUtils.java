package app.crossword.yourealwaysbe.versions;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

public interface AndroidVersionUtils {

    void setContext(Context ctx);

    void finishOnHomeButton(AppCompatActivity a);

    void holographic(AppCompatActivity playActivity);

    void onActionBarWithText(MenuItem a);

    void onActionBarWithText(SubMenu reveal);

    class Factory {
        private static AndroidVersionUtils INSTANCE;

        public static AndroidVersionUtils getInstance() {
            if (INSTANCE != null)
                return INSTANCE;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                return INSTANCE = new RUtil();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                return INSTANCE = new PieUtil();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                return INSTANCE = new OreoUtil();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                return INSTANCE = new MarshmallowUtil();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                return INSTANCE = new LollipopUtil();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
                return INSTANCE = new JellyBeanMR2Util();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                return INSTANCE = new JellyBeanMR1Util();

            return INSTANCE = new IceCreamSandwichUtil();
        }
    }

    View onActionBarCustom(AppCompatActivity a, int id);

    void hideActionBar(AppCompatActivity a);

    void onActionBarWithoutText(MenuItem a);

    void hideTitleOnPortrait(AppCompatActivity a);

    void createNotificationChannel(Context context);

    int immutablePendingIntentFlag();

    StaticLayout getStaticLayout(CharSequence text, TextPaint style, int width);

    void migrateLegacyBackgroundDownloads();

    boolean isMiniTabletish(DisplayMetrics metrics);

    void setFullScreen(Window window);

    ActivityResultLauncher<String> registerForUriContentsResult(
        AppCompatActivity activity, Consumer<List<Uri>> uriConsumer
    );

    void finishAndRemoveTask(Activity activity);

    Typeface getSemiBoldTypeface();

    boolean isInternalStorageFull(
        Context context, long minimumBytesFree
    ) throws IOException;

    boolean isExternalStorageDirectoryFull(
        File directory, long minimumBytesFree
    );

    boolean isSAFSupported();
}
