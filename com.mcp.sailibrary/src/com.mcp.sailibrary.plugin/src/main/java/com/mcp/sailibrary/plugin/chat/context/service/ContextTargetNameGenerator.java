package com.mcp.sailibrary.plugin.chat.context.service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mcp.sailibrary.plugin.chat.context.model.NamedContextTargetRole;
import com.mcp.sailibrary.plugin.chat.context.model.NamedStructuralContextType;

/* yaml_header: version: "1.0" purpose: "Gerar nomes locais curtos para alvos estruturais quando a sugestao por IA falhar." libraries: - java.util.regex.Pattern: runtime - java.text.Normalizer: runtime */
public class ContextTargetNameGenerator {

    private static final int MAX_NAME_LENGTH = 12;

    private static final Set<String> STOP_WORDS = new HashSet<String>(Arrays.asList(
        "public", "private", "protected", "static", "final", "void",
        "if", "for", "while", "try", "catch", "return",
        "this", "super", "null", "true", "false",
        "get", "set", "list", "find", "load", "build", "create"
    ));

    public String generateName(String selectedText, NamedContextTargetRole role, NamedStructuralContextType type, String fileName, String packageName, Set<String> existingNames) {

        String base = inferBaseName(selectedText, role, type, fileName, packageName);

        if (isBlank(base) || isStopWord(base)) {
            if (role == NamedContextTargetRole.PRIMARY) {
                base = "principal";
            } else if (role == NamedContextTargetRole.EDITABLE) {
                base = "editavel";
            } else {
                base = "referencia";
            }
        }

        base = normalize(base);

        if (isBlank(base) || isStopWord(base)) {
            base = "alvo";
        }

        return ensureUnique(base, existingNames);
    }

    private String inferBaseName(String selectedText, NamedContextTargetRole role, NamedStructuralContextType type, String fileName, String packageName) {

        if (type == NamedStructuralContextType.FILE && !isBlank(fileName)) {
            return removeExtension(fileName);
        }

        if (type == NamedStructuralContextType.PACKAGE && !isBlank(packageName)) {
            return lastPackageToken(packageName);
        }

        if (type == NamedStructuralContextType.FOLDER && !isBlank(fileName)) {
            return fileName;
        }

        if (isBlank(selectedText)) {
            return "alvo";
        }

        String trimmed = selectedText.trim();

        String methodName = inferMethodName(trimmed);
        if (!isBlank(methodName)) return simplifyVerbBasedName(methodName);

        String assignmentName = inferAssignedVariableName(trimmed);
        if (!isBlank(assignmentName)) return assignmentName;

        String declarationName = inferDeclaredVariableName(trimmed);
        if (!isBlank(declarationName)) return declarationName;

        String invocationName = inferInvocationName(trimmed);
        if (!isBlank(invocationName)) return simplifyVerbBasedName(invocationName);

        String controlName = inferControlBlockName(trimmed);
        if (!isBlank(controlName)) return controlName;

        String queryName = inferQueryLikeName(trimmed);
        if (!isBlank(queryName)) return queryName;

        return firstRelevantWord(trimmed);
    }

    private String inferMethodName(String text) {
        Matcher matcher = Pattern.compile("(?s)(public|protected|private)?\\s*(static\\s+)?(final\\s+)?(synchronized\\s+)?[\\w\\<\\>\\[\\],\\?\\s]+\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(").matcher(text);
        if (matcher.find()) {
            return matcher.group(5);
        }
        return "";
    }

    private String inferAssignedVariableName(String text) {
        Matcher matcher = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*)\\s*=").matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String inferDeclaredVariableName(String text) {
        Matcher matcher = Pattern.compile("[\\w\\<\\>\\[\\]]+\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*(=|;)").matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String inferInvocationName(String text) {
        Matcher matcher = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(").matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String inferControlBlockName(String text) {
        if (text.startsWith("if")) return "validacao";
        if (text.startsWith("for") || text.startsWith("while")) return "loop";
        if (text.startsWith("return")) return "retorno";
        if (text.startsWith("try")) return "tratamento";
        return "";
    }

    private String inferQueryLikeName(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("createcriteria(") || lower.contains("criteria")) return "criteria";
        if (lower.contains("createquery(") || lower.contains("<query") || lower.contains("select ")) return "query";
        if (lower.contains("createsqlquery(") || lower.contains("<sql-query") || lower.contains(" from ")) return "sql";
        if (lower.contains("preparestatement(")) return "jdbc";
        return "";
    }

    private String firstRelevantWord(String text) {
        Matcher matcher = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*)").matcher(text);
        while (matcher.find()) {
            String word = matcher.group(1);
            if (!isStopWord(word)) {
                return word;
            }
        }
        return "bloco";
    }

    private String simplifyVerbBasedName(String name) {
        if (isBlank(name)) {
            return "";
        }

        String[] prefixes = new String[] {
            "get", "set", "is", "find", "list", "load", "build", "create",
            "update", "validar", "processar", "montar", "buscar", "gerar"
        };

        for (int i = 0; i < prefixes.length; i++) {
            String prefix = prefixes[i];
            if (name.length() > prefix.length() + 2 && startsWithIgnoreCase(name, prefix)) {
                String candidate = name.substring(prefix.length());
                if (candidate.length() > 0) {
                    return candidate;
                }
            }
        }

        return name;
    }

    private String removeExtension(String fileName) {
        if (isBlank(fileName)) {
            return "";
        }

        int index = fileName.indexOf('.');
        if (index > 0) {
            return fileName.substring(0, index);
        }
        return fileName;
    }

    private String lastPackageToken(String packageName) {
        if (isBlank(packageName)) {
            return "";
        }

        int index = packageName.lastIndexOf('.');
        if (index >= 0 && index < packageName.length() - 1) {
            return packageName.substring(index + 1);
        }
        return packageName;
    }

    private String normalize(String input) {
        String value = input == null ? "" : input.trim();
        value = Normalizer.normalize(value, Normalizer.Form.NFD);
        value = value.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        value = value.toLowerCase();
        value = value.replaceAll("[^a-z0-9_]", "");
        if (value.length() > MAX_NAME_LENGTH) {
            value = value.substring(0, MAX_NAME_LENGTH);
        }
        return value;
    }

    private String ensureUnique(String base, Set<String> existingNames) {
        if (existingNames == null || !existingNames.contains(base)) {
            return base;
        }

        for (int i = 2; i < 1000; i++) {
            String suffix = String.valueOf(i);
            int maxBase = MAX_NAME_LENGTH - suffix.length();
            String candidateBase = base;
            if (candidateBase.length() > maxBase) {
                candidateBase = candidateBase.substring(0, maxBase);
            }
            String candidate = candidateBase + suffix;
            if (!existingNames.contains(candidate)) {
                return candidate;
            }
        }

        if (base.length() > MAX_NAME_LENGTH - 1) {
            base = base.substring(0, MAX_NAME_LENGTH - 1);
        }

        return base + "x";
    }

    private boolean isStopWord(String value) {
        if (isBlank(value)) {
            return true;
        }
        return STOP_WORDS.contains(value.toLowerCase());
    }

    private boolean startsWithIgnoreCase(String value, String prefix) {
        if (value == null || prefix == null) {
            return false;
        }
        if (value.length() < prefix.length()) {
            return false;
        }
        return value.substring(0, prefix.length()).equalsIgnoreCase(prefix);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}