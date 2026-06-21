package com.mcp.sailibrary.plugin.mcp.application;

import com.mcp.sailibrary.plugin.mcp.core.RequestFormatKind;
import com.mcp.sailibrary.plugin.mcp.core.ResponseFormatKind;
import com.mcp.sailibrary.plugin.mcp.core.TransportKind;
import com.mcp.sailibrary.plugin.mcp.ports.ModelConnector;
import com.mcp.sailibrary.plugin.mcp.ports.ModelExecutionProfileResolver;
import com.mcp.sailibrary.plugin.mcp.ports.ModelRequestCodec;
import com.mcp.sailibrary.plugin.mcp.ports.ModelResponseCodec;
import com.mcp.sailibrary.plugin.mcp.ports.ToolPromptSectionsPort;

/** * Classe central de bootstrapping da arquitetura hexagonal MCP. * * <p>O objetivo desta classe e concentrar o registro de: * connectors, request codecs e response codecs em um unico lugar. * Assim, o restante do sistema nao precisa conhecer detalhes de wiring.</p> * * <p>Regra pratica: * - registre aqui uma vez * - reutilize o engine e o support no resto do sistema</p> * * @author Renato Tomaz Nati * @since 2026-05-26 */
public class McpExecutionWiring {

    private final ModelExecutionEngine modelExecutionEngine;
    private final ModelExecutionProfileResolver modelExecutionProfileResolver;
    private final ToolPromptSectionsPort toolPromptSectionsPort;

    public McpExecutionWiring(ModelExecutionEngine modelExecutionEngine, ModelExecutionProfileResolver modelExecutionProfileResolver, ToolPromptSectionsPort toolPromptSectionsPort) {
        if (modelExecutionEngine == null) {
            throw new IllegalArgumentException("Erro Operacional: modelExecutionEngine nao pode ser nulo.");
        }

        if (modelExecutionProfileResolver == null) {
            throw new IllegalArgumentException("Erro Operacional: modelExecutionProfileResolver nao pode ser nulo.");
        }

        if (toolPromptSectionsPort == null) {
            throw new IllegalArgumentException("Erro Operacional: toolPromptSectionsPort nao pode ser nulo.");
        }

        this.modelExecutionEngine = modelExecutionEngine;
        this.modelExecutionProfileResolver = modelExecutionProfileResolver;
        this.toolPromptSectionsPort = toolPromptSectionsPort;
    }

    /** * Monta o engine com os registries legados preservados. * * @param legacyJsonRpcConnector connector do legado JSON-RPC * @param streamingSseConnector connector do streaming SSE * @param legacyMcpRequestCodec codec de request legado * @param streamingPromptRequestCodec codec de request streaming via prompt * @param rawJsonStreamingRequestCodec codec de request streaming via raw json * @param legacyMcpResponseCodec codec de response legado * @param streamingSseResponseCodec codec de response streaming SSE * @param plainTextResponseCodec codec de response texto puro * @return engine pronto para uso */
    public static ModelExecutionEngine buildEngine(ModelConnector legacyJsonRpcConnector,
            ModelConnector streamingSseConnector,
            ModelRequestCodec legacyMcpRequestCodec,
            ModelRequestCodec streamingPromptRequestCodec,
            ModelRequestCodec rawJsonStreamingRequestCodec,
            ModelResponseCodec legacyMcpResponseCodec,
            ModelResponseCodec streamingSseResponseCodec,
            ModelResponseCodec plainTextResponseCodec) {

        return buildEngine(
                legacyJsonRpcConnector,
                streamingSseConnector,
                streamingSseConnector,
                legacyMcpRequestCodec,
                streamingPromptRequestCodec,
                rawJsonStreamingRequestCodec,
                rawJsonStreamingRequestCodec,
                legacyMcpResponseCodec,
                streamingSseResponseCodec,
                plainTextResponseCodec,
                plainTextResponseCodec
        );
    }

