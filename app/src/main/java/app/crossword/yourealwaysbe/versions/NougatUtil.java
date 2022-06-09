
package app.crossword.yourealwaysbe.versions;

import java.util.function.Consumer;

import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Build;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree;
import androidx.fragment.app.Fragment;

@TargetApi(Build.VERSION_CODES.N)
public class NougatUtil extends MarshmallowUtil {
    @Override
    public ActivityResultLauncher<Uri> registerForSAFUriResult(
        Fragment fragment, Consumer<Uri> uriConsumer
    ) {
        return fragment.registerForActivityResult(
            new OpenDocumentTree(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    uriConsumer.accept(uri);
                }
            }
        );
    }
}
