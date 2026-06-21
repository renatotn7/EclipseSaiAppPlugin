package com.mcp.sailibrary.plugin.mcp.adapters.connector;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.mcp.sailibrary.plugin.mcp.core.McpAccessCredentials;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.RawModelResponse;
import com.mcp.sailibrary.plugin.mcp.ports.ModelConnector;

/** * Connector HTTP para o protocolo legado JSON-RPC MCP. * * @author Renato Tomaz Nati * @since 2026-05-26 */
public class LegacyJsonRpcConnector implements ModelConnector {

    private final HttpClient httpClient;

    public LegacyJsonRpcConnector() {
        this(HttpClient.newHttpClient());
    }

    public LegacyJsonRpcConnector(HttpClient httpClient) {
        this.httpClient = httpClient != null ? httpClient : HttpClient.newHttpClient();
    }

    @Override
    public RawModelResponse execute(ModelExecutionProfile profile, String requestBody, McpAccessCredentials credentials) throws Exception {

        if (profile == null) {
            throw new IllegalArgumentException("Erro Operacional: profile nao pode ser nulo.");
        }

        if (isBlank(profile.getEndpointUrl())) {
            throw new IllegalArgumentException("Erro Operacional: endpointUrl legado nao pode ser vazio.");
        }

        String safeRequestBody = requestBody != null ? requestBody : "";
        McpAccessCredentials safeCredentials = credentials != null ? credentials : new McpAccessCredentials();

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(profile.getEndpointUrl()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(safeRequestBody));

        if (safeCredentials.hasApiKey()) {
            builder.header("X-Api-Key", safeCredentials.getApiKey());
        }

        HttpRequest request = builder.build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        String contentType = response.headers()
                .firstValue("Content-Type")
                .orElse("");

        return new RawModelResponse(
                response.body(),
                response.statusCode(),
                contentType
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}