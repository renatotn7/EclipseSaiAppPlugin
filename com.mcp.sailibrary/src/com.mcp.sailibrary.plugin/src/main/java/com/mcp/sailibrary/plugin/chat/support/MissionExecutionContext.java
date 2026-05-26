package com.mcp.sailibrary.plugin.chat.support;

import java.io.File;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;

import com.mcp.sailibrary.plugin.agent.context.analise.ProjectMemoryStore;
import com.mcp.sailibrary.plugin.mcp.multimodel.policy.InvestigationCoveragePolicy;

/** * Contexto mutavel da execucao de uma missao da IA. * * @author Renato Tomaz Nati * @since 2026-05-25 */
public class MissionExecutionContext {

    private long tokenMissao;
    private String instrucaoSnapshot;
    private String pedidoOriginalSnapshot;
    private String selectedCodeSnapshot;
    private String fullFileTextSnapshot;
    private String apiKeySnapshot;
    private IDocument documentSnapshot;
    private ITextSelection selectionSnapshot;
    private ICompilationUnit compUnitSnapshot;
    private File raizProjeto;
    private int offsetAtual;

    private InvestigationCoveragePolicy.CoveragePlan coveragePlan;
    private ProjectMemoryStore projectMemoryStoreLocal;

    private int iteracoesMaximas = 30;
    private int iteracaoAtual = 0;
    private boolean missaoConcluida = false;

    private int extensoesPermitidas = 1;
    private int extensoesUsadas = 0;
    private boolean alertaProximidadeEnviado = false;

    private String instrucaoEnriquecida;

    private AiResponse ultimaRespostaEstruturadaValida;
    private String ultimoResultadoFerramentaBruto = "";
    private String ultimoNomeFerramenta = "";
    private String ultimoResumoFerramenta = "";

    public long getTokenMissao() {
        return tokenMissao;
    }

    public void setTokenMissao(long tokenMissao) {
        this.tokenMissao = tokenMissao;
    }

    public String getInstrucaoSnapshot() {
        return instrucaoSnapshot;
    }

    public void setInstrucaoSnapshot(String instrucaoSnapshot) {
        this.instrucaoSnapshot = instrucaoSnapshot;
    }

    public String getPedidoOriginalSnapshot() {
        return pedidoOriginalSnapshot;
    }

    public void setPedidoOriginalSnapshot(String pedidoOriginalSnapshot) {
        this.pedidoOriginalSnapshot = pedidoOriginalSnapshot;
    }

    public String getSelectedCodeSnapshot() {
        return selectedCodeSnapshot;
    }

    public void setSelectedCodeSnapshot(String selectedCodeSnapshot) {
        this.selectedCodeSnapshot = selectedCodeSnapshot;
    }

    public String getFullFileTextSnapshot() {
        return fullFileTextSnapshot;
    }

    public void setFullFileTextSnapshot(String fullFileTextSnapshot) {
        this.fullFileTextSnapshot = fullFileTextSnapshot;
    }

    public String getApiKeySnapshot() {
        return apiKeySnapshot;
    }

    public void setApiKeySnapshot(String apiKeySnapshot) {
        this.apiKeySnapshot = apiKeySnapshot;
    }

    public IDocument getDocumentSnapshot() {
        return documentSnapshot;
    }

    public void setDocumentSnapshot(IDocument documentSnapshot) {
        this.documentSnapshot = documentSnapshot;
    }

    public ITextSelection getSelectionSnapshot() {
        return selectionSnapshot;
    }

    public void setSelectionSnapshot(ITextSelection selectionSnapshot) {
        this.selectionSnapshot = selectionSnapshot;
    }

    public ICompilationUnit getCompUnitSnapshot() {
        return compUnitSnapshot;
    }

    public void setCompUnitSnapshot(ICompilationUnit compUnitSnapshot) {
        this.compUnitSnapshot = compUnitSnapshot;
    }

    public File getRaizProjeto() {
        return raizProjeto;
    }

