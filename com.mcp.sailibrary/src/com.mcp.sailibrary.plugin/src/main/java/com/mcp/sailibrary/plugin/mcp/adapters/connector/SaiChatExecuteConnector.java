package com.mcp.sailibrary.plugin.mcp.adapters.connector;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.mcp.sailibrary.plugin.mcp.core.McpAccessCredentials;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.RawModelResponse;
import com.mcp.sailibrary.plugin.mcp.ports.ModelConnector;

/** * Connector HTTP programatico para SAI /chatexecute. * * @author Renato Tomaz Nati * @since 2026-06-19 */
public class SaiChatExecuteConnector implements ModelConnector {

    private final HttpClient httpClient;

    public SaiChatExecuteConnector() {
        this(HttpClient.newHttpClient());
    }

    public SaiChatExecuteConnector(HttpClient httpClient) {
        this.httpClient = httpClient != null ? httpClient : HttpClient.newHttpClient();
    }

    @Override
    public RawModelResponse execute(ModelExecutionProfile profile, String requestBody, McpAccessCredentials credentials) throws Exception {
        if (profile == null) {
            throw new IllegalArgumentException("Erro Operacional: profile nao pode ser nulo.");
        }

        if (isBlank(profile.getEndpointUrl())) {
            throw new IllegalArgumentException("Erro Operacional: endpointUrl SAI chatexecute nao pode ser vazio.");
        }

        String safeRequestBody = requestBody != null ? requestBody : "";
        McpAccessCredentials safeCredentials = credentials != null ? credentials : new McpAccessCredentials();
        String cookieHeader = buildCookieHeader(safeCredentials, profile);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(profile.getEndpointUrl()))
                .version(HttpClient.Version.HTTP_1_1)
                .header("accept", "application/json, text/plain, */*")
                .header("accept-language", "[object Object]")
                .header("content-type", "application/json")
                .header("dnt", "1")
                .header("origin", resolveOrigin(profile))
                .header("priority", "u=1, i")
                .header("referer", resolveReferer(profile))
                .header("sec-ch-ua", "\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\"")
                .header("sec-ch-ua-mobile", "?0")
                .header("sec-ch-ua-platform", "\"Linux\"")
                .header("sec-fetch-dest", "empty")
                .header("sec-fetch-mode", "cors")
                .header("sec-fetch-site", "same-origin")
                .header("sec-gpc", "1")
                .header("user-agent", resolveUserAgent(profile))
                .POST(HttpRequest.BodyPublishers.ofString(safeRequestBody));

        if (!isBlank(cookieHeader)) {
            builder.header("cookie", cookieHeader);
        }

        HttpRequest request = builder.build();

        System.out.println("[SAI CHATEXECUTE CONNECTOR] ========================================");
        System.out.println("[SAI CHATEXECUTE CONNECTOR] endpoint=" + profile.getEndpointUrl());
        System.out.println("[SAI CHATEXECUTE CONNECTOR] requestBodyLength=" + safeRequestBody.length());
        System.out.println("[SAI CHATEXECUTE CONNECTOR] cookieConfigured=" + (!isBlank(cookieHeader) ? "true" : "false"));
        System.out.println("[SAI CHATEXECUTE CONNECTOR] cookieMasked=" + maskCookie(cookieHeader));
        System.out.println("[SAI CHATEXECUTE CONNECTOR] origin=" + resolveOrigin(profile));
        System.out.println("[SAI CHATEXECUTE CONNECTOR] referer=" + resolveReferer(profile));
        System.out.println("[SAI CHATEXECUTE CONNECTOR] userAgent=" + resolveUserAgent(profile));
        System.out.println("[SAI CHATEXECUTE CONNECTOR] ========================================");

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        String contentType = response.headers().firstValue("Content-Type").orElse("");

        System.out.println("[SAI CHATEXECUTE CONNECTOR] responseStatus=" + response.statusCode());
        System.out.println("[SAI CHATEXECUTE CONNECTOR] responseContentType=" + contentType);
        System.out.println("[SAI CHATEXECUTE CONNECTOR] responseBodyLength=" + (response.body() != null ? response.body().length() : 0));

        return new RawModelResponse(response.body(), response.statusCode(), contentType);
    }

    private String buildCookieHeader(McpAccessCredentials credentials, ModelExecutionProfile profile) {
        String value = "";
        if (credentials != null && credentials.hasCookieValue()) {
            value = credentials.getCookieValue();
        }
        if (isBlank(value) && profile != null) {
            value = profile.getDefaultCookieValue();
        }
        if (isBlank(value)) {
            return "";
        }
        if (value.startsWith(".AspNetCore.Cookies=")) {
            return value;
        }
        return ".AspNetCore.Cookies=" + value;
    }

    private String resolveOrigin(ModelExecutionProfile profile) {
        return "https://sai-library.saiapplications.com";
    }

    private String resolveReferer(ModelExecutionProfile profile) {
        return "https://sai-library.saiapplications.com/free-chat";
    }

    private String resolveUserAgent(ModelExecutionProfile profile) {
        return "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36";
    }

    private String maskCookie(String cookie) {
        if (isBlank(cookie)) {
            return "";
        }
        if (cookie.length() <= 70) {
            return cookie.substring(0, Math.min(25, cookie.length())) + "...REDACTED";
        }
        return cookie.substring(0, 45) + "...REDACTED..." + cookie.substring(cookie.length() - 18);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
