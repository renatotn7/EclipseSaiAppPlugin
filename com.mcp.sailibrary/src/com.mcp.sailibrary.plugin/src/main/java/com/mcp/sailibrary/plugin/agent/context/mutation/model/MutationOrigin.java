package com.mcp.sailibrary.plugin.agent.context.mutation.model;

/** * Define a origem funcional da mutacao registrada na camada de workspace. * * <p>O objetivo deste enum e rastrear se a operacao nasceu de uma decisao da * IA, de uma acao explicita do usuario ou de um fluxo interno do proprio * plugin.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public enum MutationOrigin {

    AI,
    USER,
    PLUGIN
}