package com.mcp.sailibrary.plugin.chat.service;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;

import com.mcp.sailibrary.plugin.chat.support.WorkspaceCompilationValidationResult;

/* --- version: "1.0" libraries: - IMarker - IResource - ICompilationUnit - WorkspaceCompilationValidationResult objetivo: "Validar o estado real do workspace Eclipse apos uma mutacao de codigo, detectando erros e warnings marcados no recurso." --- */

/** * Servico de validacao do workspace apos aplicacao de mutacao no editor. * * <p>Esta implementacao usa os markers do Eclipse associados ao recurso da * CompilationUnit para descobrir se a alteracao deixou o projeto com erros * reais. O objetivo e impedir falso sucesso quando o codigo aplicado nao * compila ou quebra contratos conhecidos do projeto.</p> * * @author Renato Tomaz Nati * @since 2026-05-24 */
public class CodeWorkspaceValidationService {

    /** * Caller: ChatAiController * Callee: validarMarkersDoRecurso * Objetivo: Validar o estado real do arquivo atual apos mutacao. * Feature: Usa markers do Eclipse como fonte de verdade local para impedir * sucesso falso de codigo quebrado. * Data modificacao: 2026-05-24 00:00 * * @param compilationUnit unidade de compilacao atual * @return resultado estruturado da validacao * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public WorkspaceCompilationValidationResult validarEstadoAtual(ICompilationUnit compilationUnit) {
        WorkspaceCompilationValidationResult resultado = new WorkspaceCompilationValidationResult();

        if (compilationUnit == null) {
            resultado.setResumo("Sem compilation unit para validacao.");
            return resultado;
        }

        try {
            compilationUnit.reconcile(ICompilationUnit.NO_AST, false, null, null);
        } catch (Exception e) {
            resultado.adicionarMensagemWarning("Falha ao reconciliar a compilation unit: " + e.getMessage());
        }

        try {
            IResource resource = compilationUnit.getResource();
            if (resource == null) {
                resultado.setResumo("Sem recurso Eclipse para validacao.");
                return resultado;
            }

            validarMarkersDoRecurso(resource, resultado);
        } catch (Exception e) {
            resultado.adicionarMensagemErro("Falha ao ler markers do recurso: " + e.getMessage());
        }

        if (resultado.isPossuiErros()) {
            resultado.setResumo("Workspace com erros reais apos a mutacao.");
        } else if (resultado.isPossuiWarnings()) {
            resultado.setResumo("Workspace sem erros, mas com warnings apos a mutacao.");
        } else {
            resultado.setResumo("Workspace validado sem erros.");
        }

        return resultado;
    }

    /** * Caller: validarEstadoAtual * Callee: N/A * Objetivo: Ler os markers de problema do recurso e consolidar erros e warnings. * Data modificacao: 2026-05-24 00:00 * * @param resource recurso Eclipse do arquivo * @param resultado resultado acumulado da validacao * * @author Renato Tomaz Nati * @since 2026-05-24 */
    private void validarMarkersDoRecurso(IResource resource, WorkspaceCompilationValidationResult resultado) throws Exception {
        IMarker[] markers = resource.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
        if (markers == null || markers.length == 0) {
            return;
        }

        for (int i = 0; i < markers.length; i++) {
            IMarker marker = markers[i];
            if (marker == null || !marker.exists()) {
                continue;
            }

            Object severityObject = marker.getAttribute(IMarker.SEVERITY);
            int severity = severityObject instanceof Integer ? ((Integer) severityObject).intValue() : -1;

            StringBuilder mensagem = new StringBuilder();

            Object lineNumber = marker.getAttribute(IMarker.LINE_NUMBER);
            if (lineNumber instanceof Integer && ((Integer) lineNumber).intValue() > 0) {
                mensagem.append("Linha ").append(((Integer) lineNumber).intValue()).append(": ");
            }

            Object message = marker.getAttribute(IMarker.MESSAGE);
            if (message != null) {
                mensagem.append(String.valueOf(message));
            }

            if (severity == IMarker.SEVERITY_ERROR) {
                resultado.adicionarMensagemErro(mensagem.toString());
            } else if (severity == IMarker.SEVERITY_WARNING) {
                resultado.adicionarMensagemWarning(mensagem.toString());
            }
        }
    }
}