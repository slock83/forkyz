package app.crossword.yourealwaysbe.versions;

import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Build;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.function.Consumer;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class JellyBeanMR2Util extends JellyBeanMR1Util {
    @Override
    public ActivityResultLauncher<String> registerForUriContentsResult(
        AppCompatActivity activity, Consumer<List<Uri>> uriConsumer
    ) {
        return activity.registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            new ActivityResultCallback<List<Uri>>() {
                @Override
                public void onActivityResult(List<Uri> uris) {
                    uriConsumer.accept(uris);
                }
            }
        );
    }
}
