package com.mcp.sailibrary.plugin.mcp.extractor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** * Feature: parser estruturado para respostas MCP em SSE e JSON direto. * * Libs: * - JDK 11+ * - Gson 2.x * * Objetivo: * - concentrar regras de parse do protocolo * - reduzir o tamanho da fachada McpResponseExtractor * - manter rastreabilidade de entrada e saida * * Data de criacao: 2026-05-27 12:10 * * Autor: Renato Tomaz Nati * Since: 2026-05-27 */
public class McpStructuredResponseTextExtractor {

    /** * Data: 2026-05-27 12:10 * Chamado por: * - McpResponseExtractor.extractPrimaryTextFromStreamingSse * * Chama: * - processSseEvent * * Objetivo: * - consolidar texto final de eventos SSE * - priorizar response completed * - cair para output item done, content part done e deltas */
    public String extractPrimaryTextFromStreamingSse(String rawResponse) {
        System.out.println("[MCP STRUCTURED PARSER] extractPrimaryTextFromStreamingSse input=");
        System.out.println(rawResponse);

        if (isBlank(rawResponse)) {
            return "";
        }

        if (!looksLikeSse(rawResponse)) {
            return "";
        }

        String[] lines = rawResponse.split("\\r?\\n");

        String currentEvent = "";
        StringBuilder currentData = new StringBuilder();

        StringBuilder deltaAccumulator = new StringBuilder();
        String bestCompletedText = "";
        String bestOutputItemDoneText = "";
        String bestContentPartDoneText = "";

        int lineIndex = 0;
        while (lineIndex < lines.length) {
            String currentLine = lines[lineIndex] != null ? lines[lineIndex] : "";

            if (currentLine.startsWith("event:")) {
                currentEvent = currentLine.substring("event:".length()).trim();
                lineIndex++;
                continue;
            }

            if (currentLine.startsWith("data:")) {
                String dataPart = currentLine.substring("data:".length()).trim();
                if (currentData.length() > 0) {
                    currentData.append("\n");
                }
                currentData.append(dataPart);
                lineIndex++;
                continue;
            }

            if (currentLine.trim().length() == 0) {
                ParsedSseTexts parsedTexts = processSseEvent(
                        currentEvent,
                        currentData.toString().trim(),
                        deltaAccumulator,
                        bestCompletedText,
                        bestOutputItemDoneText,
                        bestContentPartDoneText
                );

                bestCompletedText = parsedTexts.getBestCompletedText();
                bestOutputItemDoneText = parsedTexts.getBestOutputItemDoneText();
                bestContentPartDoneText = parsedTexts.getBestContentPartDoneText();

                currentEvent = "";
                currentData.setLength(0);
            }

            lineIndex++;
        }

        if (currentData.length() > 0) {
            ParsedSseTexts parsedTexts = processSseEvent(
                    currentEvent,
                    currentData.toString().trim(),
                    deltaAccumulator,
                    bestCompletedText,
                    bestOutputItemDoneText,
                    bestContentPartDoneText
            );

            bestCompletedText = parsedTexts.getBestCompletedText();
            bestOutputItemDoneText = parsedTexts.getBestOutputItemDoneText();
            bestContentPartDoneText = parsedTexts.getBestContentPartDoneText();
        }

        if (!isBlank(bestCompletedText)) {
            System.out.println("[MCP STRUCTURED PARSER] extractPrimaryTextFromStreamingSse output completed=");
            System.out.println(bestCompletedText);
            return bestCompletedText.trim();
        }

        if (!isBlank(bestOutputItemDoneText)) {
            System.out.println("[MCP STRUCTURED PARSER] extractPrimaryTextFromStreamingSse output item done=");
            System.out.println(bestOutputItemDoneText);
            return bestOutputItemDoneText.trim();
        }

        if (!isBlank(bestContentPartDoneText)) {
            System.out.println("[MCP STRUCTURED PARSER] extractPrimaryTextFromStreamingSse output part done=");
            System.out.println(bestContentPartDoneText);
            return bestContentPartDoneText.trim();
        }

        if (deltaAccumulator.length() > 0) {
            String deltaText = deltaAccumulator.toString().trim();
            System.out.println("[MCP STRUCTURED PARSER] extractPrimaryTextFromStreamingSse output delta=");
            System.out.println(deltaText);
            return deltaText;
        }

        return "";
    }

