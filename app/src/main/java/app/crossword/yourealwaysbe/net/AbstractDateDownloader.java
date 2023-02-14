package app.crossword.yourealwaysbe.net;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import app.crossword.yourealwaysbe.io.PuzzleParser;
import app.crossword.yourealwaysbe.puz.Puzzle;

/**
 * Base downloader class
 *
 * For downloads from sites that published puzzles on a given date. Each
 * date has one unique puzzle.
 */
public class AbstractDateDownloader extends AbstractDownloader {
    protected static final Logger LOG = Logger.getLogger("app.crossword.yourealwaysbe");
    protected static final Map<String, String> EMPTY_MAP = Collections.emptyMap();
    private String internalName;
    private String downloaderName;
    protected PuzzleParser puzzleParser;
    private DayOfWeek[] days;
    private String supportUrl;
    private DateTimeFormatter sourceUrlFormat;
    private DateTimeFormatter shareUrlFormat;
    private LocalDate goodFrom = LocalDate.ofEpochDay(0L);
    private Duration utcAvailabilityOffset = Duration.ZERO;

    protected AbstractDateDownloader(
        String internalName,
        String downloaderName,
        DayOfWeek[] days,
        Duration utcAvailabilityOffset,
        String supportUrl,
        PuzzleParser puzzleParser
    ) {
        this(
            internalName,
            downloaderName,
            days,
            utcAvailabilityOffset,
            supportUrl,
            puzzleParser,
            null,
            null,
            null
        );
    }

    protected AbstractDateDownloader(
        String internalName,
        String downloaderName,
        DayOfWeek[] days,
        Duration utcAvailabilityOffset,
        String supportUrl,
        PuzzleParser puzzleParser,
        String sourceUrlFormatPattern,
        String shareUrlFormatPattern
    ) {
        this(
            internalName,
            downloaderName,
            days,
            utcAvailabilityOffset,
            supportUrl,
            puzzleParser,
            sourceUrlFormatPattern,
            shareUrlFormatPattern,
            null
        );
    }

    protected AbstractDateDownloader(
        String internalName,
        String downloaderName,
        DayOfWeek[] days,
        Duration utcAvailabilityOffset,
        String supportUrl,
        PuzzleParser puzzleParser,
        String sourceUrlFormatPattern,
        String shareUrlFormatPattern,
        LocalDate goodFrom
    ) {
        this.internalName = internalName;
        this.downloaderName = downloaderName;
        this.days = days;
        this.supportUrl = supportUrl;
        this.puzzleParser = puzzleParser;
        if (sourceUrlFormatPattern != null) {
            this.sourceUrlFormat
                = DateTimeFormatter.ofPattern(sourceUrlFormatPattern);
        }
        if (shareUrlFormatPattern != null) {
            this.shareUrlFormat
                = DateTimeFormatter.ofPattern(shareUrlFormatPattern);
        }
        if (goodFrom != null)
            this.goodFrom = goodFrom;
        if (utcAvailabilityOffset != null)
            this.utcAvailabilityOffset = utcAvailabilityOffset;
    }

    /**
     * A unique consistent filename for the given date
     */
    protected String createFileName(LocalDate date) {
        return (
            date.getYear() + "-" +
            date.getMonthValue() + "-" +
            date.getDayOfMonth() + "-" +
            this.downloaderName.replaceAll(" ", "")
        );
    }

    @Override
    public String getInternalName() {
        return internalName;
    }

