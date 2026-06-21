package com.mcp.sailibrary.plugin.mcp.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** * Resposta neutra ja interpretada do modelo. * * <p> * Mantem compatibilidade com o fluxo atual e adiciona suporte a * acompanhamento de streaming. * </p> * * @author Renato Tomaz Nati * @since 2026-05-26 */
public class ModelExecutionResponse {

    private String primaryText;
    private String rawResponseBody;
    private int httpStatusCode;
    private String contentType;

    private boolean streamingResponse;
    private boolean completed;
    private String finishReason;
    private String responseId;
    private String conversationId;
    private String accumulatedStreamingText;
    private List<String> streamingDeltas;

    public ModelExecutionResponse() {
        this.primaryText = "";
        this.rawResponseBody = "";
        this.httpStatusCode = 0;
        this.contentType = "";
        this.streamingResponse = false;
        this.completed = false;
        this.finishReason = "";
        this.responseId = "";
        this.conversationId = "";
        this.accumulatedStreamingText = "";
        this.streamingDeltas = new ArrayList<String>();
    }

    public String getPrimaryText() {
        return primaryText;
    }

    public void setPrimaryText(String primaryText) {
        this.primaryText = primaryText != null ? primaryText : "";
    }

    public String getRawResponseBody() {
        return rawResponseBody;
    }

    public void setRawResponseBody(String rawResponseBody) {
        this.rawResponseBody = rawResponseBody != null ? rawResponseBody : "";
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(int httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType != null ? contentType : "";
    }

    public boolean isStreamingResponse() {
        return streamingResponse;
    }

    public void setStreamingResponse(boolean streamingResponse) {
        this.streamingResponse = streamingResponse;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason != null ? finishReason : "";
    }

    public String getResponseId() {
        return responseId;
    }

    public void setResponseId(String responseId) {
        this.responseId = responseId != null ? responseId : "";
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId != null ? conversationId : "";
    }

    public String getAccumulatedStreamingText() {
        return accumulatedStreamingText;
    }

    public void setAccumulatedStreamingText(String accumulatedStreamingText) {
        this.accumulatedStreamingText = accumulatedStreamingText != null ? accumulatedStreamingText : "";
    }

    public List<String> getStreamingDeltas() {
        return Collections.unmodifiableList(streamingDeltas);
    }

    public void setStreamingDeltas(List<String> streamingDeltas) {
        this.streamingDeltas = new ArrayList<String>();

        if (streamingDeltas == null) {
            return;
        }

        for (int i = 0; i < streamingDeltas.size(); i++) {
            String delta = streamingDeltas.get(i);
            if (delta != null && delta.length() > 0) {
                this.streamingDeltas.add(delta);
            }
        }

        rebuildAccumulatedStreamingText();
    }

    public void addStreamingDelta(String delta) {
        if (delta == null || delta.length() == 0) {
            return;
        }

        if (streamingDeltas == null) {
            streamingDeltas = new ArrayList<String>();
        }

        streamingDeltas.add(delta);
        accumulatedStreamingText = (accumulatedStreamingText != null ? accumulatedStreamingText : "") + delta;
    }

    public boolean hasPrimaryText() {
        return primaryText != null && primaryText.trim().length() > 0;
    }

    public boolean hasRawResponseBody() {
        return rawResponseBody != null && rawResponseBody.trim().length() > 0;
    }

    public boolean hasAccumulatedStreamingText() {
        return accumulatedStreamingText != null && accumulatedStreamingText.trim().length() > 0;
    }

    private void rebuildAccumulatedStreamingText() {
        StringBuilder builder = new StringBuilder();

        if (streamingDeltas != null) {
            for (int i = 0; i < streamingDeltas.size(); i++) {
                String delta = streamingDeltas.get(i);
                if (delta != null) {
                    builder.append(delta);
                }
            }
        }

        accumulatedStreamingText = builder.toString();
    }
}