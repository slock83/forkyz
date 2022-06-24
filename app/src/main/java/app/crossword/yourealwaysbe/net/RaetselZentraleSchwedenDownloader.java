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
 * This only really captures the Hamburg Abendsblatt pattern of
 * interation, so will need work later.
 *
 * Assuming archives are not available (paywall for Hamburg Abendsblatt,
 * just not available for any other i see with the same puzzle). Good
 * form is current day only.
 *
 * First URL is https://raetsel.raetselzentrale.de/l/<shortname>/schwede/
 * The above contains a puzzle ID, then we need
 * https://raetsel.raetselzentrale.de/api/r/<idnumber>
 */
public class RaetselZentraleSchwedenDownloader
        extends AbstractDateDownloader {

    private static final String ID_URL_FORMAT
        = "https://raetsel.raetselzentrale.de/l/%s/schwede/";
    private static final String JSON_URL_FORMAT
        = "https://raetsel.raetselzentrale.de/api/r/%s";

    private static final Pattern PUZZLE_ID_PAT = Pattern.compile(
        "window.__riddleId = (\\d*);"
    );
    private static final int PUZZLE_ID_GROUP = 1;

    private String idUrl;

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
            null,
            shareUrlPattern
        );
        idUrl = String.format(Locale.US, ID_URL_FORMAT, shortName);
    }

    @Override
    public LocalDate getGoodFrom() {
        return LocalDate.now();
    }

    @Override
    protected String getSourceUrl(LocalDate date) {
        return getJSONUrl(null);
    }

    @Override
    protected Puzzle download(
        LocalDate date,
        Map<String, String> headers
    ) {
        if (!LocalDate.now().equals(date))
            return null;

        try {
            String sourceUrl = getJSONUrl(headers);
            if (sourceUrl == null)
                return null;

            URL url = new URL(sourceUrl);

            try(
                InputStream is = new BufferedInputStream(
                    getInputStream(url, headers)
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

    private String getJSONUrl(Map<String, String> headers) {
        try(
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(getInputStream(new URL(idUrl), headers))
            )
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = PUZZLE_ID_PAT.matcher(line);
                if (matcher.find()) {
                    String id = matcher.group(PUZZLE_ID_GROUP);
                    return String.format(Locale.US, JSON_URL_FORMAT, id);
                }
            }
        } catch (IOException e) {
            // fall through
            e.printStackTrace();
        }
        return null;
    }
}
