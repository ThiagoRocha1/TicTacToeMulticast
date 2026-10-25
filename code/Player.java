import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;

public class Player {
    private static final int MIN_PORT_NUMBER = 1;
    private static final int MAX_PORT_NUMBER = 65535;

    // Nesta etapa o jogador apenas busca um adversário por RMI.
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
        } catch (NotBoundException missingService) {
            System.err.println("Serviço não encontrado no Registry: " + missingService.getMessage());
            System.exit(1);
        }
    }

    private static void requestOpponent(String serverHost, int registryPort, String serviceName)
            throws RemoteException, NotBoundException {
        // A leitura e validação da configuração precedem o acesso à rede.
        Map playerRequest = new HashMap();
        playerRequest.put("ip", readEnvironmentVariable("PLAYER_IP", null));
        playerRequest.put("porta", readPort("PLAYER_PORT", null));
        playerRequest.put("protocolo", readEnvironmentVariable("PLAYER_PROTOCOL", "tcp"));
        String requestJson = JsonMessages.serializeObject(playerRequest);

        Registry matchRegistry = LocateRegistry.getRegistry(serverHost, registryPort);
        MatchService matchService = (MatchService) matchRegistry.lookup(serviceName);
        String responseJson = matchService.getOponent(requestJson);
        displayMatchResponse(responseJson);
    }

    private static void displayMatchResponse(String responseJson) {
        Map matchResponse;
        try {
            matchResponse = JsonMessages.readMatchResponse(responseJson);
        } catch (IllegalArgumentException invalidResponse) {
            System.err.println("Resposta de match inválida: " + invalidResponse.getMessage());
            System.exit(1);
            return;
        }
        if (matchResponse.containsKey("erro")) {
            String errorCode = (String) matchResponse.get("erro");
            String errorMessage = (String) matchResponse.get("mensagem");
            System.err.println("Falha no pareamento [" + errorCode + "]: " + errorMessage);
            System.exit(1);
            return;
        }
        System.out.println("Oponente encontrado: " + matchResponse.get("ip")
                + ":" + matchResponse.get("porta"));
        boolean startsMatch = ((Boolean) matchResponse.get("inicia")).booleanValue();
        if (startsMatch) {
            System.out.println("Você inicia a partida.");
        } else {
            System.out.println("O adversário inicia a partida.");
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
