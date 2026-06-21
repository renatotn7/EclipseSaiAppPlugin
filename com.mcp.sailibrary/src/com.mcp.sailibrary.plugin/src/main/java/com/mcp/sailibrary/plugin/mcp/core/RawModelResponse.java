package com.mcp.sailibrary.plugin.mcp.core;

/** * Resposta crua devolvida pelo connector. * * <p>O response codec interpreta esta estrutura e extrai o texto principal.</p> * * @author Renato Tomaz Nati * @since 2026-05-26 */
public class RawModelResponse {

    private String rawBody;
    private int statusCode;
    private String contentType;

    public RawModelResponse() {
        this.rawBody = "";
        this.statusCode = 0;
        this.contentType = "";
    }

    public RawModelResponse(String rawBody, int statusCode, String contentType) {
    	System.out.println("RawModelResponse");
    	System.out.println(rawBody);
        this.rawBody = rawBody != null ? rawBody : "";
        this.statusCode = statusCode;
        this.contentType = contentType != null ? contentType : "";
    }

    public String getRawBody() {
        return rawBody;
    }

    public void setRawBody(String rawBody) {
        this.rawBody = rawBody != null ? rawBody : "";
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType != null ? contentType : "";
    }
}