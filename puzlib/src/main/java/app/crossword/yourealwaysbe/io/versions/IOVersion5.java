package app.crossword.yourealwaysbe.io.versions;

import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

// Adds history list in format
// int length (int clue boolean across)*
public class IOVersion5 extends IOVersion4 {
    private static final Logger LOG = Logger.getLogger(IOVersion5.class.getCanonicalName());

    @Override
    protected void applyMeta(Puzzle puz, PuzzleMeta meta){
        super.applyMeta(puz, meta);
        puz.setHistory(meta.historyList);
    }

    @Override
    public PuzzleMeta readMeta(DataInputStream dis) throws IOException {
        PuzzleMeta meta = super.readMeta(dis);

        meta.historyList = new LinkedList<ClueID>();

        int length = dis.readInt();
        for (int i = 0; i < length; i++) {
            int number = dis.readInt();
            boolean across = dis.readBoolean();
            String listName = across ? ClueID.ACROSS : ClueID.DOWN;

            ClueID item = new ClueID(String.valueOf(number), listName);
            meta.historyList.add(item);
        }

        return meta;
    }

    @Override
    protected void writeMeta(Puzzle puz, DataOutputStream dos)
              throws IOException {
        super.writeMeta(puz, dos);

        List<ClueID> history = puz.getHistory();

        dos.writeInt(history.size());
        for (ClueID item : puz.getHistory()) {
            // relies on puz files only have numeric numbers
            int number = Integer.valueOf(item.getClueNumber());
            String listName = item.getListName();
            if (ClueID.ACROSS.equals(listName)) {
                dos.writeInt(number);
                dos.writeBoolean(true);
            } else if (ClueID.DOWN.equals(listName)) {
                dos.writeInt(number);
                dos.writeBoolean(false);
            } else {
                // ignore
            }
        }
    }
}
