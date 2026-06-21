package com.mcp.sailibrary.tests.mcp.adapters.codec.response;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.mcp.sailibrary.plugin.mcp.adapters.codec.response.LegacyMcpResponseCodec;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionResponse;
import com.mcp.sailibrary.plugin.mcp.core.RawModelResponse;

/** * Testes do codec de resposta legado MCP. * * @author Renato Tomaz Nati */
public class LegacyMcpResponseCodecTest {

    @Test
    public void deveExtrairTextoDoEnvelopeLegado() throws Exception {
        LegacyMcpResponseCodec codec = new LegacyMcpResponseCodec();

        RawModelResponse rawResponse = new RawModelResponse(
                "{\"result\":{\"content\":[{\"text\":\"texto legado\"}]}}",
                200,
                "application/json"
        );

        ModelExecutionResponse response = codec.decode(rawResponse, new ModelExecutionProfile());

        assertEquals("texto legado", response.getPrimaryText());
        assertEquals(200, response.getHttpStatusCode());
        assertEquals("application/json", response.getContentType());
    }
}