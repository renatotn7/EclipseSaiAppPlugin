package com.mcp.sailibrary.plugin.chat.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import com.mcp.sailibrary.plugin.chat.settings.ChatRuntimeSettings;

/* --- version: "1.1" libraries: - File - FileInputStream - FileOutputStream - Properties - ChatRuntimeSettings objetivo: "Controlar a configuracao persistida da view de chat no diretorio .sai, com defaults seguros quando o arquivo ainda nao existir." --- */

/** * Controlador da configuracao da view de chat. * * <p>Responsavel por: * <ul> * <li>fornecer valores default quando nao existir arquivo de configuracao</li> * <li>carregar configuracao persistida</li> * <li>salvar configuracao em ~/.sai/chat-view.properties</li> * </ul> * </p> * * @author Renato Tomaz Nati * @since 2026-05-24 */
public class ChatViewConfigurationController {

    private static final String KEY_DEBUG = "debug";
    private static final String KEY_MODO_EXECUCAO = "modoExecucao";
    private static final String KEY_PERFIL_RACIOCINIO = "perfilRaciocinio";

    /** * Caller: ChatConfigurationPanel, ChatView * Callee: criarConfiguracaoDefault, resolverArquivoConfiguracaoUsuario * Objetivo: Carregar a configuracao persistida ou devolver defaults seguros * quando o arquivo ainda nao existir. * Feature: Garante inicializacao consistente sem depender da existencia * previa do arquivo ~/.sai/chat-view.properties. * Data modificacao: 2026-05-24 00:00 * * @return configuracao carregada * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public ChatRuntimeSettings carregarConfiguracao() {
        ChatRuntimeSettings configuracao = criarConfiguracaoDefault();

        File arquivoConfiguracao = resolverArquivoConfiguracaoUsuario();
        if (!arquivoConfiguracao.exists() || !arquivoConfiguracao.isFile()) {
            return configuracao;
        }

        Properties properties = new Properties();
        FileInputStream input = null;

        try {
            input = new FileInputStream(arquivoConfiguracao);
            properties.load(input);

            configuracao.setDebugAtivo("true".equalsIgnoreCase(properties.getProperty(KEY_DEBUG, "false")));
            configuracao.setModoExecucao(safeModoExecucao(properties.getProperty(KEY_MODO_EXECUCAO, ChatRuntimeSettings.MODO_EXECUCAO_MONO)));
            configuracao.setPerfilRaciocinio(safePerfilRaciocinio(properties.getProperty(KEY_PERFIL_RACIOCINIO, ChatRuntimeSettings.PERFIL_PADRAO)));
        } catch (Exception e) {
            return criarConfiguracaoDefault();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Exception e) {
                }
            }
        }

        return configuracao;
    }

    /** * Caller: ChatConfigurationPanel, ChatView * Callee: resolverArquivoConfiguracaoUsuario * Objetivo: Persistir a configuracao atual em ~/.sai/chat-view.properties. * Data modificacao: 2026-05-24 00:00 * * @param configuracao configuracao a persistir * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public void salvarConfiguracao(ChatRuntimeSettings configuracao) {
        if (configuracao == null) {
            return;
        }

        Properties properties = new Properties();
        properties.setProperty(KEY_DEBUG, configuracao.isDebugAtivo() ? "true" : "false");
        properties.setProperty(KEY_MODO_EXECUCAO, safeModoExecucao(configuracao.getModoExecucao()));
        properties.setProperty(KEY_PERFIL_RACIOCINIO, safePerfilRaciocinio(configuracao.getPerfilRaciocinio()));

        File arquivoConfiguracao = resolverArquivoConfiguracaoUsuario();
        File diretorioPai = arquivoConfiguracao.getParentFile();
        if (diretorioPai != null && !diretorioPai.exists()) {
            diretorioPai.mkdirs();
        }

        FileOutputStream output = null;
        try {
            output = new FileOutputStream(arquivoConfiguracao);
            properties.store(output, "chat view configuration");
        } catch (Exception e) {
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (Exception e) {
                }
            }
        }
    }

    /** * Caller: carregarConfiguracao, salvarConfiguracao * Callee: N/A * Objetivo: Resolver o arquivo fisico de configuracao do usuario. * Data modificacao: 2026-05-24 00:00 * * @return arquivo de configuracao do chat * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public File resolverArquivoConfiguracaoUsuario() {
        File diretorioSai = new File(System.getProperty("user.home"), ".sai");
        if (!diretorioSai.exists()) {
            diretorioSai.mkdirs();
        }

        return new File(diretorioSai, "chat-view.properties");
    }

    /** * Caller: carregarConfiguracao * Callee: N/A * Objetivo: Construir o estado default da configuracao. * Data modificacao: 2026-05-24 00:00 * * @return configuracao default * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public ChatRuntimeSettings criarConfiguracaoDefault() {
        ChatRuntimeSettings configuracao = new ChatRuntimeSettings();
        configuracao.setDebugAtivo(false);
        configuracao.setModoExecucao(ChatRuntimeSettings.MODO_EXECUCAO_MONO);
        configuracao.setPerfilRaciocinio(ChatRuntimeSettings.PERFIL_PADRAO);
        return configuracao;
    }

    /** * Caller: metodos internos * Callee: N/A * Objetivo: Garantir um modo de execucao valido. * Data modificacao: 2026-05-24 00:00 * * @param valor valor original * @return valor saneado * * @author Renato Tomaz Nati * @since 2026-05-24 */
    private String safeModoExecucao(String valor) {
        if (ChatRuntimeSettings.MODO_EXECUCAO_MULTI.equals(valor)) {
            return ChatRuntimeSettings.MODO_EXECUCAO_MULTI;
        }
        return ChatRuntimeSettings.MODO_EXECUCAO_MONO;
    }

    /** * Caller: metodos internos * Callee: N/A * Objetivo: Garantir um perfil de raciocinio valido. * Data modificacao: 2026-05-24 00:00 * * @param valor valor original * @return valor saneado * * @author Renato Tomaz Nati * @since 2026-05-24 */
    private String safePerfilRaciocinio(String valor) {
        if (ChatRuntimeSettings.PERFIL_ULTRA.equals(valor)) {
            return ChatRuntimeSettings.PERFIL_ULTRA;
        }
        if (ChatRuntimeSettings.PERFIL_COMPLEXO.equals(valor)) {
            return ChatRuntimeSettings.PERFIL_COMPLEXO;
        }
        return ChatRuntimeSettings.PERFIL_PADRAO;
    }
}