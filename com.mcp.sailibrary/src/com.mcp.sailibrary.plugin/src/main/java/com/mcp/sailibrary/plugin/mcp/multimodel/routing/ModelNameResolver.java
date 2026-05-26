package com.mcp.sailibrary.plugin.mcp.multimodel.routing;

/* --- version: "1.1" libraries: - N/A objetivo: "Definir o contrato de resolucao de nomes de modelos MCP por papel cognitivo, preservando suporte a monomodelo e permitindo separar investigador e planejador." --- */

/** * Contrato para resolver o nome do modelo remoto no MCP intermediador. * * <p>Esta interface permite separar os papeis de: * investigador, planejador, gerador de codigo, auditor e sumarizador. * Tambem preserva compatibilidade com o modo monomodelo, no qual um unico * modelo pode continuar atendendo a todos os papeis.</p> * * @author Renato Tomaz Nati * @since 2026-05-24 */
public interface ModelNameResolver {

    String resolveInvestigatorModelName();

    String resolvePlannerModelName();

    String resolveCodeGeneratorModelName();

    String resolveCodeAuditorModelName();

    String resolveSummarizerModelName();

  
}