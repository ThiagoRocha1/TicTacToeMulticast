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
            String rmiRegistryHost = readEnvironmentVariable("RMI_HOST", "152.92.236.14");
            int rmiRegistryPort = readPort("RMI_REGISTRY_PORT", "9821");
            String matchServiceName = readEnvironmentVariable("RMI_SERVICE_NAME", "MatchService21");

            findOpponentAndStartGame(rmiRegistryHost, rmiRegistryPort, matchServiceName);
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

    private static void findOpponentAndStartGame(String rmiRegistryHost, int rmiRegistryPort, String matchServiceName)
            throws IOException, NotBoundException {
        String localPlayerProtocol = readEnvironmentVariable("PLAYER_PROTOCOL", "tcp");

        if (!localPlayerProtocol.equalsIgnoreCase("tcp")) {
            throw new IllegalArgumentException("O jogador implementa somente TCP nesta etapa");
        }

        int localPlayerRequestedPort = readLocalPlayerRequestedPort();
        InetAddress localPlayerAddress = discoverLocalPlayerAddress(rmiRegistryHost, rmiRegistryPort);

        try (ServerSocket localPlayerListeningSocket = new ServerSocket()) {
            localPlayerListeningSocket.bind(new InetSocketAddress(localPlayerAddress, localPlayerRequestedPort));
            localPlayerListeningSocket.setSoTimeout(CONNECTION_TIMEOUT_MILLIS);

            Map localPlayerMatchRequest = new HashMap();
            localPlayerMatchRequest.put("ip", localPlayerAddress.getHostAddress());
            localPlayerMatchRequest.put("porta", localPlayerListeningSocket.getLocalPort());
            localPlayerMatchRequest.put("protocolo", localPlayerProtocol);

            String localPlayerMatchRequestJson = JsonMessages.serializeObject(localPlayerMatchRequest);
            JsonMessages.readPlayerRequest(localPlayerMatchRequestJson);

            System.out.println("Jogador escutando em " + localPlayerAddress.getHostAddress()
                    + ":" + localPlayerListeningSocket.getLocalPort());

            Registry rmiRegistry = LocateRegistry.getRegistry(rmiRegistryHost, rmiRegistryPort);
            MatchService matchService = (MatchService) rmiRegistry.lookup(matchServiceName);

            System.out.println("Buscando adversário por RMI...");
            String matchServerResponseJson = matchService.getOponent(localPlayerMatchRequestJson);
            Map matchServerResponse;

            try {
                matchServerResponse = JsonMessages.readMatchResponse(matchServerResponseJson);
            } catch (IllegalArgumentException invalidResponse) {
                throw new IOException("Resposta de match inválida: " + invalidResponse.getMessage(), invalidResponse);
            }

            if (matchServerResponse.containsKey("erro")) {
                throw new IOException("Falha no pareamento [" + matchServerResponse.get("erro")
                        + "]: " + matchServerResponse.get("mensagem"));
            }

            String opponentIpAddress = (String) matchServerResponse.get("ip");
            int opponentListeningPort = Integer.parseInt(matchServerResponse.get("porta").toString());
            boolean localPlayerStartsMatch = ((Boolean) matchServerResponse.get("inicia")).booleanValue();

            System.out.println("Oponente encontrado: " + opponentIpAddress + ":" + opponentListeningPort);

            try (Socket opponentConnectionSocket = openConnectionWithOpponent(localPlayerListeningSocket,
                    opponentIpAddress, opponentListeningPort, localPlayerStartsMatch)) {
                localPlayerListeningSocket.close();

                TcpGameSession gameSession = new TcpGameSession(opponentConnectionSocket, localPlayerStartsMatch);
                gameSession.play(localPlayerStartsMatch);
            }
        }
    }

    private static InetAddress discoverLocalPlayerAddress(String rmiRegistryHost, int rmiRegistryPort) throws IOException {
        String configuredLocalPlayerAddress = readEnvironmentVariable("PLAYER_IP", "");

        if (!configuredLocalPlayerAddress.isBlank()) {
            return InetAddress.getByName(configuredLocalPlayerAddress);
        }

        try (Socket rmiRegistryDiscoverySocket = new Socket()) {
            rmiRegistryDiscoverySocket.connect(new InetSocketAddress(rmiRegistryHost, rmiRegistryPort),
                    CONNECTION_TIMEOUT_MILLIS);

            return rmiRegistryDiscoverySocket.getLocalAddress();
        } catch (IOException discoveryFailure) {
            throw new IOException("Não foi possível descobrir o IP local pelo Registry em "
                    + rmiRegistryHost + ":" + rmiRegistryPort + ": " + discoveryFailure.getMessage(), discoveryFailure);
        }
    }

    private static int readLocalPlayerRequestedPort() {
        String configuredLocalPlayerPort = readEnvironmentVariable("PLAYER_PORT", "0");

        if (configuredLocalPlayerPort.equals("0")) {
            return 0;
        }

        return readPort("PLAYER_PORT", "0");
    }

    private static Socket openConnectionWithOpponent(ServerSocket localPlayerListeningSocket, String opponentIpAddress,
            int opponentListeningPort, boolean localPlayerStartsMatch) throws IOException {
        if (!localPlayerStartsMatch) {
            System.out.println("Aguardando conexão TCP do adversário...");
            Socket acceptedOpponentSocket = localPlayerListeningSocket.accept();

            if (!acceptedOpponentSocket.getInetAddress().equals(InetAddress.getByName(opponentIpAddress))) {
                acceptedOpponentSocket.close();
                throw new IOException("Conexão recebida de um IP diferente do adversário");
            }

            return acceptedOpponentSocket;
        }

        Socket outgoingOpponentSocket = new Socket();

        try {
            outgoingOpponentSocket.connect(new InetSocketAddress(opponentIpAddress, opponentListeningPort),
                    CONNECTION_TIMEOUT_MILLIS);

            return outgoingOpponentSocket;
        } catch (IOException connectionFailure) {
            outgoingOpponentSocket.close();
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
