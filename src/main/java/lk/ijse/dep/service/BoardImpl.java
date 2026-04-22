package lk.ijse.dep.service;

public class BoardImpl implements Board {
    private Piece[][] pieces;
    private BoardUI boardUI;
    public BoardImpl(BoardUI boardUI) {
        this.boardUI = boardUI;
        this.pieces = new Piece[NUM_OF_COLS][NUM_OF_ROWS];
        resetBoard();
    }

    private void resetBoard() {
        for (int col = 0; col < NUM_OF_COLS; col++) {
            for (int row = 0; row < NUM_OF_ROWS; row++) {
                pieces[col][row] = Piece.EMPTY;
            }
        }
    }

    public void setPieces(Piece[][] pieces) {
        this.pieces = pieces;
    }

    public Piece[][] getPieces() {
        return pieces;
    }

    public void setBoardUI(BoardUI boardUI) {
        this.boardUI = boardUI;
    }

    public BoardUI getBoardUI() {
        return boardUI;
    }

    @Override
    public int findNextAvailableSpot(int col) {
        for (int row = 0; row < NUM_OF_ROWS; row++) {
            if (pieces[col][row] == Piece.EMPTY) {
                return row;
            }
        }
        return -1;
    }

    @Override
    public boolean isLegalMove(int col) {
        return findNextAvailableSpot(col) != -1;
    }

    @Override
    public boolean existLegalMoves() {
        for (int col = 0; col < NUM_OF_COLS; col++) {
            if (isLegalMove(col)) return true;
        }
        return false;
    }

    @Override
    public void updateMove(int col, Piece move) {
        int availableRow = findNextAvailableSpot(col);
        if (availableRow != -1) {
            pieces[col][availableRow] = move;
        }
    }

    @Override
    public Winner findWinner() {
        Winner winner = checkWinnerFor(Piece.BLUE);
        if (winner.getWinningPiece() != Piece.EMPTY) return winner;

        winner = checkWinnerFor(Piece.GREEN);
        if (winner.getWinningPiece() != Piece.EMPTY) return winner;

        return new Winner(Piece.EMPTY, -1, -1, -1, -1);
    }


    private Winner checkWinnerFor(Piece piece) {

        for (int row = 0; row < NUM_OF_ROWS; row++) {
            for (int col = 0; col <= NUM_OF_COLS - 4; col++) {
                if (isConsecutive(piece, col, row, 1, 0)) {
                    return new Winner(piece, col, row, col + 3, row);
                }
            }
        }


        for (int col = 0; col < NUM_OF_COLS; col++) {
            for (int row = 0; row <= NUM_OF_ROWS - 4; row++) {
                if (isConsecutive(piece, col, row, 0, 1)) {
                    return new Winner(piece, col, row, col, row + 3);
                }
            }
        }

        return new Winner(Piece.EMPTY, -1, -1, -1, -1);
    }


    private boolean isConsecutive(Piece piece, int col, int row, int dCol, int dRow) {
        for (int i = 0; i < 4; i++) {
            if (pieces[col + i * dCol][row + i * dRow] != piece) {
                return false;
            }
        }
        return true;
    }
}