package com.mcp.sailibrary.plugin.chat.settings;

/* --- version: "1.0" libraries: - N/A objetivo: "Representar a configuracao persistida da view de chat de forma simples e desacoplada da UI." --- */

/** * Representa a configuracao funcional do chat. * * @author Renato Tomaz Nati * @since 2026-05-24 */
public class ChatRuntimeSettings {

    public static final String MODO_EXECUCAO_MONO = "MONOMODELO";
    public static final String MODO_EXECUCAO_MULTI = "MULTIMODELO";

    public static final String PERFIL_PADRAO = "PADRAO_SEGURO";
    public static final String PERFIL_COMPLEXO = "COMPLEXO_MAIOR_ASSERTIVIDADE";
    public static final String PERFIL_ULTRA = "ULTRA_COMPLEXO_MAIOR_ASSERTIVIDADE";

    private boolean debugAtivo;
    private String modoExecucao;
    private String perfilRaciocinio;

    /** * Caller: ChatConfigurationController * Callee: N/A * Objetivo: Criar configuracao padrao segura da view. * Data modificacao: 2026-05-24 00:00 * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public ChatRuntimeSettings() {
        this.debugAtivo = false;
        this.modoExecucao = MODO_EXECUCAO_MONO;
        this.perfilRaciocinio = PERFIL_PADRAO;
    }

    public boolean isDebugAtivo() {
        return debugAtivo;
    }

    public void setDebugAtivo(boolean debugAtivo) {
        this.debugAtivo = debugAtivo;
    }

    public String getModoExecucao() {
        return modoExecucao;
    }

    public void setModoExecucao(String modoExecucao) {
        this.modoExecucao = safeTrim(modoExecucao);
    }

    public String getPerfilRaciocinio() {
        return perfilRaciocinio;
    }

    public void setPerfilRaciocinio(String perfilRaciocinio) {
        this.perfilRaciocinio = safeTrim(perfilRaciocinio);
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}