package com.mcp.sailibrary.plugin.chat.controllers;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/* --- version: "1.1" libraries: - SimpleDateFormat - List - ArrayList - Date objetivo: "Controlar o log de atividade operacional do agente para exibicao na aba de atividade." --- */

/** * Controlador da aba de atividade do agente. * * <p>Responsavel por manter uma trilha textual das fases e eventos relevantes * da execucao do agente, para que o usuario veja o que esta acontecendo sem * depender apenas da barra de progresso.</p> * * @author Renato Tomaz Nati * @since 2026-05-24 */
public class ChatViewActivityController {

    private List<String> linhasAtividade;

    /** * Caller: ChatActivityPanel * Callee: N/A * Objetivo: Inicializar o controlador de atividade. * Data modificacao: 2026-05-24 00:00 * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public ChatViewActivityController() {
        this.linhasAtividade = new ArrayList<String>();
    }

    /** * Caller: ChatAiController, ChatActivityPanel, ChatView * Callee: N/A * Objetivo: Registrar uma nova linha de atividade formatada com horario e fase. * Data modificacao: 2026-05-24 00:00 * * @param fase nome da fase * @param detalhe detalhe textual * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public void registrarAtividade(String fase, String detalhe) {
        String horario = new SimpleDateFormat("HH:mm:ss").format(new Date());
        StringBuilder linha = new StringBuilder();

        linha.append("[").append(horario).append("] ");
        linha.append("[");
        linha.append(fase != null ? fase : "ATIVIDADE");
        linha.append("] ");
        linha.append(detalhe != null ? detalhe : "");

        linhasAtividade.add(linha.toString());
    }

    /** * Caller: ChatActivityPanel, ChatView * Callee: N/A * Objetivo: Limpar o historico visual de atividade. * Data modificacao: 2026-05-24 00:00 * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public void limparAtividade() {
        linhasAtividade.clear();
    }

    /** * Caller: ChatActivityPanel * Callee: N/A * Objetivo: Fornecer uma copia segura da atividade atual. * Data modificacao: 2026-05-24 00:00 * * @return lista copiada de linhas * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public List<String> getLinhasAtividade() {
        return new ArrayList<String>(linhasAtividade);
    }

    /** * Caller: ChatActivityPanel * Callee: N/A * Objetivo: Renderizar o conteudo textual consolidado da atividade. * Data modificacao: 2026-05-24 00:00 * * @return texto consolidado * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public String renderizarTextoAtividade() {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < linhasAtividade.size(); i++) {
            builder.append(linhasAtividade.get(i)).append(System.lineSeparator());
        }

        return builder.toString();
    }

    /** * Caller: ChatActivityPanel * Callee: N/A * Objetivo: Informar a mensagem de status para a aba de atividade. * Data modificacao: 2026-05-24 00:00 * * @return mensagem de status * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public String getMensagemStatus() {
        if (linhasAtividade.isEmpty()) {
            return "Sem atividade em andamento.";
        }

        return "Agente em atividade. Veja os detalhes nesta aba.";
    }
}