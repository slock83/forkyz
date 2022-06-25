package app.crossword.yourealwaysbe.net;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.io.PuzzleStreamReader;

/**
 * Custom daily downloader
 * URL: prefs.getString("customDailyLink", "")
 * Date = Daily
 */
public class CustomDailyDownloader extends AbstractDateDownloader {
    private static final String NAME
        = ForkyzApplication.getInstance().getString(R.string.custom_daily_title);

    /**
     * Create a new custom downloader
     *
     * @param title the title of the puzzles
     * @param urlDateFormatPattern java date format string for creating URL
     */
    public CustomDailyDownloader(String title, String urlDateFormatPattern) {
        super(
            makeTitle(title),
            DATE_DAILY,
            null,
            new PuzzleStreamReader(),
            urlDateFormatPattern,
            urlDateFormatPattern
        );
    }

    private static String makeTitle(String title) {
        if (title == null || title.trim().isEmpty())
            return NAME;
        return title;
    }
}
