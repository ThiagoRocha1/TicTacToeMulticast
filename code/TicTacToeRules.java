public class TicTacToeRules {
    public static final int EMPTY_CELL = 0;
    public static final int X_SYMBOL = 1;
    public static final int O_SYMBOL = 2;
    private static final int BOARD_SIZE = 9;
    private static final int ROW_SIZE = 3;

    private int[] board = new int[BOARD_SIZE];
    private int acceptedMoveCount;
    private int lastPlayedSymbol = EMPTY_CELL;

    public TicTacToeRules() {}

    public TicTacToeRules(TicTacToeRules previousRules) {
        board = previousRules.getBoardCopy();
        acceptedMoveCount = previousRules.acceptedMoveCount;
        lastPlayedSymbol = previousRules.lastPlayedSymbol;
    }

    public void placeMove(int cellPosition, int playerSymbol) {
        if (playerSymbol != X_SYMBOL && playerSymbol != O_SYMBOL) {
            throw new IllegalArgumentException("O símbolo deve ser 1 (X) ou 2 (O)");
        }
        if (cellPosition < 0 || cellPosition >= BOARD_SIZE) {
            throw new IllegalArgumentException("A casa deve estar entre 0 e 8");
        }
        if (isGameOver()) {
            throw new IllegalStateException("A partida já terminou");
        }
        if (board[cellPosition] != EMPTY_CELL) {
            throw new IllegalArgumentException("A casa escolhida já está ocupada");
        }
        if (playerSymbol == lastPlayedSymbol) {
            throw new IllegalArgumentException("É a vez do outro símbolo jogar");
        }

        board[cellPosition] = playerSymbol;
        acceptedMoveCount++;
        lastPlayedSymbol = playerSymbol;
    }

    public int getAcceptedMoveCount() {
        return acceptedMoveCount;
    }

    public int[] getBoardCopy() {
        int[] boardCopy = new int[BOARD_SIZE];
        for (int cellPosition = 0; cellPosition < BOARD_SIZE; cellPosition++) {
            boardCopy[cellPosition] = board[cellPosition];
        }
        return boardCopy;
    }

    public int getWinner() {
        for (int lineIndex = 0; lineIndex < ROW_SIZE; lineIndex++) {
            int rowStart = lineIndex * ROW_SIZE;
            int rowWinner = findLineWinner(rowStart, rowStart + 1, rowStart + 2);
            if (rowWinner != EMPTY_CELL) {
                return rowWinner;
            }
            int columnWinner = findLineWinner(lineIndex, lineIndex + 3, lineIndex + 6);
            if (columnWinner != EMPTY_CELL) {
                return columnWinner;
            }
        }

        int diagonalWinner = findLineWinner(0, 4, 8);
        if (diagonalWinner != EMPTY_CELL) {
            return diagonalWinner;
        }
        return findLineWinner(2, 4, 6);
    }

    public boolean isDraw() {
        return acceptedMoveCount == BOARD_SIZE && getWinner() == EMPTY_CELL;
    }

    public boolean isGameOver() {
        return getWinner() != EMPTY_CELL || isDraw();
    }

    private int findLineWinner(int firstPosition, int secondPosition, int thirdPosition) {
        int firstSymbol = board[firstPosition];
        if (firstSymbol != EMPTY_CELL
                && firstSymbol == board[secondPosition]
                && firstSymbol == board[thirdPosition]) {
            return firstSymbol;
        }
        return EMPTY_CELL;
    }
}
