package com.mcp.sailibrary.plugin.mcp;

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
import com.mcp.sailibrary.plugin.mcp.core.StructuralContextDetector;
import com.mcp.sailibrary.plugin.mcp.core.ToolPromptSections;
import com.mcp.sailibrary.plugin.mcp.multimodel.gateway.UnifiedMcpModelGateway;

/** * Fachada publica da integracao MCP. * * <p>Esta classe agora: * - preserva compatibilidade com os metodos antigos * - suporta configuracao por canal cognitivo * - suporta legado e streaming * - suporta prompt simples e raw json * - funciona mesmo sem passar pelo circuito multimodelo/monomodelo</p> * * @author Renato Tomaz Nati * @since 2026-05-26 */
public class SaiLibraryMcpClient {

    private static final DesenvolvimentoPromptBuilder DESENVOLVIMENTO_PROMPT_BUILDER = new DesenvolvimentoPromptBuilder();
    private static final BlockNamePromptBuilder BLOCK_NAME_PROMPT_BUILDER = new BlockNamePromptBuilder();
    private static final McpResponseExtractor RESPONSE_EXTRACTOR = new McpResponseExtractor();
    private static final StructuralContextDetector STRUCTURAL_CONTEXT_DETECTOR = new StructuralContextDetector();
    private static final EclipseToolPromptSectionsAdapter TOOL_PROMPT_SECTIONS_ADAPTER = new EclipseToolPromptSectionsAdapter();

    private static final McpExecutionSupport EXECUTION_SUPPORT = createExecutionSupport();

    private SaiLibraryMcpClient() {
    }

    /** * Metodo de compatibilidade com o nome antigo. * * <p>Apesar do nome "Gpt5", a execucao agora e resolvida por configuracao * do canal PLANNER.</p> */
    public static String callDesenvolvimentoGpt5(String selectedCode, String fullFileText, String instrucao, String apiKey) throws Exception {
        return callDesenvolvimento(selectedCode, fullFileText, instrucao, apiKey, null);
    }

    /** * Executa o fluxo de desenvolvimento usando o canal PLANNER configurado. * * @param selectedCode trecho selecionado * @param fullFileText arquivo completo * @param instrucao instrucao enriquecida * @param apiKey apiKey opcional * @param cookieValue cookie opcional * @return resposta bruta do connector * @throws Exception em caso de falha */
    public static String callDesenvolvimento(String selectedCode, String fullFileText, String instrucao, String apiKey, String cookieValue) throws Exception {

        String textoSelecionado = selectedCode != null ? selectedCode : "";
        String textoArquivoCompleto = fullFileText != null ? fullFileText : "";
        String textoInstrucao = instrucao != null ? instrucao : "";

        boolean possuiTrechoTextual = textoSelecionado.trim().length() > 0;
        boolean possuiArquivoTextual = textoArquivoCompleto.trim().length() > 0;
        boolean possuiContextoEstrutural = STRUCTURAL_CONTEXT_DETECTOR.hasStructuralContext(textoInstrucao);

        if (!possuiTrechoTextual && !possuiArquivoTextual && !possuiContextoEstrutural) {
            throw new IllegalStateException(
                    "Nenhum insumo textual ou estrutural principal foi encontrado para a chamada MCP. "
                            + "Selecione um trecho/bloco principal no editor ou mantenha um contexto estrutural principal/utilizavel na sessao."
            );
        }

        String textoSelecionadoParaPrompt = possuiTrechoTextual ? textoSelecionado : "[SEM_TRECHO_SELECIONADO]";
        String textoArquivoCompletoParaPrompt = possuiArquivoTextual ? textoArquivoCompleto : "[SEM_ARQUIVO_TEXTUAL_ATIVO]";
        String modoOperacionalDetectado = (possuiTrechoTextual || possuiArquivoTextual)
                ? "MODO_TEXTUAL"
                : "MODO_ESTRUTURAL";

        ToolPromptSections toolPromptSections = TOOL_PROMPT_SECTIONS_ADAPTER.load();

        String promptEngenharia = DESENVOLVIMENTO_PROMPT_BUILDER.build(
                modoOperacionalDetectado,
                textoSelecionadoParaPrompt,
                textoArquivoCompletoParaPrompt,
                textoInstrucao,
                toolPromptSections.getToolsSection(),
                toolPromptSections.getExamplesSection()
        );

        ModelExecutionResponse response = EXECUTION_SUPPORT.executePrompt(
                ModelChannel.PLANNER,
                promptEngenharia,
                createCredentials(apiKey, cookieValue)
        );

        return response != null ? response.getRawResponseBody() : "";
    }

  

