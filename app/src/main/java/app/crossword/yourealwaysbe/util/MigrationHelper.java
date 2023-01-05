package app.crossword.yourealwaysbe.util;

import android.content.Context;
import android.content.SharedPreferences;

import app.crossword.yourealwaysbe.net.Downloaders;
import app.crossword.yourealwaysbe.versions.AndroidVersionUtils;

public class MigrationHelper {
    public static void applyMigrations(
        Context context, SharedPreferences prefs
    ) {
        AndroidVersionUtils.Factory.getInstance()
            .migrateLegacyBackgroundDownloads(prefs);

        (new ThemeHelper(context, prefs)).migrateThemePreferences();

        (new Downloaders(context, prefs)).migrateAutoDownloaders();
    }
}
