
package app.crossword.yourealwaysbe.versions;

import java.io.Serializable;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;

@TargetApi(Build.VERSION_CODES.TIRAMISU)
public class TiramisuUtil extends RUtil {
    @Override
    public <T extends Serializable>
    T getSerializable(Bundle bundle, String key, Class<T> klass) {
        return bundle.getSerializable(key, klass);
    }
}