    /* * Feature: extrai texto de jsons diretos fora do envelope legado. * Data: 2026-05-27 03:20 * Caller: * - McpResponseExtractor.extractPrimaryText * Chama: * - extractResponseCompletedText * - extractOutputItemDoneText * - extractPartText * - appendTextsFromChoicesArray * - appendTextsFromOutputArray * - appendTextsFromItemsArray * - appendTextsFromContentArray * Objetivo: * - Cobrir response.output, output, items, choices e text direto */
    public String extractPrimaryTextFromDirectJson(String rawResponse) {
        System.out.println("[MCP STRUCTURED PARSER] extractPrimaryTextFromDirectJson input=");
        System.out.println(rawResponse);

        if (isBlank(rawResponse)) {
            return "";
        }

        try {
            JsonElement rootElement = JsonParser.parseString(rawResponse);
            if (!rootElement.isJsonObject()) {
                return "";
            }

            JsonObject rootObject = rootElement.getAsJsonObject();

            String responseCompletedText = extractResponseCompletedText(rootObject);
            if (!isBlank(responseCompletedText)) {
                System.out.println("[MCP STRUCTURED PARSER] extractPrimaryTextFromDirectJson output completed=");
                System.out.println(responseCompletedText);
                return responseCompletedText;
            }

            String outputItemDoneText = extractOutputItemDoneText(rootObject);
            if (!isBlank(outputItemDoneText)) {
                System.out.println("[MCP STRUCTURED PARSER] extractPrimaryTextFromDirectJson output outputItemDone=");
                System.out.println(outputItemDoneText);
                return outputItemDoneText;
            }

            String partText = extractPartText(rootObject);
            if (!isBlank(partText)) {
                System.out.println("[MCP STRUCTURED PARSER] extractPrimaryTextFromDirectJson output part=");
                System.out.println(partText);
                return partText;
            }

            String directText = readString(rootObject, "text");
            if (!isBlank(directText)) {
                System.out.println("[MCP STRUCTURED PARSER] extractPrimaryTextFromDirectJson output text=");
                System.out.println(directText);
                return directText;
            }

            if (rootObject.has("content")) {
                JsonElement content = rootObject.get("content");

                if (content != null && content.isJsonPrimitive() && content.getAsJsonPrimitive().isString()) {
                    String contentText = content.getAsString();
                    System.out.println("[MCP STRUCTURED PARSER] extractPrimaryTextFromDirectJson output contentString=");
                    System.out.println(contentText);
                    return contentText;
                }

                if (content != null && content.isJsonArray()) {
                    StringBuilder builder = new StringBuilder();
                    appendTextsFromContentArray(builder, content);
                    if (builder.length() > 0) {
                        String contentArrayText = builder.toString().trim();
                        System.out.println("[MCP STRUCTURED PARSER] extractPrimaryTextFromDirectJson output contentArray=");
                        System.out.println(contentArrayText);
                        return contentArrayText;
                    }
                }
            }
        } catch (Exception exception) {
            System.out.println("[MCP STRUCTURED PARSER] extractPrimaryTextFromDirectJson falha parse: " + exception.getMessage());
        }

        return "";
    }
    /* * Feature: extrai textos de arrays output. * Data: 2026-05-27 03:20 * Caller: * - extractResponseCompletedText * - extractPrimaryTextFromDirectJson * Chama: * - appendTextsFromContentArray * Objetivo: * - Cobrir o formato response.output.content.text do endpoint real */
    private void appendTextsFromOutputArray(StringBuilder builder, JsonElement outputElement) {
        if (builder == null || outputElement == null || !outputElement.isJsonArray()) {
            return;
        }

        JsonArray outputArray = outputElement.getAsJsonArray();

        int index = 0;
        while (index < outputArray.size()) {
            JsonElement outputItemElement = outputArray.get(index);
            if (outputItemElement.isJsonObject()) {
                JsonObject outputItemObject = outputItemElement.getAsJsonObject();
                appendTextsFromContentArray(builder, outputItemObject.get("content"));
            }
            index++;
        }
    }
    /** * Data: 2026-05-27 12:10 * Chamado por: * - extractPrimaryTextFromStreamingSse * * Chama: * - normalizeEventName * - extractResponseCompletedText * - extractOutputItemDoneText * - extractPartText * * Objetivo: * - consolidar um evento SSE completo e atualizar os melhores candidatos */
    private ParsedSseTexts processSseEvent(String currentEvent, String dataJson, StringBuilder deltaAccumulator, String bestCompletedText, String bestOutputItemDoneText, String bestContentPartDoneText) {

        if (isBlank(dataJson)) {
            return new ParsedSseTexts(bestCompletedText, bestOutputItemDoneText, bestContentPartDoneText);
        }

        try {
            JsonElement parsedElement = JsonParser.parseString(dataJson);
            if (!parsedElement.isJsonObject()) {
                return new ParsedSseTexts(bestCompletedText, bestOutputItemDoneText, bestContentPartDoneText);
            }

            JsonObject jsonObject = parsedElement.getAsJsonObject();
            String eventType = readString(jsonObject, "type");
            String normalizedEventName = normalizeEventName(currentEvent, eventType);

            if ("responseoutputtextdelta".equals(normalizedEventName)
                    || "outputtextdelta".equals(normalizedEventName)) {
                String deltaText = readString(jsonObject, "delta");
                if (!isBlank(deltaText)) {
                    deltaAccumulator.append(deltaText);
                }
            }

            if ("contentpartdone".equals(normalizedEventName)) {
                String partText = extractPartText(jsonObject);
                if (!isBlank(partText)) {
                    bestContentPartDoneText = partText;
                }
            }

            if ("outputitemdone".equals(normalizedEventName)) {
                String outputItemDoneText = extractOutputItemDoneText(jsonObject);
                if (!isBlank(outputItemDoneText)) {
                    bestOutputItemDoneText = outputItemDoneText;
                }
            }

            if ("responsecompleted".equals(normalizedEventName)) {
                String completedText = extractResponseCompletedText(jsonObject);
                if (!isBlank(completedText)) {
                    bestCompletedText = completedText;
                }
            }
        } catch (Exception exception) {
            System.out.println("[MCP STRUCTURED PARSER] processSseEvent falha parse: "
                    + exception.getMessage());
        }

        return new ParsedSseTexts(bestCompletedText, bestOutputItemDoneText, bestContentPartDoneText);
    }

