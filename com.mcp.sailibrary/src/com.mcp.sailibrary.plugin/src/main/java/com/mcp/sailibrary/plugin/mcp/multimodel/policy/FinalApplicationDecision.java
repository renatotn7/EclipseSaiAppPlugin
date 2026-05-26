package com.mcp.sailibrary.plugin.mcp.multimodel.policy;

/* --- version: "1.0" libraries: - N/A objetivo: "Representar a decisao final do pipeline de aplicacao de codigo." --- */

/** * Decisao final do pipeline de mutacao. * * @author Renato Tomaz Nati * @since 2026-05-25 */
public enum FinalApplicationDecision {

    APLICAR,
    APLICAR_COM_CONFIRMACAO,
    BLOQUEAR
}