    @Override
    public String getName() {
        return downloaderName;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public DayOfWeek[] getDownloadDates() {
        return days;
    }

    @Override
    public String getSupportUrl() {
        return supportUrl;
    }

    /**
     * Where to do the actual download from
     */
    protected String getSourceUrl(LocalDate date) {
        return (sourceUrlFormat == null)
            ? null
            : sourceUrlFormat.format(date);
    }

    /**
     * A user-facing URL for the puzzle
     *
     * I.e. go here to find an online playable version, rather than the
     * backend data file
     */
    protected String getShareUrl(LocalDate date) {
        return (shareUrlFormat == null) ? null : shareUrlFormat.format(date);
    }

    @Override
    public DownloadResult download(
        LocalDate date, Set<String> existingFileNames
    ) {
        String fileName = createFileName(date);
        if (existingFileNames.contains(fileName))
            return DownloadResult.ALREADY_EXISTS;

        Puzzle puz = download(date);
        if (puz != null)
            return new DownloadResult(puz, fileName);
        else
            return DownloadResult.FAILED;
    }

    protected Puzzle download(
        LocalDate date,
        Map<String, String> headers
    ){
        try {
            String sourceUrl = getSourceUrl(date);
            if (sourceUrl == null)
                return null;

            URL url = new URL(sourceUrl);
            try (InputStream is = getInputStream(url, headers)) {
                Puzzle puz = puzzleParser.parseInput(is);

                if (puz != null) {
                    puz.setDate(date);
                    puz.setSource(getName());
                    puz.setSourceUrl(sourceUrl);
                    puz.setSupportUrl(getSupportUrl());
                    puz.setShareUrl(getShareUrl(date));
                    puz.setUpdatable(false);

                    return puz;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (MalformedURLException e) {
            LOG.severe("Malformed URL in download: " + e);
        }

        return null;
    }

    protected Puzzle download(LocalDate date) {
        return download(date, EMPTY_MAP);
    }

    @Override
    public boolean alwaysRun(){
        return false;
    }

    @Override
    public boolean isAvailable(LocalDate date) {
        Duration untilAvail = getUntilAvailable(date);
        return untilAvail != null && (
            untilAvail.isZero() || untilAvail.isNegative()
        );
    }

    public Duration getUntilAvailable(LocalDate date) {
        if (date == null)
            return null;

        // check not before puzzle was available
        LocalDate goodFrom = getGoodFrom();
        if (goodFrom != null && goodFrom.isAfter(date))
            return null;

        // check not after puzzle was made unavailable
        LocalDate goodThrough = getGoodThrough();
        if (goodThrough != null && goodThrough.isBefore(date))
            return null;

        // check right day of week
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        boolean isDay = Arrays.binarySearch(
            getDownloadDates(), dayOfWeek
        ) >= 0;

        if (!isDay)
            return null;

        // check current time is before required date plus offset
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        ZonedDateTime availableFrom = ZonedDateTime.of(
            date, LocalTime.MIDNIGHT, ZoneId.of("UTC")
        );
        Duration availabilityOffset = getUTCAvailabilityOffset();
        if (availabilityOffset != null)
            availableFrom = availableFrom.plus(availabilityOffset);

        return Duration.between(now, availableFrom);
    }

    @Override
    public LocalDate getLatestDate() {
        return getLatestDate(null);
    }

    @Override
    public LocalDate getLatestDate(LocalDate until) {
        LocalDate now = LocalDate.now();

        // look a day ahead (plus offset) and in previous week for an
        // available date this relies on puzzles being weekly according
        // to our model include this day last week in case current day
        // not yet available
        int lookAhead = -1;
        Duration availabilityOffset = getUTCAvailabilityOffset();
        if (availabilityOffset != null)
            lookAhead += availabilityOffset.toDays();

        LocalDate startDate = now.plusDays(-lookAhead);
        startDate = (until == null || startDate.isBefore(until))
            ? startDate
            : until;

        for (int i = 0; i <= 7; i++) {
            LocalDate tryDate = startDate.plusDays(-i);
            if (isAvailable(tryDate))
                return tryDate;
        }

        // should never happen unless puzzle not available on any days
        return null;
    }

    /**
     * Last date of availability, or null
     *
     * Null means the puzzle is ongoing.
     */
    protected LocalDate getGoodThrough(){
        return null;
    }

    protected LocalDate getGoodFrom(){
        return LocalDate.ofEpochDay(0L);
    }

    /**
     * When the puzzle for a date is released relative to UTC midnight
     *
     * E.g. if 9am in UTC, will return +9 hours. Could be a negative
     * amount if puzzle comes from a timezone ahead of UTC. Or -24 if
     * puzzles are published at midnight one day in advance.
     */
    protected Duration getUTCAvailabilityOffset() {
        return utcAvailabilityOffset;
    }
}
