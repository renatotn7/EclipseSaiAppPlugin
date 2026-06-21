package com.mcp.sailibrary.plugin.mcp.multimodel.gateway;

import com.mcp.sailibrary.plugin.mcp.McpHttpGateway;
import com.mcp.sailibrary.plugin.mcp.McpPayloadBuilder;
import com.mcp.sailibrary.plugin.mcp.adapters.codec.request.LegacyMcpRequestCodec;
import com.mcp.sailibrary.plugin.mcp.adapters.codec.request.RawJsonStreamingRequestCodec;
import com.mcp.sailibrary.plugin.mcp.adapters.codec.request.StreamingPromptRequestCodec;
import com.mcp.sailibrary.plugin.mcp.adapters.codec.request.SaiChatExecuteRequestCodec;
import com.mcp.sailibrary.plugin.mcp.adapters.codec.response.LegacyMcpResponseCodec;
import com.mcp.sailibrary.plugin.mcp.adapters.codec.response.PlainTextResponseCodec;
import com.mcp.sailibrary.plugin.mcp.adapters.codec.response.StreamingSseResponseCodec;
import com.mcp.sailibrary.plugin.mcp.adapters.codec.response.SaiChatExecuteResponseCodec;
import com.mcp.sailibrary.plugin.mcp.adapters.config.PropertiesBackedModelExecutionProfileResolver;
import com.mcp.sailibrary.plugin.mcp.adapters.connector.LegacyJsonRpcConnector;
import com.mcp.sailibrary.plugin.mcp.adapters.connector.StreamingSseConnector;
import com.mcp.sailibrary.plugin.mcp.adapters.connector.SaiChatExecuteConnector;
import com.mcp.sailibrary.plugin.mcp.adapters.eclipse.EclipseToolPromptSectionsAdapter;
import com.mcp.sailibrary.plugin.mcp.application.McpExecutionSupport;
import com.mcp.sailibrary.plugin.mcp.application.McpExecutionWiring;
import com.mcp.sailibrary.plugin.mcp.application.ModelExecutionEngine;
import com.mcp.sailibrary.plugin.mcp.core.McpAccessCredentials;
import com.mcp.sailibrary.plugin.mcp.core.ModelChannel;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionResponse;

/* --- version: "2.0" libraries: - com.mcp.sailibrary.plugin.mcp.McpHttpGateway - com.mcp.sailibrary.plugin.mcp.McpPayloadBuilder - hexagonal MCP support classes objetivo: "Preservar compatibilidade com o callModel legado e adicionar execucao por canal cognitivo usando transporte, request e response configuraveis." --- */

/** * Gateway unificado para chamadas MCP. * * <p>Compatibilidade preservada:</p> * <ul> * <li>callModel(modelName, prompt, apiKey) continua funcionando no modo legado</li> * </ul> * * <p>Fluxo novo:</p> * <ul> * <li>execucao por canal cognitivo</li> * <li>transporte configuravel por properties</li> * <li>request/response format configuraveis por properties</li> * </ul> * * @author Renato Tomaz Nati * @since 2026-05-26 */
public class UnifiedMcpModelGateway {

    public static final String DEFAULT_MCP_API_URL = "https://sai-library.saiapplications.com/api/mcp";

    private final String mcpApiUrl;
    private final McpHttpGateway mcpHttpGateway;
    private final McpPayloadBuilder mcpPayloadBuilder;
    private final McpExecutionSupport mcpExecutionSupport;

    public UnifiedMcpModelGateway() {
        this(DEFAULT_MCP_API_URL, createDefaultExecutionSupport());
    }

    public UnifiedMcpModelGateway(String mcpApiUrl) {
        this(mcpApiUrl, createDefaultExecutionSupport());
    }

    public UnifiedMcpModelGateway(String mcpApiUrl, McpExecutionSupport mcpExecutionSupport) {
        this.mcpApiUrl = safeTrim(mcpApiUrl);
        this.mcpHttpGateway = new McpHttpGateway();
        this.mcpPayloadBuilder = new McpPayloadBuilder();
        this.mcpExecutionSupport = mcpExecutionSupport != null
                ? mcpExecutionSupport
                : createDefaultExecutionSupport();
    }

