package com.mcp.sailibrary.plugin.agent.tools.mutation.history;

import java.io.File;
import java.security.MessageDigest;
import java.util.List;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.context.mutation.JGitWorkspaceRepository;
import com.mcp.sailibrary.plugin.agent.context.mutation.ProjectMutationStore;
import com.mcp.sailibrary.plugin.agent.tools.support.ToolJsonSupport;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolParameterMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadataProvider;
/** * Inspeciona o estado atual da infraestrutura de mutacao do workspace para o * projeto ativo. * * <p>Esta tool fornece uma visao tática e somente leitura sobre o estado do * journal de mutacoes, pilhas de undo/redo e repositorio interno versionado. * O objetivo e permitir que a IA avalie se ha historico suficiente e se o * ambiente esta consistente antes de tentar undo, redo ou restauracoes * seletivas.</p> * * <p>Nenhuma alteracao e aplicada no workspace real, no espelho interno ou no * journal semantico. Esta tool apenas coleta e formata o estado atual.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class InspectWorkspaceMutationStateTool implements AgentTool, AgentToolPromptMetadataProvider {

    private final File rootDirectory;
    private final ProjectMutationStore mutationStore;
    private final JGitWorkspaceRepository gitRepository;

    /** * Inicializa a tool de inspecao do estado da mutacao do workspace. * * @param rootDirectory raiz segura do projeto atual * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public InspectWorkspaceMutationStateTool(File rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.mutationStore = new ProjectMutationStore(
                rootDirectory,
                gerarProjectKey(rootDirectory)
        );
        this.gitRepository = new JGitWorkspaceRepository(
                this.mutationStore.getMutationPaths()
        );
    }

    @Override
    public String getName() {
        return "inspecionar_estado_mutacao_workspace";
    }
    @Override
    public AgentToolPromptMetadata getPromptMetadata() {
        AgentToolPromptMetadata metadata = new AgentToolPromptMetadata();
        metadata.setToolName(getName());
        metadata.setOneLinePurpose("Inspecionar o estado atual da infraestrutura de mutacao do workspace.");
        metadata.setActivityDescription("Inspeciona o estado da mutacao do workspace, incluindo repo interno, pilhas de undo/redo e batches.");

        AgentToolParameterMetadata recentCommitLimit = new AgentToolParameterMetadata();
        recentCommitLimit.setName("recentCommitLimit");
        recentCommitLimit.setRequired(false);
        recentCommitLimit.setDescription("Quantidade maxima de commits recentes a considerar na leitura de estado.");
        recentCommitLimit.setExampleValue("5");
        metadata.addParameter(recentCommitLimit);

        metadata.addRecommendedUseCase("Use antes de tentar undo ou redo.");
        metadata.addRecommendedUseCase("Use para verificar se existe historico persistido suficiente.");
        metadata.addRecommendedUseCase("Use para detectar divergencia simples de branch antes de restaurar.");

        metadata.addGuardrail("Esta ferramenta e somente leitura.");
        metadata.addGuardrail("Use-a como triagem antes de restore ou mutacao historica.");
        metadata.addGuardrail("Nao trate branch interna e branch do projeto como equivalentes sem validacao contextual.");

        metadata.addJsonExample(
                "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"inspecionar_estado_mutacao_workspace\\\",\\\"parameters\\\":{\\\"recentCommitLimit\\\":\\\"5\\\"},\\\"explanation\\\":\\\"Preciso inspecionar o estado da infraestrutura de mutacao antes de decidir undo, redo ou restore.\\\"}"
        );

        return metadata;
    }
    @Override
    public String execute(String jsonParameters) {
        int recentCommitLimit = ToolJsonSupport.extractJsonIntValue(jsonParameters, "recentCommitLimit", 5, 1, 50);

        if (rootDirectory == null || !rootDirectory.exists() || !rootDirectory.isDirectory()) {
            return "Erro Operacional: A raiz segura do projeto esta indisponivel para inspecao do estado de mutacao.";
        }

        try {
            mutationStore.inicializarEstrutura();

            boolean projectStructureExists = mutationStore.getMutationPaths().existsProjectStructure();
            boolean workspaceGitDirectoryExists = mutationStore.getMutationPaths().existsWorkspaceGitDirectory();
            boolean workspaceGitMetadataExists = mutationStore.getMutationPaths().existsWorkspaceGitMetadataDirectory();

            String currentProjectBranch = detectarBranchAtual(rootDirectory);
            String currentInternalBranch = "";
            boolean repoInitialized = false;
            boolean hasCommits = false;
            int recentCommitCount = 0;

            try {
                if (workspaceGitMetadataExists) {
                    repoInitialized = true;
                    currentInternalBranch = gitRepository.getCurrentBranch();
                    hasCommits = gitRepository.hasCommits();
                    List<String> recentCommitIds = gitRepository.listRecentCommitIds(recentCommitLimit);
                    recentCommitCount = recentCommitIds != null ? recentCommitIds.size() : 0;
                }
            } catch (Exception e) {
                currentInternalBranch = "";
            }

            String lastUndoBatchId = mutationStore.peekUndoBatchId();
            String lastRedoBatchId = mutationStore.peekRedoBatchId();

            boolean undoAvailable = !isBlank(lastUndoBatchId);
            boolean redoAvailable = !isBlank(lastRedoBatchId);

            int totalBatches = 0;
            try {
                List<com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationBatch> batches = mutationStore.listarBatches();
                totalBatches = batches != null ? batches.size() : 0;
            } catch (Exception e) {
                totalBatches = 0;
            }

            boolean branchDivergence = false;
            if (!isBlank(currentProjectBranch) && !isBlank(currentInternalBranch)) {
                branchDivergence = !currentProjectBranch.equals(currentInternalBranch);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Estado da Mutacao do Workspace").append("\n");
            sb.append("projectRoot: ").append(normalizePath(rootDirectory)).append("\n");
            sb.append("projectKey: ").append(gerarProjectKey(rootDirectory)).append("\n");
            sb.append("projectStructureExists: ").append(projectStructureExists ? "true" : "false").append("\n");
            sb.append("workspaceGitDirectoryExists: ").append(workspaceGitDirectoryExists ? "true" : "false").append("\n");
            sb.append("workspaceGitMetadataExists: ").append(workspaceGitMetadataExists ? "true" : "false").append("\n");
            sb.append("repoInitialized: ").append(repoInitialized ? "true" : "false").append("\n");
            sb.append("currentProjectBranch: ").append(safe(currentProjectBranch)).append("\n");
            sb.append("currentInternalBranch: ").append(safe(currentInternalBranch)).append("\n");
            sb.append("branchDivergence: ").append(branchDivergence ? "true" : "false").append("\n");
            sb.append("undoAvailable: ").append(undoAvailable ? "true" : "false").append("\n");
            sb.append("redoAvailable: ").append(redoAvailable ? "true" : "false").append("\n");
            sb.append("lastUndoBatchId: ").append(safe(lastUndoBatchId)).append("\n");
            sb.append("lastRedoBatchId: ").append(safe(lastRedoBatchId)).append("\n");
            sb.append("totalBatches: ").append(totalBatches).append("\n");
            sb.append("hasCommits: ").append(hasCommits ? "true" : "false").append("\n");
            sb.append("recentCommitCount: ").append(recentCommitCount).append("\n");
            sb.append("mutationJournalFile: ").append(normalizePath(mutationStore.getMutationJournalFile())).append("\n");
            sb.append("mutationStateFile: ").append(normalizePath(mutationStore.getMutationStateFile())).append("\n");
            sb.append("mutationRepoMetaFile: ").append(normalizePath(mutationStore.getMutationRepoMetaFile())).append("\n");
            sb.append("workspaceGitDirectory: ").append(normalizePath(mutationStore.getMutationPaths().getWorkspaceGitDirectory())).append("\n");

            if (!undoAvailable && !redoAvailable && totalBatches == 0) {
                sb.append("\n");
                sb.append("Conclusao tatica: a infraestrutura existe, mas ainda nao ha historico util de mutacao para undo ou redo.");
            } else {
                sb.append("\n");
                sb.append("Conclusao tatica: ha sinais de historico persistido. Avalie branch, pilhas de undo/redo e batches antes de restaurar.");
            }

            return sb.toString();
        } catch (Exception e) {
            return "Falha ao inspecionar estado da mutacao do workspace: " + e.getMessage();
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

    /** * Detecta a branch atual do projeto a partir do arquivo .git/HEAD, quando * disponivel. * * @param projectRoot raiz fisica do projeto * @return nome da branch atual ou string vazia * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String detectarBranchAtual(File projectRoot) {
        if (projectRoot == null) {
            return "";
        }

        try {
            File gitHead = new File(projectRoot, ".git/HEAD");
            if (!gitHead.exists() || !gitHead.isFile()) {
                return "";
            }

            java.util.List<String> lines = java.nio.file.Files.readAllLines(
                    gitHead.toPath(),
                    java.nio.charset.StandardCharsets.UTF_8
            );

            if (lines.isEmpty()) {
                return "";
            }

            String line = lines.get(0) != null ? lines.get(0).trim() : "";
            String prefix = "ref: refs/heads/";
            if (line.startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }

            return line;
        } catch (Exception e) {
            return "";
        }
    }

    /** * Normaliza caminho fisico para formato com barras normais. * * @param file arquivo de origem * @return caminho normalizado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String normalizePath(File file) {
        if (file == null) {
            return "";
        }

        try {
            return file.getCanonicalPath().replace("\\", "/");
        } catch (Exception e) {
            return file.getAbsolutePath().replace("\\", "/");
        }
    }

    /** * Retorna string segura nao nula. * * @param value valor original * @return string segura * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    /** * Retorna true quando o valor informado for nulo ou vazio. * * @param value valor a validar * @return true quando o valor for branco * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}