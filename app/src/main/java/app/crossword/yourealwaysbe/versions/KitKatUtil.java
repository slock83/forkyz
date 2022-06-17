package app.crossword.yourealwaysbe.versions;

import android.annotation.TargetApi;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class KitKatUtil extends JellyBeanMR2Util {
    @Override
    public boolean isAcceptableCharacterResponse(char c) {
        return !Character.isISOControl(c) && !Character.isSurrogate(c);
    }
}
