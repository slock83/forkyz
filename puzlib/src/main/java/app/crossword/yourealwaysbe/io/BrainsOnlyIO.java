package app.crossword.yourealwaysbe.io;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleBuilder;

import static app.crossword.yourealwaysbe.util.HtmlUtil.htmlString;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: keber_000
 * Date: 2/9/14
 * Time: 5:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class BrainsOnlyIO implements PuzzleParser {

    private static final Charset CHARSET = Charset.forName("ISO-8859-1");
    private static final String ACROSS_LIST = "Across";
    private static final String DOWN_LIST = "Down";

    public static boolean convertBrainsOnly(InputStream is, DataOutputStream os, LocalDate date){
        try {
            Puzzle puz = parse(is);
            puz.setDate(date);
            IO.saveNative(puz, os);
        } catch (IOException e) {
            System.err.println("Unable to dump puzzle to output stream.");
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public Puzzle parseInput(InputStream is) throws IOException {
        return parse(is);
    }

    public static Puzzle parse(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(is, CHARSET)
        );
        String title = readLineAtOffset(reader, 4);
        int startIndex = title.indexOf(" ") + 1;
        String puzTitle = htmlString(
            title.substring(startIndex >= 0 ? startIndex : 0)
        );
        String author = htmlString(readLineAtOffset(reader, 1));

        int width = Integer.parseInt(readLineAtOffset(reader, 1));
        int height = Integer.parseInt(readLineAtOffset(reader, 1));
        if (width == 0 || height == 0) {
            throw new IOException("Invalid puzzle contents");
        }
        readLineAtOffset(reader, 4);
        Box[][] boxes = new Box[height][width];
        for(int down = 0; down < height; down++){
            String line = readLineAtOffset(reader, 0);
            if (line.length() != width)
                throw new IOException(
                    String.format(
                        "Unexpected line length for width %d grid: %s",
                        width,
                        line
                    )
                );

            for(int across = 0; across < width; across++){
                char c = line.charAt(across);
                if(c == '#'){
                    continue;
                }
                Box b = new Box();
                b.setSolution(c);
                boxes[down][across] = b;
            }
        }

        PuzzleBuilder builder = new PuzzleBuilder(boxes);
        builder.autoNumberBoxes()
            .setTitle(puzTitle)
            .setAuthor(author);

        readLineAtOffset(reader, 0);
        ArrayList<String> acrossClues = new ArrayList<String>();
        for(String clue = readLineAtOffset(reader, 0); !"".equals(clue); clue = readLineAtOffset(reader, 0)){
            acrossClues.add(clue);
        }

        ArrayList<String> downClues = new ArrayList<String>();
        for(String clue = readLineAtOffset(reader, 0); !"".equals(clue); clue = readLineAtOffset(reader, 0)){
            downClues.add(clue);
        }

        int acrossIdx = 0;
        for(int h = 0; h < height; h++){
            for(int w = 0; w < width; w++){
                Box box = builder.getBox(h, w);
                if (
                    box != null
                    && box.hasClueNumber()
                    && builder.isStartClue(h, w, true)
                ){
                    builder.addAcrossClue(
                        ACROSS_LIST,
                        box.getClueNumber(),
                        htmlString(acrossClues.get(acrossIdx))
                    );
                    acrossIdx += 1;
                }
            }
        }

        int downIdx = 0;
        for(int h = 0; h < boxes.length; h++){
            for(int w = 0; w < width; w++){
                Box box = builder.getBox(h, w);
                if(
                    box != null
                    && box.hasClueNumber()
                    && builder.isStartClue(h, w, false)
                ){
                    builder.addDownClue(
                        DOWN_LIST,
                        box.getClueNumber(),
                        htmlString(downClues.get(downIdx))
                    );
                    downIdx += 1;
                }
            }
        }

        builder.setNotes("");
        return builder.getPuzzle();
    }

    private static String readLineAtOffset(BufferedReader reader, int offset) throws IOException {
        String read = null;
        for(int i=0; i <= offset; i++){
            read = reader.readLine();
            if(read == null){
                throw new EOFException("Offset past end of file");
            }
            if(read.endsWith("\r")){
                i++;
            }
        }
        if(read == null){
            throw new EOFException("End of line");
        }
        return read.trim();
    }
}
