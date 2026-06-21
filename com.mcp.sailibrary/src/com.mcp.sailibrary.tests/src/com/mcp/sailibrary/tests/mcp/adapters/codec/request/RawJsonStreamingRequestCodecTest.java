package com.mcp.sailibrary.tests.mcp.adapters.codec.request;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.mcp.sailibrary.plugin.mcp.adapters.codec.request.RawJsonStreamingRequestCodec;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionRequest;

/** * Testes do codec de raw json para streaming. * * @author Renato Tomaz Nati */
public class RawJsonStreamingRequestCodecTest {

    @Test
    public void deveAceitarRawJsonValido() throws Exception {
        RawJsonStreamingRequestCodec codec = new RawJsonStreamingRequestCodec();

        ModelExecutionRequest request = new ModelExecutionRequest();
        request.setRawJsonBody("{\"x\":1}");

        String json = codec.encode(request, new ModelExecutionProfile());

        assertEquals("{\"x\":1}", json);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deveFalharQuandoRawJsonForInvalido() throws Exception {
        RawJsonStreamingRequestCodec codec = new RawJsonStreamingRequestCodec();

        ModelExecutionRequest request = new ModelExecutionRequest();
        request.setRawJsonBody("{x");

        codec.encode(request, new ModelExecutionProfile());
    }

    @Test(expected = IllegalArgumentException.class)
    public void deveFalharQuandoRawJsonNaoForInformado() throws Exception {
        RawJsonStreamingRequestCodec codec = new RawJsonStreamingRequestCodec();

        ModelExecutionRequest request = new ModelExecutionRequest();

        codec.encode(request, new ModelExecutionProfile());
    }
}