package com.mcp.sailibrary.plugin.mcp.adapters.codec.response;

import com.mcp.sailibrary.plugin.mcp.McpResponseExtractor;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionResponse;
import com.mcp.sailibrary.plugin.mcp.core.RawModelResponse;
import com.mcp.sailibrary.plugin.mcp.ports.ModelResponseCodec;

/** * Codec de response para o envelope legado MCP. * * @author Renato Tomaz Nati * @since 2026-05-26 */
public class LegacyMcpResponseCodec implements ModelResponseCodec {

    private final McpResponseExtractor mcpResponseExtractor;

    public LegacyMcpResponseCodec() {
        this(new McpResponseExtractor());
    }

    public LegacyMcpResponseCodec(McpResponseExtractor mcpResponseExtractor) {
        this.mcpResponseExtractor = mcpResponseExtractor != null ? mcpResponseExtractor : new McpResponseExtractor();
    }

    @Override
    public ModelExecutionResponse decode(RawModelResponse rawResponse, ModelExecutionProfile profile) throws Exception {
        ModelExecutionResponse response = new ModelExecutionResponse();

        String rawBody = rawResponse != null ? rawResponse.getRawBody() : "";
        String primaryText = mcpResponseExtractor.extractPrimaryText(rawBody);

        response.setPrimaryText(primaryText);
        response.setRawResponseBody(rawBody);
        response.setHttpStatusCode(rawResponse != null ? rawResponse.getStatusCode() : 0);
        response.setContentType(rawResponse != null ? rawResponse.getContentType() : "");

        return response;
    }
}