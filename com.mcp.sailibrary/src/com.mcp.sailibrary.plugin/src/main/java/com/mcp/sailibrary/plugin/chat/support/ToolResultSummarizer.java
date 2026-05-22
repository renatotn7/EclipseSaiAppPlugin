package com.mcp.sailibrary.plugin.chat.support;

/**
 * Resumir resultados de ferramentas para exibicao no chat e para memoria persistente.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-18
 */
public class ToolResultSummarizer {

    public String resumirParaChat(String nomeFerramenta, String parametros, String resultadoFerramenta) {
        if (resultadoFerramenta == null || resultadoFerramenta.trim().length() == 0) {
            return "Sem dados retornados.";
        }

        if ("buscar_texto_projeto".equalsIgnoreCase(nomeFerramenta)) {
            return resumirBuscaTextualParaChat(resultadoFerramenta);
        }

        if ("ler_conteudo_arquivo".equalsIgnoreCase(nomeFerramenta)) {
            return resumirLeituraArquivoParaChat(parametros, resultadoFerramenta);
        }

        if (resultadoFerramenta.length() > 1200) {
            return resultadoFerramenta.substring(0, 1200) + System.lineSeparator() + "[RESUMO]: Resultado truncado no chat para preservar legibilidade.";
        }

        return resultadoFerramenta;
    }

    public String resumirParaMemoria(String resultadoFerramentaParaChat) {
        if (resultadoFerramentaParaChat == null || resultadoFerramentaParaChat.trim().length() == 0) {
            return "Sem dados retornados.";
        }

        if (resultadoFerramentaParaChat.length() > 5000) {
            return resultadoFerramentaParaChat.substring(0, 5000) + "...";
        }

        return resultadoFerramentaParaChat;
    }

    private String resumirBuscaTextualParaChat(String resultadoFerramenta) {
        String[] linhas = resultadoFerramenta.split("\\r?\\n");
        StringBuilder resumo = new StringBuilder();
        int contadorOcorrencias = 0;

        for (int i = 0; i < linhas.length; i++) {
            String linhaAtual = linhas[i];
            if (linhaAtual.startsWith("Arquivo: ")) {
                contadorOcorrencias++;
                resumo.append(linhaAtual).append(System.lineSeparator());
                if (contadorOcorrencias >= 10) {
                    resumo.append("[RESUMO]: Limite visual de 10 ocorrencias atingido no chat.").append(System.lineSeparator());
                    break;
                }
            }
        }

        if (contadorOcorrencias == 0) {
            if (resultadoFerramenta.length() > 800) {
                return resultadoFerramenta.substring(0, 800) + System.lineSeparator() + "[RESUMO]: Resultado truncado no chat.";
            }
            return resultadoFerramenta;
        }

        return resumo.toString();
    }

    private String resumirLeituraArquivoParaChat(String parametros, String resultadoFerramenta) {
        StringBuilder resumo = new StringBuilder();
        String pathArquivo = extrairValorVariavelSimples(parametros, "path");

        if (pathArquivo != null && pathArquivo.trim().length() > 0) {
            resumo.append("Arquivo lido: ").append(pathArquivo).append(System.lineSeparator());
        }

        String[] linhas = resultadoFerramenta.split("\\r?\\n");
        String packageEncontrado = "";
        String classeEncontrada = "";
        java.util.List<String> metodosEncontrados = new java.util.ArrayList<String>();

        for (int i = 0; i < linhas.length; i++) {
            String linhaAtual = linhas[i].trim();

            if (packageEncontrado.length() == 0 && linhaAtual.startsWith("package ")) {
                packageEncontrado = linhaAtual;
            }

            if (classeEncontrada.length() == 0 && (linhaAtual.contains(" class ") || linhaAtual.startsWith("public class ") || linhaAtual.startsWith("class "))) {
                classeEncontrada = linhaAtual;
            }

            if (linhaAtual.startsWith("package ")
                    || linhaAtual.contains(" class ")
                    || linhaAtual.startsWith("public ")
                    || linhaAtual.startsWith("protected ")
                    || linhaAtual.startsWith("private ")
                    || linhaAtual.contains("DAO")
                    || linhaAtual.contains("Repository")
                    || linhaAtual.contains("Service")
                    || linhaAtual.contains("createQuery")
                    || linhaAtual.contains("createSQLQuery")
                    || linhaAtual.contains("find(")
                    || linhaAtual.contains("list(")
                    || linhaAtual.contains("save(")
                    || linhaAtual.contains("update(")
                    || linhaAtual.contains("delete(")
                    || linhaAtual.contains(" return ")) {
                if (metodosEncontrados.size() < 12) {
                    metodosEncontrados.add(linhaAtual);
                }
            }
        }

        if (packageEncontrado.length() > 0) {
            resumo.append("Package: ").append(packageEncontrado).append(System.lineSeparator());
        }

        if (classeEncontrada.length() > 0) {
            resumo.append("Classe: ").append(classeEncontrada).append(System.lineSeparator());
        }

        if (!metodosEncontrados.isEmpty()) {
            resumo.append("Linhas relevantes encontradas:").append(System.lineSeparator());
            for (int i = 0; i < metodosEncontrados.size(); i++) {
                resumo.append("- ").append(metodosEncontrados.get(i)).append(System.lineSeparator());
            }
        }

        resumo.append("Linhas carregadas para analise: ").append(linhas.length).append(System.lineSeparator());
        resumo.append("Observacao: o conteudo completo foi entregue apenas para a IA e foi resumido no chat.");

        return resumo.toString();
    }

    private String extrairValorVariavelSimples(String json, String chave) {
        String padrao = "\"" + chave + "\":\"";
        if (json != null && json.contains(padrao)) {
            int inicio = json.indexOf(padrao) + padrao.length();
            int fim = json.indexOf("\"", inicio);
            if (fim > inicio) {
                return json.substring(inicio, fim);
            }
        }
        return "";
    }
}