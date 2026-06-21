package com.mcp.sailibrary.plugin.mcp.adapters.codec.response;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionResponse;
import com.mcp.sailibrary.plugin.mcp.core.RawModelResponse;
import com.mcp.sailibrary.plugin.mcp.ports.ModelResponseCodec;

/* class_metadata: feature: "Decodificacao de resposta streaming SSE e json direto" libraries: gson: "version definida no parent" objetivo: "Extrair o texto principal a partir do protocolo SSE real e dos fallbacks de integridade" */

/** * Codec de response para SSE. * * Regra principal: * - quando houver ResponseCompleted, usar items[].content[].text * - se nao houver, usar OutputItemDone * - se ainda nao houver, usar ContentPartDone * - por ultimo, usar o acumulado de OutputTextDelta * * Data de revisao: 2026-05-27 03:45 * * @author Renato Tomaz Nati * @since 2026-05-26 */
public class StreamingSseResponseCodec implements ModelResponseCodec {

	/* * Feature: decodifica a resposta do transporte streaming com rastreabilidade. * Data: 2026-05-27 03:20 * Caller: * - ModelExecutionEngine.execute * Chama: * - looksLikeJson * - extractPrimaryTextFromJsonBody * - extractPrimaryTextFromSse * Objetivo: * - Priorizar JSON direto quando o backend devolver corpo consolidado * - Preservar o parse SSE quando a resposta vier em eventos */
	@Override
	public ModelExecutionResponse decode(RawModelResponse rawResponse, ModelExecutionProfile profile) throws Exception {
	    ModelExecutionResponse response = new ModelExecutionResponse();

	    String rawBody = rawResponse != null ? rawResponse.getRawBody() : "";
	    String primaryText = extractPrimaryTextFromSse(rawBody);

	    response.setPrimaryText(primaryText);
	    response.setRawResponseBody(rawBody);
	    response.setHttpStatusCode(rawResponse != null ? rawResponse.getStatusCode() : 0);
	    response.setContentType(rawResponse != null ? rawResponse.getContentType() : "");

	    boolean isStreaming = rawResponse != null
	            && rawResponse.getContentType() != null
	            && rawResponse.getContentType().toLowerCase().contains("text/event-stream");

	    response.setStreamingResponse(isStreaming);
	    response.setCompleted(rawBody != null && rawBody.contains("event: complete"));

	    if (primaryText != null && primaryText.length() > 0) {
	        response.setAccumulatedStreamingText(primaryText);
	    }

	    System.out.println("[STREAMING SSE CODEC] ================================================");
	    System.out.println("[STREAMING SSE CODEC] statusCode=" + (rawResponse != null ? rawResponse.getStatusCode() : 0));
	    System.out.println("[STREAMING SSE CODEC] contentType=" + (rawResponse != null ? rawResponse.getContentType() : ""));
	    System.out.println("[STREAMING SSE CODEC] rawBodyLength=" + (rawBody != null ? rawBody.length() : 0));
	    System.out.println("[STREAMING SSE CODEC] rawBody=");
	    System.out.println(truncateForDebug(rawBody, 8000));
	    System.out.println("[STREAMING SSE CODEC] primaryTextLength=" + (primaryText != null ? primaryText.length() : 0));
	    System.out.println("[STREAMING SSE CODEC] primaryText=");
	    System.out.println(truncateForDebug(primaryText, 8000));
	    System.out.println("[STREAMING SSE CODEC] ================================================");

	    return response;
	}
	private String extractPrimaryTextFromJsonBody(String rawBody) {
	    return extractPrimaryTextFromJsonEnvelope(rawBody);
	}
 

