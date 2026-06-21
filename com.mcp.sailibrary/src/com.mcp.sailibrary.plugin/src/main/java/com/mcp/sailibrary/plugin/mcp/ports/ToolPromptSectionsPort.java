package com.mcp.sailibrary.plugin.mcp.ports;

import com.mcp.sailibrary.plugin.mcp.core.ToolPromptSections;

/** * Porta para carregar secoes de ferramentas para o prompt. * * @author Renato Tomaz Nati * @since 2026-05-26 */
public interface ToolPromptSectionsPort {

    ToolPromptSections load() throws Exception;
}