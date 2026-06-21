package com.mcp.sailibrary.plugin.mcp;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/** * Gateway HTTP utilitario da camada MCP. * * <p>Esta classe deixa de ser apenas um "POST JSON-RPC legado" e passa a * oferecer operacoes HTTP reutilizaveis para: * - legado JSON-RPC * - POST JSON generico * - endpoint streaming SSE</p> * * <p>Compatibilidade preservada: * o metodo postJsonRpc(String, String, String) continua existindo.</p> * * @author Renato Tomaz Nati * @since 2026-05-26 */
public class McpHttpGateway {

    private final HttpClient httpClient;

    public McpHttpGateway() {
        this(HttpClient.newHttpClient());
    }

    public McpHttpGateway(HttpClient httpClient) {
        this.httpClient = httpClient != null ? httpClient : HttpClient.newHttpClient();
    }

    /** * Compatibilidade com o fluxo legado. * * @param apiUrl endpoint MCP legado * @param apiKey chave de autenticacao * @param jsonPayload payload JSON-RPC * @return corpo bruto da resposta * @throws Exception em caso de falha de rede ou transporte */
    public String postJsonRpc(String apiUrl, String apiKey, String jsonPayload) throws Exception {
        HttpGatewayResponse response = postJson(
                apiUrl,
                jsonPayload,
                apiKey,
                null,
                false,
                null
        );
        return response.getBody();
    }

    /** * Executa POST JSON generico. * * @param url endpoint * @param jsonPayload corpo JSON * @param headers headers adicionais * @return resposta detalhada * @throws Exception em caso de falha */
    public HttpGatewayResponse postJson(String url, String jsonPayload, Map<String, String> headers) throws Exception {
        return postJson(url, jsonPayload, null, null, false, headers);
    }

    /** * Executa POST JSON com suporte a apiKey, cookie e opcionalmente * Accept=text/event-stream. * * @param url endpoint * @param jsonPayload corpo JSON * @param apiKey api key opcional * @param cookieValue cookie opcional * @param acceptEventStream true quando a chamada espera SSE * @param extraHeaders headers adicionais * @return resposta detalhada * @throws Exception em caso de falha */
    public HttpGatewayResponse postJson(String url, String jsonPayload, String apiKey, String cookieValue, boolean acceptEventStream, Map<String, String> extraHeaders) throws Exception {

        if (isBlank(url)) {
            throw new IllegalArgumentException("Erro Operacional: URL nao pode ser vazia.");
        }

        String safePayload = jsonPayload != null ? jsonPayload : "";

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(safePayload));

        if (acceptEventStream) {
            builder.header("Accept", "text/event-stream");
            builder.header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36");
        }

        if (!isBlank(apiKey)) {
            builder.header("X-Api-Key", apiKey.trim());
        }

        if (!isBlank(cookieValue)) {
            builder.header("Cookie", ".AspNetCore.Cookies=" + cookieValue.trim());
        }

        if (extraHeaders != null && !extraHeaders.isEmpty()) {
            for (Map.Entry<String, String> entry : extraHeaders.entrySet()) {
                if (entry != null && !isBlank(entry.getKey()) && entry.getValue() != null) {
                    builder.header(entry.getKey(), entry.getValue());
                }
            }
        }

        HttpRequest request = builder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        String contentType = response.headers().firstValue("Content-Type").orElse("");

        return new HttpGatewayResponse(
                response.statusCode(),
                contentType,
                response.body()
        );
    }

    /** * Executa POST para endpoint streaming SSE. * * @param url endpoint streaming * @param jsonPayload corpo JSON * @param cookieValue cookie opcional * @param apiKey apiKey opcional * @return resposta detalhada * @throws Exception em caso de falha */
    public HttpGatewayResponse postEventStream(String url, String jsonPayload, String cookieValue, String apiKey) throws Exception {
        return postJson(url, jsonPayload, apiKey, cookieValue, true, null);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    /** * Resposta HTTP utilitaria do gateway. */
    public static class HttpGatewayResponse {

        private final int statusCode;
        private final String contentType;
        private final String body;

        public HttpGatewayResponse(int statusCode, String contentType, String body) {
            this.statusCode = statusCode;
            this.contentType = contentType != null ? contentType : "";
            this.body = body != null ? body : "";
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getContentType() {
            return contentType;
        }

        public String getBody() {
            return body;
        }
    }
}