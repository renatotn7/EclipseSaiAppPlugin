package com.mcp.sailibrary.plugin.mcp;

import java.text.Normalizer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcp.sailibrary.plugin.mcp.extractor.McpStructuredResponseTextExtractor;

/** * Feature: fachada para extracao de texto relevante em respostas MCP. * * Libs: * - JDK 11+ * - Gson 2.x * * Objetivo: * - detectar o formato da resposta * - manter a API publica estavel * - delegar parsing estruturado para classe especializada * * Observacao: * - esta classe permanece pequena para facilitar manutencao * - o parse detalhado de SSE e JSON direto foi movido para um helper * * Data de ajuste: 2026-05-27 12:10 * * Autor: Renato Tomaz Nati * Since: 2026-05-26 */
public class McpResponseExtractor {

    private final McpStructuredResponseTextExtractor structuredResponseTextExtractor;

    /* * Feature: detecta o formato da resposta antes do parse especifico. * Data: 2026-05-27 03:20 * Caller: * - servicos MCP e testes * Chama: * - looksLikeSse * - looksLikeJsonObject * - extractPrimaryTextFromLegacyEnvelope * - McpStructuredResponseTextExtractor.extractPrimaryTextFromStreamingSse * - McpStructuredResponseTextExtractor.extractPrimaryTextFromDirectJson * Objetivo: * - Reduzir falso erro de parse e escolher a estrategia correta logo no inicio */
    public String extractPrimaryText(String rawResponse) {
        System.out.println("[MCP EXTRACTOR] extractPrimaryText input=");
        System.out.println(rawResponse);

        if (rawResponse == null || rawResponse.trim().length() == 0) {
            System.out.println("[MCP EXTRACTOR] extractPrimaryText output vazio");
            return "";
        }

        String trimmedResponse = rawResponse.trim();

        if (looksLikeSse(trimmedResponse)) {
            String streamingText = structuredResponseTextExtractor.extractPrimaryTextFromStreamingSse(trimmedResponse);
            System.out.println("[MCP EXTRACTOR] extractPrimaryTextFromStreamingSse output=");
            System.out.println(streamingText);
            if (!isBlank(streamingText)) {
                System.out.println("[MCP EXTRACTOR] extractPrimaryText output streaming=");
                System.out.println(streamingText);
                return streamingText;
            }
        }

        if (looksLikeJsonObject(trimmedResponse)) {
            String legacyText = extractPrimaryTextFromLegacyEnvelope(trimmedResponse);
            if (!isBlank(legacyText)) {
                System.out.println("[MCP EXTRACTOR] extractPrimaryText output legado=");
                System.out.println(legacyText);
                return legacyText;
            }

            String directJsonText = structuredResponseTextExtractor.extractPrimaryTextFromDirectJson(trimmedResponse);
            System.out.println("[MCP EXTRACTOR] extractPrimaryTextFromDirectJson output=");
            System.out.println(directJsonText);
            if (!isBlank(directJsonText)) {
                System.out.println("[MCP EXTRACTOR] extractPrimaryText output jsonDireto=");
                System.out.println(directJsonText);
                return directJsonText;
            }
        }

        System.out.println("[MCP EXTRACTOR] extractPrimaryText output fallback=");
        System.out.println(trimmedResponse);
        return trimmedResponse;
    }

