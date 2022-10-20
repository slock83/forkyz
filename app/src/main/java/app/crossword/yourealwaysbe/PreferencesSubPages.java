package app.crossword.yourealwaysbe;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import com.google.android.material.color.DynamicColors;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.util.BackgroundDownloadManager;

public class PreferencesSubPages {
    public static class SourcesFragment extends PreferencesBaseFragment {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_sources, rootKey);
        }
    }

    public static class DailyFragment
            extends PreferencesBaseFragment {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_daily, rootKey);
        }
    }

    public static class WeeklyFragment extends PreferencesBaseFragment {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_weekly, rootKey);
        }
    }

    public static class ScraperFragment extends PreferencesBaseFragment {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            // Load the preferences from an XML resource
            setPreferencesFromResource(R.xml.preferences_scrapers, rootKey);

            findPreference("aboutScrapes")
                    .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference arg0) {
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("file:///android_asset/scrapes.html"),
                                getActivity(), HTMLActivity.class);
                        getActivity().startActivity(i);
                        return true;
                    }
                });
        }
    }

    public static class DownloadFragment extends PreferencesBaseFragment {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_download, rootKey);
        }
    }

    public static class DownloadBackgroundFragment
            extends PreferencesBaseFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(
                R.xml.preferences_download_background, rootKey
            );
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

    public static class BrowserFragment extends PreferencesBaseFragment {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_browser, rootKey);
        }
    }

    public static class KeyboardFragment extends PreferencesBaseFragment {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_keyboard, rootKey);
        }
    }

    public static class InteractionFragment extends PreferencesBaseFragment {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_interaction, rootKey);
        }
    }

    public static class DisplayFragment extends PreferencesBaseFragment {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_display, rootKey);
            findPreference("useDynamicColors")
                .setVisible(DynamicColors.isDynamicColorAvailable());
        }
    }
}
