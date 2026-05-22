package com.mcp.sailibrary.plugin.chat.service;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcp.sailibrary.plugin.chat.support.AiResponse;

/**
 * Encapsular extracao, parse, normalizacao e formatacao das respostas MCP e da IA.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-18
 */
public class McpAgentResponseService {

    public String extrairTextoMcp(String respostaBruta) {
        if (respostaBruta == null) return "";
        String texto = respostaBruta.trim();
        if (texto.length() == 0) return "";

        String textoExtraido = "";
        try {
            JsonObject envelope = JsonParser.parseString(texto).getAsJsonObject();
            if (envelope.has("result") && envelope.get("result").isJsonObject()) {
                JsonObject result = envelope.getAsJsonObject("result");
                if (result.has("content") && result.get("content").isJsonArray()) {
                    JsonArray contentArray = result.getAsJsonArray("content");
                    if (contentArray.size() > 0 && contentArray.get(0).isJsonObject()) {
                        JsonObject firstContent = contentArray.get(0).getAsJsonObject();
                        if (firstContent.has("text") && !firstContent.get("text").isJsonNull()) {
                            textoExtraido = firstContent.get("text").getAsString();
                            return isolarJsonDaTag(textoExtraido);
                        }
                    }
                }
            }
            textoExtraido = desescapeJsonString(texto);
            return isolarJsonDaTag(textoExtraido);
        } catch (Exception e) {
            textoExtraido = desescapeJsonString(texto);
            return isolarJsonDaTag(textoExtraido);
        }
    }

    public String isolarJsonDaTag(String textoLLM) {
        if (textoLLM == null) return "";
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
            jsonExtraido = jsonExtraido.substring(7);
        } else if (jsonExtraido.startsWith("```")) {
            jsonExtraido = jsonExtraido.substring(3);
        }

        if (jsonExtraido.endsWith("```")) {
            jsonExtraido = jsonExtraido.substring(0, jsonExtraido.length() - 3);
        }

        return jsonExtraido.trim();
    }

    public String desescapeJsonString(String texto) {
        if (texto == null) return "";
        String textoLimpo = texto.trim();
        textoLimpo = textoLimpo.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t").replace("\\\"", "\"").replace("\\\\", "\\");
        return textoLimpo;
    }

    public AiResponse interpretarRespostaIA(String respostaJson) {
        if (respostaJson == null) {
            return null;
        }

        String texto = respostaJson.trim();
        if (texto.length() == 0) {
            return null;
        }

        try {
            JsonObject jsonObject = JsonParser.parseString(texto).getAsJsonObject();
            AiResponse respostaIA = new AiResponse();

            if (jsonObject.has("action") && !jsonObject.get("action").isJsonNull()) {
                respostaIA.setAction(jsonObject.get("action").getAsString());
            }

            if (jsonObject.has("content") && !jsonObject.get("content").isJsonNull()) {
                respostaIA.setContent(jsonObject.get("content").getAsString());
            }

            if (jsonObject.has("explanation") && !jsonObject.get("explanation").isJsonNull()) {
                respostaIA.setExplanation(jsonObject.get("explanation").getAsString());
            }

            if (jsonObject.has("tool") && !jsonObject.get("tool").isJsonNull()) {
                respostaIA.setTool(jsonObject.get("tool").getAsString());
            }

            if (jsonObject.has("parameters") && jsonObject.get("parameters").isJsonObject()) {
                respostaIA.setParameters(jsonObject.getAsJsonObject("parameters"));
            }

            if (jsonObject.has("question") && !jsonObject.get("question").isJsonNull()) {
                respostaIA.setQuestion(jsonObject.get("question").getAsString());
            }

            if (jsonObject.has("expected_answer_type") && !jsonObject.get("expected_answer_type").isJsonNull()) {
                respostaIA.setExpectedAnswerType(jsonObject.get("expected_answer_type").getAsString());
            }

            if (jsonObject.has("options") && jsonObject.get("options").isJsonArray()) {
                respostaIA.setOptions(jsonObject.getAsJsonArray("options"));
            }

            return respostaIA;
        } catch (Exception e) {
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
}