    /** * Executa a sugestao de nome curto usando o canal SUMMARIZER configurado. */
    public static String callSugestaoNomeBloco(String selectedText, String kind, String existingNamesText, String apiKey) throws Exception {
        String prompt = buildSugestaoNomeBlocoPrompt(selectedText, kind, existingNamesText);

        com.mcp.sailibrary.plugin.mcp.multimodel.gateway.UnifiedMcpModelGateway gateway =
                new com.mcp.sailibrary.plugin.mcp.multimodel.gateway.UnifiedMcpModelGateway();

        com.mcp.sailibrary.plugin.mcp.core.ModelExecutionResponse response =
                gateway.executeContextNamingPrompt(prompt, apiKey);

        if (response == null) {
            return "";
        }

        String primaryText = response.getPrimaryText();

        if (primaryText != null && primaryText.trim().length() > 0) {
            return primaryText;
        }

        return response.getRawResponseBody();
    }
    private static String buildSugestaoNomeBlocoPrompt(String selectedText, String kind, String existingNamesText) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("TAREFA: gerar um alias curto para um contexto de codigo.\n");
        prompt.append("\n");
        prompt.append("SAIDA OBRIGATORIA:\n");
        prompt.append("Responda somente um JSON valido neste formato exato:\n");
        prompt.append("{\"name\":\"alias\"}\n");
        prompt.append("\n");
        prompt.append("REGRAS DO CAMPO name:\n");
        prompt.append("1. Use apenas letras minusculas de a a z e numeros.\n");
        prompt.append("2. Nao use acentos.\n");
        prompt.append("3. Nao use espacos.\n");
        prompt.append("4. Nao use underline, hifen, ponto, aspas ou simbolos.\n");
        prompt.append("5. Tamanho minimo: 2 caracteres.\n");
        prompt.append("6. Tamanho maximo: 12 caracteres.\n");
        prompt.append("7. O nome deve representar a finalidade funcional do trecho.\n");
        prompt.append("8. Nao use palavras de erro ou infraestrutura como access, denied, error, erro, falha, null, undefined.\n");
        prompt.append("9. Nao use palavras reservadas Java como public, private, protected, static, final, void, class, return, if, for, while, try, catch.\n");
        prompt.append("10. Se o trecho for um metodo que inicia tela, busca ou pesquisa, prefira pesquisa, inicio, filtro ou consulta.\n");
        prompt.append("11. Se houver chamada para iniciarPesquisa, prefira pesquisa.\n");
        prompt.append("12. Se houver validacao, prefira validacao.\n");
        prompt.append("13. Se houver query, SQL, HQL, Criteria, DAO ou repository, prefira query, sql, hql, criteria, dao ou repository.\n");
        prompt.append("14. Se houver retorno de dados, prefira retorno.\n");
        prompt.append("15. Nunca copie annotation, nome de permissao, mensagem de erro ou texto externo como nome.\n");
        prompt.append("\n");
        prompt.append("NOMES JA EXISTENTES:\n");
        prompt.append(existingNamesText != null ? existingNamesText : "");
        prompt.append("\n\n");
        prompt.append("TIPO DO CONTEXTO:\n");
        prompt.append(kind != null ? kind : "");
        prompt.append("\n\n");
        prompt.append("TRECHO:\n");
        prompt.append(selectedText != null ? selectedText : "");
        prompt.append("\n\n");
        prompt.append("LEMBRETE FINAL: responda somente JSON valido, sem markdown, sem explicacao, sem texto antes ou depois.\n");

