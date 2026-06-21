package com.mcp.sailibrary.plugin.agent.orchestration;

import java.io.File;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.context.analise.ProjectMemoryStore;

/** * Atua como orquestrador central do arsenal de ferramentas homologadas da IA. * * <p>Esta classe recebe o nome da ferramenta solicitado pela camada superior, * localiza a implementacao correspondente no registro ativo e delega a * execucao com tratamento defensivo de falhas.</p> * * <p>A montagem do registro de ferramentas foi extraida para uma factory * especifica, reduzindo acoplamento e mantendo esta classe focada apenas em * orquestracao e dispatch.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class AgentOrchestrator {

    private final List<AgentTool> registry;
    private final File raizProjetoWorkspace;

    /** * Inicializa o orquestrador com o arsenal homologado para o projeto e * contexto atual. * * @param raizProjetoWorkspace raiz segura do projeto atual * @param compilationUnitAtual compilation unit atual do editor, quando houver * @param offsetAtual offset atual do editor, quando houver * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public AgentOrchestrator(File raizProjetoWorkspace, ICompilationUnit compilationUnitAtual, int offsetAtual) {
        this.raizProjetoWorkspace = raizProjetoWorkspace;
        this.registry = new AgentToolRegistryFactory().build(
                raizProjetoWorkspace,
                compilationUnitAtual,
                offsetAtual
        );
    }

    /** * Executa a ferramenta homologada correspondente ao nome informado. * * @param nomeFerramenta nome logico da ferramenta * @param jsonParameters parametros JSON da ferramenta * @return resultado textual da execucao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String dispatch(String nomeFerramenta, String jsonParameters) {
        if (nomeFerramenta == null || nomeFerramenta.trim().length() == 0) {
            return "Comando abortado: Nenhuma ferramenta especificada.";
        }

        for (int i = 0; i < registry.size(); i++) {
            AgentTool tool = registry.get(i);
            if (tool.getName().equalsIgnoreCase(nomeFerramenta)) {
                System.out.println("[AGENT LOG] Executando ferramenta: " + tool.getName());
                try {
                    String resultado = tool.execute(jsonParameters);
                    persistirAnaliseFerramenta(tool.getName(), jsonParameters, resultado);
                    return resultado;
                } catch (Exception e) {
                    return "Falha critica durante execucao da ferramenta [" + nomeFerramenta + "]: " + e.getMessage();
                }
            }
        }

        return "Erro Tatico: A ferramenta [" + nomeFerramenta + "] nao existe ou nao esta homologada no arsenal ativo.";
    }
    private void persistirAnaliseFerramenta(String toolName, String jsonParameters, String resultado) {
        if (raizProjetoWorkspace == null) {
            return;
        }

        try {
            new ProjectMemoryStore(raizProjetoWorkspace).registrarAgentToolAnalise(toolName, jsonParameters, resultado);
        } catch (Exception e) {
            System.out.println("[MCP MEMORY DEBUG] agentToolAnalysis.persistError=" + e.getMessage());
        }
    }

    /* * Feature: ponto publico estavel para execucao de ferramentas. * Data: 2026-05-27 14:20 * Chamado por: * - MissionToolStepService.invocarFerramenta * Chama: * - dispatch * Objetivo: * - evitar dependencia de reflection em nomes internos */
    public String executarFerramenta(String toolName, String jsonParameters) {
        return dispatch(toolName, jsonParameters);
    }
    /** * Retorna o registro ativo de ferramentas homologadas. * * @return lista de tools registradas * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public List<AgentTool> getRegistry() {
        return registry;
    }
}