
package app.crossword.yourealwaysbe.util.files;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Logger;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import androidx.core.content.FileProvider;

import app.crossword.yourealwaysbe.io.IPuzIO;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.util.AppPuzzleUtils;

public class FileHandlerShared extends FileProvider {
    private static final Logger LOGGER
        = Logger.getLogger(FileHandlerShared.class.getCanonicalName());

    private static final String SHARE_AUTHORITY
        = "app.crossword.yourealwaysbe.forkyzfiles";
    private static final String SHARE_DIR = "shared";
    // clear older than 1 hour
    private static final long CACHE_CLEAR_AGE_MILLIS = 60 * 60 * 1000L;

    private static ExecutorService executorService
        = Executors.newSingleThreadExecutor();
    private static Handler handler = new Handler(Looper.getMainLooper());

    /**
     * Save the puzzle to cache and get Uri for sharing
     *
     * Runs off main thread to save file, calls back on main thread with
     * uriCallback.
     *
     * @param writeOriginal whether to omit current play data such as
     * flagged clues and filled in letters
     */
    synchronized public static void getShareUri(
        Context context, Puzzle puz, boolean writeOriginal,
        Consumer<Uri> uriCallback
    ) {
        executorService.execute(() -> {
            cleanCache(context);
            final Uri uri = getShareUriBackground(
                context, puz, writeOriginal, uriCallback
            );
            handler.post(() -> {
                uriCallback.accept(uri);
            });
        });
    }

    public static String getShareUriMimeType() {
        return FileHandler.MIME_TYPE_IPUZ;
    }

    private static File getShareDir(Context context) {
        File shareDir = new File(context.getCacheDir(), SHARE_DIR);
        shareDir.mkdirs();
        return shareDir;
    }

    synchronized static private Uri getShareUriBackground(
        Context context, Puzzle puz, boolean writeOriginal,
        Consumer<Uri> uriCallback
    ) {
        String fileName = AppPuzzleUtils.generateFileName(puz)
            + FileHandler.FILE_EXT_IPUZ;
        File shareFile = new File(getShareDir(context), fileName);

        try (
            OutputStream os = new BufferedOutputStream(
                new FileOutputStream(shareFile)
            )
        ) {
            IPuzIO.writePuzzle(puz, os, writeOriginal);
        } catch (IOException e) {
            LOGGER.severe("Could not create file for sharing: " + e);
            return null;
        }

        return getUriForFile(context, SHARE_AUTHORITY, shareFile);
    }

    synchronized static private void cleanCache(Context context) {
        File[] files = getShareDir(context).listFiles();
        for (File file : files) {
            long ageMillis = System.currentTimeMillis() - file.lastModified();
            if (ageMillis > CACHE_CLEAR_AGE_MILLIS)
                file.delete();
        }
    }
}
