package com.mcp.sailibrary.tests.mcp.adapters.codec.request;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcp.sailibrary.plugin.mcp.adapters.codec.request.StreamingPromptRequestCodec;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionRequest;
import com.mcp.sailibrary.plugin.mcp.core.TransportKind;

/** * Testes do codec de request para streaming por prompt. * * @author Renato Tomaz Nati */
public class StreamingPromptRequestCodecTest {

    @Test
    public void deveMontarBodyStreamingComPrompt() throws Exception {
        StreamingPromptRequestCodec codec = new StreamingPromptRequestCodec();

        ModelExecutionProfile profile = new ModelExecutionProfile();
        profile.setTransportKind(TransportKind.STREAMING_SSE_HTTP);
        profile.setStreamingModelName("gpt-5.4-2026-03-05");
        profile.setCreativity(Double.valueOf(0.15d));
        profile.setMaxTokens(Integer.valueOf(999));
        profile.setConversationId("conv-xyz");
        profile.setWorkspaceId("");
        profile.setInstructions("inst");
        profile.setCodeInterpreter(false);

        ModelExecutionRequest request = new ModelExecutionRequest();
        request.setPrompt("teste stream");

        String json = codec.encode(request, profile);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        assertEquals("teste stream",
                root.getAsJsonArray("messages").get(0).getAsJsonObject().get("content").getAsString());
        assertEquals("gpt-5.4-2026-03-05",
                root.getAsJsonObject("parameters").get("model").getAsString());
        assertEquals(0.15d,
                root.getAsJsonObject("parameters").get("temperature").getAsDouble(),
                0.0001d);
        assertEquals(999,
                root.getAsJsonObject("parameters").get("maxTokens").getAsInt());
        assertEquals("conv-xyz", root.get("conversationId").getAsString());
        assertTrue(root.get("workspaceId").isJsonNull());
    }

    @Test(expected = IllegalArgumentException.class)
    public void deveFalharQuandoRequestForNulo() throws Exception {
        StreamingPromptRequestCodec codec = new StreamingPromptRequestCodec();
        codec.encode(null, new ModelExecutionProfile());
    }

    @Test(expected = IllegalArgumentException.class)
    public void deveFalharQuandoProfileForNulo() throws Exception {
        StreamingPromptRequestCodec codec = new StreamingPromptRequestCodec();
        codec.encode(new ModelExecutionRequest(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deveFalharQuandoModeloStreamingNaoForInformado() throws Exception {
        StreamingPromptRequestCodec codec = new StreamingPromptRequestCodec();

        ModelExecutionProfile profile = new ModelExecutionProfile();
        profile.setStreamingModelName("");

        ModelExecutionRequest request = new ModelExecutionRequest();
        request.setPrompt("teste");

        codec.encode(request, profile);
    }
}