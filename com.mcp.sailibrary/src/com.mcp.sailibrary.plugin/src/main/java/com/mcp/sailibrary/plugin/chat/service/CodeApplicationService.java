package com.mcp.sailibrary.plugin.chat.service;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;

import com.mcp.sailibrary.plugin.chat.support.CodeApplicationResult;
import com.mcp.sailibrary.plugin.chat.support.CodeApplicationState;

/** * Aplicar mutacoes no documento com suporte a captura de estado, * sincronizacao com compilation unit e reversao em caso de falha. * * @author Renato Tomaz Nati * @since 2026-05-25 */
public class CodeApplicationService {

    public CodeApplicationState capturarEstadoAntesDaAplicacao(IDocument document, ITextSelection selection) throws Exception {
        if (document == null || selection == null) {
            return null;
        }

        CodeApplicationState estado = new CodeApplicationState();

        int offsetInicial = selection.getOffset();
        int comprimentoOriginal = selection.getLength();
        String conteudoAnterior = document.get(offsetInicial, comprimentoOriginal);

        estado.setOffsetInicial(offsetInicial);
        estado.setComprimentoOriginal(comprimentoOriginal);
        estado.setConteudoAnterior(conteudoAnterior);

        return estado;
    }

    public CodeApplicationResult aplicarEdicaoNoDocumento(IDocument document, String action, String conteudoNovo, CodeApplicationState estado) throws Exception {
        CodeApplicationResult resultado = new CodeApplicationResult();
        resultado.setEstadoAnterior(estado);
        resultado.setAplicou(false);
        resultado.setValidacaoOk(true);
        resultado.setRevertido(false);

        if (document == null) {
            resultado.setMensagemUsuario("Documento ausente para aplicacao.");
            resultado.setMensagemTecnica("IDocument nulo em aplicarEdicaoNoDocumento.");
            return resultado;
        }

        if (estado == null) {
            resultado.setMensagemUsuario("Estado anterior da aplicacao ausente.");
            resultado.setMensagemTecnica("CodeApplicationState nulo em aplicarEdicaoNoDocumento.");
            return resultado;
        }

        String conteudo = conteudoNovo != null ? conteudoNovo : "";

        if ("substituir".equalsIgnoreCase(action)) {
            document.replace(
                    estado.getOffsetInicial(),
                    estado.getComprimentoOriginal(),
                    conteudo
            );
            resultado.setAplicou(true);
            resultado.setMensagemUsuario("Substituicao aplicada com sucesso.");
            resultado.setMensagemTecnica("Acao substituir aplicada com sucesso.");
            return resultado;
        }

        if ("inserir_abaixo".equalsIgnoreCase(action)) {
            int offsetFinal = estado.getOffsetInicial() + estado.getComprimentoOriginal();
            document.replace(offsetFinal, 0, System.lineSeparator() + conteudo);
            resultado.setAplicou(true);
            resultado.setMensagemUsuario("Insercao abaixo aplicada com sucesso.");
            resultado.setMensagemTecnica("Acao inserir_abaixo aplicada com sucesso.");
            return resultado;
        }

        if ("anexar_acima".equalsIgnoreCase(action)) {
            document.replace(estado.getOffsetInicial(), 0, conteudo + System.lineSeparator());
            resultado.setAplicou(true);
            resultado.setMensagemUsuario("Insercao acima aplicada com sucesso.");
            resultado.setMensagemTecnica("Acao anexar_acima aplicada com sucesso.");
            return resultado;
        }

        if ("comentar".equalsIgnoreCase(action)) {
            document.replace(
                    estado.getOffsetInicial(),
                    estado.getComprimentoOriginal(),
                    conteudo
            );
            resultado.setAplicou(true);
            resultado.setMensagemUsuario("Comentario aplicado com sucesso.");
            resultado.setMensagemTecnica("Acao comentar aplicada com sucesso.");
            return resultado;
        }

        resultado.setMensagemUsuario("Acao de edicao nao suportada: " + action);
        resultado.setMensagemTecnica("Acao de edicao nao suportada recebida em aplicarEdicaoNoDocumento: " + action);
        return resultado;
    }

    public void sincronizarDocumentoComCompilationUnit(IDocument document, ICompilationUnit compUnit) {
        if (document == null || compUnit == null) {
            return;
        }

        try {
            compUnit.getBuffer().setContents(document.get());
            compUnit.makeConsistent(null);
        } catch (Exception e) {
            System.out.println("[CHAT APPLY DEBUG] Falha ao sincronizar buffer do compilation unit: " + e.getMessage());
        }
    }

    public CodeApplicationResult reverterAplicacaoAposFalha(IDocument document, ICompilationUnit compUnit, String action, String conteudoNovo, CodeApplicationState estado) {
        CodeApplicationResult resultado = new CodeApplicationResult();
        resultado.setEstadoAnterior(estado);
        resultado.setAplicou(false);
        resultado.setValidacaoOk(false);
        resultado.setRevertido(false);

        if (document == null || estado == null) {
            resultado.setMensagemUsuario("Nao foi possivel reverter a alteracao automaticamente.");
            resultado.setMensagemTecnica("Nao foi possivel reverter a aplicacao por ausencia de documento ou estado anterior.");
            return resultado;
        }

        try {
            int comprimentoAplicado = calcularComprimentoAplicado(
                    action,
                    conteudoNovo,
                    estado.getComprimentoOriginal()
            );

            document.replace(
                    estado.getOffsetInicial(),
                    comprimentoAplicado,
                    estado.getConteudoAnterior() != null ? estado.getConteudoAnterior() : ""
            );

            sincronizarDocumentoComCompilationUnit(document, compUnit);

            resultado.setRevertido(true);
            resultado.setMensagemUsuario("Alteracao revertida com sucesso apos falha de validacao.");
            resultado.setMensagemTecnica("Alteracao revertida com sucesso apos falha de validacao do workspace.");
            return resultado;
        } catch (Exception e) {
            System.out.println("[WORKSPACE VALIDATION DEBUG] Falha ao restaurar estado anterior: " + e.getMessage());
            e.printStackTrace();
            resultado.setMensagemUsuario("Falha ao restaurar estado anterior automaticamente.");
            resultado.setMensagemTecnica("Falha ao restaurar estado anterior: " + e.getMessage());
            return resultado;
        }
    }

    public int calcularComprimentoAplicado(String action, String conteudoNovo, int comprimentoOriginal) {
        if ("substituir".equalsIgnoreCase(action) || "comentar".equalsIgnoreCase(action)) {
            return conteudoNovo != null ? conteudoNovo.length() : 0;
        }

        if ("inserir_abaixo".equalsIgnoreCase(action)) {
            return (conteudoNovo != null ? conteudoNovo.length() : 0) + System.lineSeparator().length();
        }

        if ("anexar_acima".equalsIgnoreCase(action)) {
            return (conteudoNovo != null ? conteudoNovo.length() : 0) + System.lineSeparator().length();
        }

        return comprimentoOriginal;
    }

    public void logAplicacaoDebug(String action, CodeApplicationState estado, String conteudoNovo) {
        if (estado == null) {
            return;
        }

        System.out.println("[CHAT APPLY DEBUG] action=" + action);
        System.out.println("[CHAT APPLY DEBUG] offsetInicial=" + estado.getOffsetInicial());
        System.out.println("[CHAT APPLY DEBUG] comprimentoOriginal=" + estado.getComprimentoOriginal());
        System.out.println("[CHAT APPLY DEBUG] conteudoNovoLength=" + (conteudoNovo != null ? conteudoNovo.length() : 0));
    }
}