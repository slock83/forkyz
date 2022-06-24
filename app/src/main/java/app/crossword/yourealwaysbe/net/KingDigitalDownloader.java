
package app.crossword.yourealwaysbe.net;

import java.time.DayOfWeek;

import app.crossword.yourealwaysbe.io.JPZIO;

/**
 * King Digital Downloader
 * https://puzzles.kingdigital.com/jpz/<crosswordSet>/YYYYMMDD.jpz
 */
public class KingDigitalDownloader extends AbstractDateDownloader {
    private static final String SOURCE_URL_FMT_PATTERN
        = "'https://puzzles.kingdigital.com/jpz/%s/'yyyyMMdd'.jpz'";

    /**
     * Downloads from King Digital site
     *
     * @param crosswordSet used to fill in URL as above
     */
    public KingDigitalDownloader(
        String crosswordSet,
        String downloaderName,
        DayOfWeek[] days,
        String supportUrl,
        String shareUrlPattern
    ) {
        super(
            downloaderName,
            days,
            supportUrl,
            new JPZIO(),
            String.format(SOURCE_URL_FMT_PATTERN, crosswordSet),
            shareUrlPattern
        );
    }
}
