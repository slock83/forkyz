package app.crossword.yourealwaysbe.net;

import android.content.SharedPreferences;

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
public class CustomDailyDownloader extends AbstractDownloader {
    private static final String NAME
        = ForkyzApplication.getInstance().getString(R.string.custom_daily_title);
    private static final String SUPPORT_URL = "https://github.com/yourealwaysbe/forkyz";
    private DateTimeFormatter df;

    private static String puzzleTitle(SharedPreferences prefs) {
        String title = prefs.getString("customDailyTitle", NAME);
        if (title.trim().isEmpty())
            return NAME;
        return title;
    }

    public CustomDailyDownloader(SharedPreferences prefs) {
        super(
            "",
            puzzleTitle(prefs),
            DATE_DAILY,
            SUPPORT_URL,
            new IO()
        );

        this.df = DateTimeFormatter.ofPattern( prefs.getString("customDailyLink", "") );
    }

    @Override
    protected String createUrlSuffix(LocalDate date) {
        return df.format(date);
    }
}
