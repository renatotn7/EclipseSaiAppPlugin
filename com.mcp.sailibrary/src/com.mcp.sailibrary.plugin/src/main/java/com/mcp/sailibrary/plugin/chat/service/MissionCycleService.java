package com.mcp.sailibrary.plugin.chat.service;

import com.mcp.sailibrary.plugin.chat.support.AiResponse;
import com.mcp.sailibrary.plugin.chat.support.MissionExecutionContext;
import com.mcp.sailibrary.plugin.mcp.multimodel.policy.InvestigationCoveragePolicy;

/** * Regras operacionais do ciclo da missao. * * @author Renato Tomaz Nati * @since 2026-05-25 */
public class MissionCycleService {

    public boolean deveForcarNovaInvestigacao( AiResponse respostaIA, InvestigationCoveragePolicy investigationCoveragePolicy, InvestigationCoveragePolicy.CoveragePlan coveragePlan) {

        if (respostaIA == null) {
            return false;
        }

        if (investigationCoveragePolicy == null || coveragePlan == null) {
            return false;
        }

        if ("perguntar_ao_usuario".equalsIgnoreCase(respostaIA.getAction())) {
            return false;
        }

        return !investigationCoveragePolicy.podeConcluir(coveragePlan);
    }

    public String construirInstrucaoCoberturaPendente( String instrucaoAtual, InvestigationCoveragePolicy investigationCoveragePolicy, InvestigationCoveragePolicy.CoveragePlan coveragePlan) {

        if (investigationCoveragePolicy == null || coveragePlan == null) {
            return instrucaoAtual;
        }

        String pendencias = investigationCoveragePolicy.buildPendenciasMensagem(coveragePlan);

        StringBuilder novaInstrucao = new StringBuilder();
        novaInstrucao.append(instrucaoAtual != null ? instrucaoAtual : "");
        novaInstrucao.append("\n\n=== COBERTURA OBRIGATORIA NAO SATISFEITA ===\n");
        novaInstrucao.append(pendencias);
        novaInstrucao.append("\nContinue investigando antes de concluir.\n");

        return novaInstrucao.toString();
    }

    public boolean processarSinalizacaoDeProximidade(MissionExecutionContext contexto, AiResponse respostaIA) {
        if (contexto == null || respostaIA == null || respostaIA.getExplanation() == null) {
            return false;
        }

        String explanation = respostaIA.getExplanation().toUpperCase();

        if (explanation.contains("[PERTO_DA_SOLUCAO]")
                && contexto.getExtensoesUsadas() < contexto.getExtensoesPermitidas()
                && contexto.getIteracaoAtual() >= (contexto.getIteracoesMaximas() - 3)) {
            contexto.registrarExtensaoCiclos(10);
            return true;
        }

        return false;
    }

    public boolean deveEmitirAlertaProximidade(MissionExecutionContext contexto) {
        if (contexto == null) {
            return false;
        }

        return !contexto.isAlertaProximidadeEnviado()
                && (contexto.getIteracoesMaximas() - contexto.getIteracaoAtual()) <= 2;
    }

    public String anexarAlertaProximidade(String instrucaoAtual) {
        StringBuilder novaInstrucao = new StringBuilder();
        novaInstrucao.append(instrucaoAtual != null ? instrucaoAtual : "");
        novaInstrucao.append("\n\n[ALERTA DE SISTEMA]: O limite de ciclos autonomos esta acabando. Na sua proxima explanation, inclua obrigatoriamente a tag [PERTO_DA_SOLUCAO] ou [LONGE_DA_SOLUCAO].");
        return novaInstrucao.toString();
    }

    public boolean respostaFinalNaoDestrutiva(AiResponse respostaIA) {
        if (respostaIA == null || respostaIA.getAction() == null) {
            return false;
        }

        return "responder_ao_usuario".equalsIgnoreCase(respostaIA.getAction())
                || "explicar".equalsIgnoreCase(respostaIA.getAction())
                || "perguntar_ao_usuario".equalsIgnoreCase(respostaIA.getAction());
    }
}