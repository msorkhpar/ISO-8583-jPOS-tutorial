package com.example.practice;

import org.jpos.iso.ISOBinaryField;
import org.jpos.iso.ISOComponent;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;
import org.jpos.iso.ISOUtil;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

/**
 * Packs a flat ISOMsg as one JSON object, one member per field, and reads such an object back.
 * The partner's side reads it with an ordinary JSON parser.
 */
public class JsonPackager implements ISOPackager {

    @Override
    public byte[] pack(ISOComponent c) throws ISOException {
        ISOMsg m = (ISOMsg) c;
        StringBuilder json = new StringBuilder("{");
        for (Object o : new TreeMap<>(m.getChildren()).values()) {
            ISOComponent field = (ISOComponent) o;
            if ((Integer) field.getKey() < 0)
                continue;                                   // a bitmap kept from another packager
            if (field instanceof ISOMsg)
                throw new ISOException("field " + field.getKey() + ": nested fields are not supported");
            if (json.length() > 1)
                json.append(',');
            json.append('"').append(name(field)).append("\":").append(jsonValue(field));
        }
        return json.append('}').toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public int unpack(ISOComponent c, byte[] b) throws ISOException {
        ISOMsg m = (ISOMsg) c;
        for (Map.Entry<String, String> member : Json.parseObject(new String(b, StandardCharsets.UTF_8)).entrySet())
            put(m, member.getKey(), member.getValue());
        return b.length;
    }

    // ---- the three methods the partner's contract depends on ----

    /** The member name under which this field travels. */
    static String name(ISOComponent field) throws ISOException {
        return field instanceof ISOBinaryField ? field.getKey() + "b" : field.getKey().toString();
    }

    /** The JSON value for this field, exactly as it is written after the colon. */
    static String jsonValue(ISOComponent field) throws ISOException {
        if (field instanceof ISOBinaryField)
            return '"' + ISOUtil.hexString(field.getBytes()) + '"';
        String s = (String) field.getValue();
        StringBuilder out = new StringBuilder("\"");
        for (char ch : s.toCharArray()) {
            if (ch == '"' || ch == '\\')
                out.append('\\').append(ch);
            else if (ch < 0x20)
                out.append(String.format("\\u%04x", (int) ch));
            else
                out.append(ch);
        }
        return out.append('"').toString();
    }

    /** Puts one member read from the JSON back into the message: its name, and its value as text. */
    static void put(ISOMsg m, String name, String text) throws ISOException {
        if (name.endsWith("b"))
            m.set(Integer.parseInt(name.substring(0, name.length() - 1)), ISOUtil.hex2byte(text));
        else
            m.set(Integer.parseInt(name), text);
    }

    // ---- the rest of ISOPackager ----

    @Override
    public void unpack(ISOComponent m, InputStream in) throws ISOException {
        throw new ISOException("stream unpack not supported");
    }

    @Override
    public String getFieldDescription(ISOComponent m, int fldNumber) {
        return "Field " + fldNumber;
    }

    @Override
    public String getDescription() {
        return getClass().getName();
    }

    @Override
    public ISOMsg createISOMsg() {
        return new ISOMsg();
    }

    /** A small JSON reader for one flat object, as an ordinary parser reads it. */
    static final class Json {
        private final String s;
        private int i;

        private Json(String s) {
            this.s = s;
        }

        /** Member names and their values as text: a string's content, or a number as written. */
        static Map<String, String> parseObject(String text) throws ISOException {
            Json p = new Json(text);
            Map<String, String> members = new TreeMap<>();
            p.expect('{');
            if (p.peek() != '}') {
                do {
                    String name = p.string();
                    p.expect(':');
                    members.put(name, p.peek() == '"' ? p.string() : p.number());
                } while (p.accept(','));
            }
            p.expect('}');
            p.skip();
            if (p.i != p.s.length())
                throw new ISOException("unexpected text after the object at " + p.i);
            return members;
        }

        private void skip() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i)))
                i++;
        }

        private char peek() throws ISOException {
            skip();
            if (i >= s.length())
                throw new ISOException("unexpected end of JSON");
            return s.charAt(i);
        }

        private boolean accept(char c) throws ISOException {
            if (peek() != c)
                return false;
            i++;
            return true;
        }

        private void expect(char c) throws ISOException {
            if (!accept(c))
                throw new ISOException("expected '" + c + "' at " + i);
        }

        private String number() throws ISOException {
            skip();
            int start = i;
            while (i < s.length() && "-+.eE0123456789".indexOf(s.charAt(i)) >= 0)
                i++;
            if (start == i)
                throw new ISOException("expected a value at " + i);
            return new java.math.BigDecimal(s.substring(start, i)).toPlainString();
        }

        private String string() throws ISOException {
            expect('"');
            StringBuilder out = new StringBuilder();
            while (true) {
                if (i >= s.length())
                    throw new ISOException("unterminated string");
                char c = s.charAt(i++);
                if (c == '"')
                    return out.toString();
                if (c < 0x20)
                    throw new ISOException("control character in a string at " + (i - 1));
                if (c != '\\') {
                    out.append(c);
                    continue;
                }
                if (i >= s.length())
                    throw new ISOException("unterminated escape");
                char e = s.charAt(i++);
                switch (e) {
                    case '"': case '\\': case '/': out.append(e); break;
                    case 'b': out.append('\b'); break;
                    case 'f': out.append('\f'); break;
                    case 'n': out.append('\n'); break;
                    case 'r': out.append('\r'); break;
                    case 't': out.append('\t'); break;
                    case 'u':
                        if (i + 4 > s.length())
                            throw new ISOException("short \\u escape");
                        out.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        i += 4;
                        break;
                    default:
                        throw new ISOException("bad escape \\" + e);
                }
            }
        }
    }
}
