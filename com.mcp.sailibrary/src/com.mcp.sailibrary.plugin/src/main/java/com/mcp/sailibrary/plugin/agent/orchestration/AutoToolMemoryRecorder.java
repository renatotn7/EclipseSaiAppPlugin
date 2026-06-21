package com.mcp.sailibrary.plugin.agent.orchestration;

import java.io.File;

import com.mcp.sailibrary.plugin.agent.context.analise.ProjectMemoryStore;

/**
 * Persiste automaticamente achados estruturais estaveis de ferramentas de analise.
 * O armazenamento usa .sai/projects/<projeto>, por meio de ProjectMemoryStore.
 */
public final class AutoToolMemoryRecorder {

    private AutoToolMemoryRecorder() {
    }

    public static void record(String projectRoot, String toolName, String jsonParameters, String resultText) {
        if (isBlank(projectRoot) || isBlank(toolName) || isBlank(resultText)) {
            return;
        }

        try {
            if (kindFor(toolName) == null) {
                return;
            }

            String relativePath = parameter(jsonParameters, "path");
            ProjectMemoryStore store = new ProjectMemoryStore(new File(projectRoot));
            store.inicializarEstrutura();

            store.registrarAgentToolAnalise(toolName, jsonParameters, resultText);

            if ("inspecionar_dependencias_projeto".equals(toolName)) {
                recordBuildTool(store, relativePath, resultText);
                recordIfPresent(store, relativePath, "module", "moduloPreferencial", valueAfter(resultText, "moduloPreferencial:"), toolName, resultText);
                return;
            }

            if ("detectar_estilos_persistencia".equals(toolName)) {
                recordPersistence(store, relativePath, resultText, "Hibernate HBM XML");
                recordPersistence(store, relativePath, resultText, "Hibernate cfg XML");
                recordPersistence(store, relativePath, resultText, "Hibernate Session/Criteria/Query API");
                recordPersistence(store, relativePath, resultText, "JPA annotations/API");
                recordPersistence(store, relativePath, resultText, "JDBC/JdbcTemplate");
                return;
            }

            if ("descobrir_padroes_nomenclatura_projeto".equals(toolName)) {
                recordIfContains(store, relativePath, "naming", "suffix.dao", resultText, "DAO:", toolName);
                recordIfContains(store, relativePath, "naming", "suffix.form", resultText, "Form:", toolName);
                recordIfContains(store, relativePath, "naming", "suffix.action", resultText, "Action:", toolName);
                recordIfContains(store, relativePath, "naming", "suffix.bean", resultText, "Bean:", toolName);
                recordIfContains(store, relativePath, "naming", "suffix.to", resultText, "TO:", toolName);
                return;
            }

            if ("inventariar_estrutura_projeto".equals(toolName) || "gerar_mapa_operacional_ia".equals(toolName)) {
                recordIfContains(store, relativePath, "structure", "maven.modules", resultText, "pom.xml", toolName);
                recordIfContains(store, relativePath, "structure", "hibernate.resources", resultText, "hibernate_", toolName);
                recordIfContains(store, relativePath, "structure", "struts.config", resultText, "struts-config", toolName);
                return;
            }

            if ("rastrear_problemas_preexistentes".equals(toolName)) {
                recordProblemSummary(store, relativePath, resultText);
            }
        } catch (Exception ignored) {
            // Memoria automatica nao pode quebrar a execucao da ferramenta.
        }
    }

    private static void recordBuildTool(ProjectMemoryStore store, String path, String resultText) {
        String buildTool = valueAfter(resultText, "buildTool:");
        if (!isBlank(buildTool) && !"desconhecido".equalsIgnoreCase(buildTool.trim())) {
            store.recordStablePattern(path, "build", "buildTool", buildTool.trim(), "inspecionar_dependencias_projeto", 0.85);
        }
        String groupId = valueAfter(resultText, "groupIdModulo:");
        if (!isBlank(groupId)) {
            store.recordStablePattern(path, "build", "groupIdModulo", groupId.trim(), "inspecionar_dependencias_projeto", 0.80);
        }
    }

    private static void recordPersistence(ProjectMemoryStore store, String path, String resultText, String label) {
        String value = countLineValue(resultText, label);
        if (!isBlank(value) && !"0".equals(value.trim())) {
            store.recordStablePattern(path, "persistence", label, value.trim(), "detectar_estilos_persistencia", 0.85);
        }
    }

    private static void recordProblemSummary(ProjectMemoryStore store, String path, String resultText) {
        String total = valueAfter(resultText, "totalIssues:");
        String severe = valueAfter(resultText, "severeIssues:");
        if (!isBlank(total) || !isBlank(severe)) {
            String value = "totalIssues=" + safe(total) + "; severeIssues=" + safe(severe);
            store.recordStablePattern(path, "risk", "preexisting.issues.summary", value, "rastrear_problemas_preexistentes", 0.70);
        }
    }

    private static void recordIfPresent(ProjectMemoryStore store, String path, String kind, String key, String value, String toolName, String evidence) {
        if (!isBlank(value)) {
            store.recordStablePattern(path, kind, key, value.trim(), toolName + " | " + safe(evidence), 0.75);
        }
    }

    private static void recordIfContains(ProjectMemoryStore store, String path, String kind, String key, String resultText, String token, String toolName) {
        if (resultText != null && resultText.indexOf(token) >= 0) {
            store.recordStablePattern(path, kind, key, "detectado", toolName + " evidenciou " + token, 0.75);
        }
    }

    private static String countLineValue(String text, String label) {
        String[] lines = text != null ? text.split("\\r?\\n") : new String[0];
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i] == null ? "" : lines[i].trim();
            if (line.startsWith("- " + label + ":")) {
                return line.substring(line.indexOf(':') + 1).trim();
            }
        }
        return "";
    }

    private static String valueAfter(String text, String prefix) {
        if (text == null || prefix == null) {
            return "";
        }
        String[] lines = text.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i] == null ? "" : lines[i].trim();
            if (line.startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }
        }
        return "";
    }

    private static String parameter(String jsonParameters, String key) {
        if (jsonParameters == null || key == null) {
            return "";
        }
        String marker = "\"" + key + "\"";
        int keyIndex = jsonParameters.indexOf(marker);
        if (keyIndex < 0) {
            return "";
        }
        int colon = jsonParameters.indexOf(':', keyIndex + marker.length());
        if (colon < 0) {
            return "";
        }
        int start = colon + 1;
        while (start < jsonParameters.length() && Character.isWhitespace(jsonParameters.charAt(start))) {
            start++;
        }
        if (start >= jsonParameters.length()) {
            return "";
        }
        if (jsonParameters.charAt(start) == '"') {
            int end = jsonParameters.indexOf('"', start + 1);
            return end > start ? jsonParameters.substring(start + 1, end) : "";
        }
        int end = start;
        while (end < jsonParameters.length() && ",}".indexOf(jsonParameters.charAt(end)) < 0) {
            end++;
        }
        return jsonParameters.substring(start, end).trim();
    }

    private static String kindFor(String toolName) {
        if ("inventariar_estrutura_projeto".equals(toolName)
                || "gerar_mapa_operacional_ia".equals(toolName)
                || "descobrir_padroes_nomenclatura_projeto".equals(toolName)
                || "detectar_estilos_persistencia".equals(toolName)
                || "rastrear_problemas_preexistentes".equals(toolName)
                || "inspecionar_dependencias_projeto".equals(toolName)) {
            return toolName;
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
