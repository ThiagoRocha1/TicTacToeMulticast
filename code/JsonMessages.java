import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

class JsonMessages {
    private static final int MAX_REQUEST_BYTES = 16384;
    private static final int MIN_PORT_NUMBER = 1;
    private static final int MAX_PORT_NUMBER = 65535;

    static class Player {
        private String ipAddress;
        private int portNumber;
        private String playerProtocol;

        Player(String ipAddress, int portNumber, String playerProtocol) {
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

        boolean hasSameEndpoint(Player otherPlayer) {
            return ipAddress.equals(otherPlayer.ipAddress)
                    && portNumber == otherPlayer.portNumber
                    && playerProtocol.equals(otherPlayer.playerProtocol);
        }
    }

    static Player readPlayerRequest(byte[] requestBytes) {
        if (requestBytes == null || requestBytes.length == 0 || requestBytes.length > MAX_REQUEST_BYTES) {
            throw new IllegalArgumentException("JSON ausente ou maior que 16384 bytes");
        }
        try {
            String jsonText = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(requestBytes)).toString();
            Object parsedValue = JsonUtil.parseJson(jsonText);
            if (!(parsedValue instanceof Map)) {
                throw new IllegalArgumentException("Esperado um único objeto JSON");
            }
            Map jsonObject = (Map) parsedValue;
            String ipAddress = readRequiredString(jsonObject, "ip");
            String playerProtocol = readRequiredString(jsonObject, "protocolo");
            int portNumber = readPlayerPort(jsonObject);
            if (!playerProtocol.equals("tcp") && !playerProtocol.equals("udp")) {
                throw new IllegalArgumentException("Protocolo deve ser tcp ou udp");
            }
            return new Player(validateNumericIpAddress(ipAddress), portNumber, playerProtocol);
        } catch (CharacterCodingException invalidEncoding) {
            throw new IllegalArgumentException("JSON ou codificação UTF-8 inválidos", invalidEncoding);
        }
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

    static byte[] createOpponentResponse(Player player, boolean startsMatch) {
        Map opponentResponse = new HashMap();
        opponentResponse.put("ip", player.getIpAddress());
        opponentResponse.put("porta", player.getPortNumber());
        opponentResponse.put("inicia", startsMatch);
        return encodeObject(opponentResponse);
    }

    static byte[] createErrorResponse(String errorCode, String errorMessage) {
        Map errorResponse = new HashMap();
        errorResponse.put("erro", errorCode);
        errorResponse.put("mensagem", errorMessage);
        return encodeObject(errorResponse);
    }

    static byte[] encodeObject(Map jsonObject) {
        return JsonUtil.toJson(jsonObject).getBytes(StandardCharsets.UTF_8);
    }

    private JsonMessages() {}
}
