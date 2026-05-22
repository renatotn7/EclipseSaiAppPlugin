package com.mcp.sailibrary.plugin.agent.context.mutation.model;

/** * Define o estado operacional de uma mutacao individual registrada pelo * plugin. * * <p>O status permite distinguir operacoes apenas iniciadas, aplicadas com * sucesso, revertidas, refeitas ou encerradas com falha.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public enum MutationOperationStatus {

    STARTED,
    APPLIED,
    FAILED,
    UNDONE,
    REDONE,
    RESTORED
}