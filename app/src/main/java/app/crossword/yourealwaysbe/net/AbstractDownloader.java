
package app.crossword.yourealwaysbe.net;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map.Entry;
import java.util.Map;

import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;

/**
 * Base class for downloaders
 *
 * Provides some useful methods. Recommended to always use
 * getInputStream. If not, set a timeout of DOWNLOAD_TIMEOUT_MILLIS.
 */
public abstract class AbstractDownloader implements Downloader {
    private static final String PREF_DOWNLOAD_TIMEOUT = "downloadTimeout";

    private static final String DEFAULT_TIMEOUT_MILLIS = "30000";

    protected int getTimeout() {
        ForkyzApplication app = ForkyzApplication.getInstance();
        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(app);
        return Integer.valueOf(
            prefs.getString(PREF_DOWNLOAD_TIMEOUT, DEFAULT_TIMEOUT_MILLIS)
        );
    }

    protected BufferedInputStream getInputStream(
        URL url, Map<String, String> headers
    ) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        int timeout = getTimeout();
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);
        conn.setRequestProperty("Connection", "close");

        if (headers != null) {
            for (Entry<String, String> e : headers.entrySet()){
                conn.setRequestProperty(e.getKey(), e.getValue());
            }
        }

        return new BufferedInputStream(conn.getInputStream());
    }
}
