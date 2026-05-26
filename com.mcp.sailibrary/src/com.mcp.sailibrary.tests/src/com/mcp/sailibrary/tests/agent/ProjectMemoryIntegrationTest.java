package com.mcp.sailibrary.tests.agent;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.mcp.sailibrary.plugin.agent.orchestration.AgentOrchestrator;

/**
 * Testes de integracao para o ciclo de leitura e escrita da memoria do projeto
 * utilizando o AgentOrchestrator.
 *
 * @author Renato Tomaz Nati
 */
public class ProjectMemoryIntegrationTest {

    // Cria ficheiros no /tmp do sistema que serao destruidos automaticamente
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private AgentOrchestrator orchestrator;
    private File mockWorkspaceRoot;
    private String originalUserHome;

    @Before
    public void setUp() throws Exception {
        // 1. ISOLAMENTO ABSOLUTO: Guardamos o diretório /home/rtnati original
        originalUserHome = System.getProperty("user.home");
        
        // Criamos um /home falso e enganamos a JVM (afeta o ProjectMemoryPaths)
        File mockHome = tempFolder.newFolder("mock_home");
        System.setProperty("user.home", mockHome.getAbsolutePath());

        // 2. Criamos o workspace falso
        mockWorkspaceRoot = tempFolder.newFolder("mock_workspace");

        // 3. Inicializamos o orquestrador (as ferramentas serao injetadas apontando para o workspace falso)
        orchestrator = new AgentOrchestrator(mockWorkspaceRoot, null, 0);
    }

    @After
    public void tearDown() {
        // 4. LIMPEZA: Restaura o /home original para não quebrar outros testes no Eclipse
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome);
        }
    }

    @Test
    public void deveGarantirQueAsFerramentasDeMemoriaEstaoRegistadas() {
        assertNotNull("O orquestrador nao pode ser nulo", orchestrator);
        
        String resultadoQuery = orchestrator.dispatch("consultar_memoria_projeto", "{\"tipo\":\"resumo\"}");
        assertTrue("A ferramenta de consulta deve estar disponivel", 
                resultadoQuery != null && !resultadoQuery.contains("Erro Tatico"));
    }

    @Test
    public void deveGravarEConsultarPatternNaMemoriaDoProjeto() {
        // Cenario: IA detetou um padrao e quer gravar
        String jsonWritePattern = "{"
                + "\"modo\":\"pattern\","
                + "\"kind\":\"framework\","
                + "\"key\":\"hibernate\","
                + "\"value\":\"detectado\","
                + "\"evidence\":\"pom.xml\","
                + "\"confidence\":\"alta\""
                + "}";

        String resultadoWrite = orchestrator.dispatch("registrar_memoria_projeto", jsonWritePattern);
        
        assertTrue("A gravacao do pattern deve ser bem sucedida", 
                resultadoWrite.contains("registrado com sucesso para a chave [hibernate]"));

        // Cenario: IA consulta o resumo da memoria
        String jsonQuery = "{\"tipo\":\"resumo\"}";
        String resultadoQuery = orchestrator.dispatch("consultar_memoria_projeto", jsonQuery);

        assertTrue("O resumo deve conter a chave do pattern", resultadoQuery.contains("hibernate"));
        assertTrue("O resumo deve conter o valor do pattern", resultadoQuery.contains("detectado"));
    }

    @Test
    public void deveGravarEConsultarHistoricoDeFerramenta() {
        // Cenario: IA regista o que acabou de fazer
        String jsonWriteHistory = "{"
                + "\"modo\":\"tool_history\","
                + "\"tool\":\"DirectoryExplorerTool\","
                + "\"parametersSummary\":\"listar src/main/java\","
                + "\"resultSummary\":\"10 ficheiros encontrados\""
                + "}";

        String resultadoWrite = orchestrator.dispatch("registrar_memoria_projeto", jsonWriteHistory);

        assertTrue("A gravacao do historico deve ser bem sucedida", 
                resultadoWrite.contains("Historico da ferramenta [DirectoryExplorerTool] registrado"));

        // Cenario: Consulta para ler o ficheiro gerado no mock_home
        String jsonQueryHistory = "{\"tipo\":\"tool_history\"}";
        String resultadoQuery = orchestrator.dispatch("consultar_memoria_projeto", jsonQueryHistory);

        assertTrue("O ficheiro de historico deve guardar os detalhes sumarizados", 
                resultadoQuery.contains("10 ficheiros encontrados"));
    }

    @Test
    public void deveFalharEleganteSeTentarRegistarPatternSemChave() {
        // Passa um JSON sem o "key" obrigatorio
        String jsonIncompleto = "{\"modo\":\"pattern\", \"value\":\"detectado\"}";
        
        String resultadoWrite = orchestrator.dispatch("registrar_memoria_projeto", jsonIncompleto);
        
        assertTrue("Deve devolver erro devido a falha nos requisitos", 
                resultadoWrite.contains("Erro Operacional: O parametro 'key' e obrigatorio"));
    }
}