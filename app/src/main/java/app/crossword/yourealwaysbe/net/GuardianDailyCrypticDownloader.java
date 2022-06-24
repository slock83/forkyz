package app.crossword.yourealwaysbe.net;

import java.io.IOException;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.io.GuardianJSONIO;
import app.crossword.yourealwaysbe.puz.Puzzle;

/**
 * Guardian Daily Cryptic downloader
 * URL: https://www.theguardian.com/crosswords/cryptic/
 * Date = Daily
 */
public class GuardianDailyCrypticDownloader extends AbstractDateDownloader {
    private static final String NAME =
        ForkyzApplication.getInstance().getString(R.string.guardian_daily);
    private static final String SUPPORT_URL = "https://support.theguardian.com";
    private static final String BASE_SOURCE_URL
        = "https://www.theguardian.com/crosswords/cryptic/";
    private static final int BASE_CW_NUMBER = 28112;
    private static final LocalDate BASE_CW_DATE = LocalDate.of(2020, 4, 20);

    public GuardianDailyCrypticDownloader() {
        super(
            NAME,
            DATE_WEEKDAY,
            SUPPORT_URL,
            null
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
            String cwJson = getCrosswordJSON(url);

            if (cwJson == null)
                return null;

            Puzzle puz = GuardianJSONIO.readPuzzle(cwJson);
            puz.setCopyright("Guardian / " + puz.getAuthor());

            if (puz != null) {
                puz.setSource(getName());
                puz.setSupportUrl(getSupportUrl());
                puz.setSupportUrl(sourceUrl);
            }

            return puz;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected String getSourceUrl(LocalDate date) {
        return BASE_SOURCE_URL + createUrlSuffix(date);
    }

    @Override
    protected String getShareUrl(LocalDate date) {
        return getSourceUrl(date);
    }

    protected String createUrlSuffix(LocalDate date) {
        LocalDate lower = BASE_CW_DATE;
        LocalDate upper = date;
        int direction = 1;

        if (lower.isAfter(upper)) {
            lower = date;
            upper = BASE_CW_DATE;
            direction = -1;
        }

        Duration diff = Duration.between(lower.atStartOfDay(),
                                         upper.atStartOfDay());

        long daysDiff = diff.toDays();
        int yearsDiff = Period.between(lower, upper).getYears();

        long cwNumOffset = daysDiff;
        // no Sundays (base day is Monday so negative gets one more)
        cwNumOffset -= (daysDiff / 7);
        if (direction < 0 && daysDiff % 7 != 0)
            cwNumOffset -= 1;
        // no Christmas
        cwNumOffset -= countNonSundayChristmas(lower, upper);
        // no Boxing day pre 2010
        cwNumOffset -= countNonSundayBoxing(lower, LocalDate.of(2009, 12, 26));

        long cwNum = BASE_CW_NUMBER + direction * cwNumOffset;

        return Long.toString(cwNum);
    }

    private static String getCrosswordJSON(URL url) throws IOException {
        try {
            LOG.info("Downloading " + url);
            Document doc = Jsoup.connect(url.toString()).get();
            String cwJson = doc.select(".js-crossword")
                               .attr("data-crossword-data");

            if (!cwJson.isEmpty())
                return cwJson;
        } catch (HttpStatusException e) {
            LOG.info("Could not download " + url);
        }
        return null;
    }

    /**
     * Counts number of Christmasses that aren't Sunday between dates
     * (inclusive).
     *
     * Returns 0 if upper below lower
     */
    private static int countNonSundayChristmas(LocalDate lower, LocalDate upper) {
        return countNonSundaySpecial(lower, upper, 12, 25);
    }

    /**
     * Counts number of Boxing Days that aren't Sunday between dates
     * (inclusive)
     *
     * Returns 0 if upper below lower
     */
    private static int countNonSundayBoxing(LocalDate lower, LocalDate upper) {
        return countNonSundaySpecial(lower, upper, 12, 26);
    }

    /**
     * Counts number of special days that aren't Sunday between dates
     * (inclusive)
     *
     * @param lower start date inclusive
     * @param upper end date inclusive
     * @param month month of special date (1-12)
     * @param day day of special date (1-31)
     * @return number of non-sunday occurences, or 0 if upper &lt; lower
     */
    private static int countNonSundaySpecial(LocalDate lower,
                                             LocalDate upper,
                                             int month,
                                             int day) {
        if (upper.isBefore(lower))
            return 0;

        LocalDate special = LocalDate.of(lower.getYear(), month, day);
        if (lower.isAfter(special))
            special = special.plusYears(1);

        int count = 0;
        while (!special.isAfter(upper)) {
            if (DayOfWeek.from(special) != DayOfWeek.SUNDAY)
                count += 1;
            special = special.plusYears(1);
        }

        return count;
    }

}
