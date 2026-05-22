package com.mcp.sailibrary.plugin.chat.context.model;

import java.text.Normalizer;

import org.eclipse.jface.text.Position;

/* yaml_header: version: "1.0" purpose: "Representar um alvo contextual nomeado para a sessao do chat, incluindo bloco, arquivo ou package." libraries: - org.eclipse.jface.text.Position: runtime - java.text.Normalizer: runtime */
public class NamedContextTarget {

    private static final int MAX_NAME_LENGTH = 12;
    private static final int MAX_PREVIEW_LENGTH = 160;
    private static final int MAX_HINT_LENGTH = 120;

    private String name;
    private NamedContextTargetRole role;
    private NamedContextTargetType type;

    private String filePath;
    private String relativeFilePath;
    private String fileName;
    private String packageName;

    private int offset;
    private int length;
    private int startLine;
    private int endLine;

    private String preview;
    private long createdAt;

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: Session service, editor binding service * Callee: nenhum * Objetivo: Manter uma posicao viva no documento quando o alvo for um bloco de codigo. */
    private Position livePosition;

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: Document binding service * Callee: nenhum * Objetivo: Identificar em qual documento a livePosition foi registrada para evitar reuso incorreto. */
    private String boundDocumentKey;

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: Controllers e services de contexto * Callee: nenhum * Objetivo: Guardar um hint estrutural opcional para drift detection e reconciliacao futura. */
    private String parentContextHint;

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: Session service * Callee: nenhum * Objetivo: Preservar snapshot do texto original do bloco quando houver range textual. */
    private String originalTextSnapshot;

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: Session service * Callee: nenhum * Objetivo: Guardar assinatura leve do texto original para reconciliacao futura. */
    private String originalTextSignature;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = sanitizeName(name);
    }

    public NamedContextTargetRole getRole() {
        return role;
    }

    public void setRole(NamedContextTargetRole role) {
        this.role = role;
    }

    public NamedContextTargetType getType() {
        return type;
    }

    public void setType(NamedContextTargetType type) {
        this.type = type;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = normalizePath(filePath);
    }

    public String getRelativeFilePath() {
        return relativeFilePath;
    }

    public void setRelativeFilePath(String relativeFilePath) {
        this.relativeFilePath = normalizePath(relativeFilePath);
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = safeTrim(fileName);
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = safeTrim(packageName);
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = Math.max(0, offset);
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = Math.max(0, length);
    }

    public int getStartLine() {
        return startLine;
    }

    public void setStartLine(int startLine) {
        this.startLine = Math.max(0, startLine);
    }

    public int getEndLine() {
        return endLine;
    }

    public void setEndLine(int endLine) {
        this.endLine = Math.max(0, endLine);
    }

    public String getPreview() {
        return preview;
    }

    public void setPreview(String preview) {
        this.preview = sanitizePreview(preview);
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public Position getLivePosition() {
        return livePosition;
    }

    public void setLivePosition(Position livePosition) {
        this.livePosition = livePosition;
    }

    public String getBoundDocumentKey() {
        return boundDocumentKey;
    }

    public void setBoundDocumentKey(String boundDocumentKey) {
        this.boundDocumentKey = safeTrim(boundDocumentKey);
    }

    public String getParentContextHint() {
        return parentContextHint;
    }

    public void setParentContextHint(String parentContextHint) {
        this.parentContextHint = sanitizeHint(parentContextHint);
    }

    public String getOriginalTextSnapshot() {
        return originalTextSnapshot;
    }

    public void setOriginalTextSnapshot(String originalTextSnapshot) {
        this.originalTextSnapshot = sanitizeSnapshot(originalTextSnapshot);
    }

    public String getOriginalTextSignature() {
        return originalTextSignature;
    }

    public void setOriginalTextSignature(String originalTextSignature) {
        this.originalTextSignature = safeTrim(originalTextSignature);
    }

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: Highlighter, document binding service * Callee: nenhum * Objetivo: Informar se ha Position viva associada ao alvo. */
    public boolean hasLivePosition() {
        return livePosition != null;
    }

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: Document binding service * Callee: nenhum * Objetivo: Verificar se a Position viva pertence ao documento atual. */
    public boolean isBoundToDocument(String documentKey) {
        return !isBlank(boundDocumentKey) && safeEquals(boundDocumentKey, safeTrim(documentKey));
    }

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: Document binding service * Callee: nenhum * Objetivo: Limpar o vinculo vivo do documento sem apagar os dados fixos do alvo. */
    public void clearLiveBinding() {
        this.livePosition = null;
        this.boundDocumentKey = "";
    }

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: Highlighter, navigation service, prompt formatting * Callee: nenhum * Objetivo: Fornecer offset efetivo usando Position viva quando existir. */
    public int getEffectiveOffset() {
        if (livePosition != null) {
            return Math.max(0, livePosition.getOffset());
        }
        return Math.max(0, offset);
    }

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: Highlighter, navigation service, prompt formatting * Callee: nenhum * Objetivo: Fornecer comprimento efetivo usando Position viva quando existir. */
    public int getEffectiveLength() {
        if (livePosition != null) {
            return Math.max(0, livePosition.getLength());
        }
        return Math.max(0, length);
    }

    public int getEffectiveEndOffset() {
        return getEffectiveOffset() + getEffectiveLength();
    }

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: Document binding service * Callee: nenhum * Objetivo: Sincronizar range fixo a partir da Position viva apos alteracoes no documento. */
    public void syncFixedRangeFromLivePosition() {
        if (livePosition != null) {
            this.offset = Math.max(0, livePosition.getOffset());
            this.length = Math.max(0, livePosition.getLength());
        }
    }

    public void updateFixedRange(int offset, int length) {
        this.offset = Math.max(0, offset);
        this.length = Math.max(0, length);
    }

    public void updatePreview(String preview) {
        this.preview = sanitizePreview(preview);
    }

    public boolean isRangeValid() {
        return getEffectiveOffset() >= 0 && getEffectiveLength() > 0;
    }

    public boolean belongsToFile(String path) {
        String normalized = normalizePath(path);
        return safeEquals(this.filePath, normalized);
    }

    public boolean overlaps(String path, int otherOffset, int otherLength) {
        if (!belongsToFile(path)) {
            return false;
        }
        if (otherOffset < 0 || otherLength <= 0 || !isRangeValid()) {
            return false;
        }

        int thisStart = getEffectiveOffset();
        int thisEnd = getEffectiveEndOffset();
        int otherStart = otherOffset;
        int otherEnd = otherOffset + otherLength;

        return thisStart < otherEnd && thisEnd > otherStart;
    }

    public boolean contains(String path, int otherOffset, int otherLength) {
        if (!belongsToFile(path)) {
            return false;
        }
        if (otherOffset < 0 || otherLength <= 0 || !isRangeValid()) {
            return false;
        }

        int thisStart = getEffectiveOffset();
        int thisEnd = getEffectiveEndOffset();
        int otherStart = otherOffset;
        int otherEnd = otherOffset + otherLength;

        return otherStart >= thisStart && otherEnd <= thisEnd;
    }

    public boolean isUsable() {
        if (isBlank(name) || role == null || type == null) {
            return false;
        }

        if (type == NamedContextTargetType.CODE_BLOCK) {
            return !isBlank(filePath)
                    && !isBlank(fileName)
                    && startLine > 0
                    && endLine >= startLine
                    && isRangeValid();
        }

        if (type == NamedContextTargetType.FILE) {
            return !isBlank(filePath) && !isBlank(fileName);
        }

        if (type == NamedContextTargetType.PACKAGE) {
            return !isBlank(packageName) || !isBlank(relativeFilePath) || !isBlank(filePath);
        }

        return false;
    }

    public String getKey() {
        return safeTrim(filePath)
                + "|" + safeTrim(packageName)
                + "|" + offset
                + "|" + length
                + "|" + createdAt
                + "|" + safeTrim(name);
    }

    public String getDisplayLabel() {
        StringBuilder sb = new StringBuilder();
        sb.append(safeTrim(name));

        if (type == NamedContextTargetType.CODE_BLOCK && safeTrim(fileName).length() > 0) {
            sb.append(" [").append(fileName);

            if (startLine > 0 && endLine > 0) {
                sb.append(":").append(startLine);
                if (endLine != startLine) {
                    sb.append("-").append(endLine);
                }
            }

            sb.append("]");
        } else if (type == NamedContextTargetType.FILE && safeTrim(fileName).length() > 0) {
            sb.append(" [").append(fileName).append("]");
        } else if (type == NamedContextTargetType.PACKAGE && safeTrim(packageName).length() > 0) {
            sb.append(" [").append(packageName).append("]");
        }

        return sb.toString();
    }

    public String getRoleMarker() {
        if (role == NamedContextTargetRole.PRIMARY) {
            return "[P]";
        }
        if (role == NamedContextTargetRole.EDITABLE) {
            return "[E]";
        }
        return "[R]";
    }

    @Override
    public String toString() {
        return "NamedContextTarget{name='" + safeTrim(name)
                + "', role=" + role
                + ", type=" + type
                + ", filePath='" + safeTrim(filePath)
                + "', relativeFilePath='" + safeTrim(relativeFilePath)
                + "', fileName='" + safeTrim(fileName)
                + "', packageName='" + safeTrim(packageName)
                + "', effectiveOffset=" + getEffectiveOffset()
                + ", effectiveLength=" + getEffectiveLength()
                + ", startLine=" + startLine
                + ", endLine=" + endLine
                + ", hasLivePosition=" + hasLivePosition()
                + ", boundDocumentKey='" + safeTrim(boundDocumentKey)
                + "'}";
    }

    private String sanitizeName(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim().toLowerCase();
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        normalized = normalized.replaceAll("[^a-z0-9_]", "");

        if (normalized.length() > MAX_NAME_LENGTH) {
            normalized = normalized.substring(0, MAX_NAME_LENGTH);
        }

        return normalized;
    }

    private String sanitizePreview(String value) {
        if (value == null) {
            return "";
        }

        String sanitized = value.trim();
        sanitized = sanitized.replace("\r", " ");
        sanitized = sanitized.replace("\n", " ");
        sanitized = sanitized.replace("\t", " ");
        sanitized = sanitized.replaceAll("\\s+", " ");
        sanitized = sanitized.replace("\"", "'");
        sanitized = sanitized.trim();

        if (sanitized.length() > MAX_PREVIEW_LENGTH) {
            sanitized = sanitized.substring(0, MAX_PREVIEW_LENGTH) + "...";
        }

        return sanitized;
    }

    private String sanitizeHint(String value) {
        if (value == null) {
            return "";
        }

        String sanitized = value.trim().replaceAll("\\s+", " ");
        if (sanitized.length() > MAX_HINT_LENGTH) {
            sanitized = sanitized.substring(0, MAX_HINT_LENGTH) + "...";
        }
        return sanitized;
    }

    private String sanitizeSnapshot(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }

    private String normalizePath(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replace("\\", "/");
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private boolean safeEquals(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }
}