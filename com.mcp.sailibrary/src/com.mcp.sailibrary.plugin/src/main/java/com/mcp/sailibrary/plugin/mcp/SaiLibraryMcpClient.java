package com.mcp.sailibrary.plugin.mcp;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.orchestration.AgentToolRegistryFactory;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptSectionBuilder;

/** * Fachada publica de integracao com o endpoint MCP da aplicacao. * * <p>Esta classe coordena a montagem dos prompts, a serializacao do payload * JSON-RPC, o transporte HTTP e a extracao de respostas relevantes, sem expor * essas responsabilidades ao restante do plugin.</p> * * <p>O fluxo principal aceita tanto contexto textual quanto contexto * estrutural, desde que pelo menos um deles esteja presente de forma valida no * momento da chamada.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class SaiLibraryMcpClient {

    private static final String API_URL = "https://sai-library.saiapplications.com/api/mcp";

    private static final AgentToolPromptSectionBuilder TOOL_SECTION_BUILDER = new AgentToolPromptSectionBuilder();
    private static final DesenvolvimentoPromptBuilder DESENVOLVIMENTO_PROMPT_BUILDER = new DesenvolvimentoPromptBuilder();
    private static final BlockNamePromptBuilder BLOCK_NAME_PROMPT_BUILDER = new BlockNamePromptBuilder();
    private static final McpPayloadBuilder PAYLOAD_BUILDER = new McpPayloadBuilder();
    private static final McpHttpGateway HTTP_GATEWAY = new McpHttpGateway();
    private static final McpResponseExtractor RESPONSE_EXTRACTOR = new McpResponseExtractor();

    /** * Executa a chamada principal do fluxo de desenvolvimento GPT5. * * <p>Este metodo aceita cenarios textuais e estruturais. Quando nao houver * trecho textual ativo nem arquivo textual carregado, a chamada ainda pode * prosseguir se a instrucao enriquecida indicar claramente contexto * estrutural utilizavel.</p> * * @param selectedCode trecho textual selecionado no editor * @param fullFileText conteudo completo do arquivo ativo * @param instrucao instrucao enriquecida enviada pelo controlador * @param apiKey chave de autenticacao do MCP * @return corpo bruto da resposta do endpoint MCP * * @throws Exception quando ocorrer falha de validacao, serializacao ou * transporte HTTP * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public static String callDesenvolvimentoGpt5(String selectedCode, String fullFileText, String instrucao, String apiKey) throws Exception {

        String textoSelecionado = selectedCode != null ? selectedCode : "";
        String textoArquivoCompleto = fullFileText != null ? fullFileText : "";
        String textoInstrucao = instrucao != null ? instrucao : "";

        boolean possuiTrechoTextual = textoSelecionado.trim().length() > 0;
        boolean possuiArquivoTextual = textoArquivoCompleto.trim().length() > 0;
        boolean possuiContextoEstrutural = possuiContextoEstruturalNoPrompt(textoInstrucao);

        if (!possuiTrechoTextual && !possuiArquivoTextual && !possuiContextoEstrutural) {
            throw new IllegalStateException(
                    "Nenhum insumo textual ou estrutural principal foi encontrado para a chamada MCP. "
                    + "Selecione um trecho/bloco principal no editor ou mantenha um contexto estrutural principal/utilizavel na sessao."
            );
        }

        String textoSelecionadoParaPrompt = possuiTrechoTextual ? textoSelecionado : "[SEM_TRECHO_SELECIONADO]";
        String textoArquivoCompletoParaPrompt = possuiArquivoTextual ? textoArquivoCompleto : "[SEM_ARQUIVO_TEXTUAL_ATIVO]";
        String modoOperacionalDetectado = (possuiTrechoTextual || possuiArquivoTextual)
                ? "MODO_TEXTUAL"
                : "MODO_ESTRUTURAL";

        System.out.println(
                "\n\nfullFile " + textoArquivoCompleto
                + " \n\nselectedText " + textoSelecionado
                + " \n\ninstrucao " + textoInstrucao
        );

        File raizWorkspace = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
        List<AgentTool> ferramentasPrompt = new AgentToolRegistryFactory().build(raizWorkspace, null, 0);

        String secaoFerramentas = TOOL_SECTION_BUILDER.buildToolsSection(ferramentasPrompt);
        String secaoExemplosFerramentas = TOOL_SECTION_BUILDER.buildExamplesSection(ferramentasPrompt, 1);

        String promptEngenharia = DESENVOLVIMENTO_PROMPT_BUILDER.build(
                modoOperacionalDetectado,
                textoSelecionadoParaPrompt,
                textoArquivoCompletoParaPrompt,
                textoInstrucao,
                secaoFerramentas,
                secaoExemplosFerramentas
        );

        String jsonPayload = PAYLOAD_BUILDER.buildToolsCallPayload(
                "DesenvolvimentoLivreGpt5",
                "Prompt: " + promptEngenharia
        );
        
        return HTTP_GATEWAY.postJsonRpc(API_URL, apiKey, jsonPayload);
    }

    /** * Executa a chamada de sugestao de nome curto para bloco ou contexto. * * @param selectedCode trecho selecionado * @param kind tipo logico do bloco * @param existingNames nomes ja existentes na sessao * @param apiKey chave de autenticacao do MCP * @return corpo bruto da resposta do endpoint MCP * * @throws Exception quando ocorrer falha de validacao, serializacao ou * transporte HTTP * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public static String callSugestaoNomeBloco(String selectedCode, String kind, String existingNames, String apiKey) throws Exception {

        String prompt = BLOCK_NAME_PROMPT_BUILDER.build(
                selectedCode != null ? selectedCode : "",
                kind != null ? kind : "",
                existingNames != null ? existingNames : ""
        );

        String jsonPayload = PAYLOAD_BUILDER.buildToolsCallPayload(
                "DesenvolvimentoLivreGpt5",
                "Prompt: " + prompt
        );

        return HTTP_GATEWAY.postJsonRpc(API_URL, apiKey, jsonPayload);
    }

    /** * Extrai e normaliza o nome sugerido retornado pelo MCP. * * @param rawResponse resposta bruta do endpoint * @return nome sugerido normalizado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public static String extractSuggestedBlockName(String rawResponse) {
        return RESPONSE_EXTRACTOR.extractSuggestedBlockName(rawResponse);
    }

    /** * Mantem compatibilidade com chamadas antigas de sanitizacao. * * <p>O fluxo principal atual prefere a serializacao via Gson no payload, * mas este metodo continua exposto para compatibilidade com pontos antigos * que ainda dependam do escape textual manual.</p> * * @param entrada texto original * @return texto escapado para transporte JSON * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public static String safeString(String entrada) {
        return McpPayloadBuilder.escapeForJsonTransport(entrada);
    }

    /** * Detecta se a instrucao enriquecida carrega indicios fortes de contexto * estrutural utilizavel para a IA. * * <p>Este metodo nao tenta interpretar toda a semantica do prompt. Ele * apenas detecta sinais fortes de contexto estrutural, como aliases no * formato {@code @nome}, secoes estruturais da sessao e marcadores de alvo * estrutural principal.</p> * * @param textoInstrucao instrucao enriquecida enviada ao MCP * @return true quando houver indicio forte de contexto estrutural * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private static boolean possuiContextoEstruturalNoPrompt(String textoInstrucao) {
        if (textoInstrucao == null || textoInstrucao.trim().length() == 0) {
            return false;
        }

        String texto = textoInstrucao;

        if (texto.contains("@")) {
            java.util.regex.Pattern aliasPattern = java.util.regex.Pattern.compile("@[a-zA-Z0-9_]+");
            java.util.regex.Matcher matcher = aliasPattern.matcher(texto);
            if (matcher.find()) {
                return true;
            }
        }

        if (texto.contains("=== CONTEXTO ESTRUTURAL DA SESSAO ===")) {
            return true;
        }

        if (texto.contains("FOCO_PRINCIPAL_ESTRUTURAL:")) {
            return true;
        }

        if (texto.contains("ESCOPO_EDITAVEL_ESTRUTURAL:")) {
            return true;
        }

        if (texto.contains("ESCOPO_REFERENCIAL_ESTRUTURAL:")) {
            return true;
        }

        if (texto.contains("Contexto estrutural")) {
            return true;
        }

        if (texto.contains("ALVO PRINCIPAL:") && texto.contains("arquivo=")) {
            return true;
        }

        return false;
    }
}