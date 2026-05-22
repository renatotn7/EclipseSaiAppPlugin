package com.mcp.sailibrary.plugin.agent.tools.mutation.history;

import java.io.File;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.context.mutation.JGitWorkspaceRepository;
import com.mcp.sailibrary.plugin.agent.context.mutation.ProjectMutationStore;
import com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationBatch;
import com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationOperation;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolParameterMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadataProvider;
import com.mcp.sailibrary.plugin.agent.tools.support.StructuralTargetResolver;
import com.mcp.sailibrary.plugin.agent.tools.support.ToolJsonSupport;

/** * Restaura um arquivo especifico do workspace a partir do historico de mutacao * persistido no repositorio interno versionado. * * <p>Esta tool opera de forma cirurgica sobre um unico arquivo, sem acionar * undo ou redo de batch completo. O chamador pode informar o path completo do * arquivo ou combinar target estrutural com relativePath para que o alias seja * resolvido em um caminho real antes da restauracao.</p> * * <p>Modos suportados: * <ul> * <li>before: restaura o estado anterior da operacao selecionada</li> * <li>after: restaura o estado posterior da operacao selecionada</li> * <li>last_safe: prioriza beforeCommitId e faz fallback para afterCommitId</li> * </ul> * </p> * * <p>Esta implementacao nao altera pilhas de undo/redo e nao registra novo * batch semantico. Seu papel e restauracao cirurgica de arquivo a partir do * historico persistido.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class RestoreWorkspaceFileTool implements AgentTool, AgentToolPromptMetadataProvider {

    private final File rootDirectory;
    private final ProjectMutationStore mutationStore;
    private final JGitWorkspaceRepository gitRepository;
    private final StructuralTargetResolver structuralTargetResolver;

    /** * Inicializa a tool de restauracao cirurgica de arquivo do workspace. * * @param rootDirectory raiz segura do projeto atual * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public RestoreWorkspaceFileTool(File rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.mutationStore = new ProjectMutationStore(
                rootDirectory,
                gerarProjectKey(rootDirectory)
        );
        this.gitRepository = new JGitWorkspaceRepository(
                this.mutationStore.getMutationPaths()
        );
        this.structuralTargetResolver = new StructuralTargetResolver(rootDirectory);
    }

    @Override
    public String getName() {
        return "restaurar_arquivo_mutado_workspace";
    }

    @Override
    public String execute(String jsonParameters) {
        String path = ToolJsonSupport.extractJsonStringValue(jsonParameters, "path");
        String target = ToolJsonSupport.extractJsonStringValue(jsonParameters, "target");
        String relativePath = ToolJsonSupport.extractJsonStringValue(jsonParameters, "relativePath");
        String batchId = ToolJsonSupport.extractJsonStringValue(jsonParameters, "batchId");
        String operationId = ToolJsonSupport.extractJsonStringValue(jsonParameters, "operationId");
        String mode = ToolJsonSupport.extractJsonStringValue(jsonParameters, "mode");

        String resolvedRelativePath = resolveEffectiveRelativePath(path, target, relativePath);
        if (isBlank(resolvedRelativePath)) {
            return "Erro Operacional: E necessario informar 'path' ou combinar 'target' com 'relativePath' para restaurar o arquivo.";
        }

        if (rootDirectory == null || !rootDirectory.exists() || !rootDirectory.isDirectory()) {
            return "Erro Operacional: A raiz segura do projeto esta indisponivel para restauracao de arquivo.";
        }

        String normalizedPath = normalizeRelativePath(resolvedRelativePath);
        String normalizedMode = normalizeMode(mode);

        try {
            mutationStore.inicializarEstrutura();
            gitRepository.ensureRepositoryInitialized();

            MutationOperation targetOperation = resolveTargetOperation(normalizedPath, batchId, operationId);
            if (targetOperation == null) {
                return "Erro Operacional: Nenhuma operacao de mutacao compativel foi encontrada para o path informado.";
            }

            String commitId = resolveCommitIdForMode(targetOperation, normalizedMode);
            if (isBlank(commitId)) {
                return "Erro Operacional: A operacao localizada nao possui commit compativel com o modo solicitado.";
            }

            File workspaceTargetFile = new File(rootDirectory, normalizedPath);
            boolean restored = gitRepository.restoreFileFromCommit(commitId, normalizedPath, workspaceTargetFile);

            if (!restored) {
                return "Falha ao restaurar arquivo a partir do repositorio interno.";
            }

            return "Arquivo restaurado com sucesso.\n"
                    + "path: " + normalizedPath + "\n"
                    + "mode: " + normalizedMode + "\n"
                    + "operationId: " + safe(targetOperation.getOperationId()) + "\n"
                    + "batchId: " + safe(targetOperation.getBatchId()) + "\n"
                    + "commitId: " + commitId + "\n"
                    + "workspaceFile: " + normalizePath(workspaceTargetFile);
        } catch (Exception e) {
            return "Falha ao restaurar arquivo do workspace: " + e.getMessage();
        }
    }

    @Override
    public AgentToolPromptMetadata getPromptMetadata() {
        AgentToolPromptMetadata metadata = new AgentToolPromptMetadata();
        metadata.setToolName(getName());
        metadata.setOneLinePurpose("Restaurar cirurgicamente um arquivo mutado do workspace.");
        metadata.setActivityDescription("Restaura um arquivo especifico do workspace a partir do historico persistido no repositorio interno.");

        AgentToolParameterMetadata path = new AgentToolParameterMetadata();
        path.setName("path");
        path.setRequired(false);
        path.setDescription("Caminho relativo completo do arquivo a ser restaurado.");
        path.setExampleValue("src/main/java/com/exemplo/Servico.java");
        metadata.addParameter(path);

        AgentToolParameterMetadata target = new AgentToolParameterMetadata();
        target.setName("target");
        target.setRequired(false);
        target.setDescription("Alias de contexto estrutural usado como base quando path completo nao for informado.");
        target.setExampleValue("batchjob");
        metadata.addParameter(target);

        AgentToolParameterMetadata relativePath = new AgentToolParameterMetadata();
        relativePath.setName("relativePath");
        relativePath.setRequired(false);
        relativePath.setDescription("Caminho relativo do arquivo dentro do contexto estrutural informado em target.");
        relativePath.setExampleValue("AtualizacaoAgendaJobV2.java");
        metadata.addParameter(relativePath);

        AgentToolParameterMetadata batchId = new AgentToolParameterMetadata();
        batchId.setName("batchId");
        batchId.setRequired(false);
        batchId.setDescription("Batch especifico a ser usado na restauracao.");
        batchId.setExampleValue("batch_1716400000000_123");
        metadata.addParameter(batchId);

        AgentToolParameterMetadata operationId = new AgentToolParameterMetadata();
        operationId.setName("operationId");
        operationId.setRequired(false);
        operationId.setDescription("Operacao especifica a ser usada na restauracao.");
        operationId.setExampleValue("op_1716400000000_999");
        metadata.addParameter(operationId);

        AgentToolParameterMetadata mode = new AgentToolParameterMetadata();
        mode.setName("mode");
        mode.setRequired(false);
        mode.setDescription("Modo de restauracao: before, after ou last_safe.");
        mode.setExampleValue("last_safe");
        metadata.addParameter(mode);

        metadata.addRecommendedUseCase("Use quando precisar restaurar um unico arquivo sem desfazer um batch inteiro.");
        metadata.addRecommendedUseCase("Use quando operationId ou batchId ja estiverem claros no historico.");
        metadata.addRecommendedUseCase("Use para restauracao cirurgica antes de acionar undo de lote completo.");

        metadata.addGuardrail("Informe path completo ou target + relativePath.");
        metadata.addGuardrail("Prefira operationId ou batchId quando houver varias mutacoes sobre o mesmo arquivo.");
        metadata.addGuardrail("Use mode last_safe quando quiser fallback prudente para o estado mais seguro conhecido.");

        metadata.addJsonExample(
                "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"restaurar_arquivo_mutado_workspace\\\",\\\"parameters\\\":{\\\"target\\\":\\\"batchjob\\\",\\\"relativePath\\\":\\\"AtualizacaoAgendaJobV2.java\\\",\\\"mode\\\":\\\"last_safe\\\"},\\\"explanation\\\":\\\"Preciso restaurar cirurgicamente este arquivo para um estado seguro anterior.\\\"}"
        );

        return metadata;
    }

    /** * Resolve a operacao alvo com base em operationId, batchId e path. * * <p>A prioridade de resolucao e: * <ol> * <li>operationId explicito</li> * <li>batchId + path</li> * <li>path no historico global</li> * </ol> * </p> * * @param relativePath caminho relativo do arquivo alvo * @param batchId identificador opcional de batch * @param operationId identificador opcional de operacao * @return operacao compativel mais adequada ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private MutationOperation resolveTargetOperation(String relativePath, String batchId, String operationId) {
        if (!isBlank(operationId)) {
            MutationOperation byOperationId = findOperationById(operationId);
            if (byOperationId != null && operationMatchesPath(byOperationId, relativePath)) {
                return byOperationId;
            }
        }

        if (!isBlank(batchId)) {
            MutationBatch batch = mutationStore.buscarBatchPorId(batchId);
            if (batch != null && batch.getOperations() != null) {
                List<MutationOperation> operations = new ArrayList<MutationOperation>(batch.getOperations());
                ordenarOperacoesMaisRecentesPrimeiro(operations);

                for (int i = 0; i < operations.size(); i++) {
                    MutationOperation current = operations.get(i);
                    if (operationMatchesPath(current, relativePath)) {
                        return current;
                    }
                }
            }
        }

        List<MutationBatch> batches = mutationStore.listarBatches();
        ordenarBatchesMaisRecentesPrimeiro(batches);

        for (int i = 0; i < batches.size(); i++) {
            MutationBatch batch = batches.get(i);
            if (batch == null || batch.getOperations() == null) {
                continue;
            }

            List<MutationOperation> operations = new ArrayList<MutationOperation>(batch.getOperations());
            ordenarOperacoesMaisRecentesPrimeiro(operations);

            for (int j = 0; j < operations.size(); j++) {
                MutationOperation current = operations.get(j);
                if (operationMatchesPath(current, relativePath)) {
                    return current;
                }
            }
        }

        return null;
    }

    /** * Localiza uma operacao especifica em todos os batches conhecidos. * * @param operationId identificador procurado * @return operacao encontrada ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private MutationOperation findOperationById(String operationId) {
        if (isBlank(operationId)) {
            return null;
        }

        List<MutationBatch> batches = mutationStore.listarBatches();
        for (int i = 0; i < batches.size(); i++) {
            MutationBatch batch = batches.get(i);
            if (batch == null || batch.getOperations() == null) {
                continue;
            }

            for (int j = 0; j < batch.getOperations().size(); j++) {
                MutationOperation operation = batch.getOperations().get(j);
                if (operation != null && safe(operationId).equals(safe(operation.getOperationId()))) {
                    return operation;
                }
            }
        }

        return null;
    }

    /** * Resolve o commit a ser usado na restauracao com base no modo solicitado. * * @param operation operacao selecionada * @param mode modo de restauracao * @return commit id compativel ou string vazia * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String resolveCommitIdForMode(MutationOperation operation, String mode) {
        if (operation == null) {
            return "";
        }

        if ("before".equals(mode)) {
            return safe(operation.getBeforeCommitId());
        }

        if ("after".equals(mode)) {
            return safe(operation.getAfterCommitId());
        }

        if ("last_safe".equals(mode)) {
            if (!isBlank(operation.getBeforeCommitId())) {
                return safe(operation.getBeforeCommitId());
            }
            return safe(operation.getAfterCommitId());
        }

        return "";
    }

    /** * Resolve o caminho relativo efetivo do arquivo a restaurar. * * <p>A prioridade e: * <ol> * <li>path explicito</li> * <li>target estrutural + relativePath</li> * </ol> * </p> * * @param path caminho relativo completo * @param target alias estrutural * @param relativePath caminho relativo dentro do alias estrutural * @return caminho relativo final ou string vazia * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String resolveEffectiveRelativePath(String path, String target, String relativePath) {
        if (!isBlank(path)) {
            return normalizeRelativePath(path);
        }

        if (isBlank(target) || isBlank(relativePath)) {
            return "";
        }

        String baseRelativePath = structuralTargetResolver.resolveRelativePath(target);
        return joinRelativePath(baseRelativePath, relativePath);
    }

    /** * Une caminho relativo base e caminho relativo filho em um unico path * normalizado. * * @param baseRelativePath base relativa * @param childRelativePath filho relativo * @return caminho relativo final * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String joinRelativePath(String baseRelativePath, String childRelativePath) {
        String base = baseRelativePath != null ? baseRelativePath.trim().replace("\\", "/") : "";
        String child = childRelativePath != null ? childRelativePath.trim().replace("\\", "/") : "";

        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        while (child.startsWith("/")) {
            child = child.substring(1);
        }

        if (base.length() == 0) {
            return child;
        }
        if (child.length() == 0) {
            return base;
        }

        return base + "/" + child;
    }

    /** * Retorna true quando a operacao corresponde ao path informado. * * @param operation operacao a validar * @param relativePath caminho relativo alvo * @return true quando houver correspondencia de caminho * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean operationMatchesPath(MutationOperation operation, String relativePath) {
        if (operation == null) {
            return false;
        }

        String operationRelativePath = normalizeRelativePath(operation.getRelativePath());
        return safe(operationRelativePath).equals(safe(relativePath));
    }

    /** * Ordena batches do mais recente para o mais antigo. * * @param batches lista de batches a ordenar * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void ordenarBatchesMaisRecentesPrimeiro(List<MutationBatch> batches) {
        Collections.sort(batches, new Comparator<MutationBatch>() {
            @Override
            public int compare(MutationBatch a, MutationBatch b) {
                return Long.compare(b.getStartedAt(), a.getStartedAt());
            }
        });
    }

    /** * Ordena operacoes do mais recente para o mais antigo. * * @param operations lista de operacoes a ordenar * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void ordenarOperacoesMaisRecentesPrimeiro(List<MutationOperation> operations) {
        Collections.sort(operations, new Comparator<MutationOperation>() {
            @Override
            public int compare(MutationOperation a, MutationOperation b) {
                return Long.compare(b.getCreatedAt(), a.getCreatedAt());
            }
        });
    }

    /** * Normaliza o modo de restauracao com fallback seguro. * * @param mode modo original informado pelo chamador * @return modo normalizado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String normalizeMode(String mode) {
        if (isBlank(mode)) {
            return "last_safe";
        }

        String normalized = mode.trim().toLowerCase();
        if ("before".equals(normalized) || "after".equals(normalized) || "last_safe".equals(normalized)) {
            return normalized;
        }

        return "last_safe";
    }

    /** * Normaliza um caminho relativo para o formato com barras normais. * * @param relativePath caminho relativo original * @return caminho relativo normalizado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String normalizeRelativePath(String relativePath) {
        if (relativePath == null) {
            return "";
        }

        String normalized = relativePath.trim().replace("\\", "/");
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    /** * Gera a chave estavel do projeto com base na raiz fisica informada. * * @param rootDirectory raiz fisica do projeto * @return chave estavel do projeto * * @author Renato Tomaz Nati * @since 2026-05-20 */
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