    /** * Compatibilidade com o fluxo legado. * * <p>Este metodo preserva o comportamento antigo: * endpoint MCP legado + payload JSON-RPC + modelName explicito.</p> */
    public String callModel(String modelName, String prompt, String apiKey) throws Exception {
        if (isBlank(modelName)) {
            throw new IllegalArgumentException("Erro Operacional: O nome do modelo MCP nao pode ser vazio.");
        }

        String promptSeguro = safeTrim(prompt);

        String payload = mcpPayloadBuilder.buildToolsCallPayload(
                modelName,
                "Prompt: " + promptSeguro
        );

        System.out.println("[MCP GATEWAY DEBUG] ==================================================");
        System.out.println("[MCP GATEWAY DEBUG] modo=LEGACY_COMPAT");
        System.out.println("[MCP GATEWAY DEBUG] url=" + mcpApiUrl);
        System.out.println("[MCP GATEWAY DEBUG] modelName=" + modelName);
        System.out.println("[MCP GATEWAY DEBUG] modelDisplayName=" + formatModelForLog(modelName, ""));
        System.out.println("[MCP GATEWAY DEBUG] apiKeyConfigured=" + (!isBlank(apiKey) ? "true" : "false"));
        System.out.println("[MCP GATEWAY DEBUG] promptLength=" + promptSeguro.length());
        System.out.println("[MCP GATEWAY DEBUG] payloadLength=" + payload.length());
        System.out.println("[MCP GATEWAY DEBUG] payloadEntrada=");
        System.out.println(truncateForDebug(payload, 12000));

        String rawResponse = mcpHttpGateway.postJsonRpc(mcpApiUrl, apiKey, payload);

        System.out.println("[MCP GATEWAY DEBUG] rawResponseLength=" + (rawResponse != null ? rawResponse.length() : 0));
        System.out.println("[MCP GATEWAY DEBUG] rawResponseSaida=");
        System.out.println(truncateForDebug(rawResponse, 12000));
        System.out.println("[MCP GATEWAY DEBUG] ==================================================");

        return rawResponse;
    }

    public ModelExecutionResponse executePlannerPrompt(String prompt, String apiKey) throws Exception {
        return executeChannelPrompt(
                ModelChannel.PLANNER,
                prompt,
                McpAccessCredentials.forApiKey(apiKey)
        );
    }

    public ModelExecutionResponse executePlannerPrompt(String prompt, McpAccessCredentials credentials) throws Exception {
        return executeChannelPrompt(ModelChannel.PLANNER, prompt, credentials);
    }

    public ModelExecutionResponse executeInvestigatorPrompt(String prompt, String apiKey) throws Exception {
        return executeChannelPrompt(ModelChannel.INVESTIGATOR, prompt, McpAccessCredentials.forApiKey(apiKey));
    }

    public ModelExecutionResponse executeInvestigatorPrompt(String prompt, McpAccessCredentials credentials) throws Exception {
        return executeChannelPrompt(ModelChannel.INVESTIGATOR, prompt, credentials);
    }

    public ModelExecutionResponse executeCodeGeneratorPrompt(String prompt, String apiKey) throws Exception {
        return executeChannelPrompt(
                ModelChannel.CODE_GENERATOR,
                prompt,
                McpAccessCredentials.forApiKey(apiKey)
        );
    }
           

    public ModelExecutionResponse executeCodeGeneratorPrompt(String prompt, McpAccessCredentials credentials) throws Exception {
        return executeChannelPrompt(ModelChannel.CODE_GENERATOR, prompt, credentials);
    }

    public ModelExecutionResponse executeCodeAuditorPrompt(String prompt, String apiKey) throws Exception {
        return executeChannelPrompt(
                ModelChannel.CODE_AUDITOR,
                prompt,
                McpAccessCredentials.forApiKey(apiKey)
        );
    }

    public ModelExecutionResponse executeCodeAuditorPrompt(String prompt, McpAccessCredentials credentials) throws Exception {
        return executeChannelPrompt(ModelChannel.CODE_AUDITOR, prompt, credentials);
    }

