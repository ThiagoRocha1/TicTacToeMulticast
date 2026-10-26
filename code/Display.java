import java.io.IOException;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.StandardSocketOptions;

import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import java.util.HashMap;
import java.util.Map;

public class Display implements AutoCloseable {

    private static final int MAX_DATAGRAM_BYTES = 16384;
    private static final int MIN_PORT_NUMBER = 1;
    private static final int MAX_PORT_NUMBER = 65535;
    private static final int MAX_MULTICAST_TTL = 255;

    private MulticastSocket multicastSocket;
    private InetSocketAddress groupAddress;
    private NetworkInterface networkInterface;
    private int multicastTtl;

    private Display() throws IOException {
        InetAddress group = InetAddress.getByName(readSetting("MULTICAST_GROUP", "225.1.1.21"));

        if (!group.isMulticastAddress()) {
            throw new IllegalArgumentException("MULTICAST_GROUP deve ser um endereço multicast");
        }

        int groupPort = readIntegerSetting("MULTICAST_PORT", "8821", MIN_PORT_NUMBER, MAX_PORT_NUMBER);
        groupAddress = new InetSocketAddress(group, groupPort);

        String interfaceName = readSetting("MULTICAST_INTERFACE", null);
        networkInterface = NetworkInterface.getByName(interfaceName);

        if (networkInterface == null || !networkInterface.isUp()) {
            throw new IllegalArgumentException("Interface multicast ausente ou inativa: " + interfaceName);
        }

        multicastTtl = readIntegerSetting("MULTICAST_TTL", "1", 0, MAX_MULTICAST_TTL);
    }

    static Display openPublisher() {
        Display channel = null;

        try {
            channel = new Display();
            channel.openSocket(false);

            return channel;
        } catch (IOException multicastFailure) {
            System.err.println("Publicação multicast indisponível: " + multicastFailure.getMessage());
        } catch (IllegalArgumentException invalidConfiguration) {
            System.err.println("Publicação multicast indisponível: " + invalidConfiguration.getMessage());
        }

        if (channel != null) {
            channel.close();
        }

        return null;
    }

    static Display openReceiver() throws IOException {
        Display channel = new Display();

        try {
            channel.openSocket(true);

            return channel;
        } catch (IOException setupFailure) {
            channel.close();
            throw setupFailure;
        }
    }

    private void openSocket(boolean receivesMessages) throws IOException {
        multicastSocket = new MulticastSocket(null);
        multicastSocket.setReuseAddress(true);

        if (receivesMessages) {
            multicastSocket.bind(new InetSocketAddress(groupAddress.getPort()));
        } else {
            multicastSocket.bind(new InetSocketAddress(0));
        }

        multicastSocket.setNetworkInterface(networkInterface);
        multicastSocket.setTimeToLive(multicastTtl);
        multicastSocket.setOption(StandardSocketOptions.IP_MULTICAST_LOOP, true);

        if (receivesMessages) {
            multicastSocket.joinGroup(groupAddress, networkInterface);
            System.out.println("Display escutando " + groupAddress.getAddress().getHostAddress()
                    + ":" + groupAddress.getPort() + " pela interface " + networkInterface.getName());
        }
    }

    void publishMove(String sessionIdentifier, Map moveMessage) {
        if (multicastSocket == null || multicastSocket.isClosed()) {
            return;
        }

        Map displayMessage = new HashMap();
        displayMessage.put("partidaId", sessionIdentifier);
        displayMessage.put("jogada", moveMessage);

        byte[] messageBytes = JsonUtil.toJson(displayMessage).getBytes(StandardCharsets.UTF_8);

        if (messageBytes.length > MAX_DATAGRAM_BYTES) {
            System.err.println("Jogada não publicada: mensagem multicast excede 16384 bytes");
            return;
        }

        DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, groupAddress);

