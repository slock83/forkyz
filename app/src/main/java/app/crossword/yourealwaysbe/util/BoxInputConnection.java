
package app.crossword.yourealwaysbe.util;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import androidx.core.view.inputmethod.EditorInfoCompat;

public class BoxInputConnection extends BaseInputConnection {

    private static final int INPUT_TYPE =
        InputType.TYPE_CLASS_TEXT
        | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;

    private BoardEditable boardEditable;
    private View targetView;
    private BoxInputListener listener;
    private int updateExtractedTextToken = -1;

    public interface BoxInputListener {
        default void onNewResponse(String response) { }
        default void onDeleteResponse() { }
    }

    public BoxInputConnection(
        View targetView,
        String initialResponse,
        BoxInputListener listener
    ) {
        super(targetView, true);
        this.targetView = targetView;
        this.boardEditable = new BoardEditable(initialResponse);
        this.listener = listener;
    }

    public void setOutAttrs(EditorInfo outAttrs) {
        outAttrs.fieldId = targetView.getId();
        outAttrs.inputType = INPUT_TYPE;
        outAttrs.imeOptions = EditorInfo.IME_NULL;
        outAttrs.initialSelStart
            = boardEditable.getSpanStart(Selection.SELECTION_START);
        outAttrs.initialSelEnd
            = boardEditable.getSpanStart(Selection.SELECTION_END);
        outAttrs.packageName = targetView.getContext().getPackageName();
        outAttrs.initialCapsMode = TextUtils.CAP_MODE_CHARACTERS;
        EditorInfoCompat.setInitialSurroundingText(
            outAttrs, boardEditable.toString()
        );
    }

    @Override
    public Editable getEditable() {
        return boardEditable;
    }

    @Override
    public ExtractedText getExtractedText(
        ExtractedTextRequest request, int flags
    ) {
        // call with null internally for convenience
        if (request != null) {
            if ((flags & InputConnection.GET_EXTRACTED_TEXT_MONITOR) != 0)
                updateExtractedTextToken = request.token;
        }

        ExtractedText extracted = new ExtractedText();
        extracted.flags = 0;
        extracted.partialStartOffset = -1;
        extracted.selectionStart = 0;
        extracted.startOffset = 0;
        String response = boardEditable.toString();
        extracted.text = response;
        extracted.selectionEnd = response.length();

        return extracted;
    }

    @Override
    public boolean deleteSurroundingText(int before, int after) {
        // bit of a hack for FlorisBoard -- it seems to miss the
        // connection restart and think old characters are still in its
        // buffer, or there's no selection, or something. It ends up
        // trying to delete before the cursor. Treat this as a delete of
        // the cell, because that's the only delete we support
        boardEditable.clear();
        return true;
    }

    @Override
    public boolean deleteSurroundingTextInCodePoints(int before, int after) {
        // See deleteSurroundingText
        boardEditable.clear();
        return true;
    }

    public void setResponse(String response) {
        boardEditable.setResponse(response);
    }

    public void refreshInput() {
        InputMethodManager imm = getInputMethodManager();
        // this ultimately kills the input connection and creates a
        // new one, but in API 33 we can replace it with
        // invalidateInput
        imm.restartInput(targetView);
    }

    private InputMethodManager getInputMethodManager() {
        return (InputMethodManager) targetView
            .getContext()
            .getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    private class BoardEditable implements Editable {
        SpannableStringBuilder spannable;

        public BoardEditable(String initialResponse) {
            spannable = new SpannableStringBuilder(initialResponse);
            Selection.selectAll(spannable);
        }

        @Override
        public Editable append(char text) {
            return append(String.valueOf(text));
        }

        @Override
        public Editable append(CharSequence text, int start, int end) {
            return replace(length(), length(), text, start, end);
        }

        @Override
        public Editable append(CharSequence text) {
            return replace(length(), length(), text, 0, text.length());
        }

        @Override
        public void clear() {
            replace(0, length(), "", 0, 0);
        }

        @Override
        public void clearSpans() {
            spannable.clearSpans();
        }

        @Override
        public Editable delete(int st, int en) {
            return replace(st, en, "", 0, 0);
        }

        @Override
        public InputFilter[] getFilters() {
            return spannable.getFilters();
        }

        @Override
        public Editable insert(int where, CharSequence text) {
            return replace(where, where, text, 0, text.length());
        }

        @Override
        public Editable insert(
            int where, CharSequence text, int start, int end
        ) {
            return replace(where, where, text, start, end);
        }

        @Override
        public Editable replace(
            int st, int en, CharSequence source, int start, int end
        ) {
            spannable.replace(st, en, source, start, end);
            if (listener != null) {
                // if removing characters, delete
                if (spannable.length() == 0) {
                    listener.onDeleteResponse();
                } else {
                    // give whole response so emojis work
                    listener.onNewResponse(spannable.toString());
                }
            }
            return this;
        }

        @Override
        public Editable replace(int st, int en, CharSequence text) {
            return replace(st, en, text, 0, text.length());
        }

        @Override
        public void setFilters(InputFilter[] filters) {
            spannable.setFilters(filters);
        }

        @Override
        public char charAt(int index) {
            return spannable.charAt(index);
        }

        @Override
        public int length() {
            return spannable.length();
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return spannable.subSequence(start, end);
        }

        @Override
        public String toString() {
            return spannable.toString();
        }

        @Override
        public void getChars(
            int start, int end, char[] dest, int destoff
        ) {
            spannable.getChars(start, end, dest, destoff);
        }

        @Override
        public void removeSpan(Object what) {
            spannable.removeSpan(what);
        }

        @Override
        public void setSpan(Object what, int start, int end, int flags) {
            spannable.setSpan(what, start, end, flags);
        }

        @Override
        public int getSpanEnd(Object tag) {
            return spannable.getSpanEnd(tag);
        }

        @Override
        public int getSpanFlags(Object tag) {
            return spannable.getSpanFlags(tag);
        }

        @Override
        public int getSpanStart(Object tag) {
            return spannable.getSpanStart(tag);
        }

        @Override
        public <T> T[] getSpans(int start, int end, Class<T> type) {
            return spannable.getSpans(start, end, type);
        }

        @Override
        @SuppressWarnings("rawtypes")
        public int nextSpanTransition(int start, int limit, Class type) {
            return spannable.nextSpanTransition(start, limit, type);
        }

        public void setResponse(String response) {
            spannable.replace(0, spannable.length(), response);
            Selection.selectAll(spannable);

            // have to do this because the text being edited has changed
            // this causes the keyboard to reset too, which is annoying,
            // but no way around it for a box-by-box model of input (see
            // documentation of InputMethodManager.restartInput).
            refreshInput();

            // It would be nicer if we could get away with the code
            // below, but the input method stops reporting deletion. This
            // is code we should run when the selection changes, but
            // there's no point if we're going to reset the connection
            // anyway!
            //
            // InputMethodManager imm = getInputMethodManager();
            // imm.updateSelection(
            //     targetView,
            //     0, spannable.length(),
            //     getComposingSpanStart(spannable), getComposingSpanEnd(spannable)
            // );
            // if (updateExtractedTextToken > -1) {
            //     imm.updateExtractedText(
            //         targetView,
            //         updateExtractedTextToken,
            //         getExtractedText(null, 0)
            //     );
            // }
        }
    };
}
