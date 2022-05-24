
package app.crossword.yourealwaysbe.util.files;

import java.util.logging.Logger;

import android.content.Context;
import android.os.Environment;

import app.crossword.yourealwaysbe.versions.AndroidVersionUtils;

/**
 * Implementation of original Shortyz file access directly working with
 * external SD card directory.
 */
@SuppressWarnings("deprecation")
public class FileHandlerLegacy extends FileHandlerJavaFile {
    private static final Logger LOGGER
        = Logger.getLogger(FileHandlerLegacy.class.getCanonicalName());

    @SuppressWarnings("deprecation")
    public FileHandlerLegacy(Context applicationContext) {
        super(applicationContext, Environment.getExternalStorageDirectory());
    }

    @Override
    public boolean isStorageMounted() {
        return Environment.MEDIA_MOUNTED.equals(
            Environment.getExternalStorageState()
        );
    }

    @Override
    public boolean isStorageFull() {
        return AndroidVersionUtils.Factory
            .getInstance()
            .isExternalStorageDirectoryFull(
                Environment.getExternalStorageDirectory(),
                MINIMUM_STORAGE_REQUIRED
            );
    }
}
