package app.crossword.yourealwaysbe.net;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

import app.crossword.yourealwaysbe.io.UclickXMLIO;

/**
 * Uclick XML Puzzles
 * URL: https://picayune.uclick.com/comics/[puzzle]/data/[puzzle]YYMMDD-data.xml
 * crnet (Newsday) = Daily
 * usaon (USA Today) = Daily
 * fcx (Universal) = Daily
 */
public class UclickDownloader extends AbstractDateDownloader {
    private static final String SOURCE_URL_PATTERN_FORMAT
        = "'https://picayune.uclick.com/comics/%1$s/data/%1$s'yyMMdd'-data.xml'";

    private String copyright;

    public UclickDownloader(
        String internalName,
        String shortName,
        String fullName,
        String copyright,
        String supportUrl,
        DayOfWeek[] days,
        Duration utcAvailabilityOffset,
        String shareUrlPattern
    ) {
        super(
            internalName,
            fullName,
            days,
            utcAvailabilityOffset,
            supportUrl,
            new UclickXMLIO(),
            String.format(
                Locale.US,
                SOURCE_URL_PATTERN_FORMAT,
                shortName
            ),
            shareUrlPattern
        );
        this.copyright = copyright;
    }

    @Override
    public DownloadResult download(
        LocalDate date, Set<String> existingFileNames
    ) {
        DownloadResult res = super.download(date, existingFileNames);
        if (res.isSuccess()) {
            res.getPuzzle().setCopyright(
                "\u00a9 " + date.getYear() + " " + copyright
            );
        }
        return res;
    }
}
