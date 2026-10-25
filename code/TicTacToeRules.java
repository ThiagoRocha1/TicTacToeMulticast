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
        if (hasWinningLine(board, X_SYMBOL)) {
            return X_SYMBOL;
        }
        if (hasWinningLine(board, O_SYMBOL)) {
            return O_SYMBOL;
        }
        return EMPTY_CELL;
    }

    static boolean hasWinningLine(int[] boardValues, int playerSymbol) {
        for (int lineIndex = 0; lineIndex < ROW_SIZE; lineIndex++) {
            int rowStart = lineIndex * ROW_SIZE;
            if (findLineWinner(boardValues, rowStart, rowStart + 1, rowStart + 2) == playerSymbol
                    || findLineWinner(boardValues, lineIndex, lineIndex + 3, lineIndex + 6) == playerSymbol) {
                return true;
            }
        }
        return findLineWinner(boardValues, 0, 4, 8) == playerSymbol
                || findLineWinner(boardValues, 2, 4, 6) == playerSymbol;
    }

    public boolean isDraw() {
        return acceptedMoveCount == BOARD_SIZE && getWinner() == EMPTY_CELL;
    }

    public boolean isGameOver() {
        return getWinner() != EMPTY_CELL || isDraw();
    }

    private static int findLineWinner(int[] board, int firstPosition, int secondPosition, int thirdPosition) {
        int firstSymbol = board[firstPosition];
        if (firstSymbol != EMPTY_CELL
                && firstSymbol == board[secondPosition]
                && firstSymbol == board[thirdPosition]) {
            return firstSymbol;
        }
        return EMPTY_CELL;
    }
}
