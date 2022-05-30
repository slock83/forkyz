package app.crossword.yourealwaysbe.net;

import java.time.format.DateTimeFormatter;
import java.time.LocalDate;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.io.IO;

/**
 * Custom daily downloader
 * URL: prefs.getString("customDailyLink", "")
 * Date = Daily
 */
public class CustomDailyDownloader extends AbstractDateDownloader {
    private static final String NAME
        = ForkyzApplication.getInstance().getString(R.string.custom_daily_title);
    private DateTimeFormatter urlDateFormat;

    /**
     * Create a new custom downloader
     *
     * @param title the title of the puzzles
     * @param urlDateFormatPattern java date format string for creating URL
     */
    public CustomDailyDownloader(String title, String urlDateFormatPattern) {
        super("", makeTitle(title), DATE_DAILY, null, new IO());
        this.urlDateFormat = DateTimeFormatter.ofPattern(urlDateFormatPattern);
    }

    @Override
    protected String createUrlSuffix(LocalDate date) {
        return urlDateFormat.format(date);
    }

    private static String makeTitle(String title) {
        if (title == null || title.trim().isEmpty())
            return NAME;
        return title;
    }
}
