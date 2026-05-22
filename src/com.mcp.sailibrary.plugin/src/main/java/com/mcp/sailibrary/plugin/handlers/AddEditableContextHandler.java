package com.mcp.sailibrary.plugin.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mcp.sailibrary.plugin.chat.views.ChatView;

/** * Adiciona a selecao atual do editor ou explorer como contexto editavel. * * <p>Quando a acao for disparada no editor com selecao textual, o handler deve * acionar o fluxo textual. Quando a acao vier do explorer, o handler deve * acionar o fluxo estrutural.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class AddEditableContextHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        try {
            ISelection selectionAtual = resolverSelecaoAtual(event);

            IWorkbenchPage page = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();
            IWorkbenchPart activePartBefore = page.getActivePart();

            ChatView chatView = (ChatView) page.showView(ChatView.ID, null, IWorkbenchPage.VIEW_VISIBLE);

            if (chatView != null) {
                chatView.abrirAbaContexto();

                if (chatView.getNamedBlocksPanel() != null
                        && chatView.getNamedBlocksPanel().getController() != null) {

                    if (selectionAtual instanceof ITextSelection) {
                        chatView.getNamedBlocksPanel().getController().adicionarSelecaoComoEditavel();
                    } else {
                        chatView.getNamedBlocksPanel().getController().adicionarComoEditavel(selectionAtual);
                    }

                    chatView.getNamedBlocksPanel().refreshPanel();
                }
            }

            if (activePartBefore != null) {
                page.activate(activePartBefore);
            }
        } catch (PartInitException e) {
            throw new ExecutionException("Falha ao abrir ChatView para adicionar contexto editavel.", e);
        } catch (Exception e) {
            throw new ExecutionException("Falha ao resolver a selecao atual para contexto editavel.", e);
        }

        return null;
    }

    /** * Resolve a selecao atual com base no part que disparou o comando. * * <p>Quando a acao vier de um editor de texto, a selecao textual do editor * deve ter prioridade. Quando a acao vier do explorer, a selecao estrutural * do evento deve prevalecer, mesmo que exista um editor aberto.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private ISelection resolverSelecaoAtual(ExecutionEvent event) {
        org.eclipse.ui.IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
        if (activePart instanceof ITextEditor) {
            ITextEditor textEditor = (ITextEditor) activePart;
            if (textEditor.getSelectionProvider() != null) {
                ISelection editorSelection = textEditor.getSelectionProvider().getSelection();
                if (editorSelection instanceof ITextSelection) {
                    return editorSelection;
                }
            }
        }

        ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
        if (currentSelection != null) {
            return currentSelection;
        }

        if (HandlerUtil.getActiveWorkbenchWindow(event) != null
                && HandlerUtil.getActiveWorkbenchWindow(event).getSelectionService() != null) {
            return HandlerUtil.getActiveWorkbenchWindow(event).getSelectionService().getSelection();
        }

        return null;
    }
}