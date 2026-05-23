package com.mcp.sailibrary.plugin.agent.tools.support;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mcp.sailibrary.plugin.chat.context.model.NamedStructuralContext;
import com.mcp.sailibrary.plugin.chat.context.model.NamedStructuralContextType;
import com.mcp.sailibrary.plugin.chat.context.service.NamedStructuralContextSessionService;

/** * Resolve aliases de contexto estrutural nomeado para caminhos reais do * workspace, incluindo identificacao do projeto Eclipse e do modulo Maven mais * proximos. * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class StructuralTargetResolver {

    private final File rootDirectory;

    public StructuralTargetResolver(File rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public NamedStructuralContext resolveContext(String targetName) {
        if (isBlank(targetName)) {
            return null;
        }

        List<NamedStructuralContext> contexts =
                NamedStructuralContextSessionService.getInstance().getAll();

        for (int i = 0; i < contexts.size(); i++) {
            NamedStructuralContext context = contexts.get(i);
            if (context != null && targetName.equals(context.getName()) && context.isUsable()) {
                return context;
            }
        }

        return null;
    }

    public ResolvedStructuralTarget resolveTarget(String targetName) {
        NamedStructuralContext context = resolveContext(targetName);
        if (context == null) {
            return null;
        }

        File contextPath = resolveContextToFile(context);
        if (contextPath == null || !contextPath.exists()) {
            return null;
        }

        File baseDirectory = context.getType() == NamedStructuralContextType.FILE
                ? contextPath.getParentFile()
                : contextPath;

        if (baseDirectory == null || !baseDirectory.exists() || !baseDirectory.isDirectory()) {
            return null;
        }

        File owningProjectRoot = findNearestEclipseProjectRoot(baseDirectory);
        if (owningProjectRoot == null) {
            owningProjectRoot = rootDirectory;
        }

        File owningModuleRoot = findNearestMavenModuleRoot(baseDirectory);
        if (owningModuleRoot == null) {
            owningModuleRoot = owningProjectRoot;
        }

        String owningProjectName = readEclipseProjectName(owningProjectRoot);
        if (isBlank(owningProjectName)) {
            owningProjectName = owningProjectRoot.getName();
        }

        String relativeBasePathFromOwningProject = relativize(owningProjectRoot, baseDirectory);
        String relativeBasePathFromModule = relativize(owningModuleRoot, baseDirectory);

        String mirrorRelativeBasePath = owningProjectName;
        if (!isBlank(relativeBasePathFromOwningProject)) {
            mirrorRelativeBasePath = owningProjectName + "/" + relativeBasePathFromOwningProject;
        }

        ResolvedStructuralTarget resolved = new ResolvedStructuralTarget();
        resolved.setContext(context);
        resolved.setContextPath(contextPath);
        resolved.setBaseDirectory(baseDirectory);
        resolved.setOwningEclipseProjectRoot(owningProjectRoot);
        resolved.setOwningEclipseProjectName(owningProjectName);
        resolved.setOwningMavenModuleRoot(owningModuleRoot);
        resolved.setRelativeBasePathFromOwningProject(relativeBasePathFromOwningProject);
        resolved.setRelativeBasePathFromModule(relativeBasePathFromModule);
        resolved.setMirrorRelativeBasePath(mirrorRelativeBasePath);

        return resolved;
    }

    public File resolveBaseDirectory(String targetName) {
        ResolvedStructuralTarget resolved = resolveTarget(targetName);
        return resolved != null ? resolved.getBaseDirectory() : null;
    }

    public File resolveContextToFile(NamedStructuralContext context) {
        if (context == null) {
            return null;
        }

        if (!isBlank(context.getFilePath())) {
            File file = new File(context.getFilePath());
            if (file.exists()) {
                return file;
            }
        }

        if (!isBlank(context.getRelativePath()) && rootDirectory != null) {
            File file = new File(rootDirectory, context.getRelativePath());
            if (file.exists()) {
                return file;
            }
        }

        return null;
    }

    public File resolveChildFile(String targetName, String childRelativePath) {
        ResolvedStructuralTarget resolved = resolveTarget(targetName);
        if (resolved == null || !resolved.isUsable()) {
            return null;
        }

        String normalizedChild = normalizePath(childRelativePath);
        while (normalizedChild.startsWith("/")) {
            normalizedChild = normalizedChild.substring(1);
        }

        return new File(resolved.getBaseDirectory(), normalizedChild);
    }

    public String resolveRelativePath(String targetName) {
        ResolvedStructuralTarget resolved = resolveTarget(targetName);
        return resolved != null ? resolved.getRelativeBasePathFromOwningProject() : "";
    }

    public String resolveMirrorBaseRelativePath(String targetName) {
        ResolvedStructuralTarget resolved = resolveTarget(targetName);
        return resolved != null ? resolved.getMirrorRelativeBasePath() : "";
    }

    public String resolveChildRelativePath(String targetName, String childRelativePath) {
        ResolvedStructuralTarget resolved = resolveTarget(targetName);
        if (resolved == null) {
            return "";
        }

        String base = normalizePath(resolved.getRelativeBasePathFromOwningProject());
        String child = normalizePath(childRelativePath);

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

    private File findNearestEclipseProjectRoot(File start) {
        File cursor = start;
        while (cursor != null && isInsideRoot(cursor)) {
            File projectMarker = new File(cursor, ".project");
            if (projectMarker.exists() && projectMarker.isFile()) {
                return cursor;
            }

            if (sameDirectory(cursor, rootDirectory)) {
                break;
            }

            cursor = cursor.getParentFile();
        }

        return null;
    }

    private File findNearestMavenModuleRoot(File start) {
        File cursor = start;
        while (cursor != null && isInsideRoot(cursor)) {
            File pom = new File(cursor, "pom.xml");
            if (pom.exists() && pom.isFile()) {
                return cursor;
            }

            if (sameDirectory(cursor, rootDirectory)) {
                break;
            }

            cursor = cursor.getParentFile();
        }

        return null;
    }

    private String readEclipseProjectName(File projectRoot) {
        if (projectRoot == null) {
            return "";
        }

        File projectFile = new File(projectRoot, ".project");
        if (!projectFile.exists() || !projectFile.isFile()) {
            return "";
        }

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(projectFile));
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }

            Matcher matcher = Pattern.compile("<name>\\s*([^<]+?)\\s*</name>").matcher(sb.toString());
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        } catch (Exception e) {
            return "";
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception e) {
                }
            }
        }

        return "";
    }

    private String relativize(File base, File child) {
        if (base == null || child == null) {
            return "";
        }

        try {
            String basePath = base.getCanonicalPath().replace("\\", "/");
            String childPath = child.getCanonicalPath().replace("\\", "/");

            if (childPath.startsWith(basePath)) {
                String relative = childPath.substring(basePath.length());
                while (relative.startsWith("/")) {
                    relative = relative.substring(1);
                }
                return relative;
            }
        } catch (Exception e) {
        }

        return "";
    }

    private boolean isInsideRoot(File candidate) {
        if (candidate == null || rootDirectory == null) {
            return false;
        }

        try {
            String rootPath = rootDirectory.getCanonicalPath().replace("\\", "/");
            String candidatePath = candidate.getCanonicalPath().replace("\\", "/");
            return candidatePath.startsWith(rootPath);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean sameDirectory(File first, File second) {
        if (first == null || second == null) {
            return false;
        }

        try {
            return first.getCanonicalPath().equals(second.getCanonicalPath());
        } catch (Exception e) {
            return first.getAbsolutePath().equals(second.getAbsolutePath());
        }
    }

    private String normalizePath(String value) {
        return value == null ? "" : value.trim().replace("\\", "/");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}