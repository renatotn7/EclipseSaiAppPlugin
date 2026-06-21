package com.mcp.sailibrary.tests.mcp.multimodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.mcp.sailibrary.plugin.mcp.application.McpExecutionSupport;
import com.mcp.sailibrary.plugin.mcp.application.ModelExecutionEngine;
import com.mcp.sailibrary.plugin.mcp.core.McpAccessCredentials;
import com.mcp.sailibrary.plugin.mcp.core.ModelChannel;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionResponse;
import com.mcp.sailibrary.plugin.mcp.core.RequestFormatKind;
import com.mcp.sailibrary.plugin.mcp.core.ResponseFormatKind;
import com.mcp.sailibrary.plugin.mcp.core.TransportKind;
import com.mcp.sailibrary.plugin.mcp.multimodel.gateway.UnifiedMcpModelGateway;
import com.mcp.sailibrary.plugin.mcp.ports.ModelExecutionProfileResolver;

/** * Testes do gateway unificado MCP usando fakes manuais. * * @author Renato Tomaz Nati */
public class UnifiedMcpModelGatewayTest {

    @Test
    public void deveExecutarPlannerPorCanalUsandoSupport() throws Exception {
        FakeMcpExecutionSupport fakeSupport = new FakeMcpExecutionSupport();
        fakeSupport.profile = createStreamingPlannerProfile();

        ModelExecutionResponse fakeResponse = new ModelExecutionResponse();
        fakeResponse.setPrimaryText("ok");
        fakeResponse.setRawResponseBody("raw");
        fakeSupport.promptResponse = fakeResponse;

        UnifiedMcpModelGateway gateway = new UnifiedMcpModelGateway("https://x", fakeSupport);

        ModelExecutionResponse response = gateway.executePlannerPrompt("prompt abc", "api-key-123");

        assertNotNull(response);
        assertEquals("ok", response.getPrimaryText());

        assertEquals(ModelChannel.PLANNER, fakeSupport.lastPromptChannel);
        assertEquals("prompt abc", fakeSupport.lastPrompt);
        assertNotNull(fakeSupport.lastPromptCredentials);
        assertTrue(fakeSupport.lastPromptCredentials.hasApiKey());
        assertEquals("api-key-123", fakeSupport.lastPromptCredentials.getApiKey());

        ModelExecutionProfile resolved = gateway.resolveProfile(ModelChannel.PLANNER);
        assertEquals("gpt-5.4-2026-03-05", resolved.resolveEffectiveModelName());
        assertEquals(TransportKind.STREAMING_SSE_HTTP, resolved.getTransportKind());
    }

    @Test
    public void deveExecutarRawJsonPorCanal() throws Exception {
        FakeMcpExecutionSupport fakeSupport = new FakeMcpExecutionSupport();
        fakeSupport.profile = createStreamingCodeGeneratorProfile();

        ModelExecutionResponse fakeResponse = new ModelExecutionResponse();
        fakeResponse.setPrimaryText("ok raw");
        fakeResponse.setRawResponseBody("{\"status\":\"ok\"}");
        fakeSupport.rawJsonResponse = fakeResponse;

        UnifiedMcpModelGateway gateway = new UnifiedMcpModelGateway("https://x", fakeSupport);

        McpAccessCredentials credentials = McpAccessCredentials.forApiKeyAndCookie("key-1", "cookie-1");

        ModelExecutionResponse response = gateway.executeChannelRawJson(
                ModelChannel.CODE_GENERATOR,
                "{\"a\":1}",
                credentials
        );

        assertNotNull(response);
        assertEquals("ok raw", response.getPrimaryText());

        assertEquals(ModelChannel.CODE_GENERATOR, fakeSupport.lastRawJsonChannel);
        assertEquals("{\"a\":1}", fakeSupport.lastRawJsonBody);
        assertNotNull(fakeSupport.lastRawJsonCredentials);
        assertEquals("key-1", fakeSupport.lastRawJsonCredentials.getApiKey());
        assertEquals("cookie-1", fakeSupport.lastRawJsonCredentials.getCookieValue());
    }

