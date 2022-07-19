package app.crossword.yourealwaysbe.io.versions;

import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;
import app.crossword.yourealwaysbe.util.PuzzleUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

// Adds history list in format
// int length (int clue boolean across)*
public class IOVersion5 extends IOVersion4 {
    private static final Logger LOG
        = Logger.getLogger(IOVersion5.class.getCanonicalName());

    @Override
    protected void applyMeta(Puzzle puz, PuzzleMeta meta){
        super.applyMeta(puz, meta);

        List<ClueID> cidHistory = new ArrayList<>(meta.historyList.size());
        for (PuzzleMeta.ListNum ln : meta.historyList) {
            String listName = ln.getListName();
            String number = String.valueOf(ln.getNumber());
            Clue clue = puz.getClues(listName).getClueByNumber(number);
            if (clue != null)
                cidHistory.add(clue.getClueID());
        }
        puz.setHistory(cidHistory);
    }

    @Override
    public PuzzleMeta readMeta(DataInputStream dis) throws IOException {
        PuzzleMeta meta = super.readMeta(dis);

        meta.historyList = new LinkedList<PuzzleMeta.ListNum>();

        int length = dis.readInt();
        for (int i = 0; i < length; i++) {
            int number = dis.readInt();
            boolean across = dis.readBoolean();
            String listName = across ? IO.ACROSS_LIST : IO.DOWN_LIST;

            meta.historyList.add(new PuzzleMeta.ListNum(listName, number));
        }

        return meta;
    }

    @Override
    protected void writeMeta(Puzzle puz, DataOutputStream dos)
              throws IOException {
        super.writeMeta(puz, dos);

        List<ClueID> history = puz.getHistory();
        String acrossListName = PuzzleUtils.getAcrossListName(puz);
        String downListName = PuzzleUtils.getDownListName(puz);

        dos.writeInt(history.size());
        for (ClueID cid : puz.getHistory()) {
            Clue clue = puz.getClue(cid);
            // relies on puz files only have numeric numbers
            int number = Integer.valueOf(clue.getClueNumber());
            String listName = cid.getListName();
            if (Objects.equals(acrossListName, listName)) {
                dos.writeInt(number);
                dos.writeBoolean(true);
            } else if (Objects.equals(downListName, listName)) {
                dos.writeInt(number);
                dos.writeBoolean(false);
            } else {
                // ignore
            }
        }
    }
}
