package com.mcp.sailibrary.plugin.mcp.application;

import java.util.EnumMap;
import java.util.Map;

import com.mcp.sailibrary.plugin.mcp.core.McpAccessCredentials;
import com.mcp.sailibrary.plugin.mcp.core.ModelChannel;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionRequest;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionResponse;
import com.mcp.sailibrary.plugin.mcp.core.RawModelResponse;
import com.mcp.sailibrary.plugin.mcp.core.RequestFormatKind;
import com.mcp.sailibrary.plugin.mcp.core.ResponseFormatKind;
import com.mcp.sailibrary.plugin.mcp.core.TransportKind;
import com.mcp.sailibrary.plugin.mcp.ports.ModelConnector;
import com.mcp.sailibrary.plugin.mcp.ports.ModelExecutionProfileResolver;
import com.mcp.sailibrary.plugin.mcp.ports.ModelRequestCodec;
import com.mcp.sailibrary.plugin.mcp.ports.ModelResponseCodec;

/** * Motor central de execucao do hexagono MCP. * * <p>Esta classe separa claramente: * connector = como conecta * request codec = como monta o JSON * response codec = como interpreta a resposta</p> * * <p>Com isso, trocar forma de conexao, request ou response deixa de exigir * alteracoes espalhadas pelo sistema.</p> * * @author Renato Tomaz Nati * @since 2026-05-26 */
public class ModelExecutionEngine {

    private final Map<TransportKind, ModelConnector> connectors;
    private final Map<RequestFormatKind, ModelRequestCodec> requestCodecs;
    private final Map<ResponseFormatKind, ModelResponseCodec> responseCodecs;

    public ModelExecutionEngine() {
        this.connectors = new EnumMap<TransportKind, ModelConnector>(TransportKind.class);
        this.requestCodecs = new EnumMap<RequestFormatKind, ModelRequestCodec>(RequestFormatKind.class);
        this.responseCodecs = new EnumMap<ResponseFormatKind, ModelResponseCodec>(ResponseFormatKind.class);
    }

    public ModelExecutionEngine registerConnector(TransportKind transportKind, ModelConnector connector) {
        if (transportKind == null) {
            throw new IllegalArgumentException("Erro Operacional: transportKind nao pode ser nulo.");
        }

        if (connector == null) {
            throw new IllegalArgumentException("Erro Operacional: connector nao pode ser nulo.");
        }

        connectors.put(transportKind, connector);
        return this;
    }

    public ModelExecutionEngine registerRequestCodec(RequestFormatKind requestFormatKind, ModelRequestCodec codec) {
        if (requestFormatKind == null) {
            throw new IllegalArgumentException("Erro Operacional: requestFormatKind nao pode ser nulo.");
        }

        if (codec == null) {
            throw new IllegalArgumentException("Erro Operacional: request codec nao pode ser nulo.");
        }

        requestCodecs.put(requestFormatKind, codec);
        return this;
    }

    public ModelExecutionEngine registerResponseCodec(ResponseFormatKind responseFormatKind, ModelResponseCodec codec) {
        if (responseFormatKind == null) {
            throw new IllegalArgumentException("Erro Operacional: responseFormatKind nao pode ser nulo.");
        }

        if (codec == null) {
            throw new IllegalArgumentException("Erro Operacional: response codec nao pode ser nulo.");
        }

        responseCodecs.put(responseFormatKind, codec);
        return this;
    }

