package com.mcp.sailibrary.plugin.chat.support;

import java.util.ArrayList;
import java.util.List;

/* --- version: "1.0" libraries: - List - ArrayList objetivo: "Transportar o resultado da validacao real do workspace Eclipse apos uma mutacao de codigo." --- */

/** * Resultado da validacao de compilacao e consistencia do workspace apos uma * alteracao aplicada no editor. * * @author Renato Tomaz Nati * @since 2026-05-24 */
public class WorkspaceCompilationValidationResult {

    private boolean possuiErros;
    private boolean possuiWarnings;
    private String resumo;
    private String causaRaizPrincipal;
    private List<String> mensagensErro;
    private List<String> mensagensWarning;

    /** * Caller: CodeWorkspaceValidationService * Callee: N/A * Objetivo: Inicializar o resultado com colecoes vazias e estado limpo. * Data modificacao: 2026-05-24 00:00 * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public WorkspaceCompilationValidationResult() {
        this.possuiErros = false;
        this.possuiWarnings = false;
        this.resumo = "";
        this.causaRaizPrincipal = "";
        this.mensagensErro = new ArrayList<String>();
        this.mensagensWarning = new ArrayList<String>();
    }

    public boolean isPossuiErros() {
        return possuiErros;
    }

    public void setPossuiErros(boolean possuiErros) {
        this.possuiErros = possuiErros;
    }

    public boolean isPossuiWarnings() {
        return possuiWarnings;
    }

    public void setPossuiWarnings(boolean possuiWarnings) {
        this.possuiWarnings = possuiWarnings;
    }

    public String getResumo() {
        return resumo;
    }

    public void setResumo(String resumo) {
        this.resumo = safeTrim(resumo);
    }

    public String getCausaRaizPrincipal() {
        return causaRaizPrincipal;
    }

    public void setCausaRaizPrincipal(String causaRaizPrincipal) {
        this.causaRaizPrincipal = safeTrim(causaRaizPrincipal);
    }

    public void adicionarMensagemErro(String mensagem) {
        String mensagemSegura = safeTrim(mensagem);
        if (mensagemSegura.length() == 0) {
            return;
        }

        this.mensagensErro.add(mensagemSegura);
        this.possuiErros = true;

        if (this.causaRaizPrincipal.length() == 0) {
            this.causaRaizPrincipal = mensagemSegura;
        }
    }

    public void adicionarMensagemWarning(String mensagem) {
        String mensagemSegura = safeTrim(mensagem);
        if (mensagemSegura.length() == 0) {
            return;
        }

        this.mensagensWarning.add(mensagemSegura);
        this.possuiWarnings = true;
    }

    public List<String> getMensagensErro() {
        return new ArrayList<String>(mensagensErro);
    }

    public List<String> getMensagensWarning() {
        return new ArrayList<String>(mensagensWarning);
    }

    /** * Caller: ChatAiController * Callee: N/A * Objetivo: Montar uma descricao detalhada da validacao. * Data modificacao: 2026-05-24 00:00 * * @return texto detalhado da validacao * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public String toDetailedString() {
        StringBuilder builder = new StringBuilder();

        builder.append("possuiErros=").append(possuiErros).append(System.lineSeparator());
        builder.append("possuiWarnings=").append(possuiWarnings).append(System.lineSeparator());

        if (resumo.length() > 0) {
            builder.append("resumo=").append(resumo).append(System.lineSeparator());
        }

        if (causaRaizPrincipal.length() > 0) {
            builder.append("causaRaizPrincipal=").append(causaRaizPrincipal).append(System.lineSeparator());
        }

        if (!mensagensErro.isEmpty()) {
            builder.append("erros:").append(System.lineSeparator());
            for (int i = 0; i < mensagensErro.size(); i++) {
                builder.append("- ").append(mensagensErro.get(i)).append(System.lineSeparator());
            }
        }

        if (!mensagensWarning.isEmpty()) {
            builder.append("warnings:").append(System.lineSeparator());
            for (int i = 0; i < mensagensWarning.size(); i++) {
                builder.append("- ").append(mensagensWarning.get(i)).append(System.lineSeparator());
            }
        }

        return builder.toString().trim();
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}