    @Test
    public void deveResolverProfileSemExecutarChamada() {
        FakeMcpExecutionSupport fakeSupport = new FakeMcpExecutionSupport();
        fakeSupport.profile = createStreamingPlannerProfile();

        UnifiedMcpModelGateway gateway = new UnifiedMcpModelGateway("https://x", fakeSupport);

        ModelExecutionProfile resolved = gateway.resolveProfile(ModelChannel.PLANNER);

        assertNotNull(resolved);
        assertEquals("GPT54", resolved.getLegacyModelAlias());
        assertEquals("gpt-5.4-2026-03-05", resolved.getStreamingModelName());
        assertEquals(0.10d, resolved.getCreativity().doubleValue(), 0.0001d);
        assertEquals(16384, resolved.getMaxTokens().intValue());
    }

    private ModelExecutionProfile createStreamingPlannerProfile() {
        ModelExecutionProfile profile = new ModelExecutionProfile();
        profile.setTransportKind(TransportKind.STREAMING_SSE_HTTP);
        profile.setRequestFormatKind(RequestFormatKind.STREAMING_PROMPT);
        profile.setResponseFormatKind(ResponseFormatKind.STREAMING_SSE_EVENTS);
        profile.setLegacyModelAlias("GPT54");
        profile.setStreamingModelName("gpt-5.4-2026-03-05");
        profile.setCreativity(Double.valueOf(0.10d));
        profile.setMaxTokens(Integer.valueOf(16384));
        return profile;
    }

    private ModelExecutionProfile createStreamingCodeGeneratorProfile() {
        ModelExecutionProfile profile = new ModelExecutionProfile();
        profile.setTransportKind(TransportKind.STREAMING_SSE_HTTP);
        profile.setRequestFormatKind(RequestFormatKind.STREAMING_RAW_JSON);
        profile.setResponseFormatKind(ResponseFormatKind.STREAMING_SSE_EVENTS);
        profile.setLegacyModelAlias("GPT52CODEX");
        profile.setStreamingModelName("gpt-5.2-codex");
        profile.setCreativity(Double.valueOf(0.00d));
        profile.setMaxTokens(Integer.valueOf(16384));
        return profile;
    }

    private static class FakeMcpExecutionSupport extends McpExecutionSupport {

        private ModelExecutionProfile profile;
        private ModelExecutionResponse promptResponse;
        private ModelExecutionResponse rawJsonResponse;

        private ModelChannel lastPromptChannel;
        private String lastPrompt;
        private McpAccessCredentials lastPromptCredentials;

        private ModelChannel lastRawJsonChannel;
        private String lastRawJsonBody;
        private McpAccessCredentials lastRawJsonCredentials;

        public FakeMcpExecutionSupport() {
            super(new ModelExecutionEngine(), new DummyResolver());
        }

        @Override
        public ModelExecutionResponse executePrompt(ModelChannel channel, String prompt, McpAccessCredentials credentials) {
            this.lastPromptChannel = channel;
            this.lastPrompt = prompt;
            this.lastPromptCredentials = credentials;
            return promptResponse;
        }

        @Override
        public ModelExecutionResponse executeRawJson(ModelChannel channel, String rawJsonBody, McpAccessCredentials credentials) {
            this.lastRawJsonChannel = channel;
            this.lastRawJsonBody = rawJsonBody;
            this.lastRawJsonCredentials = credentials;
            return rawJsonResponse;
        }

        @Override
        public ModelExecutionProfile resolveProfile(ModelChannel channel) {
            return profile;
        }
    }

    private static class DummyResolver implements ModelExecutionProfileResolver {
        @Override
        public ModelExecutionProfile resolve(ModelChannel channel) {
            return new ModelExecutionProfile();
        }
    }
}