    public ModelExecutionResponse executeSummarizerPrompt(String prompt, String apiKey) throws Exception {
        return executeChannelPrompt(
                ModelChannel.SUMMARIZER,
                prompt,
                McpAccessCredentials.forApiKey(apiKey)
        );
    }

    public ModelExecutionResponse executeSummarizerPrompt(String prompt, McpAccessCredentials credentials) throws Exception {
        return executeChannelPrompt(ModelChannel.SUMMARIZER, prompt, credentials);
    }

    public ModelExecutionResponse executeChannelPrompt(ModelChannel channel, String prompt, McpAccessCredentials credentials) throws Exception {

        if (channel == null) {
            throw new IllegalArgumentException("Erro Operacional: channel nao pode ser nulo.");
        }

        ModelExecutionProfile profile = resolveProfile(channel);
        McpAccessCredentials effectiveCredentials = resolveEffectiveCredentials(profile, credentials);
        String promptSeguro = safeTrim(prompt);

        System.out.println("[MCP GATEWAY DEBUG] ==================================================");
        System.out.println("[MCP GATEWAY DEBUG] modo=CHANNEL_PROMPT");
        System.out.println("[MCP GATEWAY DEBUG] channel=" + channel.name());
        System.out.println("[MCP GATEWAY DEBUG] transport=" + profile.getTransportKind());
        System.out.println("[MCP GATEWAY DEBUG] requestFormat=" + profile.getRequestFormatKind());
        System.out.println("[MCP GATEWAY DEBUG] responseFormat=" + profile.getResponseFormatKind());
        System.out.println("[MCP GATEWAY DEBUG] endpoint=" + profile.getEndpointUrl());
        System.out.println("[MCP GATEWAY DEBUG] effectiveModel=" + profile.resolveEffectiveModelName());
        System.out.println("[MCP GATEWAY DEBUG] modelDisplayName=" + formatModelForLog(profile.getLegacyModelAlias(), profile.getStreamingModelName()));
        System.out.println("[MCP GATEWAY DEBUG] creativity=" + profile.getCreativity());
        System.out.println("[MCP GATEWAY DEBUG] maxTokens=" + profile.getMaxTokens());
        System.out.println("[MCP GATEWAY DEBUG] enableStreaming=" + profile.isEnableStreaming());
        System.out.println("[MCP GATEWAY DEBUG] fileSearch=" + profile.isFileSearch());
        System.out.println("[MCP GATEWAY DEBUG] codeInterpreter=" + profile.isCodeInterpreter());
        System.out.println("[MCP GATEWAY DEBUG] webSearch=" + profile.isWebSearch());
        System.out.println("[MCP GATEWAY DEBUG] conversationId=" + profile.getConversationId());
        System.out.println("[MCP GATEWAY DEBUG] workspaceId=" + profile.getWorkspaceId());
        System.out.println("[MCP GATEWAY DEBUG] hasApiKey=" + effectiveCredentials.hasApiKey());
        System.out.println("[MCP GATEWAY DEBUG] hasCookieValue=" + effectiveCredentials.hasCookieValue());
        System.out.println("[MCP GATEWAY DEBUG] promptLength=" + promptSeguro.length());
        System.out.println("[MCP GATEWAY DEBUG] promptEntrada=");
        System.out.println(truncateForDebug(promptSeguro, 12000));

        ModelExecutionResponse executionResponse = mcpExecutionSupport.executePrompt(channel, promptSeguro, effectiveCredentials);

        System.out.println("[MCP GATEWAY DEBUG] responsePrimaryTextLength="
                + (executionResponse != null && executionResponse.getPrimaryText() != null
                        ? executionResponse.getPrimaryText().length()
                        : 0));
        System.out.println("[MCP GATEWAY DEBUG] responsePrimaryText=");
        System.out.println(truncateForDebug(
                executionResponse != null ? executionResponse.getPrimaryText() : "",
                12000
        ));
        System.out.println("[MCP GATEWAY DEBUG] responseRawBodyLength="
                + (executionResponse != null && executionResponse.getRawResponseBody() != null
                        ? executionResponse.getRawResponseBody().length()
                        : 0));
        System.out.println("[MCP GATEWAY DEBUG] responseRawBody=");
        System.out.println(truncateForDebug(
                executionResponse != null ? executionResponse.getRawResponseBody() : "",
                12000
        ));
        System.out.println("[MCP GATEWAY DEBUG] ==================================================");

        return executionResponse;
    }

