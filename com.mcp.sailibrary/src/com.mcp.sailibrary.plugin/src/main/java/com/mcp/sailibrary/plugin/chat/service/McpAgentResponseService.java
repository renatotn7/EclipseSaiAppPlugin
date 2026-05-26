package com.mcp.sailibrary.plugin.chat.service;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcp.sailibrary.plugin.chat.support.AiResponse;

/** * Encapsular extracao, parse, normalizacao e formatacao das respostas MCP e da IA. * * @author Renato Tomaz Nati * @since 2026-05-18 */
public class McpAgentResponseService {

    public String extrairTextoMcp(String respostaBruta) {
        if (respostaBruta == null) {
            return "";
        }

        String texto = respostaBruta.trim();
        if (texto.length() == 0) {
            return "";
        }

        if (isModelInfrastructureFailureText(texto)) {
            return texto;
        }

        String textoExtraido = "";
        try {
            JsonObject envelope = JsonParser.parseString(texto).getAsJsonObject();

            if (envelope.has("result") && envelope.get("result").isJsonObject()) {
                JsonObject result = envelope.getAsJsonObject("result");

                if (result.has("content") && result.get("content").isJsonArray()) {
                    JsonArray contentArray = result.getAsJsonArray("content");
                    for (int i = 0; i < contentArray.size(); i++) {
                        JsonElement item = contentArray.get(i);
                        if (item != null && item.isJsonObject()) {
                            JsonObject contentObject = item.getAsJsonObject();
                            if (contentObject.has("text") && !contentObject.get("text").isJsonNull()) {
                                textoExtraido = contentObject.get("text").getAsString();
                                return limparEnvelopeTextual(textoExtraido);
                            }
                        }
                    }
                }
            }

            textoExtraido = desescapeJsonString(texto);
            return limparEnvelopeTextual(textoExtraido);
        } catch (Exception e) {
            textoExtraido = desescapeJsonString(texto);
            return limparEnvelopeTextual(textoExtraido);
        }
    }

    public String isolarJsonDaTag(String textoLLM) {
        if (textoLLM == null) {
            return "";
        }

        String tagInicio = "<codigo_final>";
        String tagFim = "</codigo_final>";
        int indexInicio = textoLLM.indexOf(tagInicio);
        int indexFim = textoLLM.lastIndexOf(tagFim);

        String jsonExtraido = textoLLM;

        if (indexInicio != -1 && indexFim != -1 && indexFim > indexInicio) {
            int inicioConteudo = indexInicio + tagInicio.length();
            jsonExtraido = textoLLM.substring(inicioConteudo, indexFim).trim();
        } else {
            jsonExtraido = textoLLM.trim();
        }

        if (jsonExtraido.startsWith("```json")) {
            jsonExtraido = jsonExtraido.substring(7).trim();
        } else if (jsonExtraido.startsWith("```")) {
            jsonExtraido = jsonExtraido.substring(3).trim();
        }

        if (jsonExtraido.endsWith("```")) {
            jsonExtraido = jsonExtraido.substring(0, jsonExtraido.length() - 3).trim();
        }

        return jsonExtraido.trim();
    }

    public String desescapeJsonString(String texto) {
        if (texto == null) {
            return "";
        }

        String textoLimpo = texto.trim();
        textoLimpo = textoLimpo
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");

        return textoLimpo;
    }

