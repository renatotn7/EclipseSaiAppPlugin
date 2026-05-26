package com.mcp.sailibrary.plugin.mcp.multimodel.policy;

import com.mcp.sailibrary.plugin.mcp.multimodel.audit.AuditExecutionStatus;
import com.mcp.sailibrary.plugin.mcp.multimodel.audit.AuditObservationLevel;
import com.mcp.sailibrary.plugin.mcp.multimodel.audit.CodeAuditResult;

/* --- version: "1.1" libraries: - CodeAuditResult - AuditExecutionStatus - AuditObservationLevel - FinalApplicationDecision objetivo: "Separar decisao de aplicacao do risco arquitetural, da falha tecnica do auditor e do nivel semantico das observacoes." --- */

/** * Politica padrao de decisao final da aplicacao. * * @author Renato Tomaz Nati * @since 2026-05-25 */
public class DefaultFinalApplicationDecisionPolicy {

    public FinalApplicationDecision decide(CodeAuditResult auditResult, String pedidoOriginal) {
        if (auditResult == null) {
            return hasExplicitUserAuthorization(pedidoOriginal)
                    ? FinalApplicationDecision.APLICAR_COM_CONFIRMACAO
                    : FinalApplicationDecision.BLOQUEAR;
        }

        AuditExecutionStatus status = auditResult.getExecutionStatus();

        if (AuditExecutionStatus.APROVADO.equals(status)) {
            if (AuditObservationLevel.PONTO_ATENCAO.equals(auditResult.getObservationLevel())) {
                return FinalApplicationDecision.APLICAR_COM_CONFIRMACAO;
            }
            return FinalApplicationDecision.APLICAR;
        }

        if (AuditExecutionStatus.FALHA_INFRA.equals(status)) {
            return hasExplicitUserAuthorization(pedidoOriginal)
                    ? FinalApplicationDecision.APLICAR_COM_CONFIRMACAO
                    : FinalApplicationDecision.BLOQUEAR;
        }

        if (AuditExecutionStatus.REPROVADO.equals(status)) {
            return FinalApplicationDecision.BLOQUEAR;
        }

        return hasExplicitUserAuthorization(pedidoOriginal)
                ? FinalApplicationDecision.APLICAR_COM_CONFIRMACAO
                : FinalApplicationDecision.BLOQUEAR;
    }

    public String buildUserConfirmationQuestion(CodeAuditResult auditResult) {
        StringBuilder pergunta = new StringBuilder();

        if (auditResult != null && AuditExecutionStatus.APROVADO.equals(auditResult.getExecutionStatus())
                && AuditObservationLevel.PONTO_ATENCAO.equals(auditResult.getObservationLevel())) {
            pergunta.append("A auditoria aprovou a alteracao, mas registrou um ponto de atencao.");
            pergunta.append(System.lineSeparator());
            pergunta.append("Deseja aplicar mesmo assim e validar pelo workspace Eclipse?");
        } else {
            pergunta.append("A auditoria final nao pode ser concluida por falha tecnica do modelo auditor.");
            pergunta.append(System.lineSeparator());
            pergunta.append("Deseja aplicar mesmo assim e validar pelo workspace Eclipse?");
        }

        if (auditResult != null && auditResult.getFeedback() != null && auditResult.getFeedback().trim().length() > 0) {
            pergunta.append(System.lineSeparator());
            pergunta.append("Detalhe tecnico: ").append(auditResult.getFeedback());
        }

        return pergunta.toString();
    }

    public String buildBlockingMessage(CodeAuditResult auditResult) {
        StringBuilder mensagem = new StringBuilder();
        mensagem.append("A implementacao foi bloqueada pela auditoria.");

        if (auditResult != null && auditResult.getNivelRisco() != null && auditResult.getNivelRisco().trim().length() > 0) {
            mensagem.append(System.lineSeparator()).append("Risco: ").append(auditResult.getNivelRisco());
        }

        if (auditResult != null && auditResult.getFeedback() != null && auditResult.getFeedback().trim().length() > 0) {
            mensagem.append(System.lineSeparator()).append("Feedback: ").append(auditResult.getFeedback());
        }

        return mensagem.toString();
    }

    private boolean hasExplicitUserAuthorization(String pedidoOriginal) {
        String texto = pedidoOriginal != null ? pedidoOriginal.toLowerCase() : "";
        return texto.contains("pode implementar")
                || texto.contains("eu autorizo")
                || texto.contains("simplesmente substitua")
                || texto.contains("aplique mesmo assim")
                || texto.contains("pode aplicar");
    }
}