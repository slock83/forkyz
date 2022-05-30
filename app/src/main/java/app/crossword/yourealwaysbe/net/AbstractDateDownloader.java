package app.crossword.yourealwaysbe.net;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map.Entry;
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
public abstract class AbstractDateDownloader implements Downloader {
    protected static final Logger LOG = Logger.getLogger("app.crossword.yourealwaysbe");
    protected static final Map<String, String> EMPTY_MAP = Collections.emptyMap();
    protected String baseUrl;
    private String downloaderName;
    protected PuzzleParser puzzleParser;
    protected LocalDate goodThrough = LocalDate.now();
    private DayOfWeek[] days;
    private String supportUrl;

    protected AbstractDateDownloader(
        String baseUrl,
        String downloaderName,
        DayOfWeek[] days,
        String supportUrl,
        PuzzleParser puzzleParser
    ) {
        this.baseUrl = baseUrl;
        this.downloaderName = downloaderName;
        this.days = days;
        this.supportUrl = supportUrl;
        this.puzzleParser = puzzleParser;
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

    protected abstract String createUrlSuffix(LocalDate date);

    @Override
    public DownloadResult download(
        LocalDate date, Set<String> existingFileNames
    ) {
        String fileName = createFileName(date);
        if (existingFileNames.contains(fileName))
            return null;

        Puzzle puz = download(date, this.createUrlSuffix(date));
        if (puz != null)
            return new DownloadResult(puz, fileName);
        else
            return null;
    }

    protected Puzzle download(
        LocalDate date,
        String urlSuffix,
        Map<String, String> headers
    ){
        try {
            URL url = new URL(this.baseUrl + urlSuffix);
            try (InputStream is = getInputStream(url, headers)) {
                Puzzle puz = puzzleParser.parseInput(is);

                if (puz != null) {
                    puz.setDate(date);
                    puz.setSource(getName());
                    puz.setSourceUrl(url.toString());
                    puz.setSupportUrl(getSupportUrl());
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

    protected Puzzle download(
        LocalDate date, String urlSuffix
    ) {
        return download(date, urlSuffix, EMPTY_MAP);
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

    protected BufferedInputStream getInputStream(
        URL url, Map<String, String> headers
    ) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Connection", "close");

        for (Entry<String, String> e : headers.entrySet()){
            conn.setRequestProperty(e.getKey(), e.getValue());
        }

        return new BufferedInputStream(conn.getInputStream());
    }
}
