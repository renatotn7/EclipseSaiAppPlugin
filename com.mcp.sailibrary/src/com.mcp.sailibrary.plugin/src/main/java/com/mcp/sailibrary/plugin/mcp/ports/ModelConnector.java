package com.mcp.sailibrary.plugin.mcp.ports;

import com.mcp.sailibrary.plugin.mcp.core.McpAccessCredentials;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.RawModelResponse;

/** * Porta de conexao com o endpoint remoto. * * @author Renato Tomaz Nati * @since 2026-05-26 */
public interface ModelConnector {

    RawModelResponse execute( ModelExecutionProfile profile, String requestBody, McpAccessCredentials credentials) throws Exception;
}