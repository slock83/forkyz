
package app.crossword.yourealwaysbe.util;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

import app.crossword.yourealwaysbe.puz.Puzzle;

public class AppPuzzleUtils {
    // date, name, uuid
    private static final String FILE_NAME_PATTERN
        = "%s-%s-%s";
    private static final String FILE_NAME_REMOVE_CHARS = "[^A-Za-z0-9]";

    /**
     * Generate a file name for the given puzzle
     */
    public static String generateFileName(Puzzle puzzle) {
        String name = puzzle.getSource();
        if (name == null || name.isEmpty())
            name = puzzle.getAuthor();
        if (name == null || name.isEmpty())
            name = puzzle.getTitle();
        if (name == null)
            name = "";

        LocalDate date = puzzle.getDate();
        if (date == null)
            date = LocalDate.now();

        String normalizedName
            = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll(FILE_NAME_REMOVE_CHARS, "");

        return String.format(
            Locale.US,
            FILE_NAME_PATTERN, date, normalizedName, UUID.randomUUID()
        );
    }
}
