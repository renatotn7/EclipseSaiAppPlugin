package com.mcp.sailibrary.plugin.mcp;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/** * Encapsula o transporte HTTP usado para chamadas MCP. * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class McpHttpGateway {

    /** * Executa uma chamada POST JSON-RPC ao endpoint MCP. * * @param apiUrl endpoint MCP * @param apiKey chave de autenticacao * @param jsonPayload payload JSON-RPC serializado * @return corpo bruto da resposta HTTP * * @throws Exception quando houver falha de rede ou transporte * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String postJsonRpc(String apiUrl, String apiKey, String jsonPayload) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("X-Api-Key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}