    /* * Feature: extrai texto do envelope legado MCP. * Data: 2026-05-27 03:20 * Caller: * - extractPrimaryText * Chama: * - looksLikeJsonObject * Objetivo: * - Tentar o parse legado apenas quando a entrada realmente puder ser JSON objeto */
    public String extractPrimaryTextFromLegacyEnvelope(String rawResponse) {
        System.out.println("[MCP EXTRACTOR] extractPrimaryTextFromLegacyEnvelope input=");
        System.out.println(rawResponse);

        if (isBlank(rawResponse) || !looksLikeJsonObject(rawResponse) || rawResponse.indexOf("\"result\"") < 0) {
            return "";
        }

        try {
            JsonObject envelope = JsonParser.parseString(rawResponse).getAsJsonObject();

            if (envelope.has("result") && envelope.get("result").isJsonObject()) {
                JsonObject resultObject = envelope.getAsJsonObject("result");

                if (resultObject.has("content") && resultObject.get("content").isJsonArray()) {
                    JsonArray contentArray = resultObject.getAsJsonArray("content");

                    if (contentArray.size() > 0 && contentArray.get(0).isJsonObject()) {
                        JsonObject firstContentObject = contentArray.get(0).getAsJsonObject();

                        if (firstContentObject.has("text") && !firstContentObject.get("text").isJsonNull()) {
                            String extractedText = firstContentObject.get("text").getAsString();
                            System.out.println("[MCP EXTRACTOR] extractPrimaryTextFromLegacyEnvelope output=");
                            System.out.println(extractedText);
                            return extractedText;
                        }
                    }
                }
            }
        } catch (Exception exception) {
            System.out.println("[MCP EXTRACTOR] extractPrimaryTextFromLegacyEnvelope falha parse: " + exception.getMessage());
        }

        return "";
    }
    /* * Feature: identifica json objeto de forma barata. * Data: 2026-05-27 03:20 * Caller: * - extractPrimaryText * - extractPrimaryTextFromLegacyEnvelope * Chama: * - nenhuma * Objetivo: * - Evitar tentativa de parse legado em SSE e texto puro */
    private boolean looksLikeJsonObject(String rawResponse) {
        if (isBlank(rawResponse)) {
            return false;
        }

        String trimmedResponse = rawResponse.trim();
        return trimmedResponse.startsWith("{") && trimmedResponse.endsWith("}");
    }
    /* * Feature: identifica se o texto bruto parece seguir protocolo SSE. * Data: 2026-05-27 03:21 * Chamado por: * - extractPrimaryText * - fluxos de autodeteccao do extrator * Chama: * - isBlank * Objetivo: * - diferenciar stream SSE de json direto e de texto puro */
    private boolean looksLikeSse(String rawResponse) {
        if (isBlank(rawResponse)) {
            return false;
        }

        boolean hasEvent = rawResponse.contains("event:");
        boolean hasData = rawResponse.contains("data:");

        System.out.println("[MCP EXTRACTOR] looksLikeSse=" + (hasEvent && hasData));
        System.out.println("[MCP EXTRACTOR] looksLikeSse possuiEvent=" + hasEvent);
        System.out.println("[MCP EXTRACTOR] looksLikeSse possuiData=" + hasData);

        return hasEvent && hasData;
    }
    /** * Data de ajuste: 2026-05-27 12:10 * Chamado por: * - extractPrimaryText * - testes de cobertura * * Chama: * - McpStructuredResponseTextExtractor.extractPrimaryTextFromStreamingSse * * Objetivo: * - manter a assinatura publica * - delegar parse SSE para classe menor e especializada */
    public String extractPrimaryTextFromStreamingSse(String rawResponse) {
        String extractedText = structuredResponseTextExtractor.extractPrimaryTextFromStreamingSse(rawResponse);

        System.out.println("[MCP EXTRACTOR] extractPrimaryTextFromStreamingSse output=");
        System.out.println(extractedText);

        return extractedText;
    }

    /** * Data de ajuste: 2026-05-27 12:10 * Chamado por: * - extractPrimaryText * - testes de cobertura * * Chama: * - McpStructuredResponseTextExtractor.extractPrimaryTextFromDirectJson * * Objetivo: * - manter a assinatura publica * - delegar parse de json direto para classe menor e especializada */
    public String extractPrimaryTextFromDirectJson(String rawResponse) {
        String extractedText = structuredResponseTextExtractor.extractPrimaryTextFromDirectJson(rawResponse);

        System.out.println("[MCP EXTRACTOR] extractPrimaryTextFromDirectJson output=");
        System.out.println(extractedText);

        return extractedText;
    }

