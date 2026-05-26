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
import com.mcp.sailibrary.plugin.chat.controllers.ChatViewActivityController;
import com.mcp.sailibrary.plugin.chat.controllers.ChatViewConfigurationController;
import com.mcp.sailibrary.plugin.chat.settings.ChatRuntimeSettings;
import com.mcp.sailibrary.plugin.chat.views.tabs.ChatActivityPanel;
import com.mcp.sailibrary.plugin.chat.views.tabs.ChatConfigurationPanel;

/** * View principal do chat de engenharia. * * <p>Responsabilidades desta classe: * <ul> * <li>compor as abas visuais</li> * <li>encaminhar eventos do usuario ao controller principal</li> * <li>manter historico visual da conversa</li> * <li>apresentar configuracao e atividade do agente</li> * </ul> * </p> * * <p>Persistencia de configuracao e trilha de atividade ficam fora desta classe, * em controllers dedicados, para reduzir acoplamento com logica nao visual.</p> * * @author Renato Tomaz Nati * @since 2026-05-24 */
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
    private ChatConfigurationPanel chatConfigurationTab;
    private ChatActivityPanel chatActivityTab;

    private ChatAiController controller;
    private ChatViewConfigurationController chatConfigurationController;
    private ChatViewActivityController chatActivityController;
    private ChatRuntimeSettings chatRuntimeSettings;

    private Color colorToolBg;
    private Color colorIaBg;
    private Color colorUserBg;
    private Color colorLockedBg;
    private Color colorWhite;
    private Color colorBlack;
    private Color colorSystemBg;
    private Color colorStatusBg;
    private Color colorPanelBg;

    /** * Inicializa a view principal do chat. * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public ChatView() {
        super();
        this.chatConfigurationController = new ChatViewConfigurationController();
        this.chatActivityController = new ChatViewActivityController();
        this.controller = new ChatAiController(this);
    }

    /** * Constroi a interface principal da view. * * @param parent componente pai * * @author Renato Tomaz Nati * @since 2026-05-24 */
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
        criarAbaConfiguracao();
        criarAbaAtividade();

        tabFolderPrincipal.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (tabFolderPrincipal == null || tabFolderPrincipal.isDisposed()) {
                    return;
                }

                CTabItem itemSelecionado = tabFolderPrincipal.getSelection();
                if (itemSelecionado == null) {
                    return;
                }

                String textoAba = itemSelecionado.getText();
                if ("Contexto".equals(textoAba) && namedBlocksPanel != null && !namedBlocksPanel.isDisposed()) {
                    namedBlocksPanel.refreshPanel();
                }
            }
        });

        tabFolderPrincipal.setSelection(0);

        criarToolbarLateral(container);
        carregarConfiguracaoPersistida();
        aplicarConfiguracaoAtualNoController();
    }

    /** * Cria a aba principal de conversa. * * @author Renato Tomaz Nati * @since 2026-05-24 */
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

    /** * Cria a aba de contexto. * * @author Renato Tomaz Nati * @since 2026-05-24 */
    private void criarAbaContexto() {
        CTabItem abaContexto = new CTabItem(tabFolderPrincipal, SWT.NONE);
        abaContexto.setText("Contexto");

        namedBlocksPanel = new NamedBlocksPanel(tabFolderPrincipal, SWT.NONE, this);
        namedBlocksPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        abaContexto.setControl(namedBlocksPanel);
    }

    /** * Cria a aba de configuracao. * * @author Renato Tomaz Nati * @since 2026-05-24 */
    private void criarAbaConfiguracao() {
        CTabItem abaConfiguracao = new CTabItem(tabFolderPrincipal, SWT.NONE);
        abaConfiguracao.setText("Configuracao");

        chatConfigurationTab = new ChatConfigurationPanel(
                tabFolderPrincipal,
                SWT.NONE,
                chatConfigurationController
        );

        chatConfigurationTab.getBtnSalvarConfiguracao().addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                salvarConfiguracaoPersistida();
                aplicarConfiguracaoAtualNoController();
                adicionarMensagem("Sistema", "Configuracao salva. Se os modelos necessarios nao estiverem acessiveis no MCP, o sistema podera pedir nomes explicitos ao usuario.");
            }
        });

        abaConfiguracao.setControl(chatConfigurationTab);
    }

    /** * Cria a aba de atividade do agente. * * @author Renato Tomaz Nati * @since 2026-05-24 */
    private void criarAbaAtividade() {
        CTabItem abaAtividade = new CTabItem(tabFolderPrincipal, SWT.NONE);
        abaAtividade.setText("Atividade");

        chatActivityTab = new ChatActivityPanel(
                tabFolderPrincipal,
                SWT.NONE,
                chatActivityController
        );

        abaAtividade.setControl(chatActivityTab);
    }

    /** * Carrega a configuracao persistida ou aplica defaults seguros quando o * arquivo ainda nao existir. * * @author Renato Tomaz Nati * @since 2026-05-24 */
    private void carregarConfiguracaoPersistida() {
        chatRuntimeSettings = chatConfigurationController.carregarConfiguracao();

        if (chatRuntimeSettings == null) {
            chatRuntimeSettings = new ChatRuntimeSettings();
            chatRuntimeSettings.setDebugAtivo(false);
            chatRuntimeSettings.setModoExecucao(ChatRuntimeSettings.MODO_EXECUCAO_MONO);
            chatRuntimeSettings.setPerfilRaciocinio(ChatRuntimeSettings.PERFIL_PADRAO);
        }

        if (chatConfigurationTab != null && !chatConfigurationTab.isDisposed()) {
            chatConfigurationTab.aplicarConfiguracao(chatRuntimeSettings);
        }
    }

    /** * Persiste a configuracao atual capturada da aba de configuracao. * * @author Renato Tomaz Nati * @since 2026-05-24 */
    private void salvarConfiguracaoPersistida() {
        if (chatConfigurationTab == null || chatConfigurationTab.isDisposed()) {
            return;
        }

        ChatRuntimeSettings settings = chatConfigurationTab.extrairConfiguracao();
        if (settings == null) {
            return;
        }

        chatConfigurationController.salvarConfiguracao(settings);
        chatRuntimeSettings = settings;
    }

    /** * Aplica no controller principal os valores de configuracao ativos. * * @author Renato Tomaz Nati * @since 2026-05-24 */
    private void aplicarConfiguracaoAtualNoController() {
        if (controller == null) {
            return;
        }

        controller.setDebugAtivo(isDebugConfigurado());
    }

    /** * Retorna se o modo debug esta ativo. * * @return true quando o debug estiver ativo * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public boolean isDebugConfigurado() {
        if (chatRuntimeSettings != null) {
            return chatRuntimeSettings.isDebugAtivo();
        }

        if (chatConfigurationTab != null && !chatConfigurationTab.isDisposed()) {
            ChatRuntimeSettings settings = chatConfigurationTab.extrairConfiguracao();
            if (settings != null) {
                return settings.isDebugAtivo();
            }
        }

        return false;
    }

    /** * Retorna o modo de execucao configurado. * * @return modo de execucao atual * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public String getModoExecucaoConfigurado() {
        if (chatRuntimeSettings != null
                && chatRuntimeSettings.getModoExecucao() != null
                && chatRuntimeSettings.getModoExecucao().trim().length() > 0) {
            return chatRuntimeSettings.getModoExecucao();
        }

        if (chatConfigurationTab != null && !chatConfigurationTab.isDisposed()) {
            ChatRuntimeSettings settings = chatConfigurationTab.extrairConfiguracao();
            if (settings != null
                    && settings.getModoExecucao() != null
                    && settings.getModoExecucao().trim().length() > 0) {
                return settings.getModoExecucao();
            }
        }

        return ChatRuntimeSettings.MODO_EXECUCAO_MONO;
    }

    /** * Retorna o perfil de raciocinio configurado. * * @return perfil de raciocinio atual * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public String getPerfilRaciocinioConfigurado() {
        if (chatRuntimeSettings != null
                && chatRuntimeSettings.getPerfilRaciocinio() != null
                && chatRuntimeSettings.getPerfilRaciocinio().trim().length() > 0) {
            return chatRuntimeSettings.getPerfilRaciocinio();
        }

        if (chatConfigurationTab != null && !chatConfigurationTab.isDisposed()) {
            ChatRuntimeSettings settings = chatConfigurationTab.extrairConfiguracao();
            if (settings != null
                    && settings.getPerfilRaciocinio() != null
                    && settings.getPerfilRaciocinio().trim().length() > 0) {
                return settings.getPerfilRaciocinio();
            }
        }

        return ChatRuntimeSettings.PERFIL_PADRAO;
    }

    /** * Registra uma nova linha de atividade do agente. * * @param fase fase logica * @param detalhe detalhe da atividade * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public void registrarAtividadeAgente(final String fase, final String detalhe) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (chatActivityTab == null || chatActivityTab.isDisposed()) {
                    return;
                }

                chatActivityTab.registrarAtividade(fase, detalhe);
            }
        });
    }

    /** * Limpa a trilha visual de atividade do agente. * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public void limparAtividadesAgente() {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (chatActivityTab == null || chatActivityTab.isDisposed()) {
                    return;
                }

                chatActivityTab.limparAtividade();
            }
        });
    }

    /** * Abre a aba de atividade. * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public void abrirAbaAtividade() {
        if (tabFolderPrincipal == null || tabFolderPrincipal.isDisposed()) {
            return;
        }

        CTabItem[] itens = tabFolderPrincipal.getItems();
        for (int i = 0; i < itens.length; i++) {
            if ("Atividade".equals(itens[i].getText())) {
                tabFolderPrincipal.setSelection(itens[i]);
                return;
            }
        }
    }

    /** * Abre a aba de configuracao. * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public void abrirAbaConfiguracao() {
        if (tabFolderPrincipal == null || tabFolderPrincipal.isDisposed()) {
            return;
        }

        CTabItem[] itens = tabFolderPrincipal.getItems();
        for (int i = 0; i < itens.length; i++) {
            if ("Configuracao".equals(itens[i].getText())) {
                tabFolderPrincipal.setSelection(itens[i]);
                return;
            }
        }
    }

    /** * Informa ao usuario que os modelos necessarios nao puderam ser alcancados * automaticamente. * * @param modelosNecessarios descricao dos modelos esperados * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public void solicitarEscolhaExplicitaDeModelos(final String modelosNecessarios) {
        StringBuilder mensagem = new StringBuilder();
        mensagem.append("Nao foi possivel alcancar automaticamente os modelos necessarios no MCP.").append(System.lineSeparator());
        mensagem.append("Abra a aba Configuracao e revise a estrategia.").append(System.lineSeparator());
        mensagem.append("Se for necessario, informe explicitamente os nomes dos modelos desejados.").append(System.lineSeparator());
        mensagem.append("Modelos esperados: ").append(modelosNecessarios != null ? modelosNecessarios : "nao informado");

        adicionarMensagem("Sistema", mensagem.toString());
        abrirAbaConfiguracao();
    }

    /** * Cria a barra lateral de comandos rapidos. * * @param parent componente pai * * @author Renato Tomaz Nati * @since 2026-05-24 */
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

    /** * Cria um item da toolbar lateral. * * @param tooltip descricao do item * @param comando comando a anexar na entrada * @param caminhoIcone caminho do icone * * @author Renato Tomaz Nati * @since 2026-05-24 */
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

    /** * Anexa comando pronto no campo de entrada. * * @param comando texto do comando * * @author Renato Tomaz Nati * @since 2026-05-24 */
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

    /** * Carrega e escala icone da toolbar. * * @param caminho caminho do icone * @param largura largura desejada * @param altura altura desejada * @return imagem carregada ou null * * @author Renato Tomaz Nati * @since 2026-05-24 */
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

    /** * Cria o painel de resumo do alvo atual. * * @param parent componente pai * * @author Renato Tomaz Nati * @since 2026-05-24 */
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

    /** * Cria o historico visual da conversa. * * @param parent componente pai * * @author Renato Tomaz Nati * @since 2026-05-24 */
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

    /** * Cria o painel de entrada de comando. * * @param parent componente pai * * @author Renato Tomaz Nati * @since 2026-05-24 */
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

    /** * Cria a barra de progresso. * * @param parent componente pai * * @author Renato Tomaz Nati * @since 2026-05-24 */
    private void criarBarraProgresso(Composite parent) {
        barraProgresso = new ProgressBar(parent, SWT.INDETERMINATE);
        GridData gdProgresso = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gdProgresso.exclude = true;
        barraProgresso.setLayoutData(gdProgresso);
        barraProgresso.setVisible(false);
    }

    /** * Alterna o estado visual de carregamento da view. * * @param ativo true quando a execucao estiver ativa * * @author Renato Tomaz Nati * @since 2026-05-24 */
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

    /** * Atualiza o resumo visual do alvo atual. * * @param resumo texto do resumo * * @author Renato Tomaz Nati * @since 2026-05-24 */
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

    /** * Metodo preservado para compatibilidade com o controller principal. * * @param status status atual * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public void atualizarStatusOperacional(final String status) {
        // Metodo preservado para compatibilidade com o controlador.
    }

    /** * Adiciona mensagem na conversa de forma assincrona. * * @param remetente remetente logico * @param mensagem conteudo textual * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public void adicionarMensagemAssincrona(final String remetente, final String mensagem) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                adicionarMensagem(remetente, mensagem);
            }
        });
    }

    /** * Define foco no componente mais apropriado da view. * * @author Renato Tomaz Nati * @since 2026-05-24 */
    @Override
    public void setFocus() {
        if (tabFolderPrincipal != null && !tabFolderPrincipal.isDisposed()) {
            CTabItem itemSelecionado = tabFolderPrincipal.getSelection();
            if (itemSelecionado != null && "Contexto".equals(itemSelecionado.getText())
                    && namedBlocksPanel != null && !namedBlocksPanel.isDisposed()) {
                namedBlocksPanel.setFocus();
                return;
            }
        }

        if (inputField != null && !inputField.isDisposed()) {
            inputField.setFocus();
        }
    }

    /** * Encaminha a atualizacao de contexto ao controller principal. * * @param selectedCode trecho textual selecionado * @param fullFileText conteudo integral do arquivo * @param apiKey chave MCP * @param document documento atual * @param selection selecao atual * @param compUnit compilation unit atual * @param textEditor editor atual * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public void setContext(String selectedCode, String fullFileText, String apiKey, IDocument document, ITextSelection selection, ICompilationUnit compUnit, ITextEditor textEditor) {
        if (this.controller != null) {
            this.controller.setContext(selectedCode, fullFileText, apiKey, document, selection, compUnit, textEditor);
        }
    }

    /** * Ativa o editor informado. * * @param editor editor alvo * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public void ativarEditor(IEditorPart editor) {
        try {
            getSite().getPage().activate(editor);
        } catch (Exception e) {
        }
    }

    /** * Processa o comando atual digitado pelo usuario. * * @author Renato Tomaz Nati * @since 2026-05-24 */
    private void processarEntrada() {
        String instrucao = inputField.getText();
        if (instrucao == null || instrucao.trim().length() == 0) {
            return;
        }

        adicionarMensagem("Usuario", instrucao);
        inputField.setText("");
        registrarAtividadeAgente("USUARIO", "Comando enviado para processamento.");
        adicionarMensagem("Sistema", "Se quiser acompanhar os detalhes da execucao, abra a aba Atividade.");

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

    /** * Adiciona mensagem visual ao historico da conversa. * * @param remetente remetente logico * @param mensagem mensagem textual * * @author Renato Tomaz Nati * @since 2026-05-24 */
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

    /** * Limpa o historico visual da conversa. * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public void limparHistorico() {
        if (chatHistory != null && !chatHistory.isDisposed()) {
            chatHistory.setText("");
        }
    }

    /** * Retorna o controller principal da view. * * @return controller principal * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public ChatAiController getController() {
        return this.controller;
    }

    /** * Libera os recursos graficos da view. * * @author Renato Tomaz Nati * @since 2026-05-24 */
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

    /** * Retorna o painel de blocos nomeados. * * @return painel de contexto nomeado * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public NamedBlocksPanel getNamedBlocksPanel() {
        return namedBlocksPanel;
    }

    /** * Abre a aba de contexto. * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public void abrirAbaContexto() {
        if (tabFolderPrincipal != null && !tabFolderPrincipal.isDisposed()) {
            CTabItem[] itens = tabFolderPrincipal.getItems();
            for (int i = 0; i < itens.length; i++) {
                if ("Contexto".equals(itens[i].getText())) {
                    tabFolderPrincipal.setSelection(itens[i]);
                    if (namedBlocksPanel != null && !namedBlocksPanel.isDisposed()) {
                        namedBlocksPanel.refreshPanel();
                    }
                    return;
                }
            }
        }
    }

    /** * Solicita ao controller a sincronizacao do alvo atual. * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public void sincronizarAlvoPrimarioGlobal() {
        if (controller != null) {
            controller.sincronizarAlvoPrimarioGlobal();
        }
    }

    /** * Anexa um alias de contexto na entrada da conversa. * * @param alias alias a ser inserido * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public void anexarAliasNaEntrada(String alias) {
        if (inputField == null || inputField.isDisposed()) {
            return;
        }

        String aliasSeguro = alias != null ? alias.trim() : "";
        if (aliasSeguro.length() == 0) {
            return;
        }

        String textoAtual = inputField.getText();
        if (textoAtual == null || textoAtual.trim().length() == 0) {
            inputField.setText(aliasSeguro);
        } else {
            String separador = textoAtual.endsWith(" ") || textoAtual.endsWith("\n") ? "" : " ";
            inputField.setText(textoAtual + separador + aliasSeguro);
        }

        inputField.setFocus();
        inputField.setSelection(inputField.getText().length());
    }
}