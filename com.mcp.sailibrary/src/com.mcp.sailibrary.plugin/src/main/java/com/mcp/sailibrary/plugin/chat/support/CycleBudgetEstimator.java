package com.mcp.sailibrary.plugin.chat.support;

/**
 * Estima dinamicamente o limite de ciclos da missao conforme complexidade do pedido.
 * Mantem um piso seguro e evita que checklists longos fiquem presos em limite fixo.
 */
public final class CycleBudgetEstimator {

    private static final int MIN_CYCLES = 8;
    private static final int DEFAULT_CYCLES = 13;
    private static final int MAX_CYCLES = 40;

    private CycleBudgetEstimator() {
    }

    public static int estimate(String instruction, String selectedCode, String fullFileText) {
        String text = normalize(instruction);
        int score = 0;

        int numberedItems = countNumberedItems(text);
        if (numberedItems > 0) {
            score = Math.max(score, numberedItems + 4);
        }

        int explicitTools = countOccurrences(text, "_projeto")
                + countOccurrences(text, "_jdt")
                + countOccurrences(text, "consultar_")
                + countOccurrences(text, "inspecionar_")
                + countOccurrences(text, "detectar_")
                + countOccurrences(text, "rastrear_")
                + countOccurrences(text, "gerar_");
        if (explicitTools > 0) {
            score = Math.max(score, explicitTools + 4);
        }

        if (containsAny(text, "checklist", "regressao", "regressão")) {
            score += 4;
        }
        if (containsAny(text, "zip", "corrigir", "corretivo", "alterar", "criar arquivo", "modificar")) {
            score += 5;
        }
        if (containsAny(text, "impacto", "seguranca", "segurança", "persistencia", "persistência", "dependencias", "dependências")) {
            score += 3;
        }
        if (containsAny(text, "multimodelo", "modelo", "log", ".sai")) {
            score += 2;
        }

        int contextSize = safeLength(selectedCode) + safeLength(fullFileText);
        if (contextSize > 50000) {
            score += 4;
        } else if (contextSize > 20000) {
            score += 2;
        }

        int estimated = Math.max(DEFAULT_CYCLES, score);
        if (numberedItems >= 10) {
            estimated = Math.max(estimated, numberedItems + 8);
        }

        return clamp(estimated, MIN_CYCLES, MAX_CYCLES);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private static int safeLength(String value) {
        return value == null ? 0 : value.length();
    }

    private static int countNumberedItems(String text) {
        if (text == null || text.length() == 0) {
            return 0;
        }
        int count = 0;
        String[] lines = text.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i] != null && lines[i].trim().matches("^\\d+[\\.)].*")) {
                count++;
            }
        }
        return count;
    }

    private static int countOccurrences(String text, String token) {
        if (text == null || token == null || token.length() == 0) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }

    private static boolean containsAny(String text, String... tokens) {
        if (text == null || tokens == null) {
            return false;
        }
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i] != null && text.indexOf(tokens[i]) >= 0) {
                return true;
            }
        }
        return false;
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
