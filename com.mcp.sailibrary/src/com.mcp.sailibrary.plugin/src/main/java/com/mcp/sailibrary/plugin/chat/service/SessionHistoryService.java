package com.mcp.sailibrary.plugin.chat.service;

/**
 * Encapsular a memoria efemera da sessao do chat com acesso sincronizado.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-18
 */
public class SessionHistoryService {

    private final StringBuffer historicoSessao;

    public SessionHistoryService() {
        this.historicoSessao = new StringBuffer();
    }

    public void adicionar(String mensagem) {
        if (mensagem == null || mensagem.trim().length() == 0) {
            return;
        }

        synchronized (historicoSessao) {
            historicoSessao.append("\n").append(mensagem);
        }
    }

    public void limpar() {
        synchronized (historicoSessao) {
            historicoSessao.setLength(0);
        }
    }

    public String obter() {
        synchronized (historicoSessao) {
            return historicoSessao.toString();
        }
    }

    public boolean possuiConteudo() {
        synchronized (historicoSessao) {
            return historicoSessao.length() > 0;
        }
    }
}