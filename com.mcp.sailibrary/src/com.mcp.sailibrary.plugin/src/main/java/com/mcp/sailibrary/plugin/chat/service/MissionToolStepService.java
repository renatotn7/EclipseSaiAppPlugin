package com.mcp.sailibrary.plugin.chat.service;

import com.mcp.sailibrary.plugin.agent.orchestration.AgentOrchestrator;
import com.mcp.sailibrary.plugin.agent.context.analise.ProjectMemoryStore;
import com.mcp.sailibrary.plugin.chat.support.ToolResultSummarizer;
import com.mcp.sailibrary.plugin.chat.support.MissionExecutionContext;
import com.mcp.sailibrary.plugin.mcp.multimodel.policy.InvestigationCoveragePolicy;

/** * Executar e consolidar o passo de ferramenta dentro da missao. * * @author Renato Tomaz Nati * @since 2026-05-25 */
public class MissionToolStepService {

    public ToolStepResult executarFerramenta( String nomeFerramenta, String parametrosFerramenta, AgentOrchestrator orquestrador, ToolResultSummarizer toolResultPresenter, ProjectMemoryStore projectMemoryStoreLocal, SessionHistoryService sessionHistoryService, InvestigationCoveragePolicy investigationCoveragePolicy, InvestigationCoveragePolicy.CoveragePlan coveragePlan, MissionExecutionContext contexto) {

        ToolStepResult resultado = new ToolStepResult();

        if (nomeFerramenta == null || nomeFerramenta.trim().length() == 0) {
            resultado.setSucesso(false);
            resultado.setMensagemErro("Erro operacional: A IA solicitou ferramenta sem informar o nome.");
            return resultado;
        }

        String resultadoFerramenta = orquestrador.dispatch(nomeFerramenta, parametrosFerramenta);

        if (investigationCoveragePolicy != null && coveragePlan != null) {
            investigationCoveragePolicy.registrarUsoFerramenta(
                    coveragePlan,
                    nomeFerramenta,
                    resultadoFerramenta
            );
        }

        String resultadoFerramentaParaChat = toolResultPresenter.resumirParaChat(nomeFerramenta, parametrosFerramenta, resultadoFerramenta);
        String resultadoFerramentaParaMemoria = toolResultPresenter.resumirParaMemoria(resultadoFerramentaParaChat);

        if (projectMemoryStoreLocal != null) {
            projectMemoryStoreLocal.registrarToolHistory(nomeFerramenta, parametrosFerramenta, resultadoFerramentaParaMemoria);
        }

        if (sessionHistoryService != null) {
            sessionHistoryService.adicionar("[Ferramenta - " + nomeFerramenta + "]: " + resultadoFerramentaParaMemoria);
        }

        if (contexto != null) {
            contexto.setUltimoResultadoFerramentaBruto(resultadoFerramenta);
            contexto.setUltimoNomeFerramenta(nomeFerramenta);
            contexto.setUltimoResumoFerramenta(resultadoFerramentaParaChat);
        }

        resultado.setSucesso(true);
        resultado.setResultadoBruto(resultadoFerramenta);
        resultado.setResultadoParaChat(resultadoFerramentaParaChat);
        resultado.setResultadoParaMemoria(resultadoFerramentaParaMemoria);

        return resultado;
    }

    public static class ToolStepResult {
        private boolean sucesso;
        private String mensagemErro;
        private String resultadoBruto;
        private String resultadoParaChat;
        private String resultadoParaMemoria;

        public boolean isSucesso() {
            return sucesso;
        }

        public void setSucesso(boolean sucesso) {
            this.sucesso = sucesso;
        }

        public String getMensagemErro() {
            return mensagemErro;
        }

        public void setMensagemErro(String mensagemErro) {
            this.mensagemErro = mensagemErro;
        }

        public String getResultadoBruto() {
            return resultadoBruto;
        }

        public void setResultadoBruto(String resultadoBruto) {
            this.resultadoBruto = resultadoBruto;
        }

        public String getResultadoParaChat() {
            return resultadoParaChat;
        }

        public void setResultadoParaChat(String resultadoParaChat) {
            this.resultadoParaChat = resultadoParaChat;
        }

        public String getResultadoParaMemoria() {
            return resultadoParaMemoria;
        }

        public void setResultadoParaMemoria(String resultadoParaMemoria) {
            this.resultadoParaMemoria = resultadoParaMemoria;
        }
    }
}