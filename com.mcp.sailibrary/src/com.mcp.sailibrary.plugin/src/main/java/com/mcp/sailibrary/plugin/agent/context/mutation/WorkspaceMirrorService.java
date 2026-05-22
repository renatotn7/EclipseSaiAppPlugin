package com.mcp.sailibrary.plugin.agent.context.mutation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** * Centraliza o espelhamento fisico incremental de artefatos mutados entre o * workspace real e o repositorio interno de mutacao. * * <p>Esta classe e responsavel apenas por manter a estrutura de arquivos * espelhados dentro do diretorio workspace_git, preservando a mesma topologia * relativa usada no projeto real. Ela nao executa commit, nao consulta * politica de mutacao e nao conhece regras de undo ou redo.</p> * * <p>O objetivo principal desta camada e reduzir duplicacao de logica de copia, * remocao, leitura e resolucao de caminhos relativos, deixando a camada Git e * a camada semantica de mutacao mais limpas.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class WorkspaceMirrorService {

    private final File projectRootDirectory;
    private final JGitWorkspaceRepository gitRepository;

    /** * Inicializa o servico de espelhamento incremental para o projeto atual. * * @param projectRootDirectory raiz fisica do projeto real no workspace * @param gitRepository backend tecnico do repositorio interno de mutacao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public WorkspaceMirrorService(File projectRootDirectory, JGitWorkspaceRepository gitRepository) {
        this.projectRootDirectory = projectRootDirectory;
        this.gitRepository = gitRepository;
    }

    /** * Espelha um arquivo real do workspace para o repositorio interno usando o * caminho relativo calculado automaticamente a partir da raiz do projeto. * * @param workspaceFile arquivo fisico real do workspace * @return arquivo espelhado dentro do workspace_git * * @throws IOException quando ocorrer falha de leitura, escrita ou resolucao * de caminho * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File mirrorWorkspaceFile(File workspaceFile) throws IOException {
        validarWorkspaceFile(workspaceFile);

        String relativePath = resolveRelativePath(workspaceFile);
        return mirrorWorkspaceFile(workspaceFile, relativePath);
    }

    /** * Espelha um arquivo real do workspace para o repositorio interno usando o * caminho relativo informado explicitamente. * * @param workspaceFile arquivo fisico real do workspace * @param relativePath caminho relativo desejado dentro do workspace_git * @return arquivo espelhado dentro do workspace_git * * @throws IOException quando ocorrer falha de leitura, escrita ou resolucao * de caminho * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File mirrorWorkspaceFile(File workspaceFile, String relativePath) throws IOException {
        validarWorkspaceFile(workspaceFile);

        try {
            return gitRepository.mirrorWorkspaceFile(workspaceFile, normalizeRelativePath(relativePath));
        } catch (Exception e) {
            throw new IOException("Falha ao espelhar arquivo do workspace: " + e.getMessage(), e);
        }
    }

    /** * Grava conteudo textual diretamente no espelho interno usando o caminho * relativo informado. * * @param relativePath caminho relativo dentro do workspace_git * @param content conteudo textual a ser gravado * @return arquivo espelhado gravado * * @throws IOException quando ocorrer falha de escrita no espelho * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File writeMirroredContent(String relativePath, String content) throws IOException {
        try {
            return gitRepository.writeMirroredFile(normalizeRelativePath(relativePath), content);
        } catch (Exception e) {
            throw new IOException("Falha ao gravar conteudo no espelho interno: " + e.getMessage(), e);
        }
    }

    /** * Remove um arquivo espelhado usando o caminho relativo informado. * * <p>Esta operacao remove o arquivo do workspace_git e o marca para remocao * posterior no indice Git quando o fluxo chamador decidir versionar a * alteracao.</p> * * @param relativePath caminho relativo dentro do espelho interno * * @throws IOException quando ocorrer falha de remocao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void removeMirroredPath(String relativePath) throws IOException {
        try {
            gitRepository.removePath(normalizeRelativePath(relativePath));
        } catch (Exception e) {
            throw new IOException("Falha ao remover caminho espelhado: " + e.getMessage(), e);
        }
    }

    /** * Retorna o arquivo fisico correspondente dentro do espelho interno para um * caminho relativo. * * @param relativePath caminho relativo dentro do workspace_git * @return arquivo fisico correspondente no espelho * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File resolveMirroredFile(String relativePath) {
        return gitRepository.resolveRepositoryFile(normalizeRelativePath(relativePath));
    }

    /** * Retorna o arquivo espelhado correspondente a um arquivo real do * workspace. * * @param workspaceFile arquivo fisico real do projeto * @return arquivo correspondente dentro do espelho interno * * @throws IOException quando ocorrer falha ao resolver o caminho relativo * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File resolveMirroredFileForWorkspaceFile(File workspaceFile) throws IOException {
        validarWorkspaceFile(workspaceFile);
        String relativePath = resolveRelativePath(workspaceFile);
        return resolveMirroredFile(relativePath);
    }

    /** * Retorna true quando o espelho de um arquivo real do workspace ja existe * no repositorio interno. * * @param workspaceFile arquivo fisico real do projeto * @return true quando o espelho correspondente existir * * @throws IOException quando ocorrer falha ao resolver o caminho relativo * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public boolean existsMirrorForWorkspaceFile(File workspaceFile) throws IOException {
        File mirroredFile = resolveMirroredFileForWorkspaceFile(workspaceFile);
        return mirroredFile.exists() && mirroredFile.isFile();
    }

    /** * Retorna true quando um caminho relativo ja existe no espelho interno. * * @param relativePath caminho relativo dentro do workspace_git * @return true quando o arquivo espelhado existir * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public boolean existsMirroredPath(String relativePath) {
        File mirroredFile = resolveMirroredFile(relativePath);
        return mirroredFile.exists() && mirroredFile.isFile();
    }

    /** * Le o conteudo textual de um arquivo espelhado correspondente ao arquivo * real informado. * * @param workspaceFile arquivo fisico real do projeto * @return conteudo espelhado ou string vazia quando inexistente * * @throws IOException quando ocorrer falha de leitura * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String readMirroredContentForWorkspaceFile(File workspaceFile) throws IOException {
        File mirroredFile = resolveMirroredFileForWorkspaceFile(workspaceFile);
        return readMirroredContent(mirroredFile);
    }

    /** * Le o conteudo textual de um caminho relativo dentro do espelho interno. * * @param relativePath caminho relativo no workspace_git * @return conteudo espelhado ou string vazia quando inexistente * * @throws IOException quando ocorrer falha de leitura * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String readMirroredContent(String relativePath) throws IOException {
        File mirroredFile = resolveMirroredFile(relativePath);
        return readMirroredContent(mirroredFile);
    }

    /** * Calcula o caminho relativo de um arquivo real do workspace em relacao a * raiz do projeto. * * @param workspaceFile arquivo fisico real do projeto * @return caminho relativo normalizado * * @throws IOException quando o arquivo estiver fora do perimetro da raiz do * projeto ou quando a resolucao canonica falhar * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String resolveRelativePath(File workspaceFile) throws IOException {
        validarWorkspaceFile(workspaceFile);

        if (projectRootDirectory == null || !projectRootDirectory.exists() || !projectRootDirectory.isDirectory()) {
            throw new IOException("Raiz do projeto invalida para resolucao de caminho relativo.");
        }

        String rootPath = projectRootDirectory.getCanonicalPath().replace("\\", "/");
        String filePath = workspaceFile.getCanonicalPath().replace("\\", "/");

        if (!filePath.startsWith(rootPath)) {
            throw new IOException("O arquivo informado esta fora do perimetro da raiz do projeto.");
        }

        String relativePath = filePath.substring(rootPath.length());
        while (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }

        return normalizeRelativePath(relativePath);
    }

    /** * Garante a existencia do diretorio pai correspondente a um caminho * relativo no espelho interno. * * @param relativePath caminho relativo dentro do workspace_git * @return diretorio pai correspondente no espelho * * @throws IOException quando ocorrer falha de criacao da arvore de * diretorios * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File ensureMirrorParentDirectory(String relativePath) throws IOException {
        File mirroredFile = resolveMirroredFile(relativePath);
        File parent = mirroredFile.getParentFile();

        if (parent != null && !parent.exists()) {
            boolean created = parent.mkdirs();
            if (!created && !parent.exists()) {
                throw new IOException("Falha ao criar diretorio pai no espelho interno.");
            }
        }

        return parent;
    }

    /** * Retorna a raiz fisica do projeto real associada a este servico. * * @return diretorio raiz do projeto * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File getProjectRootDirectory() {
        return projectRootDirectory;
    }

    /** * Retorna o backend Git associado ao espelho incremental. * * @return repositorio Git interno * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public JGitWorkspaceRepository getGitRepository() {
        return gitRepository;
    }

    /** * Le o conteudo textual de um arquivo espelhado especifico. * * @param mirroredFile arquivo fisico do espelho * @return conteudo textual ou string vazia quando inexistente * * @throws IOException quando ocorrer falha de leitura * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String readMirroredContent(File mirroredFile) throws IOException {
        if (mirroredFile == null || !mirroredFile.exists() || !mirroredFile.isFile()) {
            return "";
        }

        return Files.readString(mirroredFile.toPath(), StandardCharsets.UTF_8);
    }

    /** * Valida se o arquivo real informado pode ser usado no fluxo de espelho. * * @param workspaceFile arquivo fisico real do projeto * * @throws IOException quando o arquivo for nulo, inexistente ou nao for * arquivo regular * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void validarWorkspaceFile(File workspaceFile) throws IOException {
        if (workspaceFile == null) {
            throw new IOException("Arquivo de workspace nulo.");
        }

        if (!workspaceFile.exists() || !workspaceFile.isFile()) {
            throw new IOException("Arquivo de workspace inexistente ou invalido.");
        }
    }

    /** * Normaliza um caminho relativo para o formato padrao do espelho interno. * * @param relativePath caminho relativo original * @return caminho relativo normalizado * * @author Renato Tomaz Nati * @since 2026-05-20 */
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
}