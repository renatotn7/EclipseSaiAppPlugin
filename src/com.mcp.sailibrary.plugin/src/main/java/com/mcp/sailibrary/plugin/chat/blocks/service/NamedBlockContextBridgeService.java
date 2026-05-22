package com.mcp.sailibrary.plugin.chat.blocks.service;

import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mcp.sailibrary.plugin.chat.blocks.model.NamedCodeBlock;

public class NamedBlockContextBridgeService {

    private final NamedBlockSessionService sessionService;
    private final NamedBlockPromptFormatter promptFormatter;
    private final NamedBlockDocumentBindingService documentBindingService;

    public NamedBlockContextBridgeService() {
        this.sessionService = NamedBlockSessionService.getInstance();
        this.promptFormatter = new NamedBlockPromptFormatter();
        this.documentBindingService = new NamedBlockDocumentBindingService();
    }

    public void bindCurrentFileBlocks(ITextEditor editor, IDocument document, String currentFilePath) {
        if (editor == null || document == null || isBlank(currentFilePath)) {
            return;
        }

        documentBindingService.bindBlocksToDocument(document, sessionService.getAll(), currentFilePath);
        documentBindingService.syncBlocksFromDocument(document, sessionService.getAll(), currentFilePath);
    }

    public void syncCurrentFileBlocks(IDocument document, String currentFilePath) {
        if (document == null || isBlank(currentFilePath)) {
            return;
        }

        documentBindingService.syncBlocksFromDocument(document, sessionService.getAll(), currentFilePath);
    }

    public String formatContextForAi() {
        return promptFormatter.format(sessionService.getAll());
    }

    public List<NamedCodeBlock> getAllBlocks() {
        return sessionService.getAll();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}