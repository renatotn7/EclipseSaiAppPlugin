package com.mcp.sailibrary.plugin.chat.blocks.service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.custom.LineBackgroundEvent;
import org.eclipse.swt.custom.LineBackgroundListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mcp.sailibrary.plugin.chat.blocks.model.NamedBlockKind;
import com.mcp.sailibrary.plugin.chat.blocks.model.NamedCodeBlock;

/* yaml_header: version: "1.2" purpose: "Aplicar highlight e anotacoes semanticas para blocos textuais nomeados no editor Java." libraries: - org.eclipse.jface.text.IDocument: runtime - org.eclipse.swt.custom.StyledText: runtime - org.eclipse.jface.text.source.IAnnotationModel: runtime */
public class NamedBlockHighlighter {

    private static final NamedBlockHighlighter INSTANCE = new NamedBlockHighlighter();

    private final Map<String, Annotation> annotations = new HashMap<String, Annotation>();
    private final Map<ITextEditor, EditorPaintState> paintStates = new HashMap<ITextEditor, EditorPaintState>();

    private NamedBlockHighlighter() {
    }

    public static NamedBlockHighlighter getInstance() {
        return INSTANCE;
    }

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: NamedBlocksController, ChatAiController * Callee: installOrUpdateBackgroundPainter, clearAnnotations * Objetivo: Reaplicar anotacoes e fundo por linha para os blocos textuais do arquivo atual. */
    public int refreshHighlights(ITextEditor editor, List<NamedCodeBlock> blocks, String currentFilePath) {
        if (editor == null || isBlank(currentFilePath)) {
            return 0;
        }

        AnnotationContext context = resolveContext(editor);
        if (context == null) {
            return 0;
        }

        clearAnnotations(editor);
        installOrUpdateBackgroundPainter(editor, context.document, blocks, currentFilePath);

        int applied = 0;
        List<NamedCodeBlock> safeBlocks = blocks != null ? blocks : new ArrayList<NamedCodeBlock>();

        for (int i = 0; i < safeBlocks.size(); i++) {
            NamedCodeBlock block = safeBlocks.get(i);

            if (!isBlockEligible(block, currentFilePath)) {
                continue;
            }

            if (!isValidRange(block, context.document)) {
                continue;
            }

            try {
                String marker;
                if (block.getKind() == NamedBlockKind.PRIMARY) {
                    marker = "[P] ";
                } else if (block.getKind() == NamedBlockKind.EDITABLE) {
                    marker = "[E] ";
                } else {
                    marker = "[R] ";
                }

                String text = marker + block.getName() + " [" + block.getStartLine() + "-" + block.getEndLine() + "]";

                Annotation annotation = new Annotation(resolveAnnotationType(block.getKind()), false, text);

                Position position = block.getLivePosition() != null
                        ? block.getLivePosition()
                        : new Position(block.getEffectiveOffset(), block.getEffectiveLength());

                context.model.addAnnotation(annotation, position);
                annotations.put(block.getKey(), annotation);
                applied++;
            } catch (Exception e) {
                // Falha isolada em bloco individual nao interrompe o restante
            }
        }

        redraw(editor);
        return applied;
    }
    /** * Reaplica highlights em todos os editores atualmente rastreados pelo servico. * * <p>Esse metodo e necessario quando a sessao de blocos muda de forma global, * como na troca de PRIMARY textual por PRIMARY estrutural, evitando que um * editor antigo permaneça com pintura stale.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void refreshAllTrackedEditors(List<NamedCodeBlock> blocks) {
        List<ITextEditor> editors = new ArrayList<ITextEditor>(paintStates.keySet());

        for (int i = 0; i < editors.size(); i++) {
            ITextEditor editor = editors.get(i);
            if (editor == null) {
                continue;
            }

            EditorPaintState state = paintStates.get(editor);
            if (state == null || isBlank(state.currentFilePath)) {
                continue;
            }

            try {
                refreshHighlights(editor, blocks, state.currentFilePath);
            } catch (Exception e) {
                // Falha isolada em editor individual nao interrompe os demais.
            }
        }
    }
    public void clearHighlights(ITextEditor editor) {
        clearAnnotations(editor);
        clearBackgroundPainter(editor);
        redraw(editor);
    }

    public int getActiveAnnotationCount() {
        return annotations.size();
    }

    private void clearAnnotations(ITextEditor editor) {
        if (editor == null) {
            annotations.clear();
            return;
        }

        AnnotationContext context = resolveContext(editor);
        if (context == null) {
            annotations.clear();
            return;
        }

        try {
            List<Annotation> values = new ArrayList<Annotation>(annotations.values());
            for (int i = 0; i < values.size(); i++) {
                Annotation annotation = values.get(i);
                if (annotation != null) {
                    context.model.removeAnnotation(annotation);
                }
            }
        } catch (Exception e) {
        } finally {
            annotations.clear();
        }
    }

    private void installOrUpdateBackgroundPainter(final ITextEditor editor, final IDocument document, List<NamedCodeBlock> blocks, String currentFilePath) {
        StyledText styledText = resolveStyledText(editor);
        if (styledText == null || styledText.isDisposed()) {
            return;
        }

        EditorPaintState state = paintStates.get(editor);

        if (state != null && state.styledText != styledText) {
            detachPainter(state);
            paintStates.remove(editor);
            state = null;
        }

        if (state == null) {
            state = new EditorPaintState();
            state.document = document;
            state.styledText = styledText;
            state.primaryColor = new Color(styledText.getDisplay(), 248, 221, 214);
            state.editableColor = new Color(styledText.getDisplay(), 220, 235, 252);
            state.referenceColor = new Color(styledText.getDisplay(), 236, 226, 248);

            final EditorPaintState finalState = state;
            state.listener = new LineBackgroundListener() {
                @Override
                public void lineGetBackground(LineBackgroundEvent event) {
                    if (finalState.document == null || finalState.blocks == null || finalState.currentFilePath == null) {
                        return;
                    }

                    try {
                        int modelOffset = event.lineOffset;

                        if (finalState.textViewerExtension5 != null) {
                            int converted = finalState.textViewerExtension5.widgetOffset2ModelOffset(event.lineOffset);
                            if (converted >= 0) {
                                modelOffset = converted;
                            }
                        }

                        int lineNumber = finalState.document.getLineOfOffset(Math.max(0, modelOffset)) + 1;
                        event.lineBackground = resolveLineBackgroundByLine(finalState, lineNumber);
                    } catch (Exception e) {
                    }
                }
            };

            styledText.addLineBackgroundListener(state.listener);
            styledText.addDisposeListener(event -> {
                EditorPaintState removed = paintStates.remove(editor);
                if (removed != null) {
                    detachPainter(removed);
                }
            });

            paintStates.put(editor, state);
        }

        state.textViewer = resolveTextViewer(editor);
        if (state.textViewer instanceof org.eclipse.jface.text.ITextViewerExtension5) {
            state.textViewerExtension5 = (org.eclipse.jface.text.ITextViewerExtension5) state.textViewer;
        } else {
            state.textViewerExtension5 = null;
        }

        state.document = document;
        state.currentFilePath = currentFilePath;
        state.blocks = filterBlocksForFile(blocks, currentFilePath);
    }

    private void clearBackgroundPainter(ITextEditor editor) {
        if (editor == null) {
            return;
        }

        EditorPaintState state = paintStates.remove(editor);
        if (state == null) {
            return;
        }

        detachPainter(state);
    }

    private void detachPainter(EditorPaintState state) {
        if (state == null) {
            return;
        }

        try {
            if (state.styledText != null && !state.styledText.isDisposed() && state.listener != null) {
                state.styledText.removeLineBackgroundListener(state.listener);
            }
        } catch (Exception e) {
        }

        disposeColor(state.primaryColor);
        disposeColor(state.editableColor);
        disposeColor(state.referenceColor);
    }

    private Color resolveLineBackgroundByLine(EditorPaintState state, int lineNumber) {
        if (lineNumber <= 0) {
            return null;
        }

        Color fallback = null;

        for (int i = 0; i < state.blocks.size(); i++) {
            NamedCodeBlock block = state.blocks.get(i);
            if (block == null || !block.isRangeValid()) {
                continue;
            }

            try {
                int effectiveOffset = block.getEffectiveOffset();
                int effectiveLength = block.getEffectiveLength();

                int startLine = state.document.getLineOfOffset(effectiveOffset) + 1;
                int endLine = state.document.getLineOfOffset(effectiveOffset + Math.max(0, effectiveLength - 1)) + 1;

                block.setStartLine(startLine);
                block.setEndLine(endLine);

                if (lineNumber >= startLine && lineNumber <= endLine) {
                    if (block.getKind() == NamedBlockKind.PRIMARY) {
                        return state.primaryColor;
                    } else if (block.getKind() == NamedBlockKind.EDITABLE) {
                        return state.editableColor;
                    } else {
                        fallback = state.referenceColor;
                    }
                }
            } catch (Exception e) {
                // ignora bloco invalido neste ciclo de pintura
            }
        }

        return fallback;
    }

    private List<NamedCodeBlock> filterBlocksForFile(List<NamedCodeBlock> blocks, String currentFilePath) {
        List<NamedCodeBlock> result = new ArrayList<NamedCodeBlock>();
        if (blocks == null) {
            return result;
        }

        for (int i = 0; i < blocks.size(); i++) {
            NamedCodeBlock block = blocks.get(i);
            if (isBlockEligible(block, currentFilePath) && block.isRangeValid()) {
                result.add(block);
            }
        }

        return result;
    }

    private AnnotationContext resolveContext(ITextEditor editor) {
        try {
            if (editor == null || editor.getEditorInput() == null) {
                return null;
            }

            org.eclipse.ui.texteditor.IDocumentProvider provider = editor.getDocumentProvider();
            if (provider == null) {
                return null;
            }

            IDocument document = provider.getDocument(editor.getEditorInput());
            if (document == null) {
                return null;
            }

            IAnnotationModel model = provider.getAnnotationModel(editor.getEditorInput());
            if (model == null) {
                return null;
            }

            AnnotationContext context = new AnnotationContext();
            context.document = document;
            context.model = model;
            return context;
        } catch (Exception e) {
            return null;
        }
    }

    private StyledText resolveStyledText(ITextEditor editor) {
        if (editor == null) {
            return null;
        }

        try {
            Object adapted = editor.getAdapter(ISourceViewer.class);
            if (adapted instanceof ISourceViewer) {
                StyledText textWidget = ((ISourceViewer) adapted).getTextWidget();
                if (textWidget != null && !textWidget.isDisposed()) {
                    return textWidget;
                }
            }
        } catch (Exception e) {
        }

        try {
            Object adapted = editor.getAdapter(ITextViewer.class);
            if (adapted instanceof ITextViewer) {
                StyledText textWidget = ((ITextViewer) adapted).getTextWidget();
                if (textWidget != null && !textWidget.isDisposed()) {
                    return textWidget;
                }
            }
        } catch (Exception e) {
        }

        try {
            StyledText reflected = resolveStyledTextByReflection(editor);
            if (reflected != null && !reflected.isDisposed()) {
                return reflected;
            }
        } catch (Exception e) {
        }

        try {
            Object adapted = editor.getAdapter(Control.class);
            if (adapted instanceof StyledText) {
                return (StyledText) adapted;
            }
            if (adapted instanceof Control) {
                StyledText found = findStyledText((Control) adapted);
                if (found != null && !found.isDisposed()) {
                    return found;
                }
            }
        } catch (Exception e) {
        }

        return null;
    }

    private StyledText resolveStyledTextByReflection(ITextEditor editor) {
        Class<?> current = editor.getClass();

        while (current != null) {
            try {
                Field field = current.getDeclaredField("fSourceViewer");
                field.setAccessible(true);
                Object viewer = field.get(editor);

                if (viewer instanceof ISourceViewer) {
                    return ((ISourceViewer) viewer).getTextWidget();
                }
                if (viewer instanceof ITextViewer) {
                    return ((ITextViewer) viewer).getTextWidget();
                }
            } catch (Exception e) {
            }

            current = current.getSuperclass();
        }

        return null;
    }

    private ITextViewer resolveTextViewer(ITextEditor editor) {
        if (editor == null) {
            return null;
        }

        try {
            Object adapted = editor.getAdapter(ITextViewer.class);
            if (adapted instanceof ITextViewer) {
                return (ITextViewer) adapted;
            }
        } catch (Exception e) {
        }

        try {
            Class<?> current = editor.getClass();
            while (current != null) {
                try {
                    Field field = current.getDeclaredField("fSourceViewer");
                    field.setAccessible(true);
                    Object viewer = field.get(editor);
                    if (viewer instanceof ITextViewer) {
                        return (ITextViewer) viewer;
                    }
                } catch (Exception e) {
                }
                current = current.getSuperclass();
            }
        } catch (Exception e) {
        }

        return null;
    }

    private StyledText findStyledText(Control control) {
        if (control == null || control.isDisposed()) {
            return null;
        }

        if (control instanceof StyledText) {
            return (StyledText) control;
        }

        if (control instanceof Composite) {
            Control[] children = ((Composite) control).getChildren();
            for (int i = 0; i < children.length; i++) {
                StyledText found = findStyledText(children[i]);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private boolean isBlockEligible(NamedCodeBlock block, String currentFilePath) {
        if (block == null) {
            return false;
        }

        if (isBlank(block.getName()) || isBlank(block.getFilePath())) {
            return false;
        }

        return safeEquals(block.getFilePath(), currentFilePath);
    }

    private boolean isValidRange(NamedCodeBlock block, IDocument document) {
        if (block == null || document == null) {
            return false;
        }

        int offset = block.getEffectiveOffset();
        int length = block.getEffectiveLength();

        if (offset < 0 || length <= 0) {
            return false;
        }

        try {
            int documentLength = document.getLength();
            if (offset >= documentLength) {
                return false;
            }
            if ((offset + length) > documentLength) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String resolveAnnotationType(NamedBlockKind kind) {
        if (kind == NamedBlockKind.PRIMARY) {
            return "com.mcp.sailibrary.block.primary";
        }
        if (kind == NamedBlockKind.EDITABLE) {
            return "com.mcp.sailibrary.block.editable";
        }
        return "com.mcp.sailibrary.block.reference";
    }

    private void redraw(ITextEditor editor) {
        StyledText styledText = resolveStyledText(editor);
        if (styledText != null && !styledText.isDisposed()) {
            styledText.redraw();
        }
    }

    private void disposeColor(Color color) {
        if (color != null && !color.isDisposed()) {
            color.dispose();
        }
    }

    private boolean safeEquals(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private static class AnnotationContext {
        private IDocument document;
        private IAnnotationModel model;
    }

    private static class EditorPaintState {
        private IDocument document;
        private StyledText styledText;
        private ITextViewer textViewer;
        private org.eclipse.jface.text.ITextViewerExtension5 textViewerExtension5;
        private List<NamedCodeBlock> blocks = new ArrayList<NamedCodeBlock>();
        private String currentFilePath;
        private LineBackgroundListener listener;
        private Color primaryColor;
        private Color editableColor;
        private Color referenceColor;
    }
}