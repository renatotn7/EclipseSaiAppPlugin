package com.mcp.sailibrary.plugin.mcp.adapters.codec.response;

import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionResponse;
import com.mcp.sailibrary.plugin.mcp.core.RawModelResponse;
import com.mcp.sailibrary.plugin.mcp.ports.ModelResponseCodec;

/** * Codec simples para respostas tratadas como texto puro. * * @author Renato Tomaz Nati * @since 2026-05-26 */
public class PlainTextResponseCodec implements ModelResponseCodec {

    @Override
    public ModelExecutionResponse decode(RawModelResponse rawResponse, ModelExecutionProfile profile) throws Exception {
        ModelExecutionResponse response = new ModelExecutionResponse();

        String rawBody = rawResponse != null ? rawResponse.getRawBody() : "";

        response.setPrimaryText(rawBody != null ? rawBody : "");
        response.setRawResponseBody(rawBody);
        response.setHttpStatusCode(rawResponse != null ? rawResponse.getStatusCode() : 0);
        response.setContentType(rawResponse != null ? rawResponse.getContentType() : "");

        return response;
    }
}