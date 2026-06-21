package com.mcp.sailibrary.plugin.mcp.adapters.codec.request;

import com.mcp.sailibrary.plugin.mcp.McpPayloadBuilder;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionRequest;
import com.mcp.sailibrary.plugin.mcp.ports.ModelRequestCodec;

/** * Codec de request para o protocolo legado MCP tools/call. * * @author Renato Tomaz Nati * @since 2026-05-26 */
public class LegacyMcpRequestCodec implements ModelRequestCodec {

    private final McpPayloadBuilder mcpPayloadBuilder;

    public LegacyMcpRequestCodec() {
        this(new McpPayloadBuilder());
    }

    public LegacyMcpRequestCodec(McpPayloadBuilder mcpPayloadBuilder) {
        this.mcpPayloadBuilder = mcpPayloadBuilder != null ? mcpPayloadBuilder : new McpPayloadBuilder();
    }

    @Override
    public String encode(ModelExecutionRequest request, ModelExecutionProfile profile) throws Exception {
        if (request == null) {
            throw new IllegalArgumentException("Erro Operacional: request nao pode ser nulo.");
        }

        if (profile == null) {
            throw new IllegalArgumentException("Erro Operacional: profile nao pode ser nulo.");
        }

        String modelAlias = profile.resolveEffectiveModelName();
        if (isBlank(modelAlias)) {
            throw new IllegalArgumentException("Erro Operacional: legacy model alias nao pode ser vazio.");
        }

        String prompt = request.getPrompt() != null ? request.getPrompt() : "";
        return mcpPayloadBuilder.buildToolsCallPayload(modelAlias, "Prompt: " + prompt);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}