        return prompt.toString();
    }
    /** * Executa um prompt simples em qualquer canal configurado. * * <p>Este metodo e util para quem quer usar a configuracao sem depender * das classes multimodelo/monomodelo.</p> */
    public static String callCanalPorPrompt(ModelChannel channel, String prompt, String apiKey, String cookieValue) throws Exception {

        ModelExecutionResponse response = EXECUTION_SUPPORT.executePrompt(
                channel,
                prompt,
                createCredentials(apiKey, cookieValue)
        );

        return response != null ? response.getRawResponseBody() : "";
    }

    /** * Executa um raw json em qualquer canal configurado. * * <p>Este metodo e util quando o request format estiver configurado como * STREAMING_RAW_JSON.</p> */
    public static String callCanalRawJson(ModelChannel channel, String rawJsonBody, String apiKey, String cookieValue) throws Exception {

        ModelExecutionResponse response = EXECUTION_SUPPORT.executeRawJson(
                channel,
                rawJsonBody,
                createCredentials(apiKey, cookieValue)
        );

        return response != null ? response.getRawResponseBody() : "";
    }

    /** * Resolve o profile efetivo de um canal. * * <p>Util para debug, tela de configuracao ou suporte operacional.</p> */
    public static ModelExecutionProfile resolveExecutionProfile(ModelChannel channel) {
        return EXECUTION_SUPPORT.resolveProfile(channel);
    }

    public static String extractSuggestedBlockName(String rawResponse) {
        if (rawResponse == null || rawResponse.trim().length() == 0) {
            return "";
        }

        String value = rawResponse.trim();

        String explicitName = extractExplicitSuggestedBlockName(value);
        if (explicitName != null && explicitName.trim().length() > 0) {
            return explicitName;
        }

        String primaryText = extractPrimaryText(value);
        if (primaryText != null && primaryText.trim().length() > 0) {
            explicitName = extractExplicitSuggestedBlockName(primaryText);
            if (explicitName != null && explicitName.trim().length() > 0) {
                return explicitName;
            }

            value = primaryText.trim();
        }

        if (isInfrastructureOrAccessFailureText(value)) {
            return "";
        }

        value = value.replaceAll("(?i)```json", "");
        value = value.replaceAll("(?i)```", "");
        value = value.trim();

        if (value.indexOf('\n') >= 0) {
            String[] lines = value.split("\\r?\\n");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i] != null ? lines[i].trim() : "";
                if (line.length() > 0 && !isInfrastructureOrAccessFailureText(line)) {
                    value = line;
                    break;
                }
            }
        }

        return value;
    }
    
    private static String extractExplicitSuggestedBlockName(String value) {
        if (value == null || value.trim().length() == 0) {
            return "";
        }

        String trimmed = value.trim();

        try {
            com.google.gson.JsonElement element = com.google.gson.JsonParser.parseString(trimmed);
            if (!element.isJsonObject()) {
                return "";
            }

            com.google.gson.JsonObject object = element.getAsJsonObject();

            if (object.has("name") && !object.get("name").isJsonNull()) {
                return object.get("name").getAsString();
            }

            if (object.has("alias") && !object.get("alias").isJsonNull()) {
                return object.get("alias").getAsString();
            }

            if (object.has("suggestedName") && !object.get("suggestedName").isJsonNull()) {
                return object.get("suggestedName").getAsString();
            }

            if (object.has("nome") && !object.get("nome").isJsonNull()) {
                return object.get("nome").getAsString();
            }
        } catch (Exception e) {
            return "";
        }

        return "";
    }
    private static boolean isInfrastructureOrAccessFailureText(String value) {
        if (value == null) {
            return false;
        }

        String normalized = value.trim().toLowerCase();

        if (normalized.length() == 0) {
            return false;
        }

        return normalized.contains("access denied")
                || normalized.contains("tool is not in your favourites")
                || normalized.contains("not its owner")
                || normalized.contains("unauthorized")
                || normalized.contains("forbidden")
                || normalized.contains("http 401")
                || normalized.contains("http 403")
                || normalized.contains("http 302")
                || normalized.contains("falha")
                || normalized.contains("erro de infraestrutura")
                || normalized.contains("infrastructure failure");
    }
    private static String extractJsonLikeStringValue(String text, int fieldIndex) {
        if (text == null || fieldIndex < 0 || fieldIndex >= text.length()) {
            return "";
        }

        int colonIndex = text.indexOf(':', fieldIndex);
        if (colonIndex < 0) {
            return "";
        }

        int firstQuote = text.indexOf('"', colonIndex + 1);
        if (firstQuote < 0) {
            return "";
        }

        int secondQuote = text.indexOf('"', firstQuote + 1);
        if (secondQuote < 0) {
            return "";
        }

        return text.substring(firstQuote + 1, secondQuote);
    }
    private static String sanitizeContextName(String value) {
        if (value == null) {
            return "";
        }

        String sanitized = value.trim().toLowerCase();

        sanitized = java.text.Normalizer.normalize(sanitized, java.text.Normalizer.Form.NFD);
        sanitized = sanitized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        sanitized = sanitized.replaceAll("[^a-z0-9]", "");

        if (sanitized.length() > 12) {
            sanitized = sanitized.substring(0, 12);
        }

        return sanitized;
    }
    private static boolean isForbiddenContextName(String value) {
        if (value == null || value.trim().length() == 0) {
            return true;
        }

        String name = value.trim().toLowerCase();

        if (name.length() < 3) {
            return true;
        }

        if ("accessdenied".equals(name)
                || "denied".equals(name)
                || "forbidden".equals(name)
                || "unauthorized".equals(name)
                || "unauthoriz".equals(name)
                || "error".equals(name)
                || "erro".equals(name)
                || "exception".equals(name)
                || "falha".equals(name)
                || "invalid".equals(name)
                || "invalido".equals(name)
                || "null".equals(name)
                || "true".equals(name)
                || "false".equals(name)
                || "public".equals(name)
                || "private".equals(name)
                || "protected".equals(name)
                || "static".equals(name)
                || "final".equals(name)
                || "void".equals(name)
                || "return".equals(name)
                || "this".equals(name)
                || "super".equals(name)
                || "get".equals(name)
                || "set".equals(name)
                || "list".equals(name)
                || "find".equals(name)
                || "load".equals(name)
                || "build".equals(name)
                || "create".equals(name)
                || "metodo".equals(name)
                || "classe".equals(name)
                || "codigo".equals(name)
                || "bloco".equals(name)
                || "trecho".equals(name)
                || "contexto".equals(name)) {
            return true;
        }

        return false;
    }
    private static boolean isInfrastructureFailureNameResponse(String value) {
        if (value == null) {
            return false;
        }

        String normalized = value.trim().toLowerCase();

        if (normalized.length() == 0) {
            return false;
        }

        return normalized.contains("access denied")
                || normalized.contains("not in your favourites")
                || normalized.contains("not its owner")
                || normalized.contains("unauthorized")
                || normalized.contains("forbidden")
                || normalized.contains("401")
                || normalized.contains("403")
                || normalized.contains("erro")
                || normalized.contains("error")
                || normalized.contains("exception")
                || normalized.contains("falha")
                || "accessdenied".equals(normalized)
                || "denied".equals(normalized)
                || "unauthorized".equals(normalized)
                || "forbidden".equals(normalized);
    }
    private static String sanitizeBlockNameCandidate(String value) {
        if (value == null) {
            return "";
        }

        String sanitized = value.trim().toLowerCase();

        int lineBreakIndex = sanitized.indexOf('\n');
        if (lineBreakIndex >= 0) {
            sanitized = sanitized.substring(0, lineBreakIndex);
        }

        sanitized = java.text.Normalizer.normalize(sanitized, java.text.Normalizer.Form.NFD);
        sanitized = sanitized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        sanitized = sanitized.replaceAll("[^a-z0-9]", "");

        if (sanitized.length() > 12) {
            sanitized = sanitized.substring(0, 12);
        }

        return sanitized;
    }
    private static boolean isUsableSuggestedName(String value) {
        if (value == null) {
            return false;
        }

        String normalized = value.trim().toLowerCase();

        if (normalized.length() < 2) {
            return false;
        }

        if (normalized.length() > 12) {
            return false;
        }

        if (!normalized.matches("^[a-z0-9]+$")) {
            return false;
        }

        if ("public".equals(normalized)
                || "private".equals(normalized)
                || "protected".equals(normalized)
                || "static".equals(normalized)
                || "final".equals(normalized)
                || "void".equals(normalized)
                || "if".equals(normalized)
                || "for".equals(normalized)
                || "while".equals(normalized)
                || "try".equals(normalized)
                || "catch".equals(normalized)
                || "return".equals(normalized)
                || "this".equals(normalized)
                || "super".equals(normalized)
                || "null".equals(normalized)
                || "true".equals(normalized)
                || "false".equals(normalized)
                || "get".equals(normalized)
                || "set".equals(normalized)
                || "list".equals(normalized)
                || "find".equals(normalized)
                || "load".equals(normalized)
                || "build".equals(normalized)
                || "create".equals(normalized)
                || "accessdenied".equals(normalized)
                || "denied".equals(normalized)
                || "unauthorized".equals(normalized)
                || "forbidden".equals(normalized)
                || "error".equals(normalized)
                || "erro".equals(normalized)
                || "exception".equals(normalized)) {
            return false;
        }

        return true;
    }
    
    /** * Mantido por compatibilidade. */
    public static String safeString(String entrada) {
    	McpPayloadBuilder payloadBuilder = new McpPayloadBuilder();
        return payloadBuilder.escapeForJsonTransport(entrada);
    }
    public static String callSugestaoNomeBlocoContextNaming(String selectedText, String kindText, String existingNamesText, String apiKey) throws Exception {
        String prompt = buildPromptSugestaoNomeBloco(selectedText, kindText, existingNamesText);

        UnifiedMcpModelGateway gateway = new UnifiedMcpModelGateway();

        ModelExecutionResponse response = gateway.executeContextNamingPrompt(
                prompt,
                McpAccessCredentials.forApiKey(apiKey)
        );

        if (response == null) {
            return "";
        }

        if (response.getPrimaryText() != null && response.getPrimaryText().trim().length() > 0) {
            return response.getPrimaryText();
        }

        if (response.getRawResponseBody() != null && response.getRawResponseBody().trim().length() > 0) {
            return response.getRawResponseBody();
        }

        return "";
    }
    
    /** * Extrai texto principal de qualquer resposta bruta. */
    public static String extractPrimaryText(String rawResponse) {
        return RESPONSE_EXTRACTOR.extractPrimaryText(rawResponse);
    }

    private static McpExecutionSupport createExecutionSupport() {
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
                TOOL_PROMPT_SECTIONS_ADAPTER
        );

        return wiring.createExecutionSupport();
    }

    private static McpAccessCredentials createCredentials(String apiKey, String cookieValue) {
        return McpAccessCredentials.forApiKeyAndCookie(apiKey, cookieValue);
    }
    private static String buildPromptSugestaoNomeBloco(String selectedText, String kindText, String existingNamesText) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Voce deve gerar um nome curto para um alvo de contexto selecionado.\n");
        prompt.append("REGRAS OBRIGATORIAS:\n");
        prompt.append("1. Responda com UMA unica palavra.\n");
        prompt.append("2. Use apenas letras minusculas e numeros.\n");
        prompt.append("3. Nao use acentos.\n");
        prompt.append("4. Nao use espacos.\n");
        prompt.append("5. Nao use underline, hifen, pontuacao ou simbolos.\n");
        prompt.append("6. O nome deve ter no maximo 12 caracteres.\n");
        prompt.append("7. O nome deve representar a funcao principal do alvo selecionado.\n");
        prompt.append("8. Prefira nomes intuitivos e concretos como validacao, retorno, query, dao, criteria, montagem, atributo, pedido, municipio, usuario, service, repository, config, sql, xml.\n");
        prompt.append("9. Se o trecho parecer consulta SQL/HQL/JPQL/Hibernate/JPA, prefira query, sql, hql, criteria ou jdbc conforme o caso.\n");
        prompt.append("10. Se o trecho parecer validacao, prefira validacao.\n");
        prompt.append("11. Se o trecho parecer retorno, prefira retorno.\n");
        prompt.append("12. Se o nome colidir com nomes ja existentes, escolha outro nome curto e diferente.\n");
        prompt.append("13. Responda APENAS com o nome final, sem JSON, sem explicacao, sem aspas e sem texto adicional.\n\n");

        prompt.append("TIPO DO BLOCO: ").append(kindText != null ? kindText : "").append("\n");
        prompt.append("NOMES JA EXISTENTES: ").append(existingNamesText != null ? existingNamesText : "").append("\n\n");
        prompt.append("TRECHO SELECIONADO:\n");
        prompt.append(selectedText != null ? selectedText : "");

        return prompt.toString();
    }
}