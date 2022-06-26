package app.crossword.yourealwaysbe.io.versions;

import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.Note;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;
import app.crossword.yourealwaysbe.util.PuzzleUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Logger;

// Moves clue notes out of the puz file and into meta
// i don't really think they were allowed in the puz file format
public class IOVersion6 extends IOVersion5 {
    private static final Logger LOG = Logger.getLogger(
        IOVersion6.class.getCanonicalName()
    );

    @Override
    protected void applyMeta(Puzzle puz, PuzzleMeta meta){
        super.applyMeta(puz, meta);
        applyNotes(puz, meta.acrossNotes, true);
        applyNotes(puz, meta.downNotes, false);
    }

    @Override
    public PuzzleMeta readMeta(DataInputStream dis) throws IOException {
        PuzzleMeta meta = super.readMeta(dis);
        loadNotes(true, meta, dis);
        loadNotes(false, meta, dis);
        return meta;
    }

    @Override
    protected void writeMeta(Puzzle puz, DataOutputStream dos)
              throws IOException {
        super.writeMeta(puz, dos);
        saveNotes(dos, puz, true);
        saveNotes(dos, puz, false);
    }

    /**
     * First an int, is num notes
     * Format of notes (all null-term strings):
     *     scratch, free-form, anagram src, anagram sol
     */
    public void loadNotes(
        boolean isAcross, PuzzleMeta meta, DataInputStream input
    ) throws IOException {

        int numNotes = input.readInt();

        Note[] notes = new Note[numNotes];

        for (int x = 0; x < numNotes; x++) {
            notes[x] = readNote(input);
        }

        if (isAcross)
            meta.acrossNotes = notes;
        else
            meta.downNotes = notes;
    }

    protected Note readNote(DataInputStream dis) throws IOException {
        String scratch = IO.readNullTerminatedString(dis, getCharset());
        String text = IO.readNullTerminatedString(dis, getCharset());
        String anagramSrc = IO.readNullTerminatedString(dis, getCharset());
        String anagramSol = IO.readNullTerminatedString(dis, getCharset());
        if (scratch != null
                || text != null
                || anagramSrc != null
                || anagramSol != null) {
            return new Note(scratch, text, anagramSrc, anagramSol);
        }
        return null;
    }

    private void saveNotes(
        DataOutputStream dos, Puzzle puz, boolean isAcross
    ) throws IOException {

        String desiredList = isAcross
            ? PuzzleUtils.getAcrossListName(puz)
            : PuzzleUtils.getDownListName(puz);

        int size = 0;
        for (ClueID cid : puz.getBoardClueIDs()) {
            if (Objects.equals(desiredList, cid.getListName()))
                size += 1;
        }

        // not really useful since notes should match the number of clue
        // positions
        dos.writeInt(size);

        for (ClueID cid : puz.getBoardClueIDs()) {
            if (Objects.equals(desiredList, cid.getListName())) {
                Note note = puz.getNote(cid);
                writeNote(note, dos);
            }
        }
    }

    protected void writeNote(Note note, DataOutputStream dos)
            throws IOException {
        String scratch = null;
        String text = null;
        String anagramSrc = null;
        String anagramSol = null;

        if (note != null) {
            scratch = note.getCompressedScratch();
            text = note.getText();
            anagramSrc = note.getCompressedAnagramSource();
            anagramSol = note.getCompressedAnagramSolution();
        }

        IO.writeNullTerminatedString(dos, scratch, getCharset());
        IO.writeNullTerminatedString(dos, text, getCharset());
        IO.writeNullTerminatedString(dos, anagramSrc, getCharset());
        IO.writeNullTerminatedString(dos, anagramSol, getCharset());
    }

    private void applyNotes(Puzzle puz, Note[] notes, boolean isAcross) {
        if (notes != null) {
            String desiredList = isAcross
                ? PuzzleUtils.getAcrossListName(puz)
                : PuzzleUtils.getDownListName(puz);

            int idx = 0;
            for (ClueID cid : puz.getBoardClueIDs()) {
                if (Objects.equals(desiredList, cid.getListName())) {
                    if (idx < notes.length) {
                        Note n = notes[idx];
                        if (n != null)
                            puz.setNote(cid, n);
                        idx += 1;
                    } else {
                        LOG.info(
                            "WARNING: mismatch between number of "
                                + "clues and number of notes."
                        );
                    }
                }
            }
        }
    }
}
