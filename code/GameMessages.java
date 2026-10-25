import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class GameMessages {
    static Map readObject(String jsonText) {
        Object parsedValue = JsonUtil.parseJson(jsonText);
        if (!(parsedValue instanceof Map)) {
            throw new IllegalArgumentException("Esperado um objeto JSON");
        }
        return (Map) parsedValue;
    }

    static String readText(Map message, String fieldName) {
        Object value = message.get(fieldName);
        if (!(value instanceof String) || ((String) value).isBlank()) {
            throw new IllegalArgumentException("Campo textual obrigatório: " + fieldName);
        }
        return (String) value;
    }

    static int readInteger(Map message, String fieldName) {
        Object value = message.get(fieldName);
        if (!(value instanceof BigDecimal)) {
            throw new IllegalArgumentException("Campo inteiro obrigatório: " + fieldName);
        }
        try {
            return ((BigDecimal) value).intValueExact();
        } catch (ArithmeticException invalidInteger) {
            throw new IllegalArgumentException("Inteiro inválido: " + fieldName, invalidInteger);
        }
    }

    static Map createControl(String sessionIdentifier, String eventName) {
        Map message = new HashMap();
        message.put("partidaId", sessionIdentifier);
        message.put("evento", eventName);
        return message;
    }

    static void validateControl(Map message, String sessionIdentifier, String eventName) {
        if (!readText(message, "partidaId").equals(sessionIdentifier)
                || !readText(message, "evento").equals(eventName)) {
            throw new IllegalArgumentException("Controle incompatível com a sessão ou etapa atual");
        }
    }

    static Map createMove(TicTacToeRules gameRules, int cellPosition) {
        Map message = new HashMap();
        message.put("Numero", gameRules.getAcceptedMoveCount() - 1);
        message.put("Jogada", cellPosition);
        List boardValues = new ArrayList();
        for (int cellValue : gameRules.getBoardCopy()) {
            boardValues.add(cellValue);
        }
        message.put("Tabuleiro", boardValues);
        message.put("Resultado", expectedResult(gameRules));
        return message;
    }

    static TicTacToeRules validateMove(Map message, TicTacToeRules currentRules, int opponentSymbol) {
        if (readInteger(message, "Numero") != currentRules.getAcceptedMoveCount()) {
            throw new IllegalArgumentException("Número da jogada fora de sequência");
        }
        TicTacToeRules candidateRules = new TicTacToeRules(currentRules);
        candidateRules.placeMove(readInteger(message, "Jogada"), opponentSymbol);
        Object boardValue = message.get("Tabuleiro");
        if (!(boardValue instanceof List) || ((List) boardValue).size() != 9) {
            throw new IllegalArgumentException("Tabuleiro deve conter nove inteiros");
        }
        List receivedBoard = (List) boardValue;
        int[] expectedBoard = candidateRules.getBoardCopy();
        for (int position = 0; position < expectedBoard.length; position++) {
            Object cellValue = receivedBoard.get(position);
            if (!(cellValue instanceof BigDecimal)
                    || ((BigDecimal) cellValue).compareTo(BigDecimal.valueOf(expectedBoard[position])) != 0) {
                throw new IllegalArgumentException("Tabuleiro divergente da jogada");
            }
        }
        if (!readText(message, "Resultado").equals(expectedResult(candidateRules))) {
            throw new IllegalArgumentException("Resultado incompatível com o tabuleiro");
        }
        return candidateRules;
    }

    private static String expectedResult(TicTacToeRules gameRules) {
        if (gameRules.getWinner() != TicTacToeRules.EMPTY_CELL) {
            return "vit";
        }
        if (gameRules.isDraw()) {
            return "emp";
        }
        return "jog";
    }

    static Map createFinish(String sessionIdentifier, TicTacToeRules gameRules, String status) {
        Map message = createControl(sessionIdentifier, "fim");
        message.put("status", status);
        if (gameRules.getWinner() == TicTacToeRules.EMPTY_CELL || !status.equals("vitoria")) {
            message.put("vencedor", null);
        } else {
            message.put("vencedor", gameRules.getWinner());
        }
        return message;
    }

    static void validateFinish(Map message, String sessionIdentifier, TicTacToeRules gameRules) {
        validateControl(message, sessionIdentifier, "fim");
        String expectedStatus = "empate";
        if (gameRules.getWinner() != TicTacToeRules.EMPTY_CELL) {
            expectedStatus = "vitoria";
        }
        if (!gameRules.isGameOver() || !readText(message, "status").equals(expectedStatus)
                || !message.containsKey("vencedor")) {
            throw new IllegalArgumentException("Encerramento incompatível com o tabuleiro");
        }
        if (gameRules.isDraw()) {
            if (message.get("vencedor") != null) {
                throw new IllegalArgumentException("Empate não possui vencedor");
            }
        } else if (readInteger(message, "vencedor") != gameRules.getWinner()) {
            throw new IllegalArgumentException("Vencedor divergente");
        }
    }

    static int[] readDisplayBoard(Map message) {
        int moveNumber = readInteger(message, "Numero");
        int playedPosition = readInteger(message, "Jogada");
        if (moveNumber < 0 || moveNumber > 8 || playedPosition < 0 || playedPosition > 8) {
            throw new IllegalArgumentException("Numero e Jogada devem estar entre 0 e 8");
        }
        Object boardValue = message.get("Tabuleiro");
        if (!(boardValue instanceof List) || ((List) boardValue).size() != 9) {
            throw new IllegalArgumentException("Tabuleiro deve conter nove inteiros");
        }
        List receivedBoard = (List) boardValue;
        int[] boardValues = new int[9];
        int xCount = 0;
        int oCount = 0;
        for (int position = 0; position < boardValues.length; position++) {
            Object cellValue = receivedBoard.get(position);
            if (!(cellValue instanceof BigDecimal)) {
                throw new IllegalArgumentException("Casa do tabuleiro deve ser um inteiro");
            }
            int cellSymbol;
            try {
                cellSymbol = ((BigDecimal) cellValue).intValueExact();
            } catch (ArithmeticException invalidCell) {
                throw new IllegalArgumentException("Casa do tabuleiro deve ser um inteiro", invalidCell);
            }
            if (cellSymbol < 0 || cellSymbol > 2) {
                throw new IllegalArgumentException("Casa do tabuleiro deve conter 0, 1 ou 2");
            }
            boardValues[position] = cellSymbol;
            if (cellSymbol == TicTacToeRules.X_SYMBOL) {
                xCount++;
            } else if (cellSymbol == TicTacToeRules.O_SYMBOL) {
                oCount++;
            }
        }
        int lastSymbol = moveNumber % 2 + 1;
        if (xCount != (moveNumber + 2) / 2 || oCount != (moveNumber + 1) / 2
                || boardValues[playedPosition] != lastSymbol) {
            throw new IllegalArgumentException("Tabuleiro incompatível com Numero ou Jogada");
        }
        boolean xWon = TicTacToeRules.hasWinningLine(boardValues, TicTacToeRules.X_SYMBOL);
        boolean oWon = TicTacToeRules.hasWinningLine(boardValues, TicTacToeRules.O_SYMBOL);
        if ((xWon && oWon) || (xWon && lastSymbol != TicTacToeRules.X_SYMBOL)
                || (oWon && lastSymbol != TicTacToeRules.O_SYMBOL)) {
            throw new IllegalArgumentException("Tabuleiro com vencedores incompatíveis");
        }
        String expectedResult = "jog";
        if (xWon || oWon) {
            int[] previousBoard = boardValues.clone();
            previousBoard[playedPosition] = TicTacToeRules.EMPTY_CELL;
            if (TicTacToeRules.hasWinningLine(previousBoard, lastSymbol)) {
                throw new IllegalArgumentException("Jogada recebida após uma vitória anterior");
            }
            expectedResult = "vit";
        } else if (moveNumber == 8) {
            expectedResult = "emp";
        }
        if (!readText(message, "Resultado").equals(expectedResult)) {
            throw new IllegalArgumentException("Resultado incompatível com o tabuleiro");
        }
        return boardValues;
    }

    private GameMessages() {}
}
