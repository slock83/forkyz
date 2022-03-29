package app.crossword.yourealwaysbe.puz;

import app.crossword.yourealwaysbe.puz.Playboard.Word;

import java.io.Serializable;

public interface MovementStrategy extends Serializable {

    class Common {

        /**
         * @return if @param Word (w) is the last word in its direction in the @param board
         */
        static boolean isLastWordInDirection(Playboard board, Word w) {
            return isLastWordInDirection(board.getBoxes(), w);
        }

        /**
         * @return if @param Word (w) is the last word in its direction in @param boxes
         */
        static boolean isLastWordInDirection(Box[][] boxes, Word w) {
            if (w.across) {
                return (
                    w.start.getCol() + w.length
                    >= boxes[w.start.getRow()].length
                );
            }
            return (w.start.getRow() + w.length >= boxes.length);
        }

        /**
         * @return if @param Position (p) is the last position in @param Word (w)
         */
        static boolean isWordEnd(Position p, Word w) {
            return
                (w.across && p.getCol() == w.start.getCol() + w.length - 1) ||
                (!w.across && p.getRow() == w.start.getRow() + w.length - 1)
            ;
        }

        /**
         * @return if @param Position (p) is the first position in @param Word (w)
         */
        static boolean isWordStart(Position p, Word w) {
            return (w.across && p.getCol() == w.start.getCol())
                || (!w.across && p.getRow() == w.start.getRow())
            ;
        }
    }

    MovementStrategy MOVE_NEXT_ON_AXIS = new MovementStrategy() {

        @Override
        public Word move(Playboard board, boolean skipCompletedLetters) {
            if (board.isAcross()) {
                return board.moveRight(skipCompletedLetters);
            } else {
                return board.moveDown(skipCompletedLetters);
            }
        }

        @Override
        public Word back(Playboard board) {
            if (board.isAcross()) {
                return board.moveLeft();
            } else {
                return board.moveUp(false);
            }
        }
    };

    MovementStrategy STOP_ON_END = new MovementStrategy() {

        @Override
        public Word move(Playboard board, boolean skipCompletedLetters) {
            // This is overly complex, but I am trying to save calls to heavy
            // methods on the board.

            Position p = board.getHighlightLetter();
            Word w = board.getCurrentWord();
            boolean across = board.isAcross();

            if (Common.isWordEnd(p, w)) {
                return w;
            } else {
                MOVE_NEXT_ON_AXIS.move(board,skipCompletedLetters);
                Word newWord = board.getCurrentWord();
                if (!newWord.equals(w)) {
                    board.setHighlightLetter(p);
                    board.setAcross(across);
                } else if (skipCompletedLetters && Common.isLastWordInDirection(board, w)) {
                    // special case if this is at the end of the board
                    Position current = board.getHighlightLetter();
                    Box[][] boxes = board.getBoxes();
                    if (!boxes[current.getRow()][current.getCol()].isBlank()) {
                        board.setHighlightLetter(p);
                    }
                }
                return w;
            }
        }

        @Override
        public Word back(Playboard board) {
            Word w = board.getCurrentWord();
            Position p = board.getHighlightLetter();
            if(!p.equals(w.start)){
                MOVE_NEXT_ON_AXIS.back(board);
            }
            return w;
        }

    };