    /** * Caller: SingleModelCoordinator, CodexCodeGenerationService * Callee: extrairJsonUtilDaResposta * Objetivo: Interpretar a resposta textual da IA aceitando tanto JSON puro * quanto respostas encapsuladas nas tags thinking, racional e codigo_final. * Feature: Quando houver bloco <codigo_final>, ele passa a ter prioridade como * fonte da verdade para o parse. * Data modificacao: 2026-05-25 * * @param textoResposta texto bruto extraido do MCP * @return resposta estruturada ou null quando o parse falhar * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public AiResponse interpretarRespostaIA(String textoResposta) {
        if (textoResposta == null || textoResposta.trim().length() == 0) {
            System.out.println("[MCP PARSE DEBUG] textoResposta vazio.");
            return null;
        }

        String textoNormalizado = extrairJsonUtilDaResposta(textoResposta);
        System.out.println("[MCP PARSE DEBUG] textoNormalizadoParaJson=" + truncateForDebug(textoNormalizado, 3000));

        if (isModelInfrastructureFailureText(textoNormalizado)) {
            System.out.println("[MCP PARSE DEBUG] Resposta classificada como falha de infraestrutura do provider/modelo.");
            return null;
        }

        try {
            JsonObject json = tentarExtrairObjetoJson(textoNormalizado);

            if (json == null) {
                System.out.println("[MCP PARSE DEBUG] Nenhum objeto JSON util encontrado na resposta.");
                return null;
            }

            AiResponse resposta = new AiResponse();

            if (json.has("action") && !json.get("action").isJsonNull()) {
                resposta.setAction(json.get("action").getAsString());
            }

            if (json.has("content") && !json.get("content").isJsonNull()) {
                resposta.setContent(extractFlexibleText(json.get("content")));
            }

            if (json.has("explanation") && !json.get("explanation").isJsonNull()) {
                resposta.setExplanation(extractFlexibleText(json.get("explanation")));
            }

            if (json.has("tool") && !json.get("tool").isJsonNull()) {
                resposta.setTool(json.get("tool").getAsString());
            }

            if (json.has("question") && !json.get("question").isJsonNull()) {
                resposta.setQuestion(extractFlexibleText(json.get("question")));
            }

            if (json.has("parameters") && json.get("parameters").isJsonObject()) {
                resposta.setParameters(json.getAsJsonObject("parameters"));
            }

            if (json.has("expected_answer_type") && !json.get("expected_answer_type").isJsonNull()) {
                resposta.setExpectedAnswerType(json.get("expected_answer_type").getAsString());
            }

            if (json.has("options") && json.get("options").isJsonArray()) {
                resposta.setOptions(json.getAsJsonArray("options"));
            }

            return resposta;
        } catch (Exception e) {
            System.out.println("[MCP PARSE DEBUG] Falha ao parsear JSON: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public boolean respostaEstruturadaValida(AiResponse respostaIA) {
        if (respostaIA == null) {
            return false;
        }

        String acao = respostaIA.getAction();
        if (acao == null || acao.trim().length() == 0) {
            return false;
        }

        if ("executar_ferramenta".equalsIgnoreCase(acao)) {
            return respostaIA.getTool() != null && respostaIA.getTool().trim().length() > 0;
        }

        if ("perguntar_ao_usuario".equalsIgnoreCase(acao)) {
            return respostaIA.getQuestion() != null && respostaIA.getQuestion().trim().length() > 0;
        }

        if ("responder_ao_usuario".equalsIgnoreCase(acao) || "explicar".equalsIgnoreCase(acao)) {
            return respostaIA.getContent() != null && respostaIA.getContent().trim().length() > 0;
        }

        if ("substituir".equalsIgnoreCase(acao)
                || "comentar".equalsIgnoreCase(acao)
                || "inserir_abaixo".equalsIgnoreCase(acao)
                || "anexar_acima".equalsIgnoreCase(acao)) {
            return respostaIA.getContent() != null && respostaIA.getContent().trim().length() > 0;
        }

        return false;
    }

    public String construirInstrucaoCorrecaoFormato(String respostaInvalida) {
        StringBuilder builder = new StringBuilder();
        builder.append("\n\n=== CORRECAO DE FORMATO OBRIGATORIA ===\n");
        builder.append("A resposta anterior foi rejeitada por violar o protocolo.\n");
        builder.append("Reenvie somente um JSON valido, sem qualquer texto fora do JSON.\n");
        builder.append("Campos permitidos: action, content, explanation, tool, parameters, question, expected_answer_type, options.\n");
        builder.append("Acoes permitidas: executar_ferramenta, perguntar_ao_usuario, responder_ao_usuario, substituir, comentar, inserir_abaixo, anexar_acima, explicar.\n");
        builder.append("Se usar executar_ferramenta, use tool e parameters no nivel principal do JSON.\n");
        builder.append("Se o limite de ciclos estiver proximo ou esgotado, entregue uma resposta parcial positiva com responder_ao_usuario ou explicar, informando o que foi confirmado e o que nao foi possivel confirmar.\n");
        builder.append("Resposta rejeitada:\n");
        builder.append(respostaInvalida);
        builder.append("\n=== FIM DA CORRECAO ===\n");
        return builder.toString();
    }

    public String serializarParametrosFerramenta(JsonObject parameters) {
        if (parameters == null) {
            return "{}";
        }
        return parameters.toString();
    }

    public String montarPerguntaAoUsuario(AiResponse respostaIA) {
        StringBuilder texto = new StringBuilder();

        texto.append("Pergunta da IA: ");
        texto.append(respostaIA.getQuestion() != null ? respostaIA.getQuestion() : "");

        if (respostaIA.getExpectedAnswerType() != null && respostaIA.getExpectedAnswerType().trim().length() > 0) {
            texto.append(System.lineSeparator());
            texto.append("Tipo esperado: ").append(respostaIA.getExpectedAnswerType());
        }

        if (respostaIA.getOptions() != null && respostaIA.getOptions().size() > 0) {
            texto.append(System.lineSeparator());
            texto.append("Opcoes:");
            for (int i = 0; i < respostaIA.getOptions().size(); i++) {
                texto.append(System.lineSeparator());
                texto.append("- ").append(respostaIA.getOptions().get(i).getAsString());
            }
        }

        if (respostaIA.getExplanation() != null && respostaIA.getExplanation().trim().length() > 0) {
            texto.append(System.lineSeparator());
            texto.append("Motivo: ").append(respostaIA.getExplanation());
        }

        return texto.toString();
    }

    public String formatarRespostaIA(AiResponse respostaIA, IDocument document) {
        if (respostaIA == null) {
            return "";
        }

        String delimitador = getLineDelimiter(document);
        StringBuilder texto = new StringBuilder();

        if (respostaIA.getAction() != null) {
            texto.append("action: ").append(respostaIA.getAction()).append(delimitador);
        }

        if (respostaIA.getTool() != null && respostaIA.getTool().trim().length() > 0) {
            texto.append("tool: ").append(respostaIA.getTool()).append(delimitador);
        }

        if (respostaIA.getQuestion() != null && respostaIA.getQuestion().trim().length() > 0) {
            texto.append("question: ").append(respostaIA.getQuestion()).append(delimitador);
        }

        if (respostaIA.getExplanation() != null && respostaIA.getExplanation().trim().length() > 0) {
            texto.append("explanation: ").append(respostaIA.getExplanation()).append(delimitador);
        }

        if (respostaIA.getContent() != null && respostaIA.getContent().trim().length() > 0) {
            texto.append(delimitador);
            texto.append(respostaIA.getContent());
        }

        return texto.toString();
    }

    public AiResponse normalizarProtocoloFerramentaLegado(AiResponse respostaIA) {
        if (respostaIA == null) {
            return null;
        }

        if (!"usar_ferramenta".equalsIgnoreCase(respostaIA.getAction())) {
            return respostaIA;
        }

        String content = respostaIA.getContent();
        if (content == null || content.trim().length() == 0) {
            return respostaIA;
        }

        try {
            JsonObject jsonInterno = JsonParser.parseString(content).getAsJsonObject();

            if (jsonInterno.has("ferramenta") && !jsonInterno.get("ferramenta").isJsonNull()) {
                respostaIA.setTool(jsonInterno.get("ferramenta").getAsString());
            }

            if (jsonInterno.has("parametros") && jsonInterno.get("parametros").isJsonObject()) {
                respostaIA.setParameters(jsonInterno.getAsJsonObject("parametros"));
            }

            respostaIA.setAction("executar_ferramenta");
            return respostaIA;
        } catch (Exception e) {
            return respostaIA;
        }
    }

    public boolean isModelInfrastructureFailureText(String text) {
        String texto = safeLower(text);

        if (texto.length() == 0) {
            return false;
        }

        return texto.contains("error executing tool")
                || texto.contains("template usage limit exceeded")
                || texto.contains("max_tokens")
                || texto.contains("maximum allowed number of output tokens")
                || texto.contains("context length")
                || texto.contains("rate limit")
                || texto.contains("quota")
                || texto.contains("timeout")
                || texto.contains("timed out")
                || texto.contains("service unavailable")
                || texto.contains("unavailable")
                || texto.contains("openaicompatible")
                || texto.contains("malformedjsonexception")
                || texto.contains("provider error");
    }

    public AiResponse buildInfrastructureFailureResponse(String detalheTecnico, boolean perguntarAoUsuario) {
        AiResponse resposta = new AiResponse();

        if (perguntarAoUsuario) {
            resposta.setAction("perguntar_ao_usuario");
            resposta.setQuestion("Percebi uma falha tecnica do modelo durante a execucao. Deseja que eu tente seguir mesmo assim com o que ja foi confirmado?");
            resposta.setExplanation("Falha tecnica de infraestrutura do provider/modelo detectada antes do parse estruturado."
                    + (detalheTecnico != null && detalheTecnico.trim().length() > 0
                    ? " Detalhe tecnico: " + detalheTecnico
                    : ""));
            return resposta;
        }

        resposta.setAction("responder_ao_usuario");
        resposta.setContent("Percebi uma falha tecnica do modelo durante a execucao. Nao tratei isso como resposta valida da IA. Se quiser, posso tentar novamente ou investigar a causa.");
        resposta.setExplanation("Falha tecnica de infraestrutura do provider/modelo detectada antes do parse estruturado."
                + (detalheTecnico != null && detalheTecnico.trim().length() > 0
                ? " Detalhe tecnico: " + detalheTecnico
                : ""));
        return resposta;
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

    private String extrairJsonUtilDaResposta(String textoResposta) {
        String texto = limparEnvelopeTextual(textoResposta);

        if (isModelInfrastructureFailureText(texto)) {
            return texto;
        }

        int inicioCodigoFinal = texto.indexOf("<codigo_final>");
        int fimCodigoFinal = texto.indexOf("</codigo_final>");

        if (inicioCodigoFinal >= 0 && fimCodigoFinal > inicioCodigoFinal) {
            String conteudo = texto.substring(inicioCodigoFinal + "<codigo_final>".length(), fimCodigoFinal).trim();
            System.out.println("[MCP PARSE DEBUG] Bloco <codigo_final> encontrado.");
            return limparEnvelopeTextual(conteudo);
        }

        String jsonBalaceado = extractFirstBalancedJsonObject(texto);
        if (jsonBalaceado != null && jsonBalaceado.trim().length() > 0) {
            return jsonBalaceado;
        }

        return texto;
    }

    private String limparEnvelopeTextual(String texto) {
        String valor = texto != null ? texto.trim() : "";

        if (valor.startsWith("```json")) {
            valor = valor.substring(7).trim();
        } else if (valor.startsWith("```")) {
            valor = valor.substring(3).trim();
        }

        if (valor.endsWith("```")) {
            valor = valor.substring(0, valor.length() - 3).trim();
        }

        return valor.trim();
    }

    private JsonObject tentarExtrairObjetoJson(String textoNormalizado) {
        if (textoNormalizado == null || textoNormalizado.trim().length() == 0) {
            return null;
        }

        try {
            JsonElement root = JsonParser.parseString(textoNormalizado);
            if (root != null && root.isJsonObject()) {
                return root.getAsJsonObject();
            }
        } catch (Exception e) {
        }

        String bloco = extractFirstBalancedJsonObject(textoNormalizado);
        if (bloco != null && bloco.trim().length() > 0) {
            try {
                JsonElement root = JsonParser.parseString(bloco);
                if (root != null && root.isJsonObject()) {
                    return root.getAsJsonObject();
                }
            } catch (Exception e) {
            }
        }

        return null;
    }

    private String extractFlexibleText(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return "";
        }

        try {
            if (element.isJsonPrimitive()) {
                return element.getAsString();
            }

            if (element.isJsonObject() || element.isJsonArray()) {
                return element.toString();
            }
        } catch (Exception e) {
        }

        return "";
    }

    private String extractFirstBalancedJsonObject(String text) {
        if (text == null || text.trim().length() == 0) {
            return null;
        }

        int start = -1;
        int depth = 0;
        boolean insideString = false;
        boolean escaping = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (escaping) {
                escaping = false;
                continue;
            }

            if (c == '\\' && insideString) {
                escaping = true;
                continue;
            }

            if (c == '"') {
                insideString = !insideString;
                continue;
            }

            if (insideString) {
                continue;
            }

            if (c == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    return text.substring(start, i + 1);
                }
            }
        }

        return null;
    }

    private String getLineDelimiter(IDocument doc) {
        String delimitador = System.lineSeparator();
        if (doc != null) {
            String definidoNoDocumento = TextUtilities.getDefaultLineDelimiter(doc);
            if (definidoNoDocumento != null) {
                delimitador = definidoNoDocumento;
            }
        }
        return delimitador;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}