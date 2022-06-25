
package app.crossword.yourealwaysbe.io;

import junit.framework.TestCase;

public class PuzzleStreamReaderTest extends TestCase {
    public void testAcrossLite() {
        IOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInputStatic(
                IOTest.getTestPuzzle1InputStream()
            )
        );
    }

    public void testJPZ() {
        JPZIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInputStatic(
                JPZIOTest.getTestPuzzle1InputStream()
            )
        );
    }

    public void testBrainsOnly() {
        BrainsOnlyIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInputStatic(
                BrainsOnlyIOTest.getTestPuzzle1InputStream()
            )
        );
    }

    public void testKingFeaturesPlaintext() {
        KingFeaturesPlaintextIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInputStatic(
                KingFeaturesPlaintextIOTest.getTestPuzzle1InputStream()
            )
        );
    }

    public void testUclick() {
        UclickXMLIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInputStatic(
                UclickXMLIOTest.getTestPuzzle1InputStream()
            )
        );
    }

    public void testIPuz() throws Exception {
        IPuzIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInputStatic(
                IPuzIOTest.getTestPuzzle1InputStream()
            )
        );
    }

    public void testRaetzelZentraleSchweden() throws Exception {
        RaetselZentraleSchwedenJSONIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInputStatic(
                RaetselZentraleSchwedenJSONIOTest.getTestPuzzle1InputStream()
            )
        );
    }

    public void testRCIJeuxMFJ() throws Exception {
        RCIJeuxMFJIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInputStatic(
                RCIJeuxMFJIOTest.getTestPuzzle1InputStream()
            )
        );
    }

    public void testPrzekroj() throws Exception {
        PrzekrojIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInputStatic(
                PrzekrojIOTest.getTestPuzzle1InputStream()
            )
        );
    }
}

