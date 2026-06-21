package com.mcp.sailibrary.plugin.chat.controllers;

import java.io.File;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mcp.sailibrary.plugin.agent.context.ContextOrchestrator;
import com.mcp.sailibrary.plugin.agent.context.analise.ProjectMemoryStore;
import com.mcp.sailibrary.plugin.agent.orchestration.AgentOrchestrator;
import com.mcp.sailibrary.plugin.chat.service.CodeApplicationService;
import com.mcp.sailibrary.plugin.chat.service.CodeWorkspaceValidationService;
import com.mcp.sailibrary.plugin.chat.service.McpAgentResponseService;
import com.mcp.sailibrary.plugin.chat.service.SessionHistoryService;
import com.mcp.sailibrary.plugin.chat.settings.ChatRuntimeSettings;
import com.mcp.sailibrary.plugin.chat.support.AiResponse;
import com.mcp.sailibrary.plugin.chat.support.CodeApplicationState;
import com.mcp.sailibrary.plugin.chat.support.ToolResultSummarizer;
import com.mcp.sailibrary.plugin.chat.support.WorkspaceCompilationValidationResult;
import com.mcp.sailibrary.plugin.chat.views.ChatView;

import com.mcp.sailibrary.plugin.mcp.multimodel.coordinator.AgentModelCoordinator;
import com.mcp.sailibrary.plugin.mcp.multimodel.coordinator.MultiModelCoordinator;
import com.mcp.sailibrary.plugin.mcp.multimodel.coordinator.SingleModelCoordinator;
import com.mcp.sailibrary.plugin.mcp.multimodel.coverage.InvestigationCoverageTracker;
import com.mcp.sailibrary.plugin.mcp.multimodel.policy.ComplexityProfilePolicy;
import com.mcp.sailibrary.plugin.mcp.multimodel.policy.InvestigationCoveragePolicy;
import com.mcp.sailibrary.plugin.mcp.multimodel.policy.InvestigationCoveragePolicy.CoveragePlan;
import com.mcp.sailibrary.plugin.chat.blocks.model.NamedCodeBlock;
import com.mcp.sailibrary.plugin.chat.blocks.service.EditorNavigationService;
import com.mcp.sailibrary.plugin.chat.blocks.service.NamedBlockDocumentBindingService;
import com.mcp.sailibrary.plugin.chat.blocks.service.NamedBlockHighlighter;
import com.mcp.sailibrary.plugin.chat.blocks.service.NamedBlockPromptFormatter;
import com.mcp.sailibrary.plugin.chat.blocks.service.NamedBlockSessionService;
import com.mcp.sailibrary.plugin.chat.context.model.NamedContextTargetRole;
import com.mcp.sailibrary.plugin.chat.context.model.NamedStructuralContext;
import com.mcp.sailibrary.plugin.chat.context.model.NamedStructuralContextType;
import com.mcp.sailibrary.plugin.chat.context.service.NamedStructuralContextPromptFormatter;
import com.mcp.sailibrary.plugin.chat.context.service.NamedStructuralContextSessionService;
import com.mcp.sailibrary.plugin.chat.service.CodeApplicationService;
import com.mcp.sailibrary.plugin.chat.service.MissionCycleService;
import com.mcp.sailibrary.plugin.chat.service.MissionToolStepService;
import com.mcp.sailibrary.plugin.chat.service.MissionToolStepService.ToolStepResult;
import com.mcp.sailibrary.plugin.chat.support.CodeApplicationResult;
import com.mcp.sailibrary.plugin.chat.support.CodeApplicationState;
import com.mcp.sailibrary.plugin.chat.support.MissionExecutionContext;
/**
 * Isolar a logica de negocio, manipulacao de AST e integracao com IA da camada visual.
 *
 * @author Renato Tomaz Nati
 */
public class ChatAiController {

    private ChatView view;
    private String selectedCode;
    private String fullFileText;
    private String apiKey;
    private IDocument document;
    private ITextSelection selection;
    private ICompilationUnit compUnit;
    private boolean primeiroEco = true;
    private ITextEditor textEditor;
    private org.eclipse.jface.text.source.Annotation activeAnnotation;
    private boolean debug = false;
    private ProjectMemoryStore projectMemoryStore;
    private File raizProjetoSegura;
    private ContextOrchestrator contextOrchestrator;
    private SessionHistoryService sessionHistoryService;
    private ToolResultSummarizer toolResultPresenter;
    private McpAgentResponseService mcpResponseService;
    private NamedBlockSessionService namedBlockSessionService;
    private NamedBlockPromptFormatter namedBlockPromptFormatter;
    private volatile boolean missaoCancelada = false;
    private volatile long tokenMissaoAtual = 0L;
    private NamedBlockDocumentBindingService namedBlockDocumentBindingService;
    private NamedBlockHighlighter namedBlockHighlighter;
    private NamedStructuralContextSessionService namedStructuralContextSessionService;
    private NamedStructuralContextPromptFormatter namedStructuralContextPromptFormatter;
    private EditorNavigationService editorNavigationService;
    private AgentModelCoordinator agentModelCoordinator;
	private CodeWorkspaceValidationService codeWorkspaceValidationService;
	private ComplexityProfilePolicy complexityProfilePolicy;
	private InvestigationCoveragePolicy investigationCoveragePolicy;
	private CodeApplicationService codeApplicationService;
	private MissionCycleService missionCycleService;
	private MissionToolStepService missionToolStepService;
    /**
 * Construtor principal do controlador visual.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-18
 */
	public ChatAiController(ChatView view) {
	    this.contextOrchestrator = new ContextOrchestrator();
	    this.sessionHistoryService = new SessionHistoryService();
	    this.toolResultPresenter = new ToolResultSummarizer();
	    this.mcpResponseService = new McpAgentResponseService();
	    this.namedBlockSessionService = NamedBlockSessionService.getInstance();
	    this.namedBlockPromptFormatter = new NamedBlockPromptFormatter();
	    this.namedBlockDocumentBindingService = new NamedBlockDocumentBindingService();
	    this.namedBlockHighlighter = NamedBlockHighlighter.getInstance();
	    this.namedStructuralContextSessionService = NamedStructuralContextSessionService.getInstance();
	    this.namedStructuralContextPromptFormatter = new NamedStructuralContextPromptFormatter();
	    this.editorNavigationService = new EditorNavigationService();
	    this.agentModelCoordinator = new MultiModelCoordinator();
	    this.codeWorkspaceValidationService = new CodeWorkspaceValidationService();
	    this.complexityProfilePolicy = new ComplexityProfilePolicy();
	    this.investigationCoveragePolicy = new InvestigationCoveragePolicy();
	    this.codeApplicationService = new CodeApplicationService();
	    this.missionCycleService = new MissionCycleService();
	    this.missionToolStepService = new MissionToolStepService();
	    this.view = view;
	}

    /**
 * Alterna o modo debug da sessao atual.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-18
 */
    public void setDebugAtivo(boolean debugAtivo) {
        this.debug = debugAtivo;
    }
    /** * Caller: aplicarRespostaIA * Callee: CodeWorkspaceValidationService.validarEstadoAtual * Objetivo: Validar o estado real do workspace apos uma mutacao no codigo. * Feature: Impede que codigo quebrado seja tratado como sucesso. * Data modificacao: 2026-05-24 00:00 * * @return resultado estruturado da validacao * * @author Renato Tomaz Nati * @since 2026-05-24 */
    private WorkspaceCompilationValidationResult validarWorkspaceAposMutacao() {
        if (codeWorkspaceValidationService == null) {
            return new WorkspaceCompilationValidationResult();
        }

        return codeWorkspaceValidationService.validarEstadoAtual(compUnit);
    }
    
    /** * Caller: executarMissaoIA * Callee: N/A * Objetivo: Inicializar o rastreador de cobertura investigativa da missao atual. * Data modificacao: 2026-05-24 00:00 * * @return rastreador novo da missao * * @author Renato Tomaz Nati * @since 2026-05-24 */
    private InvestigationCoverageTracker criarRastreadorCobertura() {
        return new InvestigationCoverageTracker();
    }
    /** * Caller: executarMissaoIA * Callee: InvestigationCoveragePolicy.registrarUsoFerramenta * Objetivo: Alimentar a cobertura investigativa da missao com base na * ferramenta executada e no resultado observado. * Data modificacao: 2026-05-24 00:00 * * @param plan plano de cobertura da missao * @param nomeFerramenta nome da ferramenta executada * @param resultadoFerramenta resultado bruto da ferramenta * * @author Renato Tomaz Nati * @since 2026-05-24 */
    private void registrarCoberturaFerramentaExecutada(InvestigationCoveragePolicy.CoveragePlan plan, String nomeFerramenta, String resultadoFerramenta) {
        if (investigationCoveragePolicy == null || plan == null) {
            return;
        }

        investigationCoveragePolicy.registrarUsoFerramenta(plan, nomeFerramenta, resultadoFerramenta);
    }
    /** * Caller: executarMissaoIA * Callee: InvestigationCoveragePolicy.podeConcluir * Objetivo: Verificar se a cobertura investigativa minima da missao atual ja * foi atingida antes de permitir uma conclusao final. * Data modificacao: 2026-05-24 00:00 * * @param plan plano atual de cobertura * @return true quando a cobertura minima estiver satisfeita * * @author Renato Tomaz Nati * @since 2026-05-24 */
    private boolean podeConcluirComCoberturaAtual(InvestigationCoveragePolicy.CoveragePlan plan) {
        if (investigationCoveragePolicy == null) {
            return true;
        }

        return investigationCoveragePolicy.podeConcluir(plan);
    }
    /** * Caller: executarMissaoIA * Callee: InvestigationCoveragePolicy.buildPendenciasMensagem * Objetivo: Montar o reforco textual quando ainda faltar cobertura minima de * investigacao antes da conclusao. * Data modificacao: 2026-05-24 00:00 * * @param plan plano atual * @return mensagem de pendencias * * @author Renato Tomaz Nati * @since 2026-05-24 */
    private String montarInstrucaoCoberturaPendente(InvestigationCoveragePolicy.CoveragePlan plan) {
        if (investigationCoveragePolicy == null || plan == null) {
            return "";
        }

        return investigationCoveragePolicy.buildPendenciasMensagem(plan);
    }
    /** * Caller: aplicarRespostaIA * Callee: IDocument.replace * Objetivo: Restaurar o estado anterior do trecho quando a validacao do * workspace detectar erro real apos a mutacao. * Data modificacao: 2026-05-24 00:00 * * @param offsetInicial offset inicial da alteracao * @param comprimentoNovo comprimento do conteudo aplicado * @param conteudoAnterior conteudo original do trecho * * @author Renato Tomaz Nati * @since 2026-05-24 */
    private void restaurarEstadoAnteriorAposFalha(int offsetInicial, int comprimentoNovo, String conteudoAnterior) {
        if (document == null || offsetInicial < 0) {
            return;
        }

        try {
            document.replace(offsetInicial, comprimentoNovo, conteudoAnterior != null ? conteudoAnterior : "");
        } catch (Exception e) {
            System.out.println("[WORKSPACE VALIDATION DEBUG] Falha ao restaurar estado anterior: " + e.getMessage());
            e.printStackTrace();
        }
    }
    /**
 * Localiza a raiz segura do projeto subindo a arvore ate encontrar .git ou .project.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-18
 */
    private File descobrirRaizProjetoSegura(File diretorioInicial) {
        if (diretorioInicial == null) {
            return null;
        }

        File atual = diretorioInicial;
        File melhorRaiz = null;

        while (atual != null && atual.exists()) {
            File marcadorGit = new File(atual, ".git");
            File marcadorProject = new File(atual, ".project");

            if (marcadorGit.exists() || (marcadorProject.exists() && marcadorProject.isFile())) {
                melhorRaiz = atual;
            }

            atual = atual.getParentFile();
        }

        if (melhorRaiz != null) {
            return melhorRaiz;
        }

        return diretorioInicial;
    }

