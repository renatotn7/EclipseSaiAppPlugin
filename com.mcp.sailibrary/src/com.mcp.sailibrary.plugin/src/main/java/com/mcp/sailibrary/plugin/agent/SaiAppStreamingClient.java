package com.mcp.sailibrary.plugin.agent;

import com.mcp.sailibrary.plugin.mcp.SaiLibraryMcpClient;
import com.mcp.sailibrary.plugin.mcp.core.ModelChannel;

/**
 * Exemplo seguro de chamada programatica ao modelo configurado.
 *
 * Nao expoe porta local e nao embute cookie no codigo. A conexao real e resolvida
 * por mcp-models.properties, sendo SAI_CHAT_EXECUTE_HTTP o default.
 */
public class SaiAppStreamingClient {

    public static void main(String[] args) throws Exception {
        String prompt = args != null && args.length > 0 ? args[0] : "fazendo um teste";
        String resposta = SaiLibraryMcpClient.callCanalPorPrompt(ModelChannel.PLANNER, prompt, "", "");
        System.out.println(resposta);
    }
}
