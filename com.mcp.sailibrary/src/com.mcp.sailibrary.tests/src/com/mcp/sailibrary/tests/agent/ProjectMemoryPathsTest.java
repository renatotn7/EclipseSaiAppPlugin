package com.mcp.sailibrary.tests.agent;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.mcp.sailibrary.plugin.agent.context.analise.ProjectMemoryPaths;

/** * Testes dos caminhos de memoria persistente. * * @author Renato Tomaz Nati */
public class ProjectMemoryPathsTest {

    @Test
    public void deveCriarTodosOsCaminhosBasicos() {
        ProjectMemoryPaths paths = new ProjectMemoryPaths("meu_projeto_123");

        assertNotNull(paths.getProjectDirectory());
        assertNotNull(paths.getProjectMemoryFile());
        assertNotNull(paths.getToolHistoryFile());
        assertNotNull(paths.getDependencySnapshotFile());
        assertNotNull(paths.getDiscoveredPatternsFile());
        assertNotNull(paths.getBranchContextFile());

        assertTrue(paths.getProjectDirectory().getAbsolutePath().contains("meu_projeto_123"));
    }
}