    /** * Caller: * - decode * * Chama: * - extractResponseCompletedText * - extractOutputItemDoneText * - extractContentPartDoneText * - appendTextsFromItemsArray * - readString * * Objetivo: * - Interpretar json direto quando a API devolver o objeto final consolidado * * Alterado em: 2026-05-27 03:45 */
    private String extractPrimaryTextFromJsonEnvelope(String rawBody) {
        if (isBlank(rawBody)) {
            return "";
        }

        try {
            JsonElement parsedElement = JsonParser.parseString(rawBody);
            if (!parsedElement.isJsonObject()) {
                return "";
            }

            JsonObject rootObject = parsedElement.getAsJsonObject();
            String responseType = normalizeEventName(readString(rootObject, "type"));

            System.out.println("[STREAMING SSE CODEC] json responseType=" + responseType);

            if ("responsecompleted".equals(responseType)) {
                String responseCompletedText = extractResponseCompletedText(rootObject);
                if (!isBlank(responseCompletedText)) {
                    return responseCompletedText.trim();
                }
            }

            if ("outputitemdone".equals(responseType)) {
                String outputItemDoneText = extractOutputItemDoneText(rootObject);
                if (!isBlank(outputItemDoneText)) {
                    return outputItemDoneText.trim();
                }
            }

            if ("contentpartdone".equals(responseType)) {
                String contentPartDoneText = extractContentPartDoneText(rootObject);
                if (!isBlank(contentPartDoneText)) {
                    return contentPartDoneText.trim();
                }
            }

            StringBuilder builder = new StringBuilder();
            appendTextsFromItemsArray(builder, rootObject.get("items"));
            if (builder.length() > 0) {
                return builder.toString().trim();
            }

            return "";
        } catch (Exception exception) {
            System.out.println("[STREAMING SSE CODEC] falha ao interpretar json direto: " + exception.getMessage());
            return "";
        }
    }

    /* * Feature: extrai o texto principal do stream SSE. * Data: 2026-05-27 03:20 * Caller: * - decode * Chama: * - processCompletedSseEvent * - normalizeEventName * - isBlank * Objetivo: * - Consolidar a resposta final usando a ordem correta do protocolo real */
    private String extractPrimaryTextFromSse(String rawBody) {
        System.out.println("extractPrimaryTextFromSse");

        if (rawBody == null || rawBody.trim().length() == 0) {
            return "";
        }

        if (looksLikeJson(rawBody)) {
            return extractPrimaryTextFromJsonBody(rawBody);
        }

        String[] lines = rawBody.split("\\r?\\n");

        String currentEvent = "";
        StringBuilder currentData = new StringBuilder();

        String finalResponseCompletedText = "";
        String fallbackOutputItemDoneText = "";
        String fallbackContentPartDoneText = "";
        StringBuilder deltaAccumulator = new StringBuilder();

        for (int index = 0; index < lines.length; index++) {
            String line = lines[index] != null ? lines[index] : "";

            if (line.startsWith("event:")) {
                currentEvent = line.substring("event:".length()).trim();
                System.out.println("[STREAMING SSE CODEC] event=" + currentEvent);
                continue;
            }

            if (line.startsWith("data:")) {
                String dataPart = line.substring("data:".length()).trim();
                if (currentData.length() > 0) {
                    currentData.append("\n");
                }
                currentData.append(dataPart);
                continue;
            }

            if (line.trim().length() == 0) {
                ParsedEventState parsed = processCompletedSseEvent(
                        currentEvent,
                        currentData.toString().trim(),
                        finalResponseCompletedText,
                        fallbackOutputItemDoneText,
                        fallbackContentPartDoneText,
                        deltaAccumulator
                );

                finalResponseCompletedText = parsed.finalResponseCompletedText;
                fallbackOutputItemDoneText = parsed.fallbackOutputItemDoneText;
                fallbackContentPartDoneText = parsed.fallbackContentPartDoneText;

                currentEvent = "";
                currentData.setLength(0);
            }
        }

        if (currentData.length() > 0) {
            ParsedEventState parsed = processCompletedSseEvent(
                    currentEvent,
                    currentData.toString().trim(),
                    finalResponseCompletedText,
                    fallbackOutputItemDoneText,
                    fallbackContentPartDoneText,
                    deltaAccumulator
            );

            finalResponseCompletedText = parsed.finalResponseCompletedText;
            fallbackOutputItemDoneText = parsed.fallbackOutputItemDoneText;
            fallbackContentPartDoneText = parsed.fallbackContentPartDoneText;
        }

        if (!isBlank(finalResponseCompletedText)) {
            return finalResponseCompletedText.trim();
        }

        if (!isBlank(fallbackOutputItemDoneText)) {
            return fallbackOutputItemDoneText.trim();
        }

        if (!isBlank(fallbackContentPartDoneText)) {
            return fallbackContentPartDoneText.trim();
        }

        if (deltaAccumulator.length() > 0) {
            return deltaAccumulator.toString().trim();
        }

        return "";
    }

