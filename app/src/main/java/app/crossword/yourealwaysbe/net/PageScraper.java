package app.crossword.yourealwaysbe.net;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.io.StreamUtils;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.util.files.FileHandler;

/**
 * A downloader that scrapes from a dateless archive
 *
 * Will return dummy values (can always download) for any date funtions, and
 * just download the first found puzzle not already on file.
 */
public class PageScraper implements Downloader {
    private static final String REGEX = "http://[^ ^']*\\.puz";
    private static final String REL_REGEX = "href=\"(.*\\.puz)\"";
    private static final Pattern PAT = Pattern.compile(REGEX);
    private static final Pattern REL_PAT = Pattern.compile(REL_REGEX);

    private String sourceName;
    private String url;
    private String supportUrl;
    private boolean readReverse;
    protected boolean updateable = false;

    public PageScraper(
        String url, String sourceName, String supportUrl
    ) {
        this(url, sourceName, supportUrl, false);
    }

    /**
     * Construct a scraper
     *
     * @param readReverse whether to read from the top or bottom of the
     * page
     */
    public PageScraper(
        String url, String sourceName, String supportUrl, boolean readReverse
    ) {
        this.url = url;
        this.sourceName = sourceName;
        this.supportUrl = supportUrl;
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
            String content = this.getContent();
            Deque<String> urls = puzzleURLs(content);

            try {
                urls.addAll(puzzleRelativeURLs(url, content));
            } catch (IOException e) {
                e.printStackTrace();
            }

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
                            puz.setDate(LocalDate.now());

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

    private String getContent() throws IOException {
        URL u = new URL(url);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream is = new BufferedInputStream(u.openStream())) {
            StreamUtils.copyStream(is, baos);
        }
        return new String(baos.toByteArray());
    }

    private static Puzzle downloadPuzzle(String url) throws IOException {
        URL u = new URL(url);

        try (InputStream is = new BufferedInputStream(u.openStream())) {
            return IO.loadNative(is);
        } catch (IOException e) {
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

    private static Deque<String> puzzleRelativeURLs(String baseUrl, String input)
            throws MalformedURLException {
        URL base = new URL(baseUrl);
        LinkedList<String> result = new LinkedList<String>();
        Matcher matcher = REL_PAT.matcher(input);

        while (matcher.find()) {
            result.add(new URL(base, matcher.group(1)).toString());
        }

        return result;
    }

    private static Deque<String> puzzleURLs(String input) {
        LinkedList<String> result = new LinkedList<String>();
        Matcher matcher = PAT.matcher(input);

        while (matcher.find()) {
            result.add(matcher.group());
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
