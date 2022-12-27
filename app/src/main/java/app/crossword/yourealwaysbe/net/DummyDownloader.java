package app.crossword.yourealwaysbe.net;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Set;

/**
 * Does not actually download any puzzles; just adds an "All Available" option to the dropdown.
 */
public class DummyDownloader implements Downloader {
    @Override
    public DayOfWeek[] getDownloadDates() {
        return null;
    }

    @Override
    public String getInternalName() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getSupportUrl() {
        return null;
    }

    @Override
    public DownloadResult download(
        LocalDate date, Set<String> existingFileNames
    ) {
        return DownloadResult.FAILED;
    }

    @Override
    public String toString() {
        return "All available";
    }

    @Override
    public boolean alwaysRun(){
        return false;
    }

    @Override
    public boolean isAvailable(LocalDate date) {
        return true;
    }

    @Override
    public Duration getUntilAvailable(LocalDate date) {
        return null;
    }

    @Override
    public LocalDate getLatestDate() {
        return null;
    }

    @Override
    public LocalDate getLatestDate(LocalDate until) {
        return null;
    }

    @Override
    public void setTimeout(int timeoutMillis) { }

    @Override
    public int getTimeout() {
        return -1;
    }
}
