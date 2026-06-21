package com.mcp.sailibrary.tests.mcp.application;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.mcp.sailibrary.plugin.mcp.application.McpExecutionSupport;
import com.mcp.sailibrary.plugin.mcp.application.McpExecutionWiring;
import com.mcp.sailibrary.plugin.mcp.application.ModelExecutionEngine;
import com.mcp.sailibrary.plugin.mcp.core.McpAccessCredentials;
import com.mcp.sailibrary.plugin.mcp.core.ModelChannel;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionRequest;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionResponse;
import com.mcp.sailibrary.plugin.mcp.core.RawModelResponse;
import com.mcp.sailibrary.plugin.mcp.core.RequestFormatKind;
import com.mcp.sailibrary.plugin.mcp.core.ResponseFormatKind;
import com.mcp.sailibrary.plugin.mcp.core.ToolPromptSections;
import com.mcp.sailibrary.plugin.mcp.core.TransportKind;
import com.mcp.sailibrary.plugin.mcp.ports.ModelConnector;
import com.mcp.sailibrary.plugin.mcp.ports.ModelExecutionProfileResolver;
import com.mcp.sailibrary.plugin.mcp.ports.ModelRequestCodec;
import com.mcp.sailibrary.plugin.mcp.ports.ModelResponseCodec;
import com.mcp.sailibrary.plugin.mcp.ports.ToolPromptSectionsPort;

/** * Testes do wiring MCP. * * @author Renato Tomaz Nati */
public class McpExecutionWiringTest {

    @Test
    public void deveConstruirEngineComRegistries() {
        ModelExecutionEngine engine = McpExecutionWiring.buildEngine(
                new FakeConnector(),
                new FakeConnector(),
                new FakeRequestCodec(),
                new FakeRequestCodec(),
                new FakeRequestCodec(),
                new FakeResponseCodec(),
                new FakeResponseCodec(),
                new FakeResponseCodec()
        );

        assertTrue(engine.hasConnector(TransportKind.LEGACY_JSON_RPC_HTTP));
        assertTrue(engine.hasConnector(TransportKind.STREAMING_SSE_HTTP));
        assertTrue(engine.hasRequestCodec(RequestFormatKind.LEGACY_MCP_TOOLS_CALL));
        assertTrue(engine.hasRequestCodec(RequestFormatKind.STREAMING_PROMPT));
        assertTrue(engine.hasRequestCodec(RequestFormatKind.STREAMING_RAW_JSON));
        assertTrue(engine.hasResponseCodec(ResponseFormatKind.LEGACY_MCP_ENVELOPE));
        assertTrue(engine.hasResponseCodec(ResponseFormatKind.STREAMING_SSE_EVENTS));
        assertTrue(engine.hasResponseCodec(ResponseFormatKind.PLAIN_TEXT));
    }

    @Test
    public void deveCriarExecutionSupport() {
        ModelExecutionEngine engine = McpExecutionWiring.buildEngine(
                new FakeConnector(),
                new FakeConnector(),
                new FakeRequestCodec(),
                new FakeRequestCodec(),
                new FakeRequestCodec(),
                new FakeResponseCodec(),
                new FakeResponseCodec(),
                new FakeResponseCodec()
        );

        McpExecutionWiring wiring = new McpExecutionWiring(
                engine,
                new FakeResolver(),
                new FakeToolPromptSectionsPort()
        );

        McpExecutionSupport support = wiring.createExecutionSupport();

        assertNotNull(support);
        assertNotNull(wiring.getModelExecutionEngine());
        assertNotNull(wiring.getModelExecutionProfileResolver());
        assertNotNull(wiring.getToolPromptSectionsPort());
    }

    private static class FakeConnector implements ModelConnector {
        @Override
        public RawModelResponse execute(ModelExecutionProfile profile, String requestBody, McpAccessCredentials credentials) {
            return new RawModelResponse("{}", 200, "application/json");
        }
    }

    private static class FakeRequestCodec implements ModelRequestCodec {
        @Override
        public String encode(ModelExecutionRequest request, ModelExecutionProfile profile) {
            return "{}";
        }
    }

    private static class FakeResponseCodec implements ModelResponseCodec {
        @Override
        public ModelExecutionResponse decode(RawModelResponse rawResponse, ModelExecutionProfile profile) {
            return new ModelExecutionResponse();
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

    private static class FakeToolPromptSectionsPort implements ToolPromptSectionsPort {
        @Override
        public ToolPromptSections load() {
            return new ToolPromptSections("tools", "examples");
        }
    }
}