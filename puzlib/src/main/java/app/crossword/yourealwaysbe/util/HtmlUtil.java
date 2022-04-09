
package app.crossword.yourealwaysbe.util;

import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

public class HtmlUtil {
    // IPuz tags not to strip from HTML (preserve line breaks)
    private static final Whitelist JSOUP_CLEAN_WHITELIST = new Whitelist();
    static {
        JSOUP_CLEAN_WHITELIST.addTags("br");
    }

    /**
     * Remove IPuz HTML from a string
     * @return decoded string or null if value was null
     */
    public static String unHtmlString(String value) {
        if (value == null)
            return null;

        // this is a bit hacky: any break tag is normalised to "\r?\n<br>"
        // by the clean method, we remove the \r\ns and turn <br> into \n
        return StringEscapeUtils.unescapeHtml4(
            Jsoup.clean(value, JSOUP_CLEAN_WHITELIST)
                .replace("\r", "")
                .replace("\n", "")
                .replace("<br>", "\n")
        );
    }

    /**
     * Return IPuz HTML encoding of string
     * @return encoded string or null if value was null
     */
    public static String htmlString(String value) {
        if (value == null)
            return null;

        return StringEscapeUtils.escapeHtml4(value)
            .replace("\r", "")
            .replace("\n", "<br/>");
    }
}
