package app.crossword.yourealwaysbe.util;

import java.util.Objects;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import com.google.android.material.color.DynamicColors;

import app.crossword.yourealwaysbe.forkyz.R;

public class ThemeHelper {
    public static final String PREF_THEME = "applicationTheme";

    private static final String PREF_LEGACY_USE_DYNAMIC = "useDynamicColors";

    private static enum Theme {
        STANDARD, DYNAMIC, LEGACY_LIKE;
    }

    private Context context;
    private SharedPreferences prefs;

    public ThemeHelper(Context context, SharedPreferences prefs) {
        this.context = context;
        this.prefs = prefs;
    }

    public void themeApplication(Application app) {
        if (isDynamicColors())
            DynamicColors.applyToActivitiesIfAvailable(app);
    }

    public void themeActivity(Activity activity) {
        if (isLegacyLikeTheme())
            activity.setTheme(R.style.Theme_Forkyz_LegacyLike);
    }

    public void migrateThemePreferences() {
        boolean legacyDynamic = prefs.getBoolean(PREF_LEGACY_USE_DYNAMIC, false);
        if (legacyDynamic) {
           prefs.edit()
                .remove(PREF_LEGACY_USE_DYNAMIC)
                .putString(
                    PREF_THEME, context.getString(R.string.dynamic_theme_value)
                )
                .apply();
        }
    }

    private boolean isDynamicColors() {
        return getThemeType() == Theme.DYNAMIC;
    }

    private boolean isLegacyLikeTheme() {
        return getThemeType() == Theme.LEGACY_LIKE;
    }

    private Theme getThemeType() {
        String standardTheme = context.getString(R.string.standard_theme_value);
        String dynamicTheme = context.getString(R.string.dynamic_theme_value);
        String legacyLikeTheme
            = context.getString(R.string.legacy_like_theme_value);

        String theme = prefs.getString(PREF_THEME, standardTheme);

        if (Objects.equals(theme, dynamicTheme))
            return Theme.DYNAMIC;
        else if (Objects.equals(theme, legacyLikeTheme))
            return Theme.LEGACY_LIKE;
        else
            return Theme.STANDARD;
    }
}
