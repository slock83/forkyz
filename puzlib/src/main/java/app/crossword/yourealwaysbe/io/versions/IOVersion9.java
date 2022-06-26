
package app.crossword.yourealwaysbe.io.versions;

import java.nio.charset.Charset;

// Support utf-8
public class IOVersion9 extends IOVersion8 {
    private Charset CHARSET = Charset.forName("UTF-8");

    @Override
    public Charset getCharset() {
        return CHARSET;
    }
}
