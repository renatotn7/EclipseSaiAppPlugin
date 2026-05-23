package com.mcp.sailibrary.plugin.chat.blocks.controllers;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mcp.sailibrary.plugin.chat.blocks.model.NamedBlockKind;
import com.mcp.sailibrary.plugin.chat.blocks.model.NamedCodeBlock;
import com.mcp.sailibrary.plugin.chat.blocks.service.BlockNameSuggestionService;
import com.mcp.sailibrary.plugin.chat.blocks.service.EditorNavigationService;
import com.mcp.sailibrary.plugin.chat.blocks.service.NamedBlockDocumentBindingService;
import com.mcp.sailibrary.plugin.chat.blocks.service.NamedBlockHighlighter;
import com.mcp.sailibrary.plugin.chat.blocks.service.NamedBlockPromptFormatter;
import com.mcp.sailibrary.plugin.chat.blocks.service.NamedBlockSessionService;
import com.mcp.sailibrary.plugin.chat.blocks.views.NamedBlocksHost;
import com.mcp.sailibrary.plugin.chat.context.model.NamedContextTargetRole;
import com.mcp.sailibrary.plugin.chat.context.model.NamedStructuralContext;
import com.mcp.sailibrary.plugin.chat.context.model.NamedStructuralContextType;
import com.mcp.sailibrary.plugin.chat.context.service.ContextTargetNameSuggestionService;
import com.mcp.sailibrary.plugin.chat.context.service.NamedStructuralContextSessionService;
import com.mcp.sailibrary.plugin.chat.views.ChatView;

