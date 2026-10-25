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

    private GameMessages() {}
}
