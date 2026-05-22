package com.mcp.sailibrary.plugin.chat.blocks.views;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;

import com.mcp.sailibrary.plugin.chat.blocks.controllers.NamedBlocksController;
import com.mcp.sailibrary.plugin.chat.blocks.model.NamedCodeBlock;
import com.mcp.sailibrary.plugin.chat.context.model.NamedStructuralContext;
import com.mcp.sailibrary.plugin.chat.context.model.NamedStructuralContextType;

/* yaml_header: version: "1.0" purpose: "Exibir e gerenciar contexto hibrido da sessao: blocos textuais e contextos estruturais." libraries: - org.eclipse.swt.widgets.Composite: runtime - org.eclipse.swt.widgets.Link: runtime - java.util.List: runtime */
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

    public NamedBlocksPanel(Composite parent, int style) {
        super(parent, style);

        GridLayout selfLayout = new GridLayout(1, false);
        selfLayout.marginWidth = 0;
        selfLayout.marginHeight = 0;
        selfLayout.verticalSpacing = 0;
        setLayout(selfLayout);

        this.controller = new NamedBlocksController(this);
        createContent();
    }

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

    private void criarAcoes(Composite parent) {
        Group group = new Group(parent, SWT.NONE);
        group.setText("Contexto");
        group.setLayout(new GridLayout(5, false));
        group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        btnAddPrimary = new Button(group, SWT.PUSH);
        btnAddPrimary.setText("Adicionar Principal");
        btnAddPrimary.setToolTipText("Adiciona a selecao atual do editor como bloco principal da analise.");
        btnAddPrimary.addListener(SWT.Selection, event -> controller.adicionarSelecaoComoPrincipal());

        btnAddEditable = new Button(group, SWT.PUSH);
        btnAddEditable.setText("Adicionar Editavel");
        btnAddEditable.setToolTipText("Adiciona a selecao atual do editor como bloco editavel.");
        btnAddEditable.addListener(SWT.Selection, event -> controller.adicionarSelecaoComoEditavel());

        btnAddReference = new Button(group, SWT.PUSH);
        btnAddReference.setText("Adicionar Referencia");
        btnAddReference.setToolTipText("Adiciona a selecao atual do editor como bloco referencial.");
        btnAddReference.addListener(SWT.Selection, event -> controller.adicionarSelecaoComoReferencia());

        btnRefresh = new Button(group, SWT.PUSH);
        btnRefresh.setText("Atualizar");
        btnRefresh.setToolTipText("Atualiza a lista de contextos e reaplica os destaques no editor.");
        btnRefresh.addListener(SWT.Selection, event -> {
            controller.refreshView();
            controller.refreshHighlights();
            adicionarMensagemStatus("Contexto atualizado.");
        });

        btnClear = new Button(group, SWT.PUSH);
        btnClear.setText("Limpar Sessao");
        btnClear.setToolTipText("Remove todos os contextos da sessao atual.");
        btnClear.addListener(SWT.Selection, event -> controller.limparTudo());
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
            GridLayout layout = new GridLayout(2, false);
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

            Button remove = new Button(row, SWT.PUSH);
            remove.setText("Remover");
            remove.setToolTipText("Remover bloco " + safe(block.getName()));
            remove.addListener(SWT.Selection, new org.eclipse.swt.widgets.Listener() {
                @Override
                public void handleEvent(org.eclipse.swt.widgets.Event event) {
                    controller.removerBloco(block.getName());
                }
            });
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
            GridLayout layout = new GridLayout(2, false);
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

            Button remove = new Button(row, SWT.PUSH);
            remove.setText("Remover");
            remove.setToolTipText("Remover contexto " + safe(context.getName()));
            remove.addListener(SWT.Selection, new org.eclipse.swt.widgets.Listener() {
                @Override
                public void handleEvent(org.eclipse.swt.widgets.Event event) {
                    controller.removerBloco(context.getName());
                }
            });
        }

        if (container.getChildren().length == 0) {
            Label vazio = new Label(container, SWT.NONE);
            vazio.setText("Nenhum contexto estrutural valido.");
        }
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