    /* * Feature: interpreta cada bloco SSE fechado por linha em branco. * Data: 2026-05-27 03:20 * Caller: * - extractPrimaryTextFromSse * Chama: * - normalizeEventName * - readString * - extractResponseCompletedText * - extractOutputItemDoneText * - extractContentPartDoneText * Objetivo: * - Suportar tanto o nome real do servidor quanto a variante com pontos */
    private ParsedEventState processCompletedSseEvent(String currentEvent, String dataJson, String finalResponseCompletedText, String fallbackOutputItemDoneText, String fallbackContentPartDoneText, StringBuilder deltaAccumulator) {

        if (isBlank(dataJson)) {
            return new ParsedEventState(
                    finalResponseCompletedText,
                    fallbackOutputItemDoneText,
                    fallbackContentPartDoneText
            );
        }

        System.out.println("[STREAMING SSE CODEC] closingEvent=" + normalizeEventName(currentEvent));
        System.out.println("[STREAMING SSE CODEC] eventPayload=");
        System.out.println(dataJson);

        try {
            JsonElement parsedElement = JsonParser.parseString(dataJson);
            if (!parsedElement.isJsonObject()) {
                return new ParsedEventState(
                        finalResponseCompletedText,
                        fallbackOutputItemDoneText,
                        fallbackContentPartDoneText
                );
            }

            JsonObject json = parsedElement.getAsJsonObject();

            String type = readString(json, "type");
            String eventName = !isBlank(currentEvent) ? currentEvent : type;
            String normalizedEventName = normalizeEventName(eventName);
            String normalizedType = normalizeEventName(type);

            if ("responsecompleted".equals(normalizedEventName) || "responsecompleted".equals(normalizedType)) {
                String text = extractResponseCompletedText(json);
                if (!isBlank(text)) {
                    finalResponseCompletedText = text;
                }
            }

            if ("outputitemdone".equals(normalizedEventName) || "outputitemdone".equals(normalizedType)) {
                String text = extractOutputItemDoneText(json);
                if (!isBlank(text)) {
                    fallbackOutputItemDoneText = text;
                }
            }

            if ("contentpartdone".equals(normalizedEventName) || "contentpartdone".equals(normalizedType)) {
                String text = extractContentPartDoneText(json);
                if (!isBlank(text)) {
                    fallbackContentPartDoneText = text;
                }
            }

            if ("outputtextdelta".equals(normalizedEventName) || "outputtextdelta".equals(normalizedType)) {
                String delta = readString(json, "delta");
                if (!isBlank(delta)) {
                    deltaAccumulator.append(delta);
                }
            }
        } catch (Exception exception) {
            System.out.println("[STREAMING SSE CODEC] falha ao processar evento SSE: " + exception.getMessage());
        }

        return new ParsedEventState(
                finalResponseCompletedText,
                fallbackOutputItemDoneText,
                fallbackContentPartDoneText
        );
    }

