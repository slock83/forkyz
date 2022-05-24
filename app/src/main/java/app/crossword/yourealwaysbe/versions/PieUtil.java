package app.crossword.yourealwaysbe.versions;

import android.annotation.TargetApi;
import android.graphics.Typeface;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.P)
public class PieUtil extends OreoUtil {
    @Override
    public Typeface getSemiBoldTypeface() {
        return Typeface.create(Typeface.SANS_SERIF, 600, false);
    }
}
