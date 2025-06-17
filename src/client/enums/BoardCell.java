package client.enums;

public enum BoardCell {

    WATER('-'),
    SHIP('#'),
    HIT_AND_SUNK('X'),
    HIT_MAST('?'),
    MISS('0');

    private final char symbol;

    BoardCell(char symbol) {
        this.symbol = symbol;
    }

    public char getSymbol() {
        return symbol;
    }
}
