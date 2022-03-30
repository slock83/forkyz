package app.crossword.yourealwaysbe.puz;

import app.crossword.yourealwaysbe.puz.Playboard.Word;

import java.io.Serializable;
import java.util.Objects;

public interface MovementStrategy extends Serializable {

    MovementStrategy MOVE_NEXT_ON_AXIS = new MovementStrategy() {
        @Override
        public Word move(Playboard board, boolean skipCompletedLetters) {
            Position startPos = board.getHighlightLetter();
            Word word = board.moveZoneForward(skipCompletedLetters);
            Position newPos = board.getHighlightLetter();

            if (Objects.equals(startPos, newPos)) {
                // follow trajectory determined by last two positions in
                // zone, or assume move right
                int drow = 0;
                int dcol = 1;
                Zone zone = word.getZone();
                if (zone != null && zone.size() >= 2) {
                    Position last = zone.getPosition(zone.size() - 1);
                    Position penultimate = zone.getPosition(zone.size() - 2);
                    drow = getDirection(penultimate.getRow(), last.getRow());
                    dcol = getDirection(penultimate.getCol(), last.getCol());
                }
                Position next = board.findNextDelta(
                    newPos, skipCompletedLetters, drow, dcol
                );
                if (next != null)
                    board.setHighlightLetter(next);
            }

            return word;
        }

        @Override
        public Word back(Playboard board) {
            Position start = board.getHighlightLetter();
            Word word = board.moveZoneBack(false);
            Position end = board.getHighlightLetter();

            // if did not move, assume at start of zone
            if (Objects.equals(start, end)) {
                // follow trajectory determined by first two positions in
                // zone, or assume move left
                int drow = 0;
                int dcol = -1;
                Zone zone = word.getZone();
                if (zone != null && zone.size() >= 2) {
                    Position first = zone.getPosition(0);
                    Position second = zone.getPosition(1);
                    drow = getDirection(second.getRow(), first.getRow());
                    dcol = getDirection(second.getCol(), first.getCol());
                }
                Position next = board.findNextDelta(end, false, drow, dcol);
                if (next != null)
                    board.setHighlightLetter(next);
            }

            return word;
        }

        private int getDirection(int start, int end) {
            int diff = end - start;
            if (diff < 0) return -1;
            if (diff > 0) return 1;
            return 0;
        }
    };

    MovementStrategy STOP_ON_END = new MovementStrategy() {
        @Override
        public Word move(Playboard board, boolean skipCompletedLetters) {
            return board.moveZoneForward(skipCompletedLetters);
        }

        @Override
        public Word back(Playboard board) {
            return board.moveZoneBack(false);
        }
    };

    MovementStrategy MOVE_NEXT_CLUE = new MovementStrategy() {
        @Override
        public Word move(Playboard board, boolean skipCompletedLetters) {
            return move(board, skipCompletedLetters, null, null);
        }

        @Override
        public Word back(Playboard board) {
            return back(board, null, null);
        }

        /**
         * Keep moving to next clue unless we hit the breakPos
         *
         * If breakPos is null, keep moving until we hit the start of
         * the next clue again.
         *
         * If breakPos hit, reset to resetPos (if not null)
         */
        private Word move(
            Playboard board,
            boolean skipCompletedLetters,
            Position breakPos,
            Position resetPos
        ) {
            Position start = board.getHighlightLetter();
            Word word = board.moveZoneForward(skipCompletedLetters);
            Position newPos = board.getHighlightLetter();

            if (Objects.equals(start, newPos)) {
                ClueID clueID = board.getClue();
                if (clueID != null && clueID.hasClueNumber()) {
                    Puzzle puz = board.getPuzzle();
                    String number = clueID.getClueNumber();
                    String listName = clueID.getListName();
                    ClueList clues = puz.getClues(listName);
                    // TODO: move to next list if at end?
                    String nextNum = clues.getNextClueNumber(number, true);
                    board.jumpToClue(new ClueID(nextNum, listName));

                    Box box = board.getCurrentBox();
                    if (board.skipBox(box, skipCompletedLetters)) {
                        // only carry on if we haven't been here before..
                        newPos = board.getHighlightLetter();
                        if (Objects.equals(breakPos, newPos)) {
                            // didn't find anything, reset
                            board.setHighlightLetter(resetPos);
                        } else {
                            if (breakPos == null)
                                breakPos = newPos;
                            if (resetPos == null)
                                resetPos = start;
                            move(
                                board, skipCompletedLetters, breakPos, resetPos
                            );
                        }
                    }
                }
            }

            return word;
        }

        private Word back(
            Playboard board, Position breakPos, Position resetPos
        ) {
            Position start = board.getHighlightLetter();
            Word word = board.moveZoneBack(false);
            Position end = board.getHighlightLetter();

            // if did not move, assume at end of zone and move to
            // previous clue in same list
            if (Objects.equals(start, end)) {
                ClueID clueID = board.getClue();
                if (clueID != null && clueID.hasClueNumber()) {
                    Puzzle puz = board.getPuzzle();
                    String number = clueID.getClueNumber();
                    String listName = clueID.getListName();
                    ClueList clues = puz.getClues(listName);
                    // TODO: move to next list if at end?
                    String nextNum = clues.getPreviousClueNumber(number, true);
                    board.jumpToClueEnd(new ClueID(nextNum, listName));

                    Box box = board.getCurrentBox();
                    if (board.skipBox(box, false)) {
                        // only carry on if we haven't been here before..
                        Position newPos = board.getHighlightLetter();
                        if (Objects.equals(breakPos, newPos)) {
                            // didn't find anything, reset
                            board.setHighlightLetter(resetPos);
                        } else {
                            if (breakPos == null)
                                breakPos = newPos;
                            if (resetPos == null)
                                resetPos = start;
                            back(board, breakPos, resetPos);
                        }
                    }
                }
            }

            return word;
        }
    };

