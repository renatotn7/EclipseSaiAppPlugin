package com.mcp.sailibrary.plugin.mcp.multimodel.audit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcp.sailibrary.plugin.mcp.McpResponseExtractor;
import com.mcp.sailibrary.plugin.mcp.core.ModelChannel;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionResponse;
import com.mcp.sailibrary.plugin.mcp.multimodel.gateway.UnifiedMcpModelGateway;
import com.mcp.sailibrary.plugin.mcp.multimodel.routing.ModelNameResolver;
import com.mcp.sailibrary.plugin.mcp.multimodel.routing.PropertiesBackedMcpModelNameResolver;

/* --- version: "2.0" libraries: - McpResponseExtractor - UnifiedMcpModelGateway - CodeAuditPromptBuilder - CodeAuditResult - AuditExecutionStatus - AuditObservationLevel objetivo: "Executar auditoria por canal cognitivo CODE_AUDITOR, suportando legacy ou streaming sem alterar a classe." --- */

/** * Implementacao de auditoria de codigo baseada em modelo externo. * * @author Renato Tomaz Nati * @since 2026-05-26 */
public class ClaudeCodeAuditService implements CodeAuditService {

    private ModelNameResolver modelNameResolver;
    private UnifiedMcpModelGateway unifiedMcpModelGateway;
    private McpResponseExtractor mcpResponseExtractor;
    private CodeAuditPromptBuilder codeAuditPromptBuilder;

    public ClaudeCodeAuditService() {
        this(
                new PropertiesBackedMcpModelNameResolver(),
                new UnifiedMcpModelGateway(UnifiedMcpModelGateway.DEFAULT_MCP_API_URL),
                new McpResponseExtractor(),
                new CodeAuditPromptBuilder()
        );
    }

    public ClaudeCodeAuditService(ModelNameResolver modelNameResolver, UnifiedMcpModelGateway unifiedMcpModelGateway, McpResponseExtractor mcpResponseExtractor, CodeAuditPromptBuilder codeAuditPromptBuilder) {
        this.modelNameResolver = modelNameResolver;
        this.unifiedMcpModelGateway = unifiedMcpModelGateway != null
                ? unifiedMcpModelGateway
                : new UnifiedMcpModelGateway(UnifiedMcpModelGateway.DEFAULT_MCP_API_URL);
        this.mcpResponseExtractor = mcpResponseExtractor != null ? mcpResponseExtractor : new McpResponseExtractor();
        this.codeAuditPromptBuilder = codeAuditPromptBuilder != null ? codeAuditPromptBuilder : new CodeAuditPromptBuilder();
    }

    @Override
    public CodeAuditResult auditarCodigo(String pedidoOriginal, String planoImplementacao, String codigoCandidato, String actionEsperada, String apiKey) throws Exception {

        String promptAuditoria = codeAuditPromptBuilder.build(
                pedidoOriginal,
                planoImplementacao,
                codigoCandidato,
                actionEsperada
        );

        ModelExecutionProfile profile = unifiedMcpModelGateway.resolveProfile(ModelChannel.CODE_AUDITOR);

        System.out.println("[MCP DEBUG] ClaudeCodeAuditService");
        System.out.println("[MCP DEBUG] transport=" + profile.getTransportKind());
        System.out.println("[MCP DEBUG] requestFormat=" + profile.getRequestFormatKind());
        System.out.println("[MCP DEBUG] responseFormat=" + profile.getResponseFormatKind());
        System.out.println("[MCP DEBUG] model=" + profile.resolveEffectiveModelName());
        System.out.println("[MCP DEBUG] modelDisplayName=" + formatModelForLog(profile.getLegacyModelAlias(), profile.getStreamingModelName()));
        System.out.println("[MCP DEBUG] actionEsperada=" + actionEsperada);
        System.out.println("[MCP DEBUG] promptLength=" + promptAuditoria.length());
        System.out.println("[MCP DEBUG] pedidoOriginal=" + truncateForDebug(pedidoOriginal, 1200));
        System.out.println("[MCP DEBUG] planoImplementacao=" + truncateForDebug(planoImplementacao, 2500));
        System.out.println("[MCP DEBUG] codigoCandidato=" + truncateForDebug(codigoCandidato, 2500));

        ModelExecutionResponse executionResponse = unifiedMcpModelGateway.executeCodeAuditorPrompt(promptAuditoria, apiKey);

        if (executionResponse == null) {
            CodeAuditResult result = new CodeAuditResult();
            result.setAprovado(false);
            result.setDeveTentarNovamente(false);
            result.setNivelRisco("INDEFINIDO");
            result.setObservationLevel(AuditObservationLevel.NENHUM);
            result.setExecutionStatus(AuditExecutionStatus.FALHA_INFRA);
            result.setFeedback("Falha tecnica ao executar o canal CODE_AUDITOR. Nenhuma resposta foi retornada pelo gateway.");
            return result;
        }

        String rawResponse = executionResponse.getRawResponseBody();
        String textResponse = executionResponse.getPrimaryText();

        System.out.println("[MCP DEBUG] rawResponse=" + truncateForDebug(rawResponse, 3000));

        if (textResponse == null || textResponse.trim().length() == 0) {
            textResponse = mcpResponseExtractor.extractPrimaryText(rawResponse);
        }

        System.out.println("[MCP DEBUG] textResponse=" + truncateForDebug(textResponse, 3000));

        CodeAuditResult result = parseAuditResult(textResponse);

        if (result != null) {
            System.out.println("[MCP DEBUG] auditResult.executionStatus=" + result.getExecutionStatus());
            System.out.println("[MCP DEBUG] auditResult.aprovado=" + result.isAprovado());
            System.out.println("[MCP DEBUG] auditResult.deveTentarNovamente=" + result.isDeveTentarNovamente());
            System.out.println("[MCP DEBUG] auditResult.nivelRisco=" + result.getNivelRisco());
            System.out.println("[MCP DEBUG] auditResult.observationLevel=" + result.getObservationLevel());
            System.out.println("[MCP DEBUG] auditResult.feedback=" + truncateForDebug(result.getFeedback(), 2000));
        } else {
            System.out.println("[MCP DEBUG] auditResult=null");
        }

        return result;
    }

