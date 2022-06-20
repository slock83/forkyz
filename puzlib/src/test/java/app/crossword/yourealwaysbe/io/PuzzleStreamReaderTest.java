
package app.crossword.yourealwaysbe.io;

import junit.framework.TestCase;

public class PuzzleStreamReaderTest extends TestCase {
    public void testAcrossLite() {
        IOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInput(
                IOTest.getTestPuzzle1InputStream()
            )
        );
    }

    public void testJPZ() {
        JPZIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInput(
                JPZIOTest.getTestPuzzle1InputStream()
            )
        );
    }

    public void testBrainsOnly() {
        BrainsOnlyIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInput(
                BrainsOnlyIOTest.getTestPuzzle1InputStream()
            )
        );
    }

    public void testKingFeaturesPlaintext() {
        KingFeaturesPlaintextIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInput(
                KingFeaturesPlaintextIOTest.getTestPuzzle1InputStream()
            )
        );
    }

    public void testUclick() {
        UclickXMLIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInput(
                UclickXMLIOTest.getTestPuzzle1InputStream()
            )
        );
    }

    public void testIPuz() throws Exception {
        IPuzIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInput(
                IPuzIOTest.getTestPuzzle1InputStream()
            )
        );
    }

    public void testRaetzelZentraleSchweden() throws Exception {
        RaetselZentraleSchwedenJSONIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInput(
                RaetselZentraleSchwedenJSONIOTest.getTestPuzzle1InputStream()
            )
        );
    }
}

