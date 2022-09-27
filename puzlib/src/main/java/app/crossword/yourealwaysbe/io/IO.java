package app.crossword.yourealwaysbe.io;

import app.crossword.yourealwaysbe.io.versions.IOVersion1;
import app.crossword.yourealwaysbe.io.versions.IOVersion2;
import app.crossword.yourealwaysbe.io.versions.IOVersion3;
import app.crossword.yourealwaysbe.io.versions.IOVersion4;
import app.crossword.yourealwaysbe.io.versions.IOVersion5;
import app.crossword.yourealwaysbe.io.versions.IOVersion6;
import app.crossword.yourealwaysbe.io.versions.IOVersion7;
import app.crossword.yourealwaysbe.io.versions.IOVersion8;
import app.crossword.yourealwaysbe.io.versions.IOVersion9;
import app.crossword.yourealwaysbe.io.versions.IOVersion;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.ClueList;
import app.crossword.yourealwaysbe.puz.Note;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleBuilder;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;
import app.crossword.yourealwaysbe.util.PuzzleUtils;

import static app.crossword.yourealwaysbe.util.HtmlUtil.htmlString;
import static app.crossword.yourealwaysbe.util.HtmlUtil.unHtmlString;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class IO implements PuzzleParser {
    public static final String FILE_MAGIC = "ACROSS&DOWN";
    public static final String VERSION_STRING = "2.0";
    private static final Charset CHARSET = Charset.forName("UTF-8");
    private static final Charset CHARSET_OLD = Charset.forName("ISO-8859-1");

    // Extra Section IDs and markers
    private static final String GEXT_MARKER = "GEXT";
    private static final int GEXT = 0;
    private static final String REBUS_TABLE_MARKER = "RTBL";
    private static final int RTBL = 3;
    private static final String REBUS_BOARD_MARKER = "GRBS";
    private static final int GRBS = 4;
    private static final String REBUS_USER_MARKER = "RUSR";
    private static final int RUSR = 5;

    // LEGACY: used only for reading IOVersion5 and below that dubiously
    // stored additional clue notes in the Across Lite file
    private static final String ANTS_MARKER = "ANTS";
    private static final String DNTS_MARKER = "DNTS";
    private static final int ANTS = 1;
    private static final int DNTS = 2;

    // GEXT section bitmasks
    private static final byte GEXT_SQUARE_CIRCLED = (byte) 0x80;

    // NOTES CODES
    private static final byte NOTE_SCRATCH = (byte) 0x01;
    private static final byte NOTE_TEXT = (byte) 0x02;
    private static final byte NOTE_ANAGRAM_SRC = (byte) 0x03;
    private static final byte NOTE_ANAGRAM_SOL = (byte) 0x04;

    // string to use if a clue is missing
    private static final String UNKNOWN_CLUE = "-";

    public static final String ACROSS_LIST = "Across";
    public static final String DOWN_LIST = "Down";

    public static int cksum_region(byte[] data, int offset, int length,
                                   int cksum) {
        for (int i = offset; i < (offset + length); i++) {
            if ((cksum & 0x1) != 0) {
                cksum = (cksum >> 1) + 0x8000;
            } else {
                cksum = cksum >> 1;
            }

            cksum += (0xFF & data[i]);
            cksum = cksum & 0xFFFF;
        }

        return cksum;
    }

    public static Puzzle load(DataInputStream puzzleInput,
                              DataInputStream metaInput) throws IOException {
        Puzzle puz = IO.loadNative(puzzleInput);

        if (puz != null)
            IO.readCustom(puz, metaInput);

        return puz;
    }

    @Override
    public Puzzle parseInput(InputStream is) throws IOException {
        return loadNative(new DataInputStream(is));
    }

    public static Puzzle loadNative(InputStream input) throws IOException {
        return loadNative(new DataInputStream(input));
    }

    public static Puzzle loadNative(DataInputStream input) throws IOException {
        input.skipBytes(0x2);

        byte[] fileMagic = new byte[FILE_MAGIC.length()];

        input.read(fileMagic, 0, fileMagic.length);

        // check that this is a puz file
        if (!FILE_MAGIC.equals(new String(fileMagic, CHARSET)))
            return  null;

        // done in two steps to match saveNative method
        input.skip(1);
        input.skipBytes(0xA);

        byte[] versionBytes= new byte[3];

        for (int i = 0; i < versionBytes.length; i++) {
            versionBytes[i] = input.readByte();
        }

        String version = new String(versionBytes, CHARSET.name());

        Charset charset = ("2.0".compareTo(version) > 0)
            ? CHARSET_OLD
            : CHARSET;

        input.skip(1);

        input.skipBytes(2);
        short solutionChecksum = Short.reverseBytes(input.readShort());

        input.skipBytes(0xC);

        int width = 0xFFFF & input.readByte();
        int height = 0xFFFF & input.readByte();

        // read/skip number of clues
        input.readShort();

        input.skipBytes(2);
        boolean scrambled = input.readShort() != 0;

        Box[][] boxes = new Box[height][width];
        byte[] answerByte = new byte[1];

        for (int x = 0; x < boxes.length; x++) {
            for (int y = 0; y < boxes[x].length; y++) {
                answerByte[0] = input.readByte();

                char solution = new String(answerByte, charset.name())
                        .charAt(0);

                if (solution != '.') {
                    boxes[x][y] = new Box();
                    boxes[x][y].setSolution((char) solution);
                }
            }
        }

        for (int x = 0; x < boxes.length; x++) {
            for (int y = 0; y < boxes[x].length; y++) {
                answerByte[0] = input.readByte();

                char answer = new String(answerByte, charset.name()).charAt(0);

                if (answer == '.') {
                    continue;
                } else if (answer == '-') {
                    boxes[x][y].setBlank();
                } else if (boxes[x][y] != null) {
                    boxes[x][y].setResponse(answer);
                } else {
                    System.out.println("Unexpected answer: " + x + "," + y
                            + " " + answer);
                }
            }
        }

        PuzzleBuilder builder = new PuzzleBuilder(boxes);
        builder.autoNumberBoxes();

        builder.setSolutionChecksum(solutionChecksum);
        builder.setScrambled(scrambled);
        builder.setTitle(readNullTerminatedString(input, charset));
        builder.setAuthor(readNullTerminatedString(input, charset));
        builder.setCopyright(readNullTerminatedString(input, charset));

        for (int x = 0; x < builder.getHeight(); x++) {
            for (int y = 0; y < builder.getWidth(); y++) {
                Box box = builder.getBox(x, y);

                if (box == null) {
                    continue;
                }

                String clueNumber = box.getClueNumber();

                if (builder.isStartClue(x, y, true) && (clueNumber != null)) {
                    String value = readNullTerminatedString(input, charset);
                    builder.addAcrossClue(
                        ACROSS_LIST, clueNumber, value
                    );
                }

                if (builder.isStartClue(x, y, false) && (clueNumber != null)) {
                    String value = readNullTerminatedString(input, charset);
                    builder.addDownClue(
                        DOWN_LIST, clueNumber, value
                    );
                }
            }
        }

        builder.setNotes(htmlString(readNullTerminatedString(input, charset)));

        boolean eof = false;

        byte[][] rebusBoard = null;
        String[] solRebusTable = null;
        String[] usrRebusTable = null;

        while (!eof) {
            try {
                switch (readExtraSectionType(input)) {
                    case GEXT:
                        readGextSection(input, builder);
                        break;

                    // For reading legacy files only
                    // info now stored in meta
                    case ANTS:
                        loadNotesNative(true, builder, input, charset);
                        break;

                    case DNTS:
                        loadNotesNative(false, builder, input, charset);
                        break;

                    case GRBS:
                        rebusBoard = readRebusBoard(input, builder);
                        break;

                    case RTBL:
                        solRebusTable = readRebusTable(input, builder, charset);
                        break;

                    case RUSR:
                        usrRebusTable = readRebusTable(input, builder, charset);
                        break;

                    default:
                        skipExtraSection(input);
                }
            } catch (EOFException e) {
                eof = true;
            }
        }

        if (rebusBoard != null) {
            applyRebus(rebusBoard, solRebusTable, usrRebusTable, builder);
        }

        return builder.getPuzzle();
    }

    public static void readCustom(Puzzle puz, DataInputStream is)
            throws IOException {
        int version = is.read();
        IOVersion v = getIOVersion(version);
        v.read(puz, is);
    }

    public static int readExtraSectionType(DataInputStream input)
            throws IOException {
        byte[] title = new byte[4];

        for (int i = 0; i < title.length; i++) {
            title[i] = input.readByte();
        }

        String section = new String(title);

        if (GEXT_MARKER.equals(section)) {
            return GEXT;
        } else if (ANTS_MARKER.equals(section)) {
            return ANTS;
        } else if (DNTS_MARKER.equals(section)) {
            return DNTS;
        } else if (REBUS_BOARD_MARKER.equals(section)) {
            return GRBS;
        } else if (REBUS_TABLE_MARKER.equals(section)) {
            return RTBL;
        } else if (REBUS_USER_MARKER.equals(section)) {
            return RUSR;
        }

        return -1;
    }

    public static void readGextSection(
        DataInputStream input, PuzzleBuilder builder
    ) throws IOException {
        input.skipBytes(4);

        for (int x = 0; x < builder.getHeight(); x++) {
            for (int y = 0; y < builder.getWidth(); y++) {
                byte gextInfo = input.readByte();

                if ((gextInfo & GEXT_SQUARE_CIRCLED) != 0) {
                    Box box = builder.getBox(x, y);
                    if (box != null)
                        box.setCircled(true);
                }
            }
        }

        input.skipBytes(1);
    }

    public static PuzzleMeta readMeta(InputStream is) throws IOException {
        return readMeta(new DataInputStream(is));
    }

    public static PuzzleMeta readMeta(DataInputStream is) throws IOException {
        int version = is.read();
        IOVersion v = getIOVersion(version);
        PuzzleMeta m = v.readMeta(is);
        return m;
    }

    public static String readNullTerminatedString(
        InputStream is, Charset charset
    ) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(128);

        for (byte nextByte = (byte) is.read(); nextByte != 0x0; nextByte = (byte) is
                .read()) {
            if (nextByte != 0x0) {
                baos.write(nextByte);
            }

            if (baos.size() > 4096) {
                throw new IOException("Run on string!");
            }
        }

        return (baos.size() == 0)
            ? null
            : new String(baos.toByteArray(), charset.name());
    }

    public static void save(
        Puzzle puz,
        OutputStream puzzleOutputStream,
        OutputStream metaOutputStream
    ) throws IOException {
        save(
            puz,
            new DataOutputStream(puzzleOutputStream),
            new DataOutputStream(metaOutputStream)
        );
    }

    public static void save(
        Puzzle puz,
        DataOutputStream puzzleOutputStream,
        DataOutputStream metaOutputStream
    ) throws IOException {
        IO.saveNative(puz, puzzleOutputStream);
        puzzleOutputStream.close();
        IO.writeCustom(puz, metaOutputStream);
        metaOutputStream.close();
    }

    public static void saveNative(Puzzle puz, OutputStream dos)
            throws IOException {
        saveNative(puz, new DataOutputStream(dos));
    }

    public static void saveNative(Puzzle puz, DataOutputStream dos)
            throws IOException {
        /*
         * We write the puzzle to a temporary output stream, with 0 entered for
         * any checksums. Once we have this written out, we can calculate all of
         * the checksums and write the file to the original output stream.
         */
        ByteArrayOutputStream tmp = new ByteArrayOutputStream();
        DataOutputStream tmpDos = new DataOutputStream(tmp);

        tmpDos.writeShort(0);

        tmpDos.writeBytes(FILE_MAGIC);
        tmpDos.writeByte(0);

        tmpDos.write(new byte[10]);

        tmpDos.writeBytes(VERSION_STRING);
        tmpDos.writeByte(0);

        tmpDos.write(new byte[2]);

        tmpDos.writeShort(Short.reverseBytes(puz.getSolutionChecksum()));

        tmpDos.write(new byte[0xC]);

        int width = puz.getWidth();
        int height = puz.getHeight();
        int numberOfBoxes = width * height;

        tmpDos.writeByte(width);
        tmpDos.writeByte(height);

        int numberOfClues = getNumberOfClues(puz);

        tmpDos.writeShort(Short.reverseBytes((short) numberOfClues));
        tmpDos.writeShort(Short.reverseBytes((short) 1));

        short scrambled = puz.isScrambled() ? (short) 4 : (short) 0;
        tmpDos.writeShort(Short.reverseBytes(scrambled));

        Box[][] boxes = puz.getBoxes();

        for (int x = 0; x < boxes.length; x++) {
            for (int y = 0; y < boxes[x].length; y++) {
                if (boxes[x][y] == null) {
                    tmpDos.writeByte('.');
                } else {
                    String sSol = boxes[x][y].getSolution();
                    byte val = (sSol == null || sSol.isEmpty())
                        ? 0
                        : (byte) sSol.charAt(0);
                    tmpDos.writeByte(val);
                }
            }
        }

        for (int x = 0; x < boxes.length; x++) {
            for (int y = 0; y < boxes[x].length; y++) {
                if (boxes[x][y] == null) {
                    tmpDos.writeByte('.');
                } else {
                    String sRes = boxes[x][y].getResponse();
                    byte val = (sRes == null || sRes.isEmpty())
                        ? 0
                        : (byte) sRes.charAt(0);

                    tmpDos.writeByte((boxes[x][y].isBlank()) ? '-' : val);
                }
            }
        }

        writeNullTerminatedString(
            tmpDos, unHtmlString(puz.getTitle()), CHARSET
        );
        writeNullTerminatedString(
            tmpDos, unHtmlString(puz.getAuthor()), CHARSET
        );
        writeNullTerminatedString(
            tmpDos, unHtmlString(puz.getCopyright()), CHARSET
        );

        for (String clue : getRawClues(puz)) {
            writeNullTerminatedString(tmpDos, unHtmlString(clue), CHARSET);
        }

        writeNullTerminatedString(tmpDos, unHtmlString(puz.getNotes()), CHARSET);

        writeGEXT(tmpDos, puz);
        writeRebus(tmpDos, puz, CHARSET);

        byte[] puzByteArray = tmp.toByteArray();
        ByteBuffer bb = ByteBuffer.wrap(puzByteArray);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        // Calculate checksums and write to byte array.
        int c_cib = cksum_cib(puzByteArray, 0);
        bb.putShort(0x0E, (short) c_cib);

        int c_primary = cksum_primary_board(puzByteArray, numberOfBoxes,
                numberOfClues, c_cib);
        bb.putShort(0, (short) c_primary);

        int c_sol = cksum_solution(puzByteArray, numberOfBoxes, 0);
        int c_grid = cksum_grid(puzByteArray, numberOfBoxes, 0);
        int c_part = cksum_partial_board(puzByteArray, numberOfBoxes,
                numberOfClues, 0);

        bb.position(0x10);
        bb.put((byte) (0x49 ^ (c_cib & 0xFF)));
        bb.put((byte) (0x43 ^ (c_sol & 0xFF)));
        bb.put((byte) (0x48 ^ (c_grid & 0xFF)));
        bb.put((byte) (0x45 ^ (c_part & 0xFF)));
        bb.put((byte) (0x41 ^ ((c_cib & 0xFF00) >> 8)));
        bb.put((byte) (0x54 ^ ((c_sol & 0xFF00) >> 8)));
        bb.put((byte) (0x45 ^ ((c_grid & 0xFF00) >> 8)));
        bb.put((byte) (0x44 ^ ((c_part & 0xFF00) >> 8)));

        // Dump byte array to output stream.
        dos.write(puzByteArray);
    }

    public static void skipExtraSection(DataInputStream input)
            throws IOException {
        short numBytes = Short.reverseBytes(input.readShort());
        input.skipBytes(2); // checksum
        input.skipBytes(numBytes); // data
        input.skipBytes(1); // null terminator
    }

    /**
     * Attempts to unscramble the solution using the input key. Modifications to
     * the solution array occur in place. If true, the unscrambled solution
     * checksum is valid.
     */
    public static boolean tryUnscramble(Puzzle p, int key_int, byte[] solution) {
        p.unscrambleKey[0] = (key_int / 1000) % 10;
        p.unscrambleKey[1] = (key_int / 100) % 10;
        p.unscrambleKey[2] = (key_int / 10) % 10;
        p.unscrambleKey[3] = (key_int / 1) % 10;

        for (int i = 3; i >= 0; i--) {
            unscrambleString(p, solution);
            System.arraycopy(p.unscrambleBuf, 0, solution, 0,
                    p.unscrambleBuf.length);
            unshiftString(p, solution, p.unscrambleKey[i]);

            for (int j = 0; j < solution.length; j++) {
                int letter = (solution[j] & 0xFF) - p.unscrambleKey[j % 4];

                if (letter < 65) {
                    letter += 26;
                }

                solution[j] = (byte) letter;
            }
        }

        if (p.solutionChecksum == (short) IO.cksum_region(solution, 0,
                solution.length, 0)) {
            int s = 0;
            for (int i = 0; i < p.getBoxesList().length; i++) {
                Box b = p.getBoxesList()[i];
                if (b != null) {
                    b.setSolution((char) solution[s++]);
                }
            }
            return true;
        }
        return false;
    }

    public static void writeCustom(Puzzle puz, DataOutputStream os)
            throws IOException {
        os.write(9);
        IOVersion v = new IOVersion9();
        v.write(puz, os);
    }

    public static boolean crack(Puzzle puz) {
        for (int a = 0; a < 10000; a++) {
            if (tryUnscramble(puz, a, puz.initializeUnscrambleData())) {
                return true;
            }
        }
        return false;
    }

    public static void writeNullTerminatedString(
        OutputStream os, String value, Charset charset
    ) throws IOException {
        value = (value == null) ? "" : value;

        ByteBuffer encoded = charset.encode(value);

        os.write(encoded.array(), encoded.position(), encoded.limit());
        os.write(0);
    }

    public static void unscrambleString(Puzzle p, byte[] str) {
        int oddIndex = 0;
        int evenIndex = str.length / 2;

        for (int i = 0; i < str.length; i++) {
            if ((i % 2) == 0) {
                p.unscrambleBuf[evenIndex++] = str[i];
            } else {
                p.unscrambleBuf[oddIndex++] = str[i];
            }
        }
    }

    public static void unshiftString(Puzzle p, byte[] str, int keynum) {
        System.arraycopy(str, str.length - keynum, p.unscrambleTmp, 0, keynum);
        System.arraycopy(str, 0, str, keynum, str.length - keynum);
        System.arraycopy(p.unscrambleTmp, 0, str, 0, keynum);
    }

    private static int cksum_cib(byte[] puzByteArray, int cksum) {
        return cksum_region(puzByteArray, 0x2C, 8, cksum);
    }

    private static int cksum_grid(byte[] puzByteArray, int numberOfBoxes,
                                  int cksum) {
        return cksum_region(puzByteArray, 0x34 + numberOfBoxes, numberOfBoxes,
                cksum);
    }

    private static int cksum_partial_board(byte[] puzByteArray,
                                           int numberOfBoxes, int numberOfClues, int cksum) {
        int offset = 0x34 + (2 * numberOfBoxes);

        for (int i = 0; i < (4 + numberOfClues); i++) {
            int startOffset = offset;

            while (puzByteArray[offset] != 0) {
                offset++;
            }

            int length = offset - startOffset;

            if ((i > 2) && (i < (3 + numberOfClues))) {
                cksum = cksum_region(puzByteArray, startOffset, length, cksum);
            } else if (length > 0) {
                cksum = cksum_region(puzByteArray, startOffset, length + 1,
                        cksum);
            }

            offset++;
        }

        return cksum;
    }

    private static int cksum_primary_board(byte[] puzByteArray,
                                           int numberOfBoxes, int numberOfClues, int cksum) {
        cksum = cksum_solution(puzByteArray, numberOfBoxes, cksum);
        cksum = cksum_grid(puzByteArray, numberOfBoxes, cksum);
        cksum = cksum_partial_board(puzByteArray, numberOfBoxes, numberOfClues,
                cksum);

        return cksum;
    }

    private static int cksum_solution(byte[] puzByteArray, int numberOfBoxes,
                                      int cksum) {
        return cksum_region(puzByteArray, 0x34, numberOfBoxes, cksum);
    }

    /**
     * For reading IOVersion5 and below that dubiously stored clue notes by
     * possibly abuse of the Across Lite format.
     *
     * Boxes must be set on puz first.
     *
     * Format of a note:
     *     + 1st byte is number of fields in note
     *     + Each field is
     *         + byte identifying field
     *         + null terminated string which is field value
     */
    private static void loadNotesNative(
        boolean isAcross,
        PuzzleBuilder builder,
        DataInputStream input,
        Charset charset
    ) throws IOException {

        for (ClueID cid : builder.getBoardClueIDs()) {
            String listName = cid.getListName();

            // assume puz files only deal with across/down list
            String desiredList = isAcross ? ACROSS_LIST : DOWN_LIST;
            if (desiredList.equals(listName)) {
                String scratch = null;
                String text = null;
                String anagramSrc = null;
                String anagramSol = null;

                byte numFields = input.readByte();
                for (byte i = 0; i < numFields; i++) {
                    byte field = input.readByte();

                    String val = readNullTerminatedString(input, charset);

                    switch (field) {
                    case NOTE_SCRATCH:
                        scratch = val;
                        break;
                    case NOTE_TEXT:
                        text = val;
                        break;
                    case NOTE_ANAGRAM_SRC:
                        anagramSrc = val;
                        break;
                    case NOTE_ANAGRAM_SOL:
                        anagramSol = val;
                        break;
                    }
                }

                if (scratch != null
                        || text != null
                        || anagramSrc != null
                        || anagramSol != null) {
                    Note n = new Note(
                        scratch, text, anagramSrc, anagramSol
                    );
                    builder.setNote(cid, n);
                }
            }
        }
    }

    private static IOVersion getIOVersion(int version) throws IOException {
        switch (version) {
        case 1:
            return new IOVersion1();
        case 2:
            return new IOVersion2();
        case 3:
            return new IOVersion3();
        case 4:
            return new IOVersion4();
        case 5:
            return new IOVersion5();
        case 6:
            return new IOVersion6();
        case 7:
            return new IOVersion7();
        case 8:
            return new IOVersion8();
        case 9:
            return new IOVersion9();
        default:
            throw new IOException("UnknownVersion " + version);
        }
    }

    /**
     * Return list of clues in puz format order
     *
     * That is, in numerical order, with across appearing before down
     * when same number
     */
    private static List<String> getRawClues(Puzzle puz) {
        int numClues = getNumberOfClues(puz);

        List<String> rawClues = new ArrayList<>(numClues);

        String acrossList = PuzzleUtils.getAcrossListName(puz);
        String downList = PuzzleUtils.getDownListName(puz);

        ClueList acrossClues = null;
        if (acrossList != null)
            acrossClues = puz.getClues(acrossList);

        ClueList downClues = null;
        if (downList != null)
            downClues = puz.getClues(downList);

        for (ClueID cid : puz.getBoardClueIDs()) {
            String listName = cid.getListName();
            int index = cid.getIndex();

            // only support Across/Down in puz files
            if (Objects.equals(acrossList, listName)) {
                if (acrossClues != null && acrossClues.hasClueByIndex(index))
                    rawClues.add(acrossClues.getClueByIndex(index).getHint());
                else
                    rawClues.add(UNKNOWN_CLUE);
            } else if (Objects.equals(downList, listName)) {
                if (downClues != null && downClues.hasClueByIndex(index))
                    rawClues.add(downClues.getClueByIndex(index).getHint());
                else
                    rawClues.add(UNKNOWN_CLUE);
            }
        }

        return rawClues;
    }

    /**
     * Gets number of Across/Down clues
     */
    private static int getNumberOfClues(Puzzle puz) {
        ClueList acrossClues = PuzzleUtils.getAcrossList(puz);
        ClueList downClues = PuzzleUtils.getDownList(puz);
        int count = 0;

        if (acrossClues != null)
            count += acrossClues.size();

        if (downClues != null)
            count += downClues.size();

        return count;
    }

    /**
     * Read rebus grid from extras
     *
     * Format is flattened grid (row by row) of bytes, with 0 if no
     * rebus at that cell position, and an index into the rebus tables
     * if there is a rebus. The index is +1 (e.g. index 0 is stored as
     * 1).
     *
     * Thanks for puz.py for the rebus encoding reference.
     * https://github.com/alexdej/puzpy
     */
    private static byte[][] readRebusBoard(
        DataInputStream input, PuzzleBuilder builder
    ) throws IOException {
        int height = builder.getHeight();
        int width = builder.getWidth();
        byte[][] rebusBoard = new byte[height][width];

        input.skipBytes(4);

        for (int row = 0; row < height; row++)
            for (int col = 0; col < width; col++)
                rebusBoard[row][col] = input.readByte();

        input.skipBytes(1);

        return rebusBoard;
    }

    /**
     * Read a rebus table from extras
     *
     * Format is a string index:entry;index:entry;... mapping indices to
     * string entry values.
     *
     * Thanks for puz.py for the rebus encoding reference.
     * https://github.com/alexdej/puzpy
     */
    private static String[] readRebusTable(
        DataInputStream input, PuzzleBuilder builder, Charset charset
    ) throws IOException {
        int height = builder.getHeight();
        int width = builder.getWidth();

        String[] table = new String[width * height + 1];

        // 2 bytes len, 2 bytes checksum
        input.skipBytes(4);

        String tableString = readNullTerminatedString(input, charset);

        for (String entry : tableString.split(";")) {
            String[] items = entry.split(":");
            byte index = Byte.valueOf(items[0].trim());
            table[index] = items[1];
        }

        return table;
    }

    private static void applyRebus(
        byte[][] rebusBoard,
        String[] solRebusTable,
        String[] usrRebusTable,
        PuzzleBuilder builder
    ) {
        if (rebusBoard == null)
            return;

        int height = builder.getHeight();
        int width = builder.getWidth();

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                Box box = builder.getBox(row, col);
                int index = rebusBoard[row][col];
                if (index > 0 && box != null) {
                    int truei = (byte) (index - 1);
                    if (solRebusTable != null && solRebusTable[truei] != null)
                        box.setSolution(solRebusTable[truei]);
                    if (usrRebusTable != null && usrRebusTable[truei] != null)
                        box.setResponse(usrRebusTable[truei]);
                }
            }
        }
    }

    private static void writeGEXT(
        DataOutputStream dos, Puzzle puz
    ) throws IOException {
        if (!puz.hasCircled())
            return;

        int width = puz.getWidth();
        int height = puz.getHeight();
        int numberOfBoxes = width * height;
        Box[][] boxes = puz.getBoxes();

        byte[] gextSection = new byte[numberOfBoxes];

        for (int row = 0; row < boxes.length; row++) {
            for (int col = 0; col < boxes[row].length; col++) {
                if (boxes[row][col] != null && boxes[row][col].isCircled()) {
                    gextSection[(width * row) + col] = GEXT_SQUARE_CIRCLED;
                }
            }
        }

        writeExtraSection(dos, GEXT_MARKER, gextSection);
    }

    private static void writeRebus(
        DataOutputStream dos, Puzzle puz, Charset charset
    ) throws IOException {
        if (!hasRebus(puz))
            return;

        int width = puz.getWidth();
        int height = puz.getHeight();
        int numberOfBoxes = width * height;
        Box[][] boxes = puz.getBoxes();

        byte[] rebusBoard = new byte[numberOfBoxes];
        String[] solRebusTable = new String[numberOfBoxes];
        String[] usrRebusTable = new String[numberOfBoxes];

        int rebusIndex = 0;

        for (int row = 0; row < boxes.length; row++) {
            for (int col = 0; col < boxes[row].length; col++) {
                Box box = boxes[row][col];
                if (isRebusBox(box)) {
                    String solution = box.getSolution();
                    String response = box.getResponse();

                    int tableIndex = width * row + col;

                    rebusBoard[tableIndex] = (byte) (rebusIndex + 1);
                    solRebusTable[rebusIndex]
                        = solution == null ? "" : solution;
                    usrRebusTable[rebusIndex]
                        = response == null ? "" : response;

                    rebusIndex += 1;
                }
            }
        }

        writeExtraSection(dos, REBUS_BOARD_MARKER, rebusBoard);

        byte[] encodedSolTable = encodeRebusTable(solRebusTable, charset);
        writeExtraSection(dos, REBUS_TABLE_MARKER, encodedSolTable);

        byte[] encodedUsrTable = encodeRebusTable(usrRebusTable, charset);
        writeExtraSection(dos, REBUS_USER_MARKER, encodedUsrTable);
    }

    private static boolean hasRebus(Puzzle puz) {
        Box[][] boxes = puz.getBoxes();

        for (int row = 0; row < boxes.length; row++) {
            for (int col = 0; col < boxes[row].length; col++) {
                if (isRebusBox(boxes[row][col]))
                    return true;
            }
        }

        return false;
    }

    private static boolean isRebusBox(Box box) {
        if (box == null)
            return false;

        String solution = box.getSolution();
        String response = box.getResponse();

        int solLen = solution == null ? 0 : solution.length();
        int resLen = response == null ? 0 : response.length();

        return solLen > 1 || resLen > 1;
    }

    private static void writeExtraSection(
        DataOutputStream dos, String marker, byte[] data
    ) throws IOException {
        dos.writeBytes(marker);
        dos.writeShort(Short.reverseBytes((short) data.length));
        int chksum = cksum_region(data, 0, data.length, 0);
        dos.writeShort(Short.reverseBytes((short) chksum));
        dos.write(data);
        dos.writeByte(0);
    }

    private static byte[] encodeRebusTable(String[] table, Charset charset) {
        StringBuilder sb = new StringBuilder();
        for (int index = 0; index < table.length; index++) {
            String entry = table[index];
            if (entry != null) {
                // delete special characters
                String cleanEntry = entry.replace(";", "").replace(":", "");
                sb.append(String.valueOf(index));
                sb.append(":");
                sb.append(cleanEntry);
                sb.append(";");
            }
        }

        ByteBuffer encoded = charset.encode(sb.toString());

        byte[] array = new byte[encoded.limit()];
        encoded.get(array, encoded.position(), encoded.limit());

        return array;
    }
}
