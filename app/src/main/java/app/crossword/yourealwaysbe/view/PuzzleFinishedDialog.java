package app.crossword.yourealwaysbe;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Puzzle;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

public class PuzzleFinishedDialog extends DialogFragment {
    private static final long SECONDS = 1000;
    private static final long MINUTES = SECONDS * 60;
    private static final long HOURS = MINUTES * 60;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();

        MaterialAlertDialogBuilder builder
            = new MaterialAlertDialogBuilder(activity);

        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(
            Context.LAYOUT_INFLATER_SERVICE
        );
        View layout = inflater.inflate(
            R.layout.completed,
            (ViewGroup) activity.findViewById(R.id.finished)
        );

        builder.setTitle(activity.getString(R.string.puzzle_finished_title));
        builder.setView(layout);

        Playboard board = ForkyzApplication.getInstance().getBoard();
        Puzzle puz = board == null ? null : board.getPuzzle();
        if (board == null)
            return builder.create();

        addCompletedMsg(puz, layout.findViewById(R.id.puzzle_completed_msg));

        long elapsed = puz.getTime();
        long finishedTime = elapsed;

        long hours = elapsed / HOURS;
        elapsed = elapsed % HOURS;

        long minutes = elapsed / MINUTES;
        elapsed = elapsed % MINUTES;

        long seconds = elapsed / SECONDS;

        String elapsedString;
        if (hours > 0) {
            elapsedString = activity.getString(
                R.string.completed_time_format_with_hours,
                hours, minutes, seconds
            );
        } else {
            elapsedString = activity.getString(
                R.string.completed_time_format_no_hours,
                minutes, seconds
            );
        }

        int totalClues = puz.getNumberOfClues();
        int totalBoxes = 0;
        int cheatedBoxes = 0;
        for(Box b : puz.getBoxesList()){
            if(b == null){
                continue;
            }
            if(b.isCheated()){
                cheatedBoxes++;
            }
            totalBoxes++;
        }

        int cheatLevel = cheatedBoxes * 100 / totalBoxes;
        if(cheatLevel == 0 && cheatedBoxes > 0){
            cheatLevel = 1;
        }
        String cheatedString = activity.getString(
            R.string.num_hinted_boxes, cheatedBoxes, cheatLevel
        );

        String source = puz.getSource();
        if (source == null)
            source = puz.getTitle();
        if (source == null)
            source = "";

        final String shareMessage;
        if (puz.getDate() != null) {
            DateTimeFormatter dateFormat
                = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);

            shareMessage = activity.getResources().getQuantityString(
                R.plurals.share_message_with_date,
                cheatedBoxes,
                source, dateFormat.format(puz.getDate()), cheatedBoxes
            );
        } else {
            shareMessage = activity.getResources().getQuantityString(
                R.plurals.share_message_no_date,
                cheatedBoxes,
                source, cheatedBoxes
            );
        }

        TextView elapsedTime = layout.findViewById(R.id.elapsed);
        elapsedTime.setText(elapsedString);

        TextView totalCluesView = layout.findViewById(R.id.totalClues);
        totalCluesView.setText(String.format(
            Locale.getDefault(), "%d", totalClues)
        );

        TextView totalBoxesView = layout.findViewById(R.id.totalBoxes);
        totalBoxesView.setText(String.format(
            Locale.getDefault(), "%d", totalBoxes
        ));

        TextView cheatedBoxesView = layout.findViewById(R.id.cheatedBoxes);
        cheatedBoxesView.setText(cheatedString);

        // with apologies to the Material guidelines..
        builder.setNegativeButton(R.string.share, new OnClickListener(){
            public void onClick(DialogInterface dialog, int which) {
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                sendIntent.setType("text/plain");
                activity.startActivity(Intent.createChooser(
                    sendIntent, activity.getString(R.string.share_your_time)
                ));
            }
        });

        builder.setPositiveButton(R.string.done, null);

        return builder.create();
    }

    private void addCompletedMsg(Puzzle puz, TextView view) {
        String msg = puz.getCompletionMessage();
        if (msg == null || msg.isEmpty()) {
            view.setVisibility(View.GONE);
        } else {
            view.setText(HtmlCompat.fromHtml(msg, 0));
            view.setVisibility(View.VISIBLE);
        }
    }
}
