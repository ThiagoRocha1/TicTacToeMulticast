import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

class JsonMessages {
    static final String ERROR_TIMEOUT = "TIMEOUT";
    static final String ERROR_DUPLICATE = "DUPLICATE";
    static final String ERROR_INVALID_REQUEST = "INVALID_REQUEST";
    static final String ERROR_INTERRUPTED = "INTERRUPTED";
    private static final int MAX_JSON_CHARACTERS = 16384;
    private static final int MIN_PORT_NUMBER = 1;
    private static final int MAX_PORT_NUMBER = 65535;

    static class PlayerRegistration {
        private String ipAddress;
        private int portNumber;
        private String playerProtocol;

        PlayerRegistration(String ipAddress, int portNumber, String playerProtocol) {
            this.ipAddress = ipAddress;
            this.portNumber = portNumber;
            this.playerProtocol = playerProtocol;
        }

        String getIpAddress() {
            return ipAddress;
        }

        int getPortNumber() {
            return portNumber;
        }

        String getProtocol() {
            return playerProtocol;
        }

        boolean hasSameEndpoint(PlayerRegistration otherPlayer) {
            return ipAddress.equals(otherPlayer.ipAddress)
                    && portNumber == otherPlayer.portNumber
                    && playerProtocol.equals(otherPlayer.playerProtocol);
        }
    }

    static PlayerRegistration readPlayerRequest(String requestJson) {
        Map requestObject = readJsonObject(requestJson);
        String ipAddress = readRequiredString(requestObject, "ip");
        String playerProtocol = readRequiredString(requestObject, "protocolo");
        int portNumber = readPlayerPort(requestObject);
        if (!playerProtocol.equals("tcp") && !playerProtocol.equals("udp")) {
            throw new IllegalArgumentException("Protocolo deve ser tcp ou udp");
        }
        return new PlayerRegistration(validateNumericIpAddress(ipAddress), portNumber, playerProtocol);
    }

    static Map readMatchResponse(String responseJson) {
        Map responseObject = readJsonObject(responseJson);
        if (responseObject.containsKey("erro")) {
            String errorCode = readRequiredString(responseObject, "erro");
            readRequiredString(responseObject, "mensagem");
            switch (errorCode) {
                case ERROR_TIMEOUT:
                case ERROR_DUPLICATE:
                case ERROR_INVALID_REQUEST:
                case ERROR_INTERRUPTED:
                    return responseObject;
                default:
                    throw new IllegalArgumentException("Código de erro desconhecido: " + errorCode);
            }
        }
        validateNumericIpAddress(readRequiredString(responseObject, "ip"));
        readPlayerPort(responseObject);
        if (!(responseObject.get("inicia") instanceof Boolean)) {
            throw new IllegalArgumentException("Campo inicia deve ser true ou false");
        }
        return responseObject;
    }

    private static Map readJsonObject(String jsonText) {
        if (jsonText == null || jsonText.length() == 0 || jsonText.length() > MAX_JSON_CHARACTERS) {
            throw new IllegalArgumentException("JSON ausente ou maior que 16384 caracteres");
        }
        Object parsedValue = JsonUtil.parseJson(jsonText);
        if (!(parsedValue instanceof Map)) {
            throw new IllegalArgumentException("Esperado um único objeto JSON");
        }
        return (Map) parsedValue;
    }

    private static String readRequiredString(Map jsonObject, String fieldName) {
        Object fieldValue = jsonObject.get(fieldName);
        if (!(fieldValue instanceof String)) {
            throw new IllegalArgumentException("Campo textual obrigatório: " + fieldName);
        }
        return (String) fieldValue;
    }

    private static int readPlayerPort(Map jsonObject) {
        Object portValue = jsonObject.get("porta");
        if (!(portValue instanceof Number) || !portValue.toString().matches("[0-9]{1,5}")) {
            throw new IllegalArgumentException("Porta deve ser um inteiro entre 1 e 65535");
        }
        int portNumber = Integer.parseInt(portValue.toString());
        if (portNumber < MIN_PORT_NUMBER || portNumber > MAX_PORT_NUMBER) {
            throw new IllegalArgumentException("Porta deve estar entre 1 e 65535");
        }
        return portNumber;
    }

    private static String validateNumericIpAddress(String ipAddress) {
        if (!ipAddress.contains(":")) {
            String[] addressParts = ipAddress.split("\\.", -1);
            if (addressParts.length != 4) {
                throw new IllegalArgumentException("IP numérico inválido");
            }
            for (String addressPart : addressParts) {
                if (!addressPart.matches("0|[1-9][0-9]{0,2}") || Integer.parseInt(addressPart) > 255) {
                    throw new IllegalArgumentException("IP numérico inválido");
                }
            }
        } else if (!ipAddress.matches("[0-9a-fA-F:.]+")) {
            throw new IllegalArgumentException("IP numérico inválido");
        }
        try {
            InetAddress playerAddress = InetAddress.getByName(ipAddress);
            if (playerAddress.isAnyLocalAddress() || playerAddress.isMulticastAddress()) {
                throw new IllegalArgumentException("IP deve identificar um jogador");
            }
            return playerAddress.getHostAddress();
        } catch (UnknownHostException invalidAddress) {
            throw new IllegalArgumentException("IP numérico inválido", invalidAddress);
        }
    }

    static String createOpponentResponse(PlayerRegistration player, boolean startsMatch) {
        Map opponentResponse = new HashMap();
        opponentResponse.put("ip", player.getIpAddress());
        opponentResponse.put("porta", player.getPortNumber());
        opponentResponse.put("inicia", startsMatch);
        return serializeObject(opponentResponse);
    }

    static String createErrorResponse(String errorCode, String errorMessage) {
        Map errorResponse = new HashMap();
        errorResponse.put("erro", errorCode);
        errorResponse.put("mensagem", errorMessage);
        return serializeObject(errorResponse);
    }

    static String serializeObject(Map jsonObject) {
        return JsonUtil.toJson(jsonObject);
    }

    private JsonMessages() {}
}
