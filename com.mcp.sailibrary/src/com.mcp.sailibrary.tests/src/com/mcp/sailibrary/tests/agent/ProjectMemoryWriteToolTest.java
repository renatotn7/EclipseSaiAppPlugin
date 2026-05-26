package com.mcp.sailibrary.tests.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.mcp.sailibrary.plugin.agent.tools.memory.ProjectMemoryWriteTool;

public class ProjectMemoryWriteToolTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private ProjectMemoryWriteTool writeTool;
    private File mockWorkspaceRoot;
    private String originalUserHome;

    @Before
    public void setUp() throws Exception {
        originalUserHome = System.getProperty("user.home");
        File mockHome = tempFolder.newFolder("mock_home");
        System.setProperty("user.home", mockHome.getAbsolutePath());

        mockWorkspaceRoot = tempFolder.newFolder("mock_workspace");
        writeTool = new ProjectMemoryWriteTool(mockWorkspaceRoot);
    }

    @After
    public void tearDown() {
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome);
        }
    }

    @Test
    public void testMetadataDaFerramenta() {
        assertEquals("registrar_memoria_projeto", writeTool.getName());
        //assertNotNull(writeTool.getDescription());
        assertNotNull(writeTool.getPromptMetadata());
    }

    @Test
    public void testModoAusenteOuJsonInvalido() {
        String resultado = writeTool.execute("{}");
        assertTrue("Esperado que a ferramenta recuse JSON vazio. Recebido: " + resultado, resultado.contains("Erro"));

        String resultadoQuebrado = writeTool.execute("JSON_MAL_FORMADO");
        assertTrue("Esperado que a ferramenta recuse JSON quebrado. Recebido: " + resultadoQuebrado, resultadoQuebrado.contains("Erro"));
    }

    @Test
    public void testModoDesconhecido() {
        String json = "{\"modo\":\"modo_inexistente\"}";
        String resultado = writeTool.execute(json);
        assertTrue("Esperado que a ferramenta recuse modo desconhecido. Recebido: " + resultado, resultado.contains("Erro"));
    }

    @Test
    public void testBranchContextComSucesso() {
        String json = "{\"modo\":\"branch_context\", \"currentBranch\":\"feature/XPTO\", \"reconfirmSensitiveHints\":\"true\"}";
        String resultado = writeTool.execute(json);
        assertTrue("A gravacao deveria ser um sucesso. Recebido: " + resultado, !resultado.contains("Erro"));
    }

    @Test
    public void testPatternFaltandoKey() {
        String json = "{\"modo\":\"pattern\", \"value\":\"algo\"}";
        String resultado = writeTool.execute(json);
        assertTrue("Esperado que recuse pattern sem key. Recebido: " + resultado, resultado.contains("Erro"));
    }

    @Test
    public void testPatternComSucesso() {
        String json = "{\"modo\":\"pattern\", \"kind\":\"arch\", \"key\":\"spring\", \"value\":\"detectado\"}";
        String resultado = writeTool.execute(json);
        assertTrue("A gravacao deveria ser um sucesso. Recebido: " + resultado, !resultado.contains("Erro"));
    }

    @Test
    public void testDependencySnapshotFaltandoGroupId() {
        String json = "{\"modo\":\"dependency_snapshot\"}";
        String resultado = writeTool.execute(json);
        
        // Se a sua ferramenta não faz validação de obrigatoriedade neste caso e grava nulos,
        // o teste irá avisar mostrando "Recebido: <mensagem de sucesso>".
        assertTrue("Esperado que recuse dependencia sem groupId. Recebido: " + resultado, resultado.contains("Erro"));
    }

    @Test
    public void testDependencySnapshotComSucessoEArrays() {
        String json = "{\"modo\":\"dependency_snapshot\", \"javaVersion\":\"21\", \"groupId\":\"com.mcp\", \"modules\":[\"core\", \"api\"], \"frameworkHints\":[\"spring\"]}";
        String resultado = writeTool.execute(json);
        assertTrue("A gravacao de dependencias deveria ser um sucesso. Recebido: " + resultado, !resultado.contains("Erro"));
    }

    @Test
    public void testToolHistoryFaltandoTool() {
        String json = "{\"modo\":\"tool_history\", \"resultSummary\":\"ok\"}";
        String resultado = writeTool.execute(json);
        assertTrue("Esperado que recuse historico sem nome da tool. Recebido: " + resultado, resultado.contains("Erro"));
    }

    @Test
    public void testToolHistoryComSucesso() {
        String json = "{\"modo\":\"tool_history\", \"tool\":\"MinhaTool\", \"parametersSummary\":\"p1\", \"resultSummary\":\"r1\"}";
        String resultado = writeTool.execute(json);
        assertTrue("A gravacao do historico deveria ser um sucesso. Recebido: " + resultado, !resultado.contains("Erro"));
    }
}