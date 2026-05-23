package com.mcp.sailibrary.plugin.chat.blocks.views;

import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.mcp.sailibrary.plugin.Activator;
import com.mcp.sailibrary.plugin.chat.blocks.controllers.NamedBlocksController;
import com.mcp.sailibrary.plugin.chat.blocks.model.NamedCodeBlock;
import com.mcp.sailibrary.plugin.chat.context.model.NamedStructuralContext;
import com.mcp.sailibrary.plugin.chat.context.model.NamedStructuralContextType;
import com.mcp.sailibrary.plugin.chat.views.ChatView;

/** * Exibe e gerencia o contexto hibrido da sessao, incluindo blocos textuais e * contextos estruturais nomeados. * * <p>O painel permite: * <ul> * <li>adicionar selecoes como blocos textuais</li> * <li>listar contextos estruturais e textuais</li> * <li>focar um contexto no editor</li> * <li>remover contextos</li> * <li>inserir aliases na conversa via botao dedicado</li> * </ul> * </p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class NamedBlocksPanel extends Composite implements NamedBlocksHost {

    private NamedBlocksController controller;

    private Composite rootContainer;

    private Composite primaryBlocksContainer;
    private Composite editablesContainer;
    private Composite referencesContainer;

    private Composite primaryStructuralContainer;
    private Composite editableStructuralContainer;
    private Composite referenceStructuralContainer;

    private Group primaryBlocksGroup;
    private Group editablesGroup;
    private Group referencesGroup;

    private Group primaryStructuralGroup;
    private Group editableStructuralGroup;
    private Group referenceStructuralGroup;

    private Label statusLabel;

    private Button btnAddPrimary;
    private Button btnAddEditable;
    private Button btnAddReference;
    private Button btnRefresh;
    private Button btnClear;

    private ChatView chatView;

    private java.util.List<Image> imagensBotoes = new java.util.ArrayList<Image>();

    private Image imgAddPrimary;
    private Image imgAddEditable;
    private Image imgAddReference;
    private Image imgRefresh;
    private Image imgClearAll;
    private Image imgAlias;
    private Image imgRemove;

    /** * Inicializa o painel sem associacao direta com a view principal do chat. * * @param parent componente pai * @param style estilo SWT * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public NamedBlocksPanel(Composite parent, int style) {
        this(parent, style, null);
    }

    /** * Inicializa o painel de contextos nomeados da sessao. * * @param parent componente pai * @param style estilo SWT * @param chatView view principal de conversa, quando disponivel * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public NamedBlocksPanel(Composite parent, int style, ChatView chatView) {
        super(parent, style);

        this.chatView = chatView;

        GridLayout selfLayout = new GridLayout(1, false);
        selfLayout.marginWidth = 0;
        selfLayout.marginHeight = 0;
        selfLayout.verticalSpacing = 0;
        setLayout(selfLayout);

        carregarImagens();

        this.controller = new NamedBlocksController(this);
        createContent();
    }

    @Override
    public void inserirAliasNaConversa(String alias) {
        if (chatView != null) {
            chatView.anexarAliasNaEntrada(alias);
        }
    }

    /** * Carrega as imagens usadas pelos botoes do painel. * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void carregarImagens() {
    	imgAddPrimary = carregarImagem("icons/ui/star_icon32.png");
    	imgAddEditable = carregarImagem("icons/ui/edit_icon32.png");
    	imgAddReference = carregarImagem("icons/ui/bookmark_icon32.png");
    	imgRefresh = carregarImagem("icons/ui/atualizar_icon32.png");
    	imgClearAll = carregarImagem("icons/ui/limpar_contextos32.png");
    	imgAlias = carregarImagem("icons/ui/contagotas_icon32.png");
    	imgRemove = carregarImagem("icons/ui/lixeira_icon32.png");
    }
    /** * Cria um label com imagem clicavel para uso como acao compacta na interface. * * @param parent componente pai * @param image imagem a ser exibida * @param tooltip tooltip da acao * @param runnable acao a executar no clique * @return label configurado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    /** * Cria um label com imagem clicavel para uso como acao compacta na interface. * * @param parent componente pai * @param image imagem a ser exibida * @param tooltip tooltip da acao * @param runnable acao a executar no clique * @return label configurado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private Label criarImagemClicavel(Composite parent, Image image, String tooltip, final Runnable runnable) {
        Label label = new Label(parent, SWT.NONE);
        label.setToolTipText(tooltip);

        if (image != null) {
            label.setImage(image);
        } else {
            label.setText("?");
        }

        GridData gd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        gd.widthHint = 32;
        gd.heightHint = 32;
        label.setLayoutData(gd);

        label.setCursor(parent.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
        label.addListener(SWT.MouseDown, event -> {
            if (runnable != null) {
                runnable.run();
            }
        });

        return label;
    }
    /** * Carrega uma imagem do plugin e a registra para descarte posterior. * * @param caminho caminho relativo da imagem no plugin * @return imagem carregada ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private Image carregarImagem(String caminho) {
        try {
            System.out.println("[IMG DEBUG] Tentando carregar: " + caminho + " | plugin=" + Activator.PLUGIN_ID);

            ImageDescriptor descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID, caminho);
            if (descriptor == null) {
                System.out.println("[IMG DEBUG] Descriptor nulo para: " + caminho);
                return null;
            }

            Image image = descriptor.createImage(Display.getDefault());
            if (image == null) {
                System.out.println("[IMG DEBUG] createImage retornou null para: " + caminho);
                return null;
            }

            imagensBotoes.add(image);
            System.out.println("[IMG DEBUG] Imagem carregada com sucesso: " + caminho);
            return image;
        } catch (Exception e) {
            System.out.println("[IMG DEBUG] Falha ao carregar imagem: " + caminho + " -> " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /** * Constroi o conteudo visual principal do painel. * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void createContent() {
        rootContainer = new Composite(this, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 8;
        layout.marginHeight = 8;
        layout.verticalSpacing = 8;
        rootContainer.setLayout(layout);
        rootContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        criarAcoes(rootContainer);
        criarListaPrincipal(rootContainer);
        criarListaEditaveis(rootContainer);
        criarListaReferencias(rootContainer);
        criarListaEstruturalPrimary(rootContainer);
        criarListaEstruturalEditavel(rootContainer);
        criarListaEstruturalReferencial(rootContainer);
        criarStatus(rootContainer);

        controller.refreshView();
        controller.refreshHighlights();
    }

    /** * Cria os botoes principais de acao do painel. * * @param parent componente pai * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void criarAcoes(Composite parent) {
        Group group = new Group(parent, SWT.NONE);
        group.setText("Contexto");
        group.setLayout(new GridLayout(5, false));
        group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        criarImagemClicavel(
                group,
                imgAddPrimary,
                "Adicionar Principal",
                new Runnable() {
                    @Override
                    public void run() {
                        controller.adicionarSelecaoComoPrincipal();
                    }
                }
        );

        criarImagemClicavel(
                group,
                imgAddEditable,
                "Adicionar Editavel",
                new Runnable() {
                    @Override
                    public void run() {
                        controller.adicionarSelecaoComoEditavel();
                    }
                }
        );

        criarImagemClicavel(
                group,
                imgAddReference,
                "Adicionar Referencia",
                new Runnable() {
                    @Override
                    public void run() {
                        controller.adicionarSelecaoComoReferencia();
                    }
                }
        );

        criarImagemClicavel(
                group,
                imgRefresh,
                "Atualizar",
                new Runnable() {
                    @Override
                    public void run() {
                        controller.refreshView();
                        controller.refreshHighlights();
                        adicionarMensagemStatus("Contexto atualizado.");
                    }
                }
        );

        criarImagemClicavel(
                group,
                imgClearAll,
                "Limpar Sessao",
                new Runnable() {
                    @Override
                    public void run() {
                        controller.limparTudo();
                    }
                }
        );
    }

    private void criarListaPrincipal(Composite parent) {
        primaryBlocksGroup = new Group(parent, SWT.NONE);
        primaryBlocksGroup.setText("Bloco Principal");
        primaryBlocksGroup.setLayout(new GridLayout(1, false));
        primaryBlocksGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        primaryBlocksContainer = new Composite(primaryBlocksGroup, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.verticalSpacing = 4;
        primaryBlocksContainer.setLayout(layout);
        primaryBlocksContainer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    }

    private void criarListaEditaveis(Composite parent) {
        editablesGroup = new Group(parent, SWT.NONE);
        editablesGroup.setText("Blocos Editaveis");
        editablesGroup.setLayout(new GridLayout(1, false));
        editablesGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        editablesContainer = new Composite(editablesGroup, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.verticalSpacing = 4;
        editablesContainer.setLayout(layout);
        editablesContainer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    }

    private void criarListaReferencias(Composite parent) {
        referencesGroup = new Group(parent, SWT.NONE);
        referencesGroup.setText("Blocos Referenciais");
        referencesGroup.setLayout(new GridLayout(1, false));
        referencesGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        referencesContainer = new Composite(referencesGroup, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.verticalSpacing = 4;
        referencesContainer.setLayout(layout);
        referencesContainer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    }

    private void criarListaEstruturalPrimary(Composite parent) {
        primaryStructuralGroup = new Group(parent, SWT.NONE);
        primaryStructuralGroup.setText("Contexto Estrutural Principal");
        primaryStructuralGroup.setLayout(new GridLayout(1, false));
        primaryStructuralGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        primaryStructuralContainer = new Composite(primaryStructuralGroup, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.verticalSpacing = 4;
        primaryStructuralContainer.setLayout(layout);
        primaryStructuralContainer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    }

    private void criarListaEstruturalEditavel(Composite parent) {
        editableStructuralGroup = new Group(parent, SWT.NONE);
        editableStructuralGroup.setText("Contexto Estrutural Editavel");
        editableStructuralGroup.setLayout(new GridLayout(1, false));
        editableStructuralGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        editableStructuralContainer = new Composite(editableStructuralGroup, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.verticalSpacing = 4;
        editableStructuralContainer.setLayout(layout);
        editableStructuralContainer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    }

    private void criarListaEstruturalReferencial(Composite parent) {
        referenceStructuralGroup = new Group(parent, SWT.NONE);
        referenceStructuralGroup.setText("Contexto Estrutural Referencial");
        referenceStructuralGroup.setLayout(new GridLayout(1, false));
        referenceStructuralGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        referenceStructuralContainer = new Composite(referenceStructuralGroup, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.verticalSpacing = 4;
        referenceStructuralContainer.setLayout(layout);
        referenceStructuralContainer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    }

    private void criarStatus(Composite parent) {
        statusLabel = new Label(parent, SWT.WRAP);
        statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        statusLabel.setText("Sessao de contextos pronta.");
    }

    @Override
    public void atualizarBlocos(List<NamedCodeBlock> primaryBlocks, List<NamedCodeBlock> editables, List<NamedCodeBlock> references) {
        if (!isAlivePanel()) {
            return;
        }

        rebuildBlockList(primaryBlocksContainer, primaryBlocks);
        rebuildBlockList(editablesContainer, editables);
        rebuildBlockList(referencesContainer, references);

        if (primaryBlocksGroup != null && !primaryBlocksGroup.isDisposed()) {
            primaryBlocksGroup.setText("Bloco Principal (" + sizeOf(primaryBlocks) + ")");
        }

        if (editablesGroup != null && !editablesGroup.isDisposed()) {
            editablesGroup.setText("Blocos Editaveis (" + sizeOf(editables) + ")");
        }

        if (referencesGroup != null && !referencesGroup.isDisposed()) {
            referencesGroup.setText("Blocos Referenciais (" + sizeOf(references) + ")");
        }

        safeLayout(primaryBlocksContainer);
        safeLayout(editablesContainer);
        safeLayout(referencesContainer);
        safeLayout(primaryBlocksGroup);
        safeLayout(editablesGroup);
        safeLayout(referencesGroup);
        safeLayout(rootContainer);
        safeLayout(this);
    }

    @Override
    public void atualizarContextosEstruturais(List<NamedStructuralContext> primaryContexts, List<NamedStructuralContext> editableContexts, List<NamedStructuralContext> referenceContexts) {
        if (!isAlivePanel()) {
            return;
        }

        rebuildStructuralList(primaryStructuralContainer, primaryContexts);
        rebuildStructuralList(editableStructuralContainer, editableContexts);
        rebuildStructuralList(referenceStructuralContainer, referenceContexts);

        if (primaryStructuralGroup != null && !primaryStructuralGroup.isDisposed()) {
            primaryStructuralGroup.setText("Contexto Estrutural Principal (" + sizeOf(primaryContexts) + ")");
        }

        if (editableStructuralGroup != null && !editableStructuralGroup.isDisposed()) {
            editableStructuralGroup.setText("Contexto Estrutural Editavel (" + sizeOf(editableContexts) + ")");
        }

        if (referenceStructuralGroup != null && !referenceStructuralGroup.isDisposed()) {
            referenceStructuralGroup.setText("Contexto Estrutural Referencial (" + sizeOf(referenceContexts) + ")");
        }

        safeLayout(primaryStructuralContainer);
        safeLayout(editableStructuralContainer);
        safeLayout(referenceStructuralContainer);
        safeLayout(primaryStructuralGroup);
        safeLayout(editableStructuralGroup);
        safeLayout(referenceStructuralGroup);
        safeLayout(rootContainer);
        safeLayout(this);
    }

    private void rebuildBlockList(Composite container, List<NamedCodeBlock> blocks) {
        if (container == null || container.isDisposed()) {
            return;
        }

        Control[] children = container.getChildren();
        for (int i = 0; i < children.length; i++) {
            children[i].dispose();
        }

        if (blocks == null || blocks.isEmpty()) {
            Label vazio = new Label(container, SWT.NONE);
            vazio.setText("Nenhum bloco.");
            return;
        }

        for (int i = 0; i < blocks.size(); i++) {
            final NamedCodeBlock block = blocks.get(i);
            if (!isValidBlock(block)) {
                continue;
            }

            Composite row = new Composite(container, SWT.NONE);
            GridLayout layout = new GridLayout(3, false);
            layout.marginWidth = 0;
            layout.marginHeight = 0;
            layout.horizontalSpacing = 6;
            row.setLayout(layout);
            row.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

            Link link = new Link(row, SWT.NONE);
            link.setText("<a>" + escapeLinkText(block.getName()) + "</a> [" + safe(block.getFileName()) + ":" + block.getStartLine() + "-" + block.getEndLine() + "]");
            link.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            link.setToolTipText(buildBlockTooltip(block));
            link.addListener(SWT.Selection, new org.eclipse.swt.widgets.Listener() {
                @Override
                public void handleEvent(org.eclipse.swt.widgets.Event event) {
                    controller.focarBloco(block.getName());
                }
            });

            criarImagemClicavel(
                    row,
                    imgAlias,
                    "Inserir @" + safe(block.getName()) + " na conversa",
                    new Runnable() {
                        @Override
                        public void run() {
                            controller.inserirAliasNaConversa(block.getName());
                        }
                    }
            );

            criarImagemClicavel(
                    row,
                    imgRemove,
                    "Remover bloco " + safe(block.getName()),
                    new Runnable() {
                        @Override
                        public void run() {
                            controller.removerBloco(block.getName());
                        }
                    }
            );
        }

        if (container.getChildren().length == 0) {
            Label vazio = new Label(container, SWT.NONE);
            vazio.setText("Nenhum bloco valido.");
        }
    }

    private void rebuildStructuralList(Composite container, List<NamedStructuralContext> contexts) {
        if (container == null || container.isDisposed()) {
            return;
        }

        Control[] children = container.getChildren();
        for (int i = 0; i < children.length; i++) {
            children[i].dispose();
        }

        if (contexts == null || contexts.isEmpty()) {
            Label vazio = new Label(container, SWT.NONE);
            vazio.setText("Nenhum contexto estrutural.");
            return;
        }

        for (int i = 0; i < contexts.size(); i++) {
            final NamedStructuralContext context = contexts.get(i);
            if (!isValidStructuralContext(context)) {
                continue;
            }

            Composite row = new Composite(container, SWT.NONE);
            GridLayout layout = new GridLayout(3, false);
            layout.marginWidth = 0;
            layout.marginHeight = 0;
            layout.horizontalSpacing = 6;
            row.setLayout(layout);
            row.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

            Link link = new Link(row, SWT.NONE);
            link.setText("<a>" + escapeLinkText(context.getName()) + "</a> " + buildStructuralSuffix(context));
            link.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            link.setToolTipText(buildStructuralTooltip(context));
            link.addListener(SWT.Selection, new org.eclipse.swt.widgets.Listener() {
                @Override
                public void handleEvent(org.eclipse.swt.widgets.Event event) {
                    controller.focarBloco(context.getName());
                }
            });

            criarImagemClicavel(
                    row,
                    imgAlias,
                    "Inserir @" + safe(context.getName()) + " na conversa",
                    new Runnable() {
                        @Override
                        public void run() {
                            controller.inserirAliasNaConversa(context.getName());
                        }
                    }
            );

            criarImagemClicavel(
                    row,
                    imgRemove,
                    "Remover contexto " + safe(context.getName()),
                    new Runnable() {
                        @Override
                        public void run() {
                            controller.removerBloco(context.getName());
                        }
                    }
            );
        }

        if (container.getChildren().length == 0) {
            Label vazio = new Label(container, SWT.NONE);
            vazio.setText("Nenhum contexto estrutural valido.");
        }
    }

    @Override
    public void dispose() {
        if (imagensBotoes != null) {
            for (int i = 0; i < imagensBotoes.size(); i++) {
                Image image = imagensBotoes.get(i);
                if (image != null && !image.isDisposed()) {
                    image.dispose();
                }
            }
            imagensBotoes.clear();
        }

        super.dispose();
    }

    @Override
    public void adicionarMensagemStatus(final String message) {
        getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (statusLabel != null && !statusLabel.isDisposed()) {
                    statusLabel.setText(message != null ? message : "");
                    safeLayout(statusLabel.getParent());
                    safeLayout(rootContainer);
                    safeLayout(NamedBlocksPanel.this);
                }
            }
        });
    }

    public NamedBlocksController getController() {
        return controller;
    }

    public void refreshPanel() {
        if (controller != null) {
            controller.refreshView();
            controller.refreshHighlights();
        }
    }

    @Override
    public boolean setFocus() {
        if (btnAddPrimary != null && !btnAddPrimary.isDisposed()) {
            return btnAddPrimary.setFocus();
        }

        if (rootContainer != null && !rootContainer.isDisposed()) {
            return rootContainer.setFocus();
        }

        return super.setFocus();
    }

    private boolean isAlivePanel() {
        return rootContainer != null && !rootContainer.isDisposed();
    }

    private boolean isValidBlock(NamedCodeBlock block) {
        if (block == null) {
            return false;
        }
        if (isBlank(block.getName())) {
            return false;
        }
        if (isBlank(block.getFileName())) {
            return false;
        }
        if (block.getStartLine() <= 0 || block.getEndLine() <= 0) {
            return false;
        }
        if (block.getEndLine() < block.getStartLine()) {
            return false;
        }
        return true;
    }

    private boolean isValidStructuralContext(NamedStructuralContext context) {
        return context != null && context.isUsable();
    }

    private String buildBlockTooltip(NamedCodeBlock block) {
        StringBuilder sb = new StringBuilder();
        sb.append("Nome: ").append(safe(block.getName())).append("\n");
        sb.append("Arquivo: ").append(safe(block.getFileName())).append("\n");
        sb.append("Linhas: ").append(block.getStartLine()).append("-").append(block.getEndLine()).append("\n");
        sb.append("Preview: ").append(safe(block.getPreview()));
        return sb.toString();
    }

    private String buildStructuralTooltip(NamedStructuralContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Nome: ").append(safe(context.getName())).append("\n");
        sb.append("Role: ").append(context.getRole() != null ? context.getRole().name() : "").append("\n");
        sb.append("Tipo: ").append(context.getType() != null ? context.getType().name() : "").append("\n");

        if (!isBlank(context.getFileName())) {
            sb.append("Nome: ").append(safe(context.getFileName())).append("\n");
        }
        if (!isBlank(context.getPackageName())) {
            sb.append("Package: ").append(safe(context.getPackageName())).append("\n");
        }
        if (!isBlank(context.getRelativePath())) {
            sb.append("Caminho: ").append(safe(context.getRelativePath())).append("\n");
        }
        sb.append("Preview: ").append(safe(context.getPreview()));

        return sb.toString();
    }

    private String buildStructuralSuffix(NamedStructuralContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(context.getRoleMarker()).append(" ");

        if (context.getType() == NamedStructuralContextType.FILE) {
            sb.append("Arquivo");
            if (!isBlank(context.getFileName())) {
                sb.append(" | ").append(context.getFileName());
            }
        } else if (context.getType() == NamedStructuralContextType.PACKAGE) {
            sb.append("Package");
            if (!isBlank(context.getPackageName())) {
                sb.append(" | ").append(context.getPackageName());
            }
        } else {
            sb.append("Pasta");
            if (!isBlank(context.getFileName())) {
                sb.append(" | ").append(context.getFileName());
            } else if (!isBlank(context.getRelativePath())) {
                sb.append(" | ").append(context.getRelativePath());
            }
        }

        sb.append("]");
        return sb.toString();
    }

    private int sizeOf(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private String escapeLinkText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void safeLayout(Composite composite) {
        if (composite != null && !composite.isDisposed()) {
            composite.layout(true, true);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}