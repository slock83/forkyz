
package app.crossword.yourealwaysbe.versions;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.text.StaticLayout;
import android.text.TextPaint;

@TargetApi(23)
public class MarshmallowUtil extends LollipopUtil {
    @Override
    public int immutablePendingIntentFlag() {
        return PendingIntent.FLAG_IMMUTABLE;
    }

    @Override
    public StaticLayout getStaticLayout(
        CharSequence text, TextPaint style, int width
    ) {
        return StaticLayout.Builder.obtain(
            text, 0, text.length(), style, width
        )
        .build();
    }
}
