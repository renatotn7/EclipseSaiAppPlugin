package com.mcp.sailibrary.plugin.mcp.adapters.eclipse;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.orchestration.AgentToolRegistryFactory;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptSectionBuilder;
import com.mcp.sailibrary.plugin.mcp.core.ToolPromptSections;
import com.mcp.sailibrary.plugin.mcp.ports.ToolPromptSectionsPort;

/** * Adapter Eclipse responsavel por carregar as secoes de ferramentas do prompt. * * <p>Esta classe isola o uso do workspace Eclipse para que os services de * aplicacao nao precisem conhecer ResourcesPlugin nem a montagem concreta * do catalogo de ferramentas.</p> * * @author Renato Tomaz Nati * @since 2026-05-26 */
public class EclipseToolPromptSectionsAdapter implements ToolPromptSectionsPort {

    private final AgentToolRegistryFactory agentToolRegistryFactory;
    private final AgentToolPromptSectionBuilder agentToolPromptSectionBuilder;

    public EclipseToolPromptSectionsAdapter() {
        this(new AgentToolRegistryFactory(), new AgentToolPromptSectionBuilder());
    }

    public EclipseToolPromptSectionsAdapter(AgentToolRegistryFactory agentToolRegistryFactory, AgentToolPromptSectionBuilder agentToolPromptSectionBuilder) {
        this.agentToolRegistryFactory = agentToolRegistryFactory != null
                ? agentToolRegistryFactory
                : new AgentToolRegistryFactory();
        this.agentToolPromptSectionBuilder = agentToolPromptSectionBuilder != null
                ? agentToolPromptSectionBuilder
                : new AgentToolPromptSectionBuilder();
    }

    @Override
    public ToolPromptSections load() throws Exception {
        try {
            File raizWorkspace = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();

            List<AgentTool> ferramentasPrompt = agentToolRegistryFactory.build(raizWorkspace, null, 0);
            if (ferramentasPrompt == null) {
                ferramentasPrompt = Collections.emptyList();
            }

            String secaoFerramentas = agentToolPromptSectionBuilder.buildToolsSection(ferramentasPrompt);
            String secaoExemplosFerramentas = agentToolPromptSectionBuilder.buildExamplesSection(ferramentasPrompt, 1);

            return new ToolPromptSections(secaoFerramentas, secaoExemplosFerramentas);
        } catch (Throwable e) {
            System.out.println("[MCP TOOL SECTION DEBUG] Falha ao montar secoes de ferramentas: " + e.getMessage());
            return new ToolPromptSections("", "");
        }
    }
}