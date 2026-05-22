package com.mcp.sailibrary.plugin.agent.context.mutation;

import java.io.File;

/** * Resolve os caminhos fisicos da infraestrutura de mutacao persistente por * projeto dentro do diretorio .sai do usuario. * * <p>Esta classe centraliza a localizacao dos artefatos de mutacao, incluindo * journal semantico, estado de undo/redo, metadados do repositorio interno e * espelho versionado controlado por JGit.</p> * * <p>A responsabilidade desta classe e apenas resolver e expor caminhos. * Leitura, escrita, inicializacao de repositorio e regras de negocio devem * permanecer em componentes especificos da camada de mutacao.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class ProjectMutationPaths {

    private final File homeSaiDirectory;
    private final File projectsDirectory;
    private final File projectDirectory;

    private final File mutationJournalFile;
    private final File mutationStateFile;
    private final File mutationRepoMetaFile;
    private final File workspaceGitDirectory;

    /** * Monta os caminhos fisicos da camada de mutacao persistente para o projeto * informado. * * <p>O parametro {@code projectKey} deve ser uma chave estavel e segura do * projeto, normalmente derivada da raiz canonica do projeto e normalizada * por hash ou identificador curto.</p> * * @param projectKey chave estavel do projeto dentro da pasta * {@code ~/.sai/projects} * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public ProjectMutationPaths(String projectKey) {
        String userHome = System.getProperty("user.home");

        this.homeSaiDirectory = new File(userHome, ".sai");
        this.projectsDirectory = new File(homeSaiDirectory, "projects");
        this.projectDirectory = new File(projectsDirectory, safeProjectKey(projectKey));

        this.mutationJournalFile = new File(projectDirectory, "mutation_journal.json");
        this.mutationStateFile = new File(projectDirectory, "mutation_state.json");
        this.mutationRepoMetaFile = new File(projectDirectory, "mutation_repo_meta.json");
        this.workspaceGitDirectory = new File(projectDirectory, "workspace_git");
    }

    /** * Retorna o diretorio raiz .sai no home do usuario. * * @return diretorio ~/.sai * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File getHomeSaiDirectory() {
        return homeSaiDirectory;
    }

    /** * Retorna o diretorio agregador de projetos persistidos pela aplicacao. * * @return diretorio ~/.sai/projects * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File getProjectsDirectory() {
        return projectsDirectory;
    }

    /** * Retorna o diretorio persistente especifico do projeto atual. * * @return diretorio ~/.sai/projects/{projectKey} * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File getProjectDirectory() {
        return projectDirectory;
    }

    /** * Retorna o arquivo de journal semantico de mutacoes do projeto. * * <p>Este arquivo deve armazenar batches, operacoes, caminhos tocados, * commits before/after e metadados de origem da mutacao.</p> * * @return arquivo mutation_journal.json * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File getMutationJournalFile() {
        return mutationJournalFile;
    }

    /** * Retorna o arquivo de estado operacional de undo e redo do projeto. * * <p>Este arquivo deve conter a pilha de undo, pilha de redo e ponteiros * auxiliares da camada de restauracao.</p> * * @return arquivo mutation_state.json * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File getMutationStateFile() {
        return mutationStateFile;
    }

    /** * Retorna o arquivo de metadados do repositorio interno de mutacao. * * <p>Este arquivo deve registrar informacoes como inicializacao do repo * JGit, schema local, branch observada e configuracoes auxiliares do * espelho versionado.</p> * * @return arquivo mutation_repo_meta.json * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File getMutationRepoMetaFile() {
        return mutationRepoMetaFile;
    }

    /** * Retorna o diretorio do repositorio git interno usado para versionar os * artefatos mutados pela IA/plugin. * * <p>Este diretorio deve conter um repositorio JGit isolado do repositorio * real do usuario, evitando qualquer interferencia no historico oficial do * projeto de trabalho.</p> * * @return diretorio workspace_git * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File getWorkspaceGitDirectory() {
        return workspaceGitDirectory;
    }

    /** * Retorna o diretorio .git interno do repositorio versionado local. * * @return diretorio workspace_git/.git * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File getWorkspaceGitMetadataDirectory() {
        return new File(workspaceGitDirectory, ".git");
    }

    /** * Retorna true quando a estrutura minima fisica da camada de mutacao ja * existe em disco. * * <p>Esta verificacao e util apenas como heuristica inicial. A existencia * fisica dos caminhos nao substitui validacoes de integridade semantica dos * arquivos JSON e do repositorio JGit.</p> * * @return true quando o diretorio do projeto de mutacao existe * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public boolean existsProjectStructure() {
        return projectDirectory.exists() && projectDirectory.isDirectory();
    }

    /** * Retorna true quando o diretorio esperado do repositorio interno ja existe. * * @return true quando workspace_git existe como diretorio * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public boolean existsWorkspaceGitDirectory() {
        return workspaceGitDirectory.exists() && workspaceGitDirectory.isDirectory();
    }

    /** * Retorna true quando o metadata .git interno ja esta presente. * * @return true quando workspace_git/.git existe como diretorio * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public boolean existsWorkspaceGitMetadataDirectory() {
        File gitMetadata = getWorkspaceGitMetadataDirectory();
        return gitMetadata.exists() && gitMetadata.isDirectory();
    }

    /** * Garante que a chave de projeto usada na resolucao de caminhos seja segura * para uso em nomes de diretorio. * * <p>O metodo preserva letras, numeros, underline e hifen. Qualquer outro * caractere e convertido para underscore. Em caso de valor vazio, retorna * um nome defensivo padrao.</p> * * @param projectKey chave original recebida do chamador * @return chave segura para uso em caminho de diretorio * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String safeProjectKey(String projectKey) {
        if (projectKey == null || projectKey.trim().length() == 0) {
            return "unknown_project";
        }

        String normalized = projectKey.trim();
        normalized = normalized.replace("\\", "_");
        normalized = normalized.replace("/", "_");
        normalized = normalized.replaceAll("[^a-zA-Z0-9_\\-]", "_");

        if (normalized.length() == 0) {
            return "unknown_project";
        }

        return normalized;
    }
}