    MovementStrategy MOVE_PARALLEL_WORD = new MovementStrategy() {
        @Override
        public Word move(Playboard board, boolean skipCompletedLetters) {
            return move(board, skipCompletedLetters, null);
        }

        @Override
        public Word back(Playboard board) {
            return back(board, null);
        }

        private Word move(
            Playboard board,
            boolean skipCompletedLetters,
            Position resetPos
        ) {
            Position start = board.getHighlightLetter();
            Word word = board.moveZoneForward(skipCompletedLetters);
            Position newPos = board.getHighlightLetter();

            if (Objects.equals(start, newPos)) {
                moveToParallelWord(board, word);
                Box box = board.getCurrentBox();
                if (board.skipBox(box, skipCompletedLetters)) {
                    // only carry on if we budged
                    newPos = board.getHighlightLetter();
                    if (Objects.equals(start, newPos)) {
                        // didn't find anything, reset
                        board.setHighlightLetter(resetPos);
                    } else {
                        if (resetPos == null)
                            resetPos = start;
                        move(board, skipCompletedLetters, resetPos);
                    }
                }
            }

            return word;
        }

        /**
         * Move to nearest word in same direction that is alongside w
         *
         * Or leave unchanged if no suitable word found. Starts
         * scanning from the nearest edge of the board.
         *
         * @return new word highlighted
         */
        private Word moveToParallelWord(Playboard board, Word w) {
            boolean biasStart = getWordBias(board, w);
            boolean across = getWordDirection(w);
            Zone zone = (w == null) ? null : w.getZone();

            if (zone == null || zone.size() == 0)
                return w;

            Position start = zone.getPosition(0);
            Position end = zone.getPosition(zone.size() - 1);
            ClueID clue = w.getClueID();
            String listName = (clue == null) ? null : clue.getListName();

            // TODO: won't really work for backwards/upwards clues
            int numRows = end.getRow() - start.getRow();
            int numCols = end.getCol() - start.getCol();

            Puzzle puz = board.getPuzzle();

            if (across) {
                // scan from row below underneath the word until another
                // across word found, stop row scan if we hit the
                // current word (for diagonals)
                int height = puz.getHeight();
                for (int row = start.getRow() + 1; row < height; row++) {
                    for (int offCol = 0; offCol < numCols; offCol++) {
                        int col = biasStart
                            ? start.getCol() + offCol
                            : end.getCol() - offCol;

                        Box box = puz.checkedGetBox(row, col);

                        if (box != null && box.isPartOf(clue))
                            break;

                        ClueID newClueID = (box == null)
                            ? null
                            : box.getIsPartOfClue(listName);

                        if (newClueID != null) {
                            board.jumpToClue(newClueID);
                            return board.getCurrentWord();
                        }
                    }
                }
            } else {
                // scan from col after the word in same rows until another
                // down word found
                int width = puz.getWidth();
                for (int col = start.getCol() + 1; col < width; col++) {
                    for (int offRow = 0; offRow < numRows; offRow++) {
                        int row = biasStart
                            ? start.getRow() + offRow
                            : end.getRow() - offRow;

                        Box box = puz.checkedGetBox(row, col);

                        if (box != null && box.isPartOf(clue))
                            break;

                        ClueID newClueID = (box == null)
                            ? null
                            : box.getIsPartOfClue(listName);

                        if (newClueID != null) {
                            board.jumpToClue(newClueID);
                            return board.getCurrentWord();
                        }
                    }
                }
            }

            return w;
        }

        /**
         * If word is nearer start or end of board
         */
        private boolean getWordBias(Playboard board, Word w) {
            int startOffset;
            int endOffset;
            boolean across = getWordDirection(w);

            Zone zone = (w == null) ? null : w.getZone();

            if (zone == null || zone.size() == 0)
                return true;

            Position start = zone.getPosition(0);
            Position end = zone.getPosition(zone.size() - 1);

            if (across) {
                int width = board.getPuzzle().getWidth();
                startOffset = start.getCol();
                endOffset = width - end.getCol();
            } else {
                int height = board.getPuzzle().getHeight();
                startOffset = start.getRow();
                endOffset = height - end.getRow();
            }

            return startOffset <= endOffset;
        }

        private Word back(
            Playboard board, Position resetPos
        ) {
            Position start = board.getHighlightLetter();
            Word word = board.moveZoneBack(false);
            Position end = board.getHighlightLetter();

            if (Objects.equals(start, end)) {
                moveToParallelWordBack(board, word);
                Box box = board.getCurrentBox();
                if (board.skipBox(box, false)) {
                    // only carry on if we budged
                    Position newPos = board.getHighlightLetter();
                    if (Objects.equals(start, newPos)) {
                        // didn't find anything, reset
                        board.setHighlightLetter(resetPos);
                    } else {
                        if (resetPos == null)
                            resetPos = start;
                        back(board, resetPos);
                    }
                }
            }

            return word;
        }

        /**
         * Move to nearest word in same direction that is back and
         * alongside w
         *
         * Or leave unchanged if no suitable word found. Starts
         * scanning from the nearest edge of the board.
         *
         * @return new word highlighted
         */
        private Word moveToParallelWordBack(Playboard board, Word w) {
            Box[][] boxes = board.getBoxes();
            boolean biasStart = getWordBias(board, w);
            boolean across = getWordDirection(w);
            Zone zone = (w == null) ? null : w.getZone();

            if (zone == null || zone.size() == 0)
                return w;

            Position start = zone.getPosition(0);
            Position end = zone.getPosition(zone.size() - 1);
            ClueID clue = w.getClueID();
            String listName = (clue == null) ? null : clue.getListName();

            if (across) {
                // scan from row below underneath the word until another
                // across word found, stop if hit current clue
                for (int row = start.getRow() - 1; row >= 0; row -= 1) {
                    for (int offCol = 0; offCol < end.getCol(); offCol++) {
                        int col = biasStart
                            ? start.getCol() + offCol
                            : end.getCol() - offCol;

                        Box box = boxes[row][col];

                        if (box != null && box.isPartOf(clue))
                            break;

                        ClueID newClueID = (box == null)
                            ? null
                            : box.getIsPartOfClue(listName);

                        if (newClueID != null) {
                            board.jumpToClueEnd(newClueID);
                            return board.getCurrentWord();
                        }
                    }
                }
            } else {
                // scan from col after the word in same rows until another
                // down word found, stop if hit current clue
                for (int col = start.getCol() - 1; col >= 0; col -= 1) {
                    for (int offRow = 0; offRow < end.getRow(); offRow++) {
                        int row = biasStart
                            ? start.getRow() + offRow
                            : end.getRow() - offRow;

                        Box box = boxes[row][col];

                        if (box != null && box.isPartOf(clue))
                            break;

                        ClueID newClueID = (box == null)
                            ? null
                            : box.getIsPartOfClue(listName);

                        if (newClueID != null) {
                            board.jumpToClueEnd(newClueID);
                            return board.getCurrentWord();
                        }
                    }
                }
            }

            return w;
        }

        /**
         * Whether to search across or down
         *
         * Assumes across unless clearly down. This isn't going to
         * really work for clues that go up or left, but this movement
         * strategy is already complicated enough...
         */
        private boolean getWordDirection(Word word) {
            Zone zone = (word == null) ? null : word.getZone();
            if (zone != null && zone.size() >= 2) {
                Position pos0 = zone.getPosition(0);
                Position pos1 = zone.getPosition(1);
                if (pos0.getCol() == pos1.getCol())
                    return false;
            }
            return true;
        }
    };

    Word move(Playboard board, boolean skipCompletedLetters);

    Word back(Playboard board);

}
