package com.mcp.sailibrary.tests.mcp.adapters.codec.request;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcp.sailibrary.plugin.mcp.adapters.codec.request.LegacyMcpRequestCodec;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionRequest;

/** * Testes do codec legado de request MCP. * * @author Renato Tomaz Nati */
public class LegacyMcpRequestCodecTest {

    @Test
    public void deveGerarPayloadToolsCallComPrompt() throws Exception {
        LegacyMcpRequestCodec codec = new LegacyMcpRequestCodec();

        ModelExecutionProfile profile = new ModelExecutionProfile();
        profile.setLegacyModelAlias("GPT54");

        ModelExecutionRequest request = new ModelExecutionRequest();
        request.setPrompt("meu prompt");

        String json = codec.encode(request, profile);

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        assertEquals("2.0", root.get("jsonrpc").getAsString());
        assertEquals("tools/call", root.get("method").getAsString());
        assertEquals("GPT54", root.getAsJsonObject("params").get("name").getAsString());
        assertEquals(
                "Prompt: meu prompt",
                root.getAsJsonObject("params")
                        .getAsJsonObject("arguments")
                        .get("input")
                        .getAsString()
        );
    }

    @Test
    public void deveAceitarPromptVazio() throws Exception {
        LegacyMcpRequestCodec codec = new LegacyMcpRequestCodec();

        ModelExecutionProfile profile = new ModelExecutionProfile();
        profile.setLegacyModelAlias("GPT54");

        ModelExecutionRequest request = new ModelExecutionRequest();
        request.setPrompt("");

        String json = codec.encode(request, profile);

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        assertEquals(
                "Prompt: ",
                root.getAsJsonObject("params")
                        .getAsJsonObject("arguments")
                        .get("input")
                        .getAsString()
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void deveFalharQuandoProfileForNulo() throws Exception {
        LegacyMcpRequestCodec codec = new LegacyMcpRequestCodec();

        ModelExecutionRequest request = new ModelExecutionRequest();
        request.setPrompt("teste");

        codec.encode(request, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deveFalharQuandoRequestForNulo() throws Exception {
        LegacyMcpRequestCodec codec = new LegacyMcpRequestCodec();

        ModelExecutionProfile profile = new ModelExecutionProfile();
        profile.setLegacyModelAlias("GPT54");

        codec.encode(null, profile);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deveFalharQuandoAliasDoModeloForVazio() throws Exception {
        LegacyMcpRequestCodec codec = new LegacyMcpRequestCodec();

        ModelExecutionProfile profile = new ModelExecutionProfile();
        profile.setLegacyModelAlias("");

        ModelExecutionRequest request = new ModelExecutionRequest();
        request.setPrompt("teste");

        codec.encode(request, profile);
    }
}