    /* * Feature: executa a chamada MCP com profile ja resolvido. * Data: 2026-05-27 10:05 * Chamado por: * - McpExecutionSupport.executePrompt * - McpExecutionSupport.executeRawJson * - ModelExecutionEngine.execute(ModelExecutionRequest, ModelExecutionProfileResolver) * * Chama: * - validateRequest * - validateProfile * - resolveConnector * - resolveRequestCodec * - resolveResponseCodec * * Objetivo: * - Executar exatamente uma chamada ao connector * - Registrar rastreabilidade completa de entrada e saida * - Manter correlacao por requestIdentity para diagnostico */
    /** * Feature: executa o ciclo completo do hexagono MCP. * Data: 2026-05-27 10:20 * Quem chama: * - McpExecutionSupport.executePrompt * - McpExecutionSupport.executeRawJson * Quem eh chamado: * - validateRequest * - validateProfile * - resolveConnector * - resolveRequestCodec * - resolveResponseCodec * - connector.execute * - responseCodec.decode * Objetivo: * - montar request body * - consolidar credenciais efetivas * - enviar ao connector correto * - decodificar a resposta */
    public ModelExecutionResponse execute(ModelExecutionRequest request, ModelExecutionProfile profile) throws Exception {
        validateRequest(request);
        validateProfile(profile);

        ModelConnector connector = resolveConnector(profile.getTransportKind());
        ModelRequestCodec requestCodec = resolveRequestCodec(profile.getRequestFormatKind());
        ModelResponseCodec responseCodec = resolveResponseCodec(profile.getResponseFormatKind());

        String executionId = "engine-profile-" + Long.toHexString(System.nanoTime());
        String requestIdentity = Integer.toHexString(System.identityHashCode(request));

        McpAccessCredentials requestCredentials = request.getCredentials() != null
                ? request.getCredentials()
                : new McpAccessCredentials();

        McpAccessCredentials effectiveCredentials = new McpAccessCredentials();

        if (requestCredentials.hasApiKey()) {
            effectiveCredentials.setApiKey(requestCredentials.getApiKey());
        } else if (profile.getDefaultApiKey() != null && profile.getDefaultApiKey().trim().length() > 0) {
            effectiveCredentials.setApiKey(profile.getDefaultApiKey());
        }

        if (requestCredentials.hasCookieValue()) {
            effectiveCredentials.setCookieValue(requestCredentials.getCookieValue());
        } else if (profile.getDefaultCookieValue() != null && profile.getDefaultCookieValue().trim().length() > 0) {
            effectiveCredentials.setCookieValue(profile.getDefaultCookieValue());
        }

        System.out.println("[MCP ENGINE DEBUG] --------------------------------------------------");
        System.out.println("[MCP ENGINE DEBUG] phase=RESOLVE_PROFILE");
        System.out.println("[MCP ENGINE DEBUG] executionId=" + executionId);
        System.out.println("[MCP ENGINE DEBUG] requestIdentity=" + requestIdentity);
        System.out.println("[MCP ENGINE DEBUG] channel=" + safeChannelName(request.getChannel()));
        System.out.println("[MCP ENGINE DEBUG] requestHasApiKey=" + requestCredentials.hasApiKey());
        System.out.println("[MCP ENGINE DEBUG] requestHasCookieValue=" + requestCredentials.hasCookieValue());

        System.out.println("[MCP ENGINE DEBUG] ==================================================");
        System.out.println("[MCP ENGINE DEBUG] phase=BEFORE_ENCODE");
        System.out.println("[MCP ENGINE DEBUG] executionId=" + executionId);
        System.out.println("[MCP ENGINE DEBUG] requestIdentity=" + requestIdentity);
        System.out.println("[MCP ENGINE DEBUG] channel=" + safeChannelName(request.getChannel()));
        System.out.println("[MCP ENGINE DEBUG] transportKind=" + profile.getTransportKind());
        System.out.println("[MCP ENGINE DEBUG] requestFormatKind=" + profile.getRequestFormatKind());
        System.out.println("[MCP ENGINE DEBUG] responseFormatKind=" + profile.getResponseFormatKind());
        System.out.println("[MCP ENGINE DEBUG] endpointUrl=" + profile.getEndpointUrl());
        System.out.println("[MCP ENGINE DEBUG] effectiveModelName=" + profile.resolveEffectiveModelName());
        System.out.println("[MCP ENGINE DEBUG] conversationId=" + profile.getConversationId());
        System.out.println("[MCP ENGINE DEBUG] workspaceId=" + profile.getWorkspaceId());
        System.out.println("[MCP ENGINE DEBUG] requestHasApiKey=" + requestCredentials.hasApiKey());
        System.out.println("[MCP ENGINE DEBUG] requestHasCookieValue=" + requestCredentials.hasCookieValue());
        System.out.println("[MCP ENGINE DEBUG] profileDefaultApiKeyConfigured="
                + (profile.getDefaultApiKey() != null && profile.getDefaultApiKey().trim().length() > 0));
        System.out.println("[MCP ENGINE DEBUG] profileDefaultCookieConfigured="
                + (profile.getDefaultCookieValue() != null && profile.getDefaultCookieValue().trim().length() > 0));
        System.out.println("[MCP ENGINE DEBUG] effectiveHasApiKey=" + effectiveCredentials.hasApiKey());
        System.out.println("[MCP ENGINE DEBUG] effectiveHasCookieValue=" + effectiveCredentials.hasCookieValue());
        System.out.println("[MCP ENGINE DEBUG] connectorClass=" + connector.getClass().getName());
        System.out.println("[MCP ENGINE DEBUG] requestCodecClass=" + requestCodec.getClass().getName());
        System.out.println("[MCP ENGINE DEBUG] responseCodecClass=" + responseCodec.getClass().getName());

        String requestBody = requestCodec.encode(request, profile);

        System.out.println("[MCP ENGINE DEBUG] phase=AFTER_ENCODE");
        System.out.println("[MCP ENGINE DEBUG] executionId=" + executionId);
        System.out.println("[MCP ENGINE DEBUG] requestIdentity=" + requestIdentity);
        System.out.println("[MCP ENGINE DEBUG] requestBodyLength=" + (requestBody != null ? requestBody.length() : 0));
        System.out.println("[MCP ENGINE DEBUG] requestBody=");
        System.out.println(requestBody);

        RawModelResponse rawResponse = connector.execute(profile, requestBody, effectiveCredentials);

        System.out.println("[MCP ENGINE DEBUG] phase=AFTER_CONNECTOR");
        System.out.println("[MCP ENGINE DEBUG] executionId=" + executionId);
        System.out.println("[MCP ENGINE DEBUG] requestIdentity=" + requestIdentity);
        System.out.println("[MCP ENGINE DEBUG] rawStatusCode=" + (rawResponse != null ? rawResponse.getStatusCode() : 0));
        System.out.println("[MCP ENGINE DEBUG] rawContentType=" + (rawResponse != null ? rawResponse.getContentType() : ""));
       // System.out.println("[MCP ENGINE DEBUG] rawBodyLength="
        //        + ((rawResponse != null && rawResponse.getRawBody() != null) ? rawResponse.getRawBody().length() : 0));
        System.out.println("[MCP ENGINE DEBUG] rawBodyPreview=");
        System.out.println(truncateForDebug(rawResponse != null ? rawResponse.getRawBody() : "", 12000));

        ModelExecutionResponse executionResponse = null;

        try {
            System.out.println("[MCP ENGINE DEBUG] phase=BEFORE_DECODE");
            System.out.println("[MCP ENGINE DEBUG] executionId=" + executionId);
            System.out.println("[MCP ENGINE DEBUG] requestIdentity=" + requestIdentity);
            System.out.println("[MCP ENGINE DEBUG] responseCodecClass=" + responseCodec.getClass().getName());
            System.out.println("[MCP ENGINE DEBUG] rawBodyLengthBeforeDecode="
                    + ((rawResponse != null && rawResponse.getRawBody() != null) ? rawResponse.getRawBody().length() : 0));

            executionResponse = responseCodec.decode(rawResponse, profile);
        } catch (Throwable throwable) {
            System.out.println("[MCP ENGINE ERROR] Falha critica durante responseCodec.decode.");
            System.out.println("[MCP ENGINE ERROR] executionId=" + executionId);
            System.out.println("[MCP ENGINE ERROR] requestIdentity=" + requestIdentity);
            System.out.println("[MCP ENGINE ERROR] responseCodecClass=" + responseCodec.getClass().getName());
            System.out.println("[MCP ENGINE ERROR] throwableClass=" + throwable.getClass().getName());
            System.out.println("[MCP ENGINE ERROR] throwableMessage=" + throwable.getMessage());
            printCompactStackTrace(throwable, 80);
            throw throwable;
        }

        System.out.println("[MCP ENGINE DEBUG] phase=AFTER_DECODE");
        System.out.println("[MCP ENGINE DEBUG] executionId=" + executionId);
        System.out.println("[MCP ENGINE DEBUG] requestIdentity=" + requestIdentity);
        System.out.println("[MCP ENGINE DEBUG] primaryTextLength="
                + ((executionResponse != null && executionResponse.getPrimaryText() != null)
                        ? executionResponse.getPrimaryText().length()
                        : 0));
        System.out.println("[MCP ENGINE DEBUG] primaryText=");
        System.out.println(executionResponse != null ? executionResponse.getPrimaryText() : "");
        System.out.println("[MCP ENGINE DEBUG] ==================================================");

        return executionResponse;
    }
    private void printCompactStackTrace(Throwable throwable, int maxFrames) {
        if (throwable == null) {
            return;
        }

        StackTraceElement[] stackTrace = throwable.getStackTrace();
        int printed = 0;

        System.out.println("[MCP ENGINE ERROR] stackTraceCompactStart");

        for (int i = 0; i < stackTrace.length && printed < maxFrames; i++) {
            StackTraceElement frame = stackTrace[i];
            String className = frame.getClassName();

            if (className != null && className.startsWith("java.util.regex.")) {
                continue;
            }

            System.out.println("[MCP ENGINE ERROR] at " + frame.toString());
            printed++;
        }

        if (throwable.getCause() != null && throwable.getCause() != throwable) {
            System.out.println("[MCP ENGINE ERROR] causedBy=" + throwable.getCause().getClass().getName()
                    + ": " + throwable.getCause().getMessage());
            printCompactStackTrace(throwable.getCause(), maxFrames);
        }

        System.out.println("[MCP ENGINE ERROR] stackTraceCompactEnd");
    }
    private ModelExecutionRequest createRequestForExecution(ModelExecutionRequest originalRequest, ModelExecutionProfile profile) {
        ModelExecutionRequest requestForExecution = new ModelExecutionRequest();
        requestForExecution.setChannel(originalRequest.getChannel());
        requestForExecution.setPrompt(originalRequest.getPrompt());
        requestForExecution.setRawJsonBody(originalRequest.getRawJsonBody());
        requestForExecution.setCredentials(resolveEffectiveCredentials(profile, originalRequest.getCredentials()));
        return requestForExecution;
    }
    private McpAccessCredentials resolveEffectiveCredentials(ModelExecutionProfile profile, McpAccessCredentials requestCredentials) {
        McpAccessCredentials credentialsFromRequest = requestCredentials != null
                ? requestCredentials
                : new McpAccessCredentials();

        McpAccessCredentials effectiveCredentials = new McpAccessCredentials();

        if (credentialsFromRequest.hasApiKey()) {
            effectiveCredentials.setApiKey(credentialsFromRequest.getApiKey());
        } else if (!isBlank(profile.getDefaultApiKey())) {
            effectiveCredentials.setApiKey(profile.getDefaultApiKey());
        }

        if (credentialsFromRequest.hasCookieValue()) {
            effectiveCredentials.setCookieValue(credentialsFromRequest.getCookieValue());
        } else if (!isBlank(profile.getDefaultCookieValue())) {
            effectiveCredentials.setCookieValue(profile.getDefaultCookieValue());
        }

        return effectiveCredentials;
    }
    /* * Feature: executa a chamada MCP resolvendo o profile a partir do canal. * Data: 2026-05-27 10:05 * Chamado por: * - McpExecutionSupport.executePrompt * - McpExecutionSupport.executeRawJson * * Chama: * - validateRequest * - ModelExecutionProfileResolver.resolve * - execute(ModelExecutionRequest, ModelExecutionProfile) * * Objetivo: * - Registrar a fase de resolucao do profile * - Preservar um unico fluxo por request * - Facilitar a identificacao de duplicidades no caller */
    public ModelExecutionResponse execute(ModelExecutionRequest request, ModelExecutionProfileResolver profileResolver) throws Exception {
        if (profileResolver == null) {
            throw new IllegalArgumentException("Erro Operacional: profileResolver nao pode ser nulo.");
        }

        validateRequest(request);

        ModelChannel channel = request.getChannel();
        if (channel == null) {
            throw new IllegalArgumentException("Erro Operacional: request.channel nao pode ser nulo.");
        }

        String executionId = "engine-resolver-" + Long.toHexString(System.nanoTime());
        String requestIdentity = Integer.toHexString(System.identityHashCode(request));

        System.out.println("[MCP ENGINE DEBUG] --------------------------------------------------");
        System.out.println("[MCP ENGINE DEBUG] phase=RESOLVE_PROFILE");
        System.out.println("[MCP ENGINE DEBUG] executionId=" + executionId);
        System.out.println("[MCP ENGINE DEBUG] requestIdentity=" + requestIdentity);
        System.out.println("[MCP ENGINE DEBUG] channel=" + safeChannelName(channel));
        System.out.println("[MCP ENGINE DEBUG] requestHasApiKey="
                + (request.getCredentials() != null && request.getCredentials().hasApiKey()));
        System.out.println("[MCP ENGINE DEBUG] requestHasCookieValue="
                + (request.getCredentials() != null && request.getCredentials().hasCookieValue()));

        ModelExecutionProfile profile = profileResolver.resolve(channel);

        System.out.println("[MCP ENGINE DEBUG] phase=PROFILE_RESOLVED");
        System.out.println("[MCP ENGINE DEBUG] executionId=" + executionId);
        System.out.println("[MCP ENGINE DEBUG] requestIdentity=" + requestIdentity);
        System.out.println("[MCP ENGINE DEBUG] resolvedTransportKind=" + profile.getTransportKind());
        System.out.println("[MCP ENGINE DEBUG] resolvedRequestFormatKind=" + profile.getRequestFormatKind());
        System.out.println("[MCP ENGINE DEBUG] resolvedResponseFormatKind=" + profile.getResponseFormatKind());
        System.out.println("[MCP ENGINE DEBUG] resolvedEndpointUrl=" + profile.getEndpointUrl());
        System.out.println("[MCP ENGINE DEBUG] resolvedConversationId=" + profile.getConversationId());
        System.out.println("[MCP ENGINE DEBUG] resolvedWorkspaceId=" + profile.getWorkspaceId());
        System.out.println("[MCP ENGINE DEBUG] --------------------------------------------------");

        return execute(request, profile);
    }

