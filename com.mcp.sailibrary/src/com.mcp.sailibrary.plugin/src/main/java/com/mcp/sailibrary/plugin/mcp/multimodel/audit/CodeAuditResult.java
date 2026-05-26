package com.mcp.sailibrary.plugin.mcp.multimodel.audit;

/* --- version: "1.2" libraries: - AuditExecutionStatus - AuditObservationLevel objetivo: "Transportar o resultado estruturado da auditoria tecnica, separando risco real de falha de infraestrutura e do nivel semantico das observacoes." --- */

/** * Resultado estruturado da auditoria de codigo. * * @author Renato Tomaz Nati * @since 2026-05-24 */
public class CodeAuditResult {

    private boolean aprovado;
    private boolean deveTentarNovamente;
    private String nivelRisco;
    private String feedback;
    private AuditExecutionStatus executionStatus;
    private AuditObservationLevel observationLevel;

    public CodeAuditResult() {
        this.aprovado = false;
        this.deveTentarNovamente = false;
        this.nivelRisco = "";
        this.feedback = "";
        this.executionStatus = AuditExecutionStatus.NAO_EXECUTADA;
        this.observationLevel = AuditObservationLevel.NENHUM;
    }

    public boolean isAprovado() {
        return aprovado;
    }

    public void setAprovado(boolean aprovado) {
        this.aprovado = aprovado;
    }

    public boolean isDeveTentarNovamente() {
        return deveTentarNovamente;
    }

    public void setDeveTentarNovamente(boolean deveTentarNovamente) {
        this.deveTentarNovamente = deveTentarNovamente;
    }

    public String getNivelRisco() {
        return nivelRisco;
    }

    public void setNivelRisco(String nivelRisco) {
        this.nivelRisco = nivelRisco;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public AuditExecutionStatus getExecutionStatus() {
        return executionStatus;
    }

    public void setExecutionStatus(AuditExecutionStatus executionStatus) {
        this.executionStatus = executionStatus;
    }

    public AuditObservationLevel getObservationLevel() {
        return observationLevel;
    }

    public void setObservationLevel(AuditObservationLevel observationLevel) {
        this.observationLevel = observationLevel;
    }

    public boolean isFalhaInfra() {
        return AuditExecutionStatus.FALHA_INFRA.equals(this.executionStatus);
    }

    public boolean isReprovadoRealmente() {
        return AuditExecutionStatus.REPROVADO.equals(this.executionStatus);
    }

    public boolean isAprovadoRealmente() {
        return AuditExecutionStatus.APROVADO.equals(this.executionStatus) && this.aprovado;
    }

    public boolean exigeConfirmacaoMesmoAprovado() {
        return AuditExecutionStatus.APROVADO.equals(this.executionStatus)
                && AuditObservationLevel.PONTO_ATENCAO.equals(this.observationLevel);
    }
}