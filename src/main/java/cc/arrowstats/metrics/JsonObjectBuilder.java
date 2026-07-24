package cc.arrowstats.metrics;

/**
 * A tiny, dependency-free JSON object writer — just enough to serialise a metrics
 * submission. No third-party JSON library is pulled in, keeping the shaded jar small.
 *
 * Not general-purpose: it only writes flat objects, nested objects, and string/int
 * arrays, which is all the ingest payload needs.
 */
public class JsonObjectBuilder {

    private StringBuilder builder = new StringBuilder();
    private boolean hasAtLeastOneField = false;

    public JsonObjectBuilder() {
        builder.append('{');
    }

    public JsonObjectBuilder appendNull(String key) {
        appendFieldUnescaped(key, "null");
        return this;
    }

    public JsonObjectBuilder appendField(String key, String value) {
        if (value == null) {
            throw new IllegalArgumentException("JSON value must not be null");
        }
        appendFieldUnescaped(key, "\"" + escape(value) + "\"");
        return this;
    }

    public JsonObjectBuilder appendField(String key, int value) {
        appendFieldUnescaped(key, String.valueOf(value));
        return this;
    }

    public JsonObjectBuilder appendField(String key, boolean value) {
        appendFieldUnescaped(key, String.valueOf(value));
        return this;
    }

    public JsonObjectBuilder appendField(String key, JsonObject object) {
        if (object == null) {
            throw new IllegalArgumentException("JSON object must not be null");
        }
        appendFieldUnescaped(key, object.toString());
        return this;
    }

    public JsonObjectBuilder appendField(String key, int[] values) {
        StringBuilder escaped = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                escaped.append(',');
            }
            escaped.append(values[i]);
        }
        escaped.append(']');
        appendFieldUnescaped(key, escaped.toString());
        return this;
    }

    private void appendFieldUnescaped(String key, String escapedValue) {
        if (builder == null) {
            throw new IllegalStateException("JSON object was already built");
        }
        if (key == null) {
            throw new IllegalArgumentException("JSON key must not be null");
        }
        if (hasAtLeastOneField) {
            builder.append(',');
        }
        builder.append('"').append(escape(key)).append("\":").append(escapedValue);
        hasAtLeastOneField = true;
    }

    public JsonObject build() {
        if (builder == null) {
            throw new IllegalStateException("JSON object was already built");
        }
        JsonObject object = new JsonObject(builder.append('}').toString());
        builder = null;
        return object;
    }

    private static String escape(String value) {
        StringBuilder out = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"') {
                out.append("\\\"");
            } else if (c == '\\') {
                out.append("\\\\");
            } else if (c == '\n') {
                out.append("\\n");
            } else if (c == '\r') {
                out.append("\\r");
            } else if (c == '\t') {
                out.append("\\t");
            } else if (c < ' ') {
                out.append(String.format("\\u%04x", (int) c));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /** An immutable, already-serialised JSON object. */
    public static class JsonObject {
        private final String value;

        private JsonObject(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
