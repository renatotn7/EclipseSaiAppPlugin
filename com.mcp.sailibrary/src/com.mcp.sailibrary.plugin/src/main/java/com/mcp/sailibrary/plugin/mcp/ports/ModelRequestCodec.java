package com.mcp.sailibrary.plugin.mcp.ports;

import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionRequest;

/** * Porta para montagem do JSON de entrada. * * @author Renato Tomaz Nati * @since 2026-05-26 */
public interface ModelRequestCodec {

    String encode(ModelExecutionRequest request, ModelExecutionProfile profile) throws Exception;
}