package com.mcp.sailibrary.plugin.chat.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.IDocument;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcp.sailibrary.plugin.chat.support.AiResponse;

public class McpAgentResponseService {

    private static final int MAX_DEBUG_TEXT_LENGTH = 12000;

    private final Gson gson;

    public McpAgentResponseService() {
        this.gson = new Gson();
    }

    public AiResponse buildInfrastructureFailureResponse(String mensagem, boolean devePerguntarUsuario) {
        AiResponse resposta = new AiResponse();

        if (devePerguntarUsuario) {
            resposta.setAction("perguntar_ao_usuario");
            resposta.setQuestion("Percebi uma falha tecnica de infraestrutura do provider/modelo. Deseja que eu tente novamente ou investigue a causa?");
            resposta.setExplanation(safeTrim(mensagem));
            return resposta;
        }

        resposta.setAction("responder_ao_usuario");
        resposta.setContent("Percebi uma falha tecnica do modelo durante a execucao. Nao tratei isso como resposta valida da IA.");
        resposta.setExplanation(safeTrim(mensagem));
        return resposta;
    }

    public boolean isModelInfrastructureFailureText(String texto) {
        String normalizado = safeTrim(texto).toLowerCase();

        if (normalizado.length() == 0) {
            return false;
        }

        if (normalizado.contains("connection refused")) {
            return true;
        }

        if (normalizado.contains("connection reset")) {
            return true;
        }

        if (normalizado.contains("read timed out")) {
            return true;
        }

        if (normalizado.contains("timeout")) {
            return true;
        }

        if (normalizado.contains("service unavailable")) {
            return true;
        }

        if (normalizado.contains("bad gateway")) {
            return true;
        }

        if (normalizado.contains("gateway timeout")) {
            return true;
        }

        if (normalizado.contains("status 302")) {
            return true;
        }

        if (normalizado.contains("status 401")) {
            return true;
        }

        if (normalizado.contains("status 403")) {
            return true;
        }

        if (normalizado.contains("status 500")) {
            return true;
        }

        if (normalizado.contains("status 502")) {
            return true;
        }

        if (normalizado.contains("status 503")) {
            return true;
        }

        if (normalizado.contains("status 504")) {
            return true;
        }

        if (normalizado.contains("falha tecnica de infraestrutura do provider/modelo")) {
            return true;
        }

        if (normalizado.contains("falha tecnica do provider/modelo")) {
            return true;
        }

        if (normalizado.contains("falha de infraestrutura do provider/modelo")) {
            return true;
        }

        if (normalizado.contains("resposta malformada do modelo")) {
            return true;
        }

        if (normalizado.contains("falha ao interpretar a resposta estruturada do modelo")) {
            return true;
        }

        return false;
    }

    public AiResponse interpretarRespostaIA(String textResponse) {
        String textoNormalizado = normalizarTextoResposta(textResponse);

        if (textoNormalizado.length() == 0) {
            return null;
        }

        try {
            String textoJson = extrairBlocoJsonMaisConfiavel(textoNormalizado);

            if (textoJson.length() == 0) {
                System.out.println("[MCP PARSE DEBUG] Nenhum JSON estruturado encontrado na resposta IA.");
                System.out.println("[MCP PARSE DEBUG] respostaPreview=" + truncateForDebug(textoNormalizado, MAX_DEBUG_TEXT_LENGTH));
                return null;
            }

            if (textoJson.startsWith("```")) {
                textoJson = removerMarcadorMarkdownCodigo(textoJson);
            }

            System.out.println("[MCP PARSE DEBUG] textoNormalizadoParaJson=" + truncateForDebug(textoJson, MAX_DEBUG_TEXT_LENGTH));

            JsonElement elemento = JsonParser.parseString(textoJson);
            if (elemento == null || !elemento.isJsonObject()) {
                System.out.println("[MCP PARSE DEBUG] JSON encontrado nao e objeto.");
                return null;
            }

            JsonObject json = elemento.getAsJsonObject();

            AiResponse resposta = new AiResponse();
            resposta.setAction(readString(json, "action"));
            resposta.setContent(readString(json, "content"));
            resposta.setExplanation(readString(json, "explanation"));
            resposta.setTool(readString(json, "tool"));
            resposta.setQuestion(readString(json, "question"));

            if (json.has("parameters") && json.get("parameters").isJsonObject()) {
                resposta.setParameters(copyParameters(json.getAsJsonObject("parameters")));
            } else {
                resposta.setParameters(new JsonObject());
            }

            return resposta;
        } catch (Exception e) {
            System.out.println("[MCP PARSE DEBUG] Falha ao interpretar resposta IA: " + e.getMessage());
            System.out.println("[MCP PARSE DEBUG] respostaPreview=" + truncateForDebug(textoNormalizado, MAX_DEBUG_TEXT_LENGTH));
            e.printStackTrace();
            return null;
        }
    }

