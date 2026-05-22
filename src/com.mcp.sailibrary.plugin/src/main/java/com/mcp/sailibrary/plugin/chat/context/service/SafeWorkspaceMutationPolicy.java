package com.mcp.sailibrary.plugin.chat.context.service;

import java.io.File;

import com.mcp.sailibrary.plugin.chat.context.model.NamedContextTargetRole;
import com.mcp.sailibrary.plugin.chat.context.model.NamedStructuralContext;
import com.mcp.sailibrary.plugin.chat.context.model.NamedStructuralContextType;

/* yaml_header: version: "1.0" purpose: "Aplicar regras de seguranca para criacao, alteracao e exclusao de artefatos no workspace." libraries: - java.io.File: runtime */
public class SafeWorkspaceMutationPolicy {

    private final File rootDirectory;
    private final NamedStructuralContextSessionService structuralSessionService;
    private final CreatedArtifactRegistryService registryService;

    public SafeWorkspaceMutationPolicy(File rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.structuralSessionService = NamedStructuralContextSessionService.getInstance();
        this.registryService = new CreatedArtifactRegistryService(rootDirectory);
    }

    public boolean canCreateFile(String targetName, String relativePath) {
        if (isBlank(targetName) || isBlank(relativePath)) {
            return false;
        }

        NamedStructuralContext context = structuralSessionService.findByName(targetName);
        if (context == null) {
            return false;
        }

        if (context.getRole() != NamedContextTargetRole.EDITABLE) {
            return false;
        }

        if (context.getType() != NamedStructuralContextType.PACKAGE
                && context.getType() != NamedStructuralContextType.FOLDER) {
            return false;
        }

        File baseDirectory = resolveBaseDirectory(context);
        if (baseDirectory == null) {
            return false;
        }

        File targetFile = new File(baseDirectory, relativePath);
        return isInsideRoot(targetFile);
    }

    public boolean canCreatePackage(String targetName, String relativePath) {
        return canCreateFile(targetName, relativePath);
    }

    public boolean canUpdateFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return false;
        }

        if (registryService.isCreatedFile(file)) {
            return true;
        }

        NamedStructuralContext explicitFileContext = findExplicitFileContext(file);
        if (explicitFileContext == null) {
            return false;
        }

        return explicitFileContext.getRole() == NamedContextTargetRole.EDITABLE
                || explicitFileContext.getRole() == NamedContextTargetRole.PRIMARY;
    }

    public boolean canDeleteFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return false;
        }

        return registryService.isCreatedFile(file);
    }

    public File createBackupFile(File originalFile) {
        if (originalFile == null) {
            return null;
        }

        File backupFile = new File(originalFile.getAbsolutePath() + ".bkp");
        if (!backupFile.exists()) {
            return backupFile;
        }

        for (int i = 2; i < 1000; i++) {
            File candidate = new File(originalFile.getAbsolutePath() + ".bkp" + i);
            if (!candidate.exists()) {
                return candidate;
            }
        }

        return new File(originalFile.getAbsolutePath() + "." + System.currentTimeMillis() + ".bkp");
    }

    public CreatedArtifactRegistryService getRegistryService() {
        return registryService;
    }

    private NamedStructuralContext findExplicitFileContext(File file) {
        java.util.List<NamedStructuralContext> contexts = structuralSessionService.getAll();
        String normalized = normalizePath(file);

        for (int i = 0; i < contexts.size(); i++) {
            NamedStructuralContext context = contexts.get(i);
            if (context == null || context.getType() != NamedStructuralContextType.FILE) {
                continue;
            }

            if (normalized.equals(normalizePath(context.getFilePath()))) {
                return context;
            }
        }

        return null;
    }

    private File resolveBaseDirectory(NamedStructuralContext context) {
        if (context == null) {
            return null;
        }

        if (!isBlank(context.getFilePath())) {
            File base = new File(context.getFilePath());
            if (base.exists() && base.isDirectory()) {
                return base;
            }
        }

        if (!isBlank(context.getRelativePath())) {
            File base = new File(rootDirectory, context.getRelativePath());
            if (base.exists() && base.isDirectory()) {
                return base;
            }
        }

        return null;
    }

    private boolean isInsideRoot(File file) {
        if (file == null || rootDirectory == null) {
            return false;
        }

        try {
            String root = rootDirectory.getCanonicalPath();
            String target = file.getCanonicalPath();
            return target.startsWith(root);
        } catch (Exception e) {
            return false;
        }
    }

    private String normalizePath(File file) {
        try {
            return file.getCanonicalPath().replace("\\", "/");
        } catch (Exception e) {
            return file.getAbsolutePath().replace("\\", "/");
        }
    }

    private String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        return path.replace("\\", "/").trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}