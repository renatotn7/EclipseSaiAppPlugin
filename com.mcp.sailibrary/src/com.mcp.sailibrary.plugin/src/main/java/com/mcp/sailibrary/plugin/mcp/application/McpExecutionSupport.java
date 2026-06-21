package com.mcp.sailibrary.plugin.mcp.application;

import com.mcp.sailibrary.plugin.mcp.core.McpAccessCredentials;
import com.mcp.sailibrary.plugin.mcp.core.ModelChannel;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionRequest;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionResponse;
import com.mcp.sailibrary.plugin.mcp.ports.ModelExecutionProfileResolver;

/** * Fachada interna simples para executar chamadas por canal cognitivo. * * <p>Esta classe existe para que o restante do sistema nao precise montar * manualmente ModelExecutionRequest toda vez.</p> * * <p>Uso tipico: * - executePrompt(PLANNER, prompt, credenciais) * - executeRawJson(CODE_GENERATOR, rawJson, credenciais)</p> * * @author Renato Tomaz Nati * @since 2026-05-26 */
public class McpExecutionSupport {

    private final ModelExecutionEngine modelExecutionEngine;
    private final ModelExecutionProfileResolver modelExecutionProfileResolver;

    public McpExecutionSupport(ModelExecutionEngine modelExecutionEngine, ModelExecutionProfileResolver modelExecutionProfileResolver) {
        if (modelExecutionEngine == null) {
            throw new IllegalArgumentException("Erro Operacional: modelExecutionEngine nao pode ser nulo.");
        }

        if (modelExecutionProfileResolver == null) {
            throw new IllegalArgumentException("Erro Operacional: modelExecutionProfileResolver nao pode ser nulo.");
        }

        this.modelExecutionEngine = modelExecutionEngine;
        this.modelExecutionProfileResolver = modelExecutionProfileResolver;
    }

    /* * Feature: executa prompt simples por canal cognitivo. * Data: 2026-05-27 10:05 * Chamado por: * - UnifiedMcpModelGateway.executeChannelPrompt * - servicos cognitivos do pacote mcp.multimodel * * Chama: * - ModelExecutionRequest.setChannel * - ModelExecutionRequest.setPrompt * - ModelExecutionRequest.setCredentials * - ModelExecutionEngine.execute(ModelExecutionRequest, ModelExecutionProfileResolver) * * Objetivo: * - Encapsular a criacao do request * - Registrar rastreabilidade antes de delegar ao engine * - Nao executar retry nem chamada duplicada nesta camada */
    /** * Feature: executa prompt simples por canal. * Data: 2026-05-27 10:20 * Quem chama: * - UnifiedMcpModelGateway.executeChannelPrompt * Quem eh chamado: * - modelExecutionEngine.execute * Objetivo: * - criar o request neutro * - manter rastreabilidade da entrada * - delegar a execucao ao motor */
    public ModelExecutionResponse executePrompt(ModelChannel channel, String prompt, McpAccessCredentials credentials) throws Exception {
        ModelExecutionRequest request = new ModelExecutionRequest();
        request.setChannel(channel);
        request.setPrompt(prompt);
        request.setCredentials(credentials);

        String executionId = "support-prompt-" + Long.toHexString(System.nanoTime());
        String requestIdentity = Integer.toHexString(System.identityHashCode(request));

        System.out.println("[MCP SUPPORT DEBUG] ===============================================");
        System.out.println("[MCP SUPPORT DEBUG] modo=PROMPT");
        System.out.println("[MCP SUPPORT DEBUG] executionId=" + executionId);
        System.out.println("[MCP SUPPORT DEBUG] requestIdentity=" + requestIdentity);
        System.out.println("[MCP SUPPORT DEBUG] channel=" + (channel != null ? channel.name() : "null"));
        System.out.println("[MCP SUPPORT DEBUG] promptLength=" + (prompt != null ? prompt.length() : 0));
        System.out.println("[MCP SUPPORT DEBUG] inputHasApiKey="
                + (credentials != null && credentials.hasApiKey()));
        System.out.println("[MCP SUPPORT DEBUG] inputHasCookieValue="
                + (credentials != null && credentials.hasCookieValue()));
        System.out.println("[MCP SUPPORT DEBUG] prompt=");
        System.out.println(prompt != null ? prompt : "");
        System.out.println("[MCP SUPPORT DEBUG] ===============================================");

        ModelExecutionResponse executionResponse = modelExecutionEngine.execute(request, modelExecutionProfileResolver);

        System.out.println("[MCP SUPPORT DEBUG] -----------------------------------------------");
        System.out.println("[MCP SUPPORT DEBUG] modo=PROMPT_RESULT");
        System.out.println("[MCP SUPPORT DEBUG] executionId=" + executionId);
        System.out.println("[MCP SUPPORT DEBUG] requestIdentity=" + requestIdentity);
        System.out.println("[MCP SUPPORT DEBUG] primaryTextLength="
                + (executionResponse != null && executionResponse.getPrimaryText() != null
                        ? executionResponse.getPrimaryText().length()
                        : 0));
        System.out.println("[MCP SUPPORT DEBUG] primaryText=");
        System.out.println(executionResponse != null ? executionResponse.getPrimaryText() : "");
        System.out.println("[MCP SUPPORT DEBUG] -----------------------------------------------");

        return executionResponse;
    }

