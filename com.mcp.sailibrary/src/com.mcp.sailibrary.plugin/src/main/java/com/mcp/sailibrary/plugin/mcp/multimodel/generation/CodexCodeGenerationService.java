package com.mcp.sailibrary.plugin.mcp.multimodel.generation;

import com.mcp.sailibrary.plugin.chat.service.McpAgentResponseService;
import com.mcp.sailibrary.plugin.chat.support.AiResponse;
import com.mcp.sailibrary.plugin.mcp.McpResponseExtractor;
import com.mcp.sailibrary.plugin.mcp.multimodel.gateway.UnifiedMcpModelGateway;
import com.mcp.sailibrary.plugin.mcp.multimodel.routing.DefaultMcpModelNameResolver;
import com.mcp.sailibrary.plugin.mcp.multimodel.routing.ModelNameResolver;
import com.mcp.sailibrary.plugin.mcp.multimodel.routing.PropertiesBackedMcpModelNameResolver;

/* --- version: "1.1" libraries: - com.mcp.sailibrary.plugin.chat.service.McpAgentResponseService - com.mcp.sailibrary.plugin.chat.support.AiResponse - com.mcp.sailibrary.plugin.mcp.McpResponseExtractor - com.mcp.sailibrary.plugin.mcp.multimodel.gateway.UnifiedMcpModelGateway - com.mcp.sailibrary.plugin.mcp.multimodel.routing.DefaultMcpModelNameResolver - com.mcp.sailibrary.plugin.mcp.multimodel.routing.ModelNameResolver - com.mcp.sailibrary.plugin.mcp.multimodel.generation.CodeGenerationPromptBuilder - com.mcp.sailibrary.plugin.mcp.multimodel.generation.CodeGenerationService objetivo: "Executar geracao ou refatoracao de codigo usando um modelo especializado, sem hardcode de nome de modelo no proprio servico." --- */

/** * Implementacao concreta do servico de geracao de codigo. * * <p>Esta classe usa um modelo especializado em codigo para gerar ou refatorar * trechos a partir de um plano de implementacao previamente consolidado. O * nome do modelo e resolvido externamente pelo ModelNameResolver, evitando * acoplamento local a um nome fixo.</p> * * @author Renato Tomaz Nati * @since 2026-05-24 */
public class CodexCodeGenerationService implements CodeGenerationService {

    private ModelNameResolver modelNameResolver;
    private UnifiedMcpModelGateway unifiedMcpModelGateway;
    private McpResponseExtractor mcpResponseExtractor;
    private McpAgentResponseService mcpAgentResponseService;
    private CodeGenerationPromptBuilder codeGenerationPromptBuilder;

