package com.mcp.sailibrary.plugin.mcp.multimodel.audit;

/* --- version: "1.0" libraries: - N/A objetivo: "Representar o nivel semantico das observacoes do auditor sem misturar com o status operacional da execucao." --- */

/** * Nivel semantico das observacoes do auditor. * Nao deve ser misturado com o status de execucao da auditoria. * * @author Renato Tomaz Nati * @since 2026-05-25 */
public enum AuditObservationLevel {

    NENHUM,
    RECOMENDACAO,
    PONTO_ATENCAO
}