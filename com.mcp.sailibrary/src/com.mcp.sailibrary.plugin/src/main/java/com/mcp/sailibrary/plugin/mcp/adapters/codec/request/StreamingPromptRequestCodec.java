package com.mcp.sailibrary.plugin.mcp.adapters.codec.request;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionRequest;
import com.mcp.sailibrary.plugin.mcp.ports.ModelRequestCodec;

/** * Codec de request para o endpoint streaming usando prompt simples. * * <p>O prompt final entra como uma unica mensagem role=user.</p> * * @author Renato Tomaz Nati * @since 2026-05-26 */
public class StreamingPromptRequestCodec implements ModelRequestCodec {

    @Override
    public String encode(ModelExecutionRequest request, ModelExecutionProfile profile) throws Exception {
        if (request == null) {
            throw new IllegalArgumentException("Erro Operacional: request nao pode ser nulo.");
        }

        if (profile == null) {
            throw new IllegalArgumentException("Erro Operacional: profile nao pode ser nulo.");
        }

        String modelName = profile.resolveEffectiveModelName();
        if (isBlank(modelName)) {
            throw new IllegalArgumentException("Erro Operacional: streaming model name nao pode ser vazio.");
        }

        String prompt = request.getPrompt() != null ? request.getPrompt() : "";

        JsonObject root = new JsonObject();

        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("content", prompt);
        message.addProperty("role", "user");
        messages.add(message);
        root.add("messages", messages);

        addNullableString(root, "conversationId", profile.getConversationId());
        addNullableString(root, "workspaceId", profile.getWorkspaceId());
        root.addProperty("createScheduledTaskEnabled", profile.isCreateScheduledTask());

        JsonObject parameters = new JsonObject();
        parameters.addProperty("temperature", safeDouble(profile.getCreativity(), 0.0d));
        parameters.addProperty("maxTokens", safeInteger(profile.getMaxTokens(), 16384));
        parameters.addProperty("enableStreaming", profile.isEnableStreaming());
        parameters.addProperty("model", modelName);
        parameters.addProperty("fileSearch", profile.isFileSearch());
        parameters.addProperty("codeInterpreter", profile.isCodeInterpreter());
        parameters.addProperty("webSearch", profile.isWebSearch());

        parameters.add("toolSelector", new JsonArray());
        parameters.add("mcpServers", new JsonArray());

        parameters.addProperty("instructions", safeString(profile.getInstructions()));
        parameters.addProperty("indexerEnabled", profile.isIndexerEnabled());
        parameters.addProperty("indexerHash", safeString(profile.getIndexerHash()));
        parameters.addProperty("indexerDescription", safeString(profile.getIndexerDescription()));
        parameters.addProperty("includeIndexerMetadata", profile.isIncludeIndexerMetadata());
        parameters.addProperty("createScheduledTask", profile.isCreateScheduledTask());
        parameters.add("voice", JsonNull.INSTANCE);
        parameters.addProperty("indexerNumberOfDocuments", safeInteger(profile.getIndexerNumberOfDocuments(), 3));

        root.add("parameters", parameters);

        return root.toString();
    }

    private void addNullableString(JsonObject root, String propertyName, String value) {
        if (isBlank(value)) {
            root.add(propertyName, JsonNull.INSTANCE);
        } else {
            root.addProperty(propertyName, value.trim());
        }
    }

    private String safeString(String value) {
        return value != null ? value : "";
    }

    private double safeDouble(Double value, double defaultValue) {
        return value != null ? value.doubleValue() : defaultValue;
    }

    private int safeInteger(Integer value, int defaultValue) {
        return value != null && value.intValue() > 0 ? value.intValue() : defaultValue;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}