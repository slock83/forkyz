package app.crossword.yourealwaysbe.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;

import app.crossword.yourealwaysbe.io.GuardianJSONIO;
import app.crossword.yourealwaysbe.puz.Puzzle;

/**
 * Guardian Daily Cryptic downloader
 * URL: https://www.theguardian.com/crosswords/cryptic/
 * Date = Daily
 */
public class GuardianDailyCrypticDownloader extends AbstractDateDownloader {
    private static final String INTERNAL_NAME = "guardian";
    private static final String SUPPORT_URL = "https://support.theguardian.com";
    private static final String BASE_SOURCE_URL
        = "https://www.theguardian.com/crosswords/cryptic/";
    private static final int BASE_CW_NUMBER = 28112;
    private static final LocalDate BASE_CW_DATE = LocalDate.of(2020, 4, 20);

    public GuardianDailyCrypticDownloader(String internalName, String name) {
        super(
            internalName,
            name,
            DATE_WEEKDAY,
            Duration.ZERO, // TODO: availability time
            SUPPORT_URL,
            null
        );
    }

    @Override
    protected Puzzle download(
        LocalDate date,
        Map<String, String> headers
    ) {
        String sourceUrl = getSourceUrl(date);
        try (InputStream is = getInputStream(new URL(sourceUrl), headers)) {
            Puzzle puz = GuardianJSONIO.readFromHTML(is);
            if (puz != null) {
                puz.setCopyright("Guardian / " + puz.getAuthor());
                puz.setSource(getName());
                puz.setSupportUrl(getSupportUrl());
                puz.setSourceUrl(sourceUrl);
                puz.setShareUrl(sourceUrl);
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
