package com.mcp.sailibrary.plugin.chat.support;

/** * Estado capturado antes da aplicacao de uma mutacao no documento. * * @author Renato Tomaz Nati * @since 2026-05-25 */
public class CodeApplicationState {

    private int offsetInicial;
    private int comprimentoOriginal;
    private String conteudoAnterior;

    public int getOffsetInicial() {
        return offsetInicial;
    }

    public void setOffsetInicial(int offsetInicial) {
        this.offsetInicial = offsetInicial;
    }

    public int getComprimentoOriginal() {
        return comprimentoOriginal;
    }

    public void setComprimentoOriginal(int comprimentoOriginal) {
        this.comprimentoOriginal = comprimentoOriginal;
    }

    public String getConteudoAnterior() {
        return conteudoAnterior;
    }

    public void setConteudoAnterior(String conteudoAnterior) {
        this.conteudoAnterior = conteudoAnterior;
    }
}