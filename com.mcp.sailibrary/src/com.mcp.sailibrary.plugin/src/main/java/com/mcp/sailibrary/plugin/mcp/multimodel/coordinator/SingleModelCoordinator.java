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
import com.mcp.sailibrary.plugin.mcp.core.ModelChannel;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionResponse;
import com.mcp.sailibrary.plugin.mcp.core.StructuralContextDetector;
import com.mcp.sailibrary.plugin.mcp.multimodel.gateway.UnifiedMcpModelGateway;
import com.mcp.sailibrary.plugin.mcp.multimodel.routing.ModelNameResolver;
import com.mcp.sailibrary.plugin.mcp.multimodel.routing.PropertiesBackedMcpModelNameResolver;

/* --- version: "2.0" libraries: - File - List - ResourcesPlugin - AgentTool - AgentToolRegistryFactory - AgentToolPromptSectionBuilder - McpAgentResponseService - AiResponse - DesenvolvimentoPromptBuilder - McpResponseExtractor - UnifiedMcpModelGateway - StructuralContextDetector objetivo: "Preservar o fluxo monomodelo atual, agora suportando transporte legacy ou streaming por configuracao de canal." --- */

/** * Coordenador compativel com a estrategia atual de um unico modelo. * * <p>Agora o planner pode ser legado ou streaming sem alterar esta classe. * A decisao fica no profile do canal PLANNER.</p> * * @author Renato Tomaz Nati * @since 2026-05-26 */
public class SingleModelCoordinator implements AgentModelCoordinator {

    private ModelNameResolver modelNameResolver;
    private UnifiedMcpModelGateway unifiedMcpModelGateway;
    private McpResponseExtractor mcpResponseExtractor;
    private McpAgentResponseService mcpAgentResponseService;
    private DesenvolvimentoPromptBuilder desenvolvimentoPromptBuilder;
    private AgentToolPromptSectionBuilder agentToolPromptSectionBuilder;
    private StructuralContextDetector structuralContextDetector;
    private ModelChannel modelChannel;

    public SingleModelCoordinator() {
        this(
                new PropertiesBackedMcpModelNameResolver(),
                new UnifiedMcpModelGateway(UnifiedMcpModelGateway.DEFAULT_MCP_API_URL),
                new McpResponseExtractor(),
                new McpAgentResponseService(),
                new DesenvolvimentoPromptBuilder(),
                new AgentToolPromptSectionBuilder(),
                new StructuralContextDetector(),
                ModelChannel.PLANNER
        );
    }

    public SingleModelCoordinator(ModelChannel modelChannel) {
        this(
                new PropertiesBackedMcpModelNameResolver(),
                new UnifiedMcpModelGateway(UnifiedMcpModelGateway.DEFAULT_MCP_API_URL),
                new McpResponseExtractor(),
                new McpAgentResponseService(),
                new DesenvolvimentoPromptBuilder(),
                new AgentToolPromptSectionBuilder(),
                new StructuralContextDetector(),
                modelChannel
        );
    }

    public SingleModelCoordinator(ModelNameResolver modelNameResolver, UnifiedMcpModelGateway unifiedMcpModelGateway, McpResponseExtractor mcpResponseExtractor, McpAgentResponseService mcpAgentResponseService, DesenvolvimentoPromptBuilder desenvolvimentoPromptBuilder, AgentToolPromptSectionBuilder agentToolPromptSectionBuilder) {
        this(
                modelNameResolver,
                unifiedMcpModelGateway,
                mcpResponseExtractor,
                mcpAgentResponseService,
                desenvolvimentoPromptBuilder,
                agentToolPromptSectionBuilder,
                new StructuralContextDetector(),
                ModelChannel.PLANNER
        );
    }

    public SingleModelCoordinator(ModelNameResolver modelNameResolver, UnifiedMcpModelGateway unifiedMcpModelGateway, McpResponseExtractor mcpResponseExtractor, McpAgentResponseService mcpAgentResponseService, DesenvolvimentoPromptBuilder desenvolvimentoPromptBuilder, AgentToolPromptSectionBuilder agentToolPromptSectionBuilder, StructuralContextDetector structuralContextDetector) {
        this(modelNameResolver, unifiedMcpModelGateway, mcpResponseExtractor, mcpAgentResponseService, desenvolvimentoPromptBuilder, agentToolPromptSectionBuilder, structuralContextDetector, ModelChannel.PLANNER);
    }

