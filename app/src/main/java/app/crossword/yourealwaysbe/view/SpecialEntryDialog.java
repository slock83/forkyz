
package app.crossword.yourealwaysbe.view;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Playboard;

/**
 * Dialog to insert special characters into board
 *
 * Pass the board to the constructor. The dialog will play the special letter
 * when entered.
 */
public class SpecialEntryDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = (LayoutInflater)
            getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup root = (ViewGroup) getActivity().findViewById(
            R.id.special_entry_root
        );
        final View layout = inflater.inflate(
            R.layout.special_entry_dialog, root
        );

        MaterialAlertDialogBuilder builder
            = new MaterialAlertDialogBuilder(getActivity());

        builder.setTitle(getString(R.string.special_entry))
            .setView(layout);

        Playboard board = ForkyzApplication.getInstance().getBoard();
        if (board == null)
            return builder.create();

        Box curBox = board.getCurrentBox();
        String curLetter = null;
        if (!curBox.isBlank()) {
            EditText input = layout.findViewById(R.id.special_entry_edit_box);
            input.setText(curBox.getResponse());
        }

        builder.setPositiveButton(
                R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        EditText input = layout.findViewById(
                            R.id.special_entry_edit_box
                        );
                        String text = input.getText().toString();
                        String letter = text.length() < 1 ? Box.BLANK : text;
                        if (board != null)
                            board.playLetter(letter);
                    }
                }
            )
            .setNegativeButton(
                R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }
            );

        return builder.create();
    }
}