    public AiResponse normalizarProtocoloFerramentaLegado(AiResponse resposta) {
        if (resposta == null) {
            return null;
        }

        String action = safeTrim(resposta.getAction());
        String tool = safeTrim(resposta.getTool());
        String question = safeTrim(resposta.getQuestion());

        if ("usar_ferramenta".equalsIgnoreCase(action)) {
            resposta.setAction("executar_ferramenta");
            return resposta;
        }

        if (action.length() == 0 && tool.length() > 0) {
            resposta.setAction("executar_ferramenta");
            return resposta;
        }

        if (action.length() == 0 && question.length() > 0) {
            resposta.setAction("perguntar_ao_usuario");
            return resposta;
        }

        return resposta;
    }

    public boolean respostaEstruturadaValida(AiResponse resposta) {
        if (resposta == null) {
            return false;
        }

        String action = safeTrim(resposta.getAction()).toLowerCase();

        if (action.length() == 0) {
            return false;
        }

        if ("executar_ferramenta".equals(action)) {
            return safeTrim(resposta.getTool()).length() > 0;
        }

        if ("perguntar_ao_usuario".equals(action)) {
            return safeTrim(resposta.getQuestion()).length() > 0
                    || safeTrim(resposta.getExplanation()).length() > 0;
        }

        if ("substituir".equals(action)
                || "comentar".equals(action)
                || "explicar".equals(action)
                || "responder_ao_usuario".equals(action)
                || "inserir_abaixo".equals(action)
                || "anexar_acima".equals(action)) {
            return safeTrim(resposta.getContent()).length() > 0
                    || safeTrim(resposta.getExplanation()).length() > 0;
        }

        return true;
    }

    public String montarPerguntaAoUsuario(AiResponse resposta) {
        if (resposta == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        if (safeTrim(resposta.getQuestion()).length() > 0) {
            builder.append(resposta.getQuestion().trim());
        }

        if (safeTrim(resposta.getExplanation()).length() > 0) {
            if (builder.length() > 0) {
                builder.append(System.lineSeparator()).append(System.lineSeparator());
            }
            builder.append(resposta.getExplanation().trim());
        }

        return builder.toString();
    }

    public String formatarRespostaIA(AiResponse resposta, IDocument document) {
        if (resposta == null) {
            return "";
        }

        String action = safeTrim(resposta.getAction()).toLowerCase();

        if ("perguntar_ao_usuario".equals(action)) {
            return montarPerguntaAoUsuario(resposta);
        }

        if ("executar_ferramenta".equals(action)) {
            StringBuilder builder = new StringBuilder();
            builder.append("Ferramenta solicitada: ").append(safeTrim(resposta.getTool()));

            if (safeTrim(resposta.getExplanation()).length() > 0) {
                builder.append(System.lineSeparator());
                builder.append(resposta.getExplanation().trim());
            }

            if (resposta.getParameters() != null && !resposta.getParameters().isEmpty()) {
                builder.append(System.lineSeparator());
                builder.append(serializarParametrosFerramenta(resposta.getParameters()));
            }

            return builder.toString();
        }

        if (safeTrim(resposta.getContent()).length() > 0) {
            return resposta.getContent().trim();
        }

        if (safeTrim(resposta.getExplanation()).length() > 0) {
            return resposta.getExplanation().trim();
        }

        return "";
    }

    public String serializarParametrosFerramenta(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return "{}";
        }

        try {
            return gson.toJson(parameters);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String normalizarTextoResposta(String textoResposta) {
        String texto = textoResposta != null ? textoResposta : "";

        texto = texto.replace("\uFEFF", "");
        texto = texto.replace("\u200B", "");
        texto = texto.replace("\r\n", "\n").replace('\r', '\n');
        texto = texto.trim();

        return texto;
    }

    private String extrairBlocoJsonMaisConfiavel(String texto) {
        List<String> candidatos = new ArrayList<String>();

        List<String> blocosCodigoFinal = extrairBlocosPorTag(texto, "<codigo_final>", "</codigo_final>");
        for (int i = 0; i < blocosCodigoFinal.size(); i++) {
            String candidato = blocosCodigoFinal.get(i);
            if (isStructuredJson(candidato)) {
                candidatos.add(candidato);
            }
        }

        if (!candidatos.isEmpty()) {
            return escolherUltimoBlocoDistinto(candidatos);
        }

        if (isStructuredJson(texto)) {
            return texto;
        }

        String jsonAvulso = extrairPrimeiroJsonComAction(texto);
        if (isStructuredJson(jsonAvulso)) {
            return jsonAvulso;
        }

        return "";
    }

    private List<String> extrairBlocosPorTag(String texto, String tagInicio, String tagFim) {
        List<String> blocos = new ArrayList<String>();
        if (texto == null || tagInicio == null || tagFim == null) {
            return blocos;
        }

        int cursor = 0;
        while (cursor >= 0 && cursor < texto.length()) {
            int inicio = texto.indexOf(tagInicio, cursor);
            if (inicio < 0) {
                break;
            }

            int conteudoInicio = inicio + tagInicio.length();
            int fim = texto.indexOf(tagFim, conteudoInicio);
            if (fim < 0) {
                break;
            }

            String bloco = texto.substring(conteudoInicio, fim).trim();
            if (bloco.length() > 0) {
                blocos.add(bloco);
            }

            cursor = fim + tagFim.length();
        }

        return blocos;
    }

    private String escolherUltimoBlocoDistinto(List<String> candidatos) {
        if (candidatos == null || candidatos.isEmpty()) {
            return "";
        }

        String ultimoNormalizado = "";
        for (int i = candidatos.size() - 1; i >= 0; i--) {
            String atual = safeTrim(candidatos.get(i));
            if (atual.length() == 0) {
                continue;
            }

            String atualNormalizado = removerEspacosParaComparacao(atual);
            if (ultimoNormalizado.length() == 0) {
                ultimoNormalizado = atualNormalizado;
                return atual;
            }

            if (!ultimoNormalizado.equals(atualNormalizado)) {
                return atual;
            }
        }

        return safeTrim(candidatos.get(candidatos.size() - 1));
    }
    private String removerEspacosParaComparacao(String value) {
        if (value == null || value.length() == 0) {
            return "";
        }

        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!Character.isWhitespace(c)) {
                builder.append(c);
            }
        }

        return builder.toString();
    }
    private String removerMarcadorMarkdownCodigo(String texto) {
        String valor = safeTrim(texto);

        if (!valor.startsWith("```")) {
            return valor;
        }

        int primeiraQuebra = valor.indexOf('\n');
        if (primeiraQuebra >= 0) {
            valor = valor.substring(primeiraQuebra + 1);
        }

        int ultimoBloco = valor.lastIndexOf("```");
        if (ultimoBloco >= 0) {
            valor = valor.substring(0, ultimoBloco);
        }

        return valor.trim();
    }