    private CodeAuditResult parseAuditResult(String textResponse) {
        CodeAuditResult result = new CodeAuditResult();
        String textoNormalizado = normalizeAuditPayload(textResponse);

        if (isAuditInfrastructureFailure(textoNormalizado)) {
            result.setAprovado(false);
            result.setDeveTentarNovamente(false);
            result.setNivelRisco("INDEFINIDO");
            result.setObservationLevel(AuditObservationLevel.NENHUM);
            result.setExecutionStatus(AuditExecutionStatus.FALHA_INFRA);
            result.setFeedback(
                    "Falha tecnica da auditoria. O problema ocorreu na infraestrutura do auditor e nao prova risco real do codigo. "
                            + "Resposta recebida: " + safeTrim(textoNormalizado)
            );
            return result;
        }

        try {
            JsonObject auditJson = extractAuditDecisionJson(textoNormalizado);

            if (auditJson == null) {
                result.setAprovado(false);
                result.setDeveTentarNovamente(false);
                result.setNivelRisco("INDEFINIDO");
                result.setObservationLevel(AuditObservationLevel.NENHUM);
                result.setExecutionStatus(AuditExecutionStatus.FALHA_INFRA);
                result.setFeedback(
                        "Falha tecnica ao interpretar a resposta do auditor. O retorno nao trouxe bloco de decisao reconhecivel. "
                                + "Resposta recebida: " + safeTrim(textoNormalizado)
                );
                return result;
            }

            if (!hasExplicitApproved(auditJson)) {
                result.setAprovado(false);
                result.setDeveTentarNovamente(false);
                result.setNivelRisco(extractRiskLevelOrDefault(auditJson));
                result.setObservationLevel(detectObservationLevel(auditJson));
                result.setExecutionStatus(AuditExecutionStatus.FALHA_INFRA);
                result.setFeedback(
                        "Falha tecnica ao interpretar a resposta do auditor. O retorno nao trouxe o campo obrigatorio 'approved'. "
                                + "Resposta recebida: " + safeTrim(auditJson.toString())
                );
                return result;
            }

            boolean approved = readBooleanLenient(auditJson.get("approved"), false);
            boolean shouldRetry = readBooleanLenient(auditJson.get("shouldRetry"), false);
            String riskLevel = extractRiskLevelOrDefault(auditJson);
            String feedback = extractFeedbackOrDefault(auditJson);
            AuditObservationLevel observationLevel = detectObservationLevel(auditJson);

            result.setAprovado(approved);
            result.setDeveTentarNovamente(shouldRetry);
            result.setNivelRisco(riskLevel);
            result.setFeedback(feedback);
            result.setObservationLevel(observationLevel);
            result.setExecutionStatus(approved ? AuditExecutionStatus.APROVADO : AuditExecutionStatus.REPROVADO);

            return result;
        } catch (Exception e) {
            result.setAprovado(false);
            result.setDeveTentarNovamente(false);
            result.setNivelRisco("INDEFINIDO");
            result.setObservationLevel(AuditObservationLevel.NENHUM);
            result.setExecutionStatus(AuditExecutionStatus.FALHA_INFRA);
            result.setFeedback(
                    "Falha tecnica ao interpretar a resposta do auditor. O retorno nao seguiu o contrato esperado. "
                            + "Resposta recebida: " + safeTrim(textoNormalizado)
            );
            return result;
        }
    }