    MovementStrategy MOVE_NEXT_CLUE = new MovementStrategy() {

        /**
         * Moves to the word corresponding to the next clue.  If the current word is the last
         * across word, then this moves to the first down word, and if it is the last down word,
         * it moves to the first across word.  Returns true if the first letter of the new clue
         * is blank.
         */
        private boolean moveToNextWord(Playboard board, boolean skipCompletedLetters) {
            Word w = board.getCurrentWord();
            Box box = board.getBoxes()[w.start.getRow()][w.start.getCol()];
            int currentClueNumber = box.getClueNumber();
            boolean nextClueAcross;
            Puzzle puz = board.getPuzzle();
            int nextClueNumber
                = puz.getClues(w.across)
                    .getNextClueNumber(currentClueNumber, false);
            if(nextClueNumber < 0) {
                // At end of clues - move to first clue of other type.
                nextClueAcross = !w.across;
                nextClueNumber
                    = puz.getClues(nextClueAcross).getFirstClueNumber();
            } else {
                nextClueAcross = w.across;
            }
            board.jumpToClue(nextClueNumber, nextClueAcross);
            return !board.skipCurrentBox(board.getCurrentBox(), skipCompletedLetters);
        }

        /**
         * Moves to the last letter of the word corresponding to the
         * previous clue.  Does nothing if the current word is the first
         * across or first down clue move to the last down or across
         * respectively.
         */
        private void moveToPreviousWord(Playboard board) {
            Word w = board.getCurrentWord();
            int currentClueNumber
                = board.getBoxes()[w.start.getRow()][w.start.getCol()]
                    .getClueNumber();
            Puzzle puz = board.getPuzzle();
            int previousClueNumber
                = puz.getClues(w.across)
                    .getPreviousClueNumber(currentClueNumber, false);
            boolean previousClueAcross = w.across;
            if (previousClueNumber < 0) {
                previousClueAcross = !w.across;
                previousClueNumber
                    = puz.getClues(previousClueAcross).getLastClueNumber();
            }
            board.jumpToClue(previousClueNumber, previousClueAcross);

            // Move to last letter.
            w = board.getCurrentWord();
            Position newPos;
            if (w.across) {
                newPos = new Position(
                    w.start.getRow(), w.start.getCol() + w.length - 1
                );
            } else {
                newPos = new Position(
                    w.start.getRow() + w.length - 1, w.start.getCol()
                );
            }
            board.setHighlightLetter(newPos);
        }

        /**
         * Moves to the next blank letter in this clue, starting at position p.  Returns true if
         * such a letter was found; returns false if the clue has already been filled.
         */
        private boolean moveToNextBlank(Playboard board, Position p, boolean skipCompletedLetters) {
            Word w = board.getCurrentWord();
            Box[] wordBoxes = board.getCurrentWordBoxes();

            if(w.across) {
                for(int x = p.getCol() ; x < w.start.getCol() + w.length; x++) {
                    boolean skip = board.skipCurrentBox(
                        wordBoxes[x - w.start.getCol()], skipCompletedLetters
                    );
                    if(!skip) {
                        board.setHighlightLetter(
                            new Position(p.getRow(), x)
                        );
                        return true;
                    }
                }
            } else {
                for(int y = p.getRow(); y < w.start.getRow() + w.length; y++) {
                    boolean skip = board.skipCurrentBox(
                        wordBoxes[y - w.start.getRow()], skipCompletedLetters
                    );
                    if(!skip) {
                        board.setHighlightLetter(
                            new Position(y, p.getCol())
                        );
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public Word move(Playboard board, boolean skipCompletedLetters) {
            Position p = board.getHighlightLetter();
            Word w = board.getCurrentWord();

            if((!board.isShowErrors() && board.getPuzzle().getPercentFilled() == 100) ||
                    board.getPuzzle().getPercentComplete() == 100) {
                // Puzzle complete - don't move.
                return w;
            }

            Position nextPos;
            if (Common.isWordEnd(p, w)) {
                // At end of a word - move to the next one and continue.
                if(moveToNextWord(board, skipCompletedLetters)) {
                    return w;
                }
                nextPos = board.getHighlightLetter();
            } else {
                // In middle of word - move to the next unfilled letter.
                nextPos = w.across
                    ? new Position(p.getRow(), p.getCol() + 1)
                    : new Position(p.getRow() + 1, p.getCol());
            }
            while(!(moveToNextBlank(board, nextPos, skipCompletedLetters))) {
                if(moveToNextWord(board, skipCompletedLetters)) {
                    break;
                }
                nextPos = board.getHighlightLetter();
            }
            return w;
        }

        @Override
        public Word back(Playboard board) {
            Position p = board.getHighlightLetter();
            Word w = board.getCurrentWord();
            if ((w.across && p.getCol() == w.start.getCol())
                    || (!w.across && p.getRow() == w.start.getRow())) {
                // At beginning of word - move to previous clue.
                moveToPreviousWord(board);
            } else {
                // In middle of word - just move back one character.
                MOVE_NEXT_ON_AXIS.back(board);
            }
            return w;
        }

    };

    MovementStrategy MOVE_PARALLEL_WORD = new MovementStrategy() {

        @Override
        public Word move(Playboard board, boolean skipCompletedLetters) {
            Word w = board.getCurrentWord();
            Position p = board.getHighlightLetter();

            if (Common.isWordEnd(p, w)) {
                Word newWord = moveToParallelWord(board, w);
                // check if new word needs skipping too
                if (!w.equals(newWord)) {
                    Box currentBox = board.getCurrentBox();
                    if (board.skipCurrentBox(currentBox, skipCompletedLetters)) {
                        move(board, skipCompletedLetters);
                    }
                }
            } else {
                MOVE_NEXT_ON_AXIS.move(board, skipCompletedLetters);
                Word newWord = board.getCurrentWord();
                if (!newWord.equals(w)) {
                    Position end = new Position(
                        w.start.getRow() + (w.across ? 0 : w.length - 1),
                        w.start.getCol() + (w.across ? w.length - 1: 0)
                    );
                    board.setHighlightLetter(end);
                    this.move(board, skipCompletedLetters);
                } else {
                    //edge case - the move next on axis didn't move the position because the skipCompleted
                    //moved to the end of the word, and the next on axis tried to move onto a non-letter space
                    if (skipCompletedLetters && Common.isWordEnd(board.getHighlightLetter(), w)) {
                        //noinspection ConstantConditions
                        if (board.skipCurrentBox(board.getCurrentBox(), skipCompletedLetters)) {
                            //noinspection ConstantConditions
                            this.move(board, skipCompletedLetters);
                        }
                    } else {
                        Position newPos = board.getHighlightLetter();
                        if (p.equals(newPos) && !Common.isWordEnd(board.getHighlightLetter(), w)) {
                            Position end = new Position(
                                w.start.getRow() + (w.across ? 0 : w.length - 1),
                                w.start.getCol() + (w.across ? w.length - 1: 0)
                            );
                            board.setHighlightLetter(end);
                            this.move(board, skipCompletedLetters);
                        }
                    }
                }
            }

            return w;
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
            Box[][] boxes = board.getBoxes();
            boolean biasStart = getWordBias(board, w);

            if (w.across) {
                // scan from row below underneath the word until another
                // across word found
                for (int row = w.start.getRow() + 1; row < boxes.length; row++) {
                    for (int offCol = 0; offCol < w.length; offCol++) {
                        int col = biasStart
                            ? w.start.getCol() + offCol
                            : w.start.getCol() + w.length - offCol - 1;
                        Box box = boxes[row][col];
                        if (box != null && box.isPartOfAcross()) {
                            board.jumpToClue(
                                box.getPartOfAcrossClueNumber(), true
                            );
                            return board.getCurrentWord();
                        }
                    }
                }
            } else {
                // scan from col after the word in same rows until another
                // down word found
                int width = board.getPuzzle().getWidth();
                for (int col = w.start.getCol() + 1; col < width; col++) {
                    for (int offRow = 0; offRow < w.length; offRow++) {
                        int row = biasStart
                            ? w.start.getRow() + offRow
                            : w.start.getRow() + w.length - offRow - 1;
                        Box box = boxes[row][col];
                        if (box != null && box.isPartOfDown()) {
                            board.jumpToClue(
                                box.getPartOfDownClueNumber(), false
                            );
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

            if (w.across) {
                int width = board.getPuzzle().getWidth();
                startOffset = w.start.getCol();
                endOffset = width - (w.start.getCol() + w.length);
            } else {
                int height = board.getPuzzle().getHeight();
                startOffset = w.start.getRow();
                endOffset = height - (w.start.getRow() + w.length);
            }

            return startOffset <= endOffset;
        }

        @Override
        public Word back(Playboard board) {
            Word w = board.getCurrentWord();
            Position p = board.getHighlightLetter();

            if (Common.isWordStart(p, w)) {
                moveToParallelWordBack(board, w);
            } else {
                Word newWord = MOVE_NEXT_ON_AXIS.back(board);
                if (!newWord.equals(w)) {
                    board.setHighlightLetter(newWord.start);
                }
            }

            return w;
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

            if (w.across) {
                // scan from row below underneath the word until another
                // across word found
                for (int row = w.start.getRow() - 1; row >= 0; row -= 1) {
                    for (int offCol = 0; offCol < w.length; offCol++) {
                        int col = biasStart
                            ? w.start.getCol() + offCol
                            : w.start.getCol() + w.length - offCol - 1;
                        Box box = boxes[row][col];
                        if (box != null && box.isPartOfAcross()) {
                            board.jumpToClueEnd(
                                box.getPartOfAcrossClueNumber(), true
                            );
                            return board.getCurrentWord();
                        }
                    }
                }
            } else {
                // scan from col after the word in same rows until another
                // down word found
                for (int col = w.start.getCol() - 1; col >= 0; col -= 1) {
                    for (int offRow = 0; offRow < w.length; offRow++) {
                        int row = biasStart
                            ? w.start.getRow() + offRow
                            : w.start.getRow() + w.length - offRow - 1;
                        Box box = boxes[row][col];
                        if (box != null && box.isPartOfDown()) {
                            board.jumpToClueEnd(
                                box.getPartOfDownClueNumber(), false
                            );
                            return board.getCurrentWord();
                        }
                    }
                }
            }

            return w;
        }

    };

    Word move(Playboard board, boolean skipCompletedLetters);

    Word back(Playboard board);

}
