package com.mcp.sailibrary.plugin.mcp.multimodel.generation;

/* --- version: "1.0" libraries: - N/A objetivo: "Montar o prompt focado em escrita ou refatoracao de codigo para o modelo de codigo." --- */

/** * Monta o prompt do gerador de codigo. * * <p>Este builder e usado quando o fluxo multi-modelo ja possui um plano de * implementacao e precisa pedir ao modelo de codigo uma resposta cirurgica, * com foco em gerar o conteudo final sem reinvestigar todo o problema.</p> * * @author Renato Tomaz Nati * @since 2026-05-24 */
public class CodeGenerationPromptBuilder {

    /** * Caller: MultiModelCoordinator * Callee: N/A * Objetivo: Montar o prompt de geracao de codigo a partir do pedido * original, plano de implementacao e contexto tecnico. * Data modificacao: 2026-05-24 00:00 * * @param pedidoOriginal pedido original do usuario * @param planoImplementacao plano consolidado pelo reasoner * @param selectedCode trecho selecionado atual * @param fullFileText conteudo integral do arquivo atual * @param actionEsperada acao final esperada, como substituir ou comentar * @return prompt final para o gerador de codigo * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public String build(String pedidoOriginal, String planoImplementacao, String selectedCode, String fullFileText, String actionEsperada) {

        String pedidoSeguro = pedidoOriginal != null ? pedidoOriginal : "";
        String planoSeguro = planoImplementacao != null ? planoImplementacao : "";
        String trechoSeguro = selectedCode != null ? selectedCode : "";
        String arquivoSeguro = fullFileText != null ? fullFileText : "";
        String acaoSegura = actionEsperada != null ? actionEsperada : "";

        StringBuilder prompt = new StringBuilder();

        prompt.append("Voce atua como desenvolvedor senior executor de codigo.").append("\n");
        prompt.append("Sua funcao aqui nao e investigar o problema do zero.").append("\n");
        prompt.append("Sua funcao e implementar com precisao o plano recebido.").append("\n");
        prompt.append("Nao invente novas bibliotecas, arquivos, classes, metodos ou regras fora do que foi autorizado.").append("\n");
        prompt.append("Preserve compatibilidade com o estilo do projeto e com o nivel de linguagem do codigo existente.").append("\n");
        prompt.append("Nao use acentuacao nem caracteres especiais em codigo ou comentarios.").append("\n");
        prompt.append("Nao use lambda.").append("\n");
        prompt.append("Assuma ambiente legado e seja conservador.").append("\n");

        prompt.append("\n=== ACAO FINAL ESPERADA ===").append("\n");
        prompt.append(acaoSegura).append("\n");

        prompt.append("\n=== PEDIDO ORIGINAL DO USUARIO ===").append("\n");
        prompt.append(pedidoSeguro).append("\n");

        prompt.append("\n=== PLANO DE IMPLEMENTACAO RECEBIDO ===").append("\n");
        prompt.append(planoSeguro).append("\n");

        prompt.append("\n=== TRECHO ATUAL SELECIONADO ===").append("\n");
        if (trechoSeguro.trim().length() > 0) {
            prompt.append(trechoSeguro).append("\n");
        } else {
            prompt.append("[SEM_TRECHO_SELECIONADO]").append("\n");
        }

        prompt.append("\n=== ARQUIVO ATUAL COMPLETO ===").append("\n");
        if (arquivoSeguro.trim().length() > 0) {
            prompt.append(arquivoSeguro).append("\n");
        } else {
            prompt.append("[SEM_ARQUIVO_TEXTUAL_ATIVO]").append("\n");
        }

        prompt.append("\n=== REGRAS DE SAIDA ===").append("\n");
        prompt.append("Responda somente em JSON valido.").append("\n");
        prompt.append("Use os campos action, content e explanation.").append("\n");
        prompt.append("Nao escreva texto fora do JSON.").append("\n");
        prompt.append("Se a acao esperada for substituir, content deve conter apenas o codigo final aplicavel.").append("\n");
        prompt.append("Nao devolva o arquivo inteiro quando bastar um trecho.").append("\n");
        prompt.append("Nao invente package nem imports se o alvo for um metodo isolado.").append("\n");

        return prompt.toString();
    }
}