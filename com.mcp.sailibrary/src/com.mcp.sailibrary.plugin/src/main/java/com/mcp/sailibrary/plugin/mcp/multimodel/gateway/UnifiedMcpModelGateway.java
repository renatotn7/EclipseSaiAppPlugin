package com.mcp.sailibrary.plugin.mcp.multimodel.gateway;

import com.mcp.sailibrary.plugin.mcp.McpHttpGateway;
import com.mcp.sailibrary.plugin.mcp.McpPayloadBuilder;

/* --- version: "1.1" libraries: - com.mcp.sailibrary.plugin.mcp.McpHttpGateway - com.mcp.sailibrary.plugin.mcp.McpPayloadBuilder objetivo: "Executar chamadas MCP para diferentes modelos usando o mesmo payload e o mesmo protocolo, alterando apenas o nome do modelo remoto." --- */

/** * Gateway unificado para chamadas MCP orientadas a modelo. * * <p>Esta classe preserva o protocolo atual do plugin: * <ul> * <li>mesmo endpoint</li> * <li>mesmo payload JSON-RPC</li> * <li>mesma estrutura de chamada</li> * </ul> * </p> * * <p>A unica variacao operacional e o nome do modelo remoto informado no campo * name do MCP intermediador.</p> * * @author Renato Tomaz Nati * @since 2026-05-24 */
public class UnifiedMcpModelGateway {

    public static final String DEFAULT_MCP_API_URL = "https://sai-library.saiapplications.com/api/mcp";

    private String mcpApiUrl;
    private McpHttpGateway mcpHttpGateway;
    private McpPayloadBuilder mcpPayloadBuilder;

    /** * Caller: SingleModelCoordinator, MultiModelCoordinator, servicos de geracao e auditoria * Callee: UnifiedMcpModelGateway(String) * Objetivo: Inicializar o gateway com a URL padrao do MCP. * Data modificacao: 2026-05-24 00:00 * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public UnifiedMcpModelGateway() {
        this(DEFAULT_MCP_API_URL);
    }

    /** * Caller: SingleModelCoordinator, MultiModelCoordinator, servicos de geracao e auditoria * Callee: N/A * Objetivo: Inicializar o gateway com endpoint MCP explicito. * Feature: Mantem um unico canal de transporte para monomodelo e multimodelo. * Data modificacao: 2026-05-24 00:00 * * @param mcpApiUrl endpoint MCP * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public UnifiedMcpModelGateway(String mcpApiUrl) {
        this.mcpApiUrl = safeTrim(mcpApiUrl);
        this.mcpHttpGateway = new McpHttpGateway();
        this.mcpPayloadBuilder = new McpPayloadBuilder();
    }

    /** * Caller: SingleModelCoordinator, CodexCodeGenerationService, ClaudeCodeAuditService * Callee: McpPayloadBuilder.buildToolsCallPayload, McpHttpGateway.postJsonRpc * Objetivo: Executar uma chamada MCP para um modelo remoto especifico, * preservando o protocolo atual e alterando apenas o nome do modelo. * Feature: Registra em console os dados essenciais do envio para depuracao * operacional do circuito MCP. * Data modificacao: 2026-05-24 00:00 * * @param modelName nome do modelo remoto no intermediador MCP * @param prompt prompt final a ser enviado * @param apiKey chave de autenticacao MCP * @return corpo bruto da resposta HTTP * @throws Exception quando houver falha de serializacao ou transporte * * @author Renato Tomaz Nati * @since 2026-05-24 */
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
        System.out.println("[MCP GATEWAY DEBUG] URL=" + mcpApiUrl);
        System.out.println("[MCP GATEWAY DEBUG] modelName=" + modelName);
        System.out.println("[MCP GATEWAY DEBUG] apiKeyConfigured=" + (!isBlank(apiKey) ? "true" : "false"));
        System.out.println("[MCP GATEWAY DEBUG] promptLength=" + promptSeguro.length());
        System.out.println("[MCP GATEWAY DEBUG] payloadLength=" + payload.length());
        System.out.println("[MCP GATEWAY DEBUG] payload=");
        System.out.println(truncateForDebug(payload, 12000));

        String rawResponse = mcpHttpGateway.postJsonRpc(mcpApiUrl, apiKey, payload);

        System.out.println("[MCP GATEWAY DEBUG] rawResponseLength=" + (rawResponse != null ? rawResponse.length() : 0));
        System.out.println("[MCP GATEWAY DEBUG] rawResponse=");
        System.out.println(truncateForDebug(rawResponse, 12000));
        System.out.println("[MCP GATEWAY DEBUG] ==================================================");

        return rawResponse;
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
    /** * Caller: metodos internos * Callee: N/A * Objetivo: Normalizar texto sem aceitar nulo. * Data modificacao: 2026-05-24 00:00 * * @param value valor original * @return valor seguro * * @author Renato Tomaz Nati * @since 2026-05-24 */
    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    /** * Caller: metodos internos * Callee: N/A * Objetivo: Verificar se o valor textual esta vazio. * Data modificacao: 2026-05-24 00:00 * * @param value valor de entrada * @return true quando estiver em branco * * @author Renato Tomaz Nati * @since 2026-05-24 */
    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}