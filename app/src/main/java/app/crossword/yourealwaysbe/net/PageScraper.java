package app.crossword.yourealwaysbe.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.io.PuzzleParser;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleBuilder;
import app.crossword.yourealwaysbe.util.files.FileHandler;

/**
 * A downloader that scrapes from a dateless archive
 *
 * Will return dummy values (can always download) for any date funtions, and
 * just download the first found puzzle not already on file.
 */
public class PageScraper extends AbstractDownloader {
    private static final String REGEX_PUZ = ".*\\.puz";

    private Pattern patFile;
    private PuzzleParser parser;

    private String sourceName;
    private String scrapeUrl;
    private String supportUrl;
    private boolean shareFileUrl;
    private boolean readReverse;
    protected boolean updateable = false;

    public static class Puz extends PageScraper {
        public Puz(String url, String sourceName, String supportUrl) {
            this(url, sourceName, supportUrl, false);
        }

        public Puz(
            String scrapeUrl,
            String sourceName,
            String supportUrl,
            boolean readReverse
        ) {
            super(
                REGEX_PUZ, new IO(),
                scrapeUrl, sourceName, supportUrl, false,
                readReverse
            );
        }
    }

    /**
     * Construct a scraper
     *
     * @param readReverse whether to read from the top or bottom of the
     * page
     * @param shareFileUrl set share URL to scrapeUrl or scraped file
     * URL
     */
    public PageScraper(
        String regexFile, PuzzleParser parser,
        String scrapeUrl, String sourceName,
        String supportUrl, boolean shareFileUrl,
        boolean readReverse
    ) {
        this.patFile = Pattern.compile(regexFile);
        this.parser = parser;
        this.scrapeUrl = scrapeUrl;
        this.sourceName = sourceName;
        this.supportUrl = supportUrl;
        this.shareFileUrl = shareFileUrl;
        this.readReverse = readReverse;
    }

    @Override
    public DayOfWeek[] getDownloadDates() {
        return DATE_DAILY;
    }

    @Override
    public String getName() {
        return sourceName;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public String getSupportUrl() {
        return supportUrl;
    }

    @Override
    public boolean alwaysRun() {
        return true;
    }

    @Override
    public LocalDate getGoodThrough() {
        return LocalDate.MAX;
    }

    @Override
    public LocalDate getGoodFrom() {
        return LocalDate.ofEpochDay(0L);
    }

    @Override
    public DownloadResult download(
        LocalDate date, Set<String> existingFileNames
    ) {
        FileHandler fileHandler
            = ForkyzApplication.getInstance().getFileHandler();

        try {
            Deque<String> urls = getPuzzleURLs();
            Map<String, String> urlsToFilenames = mapURLsToFileNames(urls);

            Iterator<String> urlIterator
                = readReverse ? urls.descendingIterator() : urls.iterator();

            while (urlIterator.hasNext()) {
                String url = urlIterator.next();
                String remoteFileName = urlsToFilenames.get(url);
                String filename = getLocalFileName(remoteFileName);
                String legacyFileName = getLegacyLocalFileName(remoteFileName);

                boolean exists = existingFileNames.contains(filename)
                    || existingFileNames.contains(legacyFileName);

                if (!exists) {
                    try {
                        Puzzle puz = downloadPuzzle(url);
                        if (puz != null) {
                            // I'm not sure what purpose this has
                            // Doesn't seem to be changeable from UI
                            puz.setUpdatable(false);
                            puz.setSource(getName());
                            puz.setSourceUrl(url);
                            puz.setSupportUrl(getSupportUrl());
                            puz.setShareUrl(shareFileUrl ? url : scrapeUrl);
                            puz.setDate(LocalDate.now());

                            String title = puz.getTitle();
                            if (title == null || title.isEmpty())
                                puz.setTitle(remoteFileName);

                            PuzzleBuilder.resolveImages(puz, url);

                            return new DownloadResult(puz, filename);
                        }
                    } catch (Exception e) {
                        System.err.println("Exception downloading " + url
                                + " for " + this.sourceName);
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // failed (else returned earlier)
        return null;
    }

    private Puzzle downloadPuzzle(String url) throws IOException {
        URL u = new URL(url);

        try (InputStream is = getInputStream(u, null)) {
            return parser.parseInput(is);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Map URLs to names of file at url, with file extension removed
     */
    private static Map<String, String> mapURLsToFileNames(
        Deque<String> urls
    ) {
        HashMap<String, String> result = new HashMap<String, String>(
                urls.size());

        for (String url : urls) {
            String fileName = url;
            int lastSlashIdx = fileName.lastIndexOf("/");
            if (lastSlashIdx > 0)
                 fileName = fileName.substring(lastSlashIdx + 1);
            int extensionIdx = fileName.lastIndexOf(".");
            if (extensionIdx > 0)
                fileName = fileName.substring(0, extensionIdx);
            result.put(url, fileName);
        }

        return result;
    }

    private Deque<String> getPuzzleURLs() throws IOException {
        LinkedList<String> result = new LinkedList<String>();

        Connection conn = Jsoup.connect(scrapeUrl);
        conn.timeout(getTimeout());

        Document content = conn.get();
        for (Element a : content.select("a")) {
            String url = a.attr("abs:href");
            if (patFile.matcher(url).matches()) {
                result.add(url);
            }
        }

        return result;
    }

    private String getLocalFileName(String remoteFileName) {
        return (getName() + "-" + remoteFileName).replaceAll(" ", "");
    }

    private String getLegacyLocalFileName(String remoteFileName) {
        return remoteFileName;
    }
}
