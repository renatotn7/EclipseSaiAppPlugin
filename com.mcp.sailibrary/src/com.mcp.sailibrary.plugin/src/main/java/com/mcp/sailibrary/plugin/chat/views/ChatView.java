package com.mcp.sailibrary.plugin.chat.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mcp.sailibrary.plugin.Activator;
import com.mcp.sailibrary.plugin.chat.blocks.views.NamedBlocksPanel;
import com.mcp.sailibrary.plugin.chat.controllers.ChatAiController;

public class ChatView extends ViewPart {

    public static final String ID = "com.mcp.sailibrary.plugin.chat.views.ChatView";

    private StyledText chatHistory;
    private Text inputField;
    private org.eclipse.swt.widgets.Combo inputDepthField;
    private ProgressBar barraProgresso;
   
    private StyledText resumoAlvoText;

    private Button btnSend;
    private Button btnVoltar;
    private Button btnAbandonar;
    private Button btnContexto;
    
    private ToolBar barraLateralComandos;
    private List<Image> imagensToolbar = new ArrayList<Image>();

    private CTabFolder tabFolderPrincipal;
    private NamedBlocksPanel namedBlocksPanel;

    private ChatAiController controller;

    private Color colorToolBg;
    private Color colorIaBg;
    private Color colorUserBg;
    private Color colorLockedBg;
    private Color colorWhite;
    private Color colorBlack;
    private Color colorSystemBg;
    private Color colorStatusBg;
    private Color colorPanelBg;

    public ChatView() {
        super();
        this.controller = new ChatAiController(this);
    }

