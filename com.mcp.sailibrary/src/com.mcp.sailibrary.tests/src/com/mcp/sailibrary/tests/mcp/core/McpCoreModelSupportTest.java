package com.mcp.sailibrary.tests.mcp.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

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

/** * Testes dos modelos simples da arquitetura MCP. * * @author Renato Tomaz Nati */
public class McpCoreModelSupportTest {

    @Test
    public void deveResolverEnumsPorProperty() {
        assertEquals(TransportKind.LEGACY_JSON_RPC_HTTP, TransportKind.fromProperty("legacy"));
        assertEquals(TransportKind.STREAMING_SSE_HTTP, TransportKind.fromProperty("streaming"));
        assertEquals(RequestFormatKind.LEGACY_MCP_TOOLS_CALL, RequestFormatKind.fromProperty("legacy"));
        assertEquals(RequestFormatKind.STREAMING_PROMPT, RequestFormatKind.fromProperty("streaming"));
        assertEquals(RequestFormatKind.STREAMING_RAW_JSON, RequestFormatKind.fromProperty("STREAMING_RAW_JSON"));
        assertEquals(ResponseFormatKind.LEGACY_MCP_ENVELOPE, ResponseFormatKind.fromProperty("legacy"));
        assertEquals(ResponseFormatKind.STREAMING_SSE_EVENTS, ResponseFormatKind.fromProperty("streaming"));
        assertEquals(ResponseFormatKind.PLAIN_TEXT, ResponseFormatKind.fromProperty("PLAIN_TEXT"));
    }

    @Test
    public void deveTrabalharComCredenciais() {
        McpAccessCredentials api = McpAccessCredentials.forApiKey("abc");
        assertTrue(api.hasApiKey());
        assertFalse(api.hasCookieValue());

        McpAccessCredentials cookie = McpAccessCredentials.forCookie("cookie");
        assertFalse(cookie.hasApiKey());
        assertTrue(cookie.hasCookieValue());

        McpAccessCredentials both = McpAccessCredentials.forApiKeyAndCookie("k1", "c1");
        assertEquals("k1", both.getApiKey());
        assertEquals("c1", both.getCookieValue());
    }

    @Test
    public void deveClamparCreativityEResolverModeloEfetivo() {
        ModelExecutionProfile profile = new ModelExecutionProfile();

        profile.setLegacyModelAlias("GPT54");
        profile.setStreamingModelName("gpt-5.4-2026-03-05");

        profile.setTransportKind(TransportKind.LEGACY_JSON_RPC_HTTP);
        assertEquals("GPT54", profile.resolveEffectiveModelName());

        profile.setTransportKind(TransportKind.STREAMING_SSE_HTTP);
        assertEquals("gpt-5.4-2026-03-05", profile.resolveEffectiveModelName());

        profile.setCreativity(Double.valueOf(-1.0d));
        assertEquals(0.0d, profile.getCreativity().doubleValue(), 0.0001d);

        profile.setCreativity(Double.valueOf(5.0d));
        assertEquals(1.0d, profile.getCreativity().doubleValue(), 0.0001d);

        profile.setMaxTokens(Integer.valueOf(-10));
        assertEquals(16384, profile.getMaxTokens().intValue());
    }

    @Test
    public void deveTrabalharComRequestResponseERawResponse() {
        ModelExecutionRequest request = new ModelExecutionRequest();
        request.setChannel(ModelChannel.PLANNER);
        request.setPrompt("prompt");
        request.setRawJsonBody("{\"x\":1}");

        assertEquals(ModelChannel.PLANNER, request.getChannel());
        assertEquals("prompt", request.getPrompt());
        assertTrue(request.hasRawJsonBody());

        ModelExecutionResponse response = new ModelExecutionResponse();
        response.setPrimaryText("texto");
        response.setRawResponseBody("raw");
        response.setHttpStatusCode(200);
        response.setContentType("application/json");

        assertEquals("texto", response.getPrimaryText());
        assertEquals("raw", response.getRawResponseBody());
        assertEquals(200, response.getHttpStatusCode());
        assertEquals("application/json", response.getContentType());

        RawModelResponse raw = new RawModelResponse("body", 201, "text/plain");
        assertEquals("body", raw.getRawBody());
        assertEquals(201, raw.getStatusCode());
        assertEquals("text/plain", raw.getContentType());
    }

    @Test
    public void deveTrabalharComToolPromptSections() {
        ToolPromptSections sections = new ToolPromptSections("tools", "examples");

        assertEquals("tools", sections.getToolsSection());
        assertEquals("examples", sections.getExamplesSection());

        sections.setToolsSection("tools2");
        sections.setExamplesSection("examples2");

        assertEquals("tools2", sections.getToolsSection());
        assertEquals("examples2", sections.getExamplesSection());
    }
}