    public boolean hasConnector(TransportKind transportKind) {
        return connectors.containsKey(transportKind);
    }

    public boolean hasRequestCodec(RequestFormatKind requestFormatKind) {
        return requestCodecs.containsKey(requestFormatKind);
    }

    public boolean hasResponseCodec(ResponseFormatKind responseFormatKind) {
        return responseCodecs.containsKey(responseFormatKind);
    }

    private ModelConnector resolveConnector(TransportKind transportKind) {
        ModelConnector connector = connectors.get(transportKind);

        if (connector == null) {
            throw new IllegalStateException(
                    "Erro Operacional: Nenhum connector registrado para transportKind=" + transportKind);
        }

        return connector;
    }

    private ModelRequestCodec resolveRequestCodec(RequestFormatKind requestFormatKind) {
        ModelRequestCodec codec = requestCodecs.get(requestFormatKind);

        if (codec == null) {
            throw new IllegalStateException(
                    "Erro Operacional: Nenhum request codec registrado para requestFormatKind=" + requestFormatKind);
        }

        return codec;
    }

    private ModelResponseCodec resolveResponseCodec(ResponseFormatKind responseFormatKind) {
        ModelResponseCodec codec = responseCodecs.get(responseFormatKind);

        if (codec == null) {
            throw new IllegalStateException(
                    "Erro Operacional: Nenhum response codec registrado para responseFormatKind=" + responseFormatKind);
        }

        return codec;
    }

