package com.mcp.sailibrary.plugin.agent.tools.support;

import java.io.File;
import java.util.List;

import com.mcp.sailibrary.plugin.chat.context.model.NamedStructuralContext;
import com.mcp.sailibrary.plugin.chat.context.model.NamedStructuralContextType;
import com.mcp.sailibrary.plugin.chat.context.service.NamedStructuralContextSessionService;

/** * Resolve aliases de contexto estrutural nomeado para caminhos reais do * workspace. * * <p>Este helper permite que tools mutaveis aceitem o nome logico do alvo * estrutural no parametro target e convertam esse nome em um diretorio ou * arquivo real antes de aplicar a operacao.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class StructuralTargetResolver {

    private final File rootDirectory;

    /** * Inicializa o resolvedor com a raiz segura do projeto atual. * * @param rootDirectory raiz segura do projeto * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public StructuralTargetResolver(File rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    /** * Resolve um alias estrutural para o contexto nomeado correspondente. * * @param targetName nome logico do contexto estrutural * @return contexto estrutural encontrado ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
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

    /** * Resolve o diretorio base real de um alias estrutural. * * <p>Para FILE, retorna o diretorio pai do arquivo. Para PACKAGE e FOLDER, * retorna o proprio diretorio correspondente.</p> * * @param targetName nome logico do contexto estrutural * @return diretorio base resolvido ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File resolveBaseDirectory(String targetName) {
        NamedStructuralContext context = resolveContext(targetName);
        if (context == null) {
            return null;
        }

        File resolved = resolveContextToFile(context);
        if (resolved == null) {
            return null;
        }

        if (context.getType() == NamedStructuralContextType.FILE) {
            return resolved.getParentFile();
        }

        return resolved;
    }

    /** * Resolve o arquivo ou diretorio real correspondente ao contexto estrutural. * * @param context contexto estrutural nomeado * @return arquivo ou diretorio real correspondente * * @author Renato Tomaz Nati * @since 2026-05-20 */
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

    /** * Resolve o caminho relativo real de um alias estrutural. * * @param targetName nome logico do contexto estrutural * @return caminho relativo real ou string vazia * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String resolveRelativePath(String targetName) {
        NamedStructuralContext context = resolveContext(targetName);
        if (context == null) {
            return "";
        }

        if (!isBlank(context.getRelativePath())) {
            return normalizePath(context.getRelativePath());
        }

        File resolved = resolveContextToFile(context);
        if (resolved == null || rootDirectory == null) {
            return "";
        }

        try {
            String rootPath = rootDirectory.getCanonicalPath().replace("\\", "/");
            String filePath = resolved.getCanonicalPath().replace("\\", "/");

            if (filePath.startsWith(rootPath)) {
                String relative = filePath.substring(rootPath.length());
                while (relative.startsWith("/")) {
                    relative = relative.substring(1);
                }
                return relative;
            }
        } catch (Exception e) {
        }

        return "";
    }

    private String normalizePath(String value) {
        return value == null ? "" : value.trim().replace("\\", "/");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}