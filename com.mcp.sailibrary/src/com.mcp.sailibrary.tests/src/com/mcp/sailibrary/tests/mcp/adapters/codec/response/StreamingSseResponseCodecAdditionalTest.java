package com.mcp.sailibrary.tests.mcp.adapters.codec.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.mcp.sailibrary.plugin.mcp.adapters.codec.response.StreamingSseResponseCodec;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionResponse;
import com.mcp.sailibrary.plugin.mcp.core.RawModelResponse;

/** * Testes adicionais do codec SSE para cobrir caminhos alternativos. * * @author Renato Tomaz Nati */
public class StreamingSseResponseCodecAdditionalTest {

    @Test
    public void deveUsarOutputItemDoneQuandoNaoHaResponseCompleted() throws Exception {
        StreamingSseResponseCodec codec = new StreamingSseResponseCodec();

        String raw = ""
                + "event: outputitemdone\n"
                + "data: {\"type\":\"OutputItemDone\",\"item\":{\"content\":[{\"type\":\"output_text\",\"text\":\"texto output item\"}]}}\n"
                + "\n";

        ModelExecutionResponse response = codec.decode(
                new RawModelResponse(raw, 200, "text/event-stream"),
                new ModelExecutionProfile()
        );

        assertEquals("texto output item", response.getPrimaryText());
    }

    @Test
    public void deveUsarContentPartDoneQuandoNaoHouverRespostaFinal() throws Exception {
        StreamingSseResponseCodec codec = new StreamingSseResponseCodec();

        String raw = ""
                + "event: contentpartdone\n"
                + "data: {\"type\":\"ContentPartDone\",\"part\":{\"type\":\"output_text\",\"text\":\"texto part done\"}}\n"
                + "\n";

        ModelExecutionResponse response = codec.decode(
                new RawModelResponse(raw, 200, "text/event-stream"),
                new ModelExecutionProfile()
        );

        assertEquals("texto part done", response.getPrimaryText());
    }

    @Test
    public void deveRetornarTextoConsolidadoQuandoVierJsonDiretoDeResponseCompleted() throws Exception {
        StreamingSseResponseCodec codec = new StreamingSseResponseCodec();

        String raw =
                "{\"response\":{\"output\":[{\"content\":[{\"type\":\"output_text\",\"text\":\"texto json direto\"}]}]}}";

        ModelExecutionResponse response = codec.decode(
                new RawModelResponse(raw, 200, "application/json"),
                new ModelExecutionProfile()
        );

        assertNotNull(response);
        assertNotNull(response.getPrimaryText());
        assertTrue(response.getPrimaryText().contains("texto json direto"));
    }
}