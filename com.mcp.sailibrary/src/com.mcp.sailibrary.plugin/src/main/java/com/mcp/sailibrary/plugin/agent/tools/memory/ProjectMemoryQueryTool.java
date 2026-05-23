package com.mcp.sailibrary.plugin.agent.tools.memory;

import java.io.File;

import com.google.gson.JsonObject;
import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.context.ResolvedProjectScope;
import com.mcp.sailibrary.plugin.agent.context.SourceInsightSupport;
import com.mcp.sailibrary.plugin.agent.context.analise.ProjectMemoryJsonSupport;
import com.mcp.sailibrary.plugin.agent.context.analise.ProjectMemoryPaths;
import com.mcp.sailibrary.plugin.agent.context.analise.ProjectMemoryStore;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolParameterMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadataProvider;

/** * Expor leitura da memoria persistente do projeto para a camada de agentes. * * <p>Esta ferramenta foi ajustada para resolver de forma mais estável o * diretório efetivo do projeto, reduzindo risco de fragmentação de memória em * cenários Maven multimódulo e múltiplos `.project`.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class ProjectMemoryQueryTool implements AgentTool, AgentToolPromptMetadataProvider {

    private File rootDirectory;
    private SourceInsightSupport support;

    /** * Inicializa a ferramenta de leitura da memoria persistente do projeto. * * @param rootDirectory raiz segura do projeto atual * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public ProjectMemoryQueryTool(File rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.support = new SourceInsightSupport();
    }

    @Override
    public String getName() {
        return "consultar_memoria_projeto";
    }

    @Override
    public AgentToolPromptMetadata getPromptMetadata() {
        AgentToolPromptMetadata metadata = new AgentToolPromptMetadata();
        metadata.setToolName(getName());
        metadata.setOneLinePurpose("Consultar a memoria persistente do projeto para reduzir re-investigacao.");
        metadata.setActivityDescription("Le cache persistente do projeto para evitar re-investigacao redundante.");

        AgentToolParameterMetadata tipo = new AgentToolParameterMetadata();
        tipo.setName("tipo");
        tipo.setRequired(false);
        tipo.setDescription("Tipo de consulta desejada, como resumo, project_memory, tool_history, dependency_snapshot, discovered_patterns ou branch_context.");
        tipo.setExampleValue("resumo");
        metadata.addParameter(tipo);

        AgentToolParameterMetadata path = new AgentToolParameterMetadata();
        path.setName("path");
        path.setRequired(false);
        path.setDescription("Caminho relativo usado para resolver o escopo efetivo do projeto.");
        path.setExampleValue("src/main/java");
        metadata.addParameter(path);

        metadata.addRecommendedUseCase("Use antes de repetir investigacoes amplas no mesmo projeto.");
        metadata.addRecommendedUseCase("Use para recuperar branch, frameworks, modulos e patterns previamente descobertos.");
        metadata.addRecommendedUseCase("Use para obter contexto persistente antes de acionar buscas caras.");

        metadata.addGuardrail("Nao trate memoria persistente como verdade absoluta sem reconfirmacao quando a branch mudar.");
        metadata.addGuardrail("Use tipo resumo quando quiser visao compacta.");
        metadata.addGuardrail("Nao despeje historico completo sem necessidade quando um resumo for suficiente.");

        metadata.addJsonExample(
                "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"consultar_memoria_projeto\\\",\\\"parameters\\\":{\\\"tipo\\\":\\\"resumo\\\",\\\"path\\\":\\\"src/main/java\\\"},\\\"explanation\\\":\\\"Preciso consultar a memoria persistente antes de repetir investigacoes no projeto.\\\"}"
        );

        return metadata;
    }

    /** * Le um ou mais arquivos da memoria persistente do projeto. * * @param jsonParameters parametros JSON da ferramenta * @return conteudo consultado ou resumo textual * * @author Renato Tomaz Nati * @since 2026-05-20 */
    @Override
    public String execute(String jsonParameters) {
        File projetoRaiz = resolverProjetoRaiz(jsonParameters);
        if (projetoRaiz == null) {
            return "Erro Operacional: Nao foi possivel resolver a raiz do projeto para consulta de memoria.";
        }

        ProjectMemoryStore memoryStore = new ProjectMemoryStore(projetoRaiz);
        memoryStore.inicializarEstrutura();

        String tipoConsulta = support.extrairValorVariavel(jsonParameters, "tipo");
        if (tipoConsulta == null || tipoConsulta.trim().length() == 0) {
            tipoConsulta = "resumo";
        }

        ProjectMemoryPaths memoryPaths = memoryStore.getMemoryPaths();
        ProjectMemoryJsonSupport jsonSupport = new ProjectMemoryJsonSupport();

        if ("project_memory".equalsIgnoreCase(tipoConsulta)) {
            return jsonSupport.lerJson(memoryPaths.getProjectMemoryFile()).toString();
        }

        if ("tool_history".equalsIgnoreCase(tipoConsulta)) {
            return jsonSupport.lerJson(memoryPaths.getToolHistoryFile()).toString();
        }

        if ("dependency_snapshot".equalsIgnoreCase(tipoConsulta)) {
            return jsonSupport.lerJson(memoryPaths.getDependencySnapshotFile()).toString();
        }

        if ("discovered_patterns".equalsIgnoreCase(tipoConsulta)) {
            return jsonSupport.lerJson(memoryPaths.getDiscoveredPatternsFile()).toString();
        }

        if ("branch_context".equalsIgnoreCase(tipoConsulta)) {
            return jsonSupport.lerJson(memoryPaths.getBranchContextFile()).toString();
        }

        return montarResumoMemoria(memoryPaths, jsonSupport);
    }

    /** * Resolve a raiz efetiva do projeto para leitura da memoria persistente. * * <p>O metodo usa o escopo resolvido do projeto e prioriza: * <ol> * <li>projeto Eclipse mais proximo</li> * <li>modulo Maven mais proximo</li> * <li>raiz segura global</li> * </ol> * </p> * * @param jsonParameters parametros JSON da ferramenta * @return raiz efetiva do projeto * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private File resolverProjetoRaiz(String jsonParameters) {
        String requestedPath = support.extrairValorVariavel(jsonParameters, "path");
        File pontoInicial = support.resolverPontoInicial(rootDirectory, requestedPath);
        ResolvedProjectScope scope = support.resolverEscopoProjeto(pontoInicial, rootDirectory);

        if (scope == null) {
            return null;
        }

        if (scope.getNearestEclipseProjectRoot() != null) {
            return scope.getNearestEclipseProjectRoot();
        }

        if (scope.getNearestMavenModuleRoot() != null) {
            return scope.getNearestMavenModuleRoot();
        }

        return scope.getSafeRoot();
    }

    /** * Monta uma visao resumida da memoria persistente sem despejar tudo. * * @param memoryPaths caminhos da memoria persistente * @param jsonSupport gateway JSON da memoria * @return resumo textual * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String montarResumoMemoria(ProjectMemoryPaths memoryPaths, ProjectMemoryJsonSupport jsonSupport) {
        JsonObject projectMemory = jsonSupport.lerJson(memoryPaths.getProjectMemoryFile());
        JsonObject branchContext = jsonSupport.lerJson(memoryPaths.getBranchContextFile());
        JsonObject dependencySnapshot = jsonSupport.lerJson(memoryPaths.getDependencySnapshotFile());
        JsonObject discoveredPatterns = jsonSupport.lerJson(memoryPaths.getDiscoveredPatternsFile());

        StringBuilder resumo = new StringBuilder();
        resumo.append("Resumo da memoria persistente do projeto").append("\n");
        resumo.append("Diretorio: ").append(memoryPaths.getProjectDirectory().getAbsolutePath()).append("\n");

        if (projectMemory.has("projectRoot")) {
            resumo.append("projectRoot: ").append(projectMemory.get("projectRoot").getAsString()).append("\n");
        }
        if (projectMemory.has("safeRoot")) {
            resumo.append("safeRoot: ").append(projectMemory.get("safeRoot").getAsString()).append("\n");
        }
        if (projectMemory.has("buildTool")) {
            resumo.append("buildTool: ").append(projectMemory.get("buildTool").getAsString()).append("\n");
        }
        if (projectMemory.has("javaVersion")) {
            resumo.append("javaVersion: ").append(projectMemory.get("javaVersion").getAsString()).append("\n");
        }
        if (projectMemory.has("groupId")) {
            resumo.append("groupId: ").append(projectMemory.get("groupId").getAsString()).append("\n");
        }
        if (branchContext.has("currentBranch")) {
            resumo.append("currentBranch: ").append(branchContext.get("currentBranch").getAsString()).append("\n");
        }
        if (branchContext.has("reconfirmSensitiveHints")) {
            resumo.append("reconfirmSensitiveHints: ").append(branchContext.get("reconfirmSensitiveHints").getAsString()).append("\n");
        }
        if (dependencySnapshot.has("modules")) {
            resumo.append("modulesRegistrados: ").append(dependencySnapshot.get("modules").toString()).append("\n");
        }
        if (dependencySnapshot.has("frameworkHints")) {
            resumo.append("frameworkHints: ").append(dependencySnapshot.get("frameworkHints").toString()).append("\n");
        }
        if (discoveredPatterns.has("patterns")) {
            resumo.append("patterns: ").append(discoveredPatterns.get("patterns").toString()).append("\n");
        }
        if (discoveredPatterns.has("patternsAparentes")) {
            resumo.append("patternsAparentes: ").append(discoveredPatterns.get("patternsAparentes").toString()).append("\n");
        }

        return resumo.toString();
    }
}