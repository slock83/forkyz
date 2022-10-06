package app.crossword.yourealwaysbe.net;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

import app.crossword.yourealwaysbe.puz.Puzzle;

public interface Downloader {

    public static class DownloadResult {
        public static final DownloadResult FAILED
            = new DownloadResult(true);
        public static final DownloadResult ALREADY_EXISTS
            = new DownloadResult(false);

        Puzzle puzzle;
        String fileName;
        boolean failed;

        public DownloadResult(Puzzle puzzle, String fileName) {
            this.puzzle = puzzle;
            this.fileName = fileName;
        }

        private DownloadResult(boolean failed) {
            this.failed = failed;
        }

        public boolean isFailed() {
            return puzzle == null && fileName == null && failed;
        }

        public boolean isAlreadyExists() {
            return puzzle == null && fileName == null && !failed;
        }

        public boolean isSuccess() {
            return puzzle != null && fileName != null;
        }

        public Puzzle getPuzzle() { return puzzle; }
        public String getFileName() { return fileName; }
    }

    // These lists must be sorted for binary search.
    DayOfWeek[] DATE_SUNDAY = new DayOfWeek[] { DayOfWeek.SUNDAY };
    DayOfWeek[] DATE_MONDAY = new DayOfWeek[] { DayOfWeek.MONDAY };
    DayOfWeek[] DATE_TUESDAY = new DayOfWeek[] { DayOfWeek.TUESDAY };
    DayOfWeek[] DATE_WEDNESDAY = new DayOfWeek[] { DayOfWeek.WEDNESDAY };
    DayOfWeek[] DATE_THURSDAY = new DayOfWeek[] { DayOfWeek.THURSDAY };
    DayOfWeek[] DATE_FRIDAY = new DayOfWeek[] { DayOfWeek.FRIDAY };
    DayOfWeek[] DATE_SATURDAY = new DayOfWeek[] { DayOfWeek.SATURDAY };
    DayOfWeek[] DATE_DAILY = new DayOfWeek[] {
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
        DayOfWeek.SUNDAY
    };
    DayOfWeek[] DATE_NO_SUNDAY = new DayOfWeek[] {
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY
    };
    DayOfWeek[] DATE_WEEKDAY = new DayOfWeek[] {
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
    };

    DayOfWeek[] getDownloadDates();

    String getName();

    /**
     * Returns a URL where the user may support the crossword source
     *
     * @return null if no reasonable support/source URL (discouraged)
     */
    String getSupportUrl();

    /**
     * Download the puzzle for a given date
     *
     * Existing file names passed. Don't download if the date's puzzle
     * already exists -- downloader will not save puzzles with filenames
     * that already exist.
     */
    DownloadResult download(LocalDate date, Set<String> existingFileNames);

    boolean alwaysRun();

    LocalDate getGoodThrough();

    LocalDate getGoodFrom();
}
