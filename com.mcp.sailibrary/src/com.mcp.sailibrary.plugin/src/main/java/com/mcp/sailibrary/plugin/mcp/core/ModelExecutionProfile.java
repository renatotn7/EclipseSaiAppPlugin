package com.mcp.sailibrary.plugin.mcp.core;

/* class_context: feature: "execution profile for legacy and streaming MCP channels" java_version: "21" objective: "Concentrar a configuracao resolvida por canal, incluindo transporte, formatos, ids de conversa e credenciais padrao." libs: - "JDK 21" */

public class ModelExecutionProfile {

    private ModelChannel channel;
    private TransportKind transportKind;
    private RequestFormatKind requestFormatKind;
    private ResponseFormatKind responseFormatKind;

    private String endpointUrl;

    private String legacyModelAlias;
    private String streamingModelName;

    private Double creativity;
    private Integer maxTokens;

    private boolean enableStreaming;
    private boolean fileSearch;
    private boolean codeInterpreter;
    private boolean webSearch;

    private String conversationId;
    private String workspaceId;
    private String instructions;

    private boolean indexerEnabled;
    private String indexerHash;
    private String indexerDescription;
    private boolean includeIndexerMetadata;
    private boolean createScheduledTask;
    private Integer indexerNumberOfDocuments;

    private String defaultApiKey;
    private String defaultCookieValue;

    public ModelExecutionProfile() {
        this.transportKind = TransportKind.SAI_CHAT_EXECUTE_HTTP;
        this.requestFormatKind = RequestFormatKind.SAI_CHAT_EXECUTE;
        this.responseFormatKind = ResponseFormatKind.SAI_CHAT_EXECUTE_JSON;
        this.endpointUrl = "";
        this.legacyModelAlias = "";
        this.streamingModelName = "";
        this.creativity = Double.valueOf(0.0d);
        this.maxTokens = Integer.valueOf(16384);
        this.enableStreaming = true;
        this.fileSearch = true;
        this.codeInterpreter = true;
        this.webSearch = true;
        this.conversationId = "";
        this.workspaceId = "";
        this.instructions = "";
        this.indexerEnabled = false;
        this.indexerHash = "";
        this.indexerDescription = "";
        this.includeIndexerMetadata = false;
        this.createScheduledTask = false;
        this.indexerNumberOfDocuments = Integer.valueOf(3);
        this.defaultApiKey = "";
        this.defaultCookieValue = "";
    }

    public ModelChannel getChannel() {
        return channel;
    }

    public void setChannel(ModelChannel channel) {
        this.channel = channel;
    }

    public TransportKind getTransportKind() {
        return transportKind;
    }

    public void setTransportKind(TransportKind transportKind) {
        this.transportKind = transportKind != null ? transportKind : TransportKind.SAI_CHAT_EXECUTE_HTTP;
    }

    public RequestFormatKind getRequestFormatKind() {
        return requestFormatKind;
    }

    public void setRequestFormatKind(RequestFormatKind requestFormatKind) {
        this.requestFormatKind = requestFormatKind != null ? requestFormatKind : RequestFormatKind.SAI_CHAT_EXECUTE;
    }

    public ResponseFormatKind getResponseFormatKind() {
        return responseFormatKind;
    }

    public void setResponseFormatKind(ResponseFormatKind responseFormatKind) {
        this.responseFormatKind = responseFormatKind != null ? responseFormatKind : ResponseFormatKind.SAI_CHAT_EXECUTE_JSON;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = safeTrim(endpointUrl);
    }

    public String getLegacyModelAlias() {
        return legacyModelAlias;
    }

    public void setLegacyModelAlias(String legacyModelAlias) {
        this.legacyModelAlias = safeTrim(legacyModelAlias);
    }

    public String getStreamingModelName() {
        return streamingModelName;
    }

    public void setStreamingModelName(String streamingModelName) {
        this.streamingModelName = safeTrim(streamingModelName);
    }

    public Double getCreativity() {
        return creativity;
    }