    /** * Data de ajuste: 2026-05-27 12:10 * Chamado por: * - SaiLibraryMcpClient * - fluxo de sugestao de nome de bloco * - testes de cobertura * * Chama: * - extractExplicitSuggestedBlockName * - extractPrimaryText * - sanitizeBlockName * * Objetivo: * - priorizar campos de nome em respostas JSON * - cair para o texto principal quando nao houver campo explicito */
    public String extractSuggestedBlockName(String rawResponse) {
        System.out.println("[MCP EXTRACTOR] extractSuggestedBlockName input=");
        System.out.println(rawResponse);

        String suggestedName = extractExplicitSuggestedBlockName(rawResponse);
        if (isBlank(suggestedName)) {
            suggestedName = extractPrimaryText(rawResponse);
        }

        String sanitizedName = sanitizeBlockName(suggestedName);

        System.out.println("[MCP EXTRACTOR] extractSuggestedBlockName output=");
        System.out.println(sanitizedName);

        return sanitizedName;
    }

    /** * Data de ajuste: 2026-05-27 12:10 * Chamado por: * - extractSuggestedBlockName * - testes de cobertura * * Chama: * - Normalizer.normalize * * Objetivo: * - reduzir o nome para um identificador curto e seguro */
    public String sanitizeBlockName(String text) {
        if (isBlank(text)) {
            return "";
        }

        String normalizedText = text.trim();

        if (normalizedText.indexOf('\n') >= 0) {
            normalizedText = normalizedText.substring(0, normalizedText.indexOf('\n')).trim();
        }

        normalizedText = normalizedText.replace("\"", "");
        normalizedText = normalizedText.replace("'", "");
        normalizedText = normalizedText.trim();

        normalizedText = Normalizer.normalize(normalizedText, Normalizer.Form.NFD);
        normalizedText = normalizedText.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        normalizedText = normalizedText.toLowerCase();
        normalizedText = normalizedText.replaceAll("[^a-z0-9]", "");

        if (normalizedText.length() > 12) {
            normalizedText = normalizedText.substring(0, 12);
        }

        return normalizedText;
    }

    /** * Data de ajuste: 2026-05-27 12:10 * Chamado por: * - construtor da propria classe * * Chama: * - McpStructuredResponseTextExtractor * * Objetivo: * - inicializar dependencia interna sem expor detalhes para os chamadores */
    public McpResponseExtractor() {
        this.structuredResponseTextExtractor = new McpStructuredResponseTextExtractor();
    }

    /** * Data de ajuste: 2026-05-27 12:10 * Chamado por: * - extractSuggestedBlockName * * Chama: * - Gson JsonParser * - readString * * Objetivo: * - ler nome explicito de respostas estruturadas */
    private String extractExplicitSuggestedBlockName(String rawResponse) {
        if (isBlank(rawResponse)) {
            return "";
        }

        try {
            JsonElement rootElement = JsonParser.parseString(rawResponse);
            if (!rootElement.isJsonObject()) {
                return "";
            }

            JsonObject rootObject = rootElement.getAsJsonObject();

            String[] candidateKeys = new String[] {
                    "suggestedBlockName",
                    "suggestedName",
                    "blockName",
                    "name"
            };

            int index = 0;
            while (index < candidateKeys.length) {
                String candidateValue = readString(rootObject, candidateKeys[index]);
                if (!isBlank(candidateValue)) {
                    return candidateValue;
                }
                index++;
            }

            return "";
        } catch (Exception exception) {
            System.out.println("[MCP EXTRACTOR] extractExplicitSuggestedBlockName falha parse: "
                    + exception.getMessage());
            return "";
        }
    }

    /** * Data de ajuste: 2026-05-27 12:10 * Chamado por: * - extractPrimaryTextFromLegacyEnvelope * - extractExplicitSuggestedBlockName * * Chama: * - JsonObject.get * * Objetivo: * - ler string de forma defensiva */
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

    /** * Data de ajuste: 2026-05-27 12:10 * Chamado por: * - varios metodos desta classe * * Objetivo: * - validar string vazia de forma defensiva */
    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}