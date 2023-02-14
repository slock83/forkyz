package app.crossword.yourealwaysbe.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
        String internalName,
        String name,
        String shortName,
        DayOfWeek[] days,
        Duration utcAvailabilityOffset,
        String supportUrl,
        String shareUrlPattern
    ) {
        super(
            internalName,
            name,
            days,
            utcAvailabilityOffset,
            supportUrl,
            new RaetselZentraleSchwedenJSONIO(),
            null,
            shareUrlPattern
        );
        idUrl = String.format(Locale.US, ID_URL_FORMAT, shortName);
    }

    @Override
    protected LocalDate getGoodFrom() {
        // website has "yesterday's" puzzle up until the
        // utcAvailabilityOffset, so either return today or yesterday
        // depending on what's available
        ZonedDateTime goodFrom = ZonedDateTime.now(ZoneId.of("UTC"));
        Duration availabilityOffset = getUTCAvailabilityOffset();
        if (availabilityOffset != null)
            goodFrom =goodFrom.minus(availabilityOffset);

        return goodFrom.toLocalDate();
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
        Puzzle puz = super.download(date, headers);
        if (puz != null) {
            puz.setCopyright(getName());
            puz.setDate(date);
        }
        return puz;
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
