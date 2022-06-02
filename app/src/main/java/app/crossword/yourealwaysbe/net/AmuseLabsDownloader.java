package app.crossword.yourealwaysbe.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

import android.util.Base64;

import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.io.AmuseLabsJSONIO;

/**
 * Amuse Labs Downloader
 * URL: https://<cdn>.amuselabs.com/<shortname>/crossword?id=<idPrefix>YYMMDD&set=<setname>
 */
public class AmuseLabsDownloader extends AbstractDateDownloader {
    private static final String BASE_URL_FMT
        = "https://%s.amuselabs.com/%s/crossword";
    private static final String URL_SUFFIX_FMT = "?id=%s%02d%02d%02d&set=%s";

    private String idPrefix;
    private String setName;

    /**
     * Construct an Amuse Labs downloader
     *
     * @param name the friendly name for the crosswords
     * @param cdn the cdn used for this crossword
     * @param shortname the shortname appearing in the download URL
     * @param idPrefx the prefix of the id for the URL
     * @param setName the set of crosswords it belongs to for the URL
     * @param days as for AbstractDownloader
     * @param supportUrl where to go to support puzzle source
     */
    public AmuseLabsDownloader(
        String name,
        String cdn,
        String shortname,
        String idPrefix,
        String setName,
        DayOfWeek[] days,
        String supportUrl
    ) {
        super(
            String.format(BASE_URL_FMT, cdn, shortname), name, days, supportUrl, null
        );
        this.idPrefix = idPrefix;
        this.setName = setName;
    }

    @Override
    protected Puzzle download(
        LocalDate date,
        String urlSuffix,
        Map<String, String> headers
    ) {
        try {
            URL url = new URL(this.baseUrl + urlSuffix);
            String cwJson = getCrosswordJSON(url, headers);

            if (cwJson == null)
                return null;

            Puzzle puz = AmuseLabsJSONIO.readPuzzle(cwJson);

            if (puz != null) {
                puz.setSource(getName());
                puz.setSupportUrl(getSupportUrl());
            }

            return puz;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    protected String createUrlSuffix(LocalDate date) {
        return String.format(
            Locale.US,
            URL_SUFFIX_FMT,
            idPrefix,
            date.getYear() % 100,
            date.getMonthValue(),
            date.getDayOfMonth(),
            setName
        );
    }

    /**
     * Get JSON string from URL
     *
     * With thanks to https://github.com/thisisparker/xword-dl
     */
    private String getCrosswordJSON(
        URL url, Map<String, String> headers
    ) throws IOException {
        try(
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(getInputStream(url, headers))
            )
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("window.rawc")) {
                    String[] split = line.split("'");
                    if (split.length < 2) {
                        // there is something wrong if we don't have
                        // window.rawc = '<base64>'
                        return null;
                    }
                    return decodeRawc(split[1]);
                }
            }
        }
        return null;
    }

    private String decodeRawc(String rawc) {
        // sometimes needs segments reversing according to numbers after
        // the . (thanks to https://github.com/thisisparker/xword-dl)
        String[] split = rawc.split("\\.");
        String data = split[0];

        if (split.length > 0) {
            int[] segmentLengths = getSegmentLengths(split[1]);
            data = reverseSegments(data, segmentLengths);
        }

        return new String(
            Base64.decode(data, Base64.DEFAULT),
            Charset.forName("UTF-8")
        );
    }

    private int[] getSegmentLengths(String hexLengths) {
        int numLengths = hexLengths.length();
        int[] segmentLengths = new int[numLengths];
        for (int i = 0; i < numLengths; i++) {
            segmentLengths[numLengths - i - 1]
                = Integer.parseInt(hexLengths.substring(i, i + 1), 16) + 2;
        }
        return segmentLengths;
    }

    private String reverseSegments(String data, int[] segmentLengths) {
        int numLengths = segmentLengths.length;
        char[] charData = data.toCharArray();

        int segNo = 0;
        int segStart = 0;

        while (segStart < charData.length - 1) {
            int segEnd = Math.min(
                segStart + segmentLengths[segNo] - 1, charData.length - 1
            );
            int segLen = segEnd - segStart + 1;

            for (int i = 0; i < (segLen / 2); i++) {
                char tmp = charData[segStart + i];
                charData[segStart + i] = charData[segEnd - i];
                charData[segEnd - i] = tmp;
            }

            segStart += segLen;
            segNo = (segNo + 1) % numLengths;
        }
        return new String(charData);
    }
}
