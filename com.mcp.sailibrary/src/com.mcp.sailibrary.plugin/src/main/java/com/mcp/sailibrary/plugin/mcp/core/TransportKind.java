package com.mcp.sailibrary.plugin.mcp.core;

/** * Define a forma de conexao com o modelo remoto. * * @author Renato Tomaz Nati * @since 2026-05-26 */
public enum TransportKind {

    LEGACY_JSON_RPC_HTTP,
    STREAMING_SSE_HTTP,
    SAI_CHAT_EXECUTE_HTTP;

    public static TransportKind fromProperty(String value) {
        String safe = value != null ? value.trim() : "";

        if ("legacy".equalsIgnoreCase(safe)
                || "LEGACY_JSON_RPC_HTTP".equalsIgnoreCase(safe)) {
            return LEGACY_JSON_RPC_HTTP;
        }

        if ("streaming".equalsIgnoreCase(safe)
                || "STREAMING_SSE_HTTP".equalsIgnoreCase(safe)) {
            return STREAMING_SSE_HTTP;
        }

        if ("sai".equalsIgnoreCase(safe)
                || "sai_chatexecute".equalsIgnoreCase(safe)
                || "sai-chat-execute".equalsIgnoreCase(safe)
                || "chatexecute".equalsIgnoreCase(safe)
                || "SAI_CHAT_EXECUTE_HTTP".equalsIgnoreCase(safe)) {
            return SAI_CHAT_EXECUTE_HTTP;
        }

        return SAI_CHAT_EXECUTE_HTTP;
    }
}
