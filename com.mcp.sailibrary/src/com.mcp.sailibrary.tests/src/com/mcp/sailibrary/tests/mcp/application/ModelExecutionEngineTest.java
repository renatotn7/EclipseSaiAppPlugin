package com.mcp.sailibrary.tests.mcp.application;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.mcp.sailibrary.plugin.mcp.application.ModelExecutionEngine;
import com.mcp.sailibrary.plugin.mcp.core.McpAccessCredentials;
import com.mcp.sailibrary.plugin.mcp.core.ModelChannel;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionRequest;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionResponse;
import com.mcp.sailibrary.plugin.mcp.core.RawModelResponse;
import com.mcp.sailibrary.plugin.mcp.core.RequestFormatKind;
import com.mcp.sailibrary.plugin.mcp.core.ResponseFormatKind;
import com.mcp.sailibrary.plugin.mcp.core.TransportKind;
import com.mcp.sailibrary.plugin.mcp.ports.ModelConnector;
import com.mcp.sailibrary.plugin.mcp.ports.ModelExecutionProfileResolver;
import com.mcp.sailibrary.plugin.mcp.ports.ModelRequestCodec;
import com.mcp.sailibrary.plugin.mcp.ports.ModelResponseCodec;

public class ModelExecutionEngineTest {

    @Test
    public void deveExecutarFluxoCompleto() throws Exception {
        ModelExecutionEngine engine = new ModelExecutionEngine();

        engine.registerConnector(TransportKind.LEGACY_JSON_RPC_HTTP, new FakeConnector());
        engine.registerRequestCodec(RequestFormatKind.LEGACY_MCP_TOOLS_CALL, new FakeRequestCodec());
        engine.registerResponseCodec(ResponseFormatKind.LEGACY_MCP_ENVELOPE, new FakeResponseCodec());

        ModelExecutionProfile profile = new ModelExecutionProfile();
        profile.setTransportKind(TransportKind.LEGACY_JSON_RPC_HTTP);
        profile.setRequestFormatKind(RequestFormatKind.LEGACY_MCP_TOOLS_CALL);
        profile.setResponseFormatKind(ResponseFormatKind.LEGACY_MCP_ENVELOPE);

        ModelExecutionRequest request = new ModelExecutionRequest();
        request.setChannel(ModelChannel.PLANNER);
        request.setPrompt("abc");
        request.setCredentials(McpAccessCredentials.forApiKey("key"));

        ModelExecutionResponse response = engine.execute(request, profile);

        assertEquals("texto interpretado", response.getPrimaryText());
    }

    @Test
    public void deveExecutarViaResolver() throws Exception {
        ModelExecutionEngine engine = new ModelExecutionEngine();

        engine.registerConnector(TransportKind.LEGACY_JSON_RPC_HTTP, new FakeConnector());
        engine.registerRequestCodec(RequestFormatKind.LEGACY_MCP_TOOLS_CALL, new FakeRequestCodec());
        engine.registerResponseCodec(ResponseFormatKind.LEGACY_MCP_ENVELOPE, new FakeResponseCodec());

        ModelExecutionRequest request = new ModelExecutionRequest();
        request.setChannel(ModelChannel.PLANNER);
        request.setPrompt("abc");

        ModelExecutionResponse response = engine.execute(request, new FakeResolver());

        assertEquals("texto interpretado", response.getPrimaryText());
    }

    private static class FakeConnector implements ModelConnector {
        public RawModelResponse execute(ModelExecutionProfile profile, String requestBody, McpAccessCredentials credentials) {
            return new RawModelResponse("{\"x\":1}", 200, "application/json");
        }
    }

    private static class FakeRequestCodec implements ModelRequestCodec {
        public String encode(ModelExecutionRequest request, ModelExecutionProfile profile) {
            return "{\"fake\":true}";
        }
    }

    private static class FakeResponseCodec implements ModelResponseCodec {
        public ModelExecutionResponse decode(RawModelResponse rawResponse, ModelExecutionProfile profile) {
            ModelExecutionResponse response = new ModelExecutionResponse();
            response.setPrimaryText("texto interpretado");
            return response;
        }
    }

    private static class FakeResolver implements ModelExecutionProfileResolver {
        public ModelExecutionProfile resolve(ModelChannel channel) {
            ModelExecutionProfile profile = new ModelExecutionProfile();
            profile.setTransportKind(TransportKind.LEGACY_JSON_RPC_HTTP);
            profile.setRequestFormatKind(RequestFormatKind.LEGACY_MCP_TOOLS_CALL);
            profile.setResponseFormatKind(ResponseFormatKind.LEGACY_MCP_ENVELOPE);
            return profile;
        }
    }
}