    public ModelExecutionResponse executeChannelRawJson(ModelChannel channel, String rawJsonBody, McpAccessCredentials credentials) throws Exception {

        if (channel == null) {
            throw new IllegalArgumentException("Erro Operacional: channel nao pode ser nulo.");
        }

        ModelExecutionProfile profile = resolveProfile(channel);
        McpAccessCredentials effectiveCredentials = resolveEffectiveCredentials(profile, credentials);
        String rawJsonSeguro = safeTrim(rawJsonBody);

        System.out.println("[MCP GATEWAY DEBUG] ==================================================");
        System.out.println("[MCP GATEWAY DEBUG] modo=CHANNEL_RAW_JSON");
        System.out.println("[MCP GATEWAY DEBUG] channel=" + channel.name());
        System.out.println("[MCP GATEWAY DEBUG] transport=" + profile.getTransportKind());
        System.out.println("[MCP GATEWAY DEBUG] requestFormat=" + profile.getRequestFormatKind());
        System.out.println("[MCP GATEWAY DEBUG] responseFormat=" + profile.getResponseFormatKind());
        System.out.println("[MCP GATEWAY DEBUG] endpoint=" + profile.getEndpointUrl());
        System.out.println("[MCP GATEWAY DEBUG] effectiveModel=" + profile.resolveEffectiveModelName());
        System.out.println("[MCP GATEWAY DEBUG] modelDisplayName=" + formatModelForLog(profile.getLegacyModelAlias(), profile.getStreamingModelName()));
        System.out.println("[MCP GATEWAY DEBUG] conversationId=" + profile.getConversationId());
        System.out.println("[MCP GATEWAY DEBUG] workspaceId=" + profile.getWorkspaceId());
        System.out.println("[MCP GATEWAY DEBUG] hasApiKey=" + effectiveCredentials.hasApiKey());
        System.out.println("[MCP GATEWAY DEBUG] hasCookieValue=" + effectiveCredentials.hasCookieValue());
        System.out.println("[MCP GATEWAY DEBUG] rawJsonLength=" + rawJsonSeguro.length());
        System.out.println("[MCP GATEWAY DEBUG] rawJsonEntrada=");
        System.out.println(truncateForDebug(rawJsonSeguro, 12000));

        ModelExecutionResponse executionResponse = mcpExecutionSupport.executeRawJson(channel, rawJsonSeguro, effectiveCredentials);

        System.out.println("[MCP GATEWAY DEBUG] responsePrimaryTextLength="
                + (executionResponse != null && executionResponse.getPrimaryText() != null
                        ? executionResponse.getPrimaryText().length()
                        : 0));
        System.out.println("[MCP GATEWAY DEBUG] responsePrimaryText=");
        System.out.println(truncateForDebug(
                executionResponse != null ? executionResponse.getPrimaryText() : "",
                12000
        ));
        System.out.println("[MCP GATEWAY DEBUG] responseRawBodyLength="
                + (executionResponse != null && executionResponse.getRawResponseBody() != null
                        ? executionResponse.getRawResponseBody().length()
                        : 0));
        System.out.println("[MCP GATEWAY DEBUG] responseRawBody=");
        System.out.println(truncateForDebug(
                executionResponse != null ? executionResponse.getRawResponseBody() : "",
                12000
        ));
        System.out.println("[MCP GATEWAY DEBUG] ==================================================");

        return executionResponse;
    }
    private McpAccessCredentials resolveEffectiveCredentials(ModelExecutionProfile profile, McpAccessCredentials explicitCredentials) {

        McpAccessCredentials effectiveCredentials = new McpAccessCredentials();

        String apiKeySource = "none";
        String cookieSource = "none";

        if (explicitCredentials != null && explicitCredentials.hasApiKey()) {
            effectiveCredentials.setApiKey(explicitCredentials.getApiKey());
            apiKeySource = "request";
        } else if (profile != null && profile.hasDefaultApiKey()) {
            effectiveCredentials.setApiKey(profile.getDefaultApiKey());
            apiKeySource = "profile";
        }

        if (explicitCredentials != null && explicitCredentials.hasCookieValue()) {
            effectiveCredentials.setCookieValue(explicitCredentials.getCookieValue());
            cookieSource = "request";
        } else if (profile != null && profile.hasDefaultCookieValue()) {
            effectiveCredentials.setCookieValue(profile.getDefaultCookieValue());
            cookieSource = "profile";
        }

        System.out.println("[MCP GATEWAY DEBUG] resolveEffectiveCredentials.apiKeySource=" + apiKeySource);
        System.out.println("[MCP GATEWAY DEBUG] resolveEffectiveCredentials.cookieSource=" + cookieSource);
        System.out.println("[MCP GATEWAY DEBUG] resolveEffectiveCredentials.hasApiKey=" + effectiveCredentials.hasApiKey());
        System.out.println("[MCP GATEWAY DEBUG] resolveEffectiveCredentials.hasCookieValue=" + effectiveCredentials.hasCookieValue());

        return effectiveCredentials;
    }