    @Override
    public void createPartControl(Composite parent) {
        Display display = parent.getDisplay();

        colorToolBg = new Color(display, 70, 130, 180);
        colorIaBg = new Color(display, 20, 50, 85);
        colorUserBg = new Color(display, 235, 235, 235);
        colorLockedBg = new Color(display, 60, 60, 60);
        colorWhite = new Color(display, 255, 255, 255);
        colorBlack = new Color(display, 0, 0, 0);
        colorSystemBg = new Color(display, 90, 90, 90);
        colorStatusBg = new Color(display, 245, 247, 250);
        colorPanelBg = new Color(display, 248, 248, 248);

        Composite container = new Composite(parent, SWT.NONE);
        GridLayout layoutContainer = new GridLayout(2, false);
        layoutContainer.marginWidth = 0;
        layoutContainer.marginHeight = 0;
        layoutContainer.horizontalSpacing = 0;
        layoutContainer.verticalSpacing = 0;
        container.setLayout(layoutContainer);
        container.setBackground(colorWhite);

        Composite areaPrincipal = new Composite(container, SWT.NONE);
        GridLayout layoutPrincipal = new GridLayout(1, false);
        layoutPrincipal.marginWidth = 4;
        layoutPrincipal.marginHeight = 4;
        layoutPrincipal.verticalSpacing = 4;
        areaPrincipal.setLayout(layoutPrincipal);
        areaPrincipal.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        areaPrincipal.setBackground(colorWhite);

        tabFolderPrincipal = new CTabFolder(areaPrincipal, SWT.BORDER);
        tabFolderPrincipal.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        tabFolderPrincipal.setSimple(false);
        tabFolderPrincipal.setUnselectedCloseVisible(false);
        tabFolderPrincipal.setTabHeight(26);

        criarAbaConversa();
        criarAbaContexto();

        tabFolderPrincipal.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (tabFolderPrincipal != null && !tabFolderPrincipal.isDisposed()) {
                    if (tabFolderPrincipal.getSelectionIndex() == 1 && namedBlocksPanel != null && !namedBlocksPanel.isDisposed()) {
                        namedBlocksPanel.refreshPanel();
                    }
                }
            }
        });

        tabFolderPrincipal.setSelection(0);

        criarToolbarLateral(container);
    }

    private void criarAbaConversa() {
        CTabItem abaChat = new CTabItem(tabFolderPrincipal, SWT.NONE);
        abaChat.setText("Conversa");

        Composite conteudoChat = new Composite(tabFolderPrincipal, SWT.NONE);
        GridLayout layoutChat = new GridLayout(1, false);
        layoutChat.marginWidth = 4;
        layoutChat.marginHeight = 4;
        layoutChat.verticalSpacing = 4;
        conteudoChat.setLayout(layoutChat);
        conteudoChat.setBackground(colorWhite);

        criarPainelResumoAlvo(conteudoChat);
        criarHistoricoChat(conteudoChat);
        criarPainelEntrada(conteudoChat);
        criarBarraProgresso(conteudoChat);

        abaChat.setControl(conteudoChat);
    }

    private void criarAbaContexto() {
        CTabItem abaContexto = new CTabItem(tabFolderPrincipal, SWT.NONE);
        abaContexto.setText("Contexto");

        namedBlocksPanel = new NamedBlocksPanel(tabFolderPrincipal, SWT.NONE);
        namedBlocksPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        abaContexto.setControl(namedBlocksPanel);
    }

    private void criarToolbarLateral(Composite parent) {
        Composite lateral = new Composite(parent, SWT.NONE);
        GridLayout layoutLateral = new GridLayout(1, false);
        layoutLateral.marginWidth = 2;
        layoutLateral.marginHeight = 4;
        layoutLateral.verticalSpacing = 4;
        lateral.setLayout(layoutLateral);

        GridData gdLateral = new GridData(SWT.RIGHT, SWT.FILL, false, true);
        gdLateral.widthHint = 44;
        lateral.setLayoutData(gdLateral);
        lateral.setBackground(colorPanelBg);

        Composite espacoExpansivel = new Composite(lateral, SWT.NONE);
        espacoExpansivel.setBackground(colorPanelBg);
        espacoExpansivel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        barraLateralComandos = new ToolBar(lateral, SWT.FLAT | SWT.VERTICAL);
        barraLateralComandos.setBackground(colorPanelBg);
        barraLateralComandos.setLayoutData(new GridData(SWT.CENTER, SWT.END, true, false));

        criarItemToolbar(
            "Explicar o comportamento, a estrutura e a finalidade do trecho selecionado",
            "Explique o comportamento, a estrutura e a finalidade deste trecho de codigo. Destaque responsabilidades, fluxo principal, dependencias relevantes e possiveis pontos de atencao.",
            "icons/icon_explicar@2x.png"
        );

        criarItemToolbar(
            "Sugerir melhorias tecnicas com foco em clareza, seguranca e manutencao",
            "Analise este trecho e proponha melhorias tecnicas com foco em clareza, seguranca, legibilidade, manutencao e prevencao de regressao. Preserve o comportamento existente sempre que possivel.",
            "icons/icon_melhorar@2x.png"
        );

        criarItemToolbar(
            "Identificar chamadores, contexto de uso e impacto de alteracao",
            "Identifique quem chama este trecho ou metodo, em que contexto ele e utilizado e qual o impacto potencial de uma alteracao neste ponto.",
            "icons/icon_chamadores@2x.png"
        );

        criarItemToolbar(
            "Localizar implementacao concreta, classes relacionadas ou fluxo delegado",
            "Localize a implementacao concreta, classes relacionadas ou o fluxo real delegado por este trecho, especialmente quando houver interface, abstracao, heranca ou indirecao.",
            "icons/icon_implementar@2x.png"
        );

        criarItemToolbar(
            "Mapear queries, acessos a dados e chamadas indiretas",
            "Mapeie queries, acessos a banco, chamadas indiretas, delegacoes, DAOs, repositories, services ou qualquer operacao de persistencia relacionada a este trecho.",
            "icons/icon_queries@2x.png"
        );

        criarItemToolbar(
            "Gerar comentario tecnico curto, claro e util",
            "Gere um comentario tecnico curto, claro e util para este trecho, descrevendo sua finalidade e regra principal sem repetir o codigo.",
            "icons/icon_comentar@2x.png"
        );
    }

    private void criarItemToolbar(final String tooltip, final String comando, String caminhoIcone) {
        ToolItem item = new ToolItem(barraLateralComandos, SWT.PUSH);

        Image imagem = carregarImagemToolbar(caminhoIcone, 24, 24);
        if (imagem != null) {
            item.setImage(imagem);
            imagensToolbar.add(imagem);
        } else {
            item.setText("?");
        }

        item.setToolTipText(tooltip);

        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                anexarComandoNaEntrada(comando);
            }
        });
    }

    private void anexarComandoNaEntrada(String comando) {
        if (inputField != null && !inputField.isDisposed()) {
            String textoAtual = inputField.getText();
            String textoNovo = comando != null ? comando.trim() : "";

            if (textoNovo.length() == 0) {
                inputField.setFocus();
                return;
            }

            String bloco = "Instrucao complementar:" + System.lineSeparator() + textoNovo;

            if (textoAtual == null || textoAtual.trim().length() == 0) {
                inputField.setText(bloco);
            } else {
                String separador = textoAtual.endsWith("\n") || textoAtual.endsWith("\r\n")
                        ? ""
                        : System.lineSeparator() + System.lineSeparator();
                inputField.setText(textoAtual + separador + bloco);
            }

            inputField.setFocus();
            inputField.setSelection(inputField.getText().length());
        }
    }

    private Image carregarImagemToolbar(String caminho, int largura, int altura) {
        try {
            ImageDescriptor descriptor = org.eclipse.ui.plugin.AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID, caminho);
            if (descriptor != null) {
                Image original = descriptor.createImage();
                if (original != null) {
                    org.eclipse.swt.graphics.ImageData dataOriginal = original.getImageData();
                    org.eclipse.swt.graphics.ImageData dataEscalada = dataOriginal.scaledTo(largura, altura);
                    Image escalada = new Image(Display.getDefault(), dataEscalada);
                    original.dispose();
                    return escalada;
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    private void criarPainelResumoAlvo(Composite parent) {
        Group grupoResumo = new Group(parent, SWT.NONE);
        grupoResumo.setText("Alvo atual");
        grupoResumo.setLayout(new GridLayout(1, false));
        grupoResumo.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        grupoResumo.setBackground(colorPanelBg);
        grupoResumo.setForeground(colorBlack);

        resumoAlvoText = new StyledText(grupoResumo, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        GridData gdResumo = new GridData(SWT.FILL, SWT.FILL, true, false);
        gdResumo.heightHint = 42;
        resumoAlvoText.setLayoutData(gdResumo);
        resumoAlvoText.setEditable(false);
        resumoAlvoText.setCaret(null);
        resumoAlvoText.setBackground(colorWhite);
        resumoAlvoText.setForeground(colorBlack);
        resumoAlvoText.setText("Nenhum alvo ativo");
    }

 
    private void criarHistoricoChat(Composite parent) {
        chatHistory = new StyledText(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
        GridData gdHistory = new GridData(SWT.FILL, SWT.FILL, true, true);
        gdHistory.heightHint = 360;
        chatHistory.setLayoutData(gdHistory);
        chatHistory.setBackground(colorWhite);
        chatHistory.setMargins(8, 8, 8, 8);
        chatHistory.setCaret(null);
        chatHistory.setEditable(false);
    }

    private void criarPainelEntrada(Composite parent) {
        Group grupoEntrada = new Group(parent, SWT.NONE);
        grupoEntrada.setText("Comando");
        grupoEntrada.setLayout(new GridLayout(1, false));
        grupoEntrada.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        grupoEntrada.setBackground(colorPanelBg);
        grupoEntrada.setForeground(colorBlack);

        Composite topoEntrada = new Composite(grupoEntrada, SWT.NONE);
        topoEntrada.setLayout(new GridLayout(3, false));
        topoEntrada.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        topoEntrada.setBackground(colorPanelBg);

        Label lblDepth = new Label(topoEntrada, SWT.NONE);
        lblDepth.setText("Profundidade:");
        lblDepth.setBackground(colorPanelBg);
        lblDepth.setForeground(colorBlack);

        inputDepthField = new org.eclipse.swt.widgets.Combo(topoEntrada, SWT.DROP_DOWN | SWT.READ_ONLY);
        inputDepthField.setItems(new String[] { "1", "2", "3", "4", "5" });
        inputDepthField.select(2);
        inputDepthField.setToolTipText("1 = metodo atual | 2 = chamadas diretas | 3 = fluxo ampliado | 4 e 5 = analise pesada");
        GridData gdDepth = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        gdDepth.widthHint = 70;
        inputDepthField.setLayoutData(gdDepth);

        Label lblDicaEnter = new Label(topoEntrada, SWT.NONE);
        lblDicaEnter.setText("Enter envia | Shift+Enter quebra linha");
        lblDicaEnter.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        lblDicaEnter.setBackground(colorPanelBg);
        lblDicaEnter.setForeground(colorBlack);

        inputField = new Text(grupoEntrada, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        GridData gdInput = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gdInput.heightHint = 78;
        inputField.setLayoutData(gdInput);
        inputField.setMessage("Ex: explique este metodo | melhore com foco em seguranca | descubra quem chama este metodo");
        inputField.setBackground(colorWhite);
        inputField.setForeground(colorBlack);

        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.character == SWT.CR || e.character == SWT.LF) {
                    if ((e.stateMask & SWT.SHIFT) != 0) {
                        return;
                    }
                    e.doit = false;
                    processarEntrada();
                }
            }
        });

        Composite botoesAcoes = new Composite(grupoEntrada, SWT.NONE);
        botoesAcoes.setLayout(new GridLayout(4, false));
        botoesAcoes.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        botoesAcoes.setBackground(colorPanelBg);

        btnSend = new Button(botoesAcoes, SWT.PUSH);
        btnSend.setText("Enviar");
        btnSend.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                processarEntrada();
            }
        });

        btnVoltar = new Button(botoesAcoes, SWT.PUSH);
        btnVoltar.setText("Voltar ao arquivo");
        btnVoltar.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                if (controller != null) {
                    controller.voltarAoArquivo();
                }
            }
        });

        btnContexto = new Button(botoesAcoes, SWT.PUSH);
        btnContexto.setText("Contexto");
        btnContexto.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                abrirAbaContexto();
            }
        });

        btnAbandonar = new Button(botoesAcoes, SWT.PUSH);
        btnAbandonar.setText("Cancelar analise");
        btnAbandonar.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                if (controller != null) {
                    controller.abandonarMissao();
                }
            }
        });
    }

    private void criarBarraProgresso(Composite parent) {
        barraProgresso = new ProgressBar(parent, SWT.INDETERMINATE);
        GridData gdProgresso = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gdProgresso.exclude = true;
        barraProgresso.setLayoutData(gdProgresso);
        barraProgresso.setVisible(false);
    }

    public void alternarCarregamento(final boolean ativo) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (barraProgresso != null && !barraProgresso.isDisposed()) {
                    barraProgresso.setVisible(ativo);

                    GridData data = (GridData) barraProgresso.getLayoutData();
                    data.exclude = !ativo;
                    barraProgresso.getParent().layout(true, true);
                }

                if (inputField != null && !inputField.isDisposed()) {
                    inputField.setEnabled(!ativo);
                    if (ativo) {
                        inputField.setBackground(colorLockedBg);
                        inputField.setForeground(colorWhite);
                    } else {
                        inputField.setBackground(colorWhite);
                        inputField.setForeground(colorBlack);
                    }
                }

                if (inputDepthField != null && !inputDepthField.isDisposed()) {
                    inputDepthField.setEnabled(!ativo);
                }

                if (btnSend != null && !btnSend.isDisposed()) {
                    btnSend.setEnabled(!ativo);
                }

                if (btnVoltar != null && !btnVoltar.isDisposed()) {
                    btnVoltar.setEnabled(!ativo);
                }

                if (barraLateralComandos != null && !barraLateralComandos.isDisposed()) {
                    ToolItem[] itens = barraLateralComandos.getItems();
                    for (int i = 0; i < itens.length; i++) {
                        itens[i].setEnabled(!ativo);
                    }
                }

                if (btnAbandonar != null && !btnAbandonar.isDisposed()) {
                    btnAbandonar.setEnabled(true);
                }

                if (namedBlocksPanel != null && !namedBlocksPanel.isDisposed()) {
                    namedBlocksPanel.setEnabled(!ativo);
                }
            }
        });
    }

    public void atualizarResumoAlvo(final String resumo) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (resumoAlvoText != null && !resumoAlvoText.isDisposed()) {
                    resumoAlvoText.setText(resumo != null ? resumo : "Nenhum alvo ativo");
                }
            }
        });
    }

    public void atualizarStatusOperacional(final String status) {
        // Metodo preservado para compatibilidade com o controlador.
    }

    public void adicionarMensagemAssincrona(final String remetente, final String mensagem) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                adicionarMensagem(remetente, mensagem);
            }
        });
    }

    @Override
    public void setFocus() {
        if (tabFolderPrincipal != null && !tabFolderPrincipal.isDisposed()) {
            int index = tabFolderPrincipal.getSelectionIndex();
            if (index == 1 && namedBlocksPanel != null && !namedBlocksPanel.isDisposed()) {
                namedBlocksPanel.setFocus();
                return;
            }
        }

        if (inputField != null && !inputField.isDisposed()) {
            inputField.setFocus();
        }
    }

    public void setContext(String selectedCode, String fullFileText, String apiKey, IDocument document, ITextSelection selection, ICompilationUnit compUnit, ITextEditor textEditor) {
        if (this.controller != null) {
            this.controller.setContext(selectedCode, fullFileText, apiKey, document, selection, compUnit, textEditor);
        }
    }

    public void ativarEditor(IEditorPart editor) {
        try {
            getSite().getPage().activate(editor);
        } catch (Exception e) {
        }
    }

    private void processarEntrada() {
        String instrucao = inputField.getText();
        if (instrucao == null || instrucao.trim().length() == 0) {
            return;
        }

        adicionarMensagem("Usuario", instrucao);
        inputField.setText("");

        String profundidadeStr = inputDepthField.getText();
        int profundidadeMax = 0;
        try {
            profundidadeMax = Integer.parseInt(profundidadeStr);
        } catch (Exception e) {
            profundidadeMax = 0;
        }

        if (this.controller != null) {
            this.controller.executarMissaoIA(instrucao, profundidadeMax, instrucao);
        }
    }

    public void adicionarMensagem(String remetente, String mensagem) {
        if (chatHistory == null || chatHistory.isDisposed()) {
            return;
        }

        String remetenteLower = remetente != null ? remetente.toLowerCase() : "";
        
        if (remetenteLower.contains("debug")) {
            return;
        }

        int startChar = chatHistory.getCharCount();
        int startLine = chatHistory.getLineCount() - 1;

        String header = "[" + remetente + "] ";
        String body = mensagem + System.lineSeparator();
        String fullMessage = header + body;

        chatHistory.append(fullMessage);

        int endMessageLine = chatHistory.getLineCount() - 1;

        Color bgColor;
        Color fgColor;

        if (remetenteLower.contains("ferramenta")) {
            bgColor = colorToolBg;
            fgColor = colorWhite;
        } else if (remetenteLower.contains("sistema")) {
            bgColor = colorSystemBg;
            fgColor = colorWhite;
        } else if (remetenteLower.contains("ia") || remetenteLower.contains("comando central")) {
            bgColor = colorIaBg;
            fgColor = colorWhite;
        } else {
            bgColor = colorUserBg;
            fgColor = colorBlack;
        }

        chatHistory.setLineBackground(startLine, (endMessageLine - startLine), bgColor);

        StyleRange srHeader = new StyleRange();
        srHeader.start = startChar;
        srHeader.length = header.length();
        srHeader.fontStyle = SWT.BOLD;
        srHeader.foreground = fgColor;

        StyleRange srBody = new StyleRange();
        srBody.start = startChar + header.length();
        srBody.length = body.length();
        srBody.fontStyle = SWT.NORMAL;
        srBody.foreground = fgColor;

        chatHistory.setStyleRange(srHeader);
        chatHistory.setStyleRange(srBody);

        int separatorLine = chatHistory.getLineCount() - 1;
        chatHistory.append(System.lineSeparator());
        chatHistory.setLineBackground(separatorLine, 1, colorWhite);

        chatHistory.setTopIndex(chatHistory.getLineCount() - 1);
    }

    public void limparHistorico() {
        if (chatHistory != null && !chatHistory.isDisposed()) {
            chatHistory.setText("");
        }
    }

    public ChatAiController getController() {
        return this.controller;
    }

    @Override
    public void dispose() {
        if (colorToolBg != null && !colorToolBg.isDisposed()) colorToolBg.dispose();
        if (colorIaBg != null && !colorIaBg.isDisposed()) colorIaBg.dispose();
        if (colorUserBg != null && !colorUserBg.isDisposed()) colorUserBg.dispose();
        if (colorLockedBg != null && !colorLockedBg.isDisposed()) colorLockedBg.dispose();
        if (colorWhite != null && !colorWhite.isDisposed()) colorWhite.dispose();
        if (colorBlack != null && !colorBlack.isDisposed()) colorBlack.dispose();
        if (colorSystemBg != null && !colorSystemBg.isDisposed()) colorSystemBg.dispose();
        if (colorStatusBg != null && !colorStatusBg.isDisposed()) colorStatusBg.dispose();
        if (colorPanelBg != null && !colorPanelBg.isDisposed()) colorPanelBg.dispose();

        if (imagensToolbar != null) {
            for (int i = 0; i < imagensToolbar.size(); i++) {
                Image imagem = imagensToolbar.get(i);
                if (imagem != null && !imagem.isDisposed()) {
                    imagem.dispose();
                }
            }
            imagensToolbar.clear();
        }

        super.dispose();
    }

    public NamedBlocksPanel getNamedBlocksPanel() {
        return namedBlocksPanel;
    }

    public void abrirAbaContexto() {
        if (tabFolderPrincipal != null && !tabFolderPrincipal.isDisposed()) {
            tabFolderPrincipal.setSelection(1);
            if (namedBlocksPanel != null && !namedBlocksPanel.isDisposed()) {
                namedBlocksPanel.refreshPanel();
            }
        }
    }
    /** * Solicita ao controlador a sincronizacao do alvo atual a partir do PRIMARY * global da sessao. * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void sincronizarAlvoPrimarioGlobal() {
        if (controller != null) {
            controller.sincronizarAlvoPrimarioGlobal();
        }
    }
}