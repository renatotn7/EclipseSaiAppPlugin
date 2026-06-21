package com.mcp.sailibrary.plugin.mcp.ports;

import com.mcp.sailibrary.plugin.mcp.core.ModelChannel;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;

/** * Porta para resolver o perfil completo de execucao por papel cognitivo. * * @author Renato Tomaz Nati * @since 2026-05-26 */
public interface ModelExecutionProfileResolver {

    ModelExecutionProfile resolve(ModelChannel channel);
}