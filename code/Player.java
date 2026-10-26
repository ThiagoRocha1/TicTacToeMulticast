import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;

public class Player {
    private static final int MIN_PORT_NUMBER = 1;
    private static final int MAX_PORT_NUMBER = 65535;
    private static final int CONNECTION_TIMEOUT_MILLIS = 120000;

    public static void main(String[] arguments) {
        try {
            String serverHost = readEnvironmentVariable("RMI_HOST", "152.92.236.14");
            int registryPort = readPort("RMI_REGISTRY_PORT", "9821");
            String serviceName = readEnvironmentVariable("RMI_SERVICE_NAME", "MatchService21");
            requestOpponent(serverHost, registryPort, serviceName);
        } catch (IllegalArgumentException invalidConfiguration) {
            System.err.println("Configuração inválida: " + invalidConfiguration.getMessage());
            System.exit(1);
        } catch (RemoteException communicationFailure) {
            System.err.println("Falha na comunicação RMI: " + communicationFailure.getMessage());
            System.err.println("Se o pedido já foi enviado, o resultado pode ser desconhecido. Não haverá nova tentativa automática.");
            System.exit(1);
        } catch (SocketTimeoutException connectionTimeout) {
            System.err.println("Prazo de conexão ou resposta TCP esgotado: " + connectionTimeout.getMessage());
            System.exit(1);
        } catch (IOException socketFailure) {
            System.err.println("Falha na partida TCP: " + socketFailure.getMessage());
            System.exit(1);
        } catch (NotBoundException missingService) {
            System.err.println("Serviço não encontrado no Registry: " + missingService.getMessage());
            System.exit(1);
        }
    }

    private static void requestOpponent(String serverHost, int registryPort, String serviceName)
            throws IOException, NotBoundException {
        String playerProtocol = readEnvironmentVariable("PLAYER_PROTOCOL", "tcp");
        if (!playerProtocol.equalsIgnoreCase("tcp")) {
            throw new IllegalArgumentException("O jogador implementa somente TCP nesta etapa");
        }
        int playerPort = readPlayerPort();
        InetAddress playerAddress = discoverPlayerAddress(serverHost, registryPort);
        try (ServerSocket listeningSocket = new ServerSocket()) {
            listeningSocket.bind(new InetSocketAddress(playerAddress, playerPort));
            listeningSocket.setSoTimeout(CONNECTION_TIMEOUT_MILLIS);
            Map playerRequest = new HashMap();
            playerRequest.put("ip", playerAddress.getHostAddress());
            playerRequest.put("porta", listeningSocket.getLocalPort());
            playerRequest.put("protocolo", playerProtocol);
            String requestJson = JsonMessages.serializeObject(playerRequest);
            JsonMessages.readPlayerRequest(requestJson);
            System.out.println("Jogador escutando em " + playerAddress.getHostAddress()
                    + ":" + listeningSocket.getLocalPort());
            Registry matchRegistry = LocateRegistry.getRegistry(serverHost, registryPort);
            MatchService matchService = (MatchService) matchRegistry.lookup(serviceName);
            System.out.println("Buscando adversário por RMI...");
            String responseJson = matchService.getOponent(requestJson);
            Map matchResponse;
            try {
                matchResponse = JsonMessages.readMatchResponse(responseJson);
            } catch (IllegalArgumentException invalidResponse) {
                throw new IOException("Resposta de match inválida: " + invalidResponse.getMessage(), invalidResponse);
            }
            if (matchResponse.containsKey("erro")) {
                throw new IOException("Falha no pareamento [" + matchResponse.get("erro")
                        + "]: " + matchResponse.get("mensagem"));
            }
            String opponentAddress = (String) matchResponse.get("ip");
            int opponentPort = Integer.parseInt(matchResponse.get("porta").toString());
            boolean startsMatch = ((Boolean) matchResponse.get("inicia")).booleanValue();
            System.out.println("Oponente encontrado: " + opponentAddress + ":" + opponentPort);
            try (Socket opponentSocket = openOpponentConnection(listeningSocket, opponentAddress, opponentPort, startsMatch)) {
                listeningSocket.close();
                TcpGameSession gameSession = new TcpGameSession(opponentSocket, startsMatch);
                gameSession.play(startsMatch);
            }
        }
    }

    private static InetAddress discoverPlayerAddress(String serverHost, int registryPort) throws IOException {
        String configuredAddress = readEnvironmentVariable("PLAYER_IP", "");
        if (!configuredAddress.isBlank()) {
            return InetAddress.getByName(configuredAddress);
        }
        try (Socket discoverySocket = new Socket()) {
            discoverySocket.connect(new InetSocketAddress(serverHost, registryPort), CONNECTION_TIMEOUT_MILLIS);
            return discoverySocket.getLocalAddress();
        } catch (IOException discoveryFailure) {
            throw new IOException("Não foi possível descobrir o IP local pelo Registry em "
                    + serverHost + ":" + registryPort + ": " + discoveryFailure.getMessage(), discoveryFailure);
        }
    }

    private static int readPlayerPort() {
        String configuredPort = readEnvironmentVariable("PLAYER_PORT", "0");
        if (configuredPort.equals("0")) {
            return 0;
        }
        return readPort("PLAYER_PORT", "0");
    }

    private static Socket openOpponentConnection(ServerSocket listeningSocket, String opponentAddress,
            int opponentPort, boolean startsMatch) throws IOException {
        if (!startsMatch) {
            System.out.println("Aguardando conexão TCP do adversário...");
            Socket acceptedSocket = listeningSocket.accept();
            if (!acceptedSocket.getInetAddress().equals(InetAddress.getByName(opponentAddress))) {
                acceptedSocket.close();
                throw new IOException("Conexão recebida de um IP diferente do adversário");
            }
            return acceptedSocket;
        }
        Socket connectionSocket = new Socket();
        try {
            connectionSocket.connect(new InetSocketAddress(opponentAddress, opponentPort), CONNECTION_TIMEOUT_MILLIS);
            return connectionSocket;
        } catch (IOException connectionFailure) {
            connectionSocket.close();
            throw connectionFailure;
        }
    }

    private static String readEnvironmentVariable(String variableName, String defaultValue) {
        String configuredValue = System.getenv(variableName);
        if (configuredValue != null && !configuredValue.isBlank()) {
            return configuredValue;
        }
        if (defaultValue == null) {
            throw new IllegalArgumentException("Defina a variável " + variableName);
        }
        return defaultValue;
    }

    private static int readPort(String variableName, String defaultValue) {
        String configuredPort = readEnvironmentVariable(variableName, defaultValue);
        int portNumber;
        try {
            portNumber = Integer.parseInt(configuredPort);
        } catch (NumberFormatException invalidPort) {
            throw new IllegalArgumentException(variableName + " deve conter uma porta inteira", invalidPort);
        }
        if (portNumber < MIN_PORT_NUMBER || portNumber > MAX_PORT_NUMBER) {
            throw new IllegalArgumentException(variableName + " deve estar entre 1 e 65535");
        }
        return portNumber;
    }
}
