package app.crossword.yourealwaysbe.util;

import java.util.Objects;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import com.google.android.material.color.DynamicColors;

import app.crossword.yourealwaysbe.forkyz.R;

public class ThemeHelper {
    public static final String PREF_THEME = "applicationTheme";

    private static final String PREF_LEGACY_USE_DYNAMIC = "useDynamicColors";

    private static enum Theme {
        STANDARD, DYNAMIC, LEGACY_LIKE;
    }

    public static void themeApplication(Application app) {
        if (isDynamicColors(app))
            DynamicColors.applyToActivitiesIfAvailable(app);
    }

    public static void themeActivity(Activity activity) {
        if (isLegacyLikeTheme(activity))
            activity.setTheme(R.style.Theme_Forkyz_LegacyLike);
    }

    public static void migrateThemePreferences(Context context) {
        SharedPreferences prefs = getPrefs(context);
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

    private static boolean isDynamicColors(Context context) {
        return getThemeType(context) == Theme.DYNAMIC;
    }

    private static boolean isLegacyLikeTheme(Context context) {
        return getThemeType(context) == Theme.LEGACY_LIKE;
    }

    private static Theme getThemeType(Context context) {
        String standardTheme = context.getString(R.string.standard_theme_value);
        String dynamicTheme = context.getString(R.string.dynamic_theme_value);
        String legacyLikeTheme
            = context.getString(R.string.legacy_like_theme_value);

        SharedPreferences prefs = getPrefs(context);
        String theme = prefs.getString(PREF_THEME, standardTheme);

        if (Objects.equals(theme, dynamicTheme))
            return Theme.DYNAMIC;
        else if (Objects.equals(theme, legacyLikeTheme))
            return Theme.LEGACY_LIKE;
        else
            return Theme.STANDARD;
    }

    private static SharedPreferences getPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