    public void setRaizProjeto(File raizProjeto) {
        this.raizProjeto = raizProjeto;
    }

    public int getOffsetAtual() {
        return offsetAtual;
    }

    public void setOffsetAtual(int offsetAtual) {
        this.offsetAtual = offsetAtual;
    }

    public InvestigationCoveragePolicy.CoveragePlan getCoveragePlan() {
        return coveragePlan;
    }

    public void setCoveragePlan(InvestigationCoveragePolicy.CoveragePlan coveragePlan) {
        this.coveragePlan = coveragePlan;
    }

    public ProjectMemoryStore getProjectMemoryStoreLocal() {
        return projectMemoryStoreLocal;
    }

    public void setProjectMemoryStoreLocal(ProjectMemoryStore projectMemoryStoreLocal) {
        this.projectMemoryStoreLocal = projectMemoryStoreLocal;
    }

    public int getIteracoesMaximas() {
        return iteracoesMaximas;
    }

    public void setIteracoesMaximas(int iteracoesMaximas) {
        this.iteracoesMaximas = iteracoesMaximas;
    }

    public int getIteracaoAtual() {
        return iteracaoAtual;
    }

    public void setIteracaoAtual(int iteracaoAtual) {
        this.iteracaoAtual = iteracaoAtual;
    }

    public boolean isMissaoConcluida() {
        return missaoConcluida;
    }

    public void setMissaoConcluida(boolean missaoConcluida) {
        this.missaoConcluida = missaoConcluida;
    }

    public int getExtensoesPermitidas() {
        return extensoesPermitidas;
    }

    public void setExtensoesPermitidas(int extensoesPermitidas) {
        this.extensoesPermitidas = extensoesPermitidas;
    }

    public int getExtensoesUsadas() {
        return extensoesUsadas;
    }

    public void setExtensoesUsadas(int extensoesUsadas) {
        this.extensoesUsadas = extensoesUsadas;
    }

    public boolean isAlertaProximidadeEnviado() {
        return alertaProximidadeEnviado;
    }

    public void setAlertaProximidadeEnviado(boolean alertaProximidadeEnviado) {
        this.alertaProximidadeEnviado = alertaProximidadeEnviado;
    }

    public String getInstrucaoEnriquecida() {
        return instrucaoEnriquecida;
    }

    public void setInstrucaoEnriquecida(String instrucaoEnriquecida) {
        this.instrucaoEnriquecida = instrucaoEnriquecida;
    }

    public AiResponse getUltimaRespostaEstruturadaValida() {
        return ultimaRespostaEstruturadaValida;
    }

    public void setUltimaRespostaEstruturadaValida(AiResponse ultimaRespostaEstruturadaValida) {
        this.ultimaRespostaEstruturadaValida = ultimaRespostaEstruturadaValida;
    }

    public String getUltimoResultadoFerramentaBruto() {
        return ultimoResultadoFerramentaBruto;
    }

    public void setUltimoResultadoFerramentaBruto(String ultimoResultadoFerramentaBruto) {
        this.ultimoResultadoFerramentaBruto = ultimoResultadoFerramentaBruto;
    }

    public String getUltimoNomeFerramenta() {
        return ultimoNomeFerramenta;
    }

    public void setUltimoNomeFerramenta(String ultimoNomeFerramenta) {
        this.ultimoNomeFerramenta = ultimoNomeFerramenta;
    }

    public String getUltimoResumoFerramenta() {
        return ultimoResumoFerramenta;
    }

    public void setUltimoResumoFerramenta(String ultimoResumoFerramenta) {
        this.ultimoResumoFerramenta = ultimoResumoFerramenta;
    }

    public void avancarIteracao() {
        this.iteracaoAtual++;
    }

    public boolean podeExecutarNovoCiclo() {
        return this.iteracaoAtual < this.iteracoesMaximas && !this.missaoConcluida;
    }

    public void registrarExtensaoCiclos(int quantidade) {
        this.iteracoesMaximas += quantidade;
        this.extensoesUsadas++;
    }
}