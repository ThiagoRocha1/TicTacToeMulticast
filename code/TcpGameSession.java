import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

class TcpGameSession {
    private static final int RESPONSE_TIMEOUT_SECONDS = 120;
    private static final int MAX_MESSAGE_CHARACTERS = 16384;
    private Socket opponentSocket;
    private BufferedReader networkReader;
    private BufferedWriter networkWriter;
    private BufferedReader terminalReader;
    private String sessionIdentifier;
    private int localSymbol;
    private int opponentSymbol;
    private TicTacToeRules gameRules = new TicTacToeRules();

    TcpGameSession(Socket opponentSocket, boolean startsMatch) throws IOException {
        this.opponentSocket = opponentSocket;
        networkReader = new BufferedReader(new InputStreamReader(opponentSocket.getInputStream(),
                StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)));
        networkWriter = new BufferedWriter(new OutputStreamWriter(opponentSocket.getOutputStream(), StandardCharsets.UTF_8));
        terminalReader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        localSymbol = TicTacToeRules.O_SYMBOL;
        opponentSymbol = TicTacToeRules.X_SYMBOL;
        if (startsMatch) {
            localSymbol = TicTacToeRules.X_SYMBOL;
            opponentSymbol = TicTacToeRules.O_SYMBOL;
        }
    }

    void play(boolean startsMatch) throws IOException {
        try {
            exchangeSessionIdentifier(startsMatch);
            System.out.println("Conexão TCP estabelecida. Você joga com " + symbolName(localSymbol) + ".");
            showBoard();
            if (!startsMatch) {
                receiveOpponentTurn();
            }
            while (!gameRules.isGameOver()) {
                playLocalTurn();
            }
            showResult();
        } catch (SocketTimeoutException inactivity) {
            sendFailureWhenPossible("inatividade", "Prazo de 120 segundos esgotado");
            throw inactivity;
        } catch (IOException communicationFailure) {
            sendFailureWhenPossible("erro", communicationFailure.getMessage());
            throw communicationFailure;
        } catch (IllegalArgumentException invalidMessage) {
            sendFailureWhenPossible("erro", invalidMessage.getMessage());
            throw new IOException("Mensagem de controle inválida: " + invalidMessage.getMessage(), invalidMessage);
        }
    }

    private void exchangeSessionIdentifier(boolean startsMatch) throws IOException {
        long deadline = newResponseDeadline();
        if (startsMatch) {
            sessionIdentifier = UUID.randomUUID().toString();
            sendMessage(GameMessages.createControl(sessionIdentifier, "inicio"));
        } else {
            Map initialMessage = readMessage(deadline);
            sessionIdentifier = GameMessages.readText(initialMessage, "partidaId");
            UUID.fromString(sessionIdentifier);
            GameMessages.validateControl(initialMessage, sessionIdentifier, "inicio");
        }
        System.out.println("Partida: " + sessionIdentifier);
    }

    private void playLocalTurn() throws IOException {
        TicTacToeRules previousRules = gameRules;
        long responseDeadline = 0;
        while (true) {
            gameRules = previousRules;
            int cellPosition = readLocalMove(responseDeadline);
            TicTacToeRules candidateRules = new TicTacToeRules(previousRules);
            candidateRules.placeMove(cellPosition, localSymbol);
            if (responseDeadline == 0) {
                responseDeadline = newResponseDeadline();
            }
            ensureTimeRemaining(responseDeadline);
            sendMessage(GameMessages.createMove(candidateRules, cellPosition));
            gameRules = candidateRules;
            showBoard();
            Map response = readMessage(responseDeadline);
            if (response.containsKey("evento")) {
                String eventName = GameMessages.readText(response, "evento");
                if (eventName.equals("rejeicao")) {
                    GameMessages.validateControl(response, sessionIdentifier, "rejeicao");
                    if (!Boolean.TRUE.equals(response.get("refazer"))) {
                        throw new IOException("Rejeição sem pedido de correção");
                    }
                    System.out.println("Jogada rejeitada: " + GameMessages.readText(response, "motivo"));
                    gameRules = previousRules;
                    showBoard();
                    continue;
                }
                processRemoteFailure(response);
                GameMessages.validateFinish(response, sessionIdentifier, gameRules);
                sendMessage(GameMessages.createControl(sessionIdentifier, "confirmacao_fim"));
                return;
            }
            if (gameRules.isGameOver()) {
                throw new IOException("Esperado controle fim após a jogada conclusiva");
            }
            acceptOpponentMove(response, responseDeadline);
            return;
        }
    }

    private void receiveOpponentTurn() throws IOException {
        long deadline = newResponseDeadline();
        System.out.println("Aguardando a jogada do adversário (até 120 segundos)...");
        Map message = readMessage(deadline);
        acceptOpponentMove(message, deadline);
    }

    private void acceptOpponentMove(Map message, long deadline) throws IOException {
        while (true) {
            if (message.containsKey("evento")) {
                processRemoteFailure(message);
                throw new IOException("Controle inesperado enquanto aguardava jogada");
            }
            TicTacToeRules candidateRules;
            try {
                candidateRules = GameMessages.validateMove(message, gameRules, opponentSymbol);
            } catch (IllegalArgumentException invalidMove) {
                Map rejection = GameMessages.createControl(sessionIdentifier, "rejeicao");
                rejection.put("motivo", invalidMove.getMessage());
                rejection.put("refazer", true);
                sendMessage(rejection);
                System.out.println("Jogada adversária rejeitada: " + invalidMove.getMessage());
                message = readMessage(deadline);
                continue;
            }
            gameRules = candidateRules;
            showBoard();
            if (gameRules.isGameOver()) {
                String status = "vitoria";
                if (gameRules.isDraw()) {
                    status = "empate";
                }
                sendMessage(GameMessages.createFinish(sessionIdentifier, gameRules, status));
                Map confirmation = readMessage(newResponseDeadline());
                processRemoteFailure(confirmation);
                GameMessages.validateControl(confirmation, sessionIdentifier, "confirmacao_fim");
            }
            return;
        }
    }

    private int readLocalMove(long correctionDeadline) throws IOException {
        while (true) {
            if (correctionDeadline != 0) {
                ensureTimeRemaining(correctionDeadline);
            }
            System.out.print("Escolha uma casa de 0 a 8: ");
            System.out.flush();
            TerminalLineReader inputTask = new TerminalLineReader(terminalReader);
            Thread inputThread = new Thread(inputTask, "entrada-da-jogada");
            inputThread.setDaemon(true);
            inputThread.start();
            while (!inputTask.isCompleted()) {
                if (correctionDeadline != 0) {
                    ensureTimeRemaining(correctionDeadline);
                }
                opponentSocket.setSoTimeout(100);
                int firstCharacter;
                try {
                    firstCharacter = networkReader.read();
                } catch (SocketTimeoutException noRemoteMessage) {
                    continue;
                }
                if (firstCharacter == -1) {
                    throw new EOFException("O adversário encerrou a conexão");
                }
                Map unexpectedMessage = readMessage(newResponseDeadline(), firstCharacter);
                processRemoteFailure(unexpectedMessage);
                throw new IOException("Mensagem recebida fora do turno do adversário");
            }
            String inputText = inputTask.getInputText();
            if (inputText == null) {
                throw new EOFException("Entrada do terminal encerrada");
            }
            try {
                int cellPosition = Integer.parseInt(inputText.trim());
                TicTacToeRules candidateRules = new TicTacToeRules(gameRules);
                candidateRules.placeMove(cellPosition, localSymbol);
                return cellPosition;
            } catch (NumberFormatException invalidNumber) {
                System.out.println("Digite um número inteiro de 0 a 8.");
            } catch (IllegalArgumentException invalidInput) {
                System.out.println("Jogada inválida: " + invalidInput.getMessage());
            }
        }
    }

    private Map readMessage(long deadline) throws IOException {
        return readMessage(deadline, -2);
    }

    private Map readMessage(long deadline, int firstCharacter) throws IOException {
        StringBuilder messageText = new StringBuilder();
        int nextCharacter = firstCharacter;
        while (true) {
            int remainingMillis = ensureTimeRemaining(deadline);
            opponentSocket.setSoTimeout(remainingMillis);
            if (nextCharacter == -2) {
                nextCharacter = networkReader.read();
            }
            if (nextCharacter == -1) {
                throw new EOFException("O adversário encerrou a conexão");
            }
            if (nextCharacter == '\n') {
                try {
                    return GameMessages.readObject(messageText.toString());
                } catch (IllegalArgumentException invalidJson) {
                    throw new IOException("JSON inválido: " + invalidJson.getMessage(), invalidJson);
                }
            }
            if (messageText.length() >= MAX_MESSAGE_CHARACTERS) {
                throw new IOException("Mensagem TCP maior que 16384 caracteres");
            }
            messageText.append((char) nextCharacter);
            nextCharacter = -2;
        }
    }

    private void sendMessage(Map message) throws IOException {
        networkWriter.write(JsonUtil.toJson(message));
        networkWriter.write('\n');
        networkWriter.flush();
    }

    private void processRemoteFailure(Map message) throws IOException {
        if (!"fim".equals(message.get("evento"))) {
            return;
        }
        GameMessages.validateControl(message, sessionIdentifier, "fim");
        String status = GameMessages.readText(message, "status");
        if (status.equals("erro") || status.equals("inatividade")) {
            throw new IOException("Adversário encerrou por " + status);
        }
    }

    private void sendFailureWhenPossible(String status, String reason) {
        System.err.println("Partida encerrada por " + status + ": " + reason);
        if (sessionIdentifier == null) {
            return;
        }
        try {
            Map failure = GameMessages.createFinish(sessionIdentifier, gameRules, status);
            failure.put("motivo", reason);
            sendMessage(failure);
        } catch (IOException notificationFailure) {
            System.err.println("Não foi possível avisar o adversário: " + notificationFailure.getMessage());
        }
    }

    private long newResponseDeadline() {
        return System.nanoTime() + TimeUnit.SECONDS.toNanos(RESPONSE_TIMEOUT_SECONDS);
    }

    private int ensureTimeRemaining(long deadline) throws SocketTimeoutException {
        long remainingNanos = deadline - System.nanoTime();
        if (remainingNanos <= 0) {
            throw new SocketTimeoutException("O adversário não respondeu dentro do prazo");
        }
        return (int) Math.max(1, TimeUnit.NANOSECONDS.toMillis(remainingNanos));
    }

    private void showBoard() {
        int[] board = gameRules.getBoardCopy();
        for (int row = 0; row < 3; row++) {
            String line = "";
            for (int column = 0; column < 3; column++) {
                int position = row * 3 + column;
                String cell = Integer.toString(position);
                if (board[position] != TicTacToeRules.EMPTY_CELL) {
                    cell = symbolName(board[position]);
                }
                if (column > 0) {
                    line += " | ";
                }
                line += cell;
            }
            System.out.println(line);
        }
    }

    private static class TerminalLineReader implements Runnable {
        private BufferedReader terminalReader;
        private String inputText;
        private IOException inputFailure;
        private volatile boolean completed;

        TerminalLineReader(BufferedReader terminalReader) {
            this.terminalReader = terminalReader;
        }

        public void run() {
            try {
                inputText = terminalReader.readLine();
            } catch (IOException readFailure) {
                inputFailure = readFailure;
            } finally {
                completed = true;
            }
        }

        boolean isCompleted() {
            return completed;
        }

        String getInputText() throws IOException {
            if (inputFailure != null) {
                throw inputFailure;
            }
            return inputText;
        }
    }

    private String symbolName(int symbol) {
        if (symbol == TicTacToeRules.X_SYMBOL) {
            return "X";
        }
        return "O";
    }

    private void showResult() {
        if (gameRules.isDraw()) {
            System.out.println("Partida encerrada: empate.");
        } else if (gameRules.getWinner() == localSymbol) {
            System.out.println("Partida encerrada: você venceu.");
        } else {
            System.out.println("Partida encerrada: você perdeu.");
        }
    }
}
