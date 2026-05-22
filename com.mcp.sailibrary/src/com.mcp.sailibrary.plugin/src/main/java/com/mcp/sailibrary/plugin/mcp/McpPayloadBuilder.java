package com.mcp.sailibrary.plugin.mcp;

import com.google.gson.JsonObject;

/** * Monta payloads JSON-RPC para chamada do endpoint MCP. * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class McpPayloadBuilder {

    /** * Construi payload JSON-RPC para tools/call. * * @param toolName nome da ferramenta remota do MCP * @param inputPrompt texto completo de entrada * @return payload JSON-RPC serializado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String buildToolsCallPayload(String toolName, String inputPrompt) {
        JsonObject root = new JsonObject();
        root.addProperty("jsonrpc", "2.0");
        root.addProperty("method", "tools/call");

        JsonObject params = new JsonObject();
        params.addProperty("name", toolName);

        JsonObject arguments = new JsonObject();
        arguments.addProperty("input", inputPrompt);

        params.add("arguments", arguments);
        root.add("params", params);
        root.addProperty("id", 1);

        return root.toString();
    }

    /** * Escapa texto para transporte seguro dentro de payload JSON textual. * * <p>Este metodo deve ser usado apenas quando realmente houver necessidade * de serializacao manual. Quando o payload for montado com Gson, o escape * ja e tratado naturalmente pelo serializador.</p> * * @param input texto original * @return texto adaptado para transporte JSON * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public static String escapeForJsonTransport(String input) {
        if (input == null) {
            return "";
        }

        StringBuilder escaped = new StringBuilder(input.length() + 32);

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            switch (c) {
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                default:
                    if (c < 0x20) {
                        String hex = Integer.toHexString(c);
                        escaped.append("\\u");
                        for (int j = hex.length(); j < 4; j++) {
                            escaped.append('0');
                        }
                        escaped.append(hex);
                    } else {
                        escaped.append(c);
                    }
                    break;
            }
        }

        return escaped.toString();
    }
}