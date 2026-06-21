package com.mcp.sailibrary.tests.mcp.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.mcp.sailibrary.plugin.mcp.application.McpExecutionSupport;
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

/** * Testes do support de execucao MCP. * * @author Renato Tomaz Nati */
public class McpExecutionSupportTest {

    @Test
    public void deveExecutarPromptPorCanal() throws Exception {
        ModelExecutionEngine engine = new ModelExecutionEngine();
        engine.registerConnector(TransportKind.LEGACY_JSON_RPC_HTTP, new FakeConnector());
        engine.registerRequestCodec(RequestFormatKind.LEGACY_MCP_TOOLS_CALL, new FakeRequestCodec());
        engine.registerResponseCodec(ResponseFormatKind.LEGACY_MCP_ENVELOPE, new FakeResponseCodec());

        McpExecutionSupport support = new McpExecutionSupport(engine, new FakeResolver());

        ModelExecutionResponse response = support.executePrompt(
                ModelChannel.PLANNER,
                "prompt abc",
                McpAccessCredentials.forApiKey("k1")
        );

        assertNotNull(response);
        assertEquals("texto interpretado", response.getPrimaryText());
    }

    @Test
    public void deveExecutarRawJsonPorCanal() throws Exception {
        ModelExecutionEngine engine = new ModelExecutionEngine();
        engine.registerConnector(TransportKind.LEGACY_JSON_RPC_HTTP, new FakeConnector());
        engine.registerRequestCodec(RequestFormatKind.LEGACY_MCP_TOOLS_CALL, new FakeRequestCodec());
        engine.registerResponseCodec(ResponseFormatKind.LEGACY_MCP_ENVELOPE, new FakeResponseCodec());

        McpExecutionSupport support = new McpExecutionSupport(engine, new FakeResolver());

        ModelExecutionResponse response = support.executeRawJson(
                ModelChannel.CODE_GENERATOR,
                "{\"a\":1}",
                McpAccessCredentials.forApiKeyAndCookie("k", "c")
        );

        assertNotNull(response);
        assertEquals("texto interpretado", response.getPrimaryText());
    }

    @Test
    public void deveResolverProfile() {
        McpExecutionSupport support = new McpExecutionSupport(new ModelExecutionEngine(), new FakeResolver());

        ModelExecutionProfile profile = support.resolveProfile(ModelChannel.SUMMARIZER);

        assertNotNull(profile);
        assertEquals(TransportKind.LEGACY_JSON_RPC_HTTP, profile.getTransportKind());
    }

    private static class FakeConnector implements ModelConnector {
        @Override
        public RawModelResponse execute(ModelExecutionProfile profile, String requestBody, McpAccessCredentials credentials) {
            return new RawModelResponse("{\"x\":1}", 200, "application/json");
        }
    }

    private static class FakeRequestCodec implements ModelRequestCodec {
        @Override
        public String encode(ModelExecutionRequest request, ModelExecutionProfile profile) {
            return "{\"fake\":true}";
        }
    }

    private static class FakeResponseCodec implements ModelResponseCodec {
        @Override
        public ModelExecutionResponse decode(RawModelResponse rawResponse, ModelExecutionProfile profile) {
            ModelExecutionResponse response = new ModelExecutionResponse();
            response.setPrimaryText("texto interpretado");
            response.setRawResponseBody(rawResponse.getRawBody());
            return response;
        }
    }

    private static class FakeResolver implements ModelExecutionProfileResolver {
        @Override
        public ModelExecutionProfile resolve(ModelChannel channel) {
            ModelExecutionProfile profile = new ModelExecutionProfile();
            profile.setTransportKind(TransportKind.LEGACY_JSON_RPC_HTTP);
            profile.setRequestFormatKind(RequestFormatKind.LEGACY_MCP_TOOLS_CALL);
            profile.setResponseFormatKind(ResponseFormatKind.LEGACY_MCP_ENVELOPE);
            return profile;
        }
    }
}