    private void validateRequest(ModelExecutionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Erro Operacional: request nao pode ser nulo.");
        }

        if (request.getChannel() == null) {
            throw new IllegalArgumentException("Erro Operacional: request.channel nao pode ser nulo.");
        }
    }

    private void validateProfile(ModelExecutionProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("Erro Operacional: profile nao pode ser nulo.");
        }

        if (profile.getTransportKind() == null) {
            throw new IllegalArgumentException("Erro Operacional: profile.transportKind nao pode ser nulo.");
        }

        if (profile.getRequestFormatKind() == null) {
            throw new IllegalArgumentException("Erro Operacional: profile.requestFormatKind nao pode ser nulo.");
        }

        if (profile.getResponseFormatKind() == null) {
            throw new IllegalArgumentException("Erro Operacional: profile.responseFormatKind nao pode ser nulo.");
        }
    }

    private String safeChannelName(ModelChannel channel) {
        return channel != null ? channel.name() : "null";
    }
    private boolean hasApiKey(McpAccessCredentials credentials) {
        return credentials != null && credentials.hasApiKey();
    }

    private boolean hasCookieValue(McpAccessCredentials credentials) {
        return credentials != null && credentials.hasCookieValue();
    }

    private String safeString(String value) {
        return value != null ? value : "";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private String truncateForDebug(String value, int maxLength) {
        if (value == null) {
            return "";
        }

        if (maxLength <= 0 || value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength) + "...";
    }
}