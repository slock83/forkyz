
package app.crossword.yourealwaysbe.io;

import org.junit.jupiter.api.Test;

public class PuzzleStreamReaderTest {

    @Test
    public void testAcrossLite() {
        IOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInputStatic(
                IOTest.getTestPuzzle1InputStream()
            )
        );
    }

    @Test
    public void testJPZ() {
        JPZIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInputStatic(
                JPZIOTest.getTestPuzzle1InputStream()
            )
        );
    }

    @Test
    public void testBrainsOnly() {
        BrainsOnlyIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInputStatic(
                BrainsOnlyIOTest.getTestPuzzle1InputStream()
            )
        );
    }

    @Test
    public void testKingFeaturesPlaintext() {
        KingFeaturesPlaintextIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInputStatic(
                KingFeaturesPlaintextIOTest.getTestPuzzle1InputStream()
            )
        );
    }

    @Test
    public void testUclick() {
        UclickXMLIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInputStatic(
                UclickXMLIOTest.getTestPuzzle1InputStream()
            )
        );
    }

    @Test
    public void testIPuz() throws Exception {
        IPuzIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInputStatic(
                IPuzIOTest.getTestPuzzle1InputStream()
            )
        );
    }

    @Test
    public void testRaetzelZentraleSchweden() throws Exception {
        RaetselZentraleSchwedenJSONIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInputStatic(
                RaetselZentraleSchwedenJSONIOTest.getTestPuzzle1InputStream()
            )
        );
    }

    @Test
    public void testRCIJeuxMFJ() throws Exception {
        RCIJeuxMFJIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInputStatic(
                RCIJeuxMFJIOTest.getTestPuzzle1InputStream()
            )
        );
    }

    @Test
    public void testPrzekroj() throws Exception {
        PrzekrojIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInputStatic(
                PrzekrojIOTest.getTestPuzzle1InputStream()
            )
        );
    }

    @Test
    public void testKeesingXML() throws Exception {
        KeesingXMLIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInputStatic(
                KeesingXMLIOTest.getTestPuzzle1InputStream()
            )
        );
    }
}

