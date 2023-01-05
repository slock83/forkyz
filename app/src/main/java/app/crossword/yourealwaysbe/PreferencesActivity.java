package app.crossword.yourealwaysbe;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.util.ThemeHelper;
import app.crossword.yourealwaysbe.util.NightModeHelper;
import app.crossword.yourealwaysbe.versions.AndroidVersionUtils;

public class PreferencesActivity
       extends AppCompatActivity
       implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    public static final String NIGHT_MODE = "nightMode";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.preferences_activity);

        SharedPreferences prefs
            = PreferenceManager.getDefaultSharedPreferences(this);

        (new ThemeHelper(this, prefs)).themeActivity(this);

        NightModeHelper nightMode = NightModeHelper.bind(this);
        nightMode.restoreNightMode();

        AndroidVersionUtils utils = AndroidVersionUtils.Factory.getInstance();
        utils.holographic(this);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                                       .replace(R.id.preferencesActivity,
                                                new PreferencesFragment())
                                       .commit();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        FragmentManager fragmentManager = getSupportFragmentManager();

        if (fragmentManager.getBackStackEntryCount() == 0) {
            finish();
            return true;
        }

        if (fragmentManager.popBackStackImmediate()) {
            return true;
        }

        return super.onSupportNavigateUp();
    }

    // from https://developer.android.com/guide/topics/ui/settings/organize-your-settings
    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller,
                                             Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment
            = getSupportFragmentManager().getFragmentFactory()
                                         .instantiate(getClassLoader(),
                                                      pref.getFragment());
        fragment.setArguments(args);

        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.preferencesActivity, fragment)
                .addToBackStack(null)
                .commit();
        return true;
    }
}
