package com.mcp.sailibrary.plugin.chat.context.decorators;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;

import com.mcp.sailibrary.plugin.chat.context.model.NamedContextTargetRole;
import com.mcp.sailibrary.plugin.chat.context.model.NamedStructuralContext;
import com.mcp.sailibrary.plugin.chat.context.model.NamedStructuralContextType;
import com.mcp.sailibrary.plugin.chat.context.service.NamedStructuralContextSessionService;

/* yaml_header: version: "1.2" purpose: "Adicionar overlay de contexto estrutural no Project Explorer e Package Explorer com suporte a ICompilationUnit." libraries: - org.eclipse.jface.viewers.ILightweightLabelDecorator: runtime - org.eclipse.core.resources.IFile: runtime - org.eclipse.jdt.core.IPackageFragment: runtime - org.eclipse.jdt.core.ICompilationUnit: runtime */
public class NamedStructuralContextDecorator implements ILightweightLabelDecorator {

    public static final String DECORATOR_ID = "com.mcp.sailibrary.plugin.chat.context.decorators.NamedStructuralContextDecorator";

    private final NamedStructuralContextSessionService sessionService;
    private final ExplorerOverlayIconService overlayIconService;

    public NamedStructuralContextDecorator() {
        this.sessionService = NamedStructuralContextSessionService.getInstance();
        this.overlayIconService = ExplorerOverlayIconService.getInstance();
    }

    @Override
    public void decorate(Object element, IDecoration decoration) {
        if (element == null || decoration == null) {
            return;
        }

        NamedStructuralContext context = resolveContext(element);
        if (context == null || context.getRole() == null) {
            return;
        }

        ImageDescriptor overlay = resolveOverlay(context.getRole());
        if (overlay == null) {
            return;
        }

        decoration.addOverlay(overlay, IDecoration.TOP_RIGHT);
        decoration.addSuffix(" " + context.getRoleMarker());
    }

    private NamedStructuralContext resolveContext(Object element) {
        if (element instanceof ICompilationUnit) {
            return resolveCompilationUnitContext((ICompilationUnit) element);
        }

        if (element instanceof IFile) {
            return resolveFileContext((IFile) element);
        }

        if (element instanceof IPackageFragment) {
            return resolvePackageContext((IPackageFragment) element);
        }

        if (element instanceof IContainer) {
            return resolveFolderContext((IContainer) element);
        }

        if (element instanceof IAdaptable) {
            ICompilationUnit compilationUnit = (ICompilationUnit) ((IAdaptable) element).getAdapter(ICompilationUnit.class);
            if (compilationUnit != null) {
                return resolveCompilationUnitContext(compilationUnit);
            }

            IFile file = (IFile) ((IAdaptable) element).getAdapter(IFile.class);
            if (file != null) {
                return resolveFileContext(file);
            }

            IPackageFragment pkg = (IPackageFragment) ((IAdaptable) element).getAdapter(IPackageFragment.class);
            if (pkg != null) {
                return resolvePackageContext(pkg);
            }

            IContainer folder = (IContainer) ((IAdaptable) element).getAdapter(IContainer.class);
            if (folder != null) {
                return resolveFolderContext(folder);
            }
        }

        return null;
    }

    private NamedStructuralContext resolveCompilationUnitContext(ICompilationUnit compilationUnit) {
        if (compilationUnit == null) {
            return null;
        }

        String absolutePath = "";
        String relativePath = "";

        try {
            if (compilationUnit.getResource() != null) {
                if (compilationUnit.getResource().getLocation() != null) {
                    absolutePath = normalizePath(compilationUnit.getResource().getLocation().toFile().getAbsolutePath());
                }
                if (compilationUnit.getResource().getProjectRelativePath() != null) {
                    relativePath = compilationUnit.getResource().getProjectRelativePath().toString().replace("\\", "/");
                }
            }
        } catch (Exception e) {
        }

        java.util.List<NamedStructuralContext> all = sessionService.getAll();
        for (int i = 0; i < all.size(); i++) {
            NamedStructuralContext current = all.get(i);
            if (current == null || current.getType() != NamedStructuralContextType.FILE) {
                continue;
            }

            if (safeEquals(current.getFilePath(), absolutePath) || safeEquals(current.getRelativePath(), relativePath)) {
                return current;
            }
        }

        return null;
    }

