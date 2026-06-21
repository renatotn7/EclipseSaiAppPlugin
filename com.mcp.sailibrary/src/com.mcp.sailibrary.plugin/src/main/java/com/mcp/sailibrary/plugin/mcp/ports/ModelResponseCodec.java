package com.mcp.sailibrary.plugin.mcp.ports;

import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionResponse;
import com.mcp.sailibrary.plugin.mcp.core.RawModelResponse;

/** * Porta para interpretacao da resposta bruta. * * @author Renato Tomaz Nati * @since 2026-05-26 */
public interface ModelResponseCodec {

    ModelExecutionResponse decode(RawModelResponse rawResponse, ModelExecutionProfile profile) throws Exception;
}