    private String extrairPrimeiroJsonComAction(String texto) {
        if (texto == null) {
            return "";
        }

        int inicio = texto.indexOf('{');
        while (inicio >= 0 && inicio < texto.length()) {
            int profundidade = 0;
            boolean emString = false;
            boolean escape = false;

            for (int i = inicio; i < texto.length(); i++) {
                char c = texto.charAt(i);

                if (escape) {
                    escape = false;
                    continue;
                }

                if (c == '\\') {
                    escape = true;
                    continue;
                }

                if (c == '"') {
                    emString = !emString;
                    continue;
                }

                if (emString) {
                    continue;
                }

                if (c == '{') {
                    profundidade++;
                } else if (c == '}') {
                    profundidade--;
                    if (profundidade == 0) {
                        String candidato = texto.substring(inicio, i + 1).trim();
                        if (isStructuredJson(candidato)) {
                            return candidato;
                        }
                        break;
                    }
                }
            }

            inicio = texto.indexOf('{', inicio + 1);
        }

        return "";
    }

    private boolean isStructuredJson(String texto) {
        String candidato = safeTrim(texto);
        if (candidato.length() == 0) {
            return false;
        }

        try {
            JsonElement parsedElement = JsonParser.parseString(candidato);
            if (!parsedElement.isJsonObject()) {
                return false;
            }

            JsonObject json = parsedElement.getAsJsonObject();

            if (json.has("action")) {
                return true;
            }

            if (json.has("tool")) {
                return true;
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private JsonObject copyParameters(JsonObject originalParameters) {
        if (originalParameters == null) {
            return new JsonObject();
        }

        try {
            return originalParameters.deepCopy();
        } catch (Exception e) {
            System.out.println("[MCP PARSE DEBUG] Falha ao copiar parameters: " + e.getMessage());
            return new JsonObject();
        }
    }
    public String serializarParametrosFerramenta(JsonObject parameters) {
        if (parameters == null) {
            return "{}";
        }

        try {
            return parameters.toString();
        } catch (Exception e) {
            System.out.println("[MCP PARSE DEBUG] Falha ao serializar parameters JsonObject: " + e.getMessage());
            return "{}";
        }
    }
    private Object convertJsonElement(JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return null;
        }

        if (value.isJsonPrimitive()) {
            try {
                if (value.getAsJsonPrimitive().isBoolean()) {
                    return Boolean.valueOf(value.getAsBoolean());
                }
            } catch (Exception e) {
            }

            try {
                if (value.getAsJsonPrimitive().isNumber()) {
                    return value.getAsNumber();
                }
            } catch (Exception e) {
            }

            try {
                if (value.getAsJsonPrimitive().isString()) {
                    return value.getAsString();
                }
            } catch (Exception e) {
            }
        }

        return gson.fromJson(value, Object.class);
    }

    private String readString(JsonObject json, String propertyName) {
        if (json == null || propertyName == null || !json.has(propertyName) || json.get(propertyName).isJsonNull()) {
            return "";
        }

        try {
            return json.get(propertyName).getAsString();
        } catch (Exception e) {
            return "";
        }
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String truncateForDebug(String value, int max) {
        if (value == null) {
            return "null";
        }

        if (value.length() <= max) {
            return value;
        }

        return value.substring(0, max) + "... [TRUNCATED]";
    }
}