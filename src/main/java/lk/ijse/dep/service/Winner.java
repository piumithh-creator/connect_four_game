

package lk.ijse.dep.service;

public class Winner {
    private Piece winnigPiece;
    private int col1;
    private int row1;
    private int col2;
    private int row2;

    public void setWinnigPiece(Piece winnigPiece) {
        this.winnigPiece = winnigPiece;
    }
    public Piece getWinningPiece() {
        return this.winnigPiece;
    }

    public void setCol1(int col1) {
        this.col1 = col1;
    }
    public int getCol1() {
        return this.col1;
    }

    public void setRow1(int row1) {
        this.row1 = row1;
    }
    public int getRow1() {
        return this.row1;
    }

    public void setCol2(int col2) {
        this.col2 = col2;
    }
    public int getCol2() {
        return this.col2;
    }

    public void setRow2(int row2) {
        this.row2 = row2;
    }
    public int getRow2() {
        return this.row2;
    }

    public Winner(Piece winningPiece) {
        this.winnigPiece = winnigPiece;

    }

    public Winner(Piece winnigPiece, int col1, int row1, int col2, int row2) {
        this.winnigPiece = winnigPiece;
        this.col1 = col1;
        this.row1 = row1;
        this.col2 = col2;
        this.row2 = row2;
    }

    public String toString() {
        return "Winner{" +
                "winningPiece=" + winnigPiece +
                ", col1=" + col1 +
                ", row1=" + row1 +
                ", col2=" + col2 +
                ", row2=" + row2 +
                '}';
    }
}