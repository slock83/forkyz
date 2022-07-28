
package app.crossword.yourealwaysbe.net;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map.Entry;
import java.util.Map;

/**
 * Base class for downloaders
 *
 * Provides some useful methods. Recommended to always use
 * getInputStream. If not, set a timeout of DOWNLOAD_TIMEOUT_MILLIS.
 */
public abstract class AbstractDownloader implements Downloader {
    protected static final int DOWNLOAD_TIMEOUT_MILLIS = 10000;

    protected BufferedInputStream getInputStream(
        URL url, Map<String, String> headers
    ) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(DOWNLOAD_TIMEOUT_MILLIS);
        conn.setReadTimeout(DOWNLOAD_TIMEOUT_MILLIS);
        conn.setRequestProperty("Connection", "close");

        if (headers != null) {
            for (Entry<String, String> e : headers.entrySet()){
                conn.setRequestProperty(e.getKey(), e.getValue());
            }
        }

        return new BufferedInputStream(conn.getInputStream());
    }
}
