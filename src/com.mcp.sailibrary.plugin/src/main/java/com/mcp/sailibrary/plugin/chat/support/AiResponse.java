package com.mcp.sailibrary.plugin.chat.support;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Transportar a resposta estruturada da LLM entre parser, validador e executor.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-18
 */
public class AiResponse {

    private String action;
    private String content;
    private String explanation;
    private String tool;
    private JsonObject parameters;
    private String question;
    private String expectedAnswerType;
    private JsonArray options;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getTool() {
        return tool;
    }

    public void setTool(String tool) {
        this.tool = tool;
    }

    public JsonObject getParameters() {
        return parameters;
    }

    public void setParameters(JsonObject parameters) {
        this.parameters = parameters;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getExpectedAnswerType() {
        return expectedAnswerType;
    }

    public void setExpectedAnswerType(String expectedAnswerType) {
        this.expectedAnswerType = expectedAnswerType;
    }

    public JsonArray getOptions() {
        return options;
    }

    public void setOptions(JsonArray options) {
        this.options = options;
    }
}