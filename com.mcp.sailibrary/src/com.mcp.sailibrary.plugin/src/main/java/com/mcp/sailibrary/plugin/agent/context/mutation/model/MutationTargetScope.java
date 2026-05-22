package com.mcp.sailibrary.plugin.agent.context.mutation.model;

/** * Define o escopo principal do alvo atingido por uma mutacao. * * <p>O escopo ajuda a distinguir operacoes que atuam sobre arquivo, diretorio, * package ou lote completo de alteracoes.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public enum MutationTargetScope {

    FILE,
    DIRECTORY,
    PACKAGE,
    BATCH
}