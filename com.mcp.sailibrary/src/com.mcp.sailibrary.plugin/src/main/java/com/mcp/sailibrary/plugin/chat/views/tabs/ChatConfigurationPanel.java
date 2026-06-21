package com.mcp.sailibrary.plugin.chat.views.tabs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import com.mcp.sailibrary.plugin.chat.controllers.ChatViewConfigurationController;
import com.mcp.sailibrary.plugin.chat.settings.ChatRuntimeSettings;

public class ChatConfigurationPanel extends Composite {

    private ChatViewConfigurationController configurationController;

    private Button checkDebugConfiguracao;
    private org.eclipse.swt.widgets.Combo comboModoExecucao;
    private org.eclipse.swt.widgets.Combo comboPerfilRaciocinio;
    private Button btnSalvarConfiguracao;

    public ChatConfigurationPanel(Composite parent, int style, ChatViewConfigurationController configurationController) {
        super(parent, style);
        this.configurationController = configurationController;

        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 8;
        layout.marginHeight = 8;
        layout.verticalSpacing = 8;
        setLayout(layout);

        criarConteudo();
        aplicarConfiguracao(configurationController != null ? configurationController.carregarConfiguracao() : null);
    }

    private void criarConteudo() {
        Group grupoExecucao = new Group(this, SWT.NONE);
        grupoExecucao.setText("Execucao do agente");
        grupoExecucao.setLayout(new GridLayout(2, false));
        grupoExecucao.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        Label lblDebug = new Label(grupoExecucao, SWT.NONE);
        lblDebug.setText("Modo debug:");

        checkDebugConfiguracao = new Button(grupoExecucao, SWT.CHECK);
        checkDebugConfiguracao.setText("Ativo");

        Label lblModoExecucao = new Label(grupoExecucao, SWT.NONE);
        lblModoExecucao.setText("Modo de execucao:");

        comboModoExecucao = new org.eclipse.swt.widgets.Combo(grupoExecucao, SWT.DROP_DOWN | SWT.READ_ONLY);
        comboModoExecucao.setItems(new String[] {
                "Monomodelo - mais previsivel",
                "Multimodelo - mais verificacao"
        });
        comboModoExecucao.select(0);
        comboModoExecucao.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Label lblPerfilRaciocinio = new Label(grupoExecucao, SWT.NONE);
        lblPerfilRaciocinio.setText("Perfil para raciocinios complexos:");

        comboPerfilRaciocinio = new org.eclipse.swt.widgets.Combo(grupoExecucao, SWT.DROP_DOWN | SWT.READ_ONLY);
        comboPerfilRaciocinio.setItems(new String[] {
                "Padrao",
                "Complexo com maior assertividade",
                "Ultra complexo com maior assertividade"
        });
        comboPerfilRaciocinio.select(0);
        comboPerfilRaciocinio.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Label lblAjuda = new Label(this, SWT.WRAP);
        lblAjuda.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        lblAjuda.setText(
                "Use monomodelo quando quiser mais previsibilidade e menos etapas internas. "
              + "Use multimodelo quando quiser maior verificacao em tarefas de codigo mais sensiveis. "
              + "Os perfis de raciocinio deixam claro para o usuario o nivel de profundidade esperado em casos complexos e ultra complexos."
        );

        btnSalvarConfiguracao = new Button(this, SWT.PUSH);
        btnSalvarConfiguracao.setText("Salvar configuracao");
        btnSalvarConfiguracao.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
    }

    public void aplicarConfiguracao(ChatRuntimeSettings configuracao) {
        if (configuracao == null) {
            return;
        }

        checkDebugConfiguracao.setSelection(configuracao.isDebugAtivo());

        if (ChatRuntimeSettings.MODO_EXECUCAO_MULTI.equals(configuracao.getModoExecucao())) {
            comboModoExecucao.select(1);
        } else {
            comboModoExecucao.select(0);
        }

        if (ChatRuntimeSettings.PERFIL_ULTRA.equals(configuracao.getPerfilRaciocinio())) {
            comboPerfilRaciocinio.select(2);
        } else if (ChatRuntimeSettings.PERFIL_COMPLEXO.equals(configuracao.getPerfilRaciocinio())) {
            comboPerfilRaciocinio.select(1);
        } else {
            comboPerfilRaciocinio.select(0);
        }
    }

    public void aplicarConfiguracaoNaTela(ChatRuntimeSettings configuracao) {
        aplicarConfiguracao(configuracao);
    }

    public ChatRuntimeSettings extrairConfiguracao() {
        ChatRuntimeSettings configuracao = new ChatRuntimeSettings();
        configuracao.setDebugAtivo(checkDebugConfiguracao != null && checkDebugConfiguracao.getSelection());

        if (comboModoExecucao != null && comboModoExecucao.getSelectionIndex() == 1) {
            configuracao.setModoExecucao(ChatRuntimeSettings.MODO_EXECUCAO_MULTI);
        } else {
            configuracao.setModoExecucao(ChatRuntimeSettings.MODO_EXECUCAO_MONO);
        }

        if (comboPerfilRaciocinio != null && comboPerfilRaciocinio.getSelectionIndex() == 2) {
            configuracao.setPerfilRaciocinio(ChatRuntimeSettings.PERFIL_ULTRA);
        } else if (comboPerfilRaciocinio != null && comboPerfilRaciocinio.getSelectionIndex() == 1) {
            configuracao.setPerfilRaciocinio(ChatRuntimeSettings.PERFIL_COMPLEXO);
        } else {
            configuracao.setPerfilRaciocinio(ChatRuntimeSettings.PERFIL_PADRAO);
        }

        return configuracao;
    }

    public void salvarConfiguracaoAtual() {
        if (configurationController == null) {
            return;
        }

        configurationController.salvarConfiguracao(extrairConfiguracao());
    }

    public Button getBtnSalvarConfiguracao() {
        return btnSalvarConfiguracao;
    }

    public boolean isDebugConfigurado() {
        return checkDebugConfiguracao != null
                && !checkDebugConfiguracao.isDisposed()
                && checkDebugConfiguracao.getSelection();
    }

    public String getModoExecucaoConfigurado() {
        if (comboModoExecucao == null || comboModoExecucao.isDisposed()) {
            return ChatRuntimeSettings.MODO_EXECUCAO_MONO;
        }

        if (comboModoExecucao.getSelectionIndex() == 1) {
            return ChatRuntimeSettings.MODO_EXECUCAO_MULTI;
        }

        return ChatRuntimeSettings.MODO_EXECUCAO_MONO;
    }

    public String getPerfilRaciocinioConfigurado() {
        if (comboPerfilRaciocinio == null || comboPerfilRaciocinio.isDisposed()) {
            return ChatRuntimeSettings.PERFIL_PADRAO;
        }

        if (comboPerfilRaciocinio.getSelectionIndex() == 2) {
            return ChatRuntimeSettings.PERFIL_ULTRA;
        }

        if (comboPerfilRaciocinio.getSelectionIndex() == 1) {
            return ChatRuntimeSettings.PERFIL_COMPLEXO;
        }

        return ChatRuntimeSettings.PERFIL_PADRAO;
    }
}