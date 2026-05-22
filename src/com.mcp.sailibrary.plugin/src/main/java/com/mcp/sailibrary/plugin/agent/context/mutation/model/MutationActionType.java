package com.mcp.sailibrary.plugin.agent.context.mutation.model;

/** * Define os tipos semanticos de mutacao que podem ser registrados na camada de * versionamento interno do workspace. * * <p>Este enum representa a intencao de negocio da operacao e nao o detalhe * tecnico de como ela foi executada em disco ou no repositorio interno.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public enum MutationActionType {

    CREATE_FILE,
    UPDATE_FILE,
    DELETE_CREATED_FILE,
    CREATE_PACKAGE,
    RESTORE_FILE,
    RESTORE_DIRECTORY,
    UNDO_BATCH,
    REDO_BATCH
}