/** * Aplica highlight e anotacoes semanticas para blocos textuais nomeados no * editor. * * <p>O servico reaproveita anotacoes por sessao e tambem aplica pintura de * fundo por linha para blocos do arquivo atualmente aberto.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class NamedBlocksController {

    private static final int MAX_BLOCKS_PER_SESSION = 12;
    private static final int MAX_SELECTION_LENGTH = 12000;

    private final NamedBlocksHost view;
    private final NamedBlockSessionService blockSessionService;
    private final NamedStructuralContextSessionService structuralSessionService;
    private final NamedBlockHighlighter highlighter;
    private final BlockNameSuggestionService blockNameSuggestionService;
    private final ContextTargetNameSuggestionService structuralNameSuggestionService;
    private final NamedBlockPromptFormatter blockPromptFormatter;
    private final NamedBlockDocumentBindingService documentBindingService;
    private final EditorNavigationService navigationService;

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: NamedBlocksPanel * Callee: sessoes e services de contexto * Objetivo: Inicializar o controlador hibrido preservando o fluxo atual de blocos. */
    public NamedBlocksController(NamedBlocksHost view) {
        this.view = view;
        this.blockSessionService = NamedBlockSessionService.getInstance();
        this.structuralSessionService = NamedStructuralContextSessionService.getInstance();
        this.highlighter = NamedBlockHighlighter.getInstance();
        this.blockNameSuggestionService = new BlockNameSuggestionService();
        this.structuralNameSuggestionService = new ContextTargetNameSuggestionService();
        this.blockPromptFormatter = new NamedBlockPromptFormatter();
        this.documentBindingService = new NamedBlockDocumentBindingService();
        this.navigationService = new EditorNavigationService();
    }

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: painel de contexto * Callee: adicionarSelecaoTextual * Objetivo: Registrar a selecao atual do editor como bloco principal da analise. */
    public void adicionarSelecaoComoPrincipal() {
        adicionarSelecaoTextual(NamedBlockKind.PRIMARY);
    }

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: painel de contexto * Callee: adicionarSelecaoTextual * Objetivo: Registrar a selecao atual do editor como bloco editavel. */
    public void adicionarSelecaoComoEditavel() {
        adicionarSelecaoTextual(NamedBlockKind.EDITABLE);
    }

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: painel de contexto * Callee: adicionarSelecaoTextual * Objetivo: Registrar a selecao atual do editor como bloco referencial. */
    public void adicionarSelecaoComoReferencia() {
        adicionarSelecaoTextual(NamedBlockKind.REFERENCE);
    }

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: handlers do explorer * Callee: adicionarSelecaoEstruturalComoContexto * Objetivo: Adicionar item estrutural atual como foco principal. */
    public void adicionarComoPrincipal(ISelection selection) {
        adicionarSelecaoEstruturalComoContexto(NamedContextTargetRole.PRIMARY, selection);
    }

    public void adicionarComoEditavel(ISelection selection) {
        adicionarSelecaoEstruturalComoContexto(NamedContextTargetRole.EDITABLE, selection);
    }

    public void adicionarComoReferencia(ISelection selection) {
        adicionarSelecaoEstruturalComoContexto(NamedContextTargetRole.REFERENCE, selection);
    }
    /** * Solicita a insercao de um alias de contexto na conversa. * * @param name nome do bloco ou contexto estrutural * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void inserirAliasNaConversa(String name) {
        if (view == null || isBlank(name)) {
            return;
        }

        view.inserirAliasNaConversa("@" + name);
    }
    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: adicionarSelecaoComoPrincipal, adicionarSelecaoComoEditavel, adicionarSelecaoComoReferencia * Callee: NamedBlockSessionService.addBlock, NamedBlockDocumentBindingService.bindBlocksToDocument * Objetivo: Criar e registrar um bloco textual a partir da selecao atual do editor. */
    private void adicionarSelecaoTextual(NamedBlockKind kind) {
        try {
            if (blockSessionService.getAll().size() >= MAX_BLOCKS_PER_SESSION) {
                informarStatus("Limite de blocos da sessao atingido. Limpe ou remova blocos antes de adicionar novos.");
                return;
            }

            ITextEditor editor = getActiveTextEditor();
            if (editor == null) {
                informarStatus("Nenhum editor de texto ativo.");
                return;
            }

            if (editor.getSelectionProvider() == null || editor.getSelectionProvider().getSelection() == null) {
                informarStatus("Nao foi possivel obter a selecao atual do editor.");
                return;
            }

            if (!(editor.getSelectionProvider().getSelection() instanceof ITextSelection)) {
                informarStatus("A selecao atual nao e textual.");
                return;
            }

            ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();
            if (selection.getLength() <= 0) {
                informarStatus("Selecione um bloco antes de adicionar.");
                return;
            }

            if (selection.getLength() > MAX_SELECTION_LENGTH) {
                informarStatus("A selecao e grande demais para virar bloco nomeado. Reduza o trecho selecionado.");
                return;
            }

            IDocument document = resolveDocument(editor);

            String selectedText = selection.getText();
            if (selectedText == null || selectedText.trim().length() == 0) {
                informarStatus("A selecao atual nao possui conteudo util.");
                return;
            }

            ICompilationUnit compUnit = resolveCompilationUnit(editor);

            String filePath = "";
            String fileName = "Desconhecido";

            if (compUnit != null && compUnit.getResource() != null && compUnit.getResource().getLocation() != null) {
                File file = compUnit.getResource().getLocation().toFile();
                filePath = normalizePath(file);
                fileName = file.getName();
            } else if (editor.getEditorInput() != null) {
                fileName = editor.getEditorInput().getName();
            }

            if (filePath.length() == 0) {
                informarStatus("Nao foi possivel resolver o caminho fisico do arquivo atual.");
                return;
            }

            String apiKey = resolveApiKey();

            String suggestedName = blockNameSuggestionService.suggestName(
                    selectedText,
                    kind,
                    blockSessionService.collectNames(),
                    apiKey
            );

            if (kind == NamedBlockKind.PRIMARY) {
                limparPrimaryEstruturalAtivo();
            }

            NamedCodeBlock block = blockSessionService.addBlock(
                    suggestedName,
                    selectedText,
                    kind,
                    filePath,
                    fileName,
                    selection.getOffset(),
                    selection.getLength(),
                    selection.getStartLine() + 1,
                    selection.getEndLine() + 1
            );

            if (document != null) {
                documentBindingService.bindBlocksToDocument(document, blockSessionService.getAll(), filePath);
                documentBindingService.syncBlocksFromDocument(document, blockSessionService.getAll(), filePath);
            }

            refreshView();
            refreshHighlights();
            refreshExplorerDecorations();
            sincronizarChatComPrimaryGlobal();
            informarStatus("Bloco adicionado: " + block.getName() + " [" + block.getKind().name() + "]");
        } catch (Exception e) {
            informarStatus("Falha ao adicionar bloco: " + safeMessage(e));
        }
    }

    /** * Resolve a selecao estrutural atual do workbench e adiciona um ou mais * contextos estruturais conforme o papel solicitado. * * <p>PRIMARY estrutural continua aceitando apenas um unico item por vez. * EDITABLE e REFERENCE aceitam selecao multipla no explorer.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void adicionarSelecaoEstruturalComoContexto(NamedContextTargetRole role, ISelection selection) {
        try {
            IStructuredSelection structuredSelection = toStructuredSelection(selection);
            if (structuredSelection == null || structuredSelection.isEmpty()) {
                informarStatus("Nenhuma selecao estrutural valida foi encontrada no explorer.");
                return;
            }

            List<?> selectedItems = structuredSelection.toList();

            if (role == NamedContextTargetRole.PRIMARY && selectedItems.size() > 1) {
                informarStatus("Selecione apenas um arquivo estrutural para definir o PRIMARY.");
                return;
            }

            int adicionados = 0;
            int ignorados = 0;

            for (int i = 0; i < selectedItems.size(); i++) {
                Object selectedObject = selectedItems.get(i);
                boolean added = adicionarItemEstruturalComoContexto(role, selectedObject);
                if (added) {
                    adicionados++;
                } else {
                    ignorados++;
                }
            }

            if (adicionados > 0) {
                refreshView();
                refreshHighlights();
                refreshExplorerDecorations();
                sincronizarChatComPrimaryGlobal();

                if (ignorados > 0) {
                    informarStatus("Contextos estruturais adicionados: " + adicionados + ". Itens ignorados: " + ignorados + ".");
                } else {
                    informarStatus("Contextos estruturais adicionados: " + adicionados + ".");
                }
                return;
            }

            informarStatus("Nenhum item selecionado e suportado como contexto estrutural.");
        } catch (Exception e) {
            informarStatus("Falha ao adicionar contexto estrutural: " + safeMessage(e));
        }
    }
    /** * Tenta adicionar um unico item estrutural como contexto da sessao. * * <p>O metodo retorna true quando o item foi reconhecido e encaminhado para o * fluxo estrutural correspondente. Itens nao suportados retornam false sem * interromper o processamento da selecao multipla.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean adicionarItemEstruturalComoContexto(NamedContextTargetRole role, Object selectedObject) {
        if (selectedObject instanceof ICompilationUnit) {
            adicionarCompilationUnitComoContexto((ICompilationUnit) selectedObject, role);
            return true;
        }

        if (selectedObject instanceof IFile) {
            adicionarArquivoComoContexto((IFile) selectedObject, role);
            return true;
        }

        if (selectedObject instanceof IPackageFragment) {
            adicionarPackageComoContexto((IPackageFragment) selectedObject, role);
            return true;
        }

        if (selectedObject instanceof IFolder) {
            adicionarPastaComoContexto((IFolder) selectedObject, role);
            return true;
        }

        if (selectedObject instanceof IContainer) {
            adicionarPastaComoContexto((IContainer) selectedObject, role);
            return true;
        }

        if (selectedObject instanceof IAdaptable) {
            ICompilationUnit cu = (ICompilationUnit) ((IAdaptable) selectedObject).getAdapter(ICompilationUnit.class);
            if (cu != null) {
                adicionarCompilationUnitComoContexto(cu, role);
                return true;
            }

            IFile file = (IFile) ((IAdaptable) selectedObject).getAdapter(IFile.class);
            if (file != null) {
                adicionarArquivoComoContexto(file, role);
                return true;
            }

            IPackageFragment pkg = (IPackageFragment) ((IAdaptable) selectedObject).getAdapter(IPackageFragment.class);
            if (pkg != null) {
                adicionarPackageComoContexto(pkg, role);
                return true;
            }

            IFolder folder = (IFolder) ((IAdaptable) selectedObject).getAdapter(IFolder.class);
            if (folder != null) {
                adicionarPastaComoContexto(folder, role);
                return true;
            }

            IContainer container = (IContainer) ((IAdaptable) selectedObject).getAdapter(IContainer.class);
            if (container != null) {
                adicionarPastaComoContexto(container, role);
                return true;
            }
        }

        return false;
    }
    public void adicionarCompilationUnitComoContexto(ICompilationUnit compilationUnit, NamedContextTargetRole role) {
        if (compilationUnit == null) {
            informarStatus("Arquivo Java invalido para contexto.");
            return;
        }

        try {
            String apiKey = resolveApiKey();
            String fileName = compilationUnit.getElementName();
            String absolutePath = "";
            String relativePath = "";
            String packageName = "";

            if (compilationUnit.getResource() != null) {
                if (compilationUnit.getResource().getLocation() != null) {
                    absolutePath = normalizePath(compilationUnit.getResource().getLocation().toFile());
                }
                if (compilationUnit.getResource().getProjectRelativePath() != null) {
                    relativePath = compilationUnit.getResource().getProjectRelativePath().toString().replace("\\", "/");
                }
            }

            if (compilationUnit.getParent() != null) {
                packageName = compilationUnit.getParent().getElementName();
            }

            String suggestedName = structuralNameSuggestionService.suggestName(
                    fileName,
                    role,
                    NamedStructuralContextType.FILE,
                    fileName,
                    packageName,
                    structuralSessionService.collectNames(),
                    apiKey
            );

            if (role == NamedContextTargetRole.PRIMARY) {
                limparPrimaryTextualAtivo();
            }

            NamedStructuralContext context = new NamedStructuralContext();
            context.setName(suggestedName);
            context.setRole(role);
            context.setType(NamedStructuralContextType.FILE);
            context.setFilePath(absolutePath);
            context.setRelativePath(relativePath);
            context.setFileName(fileName);
            context.setPackageName(packageName);
            context.setPreview("Arquivo Java estrutural");
            context.setCreatedAt(System.currentTimeMillis());

            structuralSessionService.addContext(context);
            refreshView();
            refreshHighlights();
            refreshExplorerDecorations();
            sincronizarChatComPrimaryGlobal();
        } catch (Exception e) {
            informarStatus("Falha ao adicionar arquivo Java como contexto: " + safeMessage(e));
        }
    }

    public void adicionarArquivoComoContexto(IFile file, NamedContextTargetRole role) {
        try {
            if (role == NamedContextTargetRole.PRIMARY) {
                limparPrimaryTextualAtivo();
            }

            adicionarArquivoComoContextoInterno(file, role);

            refreshView();
            refreshHighlights();
            refreshExplorerDecorations();
            sincronizarChatComPrimaryGlobal();
            informarStatus("Arquivo adicionado como contexto.");
        } catch (Exception e) {
            informarStatus("Falha ao adicionar arquivo como contexto: " + safeMessage(e));
        }
    }
    /** * Registra um arquivo estrutural na sessao sem acionar refresh visual imediato. * * <p>Esse metodo deve ser usado por fluxos em lote, permitindo que a camada * chamadora controle o momento exato de atualizar view, highlights, explorer e * sincronizacao do alvo principal.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void adicionarArquivoComoContextoInterno(IFile file, NamedContextTargetRole role) {
        if (file == null) {
            throw new IllegalArgumentException("Arquivo invalido para contexto.");
        }

        String apiKey = resolveApiKey();
        String fileName = file.getName();
        String absolutePath = file.getLocation() != null ? normalizePath(file.getLocation().toFile()) : "";
        String relativePath = file.getProjectRelativePath() != null
                ? file.getProjectRelativePath().toString().replace("\\", "/")
                : "";

        String suggestedName = structuralNameSuggestionService.suggestName(
                fileName,
                role,
                NamedStructuralContextType.FILE,
                fileName,
                "",
                structuralSessionService.collectNames(),
                apiKey
        );

        NamedStructuralContext context = new NamedStructuralContext();
        context.setName(suggestedName);
        context.setRole(role);
        context.setType(NamedStructuralContextType.FILE);
        context.setFilePath(absolutePath);
        context.setRelativePath(relativePath);
        context.setFileName(fileName);
        context.setPreview("Arquivo estrutural");
        context.setCreatedAt(System.currentTimeMillis());

        structuralSessionService.addContext(context);
    }
    public void adicionarPackageComoContexto(IPackageFragment packageFragment, NamedContextTargetRole role) {
        if (packageFragment == null) {
            informarStatus("Package invalido para contexto.");
            return;
        }

        try {
            String apiKey = resolveApiKey();
            String packageName = packageFragment.getElementName();
            String relativePath = "";
            String absolutePath = "";
            String fileName = packageName;

            if (packageFragment.getResource() != null) {
                if (packageFragment.getResource().getProjectRelativePath() != null) {
                    relativePath = packageFragment.getResource().getProjectRelativePath().toString().replace("\\", "/");
                }
                if (packageFragment.getResource().getLocation() != null) {
                    absolutePath = normalizePath(packageFragment.getResource().getLocation().toFile());
                }
                fileName = packageFragment.getResource().getName();
            }

            String suggestedName = structuralNameSuggestionService.suggestName(
                    packageName,
                    role,
                    NamedStructuralContextType.PACKAGE,
                    fileName,
                    packageName,
                    structuralSessionService.collectNames(),
                    apiKey
            );

            NamedStructuralContext context = new NamedStructuralContext();
            context.setName(suggestedName);
            context.setRole(role);
            context.setType(NamedStructuralContextType.PACKAGE);
            context.setFilePath(absolutePath);
            context.setRelativePath(relativePath);
            context.setFileName(fileName);
            context.setPackageName(packageName);
            context.setPreview("Package estrutural");
            context.setCreatedAt(System.currentTimeMillis());

            structuralSessionService.addContext(context);
            refreshView();
            refreshHighlights();
            refreshExplorerDecorations();
            sincronizarChatComPrimaryGlobal();
        } catch (Exception e) {
            informarStatus("Falha ao adicionar package como contexto: " + safeMessage(e));
        }
    }

    public void adicionarPastaComoContexto(IContainer folder, NamedContextTargetRole role) {
        if (folder == null) {
            informarStatus("Pasta invalida para contexto.");
            return;
        }

        try {
            String apiKey = resolveApiKey();
            String folderName = folder.getName();
            String relativePath = folder.getProjectRelativePath() != null
                    ? folder.getProjectRelativePath().toString().replace("\\", "/")
                    : "";
            String absolutePath = folder.getLocation() != null
                    ? normalizePath(folder.getLocation().toFile())
                    : "";

            String suggestedName = structuralNameSuggestionService.suggestName(
                    folderName,
                    role,
                    NamedStructuralContextType.FOLDER,
                    folderName,
                    "",
                    structuralSessionService.collectNames(),
                    apiKey
            );

            NamedStructuralContext context = new NamedStructuralContext();
            context.setName(suggestedName);
            context.setRole(role);
            context.setType(NamedStructuralContextType.FOLDER);
            context.setFilePath(absolutePath);
            context.setRelativePath(relativePath);
            context.setFileName(folderName);
            context.setPreview("Pasta estrutural");
            context.setCreatedAt(System.currentTimeMillis());

            structuralSessionService.addContext(context);
            refreshView();
            refreshHighlights();
            refreshExplorerDecorations();
            sincronizarChatComPrimaryGlobal();
        } catch (Exception e) {
            informarStatus("Falha ao adicionar pasta como contexto: " + safeMessage(e));
        }
    }

    public void removerBloco(String name) {
        if (isBlank(name)) {
            informarStatus("Nome do contexto invalido para remocao.");
            return;
        }

        boolean removedBlock = blockSessionService.removeByName(name);
        boolean removedStructural = structuralSessionService.removeByName(name);

        refreshView();
        refreshHighlights();
        refreshExplorerDecorations();
        sincronizarChatComPrimaryGlobal();

        if (removedBlock || removedStructural) {
            informarStatus("Contexto removido: " + name);
        } else {
            informarStatus("Contexto nao encontrado para remocao: " + name);
        }
    }

    public void limparTudo() {
        blockSessionService.clearAll();
        structuralSessionService.clearAll();

        ITextEditor editor = getActiveTextEditor();
        if (editor != null) {
            highlighter.clearHighlights(editor);
        }

        refreshView();
        refreshHighlights();
        refreshExplorerDecorations();
        sincronizarChatComPrimaryGlobal();
        informarStatus("Sessao de contexto limpa.");
    }

    public void focarBloco(String name) {
        try {
            if (isBlank(name)) {
                return;
            }

            NamedCodeBlock block = blockSessionService.findByName(name);
            if (block != null) {
                ITextEditor editor = navigationService.openTextEditorForFilePath(block.getFilePath());
                if (editor == null) {
                    informarStatus("Nao foi possivel abrir o arquivo do bloco: " + block.getFileName());
                    return;
                }

                IDocument document = resolveDocument(editor);
                if (document != null) {
                    documentBindingService.bindBlocksToDocument(document, blockSessionService.getAll(), block.getFilePath());
                    documentBindingService.syncBlocksFromDocument(document, blockSessionService.getAll(), block.getFilePath());
                }

                highlighter.refreshHighlights(editor, blockSessionService.getAll(), block.getFilePath());
                navigationService.focusRange(editor, block.getEffectiveOffset(), block.getEffectiveLength());

                refreshView();
                informarStatus("Bloco focado: " + block.getName());
                return;
            }

            NamedStructuralContext structural = structuralSessionService.findByName(name);
            if (structural != null) {
                if (structural.getType() == NamedStructuralContextType.FILE && !isBlank(structural.getFilePath())) {
                    ITextEditor editor = navigationService.openTextEditorForFilePath(structural.getFilePath());
                    if (editor != null) {
                        refreshView();
                        informarStatus("Arquivo focado: " + structural.getName());
                        return;
                    }
                    informarStatus("Nao foi possivel abrir o arquivo do contexto: " + structural.getFileName());
                    return;
                }

                if (structural.getType() == NamedStructuralContextType.PACKAGE) {
                    informarStatus("Package localizado no contexto: " + structural.getName());
                    return;
                }

                if (structural.getType() == NamedStructuralContextType.FOLDER) {
                    informarStatus("Pasta localizada no contexto: " + structural.getName());
                    return;
                }
            }

            informarStatus("Contexto nao encontrado: " + name);
        } catch (Exception e) {
            informarStatus("Falha ao focar contexto: " + safeMessage(e));
        }
    }
    /** * Remove o bloco textual PRIMARY atual, quando existir. * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void limparPrimaryTextualAtivo() {
        NamedCodeBlock primary = blockSessionService.findPrimary();
        if (primary != null) {
            blockSessionService.removeByName(primary.getName());
        }
    }

    /** * Remove o contexto estrutural PRIMARY atual, quando existir. * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void limparPrimaryEstruturalAtivo() {
        NamedStructuralContext primary = structuralSessionService.findPrimary();
        if (primary != null) {
            structuralSessionService.removeByName(primary.getName());
        }
    }

    /** * Solicita a sincronizacao da aba Conversa com o PRIMARY global atual da sessao. * * <p>Esse metodo deve ser chamado sempre que houver adicao, remocao ou limpeza * de um contexto que possa alterar o alvo principal efetivo.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void sincronizarChatComPrimaryGlobal() {
        try {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (PlatformUI.getWorkbench() == null
                                || PlatformUI.getWorkbench().getActiveWorkbenchWindow() == null
                                || PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage() == null) {
                            return;
                        }

                        IViewPart viewPart = PlatformUI.getWorkbench()
                                .getActiveWorkbenchWindow()
                                .getActivePage()
                                .findView(ChatView.ID);

                        if (viewPart instanceof ChatView) {
                            ((ChatView) viewPart).sincronizarAlvoPrimarioGlobal();
                        }
                    } catch (Exception e) {
                    }
                }
            });
        } catch (Exception e) {
        }
    }
    public void refreshView() {
        if (view == null) {
            return;
        }

        final List<NamedCodeBlock> primaryBlocks = blockSessionService.getByKind(NamedBlockKind.PRIMARY);
        final List<NamedCodeBlock> editables = blockSessionService.getByKind(NamedBlockKind.EDITABLE);
        final List<NamedCodeBlock> references = blockSessionService.getByKind(NamedBlockKind.REFERENCE);

        final List<NamedStructuralContext> primaryContexts = structuralSessionService.getByRole(NamedContextTargetRole.PRIMARY);
        final List<NamedStructuralContext> editableContexts = structuralSessionService.getByRole(NamedContextTargetRole.EDITABLE);
        final List<NamedStructuralContext> referenceContexts = structuralSessionService.getByRole(NamedContextTargetRole.REFERENCE);

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (view != null) {
                    view.atualizarBlocos(primaryBlocks, editables, references);
                    view.atualizarContextosEstruturais(primaryContexts, editableContexts, referenceContexts);
                }
            }
        });
    }

    /** * Atualiza os highlights dos blocos nomeados no editor ativo e tambem em todos * os editores previamente rastreados pelo servico de highlight. * * <p>Esse comportamento evita sujeira visual quando um PRIMARY textual e * removido da sessao por troca de alvo principal.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void refreshHighlights() {
        try {
            ITextEditor editor = getActiveTextEditor();

            if (editor != null) {
                String currentFilePath = "";
                ICompilationUnit compUnit = resolveCompilationUnit(editor);

                if (compUnit != null && compUnit.getResource() != null && compUnit.getResource().getLocation() != null) {
                    currentFilePath = normalizePath(compUnit.getResource().getLocation().toFile());
                }

                if (currentFilePath.length() > 0) {
                    IDocument document = resolveDocument(editor);
                    if (document != null) {
                        documentBindingService.bindBlocksToDocument(document, blockSessionService.getAll(), currentFilePath);
                        documentBindingService.syncBlocksFromDocument(document, blockSessionService.getAll(), currentFilePath);
                    }

                    highlighter.refreshHighlights(editor, blockSessionService.getAll(), currentFilePath);
                }
            }

            highlighter.refreshAllTrackedEditors(blockSessionService.getAll());
        } catch (Exception e) {
            informarStatus("Falha ao atualizar destaques: " + safeMessage(e));
        }
    }

    public String formatarContextoParaIA() {
        StringBuilder sb = new StringBuilder();

        String blockContext = blockPromptFormatter.format(blockSessionService.getAll());
        if (!isBlank(blockContext)) {
            sb.append(blockContext);
        }

        String structuralContext = formatarContextoEstruturalParaIA();
        if (!isBlank(structuralContext)) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append(structuralContext);
        }

        return sb.toString();
    }

    private String formatarContextoEstruturalParaIA() {
        List<NamedStructuralContext> all = structuralSessionService.getAll();
        if (all == null || all.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== CONTEXTO ESTRUTURAL DA SESSAO ===\n");

        List<NamedStructuralContext> primary = structuralSessionService.getByRole(NamedContextTargetRole.PRIMARY);
        List<NamedStructuralContext> editables = structuralSessionService.getByRole(NamedContextTargetRole.EDITABLE);
        List<NamedStructuralContext> references = structuralSessionService.getByRole(NamedContextTargetRole.REFERENCE);

        appendStructuralRole(sb, "FOCO_PRINCIPAL_ESTRUTURAL", primary);
        appendStructuralRole(sb, "ESCOPO_EDITAVEL_ESTRUTURAL", editables);
        appendStructuralRole(sb, "ESCOPO_REFERENCIAL_ESTRUTURAL", references);

        sb.append("REGRAS:\n");
        sb.append("1. Arquivos, packages e pastas estruturais complementam o contexto textual.\n");
        sb.append("2. Itens estruturais do tipo REFERENCE nao devem ser alterados.\n");
        sb.append("3. Itens estruturais do tipo EDITABLE podem servir como destino de alteracao ou criacao quando a tarefa exigir isso.\n");
        sb.append("4. Packages vazias e pastas nao devem ser tratadas como foco principal por padrao.\n");

        return sb.toString();
    }

    private void appendStructuralRole(StringBuilder sb, String title, List<NamedStructuralContext> contexts) {
        sb.append(title).append(":\n");

        if (contexts == null || contexts.isEmpty()) {
            sb.append("- nenhum\n");
            return;
        }

        for (int i = 0; i < contexts.size(); i++) {
            NamedStructuralContext context = contexts.get(i);
            if (context == null || !context.isUsable()) {
                continue;
            }

            sb.append("- ").append(context.getName())
              .append(" -> tipo=").append(context.getType() != null ? context.getType().name() : "")
              .append(" | role=").append(context.getRole() != null ? context.getRole().name() : "");

            if (!isBlank(context.getFileName())) {
                sb.append(" | nome=").append(context.getFileName());
            }
            if (!isBlank(context.getPackageName())) {
                sb.append(" | package=").append(context.getPackageName());
            }
            if (!isBlank(context.getRelativePath())) {
                sb.append(" | caminho=").append(context.getRelativePath());
            }

            sb.append("\n");
        }
    }

    private ICompilationUnit resolveCompilationUnit(ITextEditor editor) {
        if (editor == null || editor.getEditorInput() == null) {
            return null;
        }

        try {
            org.eclipse.jdt.core.IJavaElement javaElement = org.eclipse.jdt.ui.JavaUI.getEditorInputJavaElement(editor.getEditorInput());
            if (javaElement instanceof ICompilationUnit) {
                return (ICompilationUnit) javaElement;
            }
        } catch (Exception e) {
        }

        return null;
    }

    private IDocument resolveDocument(ITextEditor editor) {
        if (editor == null || editor.getEditorInput() == null || editor.getDocumentProvider() == null) {
            return null;
        }

        try {
            return editor.getDocumentProvider().getDocument(editor.getEditorInput());
        } catch (Exception e) {
            return null;
        }
    }

    private ITextEditor getActiveTextEditor() {
        try {
            if (PlatformUI.getWorkbench() == null
                    || PlatformUI.getWorkbench().getActiveWorkbenchWindow() == null
                    || PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage() == null) {
                return null;
            }

            IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
            if (editor instanceof ITextEditor) {
                return (ITextEditor) editor;
            }
        } catch (Exception e) {
        }
        return null;
    }

    private String resolveApiKey() {
        String apiKey = System.getenv("SAI_MCP_API_KEY");
        if (apiKey == null || apiKey.trim().length() == 0) {
            apiKey = System.getProperty("SAI_MCP_API_KEY");
        }
        return apiKey;
    }

    private String normalizePath(File file) {
        if (file == null) {
            return "";
        }

        try {
            return file.getCanonicalPath().replace("\\", "/");
        } catch (Exception e) {
            return file.getAbsolutePath().replace("\\", "/");
        }
    }

    private void informarStatus(String message) {
        if (view != null) {
            view.adicionarMensagemStatus(message);
        }
    }

    private String safeMessage(Exception e) {
        if (e == null || e.getMessage() == null || e.getMessage().trim().length() == 0) {
            return "falha interna";
        }
        return e.getMessage();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private void refreshExplorerDecorations() {
        try {
            if (PlatformUI.getWorkbench() != null && PlatformUI.getWorkbench().getDecoratorManager() != null) {
                PlatformUI.getWorkbench().getDecoratorManager().update(
                        "com.mcp.sailibrary.plugin.chat.context.decorators.NamedStructuralContextDecorator");
            }
        } catch (Exception e) {
        }
    }

    private IStructuredSelection toStructuredSelection(ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            return (IStructuredSelection) selection;
        }
        return null;
    }
}