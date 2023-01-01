
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
 * getInputStream. If not, set a timeout of getTimeout().
 */
public abstract class AbstractDownloader implements Downloader {
    private int timeoutMillis = 30000;

    public void setTimeout(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public int getTimeout() {
        return timeoutMillis;
    }

    protected BufferedInputStream getInputStream(
        URL url, Map<String, String> headers
    ) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        int timeout = getTimeout();
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);
        conn.setRequestProperty("Connection", "close");
        conn.setRequestProperty("Accept","*/*");

        if (headers != null) {
            for (Entry<String, String> e : headers.entrySet()){
                conn.setRequestProperty(e.getKey(), e.getValue());
            }
        }

        return new BufferedInputStream(conn.getInputStream());
    }
}
