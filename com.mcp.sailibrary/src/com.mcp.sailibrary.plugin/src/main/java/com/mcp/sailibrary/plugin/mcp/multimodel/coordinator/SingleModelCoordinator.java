package com.mcp.sailibrary.plugin.mcp.multimodel.coordinator;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.orchestration.AgentToolRegistryFactory;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptSectionBuilder;
import com.mcp.sailibrary.plugin.chat.service.McpAgentResponseService;
import com.mcp.sailibrary.plugin.chat.support.AiResponse;
import com.mcp.sailibrary.plugin.mcp.DesenvolvimentoPromptBuilder;
import com.mcp.sailibrary.plugin.mcp.McpResponseExtractor;
import com.mcp.sailibrary.plugin.mcp.multimodel.gateway.UnifiedMcpModelGateway;
import com.mcp.sailibrary.plugin.mcp.multimodel.routing.DefaultMcpModelNameResolver;
import com.mcp.sailibrary.plugin.mcp.multimodel.routing.ModelNameResolver;
import com.mcp.sailibrary.plugin.mcp.multimodel.routing.PropertiesBackedMcpModelNameResolver;

/* --- version: "1.2" libraries: - File - List - ResourcesPlugin - AgentTool - AgentToolRegistryFactory - AgentToolPromptSectionBuilder - McpAgentResponseService - AiResponse - DesenvolvimentoPromptBuilder - McpResponseExtractor - UnifiedMcpModelGateway - DefaultMcpModelNameResolver - ModelNameResolver objetivo: "Preservar o fluxo monomodelo atual do plugin usando componentes desacoplados para prompt, transporte MCP e parse da resposta, distinguindo falha de infraestrutura de resposta valida da IA." --- */

/** * Coordenador compativel com a estrategia atual de um unico modelo. * * @author Renato Tomaz Nati * @since 2026-05-24 */
public class SingleModelCoordinator implements AgentModelCoordinator {

    private ModelNameResolver modelNameResolver;
    private UnifiedMcpModelGateway unifiedMcpModelGateway;
    private McpResponseExtractor mcpResponseExtractor;
    private McpAgentResponseService mcpAgentResponseService;
    private DesenvolvimentoPromptBuilder desenvolvimentoPromptBuilder;
    private AgentToolPromptSectionBuilder agentToolPromptSectionBuilder;

    public SingleModelCoordinator() {
        this(
                new PropertiesBackedMcpModelNameResolver(),
                new UnifiedMcpModelGateway(UnifiedMcpModelGateway.DEFAULT_MCP_API_URL),
                new McpResponseExtractor(),
                new McpAgentResponseService(),
                new DesenvolvimentoPromptBuilder(),
                new AgentToolPromptSectionBuilder()
        );
    }

    public SingleModelCoordinator(ModelNameResolver modelNameResolver, UnifiedMcpModelGateway unifiedMcpModelGateway, McpResponseExtractor mcpResponseExtractor, McpAgentResponseService mcpAgentResponseService, DesenvolvimentoPromptBuilder desenvolvimentoPromptBuilder, AgentToolPromptSectionBuilder agentToolPromptSectionBuilder) {
        this.modelNameResolver = modelNameResolver;
        this.unifiedMcpModelGateway = unifiedMcpModelGateway;
        this.mcpResponseExtractor = mcpResponseExtractor;
        this.mcpAgentResponseService = mcpAgentResponseService;
        this.desenvolvimentoPromptBuilder = desenvolvimentoPromptBuilder;
        this.agentToolPromptSectionBuilder = agentToolPromptSectionBuilder;
    }

