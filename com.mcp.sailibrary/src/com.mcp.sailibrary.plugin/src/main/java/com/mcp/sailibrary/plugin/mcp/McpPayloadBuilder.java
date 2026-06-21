package com.mcp.sailibrary.plugin.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;

public class McpPayloadBuilder {

    public McpPayloadBuilder() {
    }

    /* * Feature: monta payload legado de tools call. * Data: 2026-05-27 04:20 * Chamado por: * - UnifiedMcpModelGateway.callModel * - LegacyMcpRequestCodec.encode * Chama: * - safeString * - truncateForDebug * Objetivo: * - preservar o contrato legado em JSON-RPC * - manter rastreabilidade de entrada e saida para diagnostico */
    public String buildToolsCallPayload(String modelName, String promptContent) {
        String safeModelName = safeString(modelName);
        String safePromptContent = safeString(promptContent);

        JsonObject argumentsObject = new JsonObject();
        argumentsObject.addProperty("prompt", safePromptContent);

        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("name", "chat");
        paramsObject.add("arguments", argumentsObject);

        JsonObject payloadObject = new JsonObject();
        payloadObject.addProperty("jsonrpc", "2.0");
        payloadObject.addProperty("id", "legacy-tools-call");
        payloadObject.addProperty("method", "tools/call");
        payloadObject.add("params", paramsObject);

        if (!isBlank(safeModelName)) {
            payloadObject.addProperty("model", safeModelName);
        }

        String payload = payloadObject.toString();

        System.out.println("[MCP PAYLOAD BUILDER] ===============================================");
        System.out.println("[MCP PAYLOAD BUILDER] modo=LEGACY_TOOLS_CALL");
        System.out.println("[MCP PAYLOAD BUILDER] modelName=" + safeModelName);
        System.out.println("[MCP PAYLOAD BUILDER] promptLength=" + safePromptContent.length());
        System.out.println("[MCP PAYLOAD BUILDER] promptPreview=");
        System.out.println(truncateForDebug(safePromptContent, 2000));
        System.out.println("[MCP PAYLOAD BUILDER] payload=");
        System.out.println(truncateForDebug(payload, 12000));
        System.out.println("[MCP PAYLOAD BUILDER] ===============================================");

        return payload;
    }

    public String buildStreamingPromptPayload(String prompt, ModelExecutionProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("Erro Operacional: profile nao pode ser nulo.");
        }

        String safePrompt = safeString(prompt);

        JsonObject messageObject = new JsonObject();
        messageObject.addProperty("content", safePrompt);
        messageObject.addProperty("role", "user");

        JsonArray messagesArray = new JsonArray();
        messagesArray.add(messageObject);

        JsonObject parametersObject = new JsonObject();
        parametersObject.addProperty("temperature", safeDouble(profile.getCreativity(), 0.0d));
        parametersObject.addProperty("maxTokens", safeInteger(profile.getMaxTokens(), 16384));
        parametersObject.addProperty("enableStreaming", profile.isEnableStreaming());
        parametersObject.addProperty("model", safeString(profile.getStreamingModelName()));
        parametersObject.addProperty("fileSearch", profile.isFileSearch());
        parametersObject.addProperty("codeInterpreter", profile.isCodeInterpreter());
        parametersObject.addProperty("webSearch", profile.isWebSearch());

        parametersObject.add("toolSelector", new JsonArray());
        parametersObject.add("mcpServers", new JsonArray());

        parametersObject.addProperty("instructions", safeString(profile.getInstructions()));
        parametersObject.addProperty("indexerEnabled", profile.isIndexerEnabled());
        parametersObject.addProperty("indexerHash", safeString(profile.getIndexerHash()));
        parametersObject.addProperty("indexerDescription", safeString(profile.getIndexerDescription()));
        parametersObject.addProperty("includeIndexerMetadata", profile.isIncludeIndexerMetadata());
        parametersObject.addProperty("createScheduledTask", profile.isCreateScheduledTask());
        parametersObject.add("voice", JsonNull.INSTANCE);
        parametersObject.addProperty("indexerNumberOfDocuments", safeInteger(profile.getIndexerNumberOfDocuments(), 3));

        JsonObject rootObject = new JsonObject();
        rootObject.add("messages", messagesArray);

