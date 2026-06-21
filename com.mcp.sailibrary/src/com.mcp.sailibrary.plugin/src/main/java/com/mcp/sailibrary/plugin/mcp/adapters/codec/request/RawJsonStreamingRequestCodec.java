package com.mcp.sailibrary.plugin.mcp.adapters.codec.request;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionRequest;
import com.mcp.sailibrary.plugin.mcp.ports.ModelRequestCodec;

/** * Codec de request para o endpoint streaming quando o body JSON ja vem pronto. * * @author Renato Tomaz Nati * @since 2026-05-26 */
public class RawJsonStreamingRequestCodec implements ModelRequestCodec {

    @Override
    public String encode(ModelExecutionRequest request, ModelExecutionProfile profile) throws Exception {
        if (request == null) {
            throw new IllegalArgumentException("Erro Operacional: request nao pode ser nulo.");
        }

        if (!request.hasRawJsonBody()) {
            throw new IllegalArgumentException(
                    "Erro Operacional: rawJsonBody obrigatorio para request format STREAMING_RAW_JSON.");
        }

        String rawJsonBody = request.getRawJsonBody().trim();

        try {
            JsonElement element = JsonParser.parseString(rawJsonBody);
            return element.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Erro Operacional: rawJsonBody invalido para STREAMING_RAW_JSON. " + e.getMessage(), e);
        }
    }
}