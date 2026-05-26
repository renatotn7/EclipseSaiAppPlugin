package com.mcp.sailibrary.plugin.mcp.multimodel.audit;

/* --- version: "1.0" libraries: - N/A objetivo: "Representar o resultado operacional da execucao da auditoria, separando reprovacao real de falha tecnica." --- */

/** * Status operacional da execucao da auditoria. * * @author Renato Tomaz Nati * @since 2026-05-25 */
public enum AuditExecutionStatus {

    APROVADO,
    REPROVADO,
    FALHA_INFRA,
    NAO_EXECUTADA
}