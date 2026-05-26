package com.mcp.sailibrary.plugin.mcp.multimodel.audit;

/* --- version: "1.0" libraries: - N/A objetivo: "Montar o prompt de auditoria de codigo para o modelo revisor." --- */

/** * Monta o prompt do auditor de codigo. * * <p>O auditor nao deve reescrever o sistema inteiro nem investigar do zero. * Ele deve apenas julgar se o codigo candidato respeita o plano, o pedido do * usuario e as restricoes operacionais do projeto.</p> * * @author Renato Tomaz Nati * @since 2026-05-24 */
public class CodeAuditPromptBuilder {

    /** * Caller: MultiModelCoordinator * Callee: N/A * Objetivo: Montar o prompt de auditoria a partir do pedido original, do * plano de implementacao e do codigo candidato. * Data modificacao: 2026-05-24 00:00 * * @param pedidoOriginal pedido original do usuario * @param planoImplementacao plano consolidado * @param codigoCandidato codigo gerado para avaliacao * @param actionEsperada acao final esperada * @return prompt final do auditor * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public String build(String pedidoOriginal, String planoImplementacao, String codigoCandidato, String actionEsperada) {

        String pedidoSeguro = pedidoOriginal != null ? pedidoOriginal : "";
        String planoSeguro = planoImplementacao != null ? planoImplementacao : "";
        String codigoSeguro = codigoCandidato != null ? codigoCandidato : "";
        String acaoSegura = actionEsperada != null ? actionEsperada : "";

        StringBuilder prompt = new StringBuilder();

        prompt.append("Voce atua como auditor tecnico e analista de riscos.").append("\n");
        prompt.append("Sua funcao e revisar o codigo candidato com rigidez.").append("\n");
        prompt.append("Nao tente ser criativo.").append("\n");
        prompt.append("Nao replaneje o problema do zero.").append("\n");
        prompt.append("Aplique verificacao rigorosa sobre aderencia ao pedido, ao plano e ao contexto legado.").append("\n");
        prompt.append("Considere como falhas graves:").append("\n");
        prompt.append("- regressao funcional provavel").append("\n");
        prompt.append("- violacao de restricoes de Java legado").append("\n");
        prompt.append("- uso de lambda").append("\n");
        prompt.append("- uso de biblioteca nao prevista").append("\n");
        prompt.append("- alteracao de escopo alem do pedido").append("\n");
        prompt.append("- quebra de formato da acao esperada").append("\n");
        prompt.append("- comentario ou codigo excessivamente inventado").append("\n");

        prompt.append("\n=== ACAO FINAL ESPERADA ===").append("\n");
        prompt.append(acaoSegura).append("\n");

        prompt.append("\n=== PEDIDO ORIGINAL DO USUARIO ===").append("\n");
        prompt.append(pedidoSeguro).append("\n");

        prompt.append("\n=== PLANO DE IMPLEMENTACAO ===").append("\n");
        prompt.append(planoSeguro).append("\n");

        prompt.append("\n=== CODIGO CANDIDATO ===").append("\n");
        prompt.append(codigoSeguro).append("\n");

        prompt.append("\n=== FORMATO OBRIGATORIO DA RESPOSTA ===").append("\n");
        prompt.append("Responda somente em JSON valido.").append("\n");
        prompt.append("Campos obrigatorios: approved, shouldRetry, riskLevel, feedback.").append("\n");
        prompt.append("approved deve ser true ou false.").append("\n");
        prompt.append("shouldRetry deve ser true ou false.").append("\n");
        prompt.append("riskLevel deve ser BAIXO, MEDIO ou ALTO.").append("\n");
        prompt.append("feedback deve conter a justificativa objetiva.").append("\n");
        prompt.append("Nao escreva texto fora do JSON.").append("\n");

        return prompt.toString();
    }
}