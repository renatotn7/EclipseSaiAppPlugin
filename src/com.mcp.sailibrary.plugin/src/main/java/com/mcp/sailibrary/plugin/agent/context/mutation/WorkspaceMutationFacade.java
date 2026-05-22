package com.mcp.sailibrary.plugin.agent.context.mutation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.gson.JsonObject;
import com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationActionType;
import com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationBatch;
import com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationContext;
import com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationOperation;
import com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationOperationStatus;
import com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationOrigin;
import com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationTargetScope;
import com.mcp.sailibrary.plugin.chat.context.service.SafeWorkspaceMutationPolicy;

/** * Fachada central de mutacao segura do workspace. * * <p>Esta classe concentra o pipeline de mutacao da IA/plugin: * <ol> * <li>validar politica de mutacao</li> * <li>registrar batch e operacao semantica</li> * <li>espelhar estado before no repositorio interno</li> * <li>aplicar a mutacao real no workspace</li> * <li>espelhar estado after</li> * <li>persistir journal e pilhas de undo/redo</li> * </ol> * </p> * * <p>O objetivo principal e impedir que tools mutaveis espalhem logica de * escrita em disco, backup, journal, espelho e versionamento por diversos * pontos do plugin.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class WorkspaceMutationFacade {

    private static final String PACKAGE_KEEP_FILE = ".sai_keep";

    private final File projectRootDirectory;
    private final SafeWorkspaceMutationPolicy mutationPolicy;
    private final ProjectMutationStore mutationStore;
    private final JGitWorkspaceRepository gitRepository;
    private final WorkspaceMirrorService mirrorService;
    private final ProjectMutationJsonSupport jsonSupport;

    /** * Inicializa a fachada de mutacao para o projeto informado. * * @param projectRootDirectory raiz fisica do projeto real * @param projectKey chave estavel do projeto dentro da estrutura .sai * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public WorkspaceMutationFacade(File projectRootDirectory, String projectKey) {
        this.projectRootDirectory = projectRootDirectory;
        this.mutationPolicy = new SafeWorkspaceMutationPolicy(projectRootDirectory);
        this.mutationStore = new ProjectMutationStore(projectRootDirectory, projectKey);
        this.gitRepository = new JGitWorkspaceRepository(this.mutationStore.getMutationPaths());
        this.mirrorService = new WorkspaceMirrorService(projectRootDirectory, gitRepository);
        this.jsonSupport = new ProjectMutationJsonSupport();
    }

    /** * Inicializa a infraestrutura persistente de mutacao e garante a existencia * do repositorio interno de versionamento. * * @throws Exception quando ocorrer falha na inicializacao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void initializeInfrastructure() throws Exception {
        mutationStore.inicializarEstrutura();
        gitRepository.ensureRepositoryInitialized();
        atualizarRepoMetaInicializado(true);
    }

    /** * Cria um novo arquivo no projeto respeitando a politica de mutacao e * registrando a operacao no journal interno. * * @param context contexto semantico da mutacao * @param relativePath caminho relativo do novo arquivo * @param content conteudo textual inicial * @return relatorio textual da operacao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String applyCreateFile(MutationContext context, String relativePath, String content) {
        String safeRelativePath = normalizeRelativePath(relativePath);
        if (!validarContexto(context)) {
            return "Erro Operacional: Contexto de mutacao invalido para criacao de arquivo.";
        }

        if (!mutationPolicy.canCreateFile(context.getTargetName(), safeRelativePath)) {
            return "Erro Operacional: A politica de mutacao nao permite criar este arquivo no alvo informado.";
        }

        File targetFile = resolveWorkspaceFile(safeRelativePath);
        if (targetFile.exists()) {
            return "Erro Operacional: O arquivo alvo ja existe no workspace.";
        }

        String batchId = gerarBatchId();
        String operationId = gerarOperationId();

        MutationBatch batch = criarBatchInicial(batchId, context);
        MutationOperation operation = criarOperationInicial(
                operationId,
                batchId,
                MutationActionType.CREATE_FILE,
                MutationTargetScope.FILE,
                context,
                safeRelativePath,
                targetFile,
                "Criacao de novo arquivo no workspace."
        );

        try {
            initializeInfrastructure();
            mutationStore.salvarBatch(batch);
            mutationStore.salvarOuAtualizarOperacao(
                    batchId,
                    operation,
                    context.getInstructionSummary(),
                    context.getOrigin(),
                    context.getBranchName()
            );

            ensureParentDirectory(targetFile);
            writeWorkspaceFile(targetFile, content);

            File mirroredFile = mirrorService.mirrorWorkspaceFile(targetFile, safeRelativePath);
            gitRepository.addPath(safeRelativePath);

            String afterCommitId = safeCommitOrHead(
                    "[BATCH=" + batchId + "] [OP=" + operationId + "] CREATE_FILE " + safeRelativePath
            );

            operation.setAfterCommitId(afterCommitId);
            operation.setStatus(MutationOperationStatus.APPLIED);

            batch.setStatus(MutationOperationStatus.APPLIED);
            batch.setFinishedAt(System.currentTimeMillis());

            mutationStore.salvarOuAtualizarOperacao(
                    batchId,
                    operation,
                    context.getInstructionSummary(),
                    context.getOrigin(),
                    context.getBranchName()
            );
            mutationStore.atualizarStatusBatch(batchId, MutationOperationStatus.APPLIED);
            mutationStore.pushUndoBatch(batchId);

            return "Arquivo criado com sucesso.\n"
                    + "path: " + safeRelativePath + "\n"
                    + "batchId: " + batchId + "\n"
                    + "operationId: " + operationId + "\n"
                    + "afterCommitId: " + afterCommitId + "\n"
                    + "mirror: " + normalizePath(mirroredFile);
        } catch (Exception e) {
            operation.setStatus(MutationOperationStatus.FAILED);
            mutationStore.salvarOuAtualizarOperacao(
                    batchId,
                    operation,
                    context.getInstructionSummary(),
                    context.getOrigin(),
                    context.getBranchName()
            );
            mutationStore.atualizarStatusBatch(batchId, MutationOperationStatus.FAILED);

            return "Falha ao criar arquivo no workspace: " + e.getMessage();
        }
    }

    /** * Altera um arquivo existente no projeto respeitando a politica de mutacao, * gerando backup .bkp e registrando estado before/after no journal interno. * * @param context contexto semantico da mutacao * @param relativePath caminho relativo do arquivo a alterar * @param content novo conteudo textual * @return relatorio textual da operacao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String applyUpdateFile(MutationContext context, String relativePath, String content) {
        String safeRelativePath = normalizeRelativePath(relativePath);
        if (!validarContexto(context)) {
            return "Erro Operacional: Contexto de mutacao invalido para alteracao de arquivo.";
        }

        File targetFile = resolveWorkspaceFile(safeRelativePath);
        if (!targetFile.exists() || !targetFile.isFile()) {
            return "Erro Operacional: O arquivo alvo nao existe para alteracao.";
        }

        if (!mutationPolicy.canUpdateFile(targetFile)) {
            return "Erro Operacional: A politica de mutacao nao permite alterar este arquivo.";
        }

        String batchId = gerarBatchId();
        String operationId = gerarOperationId();

        MutationBatch batch = criarBatchInicial(batchId, context);
        MutationOperation operation = criarOperationInicial(
                operationId,
                batchId,
                MutationActionType.UPDATE_FILE,
                MutationTargetScope.FILE,
                context,
                safeRelativePath,
                targetFile,
                "Alteracao de arquivo existente com backup."
        );

        try {
            initializeInfrastructure();
            mutationStore.salvarBatch(batch);
            mutationStore.salvarOuAtualizarOperacao(
                    batchId,
                    operation,
                    context.getInstructionSummary(),
                    context.getOrigin(),
                    context.getBranchName()
            );

            String backupPath = criarBackupArquivo(targetFile);

            mirrorService.mirrorWorkspaceFile(targetFile, safeRelativePath);
            gitRepository.addPath(safeRelativePath);
            String beforeCommitId = safeCommitOrHead(
                    "[BATCH=" + batchId + "] [OP=" + operationId + "] BEFORE_UPDATE " + safeRelativePath
            );

            writeWorkspaceFile(targetFile, content);

            mirrorService.mirrorWorkspaceFile(targetFile, safeRelativePath);
            gitRepository.addPath(safeRelativePath);
            String afterCommitId = safeCommitOrHead(
                    "[BATCH=" + batchId + "] [OP=" + operationId + "] AFTER_UPDATE " + safeRelativePath
            );

            operation.setBeforeCommitId(beforeCommitId);
            operation.setAfterCommitId(afterCommitId);
            operation.setStatus(MutationOperationStatus.APPLIED);
            operation.setSummary("Alteracao aplicada com backup em " + backupPath);

            batch.setStatus(MutationOperationStatus.APPLIED);
            batch.setFinishedAt(System.currentTimeMillis());

            mutationStore.salvarOuAtualizarOperacao(
                    batchId,
                    operation,
                    context.getInstructionSummary(),
                    context.getOrigin(),
                    context.getBranchName()
            );
            mutationStore.atualizarStatusBatch(batchId, MutationOperationStatus.APPLIED);
            mutationStore.pushUndoBatch(batchId);

            return "Arquivo alterado com sucesso.\n"
                    + "path: " + safeRelativePath + "\n"
                    + "backup: " + backupPath + "\n"
                    + "batchId: " + batchId + "\n"
                    + "operationId: " + operationId + "\n"
                    + "beforeCommitId: " + beforeCommitId + "\n"
                    + "afterCommitId: " + afterCommitId;
        } catch (Exception e) {
            operation.setStatus(MutationOperationStatus.FAILED);
            mutationStore.salvarOuAtualizarOperacao(
                    batchId,
                    operation,
                    context.getInstructionSummary(),
                    context.getOrigin(),
                    context.getBranchName()
            );
            mutationStore.atualizarStatusBatch(batchId, MutationOperationStatus.FAILED);

            return "Falha ao alterar arquivo no workspace: " + e.getMessage();
        }
    }

    /** * Apaga um arquivo previamente permitido pela politica de mutacao e registra * estado before/after no journal. * * @param context contexto semantico da mutacao * @param relativePath caminho relativo do arquivo a apagar * @return relatorio textual da operacao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String applyDeleteCreatedFile(MutationContext context, String relativePath) {
        String safeRelativePath = normalizeRelativePath(relativePath);
        if (!validarContexto(context)) {
            return "Erro Operacional: Contexto de mutacao invalido para remocao de arquivo.";
        }

        File targetFile = resolveWorkspaceFile(safeRelativePath);
        if (!targetFile.exists() || !targetFile.isFile()) {
            return "Erro Operacional: O arquivo alvo nao existe para remocao.";
        }

        if (!mutationPolicy.canDeleteFile(targetFile)) {
            return "Erro Operacional: A politica de mutacao nao permite apagar este arquivo.";
        }

        String batchId = gerarBatchId();
        String operationId = gerarOperationId();

        MutationBatch batch = criarBatchInicial(batchId, context);
        MutationOperation operation = criarOperationInicial(
                operationId,
                batchId,
                MutationActionType.DELETE_CREATED_FILE,
                MutationTargetScope.FILE,
                context,
                safeRelativePath,
                targetFile,
                "Remocao de arquivo previamente permitido."
        );

        try {
            initializeInfrastructure();
            mutationStore.salvarBatch(batch);
            mutationStore.salvarOuAtualizarOperacao(
                    batchId,
                    operation,
                    context.getInstructionSummary(),
                    context.getOrigin(),
                    context.getBranchName()
            );

            mirrorService.mirrorWorkspaceFile(targetFile, safeRelativePath);
            gitRepository.addPath(safeRelativePath);
            String beforeCommitId = safeCommitOrHead(
                    "[BATCH=" + batchId + "] [OP=" + operationId + "] BEFORE_DELETE " + safeRelativePath
            );

            Files.delete(targetFile.toPath());

            if (mirrorService.existsMirroredPath(safeRelativePath)) {
                mirrorService.removeMirroredPath(safeRelativePath);
            }

            String afterCommitId = safeCommitOrHead(
                    "[BATCH=" + batchId + "] [OP=" + operationId + "] AFTER_DELETE " + safeRelativePath
            );

            operation.setBeforeCommitId(beforeCommitId);
            operation.setAfterCommitId(afterCommitId);
            operation.setStatus(MutationOperationStatus.APPLIED);

            batch.setStatus(MutationOperationStatus.APPLIED);
            batch.setFinishedAt(System.currentTimeMillis());

            mutationStore.salvarOuAtualizarOperacao(
                    batchId,
                    operation,
                    context.getInstructionSummary(),
                    context.getOrigin(),
                    context.getBranchName()
            );
            mutationStore.atualizarStatusBatch(batchId, MutationOperationStatus.APPLIED);
            mutationStore.pushUndoBatch(batchId);

            return "Arquivo apagado com sucesso.\n"
                    + "path: " + safeRelativePath + "\n"
                    + "batchId: " + batchId + "\n"
                    + "operationId: " + operationId + "\n"
                    + "beforeCommitId: " + beforeCommitId + "\n"
                    + "afterCommitId: " + afterCommitId;
        } catch (Exception e) {
            operation.setStatus(MutationOperationStatus.FAILED);
            mutationStore.salvarOuAtualizarOperacao(
                    batchId,
                    operation,
                    context.getInstructionSummary(),
                    context.getOrigin(),
                    context.getBranchName()
            );
            mutationStore.atualizarStatusBatch(batchId, MutationOperationStatus.FAILED);

            return "Falha ao apagar arquivo no workspace: " + e.getMessage();
        }
    }

    /** * Cria uma package ou pasta no projeto respeitando a politica de mutacao e * registrando a operacao no journal. * * <p>Como Git nao versiona diretorio vazio, esta implementacao usa um * marcador tecnico .sai_keep apenas no espelho interno.</p> * * @param context contexto semantico da mutacao * @param relativePath caminho relativo da package/pasta a criar * @return relatorio textual da operacao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String applyCreatePackage(MutationContext context, String relativePath) {
        String safeRelativePath = normalizeRelativePath(relativePath);
        if (!validarContexto(context)) {
            return "Erro Operacional: Contexto de mutacao invalido para criacao de package.";
        }

        if (!mutationPolicy.canCreatePackage(context.getTargetName(), safeRelativePath)) {
            return "Erro Operacional: A politica de mutacao nao permite criar esta package/pasta.";
        }

        File targetDirectory = resolveWorkspaceFile(safeRelativePath);
        if (targetDirectory.exists()) {
            return "Erro Operacional: A package/pasta alvo ja existe no workspace.";
        }

        String batchId = gerarBatchId();
        String operationId = gerarOperationId();

        MutationBatch batch = criarBatchInicial(batchId, context);
        MutationOperation operation = criarOperationInicial(
                operationId,
                batchId,
                MutationActionType.CREATE_PACKAGE,
                MutationTargetScope.PACKAGE,
                context,
                safeRelativePath,
                targetDirectory,
                "Criacao de package/pasta no workspace."
        );

        try {
            initializeInfrastructure();
            mutationStore.salvarBatch(batch);
            mutationStore.salvarOuAtualizarOperacao(
                    batchId,
                    operation,
                    context.getInstructionSummary(),
                    context.getOrigin(),
                    context.getBranchName()
            );

            boolean created = targetDirectory.mkdirs();
            if (!created && !targetDirectory.exists()) {
                throw new IOException("Falha ao criar package/pasta fisica no workspace.");
            }

            String keepRelativePath = buildPackageKeepRelativePath(safeRelativePath);
            mirrorService.writeMirroredContent(keepRelativePath, "package-created");
            gitRepository.addPath(keepRelativePath);

            String afterCommitId = safeCommitOrHead(
                    "[BATCH=" + batchId + "] [OP=" + operationId + "] CREATE_PACKAGE " + safeRelativePath
            );

            operation.setAfterCommitId(afterCommitId);
            operation.setStatus(MutationOperationStatus.APPLIED);

            batch.setStatus(MutationOperationStatus.APPLIED);
            batch.setFinishedAt(System.currentTimeMillis());

            mutationStore.salvarOuAtualizarOperacao(
                    batchId,
                    operation,
                    context.getInstructionSummary(),
                    context.getOrigin(),
                    context.getBranchName()
            );
            mutationStore.atualizarStatusBatch(batchId, MutationOperationStatus.APPLIED);
            mutationStore.pushUndoBatch(batchId);

            return "Package/pasta criada com sucesso.\n"
                    + "path: " + safeRelativePath + "\n"
                    + "batchId: " + batchId + "\n"
                    + "operationId: " + operationId + "\n"
                    + "afterCommitId: " + afterCommitId;
        } catch (Exception e) {
            operation.setStatus(MutationOperationStatus.FAILED);
            mutationStore.salvarOuAtualizarOperacao(
                    batchId,
                    operation,
                    context.getInstructionSummary(),
                    context.getOrigin(),
                    context.getBranchName()
            );
            mutationStore.atualizarStatusBatch(batchId, MutationOperationStatus.FAILED);

            return "Falha ao criar package/pasta no workspace: " + e.getMessage();
        }
    }

    /** * Desfaz o ultimo batch disponivel na pilha de undo. * * @param context contexto semantico da solicitacao de undo * @return relatorio textual da operacao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String undoLastBatch(MutationContext context) {
        if (!validarContexto(context)) {
            return "Erro Operacional: Contexto de mutacao invalido para undo.";
        }

        try {
            initializeInfrastructure();

            String batchId = mutationStore.peekUndoBatchId();
            if (isBlank(batchId)) {
                return "Nenhum batch disponivel para undo.";
            }

            MutationBatch batch = mutationStore.buscarBatchPorId(batchId);
            if (batch == null || batch.getOperations() == null || batch.getOperations().isEmpty()) {
                return "Erro Operacional: O batch de undo nao possui operacoes validas.";
            }

            List<MutationOperation> operations = batch.getOperations();
            Collections.sort(operations, new Comparator<MutationOperation>() {
                @Override
                public int compare(MutationOperation a, MutationOperation b) {
                    return Long.compare(b.getCreatedAt(), a.getCreatedAt());
                }
            });

            for (int i = 0; i < operations.size(); i++) {
                desfazerOperacao(operations.get(i));
            }

            mutationStore.atualizarStatusBatch(batchId, MutationOperationStatus.UNDONE);
            mutationStore.moverUltimoUndoParaRedo();

            return "Undo aplicado com sucesso.\n"
                    + "batchId: " + batchId + "\n"
                    + "operations: " + operations.size();
        } catch (Exception e) {
            return "Falha ao desfazer ultimo batch: " + e.getMessage();
        }
    }

    /** * Refaz o ultimo batch disponivel na pilha de redo. * * @param context contexto semantico da solicitacao de redo * @return relatorio textual da operacao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String redoLastBatch(MutationContext context) {
        if (!validarContexto(context)) {
            return "Erro Operacional: Contexto de mutacao invalido para redo.";
        }

        try {
            initializeInfrastructure();

            String batchId = mutationStore.peekRedoBatchId();
            if (isBlank(batchId)) {
                return "Nenhum batch disponivel para redo.";
            }

            MutationBatch batch = mutationStore.buscarBatchPorId(batchId);
            if (batch == null || batch.getOperations() == null || batch.getOperations().isEmpty()) {
                return "Erro Operacional: O batch de redo nao possui operacoes validas.";
            }

            List<MutationOperation> operations = batch.getOperations();
            Collections.sort(operations, new Comparator<MutationOperation>() {
                @Override
                public int compare(MutationOperation a, MutationOperation b) {
                    return Long.compare(a.getCreatedAt(), b.getCreatedAt());
                }
            });

            for (int i = 0; i < operations.size(); i++) {
                refazerOperacao(operations.get(i));
            }

            mutationStore.atualizarStatusBatch(batchId, MutationOperationStatus.REDONE);
            mutationStore.moverUltimoRedoParaUndo();

            return "Redo aplicado com sucesso.\n"
                    + "batchId: " + batchId + "\n"
                    + "operations: " + operations.size();
        } catch (Exception e) {
            return "Falha ao refazer ultimo batch: " + e.getMessage();
        }
    }

    /** * Retorna o store semantico de mutacoes usado pela fachada. * * @return store de mutacao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public ProjectMutationStore getMutationStore() {
        return mutationStore;
    }

    /** * Retorna o backend Git interno usado pela fachada. * * @return repositorio Git interno * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public JGitWorkspaceRepository getGitRepository() {
        return gitRepository;
    }

    /** * Retorna o servico de espelho incremental usado pela fachada. * * @return servico de espelho * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public WorkspaceMirrorService getMirrorService() {
        return mirrorService;
    }

    /** * Desfaz uma operacao individual com base no seu tipo semantico. * * @param operation operacao a desfazer * * @throws Exception quando a reversao falhar * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void desfazerOperacao(MutationOperation operation) throws Exception {
        if (operation == null || operation.getActionType() == null) {
            return;
        }

        String relativePath = normalizeRelativePath(operation.getRelativePath());
        File workspaceTarget = resolveWorkspaceFile(relativePath);

        switch (operation.getActionType()) {
            case CREATE_FILE:
                if (workspaceTarget.exists()) {
                    Files.delete(workspaceTarget.toPath());
                }
                if (mirrorService.existsMirroredPath(relativePath)) {
                    mirrorService.removeMirroredPath(relativePath);
                }
                gitRepository.commit("[UNDO] CREATE_FILE " + relativePath);
                mutationStore.atualizarStatusOperacao(operation.getBatchId(), operation.getOperationId(), MutationOperationStatus.UNDONE);
                break;

            case UPDATE_FILE:
                if (!isBlank(operation.getBeforeCommitId())) {
                    boolean restored = gitRepository.restoreFileFromCommit(operation.getBeforeCommitId(), relativePath, workspaceTarget);
                    if (!restored) {
                        throw new IOException("Nao foi possivel restaurar estado before do arquivo.");
                    }
                    mirrorService.mirrorWorkspaceFile(workspaceTarget, relativePath);
                    gitRepository.addPath(relativePath);
                    gitRepository.commit("[UNDO] UPDATE_FILE " + relativePath);
                }
                mutationStore.atualizarStatusOperacao(operation.getBatchId(), operation.getOperationId(), MutationOperationStatus.UNDONE);
                break;

            case DELETE_CREATED_FILE:
                if (!isBlank(operation.getBeforeCommitId())) {
                    boolean restoredDelete = gitRepository.restoreFileFromCommit(operation.getBeforeCommitId(), relativePath, workspaceTarget);
                    if (!restoredDelete) {
                        throw new IOException("Nao foi possivel restaurar arquivo apagado.");
                    }
                    mirrorService.mirrorWorkspaceFile(workspaceTarget, relativePath);
                    gitRepository.addPath(relativePath);
                    gitRepository.commit("[UNDO] DELETE_CREATED_FILE " + relativePath);
                }
                mutationStore.atualizarStatusOperacao(operation.getBatchId(), operation.getOperationId(), MutationOperationStatus.UNDONE);
                break;

            case CREATE_PACKAGE:
                if (workspaceTarget.exists()) {
                    deleteRecursively(workspaceTarget);
                }
                String keepRelativePath = buildPackageKeepRelativePath(relativePath);
                if (mirrorService.existsMirroredPath(keepRelativePath)) {
                    mirrorService.removeMirroredPath(keepRelativePath);
                }
                gitRepository.commit("[UNDO] CREATE_PACKAGE " + relativePath);
                mutationStore.atualizarStatusOperacao(operation.getBatchId(), operation.getOperationId(), MutationOperationStatus.UNDONE);
                break;

            default:
                break;
        }
    }

    /** * Refaz uma operacao individual com base no seu tipo semantico. * * @param operation operacao a refazer * * @throws Exception quando a reaplicacao falhar * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void refazerOperacao(MutationOperation operation) throws Exception {
        if (operation == null || operation.getActionType() == null) {
            return;
        }

        String relativePath = normalizeRelativePath(operation.getRelativePath());
        File workspaceTarget = resolveWorkspaceFile(relativePath);

        switch (operation.getActionType()) {
            case CREATE_FILE:
                if (!isBlank(operation.getAfterCommitId())) {
                    boolean restored = gitRepository.restoreFileFromCommit(operation.getAfterCommitId(), relativePath, workspaceTarget);
                    if (!restored) {
                        throw new IOException("Nao foi possivel refazer criacao do arquivo.");
                    }
                    mirrorService.mirrorWorkspaceFile(workspaceTarget, relativePath);
                    gitRepository.addPath(relativePath);
                    gitRepository.commit("[REDO] CREATE_FILE " + relativePath);
                }
                mutationStore.atualizarStatusOperacao(operation.getBatchId(), operation.getOperationId(), MutationOperationStatus.REDONE);
                break;

            case UPDATE_FILE:
                if (!isBlank(operation.getAfterCommitId())) {
                    boolean restoredUpdate = gitRepository.restoreFileFromCommit(operation.getAfterCommitId(), relativePath, workspaceTarget);
                    if (!restoredUpdate) {
                        throw new IOException("Nao foi possivel refazer alteracao do arquivo.");
                    }
                    mirrorService.mirrorWorkspaceFile(workspaceTarget, relativePath);
                    gitRepository.addPath(relativePath);
                    gitRepository.commit("[REDO] UPDATE_FILE " + relativePath);
                }
                mutationStore.atualizarStatusOperacao(operation.getBatchId(), operation.getOperationId(), MutationOperationStatus.REDONE);
                break;

            case DELETE_CREATED_FILE:
                if (workspaceTarget.exists()) {
                    Files.delete(workspaceTarget.toPath());
                }
                if (mirrorService.existsMirroredPath(relativePath)) {
                    mirrorService.removeMirroredPath(relativePath);
                }
                gitRepository.commit("[REDO] DELETE_CREATED_FILE " + relativePath);
                mutationStore.atualizarStatusOperacao(operation.getBatchId(), operation.getOperationId(), MutationOperationStatus.REDONE);
                break;

            case CREATE_PACKAGE:
                if (!workspaceTarget.exists()) {
                    workspaceTarget.mkdirs();
                }
                String keepRelativePath = buildPackageKeepRelativePath(relativePath);
                mirrorService.writeMirroredContent(keepRelativePath, "package-created");
                gitRepository.addPath(keepRelativePath);
                gitRepository.commit("[REDO] CREATE_PACKAGE " + relativePath);
                mutationStore.atualizarStatusOperacao(operation.getBatchId(), operation.getOperationId(), MutationOperationStatus.REDONE);
                break;

            default:
                break;
        }
    }

    /** * Cria um batch inicial em estado STARTED. * * @param batchId identificador do lote * @param context contexto semantico da mutacao * @return batch inicializado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private MutationBatch criarBatchInicial(String batchId, MutationContext context) {
        MutationBatch batch = new MutationBatch();
        batch.setBatchId(batchId);
        batch.setInstructionSummary(context.getInstructionSummary());
        batch.setOrigin(context.getOrigin());
        batch.setStatus(MutationOperationStatus.STARTED);
        batch.setBranchAtOperation(context.getBranchName());
        batch.setStartedAt(System.currentTimeMillis());
        batch.setFinishedAt(0L);
        return batch;
    }

    /** * Cria uma operacao inicial em estado STARTED. * * @param operationId identificador da operacao * @param batchId identificador do lote * @param actionType tipo semantico da mutacao * @param scope escopo principal do alvo * @param context contexto semantico da mutacao * @param relativePath caminho relativo alvo * @param targetFile arquivo ou diretorio real relacionado * @param summary resumo curto da operacao * @return operacao inicializada * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private MutationOperation criarOperationInicial(String operationId, String batchId, MutationActionType actionType, MutationTargetScope scope, MutationContext context, String relativePath, File targetFile, String summary) {

        MutationOperation operation = new MutationOperation();
        operation.setOperationId(operationId);
        operation.setBatchId(batchId);
        operation.setActionType(actionType);
        operation.setStatus(MutationOperationStatus.STARTED);
        operation.setOrigin(context.getOrigin() != null ? context.getOrigin() : MutationOrigin.PLUGIN);
        operation.setScope(scope);
        operation.setToolName(context.getToolName());
        operation.setTargetName(context.getTargetName());
        operation.setRelativePath(relativePath);
        operation.setAbsolutePath(targetFile != null ? normalizePath(targetFile) : "");
        operation.setBranchAtOperation(context.getBranchName());
        operation.setSummary(summary);
        operation.setCreatedAt(System.currentTimeMillis());

        return operation;
    }

    /** * Atualiza o metadata do repositorio interno indicando se ele ja foi * inicializado. * * @param initialized true quando o repositorio interno ja esta pronto * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void atualizarRepoMetaInicializado(boolean initialized) {
        JsonObject meta = jsonSupport.lerJsonObject(mutationStore.getMutationRepoMetaFile());
        meta.addProperty("projectRoot", projectRootDirectory != null ? normalizePath(projectRootDirectory) : "");
        meta.addProperty("repoInitialized", initialized ? "true" : "false");
        meta.addProperty("repoDirectory", normalizePath(mutationStore.getMutationPaths().getWorkspaceGitDirectory()));
        meta.addProperty("lastUpdatedAt", String.valueOf(System.currentTimeMillis()));
        jsonSupport.gravarJsonObject(mutationStore.getMutationRepoMetaFile(), meta);
    }

    /** * Resolve um arquivo real do workspace a partir de um caminho relativo. * * @param relativePath caminho relativo dentro do projeto real * @return arquivo ou diretorio correspondente no workspace * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private File resolveWorkspaceFile(String relativePath) {
        return new File(projectRootDirectory, normalizeRelativePath(relativePath));
    }

    /** * Garante a existencia do diretorio pai de um arquivo real do workspace. * * @param targetFile arquivo alvo * @throws IOException quando ocorrer falha ao criar a arvore de diretorios * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void ensureParentDirectory(File targetFile) throws IOException {
        if (targetFile == null) {
            return;
        }

        File parent = targetFile.getParentFile();
        if (parent != null && !parent.exists()) {
            boolean created = parent.mkdirs();
            if (!created && !parent.exists()) {
                throw new IOException("Falha ao criar diretorio pai do arquivo alvo.");
            }
        }
    }

    /** * Grava conteudo textual em um arquivo real do workspace. * * @param targetFile arquivo alvo * @param content conteudo textual * @throws IOException quando ocorrer falha de escrita * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void writeWorkspaceFile(File targetFile, String content) throws IOException {
        byte[] bytes = content != null ? content.getBytes(StandardCharsets.UTF_8) : new byte[0];
        Files.write(targetFile.toPath(), bytes);
    }

    /** * Cria backup .bkp de um arquivo real do workspace. * * @param targetFile arquivo original * @return caminho do backup criado * @throws IOException quando ocorrer falha de copia * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String criarBackupArquivo(File targetFile) throws IOException {
        File backupFile = mutationPolicy.createBackupFile(targetFile);
        if (backupFile == null) {
            throw new IOException("Nao foi possivel resolver o arquivo de backup.");
        }

        ensureParentDirectory(backupFile);
        Files.copy(targetFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return normalizePath(backupFile);
    }

    /** * Retorna um commit valido do repositorio interno. * * <p>Se o commit atual nao gerar diff novo, o metodo reaproveita o HEAD * atual quando disponivel.</p> * * @param message mensagem desejada de commit * @return hash do commit efetivo ou string vazia * * @throws Exception quando ocorrer falha no backend Git * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String safeCommitOrHead(String message) throws Exception {
        String commitId = gitRepository.commit(message);
        if (!isBlank(commitId)) {
            return commitId;
        }

        return gitRepository.getHeadCommitId();
    }

    /** * Monta o caminho relativo do arquivo marcador usado para versionar package * vazia no repositorio interno. * * @param packageRelativePath caminho relativo da package criada * @return caminho relativo do marcador tecnico * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String buildPackageKeepRelativePath(String packageRelativePath) {
        String normalized = normalizeRelativePath(packageRelativePath);
        if (isBlank(normalized)) {
            return PACKAGE_KEEP_FILE;
        }
        return normalized + "/" + PACKAGE_KEEP_FILE;
    }

    /** * Remove recursivamente um diretorio ou arquivo do workspace real. * * @param target alvo fisico a remover * @throws IOException quando ocorrer falha de remocao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void deleteRecursively(File target) throws IOException {
        if (target == null || !target.exists()) {
            return;
        }

        if (target.isDirectory()) {
            File[] children = target.listFiles();
            if (children != null) {
                for (int i = 0; i < children.length; i++) {
                    deleteRecursively(children[i]);
                }
            }
        }

        Files.deleteIfExists(target.toPath());
    }

    /** * Gera identificador unico de batch. * * @return identificador textual de lote * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String gerarBatchId() {
        return "batch_" + System.currentTimeMillis() + "_" + Math.abs(System.nanoTime());
    }

    /** * Gera identificador unico de operacao. * * @return identificador textual de operacao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String gerarOperationId() {
        return "op_" + System.currentTimeMillis() + "_" + Math.abs(System.nanoTime());
    }

    /** * Retorna true quando o contexto de mutacao esta minimamente consistente. * * @param context contexto a validar * @return true quando o contexto puder ser usado com seguranca * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean validarContexto(MutationContext context) {
        return context != null
                && context.isUsable()
                && projectRootDirectory != null
                && projectRootDirectory.exists()
                && projectRootDirectory.isDirectory();
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

    /** * Retorna true quando o valor informado for nulo ou vazio. * * @param value valor a validar * @return true quando o valor for branco * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}