package com.mcp.sailibrary.plugin.chat.blocks.model;

import java.text.Normalizer;

import org.eclipse.jface.text.Position;

public class NamedCodeBlock {

    private static final int MAX_NAME_LENGTH = 12;
    private static final int MAX_PREVIEW_LENGTH = 160;
    private static final int MAX_HINT_LENGTH = 120;

    private String name;
    private NamedBlockKind kind;
    private String filePath;
    private String relativeFilePath;
    private String fileName;

    private int offset;
    private int length;
    private int startLine;
    private int endLine;

    private String preview;
    private long createdAt;

    /** * Posicao viva no documento. Quando registrada no IDocument com updater, * acompanha insercoes e remocoes de texto. */
    private Position livePosition;

    /** * Identifica em qual documento a livePosition foi bindada. * Isso evita reaproveitar uma Position antiga em outro IDocument. */
    private String boundDocumentKey;

    /** * Identificador opcional do metodo pai ou ancora estrutural. * Pode ser usado no futuro para drift detection e reancoragem. */
    private String parentContextHint;

    /** * Snapshot opcional do texto original do bloco no momento da criacao. * Ajuda em reconciliacao futura quando o documento sofrer alteracoes amplas. */
    private String originalTextSnapshot;

    /** * Hash simples opcional do snapshot original. * Pode ajudar em validacoes futuras sem armazenar muito texto. */
    private String originalTextSignature;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = sanitizeName(name);
    }

    public NamedBlockKind getKind() {
        return kind;
    }

    public void setKind(NamedBlockKind kind) {
        this.kind = kind;
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

    /** * Retorna true se o bloco possui Position viva associada. */
    public boolean hasLivePosition() {
        return livePosition != null;
    }

    /** * Retorna true se a Position viva atual pertence ao documento informado. */
    public boolean isBoundToDocument(String documentKey) {
        return !isBlank(boundDocumentKey) && safeEquals(boundDocumentKey, safeTrim(documentKey));
    }

    /** * Limpa o vinculo vivo do documento. */
    public void clearLiveBinding() {
        this.livePosition = null;
        this.boundDocumentKey = "";
    }

    /** * Offset efetivo: usa Position viva quando existir, senao usa o offset fixo. */
    public int getEffectiveOffset() {
        if (livePosition != null) {
            return Math.max(0, livePosition.getOffset());
        }
        return Math.max(0, offset);
    }

    /** * Comprimento efetivo: usa Position viva quando existir, senao usa o length fixo. */
    public int getEffectiveLength() {
        if (livePosition != null) {
            return Math.max(0, livePosition.getLength());
        }
        return Math.max(0, length);
    }

    /** * Offset final efetivo. */
    public int getEffectiveEndOffset() {
        return getEffectiveOffset() + getEffectiveLength();
    }

    /** * Sincroniza os campos fixos offset/length com a Position viva atual. */
    public void syncFixedRangeFromLivePosition() {
        if (livePosition != null) {
            this.offset = Math.max(0, livePosition.getOffset());
            this.length = Math.max(0, livePosition.getLength());
        }
    }

    /** * Atualiza o range fixo explicitamente. */
    public void updateFixedRange(int offset, int length) {
        this.offset = Math.max(0, offset);
        this.length = Math.max(0, length);
    }

    /** * Atualiza o preview com sanitizacao. */
    public void updatePreview(String preview) {
        this.preview = sanitizePreview(preview);
    }

    /** * Retorna true se o range efetivo parece valido em termos basicos. */
    public boolean isRangeValid() {
        return getEffectiveOffset() >= 0 && getEffectiveLength() > 0;
    }

    /** * Verifica se o bloco pertence ao arquivo informado. */
    public boolean belongsToFile(String path) {
        String normalized = normalizePath(path);
        return safeEquals(this.filePath, normalized);
    }

    /** * Testa sobreposicao entre este bloco e um range arbitrario no mesmo arquivo. */
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

    /** * Verifica se este bloco contem totalmente outro range. */
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

    /** * Verifica se este bloco e semanticamente minimamente utilizavel. */
    public boolean isUsable() {
        return !isBlank(name)
                && kind != null
                && !isBlank(filePath)
                && !isBlank(fileName)
                && startLine > 0
                && endLine >= startLine
                && isRangeValid();
    }

    /** * Chave estavel baseada em identidade de criacao e localizacao original. */
    public String getKey() {
        return safeTrim(filePath) + "|" + offset + "|" + length + "|" + createdAt;
    }

    /** * Label amigavel para UI. */
    public String getDisplayLabel() {
        StringBuilder sb = new StringBuilder();
        sb.append(safeTrim(name));

        if (safeTrim(fileName).length() > 0) {
            sb.append(" [").append(fileName);

            if (startLine > 0 && endLine > 0) {
                sb.append(":").append(startLine);
                if (endLine != startLine) {
                    sb.append("-").append(endLine);
                }
            }

            sb.append("]");
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return "NamedCodeBlock{name='" + safeTrim(name)
                + "', kind=" + kind
                + ", filePath='" + safeTrim(filePath)
                + "', relativeFilePath='" + safeTrim(relativeFilePath)
                + "', fileName='" + safeTrim(fileName)
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