    private JsonObject extractAuditDecisionJson(String textoNormalizado) {
        if (isBlank(textoNormalizado)) {
            return null;
        }

        JsonObject directObject = tryParseAuditJsonObject(textoNormalizado);
        if (directObject != null) {
            return directObject;
        }

        String extractedJsonBlock = extractFirstBalancedJsonObject(textoNormalizado);
        if (!isBlank(extractedJsonBlock)) {
            JsonObject extractedObject = tryParseAuditJsonObject(extractedJsonBlock);
            if (extractedObject != null) {
                return extractedObject;
            }
        }

        return null;
    }

    private JsonObject tryParseAuditJsonObject(String rawText) {
        String texto = normalizeAuditPayload(rawText);

        if (isBlank(texto) || isAuditInfrastructureFailure(texto)) {
            return null;
        }

        try {
            JsonElement rootElement = JsonParser.parseString(texto);

            if (rootElement == null || !rootElement.isJsonObject()) {
                return null;
            }

            JsonObject rootObject = rootElement.getAsJsonObject();

            if (looksLikeAuditDecision(rootObject)) {
                return rootObject;
            }

            JsonObject nestedFromContent = extractDecisionFromNamedField(rootObject, "content");
            if (nestedFromContent != null) {
                return nestedFromContent;
            }

            JsonObject nestedFromExplanation = extractDecisionFromNamedField(rootObject, "explanation");
            if (nestedFromExplanation != null) {
                return nestedFromExplanation;
            }

        } catch (Exception e) {
            return null;
        }

        return null;
    }

    private JsonObject extractDecisionFromNamedField(JsonObject rootObject, String fieldName) {
        if (rootObject == null || isBlank(fieldName) || !rootObject.has(fieldName)) {
            return null;
        }

        JsonElement fieldElement = rootObject.get(fieldName);

        if (fieldElement == null || fieldElement.isJsonNull()) {
            return null;
        }

        if (fieldElement.isJsonObject()) {
            JsonObject fieldObject = fieldElement.getAsJsonObject();
            if (looksLikeAuditDecision(fieldObject)) {
                return fieldObject;
            }
        }

        if (fieldElement.isJsonPrimitive() && fieldElement.getAsJsonPrimitive().isString()) {
            String fieldText = fieldElement.getAsString();

            JsonObject nestedParsed = tryParseAuditJsonObject(fieldText);
            if (nestedParsed != null) {
                return nestedParsed;
            }

            String nestedJsonBlock = extractFirstBalancedJsonObject(fieldText);
            if (!isBlank(nestedJsonBlock)) {
                JsonObject nestedParsedFromBlock = tryParseAuditJsonObject(nestedJsonBlock);
                if (nestedParsedFromBlock != null) {
                    return nestedParsedFromBlock;
                }
            }
        }

        return null;
    }

    private boolean looksLikeAuditDecision(JsonObject object) {
        if (object == null) {
            return false;
        }

        boolean hasApproved = object.has("approved") && !object.get("approved").isJsonNull();
        boolean hasShouldRetry = object.has("shouldRetry") && !object.get("shouldRetry").isJsonNull();
        boolean hasRiskLevel = object.has("riskLevel") && !object.get("riskLevel").isJsonNull();
        boolean hasFeedback = object.has("feedback") && !object.get("feedback").isJsonNull();

        return hasApproved
                || (hasShouldRetry && hasRiskLevel)
                || (hasRiskLevel && hasFeedback);
    }

