
package app.crossword.yourealwaysbe.net;

import java.time.LocalDate;
import java.util.AbstractList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Parameterized;

import android.content.Context;
import android.content.SharedPreferences;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class DownloadersTest {

    private static final String DEFAULT_NAME = "puzzle";

    /**
     * Parameters are the list of downloaders
     *
     * Gotten from the Downloaders class (needs mocking of context and
     * prefs). Returned as a list of pairs: (internalName, downloader).
     */
    @Parameters(name = "Downloader {0}")
    public static Iterable<Object[]> downloaders() {
        Context context = mock(Context.class);
        SharedPreferences prefs = mock(SharedPreferences.class);
        initialiseMocks(context, prefs);
        final List<Downloader> downloaders
            = (new Downloaders(context, prefs)).getDownloaders();
        return new AbstractList<Object[]>() {
            @Override
            public int size() { return downloaders.size(); }

            @Override
            public Object[] get(int i) {
                Downloader d = downloaders.get(i);
                return new Object[] { d.getInternalName(), d };
            }
        };
    }

    private Downloader downloader;

    public DownloadersTest(String name, Downloader downloader) {
        this.downloader = downloader;
    }

    @Test
    public void testDownloaders() {
        LocalDate latest = downloader.getLatestDate();
        Downloader.DownloadResult res =
            downloader.download(latest, Collections.emptySet());
        assertTrue(res.isSuccess());
    }

    private static void initialiseMocks(
        Context context, SharedPreferences prefs
    ) {
        // say yes to all downloaders
        when(prefs.getBoolean(anyString(), anyBoolean())).thenReturn(true);
        // except dummy custom downloader
        when(prefs.getBoolean(eq("downloadCustomDaily"), anyBoolean()))
            .thenReturn(false);

        // timeout
        when(
            prefs.getString(eq(Downloaders.PREF_DOWNLOAD_TIMEOUT), anyString())
        ).thenReturn("30000");

        // when names come from context, give any
        when(context.getString(anyInt())).thenReturn(DEFAULT_NAME);
    }
}

