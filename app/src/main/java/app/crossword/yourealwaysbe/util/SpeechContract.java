
package app.crossword.yourealwaysbe.util;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.speech.RecognizerIntent;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Voice recognition contract
 *
 * Takes speech model (e.g. RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).
 * Returns array of strings heard.
 */
public class SpeechContract extends ActivityResultContract<String, List<String>> {
    @NonNull
    @Override
    public Intent createIntent(
        @NonNull Context context, @NonNull String model
    ) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL, model
        );
        return intent;
    }

    @Override
    public List<String> parseResult(int resultCode, @Nullable Intent intent) {
        List<String> texts = null;
        if (resultCode == Activity.RESULT_OK) {
            texts = intent.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS
            );
        }
        return texts;
    }
}
