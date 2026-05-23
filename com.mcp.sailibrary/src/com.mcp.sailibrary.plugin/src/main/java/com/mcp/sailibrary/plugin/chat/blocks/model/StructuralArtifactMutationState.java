package com.mcp.sailibrary.plugin.chat.blocks.model;

/** * Representa o estado visual de mutacao de um artefato estrutural no * workspace. * * <p>Este estado e usado apenas para decoracao visual no explorer, sem alterar * a logica semantica do contexto estrutural da sessao.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public enum StructuralArtifactMutationState {

    NONE,
    ADDED,
    MODIFIED,
    RESTORED
}