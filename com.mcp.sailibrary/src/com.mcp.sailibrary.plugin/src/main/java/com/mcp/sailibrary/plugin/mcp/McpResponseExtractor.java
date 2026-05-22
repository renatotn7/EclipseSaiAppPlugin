package com.mcp.sailibrary.plugin.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** * Extrai e normaliza conteudos relevantes das respostas MCP. * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class McpResponseExtractor {

    /** * Extrai o texto principal de uma resposta MCP quando disponivel. * * @param rawResponse resposta bruta * @return texto principal extraido ou a propria resposta original quando a * estrutura esperada nao estiver presente * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String extractPrimaryText(String rawResponse) {
        if (rawResponse == null || rawResponse.trim().length() == 0) {
            return "";
        }

        String text = rawResponse.trim();

        try {
            JsonObject envelope = JsonParser.parseString(text).getAsJsonObject();
            if (envelope.has("result") && envelope.get("result").isJsonObject()) {
                JsonObject result = envelope.getAsJsonObject("result");
                if (result.has("content") && result.get("content").isJsonArray()) {
                    JsonArray contentArray = result.getAsJsonArray("content");
                    if (contentArray.size() > 0 && contentArray.get(0).isJsonObject()) {
                        JsonObject firstContent = contentArray.get(0).getAsJsonObject();
                        if (firstContent.has("text") && !firstContent.get("text").isJsonNull()) {
                            return firstContent.get("text").getAsString();
                        }
                    }
                }
            }
        } catch (Exception e) {
        }

        return text;
    }

    /** * Extrai e normaliza sugestao de nome curto para bloco/contexto. * * @param rawResponse resposta bruta do MCP * @return nome sugerido normalizado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String extractSuggestedBlockName(String rawResponse) {
        String text = extractPrimaryText(rawResponse);
        if (text == null || text.trim().length() == 0) {
            return "";
        }

        text = text.trim();

        if (text.contains("\n")) {
            text = text.substring(0, text.indexOf('\n')).trim();
        }

        text = text.replace("\"", "").replace("'", "").trim();
        text = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD);
        text = text.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        text = text.toLowerCase();
        text = text.replaceAll("[^a-z0-9]", "");

        if (text.length() > 12) {
            text = text.substring(0, 12);
        }

        return text;
    }
}