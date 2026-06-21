package com.mcp.sailibrary.plugin.mcp.adapters.codec.request;

import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionRequest;
import com.mcp.sailibrary.plugin.mcp.ports.ModelRequestCodec;

/** * Codec para chamada direta SAI /chatexecute. * * @author Renato Tomaz Nati * @since 2026-06-19 */
public class SaiChatExecuteRequestCodec implements ModelRequestCodec {

    @Override
    public String encode(ModelExecutionRequest request, ModelExecutionProfile profile) throws Exception {
        if (request == null) {
            throw new IllegalArgumentException("Erro Operacional: request nao pode ser nulo.");
        }

        String prompt = request.getPrompt() != null ? request.getPrompt() : "";

        StringBuilder body = new StringBuilder();
        body.append("{\"messages\":[{\"content\":\"");
        body.append(escapeJson(prompt));
        body.append("\",\"role\":\"user\"}]}");

        System.out.println("[SAI CHATEXECUTE REQUEST CODEC] =====================================");
        System.out.println("[SAI CHATEXECUTE REQUEST CODEC] promptLength=" + prompt.length());
        System.out.println("[SAI CHATEXECUTE REQUEST CODEC] bodyLength=" + body.length());
        System.out.println("[SAI CHATEXECUTE REQUEST CODEC] profileChannel=" + (profile != null && profile.getChannel() != null ? profile.getChannel().name() : ""));
        System.out.println("[SAI CHATEXECUTE REQUEST CODEC] =====================================");

        return body.toString();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder escaped = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '\\') {
                escaped.append("\\\\");
            } else if (current == '"') {
                escaped.append("\\\"");
            } else if (current == '\n') {
                escaped.append("\\n");
            } else if (current == '\r') {
                escaped.append("\\r");
            } else if (current == '\t') {
                escaped.append("\\t");
            } else if (current < 32) {
                String hex = Integer.toHexString(current);
                escaped.append("\\u");
                for (int i = hex.length(); i < 4; i++) {
                    escaped.append('0');
                }
                escaped.append(hex);
            } else {
                escaped.append(current);
            }
        }
        return escaped.toString();
    }
}
