package app.crossword.yourealwaysbe.util;

import android.content.Context;

import app.crossword.yourealwaysbe.versions.AndroidVersionUtils;

public class MigrationHelper {
    public static void applyMigrations(Context context) {
        AndroidVersionUtils.Factory.getInstance()
            .migrateLegacyBackgroundDownloads();

        ThemeHelper.migrateThemePreferences(context);
    }
}
