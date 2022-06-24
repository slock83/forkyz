
package app.crossword.yourealwaysbe.net;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Locale;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.io.RCIJeuxMFJIO;

/**
 * Le Parisien Mots Fleche Downloader (Force 2)
 * URL: https://www.leparisien.fr/jeux/mots-fleches/force-2/
 * Date = Daily
 */
public class LeParisienDownloader extends AbstractDateDownloader {
    private static final String NAME =
        ForkyzApplication.getInstance().getString(R.string.le_parisien_daily);
    private static final String SUPPORT_URL = "https://abonnement.leparisien.fr";
    private static final String SHARE_URL
        = "https://www.leparisien.fr/jeux/mots-fleches/force-2/";

    // ultimately this could be split out into a general rcijeux mots fleches
    // downloader, but no need yet.
    private static final String SOURCE_URL_FORMAT
        = "https://www.rcijeux.fr/drupal_game/leparisien/mfleches1/grids/"
            + "mfleches_2_%d.mfj";

    private static final int BASE_CW_NUMBER = 2536;
    private static final LocalDate BASE_CW_DATE = LocalDate.of(2022, 6, 21);

    private static final int ARCHIVE_LENGTH_DAYS = 364;

    public LeParisienDownloader() {
        super(
            NAME,
            DATE_DAILY,
            SUPPORT_URL,
            new RCIJeuxMFJIO()
        );
    }

    @Override
    public LocalDate getGoodFrom() {
        return LocalDate.now().minusDays(ARCHIVE_LENGTH_DAYS);
    }

    @Override
    protected String getSourceUrl(LocalDate date) {
        Duration diff = Duration.between(
            BASE_CW_DATE.atStartOfDay(), date.atStartOfDay()
        );

        long cwNumber = BASE_CW_NUMBER + diff.toDays();

        return String.format(Locale.US, SOURCE_URL_FORMAT, cwNumber);
    }

    @Override
    protected String getShareUrl(LocalDate date) {
        return SHARE_URL;
    }
}
