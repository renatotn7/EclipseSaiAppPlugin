package com.mcp.sailibrary.plugin.agent.context.mutation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

/** * Encapsula o repositorio JGit interno usado para versionar o espelho local * dos artefatos mutados pela IA/plugin. * * <p>Esta classe atua apenas como backend tecnico de versionamento. Ela nao * decide politica de mutacao, nao controla undo/redo semantico e nao conhece * regras de negocio de batches. Seu papel e oferecer primitivas seguras de * inicializacao, commit, leitura de conteudo versionado e restauracao por * caminho.</p> * * <p>O repositorio interno deve ser isolado do .git real do projeto do usuario, * operando apenas dentro da estrutura persistente da pasta .sai.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class JGitWorkspaceRepository {

    private static final String DEFAULT_BRANCH = "master";
    private static final String DEFAULT_COMMIT_MESSAGE = "snapshot";
    private static final String DEFAULT_AUTHOR_NAME = "SAI Plugin";
    private static final String DEFAULT_AUTHOR_EMAIL = "sai-plugin@local";

    private final ProjectMutationPaths mutationPaths;

    /** * Inicializa o repositorio interno com base nos caminhos de mutacao do * projeto atual. * * @param mutationPaths resolvedor de caminhos fisicos da camada de mutacao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public JGitWorkspaceRepository(ProjectMutationPaths mutationPaths) {
        this.mutationPaths = mutationPaths;
    }

    /** * Garante a existencia do repositorio git interno em disco. * * <p>Se o repositorio ainda nao existir, o metodo cria o diretorio * workspace_git e inicializa o metadata .git correspondente.</p> * * @return diretorio raiz do repositorio interno * * @throws IOException quando ocorrer falha de IO na estrutura fisica * @throws GitAPIException quando a inicializacao do repositório falhar * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File ensureRepositoryInitialized() throws IOException, GitAPIException {
        File repoDirectory = getRepositoryDirectory();

        if (!repoDirectory.exists()) {
            repoDirectory.mkdirs();
        }

        if (!existsGitMetadata()) {
            Git.init()
               .setDirectory(repoDirectory)
               .setInitialBranch(DEFAULT_BRANCH)
               .call()
               .close();
        }

        return repoDirectory;
    }

    /** * Retorna true quando o metadata .git interno ja existe. * * @return true quando o repositorio interno ja foi inicializado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public boolean existsGitMetadata() {
        if (mutationPaths == null) {
            return false;
        }

        File metadataDirectory = mutationPaths.getWorkspaceGitMetadataDirectory();
        return metadataDirectory.exists() && metadataDirectory.isDirectory();
    }

    /** * Retorna o diretorio fisico do repositorio interno. * * @return diretorio workspace_git * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File getRepositoryDirectory() {
        return mutationPaths.getWorkspaceGitDirectory();
    }

    /** * Abre o repositorio interno ja inicializado. * * @return instancia Git pronta para uso * * @throws IOException quando o repositorio nao puder ser aberto * @throws GitAPIException quando a inicializacao necessaria falhar * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public Git openGit() throws IOException, GitAPIException {
        ensureRepositoryInitialized();
        return Git.open(getRepositoryDirectory());
    }

    /** * Grava ou sobrescreve um arquivo espelhado dentro do repositorio interno. * * <p>O caminho deve ser relativo ao repositorio interno e usar formato com * barras normais. O metodo apenas grava em disco. Para versionar a mudanca, * chame addPath e commit em seguida.</p> * * @param relativePath caminho relativo dentro do workspace_git * @param content conteudo textual do arquivo * * @return arquivo espelhado gravado * * @throws IOException quando ocorrer falha de IO * @throws GitAPIException quando a inicializacao necessaria falhar * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File writeMirroredFile(String relativePath, String content) throws IOException, GitAPIException {
        ensureRepositoryInitialized();

        String safeRelativePath = normalizeRelativePath(relativePath);
        File targetFile = resolveRepositoryFile(safeRelativePath);

        File parent = targetFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        byte[] bytes = content != null ? content.getBytes(StandardCharsets.UTF_8) : new byte[0];
        Files.write(targetFile.toPath(), bytes);

        return targetFile;
    }

    /** * Copia o conteudo de um arquivo real do workspace para o espelho do * repositorio interno. * * @param workspaceFile arquivo real do workspace * @param relativePath caminho relativo desejado dentro do repo interno * * @return arquivo espelhado gravado * * @throws IOException quando ocorrer falha de leitura ou escrita * @throws GitAPIException quando a inicializacao necessaria falhar * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File mirrorWorkspaceFile(File workspaceFile, String relativePath) throws IOException, GitAPIException {
        if (workspaceFile == null || !workspaceFile.exists() || !workspaceFile.isFile()) {
            throw new IOException("Arquivo de origem invalido para espelhamento.");
        }

        String content = Files.readString(workspaceFile.toPath(), StandardCharsets.UTF_8);
        return writeMirroredFile(relativePath, content);
    }

    /** * Marca um caminho relativo para inclusao ou atualizacao no indice do * repositorio interno. * * @param relativePath caminho relativo dentro do workspace_git * * @throws IOException quando o repositorio nao puder ser aberto * @throws GitAPIException quando o add falhar * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void addPath(String relativePath) throws IOException, GitAPIException {
        String safeRelativePath = normalizeRelativePath(relativePath);

        Git git = null;
        try {
            git = openGit();
            git.add().addFilepattern(safeRelativePath).call();
        } finally {
            closeQuietly(git);
        }
    }

    /** * Remove um caminho relativo do espelho local e o marca para remocao no * indice do repositorio interno. * * @param relativePath caminho relativo dentro do workspace_git * * @throws IOException quando ocorrer falha de IO * @throws GitAPIException quando a remocao no indice falhar * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void removePath(String relativePath) throws IOException, GitAPIException {
        String safeRelativePath = normalizeRelativePath(relativePath);
        File targetFile = resolveRepositoryFile(safeRelativePath);

        if (targetFile.exists() && targetFile.isFile()) {
            Files.delete(targetFile.toPath());
        }

        Git git = null;
        try {
            git = openGit();
            git.rm().addFilepattern(safeRelativePath).call();
        } finally {
            closeQuietly(git);
        }
    }

    /** * Cria um commit no repositorio interno quando houver mudancas pendentes. * * <p>Se nao houver diferenca staged ou unstaged, o metodo devolve string * vazia e nao cria commit desnecessario.</p> * * @param message mensagem de commit * @return hash do commit criado ou string vazia quando nao houve mudanca * * @throws IOException quando o repositorio nao puder ser aberto * @throws GitAPIException quando a operacao de commit falhar * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String commit(String message) throws IOException, GitAPIException {
        Git git = null;
        try {
            git = openGit();

            Status status = git.status().call();
            if (status.isClean()) {
                return "";
            }

            RevCommit commit = git.commit()
                    .setAll(true)
                    .setMessage(safeCommitMessage(message))
                    .setAuthor(defaultPerson())
                    .setCommitter(defaultPerson())
                    .call();

            return commit != null ? commit.getName() : "";
        } finally {
            closeQuietly(git);
        }
    }

    /** * Retorna o id do commit mais recente do repositorio interno. * * @return hash do ultimo commit ou string vazia quando ainda nao houver * historico * * @throws IOException quando o repositorio nao puder ser aberto * @throws GitAPIException quando a consulta ao log falhar * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String getHeadCommitId() throws IOException, GitAPIException {
        Git git = null;
        try {
            git = openGit();

            Iterable<RevCommit> commits = git.log().setMaxCount(1).call();
            for (RevCommit commit : commits) {
                return commit.getName();
            }

            return "";
        } finally {
            closeQuietly(git);
        }
    }

    /** * Retorna o conteudo versionado de um arquivo para um commit especifico. * * @param commitId hash do commit desejado * @param relativePath caminho relativo dentro do repositorio interno * @return conteudo textual do arquivo naquela revisao * * @throws IOException quando ocorrer falha de leitura do objeto git * @throws GitAPIException quando a abertura do repositorio falhar * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String readFileContentAtCommit(String commitId, String relativePath) throws IOException, GitAPIException {
        if (isBlank(commitId)) {
            return "";
        }

        String safeRelativePath = normalizeRelativePath(relativePath);

        Git git = null;
        RevWalk revWalk = null;
        TreeWalk treeWalk = null;

        try {
            git = openGit();

            ObjectId commitObjectId = git.getRepository().resolve(commitId);
            if (commitObjectId == null) {
                return "";
            }

            revWalk = new RevWalk(git.getRepository());
            RevCommit commit = revWalk.parseCommit(commitObjectId);
            RevTree tree = commit.getTree();

            treeWalk = TreeWalk.forPath(git.getRepository(), safeRelativePath, tree);
            if (treeWalk == null) {
                return "";
            }

            ObjectId blobId = treeWalk.getObjectId(0);
            ObjectLoader loader = git.getRepository().open(blobId);
            byte[] bytes = loader.getBytes();

            return new String(bytes, StandardCharsets.UTF_8);
        } finally {
            closeQuietly(treeWalk);
            closeQuietly(revWalk);
            closeQuietly(git);
        }
    }

    /** * Restaura um arquivo fisico do workspace real usando o conteudo * versionado em um commit especifico do repositorio interno. * * @param commitId hash do commit fonte * @param relativePath caminho relativo dentro do repositorio interno * @param workspaceTargetFile arquivo real a ser sobrescrito * * @return true quando a restauracao for concluida com sucesso * * @throws IOException quando ocorrer falha de IO * @throws GitAPIException quando a leitura do repositorio falhar * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public boolean restoreFileFromCommit(String commitId, String relativePath, File workspaceTargetFile)
            throws IOException, GitAPIException {

        if (workspaceTargetFile == null) {
            return false;
        }

        String content = readFileContentAtCommit(commitId, relativePath);
        if (content == null || content.length() == 0) {
            return false;
        }

        File parent = workspaceTargetFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        Files.write(workspaceTargetFile.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return true;
    }

    /** * Lista commits recentes do repositorio interno, respeitando o limite * informado. * * @param limit quantidade maxima de commits desejada * @return lista de hashes em ordem do mais recente para o mais antigo * * @throws IOException quando o repositorio nao puder ser aberto * @throws GitAPIException quando a consulta ao log falhar * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public List<String> listRecentCommitIds(int limit) throws IOException, GitAPIException {
        int safeLimit = limit > 0 ? limit : 10;

        List<String> commitIds = new ArrayList<String>();
        Git git = null;

        try {
            git = openGit();

            LogCommand logCommand = git.log().setMaxCount(safeLimit);
            Iterable<RevCommit> commits = logCommand.call();

            for (RevCommit commit : commits) {
                commitIds.add(commit.getName());
            }

            return commitIds;
        } finally {
            closeQuietly(git);
        }
    }

    /** * Retorna true quando o repositorio interno possui pelo menos um commit. * * @return true quando houver historico disponivel * * @throws IOException quando o repositorio nao puder ser aberto * @throws GitAPIException quando a consulta ao log falhar * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public boolean hasCommits() throws IOException, GitAPIException {
        return !isBlank(getHeadCommitId());
    }

    /** * Resolve um arquivo fisico dentro do workspace_git a partir de um caminho * relativo. * * @param relativePath caminho relativo dentro do repositorio interno * @return arquivo correspondente dentro do espelho local * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File resolveRepositoryFile(String relativePath) {
        String safeRelativePath = normalizeRelativePath(relativePath);
        return new File(getRepositoryDirectory(), safeRelativePath);
    }

    /** * Normaliza um caminho relativo para uso interno no repositorio. * * <p>O metodo remove barras invertidas, trim e barras iniciais redundantes. * O retorno nao deve ser usado para escapar do perimetro previsto pelo * chamador.</p> * * @param relativePath caminho relativo original * @return caminho relativo normalizado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String normalizeRelativePath(String relativePath) {
        if (relativePath == null) {
            return "";
        }

        String normalized = relativePath.trim().replace("\\", "/");
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    /** * Retorna o nome da branch atualmente resolvida no repositorio interno. * * @return nome da branch atual ou string vazia quando indisponivel * * @throws IOException quando o repositorio nao puder ser aberto * @throws GitAPIException quando a abertura necessaria falhar * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String getCurrentBranch() throws IOException, GitAPIException {
        Git git = null;
        try {
            git = openGit();
            String branch = git.getRepository().getBranch();
            return branch != null ? branch : "";
        } finally {
            closeQuietly(git);
        }
    }

    /** * Fecha silenciosamente recursos AutoCloseable usados internamente. * * @param closeable recurso a ser fechado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }

        try {
            closeable.close();
        } catch (Exception e) {
        }
    }

    /** * Retorna uma mensagem de commit segura. * * @param message mensagem original * @return mensagem normalizada e nao vazia * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String safeCommitMessage(String message) {
        if (isBlank(message)) {
            return DEFAULT_COMMIT_MESSAGE;
        }
        return message.trim();
    }

    /** * Retorna o identificador padrao de autor/committer do repositorio interno. * * @return identidade padrao do plugin * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private PersonIdent defaultPerson() {
        return new PersonIdent(DEFAULT_AUTHOR_NAME, DEFAULT_AUTHOR_EMAIL);
    }

    /** * Retorna true quando o valor informado for nulo ou vazio. * * @param value valor a ser testado * @return true quando o valor for branco * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}