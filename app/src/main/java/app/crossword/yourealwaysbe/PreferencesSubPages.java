package app.crossword.yourealwaysbe;

import java.util.List;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import com.google.android.material.color.DynamicColors;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.net.Downloader;
import app.crossword.yourealwaysbe.net.Downloaders;
import app.crossword.yourealwaysbe.util.BackgroundDownloadManager;
import app.crossword.yourealwaysbe.util.ThemeHelper;

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
            setAvailableDownloaders();
        }

        private void setAvailableDownloaders() {
            SharedPreferences prefs
                = PreferenceManager.getDefaultSharedPreferences(getActivity());

            List<Downloader> downloaders
                = (new Downloaders(getActivity(), prefs)).getDownloaders();

            int len = downloaders.size();
            CharSequence[] values = new CharSequence[len];
            CharSequence[] labels = new CharSequence[len];

            // done this way because i know the downloaders list is a linked
            // list
            int index = 0;
            for (Downloader downloader : downloaders) {
                values[index] = downloader.getInternalName();
                labels[index] = downloader.getName();
                index += 1;
            }

            MultiSelectListPreference available = findPreference(
                Downloaders.PREF_AUTO_DOWNLOADERS
            );
            available.setEntries(labels);
            available.setEntryValues(values);
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

    public static class VoiceFragment extends PreferencesBaseFragment {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_voice, rootKey);

            findPreference("aboutVoiceCommands")
                    .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference arg0) {
                        Intent i = new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(
                                "file:///android_asset/voice_commands.html"
                            ),
                            getActivity(),
                            HTMLActivity.class
                        );
                        getActivity().startActivity(i);
                        return true;
                    }
                });
        }
    }

    public static class DisplayFragment extends PreferencesBaseFragment {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_display, rootKey);
            if (!DynamicColors.isDynamicColorAvailable()) {

                ListPreference themePref
                    = findPreference(ThemeHelper.PREF_THEME);

                Resources res = getResources();
                themePref.setEntries(
                    res.getStringArray(R.array.themeTypeLabelsNoDynamic)
                );
                themePref.setEntryValues(
                    res.getStringArray(R.array.themeTypeValuesNoDynamic)
                );
            }
        }
    }
}