    /** * Caller: bootstrapping do circuito multi-modelo * Callee: DefaultMcpModelNameResolver, UnifiedMcpModelGateway, McpResponseExtractor, McpAgentResponseService, CodeGenerationPromptBuilder * Objetivo: Inicializar o gerador de codigo com configuracao padrao. * Feature: Mantem compatibilidade com o circuito atual, sem fixar o nome do modelo aqui. * Data modificacao: 2026-05-24 00:00 * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public CodexCodeGenerationService() {
        this(
                new PropertiesBackedMcpModelNameResolver(),
                new UnifiedMcpModelGateway(UnifiedMcpModelGateway.DEFAULT_MCP_API_URL),
                new McpResponseExtractor(),
                new McpAgentResponseService(),
                new CodeGenerationPromptBuilder()
        );
    }

    /** * Caller: bootstrapping do circuito multi-modelo * Callee: N/A * Objetivo: Inicializar o gerador de codigo com injecao explicita de resolver, gateway e builders. * Data modificacao: 2026-05-24 00:00 * * @param modelNameResolver resolvedor de nomes de modelos * @param unifiedMcpModelGateway gateway MCP unificado * @param mcpResponseExtractor extrator de texto MCP * @param mcpAgentResponseService interpretador de resposta interna * @param codeGenerationPromptBuilder builder do prompt de geracao * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public CodexCodeGenerationService(ModelNameResolver modelNameResolver, UnifiedMcpModelGateway unifiedMcpModelGateway, McpResponseExtractor mcpResponseExtractor, McpAgentResponseService mcpAgentResponseService, CodeGenerationPromptBuilder codeGenerationPromptBuilder) {
        this.modelNameResolver = modelNameResolver;
        this.unifiedMcpModelGateway = unifiedMcpModelGateway;
        this.mcpResponseExtractor = mcpResponseExtractor;
        this.mcpAgentResponseService = mcpAgentResponseService;
        this.codeGenerationPromptBuilder = codeGenerationPromptBuilder;
    }

    /** * Caller: MultiModelCoordinator * Callee: CodeGenerationPromptBuilder.build, ModelNameResolver.resolveCodeGeneratorModelName, UnifiedMcpModelGateway.callModel, McpResponseExtractor.extractPrimaryText, McpAgentResponseService.interpretarRespostaIA, McpAgentResponseService.normalizarProtocoloFerramentaLegado * Objetivo: Gerar codigo a partir do pedido original, do plano e do contexto textual atual, retornando um AiResponse compativel com o fluxo do plugin. * Data modificacao: 2026-05-24 00:00 * * @param pedidoOriginal pedido original do usuario * @param planoImplementacao plano tecnico consolidado * @param selectedCode trecho textual selecionado * @param fullFileText conteudo integral do arquivo atual * @param actionEsperada acao final esperada * @param apiKey chave MCP * @return resposta estruturada no formato AiResponse * @throws Exception quando houver falha de transporte, parse ou geracao * * @author Renato Tomaz Nati * @since 2026-05-24 */
    @Override
    public AiResponse gerarCodigo(String pedidoOriginal, String planoImplementacao, String selectedCode, String fullFileText, String actionEsperada, String apiKey) throws Exception {

        String promptGeracaoCodigo = codeGenerationPromptBuilder.build(
                pedidoOriginal,
                planoImplementacao,
                selectedCode,
                fullFileText,
                actionEsperada
        );

        String modelName = resolveCodeGeneratorModelNameSeguro();

        System.out.println("[MCP DEBUG] CodexCodeGenerationService");
        System.out.println("[MCP DEBUG] modelName=" + modelName);
        System.out.println("[MCP DEBUG] actionEsperada=" + actionEsperada);
        System.out.println("[MCP DEBUG] promptLength=" + promptGeracaoCodigo.length());
        System.out.println("[MCP DEBUG] pedidoOriginal="
                + truncateForDebug(pedidoOriginal, 1000));
        System.out.println("[MCP DEBUG] planoImplementacao="
                + truncateForDebug(planoImplementacao, 2000));

        String rawResponse = unifiedMcpModelGateway.callModel(modelName, promptGeracaoCodigo, apiKey);

        System.out.println("[MCP DEBUG] rawResponse="
                + truncateForDebug(rawResponse, 3000));

        String textResponse = mcpResponseExtractor.extractPrimaryText(rawResponse);

        System.out.println("[MCP DEBUG] textResponse="
                + truncateForDebug(textResponse, 3000));

        AiResponse respostaGerada = mcpAgentResponseService.interpretarRespostaIA(textResponse);
        respostaGerada = mcpAgentResponseService.normalizarProtocoloFerramentaLegado(respostaGerada);

        if (respostaGerada != null) {
            System.out.println("[MCP DEBUG] generatedAiResponse.action=" + respostaGerada.getAction());
            System.out.println("[MCP DEBUG] generatedAiResponse.tool=" + respostaGerada.getTool());
            System.out.println("[MCP DEBUG] generatedAiResponse.explanation="
                    + truncateForDebug(respostaGerada.getExplanation(), 1000));
            System.out.println("[MCP DEBUG] generatedAiResponse.content="
                    + truncateForDebug(respostaGerada.getContent(), 2000));
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
    /** * Caller: gerarCodigo * Callee: ModelNameResolver.resolveCodeGeneratorModelName * Objetivo: Resolver o nome do modelo de geracao com fallback seguro. * Data modificacao: 2026-05-24 00:00 * * @return nome do modelo de geracao * * @author Renato Tomaz Nati * @since 2026-05-24 */
    /** * Caller: gerarCodigo * Callee: ModelNameResolver.resolveCodeGeneratorModelName, DefaultMcpModelNameResolver.resolveCodeGeneratorModelName * Objetivo: Resolver o nome do modelo gerador com fallback controlado. * Feature: Usa o fallback centralizado do resolver default e evita nomes * hardcoded espalhados pelo servico. * Data modificacao: 2026-05-24 00:00 * * @return nome do modelo gerador de codigo * * @author Renato Tomaz Nati * @since 2026-05-24 */
    private String resolveCodeGeneratorModelNameSeguro() {
        ModelNameResolver resolverEfetivo = modelNameResolver;
        if (resolverEfetivo == null) {
            System.out.println("[MCP CONFIG DEBUG] ModelNameResolver nulo no CodexCodeGenerationService. Usando resolver default.");
            resolverEfetivo = new DefaultMcpModelNameResolver();
        }

        String modelName = resolverEfetivo.resolveCodeGeneratorModelName();
        if (isBlank(modelName)) {
            System.out.println("[MCP CONFIG DEBUG] resolveCodeGeneratorModelName vazio no CodexCodeGenerationService. Usando fallback do resolver default.");
            modelName = new DefaultMcpModelNameResolver().resolveCodeGeneratorModelName();
        }

        System.out.println("[MCP CONFIG DEBUG] CodexCodeGenerationService usando model=[" + modelName + "]");
        return modelName;
    }

    /** * Caller: metodos internos * Callee: N/A * Objetivo: Validar se um texto esta em branco. * Data modificacao: 2026-05-24 00:00 * * @param value valor a validar * @return true quando estiver em branco * * @author Renato Tomaz Nati * @since 2026-05-24 */
    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}