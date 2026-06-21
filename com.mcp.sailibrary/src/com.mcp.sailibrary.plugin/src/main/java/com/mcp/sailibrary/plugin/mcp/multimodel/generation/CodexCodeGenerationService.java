package com.mcp.sailibrary.plugin.mcp.multimodel.generation;

import com.mcp.sailibrary.plugin.chat.service.McpAgentResponseService;
import com.mcp.sailibrary.plugin.chat.support.AiResponse;
import com.mcp.sailibrary.plugin.mcp.McpResponseExtractor;
import com.mcp.sailibrary.plugin.mcp.core.ModelChannel;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionResponse;
import com.mcp.sailibrary.plugin.mcp.multimodel.gateway.UnifiedMcpModelGateway;
import com.mcp.sailibrary.plugin.mcp.multimodel.routing.ModelNameResolver;
import com.mcp.sailibrary.plugin.mcp.multimodel.routing.PropertiesBackedMcpModelNameResolver;

/* --- version: "2.0" libraries: - McpAgentResponseService - AiResponse - McpResponseExtractor - UnifiedMcpModelGateway - CodeGenerationPromptBuilder objetivo: "Executar geracao de codigo por canal cognitivo CODE_GENERATOR, suportando legacy ou streaming sem alterar a classe." --- */

/** * Implementacao concreta do servico de geracao de codigo. * * @author Renato Tomaz Nati * @since 2026-05-26 */
public class CodexCodeGenerationService implements CodeGenerationService {

    private ModelNameResolver modelNameResolver;
    private UnifiedMcpModelGateway unifiedMcpModelGateway;
    private McpResponseExtractor mcpResponseExtractor;
    private McpAgentResponseService mcpAgentResponseService;
    private CodeGenerationPromptBuilder codeGenerationPromptBuilder;

    public CodexCodeGenerationService() {
        this(
                new PropertiesBackedMcpModelNameResolver(),
                new UnifiedMcpModelGateway(UnifiedMcpModelGateway.DEFAULT_MCP_API_URL),
                new McpResponseExtractor(),
                new McpAgentResponseService(),
                new CodeGenerationPromptBuilder()
        );
    }

    public CodexCodeGenerationService(ModelNameResolver modelNameResolver, UnifiedMcpModelGateway unifiedMcpModelGateway, McpResponseExtractor mcpResponseExtractor, McpAgentResponseService mcpAgentResponseService, CodeGenerationPromptBuilder codeGenerationPromptBuilder) {
        this.modelNameResolver = modelNameResolver;
        this.unifiedMcpModelGateway = unifiedMcpModelGateway != null
                ? unifiedMcpModelGateway
                : new UnifiedMcpModelGateway(UnifiedMcpModelGateway.DEFAULT_MCP_API_URL);
        this.mcpResponseExtractor = mcpResponseExtractor != null ? mcpResponseExtractor : new McpResponseExtractor();
        this.mcpAgentResponseService = mcpAgentResponseService != null ? mcpAgentResponseService : new McpAgentResponseService();
        this.codeGenerationPromptBuilder = codeGenerationPromptBuilder != null ? codeGenerationPromptBuilder : new CodeGenerationPromptBuilder();
    }

    @Override
    public AiResponse gerarCodigo(String pedidoOriginal, String planoImplementacao, String selectedCode, String fullFileText, String actionEsperada, String apiKey) throws Exception {

        String promptGeracaoCodigo = codeGenerationPromptBuilder.build(
                pedidoOriginal,
                planoImplementacao,
                selectedCode,
                fullFileText,
                actionEsperada
        );

        ModelExecutionProfile profile = unifiedMcpModelGateway.resolveProfile(ModelChannel.CODE_GENERATOR);

        System.out.println("[MCP DEBUG] CodexCodeGenerationService");
        System.out.println("[MCP DEBUG] transport=" + profile.getTransportKind());
        System.out.println("[MCP DEBUG] requestFormat=" + profile.getRequestFormatKind());
        System.out.println("[MCP DEBUG] responseFormat=" + profile.getResponseFormatKind());
        System.out.println("[MCP DEBUG] model=" + profile.resolveEffectiveModelName());
        System.out.println("[MCP DEBUG] modelAlias=" + profile.getLegacyModelAlias());
        System.out.println("[MCP DEBUG] modelDisplayName=" + formatModelForLog(profile.getLegacyModelAlias(), profile.getStreamingModelName()));
        System.out.println("[MCP DEBUG] actionEsperada=" + actionEsperada);
        System.out.println("[MCP DEBUG] promptLength=" + promptGeracaoCodigo.length());
        System.out.println("[MCP DEBUG] pedidoOriginal=" + truncateForDebug(pedidoOriginal, 1000));
        System.out.println("[MCP DEBUG] planoImplementacao=" + truncateForDebug(planoImplementacao, 2000));

        ModelExecutionResponse executionResponse = unifiedMcpModelGateway.executeCodeGeneratorPrompt(promptGeracaoCodigo, apiKey);

        if (executionResponse == null) {
            System.out.println("[MCP DEBUG] executionResponse=null");
            return null;
        }

        String rawResponse = executionResponse.getRawResponseBody();
        String textResponse = executionResponse.getPrimaryText();

        System.out.println("[MCP DEBUG] rawResponse=" + truncateForDebug(rawResponse, 3000));

        if (textResponse == null || textResponse.trim().length() == 0) {
            textResponse = mcpResponseExtractor.extractPrimaryText(rawResponse);
        }

        System.out.println("[MCP DEBUG] textResponse=" + truncateForDebug(textResponse, 3000));

        AiResponse respostaGerada = mcpAgentResponseService.interpretarRespostaIA(textResponse);
        respostaGerada = mcpAgentResponseService.normalizarProtocoloFerramentaLegado(respostaGerada);

        if (respostaGerada != null) {
            System.out.println("[MCP DEBUG] generatedAiResponse.action=" + respostaGerada.getAction());
            System.out.println("[MCP DEBUG] generatedAiResponse.tool=" + respostaGerada.getTool());
            System.out.println("[MCP DEBUG] generatedAiResponse.explanation=" + truncateForDebug(respostaGerada.getExplanation(), 1000));
            System.out.println("[MCP DEBUG] generatedAiResponse.content=" + truncateForDebug(respostaGerada.getContent(), 2000));
        } else {
            System.out.println("[MCP DEBUG] generatedAiResponse=null");
        }

        return respostaGerada;
    }

    private String truncateForDebug(String value, int max) {
        if (value == null) {
            return "null";
        }

        if (value.length() <= max) {
            return value;
        }

        return value.substring(0, max) + "... [TRUNCATED]";
    }

    private static String formatModelForLog(String legacyAlias, String streamingModelName) {
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

    private static String resolveSemanticModelAlias(String legacyAlias, String streamingModelName) {
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

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}