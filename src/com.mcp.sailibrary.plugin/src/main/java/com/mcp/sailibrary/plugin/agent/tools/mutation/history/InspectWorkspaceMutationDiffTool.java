package com.mcp.sailibrary.plugin.agent.tools.mutation.history;

import java.io.File;
import java.security.MessageDigest;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.context.mutation.JGitWorkspaceRepository;
import com.mcp.sailibrary.plugin.agent.context.mutation.ProjectMutationStore;
import com.mcp.sailibrary.plugin.agent.context.mutation.diff.WorkspaceMutationDiffService;
import com.mcp.sailibrary.plugin.agent.tools.support.ToolJsonSupport;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolParameterMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadataProvider;
/** * Inspeciona o diff de um arquivo mutado no workspace a partir dos estados * persistidos na infraestrutura interna de mutacao. * * <p>Esta tool e somente leitura e foi desenhada para apoiar a IA em tarefas * como: * <ul> * <li>explicar o que mudou em um arquivo</li> * <li>avaliar se vale desfazer ou refazer uma mutacao</li> * <li>comparar before, after e current de uma operacao</li> * <li>medir a magnitude aproximada de uma mudanca</li> * </ul> * </p> * * <p>Ela nao altera o workspace real, nao grava no journal e nao movimenta as * pilhas de undo/redo.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class InspectWorkspaceMutationDiffTool implements AgentTool, AgentToolPromptMetadataProvider {

    private final File rootDirectory;
    private final ProjectMutationStore mutationStore;
    private final JGitWorkspaceRepository gitRepository;
    private final WorkspaceMutationDiffService diffService;

    /** * Inicializa a tool de inspecao de diff da camada de mutacao. * * @param rootDirectory raiz segura do projeto atual * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public InspectWorkspaceMutationDiffTool(File rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.mutationStore = new ProjectMutationStore(
                rootDirectory,
                gerarProjectKey(rootDirectory)
        );
        this.gitRepository = new JGitWorkspaceRepository(
                this.mutationStore.getMutationPaths()
        );
        this.diffService = new WorkspaceMutationDiffService(
                rootDirectory,
                this.mutationStore,
                this.gitRepository
        );
    }

    @Override
    public String getName() {
        return "inspecionar_diff_mutacao_workspace";
    }
    @Override
    public AgentToolPromptMetadata getPromptMetadata() {
        AgentToolPromptMetadata metadata = new AgentToolPromptMetadata();
        metadata.setToolName(getName());
        metadata.setOneLinePurpose("Inspecionar o diff de um arquivo mutado no workspace.");
        metadata.setActivityDescription("Inspeciona diff de mutacao do workspace com comparacao entre before, after e current.");

        AgentToolParameterMetadata path = new AgentToolParameterMetadata();
        path.setName("path");
        path.setRequired(true);
        path.setDescription("Caminho relativo do arquivo alvo.");
        path.setExampleValue("src/main/java/com/exemplo/Servico.java");
        metadata.addParameter(path);

        AgentToolParameterMetadata batchId = new AgentToolParameterMetadata();
        batchId.setName("batchId");
        batchId.setRequired(false);
        batchId.setDescription("Batch especifico a ser usado na comparacao.");
        batchId.setExampleValue("batch_1716400000000_123");
        metadata.addParameter(batchId);

        AgentToolParameterMetadata operationId = new AgentToolParameterMetadata();
        operationId.setName("operationId");
        operationId.setRequired(false);
        operationId.setDescription("Operacao especifica a ser usada na comparacao.");
        operationId.setExampleValue("op_1716400000000_999");
        metadata.addParameter(operationId);

        AgentToolParameterMetadata mode = new AgentToolParameterMetadata();
        mode.setName("mode");
        mode.setRequired(false);
        mode.setDescription("Modo de comparacao, como before_after, current_before ou current_after.");
        mode.setExampleValue("before_after");
        metadata.addParameter(mode);

        AgentToolParameterMetadata maxLines = new AgentToolParameterMetadata();
        maxLines.setName("maxLines");
        maxLines.setRequired(false);
        maxLines.setDescription("Quantidade maxima de linhas exibidas no trecho diff.");
        maxLines.setExampleValue("80");
        metadata.addParameter(maxLines);

        metadata.addRecommendedUseCase("Use quando precisar explicar o que mudou em um arquivo.");
        metadata.addRecommendedUseCase("Use antes de decidir por undo, redo ou restore cirurgico.");
        metadata.addRecommendedUseCase("Use para comparar before, after e current de uma mutacao.");

        metadata.addGuardrail("Nao use diff textual gigante sem necessidade.");
        metadata.addGuardrail("Prefira operationId ou batchId quando o historico for grande.");
        metadata.addGuardrail("Use modo before_after para leitura rapida da mutacao original.");

        metadata.addJsonExample(
                "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"inspecionar_diff_mutacao_workspace\\\",\\\"parameters\\\":{\\\"path\\\":\\\"src/main/java/com/exemplo/Servico.java\\\",\\\"mode\\\":\\\"before_after\\\",\\\"maxLines\\\":\\\"80\\\"},\\\"explanation\\\":\\\"Preciso inspecionar o diff do arquivo antes de decidir restauracao ou manter a mudanca.\\\"}"
        );

        return metadata;
    }
    @Override
    public String execute(String jsonParameters) {
        String relativePath = ToolJsonSupport.extractJsonStringValue(jsonParameters, "path");
        String batchId = ToolJsonSupport.extractJsonStringValue(jsonParameters, "batchId");
        String operationId = ToolJsonSupport.extractJsonStringValue(jsonParameters, "operationId");
        String mode = ToolJsonSupport.extractJsonStringValue(jsonParameters, "mode");
        int maxLines = ToolJsonSupport.extractJsonIntValue(jsonParameters, "maxLines", 80, 10, 400);

        if (rootDirectory == null || !rootDirectory.exists() || !rootDirectory.isDirectory()) {
            return "Erro Operacional: A raiz segura do projeto esta indisponivel para inspecao de diff de mutacao.";
        }

        try {
            mutationStore.inicializarEstrutura();
            gitRepository.ensureRepositoryInitialized();

            return diffService.inspectDiff(
                    relativePath,
                    batchId,
                    operationId,
                    mode,
                    maxLines
            );
        } catch (Exception e) {
            return "Falha ao inspecionar diff de mutacao do workspace: " + e.getMessage();
        }
    }

    /** * Gera a chave estavel do projeto com base na raiz fisica informada. * * <p>O formato segue a mesma estrategia defensiva usada na camada de * memoria persistente e mutacao, preservando nome base normalizado e hash * curto da raiz canonica.</p> * * @param rootDirectory raiz fisica do projeto * @return chave estavel do projeto * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String gerarProjectKey(File rootDirectory) {
        if (rootDirectory == null) {
            return "unknown_project";
        }

        try {
            String canonicalPath = rootDirectory.getCanonicalPath();
            String baseName = rootDirectory.getName();
            String hash = gerarHashCurto(canonicalPath);
            return normalizarNome(baseName) + "_" + hash;
        } catch (Exception e) {
            return normalizarNome(rootDirectory.getName()) + "_fallback";
        }
    }

    /** * Gera hash curto deterministico para a raiz canonica do projeto. * * @param value valor base para hash * @return hash curto em hexadecimal * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String gerarHashCurto(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] digest = messageDigest.digest(value.getBytes("UTF-8"));

            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < digest.length && builder.length() < 8; i++) {
                String hex = Integer.toHexString(digest[i] & 0xff);
                if (hex.length() == 1) {
                    builder.append("0");
                }
                builder.append(hex);
            }

            return builder.toString();
        } catch (Exception e) {
            return "hashfail";
        }
    }

    /** * Normaliza nome de projeto para uso seguro em identificadores internos. * * @param name nome base do projeto * @return nome normalizado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String normalizarNome(String name) {
        if (name == null || name.trim().length() == 0) {
            return "project";
        }

        String normalized = name.toLowerCase();
        normalized = normalized.replaceAll("[^a-z0-9_\\-]", "_");
        return normalized;
    }
}