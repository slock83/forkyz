
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
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import app.crossword.yourealwaysbe.io.KeesingXMLIO;
import app.crossword.yourealwaysbe.puz.Puzzle;

/**
 * Downloader for Keesing Crosswords
 *
 * First URL is
 *
 *   https://web.keesing.com/Content/GetPuzzleInfo/?clientid=<clientid>&puzzleid=<puzzlesetid>_yyyymmdd
 *
 * Returns JSON with Puzzle ID.
 *
 * Then gets puzzle from
 *
 *   https://web.keesing.com/content/getxml?clientid=<clientid>&puzzleid=<puzzleid>"
 *
 * Assuming that archives are not available. I don't think the date at
 * the end of the puzzleid in the first url does anything. It works
 * without. Later discovered Keesing sources may prove otherwise.
 */
public class KeesingDownloader
        extends AbstractDateDownloader {

    private static final String ID_URL_FORMAT
        =  "'https://web.keesing.com/Content/GetPuzzleInfo/?"
                + "clientid=%s&puzzleid=%s_'yyyyMMdd";
    private static final String XML_URL_FORMAT
        = "https://web.keesing.com/content/getxml?clientid=%s&puzzleid=%s";

    private String clientID;
    private DateTimeFormatter idUrlFormat;

    public KeesingDownloader(
        String internalName,
        String name,
        DayOfWeek[] days,
        Duration utcAvailabilityOffset,
        String supportUrl,
        String shareUrlPattern,
        String clientID,
        String puzzleSetID
    ) {
        super(
            internalName,
            name,
            days,
            utcAvailabilityOffset,
            supportUrl,
            new KeesingXMLIO(),
            null,
            shareUrlPattern
        );
        this.clientID = clientID;
        this.idUrlFormat = DateTimeFormatter.ofPattern(
            String.format(Locale.US, ID_URL_FORMAT, clientID, puzzleSetID)
        );
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
        String idUrl = idUrlFormat.format(date);
        try(
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(getInputStream(new URL(idUrl), null))
            )
        ) {
            StringBuilder rawJson = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null)
                rawJson.append(line + "\n");

            JSONObject json
                = new JSONObject(new JSONTokener(rawJson.toString()));
            String puzzleID = json.getString("puzzleID");
            return String.format(
                Locale.US, XML_URL_FORMAT, clientID, puzzleID
            );
        } catch (JSONException | IOException e) {
            LOG.severe("Could not read Keesing JSON: " + e);
            return null;
        }
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
}
