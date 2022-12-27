
package app.crossword.yourealwaysbe.net;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import android.content.Context;
import android.content.SharedPreferences;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DownloadersTest {

    private static final String DEFAULT_NAME = "puzzle";

    @Mock
    Context context;

    @Mock
    SharedPreferences prefs;

    @Test
    public void testDownloaders() {
        initialiseMocks();

        List<Downloader> downloaders
            = Downloaders.getDownloaders(prefs, context);

        for (Downloader downloader : downloaders) {
            System.out.println(
                "Testing " + downloader.getInternalName() + " download."
            );
            LocalDate latest = downloader.getLatestDate();
            Downloader.DownloadResult res =
                downloader.download(latest, Collections.emptySet());
            assertTrue(res.isSuccess());
        }
    }

    private void initialiseMocks() {
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

