package app.crossword.yourealwaysbe.versions;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View.OnClickListener;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class IceCreamSandwichUtil implements AndroidVersionUtils {
    @Override
    public void setContext(Context ctx) { }

    @Override
    public boolean isMiniTabletish(DisplayMetrics metrics) {
        double x = Math.pow(metrics.widthPixels/metrics.xdpi,2);
        double y = Math.pow(metrics.heightPixels/metrics.ydpi,2);
        double screenInches = Math.sqrt(x+y);
        if (screenInches > 5.5 && screenInches <= 9) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void finishOnHomeButton(final AppCompatActivity a) {
        ActionBar bar = a.getSupportActionBar();
        if(bar == null){
            return;
        }
        bar.setDisplayHomeAsUpEnabled(true);
        View home = a.findViewById(android.R.id.home);
        if(home != null){
            home.setOnClickListener(new OnClickListener() {
                    public void onClick(View arg0) {
                        a.finish();
                    }
                });
        }
    }

    @Override
    public void holographic(AppCompatActivity a) {
        ActionBar bar = a.getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onActionBarWithText(MenuItem a) {
        a.setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT + MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }

    public void onActionBarWithoutText(MenuItem a) {
        a.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM + MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }

    public void hideTitleOnPortrait(AppCompatActivity a) {
        if(a.getResources().getConfiguration().orientation != Configuration.ORIENTATION_PORTRAIT){
            return;
        }
        ActionBar bar = a.getSupportActionBar();
        if (bar != null) {
            bar.setDisplayShowTitleEnabled(false);
        }
    }

    public void onActionBarWithText(SubMenu a) {
        this.onActionBarWithText(a.getItem());
    }

    public View onActionBarCustom(AppCompatActivity a, int id) {
        ActionBar bar = a.getSupportActionBar();
        if(bar == null){
            return null;
        }
        bar.setDisplayShowCustomEnabled(true);
        bar.setDisplayShowTitleEnabled(false);
        bar.setDisplayShowHomeEnabled(true);
        bar.setCustomView(id);
        return bar.getCustomView();
    }

    public void hideActionBar(AppCompatActivity a) {
        ActionBar ab = a.getSupportActionBar();
        if(ab == null){
            return;
        }
        ab.hide();
    }

    @Override
    public void createNotificationChannel(Context context) {

    }

    @Override
    public int immutablePendingIntentFlag() {
        return 0;
    }

    @SuppressWarnings("deprecation")
    @Override
    public StaticLayout getStaticLayout(
        CharSequence text, TextPaint style, int width
    ) {
        return new StaticLayout(
            text,
            style, width, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0, false
        );
    }

    @Override
    public void migrateLegacyBackgroundDownloads() {
        // do nothing: legacy background download needed lollipop
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setFullScreen(Window window) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
    }

    @Override
    public ActivityResultLauncher<String> registerForUriContentsResult(
        AppCompatActivity activity, Consumer<List<Uri>> uriConsumer
    ) {
        return activity.registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    uriConsumer.accept(Collections.singletonList(uri));
                }
            }
        );
    }

    @Override
    public void finishAndRemoveTask(Activity activity) {
        activity.finish();
    }
}
