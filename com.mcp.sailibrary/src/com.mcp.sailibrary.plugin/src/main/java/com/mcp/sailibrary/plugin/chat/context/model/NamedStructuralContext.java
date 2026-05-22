package com.mcp.sailibrary.plugin.chat.context.model;

import java.text.Normalizer;

/* yaml_header: version: "1.0" purpose: "Representar um alvo estrutural nomeado da sessao, como arquivo, package ou pasta." libraries: - java.text.Normalizer: runtime */
public class NamedStructuralContext {

    private static final int MAX_NAME_LENGTH = 12;
    private static final int MAX_PREVIEW_LENGTH = 160;

    private String name;
    private NamedContextTargetRole role;
    private NamedStructuralContextType type;

    private String filePath;
    private String relativePath;
    private String fileName;
    private String packageName;
    private String preview;

    private long createdAt;

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: services e controllers de contexto * Callee: nenhum * Objetivo: Retornar o nome curto do alvo estrutural. */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = sanitizeName(name);
    }

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: services e controllers de contexto * Callee: nenhum * Objetivo: Informar o papel do alvo estrutural na sessao. */
    public NamedContextTargetRole getRole() {
        return role;
    }

    public void setRole(NamedContextTargetRole role) {
        this.role = role;
    }

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: services e controllers de contexto * Callee: nenhum * Objetivo: Informar o tipo estrutural do alvo. */
    public NamedStructuralContextType getType() {
        return type;
    }

    public void setType(NamedStructuralContextType type) {
        this.type = type;
    }

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: services de navigator e explorer * Callee: nenhum * Objetivo: Guardar caminho absoluto normalizado quando houver. */
    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = normalizePath(filePath);
    }

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: services de workspace e prompt * Callee: nenhum * Objetivo: Guardar caminho relativo amigavel para prompt e UI. */
    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = normalizePath(relativePath);
    }

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: UI e prompt * Callee: nenhum * Objetivo: Guardar nome do arquivo ou pasta para exibicao amigavel. */
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = safeTrim(fileName);
    }

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: UI e prompt * Callee: nenhum * Objetivo: Guardar packageName quando o alvo estrutural representar package. */
    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = safeTrim(packageName);
    }

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: UI e prompt * Callee: nenhum * Objetivo: Guardar preview curto e seguro do conteudo ou do significado do alvo. */
    public String getPreview() {
        return preview;
    }

    public void setPreview(String preview) {
        this.preview = sanitizePreview(preview);
    }

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: session service * Callee: nenhum * Objetivo: Preservar momento de criacao do alvo estrutural. */
    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: panel de contexto e prompt formatter * Callee: nenhum * Objetivo: Retornar true se o alvo estrutural possui dados minimos para uso seguro. */
    public boolean isUsable() {
        if (isBlank(name) || role == null || type == null) {
            return false;
        }

        if (type == NamedStructuralContextType.FILE) {
            return !isBlank(fileName) || !isBlank(filePath) || !isBlank(relativePath);
        }

        if (type == NamedStructuralContextType.PACKAGE) {
            return !isBlank(packageName) || !isBlank(relativePath) || !isBlank(filePath);
        }

        if (type == NamedStructuralContextType.FOLDER) {
            return !isBlank(fileName) || !isBlank(relativePath) || !isBlank(filePath);
        }

        return false;
    }

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: panel de contexto * Callee: nenhum * Objetivo: Fornecer label amigavel e compacta para exibicao. */
    public String getDisplayLabel() {
        StringBuilder sb = new StringBuilder();
        sb.append(safeTrim(name));

        if (type == NamedStructuralContextType.FILE && !isBlank(fileName)) {
            sb.append(" [").append(fileName).append("]");
        } else if (type == NamedStructuralContextType.PACKAGE && !isBlank(packageName)) {
            sb.append(" [").append(packageName).append("]");
        } else if (type == NamedStructuralContextType.FOLDER) {
            if (!isBlank(fileName)) {
                sb.append(" [").append(fileName).append("]");
            } else if (!isBlank(relativePath)) {
                sb.append(" [").append(relativePath).append("]");
            }
        }

        return sb.toString();
    }

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: panel de contexto e future prompt formatter * Callee: nenhum * Objetivo: Retornar marcador curto do papel do alvo na sessao. */
    public String getRoleMarker() {
        if (role == NamedContextTargetRole.PRIMARY) {
            return "[P]";
        }
        if (role == NamedContextTargetRole.EDITABLE) {
            return "[E]";
        }
        return "[R]";
    }

    /* * Feature * Data: 2026-05-20 00:00:00 * Caller: session service * Callee: nenhum * Objetivo: Fornecer chave estavel do alvo estrutural. */
    public String getKey() {
        return safeTrim(filePath)
                + "|"
                + safeTrim(relativePath)
                + "|"
                + safeTrim(packageName)
                + "|"
                + safeTrim(fileName)
                + "|"
                + createdAt
                + "|"
                + safeTrim(name);
    }

    @Override
    public String toString() {
        return "NamedStructuralContext{name='" + safeTrim(name)
                + "', role=" + role
                + ", type=" + type
                + ", filePath='" + safeTrim(filePath)
                + "', relativePath='" + safeTrim(relativePath)
                + "', fileName='" + safeTrim(fileName)
                + "', packageName='" + safeTrim(packageName)
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
}