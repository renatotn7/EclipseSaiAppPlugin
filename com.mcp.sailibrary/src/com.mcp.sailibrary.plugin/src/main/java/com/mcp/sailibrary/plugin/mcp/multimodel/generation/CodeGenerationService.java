package com.mcp.sailibrary.plugin.mcp.multimodel.generation;

import com.mcp.sailibrary.plugin.chat.support.AiResponse;

/* --- version: "1.0" libraries: - com.mcp.sailibrary.plugin.chat.support.AiResponse objetivo: "Definir o contrato de geracao de codigo para o circuito multi-modelo." --- */

/** * Contrato para servicos responsaveis por gerar ou refatorar codigo. * * @author Renato Tomaz Nati * @since 2026-05-24 */
public interface CodeGenerationService {

    /** * Caller: MultiModelCoordinator * Callee: Implementacao concreta do servico de geracao * Objetivo: Gerar ou refatorar codigo a partir do pedido original, do plano * de implementacao e do contexto textual atual. * Data modificacao: 2026-05-24 00:00 * * @param pedidoOriginal pedido original do usuario * @param planoImplementacao plano tecnico consolidado * @param selectedCode trecho textual selecionado * @param fullFileText conteudo integral do arquivo atual * @param actionEsperada acao final esperada * @param apiKey chave de autenticacao MCP * @return resposta estruturada de codigo * @throws Exception quando ocorrer falha de transporte ou geracao * * @author Renato Tomaz Nati * @since 2026-05-24 */
    AiResponse gerarCodigo(String pedidoOriginal, String planoImplementacao, String selectedCode, String fullFileText, String actionEsperada, String apiKey) throws Exception;
}