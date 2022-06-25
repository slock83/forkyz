package app.crossword.yourealwaysbe;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.logging.Logger;

import android.content.ContentResolver;
import android.net.Uri;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.io.PuzzleStreamReader;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.util.AppPuzzleUtils;
import app.crossword.yourealwaysbe.util.files.FileHandler;
import app.crossword.yourealwaysbe.util.files.PuzHandle;

/**
 * Takes an arbitrary URI and tries to convert to a puzzle and import
 */
public class PuzzleImporter {
    private static final Logger LOGGER
        = Logger.getLogger(PuzzleImporter.class.getCanonicalName());

    private static final String IMPORT_FALLBACK_SOURCE
        = ForkyzApplication.getInstance().getString(
            R.string.import_fallback_source
        );

    /** Import from a URI supported by resolver
     *
     * Currently does not use file extension or MIME type. Instead, use puzlib
     * that tries each known format in turn until one succeeds. Clunky, but
     * hopefully robust.
     *
     * @return new puz handle if succeeded (will return null if failed
     * or uri is null)
     */
    public static PuzHandle importUri(ContentResolver resolver, Uri uri) {
        if (uri == null)
            return null;

        FileHandler fileHandler =
            ForkyzApplication.getInstance().getFileHandler();

        Puzzle puz = null;

        try {
            puz = PuzzleStreamReader.parseInputStatic(
                new BufferedInputStream(resolver.openInputStream(uri))
            );
        } catch (FileNotFoundException e) {
            LOGGER.info("FileNotFoundException with " + uri);
        }

        if (puz == null)
            return null;

        ensurePuzDate(puz);
        ensurePuzSource(puz);

        try {
            return fileHandler.saveNewPuzzle(
                puz, AppPuzzleUtils.generateFileName(puz)
            );
        } catch (IOException e) {
            LOGGER.severe("Failed to save imported puzzle: " + e);
            return null;
        }
    }

    /**
     * Try best to make sure there is some source
     *
     * Fall back to author, title, fallback
     */
    private static void ensurePuzSource(Puzzle puz) {
        String source = puz.getSource();
        if (source == null || source.isEmpty())
            puz.setSource(puz.getAuthor());
        source = puz.getSource();
        if (source == null || source.isEmpty())
            puz.setSource(puz.getTitle());
        source = puz.getSource();
        if (source == null || source.isEmpty())
            puz.setSource(IMPORT_FALLBACK_SOURCE);
    }

    private static void ensurePuzDate(Puzzle puz) {
        if (puz.getDate() == null)
            puz.setDate(LocalDate.now());
    }
}