    public SingleModelCoordinator(ModelNameResolver modelNameResolver, UnifiedMcpModelGateway unifiedMcpModelGateway, McpResponseExtractor mcpResponseExtractor, McpAgentResponseService mcpAgentResponseService, DesenvolvimentoPromptBuilder desenvolvimentoPromptBuilder, AgentToolPromptSectionBuilder agentToolPromptSectionBuilder, StructuralContextDetector structuralContextDetector, ModelChannel modelChannel) {
        this.modelNameResolver = modelNameResolver;
        this.unifiedMcpModelGateway = unifiedMcpModelGateway != null
                ? unifiedMcpModelGateway
                : new UnifiedMcpModelGateway(UnifiedMcpModelGateway.DEFAULT_MCP_API_URL);
        this.mcpResponseExtractor = mcpResponseExtractor != null ? mcpResponseExtractor : new McpResponseExtractor();
        this.mcpAgentResponseService = mcpAgentResponseService != null ? mcpAgentResponseService : new McpAgentResponseService();
        this.desenvolvimentoPromptBuilder = desenvolvimentoPromptBuilder != null ? desenvolvimentoPromptBuilder : new DesenvolvimentoPromptBuilder();
        this.agentToolPromptSectionBuilder = agentToolPromptSectionBuilder != null ? agentToolPromptSectionBuilder : new AgentToolPromptSectionBuilder();
        this.structuralContextDetector = structuralContextDetector != null ? structuralContextDetector : new StructuralContextDetector();
        this.modelChannel = modelChannel != null ? modelChannel : ModelChannel.PLANNER;
    }

