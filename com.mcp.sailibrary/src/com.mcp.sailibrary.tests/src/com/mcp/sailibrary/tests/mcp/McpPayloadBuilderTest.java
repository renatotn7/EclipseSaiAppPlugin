package com.mcp.sailibrary.tests.mcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcp.sailibrary.plugin.mcp.McpPayloadBuilder;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.TransportKind;

public class McpPayloadBuilderTest {

    @Test
    public void deveMontarPayloadLegadoToolsCall() {
        McpPayloadBuilder builder = new McpPayloadBuilder();

        String json = builder.buildToolsCallPayload("GPT54", "Prompt: teste");
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        assertEquals("2.0", root.get("jsonrpc").getAsString());
        assertEquals("tools/call", root.get("method").getAsString());
        assertEquals("GPT54", root.getAsJsonObject("params").get("name").getAsString());
        assertEquals("Prompt: teste",
                root.getAsJsonObject("params")
                        .getAsJsonObject("arguments")
                        .get("input")
                        .getAsString());
    }

    @Test
    public void deveMontarPayloadStreamingPrompt() {
        McpPayloadBuilder builder = new McpPayloadBuilder();

        ModelExecutionProfile profile = new ModelExecutionProfile();
        profile.setTransportKind(TransportKind.STREAMING_SSE_HTTP);
        profile.setStreamingModelName("gpt-5.4-2026-03-05");
        profile.setCreativity(Double.valueOf(0.10d));
        profile.setMaxTokens(Integer.valueOf(1234));
        profile.setConversationId("conv-1");
        profile.setInstructions("inst");
        profile.setCodeInterpreter(false);

        String json = builder.buildStreamingPromptPayload("meu prompt", profile);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        assertEquals("meu prompt",
                root.getAsJsonArray("messages").get(0).getAsJsonObject().get("content").getAsString());
        assertEquals("gpt-5.4-2026-03-05",
                root.getAsJsonObject("parameters").get("model").getAsString());
        assertEquals(0.10d,
                root.getAsJsonObject("parameters").get("temperature").getAsDouble(),
                0.0001d);
        assertEquals(1234,
                root.getAsJsonObject("parameters").get("maxTokens").getAsInt());
    }

    @Test
    public void deveNormalizarRawJson() {
        McpPayloadBuilder builder = new McpPayloadBuilder();

        String normalized = builder.normalizeRawJsonPayload("{\"a\":1}");

        assertEquals("{\"a\":1}", normalized);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deveFalharQuandoRawJsonForInvalido() {
        McpPayloadBuilder builder = new McpPayloadBuilder();

        builder.normalizeRawJsonPayload("{invalido");
    }

    @Test
    public void deveEscaparTextoParaJson() {
        String escaped = McpPayloadBuilder.escapeForJsonTransport("linha1\n\"x\"\t\\");

        assertTrue(escaped.contains("\\n"));
        assertTrue(escaped.contains("\\\"x\\\""));
        assertTrue(escaped.contains("\\t"));
        assertTrue(escaped.contains("\\\\"));
    }
}