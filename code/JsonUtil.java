import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class JsonUtil {
    private static final int MAX_NESTING_DEPTH = 32;
    private static final String JSON_NUMBER_PATTERN = "-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?";

    static Object parseJson(String jsonText) {
        JsonParser jsonParser = new JsonParser(jsonText);
        Object jsonValue = jsonParser.readValue(0);
        jsonParser.skipWhitespace();
        if (jsonParser.currentPosition != jsonText.length()) {
            throw jsonParser.invalidJson();
        }
        return jsonValue;
    }

    static String toJson(Object jsonValue) {
        StringBuilder jsonResult = new StringBuilder();
        appendJsonValue(jsonValue, jsonResult, 0);
        return jsonResult.toString();
    }

    private static void appendJsonValue(Object jsonValue, StringBuilder jsonOutput, int nestingDepth) {
        if (nestingDepth > MAX_NESTING_DEPTH) {
            throw new IllegalArgumentException("JSON excede 32 níveis");
        }
        if (jsonValue == null || jsonValue instanceof Boolean) {
            jsonOutput.append(jsonValue);
        } else if (jsonValue instanceof String) {
            appendJsonString((String) jsonValue, jsonOutput);
        } else if (jsonValue instanceof Number) {
            String numberText = jsonValue.toString();
            if (!numberText.matches(JSON_NUMBER_PATTERN)) {
                throw new IllegalArgumentException("Número JSON inválido");
            }
            jsonOutput.append(numberText);
        } else if (jsonValue instanceof Map) {
            appendJsonObject((Map) jsonValue, jsonOutput, nestingDepth);
        } else if (jsonValue instanceof List) {
            appendJsonArray((List) jsonValue, jsonOutput, nestingDepth);
        } else {
            throw new IllegalArgumentException("Tipo não suportado em JSON");
        }
    }

    private static void appendJsonString(String jsonText, StringBuilder jsonOutput) {
        jsonOutput.append('"');
        for (int characterIndex = 0; characterIndex < jsonText.length(); characterIndex++) {
            char currentCharacter = jsonText.charAt(characterIndex);
            switch (currentCharacter) {
                case '"':
                    jsonOutput.append("\\\"");
                    break;
                case '\\':
                    jsonOutput.append("\\\\");
                    break;
                case '\b':
                    jsonOutput.append("\\b");
                    break;
                case '\f':
                    jsonOutput.append("\\f");
                    break;
                case '\n':
                    jsonOutput.append("\\n");
                    break;
                case '\r':
                    jsonOutput.append("\\r");
                    break;
                case '\t':
                    jsonOutput.append("\\t");
                    break;
                default: {
                    if (currentCharacter < 32 || Character.isSurrogate(currentCharacter)) {
                        jsonOutput.append(String.format("\\u%04x", (int) currentCharacter));
                    } else {
                        jsonOutput.append(currentCharacter);
                    }
                    break;
                }
            }
        }
        jsonOutput.append('"');
    }

    private static void appendJsonObject(Map jsonObject, StringBuilder jsonOutput, int nestingDepth) {
        jsonOutput.append('{');
        boolean firstProperty = true;
        for (Object propertyObject : jsonObject.entrySet()) {
            Map.Entry property = (Map.Entry) propertyObject;
            if (!(property.getKey() instanceof String)) {
                throw new IllegalArgumentException("Chave JSON deve ser texto");
            }
            if (!firstProperty) {
                jsonOutput.append(',');
            }
            appendJsonValue(property.getKey(), jsonOutput, nestingDepth + 1);
            jsonOutput.append(':');
            appendJsonValue(property.getValue(), jsonOutput, nestingDepth + 1);
            firstProperty = false;
        }
        jsonOutput.append('}');
    }

    private static void appendJsonArray(List jsonArray, StringBuilder jsonOutput, int nestingDepth) {
        jsonOutput.append('[');
        for (int elementIndex = 0; elementIndex < jsonArray.size(); elementIndex++) {
            if (elementIndex > 0) {
                jsonOutput.append(',');
            }
            appendJsonValue(jsonArray.get(elementIndex), jsonOutput, nestingDepth + 1);
        }
        jsonOutput.append(']');
    }

    private static class JsonParser {
        private String jsonText;
        private int currentPosition;

        JsonParser(String jsonText) {
            if (jsonText == null) {
                throw new IllegalArgumentException("JSON ausente");
            }
            this.jsonText = jsonText;
        }

        IllegalArgumentException invalidJson() {
            return new IllegalArgumentException("JSON inválido na posição " + currentPosition);
        }

        void skipWhitespace() {
            while (currentPosition < jsonText.length() && " \t\r\n".indexOf(jsonText.charAt(currentPosition)) >= 0) {
                currentPosition++;
            }
        }

        boolean consumeCharacter(char currentCharacter) {
            if (currentPosition < jsonText.length() && jsonText.charAt(currentPosition) == currentCharacter) {
                currentPosition++;
                return true;
            }
            return false;
        }

        void requireCharacter(char currentCharacter) {
            skipWhitespace();
            if (!consumeCharacter(currentCharacter)) {
                throw invalidJson();
            }
        }

        Object readValue(int nestingDepth) {
            skipWhitespace();
            if (nestingDepth > MAX_NESTING_DEPTH || currentPosition == jsonText.length()) {
                throw invalidJson();
            }
            char currentCharacter = jsonText.charAt(currentPosition);
            if (currentCharacter == '"') {
                return readString();
            }
            if (consumeCharacter('{')) {
                return readObject(nestingDepth);
            }
            if (consumeCharacter('[')) {
                return readArray(nestingDepth);
            }
            for (String literalText : new String[]{"true", "false", "null"}) {
                if (jsonText.startsWith(literalText, currentPosition)) {
                    currentPosition += literalText.length();
                    if (literalText.equals("null")) {
                        return null;
                    }
                    return Boolean.valueOf(literalText);
                }
            }
            return readNumber();
        }

        private Map readObject(int nestingDepth) {
            Map jsonObject = new LinkedHashMap();
            skipWhitespace();
            if (consumeCharacter('}')) {
                return jsonObject;
            }
            do {
                skipWhitespace();
                String propertyName = readString();
                requireCharacter(':');
                if (jsonObject.containsKey(propertyName)) {
                    throw invalidJson();
                }
                jsonObject.put(propertyName, readValue(nestingDepth + 1));
                skipWhitespace();
                if (consumeCharacter('}')) {
                    return jsonObject;
                }
                requireCharacter(',');
            } while (true);
        }

        private List readArray(int nestingDepth) {
            List jsonArray = new ArrayList();
            skipWhitespace();
            if (consumeCharacter(']')) {
                return jsonArray;
            }
            do {
                jsonArray.add(readValue(nestingDepth + 1));
                skipWhitespace();
                if (consumeCharacter(']')) {
                    return jsonArray;
                }
                requireCharacter(',');
            } while (true);
        }

        private BigDecimal readNumber() {
            int numberStartPosition = currentPosition;
            while (currentPosition < jsonText.length() && "-+0123456789.eE".indexOf(jsonText.charAt(currentPosition)) >= 0) {
                currentPosition++;
            }
            String numericValue = jsonText.substring(numberStartPosition, currentPosition);
            if (!numericValue.matches(JSON_NUMBER_PATTERN)) {
                throw invalidJson();
            }
            try {
                return new BigDecimal(numericValue);
            } catch (NumberFormatException invalidNumber) {
                throw invalidJson();
            }
        }

        private char readUnicodeCharacter() {
            if (currentPosition + 4 > jsonText.length()) {
                throw invalidJson();
            }
            int unicodeValue = 0;
            for (int characterIndex = 0; characterIndex < 4; characterIndex++) {
                char hexCharacter = jsonText.charAt(currentPosition++);
                int hexDigit = "0123456789abcdef".indexOf(Character.toLowerCase(hexCharacter));
                if (hexDigit < 0) {
                    throw invalidJson();
                }
                unicodeValue = unicodeValue * 16 + hexDigit;
            }
            return (char) unicodeValue;
        }

        String readString() {
            if (!consumeCharacter('"')) {
                throw invalidJson();
            }
            StringBuilder jsonResult = new StringBuilder();
            while (currentPosition < jsonText.length()) {
                char currentCharacter = jsonText.charAt(currentPosition++);
                if (currentCharacter == '"') {
                    return jsonResult.toString();
                }
                if (currentCharacter < 32) {
                    throw invalidJson();
                }
                if (currentCharacter != '\\') {
                    jsonResult.append(currentCharacter);
                    continue;
                }
                if (currentPosition == jsonText.length()) {
                    throw invalidJson();
                }
                char escapeCharacter = jsonText.charAt(currentPosition++);
                switch (escapeCharacter) {
                    case '"':
                    case '\\':
                    case '/':
                        jsonResult.append(escapeCharacter);
                        break;
                    case 'b':
                        jsonResult.append('\b');
                        break;
                    case 'f':
                        jsonResult.append('\f');
                        break;
                    case 'n':
                        jsonResult.append('\n');
                        break;
                    case 'r':
                        jsonResult.append('\r');
                        break;
                    case 't':
                        jsonResult.append('\t');
                        break;
                    case 'u':
                        jsonResult.append(readUnicodeCharacter());
                        break;
                    default:
                        throw invalidJson();
                }
            }
            throw invalidJson();
        }
    }

    private JsonUtil() {}
}
