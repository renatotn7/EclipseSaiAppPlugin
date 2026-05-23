package com.mcp.sailibrary.plugin.chat.blocks.service;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;

import com.mcp.sailibrary.plugin.agent.context.mutation.ProjectMutationStore;
import com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationActionType;
import com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationBatch;
import com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationOperation;
import com.mcp.sailibrary.plugin.chat.blocks.model.StructuralArtifactMutationState;
import com.mcp.sailibrary.plugin.chat.context.service.CreatedArtifactRegistryService;

/** * Resolve o estado visual de mutacao de arquivos, packages e pastas para uso * em decorators do explorer. * * <p>O service combina: * <ul> * <li>registro de artefatos criados pela propria IA/plugin</li> * <li>historico de mutacoes persistido no ProjectMutationStore</li> * </ul> * </p> * * <p>Regra de prioridade: * <ol> * <li>arquivo criado ainda existente no registry => ADDED</li> * <li>ultima operacao relevante com status UNDONE/REDONE/RESTORE_* => RESTORED</li> * <li>ultima operacao relevante de update => MODIFIED</li> * <li>caso contrario => NONE</li> * </ol> * </p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class StructuralArtifactMutationStateService {

    /** * Resolve o estado de mutacao de um arquivo Java. * * @param compilationUnit unidade Java do explorer * @return estado visual de mutacao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public StructuralArtifactMutationState resolveForCompilationUnit(ICompilationUnit compilationUnit) {
        if (compilationUnit == null || compilationUnit.getResource() == null || compilationUnit.getResource().getLocation() == null) {
            return StructuralArtifactMutationState.NONE;
        }

        File file = compilationUnit.getResource().getLocation().toFile();
        String relativePath = compilationUnit.getResource().getProjectRelativePath() != null
                ? normalizePath(compilationUnit.getResource().getProjectRelativePath().toString())
                : "";

        return resolveForFileInternal(file, relativePath);
    }

    /** * Resolve o estado de mutacao de um arquivo do explorer. * * @param file arquivo estrutural * @return estado visual de mutacao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public StructuralArtifactMutationState resolveForFile(IFile file) {
        if (file == null || file.getLocation() == null) {
            return StructuralArtifactMutationState.NONE;
        }

        File realFile = file.getLocation().toFile();
        String relativePath = file.getProjectRelativePath() != null
                ? normalizePath(file.getProjectRelativePath().toString())
                : "";

        return resolveForFileInternal(realFile, relativePath);
    }

    /** * Resolve o estado de mutacao de uma package. * * <p>Para package, o estado e inferido pelo historico das operacoes dentro * do caminho relativo correspondente.</p> * * @param packageFragment package estrutural * @return estado visual de mutacao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public StructuralArtifactMutationState resolveForPackage(IPackageFragment packageFragment) {
        if (packageFragment == null || packageFragment.getResource() == null) {
            return StructuralArtifactMutationState.NONE;
        }

        String relativePath = packageFragment.getResource().getProjectRelativePath() != null
                ? normalizePath(packageFragment.getResource().getProjectRelativePath().toString())
                : "";

        File projectRoot = packageFragment.getJavaProject() != null
                && packageFragment.getJavaProject().getProject() != null
                && packageFragment.getJavaProject().getProject().getLocation() != null
                ? packageFragment.getJavaProject().getProject().getLocation().toFile()
                : null;

        return resolveForContainerInternal(projectRoot, relativePath);
    }

    /** * Resolve o estado de mutacao de uma pasta/contêiner. * * @param container contêiner estrutural * @return estado visual de mutacao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public StructuralArtifactMutationState resolveForContainer(IContainer container) {
        if (container == null || container.getLocation() == null || container.getProject() == null || container.getProject().getLocation() == null) {
            return StructuralArtifactMutationState.NONE;
        }

        File projectRoot = container.getProject().getLocation().toFile();
        String relativePath = container.getProjectRelativePath() != null
                ? normalizePath(container.getProjectRelativePath().toString())
                : "";

        return resolveForContainerInternal(projectRoot, relativePath);
    }

    /** * Resolve o estado visual de um arquivo real a partir do registry e do * historico de mutacao. * * @param realFile arquivo real * @param relativePath caminho relativo no projeto * @return estado visual de mutacao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private StructuralArtifactMutationState resolveForFileInternal(File realFile, String relativePath) {
        if (realFile == null) {
            return StructuralArtifactMutationState.NONE;
        }

        File projectRoot = encontrarProjetoEclipseMaisProximo(realFile);
        if (projectRoot == null) {
            return StructuralArtifactMutationState.NONE;
        }

        CreatedArtifactRegistryService registryService = new CreatedArtifactRegistryService(projectRoot);
        if (registryService.isCreatedFile(realFile)) {
            return StructuralArtifactMutationState.ADDED;
        }

        ProjectMutationStore mutationStore = new ProjectMutationStore(projectRoot, gerarProjectKey(projectRoot));
        mutationStore.inicializarEstrutura();

        MutationOperation operation = localizarUltimaOperacaoPorPath(mutationStore.listarBatches(), relativePath);
        return classifyOperationState(operation);
    }

    /** * Resolve o estado visual de uma package/pasta a partir do historico de * mutacoes registradas abaixo do caminho informado. * * @param projectRoot raiz do projeto Eclipse dono * @param relativePath caminho relativo da package/pasta * @return estado visual de mutacao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private StructuralArtifactMutationState resolveForContainerInternal(File projectRoot, String relativePath) {
        if (projectRoot == null || relativePath == null || relativePath.trim().length() == 0) {
            return StructuralArtifactMutationState.NONE;
        }

        ProjectMutationStore mutationStore = new ProjectMutationStore(projectRoot, gerarProjectKey(projectRoot));
        mutationStore.inicializarEstrutura();

        MutationOperation operation = localizarUltimaOperacaoPorPrefixo(mutationStore.listarBatches(), relativePath);
        return classifyOperationState(operation);
    }

    /** * Localiza a ultima operacao relevante exatamente para um path relativo. * * @param batches batches conhecidos * @param relativePath path relativo procurado * @return operacao mais recente compatível ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private MutationOperation localizarUltimaOperacaoPorPath(List<MutationBatch> batches, String relativePath) {
        if (batches == null || relativePath == null) {
            return null;
        }

        MutationOperation melhor = null;

        for (int i = 0; i < batches.size(); i++) {
            MutationBatch batch = batches.get(i);
            if (batch == null || batch.getOperations() == null) {
                continue;
            }

            for (int j = 0; j < batch.getOperations().size(); j++) {
                MutationOperation atual = batch.getOperations().get(j);
                if (atual == null) {
                    continue;
                }

                String rp = normalizePath(atual.getRelativePath());
                if (!relativePath.equals(rp)) {
                    continue;
                }

                if (melhor == null || atual.getCreatedAt() > melhor.getCreatedAt()) {
                    melhor = atual;
                }
            }
        }

        return melhor;
    }

    /** * Localiza a ultima operacao relevante abaixo de um prefixo relativo. * * @param batches batches conhecidos * @param relativePathPrefix prefixo relativo da package/pasta * @return operacao mais recente compatível ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private MutationOperation localizarUltimaOperacaoPorPrefixo(List<MutationBatch> batches, String relativePathPrefix) {
        if (batches == null || relativePathPrefix == null) {
            return null;
        }

        String prefix = normalizePath(relativePathPrefix);
        while (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }

        MutationOperation melhor = null;

        for (int i = 0; i < batches.size(); i++) {
            MutationBatch batch = batches.get(i);
            if (batch == null || batch.getOperations() == null) {
                continue;
            }

            for (int j = 0; j < batch.getOperations().size(); j++) {
                MutationOperation atual = batch.getOperations().get(j);
                if (atual == null) {
                    continue;
                }

                String rp = normalizePath(atual.getRelativePath());
                if (!(rp.equals(prefix) || rp.startsWith(prefix + "/"))) {
                    continue;
                }

                if (melhor == null || atual.getCreatedAt() > melhor.getCreatedAt()) {
                    melhor = atual;
                }
            }
        }

        return melhor;
    }

    /** * Classifica o estado visual a partir da operacao mais recente. * * @param operation operacao encontrada * @return estado visual correspondente * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private StructuralArtifactMutationState classifyOperationState(MutationOperation operation) {
        if (operation == null) {
            return StructuralArtifactMutationState.NONE;
        }

        if (operation.getStatus() != null) {
            if ("UNDONE".equals(operation.getStatus().name()) || "REDONE".equals(operation.getStatus().name()) || "RESTORED".equals(operation.getStatus().name())) {
                return StructuralArtifactMutationState.RESTORED;
            }
        }

        if (operation.getActionType() == MutationActionType.UPDATE_FILE) {
            return StructuralArtifactMutationState.MODIFIED;
        }

        if (operation.getActionType() == MutationActionType.CREATE_FILE || operation.getActionType() == MutationActionType.CREATE_PACKAGE) {
            return StructuralArtifactMutationState.ADDED;
        }

        return StructuralArtifactMutationState.NONE;
    }

    /** * Encontra o projeto Eclipse mais proximo subindo a arvore em busca de * `.project`. * * @param file arquivo real * @return raiz do projeto Eclipse mais proximo ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private File encontrarProjetoEclipseMaisProximo(File file) {
        File cursor = file != null && file.isFile() ? file.getParentFile() : file;

        while (cursor != null && cursor.exists()) {
            File projectFile = new File(cursor, ".project");
            if (projectFile.exists() && projectFile.isFile()) {
                return cursor;
            }
            cursor = cursor.getParentFile();
        }

        return null;
    }

    /** * Gera a chave estavel do projeto a partir da raiz Eclipse mais proxima. * * @param rootDirectory raiz do projeto * @return projectKey estável * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String gerarProjectKey(File rootDirectory) {
        if (rootDirectory == null) {
            return "unknown_project";
        }

        try {
            String canonicalPath = rootDirectory.getCanonicalPath();
            String nomeBase = rootDirectory.getName();
            String hash = gerarHashCurto(canonicalPath);
            return normalizarNome(nomeBase) + "_" + hash;
        } catch (Exception e) {
            return normalizarNome(rootDirectory.getName()) + "_fallback";
        }
    }

    /** * Gera hash curto deterministico. * * @param valor valor base * @return hash curto hexadecimal * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String gerarHashCurto(String valor) {
        try {
            java.security.MessageDigest messageDigest = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = messageDigest.digest(valor.getBytes("UTF-8"));

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

    /** * Normaliza nome de projeto para uso seguro em identificadores internos. * * @param nome nome original * @return nome normalizado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String normalizarNome(String nome) {
        if (nome == null || nome.trim().length() == 0) {
            return "project";
        }

        String normalizado = nome.toLowerCase();
        normalizado = normalizado.replaceAll("[^a-z0-9_\\-]", "_");
        return normalizado;
    }

    /** * Normaliza caminho textual para comparacoes. * * @param value caminho original * @return caminho normalizado com barras normais * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String normalizePath(String value) {
        return value == null ? "" : value.trim().replace("\\", "/");
    }
}