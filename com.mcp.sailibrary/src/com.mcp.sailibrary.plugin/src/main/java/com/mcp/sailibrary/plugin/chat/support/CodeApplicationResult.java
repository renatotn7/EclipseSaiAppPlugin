package com.mcp.sailibrary.plugin.chat.support;

/** * Resultado da aplicacao de uma mutacao no documento. * * @author Renato Tomaz Nati * @since 2026-05-25 */
public class CodeApplicationResult {

    private boolean aplicou;
    private boolean validacaoOk;
    private boolean revertido;
    private String mensagemUsuario;
    private String mensagemTecnica;
    private CodeApplicationState estadoAnterior;

    public CodeApplicationResult() {
        this.aplicou = false;
        this.validacaoOk = true;
        this.revertido = false;
        this.mensagemUsuario = "";
        this.mensagemTecnica = "";
    }

    public boolean isAplicou() {
        return aplicou;
    }

    public void setAplicou(boolean aplicou) {
        this.aplicou = aplicou;
    }

    public boolean isValidacaoOk() {
        return validacaoOk;
    }

    public void setValidacaoOk(boolean validacaoOk) {
        this.validacaoOk = validacaoOk;
    }

    public boolean isRevertido() {
        return revertido;
    }

    public void setRevertido(boolean revertido) {
        this.revertido = revertido;
    }

    public String getMensagemUsuario() {
        return mensagemUsuario;
    }

    public void setMensagemUsuario(String mensagemUsuario) {
        this.mensagemUsuario = mensagemUsuario;
    }

    public String getMensagemTecnica() {
        return mensagemTecnica;
    }

    public void setMensagemTecnica(String mensagemTecnica) {
        this.mensagemTecnica = mensagemTecnica;
    }

    public CodeApplicationState getEstadoAnterior() {
        return estadoAnterior;
    }

    public void setEstadoAnterior(CodeApplicationState estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }
}