    private boolean hasExplicitApproved(JsonObject object) {
        if (object == null || !object.has("approved") || object.get("approved").isJsonNull()) {
            return false;
        }

        JsonElement approvedElement = object.get("approved");
        if (!approvedElement.isJsonPrimitive()) {
            return false;
        }

        try {
            if (approvedElement.getAsJsonPrimitive().isBoolean()) {
                return true;
            }

            String text = safeTrim(approvedElement.getAsString());
            return "true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean readBooleanLenient(JsonElement element, boolean defaultValue) {
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return defaultValue;
        }

        try {
            if (element.getAsJsonPrimitive().isBoolean()) {
                return element.getAsBoolean();
            }

            String text = safeTrim(element.getAsString());
            if ("true".equalsIgnoreCase(text)) {
                return true;
            }
            if ("false".equalsIgnoreCase(text)) {
                return false;
            }
        } catch (Exception e) {
        }

        return defaultValue;
    }

    private String extractRiskLevelOrDefault(JsonObject auditJson) {
        if (auditJson != null && auditJson.has("riskLevel") && !auditJson.get("riskLevel").isJsonNull()) {
            try {
                return auditJson.get("riskLevel").getAsString();
            } catch (Exception e) {
                return "INDEFINIDO";
            }
        }
        return "INDEFINIDO";
    }

    private String extractFeedbackOrDefault(JsonObject auditJson) {
        if (auditJson != null && auditJson.has("feedback") && !auditJson.get("feedback").isJsonNull()) {
            try {
                return auditJson.get("feedback").getAsString();
            } catch (Exception e) {
                return "";
            }
        }
        return "";
    }

    private AuditObservationLevel detectObservationLevel(JsonObject auditJson) {
        String riskLevel = safeLower(extractRiskLevelOrDefault(auditJson));
        String feedback = safeLower(extractFeedbackOrDefault(auditJson));

        if (containsAny(
                riskLevel,
                new String[] { "medio", "médio", "alto", "critico", "crítico", "alerta", "atenção", "atencao" })) {
            return AuditObservationLevel.PONTO_ATENCAO;
        }

        if (containsAny(
                feedback,
                new String[] {
                        "ponto de atencao",
                        "ponto de atenção",
                        "atenção",
                        "atencao",
                        "ressalva",
                        "cuidado",
                        "warning",
                        "warn"
                })) {
            return AuditObservationLevel.PONTO_ATENCAO;
        }

        if (containsAny(
                feedback,
                new String[] {
                        "recomendacao",
                        "recomendação",
                        "melhoria",
                        "sugestao",
                        "sugestão",
                        "opcional",
                        "recommended",
                        "recommendation"
                })) {
            return AuditObservationLevel.RECOMENDACAO;
        }

        return AuditObservationLevel.NENHUM;
    }

    private boolean containsAny(String text, String[] candidates) {
        if (isBlank(text) || candidates == null || candidates.length == 0) {
            return false;
        }

        for (int i = 0; i < candidates.length; i++) {
            String candidate = candidates[i];
            if (!isBlank(candidate) && text.contains(candidate.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    private String extractFirstBalancedJsonObject(String text) {
        if (isBlank(text)) {
            return null;
        }

        int start = -1;
        int depth = 0;
        boolean insideString = false;
        boolean escaping = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (escaping) {
                escaping = false;
                continue;
            }

            if (c == '\\' && insideString) {
                escaping = true;
                continue;
            }

            if (c == '"') {
                insideString = !insideString;
                continue;
            }

            if (insideString) {
                continue;
            }

            if (c == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    return text.substring(start, i + 1);
                }
            }
        }

        return null;
    }

    private String normalizeAuditPayload(String textResponse) {
        String texto = safeTrim(textResponse);

        if (texto.startsWith("```json")) {
            texto = texto.substring(7).trim();
        } else if (texto.startsWith("```")) {
            texto = texto.substring(3).trim();
        }

        if (texto.endsWith("```")) {
            texto = texto.substring(0, texto.length() - 3).trim();
        }

        return texto;
    }

    private boolean isAuditInfrastructureFailure(String textResponse) {
        String texto = safeLower(textResponse);

        if (texto.length() == 0) {
            return true;
        }

        return texto.contains("max_tokens")
                || texto.contains("timeout")
                || texto.contains("timed out")
                || texto.contains("unavailable")
                || texto.contains("access denied")
                || texto.contains("tool error")
                || texto.contains("error executing tool")
                || texto.contains("quota")
                || texto.contains("rate limit")
                || texto.contains("openaicompatible")
                || texto.contains("context length")
                || texto.contains("maximum allowed number of output tokens")
                || texto.contains("template usage limit exceeded");
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

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeLower(String value) {
        return safeTrim(value).toLowerCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
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
