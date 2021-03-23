package app.crossword.yourealwaysbe.versions;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View.OnClickListener;
import android.view.View;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import app.crossword.yourealwaysbe.ForkyzActivity;
import app.crossword.yourealwaysbe.net.DownloadReceiver;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;
import app.crossword.yourealwaysbe.util.NightModeHelper;
import app.crossword.yourealwaysbe.util.files.DirHandle;

public class HoneycombUtil extends DefaultUtil {

    public void setContext(Context ctx) { }

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
    public boolean isNightModeAvailable(){
        return true;
    }

    @Override
    public void nextNightMode(ForkyzActivity activity){
        activity.nightMode.next();
        if(activity.nightMode.isNightMode()){
            AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_YES
            );
        } else {
            AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_NO
            );
        }
    }

    @Override
    public void restoreNightMode(ForkyzActivity forkyzActivity) {
        restoreNightMode(forkyzActivity.nightMode);
    }

    @Override
    public void restoreNightMode(NightModeHelper nightMode) {
        nightMode.restoreNightMode();
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

    public void hideWindowTitle(AppCompatActivity a) {
        // no op;
    }

    public void hideActionBar(AppCompatActivity a) {
        ActionBar ab = a.getSupportActionBar();
        if(ab == null){
            return;
        }
        ab.hide();
    }

    public void storeMetas(Uri uri, PuzzleMeta meta, DirHandle parentDir) {
        DownloadReceiver.metas.put(uri,
            new DownloadReceiver.Metas(meta, parentDir)
        );
    }

    public DownloadReceiver.Metas removeMetas(Uri uri) {
        return DownloadReceiver.metas.remove(uri);
    }

    @Override
    public boolean isBackgroundDownloadAvaliable() {
        return false;
    }

    @Override
    public boolean checkBackgroundDownload(SharedPreferences prefs, boolean hasWritePermissions) {
        return false;
    }

    @Override
    public void clearBackgroundDownload(SharedPreferences prefs) {

    }

    @Override
    public void createNotificationChannel(Context context) {

    }


}