    private NamedStructuralContext resolveFileContext(IFile file) {
        if (file == null) {
            return null;
        }

        String absolutePath = "";
        if (file.getLocation() != null) {
            absolutePath = normalizePath(file.getLocation().toFile().getAbsolutePath());
        }

        String relativePath = file.getProjectRelativePath() != null
                ? file.getProjectRelativePath().toString().replace("\\", "/")
                : "";

        java.util.List<NamedStructuralContext> all = sessionService.getAll();
        for (int i = 0; i < all.size(); i++) {
            NamedStructuralContext current = all.get(i);
            if (current == null || current.getType() != NamedStructuralContextType.FILE) {
                continue;
            }

            if (safeEquals(current.getFilePath(), absolutePath) || safeEquals(current.getRelativePath(), relativePath)) {
                return current;
            }
        }

        return null;
    }

    private NamedStructuralContext resolvePackageContext(IPackageFragment pkg) {
        if (pkg == null) {
            return null;
        }

        String packageName = pkg.getElementName();
        String relativePath = "";
        String absolutePath = "";

        try {
            if (pkg.getResource() != null) {
                if (pkg.getResource().getProjectRelativePath() != null) {
                    relativePath = pkg.getResource().getProjectRelativePath().toString().replace("\\", "/");
                }
                if (pkg.getResource().getLocation() != null) {
                    absolutePath = normalizePath(pkg.getResource().getLocation().toFile().getAbsolutePath());
                }
            }
        } catch (Exception e) {
        }

        java.util.List<NamedStructuralContext> all = sessionService.getAll();
        for (int i = 0; i < all.size(); i++) {
            NamedStructuralContext current = all.get(i);
            if (current == null || current.getType() != NamedStructuralContextType.PACKAGE) {
                continue;
            }

            if (safeEquals(current.getPackageName(), packageName)
                    || safeEquals(current.getRelativePath(), relativePath)
                    || safeEquals(current.getFilePath(), absolutePath)) {
                return current;
            }
        }

        return null;
    }

    private NamedStructuralContext resolveFolderContext(IContainer folder) {
        if (folder == null) {
            return null;
        }

        String absolutePath = "";
        if (folder.getLocation() != null) {
            absolutePath = normalizePath(folder.getLocation().toFile().getAbsolutePath());
        }

        String relativePath = folder.getProjectRelativePath() != null
                ? folder.getProjectRelativePath().toString().replace("\\", "/")
                : "";

        java.util.List<NamedStructuralContext> all = sessionService.getAll();
        for (int i = 0; i < all.size(); i++) {
            NamedStructuralContext current = all.get(i);
            if (current == null || current.getType() != NamedStructuralContextType.FOLDER) {
                continue;
            }

            if (safeEquals(current.getFilePath(), absolutePath)
                    || safeEquals(current.getRelativePath(), relativePath)) {
                return current;
            }
        }

        return null;
    }

    private ImageDescriptor resolveOverlay(NamedContextTargetRole role) {
        if (role == NamedContextTargetRole.PRIMARY) {
            return overlayIconService.getOverlayDescriptor("icons/icon_star@2x.png");
        }

        if (role == NamedContextTargetRole.EDITABLE) {
            return overlayIconService.getOverlayDescriptor("icons/icon_pen@2x.png");
        }

        return overlayIconService.getOverlayDescriptor("icons/icon_bookmark@2x.png");
    }

    @Override
    public void addListener(ILabelProviderListener listener) {
        // Nenhum estado observavel local para notificar nesta implementacao
    }

    @Override
    public void dispose() {
        // Nada a liberar localmente aqui
    }

    @Override
    public boolean isLabelProperty(Object element, String property) {
        return false;
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {
        // Nenhum listener mantido localmente
    }

    private String normalizePath(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replace("\\", "/");
    }

    private boolean safeEquals(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }
}