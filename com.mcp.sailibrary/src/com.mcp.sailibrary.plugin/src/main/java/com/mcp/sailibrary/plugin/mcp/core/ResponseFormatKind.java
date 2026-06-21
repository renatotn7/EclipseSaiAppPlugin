package com.mcp.sailibrary.plugin.mcp.core;

/** * Define como a resposta bruta sera interpretada. * * @author Renato Tomaz Nati * @since 2026-05-26 */
public enum ResponseFormatKind {

    LEGACY_MCP_ENVELOPE,
    STREAMING_SSE_EVENTS,
    PLAIN_TEXT,
    SAI_CHAT_EXECUTE_JSON;

    public static ResponseFormatKind fromProperty(String value) {
        String safe = value != null ? value.trim() : "";

        if ("LEGACY_MCP_ENVELOPE".equalsIgnoreCase(safe)) {
            return LEGACY_MCP_ENVELOPE;
        }

        if ("STREAMING_SSE_EVENTS".equalsIgnoreCase(safe)) {
            return STREAMING_SSE_EVENTS;
        }

        if ("PLAIN_TEXT".equalsIgnoreCase(safe)) {
            return PLAIN_TEXT;
        }

        if ("SAI_CHAT_EXECUTE_JSON".equalsIgnoreCase(safe)
                || "sai".equalsIgnoreCase(safe)
                || "sai_chatexecute".equalsIgnoreCase(safe)
                || "chatexecute".equalsIgnoreCase(safe)) {
            return SAI_CHAT_EXECUTE_JSON;
        }

        if ("legacy".equalsIgnoreCase(safe)) {
            return LEGACY_MCP_ENVELOPE;
        }

        if ("streaming".equalsIgnoreCase(safe)) {
            return STREAMING_SSE_EVENTS;
        }

        return SAI_CHAT_EXECUTE_JSON;
    }
}