    /* * Feature: extrai o texto final consolidado do json. * Data: 2026-05-27 03:20 * Caller: * - extractPrimaryTextFromDirectJson * - extractPrimaryTextFromStreamingSse * Chama: * - appendTextsFromItemsArray * - appendTextsFromOutputArray * Objetivo: * - Unificar o parse dos formatos responsecompleted e response.output */
    private String extractResponseCompletedText(JsonObject json) {
        if (json == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        if (json.has("items") && json.get("items").isJsonArray()) {
            appendTextsFromItemsArray(builder, json.get("items"));
        }

        if (builder.length() == 0 && json.has("response") && json.get("response").isJsonObject()) {
            JsonObject responseObject = json.getAsJsonObject("response");
            extractTextsFromResponseOutput(builder, responseObject);
        }

        if (builder.length() == 0 && json.has("output") && json.get("output").isJsonArray()) {
            extractTextsFromResponseOutput(builder, json);
        }

        return builder.toString().trim();
    }
    /* * Feature: fallback para respostas no formato choices. * Data: 2026-05-27 03:20 * Caller: * - extractPrimaryTextFromDirectJson * Chama: * - readString * Objetivo: * - Cobrir respostas legadas simplificadas usadas por testes e integracoes antigas */
    private void appendTextsFromChoicesArray(StringBuilder builder, JsonElement choicesElement) {
        if (builder == null || choicesElement == null || !choicesElement.isJsonArray()) {
            return;
        }

        JsonArray choicesArray = choicesElement.getAsJsonArray();

        int index = 0;
        while (index < choicesArray.size()) {
            JsonElement choiceElement = choicesArray.get(index);
            if (choiceElement.isJsonObject()) {
                JsonObject choiceObject = choiceElement.getAsJsonObject();

                String text = readString(choiceObject, "text");
                if (!isBlank(text)) {
                    builder.append(text);
                }

                if (builder.length() == 0 && choiceObject.has("message") && choiceObject.get("message").isJsonObject()) {
                    JsonObject messageObject = choiceObject.getAsJsonObject("message");
                    appendTextsFromContentArray(builder, messageObject.get("content"));
                    String messageText = readString(messageObject, "text");
                    if (!isBlank(messageText)) {
                        builder.append(messageText);
                    }
                }
            }
            index++;
        }
    }
    /* * Feature: extrai textos de arrays items. * Data: 2026-05-27 03:20 * Caller: * - extractResponseCompletedText * - extractPrimaryTextFromDirectJson * Chama: * - appendTextsFromContentArray * Objetivo: * - Reaproveitar o parse de content e raw_item.content */
    private void appendTextsFromItemsArray(StringBuilder builder, JsonElement itemsElement) {
        if (builder == null || itemsElement == null || !itemsElement.isJsonArray()) {
            return;
        }

        JsonArray itemsArray = itemsElement.getAsJsonArray();

        for (int index = 0; index < itemsArray.size(); index++) {
            JsonElement itemElement = itemsArray.get(index);
            if (!itemElement.isJsonObject()) {
                continue;
            }

            JsonObject itemObject = itemElement.getAsJsonObject();
            appendPreferredTextsFromItemObject(builder, itemObject);
        }
    }
    private void appendPreferredTextsFromItemObject(StringBuilder builder, JsonObject itemObject) {
        if (builder == null || itemObject == null) {
            return;
        }

        int sizeBeforeMainContent = builder.length();
        appendTextsFromContentArray(builder, itemObject.get("content"));

        if (builder.length() > sizeBeforeMainContent) {
            return;
        }

        if (!itemObject.has("raw_item") || !itemObject.get("raw_item").isJsonObject()) {
            return;
        }

        JsonObject rawItemObject = itemObject.getAsJsonObject("raw_item");
        appendTextsFromContentArray(builder, rawItemObject.get("content"));
    }
    private void extractTextsFromResponseOutput(StringBuilder builder, JsonObject sourceObject) {
        if (builder == null || sourceObject == null) {
            return;
        }

        JsonElement outputElement = sourceObject.get("output");
        if (outputElement == null || !outputElement.isJsonArray()) {
            return;
        }

        JsonArray outputArray = outputElement.getAsJsonArray();

        for (int index = 0; index < outputArray.size(); index++) {
            JsonElement outputItemElement = outputArray.get(index);
            if (!outputItemElement.isJsonObject()) {
                continue;
            }

            JsonObject outputItemObject = outputItemElement.getAsJsonObject();
            appendTextsFromContentArray(builder, outputItemObject.get("content"));
        }
    }

    /** * Data: 2026-05-27 12:10 * Chamado por: * - processSseEvent * - extractPrimaryTextFromDirectJson * * Chama: * - appendTextsFromItemObject * * Objetivo: * - extrair texto de evento output item done */
    private String extractOutputItemDoneText(JsonObject json) {
        if (json == null || !json.has("item") || !json.get("item").isJsonObject()) {
            return "";
        }

        JsonObject itemObject = json.getAsJsonObject("item");
        StringBuilder builder = new StringBuilder();

        appendPreferredTextsFromItemObject(builder, itemObject);

        return builder.toString().trim();
    }
   

    /** * Data: 2026-05-27 12:10 * Chamado por: * - processSseEvent * - extractPrimaryTextFromDirectJson * * Chama: * - readString * * Objetivo: * - extrair texto do bloco part */
    private String extractPartText(JsonObject jsonObject) {
        if (jsonObject == null || !jsonObject.has("part") || !jsonObject.get("part").isJsonObject()) {
            return "";
        }

        JsonObject partObject = jsonObject.getAsJsonObject("part");
        return readString(partObject, "text");
    }

   

 

    /** * Data: 2026-05-27 12:10 * Chamado por: * - appendTextsFromItemsArray * - appendTextsFromOutputArray * - extractOutputItemDoneText * * Chama: * - appendTextsFromContentArray * * Objetivo: * - extrair texto de item.content e item.raw_item.content */
    private void appendTextsFromItemObject(StringBuilder builder, JsonElement itemElement) {
        if (builder == null || itemElement == null || !itemElement.isJsonObject()) {
            return;
        }

        JsonObject itemObject = itemElement.getAsJsonObject();
        appendTextsFromContentArray(builder, itemObject.get("content"));

        if (itemObject.has("raw_item") && itemObject.get("raw_item").isJsonObject()) {
            JsonObject rawItemObject = itemObject.getAsJsonObject("raw_item");
            appendTextsFromContentArray(builder, rawItemObject.get("content"));
        }
    }

    /** * Data: 2026-05-27 12:10 * Chamado por: * - appendTextsFromItemObject * - extractPrimaryTextFromDirectJson * * Chama: * - readString * - normalizeSimpleToken * * Objetivo: * - extrair texto de content[] preservando output_text */
    private void appendTextsFromContentArray(StringBuilder builder, JsonElement contentElement) {
        if (builder == null || contentElement == null || !contentElement.isJsonArray()) {
            return;
        }

        JsonArray contentArray = contentElement.getAsJsonArray();

        int contentIndex = 0;
        while (contentIndex < contentArray.size()) {
            JsonElement contentItemElement = contentArray.get(contentIndex);

            if (contentItemElement != null && contentItemElement.isJsonObject()) {
                JsonObject contentObject = contentItemElement.getAsJsonObject();
                String contentType = normalizeSimpleToken(readString(contentObject, "type"));
                String text = readString(contentObject, "text");

                if (!isBlank(text)) {
                    if (isBlank(contentType) || "outputtext".equals(contentType)) {
                        builder.append(text);
                    }
                }
            }

            contentIndex++;
        }
    }

    /** * Data: 2026-05-27 12:10 * Chamado por: * - processSseEvent * * Objetivo: * - normalizar nome do evento para uma comparacao estavel */
    private String normalizeEventName(String currentEvent, String eventType) {
        String baseEventName = !isBlank(currentEvent) ? currentEvent : eventType;
        return normalizeSimpleToken(baseEventName);
    }

    /** * Data: 2026-05-27 12:10 * Chamado por: * - normalizeEventName * - appendTextsFromContentArray * * Objetivo: * - remover pontuacao de nomes de evento e tipo para comparacao segura */
    private String normalizeSimpleToken(String value) {
        if (isBlank(value)) {
            return "";
        }

        return value.toLowerCase()
                .replace(".", "")
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "")
                .trim();
    }

