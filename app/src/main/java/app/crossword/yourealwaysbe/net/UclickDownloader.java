package app.crossword.yourealwaysbe.net;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

import app.crossword.yourealwaysbe.io.UclickXMLIO;

/**
 * Uclick XML Puzzles
 * URL: https://picayune.uclick.com/comics/[puzzle]/data/[puzzle]YYMMDD-data.xml
 * crnet (Newsday) = Daily
 * usaon (USA Today) = Monday-Saturday (not holidays)
 * fcx (Universal) = Daily
 */
public class UclickDownloader extends AbstractDateDownloader {
    DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
    NumberFormat nf = NumberFormat.getInstance();
    private String copyright;
    private String shortName;

    public UclickDownloader(
        String prefix,
        String shortName, String fullName, String copyright, String supportUrl,
        DayOfWeek[] days
    ){
        super(
            prefix+shortName+"/data/",
            fullName,
            days,
            supportUrl,
            new UclickXMLIO()
        );
        this.shortName = shortName;
        this.copyright = copyright;
        nf.setMinimumIntegerDigits(2);
        nf.setMaximumFractionDigits(0);
    }

    public UclickDownloader(
        String shortName, String fullName, String copyright, String supportUrl,
        DayOfWeek[] days
    ) {
        this(
            "https://picayune.uclick.com/comics/",
            shortName, fullName, copyright, supportUrl,
            days
        );
    }

    @Override
    public DownloadResult download(
        LocalDate date, Set<String> existingFileNames
    ) {
        DownloadResult res = super.download(date, existingFileNames);
        if (res != null) {
            res.getPuzzle().setCopyright(
                "\u00a9 " + date.getYear() + " " + copyright
            );
        }
        return res;
    }

    @Override
    protected String createUrlSuffix(LocalDate date) {
        return this.shortName + nf.format(date.getYear() % 100) + nf.format(date.getMonthValue()) +
        nf.format(date.getDayOfMonth()) + "-data.xml";
    }
}
