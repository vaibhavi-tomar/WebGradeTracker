import java.util.*;

/**
 * A minimal, dependency-free JSON reader/writer.
 * We avoid pulling in an external library (like Gson) so the whole
 * backend runs with nothing but the plain JDK.
 */
public class Json {

    private final String src;
    private int pos;

    private Json(String src) {
        this.src = src;
        this.pos = 0;
    }

    public static Object parse(String text) {
        Json parser = new Json(text);
        parser.skipWhitespace();
        return parser.parseValue();
    }

    private void skipWhitespace() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
    }

    private Object parseValue() {
        skipWhitespace();
        char c = src.charAt(pos);
        if (c == '{') return parseObject();
        if (c == '[') return parseArray();
        if (c == '"') return parseString();
        if (c == 't') { pos += 4; return Boolean.TRUE; }
        if (c == 'f') { pos += 5; return Boolean.FALSE; }
        if (c == 'n') { pos += 4; return null; }
        return parseNumber();
    }

    private Map<String, Object> parseObject() {
        Map<String, Object> map = new LinkedHashMap<>();
        pos++; // consume '{'
        skipWhitespace();
        if (src.charAt(pos) == '}') { pos++; return map; }
        while (true) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            pos++; // consume ':'
            Object value = parseValue();
            map.put(key, value);
            skipWhitespace();
            char next = src.charAt(pos);
            pos++;
            if (next == '}') break;
        }
        return map;
    }

    private List<Object> parseArray() {
        List<Object> list = new ArrayList<>();
        pos++; // consume '['
        skipWhitespace();
        if (src.charAt(pos) == ']') { pos++; return list; }
        while (true) {
            list.add(parseValue());
            skipWhitespace();
            char next = src.charAt(pos);
            pos++;
            if (next == ']') break;
        }
        return list;
    }

    private String parseString() {
        pos++; // consume opening quote
        StringBuilder sb = new StringBuilder();
        while (src.charAt(pos) != '"') {
            char c = src.charAt(pos);
            if (c == '\\') {
                pos++;
                char escaped = src.charAt(pos);
                switch (escaped) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    default: sb.append(escaped);
                }
            } else {
                sb.append(c);
            }
            pos++;
        }
        pos++; // consume closing quote
        return sb.toString();
    }

    private Double parseNumber() {
        int start = pos;
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (Character.isDigit(c) || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                pos++;
            } else {
                break;
            }
        }
        return Double.parseDouble(src.substring(start, pos));
    }

    // ---------- Writing (Java objects -> JSON text) ----------

    public static String stringify(Object obj) {
        StringBuilder sb = new StringBuilder();
        writeValue(obj, sb);
        return sb.toString();
    }

    private static void writeValue(Object obj, StringBuilder sb) {
        if (obj == null) {
            sb.append("null");
        } else if (obj instanceof String) {
            writeString((String) obj, sb);
        } else if (obj instanceof Number || obj instanceof Boolean) {
            sb.append(obj.toString());
        } else if (obj instanceof Map) {
            writeObject((Map<?, ?>) obj, sb);
        } else if (obj instanceof List) {
            writeArray((List<?>) obj, sb);
        } else {
            writeString(obj.toString(), sb);
        }
    }

    private static void writeString(String str, StringBuilder sb) {
        sb.append('"');
        for (char c : str.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        sb.append('"');
    }

    private static void writeObject(Map<?, ?> map, StringBuilder sb) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            writeString(String.valueOf(entry.getKey()), sb);
            sb.append(':');
            writeValue(entry.getValue(), sb);
        }
        sb.append('}');
    }

    private static void writeArray(List<?> list, StringBuilder sb) {
        sb.append('[');
        boolean first = true;
        for (Object o : list) {
            if (!first) sb.append(',');
            first = false;
            writeValue(o, sb);
        }
        sb.append(']');
    }
}
