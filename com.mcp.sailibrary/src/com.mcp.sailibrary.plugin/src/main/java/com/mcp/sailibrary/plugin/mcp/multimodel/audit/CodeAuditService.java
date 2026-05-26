package com.mcp.sailibrary.plugin.mcp.multimodel.audit;

/* --- version: "1.0" libraries: - com.mcp.sailibrary.plugin.mcp.multimodel.CodeAuditResult objetivo: "Definir o contrato de auditoria de codigo para o circuito multi-modelo." --- */

/** * Contrato para servicos responsaveis por auditar codigo gerado. * * @author Renato Tomaz Nati * @since 2026-05-24 */
public interface CodeAuditService {

    /** * Caller: MultiModelCoordinator * Callee: Implementacao concreta do servico de auditoria * Objetivo: Auditar o codigo candidato com base no pedido original, no * plano de implementacao e na acao final esperada. * Data modificacao: 2026-05-24 00:00 * * @param pedidoOriginal pedido original do usuario * @param planoImplementacao plano tecnico consolidado * @param codigoCandidato codigo gerado para avaliacao * @param actionEsperada acao final esperada * @param apiKey chave de autenticacao MCP * @return resultado estruturado da auditoria * @throws Exception quando ocorrer falha de transporte ou auditoria * * @author Renato Tomaz Nati * @since 2026-05-24 */
    CodeAuditResult auditarCodigo(String pedidoOriginal, String planoImplementacao, String codigoCandidato, String actionEsperada, String apiKey) throws Exception;
}