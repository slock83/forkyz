package app.crossword.yourealwaysbe.io.versions;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;
import app.crossword.yourealwaysbe.util.PuzzleUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Logger;

// Saves the current board position and clue orientation.
public class IOVersion3 extends IOVersion2 {
    private static final Logger LOG = Logger.getLogger(IOVersion3.class.getCanonicalName());

    @Override
    protected void applyMeta(Puzzle puz, PuzzleMeta meta){
        super.applyMeta(puz, meta);
        puz.setPosition(meta.position);
        Box box = puz.checkedGetBox(meta.position);
        if (box != null) {
            String desiredList = PuzzleUtils.getAcrossListName(puz);
            ClueID curCid = box.getIsPartOfClue(desiredList);
            puz.setCurrentClueID(curCid);
        }
    }

    @Override
    public PuzzleMeta readMeta(DataInputStream dis) throws IOException {
        PuzzleMeta meta = super.readMeta(dis);
        int x = dis.readInt();
        int y = dis.readInt();
        meta.position = new Position(y, x);
        meta.across = dis.read() == 1;
        return meta;
    }

    @Override
    protected void writeMeta(Puzzle puz, DataOutputStream dos)
              throws IOException {
        super.writeMeta(puz, dos);
        Position p = puz.getPosition();
        if (p != null) {
            dos.writeInt(p.getCol());
            dos.writeInt(p.getRow());
        } else {
            dos.writeInt(0);
            dos.writeInt(0);
        }
        ClueID curCid = puz.getCurrentClueID();
        String acrossList = PuzzleUtils.getAcrossListName(puz);
        if (curCid == null || Objects.equals(acrossList, curCid.getListName()))
            dos.write(1);
        else
            dos.write(-1);
    }
}
