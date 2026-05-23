package com.mcp.sailibrary.plugin.agent.tools.memory;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.context.ResolvedProjectScope;
import com.mcp.sailibrary.plugin.agent.context.SourceInsightSupport;
import com.mcp.sailibrary.plugin.agent.context.analise.ProjectMemoryStore;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolParameterMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadataProvider;

/** * Expor gravacao controlada da memoria persistente do projeto para a camada de * agentes. * * <p>Esta ferramenta foi ajustada para resolver de forma mais estavel o * diretorio efetivo do projeto, reduzindo risco de fragmentacao de memoria em * cenarios Maven multimodulo e multiplos `.project`.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class ProjectMemoryWriteTool implements AgentTool, AgentToolPromptMetadataProvider {

    private File rootDirectory;
    private SourceInsightSupport support;

    /** * Inicializa a ferramenta de gravacao da memoria persistente do projeto. * * @param rootDirectory raiz segura do projeto atual * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public ProjectMemoryWriteTool(File rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.support = new SourceInsightSupport();
    }

    @Override
    public String getName() {
        return "registrar_memoria_projeto";
    }

    @Override
    public AgentToolPromptMetadata getPromptMetadata() {
        AgentToolPromptMetadata metadata = new AgentToolPromptMetadata();
        metadata.setToolName(getName());
        metadata.setOneLinePurpose("Registrar conhecimento estrutural reutilizavel na memoria persistente do projeto.");
        metadata.setActivityDescription("Grava padroes arquiteturais confirmados na memoria persistente.");

        AgentToolParameterMetadata modo = new AgentToolParameterMetadata();
        modo.setName("modo");
        modo.setRequired(false);
        modo.setDescription("Modo de registro, como pattern, project_memory, dependency_snapshot ou tool_history.");
        modo.setExampleValue("pattern");
        metadata.addParameter(modo);

        AgentToolParameterMetadata path = new AgentToolParameterMetadata();
        path.setName("path");
        path.setRequired(false);
        path.setDescription("Caminho relativo usado para resolver o escopo efetivo do projeto.");
        path.setExampleValue("src/main/java");
        metadata.addParameter(path);

        AgentToolParameterMetadata kind = new AgentToolParameterMetadata();
        kind.setName("kind");
        kind.setRequired(false);
        kind.setDescription("Categoria do pattern a registrar.");
        kind.setExampleValue("framework");
        metadata.addParameter(kind);

        AgentToolParameterMetadata key = new AgentToolParameterMetadata();
        key.setName("key");
        key.setRequired(false);
        key.setDescription("Chave principal do pattern ou dado persistido.");
        key.setExampleValue("hibernate");
        metadata.addParameter(key);

        AgentToolParameterMetadata value = new AgentToolParameterMetadata();
        value.setName("value");
        value.setRequired(false);
        value.setDescription("Valor associado a chave persistida.");
        value.setExampleValue("detectado");
        metadata.addParameter(value);

        AgentToolParameterMetadata evidence = new AgentToolParameterMetadata();
        evidence.setName("evidence");
        evidence.setRequired(false);
        evidence.setDescription("Evidencia resumida que justificou o registro.");
        evidence.setExampleValue("pom.xml");
        metadata.addParameter(evidence);

        AgentToolParameterMetadata confidence = new AgentToolParameterMetadata();
        confidence.setName("confidence");
        confidence.setRequired(false);
        confidence.setDescription("Nivel de confianca do registro, como alta, baixa ou confirmado.");
        confidence.setExampleValue("alta");
        metadata.addParameter(confidence);

        metadata.addRecommendedUseCase("Use quando encontrar padrao estrutural estavel e reutilizavel.");
        metadata.addRecommendedUseCase("Use para registrar memoria de projeto, snapshot de dependencias ou historico compacto.");
        metadata.addRecommendedUseCase("Use apenas para conhecimento duravel, nao para trechos grandes de codigo.");

        metadata.addGuardrail("Nao registre selecoes do editor nem conteudo integral de arquivos.");
        metadata.addGuardrail("Prefira observacoes estruturais e duraveis entre branches.");
        metadata.addGuardrail("Use pattern para conhecimento reutilizavel e project_memory para metadados estruturais do projeto.");

        metadata.addJsonExample(
                "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"registrar_memoria_projeto\\\",\\\"parameters\\\":{\\\"modo\\\":\\\"pattern\\\",\\\"path\\\":\\\"src/main/java\\\",\\\"kind\\\":\\\"framework\\\",\\\"key\\\":\\\"hibernate\\\",\\\"value\\\":\\\"detectado\\\",\\\"evidence\\\":\\\"pom.xml\\\",\\\"confidence\\\":\\\"alta\\\"},\\\"explanation\\\":\\\"Preciso registrar um padrao estrutural confirmado para reutilizacao futura.\\\"}"
        );

        return metadata;
    }

    /** * Registra hints genericos, snapshots e historico de uso do projeto. * * @param jsonParameters parametros JSON da ferramenta * @return resultado textual do registro * * @author Renato Tomaz Nati * @since 2026-05-20 */
    @Override
    public String execute(String jsonParameters) {
        File projetoRaiz = resolverProjetoRaiz(jsonParameters);
        if (projetoRaiz == null) {
            return "Erro Operacional: Nao foi possivel resolver a raiz do projeto para registro de memoria.";
        }

        ProjectMemoryStore memoryStore = new ProjectMemoryStore(projetoRaiz);
        memoryStore.inicializarEstrutura();
        memoryStore.atualizarBranchContexto();

        String modo = support.extrairValorVariavel(jsonParameters, "modo");
        if (modo == null || modo.trim().length() == 0) {
            modo = "pattern";
        }

        if ("pattern".equalsIgnoreCase(modo)) {
            return registrarPattern(jsonParameters, memoryStore);
        }

        if ("project_memory".equalsIgnoreCase(modo)) {
            return registrarProjectMemory(jsonParameters, memoryStore);
        }

        if ("dependency_snapshot".equalsIgnoreCase(modo)) {
            return registrarDependencySnapshot(jsonParameters, memoryStore);
        }

        if ("tool_history".equalsIgnoreCase(modo)) {
            return registrarToolHistory(jsonParameters, memoryStore);
        }

        return "Erro Operacional: Modo de registro nao suportado. Valores aceitos: pattern, project_memory, dependency_snapshot, tool_history.";
    }

    /** * Resolve a raiz efetiva do projeto para gravacao local persistente. * * <p>O metodo usa o escopo resolvido do projeto e prioriza: * <ol> * <li>projeto Eclipse mais proximo</li> * <li>modulo Maven mais proximo</li> * <li>raiz segura global</li> * </ol> * </p> * * @param jsonParameters parametros JSON da ferramenta * @return raiz efetiva do projeto * * @author Renato Tomaz Nati * @since 2026-05-20 */
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

    /** * Registra um padrao generico deduplicado e reutilizavel entre branches. * * @param jsonParameters parametros JSON * @param memoryStore store de memoria persistente * @return mensagem de resultado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String registrarPattern(String jsonParameters, ProjectMemoryStore memoryStore) {
        String kind = support.extrairValorVariavel(jsonParameters, "kind");
        String key = support.extrairValorVariavel(jsonParameters, "key");
        String value = support.extrairValorVariavel(jsonParameters, "value");
        String evidence = support.extrairValorVariavel(jsonParameters, "evidence");
        String confidence = support.extrairValorVariavel(jsonParameters, "confidence");

        if (key == null || key.trim().length() == 0) {
            return "Erro Operacional: O parametro 'key' e obrigatorio para registrar pattern.";
        }

        memoryStore.registrarPattern(kind, key, value, evidence, confidence);
        return "Pattern registrado com sucesso para a chave [" + key + "].";
    }

    /** * Registra memoria estrutural estavel do projeto. * * @param jsonParameters parametros JSON * @param memoryStore store de memoria persistente * @return mensagem de resultado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String registrarProjectMemory(String jsonParameters, ProjectMemoryStore memoryStore) {
        String safeRoot = support.extrairValorVariavel(jsonParameters, "safeRoot");
        String buildTool = support.extrairValorVariavel(jsonParameters, "buildTool");
        String javaVersion = support.extrairValorVariavel(jsonParameters, "javaVersion");
        String groupId = support.extrairValorVariavel(jsonParameters, "groupId");

        memoryStore.registrarProjectMemory(safeRoot, buildTool, javaVersion, groupId);
        return "Memoria estrutural do projeto atualizada com sucesso.";
    }

    /** * Registra snapshot simples de dependencias, frameworks e modulos. * * @param jsonParameters parametros JSON * @param memoryStore store de memoria persistente * @return mensagem de resultado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String registrarDependencySnapshot(String jsonParameters, ProjectMemoryStore memoryStore) {
        JsonArray dependencies = extrairArraySimples(jsonParameters, "dependencies");
        JsonArray frameworks = extrairArraySimples(jsonParameters, "frameworks");
        JsonArray modules = extrairArraySimples(jsonParameters, "modules");

        memoryStore.registrarDependencySnapshot(dependencies, frameworks, modules);
        return "Snapshot de dependencias registrado com sucesso.";
    }

    /** * Registra historico compacto da ultima execucao de ferramenta. * * @param jsonParameters parametros JSON * @param memoryStore store de memoria persistente * @return mensagem de resultado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String registrarToolHistory(String jsonParameters, ProjectMemoryStore memoryStore) {
        String toolName = support.extrairValorVariavel(jsonParameters, "tool");
        String parametersSummary = support.extrairValorVariavel(jsonParameters, "parametersSummary");
        String resultSummary = support.extrairValorVariavel(jsonParameters, "resultSummary");

        if (toolName == null || toolName.trim().length() == 0) {
            return "Erro Operacional: O parametro 'tool' e obrigatorio para registrar historico.";
        }

        memoryStore.registrarToolHistory(toolName, parametersSummary, resultSummary);
        return "Historico da ferramenta [" + toolName + "] registrado com sucesso.";
    }

    /** * Extrai array simples do JSON recebido, com fallback para array vazio. * * @param jsonParameters texto JSON * @param chave chave do array * @return array encontrado ou vazio * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private JsonArray extrairArraySimples(String jsonParameters, String chave) {
        JsonArray array = new JsonArray();

        try {
            JsonObject jsonObject = com.google.gson.JsonParser.parseString(jsonParameters).getAsJsonObject();
            if (jsonObject.has(chave) && jsonObject.get(chave).isJsonArray()) {
                return jsonObject.getAsJsonArray(chave);
            }
        } catch (Exception e) {
        }

        return array;
    }
}