package com.mcp.sailibrary.tests.mcp.adapters.codec.response;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.mcp.sailibrary.plugin.mcp.adapters.codec.response.StreamingSseResponseCodec;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionResponse;
import com.mcp.sailibrary.plugin.mcp.core.RawModelResponse;

public class StreamingSseResponseCodecTest {

    @Test
    public void deveExtrairTextoDoResponseCompleted() throws Exception {
        StreamingSseResponseCodec codec = new StreamingSseResponseCodec();

        String raw = ""
                + "event: responsecreated\n"
                + "data: {\"type\":\"ResponseCreated\"}\n"
                + "\n"
                + "event: outputtextdelta\n"
                + "data: {\"type\":\"OutputTextDelta\",\"delta\":\"texto parcial\"}\n"
                + "\n"
                + "event: responsecompleted\n"
                + "data: {\"type\":\"ResponseCompleted\",\"items\":[{\"role\":\"assistant\",\"content\":[{\"type\":\"output_text\",\"text\":\"Recebi. Pode continuar com o teste.\"}]}]}\n"
                + "\n"
                + "event: complete\n"
                + "data: {\"status\":\"finished\"}\n";

        RawModelResponse rawResponse = new RawModelResponse(raw, 200, "text/event-stream");

        ModelExecutionResponse response = codec.decode(rawResponse, new ModelExecutionProfile());

        assertEquals("Recebi. Pode continuar com o teste.", response.getPrimaryText());
        assertEquals(200, response.getHttpStatusCode());
    }

    @Test
    public void deveUsarDeltaQuandoNaoHouverBlocoFinal() throws Exception {
        StreamingSseResponseCodec codec = new StreamingSseResponseCodec();

        String raw = ""
                + "event: outputtextdelta\n"
                + "data: {\"type\":\"OutputTextDelta\",\"delta\":\"ola \"}\n"
                + "\n"
                + "event: outputtextdelta\n"
                + "data: {\"type\":\"OutputTextDelta\",\"delta\":\"mundo\"}\n"
                + "\n";

        ModelExecutionResponse response = codec.decode(
                new RawModelResponse(raw, 200, "text/event-stream"),
                new ModelExecutionProfile()
        );

        assertEquals("ola mundo", response.getPrimaryText());
    }
}