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

import com.mcp.sailibrary.plugin.chat.blocks.model.StructuralArtifactMutationState;
import com.mcp.sailibrary.plugin.chat.blocks.service.StructuralArtifactMutationStateService;
import com.mcp.sailibrary.plugin.chat.context.model.NamedContextTargetRole;
import com.mcp.sailibrary.plugin.chat.context.model.NamedStructuralContext;
import com.mcp.sailibrary.plugin.chat.context.model.NamedStructuralContextType;
import com.mcp.sailibrary.plugin.chat.context.service.NamedStructuralContextSessionService;


/** * Adiciona decoracao visual de contexto estrutural no Project Explorer e no * Package Explorer. * * <p>Esta implementacao identifica arquivos, packages e pastas registrados na * sessao estrutural e aplica: * <ul> * <li>overlay de role estrutural no canto superior direito</li> * <li>overlay de estado de mutacao no canto inferior direito</li> * <li>marcador textual curto no label do item</li> * </ul> * </p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class NamedStructuralContextDecorator implements ILightweightLabelDecorator {

    public static final String DECORATOR_ID = "com.mcp.sailibrary.plugin.chat.context.decorators.NamedStructuralContextDecorator";

    private final NamedStructuralContextSessionService sessionService;
    private final ExplorerOverlayIconService overlayIconService;
    private final StructuralArtifactMutationStateService mutationStateService;

    /** * Inicializa o decorator estrutural do explorer. * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public NamedStructuralContextDecorator() {
        this.sessionService = NamedStructuralContextSessionService.getInstance();
        this.overlayIconService = ExplorerOverlayIconService.getInstance();
        this.mutationStateService = new StructuralArtifactMutationStateService();
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

        ImageDescriptor roleOverlay = resolveRoleOverlay(context.getRole());
        if (roleOverlay != null) {
            decoration.addOverlay(roleOverlay, IDecoration.TOP_RIGHT);
        }

        StructuralArtifactMutationState mutationState = resolveMutationState(element);
        ImageDescriptor mutationOverlay = resolveMutationOverlay(mutationState);
        if (mutationOverlay != null) {
            decoration.addOverlay(mutationOverlay, IDecoration.BOTTOM_RIGHT);
        }

        decoration.addSuffix(" " + context.getRoleMarker());
    }

    /** * Resolve o contexto estrutural associado ao item do explorer. * * @param element elemento visual do explorer * @return contexto estrutural correspondente ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
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

    /** * Resolve o estado visual de mutacao do item decorado. * * @param element elemento visual do explorer * @return estado de mutacao correspondente * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private StructuralArtifactMutationState resolveMutationState(Object element) {
        try {
            if (element instanceof ICompilationUnit) {
                return mutationStateService.resolveForCompilationUnit((ICompilationUnit) element);
            }

            if (element instanceof IFile) {
                return mutationStateService.resolveForFile((IFile) element);
            }

            if (element instanceof IPackageFragment) {
                return mutationStateService.resolveForPackage((IPackageFragment) element);
            }

            if (element instanceof IContainer) {
                return mutationStateService.resolveForContainer((IContainer) element);
            }

            if (element instanceof IAdaptable) {
                ICompilationUnit compilationUnit = (ICompilationUnit) ((IAdaptable) element).getAdapter(ICompilationUnit.class);
                if (compilationUnit != null) {
                    return mutationStateService.resolveForCompilationUnit(compilationUnit);
                }

                IFile file = (IFile) ((IAdaptable) element).getAdapter(IFile.class);
                if (file != null) {
                    return mutationStateService.resolveForFile(file);
                }

                IPackageFragment pkg = (IPackageFragment) ((IAdaptable) element).getAdapter(IPackageFragment.class);
                if (pkg != null) {
                    return mutationStateService.resolveForPackage(pkg);
                }

                IContainer container = (IContainer) ((IAdaptable) element).getAdapter(IContainer.class);
                if (container != null) {
                    return mutationStateService.resolveForContainer(container);
                }
            }
        } catch (Exception e) {
        }

        return StructuralArtifactMutationState.NONE;
    }

    /** * Resolve o contexto estrutural de uma compilation unit. * * @param compilationUnit unidade Java * @return contexto correspondente ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
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

    /** * Resolve o contexto estrutural de um arquivo. * * @param file arquivo do explorer * @return contexto correspondente ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
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

    /** * Resolve o contexto estrutural de uma package. * * @param pkg package do explorer * @return contexto correspondente ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
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

    /** * Resolve o contexto estrutural de uma pasta. * * @param folder pasta do explorer * @return contexto correspondente ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
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

    /** * Resolve o overlay de role estrutural. * * @param role role do contexto estrutural * @return image descriptor correspondente * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private ImageDescriptor resolveRoleOverlay(NamedContextTargetRole role) {
        if (role == NamedContextTargetRole.PRIMARY) {
            return overlayIconService.getOverlayDescriptor("icons/icon_star@2x.png");
        }

        if (role == NamedContextTargetRole.EDITABLE) {
            return overlayIconService.getOverlayDescriptor("icons/icon_pen@2x.png");
        }

        return overlayIconService.getOverlayDescriptor("icons/icon_bookmark@2x.png");
    }

    /** * Resolve o overlay de estado de mutacao. * * @param state estado de mutacao visual * @return image descriptor correspondente ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private ImageDescriptor resolveMutationOverlay(StructuralArtifactMutationState state) {
        if (state == null || state == StructuralArtifactMutationState.NONE) {
            return null;
        }

        if (state == StructuralArtifactMutationState.ADDED) {
            return overlayIconService.getOverlayDescriptor("icons/overlay_added_8.png");
        }

        if (state == StructuralArtifactMutationState.MODIFIED) {
            return overlayIconService.getOverlayDescriptor("icons/overlay_modified_8.png");
        }

        if (state == StructuralArtifactMutationState.RESTORED) {
            return overlayIconService.getOverlayDescriptor("icons/overlay_restore_8.png");
        }

        return null;
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

    /** * Normaliza caminho textual para comparacao. * * @param value caminho original * @return caminho normalizado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String normalizePath(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replace("\\", "/");
    }

    /** * Compara strings com protecao nula. * * @param a primeiro valor * @param b segundo valor * @return true quando os valores forem equivalentes * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean safeEquals(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }
}