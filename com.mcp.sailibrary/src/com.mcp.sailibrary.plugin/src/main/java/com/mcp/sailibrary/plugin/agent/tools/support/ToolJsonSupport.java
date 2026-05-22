	package com.mcp.sailibrary.plugin.agent.tools.support;

public final class ToolJsonSupport {

    private ToolJsonSupport() {
    }

    public static String extractJsonStringValue(String json, String key) {
        if (json == null || json.trim().length() == 0) {
            return "";
        }

        if (key == null || key.trim().length() == 0) {
            return "";
        }

        String quotedKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(quotedKey);
        if (keyIndex < 0) {
            return "";
        }

        int colonIndex = json.indexOf(':', keyIndex + quotedKey.length());
        if (colonIndex < 0) {
            return "";
        }

        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }

        if (valueStart >= json.length() || json.charAt(valueStart) != '"') {
            return "";
        }

        valueStart++;

        StringBuilder value = new StringBuilder();
        boolean escaping = false;

        for (int i = valueStart; i < json.length(); i++) {
            char current = json.charAt(i);

            if (escaping) {
                if (current == 'n') {
                    value.append('\n');
                } else if (current == 'r') {
                    value.append('\r');
                } else if (current == 't') {
                    value.append('\t');
                } else {
                    value.append(current);
                }
                escaping = false;
                continue;
            }

            if (current == '\\') {
                escaping = true;
                continue;
            }

            if (current == '"') {
                return value.toString();
            }

            value.append(current);
        }

        return "";
    }

    public static int extractJsonIntValue(String json, String key, int defaultValue, int minValue, int maxValue) {
        if (json == null || json.trim().length() == 0) {
            return clamp(defaultValue, minValue, maxValue);
        }

        if (key == null || key.trim().length() == 0) {
            return clamp(defaultValue, minValue, maxValue);
        }

        String quotedKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(quotedKey);
        if (keyIndex < 0) {
            return clamp(defaultValue, minValue, maxValue);
        }

        int colonIndex = json.indexOf(':', keyIndex + quotedKey.length());
        if (colonIndex < 0) {
            return clamp(defaultValue, minValue, maxValue);
        }

        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }

        if (valueStart >= json.length()) {
            return clamp(defaultValue, minValue, maxValue);
        }

        int valueEnd = valueStart;
        while (valueEnd < json.length()) {
            char current = json.charAt(valueEnd);
            if ((current >= '0' && current <= '9') || current == '-') {
                valueEnd++;
            } else {
                break;
            }
        }

        if (valueEnd <= valueStart) {
            return clamp(defaultValue, minValue, maxValue);
        }

        try {
            int parsed = Integer.parseInt(json.substring(valueStart, valueEnd));
            return clamp(parsed, minValue, maxValue);
        } catch (Exception e) {
            return clamp(defaultValue, minValue, maxValue);
        }
    }

    public static boolean hasKey(String json, String key) {
        if (json == null || key == null || key.trim().length() == 0) {
            return false;
        }
        return json.indexOf("\"" + key + "\"") >= 0;
    }

    private static int clamp(int value, int minValue, int maxValue) {
        int adjusted = value;
        if (adjusted < minValue) {
            adjusted = minValue;
        }
        if (adjusted > maxValue) {
            adjusted = maxValue;
        }
        return adjusted;
    }
}