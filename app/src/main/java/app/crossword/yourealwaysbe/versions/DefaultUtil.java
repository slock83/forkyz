package app.crossword.yourealwaysbe.versions;

import android.content.Context;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.Window;
import androidx.appcompat.app.AppCompatActivity;

public abstract class DefaultUtil implements AndroidVersionUtils {
    public abstract void setContext(Context ctx);

    public abstract void onActionBarWithText(MenuItem a);

    public abstract void onActionBarWithText(SubMenu reveal);

    public void hideWindowTitle(AppCompatActivity a) {
        a.requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    public abstract void onActionBarWithoutText(MenuItem a);

    @Override
    public abstract int immutablePendingIntentFlag();

    @Override
    public abstract StaticLayout getStaticLayout(
        CharSequence text, TextPaint style, int width
    );
}
