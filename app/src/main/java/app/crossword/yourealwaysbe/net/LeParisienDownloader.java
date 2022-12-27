
package app.crossword.yourealwaysbe.net;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

import app.crossword.yourealwaysbe.io.RCIJeuxMFJIO;
import app.crossword.yourealwaysbe.puz.Puzzle;

/**
 * Le Parisien Mots Fleche Downloader (Force 2)
 * URL: https://www.leparisien.fr/jeux/mots-fleches/force-2/
 * Date = Daily
 */
public class LeParisienDownloader extends AbstractDateDownloader {
    private static final String SUPPORT_URL = "https://abonnement.leparisien.fr";
    private static final String SHARE_URL
        = "https://www.leparisien.fr/jeux/mots-fleches/force-2/";
    private static final DateTimeFormatter titleDateFormat
        = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ultimately this could be split out into a general rcijeux mots fleches
    // downloader, but no need yet.
    private static final String SOURCE_URL_FORMAT
        = "https://www.rcijeux.fr/drupal_game/leparisien/mfleches1/grids/"
            + "mfleches_2_%d.mfj";

    private static final int BASE_CW_NUMBER = 2536;
    private static final LocalDate BASE_CW_DATE = LocalDate.of(2022, 6, 21);

    private static final int ARCHIVE_LENGTH_DAYS = 364;

    public LeParisienDownloader(String internalName, String name) {
        super(
            internalName,
            name,
            DATE_DAILY,
            Duration.ofHours(1), // take available at midnight in France
            SUPPORT_URL,
            new RCIJeuxMFJIO()
        );
    }

    @Override
    protected LocalDate getGoodFrom() {
        return LocalDate.now().minusDays(ARCHIVE_LENGTH_DAYS);
    }

    @Override
    protected String getSourceUrl(LocalDate date) {
        long cwNumber = getCrosswordNumber(date);
        return String.format(Locale.US, SOURCE_URL_FORMAT, cwNumber);
    }

    @Override
    protected String getShareUrl(LocalDate date) {
        return SHARE_URL;
    }

    @Override
    protected Puzzle download(
        LocalDate date,
        Map<String, String> headers
    ){
        Puzzle puz = super.download(date, headers);
        if (puz != null) {
            puz.setTitle(getCrosswordTitle(date));
        }
        return puz;
    }

    private long getCrosswordNumber(LocalDate date) {
        Duration diff = Duration.between(
            BASE_CW_DATE.atStartOfDay(), date.atStartOfDay()
        );

        return BASE_CW_NUMBER + diff.toDays();
    }

    private String getCrosswordTitle(LocalDate date) {
        return getCrosswordNumber(date) + ", " + titleDateFormat.format(date);
    }
}
