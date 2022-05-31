
package app.crossword.yourealwaysbe.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.fragment.app.DialogFragment;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Playboard;

/**
 * Dialog to insert special characters into board
 *
 * Pass the board to the constructor. The dialog will play the special letter
 * when entered.
 */
public class InsertSpecialCharacterDialog extends DialogFragment {
    private Playboard board;

    public InsertSpecialCharacterDialog(Playboard board) {
        this.board = board;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = (LayoutInflater)
            getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup root = (ViewGroup) getActivity().findViewById(
            R.id.special_character_root
        );
        final View layout = inflater.inflate(
            R.layout.special_character_dialog, root
        );

        AlertDialog.Builder builder
            = new AlertDialog.Builder(getActivity());

        builder.setTitle(getString(R.string.insert_special_character))
            .setView(layout)
            .setPositiveButton(
                R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        EditText input = layout.findViewById(
                            R.id.special_character_edit_box
                        );
                        String text = input.getText().toString();
                        char letter = text.length() < 1
                            ? Box.BLANK
                            : text.charAt(0);
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
