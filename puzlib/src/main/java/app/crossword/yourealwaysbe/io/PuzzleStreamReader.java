package app.crossword.yourealwaysbe.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import app.crossword.yourealwaysbe.puz.Puzzle;

public class PuzzleStreamReader implements PuzzleParser {
    private static final Logger LOGGER
        = Logger.getLogger(PuzzleStreamReader.class.getCanonicalName());

    private static final PuzzleParser[] PARSERS = {
        new IO(),
        new JPZIO(),
        new IPuzIO(),
        new UclickXMLIO(),
        new BrainsOnlyIO(),
        new KingFeaturesPlaintextIO(),
        new GuardianJSONIO(),
        new AmuseLabsJSONIO(),
        new RaetselZentraleSchwedenJSONIO(),
        new RCIJeuxMFJIO(),
        new PrzekrojIO()
    };

    /**
     * Read the puzzle from the input stream, try multiple formats
     *
     * The method will try known file formats until it finds one that
     * parses to completion.
     */
    @Override
    public Puzzle parseInput(InputStream is) {
        return parseInputStatic(is);
    }

    public static Puzzle parseInputStatic(InputStream is) {
        try {
            ByteArrayInputStream unzipped = StreamUtils.unzipOrPassThrough(is);
            for (PuzzleParser parser : PARSERS) {
                try {
                    unzipped.reset();
                    Puzzle puz = parser.parseInput(unzipped);
                    if (puz != null)
                        return puz;
                } catch (Exception e) {
                    LOGGER.info("Parse attempt failed with " + e);
                    // on to the next one
                }
            }
        } catch (IOException e) {
            LOGGER.info("Could not read input stream with " + e);
        }
        return null;
    }
}
