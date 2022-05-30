package app.crossword.yourealwaysbe.net;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.util.files.FileHandler;

public class PageScraper {
    private static final String REGEX = "http://[^ ^']*\\.puz";
    private static final String REL_REGEX = "href=\"(.*\\.puz)\"";
    private static final Pattern PAT = Pattern.compile(REGEX);
    private static final Pattern REL_PAT = Pattern.compile(REL_REGEX);
    private static final int NUM_FILES_PER_DOWNLOAD = 1;

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

    protected String getContent() throws IOException {
        URL u = new URL(url);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream is = new BufferedInputStream(u.openStream())) {
            IO.copyStream(is, baos);
        }
        return new String(baos.toByteArray());
    }

    public static Puzzle download(String url) throws IOException {
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
    protected static Map<String, String> mapURLsToFileNames(
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

    protected static Deque<String> puzzleRelativeURLs(String baseUrl, String input)
            throws MalformedURLException {
        URL base = new URL(baseUrl);
        LinkedList<String> result = new LinkedList<String>();
        Matcher matcher = REL_PAT.matcher(input);

        while (matcher.find()) {
            result.add(new URL(base, matcher.group(1)).toString());
        }

        return result;
    }

    protected static Deque<String> puzzleURLs(String input) {
        LinkedList<String> result = new LinkedList<String>();
        Matcher matcher = PAT.matcher(input);

        while (matcher.find()) {
            result.add(matcher.group());
        }

        return result;
    }

    public String getSourceName() {
        return this.sourceName;
    }

    public String getSupportUrl() {
        return this.supportUrl;
    }

    /**
     * Add some meta data to file and save it to the file system
     */
    private boolean processPuzzle(
        Puzzle puz, String fileName, String sourceUrl
    ) {
        final FileHandler fileHandler
            = ForkyzApplication.getInstance().getFileHandler();
        try {
            // I'm not sure what purpose this has
            // Doesn't seem to be changeable from UI
            puz.setUpdatable(false);
            puz.setSource(this.sourceName);
            puz.setSourceUrl(sourceUrl);
            puz.setSupportUrl(this.supportUrl);
            puz.setDate(LocalDate.now());

            return fileHandler.saveNewPuzzle(puz, fileName) != null;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns a set of file names downloaded
     */
    public Set<String> scrape() {
        FileHandler fileHandler
            = ForkyzApplication.getInstance().getFileHandler();

        Set<String> scrapedFiles = new HashSet<>();

        try {
            String content = this.getContent();
            Deque<String> urls = puzzleURLs(content);

            try {
                urls.addAll(puzzleRelativeURLs(url, content));
            } catch (IOException e) {
                e.printStackTrace();
            }

            Map<String, String> urlsToFilenames = mapURLsToFileNames(urls);

            Set<String> existingFiles = fileHandler.getPuzzleNames();

            Iterator<String> urlIterator
                = readReverse ? urls.descendingIterator() : urls.iterator();

            while (urlIterator.hasNext()) {
                String url = urlIterator.next();
                String filename = urlsToFilenames.get(url);

                boolean exists = existingFiles.contains(filename);

                if (!exists && (scrapedFiles.size() < NUM_FILES_PER_DOWNLOAD)) {
                    try {
                        Puzzle puz = download(url);
                        if (puz != null) {
                            if (this.processPuzzle(puz, filename, url))
                                scrapedFiles.add(filename);
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

        return scrapedFiles;
    }
}
