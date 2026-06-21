package com.mcp.sailibrary.plugin.mcp.adapters.codec.response;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionResponse;
import com.mcp.sailibrary.plugin.mcp.core.RawModelResponse;
import com.mcp.sailibrary.plugin.mcp.ports.ModelResponseCodec;

/** * Codec de resposta para SAI /chatexecute. * * @author Renato Tomaz Nati * @since 2026-06-19 */
public class SaiChatExecuteResponseCodec implements ModelResponseCodec {

    @Override
    public ModelExecutionResponse decode(RawModelResponse rawResponse, ModelExecutionProfile profile) throws Exception {
        System.out.println("[SAI CHATEXECUTE RESPONSE CODEC] ===================================");
        System.out.println("[SAI CHATEXECUTE RESPONSE CODEC] phase=START");

        String rawBody = rawResponse != null && rawResponse.getRawBody() != null
                ? rawResponse.getRawBody()
                : "";

        System.out.println("[SAI CHATEXECUTE RESPONSE CODEC] httpStatus=" + (rawResponse != null ? rawResponse.getStatusCode() : 0));
        System.out.println("[SAI CHATEXECUTE RESPONSE CODEC] contentType=" + (rawResponse != null ? rawResponse.getContentType() : ""));
        System.out.println("[SAI CHATEXECUTE RESPONSE CODEC] rawLength=" + rawBody.length());

        ModelExecutionResponse response = new ModelExecutionResponse();

        if (rawResponse != null) {
            response.setRawResponseBody(rawResponse.getRawBody());
            response.setHttpStatusCode(rawResponse.getStatusCode());
            response.setContentType(rawResponse.getContentType());
        }

        String primaryText = "";

        try {
            JsonElement rootElement = JsonParser.parseString(rawBody);

            if (rootElement != null && rootElement.isJsonObject()) {
                JsonObject root = rootElement.getAsJsonObject();

                primaryText = readString(root, "message");

                if (primaryText.length() == 0 && root.has("message") && root.get("message").isJsonObject()) {
                    primaryText = readString(root.getAsJsonObject("message"), "content");
                }

                if (primaryText.length() == 0) {
                    primaryText = readFirstAvailableString(root);
                }

                System.out.println("[SAI CHATEXECUTE RESPONSE CODEC] maxTokensExpired=" + readString(root, "maxTokensExpired"));
                System.out.println("[SAI CHATEXECUTE RESPONSE CODEC] toolHistoryId=" + readString(root, "toolHistoryId"));
            } else {
                primaryText = rawBody;
            }
        } catch (Throwable throwable) {
            System.out.println("[SAI CHATEXECUTE RESPONSE CODEC] Falha ao parsear JSON SAI: "
                    + throwable.getClass().getName() + " - " + throwable.getMessage());
            primaryText = rawBody;
        }

        if (primaryText == null || primaryText.trim().length() == 0) {
            primaryText = "[SAI Warning: resposta vazia.]";
        }

        response.setPrimaryText(primaryText);

        System.out.println("[SAI CHATEXECUTE RESPONSE CODEC] primaryTextLength=" + primaryText.length());
        System.out.println("[SAI CHATEXECUTE RESPONSE CODEC] phase=END");
        System.out.println("[SAI CHATEXECUTE RESPONSE CODEC] ===================================");

        return response;
    }

    private String readString(JsonObject json, String propertyName) {
        if (json == null || propertyName == null || !json.has(propertyName) || json.get(propertyName).isJsonNull()) {
            return "";
        }

        try {
            return json.get(propertyName).getAsString();
        } catch (Exception exception) {
            return "";
        }
    }

    private String readFirstAvailableString(JsonObject json) {
        if (json == null) {
            return "";
        }

        String[] fields = new String[] {
                "content",
                "text",
                "response",
                "answer",
                "result",
                "output"
        };

        for (int i = 0; i < fields.length; i++) {
            String value = readString(json, fields[i]);
            if (value.length() > 0) {
                return value;
            }
        }

        return json.toString();
    }
}