        try {
            multicastSocket.send(packet);
        } catch (IOException sendFailure) {
            System.err.println("Falha ao publicar jogada; partida continua: " + sendFailure.getMessage());
            close();
        }
    }

    DatagramPacket receivePacket() throws IOException {
        byte[] receiveBuffer = new byte[MAX_DATAGRAM_BYTES + 1];
        DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        multicastSocket.receive(packet);

        return packet;
    }

    static Map readPacket(DatagramPacket packet) throws IOException {
        if (packet.getLength() > MAX_DATAGRAM_BYTES) {
            throw new IllegalArgumentException("Datagrama maior que 16384 bytes");
        }

        String jsonText = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(packet.getData(), packet.getOffset(), packet.getLength())).toString();

        return GameMessages.readObject(jsonText);
    }

    public void close() {
        if (multicastSocket != null) {
            multicastSocket.close();
        }
    }

    private static String readSetting(String settingName, String defaultValue) {
        String configuredValue = System.getenv(settingName);

        if (configuredValue != null && !configuredValue.isBlank()) {
            return configuredValue;
        }

        if (defaultValue == null) {
            throw new IllegalArgumentException("Defina a variável " + settingName);
        }

        return defaultValue;
    }

    private static int readIntegerSetting(String settingName, String defaultValue, int minimumValue, int maximumValue) {
        String configuredValue = readSetting(settingName, defaultValue);
        int parsedValue;

        try {
            parsedValue = Integer.parseInt(configuredValue);
        } catch (NumberFormatException invalidNumber) {
            throw new IllegalArgumentException(settingName + " deve conter um inteiro", invalidNumber);
        }

        if (parsedValue < minimumValue || parsedValue > maximumValue) {
            throw new IllegalArgumentException(settingName + " deve estar entre " + minimumValue + " e " + maximumValue);
        }

        return parsedValue;
    }

    public static void main(String[] arguments) {
        try (Display channel = Display.openReceiver()) {
            while (true) {
                DatagramPacket packet = channel.receivePacket();

                try {
                    Map displayMessage = Display.readPacket(packet);
                    String sessionIdentifier = GameMessages.readText(displayMessage, "partidaId");
                    Object moveValue = displayMessage.get("jogada");

                    if (!(moveValue instanceof Map)) {
                        throw new IllegalArgumentException("Campo objeto obrigatório: jogada");
                    }

                    Map moveMessage = (Map) moveValue;
                    int[] boardValues = GameMessages.readDisplayBoard(moveMessage);
                    showMove(sessionIdentifier, moveMessage, boardValues, packet.getAddress().getHostAddress());
                } catch (IllegalArgumentException invalidMessage) {
                    System.err.println("Datagrama descartado: " + invalidMessage.getMessage());
                } catch (IOException invalidEncoding) {
                    System.err.println("Datagrama descartado: UTF-8 inválido");
                }
            }
        } catch (IllegalArgumentException invalidConfiguration) {
            System.err.println("Configuração multicast inválida: " + invalidConfiguration.getMessage());
            System.exit(1);
        } catch (IOException receiveFailure) {
            System.err.println("Falha no display multicast: " + receiveFailure.getMessage());
            System.exit(1);
        }
    }

    private static void showMove(String sessionIdentifier, Map moveMessage, int[] boardValues, String sourceAddress) {
        System.out.println();
        System.out.println("Partida: " + sessionIdentifier);
        System.out.println("Jogada " + moveMessage.get("Numero") + ", casa " + moveMessage.get("Jogada")
                + " (publicada por " + sourceAddress + ")");

        for (int row = 0; row < 3; row++) {
            String rowText = "";

            for (int column = 0; column < 3; column++) {
                int position = row * 3 + column;
                String cellText = Integer.toString(position);

                if (boardValues[position] == TicTacToeRules.X_SYMBOL) {
                    cellText = "X";
                } else if (boardValues[position] == TicTacToeRules.O_SYMBOL) {
                    cellText = "O";
                }

                if (column > 0) {
                    rowText += " | ";
                }

                rowText += cellText;
            }

            System.out.println(rowText);
        }

        String result = GameMessages.readText(moveMessage, "Resultado");

        if (result.equals("vit")) {
            String winnerSymbol = "O";

            if (TicTacToeRules.hasWinningLine(boardValues, TicTacToeRules.X_SYMBOL)) {
                winnerSymbol = "X";
            }

            System.out.println("Vitória de " + winnerSymbol + ".");
        } else if (result.equals("emp")) {
            System.out.println("Empate.");
        }
    }
}
