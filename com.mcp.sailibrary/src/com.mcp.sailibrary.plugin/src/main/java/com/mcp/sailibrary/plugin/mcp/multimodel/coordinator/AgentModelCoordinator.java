package com.mcp.sailibrary.plugin.mcp.multimodel.coordinator;

import com.mcp.sailibrary.plugin.chat.support.AiResponse;

/** * --- * yaml_header: * version: "1.0" * dependencies: * - com.mcp.sailibrary.plugin.chat.support.AiResponse * purpose: "Definir o contrato de coordenacao entre a camada de controller e os modelos de IA, mantendo compatibilidade com o fluxo atual." * design_pattern: "Strategy / Coordinator" * --- */
public interface AgentModelCoordinator {

    /** * Caller: ChatAiController * Callee: Implementacao concreta do coordenador * Objetivo: Executar a missao cognitiva principal e devolver a resposta ja interpretada no formato interno do plugin. * Feature: Mantem a controller desacoplada do modo single-modelo ou multi-modelo. * Data modificacao: 2026-05-24 00:00 * * @param selectedCode trecho textual selecionado no editor * @param fullFileText conteudo completo do arquivo atual * @param instrucao instrucao enriquecida pronta para o modelo * @param apiKey chave de autenticacao do endpoint remoto * @return resposta estruturada da IA * @throws Exception quando ocorrer falha de transporte, parse ou coordenacao * * @author Renato Tomaz Nati * @since 2026-05-24 */
    AiResponse executarMissao(String selectedCode, String fullFileText, String instrucao, String apiKey) throws Exception;
}