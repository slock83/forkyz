package app.crossword.yourealwaysbe;

import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;
import androidx.preference.PreferenceManager;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.util.NightModeHelper;
import app.crossword.yourealwaysbe.util.ThemeHelper;
import app.crossword.yourealwaysbe.util.files.FileHandler;
import app.crossword.yourealwaysbe.util.files.FileHandlerSAF;
import app.crossword.yourealwaysbe.versions.AndroidVersionUtils;

import java.util.logging.Logger;

public class ForkyzActivity extends AppCompatActivity {
    private static final Logger LOG
        = Logger.getLogger(ForkyzActivity.class.getCanonicalName());

    protected AndroidVersionUtils utils = AndroidVersionUtils.Factory
            .getInstance();
    protected SharedPreferences prefs;
    public NightModeHelper nightMode;

    private OnSharedPreferenceChangeListener prefChangeListener
        = new OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(
                SharedPreferences prefs, String key
            ) {
                if (ForkyzApplication.STORAGE_LOC_PREF.equals(key)
                        || FileHandlerSAF.SAF_ROOT_URI_PREF.equals(key)) {
                    ForkyzActivity.this.finish();
                }
            }
        };

    protected FileHandler getFileHandler() {
        return ForkyzApplication.getInstance().getFileHandler();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.prefs = PreferenceManager.getDefaultSharedPreferences(this);

        this.prefs.registerOnSharedPreferenceChangeListener(prefChangeListener);

        final FileHandler fileHandler = getFileHandler();

        doOrientation();
        (new ThemeHelper(this, this.prefs)).themeActivity(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        nightMode.restoreNightMode();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(nightMode == null) {
            nightMode = NightModeHelper.bind(this);
            nightMode.restoreNightMode();
        }

        doOrientation();
    }

    @SuppressWarnings("SourceLockedOrientationActivity")
    private void doOrientation() {
        try {
            if ("PORT".equals(prefs.getString("orientationLock", "UNLOCKED"))) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else if ("LAND"
                    .equals(prefs.getString("orientationLock", "UNLOCKED"))) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            }
        } catch(RuntimeException e) {
            Toast.makeText(this, "Sorry, orientation lock is not supported without " +
                    "fullscreen mode anymore because of an Android change.", Toast.LENGTH_LONG).show();
        }
    }

    protected Bitmap createBitmap(String fontFile, String character){
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int dpi = Math.round(160F * metrics.density);
        int size = dpi / 2;
        Bitmap bitmap = Bitmap.createBitmap(size , size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint p = new Paint();
        p.setColor(Color.WHITE);
        p.setStyle(Paint.Style.FILL);
        p.setTypeface(Typeface.createFromAsset(getAssets(), fontFile));
        p.setTextSize(size);
        p.setAntiAlias(true);
        p.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(character, size/2, size - size / 9, p );
        return bitmap;
    }

    protected static Spanned smartHtml(String text) {
        return text == null ? null : HtmlCompat.fromHtml(text, 0);
    }
}
