package com.mcp.sailibrary.plugin.mcp.adapters.connector;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Iterator;
import java.util.stream.Stream;

import com.mcp.sailibrary.plugin.mcp.core.McpAccessCredentials;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.RawModelResponse;
import com.mcp.sailibrary.plugin.mcp.ports.ModelConnector;

/* class_metadata: feature: "Conector HTTP para streaming SSE" libraries: java_http_client: "JDK padrao" objetivo: "Executar o POST de streaming com cookie opcional, sem api key no header, e com rastreabilidade detalhada" */

/** * Connector HTTP para endpoint streaming SSE. * * Regras operacionais: * - metodo POST * - autenticacao via cookie opcional * - nao envia X-Api-Key no streaming * - aceita stream e acumula o corpo cru para o codec * * Data de revisao: 2026-05-27 03:45 * * @author Renato Tomaz Nati * @since 2026-05-26 */
public class StreamingSseConnector implements ModelConnector {

    private final HttpClient httpClient;

    public StreamingSseConnector() {
        this(HttpClient.newHttpClient());
    }

    public StreamingSseConnector(HttpClient httpClient) {
        this.httpClient = httpClient != null ? httpClient : HttpClient.newHttpClient();
    }

    /** * Caller: * - ModelExecutionEngine * * Chama: * - isBlank * * Objetivo: * - Executar a chamada SSE real * - Registrar body de entrada, headers emitidos e body de saida * * Alterado em: 2026-05-27 03:45 */
    @Override
    public RawModelResponse execute(ModelExecutionProfile profile, String requestBody, McpAccessCredentials credentials) throws Exception {

        if (profile == null) {
            throw new IllegalArgumentException("Erro Operacional: profile nao pode ser nulo.");
        }

        if (isBlank(profile.getEndpointUrl())) {
            throw new IllegalArgumentException("Erro Operacional: endpointUrl streaming nao pode ser vazio.");
        }

        String safeRequestBody = requestBody != null ? requestBody : "";
        McpAccessCredentials safeCredentials = credentials != null ? credentials : new McpAccessCredentials();

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(profile.getEndpointUrl()))
                .version(HttpClient.Version.HTTP_1_1)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36")
                .POST(HttpRequest.BodyPublishers.ofString(safeRequestBody));

        String cookieHeaderValue = "";
        if (safeCredentials.hasCookieValue()) {
            cookieHeaderValue = ".AspNetCore.Cookies=" + safeCredentials.getCookieValue();
            requestBuilder.header("Cookie", cookieHeaderValue);
        }

        HttpRequest request = requestBuilder.build();

        System.out.println("[STREAMING SSE CONNECTOR] =========================================");
        System.out.println("[STREAMING SSE CONNECTOR] endpoint=" + profile.getEndpointUrl());
        System.out.println("[STREAMING SSE CONNECTOR] accept=text/event-stream");
        System.out.println("[STREAMING SSE CONNECTOR] contentType=application/json");
        System.out.println("[STREAMING SSE CONNECTOR] userAgent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36");
        System.out.println("[STREAMING SSE CONNECTOR] cookieConfigured=" + (!isBlank(cookieHeaderValue) ? "true" : "false"));
        System.out.println("[STREAMING SSE CONNECTOR] cookieHeader=" + cookieHeaderValue);
        System.out.println("[STREAMING SSE CONNECTOR] requestBodyLength=" + safeRequestBody.length());
        System.out.println("[STREAMING SSE CONNECTOR] requestBody=");
        System.out.println(truncateForDebug(safeRequestBody, 20000));

        HttpResponse<Stream<String>> response = httpClient.send(request, HttpResponse.BodyHandlers.ofLines());

        StringBuilder rawSseBuilder = new StringBuilder(8192);
        Stream<String> bodyLines = response.body();
        try {
            Iterator<String> lineIterator = bodyLines.iterator();
            while (lineIterator.hasNext()) {
                String currentLine = lineIterator.next();
                rawSseBuilder.append(currentLine).append('\n');
                System.out.println("[STREAMING SSE CONNECTOR] line=" + currentLine);
            }
        } finally {
            if (bodyLines != null) {
                bodyLines.close();
            }
        }

        String contentType = response.headers().firstValue("Content-Type").orElse("");
        String rawSseBody = rawSseBuilder.toString();

        System.out.println("[STREAMING SSE CONNECTOR] statusCode=" + response.statusCode());
        System.out.println("[STREAMING SSE CONNECTOR] responseContentType=" + contentType);
        System.out.println("[STREAMING SSE CONNECTOR] responseBodyLength=" + rawSseBody.length());
        System.out.println("[STREAMING SSE CONNECTOR] responseBody=");
        System.out.println(truncateForDebug(rawSseBody, 30000));
        System.out.println("[STREAMING SSE CONNECTOR] =========================================");

        return new RawModelResponse(rawSseBody, response.statusCode(), contentType);
    }

    /** * Caller: * - execute * * Objetivo: * - Teste simples de branco * * Alterado em: 2026-05-27 03:45 */
    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    /** * Caller: * - execute * * Objetivo: * - Limitar o volume do log sem perder rastreabilidade util * * Alterado em: 2026-05-27 03:45 */
    private String truncateForDebug(String value, int maxLength) {
        if (value == null) {
            return "null";
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength) + "... [TRUNCATED]";
    }
}