        if (isBlank(profile.getConversationId())) {
            rootObject.add("conversationId", JsonNull.INSTANCE);
        } else {
            rootObject.addProperty("conversationId", safeString(profile.getConversationId()));
        }

        if (isBlank(profile.getWorkspaceId())) {
            rootObject.add("workspaceId", JsonNull.INSTANCE);
        } else {
            rootObject.addProperty("workspaceId", safeString(profile.getWorkspaceId()));
        }

        rootObject.addProperty("createScheduledTaskEnabled", profile.isCreateScheduledTask());
        rootObject.add("parameters", parametersObject);

        String payload = rootObject.toString();

        System.out.println("[MCP PAYLOAD BUILDER] ===============================================");
        System.out.println("[MCP PAYLOAD BUILDER] modo=STREAMING_PROMPT");
        System.out.println("[MCP PAYLOAD BUILDER] promptLength=" + safePrompt.length());
        System.out.println("[MCP PAYLOAD BUILDER] conversationId=" + safeString(profile.getConversationId()));
        System.out.println("[MCP PAYLOAD BUILDER] workspaceId=" + safeString(profile.getWorkspaceId()));
        System.out.println("[MCP PAYLOAD BUILDER] model=" + safeString(profile.getStreamingModelName()));
        System.out.println("[MCP PAYLOAD BUILDER] payload=");
        System.out.println(payload);
        System.out.println("[MCP PAYLOAD BUILDER] ===============================================");

        return payload;
    }

    public String normalizeRawJsonPayload(String rawJsonPayload) {
        if (isBlank(rawJsonPayload)) {
            return "{}";
        }

        String trimmedPayload = rawJsonPayload.trim();

        try {
            JsonElement parsedElement = JsonParser.parseString(trimmedPayload);
            String normalizedPayload = parsedElement.toString();

            System.out.println("[MCP PAYLOAD BUILDER] modo=RAW_JSON_NORMALIZE");
            System.out.println("[MCP PAYLOAD BUILDER] rawInput=");
            System.out.println(trimmedPayload);
            System.out.println("[MCP PAYLOAD BUILDER] rawOutput=");
            System.out.println(normalizedPayload);

            return normalizedPayload;
        } catch (Exception exception) {
            System.out.println("[MCP PAYLOAD BUILDER] falha ao normalizar raw json: " + exception.getMessage());
            return trimmedPayload;
        }
    }

    public static String escapeForJsonTransport(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder(value.length() + 16);

        for (int index = 0; index < value.length(); index++) {
            char currentChar = value.charAt(index);

            if (currentChar == '\\') {
                builder.append("\\\\");
                continue;
            }

            if (currentChar == '"') {
                builder.append("\\\"");
                continue;
            }

            if (currentChar == '\n') {
                builder.append("\\n");
                continue;
            }

            if (currentChar == '\r') {
                builder.append("\\r");
                continue;
            }

            if (currentChar == '\t') {
                builder.append("\\t");
                continue;
            }

            builder.append(currentChar);
        }

        return builder.toString();
    }

    private void addNullableString(JsonObject targetObject, String propertyName, String value) {
        if (targetObject == null || propertyName == null) {
            return;
        }

        if (isBlank(value)) {
            targetObject.add(propertyName, JsonNull.INSTANCE);
            return;
        }

        targetObject.addProperty(propertyName, value.trim());
    }

    private String resolveStreamingModelName(ModelExecutionProfile profile) {
        String streamingModelName = safeString(profile.getStreamingModelName());
        if (!isBlank(streamingModelName)) {
            return streamingModelName;
        }

        String effectiveModelName = safeString(profile.resolveEffectiveModelName());
        if (!isBlank(effectiveModelName)) {
            return effectiveModelName;
        }

        return "gpt-5.4-2026-03-05";
    }

    private String safeString(String value) {
        return value == null ? "" : value.trim();
    }

    private double safeDouble(Double value, double defaultValue) {
        return value != null ? value.doubleValue() : defaultValue;
    }

    private int safeInteger(Integer value, int defaultValue) {
        if (value == null || value.intValue() <= 0) {
            return defaultValue;
        }

        return value.intValue();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private String truncateForDebug(String value, int maxLength) {
        if (value == null) {
            return "";
        }

        if (maxLength <= 0 || value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength) + "...";
    }
}