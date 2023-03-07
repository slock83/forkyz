package app.crossword.yourealwaysbe.net;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

import app.crossword.yourealwaysbe.io.PuzzleParser;
import app.crossword.yourealwaysbe.io.RCIJeuxMFJIO;
import app.crossword.yourealwaysbe.puz.Puzzle;

/**
 * Le Parisien Mots Fleche Downloader (Force 2)
 * URL: https://www.leparisien.fr/jeux/mots-fleches/force-2/
 * Date = Daily
 */
public class AbstractRCIJeuxMFJDateDownloader extends AbstractDateDownloader {
    private String support_url = "https://abonnement.leparisien.fr";
    private String share_url
            = "https://www.leparisien.fr/jeux/mots-fleches/force-2/";
    private static final DateTimeFormatter titleDateFormat
            = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ultimately this could be split out into a general rcijeux mots fleches
    // downloader, but no need yet.
    private String source_url_format
            = "https://www.rcijeux.fr/drupal_game/leparisien/mfleches1/grids/"
            + "mfleches_2_%d.mfj";

    private  int base_cw_number = 2536;
    private LocalDate base_date = LocalDate.of(2022, 6, 21);

    private  int ARCHIVE_LENGTH_DAYS = 364;

    protected AbstractRCIJeuxMFJDateDownloader(
            String internalName,
            String downloaderName,
            DayOfWeek[] days,
            Duration utcAvailabilityOffset,
            String supportUrl,
            String sourceUrlFormatPattern,
            String shareUrlFormatPattern,
            int base_number,
            LocalDate base_date

    ) {
        super(
                internalName,
                downloaderName,
                days,
                utcAvailabilityOffset,
                supportUrl,
                new RCIJeuxMFJIO()
        );
        this.base_date = base_date;
        this.base_cw_number = base_number;
        this.share_url = shareUrlFormatPattern;
        this.source_url_format = sourceUrlFormatPattern;
    }

    @Override
    protected LocalDate getGoodFrom() {
        return LocalDate.now().minusDays(ARCHIVE_LENGTH_DAYS);
    }

    @Override
    protected String getSourceUrl(LocalDate date) {
        long cwNumber = getCrosswordNumber(date);
        return String.format(Locale.US, this.source_url_format, cwNumber);
    }

    @Override
    protected String getShareUrl(LocalDate date) {
        return this.share_url;
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
                this.base_date.atStartOfDay(), date.atStartOfDay()
        );
        long delta =   Math.floorDiv(getDownloadDates().length* (int)diff.toDays(), 7);


        return this.base_cw_number + delta;
    }

    private String getCrosswordTitle(LocalDate date) {
        return getCrosswordNumber(date) + ", " + titleDateFormat.format(date);
    }
}