    public ModelExecutionProfile resolveProfile(ModelChannel channel) {
        return mcpExecutionSupport.resolveProfile(channel);
    }

    public McpExecutionSupport getMcpExecutionSupport() {
        return mcpExecutionSupport;
    }

    private static McpExecutionSupport createDefaultExecutionSupport() {
        System.out.println("[MCP GATEWAY DEBUG] Criando infraestrutura padrao MCP.");

        ModelExecutionEngine engine = McpExecutionWiring.buildEngine(
                new LegacyJsonRpcConnector(),
                new StreamingSseConnector(),
                new SaiChatExecuteConnector(),
                new LegacyMcpRequestCodec(),
                new StreamingPromptRequestCodec(),
                new RawJsonStreamingRequestCodec(),
                new SaiChatExecuteRequestCodec(),
                new LegacyMcpResponseCodec(),
                new StreamingSseResponseCodec(),
                new PlainTextResponseCodec(),
                new SaiChatExecuteResponseCodec()
        );

        McpExecutionWiring wiring = new McpExecutionWiring(
                engine,
                new PropertiesBackedModelExecutionProfileResolver(),
                new EclipseToolPromptSectionsAdapter()
        );

        return wiring.createExecutionSupport();
    }

    private String truncateForDebug(String value, int max) {
        if (value == null) {
            return "null";
        }

        if (max <= 0) {
            return "";
        }

        if (value.length() <= max) {
            return value;
        }

        return value.substring(0, max) + "... [TRUNCATED]";
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
    
    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
    public ModelExecutionResponse executePlannerPrompt(String prompt, String apiKey, com.mcp.sailibrary.plugin.mcp.core.ModelStreamingListener streamingListener) throws Exception {
        return executeChannelPrompt(
                ModelChannel.PLANNER,
                prompt,
                McpAccessCredentials.forApiKey(apiKey),
                streamingListener
        );
    }

    public ModelExecutionResponse executeChannelPrompt(ModelChannel channel, String prompt, McpAccessCredentials credentials, com.mcp.sailibrary.plugin.mcp.core.ModelStreamingListener streamingListener) throws Exception {

        if (channel == null) {
            throw new IllegalArgumentException("Erro Operacional: channel nao pode ser nulo.");
        }

        ModelExecutionProfile profile = resolveProfile(channel);
        McpAccessCredentials effectiveCredentials = resolveEffectiveCredentials(profile, credentials);
        String promptSeguro = safeTrim(prompt);

        System.out.println("[MCP GATEWAY DEBUG] ==================================================");
        System.out.println("[MCP GATEWAY DEBUG] modo=CHANNEL_PROMPT_STREAM_OBSERVABLE");
        System.out.println("[MCP GATEWAY DEBUG] channel=" + channel.name());
        System.out.println("[MCP GATEWAY DEBUG] transport=" + profile.getTransportKind());
        System.out.println("[MCP GATEWAY DEBUG] requestFormat=" + profile.getRequestFormatKind());
        System.out.println("[MCP GATEWAY DEBUG] responseFormat=" + profile.getResponseFormatKind());
        System.out.println("[MCP GATEWAY DEBUG] endpoint=" + profile.getEndpointUrl());
        System.out.println("[MCP GATEWAY DEBUG] effectiveModel=" + profile.resolveEffectiveModelName());
        System.out.println("[MCP GATEWAY DEBUG] modelDisplayName=" + formatModelForLog(profile.getLegacyModelAlias(), profile.getStreamingModelName()));
        System.out.println("[MCP GATEWAY DEBUG] promptLength=" + promptSeguro.length());

        /* * Observacao: * Este metodo so mostrara streaming real no chat quando McpExecutionSupport, * ModelExecutionEngine e StreamingSseConnector tambem receberem e propagarem * o streamingListener. Enquanto isso nao for feito, ele preserva compatibilidade * e executa pelo fluxo atual. */
        ModelExecutionResponse executionResponse = mcpExecutionSupport.executePrompt(channel, promptSeguro, effectiveCredentials);

        System.out.println("[MCP GATEWAY DEBUG] responsePrimaryTextLength="
                + (executionResponse != null && executionResponse.getPrimaryText() != null
                        ? executionResponse.getPrimaryText().length()
                        : 0));
        System.out.println("[MCP GATEWAY DEBUG] responsePrimaryText=");
        System.out.println(truncateForDebug(
                executionResponse != null ? executionResponse.getPrimaryText() : "",
                12000
        ));
        System.out.println("[MCP GATEWAY DEBUG] ==================================================");

        return executionResponse;
    }
    public ModelExecutionResponse executeContextNamingPrompt(String prompt, String apiKey) throws Exception {
        return executeChannelPrompt(
                ModelChannel.CONTEXT_NAMING,
                prompt,
                McpAccessCredentials.forApiKey(apiKey)
        );
    }

    public ModelExecutionResponse executeContextNamingPrompt(String prompt, McpAccessCredentials credentials) throws Exception {
        return executeChannelPrompt(ModelChannel.CONTEXT_NAMING, prompt, credentials);
    }
    
    private String formatModelForLog(String legacyAlias, String streamingModelName) {
        String alias = safeTrim(legacyAlias);
        String streaming = safeTrim(streamingModelName);
        String semantic = resolveSemanticModelAlias(alias, streaming);

        StringBuilder builder = new StringBuilder();
        if (!isBlank(alias)) {
            builder.append(alias);
        }
        if (!isBlank(semantic) && !semantic.equals(alias)) {
            if (builder.length() > 0) {
                builder.append(" | ");
            }
            builder.append(semantic);
        }
        if (!isBlank(streaming) && !streaming.equals(alias) && !streaming.equals(semantic)) {
            if (builder.length() > 0) {
                builder.append(" | ");
            }
            builder.append(streaming);
        }
        return builder.length() > 0 ? builder.toString() : "desconhecido";
    }

    private String resolveSemanticModelAlias(String legacyAlias, String streamingModelName) {
        String alias = safeTrim(legacyAlias).toUpperCase();
        String streaming = safeTrim(streamingModelName).toLowerCase();

        if (alias.contains("GPT54") || streaming.contains("5.4")) {
            return "gpt5ponto4";
        }
        if (alias.contains("GPT52") || streaming.contains("5.2")) {
            return "gpt5ponto2";
        }
        if (alias.contains("GPT5") || streaming.contains("gpt-5")) {
            return "gpt5";
        }
        if (alias.contains("O3") || streaming.contains("o3")) {
            return "o3";
        }
        if (alias.contains("CLAUDE") || streaming.contains("claude")) {
            return "claude";
        }
        if (!isBlank(alias)) {
            return alias.toLowerCase();
        }
        return streaming;
    }


}
