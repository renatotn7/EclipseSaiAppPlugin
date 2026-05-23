package com.mcp.sailibrary.plugin.agent.tools.support;

import java.io.File;

import com.mcp.sailibrary.plugin.chat.context.model.NamedStructuralContext;
import com.mcp.sailibrary.plugin.chat.context.model.NamedStructuralContextType;

/** * Representa a resolucao completa de um alias estrutural para uso seguro em * mutacoes e espelhamento. * * <p>Este objeto separa claramente: * <ul> * <li>o contexto estrutural original da sessao</li> * <li>o diretorio base real onde a operacao deve acontecer</li> * <li>o projeto Eclipse dono do contexto</li> * <li>o modulo Maven mais proximo</li> * <li>o caminho base de espelho no workspace_git</li> * </ul> * </p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class ResolvedStructuralTarget {

    private NamedStructuralContext context;
    private File contextPath;
    private File baseDirectory;
    private File owningEclipseProjectRoot;
    private String owningEclipseProjectName;
    private File owningMavenModuleRoot;
    private String relativeBasePathFromOwningProject;
    private String relativeBasePathFromModule;
    private String mirrorRelativeBasePath;

    public NamedStructuralContext getContext() {
        return context;
    }

    public void setContext(NamedStructuralContext context) {
        this.context = context;
    }

    public File getContextPath() {
        return contextPath;
    }

    public void setContextPath(File contextPath) {
        this.contextPath = contextPath;
    }

    public File getBaseDirectory() {
        return baseDirectory;
    }

    public void setBaseDirectory(File baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    public File getOwningEclipseProjectRoot() {
        return owningEclipseProjectRoot;
    }

    public void setOwningEclipseProjectRoot(File owningEclipseProjectRoot) {
        this.owningEclipseProjectRoot = owningEclipseProjectRoot;
    }

    public String getOwningEclipseProjectName() {
        return owningEclipseProjectName;
    }

    public void setOwningEclipseProjectName(String owningEclipseProjectName) {
        this.owningEclipseProjectName = safeTrim(owningEclipseProjectName);
    }

    public File getOwningMavenModuleRoot() {
        return owningMavenModuleRoot;
    }

    public void setOwningMavenModuleRoot(File owningMavenModuleRoot) {
        this.owningMavenModuleRoot = owningMavenModuleRoot;
    }

    public String getRelativeBasePathFromOwningProject() {
        return relativeBasePathFromOwningProject;
    }

    public void setRelativeBasePathFromOwningProject(String relativeBasePathFromOwningProject) {
        this.relativeBasePathFromOwningProject = normalizePath(relativeBasePathFromOwningProject);
    }

    public String getRelativeBasePathFromModule() {
        return relativeBasePathFromModule;
    }

    public void setRelativeBasePathFromModule(String relativeBasePathFromModule) {
        this.relativeBasePathFromModule = normalizePath(relativeBasePathFromModule);
    }

    public String getMirrorRelativeBasePath() {
        return mirrorRelativeBasePath;
    }

    public void setMirrorRelativeBasePath(String mirrorRelativeBasePath) {
        this.mirrorRelativeBasePath = normalizePath(mirrorRelativeBasePath);
    }

    public NamedStructuralContextType getContextType() {
        return context != null ? context.getType() : null;
    }

    public boolean isUsable() {
        return context != null
                && context.isUsable()
                && baseDirectory != null
                && baseDirectory.exists()
                && baseDirectory.isDirectory()
                && owningEclipseProjectRoot != null
                && owningEclipseProjectRoot.exists()
                && owningEclipseProjectRoot.isDirectory()
                && owningEclipseProjectName != null
                && owningEclipseProjectName.trim().length() > 0
                && mirrorRelativeBasePath != null
                && mirrorRelativeBasePath.trim().length() > 0;
    }

    private String normalizePath(String value) {
        return value == null ? "" : value.trim().replace("\\", "/");
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}