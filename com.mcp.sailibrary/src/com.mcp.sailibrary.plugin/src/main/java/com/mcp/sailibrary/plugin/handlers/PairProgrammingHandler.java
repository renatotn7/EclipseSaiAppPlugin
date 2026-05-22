package com.mcp.sailibrary.plugin.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mcp.sailibrary.plugin.chat.controllers.ChatAiController;
import com.mcp.sailibrary.plugin.chat.views.ChatView;

/*
 * Objetivo: Interceptar o comando Ctrl+I e orquestrar o envio de contexto para a View de IA. Dependencias:
 * org.eclipse.ui org.eclipse.jface.text org.eclipse.jdt.core Versoes: Compativel com Eclipse Indigo/Juno e superiores
 * (Java 7+).
 */
public class PairProgrammingHandler extends AbstractHandler {

	/**
 * Descrição não fornecida.
 *
 * @author Renato Tomaz Nati
 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
	    IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
	    
	    try {
	        // Garante que a interface visual esta aberta e em foco
	        org.eclipse.ui.IViewPart viewPart = window.getActivePage().showView(ChatView.ID);
	        
	        if (viewPart instanceof ChatView) {
	            ChatView chatView = (ChatView) viewPart;
	            
	            // Feature: O Handler civil acessa o Controlador militar e ordena o escaneamento automatico
	            chatView.getController().engajarAlvoAtual(true);
	        }
	    } catch (PartInitException e) {
	        MessageDialog.openError(window.getShell(), "Alerta de Operacao", "Falha ao inicializar janela base: " + e.getMessage());
	    }
	    
	    return null;
	}
	
}