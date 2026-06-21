package com.mcp.sailibrary.tests.mcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.mcp.sailibrary.plugin.mcp.adapters.codec.request.SaiChatExecuteRequestCodec;
import com.mcp.sailibrary.plugin.mcp.adapters.codec.response.SaiChatExecuteResponseCodec;
import com.mcp.sailibrary.plugin.mcp.core.ModelChannel;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionRequest;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionResponse;
import com.mcp.sailibrary.plugin.mcp.core.RawModelResponse;
import com.mcp.sailibrary.plugin.mcp.core.RequestFormatKind;
import com.mcp.sailibrary.plugin.mcp.core.ResponseFormatKind;
import com.mcp.sailibrary.plugin.mcp.core.TransportKind;

public class SaiChatExecuteConnectionTest {

    @Test
    public void deveReconhecerTerceiroTransporte() {
        assertEquals(TransportKind.SAI_CHAT_EXECUTE_HTTP, TransportKind.fromProperty("sai_chatexecute"));
        assertEquals(RequestFormatKind.SAI_CHAT_EXECUTE, RequestFormatKind.fromProperty("chatexecute"));
        assertEquals(ResponseFormatKind.SAI_CHAT_EXECUTE_JSON, ResponseFormatKind.fromProperty("chatexecute"));
    }

    @Test
    public void deveMontarPayloadChatExecute() throws Exception {
        ModelExecutionRequest request = new ModelExecutionRequest();
        request.setChannel(ModelChannel.PLANNER);
        request.setPrompt("ola");

        String json = new SaiChatExecuteRequestCodec().encode(request, new ModelExecutionProfile());

        assertTrue(json.contains("\"messages\""));
        assertTrue(json.contains("\"role\":\"user\""));
        assertTrue(json.contains("\"content\":\"ola\""));
    }

    @Test
    public void deveExtrairMensagemDaRespostaSai() throws Exception {
        RawModelResponse raw = new RawModelResponse("{\"message\":\"resposta ok\"}", 200, "application/json");
        ModelExecutionResponse response = new SaiChatExecuteResponseCodec().decode(raw, new ModelExecutionProfile());
        assertEquals("resposta ok", response.getPrimaryText());
    }

    @Test
    public void deveExtrairMensagemAninhadaDaRespostaSai() throws Exception {
        RawModelResponse raw = new RawModelResponse("{\"message\":{\"content\":\"texto interno\"}}", 200, "application/json");
        ModelExecutionResponse response = new SaiChatExecuteResponseCodec().decode(raw, new ModelExecutionProfile());
        assertEquals("texto interno", response.getPrimaryText());
    }
}
