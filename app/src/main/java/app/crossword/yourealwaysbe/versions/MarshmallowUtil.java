
package app.crossword.yourealwaysbe.versions;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

@TargetApi(Build.VERSION_CODES.M)
public class MarshmallowUtil extends LollipopUtil {
    @Override
    public int immutablePendingIntentFlag() {
        return PendingIntent.FLAG_IMMUTABLE;
    }

    @Override
    public StaticLayout getStaticLayout(
        CharSequence text, TextPaint style, int width, Layout.Alignment align
    ) {
        return StaticLayout.Builder.obtain(
            text, 0, text.length(), style, width
        ).setAlignment(align)
        .build();
    }
}
