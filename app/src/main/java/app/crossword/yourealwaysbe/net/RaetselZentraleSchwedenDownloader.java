package app.crossword.yourealwaysbe.net;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
public class RaetselZentraleSchwedenDownloader extends AbstractDateDownloader {

    private static final String BASE_URL
        = "https://raetsel.raetselzentrale.de/l";
    private static final String URL_SUFFIX_FORMAT
        = "/%s/schwedea/%s";
    private static final DateTimeFormatter DATE_FORMATTER
        = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.US);
    private static final String JSON_URL_FORMAT
        = "https://raetsel.raetselzentrale.de/api/r/%s";

    private static final Pattern PUZZLE_ID_PAT = Pattern.compile(
        "window.__riddleId = (\\d*);"
    );
    private static final int PUZZLE_ID_GROUP = 1;

    private String shortName;

    public RaetselZentraleSchwedenDownloader(
        String name, String shortName, DayOfWeek[] days, String supportUrl
    ) {
        super(
            BASE_URL,
            name,
            days,
            supportUrl,
            new RaetselZentraleSchwedenJSONIO()
        );
        this.shortName = shortName;
    }

    @Override
    protected Puzzle download(
        LocalDate date,
        String urlSuffix,
        Map<String, String> headers
    ) {
        try {
            URL url = new URL(this.baseUrl + urlSuffix);
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

    protected String createUrlSuffix(LocalDate date) {
        return String.format(
            Locale.US,
            URL_SUFFIX_FORMAT,
            shortName,
            DATE_FORMATTER.format(date)
        );
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
