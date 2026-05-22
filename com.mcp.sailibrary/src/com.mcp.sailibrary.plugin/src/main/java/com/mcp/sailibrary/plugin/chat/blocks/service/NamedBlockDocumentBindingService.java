package com.mcp.sailibrary.plugin.chat.blocks.service;

import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.Position;

import com.mcp.sailibrary.plugin.chat.blocks.model.NamedCodeBlock;

public class NamedBlockDocumentBindingService {

    private static final String POSITION_CATEGORY = "com.mcp.sailibrary.plugin.chat.blocks.positions";

    public void bindBlocksToDocument(IDocument document, List<NamedCodeBlock> blocks, String currentFilePath) {
        if (document == null || blocks == null || isBlank(currentFilePath)) {
            return;
        }

        ensurePositionInfrastructure(document);
        String documentKey = buildDocumentKey(document, currentFilePath);

        for (int i = 0; i < blocks.size(); i++) {
            NamedCodeBlock block = blocks.get(i);
            if (block == null) {
                continue;
            }

            if (!block.belongsToFile(currentFilePath)) {
                continue;
            }

            if (block.hasLivePosition() && block.isBoundToDocument(documentKey)) {
                continue;
            }

            if (block.hasLivePosition() && !block.isBoundToDocument(documentKey)) {
                block.clearLiveBinding();
            }

            int offset = block.getOffset();
            int length = block.getLength();

            if (!isValidRange(document, offset, length)) {
                continue;
            }

            try {
                Position position = new Position(offset, length);
                document.addPosition(POSITION_CATEGORY, position);
                block.setLivePosition(position);
                block.setBoundDocumentKey(documentKey);
            } catch (Exception e) {
                block.clearLiveBinding();
            }
        }
    }

    public void unbindBlocksFromDocument(IDocument document, List<NamedCodeBlock> blocks, String currentFilePath) {
        if (document == null || blocks == null || isBlank(currentFilePath)) {
            return;
        }

        if (!document.containsPositionCategory(POSITION_CATEGORY)) {
            return;
        }

        String documentKey = buildDocumentKey(document, currentFilePath);

        for (int i = 0; i < blocks.size(); i++) {
            NamedCodeBlock block = blocks.get(i);
            if (block == null || !block.belongsToFile(currentFilePath)) {
                continue;
            }

            if (!block.hasLivePosition() || !block.isBoundToDocument(documentKey)) {
                continue;
            }

            Position live = block.getLivePosition();

            try {
                document.removePosition(POSITION_CATEGORY, live);
            } catch (Exception e) {
                // Falha silenciosa segura
            } finally {
                block.syncFixedRangeFromLivePosition();
                block.clearLiveBinding();
            }
        }
    }

    public void syncBlocksFromDocument(IDocument document, List<NamedCodeBlock> blocks, String currentFilePath) {
        if (document == null || blocks == null || isBlank(currentFilePath)) {
            return;
        }

        String documentKey = buildDocumentKey(document, currentFilePath);

        for (int i = 0; i < blocks.size(); i++) {
            NamedCodeBlock block = blocks.get(i);
            if (block == null || !block.belongsToFile(currentFilePath)) {
                continue;
            }

            if (!block.hasLivePosition() || !block.isBoundToDocument(documentKey)) {
                continue;
            }

            block.syncFixedRangeFromLivePosition();
            syncLinesAndPreview(document, block);
        }
    }

    public void rebindSingleBlock(IDocument document, NamedCodeBlock block, String currentFilePath) {
        if (document == null || block == null || isBlank(currentFilePath)) {
            return;
        }

        ensurePositionInfrastructure(document);
        String documentKey = buildDocumentKey(document, currentFilePath);

        try {
            if (block.hasLivePosition() && block.isBoundToDocument(documentKey)) {
                try {
                    document.removePosition(POSITION_CATEGORY, block.getLivePosition());
                } catch (Exception e) {
                }
                block.clearLiveBinding();
            } else if (block.hasLivePosition() && !block.isBoundToDocument(documentKey)) {
                block.clearLiveBinding();
            }

            if (!isValidRange(document, block.getOffset(), block.getLength())) {
                return;
            }

            Position position = new Position(block.getOffset(), block.getLength());
            document.addPosition(POSITION_CATEGORY, position);
            block.setLivePosition(position);
            block.setBoundDocumentKey(documentKey);

            syncLinesAndPreview(document, block);
        } catch (Exception e) {
            block.clearLiveBinding();
        }
    }

    public void syncLinesAndPreview(IDocument document, NamedCodeBlock block) {
        if (document == null || block == null || !block.isRangeValid()) {
            return;
        }

        try {
            int offset = block.getEffectiveOffset();
            int length = block.getEffectiveLength();

            if (!isValidRange(document, offset, length)) {
                return;
            }

            int startLine = document.getLineOfOffset(offset) + 1;
            int endLine = document.getLineOfOffset(offset + Math.max(0, length - 1)) + 1;

            block.setStartLine(startLine);
            block.setEndLine(endLine);

            String text = document.get(offset, length);
            block.updatePreview(buildPreview(text));
        } catch (BadLocationException e) {
        }
    }

    private void ensurePositionInfrastructure(IDocument document) {
        try {
            if (!document.containsPositionCategory(POSITION_CATEGORY)) {
                document.addPositionCategory(POSITION_CATEGORY);
            }
        } catch (Exception e) {
        }

        try {
            IPositionUpdater[] updaters = document.getPositionUpdaters();
            boolean exists = false;

            for (int i = 0; i < updaters.length; i++) {
                if (updaters[i] instanceof DefaultPositionUpdater) {
                    DefaultPositionUpdater updater = (DefaultPositionUpdater) updaters[i];
                    if (POSITION_CATEGORY.equals(readCategory(updater))) {
                        exists = true;
                        break;
                    }
                }
            }

            if (!exists) {
                document.addPositionUpdater(new DefaultPositionUpdater(POSITION_CATEGORY));
            }
        } catch (Exception e) {
        }
    }

    private String readCategory(DefaultPositionUpdater updater) {
        try {
            java.lang.reflect.Field field = DefaultPositionUpdater.class.getDeclaredField("fCategory");
            field.setAccessible(true);
            Object value = field.get(updater);
            return value != null ? value.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String buildDocumentKey(IDocument document, String currentFilePath) {
        if (document == null) {
            return "";
        }
        return normalizePath(currentFilePath) + "@" + System.identityHashCode(document);
    }

    private boolean isValidRange(IDocument document, int offset, int length) {
        if (document == null || offset < 0 || length <= 0) {
            return false;
        }

        try {
            int docLength = document.getLength();
            if (offset >= docLength) {
                return false;
            }
            if (offset + length > docLength) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String buildPreview(String text) {
        if (text == null) {
            return "";
        }

        String value = text.trim();
        value = value.replace("\r", " ");
        value = value.replace("\n", " ");
        value = value.replace("\t", " ");
        value = value.replaceAll("\\s+", " ");
        value = value.replace("\"", "'");
        value = value.trim();

        if (value.length() > 120) {
            value = value.substring(0, 120) + "...";
        }

        return value;
    }

    private String normalizePath(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replace("\\", "/");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}