    public void setCreativity(Double creativity) {
        if (creativity == null) {
            this.creativity = Double.valueOf(0.0d);
            return;
        }

        double safeValue = creativity.doubleValue();
        if (safeValue < 0.0d) {
            safeValue = 0.0d;
        }
        if (safeValue > 1.0d) {
            safeValue = 1.0d;
        }

        this.creativity = Double.valueOf(safeValue);
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        if (maxTokens == null || maxTokens.intValue() <= 0) {
            this.maxTokens = Integer.valueOf(16384);
            return;
        }

        this.maxTokens = maxTokens;
    }

    public boolean isEnableStreaming() {
        return enableStreaming;
    }

    public void setEnableStreaming(boolean enableStreaming) {
        this.enableStreaming = enableStreaming;
    }

    public boolean isFileSearch() {
        return fileSearch;
    }

    public void setFileSearch(boolean fileSearch) {
        this.fileSearch = fileSearch;
    }

    public boolean isCodeInterpreter() {
        return codeInterpreter;
    }

    public void setCodeInterpreter(boolean codeInterpreter) {
        this.codeInterpreter = codeInterpreter;
    }

    public boolean isWebSearch() {
        return webSearch;
    }

    public void setWebSearch(boolean webSearch) {
        this.webSearch = webSearch;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = safeTrim(conversationId);
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = safeTrim(workspaceId);
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = safeTrim(instructions);
    }

    public boolean isIndexerEnabled() {
        return indexerEnabled;
    }

    public void setIndexerEnabled(boolean indexerEnabled) {
        this.indexerEnabled = indexerEnabled;
    }

    public String getIndexerHash() {
        return indexerHash;
    }

    public void setIndexerHash(String indexerHash) {
        this.indexerHash = safeTrim(indexerHash);
    }

    public String getIndexerDescription() {
        return indexerDescription;
    }

    public void setIndexerDescription(String indexerDescription) {
        this.indexerDescription = safeTrim(indexerDescription);
    }

    public boolean isIncludeIndexerMetadata() {
        return includeIndexerMetadata;
    }

    public void setIncludeIndexerMetadata(boolean includeIndexerMetadata) {
        this.includeIndexerMetadata = includeIndexerMetadata;
    }

    public boolean isCreateScheduledTask() {
        return createScheduledTask;
    }

    public void setCreateScheduledTask(boolean createScheduledTask) {
        this.createScheduledTask = createScheduledTask;
    }

    public Integer getIndexerNumberOfDocuments() {
        return indexerNumberOfDocuments;
    }

    public void setIndexerNumberOfDocuments(Integer indexerNumberOfDocuments) {
        if (indexerNumberOfDocuments == null || indexerNumberOfDocuments.intValue() <= 0) {
            this.indexerNumberOfDocuments = Integer.valueOf(3);
            return;
        }

        this.indexerNumberOfDocuments = indexerNumberOfDocuments;
    }

    /* * Feature: credenciais padrao resolvidas do properties. * Data: 2026-05-27 04:35 * Chamado por: * - PropertiesBackedModelExecutionProfileResolver.resolve * - UnifiedMcpModelGateway.resolveEffectiveCredentials * Chama: safeTrim * Objetivo: permitir cookie default no streaming e api key default no legado. */
    public String getDefaultApiKey() {
        return defaultApiKey;
    }

    public void setDefaultApiKey(String defaultApiKey) {
        this.defaultApiKey = safeTrim(defaultApiKey);
    }

    public String getDefaultCookieValue() {
        return defaultCookieValue;
    }

    public void setDefaultCookieValue(String defaultCookieValue) {
        this.defaultCookieValue = safeTrim(defaultCookieValue);
    }

    public boolean hasDefaultApiKey() {
        return defaultApiKey != null && defaultApiKey.trim().length() > 0;
    }

    public boolean hasDefaultCookieValue() {
        return defaultCookieValue != null && defaultCookieValue.trim().length() > 0;
    }

    public String resolveEffectiveModelName() {
        if (TransportKind.STREAMING_SSE_HTTP.equals(transportKind)) {
            return safeTrim(streamingModelName);
        }

        return safeTrim(legacyModelAlias);
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}