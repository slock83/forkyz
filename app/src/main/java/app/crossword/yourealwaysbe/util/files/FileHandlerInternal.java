
package app.crossword.yourealwaysbe.util.files;

import java.io.IOException;
import java.util.logging.Logger;

import android.content.Context;

import app.crossword.yourealwaysbe.versions.AndroidVersionUtils;

/**
 * File storage in the default internal app storage location
 */
public class FileHandlerInternal extends FileHandlerJavaFile {
    private static final Logger LOGGER
        = Logger.getLogger(FileHandlerInternal.class.getCanonicalName());

    public FileHandlerInternal(Context applicationContext) {
        super(applicationContext, applicationContext.getFilesDir());
    }

    @Override
    public boolean isStorageMounted() {
        return true;
    }

    @Override
    public boolean isStorageFull() {
        try {
            return AndroidVersionUtils.Factory
                .getInstance()
                .isInternalStorageFull(
                    getApplicationContext(), MINIMUM_STORAGE_REQUIRED
                );
        } catch (IOException e) {
            // we don't know it's not full...
            return false;
        }
    }
}
