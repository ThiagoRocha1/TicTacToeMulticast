import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import java.time.Duration;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MatchServer implements MatchService, Runnable {

    private static final int DEFAULT_MATCH_TIMEOUT_SECONDS = 300;
    private static final int MIN_PORT_NUMBER = 1;
    private static final int MAX_PORT_NUMBER = 65535;

    private Object matchmakingMonitor = new Object();
    private Map requestsByProtocol = new HashMap();
    private long matchTimeoutNanoseconds;
    private Registry matchRegistry;

    private static class PendingMatchRequest {

        JsonMessages.PlayerRegistration player;
        long expirationNanoseconds;
        String responseJson;

        PendingMatchRequest(JsonMessages.PlayerRegistration player, long matchTimeoutNanoseconds) {
            this.player = player;
            expirationNanoseconds = System.nanoTime() + matchTimeoutNanoseconds;
        }
    }

    public MatchServer(Duration matchTimeout) {
        try {
            matchTimeoutNanoseconds = matchTimeout.toNanos();
        } catch (ArithmeticException unsupportedDuration) {
            throw new IllegalArgumentException("Timeout não pode ser representado em nanossegundos", unsupportedDuration);
        }

        if (matchTimeoutNanoseconds <= 0) {
            throw new IllegalArgumentException("Timeout deve ser positivo");
        }

        requestsByProtocol.put("tcp", new ArrayDeque());
        requestsByProtocol.put("udp", new ArrayDeque());
    }

    @Override
    public String getOponent(String requestJson) {
        JsonMessages.PlayerRegistration requestingPlayer;

        try {
            requestingPlayer = JsonMessages.readPlayerRequest(requestJson);
        } catch (IllegalArgumentException invalidRequest) {
            return JsonMessages.createErrorResponse(JsonMessages.ERROR_INVALID_REQUEST, invalidRequest.getMessage());
        }

        PendingMatchRequest currentRequest = new PendingMatchRequest(requestingPlayer, matchTimeoutNanoseconds);
        ArrayDeque protocolRequests = (ArrayDeque) requestsByProtocol.get(requestingPlayer.getProtocol());

        synchronized (matchmakingMonitor) {
            Iterator requestIterator = protocolRequests.iterator();

            while (requestIterator.hasNext()) {
                PendingMatchRequest pendingRequest = (PendingMatchRequest) requestIterator.next();

                if (System.nanoTime() - pendingRequest.expirationNanoseconds >= 0) {
                    pendingRequest.responseJson = createTimeoutResponse();
                    requestIterator.remove();
                    matchmakingMonitor.notifyAll();
                }
            }

            for (Object queuedRequest : protocolRequests) {
                PendingMatchRequest pendingRequest = (PendingMatchRequest) queuedRequest;

                if (pendingRequest.player.hasSameEndpoint(requestingPlayer)) {
                    return JsonMessages.createErrorResponse(JsonMessages.ERROR_DUPLICATE, "Jogador já está aguardando um oponente");
                }
            }

            PendingMatchRequest opponentRequest = (PendingMatchRequest) protocolRequests.pollFirst();

            if (opponentRequest == null) {
                protocolRequests.addLast(currentRequest);
            } else {
                opponentRequest.responseJson = JsonMessages.createOpponentResponse(requestingPlayer, true);
                currentRequest.responseJson = JsonMessages.createOpponentResponse(opponentRequest.player, false);
                matchmakingMonitor.notifyAll();
            }

            while (currentRequest.responseJson == null) {
                long remainingNanoseconds = currentRequest.expirationNanoseconds - System.nanoTime();

                if (remainingNanoseconds <= 0) {
                    protocolRequests.remove(currentRequest);
                    currentRequest.responseJson = createTimeoutResponse();
                    break;
                }

                try {
                    TimeUnit.NANOSECONDS.timedWait(matchmakingMonitor, remainingNanoseconds);
                } catch (InterruptedException interruptedWait) {
                    if (currentRequest.responseJson == null) {
                        protocolRequests.remove(currentRequest);
                        currentRequest.responseJson = JsonMessages.createErrorResponse(JsonMessages.ERROR_INTERRUPTED, "Espera interrompida");
                    }

                    Thread.currentThread().interrupt();
                    break;
                }
            }

            return currentRequest.responseJson;
        }
    }

    private String createTimeoutResponse() {
        return JsonMessages.createErrorResponse(JsonMessages.ERROR_TIMEOUT, "Nenhum oponente encontrado no prazo de "
                + Duration.ofNanos(matchTimeoutNanoseconds).toMillis() + " ms");
    }

    public static void main(String[] arguments) {
        try {
            String serverHost = readEnvironmentVariable("RMI_HOST", "152.92.236.14");
            int registryPort = readPort("RMI_REGISTRY_PORT", "9821");
            String serviceName = readEnvironmentVariable("RMI_SERVICE_NAME", "MatchService21");
            startMatchServer(serverHost, registryPort, serviceName);
        } catch (IllegalArgumentException invalidConfiguration) {
            System.err.println("Configuração inválida: " + invalidConfiguration.getMessage());
            System.exit(1);
        } catch (RemoteException serverFailure) {
            System.err.println("Falha ao iniciar o servidor RMI: " + serverFailure.getMessage());
            System.exit(1);
        } catch (InterruptedException interruptedServer) {
            Thread.currentThread().interrupt();
            System.err.println("Espera do servidor interrompida. Encerrando os recursos RMI.");
            System.exit(1);
        }
    }

    private static void startMatchServer(String serverHost, int registryPort, String serviceName)
            throws RemoteException, InterruptedException {
        int remoteObjectPort = readPort("RMI_OBJECT_PORT", null);
        long matchTimeoutSeconds = readMatchTimeoutSeconds();

        System.setProperty("java.rmi.server.hostname", serverHost);

        MatchServer matchServer = new MatchServer(Duration.ofSeconds(matchTimeoutSeconds));
        MatchService remoteService = (MatchService) UnicastRemoteObject.exportObject(matchServer, remoteObjectPort);
        Registry matchRegistry;

        try {
            matchRegistry = LocateRegistry.createRegistry(registryPort);
        } catch (RemoteException registryFailure) {
            closeRemoteObject(matchServer, "servidor de match");
            throw registryFailure;
        }

        try {
            matchRegistry.rebind(serviceName, remoteService);
        } catch (RemoteException registrationFailure) {
            closeRemoteObject(matchRegistry, "Registry");
            closeRemoteObject(matchServer, "servidor de match");
            throw registrationFailure;
        }

        matchServer.matchRegistry = matchRegistry;

        Runtime.getRuntime().addShutdownHook(new Thread(matchServer, "encerramento-rmi"));

        System.out.printf("Registry %s:%d, serviço %s, objeto remoto na porta %d%n",
                serverHost, registryPort, serviceName, remoteObjectPort);

        new CountDownLatch(1).await();
    }

    @Override
    public void run() {
        shutdownServer();
    }

    private void shutdownServer() {
        closeRemoteObject(matchRegistry, "Registry");
        closeRemoteObject(this, "servidor de match");
    }

    private static void closeRemoteObject(Remote remoteObject, String resourceName) {
        try {
            UnicastRemoteObject.unexportObject(remoteObject, true);
        } catch (NoSuchObjectException alreadyClosedResource) {
            System.err.println("Recurso RMI já encerrado: " + resourceName);
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

    private static long readMatchTimeoutSeconds() {
        String configuredTimeout = readEnvironmentVariable("MATCH_TIMEOUT_SECONDS",
                Integer.toString(DEFAULT_MATCH_TIMEOUT_SECONDS));
        long timeoutSeconds;

        try {
            timeoutSeconds = Long.parseLong(configuredTimeout);
        } catch (NumberFormatException invalidTimeout) {
            throw new IllegalArgumentException("MATCH_TIMEOUT_SECONDS deve conter um inteiro positivo", invalidTimeout);
        }

        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("MATCH_TIMEOUT_SECONDS deve ser positivo");
        }

        return timeoutSeconds;
    }
}
