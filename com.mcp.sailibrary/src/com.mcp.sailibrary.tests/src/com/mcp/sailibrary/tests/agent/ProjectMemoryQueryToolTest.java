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

import com.mcp.sailibrary.plugin.agent.tools.memory.ProjectMemoryQueryTool;
import com.mcp.sailibrary.plugin.agent.tools.memory.ProjectMemoryWriteTool;

/** * Testes de unidade da ferramenta de leitura da memoria persistente do projeto. * * <p>Os testes usam user.home e workspace isolados para evitar poluicao do * ambiente local do desenvolvedor.</p> * * @author Renato Tomaz Nati */
public class ProjectMemoryQueryToolTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private ProjectMemoryQueryTool queryTool;
    private ProjectMemoryWriteTool writeTool;
    private File mockWorkspaceRoot;
    private String originalUserHome;

    @Before
    public void setUp() throws Exception {
        originalUserHome = System.getProperty("user.home");

        File mockHome = tempFolder.newFolder("mock_home");
        System.setProperty("user.home", mockHome.getAbsolutePath());

        mockWorkspaceRoot = tempFolder.newFolder("mock_workspace");

        queryTool = new ProjectMemoryQueryTool(mockWorkspaceRoot);
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
        assertEquals("consultar_memoria_projeto", queryTool.getName());
        assertNotNull(queryTool.getPromptMetadata());
    }

    @Test
    public void testLeituraResumoVazio() {
        String json = "{\"tipo\":\"resumo\"}";
        String resultado = queryTool.execute(json);

        assertTrue("O resumo deveria conter o cabecalho padrao. Recebido: " + resultado,
                resultado.contains("Resumo da memoria persistente do projeto"));
        assertTrue("O resumo deveria conter o diretorio do projeto. Recebido: " + resultado,
                resultado.contains("Diretorio: "));
    }

    @Test
    public void testLeituraFallbackParaResumoSemTipo() {
        String resultadoVazio = queryTool.execute("{}");
        assertTrue("Sem tipo, a consulta deveria cair para resumo. Recebido: " + resultadoVazio,
                resultadoVazio.contains("Resumo da memoria persistente do projeto"));

        String resultadoMalFormado = queryTool.execute("IstoNaoEJSON");
        assertTrue("JSON mal formado deveria cair para resumo. Recebido: " + resultadoMalFormado,
                resultadoMalFormado.contains("Resumo da memoria persistente do projeto"));
    }

    @Test
    public void testLeituraResumoComDadosPopulados() {
        writeTool.execute("{\"modo\":\"branch_context\", \"currentBranch\":\"master\"}");
        writeTool.execute("{\"modo\":\"pattern\", \"key\":\"banco\", \"value\":\"mysql\"}");
        writeTool.execute("{\"modo\":\"dependency_snapshot\", \"javaVersion\":\"21\", \"groupId\":\"com.mcp\"}");

        String resultado = queryTool.execute("{\"tipo\":\"resumo\"}");

        assertTrue("Faltou 'master' no resumo: " + resultado, resultado.contains("master"));
        assertTrue("Faltou '21' no resumo: " + resultado, resultado.contains("21"));
        assertTrue("Faltou 'com.mcp' no resumo: " + resultado, resultado.contains("com.mcp"));
        assertTrue("Faltou 'banco' no resumo: " + resultado, resultado.contains("banco"));
        assertTrue("Faltou 'mysql' no resumo: " + resultado, resultado.contains("mysql"));
    }

    @Test
    public void testLeituraToolHistory() {
        writeTool.execute("{\"modo\":\"tool_history\", \"tool\":\"GitTool\", \"parametersSummary\":\"commit\", \"resultSummary\":\"sucesso\"}");

        String resultado = queryTool.execute("{\"tipo\":\"tool_history\"}");

        assertTrue("Faltou 'GitTool' no historico: " + resultado, resultado.contains("GitTool"));
        assertTrue("Faltou 'commit' no historico: " + resultado, resultado.contains("commit"));
        assertTrue("Faltou 'sucesso' no historico: " + resultado, resultado.contains("sucesso"));
    }
}