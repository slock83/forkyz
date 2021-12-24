
package app.crossword.yourealwaysbe.versions;

import android.annotation.TargetApi;
import android.app.PendingIntent;

@TargetApi(23)
public class MarshmallowUtil extends LollipopUtil {
    @Override
    public int immutablePendingIntentFlag() {
        return PendingIntent.FLAG_IMMUTABLE;
    }
}
