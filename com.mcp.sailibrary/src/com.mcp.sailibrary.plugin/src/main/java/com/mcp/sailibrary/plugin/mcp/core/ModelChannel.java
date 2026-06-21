package com.mcp.sailibrary.plugin.mcp.core;

/** * Representa o papel cognitivo do modelo dentro do fluxo do plugin. * * <p>O objetivo desta enumeracao e desacoplar o restante do sistema do nome * fisico do modelo e da forma de transporte. O sistema trabalha por papel * cognitivo e o resolver decide como cada papel sera executado.</p> * * @author Renato Tomaz Nati * @since 2026-05-26 */
public enum ModelChannel {

    INVESTIGATOR("investigator"),
    PLANNER("planner"),
    CODE_GENERATOR("code.generator"),
    CODE_AUDITOR("code.auditor"),
    SUMMARIZER("summarizer"),
	CONTEXT_NAMING("context.naming");

    private final String propertySuffix;

    private ModelChannel(String propertySuffix) {
        this.propertySuffix = propertySuffix;
    }

    public String getPropertySuffix() {
        return propertySuffix;
    }
}