    @Override
    public AiResponse executarMissao(String selectedCode, String fullFileText, String instrucao, String apiKey) throws Exception {

        String textoSelecionado = selectedCode != null ? selectedCode : "";
        String textoArquivoCompleto = fullFileText != null ? fullFileText : "";
        String textoInstrucao = instrucao != null ? instrucao : "";

        boolean possuiTrechoTextual = textoSelecionado.trim().length() > 0;
        boolean possuiArquivoTextual = textoArquivoCompleto.trim().length() > 0;
        boolean possuiContextoEstrutural = structuralContextDetector.hasStructuralContext(textoInstrucao);

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

        ModelExecutionProfile profile = unifiedMcpModelGateway.resolveProfile(modelChannel);

        System.out.println("[MCP DEBUG] SingleModelCoordinator");
        System.out.println("[MCP DEBUG] channel=" + modelChannel.name());
        System.out.println("[MCP DEBUG] " + modelChannel.getPropertySuffix() + ".transport=" + profile.getTransportKind());
        System.out.println("[MCP DEBUG] " + modelChannel.getPropertySuffix() + ".requestFormat=" + profile.getRequestFormatKind());
        System.out.println("[MCP DEBUG] " + modelChannel.getPropertySuffix() + ".responseFormat=" + profile.getResponseFormatKind());
        System.out.println("[MCP DEBUG] " + modelChannel.getPropertySuffix() + ".model=" + profile.resolveEffectiveModelName());
        System.out.println("[MCP DEBUG] modoOperacionalDetectado=" + modoOperacionalDetectado);
        System.out.println("[MCP DEBUG] promptLength=" + promptEngenharia.length());

        ModelExecutionResponse executionResponse = unifiedMcpModelGateway.executeChannelPrompt(modelChannel, promptEngenharia, com.mcp.sailibrary.plugin.mcp.core.McpAccessCredentials.forApiKey(apiKey));

        if (executionResponse == null) {
            return mcpAgentResponseService.buildInfrastructureFailureResponse(
                    "Falha ao executar o planner. Nenhuma resposta estruturada foi retornada pelo gateway.",
                    false
            );
        }

        String rawResponse = executionResponse.getRawResponseBody();
        String textResponse = executionResponse.getPrimaryText();

        String textResponseNormalizado = normalizarRespostaEstruturada(textResponse);
        System.out.println("[MCP DEBUG] textResponseNormalizado=" + truncateForDebug(textResponseNormalizado, 3000));

        if (mcpAgentResponseService.isModelInfrastructureFailureText(textResponseNormalizado)) {
            System.out.println("[MCP DEBUG] textResponse classificado como falha de infraestrutura.");
            return mcpAgentResponseService.buildInfrastructureFailureResponse(
                    truncateForDebug(textResponseNormalizado, 1200),
                    false
            );
        }

        AiResponse resposta = mcpAgentResponseService.interpretarRespostaIA(textResponseNormalizado);
        resposta = mcpAgentResponseService.normalizarProtocoloFerramentaLegado(resposta);

        if ((resposta == null || !mcpAgentResponseService.respostaEstruturadaValida(resposta))
                && rawResponse != null
                && rawResponse.trim().length() > 0) {

            String respostaCruaNormalizada = normalizarRespostaEstruturada(
                    mcpResponseExtractor.extractPrimaryText(rawResponse)
            );

            if (respostaCruaNormalizada != null
                    && respostaCruaNormalizada.trim().length() > 0
                    && !respostaCruaNormalizada.equals(textResponseNormalizado)) {

                System.out.println("[MCP DEBUG] Tentando recuperar parse a partir do raw response normalizado.");
                resposta = mcpAgentResponseService.interpretarRespostaIA(respostaCruaNormalizada);
                resposta = mcpAgentResponseService.normalizarProtocoloFerramentaLegado(resposta);

                if (resposta != null && mcpAgentResponseService.respostaEstruturadaValida(resposta)) {
                    textResponseNormalizado = respostaCruaNormalizada;
                }
            }
        }

        if (resposta == null || !mcpAgentResponseService.respostaEstruturadaValida(resposta)) {
            System.out.println("[MCP DEBUG] parsedAiResponse invalido apos normalizacao.");
            return mcpAgentResponseService.buildInfrastructureFailureResponse(
                    "Falha de protocolo da IA. O retorno veio duplicado, incompleto ou fora do contrato estruturado esperado.",
                    false
            );
        }

        System.out.println("[MCP DEBUG] parsedAiResponse.action=" + resposta.getAction());
        System.out.println("[MCP DEBUG] parsedAiResponse.tool=" + resposta.getTool());
        System.out.println("[MCP DEBUG] parsedAiResponse.question=" + resposta.getQuestion());
        System.out.println("[MCP DEBUG] parsedAiResponse.explanation=" + truncateForDebug(resposta.getExplanation(), 1000));
        System.out.println("[MCP DEBUG] parsedAiResponse.content=" + truncateForDebug(resposta.getContent(), 1500));

        return resposta;
    }
    private String normalizarRespostaEstruturada(String texto) {
        if (texto == null) {
            return "";
        }

        String textoNormalizado = texto.trim();
        String primeiroBloco = extrairPrimeiroBlocoEstruturado(textoNormalizado);

        if (primeiroBloco.length() > 0) {
            if (!primeiroBloco.equals(textoNormalizado)) {
                System.out.println("[MCP DEBUG] Resposta estruturada com ruido ou duplicacao detectada. Usando primeiro bloco valido.");
            }
            return primeiroBloco;
        }

        return textoNormalizado;
    }

    private String extrairPrimeiroBlocoEstruturado(String texto) {
        if (texto == null) {
            return "";
        }

        int inicioThinking = texto.indexOf("<thinking>");
        int inicioCodigo = texto.indexOf("<codigo_final>");
        int fimCodigo = texto.indexOf("</codigo_final>");

        if (inicioThinking >= 0 && fimCodigo > inicioThinking) {
            return texto.substring(inicioThinking, fimCodigo + "</codigo_final>".length()).trim();
        }

        if (inicioCodigo >= 0 && fimCodigo > inicioCodigo) {
            return texto.substring(inicioCodigo, fimCodigo + "</codigo_final>".length()).trim();
        }

        return "";
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
}