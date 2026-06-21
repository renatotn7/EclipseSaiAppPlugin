package com.mcp.sailibrary.tests.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.JsonArray;
import com.mcp.sailibrary.plugin.agent.context.analise.ProjectMemoryJsonSupport;
import com.mcp.sailibrary.plugin.agent.context.analise.ProjectMemoryStore;

/** * Testes diretos do store de memoria persistente. * * @author Renato Tomaz Nati */
public class ProjectMemoryStoreTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File mockWorkspaceRoot;
    private String originalUserHome;

    @Before
    public void setUp() throws Exception {
        originalUserHome = System.getProperty("user.home");
        File mockHome = tempFolder.newFolder("mock_home");
        System.setProperty("user.home", mockHome.getAbsolutePath());

        mockWorkspaceRoot = tempFolder.newFolder("mock_workspace");
    }

    @After
    public void tearDown() {
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome);
        }
    }

    @Test
    public void deveInicializarEstrutura() {
        ProjectMemoryStore store = new ProjectMemoryStore(mockWorkspaceRoot);

        store.inicializarEstrutura();

        assertTrue(store.getMemoryPaths().getProjectMemoryFile().exists());
        assertTrue(store.getMemoryPaths().getToolHistoryFile().exists());
        assertTrue(store.getMemoryPaths().getDependencySnapshotFile().exists());
        assertTrue(store.getMemoryPaths().getDiscoveredPatternsFile().exists());
        assertTrue(store.getMemoryPaths().getBranchContextFile().exists());
    }

    @Test
    public void deveRegistrarBranchContextExplicito() {
        ProjectMemoryStore store = new ProjectMemoryStore(mockWorkspaceRoot);
        store.inicializarEstrutura();

        store.registrarBranchContext("master", "true");

        String resumo = store.consultarResumoMemoria();
        assertTrue(resumo.contains("currentBranch: master"));
        assertTrue(resumo.contains("reconfirmSensitiveHints: true"));
    }

    @Test
    public void deveRegistrarDependencySnapshotComJavaVersionEGroupId() {
        ProjectMemoryStore store = new ProjectMemoryStore(mockWorkspaceRoot);
        store.inicializarEstrutura();

        JsonArray modules = new JsonArray();
        modules.add("core");

        JsonArray frameworks = new JsonArray();
        frameworks.add("spring");

        store.registrarDependencySnapshot(new JsonArray(), frameworks, modules, "21", "com.mcp");

        String resumo = store.consultarResumoMemoria();

        assertTrue(resumo.contains("javaVersion: 21"));
        assertTrue(resumo.contains("groupId: com.mcp"));
        assertTrue(resumo.contains("frameworkHints: [\"spring\"]"));
        assertTrue(resumo.contains("modules: [\"core\"]"));
    }

    @Test
    public void deveRegistrarPattern() {
        ProjectMemoryStore store = new ProjectMemoryStore(mockWorkspaceRoot);
        store.inicializarEstrutura();

        store.registrarPattern("framework", "hibernate", "detectado", "pom.xml", "alta");

        String resumo = store.consultarResumoMemoria();

        assertTrue(resumo.contains("hibernate"));
        assertTrue(resumo.contains("detectado"));
    }
}