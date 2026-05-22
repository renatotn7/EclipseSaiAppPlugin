package com.mcp.sailibrary.plugin.agent.prompt;

/** * Centraliza utilidades de adaptacao textual para transporte do prompt em * payload JSON. * * <p>O objetivo e permitir que o builder trabalhe com texto legivel e normal, * enquanto esta classe faz a adaptacao final para o formato seguro de envio ao * MCP.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class PromptTransportSupport {

    /** * Escapa uma string para transporte seguro dentro de payload JSON textual. * * @param input texto original * @return texto adaptado para transporte JSON * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String adaptForJsonTransport(String input) {
        if (input == null) {
            return "";
        }

        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", "\\t");
    }
}