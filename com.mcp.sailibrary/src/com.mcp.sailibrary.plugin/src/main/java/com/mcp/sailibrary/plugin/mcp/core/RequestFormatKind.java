package com.mcp.sailibrary.plugin.mcp.core;

/** * Define como o JSON de entrada sera montado. * * @author Renato Tomaz Nati * @since 2026-05-26 */
public enum RequestFormatKind {

    LEGACY_MCP_TOOLS_CALL,
    STREAMING_PROMPT,
    STREAMING_RAW_JSON,
    SAI_CHAT_EXECUTE;

    public static RequestFormatKind fromProperty(String value) {
        String safe = value != null ? value.trim() : "";

        if ("LEGACY_MCP_TOOLS_CALL".equalsIgnoreCase(safe)) {
            return LEGACY_MCP_TOOLS_CALL;
        }

        if ("STREAMING_PROMPT".equalsIgnoreCase(safe)) {
            return STREAMING_PROMPT;
        }

        if ("STREAMING_RAW_JSON".equalsIgnoreCase(safe)) {
            return STREAMING_RAW_JSON;
        }

        if ("SAI_CHAT_EXECUTE".equalsIgnoreCase(safe)
                || "sai".equalsIgnoreCase(safe)
                || "sai_chatexecute".equalsIgnoreCase(safe)
                || "chatexecute".equalsIgnoreCase(safe)) {
            return SAI_CHAT_EXECUTE;
        }

        if ("legacy".equalsIgnoreCase(safe)) {
            return LEGACY_MCP_TOOLS_CALL;
        }

        if ("streaming".equalsIgnoreCase(safe)) {
            return STREAMING_PROMPT;
        }

        return SAI_CHAT_EXECUTE;
    }
}