    /** * Feature: executa raw json por canal. * Data: 2026-05-27 10:20 * Quem chama: * - UnifiedMcpModelGateway.executeChannelRawJson * Quem eh chamado: * - modelExecutionEngine.execute * Objetivo: * - encaminhar body cru para codecs de request raw * - manter rastreabilidade da entrada * - delegar a execucao ao motor */
    public ModelExecutionResponse executeRawJson(ModelChannel channel, String rawJsonBody, McpAccessCredentials credentials) throws Exception {
        ModelExecutionRequest request = new ModelExecutionRequest();
        request.setChannel(channel);
        request.setRawJsonBody(rawJsonBody);
        request.setCredentials(credentials);

        String executionId = "support-raw-" + Long.toHexString(System.nanoTime());
        String requestIdentity = Integer.toHexString(System.identityHashCode(request));

        System.out.println("[MCP SUPPORT DEBUG] ===============================================");
        System.out.println("[MCP SUPPORT DEBUG] modo=RAW_JSON");
        System.out.println("[MCP SUPPORT DEBUG] executionId=" + executionId);
        System.out.println("[MCP SUPPORT DEBUG] requestIdentity=" + requestIdentity);
        System.out.println("[MCP SUPPORT DEBUG] channel=" + (channel != null ? channel.name() : "null"));
        System.out.println("[MCP SUPPORT DEBUG] rawJsonLength=" + (rawJsonBody != null ? rawJsonBody.length() : 0));
        System.out.println("[MCP SUPPORT DEBUG] inputHasApiKey="
                + (credentials != null && credentials.hasApiKey()));
        System.out.println("[MCP SUPPORT DEBUG] inputHasCookieValue="
                + (credentials != null && credentials.hasCookieValue()));
        System.out.println("[MCP SUPPORT DEBUG] rawJsonBody=");
        System.out.println(rawJsonBody != null ? rawJsonBody : "");
        System.out.println("[MCP SUPPORT DEBUG] ===============================================");

        ModelExecutionResponse executionResponse = modelExecutionEngine.execute(request, modelExecutionProfileResolver);

        System.out.println("[MCP SUPPORT DEBUG] -----------------------------------------------");
        System.out.println("[MCP SUPPORT DEBUG] modo=RAW_JSON_RESULT");
        System.out.println("[MCP SUPPORT DEBUG] executionId=" + executionId);
        System.out.println("[MCP SUPPORT DEBUG] requestIdentity=" + requestIdentity);
        System.out.println("[MCP SUPPORT DEBUG] primaryTextLength="
                + (executionResponse != null && executionResponse.getPrimaryText() != null
                        ? executionResponse.getPrimaryText().length()
                        : 0));
        System.out.println("[MCP SUPPORT DEBUG] primaryText=");
        System.out.println(executionResponse != null ? executionResponse.getPrimaryText() : "");
        System.out.println("[MCP SUPPORT DEBUG] -----------------------------------------------");

        return executionResponse;
    }

    /** * Resolve o profile efetivo de um canal. * * @param channel canal cognitivo * @return profile resolvido */
    public ModelExecutionProfile resolveProfile(ModelChannel channel) {
        return modelExecutionProfileResolver.resolve(channel);
    }

    public ModelExecutionEngine getModelExecutionEngine() {
        return modelExecutionEngine;
    }

    public ModelExecutionProfileResolver getModelExecutionProfileResolver() {
        return modelExecutionProfileResolver;
    }
    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}