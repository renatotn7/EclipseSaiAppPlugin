package com.mcp.sailibrary.plugin.agent.context.analise;

import java.io.File;

/** * --- * yaml_header: * version: "1.0" * dependencies: * - java.io.File * purpose: "Resolver a estrutura persistente da memoria por projeto dentro do home do usuario." * design_pattern: "Value Object / Path Resolver" * --- */
public class ProjectMemoryPaths {

    private File homeSaiDirectory;
    private File projectsDirectory;
    private File projectDirectory;
    private File projectMemoryFile;
    private File toolHistoryFile;
    private File dependencySnapshotFile;
    private File discoveredPatternsFile;
    private File branchContextFile;

    /**
 * Monta os caminhos fisicos da memoria persistente do projeto.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
    public ProjectMemoryPaths(String projectKey) {
        String userHome = System.getProperty("user.home");
        this.homeSaiDirectory = new File(userHome, ".sai");
        this.projectsDirectory = new File(homeSaiDirectory, "projects");
        this.projectDirectory = new File(projectsDirectory, projectKey);
        this.projectMemoryFile = new File(projectDirectory, "project_memory.json");
        this.toolHistoryFile = new File(projectDirectory, "tool_history.json");
        this.dependencySnapshotFile = new File(projectDirectory, "dependency_snapshot.json");
        this.discoveredPatternsFile = new File(projectDirectory, "discovered_patterns.json");
        this.branchContextFile = new File(projectDirectory, "branch_context.json");
    }

    public File getHomeSaiDirectory() {
        return homeSaiDirectory;
    }

    public File getProjectsDirectory() {
        return projectsDirectory;
    }

    public File getProjectDirectory() {
        return projectDirectory;
    }

    public File getProjectMemoryFile() {
        return projectMemoryFile;
    }

    public File getToolHistoryFile() {
        return toolHistoryFile;
    }

    public File getDependencySnapshotFile() {
        return dependencySnapshotFile;
    }

    public File getDiscoveredPatternsFile() {
        return discoveredPatternsFile;
    }

    public File getBranchContextFile() {
        return branchContextFile;
    }
}