    @Override
    public AiResponse executarMissao(String selectedCode, String fullFileText, String instrucao, String apiKey) throws Exception {

        String textoSelecionado = selectedCode != null ? selectedCode : "";
        String textoArquivoCompleto = fullFileText != null ? fullFileText : "";
        String textoInstrucao = instrucao != null ? instrucao : "";

        boolean possuiTrechoTextual = textoSelecionado.trim().length() > 0;
        boolean possuiArquivoTextual = textoArquivoCompleto.trim().length() > 0;
        boolean possuiContextoEstrutural = possuiContextoEstruturalNoPrompt(textoInstrucao);

        if (!possuiTrechoTextual && !possuiArquivoTextual && !possuiContextoEstrutural) {
            throw new IllegalStateException(
                    "Nenhum insumo textual ou estrutural principal foi encontrado para a chamada MCP. "
                            + "Selecione um trecho ou mantenha um contexto estrutural utilizavel na sessao."
            );
        }

        String textoSelecionadoParaPrompt = possuiTrechoTextual ? textoSelecionado : "[SEM_TRECHO_SELECIONADO]";
        String textoArquivoCompletoParaPrompt = possuiArquivoTextual ? textoArquivoCompleto : "[SEM_ARQUIVO_TEXTUAL_ATIVO]";
        String modoOperacionalDetectado = (possuiTrechoTextual || possuiArquivoTextual)
                ? "MODO_TEXTUAL"
                : "MODO_ESTRUTURAL";

        File raizWorkspace = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
        List<AgentTool> ferramentasPrompt = new AgentToolRegistryFactory().build(raizWorkspace, null, 0);

        String secaoFerramentas = agentToolPromptSectionBuilder.buildToolsSection(ferramentasPrompt);
        String secaoExemplosFerramentas = agentToolPromptSectionBuilder.buildExamplesSection(ferramentasPrompt, 1);

        String promptEngenharia = desenvolvimentoPromptBuilder.build(
                modoOperacionalDetectado,
                textoSelecionadoParaPrompt,
                textoArquivoCompletoParaPrompt,
                textoInstrucao,
                secaoFerramentas,
                secaoExemplosFerramentas
        );

        String modelName = resolvePlannerModelNameSeguro();

        System.out.println("[MCP DEBUG] SingleModelCoordinator");
        System.out.println("[MCP DEBUG] modelName=" + modelName);
        System.out.println("[MCP DEBUG] modoOperacionalDetectado=" + modoOperacionalDetectado);
        System.out.println("[MCP DEBUG] promptLength=" + promptEngenharia.length());

        String rawResponse = unifiedMcpModelGateway.callModel(modelName, promptEngenharia, apiKey);

        System.out.println("[MCP DEBUG] rawResponse=" + truncateForDebug(rawResponse, 3000));

        if (mcpAgentResponseService.isModelInfrastructureFailureText(rawResponse)) {
            System.out.println("[MCP DEBUG] rawResponse classificado como falha de infraestrutura.");
            return mcpAgentResponseService.buildInfrastructureFailureResponse(
                    truncateForDebug(rawResponse, 1200),
                    false
            );
        }

        String textResponse = mcpResponseExtractor.extractPrimaryText(rawResponse);

        System.out.println("[MCP DEBUG] textResponse=" + truncateForDebug(textResponse, 3000));

        if (mcpAgentResponseService.isModelInfrastructureFailureText(textResponse)) {
            System.out.println("[MCP DEBUG] textResponse classificado como falha de infraestrutura.");
            return mcpAgentResponseService.buildInfrastructureFailureResponse(
                    truncateForDebug(textResponse, 1200),
                    false
            );
        }

        AiResponse resposta = mcpAgentResponseService.interpretarRespostaIA(textResponse);
        resposta = mcpAgentResponseService.normalizarProtocoloFerramentaLegado(resposta);

        if (resposta == null) {
            System.out.println("[MCP DEBUG] parsedAiResponse=null");
            return mcpAgentResponseService.buildInfrastructureFailureResponse(
                    "Falha ao interpretar a resposta estruturada do modelo. O retorno nao foi tratado como JSON valido do protocolo interno.",
                    false
            );
        }

        if (resposta != null) {
            System.out.println("[MCP DEBUG] parsedAiResponse.action=" + resposta.getAction());
            System.out.println("[MCP DEBUG] parsedAiResponse.tool=" + resposta.getTool());
            System.out.println("[MCP DEBUG] parsedAiResponse.question=" + resposta.getQuestion());
            System.out.println("[MCP DEBUG] parsedAiResponse.explanation=" + truncateForDebug(resposta.getExplanation(), 1000));
            System.out.println("[MCP DEBUG] parsedAiResponse.content=" + truncateForDebug(resposta.getContent(), 1500));
        }

        return resposta;
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

    private String resolvePlannerModelNameSeguro() {
        ModelNameResolver resolverEfetivo = modelNameResolver;
        if (resolverEfetivo == null) {
            System.out.println("[MCP CONFIG DEBUG] ModelNameResolver nulo no SingleModelCoordinator. Usando resolver default.");
            resolverEfetivo = new DefaultMcpModelNameResolver();
        }

        String modelName = resolverEfetivo.resolvePlannerModelName();
        if (isBlank(modelName)) {
            System.out.println("[MCP CONFIG DEBUG] resolvePlannerModelName vazio no SingleModelCoordinator. Usando fallback do resolver default.");
            modelName = new DefaultMcpModelNameResolver().resolvePlannerModelName();
        }

        System.out.println("[MCP CONFIG DEBUG] SingleModelCoordinator usando planner model=[" + modelName + "]");
        return modelName;
    }

    private boolean possuiContextoEstruturalNoPrompt(String textoInstrucao) {
        if (textoInstrucao == null || textoInstrucao.trim().length() == 0) {
            return false;
        }

        String texto = textoInstrucao;

        if (texto.contains("@")) {
            java.util.regex.Pattern aliasPattern = java.util.regex.Pattern.compile("@[a-zA-Z0-9_]+");
            java.util.regex.Matcher matcher = aliasPattern.matcher(texto);
            if (matcher.find()) {
                return true;
            }
        }

        if (texto.contains("=== CONTEXTO ESTRUTURAL DA SESSAO ===")) {
            return true;
        }

        if (texto.contains("FOCO_PRINCIPAL_ESTRUTURAL:")) {
            return true;
        }

        if (texto.contains("ESCOPO_EDITAVEL_ESTRUTURAL:")) {
            return true;
        }

        if (texto.contains("ESCOPO_REFERENCIAL_ESTRUTURAL:")) {
            return true;
        }

        if (texto.contains("Contexto estrutural")) {
            return true;
        }

        if (texto.contains("ALVO PRINCIPAL:") && texto.contains("arquivo=")) {
            return true;
        }

        return false;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}