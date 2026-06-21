package com.mcp.sailibrary.plugin.mcp.core;

/** * Representa credenciais de acesso para os diferentes tipos de transporte. * * <p>O modo legado usa tipicamente apiKey. O modo streaming pode usar cookie. * A classe permite coexistencia dos dois para manter compatibilidade e reduzir * acoplamento entre a camada de uso e a forma concreta de autenticacao.</p> * * @author Renato Tomaz Nati * @since 2026-05-26 */
public class McpAccessCredentials {

    private String apiKey;
    private String cookieValue;

    public McpAccessCredentials() {
    }

    public static McpAccessCredentials forApiKey(String apiKey) {
        McpAccessCredentials credentials = new McpAccessCredentials();
        credentials.setApiKey(apiKey);
        return credentials;
    }

    public static McpAccessCredentials forCookie(String cookieValue) {
        McpAccessCredentials credentials = new McpAccessCredentials();
        credentials.setCookieValue(cookieValue);
        return credentials;
    }

    public static McpAccessCredentials forApiKeyAndCookie(String apiKey, String cookieValue) {
        McpAccessCredentials credentials = new McpAccessCredentials();
        credentials.setApiKey(apiKey);
        credentials.setCookieValue(cookieValue);
        return credentials;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = safeTrim(apiKey);
    }

    public String getCookieValue() {
        return cookieValue;
    }

    public void setCookieValue(String cookieValue) {
        this.cookieValue = safeTrim(cookieValue);
    }

    public boolean hasApiKey() {
        return apiKey != null && apiKey.trim().length() > 0;
    }

    public boolean hasCookieValue() {
        return cookieValue != null && cookieValue.trim().length() > 0;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}