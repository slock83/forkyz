package app.crossword.yourealwaysbe.versions;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.function.Consumer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                return INSTANCE = new TiramisuUtil();
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

    StaticLayout getStaticLayout(
        CharSequence text, TextPaint style, int width, Layout.Alignment align
    );

    default StaticLayout getStaticLayout(
        CharSequence text, TextPaint style, int width
    ) {
        return getStaticLayout(
            text, style, width, Layout.Alignment.ALIGN_NORMAL
        );
    }

    void migrateLegacyBackgroundDownloads(SharedPreferences prefs);

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

    ActivityResultLauncher<Uri> registerForSAFUriResult(
        Fragment fragment, Consumer<Uri> uriConsumer
    );

    /**
     * True if the character can go into a box
     *
     * I.e. not a control char of some kind
     */
    boolean isAcceptableCharacterResponse(char c);

    boolean hasNetworkConnection(Context context);

    /**
     * Convenience method for type-safe Bundle.getSerializalbe
     *
     * Should end up in a BundleCompat one day, i hope.
     */
    <T extends Serializable> T
    getSerializable(Bundle bundle, String key, Class<T> klass);
}