    /** * Monta o engine com todos os registries de connector e codecs. * * <p>Este e o ponto correto para chamar: * registerConnector, registerRequestCodec e registerResponseCodec.</p> * * @param legacyJsonRpcConnector connector do legado JSON-RPC * @param streamingSseConnector connector do streaming SSE * @param saiChatExecuteConnector connector da SAI chatexecute * @param legacyMcpRequestCodec codec de request legado * @param streamingPromptRequestCodec codec de request streaming via prompt * @param rawJsonStreamingRequestCodec codec de request streaming via raw json * @param saiChatExecuteRequestCodec codec de request SAI chatexecute * @param legacyMcpResponseCodec codec de response legado * @param streamingSseResponseCodec codec de response streaming SSE * @param plainTextResponseCodec codec de response texto puro * @param saiChatExecuteResponseCodec codec de response SAI chatexecute * @return engine pronto para uso */
    public static ModelExecutionEngine buildEngine(ModelConnector legacyJsonRpcConnector,
            ModelConnector streamingSseConnector,
            ModelConnector saiChatExecuteConnector,
            ModelRequestCodec legacyMcpRequestCodec,
            ModelRequestCodec streamingPromptRequestCodec,
            ModelRequestCodec rawJsonStreamingRequestCodec,
            ModelRequestCodec saiChatExecuteRequestCodec,
            ModelResponseCodec legacyMcpResponseCodec,
            ModelResponseCodec streamingSseResponseCodec,
            ModelResponseCodec plainTextResponseCodec,
            ModelResponseCodec saiChatExecuteResponseCodec) {

        if (legacyJsonRpcConnector == null) {
            throw new IllegalArgumentException("Erro Operacional: legacyJsonRpcConnector nao pode ser nulo.");
        }

        if (streamingSseConnector == null) {
            throw new IllegalArgumentException("Erro Operacional: streamingSseConnector nao pode ser nulo.");
        }

        if (saiChatExecuteConnector == null) {
            throw new IllegalArgumentException("Erro Operacional: saiChatExecuteConnector nao pode ser nulo.");
        }

        if (legacyMcpRequestCodec == null) {
            throw new IllegalArgumentException("Erro Operacional: legacyMcpRequestCodec nao pode ser nulo.");
        }

        if (streamingPromptRequestCodec == null) {
            throw new IllegalArgumentException("Erro Operacional: streamingPromptRequestCodec nao pode ser nulo.");
        }

        if (rawJsonStreamingRequestCodec == null) {
            throw new IllegalArgumentException("Erro Operacional: rawJsonStreamingRequestCodec nao pode ser nulo.");
        }

        if (saiChatExecuteRequestCodec == null) {
            throw new IllegalArgumentException("Erro Operacional: saiChatExecuteRequestCodec nao pode ser nulo.");
        }

        if (legacyMcpResponseCodec == null) {
            throw new IllegalArgumentException("Erro Operacional: legacyMcpResponseCodec nao pode ser nulo.");
        }

        if (streamingSseResponseCodec == null) {
            throw new IllegalArgumentException("Erro Operacional: streamingSseResponseCodec nao pode ser nulo.");
        }

        if (plainTextResponseCodec == null) {
            throw new IllegalArgumentException("Erro Operacional: plainTextResponseCodec nao pode ser nulo.");
        }

        if (saiChatExecuteResponseCodec == null) {
            throw new IllegalArgumentException("Erro Operacional: saiChatExecuteResponseCodec nao pode ser nulo.");
        }

        ModelExecutionEngine engine = new ModelExecutionEngine();

        engine.registerConnector(TransportKind.LEGACY_JSON_RPC_HTTP, legacyJsonRpcConnector);
        engine.registerConnector(TransportKind.STREAMING_SSE_HTTP, streamingSseConnector);
        engine.registerConnector(TransportKind.SAI_CHAT_EXECUTE_HTTP, saiChatExecuteConnector);

        engine.registerRequestCodec(RequestFormatKind.LEGACY_MCP_TOOLS_CALL, legacyMcpRequestCodec);
        engine.registerRequestCodec(RequestFormatKind.STREAMING_PROMPT, streamingPromptRequestCodec);
        engine.registerRequestCodec(RequestFormatKind.STREAMING_RAW_JSON, rawJsonStreamingRequestCodec);
        engine.registerRequestCodec(RequestFormatKind.SAI_CHAT_EXECUTE, saiChatExecuteRequestCodec);

        engine.registerResponseCodec(ResponseFormatKind.LEGACY_MCP_ENVELOPE, legacyMcpResponseCodec);
        engine.registerResponseCodec(ResponseFormatKind.STREAMING_SSE_EVENTS, streamingSseResponseCodec);
        engine.registerResponseCodec(ResponseFormatKind.PLAIN_TEXT, plainTextResponseCodec);
        engine.registerResponseCodec(ResponseFormatKind.SAI_CHAT_EXECUTE_JSON, saiChatExecuteResponseCodec);

        System.out.println("[MCP WIRING DEBUG] Connectors registrados: LEGACY_JSON_RPC_HTTP, STREAMING_SSE_HTTP, SAI_CHAT_EXECUTE_HTTP");
        System.out.println("[MCP WIRING DEBUG] Request codecs registrados: LEGACY_MCP_TOOLS_CALL, STREAMING_PROMPT, STREAMING_RAW_JSON, SAI_CHAT_EXECUTE");
        System.out.println("[MCP WIRING DEBUG] Response codecs registrados: LEGACY_MCP_ENVELOPE, STREAMING_SSE_EVENTS, PLAIN_TEXT, SAI_CHAT_EXECUTE_JSON");

        return engine;
    }

    /** * Cria um support simplificado para execucao por canal. * * @return support pronto para uso */
    public McpExecutionSupport createExecutionSupport() {
        return new McpExecutionSupport(modelExecutionEngine, modelExecutionProfileResolver);
    }

    public ModelExecutionEngine getModelExecutionEngine() {
        return modelExecutionEngine;
    }

    public ModelExecutionProfileResolver getModelExecutionProfileResolver() {
        return modelExecutionProfileResolver;
    }

    public ToolPromptSectionsPort getToolPromptSectionsPort() {
        return toolPromptSectionsPort;
    }
}
