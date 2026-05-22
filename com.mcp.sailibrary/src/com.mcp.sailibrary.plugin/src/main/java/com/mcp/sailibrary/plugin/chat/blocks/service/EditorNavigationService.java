package com.mcp.sailibrary.plugin.chat.blocks.service;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;

public class EditorNavigationService {

    public ITextEditor openTextEditorForFilePath(String filePath) {
        if (filePath == null || filePath.trim().length() == 0) {
            return null;
        }

        try {
            IWorkbench workbench = PlatformUI.getWorkbench();
            if (workbench == null || workbench.getActiveWorkbenchWindow() == null || workbench.getActiveWorkbenchWindow().getActivePage() == null) {
                return null;
            }

            IWorkbenchPage page = workbench.getActiveWorkbenchWindow().getActivePage();
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

            IPath path = Path.fromOSString(filePath);
            IFile file = root.getFileForLocation(path);

            if (file == null || !file.exists()) {
                return null;
            }

            IEditorPart openedEditor = IDE.openEditor(page, file, true);
            if (openedEditor instanceof ITextEditor) {
                return (ITextEditor) openedEditor;
            }
        } catch (Exception e) {
        }

        return null;
    }

    public ITextEditor getActiveTextEditor() {
        try {
            if (PlatformUI.getWorkbench() == null
                    || PlatformUI.getWorkbench().getActiveWorkbenchWindow() == null
                    || PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage() == null) {
                return null;
            }

            IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
            if (editor instanceof ITextEditor) {
                return (ITextEditor) editor;
            }
        } catch (Exception e) {
        }
        return null;
    }

    public void focusRange(ITextEditor editor, int offset, int length) {
        if (editor == null || offset < 0 || length <= 0) {
            return;
        }

        try {
            editor.selectAndReveal(offset, length);

            if (PlatformUI.getWorkbench() != null
                    && PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null
                    && PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage() != null) {
                PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().activate(editor);
            }
        } catch (Exception e) {
        }
    }

    public String normalizePath(File file) {
        if (file == null) {
            return "";
        }

        try {
            return file.getCanonicalPath().replace("\\", "/");
        } catch (Exception e) {
            return file.getAbsolutePath().replace("\\", "/");
        }
    }
}