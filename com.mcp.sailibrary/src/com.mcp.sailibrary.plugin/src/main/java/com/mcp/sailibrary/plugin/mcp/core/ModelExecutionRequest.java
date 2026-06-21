package com.mcp.sailibrary.plugin.mcp.core;

/** * Request neutro de execucao de modelo. * * <p>Esta classe permite trabalhar com prompt simples ou com rawJsonBody * explicito, dependendo do codec configurado.</p> * * @author Renato Tomaz Nati * @since 2026-05-26 */
public class ModelExecutionRequest {

    private ModelChannel channel;
    private String prompt;
    private String rawJsonBody;
    private McpAccessCredentials credentials;

    public ModelExecutionRequest() {
        this.prompt = "";
        this.rawJsonBody = "";
        this.credentials = new McpAccessCredentials();
    }

    public ModelChannel getChannel() {
        return channel;
    }

    public void setChannel(ModelChannel channel) {
        this.channel = channel;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = safeTrim(prompt);
    }

    public String getRawJsonBody() {
        return rawJsonBody;
    }

    public void setRawJsonBody(String rawJsonBody) {
        this.rawJsonBody = safeTrim(rawJsonBody);
    }

    public McpAccessCredentials getCredentials() {
        return credentials;
    }

    public void setCredentials(McpAccessCredentials credentials) {
        this.credentials = credentials != null ? credentials : new McpAccessCredentials();
    }

    public boolean hasRawJsonBody() {
        return rawJsonBody != null && rawJsonBody.trim().length() > 0;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}