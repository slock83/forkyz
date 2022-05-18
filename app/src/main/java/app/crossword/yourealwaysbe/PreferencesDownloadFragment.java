package app.crossword.yourealwaysbe;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.util.BackgroundDownloadManager;

public class PreferencesDownloadFragment
        extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences_download, rootKey);
    }

    @Override
    public void onResume() {
        PreferenceManager
            .getDefaultSharedPreferences(getActivity().getApplicationContext())
            .registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        PreferenceManager
            .getDefaultSharedPreferences(getActivity().getApplicationContext())
            .unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(
        SharedPreferences sharedPreferences, String pref
    ) {
        if (BackgroundDownloadManager.isBackgroundDownloadConfigPref(pref))
            BackgroundDownloadManager.updateBackgroundDownloads();
    }
}
