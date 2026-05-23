package com.mcp.sailibrary.plugin.agent.context.mutation.model;

import java.io.File;

/** * Representa o contexto semantico minimo de uma mutacao em execucao. * * <p>Este objeto evita proliferacao de parametros soltos entre tools, * facades, services e repositories, concentrando informacoes como projeto, * branch observada, ferramenta de origem, contexto-alvo e resumo da * instrucao.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class MutationContext {

    private File projectRootDirectory;
    private String projectKey;
    private String branchName;
    private String toolName;
    private String instructionSummary;
    private String targetName;
    private MutationOrigin origin;
    private String targetAbsoluteBasePath;
    private String targetRelativeBasePath;
    private String targetOwningProjectRootPath;
    private String targetOwningProjectName;
    private String targetMirrorBaseRelativePath;
    /** * Retorna o diretorio raiz do projeto associado a mutacao. * * @return raiz fisica do projeto * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File getProjectRootDirectory() {
        return projectRootDirectory;
    }

    public void setProjectRootDirectory(File projectRootDirectory) {
        this.projectRootDirectory = projectRootDirectory;
    }

    /** * Retorna a chave estavel do projeto dentro da estrutura .sai. * * @return chave do projeto * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String getProjectKey() {
        return projectKey;
    }

    public void setProjectKey(String projectKey) {
        this.projectKey = safeTrim(projectKey);
    }

    /** * Retorna a branch observada no momento da mutacao. * * @return nome da branch percebida * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = safeTrim(branchName);
    }

    /** * Retorna o nome da tool responsavel pela operacao. * * @return nome homologado da ferramenta de origem * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = safeTrim(toolName);
    }

    /** * Retorna um resumo curto da instrucao que originou a mutacao. * * @return resumo semantico da instrucao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String getInstructionSummary() {
        return instructionSummary;
    }

    public void setInstructionSummary(String instructionSummary) {
        this.instructionSummary = safeTrim(instructionSummary);
    }

    /** * Retorna o nome logico do alvo de contexto associado a mutacao. * * <p>Exemplos tipicos incluem nomes de contexto estrutural como service, * dao, xml, config ou aliases nomeados da sessao.</p> * * @return nome do alvo semantico * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = safeTrim(targetName);
    }

    /** * Retorna a origem funcional da mutacao. * * @return origem da mutacao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public MutationOrigin getOrigin() {
        return origin;
    }

    public void setOrigin(MutationOrigin origin) {
        this.origin = origin;
    }

    /** * Retorna true quando o contexto possui dados minimos para uso seguro. * * @return true quando projeto e origem estao minimamente definidos * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public boolean isUsable() {
        return projectRootDirectory != null
                && !isBlank(projectKey)
                && origin != null;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
    public String getTargetAbsoluteBasePath() {
        return targetAbsoluteBasePath;
    }

    public void setTargetAbsoluteBasePath(String targetAbsoluteBasePath) {
        this.targetAbsoluteBasePath = safeTrim(targetAbsoluteBasePath);
    }

    public String getTargetRelativeBasePath() {
        return targetRelativeBasePath;
    }

    public void setTargetRelativeBasePath(String targetRelativeBasePath) {
        this.targetRelativeBasePath = safeTrim(targetRelativeBasePath);
    }

    public String getTargetOwningProjectRootPath() {
        return targetOwningProjectRootPath;
    }

    public void setTargetOwningProjectRootPath(String targetOwningProjectRootPath) {
        this.targetOwningProjectRootPath = safeTrim(targetOwningProjectRootPath);
    }

    public String getTargetOwningProjectName() {
        return targetOwningProjectName;
    }

    public void setTargetOwningProjectName(String targetOwningProjectName) {
        this.targetOwningProjectName = safeTrim(targetOwningProjectName);
    }

    public String getTargetMirrorBaseRelativePath() {
        return targetMirrorBaseRelativePath;
    }

    public void setTargetMirrorBaseRelativePath(String targetMirrorBaseRelativePath) {
        this.targetMirrorBaseRelativePath = safeTrim(targetMirrorBaseRelativePath);
    }
}