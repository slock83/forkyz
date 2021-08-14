
package app.crossword.yourealwaysbe.net;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Locale;

/**
 * King Digital Downloader
 * https://puzzles.kingdigital.com/jpz/<crosswordSet>/YYYYMMDD.jpz
 */
public class KingDigitalDownloader extends AbstractJPZDownloader {
    private static final String BASE_URL_FMT
        = "https://puzzles.kingdigital.com/jpz/%s/";

    /**
     * Downloads from King Digital site
     *
     * @param crosswordSet used to fill in URL as above
     */
    public KingDigitalDownloader(
        String crosswordSet,
        String downloaderName,
        DayOfWeek[] days,
        String supportUrl
    ) {
        super(
            String.format(BASE_URL_FMT, crosswordSet),
            downloaderName,
            days,
            supportUrl
        );
    }

    protected String createUrlSuffix(LocalDate date) {
        return String.format(
            Locale.US,
            "%04d%02d%02d.jpz",
            date.getYear(),
            date.getMonthValue(),
            date.getDayOfMonth()
        );
    }
}