    /** * Caller: * - extractPrimaryTextFromJsonEnvelope * - processCompletedSseEvent * * Chama: * - appendTextsFromItemsArray * * Objetivo: * - Extrair o texto consolidado do evento ResponseCompleted * * Alterado em: 2026-05-27 03:45 */
    /* * Feature: extrai o texto final consolidado. * Data: 2026-05-27 03:20 * Caller: * - extractPrimaryTextFromJsonBody * - processCompletedSseEvent * Chama: * - appendTextsFromItemsArray * - appendTextsFromOutputArray * Objetivo: * - Cobrir items e response.output do contrato real */
    private String extractResponseCompletedText(JsonObject json) {
        if (json == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        appendTextsFromItemsArray(builder, json.get("items"));
        appendTextsFromOutputArray(builder, json.get("output"));

        if (builder.length() == 0 && json.has("response") && json.get("response").isJsonObject()) {
            JsonObject responseObject = json.getAsJsonObject("response");
            appendTextsFromItemsArray(builder, responseObject.get("items"));
            appendTextsFromOutputArray(builder, responseObject.get("output"));
        }

        return builder.toString().trim();
    }

    /** * Caller: * - extractPrimaryTextFromJsonEnvelope * - processCompletedSseEvent * * Chama: * - appendTextsFromContentArray * * Objetivo: * - Extrair o texto consolidado do evento OutputItemDone * * Alterado em: 2026-05-27 03:45 */
    /* * Feature: fallback por item concluido. * Data: 2026-05-27 03:20 * Caller: * - extractPrimaryTextFromJsonBody * - processCompletedSseEvent * Chama: * - appendTextsFromContentArray * Objetivo: * - Aproveitar item.content e item.raw_item.content */
    private String extractOutputItemDoneText(JsonObject json) {
        if (json == null || !json.has("item") || !json.get("item").isJsonObject()) {
            return "";
        }

        JsonObject itemObject = json.getAsJsonObject("item");
        StringBuilder builder = new StringBuilder();

        appendPreferredTextsFromItemObject(builder, itemObject);

        return builder.toString().trim();
    }
    private void appendPreferredTextsFromItemObject(StringBuilder builder, JsonObject itemObject) {
        if (builder == null || itemObject == null) {
            return;
        }

        int builderSizeBeforeMainContent = builder.length();
        appendTextsFromContentArray(builder, itemObject.get("content"));

        if (builder.length() > builderSizeBeforeMainContent) {
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
    

    /* * Feature: fallback por parte concluida. * Data: 2026-05-27 03:20 * Caller: * - extractPrimaryTextFromJsonBody * - processCompletedSseEvent * Chama: * - readString * Objetivo: * - Recuperar o texto integral entregue no fechamento da parte */
    private String extractContentPartDoneText(JsonObject json) {
        if (json == null || !json.has("part") || !json.get("part").isJsonObject()) {
            return "";
        }

        JsonObject partObject = json.getAsJsonObject("part");
        return readString(partObject, "text");
    }
    /* * Feature: extrai textos de arrays output. * Data: 2026-05-27 03:20 * Caller: * - extractResponseCompletedText * Chama: * - appendTextsFromContentArray * Objetivo: * - Cobrir o JSON consolidado com response.output do endpoint real */
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

    /** * Caller: * - extractPrimaryTextFromJsonEnvelope * - extractResponseCompletedText * * Chama: * - appendTextsFromContentArray * * Objetivo: * - Percorrer items[].content[] e raw_item.content[] * * Alterado em: 2026-05-27 03:45 */
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

            int beforeMainContent = builder.length();
            appendTextsFromContentArray(builder, itemObject.get("content"));

            /* * Importante: * raw_item.content normalmente duplica o content principal. * So use raw_item como fallback quando content principal nao trouxe texto. */
            if (builder.length() > beforeMainContent) {
                continue;
            }

            if (itemObject.has("raw_item") && itemObject.get("raw_item").isJsonObject()) {
                JsonObject rawItemObject = itemObject.getAsJsonObject("raw_item");
                appendTextsFromContentArray(builder, rawItemObject.get("content"));
            }
        }
    }

    /* * Feature: extrai textos de content[]. * Data: 2026-05-27 03:20 * Caller: * - appendTextsFromItemsArray * - appendTextsFromOutputArray * - extractOutputItemDoneText * Objetivo: * - Ler output_text.text sem perder textos sem tipo explicito */
    private void appendTextsFromContentArray(StringBuilder builder, JsonElement contentElement) {
        if (builder == null || contentElement == null || !contentElement.isJsonArray()) {
            return;
        }

        JsonArray contentArray = contentElement.getAsJsonArray();

        int index = 0;
        while (index < contentArray.size()) {
            JsonElement contentItemElement = contentArray.get(index);
            if (contentItemElement.isJsonObject()) {
                JsonObject contentObject = contentItemElement.getAsJsonObject();

                String contentType = readString(contentObject, "type");
                String text = readString(contentObject, "text");

                if (!isBlank(text)) {
                    if ("output_text".equalsIgnoreCase(contentType)
                            || "text".equalsIgnoreCase(contentType)
                            || isBlank(contentType)) {
                        builder.append(text);
                    }
                }
            }

            index++;
        }
    }

    /* * Feature: leitura defensiva de string em JSON. * Data: 2026-05-27 03:20 * Caller: * - multiplos metodos internos desta classe * Chama: * - nenhuma * Objetivo: * - Evitar quebra de parse por campos inesperados */
    private String readString(JsonObject json, String propertyName) {
        if (json == null || propertyName == null || !json.has(propertyName) || json.get(propertyName).isJsonNull()) {
            return "";
        }

        try {
            return json.get(propertyName).getAsString();
        } catch (Exception exception) {
            return "";
        }
    }

    /** * Caller: * - processCompletedSseEvent * - extractPrimaryTextFromJsonEnvelope * * Objetivo: * - Padronizar nomes de evento para comparacao segura * * Alterado em: 2026-05-27 03:45 */
    /* * Feature: normaliza nomes de evento SSE. * Data: 2026-05-27 03:20 * Caller: * - processCompletedSseEvent * Chama: * - nenhuma * Objetivo: * - Tornar equivalente o nome real do servidor e a variante com pontos */
    private String normalizeEventName(String value) {
        if (value == null) {
            return "";
        }

        String trimmedValue = value.trim().toLowerCase();
        StringBuilder normalizedBuilder = new StringBuilder(trimmedValue.length());

        int index = 0;
        while (index < trimmedValue.length()) {
            char currentChar = trimmedValue.charAt(index);
            if ((currentChar >= 'a' && currentChar <= 'z') || (currentChar >= '0' && currentChar <= '9')) {
                normalizedBuilder.append(currentChar);
            }
            index++;
        }

        return normalizedBuilder.toString();
    }
    

    /** * Caller: * - decode * * Objetivo: * - Detectar se o corpo veio como json unico em vez de SSE linha a linha * * Alterado em: 2026-05-27 03:45 */
    private boolean looksLikeJson(String rawBody) {
        if (isBlank(rawBody)) {
            return false;
        }

        String trimmedBody = rawBody.trim();
        return trimmedBody.startsWith("{") || trimmedBody.startsWith("[");
    }

    /** * Caller: * - decode * - extractPrimaryTextFromJsonEnvelope * - extractPrimaryTextFromSse * - processCompletedSseEvent * * Objetivo: * - Teste simples de branco * * Alterado em: 2026-05-27 03:45 */
    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    /** * Caller: * - decode * - processCompletedSseEvent * * Objetivo: * - Limitar rastreabilidade para nao poluir o console em excesso * * Alterado em: 2026-05-27 03:45 */
    private String truncateForDebug(String value, int maxLength) {
        if (value == null) {
            return "null";
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength) + "... [TRUNCATED]";
    }

    /** * Objetivo: * - Transportar o melhor estado consolidado ao fechar cada evento SSE */
    private static class ParsedEventState {

        private final String finalResponseCompletedText;
        private final String fallbackOutputItemDoneText;
        private final String fallbackContentPartDoneText;

        private ParsedEventState(String finalResponseCompletedText, String fallbackOutputItemDoneText, String fallbackContentPartDoneText) {
            this.finalResponseCompletedText = finalResponseCompletedText != null ? finalResponseCompletedText : "";
            this.fallbackOutputItemDoneText = fallbackOutputItemDoneText != null ? fallbackOutputItemDoneText : "";
            this.fallbackContentPartDoneText = fallbackContentPartDoneText != null ? fallbackContentPartDoneText : "";
        }

        private String getFinalResponseCompletedText() {
            return finalResponseCompletedText;
        }

        private String getFallbackOutputItemDoneText() {
            return fallbackOutputItemDoneText;
        }

        private String getFallbackContentPartDoneText() {
            return fallbackContentPartDoneText;
        }
    }
    
}