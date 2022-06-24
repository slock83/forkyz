package app.crossword.yourealwaysbe.net;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.crossword.yourealwaysbe.io.RaetselZentraleSchwedenJSONIO;
import app.crossword.yourealwaysbe.puz.Puzzle;

/**
 * Downloader for RaetselZentrale Swedish Crosswords
 *
 * First URL is https://raetsel.raetselzentrale.de/l/<shortname>/schwedea/<YYYYMMDD>.
 * The above contains a puzzle ID, then we need
 * https://raetsel.raetselzentrale.de/api/r/<idnumber>
 */
public class RaetselZentraleSchwedenDownloader
        extends AbstractDateDownloader {

    private static final String SOURCE_URL_PATTERN_FORMAT
        = "'https://raetsel.raetselzentrale.de/l/%s/schwedea/'yyyyMMdd";
    private static final String JSON_URL_FORMAT
        = "https://raetsel.raetselzentrale.de/api/r/%s";

    private static final Pattern PUZZLE_ID_PAT = Pattern.compile(
        "window.__riddleId = (\\d*);"
    );
    private static final int PUZZLE_ID_GROUP = 1;

    public RaetselZentraleSchwedenDownloader(
        String name,
        String shortName,
        DayOfWeek[] days,
        String supportUrl,
        String shareUrlPattern
    ) {
        super(
            name,
            days,
            supportUrl,
            new RaetselZentraleSchwedenJSONIO(),
            String.format(Locale.US, SOURCE_URL_PATTERN_FORMAT, shortName),
            shareUrlPattern
        );
    }

    @Override
    protected Puzzle download(
        LocalDate date,
        Map<String, String> headers
    ) {
        try {
            String sourceUrl = getSourceUrl(date);
            URL url = new URL(sourceUrl);
            LOG.info("Getting Raetsel Zentrale puzzle ID from " + url);
            URL jsonUrl = getCrosswordJSONURL(url, headers);
            LOG.info("Getting Raetsel Zentrale puzzle JSON from " + jsonUrl);
            if (jsonUrl == null)
                return null;

            try(
                InputStream is = new BufferedInputStream(
                    getInputStream(jsonUrl, headers)
                )
            ) {
                Puzzle puz = RaetselZentraleSchwedenJSONIO.readPuzzle(is);
                if (puz != null) {
                    puz.setCopyright(getName());
                    puz.setSource(getName());
                    puz.setSourceUrl(sourceUrl);
                    puz.setSupportUrl(getSupportUrl());
                    puz.setDate(date);
                }
                return puz;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private URL getCrosswordJSONURL(
        URL url, Map<String, String> headers
    ) throws IOException {
        try(
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(getInputStream(url, headers))
            )
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = PUZZLE_ID_PAT.matcher(line);
                if (matcher.find()) {
                    String id = matcher.group(PUZZLE_ID_GROUP);
                    return new URL(
                        String.format(Locale.US, JSON_URL_FORMAT, id)
                    );
                }
            }
        }
        return null;
    }
}