    /** * Data: 2026-05-27 12:10 * Chamado por: * - varios metodos de parse * * Objetivo: * - ler string de JsonObject sem lancar excecao */
    private String readString(JsonObject jsonObject, String propertyName) {
        if (jsonObject == null || propertyName == null || !jsonObject.has(propertyName)
                || jsonObject.get(propertyName).isJsonNull()) {
            return "";
        }

        try {
            return jsonObject.get(propertyName).getAsString();
        } catch (Exception exception) {
            return "";
        }
    }

    /** * Data: 2026-05-27 12:10 * Chamado por: * - extractPrimaryTextFromStreamingSse * * Objetivo: * - detectar se o texto recebido tem formato bruto de SSE */
    private boolean looksLikeSse(String rawResponse) {
        String safeText = rawResponse != null ? rawResponse : "";
        return safeText.contains("event:") && safeText.contains("data:");
    }

    /** * Data: 2026-05-27 12:10 * Chamado por: * - varios metodos desta classe * * Objetivo: * - validacao defensiva de string */
    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
    /* * Feature: identifica json objeto de forma barata. * Data: 2026-05-27 03:20 * Caller: * - extractPrimaryText * - extractPrimaryTextFromLegacyEnvelope * Chama: * - nenhuma * Objetivo: * - Evitar tentativa de parse legado em SSE e texto puro */
    /* * Feature: valida rapidamente se a resposta parece um objeto json. * Data: 2026-05-27 03:20 * Chamado por: * - extractPrimaryTextFromDirectJson * - pontos de decisao de parse estruturado * Chama: * - isBlank * Objetivo: * - evitar parse desnecessario em texto puro ou em stream SSE */
    private boolean looksLikeJsonObject(String rawResponse) {
        if (isBlank(rawResponse)) {
            return false;
        }

        String trimmedResponse = rawResponse.trim();
        return trimmedResponse.startsWith("{") && trimmedResponse.endsWith("}");
    }
    /** * Feature auxiliar para transporte interno do melhor estado de parse SSE. */
    private static class ParsedSseTexts {

        private final String bestCompletedText;
        private final String bestOutputItemDoneText;
        private final String bestContentPartDoneText;

        private ParsedSseTexts(String bestCompletedText, String bestOutputItemDoneText, String bestContentPartDoneText) {
            this.bestCompletedText = bestCompletedText != null ? bestCompletedText : "";
            this.bestOutputItemDoneText = bestOutputItemDoneText != null ? bestOutputItemDoneText : "";
            this.bestContentPartDoneText = bestContentPartDoneText != null ? bestContentPartDoneText : "";
        }

        private String getBestCompletedText() {
            return bestCompletedText;
        }

        private String getBestOutputItemDoneText() {
            return bestOutputItemDoneText;
        }

        private String getBestContentPartDoneText() {
            return bestContentPartDoneText;
        }
    }
}