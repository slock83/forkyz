package app.crossword.yourealwaysbe.net;

import app.crossword.yourealwaysbe.io.PuzzleStreamReader;

import java.time.Duration;

/**
 * Custom daily downloader
 * URL: prefs.getString("customDailyLink", "")
 * Date = Daily
 */
public class CustomDailyDownloader extends AbstractDateDownloader {
    /**
     * Create a new custom downloader
     *
     * @param title the title of the puzzles
     * @param urlDateFormatPattern java date format string for creating URL
     */
    public CustomDailyDownloader(
        String internalName, String title, String urlDateFormatPattern
    ) {
        super(
            internalName,
            title,
            DATE_DAILY,
            // Currently no option to configure availability time
            Duration.ZERO,
            null,
            new PuzzleStreamReader(),
            urlDateFormatPattern,
            urlDateFormatPattern
        );
    }
}