    /**
 * Atualiza o contexto do editor e refresca a UX do alvo atual.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-18
 */
    /** * Atualiza o contexto corrente do controlador com base no estado atual do * editor e da selecao. * * <p>Este metodo sincroniza o estado interno do alvo operacional, reaplica * vinculos e highlights de blocos nomeados quando houver editor valido, * atualiza o resumo exibido na view e emite um eco inicial do alvo quando * houver contexto textual suficiente.</p> * * <p>Quando o alvo muda, o controlador limpa destaque anterior, invalida a * missao corrente e reseta a memoria recente de sessao para evitar mistura de * contexto entre alvos diferentes.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void setContext(String selectedCode, String fullFileText, String apiKey, IDocument document, ITextSelection selection, ICompilationUnit compUnit, ITextEditor textEditor) {

        boolean mudouAlvo = detectarMudancaDeAlvo(document, selection);

        if (mudouAlvo) {
            prepararTrocaDeAlvo();
        }

        this.selectedCode = selectedCode;
        this.fullFileText = fullFileText;
        this.apiKey = apiKey;
        this.document = document;
        this.selection = selection;
        this.compUnit = compUnit;
        this.textEditor = textEditor;

        sincronizarBlocosNomeadosComContextoAtual();

        if (mudouAlvo && possuiSelecaoTextualValida(selection)) {
            if (deveAplicarDestaqueOperacional()) {
                aplicarDestaque();
            }
        }

        atualizarResumoAlvoNaView();
        emitirEcoInicialDoAlvoSeNecessario();
    }
    /** * Verifica se houve mudanca relevante de alvo operacional entre o contexto * anterior e o novo contexto informado. * * <p>A comparacao considera documento, offset e comprimento da selecao. Quando * nao houver selecao nova valida, o metodo considera que nao ha base segura * para comparacao de range textual.</p> * * @param novoDocumento documento atual informado * @param novaSelecao selecao atual informada * @return true quando houver mudanca relevante de alvo * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean detectarMudancaDeAlvo(IDocument novoDocumento, ITextSelection novaSelecao) {
        if (this.document != novoDocumento) {
            return true;
        }

        if (this.selection == null && novaSelecao != null) {
            return true;
        }

        if (this.selection != null && novaSelecao == null) {
            return true;
        }

        if (this.selection == null || novaSelecao == null) {
            return false;
        }

        if (this.selection.getOffset() != novaSelecao.getOffset()) {
            return true;
        }

        if (this.selection.getLength() != novaSelecao.getLength()) {
            return true;
        }

        return false;
    }
    /** * Prepara o controlador para uma troca de alvo operacional. * * <p>Este metodo remove destaque visual anterior, invalida a missao em curso, * avanca o token interno de controle e limpa a memoria recente da sessao para * evitar contaminacao entre alvos diferentes.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void prepararTrocaDeAlvo() {
        removerDestaque();
        this.primeiroEco = true;
        this.missaoCancelada = true;
        this.tokenMissaoAtual++;
        sessionHistoryService.limpar();
    }
    /** * Retorna true quando a selecao textual informada existe e possui comprimento * maior que zero. * * @param selecao selecao textual a validar * @return true quando a selecao puder ser tratada como alvo textual * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean possuiSelecaoTextualValida(ITextSelection selecao) {
        return selecao != null && selecao.getLength() > 0;
    }
    /** * Sincroniza vinculos e highlights de blocos nomeados com o contexto atual do * editor, quando houver dados suficientes. * * <p>Falhas isoladas nessa etapa nao devem interromper o fluxo principal de * atualizacao do contexto.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void sincronizarBlocosNomeadosComContextoAtual() {
        try {
            if (this.document == null || this.compUnit == null || this.textEditor == null) {
                return;
            }

            if (this.compUnit.getResource() == null || this.compUnit.getResource().getLocation() == null) {
                return;
            }

            String currentFilePath = this.compUnit.getResource()
                    .getLocation()
                    .toFile()
                    .getAbsolutePath()
                    .replace("\\", "/");

            namedBlockDocumentBindingService.bindBlocksToDocument(
                    this.document,
                    namedBlockSessionService.getAll(),
                    currentFilePath
            );

            namedBlockDocumentBindingService.syncBlocksFromDocument(
                    this.document,
                    namedBlockSessionService.getAll(),
                    currentFilePath
            );

            namedBlockHighlighter.refreshHighlights(
                    this.textEditor,
                    namedBlockSessionService.getAll(),
                    currentFilePath
            );
        } catch (Exception e) {
            // Falha silenciosa segura para nao interromper a atualizacao do contexto.
        }
    }
    /** * Emite uma mensagem inicial de eco do alvo atual quando houver contexto * textual suficiente e a flag de primeiro eco ainda estiver ativa. * * <p>O objetivo e dar visibilidade imediata ao alvo travado sem repetir a * mensagem indefinidamente em toda sincronizacao subsequente do mesmo alvo.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void emitirEcoInicialDoAlvoSeNecessario() {
        if (!primeiroEco) {
            return;
        }

        if (document == null || selection == null || compUnit == null) {
            return;
        }

        try {
            int startLine = selection.getStartLine() + 1;
            int startLineOffset = document.getLineOffset(selection.getStartLine());
            int startCol = selection.getOffset() - startLineOffset + 1;

            int endLine = selection.getEndLine() + 1;
            int endLineOffset = document.getLineOffset(selection.getEndLine());
            int endCol = (selection.getOffset() + selection.getLength()) - endLineOffset + 1;

            String nomeClasse = compUnit.getElementName();
            NamedCodeBlock blocoPrincipalAtivo = resolverBlocoPrincipalAtivo();

            String eco = "Alvo escolhido: Classe [" + nomeClasse + "] "
                    + "| Linha " + startLine + " Col " + startCol
                    + " ate Linha " + endLine + " Col " + endCol;

            if (selectedCode != null && selectedCode.trim().length() > 0) {
                eco += System.lineSeparator()
                        + "Trecho em foco:"
                        + System.lineSeparator()
                        + selectedCode;
            }

            if (blocoPrincipalAtivo != null) {
                eco += System.lineSeparator()
                        + "Alvo principal da analise: "
                        + blocoPrincipalAtivo.getName()
                        + " ["
                        + blocoPrincipalAtivo.getFileName()
                        + ":"
                        + blocoPrincipalAtivo.getStartLine()
                        + "-"
                        + blocoPrincipalAtivo.getEndLine()
                        + "]";
            }

            view.adicionarMensagem("Sistema", eco);
            atualizarStatusNaView("Alvo obtido e pronto para analise");
            this.primeiroEco = false;
        } catch (Exception e) {
            view.adicionarMensagem("Sistema", "Alvo obtido. Falha anotada.");
            atualizarStatusNaView("Alvo escolhido com informacao parcial");
            this.primeiroEco = false;
        }
    }
    /** * Verifica se a sessao atual possui contexto estrutural suficiente para * permitir uma missao sem selecao textual ativa no editor. * * <p>Esse metodo atende cenarios em que a IA precisa criar arquivos ou * packages em alvos estruturais nomeados, mesmo quando nao existe um trecho * selecionado no editor.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean possuiContextoEstruturalUtilizavel() {
        if (namedStructuralContextSessionService == null) {
            return false;
        }

        java.util.List<NamedStructuralContext> contexts = namedStructuralContextSessionService.getAll();
        if (contexts == null || contexts.isEmpty()) {
            return false;
        }

        for (int i = 0; i < contexts.size(); i++) {
            NamedStructuralContext context = contexts.get(i);
            if (context != null && context.isUsable()) {
                return true;
            }
        }

        return false;
    }
    /** * Decide se o destaque operacional do alvo atual deve ser aplicado no editor. * * <p>Quando existe um PRIMARY ativo na sessao, o destaque visual principal deve * ficar a cargo do sistema de blocos nomeados, preservando apenas a pintura * semantica ja existente no editor e evitando sobreposicao com o destaque * operacional do chat.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean deveAplicarDestaqueOperacional() {
        NamedCodeBlock blocoPrincipalAtivo = resolverBlocoPrincipalAtivo();
        if (blocoPrincipalAtivo != null) {
            return false;
        }

        NamedStructuralContext contextoPrincipalAtivo = resolverContextoEstruturalPrincipalAtivo();
        if (contextoPrincipalAtivo != null) {
            return false;
        }

        return true;
    }
    /**
 * Escaneia o editor em foco e sincroniza o alvo atual.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-18
 */
    public void engajarAlvoAtual(boolean engajamentoManual) {
        try {
            org.eclipse.ui.IWorkbenchWindow window = org.eclipse.ui.PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            if (window == null || window.getActivePage() == null) {
                return;
            }

            IEditorPart activeEditor = window.getActivePage().getActiveEditor();

            if (activeEditor instanceof org.eclipse.ui.texteditor.ITextEditor) {
                ITextEditor editor = (org.eclipse.ui.texteditor.ITextEditor) activeEditor;
                ITextSelection sel = (org.eclipse.jface.text.ITextSelection) editor.getSelectionProvider().getSelection();
                IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());

                String textoSelecionado = sel.getText();
                String textoCompleto = doc.get();

                org.eclipse.jdt.core.ICompilationUnit unidadeCompilacao = null;
                try {
                    org.eclipse.jdt.core.IJavaElement javaElement = org.eclipse.jdt.ui.JavaUI.getEditorInputJavaElement(editor.getEditorInput());
                    if (javaElement instanceof org.eclipse.jdt.core.ICompilationUnit) {
                        unidadeCompilacao = (org.eclipse.jdt.core.ICompilationUnit) javaElement;
                    }
                } catch (Exception ignore) {
                }

                String suaApiKey = System.getenv("SAI_MCP_API_KEY");
                if (suaApiKey == null || suaApiKey.trim().length() == 0) {
                    suaApiKey = System.getProperty("SAI_MCP_API_KEY");
                }

                if (suaApiKey == null || suaApiKey.trim().length() == 0) {
                    if (engajamentoManual) {
                        view.adicionarMensagem("Sistema", "Erro operacional: Chave SAI_MCP_API_KEY nao configurada. Defina a chave como variavel de ambiente ou propriedade JVM (-D).");
                        atualizarStatusNaView("Chave de API ausente");
                    }
                    return;
                }

                setContext(textoSelecionado, textoCompleto, suaApiKey, doc, sel, unidadeCompilacao, editor);
                return;
            }

            if (engajamentoManual) {
                view.adicionarMensagem("Sistema", "Alvo invalido. Abra um arquivo de texto ou Java para que a IA possa rastrear.");
                atualizarStatusNaView("Nenhum alvo valido aberto");
            } else {
                view.adicionarMensagem("Sistema", "Sistemas inicializados. Aguardando novo alvo.");
                atualizarStatusNaView("Aguardando alvo");
            }
        } catch (Exception e) {
            if (engajamentoManual) {
                view.adicionarMensagem("Sistema", "Falha critica no radar de contexto: " + e.getMessage());
                atualizarStatusNaView("Falha no radar de contexto");
            }
        }
    }
    /** * Sincroniza o alvo operacional da conversa a partir do PRIMARY global atual. * * <p>O PRIMARY global pode ser um bloco textual PRIMARY ou um contexto * estrutural PRIMARY do tipo FILE. Quando o PRIMARY estrutural for de arquivo, * o arquivo e aberto no editor e passa a ser o alvo operacional da sessao.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void sincronizarAlvoPrimarioGlobal() {
        NamedCodeBlock blocoPrincipal = resolverBlocoPrincipalAtivo();
        if (blocoPrincipal != null) {
            sincronizarAlvoPorBlocoPrincipal(blocoPrincipal);
            return;
        }

        NamedStructuralContext contextoPrincipal = resolverContextoEstruturalPrincipalAtivo();
        if (contextoPrincipal != null) {
            sincronizarAlvoPorArquivoEstruturalPrincipal(contextoPrincipal);
            return;
        }

        limparAlvoOperacional();
        view.atualizarResumoAlvo("Nenhum alvo ativo");
        atualizarStatusNaView("Aguardando alvo");
    }

    /** * Sincroniza o alvo atual priorizando o PRIMARY global da sessao. * * <p>Se nao houver PRIMARY global valido, o controlador tenta usar a selecao * atual do editor como fallback operacional.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void sincronizarAlvoPrimarioGlobalOuSelecaoAtual() {
        NamedCodeBlock blocoPrincipal = resolverBlocoPrincipalAtivo();
        if (blocoPrincipal != null) {
            sincronizarAlvoPorBlocoPrincipal(blocoPrincipal);
            return;
        }

        NamedStructuralContext contextoPrincipal = resolverContextoEstruturalPrincipalAtivo();
        if (contextoPrincipal != null) {
            sincronizarAlvoPorArquivoEstruturalPrincipal(contextoPrincipal);
            return;
        }

        engajarAlvoAtual(true);
    }

    /** * Sincroniza o alvo operacional do chat a partir de um bloco textual PRIMARY. * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void sincronizarAlvoPorBlocoPrincipal(NamedCodeBlock blocoPrincipal) {
        if (blocoPrincipal == null || editorNavigationService == null) {
            return;
        }

        try {
            ITextEditor editor = editorNavigationService.openTextEditorForFilePath(blocoPrincipal.getFilePath());
            if (editor == null) {
                view.adicionarMensagem("Sistema", "Nao foi possivel abrir o arquivo do bloco principal.");
                atualizarStatusNaView("Falha ao abrir arquivo do bloco principal");
                return;
            }

            IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
            if (doc == null) {
                view.adicionarMensagem("Sistema", "Nao foi possivel resolver o documento do bloco principal.");
                atualizarStatusNaView("Documento do bloco principal indisponivel");
                return;
            }

            int offset = blocoPrincipal.getEffectiveOffset();
            int length = blocoPrincipal.getEffectiveLength();

            if (offset < 0 || length <= 0 || offset + length > doc.getLength()) {
                view.adicionarMensagem("Sistema", "Range do bloco principal ficou invalido no documento atual.");
                atualizarStatusNaView("Range invalido do bloco principal");
                return;
            }

            org.eclipse.jface.text.TextSelection selecao = new org.eclipse.jface.text.TextSelection(doc, offset, length);

            ICompilationUnit unidadeCompilacao = null;
            try {
                org.eclipse.jdt.core.IJavaElement javaElement = org.eclipse.jdt.ui.JavaUI.getEditorInputJavaElement(editor.getEditorInput());
                if (javaElement instanceof ICompilationUnit) {
                    unidadeCompilacao = (ICompilationUnit) javaElement;
                }
            } catch (Exception e) {
            }

            String textoSelecionado = doc.get(offset, length);
            String textoCompleto = doc.get();
            String suaApiKey = resolverApiKeyAtual();

            setContext(
                    textoSelecionado,
                    textoCompleto,
                    suaApiKey,
                    doc,
                    selecao,
                    unidadeCompilacao,
                    editor
            );

            editorNavigationService.focusRange(editor, offset, length);
            atualizarStatusNaView("Bloco principal sincronizado como alvo ativo");
        } catch (Exception e) {
            view.adicionarMensagem("Sistema", "Falha ao sincronizar bloco principal: " + e.getMessage());
            atualizarStatusNaView("Falha ao sincronizar alvo principal");
        }
    }
    /** * Sincroniza o alvo operacional do chat a partir de um arquivo estrutural * PRIMARY. * * <p>Nesse caso, o arquivo inteiro passa a ser o alvo principal da conversa, * mas sem transformar todo o documento em selecao visual ativa no editor.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void sincronizarAlvoPorArquivoEstruturalPrincipal(NamedStructuralContext contextoPrincipal) {
        if (contextoPrincipal == null || editorNavigationService == null) {
            return;
        }

        if (contextoPrincipal.getType() != NamedStructuralContextType.FILE) {
            return;
        }

        try {
            ITextEditor editor = editorNavigationService.openTextEditorForFilePath(contextoPrincipal.getFilePath());
            if (editor == null) {
                view.adicionarMensagem("Sistema", "Nao foi possivel abrir o arquivo estrutural principal.");
                atualizarStatusNaView("Falha ao abrir arquivo estrutural principal");
                return;
            }

            IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
            if (doc == null) {
                view.adicionarMensagem("Sistema", "Nao foi possivel resolver o documento do arquivo estrutural principal.");
                atualizarStatusNaView("Documento do arquivo principal indisponivel");
                return;
            }

            org.eclipse.jface.text.TextSelection selecao = new org.eclipse.jface.text.TextSelection(doc, 0, 0);

            ICompilationUnit unidadeCompilacao = null;
            try {
                org.eclipse.jdt.core.IJavaElement javaElement = org.eclipse.jdt.ui.JavaUI.getEditorInputJavaElement(editor.getEditorInput());
                if (javaElement instanceof ICompilationUnit) {
                    unidadeCompilacao = (ICompilationUnit) javaElement;
                }
            } catch (Exception e) {
            }

            String textoSelecionado = "";
            String textoCompleto = doc.get();
            String suaApiKey = resolverApiKeyAtual();

            setContext(
                    textoSelecionado,
                    textoCompleto,
                    suaApiKey,
                    doc,
                    selecao,
                    unidadeCompilacao,
                    editor
            );

            atualizarStatusNaView("Arquivo principal sincronizado como alvo ativo");
        } catch (Exception e) {
            view.adicionarMensagem("Sistema", "Falha ao sincronizar arquivo principal: " + e.getMessage());
            atualizarStatusNaView("Falha ao sincronizar arquivo principal");
        }
    }

    /** * Limpa o alvo operacional atual do controlador sem apagar os contextos * nomeados da sessao. * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void limparAlvoOperacional() {
        removerDestaque();
        this.selectedCode = null;
        this.fullFileText = null;
        this.apiKey = null;
        this.document = null;
        this.selection = null;
        this.compUnit = null;
        this.textEditor = null;
        this.primeiroEco = true;
    }

    /** * Resolve a API key atual de forma segura para sincronizacao de contexto. * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String resolverApiKeyAtual() {
        String suaApiKey = this.apiKey;
        if (suaApiKey == null || suaApiKey.trim().length() == 0) {
            suaApiKey = System.getenv("SAI_MCP_API_KEY");
        }
        if (suaApiKey == null || suaApiKey.trim().length() == 0) {
            suaApiKey = System.getProperty("SAI_MCP_API_KEY");
        }
        return suaApiKey;
    }

    /** * Resolve o contexto estrutural PRIMARY atual, aceitando apenas arquivos * estruturais como alvo principal operacional. * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private NamedStructuralContext resolverContextoEstruturalPrincipalAtivo() {
        if (namedStructuralContextSessionService == null) {
            return null;
        }

        NamedStructuralContext context = namedStructuralContextSessionService.findPrimary();
        if (context == null) {
            return null;
        }

        if (context.getRole() != NamedContextTargetRole.PRIMARY) {
            return null;
        }

        if (context.getType() != NamedStructuralContextType.FILE) {
            return null;
        }

        return context;
    }
    /**
 * Aplica destaque persistente no trecho sob analise.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-18
 */
    private void aplicarDestaque() {
        if (textEditor == null || selection == null || selection.getLength() == 0) {
            return;
        }

        try {
            org.eclipse.ui.texteditor.IDocumentProvider provider = textEditor.getDocumentProvider();
            if (provider != null) {
                org.eclipse.jface.text.source.IAnnotationModel annotationModel = provider.getAnnotationModel(textEditor.getEditorInput());
                if (annotationModel != null) {
                	activeAnnotation = new org.eclipse.jface.text.source.Annotation("com.mcp.sailibrary.chat.target", false, "Alvo da IA");
                    org.eclipse.jface.text.Position position = new org.eclipse.jface.text.Position(selection.getOffset(), selection.getLength());
                    annotationModel.addAnnotation(activeAnnotation, position);
                    textEditor.selectAndReveal(selection.getOffset(), selection.getLength());
                }
            }
        } catch (Exception e) {
        }
    }

    /**
 * Remove o destaque persistente do trecho analisado.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-18
 */
    private void removerDestaque() {
        if (activeAnnotation != null && textEditor != null) {
            try {
                org.eclipse.ui.texteditor.IDocumentProvider provider = textEditor.getDocumentProvider();
                if (provider != null) {
                    org.eclipse.jface.text.source.IAnnotationModel annotationModel = provider.getAnnotationModel(textEditor.getEditorInput());
                    if (annotationModel != null) {
                        annotationModel.removeAnnotation(activeAnnotation);
                    }
                }
            } catch (Exception e) {
            } finally {
                activeAnnotation = null;
            }
        }
    }

    /**
 * Retorna o foco para o arquivo e revela o alvo atual.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-18
 */
    public void voltarAoArquivo() {
        if (textEditor != null && selection != null) {
            view.ativarEditor(textEditor);
            textEditor.selectAndReveal(selection.getOffset(), selection.getLength());
            atualizarStatusNaView("Editor reativado no alvo atual");
            return;
        }

        view.adicionarMensagem("Sistema", "Nenhum arquivo ativo no radar para retornar.");
        atualizarStatusNaView("Sem editor alvo para retorno");
    }

    /** * Cancela a missao atual e limpa o estado local do controlador. * * @author Renato Tomaz Nati * @since 2026-05-18 */
    public void abandonarMissao() {
        this.missaoCancelada = true;
        this.tokenMissaoAtual++;

        removerDestaque();
        this.textEditor = null;
        this.document = null;
        this.selection = null;
        this.primeiroEco = true;
        this.projectMemoryStore = null;
        this.raizProjetoSegura = null;

        sessionHistoryService.limpar();
        view.atualizarResumoAlvo("Nenhum alvo ativo");
        atualizarStatusNaView("Operacao cancelada");
        view.adicionarMensagem("Sistema", "Operacao abandonada. Selecao desbloqueada e cache limpo.");
        registrarAtividadeOperacional("CANCELAMENTO", "Operacao cancelada pelo usuario.");
    }
    /**
 * Adiciona item ao historico da sessao com protecao simples de concorrencia.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-18
 */
 

    /**
 * Verifica se a missao atual ainda esta autorizada a continuar.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-18
 */
    private boolean missaoAindaAtiva(long tokenMissao) {
        return !missaoCancelada && tokenMissaoAtual == tokenMissao;
    }

    public void executarMissaoIA(final String instrucao, final int profundidadeMax, final String pedidoOriginal) {
        registrarAtividadeOperacional("MISSAO", "Preparando contexto da missao.");

        if (document == null || selection == null) {
            sincronizarAlvoPrimarioGlobal();
        }

        if ((document == null || selection == null) && !possuiContextoEstruturalUtilizavel()) {
            view.adicionarMensagem("Sistema", "Erro operacional: Nenhum documento, selecao ou contexto estrutural utilizavel foi encontrado. Defina um PRIMARY valido, selecione um trecho ou use um contexto estrutural nomeado adequado.");
            atualizarStatusNaView("Nenhum alvo operacional ativo");
            registrarAtividadeOperacional("ERRO", "Nenhum alvo operacional ou estrutural utilizavel foi encontrado.");
            return;
        }

        this.missaoCancelada = false;
        this.tokenMissaoAtual++;

        final long tokenMissao = this.tokenMissaoAtual;
        final String instrucaoSnapshot = instrucao;
        final String pedidoOriginalSnapshot = pedidoOriginal;
        final String selectedCodeSnapshot = this.selectedCode != null ? this.selectedCode : "";
        final String fullFileTextSnapshot = this.fullFileText != null ? this.fullFileText : "";
        final String apiKeySnapshot = this.apiKey;
        final IDocument documentSnapshot = this.document;
        final ITextSelection selectionSnapshot = this.selection;
        final ICompilationUnit compUnitSnapshot = this.compUnit;

        File raizTemp = null;
        if (compUnitSnapshot != null && compUnitSnapshot.getJavaProject() != null && compUnitSnapshot.getJavaProject().getProject() != null) {
            raizTemp = compUnitSnapshot.getJavaProject().getProject().getLocation().toFile();
        } else {
            raizTemp = org.eclipse.core.resources.ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
        }

        final File raizProjeto = descobrirRaizProjetoSegura(raizTemp);
        this.raizProjetoSegura = raizProjeto;

        final int offsetAtual = (selectionSnapshot != null) ? selectionSnapshot.getOffset() : 0;

        view.limparAtividadesAgente();
        registrarAtividadeOperacional("MISSAO", "Nova execucao iniciada pelo usuario.");
        view.alternarCarregamento(true);
        view.adicionarMensagem("Sistema", "Reconhecimento assincrono iniciado. Aguardando processamento da IA.");
        view.adicionarMensagem("Sistema", "Se quiser acompanhar a atividade detalhada do agente, abra a aba Atividade.");
        atualizarStatusNaView("Preparando contexto da missao");
        reconfigurarCoordenadorModelosAtual();

        final InvestigationCoveragePolicy.CoveragePlan coveragePlan =
                investigationCoveragePolicy != null
                        ? investigationCoveragePolicy.createPlan(
                                view != null ? view.getPerfilRaciocinioConfigurado() : ChatRuntimeSettings.PERFIL_PADRAO,
                                instrucaoSnapshot
                        )
                        : null;

        final MissionExecutionContext contextoMissao = criarMissionExecutionContext(
                instrucaoSnapshot,
                pedidoOriginalSnapshot,
                tokenMissao,
                coveragePlan,
                raizProjeto,
                offsetAtual
        );

        Thread missaoThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    registrarAtividadeOperacional("CONTEXTO", "Construindo contexto inicial.");
                    atualizarStatusNaView("Construindo contexto inicial");

                    if (!missaoAindaAtiva(tokenMissao)) {
                        return;
                    }

                    String instrucaoEnriquecida = construirInstrucaoFinal(
                            instrucaoSnapshot,
                            compUnitSnapshot,
                            selectionSnapshot,
                            profundidadeMax,
                            coveragePlan
                    );

                    contextoMissao.setInstrucaoEnriquecida(instrucaoEnriquecida);

                    AgentOrchestrator orquestrador = new AgentOrchestrator(
                            contextoMissao.getRaizProjeto(),
                            compUnitSnapshot,
                            contextoMissao.getOffsetAtual()
                    );

                    final ProjectMemoryStore projectMemoryStoreLocal = new ProjectMemoryStore(contextoMissao.getRaizProjeto());
                    ChatAiController.this.projectMemoryStore = projectMemoryStoreLocal;

                    projectMemoryStoreLocal.inicializarEstrutura();
                    projectMemoryStoreLocal.atualizarBranchContexto();
                    projectMemoryStoreLocal.registrarProjectMemoryBasica(contextoMissao.getRaizProjeto().getAbsolutePath());

                    String resumoMemoriaProjeto = projectMemoryStoreLocal.consultarResumoMemoria();
                    if (resumoMemoriaProjeto != null && resumoMemoriaProjeto.trim().length() > 0) {
                        contextoMissao.setInstrucaoEnriquecida(
                                contextoMissao.getInstrucaoEnriquecida()
                                        + "\n\n=== MEMORIA PERSISTENTE DO PROJETO ===\n"
                                        + resumoMemoriaProjeto
                                        + "\n======================================\n"
                        );
                    }

                    if (investigationCoveragePolicy != null && contextoMissao.getCoveragePlan() != null) {
                        investigationCoveragePolicy.registrarFonteDeProjetoViaMemoria(contextoMissao.getCoveragePlan(), resumoMemoriaProjeto);
                    }

                    AiResponse ultimaRespostaEstruturadaValida = null;
                    int tentativasFalhaProtocolo = 0;
                    String ultimaAssinaturaFerramenta = null;
                    int repeticoesMesmaFerramenta = 0;
                    while (contextoMissao.getIteracaoAtual() < contextoMissao.getIteracoesMaximas()
                            && !contextoMissao.isMissaoConcluida()) {

                        contextoMissao.avancarIteracao();

                        atualizarStatusNaView("Ciclo " + contextoMissao.getIteracaoAtual() + " de " + contextoMissao.getIteracoesMaximas() + " em andamento");
                        registrarAtividadeOperacional("CICLO", "Executando ciclo " + contextoMissao.getIteracaoAtual() + " de " + contextoMissao.getIteracoesMaximas() + ".");

                        if (!missaoAindaAtiva(tokenMissao)) {
                            return;
                        }

                        registrarAtividadeOperacional("PLANEJAMENTO", "Acionando coordenador de modelos.");
                        AiResponse respostaIA = agentModelCoordinator.executarMissao(
                                selectedCodeSnapshot,
                                fullFileTextSnapshot,
                                contextoMissao.getInstrucaoEnriquecida(),
                                apiKeySnapshot
                        );

                        if (respostaIA == null) {
                            view.adicionarMensagemAssincrona("Sistema", "Falha operacional: o coordenador de modelos retornou resposta nula.");
                            view.adicionarMensagemAssincrona("Sistema", "Percebi uma falha de infraestrutura ou parse da resposta do modelo. Se quiser, posso investigar a causa.");
                            atualizarStatusNaView("Falha no coordenador de modelos");
                            registrarAtividadeOperacional("ERRO", "O coordenador de modelos retornou resposta nula.");
                            return;
                        }

                        if (debug) {
                            StringBuilder mensagemDebug = new StringBuilder();

                            mensagemDebug.append("COORDENADOR DE MODELOS").append(System.lineSeparator());
                            mensagemDebug.append("selectedCodeLength=").append(selectedCodeSnapshot != null ? selectedCodeSnapshot.length() : 0).append(System.lineSeparator());
                            mensagemDebug.append("fullFileTextLength=").append(fullFileTextSnapshot != null ? fullFileTextSnapshot.length() : 0).append(System.lineSeparator());
                            mensagemDebug.append("instrucaoLength=").append(contextoMissao.getInstrucaoEnriquecida() != null ? contextoMissao.getInstrucaoEnriquecida().length() : 0).append(System.lineSeparator());
                            mensagemDebug.append("apiKeyConfigured=").append(apiKeySnapshot != null && apiKeySnapshot.trim().length() > 0 ? "true" : "false").append(System.lineSeparator());
                            mensagemDebug.append(System.lineSeparator());

                            mensagemDebug.append("RESPOSTA ESTRUTURADA").append(System.lineSeparator());
                            mensagemDebug.append("action=").append(respostaIA.getAction()).append(System.lineSeparator());
                            mensagemDebug.append("tool=").append(respostaIA.getTool()).append(System.lineSeparator());
                            mensagemDebug.append("question=").append(respostaIA.getQuestion()).append(System.lineSeparator());
                            mensagemDebug.append("explanation=").append(respostaIA.getExplanation()).append(System.lineSeparator());

                            if (respostaIA.getParameters() != null) {
                                try {
                                    mensagemDebug.append("parameters=").append(mcpResponseService.serializarParametrosFerramenta(respostaIA.getParameters())).append(System.lineSeparator());
                                } catch (Exception e) {
                                    mensagemDebug.append("parameters=[falha ao serializar]").append(System.lineSeparator());
                                }
                            } else {
                                mensagemDebug.append("parameters=null").append(System.lineSeparator());
                            }

                            if (respostaIA.getContent() != null) {
                                String conteudoDebug = respostaIA.getContent();
                                if (conteudoDebug.length() > 1200) {
                                    conteudoDebug = conteudoDebug.substring(0, 1200) + "\n[DEBUG]: Conteudo truncado para preservar legibilidade.";
                                }
                                mensagemDebug.append("content=").append(conteudoDebug).append(System.lineSeparator());
                            } else {
                                mensagemDebug.append("content=null").append(System.lineSeparator());
                            }

                            view.adicionarMensagemAssincrona("DEBUG", mensagemDebug.toString());
                        }

                        if (!mcpResponseService.respostaEstruturadaValida(respostaIA)) {
                            tentativasFalhaProtocolo++;

                            String detalheFalhaProtocolo = montarDetalheFalhaProtocolo(respostaIA);
                            view.adicionarMensagemAssincrona("IA", detalheFalhaProtocolo);
                            atualizarStatusNaView("Falha de protocolo da IA");
                            registrarAtividadeOperacional("PROTOCOLO", detalheFalhaProtocolo);

                            if (tentativasFalhaProtocolo <= 2) {
                                contextoMissao.setInstrucaoEnriquecida(
                                        anexarInstrucaoRecuperacaoProtocolo(
                                                contextoMissao.getInstrucaoEnriquecida(),
                                                tentativasFalhaProtocolo
                                        )
                                );

                                view.adicionarMensagemAssincrona(
                                        "Sistema",
                                        "Falha de protocolo detectada. Tentando recuperar a resposta com protocolo estrito. Tentativa "
                                                + tentativasFalhaProtocolo + " de 2."
                                );
                                registrarAtividadeOperacional(
                                        "PROTOCOLO",
                                        "Solicitando nova resposta com protocolo estrito. Tentativa "
                                                + tentativasFalhaProtocolo + " de 2."
                                );
                                continue;
                            }

                            view.adicionarMensagemAssincrona(
                                    "Sistema",
                                    "Falha de protocolo persistente. Encerrando para evitar ciclo improdutivo."
                            );
                            registrarAtividadeOperacional(
                                    "ERRO",
                                    "Falha de protocolo persistente apos tentativas de recuperacao."
                            );
                            return;
                        }

                        tentativasFalhaProtocolo = 0;
                        if (isRespostaDeFalhaTecnicaModelo(respostaIA)) {
                            contextoMissao.setMissaoConcluida(true);
                            sessionHistoryService.adicionar("[IA - FALHA_INFRA]: " + respostaIA.getExplanation());
                            view.adicionarMensagemAssincrona("IA", mcpResponseService.formatarRespostaIA(respostaIA, documentSnapshot));
                            atualizarStatusNaView("Falha tecnica do modelo");
                            registrarAtividadeOperacional("ERRO", "Falha tecnica de infraestrutura detectada no provider/modelo. Encerrando ciclo sem forcar nova investigacao.");
                            return;
                        }
                        ultimaRespostaEstruturadaValida = respostaIA;

                        boolean houveExtensao = missionCycleService.processarSinalizacaoDeProximidade(contextoMissao, respostaIA);
                        if (houveExtensao) {
                            view.adicionarMensagemAssincrona("Sistema", "Relatorio de Status: A IA reportou estar muito proxima da solucao. Extensao de perimetro concedida.");
                            atualizarStatusNaView("Extensao de ciclos concedida");
                            registrarAtividadeOperacional("SISTEMA", "Extensao de ciclos concedida pela proximidade da solucao.");
                        } else if (respostaIA.getExplanation() != null) {
                            String explanation = respostaIA.getExplanation().toUpperCase();
                            if (explanation.contains("[LONGE_DA_SOLUCAO]") && !explanation.contains("[PERTO_DA_SOLUCAO]")) {
                                view.adicionarMensagemAssincrona("Sistema", "Relatorio de Status: A IA relatou baixa visibilidade sobre o alvo. Limite estrito mantido.");
                                atualizarStatusNaView("IA reportou baixa visibilidade");
                                registrarAtividadeOperacional("SISTEMA", "IA reportou baixa visibilidade sobre o alvo.");
                            }
                        }

                        if ("executar_ferramenta".equalsIgnoreCase(respostaIA.getAction())) {
                            String nomeFerramenta = respostaIA.getTool();
                            String assinaturaFerramentaAtual = construirAssinaturaFerramenta(respostaIA);

                            if (assinaturaFerramentaAtual.equals(ultimaAssinaturaFerramenta)) {
                                repeticoesMesmaFerramenta++;
                            } else {
                                ultimaAssinaturaFerramenta = assinaturaFerramentaAtual;
                                repeticoesMesmaFerramenta = 1;
                            }

                            if (repeticoesMesmaFerramenta > 2) {
                                String mensagemLoop = "Mesma ferramenta repetida em sequencia: " + assinaturaFerramentaAtual;
                                view.adicionarMensagemAssincrona("Sistema", mensagemLoop);
                                registrarAtividadeOperacional("RESILIENCIA", mensagemLoop);

                                contextoMissao.setInstrucaoEnriquecida(
                                        contextoMissao.getInstrucaoEnriquecida()
                                                + "\n\n=== ALERTA DE RESILIENCIA ===\n"
                                                + "A mesma ferramenta foi repetida em sequencia.\n"
                                                + "E proibido repetir a ferramenta "
                                                + nomeFerramenta
                                                + " novamente com os mesmos parametros.\n"
                                                + "Mude a trilha de investigacao ou conclua com o que ja foi confirmado.\n"
                                );

                                if (repeticoesMesmaFerramenta > 3) {
                                    view.adicionarMensagemAssincrona(
                                            "Sistema",
                                            "Loop de ferramenta detectado. Encerrando para preservar custo e estabilidade."
                                    );
                                    atualizarStatusNaView("Loop de ferramenta detectado");
                                    registrarAtividadeOperacional("ERRO", "Loop de ferramenta detectado.");
                                    return;
                                }
                            }
                            String parametrosFerramenta = mcpResponseService.serializarParametrosFerramenta(respostaIA.getParameters());

                            atualizarStatusNaView("Executando " + nomeFerramenta);
                            registrarAtividadeOperacional("TOOL", "Executando ferramenta " + nomeFerramenta + ".");

                            view.adicionarMensagemAssincrona(
                                    "Sistema",
                                    montarMensagemFerramentaParaUsuario(
                                            nomeFerramenta,
                                            respostaIA.getExplanation()
                                    )
                            );

                            ToolStepResult resultadoTool = missionToolStepService.executarFerramenta(
                                    nomeFerramenta,
                                    parametrosFerramenta,
                                    orquestrador,
                                    toolResultPresenter,
                                    projectMemoryStoreLocal,
                                    sessionHistoryService,
                                    investigationCoveragePolicy,
                                    contextoMissao.getCoveragePlan(),
                                    contextoMissao
                            );

                            if (!resultadoTool.isSucesso()) {
                                view.adicionarMensagemAssincrona("Sistema", resultadoTool.getMensagemErro());
                                atualizarStatusNaView("Erro de ferramenta");
                                registrarAtividadeOperacional("ERRO", resultadoTool.getMensagemErro());
                                return;
                            }

                            if (investigationCoveragePolicy != null && contextoMissao.getCoveragePlan() != null) {
                                registrarAtividadeOperacional("COBERTURA", investigationCoveragePolicy.buildPendenciasMensagem(contextoMissao.getCoveragePlan()));
                            }

                            if ("ler_conteudo_arquivo".equals(nomeFerramenta)
                                    || "buscar_texto_projeto".equals(nomeFerramenta)
                                    || "explorar_diretorio".equals(nomeFerramenta)) {
                                view.adicionarMensagemAssincrona("Ferramenta", nomeFerramenta + System.lineSeparator() + "Parametros: " + parametrosFerramenta);
                            } else {
                            	view.adicionarMensagemAssincrona(
                            	        "Ferramenta",
                            	        montarResultadoFerramentaParaUsuario(nomeFerramenta, resultadoTool.getResultadoParaChat())
                            	);
                            }

                            String resultadoFerramentaParaPrompt = resumirResultadoFerramentaParaPrompt(
                                    nomeFerramenta,
                                    compactarResultadoFerramentaParaPrompt(nomeFerramenta, resultadoTool.getResultadoBruto()),
                                    resultadoTool.getResultadoParaChat()
                            );

                            String novaInstrucao = contextoMissao.getInstrucaoEnriquecida()
                                    + "\n\n=== RESULTADO DA FERRAMENTA [" + nomeFerramenta + "] ===\n"
                                    + "PARAMETROS: " + parametrosFerramenta + "\n"
                                    + resultadoFerramentaParaPrompt
                                    + "\n=========================================\n"
                                    + "Regras obrigatorias apos usar ferramenta:\n"
                                    + "1. Reutilize a raiz segura e os caminhos relativos descobertos.\n"
                                    + "2. Nao repita a mesma ferramenta com os mesmos parametros se o resultado ja respondeu a duvida.\n"
                                    + "3. Se o resultado listar arquivos, classes, pacotes, DAOs ou metodos, use essas pistas no proximo passo.\n"
                                    + "4. Se um arquivo foi lido, aproveite package, classe e metodos encontrados antes de pedir nova busca textual.\n"
                                    + "5. Se o metodo atual delega para um DAO, priorize localizar o DAO e seus metodos em vez de reler a mesma classe.\n"
                                    + "6. Responda com um JSON valido contendo action, content e explanation.";

                            contextoMissao.setInstrucaoEnriquecida(novaInstrucao);

                            if (missionCycleService.deveEmitirAlertaProximidade(contextoMissao)) {
                                contextoMissao.setInstrucaoEnriquecida(
                                        missionCycleService.anexarAlertaProximidade(contextoMissao.getInstrucaoEnriquecida())
                                );
                                contextoMissao.setAlertaProximidadeEnviado(true);
                            }

                            continue;
                        }

                        if (missionCycleService.deveForcarNovaInvestigacao(
                                respostaIA,
                                investigationCoveragePolicy,
                                contextoMissao.getCoveragePlan())) {

                            contextoMissao.setInstrucaoEnriquecida(
                                    missionCycleService.construirInstrucaoCoberturaPendente(
                                            contextoMissao.getInstrucaoEnriquecida(),
                                            investigationCoveragePolicy,
                                            contextoMissao.getCoveragePlan()
                                    )
                            );

                            registrarAtividadeOperacional("COBERTURA", "Cobertura minima ainda nao satisfeita. Forcando nova investigacao.");
                            continue;
                        }

                        if (contextoMissao.getIteracaoAtual() == 1) {
                            sessionHistoryService.adicionar("[Usuario]: " + instrucaoSnapshot);
                        }

                        if ("perguntar_ao_usuario".equalsIgnoreCase(respostaIA.getAction())) {
                            contextoMissao.setMissaoConcluida(true);
                            sessionHistoryService.adicionar("[IA - Pergunta]: " + respostaIA.getQuestion());
                            view.adicionarMensagemAssincrona("IA", mcpResponseService.montarPerguntaAoUsuario(respostaIA));

                            if (isPerguntaDeFalhaTecnicaAuditoria(respostaIA)) {
                                atualizarStatusNaView("Confirmacao do usuario requerida por falha tecnica da auditoria");
                                registrarAtividadeOperacional("AUDITORIA", "Falha tecnica da auditoria. Aguardando confirmacao do usuario para seguir sem auditoria final.");
                            } else {
                                atualizarStatusNaView("IA aguardando resposta do usuario");
                                registrarAtividadeOperacional("RESPOSTA", "IA aguardando resposta do usuario.");
                            }

                            return;
                        }

                        if ("responder_ao_usuario".equalsIgnoreCase(respostaIA.getAction()) || "explicar".equalsIgnoreCase(respostaIA.getAction())) {
                            contextoMissao.setMissaoConcluida(true);
                            sessionHistoryService.adicionar("[IA - " + respostaIA.getAction() + "]: " + respostaIA.getExplanation());
                            view.adicionarMensagemAssincrona("IA", mcpResponseService.formatarRespostaIA(respostaIA, documentSnapshot));
                            atualizarStatusNaView("Resposta final entregue");
                            registrarAtividadeOperacional("RESPOSTA", "Resposta final preparada para o usuario.");
                            return;
                        }

                        contextoMissao.setMissaoConcluida(true);

                        if (!pareceAcaoDeEdicao(respostaIA.getAction())) {
                            sessionHistoryService.adicionar("[IA - " + respostaIA.getAction() + "]: " + respostaIA.getExplanation());
                            view.adicionarMensagemAssincrona("IA", mcpResponseService.formatarRespostaIA(respostaIA, documentSnapshot));
                            atualizarStatusNaView("Resposta nao destrutiva entregue");
                            registrarAtividadeOperacional("RESPOSTA", "Resposta nao destrutiva entregue.");
                            return;
                        }

                        sessionHistoryService.adicionar("[IA - " + respostaIA.getAction() + "]: " + respostaIA.getExplanation());

                        if ("substituir".equalsIgnoreCase(respostaIA.getAction())) {
                            if (!conteudoCompativelComSelecao(respostaIA.getContent(), selectedCodeSnapshot)) {
                                view.adicionarMensagemAssincrona("Sistema", "Alerta: Conteudo incompativel com a selecao. Substituicao abortada por seguranca.");
                                view.adicionarMensagemAssincrona("IA", mcpResponseService.formatarRespostaIA(respostaIA, documentSnapshot));
                                atualizarStatusNaView("Substituicao bloqueada por seguranca");
                                registrarAtividadeOperacional("ERRO", "Conteudo de substituicao rejeitado por seguranca.");
                                return;
                            }

                            String conteudoNormalizado = normalizarFormatacao(respostaIA.getContent(), documentSnapshot, selectionSnapshot);

                            final AiResponse respostaFinal = new AiResponse();
                            respostaFinal.setAction(respostaIA.getAction());
                            respostaFinal.setContent(conteudoNormalizado);
                            respostaFinal.setExplanation(respostaIA.getExplanation());

                            Display.getDefault().asyncExec(new Runnable() {
                                public void run() {
                                    try {
                                        if (!missaoAindaAtiva(tokenMissao)) {
                                            return;
                                        }
                                        registrarAtividadeOperacional("CODIGO", "Aplicando alteracao no codigo.");
                                        aplicarRespostaIA(respostaFinal, pedidoOriginalSnapshot);
                                    } catch (Exception e) {
                                        view.adicionarMensagem("Erro Tatico", "Falha critica ao tentar aplicar codigo substituido: " + e.getMessage());
                                    }
                                }
                            });
                            atualizarStatusNaView("Aplicando alteracao no codigo");
                            return;
                        }

                        final AiResponse respIAFinal = respostaIA;
                        Display.getDefault().asyncExec(new Runnable() {
                            public void run() {
                                try {
                                    if (!missaoAindaAtiva(tokenMissao)) {
                                        return;
                                    }
                                    registrarAtividadeOperacional("CODIGO", "Aplicando alteracao no documento.");
                                    aplicarRespostaIA(respIAFinal, pedidoOriginalSnapshot);
                                } catch (Exception e) {
                                    view.adicionarMensagem("Erro Tatico", "Falha critica ao tentar aplicar edicao no documento: " + e.getMessage());
                                }
                            }
                        });
                        atualizarStatusNaView("Aplicando resposta final");
                    }

                    if (!contextoMissao.isMissaoConcluida()) {
                        String respostaDeContingencia = tentarConclusaoDeContingencia(
                                contextoMissao.getInstrucaoEnriquecida(),
                                ultimaRespostaEstruturadaValida,
                                contextoMissao.getUltimoNomeFerramenta(),
                                contextoMissao.getUltimoResumoFerramenta(),
                                contextoMissao.getUltimoResultadoFerramentaBruto(),
                                selectedCodeSnapshot,
                                fullFileTextSnapshot,
                                apiKeySnapshot,
                                documentSnapshot);

                        if (respostaDeContingencia != null && respostaDeContingencia.trim().length() > 0) {
                            view.adicionarMensagemAssincrona("IA", respostaDeContingencia);
                            atualizarStatusNaView("Resposta parcial de contingencia entregue");
                            registrarAtividadeOperacional("RESPOSTA", "Resposta parcial de contingencia entregue.");
                        } else {
                            view.adicionarMensagemAssincrona("Sistema", "Limite de ciclos atingido. Foi entregue apenas o que ja pode ser confirmado com seguranca.");
                            atualizarStatusNaView("Limite de ciclos atingido");
                            registrarAtividadeOperacional("LIMITE", "Limite de ciclos atingido.");
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();

                    String tipoErro = ex.getClass() != null ? ex.getClass().getName() : "ErroDesconhecido";
                    String mensagemErro = ex.getMessage() != null ? ex.getMessage() : "sem mensagem";

                    view.adicionarMensagemAssincrona(
                            "Sistema",
                            "Falha durante execucao da missao: " + tipoErro + " - " + mensagemErro
                    );
                    atualizarStatusNaView("Falha de comunicacao");
                    registrarAtividadeOperacional("ERRO", "Falha durante execucao da missao: " + tipoErro + " - " + mensagemErro);
                } finally {
                    view.alternarCarregamento(false);
                }
            }
        });

        missaoThread.setName("Operacao-Autonoma-IA");
        missaoThread.start();
    }
    private String compactarResultadoFerramentaParaPrompt(String nomeFerramenta, String resultadoBruto) {
        if (resultadoBruto == null) {
            return "";
        }

        int limite = 6000;

        if ("resumir_impacto_alteracao".equals(nomeFerramenta)) {
            limite = 5000;
        } else if ("leitura_cirurgica_jdt".equals(nomeFerramenta)) {
            limite = 7000;
        } else if ("buscar_texto_projeto".equals(nomeFerramenta)) {
            limite = 3500;
        } else if ("explorar_diretorio".equals(nomeFerramenta)) {
            limite = 3000;
        } else if ("ler_conteudo_arquivo".equals(nomeFerramenta)) {
            limite = 8000;
        }

        if (resultadoBruto.length() <= limite) {
            return resultadoBruto;
        }

        return resultadoBruto.substring(0, limite)
                + System.lineSeparator()
                + "[RESUMO INTERNO]: Resultado truncado para preservar contexto. Use nova ferramenta se precisar de mais detalhes.";
    }
    private String montarResultadoFerramentaParaUsuario(String nomeFerramenta, String resultadoParaChat) {
        StringBuilder mensagem = new StringBuilder();

        mensagem.append("Resultado da verificacao: ");
        mensagem.append(nomeFerramentaParaUsuario(nomeFerramenta));
        mensagem.append(System.lineSeparator());

        String conclusao = extrairConclusaoFerramentaParaUsuario(nomeFerramenta, resultadoParaChat);
        mensagem.append(conclusao);

        return mensagem.toString();
    }

    private String extrairConclusaoFerramentaParaUsuario(String nomeFerramenta, String resultado) {
        if (resultado == null || resultado.trim().length() == 0) {
            return "Nao foi possivel obter uma conclusao util dessa verificacao.";
        }

        String texto = resultado.trim();

        String conclusao = extrairLinhaAposMarcador(texto, "Conclusao tatica final");
        if (!isBlank(conclusao)) {
            return conclusao;
        }

        conclusao = extrairLinhaAposMarcador(texto, "Conclusao tatica");
        if (!isBlank(conclusao)) {
            return conclusao;
        }

        if ("buscar_implementacoes_tipo".equals(nomeFerramenta)) {
            if (texto.contains("Implementacao localizada em:")) {
                return "Encontrei a implementacao real usada pelo sistema. Vou abrir esse ponto para confirmar o comportamento antes de alterar o codigo.";
            }
            return "Nao encontrei uma implementacao concreta clara para esse contrato.";
        }

        if ("leitura_cirurgica_jdt".equals(nomeFerramenta)) {
            if (texto.contains("EXTRACAO CIRURGICA DE METODO")) {
                return "Consegui ler o metodo especifico necessario para continuar a analise com menos suposicao.";
            }
            return "A leitura especifica nao trouxe um metodo claro.";
        }

        if ("buscar_callees_jdt".equals(nomeFerramenta)) {
            return resumirPrimeirasLinhas(texto, 5);
        }

        if ("buscar_chamadores_jdt".equals(nomeFerramenta)) {
            return resumirPrimeirasLinhas(texto, 5);
        }

        if ("inspecionar_efeitos_colaterais".equals(nomeFerramenta)) {
            return resumirPrimeirasLinhas(texto, 6);
        }

        return resumirPrimeirasLinhas(texto, 6);
    }
    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
    private String extrairLinhaAposMarcador(String texto, String marcador) {
        if (texto == null || marcador == null) {
            return "";
        }

        int indice = texto.indexOf(marcador);
        if (indice < 0) {
            return "";
        }

        String restante = texto.substring(indice + marcador.length()).trim();
        String[] linhas = restante.split("\\r?\\n");

        for (int i = 0; i < linhas.length; i++) {
            String linha = linhas[i] != null ? linhas[i].trim() : "";
            if (linha.length() > 0) {
                return linha;
            }
        }

        return "";
    }

    private String resumirPrimeirasLinhas(String texto, int maxLinhas) {
        if (texto == null) {
            return "";
        }

        String[] linhas = texto.split("\\r?\\n");
        StringBuilder builder = new StringBuilder();

        int adicionadas = 0;
        for (int i = 0; i < linhas.length && adicionadas < maxLinhas; i++) {
            String linha = linhas[i] != null ? linhas[i].trim() : "";
            if (linha.length() == 0) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(System.lineSeparator());
            }

            builder.append(linha);
            adicionadas++;
        }

        if (linhas.length > maxLinhas) {
            builder.append(System.lineSeparator()).append("A verificacao completa foi usada internamente para continuar a analise.");
        }

        return builder.toString();
    }
    private String montarMensagemFerramentaParaUsuario(String nomeFerramenta, String explicacao) {
        String nomeAmigavel = nomeFerramentaParaUsuario(nomeFerramenta);

        StringBuilder mensagem = new StringBuilder();
        mensagem.append("Estou verificando: ").append(nomeAmigavel).append(".");

        if (explicacao != null && explicacao.trim().length() > 0) {
            mensagem.append(System.lineSeparator());
            mensagem.append("Motivo: ").append(simplificarExplicacaoParaUsuario(explicacao));
        }

        return mensagem.toString();
    }

    private String nomeFerramentaParaUsuario(String nomeFerramenta) {
        if (nomeFerramenta == null) {
            return "informacoes do projeto";
        }

        if ("resumir_impacto_alteracao".equals(nomeFerramenta)) {
            return "impacto da alteracao";
        }

        if ("buscar_implementacoes_tipo".equals(nomeFerramenta)) {
            return "implementacao real usada pelo sistema";
        }

        if ("leitura_cirurgica_jdt".equals(nomeFerramenta)) {
            return "trecho especifico do codigo";
        }

        if ("buscar_chamadores_jdt".equals(nomeFerramenta)) {
            return "quem chama este metodo";
        }

        if ("buscar_callees_jdt".equals(nomeFerramenta)) {
            return "o que este metodo chama";
        }

        if ("inspecionar_efeitos_colaterais".equals(nomeFerramenta)) {
            return "efeitos colaterais e mutacoes";
        }

        if ("extrair_queries_trecho".equals(nomeFerramenta)) {
            return "consultas e acesso a dados";
        }

        if ("inspecionar_override_metodo".equals(nomeFerramenta)) {
            return "heranca e sobrescritas";
        }

        if ("inspecionar_dependencias_projeto".equals(nomeFerramenta)) {
            return "dependencias do projeto";
        }

        return nomeFerramenta.replace("_", " ");
    }

    private String simplificarExplicacaoParaUsuario(String explicacao) {
        if (explicacao == null) {
            return "";
        }

        String texto = explicacao.trim();

        texto = texto.replace("@pesquisa", "o alvo selecionado");
        texto = texto.replace("conclusao segura", "responder com seguranca");
        texto = texto.replace("trilha concreta", "fluxo real do codigo");
        texto = texto.replace("endurecer", "tornar mais seguro");

        if (texto.length() > 220) {
            texto = texto.substring(0, 220) + "...";
        }

        return texto;
    }
    private String montarDetalheFalhaProtocolo(AiResponse respostaIA) {
        StringBuilder detalhe = new StringBuilder();
        detalhe.append("Falha de protocolo da IA.");
        detalhe.append(System.lineSeparator());
        detalhe.append("action=").append(respostaIA != null ? respostaIA.getAction() : null);
        detalhe.append(System.lineSeparator());
        detalhe.append("tool=").append(respostaIA != null ? respostaIA.getTool() : null);
        detalhe.append(System.lineSeparator());
        detalhe.append("question=").append(respostaIA != null ? respostaIA.getQuestion() : null);
        detalhe.append(System.lineSeparator());
        detalhe.append("explanation=").append(respostaIA != null ? respostaIA.getExplanation() : null);
        return detalhe.toString();
    }

    private String anexarInstrucaoRecuperacaoProtocolo(String instrucaoAtual, int tentativa) {
        StringBuilder builder = new StringBuilder();
        builder.append(instrucaoAtual != null ? instrucaoAtual : "");
        builder.append("\n\n=== CORRECAO DE PROTOCOLO OBRIGATORIA ===\n");
        builder.append("Tentativa de recuperacao: ").append(tentativa).append("\n");
        builder.append("A resposta anterior violou o protocolo.\n");
        builder.append("Responda com exatamente um bloco <thinking>, um bloco <racional> e um bloco <codigo_final>.\n");
        builder.append("Nao repita blocos.\n");
        builder.append("Nao duplique JSON.\n");
        builder.append("Se for usar ferramenta, devolva apenas um unico JSON valido dentro de <codigo_final>.\n");
        builder.append("Se nao conseguir cumprir, use action responder_ao_usuario explicando a limitacao.\n");
        return builder.toString();
    }

    private String construirAssinaturaFerramenta(AiResponse respostaIA) {
        String nomeFerramenta = respostaIA != null && respostaIA.getTool() != null
                ? respostaIA.getTool().trim()
                : "";

        String parametros = "";
        try {
            parametros = respostaIA != null
                    ? mcpResponseService.serializarParametrosFerramenta(respostaIA.getParameters())
                    : "";
        } catch (Exception e) {
            parametros = "";
        }

        return nomeFerramenta + "|" + parametros;
    }

    private String resumirResultadoFerramentaParaPrompt(String nomeFerramenta, String resultadoBruto, String resultadoResumido) {
        String textoBase = resultadoResumido != null && resultadoResumido.trim().length() > 0
                ? resultadoResumido
                : resultadoBruto;

        if (textoBase == null) {
            return "";
        }

        String textoNormalizado = textoBase.trim();
        int limite = 6000;

        if (textoNormalizado.length() <= limite) {
            return textoNormalizado;
        }

        StringBuilder resumo = new StringBuilder();
        resumo.append(textoNormalizado.substring(0, limite));
        resumo.append("\n[RESUMO]: Conteudo truncado para preservar estabilidade do protocolo.");
        resumo.append("\n[FONTE]: ").append(nomeFerramenta != null ? nomeFerramenta : "");
        return resumo.toString();
    }
    private boolean isRespostaDeFalhaTecnicaModelo(AiResponse respostaIA) {
        if (respostaIA == null) {
            return false;
        }

        String action = respostaIA.getAction() != null ? respostaIA.getAction().toLowerCase() : "";
        String explanation = respostaIA.getExplanation() != null ? respostaIA.getExplanation().toLowerCase() : "";
        String content = respostaIA.getContent() != null ? respostaIA.getContent().toLowerCase() : "";

        if (!"responder_ao_usuario".equals(action) && !"perguntar_ao_usuario".equals(action)) {
            return false;
        }

        if (explanation.contains("falha tecnica de infraestrutura do provider/modelo")
                || explanation.contains("falha tecnica do provider/modelo")
                || explanation.contains("falha de infraestrutura do provider/modelo")
                || explanation.contains("detectada antes do parse estruturado")
                || explanation.contains("resposta malformada do modelo")
                || explanation.contains("falha ao interpretar a resposta estruturada do modelo")) {
            return true;
        }

        if (content.contains("falha tecnica do modelo durante a execucao")
                || content.contains("nao tratei isso como resposta valida da ia")
                || content.contains("posso tentar novamente ou investigar a causa")) {
            return true;
        }

        return false;
    }
    /** * Tenta consolidar uma resposta final parcial quando o limite de ciclos for atingido. * * @author Renato Tomaz Nati * @since 2026-05-18 */
    private String tentarConclusaoDeContingencia(String instrucaoEnriquecida, AiResponse ultimaRespostaEstruturadaValida, String ultimoNomeFerramenta, String ultimoResumoFerramenta, String ultimoResultadoFerramentaBruto, String selectedCodeSnapshot, String fullFileTextSnapshot, String apiKeySnapshot, IDocument documentSnapshot) {
        try {
            StringBuilder instrucaoFinalForcada = new StringBuilder();
            instrucaoFinalForcada.append(instrucaoEnriquecida);
            instrucaoFinalForcada.append("\n\n=== ORDEM FINAL DE CONTINGENCIA ===\n");
            instrucaoFinalForcada.append("O limite de ciclos de investigacao foi atingido.\n");
            instrucaoFinalForcada.append("A partir deste ponto, e proibido executar novas ferramentas.\n");
            instrucaoFinalForcada.append("Voce deve responder de forma util, positiva, parcial e estruturada ao usuario com o melhor que ja foi confirmado.\n");
            instrucaoFinalForcada.append("Explique claramente:\n");
            instrucaoFinalForcada.append("1. o que foi confirmado\n");
            instrucaoFinalForcada.append("2. o que parece provavel mas nao foi confirmado\n");
            instrucaoFinalForcada.append("3. o que nao pode ser analisado com seguranca\n");
            instrucaoFinalForcada.append("4. qual seria o proximo passo ideal se houvesse mais ciclos\n");
            instrucaoFinalForcada.append("Use action = responder_ao_usuario ou action = explicar.\n");
            instrucaoFinalForcada.append("Nao use executar_ferramenta.\n");

            if (ultimoNomeFerramenta != null && ultimoNomeFerramenta.trim().length() > 0) {
                instrucaoFinalForcada.append("\nUltima ferramenta executada: ").append(ultimoNomeFerramenta).append("\n");
            }

            if (ultimoResumoFerramenta != null && ultimoResumoFerramenta.trim().length() > 0) {
                instrucaoFinalForcada.append("\nResumo do ultimo resultado:\n").append(ultimoResumoFerramenta).append("\n");
            }

            String historicoAtual = sessionHistoryService.obter();
            if (historicoAtual != null && historicoAtual.trim().length() > 0) {
                instrucaoFinalForcada.append("\nHistorico da sessao:\n");
                instrucaoFinalForcada.append(historicoAtual).append("\n");
            }

            registrarAtividadeOperacional("CONTINGENCIA", "Solicitando resposta final de contingencia.");
            AiResponse respostaIA = agentModelCoordinator.executarMissao(
                    selectedCodeSnapshot,
                    fullFileTextSnapshot,
                    instrucaoFinalForcada.toString(),
                    apiKeySnapshot
            );

            if (respostaIA != null && mcpResponseService.respostaEstruturadaValida(respostaIA)) {
                if ("responder_ao_usuario".equalsIgnoreCase(respostaIA.getAction())
                        || "explicar".equalsIgnoreCase(respostaIA.getAction())) {
                    return mcpResponseService.formatarRespostaIA(respostaIA, documentSnapshot);
                }
            }
        } catch (Exception e) {
            registrarAtividadeOperacional("ERRO", "Falha ao montar resposta de contingencia: " + e.getMessage());
        }

        return montarRespostaLocalDeContingencia(
                ultimaRespostaEstruturadaValida,
                ultimoNomeFerramenta,
                ultimoResumoFerramenta,
                ultimoResultadoFerramentaBruto
        );
    }

    /**
 * Gera uma resposta local minima quando a IA nao conseguir concluir.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-18
 */
    private String montarRespostaLocalDeContingencia(AiResponse ultimaRespostaEstruturadaValida, String ultimoNomeFerramenta, String ultimoResumoFerramenta, String ultimoResultadoFerramentaBruto) {
        String delimitador = getLineDelimiter(document);
        StringBuilder resposta = new StringBuilder();

        resposta.append("action: responder_ao_usuario").append(delimitador);
        resposta.append("explanation: Limite de ciclos atingido. Resposta parcial montada com o que foi confirmado ate aqui.").append(delimitador);
        resposta.append(delimitador);

        resposta.append("Conclusao parcial").append(delimitador);
        resposta.append("- O processo de investigacao atingiu o limite de ciclos antes de fechar toda a trilha com seguranca.").append(delimitador);

        if (ultimoNomeFerramenta != null && ultimoNomeFerramenta.trim().length() > 0) {
            resposta.append("- Ultima ferramenta executada: ").append(ultimoNomeFerramenta).append(delimitador);
        }

        if (ultimoResumoFerramenta != null && ultimoResumoFerramenta.trim().length() > 0) {
            resposta.append("- Ultimo achado relevante: ").append(delimitador);
            resposta.append(ultimoResumoFerramenta).append(delimitador);
        }

        if (ultimaRespostaEstruturadaValida != null && ultimaRespostaEstruturadaValida.getExplanation() != null
                && ultimaRespostaEstruturadaValida.getExplanation().trim().length() > 0) {
            resposta.append("- Ultima linha de raciocinio valida da IA: ").append(ultimaRespostaEstruturadaValida.getExplanation()).append(delimitador);
        }

        resposta.append(delimitador);
        resposta.append("O que foi possivel analisar").append(delimitador);
        resposta.append("- Foram coletados indicios e resultados parciais suficientes para orientar uma resposta inicial.").append(delimitador);
        resposta.append("- O sistema reaproveitou o contexto atual, os resultados das ferramentas e o historico da sessao.").append(delimitador);

        resposta.append(delimitador);
        resposta.append("O que nao foi possivel confirmar completamente").append(delimitador);
        resposta.append("- Nem toda a cadeia indireta foi fechada com seguranca dentro do limite de ciclos.").append(delimitador);
        resposta.append("- Se houver query indireta em DAO, XML, framework ou classe externa ainda nao lida, ela pode nao ter sido totalmente confirmada.").append(delimitador);

        resposta.append(delimitador);
        resposta.append("Como interpretar esta resposta").append(delimitador);
        resposta.append("- Trate os pontos confirmados como seguros.").append(delimitador);
        resposta.append("- Trate pontos nao confirmados como parciais e dependentes de investigacao adicional.").append(delimitador);

        return resposta.toString();
    }

  
    /** * Aplica a resposta de edicao da IA no documento e usa a validacao real do * workspace como arbitro final. * * <p>Se houver erro de compilacao apos a aplicacao, a alteracao e revertida e * o sistema nao declara sucesso. Em vez disso, informa o erro e pergunta se o * usuario deseja que a causa seja investigada.</p> * * @author Renato Tomaz Nati * @since 2026-05-25 */
    private void aplicarRespostaIA(AiResponse respostaIA, String pedidoOriginal) throws Exception {
        if (respostaIA == null) {
            view.adicionarMensagem("Sistema", "Falha operacional: resposta da IA ausente para aplicacao.");
            atualizarStatusNaView("Resposta ausente para aplicacao");
            registrarAtividadeOperacional("ERRO", "Resposta da IA ausente para aplicacao.");
            return;
        }

        if (document == null || selection == null) {
            view.adicionarMensagem("Sistema", "Falha operacional: documento ou selecao indisponivel para aplicacao.");
            atualizarStatusNaView("Documento indisponivel");
            registrarAtividadeOperacional("ERRO", "Documento ou selecao indisponivel para aplicacao.");
            return;
        }

        String action = respostaIA.getAction();
        String conteudoNovo = respostaIA.getContent() != null ? respostaIA.getContent() : "";

        CodeApplicationState estado = codeApplicationService.capturarEstadoAntesDaAplicacao(document, selection);

        registrarAtividadeOperacional("CODIGO", "Aplicando acao [" + action + "] no documento.");
        codeApplicationService.logAplicacaoDebug(action, estado, conteudoNovo);

        CodeApplicationResult resultadoAplicacao =
                codeApplicationService.aplicarEdicaoNoDocumento(document, action, conteudoNovo, estado);

        if (!resultadoAplicacao.isAplicou()) {
            view.adicionarMensagem("Sistema", resultadoAplicacao.getMensagemUsuario());
            atualizarStatusNaView("Acao nao suportada");
            registrarAtividadeOperacional("ERRO", resultadoAplicacao.getMensagemTecnica());
            return;
        }

        codeApplicationService.sincronizarDocumentoComCompilationUnit(document, compUnit);
        sincronizarBlocosNomeadosNoEditorAtual();

        WorkspaceCompilationValidationResult resultadoValidacao =
                codeWorkspaceValidationService != null
                        ? codeWorkspaceValidationService.validarEstadoAtual(compUnit)
                        : null;

        if (!workspaceValidationSucceeded(resultadoValidacao)) {
        	CodeApplicationResult resultadoReversao =
        	        codeApplicationService.reverterAplicacaoAposFalha(document, compUnit, action, conteudoNovo, estado);
            sincronizarBlocosNomeadosNoEditorAtual();

            String mensagemValidacao = buildWorkspaceValidationMessage(resultadoValidacao);

            view.adicionarMensagem("Sistema",
                    "Percebi erros de compilacao no workspace apos a alteracao. A mudanca foi revertida automaticamente.");
            view.adicionarMensagem("Sistema", mensagemValidacao);
            view.adicionarMensagem("Sistema", "Quer que eu avalie a causa desses erros agora?");
            atualizarStatusNaView("Alteracao revertida por erro de compilacao");
            registrarAtividadeOperacional("VALIDACAO", "Compilacao com erro apos alteracao. Mudanca revertida automaticamente.");
            System.out.println("[CHAT APPLY DEBUG] Alteracao revertida por falha de compilacao.");
            System.out.println("[CHAT APPLY DEBUG] reversao.mensagem=" + resultadoReversao.getMensagemTecnica());
            return;
        }

        view.adicionarMensagem("IA", mcpResponseService.formatarRespostaIA(respostaIA, document));
        atualizarStatusNaView("Alteracao aplicada com sucesso");
        registrarAtividadeOperacional("VALIDACAO", "Workspace validado com sucesso apos aplicacao.");
        System.out.println("[CHAT APPLY DEBUG] Alteracao aplicada com sucesso e workspace validado.");
    }
    private MissionExecutionContext criarMissionExecutionContext( String instrucao, String pedidoOriginal, long tokenMissao, InvestigationCoveragePolicy.CoveragePlan coveragePlan, File raizProjeto, int offsetAtual) {

        MissionExecutionContext contexto = new MissionExecutionContext();
        contexto.setTokenMissao(tokenMissao);
        contexto.setInstrucaoSnapshot(instrucao);
        contexto.setPedidoOriginalSnapshot(pedidoOriginal);
        contexto.setSelectedCodeSnapshot(this.selectedCode != null ? this.selectedCode : "");
        contexto.setFullFileTextSnapshot(this.fullFileText != null ? this.fullFileText : "");
        contexto.setApiKeySnapshot(this.apiKey);
        contexto.setDocumentSnapshot(this.document);
        contexto.setSelectionSnapshot(this.selection);
        contexto.setCompUnitSnapshot(this.compUnit);
        contexto.setCoveragePlan(coveragePlan);
        contexto.setRaizProjeto(raizProjeto);
        contexto.setOffsetAtual(offsetAtual);
        return contexto;
    }
    
    private void sincronizarBlocosNomeadosNoEditorAtual() {
        try {
            if (document != null && textEditor != null && compUnit != null
                    && compUnit.getResource() != null
                    && compUnit.getResource().getLocation() != null) {

                String currentFilePath = compUnit.getResource().getLocation().toFile().getAbsolutePath().replace("\\", "/");

                namedBlockDocumentBindingService.bindBlocksToDocument(document, namedBlockSessionService.getAll(), currentFilePath);
                namedBlockDocumentBindingService.syncBlocksFromDocument(document, namedBlockSessionService.getAll(), currentFilePath);
                namedBlockHighlighter.refreshHighlights(textEditor, namedBlockSessionService.getAll(), currentFilePath);
            }
        } catch (Exception e) {
            // Falha silenciosa segura para nao interromper fluxo principal de edicao
        }
    }
    
    /** * Identifica se a pergunta ao usuario foi gerada por falha tecnica da auditoria, * e nao por uma duvida funcional normal do fluxo. * * @param respostaIA resposta estruturada atual * @return true quando a pergunta estiver relacionada a indisponibilidade tecnica da auditoria * * @author Renato Tomaz Nati * @since 2026-05-25 */
    private boolean isPerguntaDeFalhaTecnicaAuditoria(AiResponse respostaIA) {
        if (respostaIA == null) {
            return false;
        }

        String question = respostaIA.getQuestion() != null ? respostaIA.getQuestion().toLowerCase() : "";
        String explanation = respostaIA.getExplanation() != null ? respostaIA.getExplanation().toLowerCase() : "";

        if (question.contains("auditoria final")
                || question.contains("seguir sem auditoria")
                || question.contains("validar pelo workspace eclipse")
                || question.contains("auditoria indisponivel")
                || question.contains("auditoria inconclusiva")) {
            return true;
        }

        if (explanation.contains("auditoria final indisponivel")
                || explanation.contains("confirmacao do usuario requerida antes de aplicar")
                || explanation.contains("falha tecnica da auditoria")
                || explanation.contains("seguir sem auditoria final")) {
            return true;
        }

        return false;
    }
    /** * Verifica o resultado de validacao do workspace sem depender de um getter * especifico da classe de retorno. * * @author Renato Tomaz Nati * @since 2026-05-25 */
    private boolean workspaceValidationSucceeded(WorkspaceCompilationValidationResult resultadoValidacao) {
        if (resultadoValidacao == null) {
            return true;
        }

        Boolean valor = invokeBooleanGetterIfExists(
                resultadoValidacao,
                "isSucesso",
                "isValido",
                "isCompilouSemErros",
                "isSemErros",
                "isSuccess"
        );

        if (valor != null) {
            return valor.booleanValue();
        }

        String texto = resultadoValidacao.toString() != null ? resultadoValidacao.toString().toLowerCase() : "";
        if (texto.contains("erro") || texto.contains("error") || texto.contains("falha") || texto.contains("failure")) {
            return false;
        }

        return true;
    }

    /** * Monta mensagem amigavel da validacao do workspace sem depender de um getter * especifico. * * @author Renato Tomaz Nati * @since 2026-05-25 */
    private String buildWorkspaceValidationMessage(WorkspaceCompilationValidationResult resultadoValidacao) {
        if (resultadoValidacao == null) {
            return "Nao foi possivel obter detalhes da validacao do workspace.";
        }

        String valor = invokeStringGetterIfExists(
                resultadoValidacao,
                "getMensagemUsuario",
                "getMensagem",
                "getResumoErros",
                "getSummary",
                "getDescription"
        );

        if (valor != null && valor.trim().length() > 0) {
            return valor;
        }

        return resultadoValidacao.toString();
    }

    private Boolean invokeBooleanGetterIfExists(Object alvo, String... nomesMetodos) {
        if (alvo == null || nomesMetodos == null) {
            return null;
        }

        for (int i = 0; i < nomesMetodos.length; i++) {
            try {
                java.lang.reflect.Method method = alvo.getClass().getMethod(nomesMetodos[i]);
                Object retorno = method.invoke(alvo);
                if (retorno instanceof Boolean) {
                    return (Boolean) retorno;
                }
            } catch (Exception e) {
            }
        }

        return null;
    }

    private String invokeStringGetterIfExists(Object alvo, String... nomesMetodos) {
        if (alvo == null || nomesMetodos == null) {
            return null;
        }

        for (int i = 0; i < nomesMetodos.length; i++) {
            try {
                java.lang.reflect.Method method = alvo.getClass().getMethod(nomesMetodos[i]);
                Object retorno = method.invoke(alvo);
                if (retorno instanceof String) {
                    return (String) retorno;
                }
            } catch (Exception e) {
            }
        }

        return null;
    }

    private int calcularComprimentoAplicado(String action, String conteudoNovo, int comprimentoOriginal) {
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
    /** * Bloqueia comentarios que tentem duplicar o metodo inteiro ou que tragam * estrutura de codigo no lugar de comentario explicativo. * * @param conteudoComentario conteudo retornado pela IA * @param trechoSelecionado trecho original selecionado * @return true quando o comentario for inseguro ou inadequado * * @author Renato Tomaz Nati * @since 2026-05-18 */
    private boolean comentarioInvalidoParaTrecho(String conteudoComentario, String trechoSelecionado) {
        if (conteudoComentario == null || conteudoComentario.trim().length() == 0) {
            return true;
        }

        String comentarioNormalizado = conteudoComentario.trim();
        String trechoNormalizado = trechoSelecionado != null ? trechoSelecionado.trim() : "";

        if (comentarioNormalizado.contains("@Override")) {
            return true;
        }

        if (comentarioNormalizado.contains("protected void ")
                || comentarioNormalizado.contains("public void ")
                || comentarioNormalizado.contains("private void ")
                || comentarioNormalizado.contains(" throws Exception")) {
            return true;
        }

        if (comentarioNormalizado.contains("{") || comentarioNormalizado.contains("}")) {
            return true;
        }

        if (trechoNormalizado.length() > 0) {
            if (comentarioNormalizado.length() > trechoNormalizado.length() * 2) {
                return true;
            }

            String trechoSemEspacos = trechoNormalizado.replaceAll("\\s+", "");
            String comentarioSemEspacos = comentarioNormalizado.replaceAll("\\s+", "");

            if (trechoSemEspacos.length() > 0 && comentarioSemEspacos.contains(trechoSemEspacos)) {
                return true;
            }
        }

        return false;
    }
  /**
 * Monta a instrucao enriquecida usada pela IA.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-18
 */
    private String construirInstrucaoFinal(String instrucao, ICompilationUnit unidade, ITextSelection selecao, int profundidadeMaxima, InvestigationCoveragePolicy.CoveragePlan coveragePlan) {
        StringBuilder instrucaoFinal = new StringBuilder();
        if (instrucao != null) {
            instrucaoFinal.append("INSTRUCAO ATUAL: ").append(instrucao);
        }
        instrucaoFinal.append("\n\n");

        String perfilRaciocinioAtual = view != null ? view.getPerfilRaciocinioConfigurado() : ChatRuntimeSettings.PERFIL_PADRAO;
        String perfilRaciocinioNormalizado = complexityProfilePolicy != null
                ? complexityProfilePolicy.normalizarPerfil(perfilRaciocinioAtual)
                : ChatRuntimeSettings.PERFIL_PADRAO;

        int profundidadeEfetiva = profundidadeMaxima;
        if (ChatRuntimeSettings.PERFIL_ULTRA.equals(perfilRaciocinioNormalizado) && profundidadeEfetiva < 4) {
            profundidadeEfetiva = 4;
        } else if (ChatRuntimeSettings.PERFIL_COMPLEXO.equals(perfilRaciocinioNormalizado) && profundidadeEfetiva < 3) {
            profundidadeEfetiva = 3;
        } else if (profundidadeEfetiva < 2) {
            profundidadeEfetiva = 2;
        }

        NamedCodeBlock blocoPrincipalAtivo = resolverBlocoPrincipalAtivo();
        if (blocoPrincipalAtivo != null) {
            instrucaoFinal.append("ALVO PRINCIPAL: ")
                    .append(blocoPrincipalAtivo.getName())
                    .append(" | arquivo=")
                    .append(blocoPrincipalAtivo.getFileName())
                    .append(" | linhas=")
                    .append(blocoPrincipalAtivo.getStartLine())
                    .append("-")
                    .append(blocoPrincipalAtivo.getEndLine())
                    .append("\n");
            instrucaoFinal.append("REGRA: Se existir bloco PRIMARY ativo, ele e o sujeito principal da analise e tambem pode ser alterado quando a tarefa exigir mutacao. ");
            instrucaoFinal.append("A selecao atual do editor continua relevante como contexto auxiliar, salvo se o usuario mandar explicitamente usar outro alvo. ");
            instrucaoFinal.append("Blocos REFERENCE continuam somente leitura.\n\n");
        }

        String historicoAtual = sessionHistoryService.obter();
        if (historicoAtual != null && historicoAtual.trim().length() > 0) {
            instrucaoFinal.append("=== HISTORICO DESTA SESSAO (MEMORIA RECENTE) ===\n");
            instrucaoFinal.append(historicoAtual);
            instrucaoFinal.append("\n================================================\n\n");
        }

        if (complexityProfilePolicy != null) {
            instrucaoFinal.append(complexityProfilePolicy.buildProfileDirective(perfilRaciocinioNormalizado, instrucao)).append("\n\n");
        }

        if (investigationCoveragePolicy != null && coveragePlan != null) {
            instrucaoFinal.append(investigationCoveragePolicy.buildPromptDirective(coveragePlan)).append("\n\n");
        }

        instrucaoFinal.append("Responda somente em JSON valido com os campos action, content e explanation.\n");
        instrucaoFinal.append("Valores aceitos para action: executar_ferramenta, responder_ao_usuario, perguntar_ao_usuario, substituir, comentar, explicar, inserir_abaixo, anexar_acima.\n");
        instrucaoFinal.append("Nao escreva texto fora do JSON.\n");
        instrucaoFinal.append("Regras operacionais de ferramenta:\n");
        instrucaoFinal.append("1. Sempre use path relativo a raiz segura do projeto.\n");
        instrucaoFinal.append("2. Nunca use caminho absoluto.\n");
        instrucaoFinal.append("3. Se houver duvida sobre a base do projeto, use verificar_raiz_projeto antes de explorar ou buscar texto.\n");
        instrucaoFinal.append("4. Se uma ferramenta devolver uma raiz segura, reutilize essa base nas proximas buscas.\n");
        instrucaoFinal.append("5. Nao repita a mesma ferramenta com os mesmos parametros se o resultado anterior ja trouxe a resposta.\n");
        instrucaoFinal.append("6. Sempre aproveite os nomes de arquivos, classes e pacotes devolvidos pelas ferramentas.\n");
        instrucaoFinal.append("7. Se o usuario pedir alteracao de arquivo preexistente que esteja apenas dentro de um package ou pasta marcada como editavel, mas esse arquivo nao estiver explicitamente marcado como editavel, bloqueie a alteracao direta.\n");
        instrucaoFinal.append("8. Nessa situacao, explique de forma curta e objetiva que o package ou pasta editavel autoriza criacao, mas nao autoriza alterar arquivos preexistentes nao marcados.\n");
        instrucaoFinal.append("9. Nessa mesma situacao, ofereca caminho seguro: marcar o arquivo explicitamente como editavel ou criar novo arquivo dentro do package ou pasta editavel.\n");
        instrucaoFinal.append("10. Se o arquivo preexistente tambem estiver explicitamente marcado como editavel, a alteracao pode prosseguir, respeitando backup obrigatorio quando a politica exigir.\n");
        instrucaoFinal.append("11. Antes de propor criacao, alteracao ou exclusao em contexto estrutural, prefira consultar a politica real com a ferramenta consultar_politica_mutacao_contexto quando houver qualquer duvida.\n");

        if (profundidadeEfetiva > 0 && unidade != null && selecao != null) {
            instrucaoFinal.append("Prompt fundamental deve ser seguido mesmo sem breadcrumb.\n");

            final ICompilationUnit unidadeFinal = unidade;
            final ITextSelection selecaoFinal = selecao;
            final int profundidadeFinal = profundidadeEfetiva;
            final String[] breadcrumbHolder = new String[1];

            Thread worker = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        breadcrumbHolder[0] = contextOrchestrator.enraizarChamadas(unidadeFinal, selecaoFinal.getOffset(), profundidadeFinal);
                    } catch (Exception e) {
                        breadcrumbHolder[0] = null;
                    }
                }
            });
            worker.setName("Breadcrumb-Timebox");
            worker.setDaemon(true);
            worker.start();

            try {
                worker.join(1500L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (worker.isAlive()) {
                worker.interrupt();
            }

            String breadcrumb = breadcrumbHolder[0];
            if (breadcrumb != null && breadcrumb.length() > 0) {
                instrucaoFinal.append("\n// REFERENCIA OBRIGATORIA: OS METODOS ABAIXO FAZEM PARTE DO FLUXO DE EXECUCAO E COMPOEM O CONTEXTO DO PROJETO:\n");
                instrucaoFinal.append(breadcrumb);
            } else {
                instrucaoFinal.append("\n// Observacao: breadcrumb indisponivel por limite de tempo ou falha isolada.\n");
            }
        }

        String contextoBlocos = namedBlockPromptFormatter.format(namedBlockSessionService.getAll());
        if (contextoBlocos != null && contextoBlocos.trim().length() > 0) {
            instrucaoFinal.append("\n\n");
            instrucaoFinal.append(contextoBlocos);
            instrucaoFinal.append("\n");
            instrucaoFinal.append("Se o usuario mencionar nomes de blocos, trate esses nomes como referencias exatas aos trechos nomeados.\n");
        }

        String contextoEstrutural = namedStructuralContextPromptFormatter.format(namedStructuralContextSessionService.getAll());
        if (contextoEstrutural != null && contextoEstrutural.trim().length() > 0) {
            instrucaoFinal.append("\n\n");
            instrucaoFinal.append(contextoEstrutural);
            instrucaoFinal.append("\n");
            instrucaoFinal.append("Se o usuario mencionar nomes de arquivos, packages ou pastas estruturais, trate esses nomes como referencias exatas aos contextos estruturais nomeados.\n");
        }

        return instrucaoFinal.toString();
    }

 


    

   

    private boolean pareceAcaoDeEdicao(String acao) {
        if (acao == null) return false;
        return "substituir".equalsIgnoreCase(acao) || "comentar".equalsIgnoreCase(acao) || "inserir_abaixo".equalsIgnoreCase(acao) || "anexar_acima".equalsIgnoreCase(acao);
    }

    private boolean conteudoCompativelComSelecao(String conteudo, String trechoSelecionado) {
        if (conteudo == null || trechoSelecionado == null) return false;
        String conteudoNormalizado = conteudo.trim();
        String selecaoNormalizada = trechoSelecionado.trim();
        if (conteudoNormalizado.length() == 0 || selecaoNormalizada.length() == 0) return false;
        if (conteudoNormalizado.length() > selecaoNormalizada.length() * 8) return false;
        if (conteudoNormalizado.contains("\npackage ") || conteudoNormalizado.startsWith("package ")) return false;
        if (conteudoNormalizado.contains("\nimport ") || conteudoNormalizado.startsWith("import ")) return false;
        if (conteudoNormalizado.contains("public class ") || conteudoNormalizado.contains("class ") && conteudoNormalizado.indexOf("class ") < 120) return false;
        return true;
    }

    private String montarComentarioBloco(String conteudo) {
        StringBuilder comentario = new StringBuilder();
        String delimitador = getLineDelimiter(document);
        String[] linhas = conteudo.split("\\r?\\n");

        comentario.append("/*").append(delimitador);
        for (int i = 0; i < linhas.length; i++) {
            String linhaAtual = linhas[i] != null ? linhas[i].trim() : "";
            if (linhaAtual.length() > 0) {
                comentario.append(" ").append(linhaAtual).append(delimitador);
            }
        }
        comentario.append("*/").append(delimitador);
        return comentario.toString();
    }

    private String getLineDelimiter(IDocument doc) {
        String delimitador = System.lineSeparator();
        if (doc != null) {
            String definidoNoDocumento = TextUtilities.getDefaultLineDelimiter(doc);
            if (definidoNoDocumento != null) {
                delimitador = definidoNoDocumento;
            }
        }
        return delimitador;
    }

    private String normalizarFormatacao(String codigoBruto, IDocument documentSnapshot, ITextSelection selectionSnapshot) {
        String codigoLimpo = codigoBruto;
        try {
            if (codigoLimpo.contains("```")) {
                int indexInicio = codigoLimpo.indexOf("```");
                int indexFim = codigoLimpo.lastIndexOf("```");
                if (indexFim > indexInicio) {
                    codigoLimpo = codigoLimpo.substring(indexInicio, indexFim);
                    if (codigoLimpo.contains("\n")) {
                        codigoLimpo = codigoLimpo.substring(codigoLimpo.indexOf('\n') + 1);
                    }
                }
            }

            String indentacaoBase = "";
            if (documentSnapshot != null && selectionSnapshot != null) {
                int startLine = documentSnapshot.getLineOfOffset(selectionSnapshot.getOffset());
                int lineOffset = documentSnapshot.getLineOffset(startLine);
                String textOfLine = documentSnapshot.get(lineOffset, documentSnapshot.getLineLength(startLine));
                StringBuilder sbIndent = new StringBuilder();
                for (int i = 0; i < textOfLine.length(); i++) {
                    char c = textOfLine.charAt(i);
                    if (c == ' ' || c == '\t') {
                        sbIndent.append(c);
                    } else {
                        break;
                    }
                }
                indentacaoBase = sbIndent.toString();
            }

            codigoLimpo = codigoLimpo.replace("\r\n", "\n").replace("\r", "\n");
            String delimitador = System.lineSeparator();
            if (documentSnapshot != null) {
                delimitador = TextUtilities.getDefaultLineDelimiter(documentSnapshot);
                if (delimitador == null) {
                    delimitador = System.lineSeparator();
                }
            }

            boolean iaJaMandouIndentacao = false;
            String[] linhas = codigoLimpo.split("\n");
            if (linhas.length > 1 && linhas[1].startsWith(indentacaoBase) && indentacaoBase.length() > 0) {
                iaJaMandouIndentacao = true;
            }

            if (!iaJaMandouIndentacao) {
                codigoLimpo = codigoLimpo.replace("\n", delimitador + indentacaoBase);
                if (documentSnapshot != null && selectionSnapshot != null) {
                    int startLine = documentSnapshot.getLineOfOffset(selectionSnapshot.getOffset());
                    int lineOffset = documentSnapshot.getLineOffset(startLine);
                    if (selectionSnapshot.getOffset() <= lineOffset + indentacaoBase.length()) {
                        if (!codigoLimpo.startsWith(indentacaoBase)) {
                            codigoLimpo = indentacaoBase + codigoLimpo;
                        }
                    }
                }
            } else {
                codigoLimpo = codigoLimpo.replace("\n", delimitador);
            }
        } catch (Exception e) {
            System.err.println("Alerta: Falha na normalizacao - " + e.getMessage());
        }

        return codigoLimpo.replaceAll("\\s+$", "");
    }
    private NamedCodeBlock resolverBlocoPrincipalAtivo() {
        if (namedBlockSessionService == null) {
            return null;
        }
        return namedBlockSessionService.findPrimary();
    }
    /**
 * Atualiza o resumo do alvo na view.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-18
 */
    /** * Atualiza o resumo do alvo na view de conversa de forma compacta. * * <p>O resumo nao deve exibir preview textual da selecao ativa. Para alvo * estrutural de arquivo, a exibicao deve indicar que o escopo e o arquivo * inteiro, sem simular uma selecao de linhas.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
    /** * Atualiza o resumo do alvo na view de conversa de forma compacta. * * <p>O resumo nao deve exibir preview textual da selecao ativa. Para alvo * estrutural de arquivo, a exibicao deve indicar que o escopo e o arquivo * inteiro, sem simular uma selecao de linhas.</p> * * <p>Quando nao houver alvo textual ou arquivo estrutural principal ativo, mas * houver contextos estruturais utilizaveis na sessao, a view deve refletir * isso para evitar a falsa impressao de ausencia total de contexto.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void atualizarResumoAlvoNaView() {
        if (view == null) {
            return;
        }

        StringBuilder resumo = new StringBuilder();

        if (compUnit != null) {
            resumo.append("Arquivo: ").append(compUnit.getElementName()).append(System.lineSeparator());
        } else {
            NamedStructuralContext contextoPrincipal = resolverContextoEstruturalPrincipalAtivo();
            if (contextoPrincipal != null && contextoPrincipal.getFileName() != null && contextoPrincipal.getFileName().trim().length() > 0) {
                resumo.append("Arquivo: ").append(contextoPrincipal.getFileName()).append(System.lineSeparator());
            } else {
                resumo.append("Arquivo: Desconhecido").append(System.lineSeparator());
            }
        }

        NamedCodeBlock blocoPrincipalAtivo = resolverBlocoPrincipalAtivo();
        if (blocoPrincipalAtivo != null) {
            if (selection != null && selection.getLength() > 0 && document != null) {
                try {
                    int startLine = selection.getStartLine() + 1;
                    int endLine = selection.getEndLine() + 1;
                    resumo.append("Linhas: ").append(startLine).append(" ate ").append(endLine).append(System.lineSeparator());
                } catch (Exception e) {
                    resumo.append("Linhas: indisponivel").append(System.lineSeparator());
                }
            }

            resumo.append("Principal: ")
                  .append(blocoPrincipalAtivo.getName())
                  .append(" [")
                  .append(blocoPrincipalAtivo.getFileName())
                  .append(":")
                  .append(blocoPrincipalAtivo.getStartLine())
                  .append("-")
                  .append(blocoPrincipalAtivo.getEndLine())
                  .append("]");

            view.atualizarResumoAlvo(resumo.toString());
            return;
        }

        NamedStructuralContext contextoPrincipal = resolverContextoEstruturalPrincipalAtivo();
        if (contextoPrincipal != null) {
            resumo.append("Escopo: arquivo inteiro").append(System.lineSeparator());
            resumo.append("Principal: ")
                  .append(contextoPrincipal.getName())
                  .append(" [FILE")
                  .append(" | ")
                  .append(contextoPrincipal.getFileName() != null ? contextoPrincipal.getFileName() : "")
                  .append(" | ")
                  .append(contextoPrincipal.getRelativePath() != null ? contextoPrincipal.getRelativePath() : "")
                  .append("]");

            view.atualizarResumoAlvo(resumo.toString());
            return;
        }

        if (possuiContextoEstruturalUtilizavel()) {
            java.util.List<NamedStructuralContext> contexts = namedStructuralContextSessionService.getAll();

            resumo = new StringBuilder();
            resumo.append("Contexto estrutural ativo").append(System.lineSeparator());

            int adicionados = 0;
            for (int i = 0; i < contexts.size(); i++) {
                NamedStructuralContext context = contexts.get(i);
                if (context == null || !context.isUsable()) {
                    continue;
                }

                resumo.append("- ")
                      .append(context.getName())
                      .append(" [")
                      .append(context.getRole() != null ? context.getRole().name() : "")
                      .append(" | ")
                      .append(context.getType() != null ? context.getType().name() : "")
                      .append("]");

                if (context.getRelativePath() != null && context.getRelativePath().trim().length() > 0) {
                    resumo.append(" -> ").append(context.getRelativePath());
                } else if (context.getFileName() != null && context.getFileName().trim().length() > 0) {
                    resumo.append(" -> ").append(context.getFileName());
                }

                resumo.append(System.lineSeparator());
                adicionados++;

                if (adicionados >= 4) {
                    break;
                }
            }

            view.atualizarResumoAlvo(resumo.toString().trim());
            return;
        }

        view.atualizarResumoAlvo("Nenhum alvo ativo");
    }
    /**
 * Atualiza o status operacional da view de forma segura.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-18
 */
    private void atualizarStatusNaView(final String status) {
        if (view == null) {
            return;
        }

        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                view.atualizarStatusOperacional(status);
            }
        });
    }
    /** * Caller: executarMissaoIA, setContext, sincronizarAlvoPrimarioGlobal e demais fluxos de controle * Callee: ChatView.registrarAtividadeAgente * Objetivo: Registrar uma linha de atividade operacional para que o usuario * acompanhe o que o agente esta fazendo sem depender apenas da barra de progresso. * Data modificacao: 2026-05-24 00:00 * * @param fase fase logica da operacao * @param detalhe detalhe objetivo da atividade * * @author Renato Tomaz Nati * @since 2026-05-24 */
    private void registrarAtividadeOperacional(String fase, String detalhe) {
        if (view == null) {
            return;
        }

        view.registrarAtividadeAgente(fase, detalhe);
    }
    /** * Caller: executarMissaoIA * Callee: N/A * Objetivo: Reconfigurar o coordenador de modelos com base na configuracao atual da view. * Feature: Garante que o modo monomodelo ou multimodelo seja aplicado em runtime antes da missao. * Data modificacao: 2026-05-24 00:00 * * @author Renato Tomaz Nati * @since 2026-05-24 */
    /** * Caller: executarMissaoIA * Callee: SingleModelCoordinator, MultiModelCoordinator * Objetivo: Reconfigurar o coordenador de modelos com base na configuracao * atual da view antes de cada missao. * Feature: Garante que mudancas de modo na aba de configuracao sejam aplicadas * imediatamente, sem depender do tipo anterior do objeto em memoria. * Data modificacao: 2026-05-24 00:00 * * @author Renato Tomaz Nati * @since 2026-05-24 */
    private void reconfigurarCoordenadorModelosAtual() {
        if (view == null) {
            agentModelCoordinator = new SingleModelCoordinator();
            System.out.println("[MCP COORDINATOR DEBUG] coordinatorClass=" + agentModelCoordinator.getClass().getName()
                    + " | viewAvailable=false | modoExecucao=single | multiModelEnabled=false");
            return;
        }

        String modoExecucao = view.getModoExecucaoConfigurado();

        if (ChatRuntimeSettings.MODO_EXECUCAO_MULTI.equals(modoExecucao)) {
            agentModelCoordinator = new MultiModelCoordinator();
            System.out.println("[MCP COORDINATOR DEBUG] coordinatorClass=" + agentModelCoordinator.getClass().getName()
                    + " | modoExecucao=" + modoExecucao + " | multiModelEnabled=true");
            return;
        }

        agentModelCoordinator = new SingleModelCoordinator();
        System.out.println("[MCP COORDINATOR DEBUG] coordinatorClass=" + agentModelCoordinator.getClass().getName()
                + " | modoExecucao=" + modoExecucao + " | multiModelEnabled=false");
    }
    private boolean validarPreCondicoesAplicacao(AiResponse respostaIA) {
        if (respostaIA == null) {
            view.adicionarMensagem("Sistema", "Falha operacional: resposta da IA ausente para aplicacao.");
            atualizarStatusNaView("Resposta ausente para aplicacao");
            registrarAtividadeOperacional("ERRO", "Resposta da IA ausente para aplicacao.");
            return false;
        }

        if (document == null || selection == null) {
            view.adicionarMensagem("Sistema", "Falha operacional: documento ou selecao indisponivel para aplicacao.");
            atualizarStatusNaView("Documento indisponivel");
            registrarAtividadeOperacional("ERRO", "Documento ou selecao indisponivel para aplicacao.");
            return false;
        }

        return true;
    }
    private EstadoAplicacaoCodigo capturarEstadoAntesDaAplicacao(String action, String conteudoNovo) throws Exception {
        EstadoAplicacaoCodigo estado = new EstadoAplicacaoCodigo();

        int offsetInicial = selection.getOffset();
        int comprimentoOriginal = selection.getLength();
        String conteudoAnterior = document.get(offsetInicial, comprimentoOriginal);

        estado.setOffsetInicial(offsetInicial);
        estado.setComprimentoOriginal(comprimentoOriginal);
        estado.setConteudoAnterior(conteudoAnterior);

        return estado;
    }
    private void logAplicacaoDebug(String action, EstadoAplicacaoCodigo estado, String conteudoNovo) {
        System.out.println("[CHAT APPLY DEBUG] action=" + action);
        System.out.println("[CHAT APPLY DEBUG] offsetInicial=" + estado.getOffsetInicial());
        System.out.println("[CHAT APPLY DEBUG] comprimentoOriginal=" + estado.getComprimentoOriginal());
        System.out.println("[CHAT APPLY DEBUG] conteudoNovoLength=" + (conteudoNovo != null ? conteudoNovo.length() : 0));
    }
    private boolean aplicarEdicaoNoDocumento(String action, String conteudoNovo, EstadoAplicacaoCodigo estado) throws Exception {
        if ("substituir".equalsIgnoreCase(action)) {
            document.replace(
                    estado.getOffsetInicial(),
                    estado.getComprimentoOriginal(),
                    conteudoNovo
            );
            return true;
        }

        if ("inserir_abaixo".equalsIgnoreCase(action)) {
            int offsetFinal = estado.getOffsetInicial() + estado.getComprimentoOriginal();
            document.replace(offsetFinal, 0, System.lineSeparator() + conteudoNovo);
            return true;
        }

        if ("anexar_acima".equalsIgnoreCase(action)) {
            document.replace(estado.getOffsetInicial(), 0, conteudoNovo + System.lineSeparator());
            return true;
        }

        if ("comentar".equalsIgnoreCase(action)) {
            document.replace(
                    estado.getOffsetInicial(),
                    estado.getComprimentoOriginal(),
                    conteudoNovo
            );
            return true;
        }

        view.adicionarMensagem("Sistema", "Acao de edicao nao suportada para aplicacao: " + action);
        atualizarStatusNaView("Acao nao suportada");
        registrarAtividadeOperacional("ERRO", "Acao de edicao nao suportada para aplicacao: " + action);
        return false;
    }
  
    private static class EstadoAplicacaoCodigo {
        private int offsetInicial;
        private int comprimentoOriginal;
        private String conteudoAnterior;

        public int getOffsetInicial() {
            return offsetInicial;
        }

        public void setOffsetInicial(int offsetInicial) {
            this.offsetInicial = offsetInicial;
        }

        public int getComprimentoOriginal() {
            return comprimentoOriginal;
        }

        public void setComprimentoOriginal(int comprimentoOriginal) {
            this.comprimentoOriginal = comprimentoOriginal;
        }

        public String getConteudoAnterior() {
            return conteudoAnterior;
        }

        public void setConteudoAnterior(String conteudoAnterior) {
            this.conteudoAnterior = conteudoAnterior;
        }
    }
}