package app.crossword.yourealwaysbe.net;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import app.crossword.yourealwaysbe.io.PuzzleParser;
import app.crossword.yourealwaysbe.puz.Puzzle;

/**
 * Base downloader class
 *
 * For downloads from sites that published puzzles on a given date. Each
 * date has one unique puzzle.
 */
public class AbstractDateDownloader extends AbstractDownloader {
    protected static final Logger LOG = Logger.getLogger("app.crossword.yourealwaysbe");
    protected static final Map<String, String> EMPTY_MAP = Collections.emptyMap();
    private String downloaderName;
    protected PuzzleParser puzzleParser;
    protected LocalDate goodThrough = LocalDate.now();
    private DayOfWeek[] days;
    private String supportUrl;
    private DateTimeFormatter sourceUrlFormat;
    private DateTimeFormatter shareUrlFormat;
    private LocalDate goodFrom = LocalDate.ofEpochDay(0L);

    protected AbstractDateDownloader(
        String downloaderName,
        DayOfWeek[] days,
        String supportUrl,
        PuzzleParser puzzleParser
    ) {
        this(
            downloaderName,
            days,
            supportUrl,
            puzzleParser,
            null,
            null,
            null
        );
    }

    protected AbstractDateDownloader(
        String downloaderName,
        DayOfWeek[] days,
        String supportUrl,
        PuzzleParser puzzleParser,
        String sourceUrlFormatPattern,
        String shareUrlFormatPattern
    ) {
        this(
            downloaderName,
            days,
            supportUrl,
            puzzleParser,
            sourceUrlFormatPattern,
            shareUrlFormatPattern,
            null
        );
    }

    protected AbstractDateDownloader(
        String downloaderName,
        DayOfWeek[] days,
        String supportUrl,
        PuzzleParser puzzleParser,
        String sourceUrlFormatPattern,
        String shareUrlFormatPattern,
        LocalDate goodFrom
    ) {
        this.downloaderName = downloaderName;
        this.days = days;
        this.supportUrl = supportUrl;
        this.puzzleParser = puzzleParser;
        if (sourceUrlFormatPattern != null) {
            this.sourceUrlFormat
                = DateTimeFormatter.ofPattern(sourceUrlFormatPattern);
        }
        if (shareUrlFormatPattern != null) {
            this.shareUrlFormat
                = DateTimeFormatter.ofPattern(shareUrlFormatPattern);
        }
        if (goodFrom != null)
            this.goodFrom = goodFrom;
    }

    /**
     * A unique consistent filename for the given date
     */
    protected String createFileName(LocalDate date) {
        return (
            date.getYear() + "-" +
            date.getMonthValue() + "-" +
            date.getDayOfMonth() + "-" +
            this.downloaderName.replaceAll(" ", "")
        );
    }

    @Override
    public String getName() {
        return downloaderName;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public DayOfWeek[] getDownloadDates() {
        return days;
    }

    @Override
    public String getSupportUrl() {
        return supportUrl;
    }

    /**
     * Where to do the actual download from
     */
    protected String getSourceUrl(LocalDate date) {
        return (sourceUrlFormat == null)
            ? null
            : sourceUrlFormat.format(date);
    }

    /**
     * A user-facing URL for the puzzle
     *
     * I.e. go here to find an online playable version, rather than the
     * backend data file
     */
    protected String getShareUrl(LocalDate date) {
        return (shareUrlFormat == null) ? null : shareUrlFormat.format(date);
    }

    @Override
    public DownloadResult download(
        LocalDate date, Set<String> existingFileNames
    ) {
        String fileName = createFileName(date);
        if (existingFileNames.contains(fileName))
            return DownloadResult.ALREADY_EXISTS;

        Puzzle puz = download(date);
        if (puz != null)
            return new DownloadResult(puz, fileName);
        else
            return DownloadResult.FAILED;
    }

    protected Puzzle download(
        LocalDate date,
        Map<String, String> headers
    ){
        try {
            String sourceUrl = getSourceUrl(date);
            URL url = new URL(sourceUrl);
            try (InputStream is = getInputStream(url, headers)) {
                Puzzle puz = puzzleParser.parseInput(is);

                if (puz != null) {
                    puz.setDate(date);
                    puz.setSource(getName());
                    puz.setSourceUrl(sourceUrl);
                    puz.setSupportUrl(getSupportUrl());
                    puz.setShareUrl(getShareUrl(date));
                    puz.setUpdatable(false);

                    return puz;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (MalformedURLException e) {
            LOG.severe("Malformed URL in download: " + e);
        }

        return null;
    }

    protected Puzzle download(LocalDate date) {
        return download(date, EMPTY_MAP);
    }

    @Override
    public boolean alwaysRun(){
        return false;
    }

    public LocalDate getGoodThrough(){
        return this.goodThrough;
    }

    public LocalDate getGoodFrom(){
        return LocalDate.ofEpochDay(0L);
    }
}
