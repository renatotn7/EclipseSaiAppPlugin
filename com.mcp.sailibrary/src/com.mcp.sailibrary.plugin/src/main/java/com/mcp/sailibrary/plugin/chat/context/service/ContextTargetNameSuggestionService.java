package com.mcp.sailibrary.plugin.chat.context.service;

import java.util.Set;

import com.mcp.sailibrary.plugin.chat.context.model.NamedContextTargetRole;
import com.mcp.sailibrary.plugin.chat.context.model.NamedStructuralContextType;
import com.mcp.sailibrary.plugin.mcp.SaiLibraryMcpClient;

/* yaml_header: version: "1.1" purpose: "Sugerir nomes curtos para alvos estruturais usando canal CONTEXT_NAMING com fallback local seguro." libraries: - com.mcp.sailibrary.plugin.SaiLibraryMcpClient: projeto */
public class ContextTargetNameSuggestionService {

    private final ContextTargetNameGenerator fallbackGenerator;

    public ContextTargetNameSuggestionService() {
        this.fallbackGenerator = new ContextTargetNameGenerator();
    }

    public String suggestName(String selectedText, NamedContextTargetRole role, NamedStructuralContextType type, String fileName, String packageName, Set<String> existingNames, String apiKey) {

        if (apiKey != null && apiKey.trim().length() > 0) {
            try {
                String existingNamesText = buildExistingNamesText(existingNames);
                String kindText = buildKindText(role, type, fileName, packageName);

                String rawResponse = SaiLibraryMcpClient.callSugestaoNomeBloco(
                        selectedText,
                        kindText,
                        existingNamesText,
                        apiKey
                );

                if (isInfrastructureFailure(rawResponse)) {
                    return fallbackGenerator.generateName(selectedText, role, type, fileName, packageName, existingNames);
                }

                String suggestedName = SaiLibraryMcpClient.extractSuggestedBlockName(rawResponse);

                if (isInfrastructureFailure(suggestedName)) {
                    return fallbackGenerator.generateName(selectedText, role, type, fileName, packageName, existingNames);
                }

                suggestedName = sanitizeSuggestedName(suggestedName);

                if (isUsableName(suggestedName)) {
                    return ensureUnique(suggestedName, existingNames);
                }
            } catch (Exception e) {
                // Fallback local
            }
        }

        return fallbackGenerator.generateName(selectedText, role, type, fileName, packageName, existingNames);
    }
    private boolean isInfrastructureFailure(String value) {
        if (value == null) {
            return false;
        }

        String normalized = value.trim().toLowerCase();

        if (normalized.length() == 0) {
            return false;
        }

        return normalized.contains("access denied")
                || normalized.contains("unauthorized")
                || normalized.contains("forbidden")
                || normalized.contains("not in your favourites")
                || normalized.contains("not its owner")
                || normalized.contains("tool is not")
                || normalized.contains("erro operacional")
                || normalized.contains("falha")
                || normalized.contains("exception")
                || normalized.contains("http 302")
                || normalized.contains("http 401")
                || normalized.contains("http 403")
                || normalized.contains("login")
                || normalized.contains("redirect");
    }
    private String buildKindText(NamedContextTargetRole role, NamedStructuralContextType type, String fileName, String packageName) {
        StringBuilder sb = new StringBuilder();
        sb.append("role=").append(role != null ? role.name() : "");
        sb.append(", type=").append(type != null ? type.name() : "");
        if (fileName != null && fileName.trim().length() > 0) {
            sb.append(", file=").append(fileName.trim());
        }
        if (packageName != null && packageName.trim().length() > 0) {
            sb.append(", package=").append(packageName.trim());
        }
        return sb.toString();
    }

    private String buildExistingNamesText(Set<String> existingNames) {
        if (existingNames == null || existingNames.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        java.util.Iterator<String> it = existingNames.iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    private String sanitizeSuggestedName(String value) {
        if (value == null) {
            return "";
        }

        String sanitized = value.trim().toLowerCase();
        sanitized = java.text.Normalizer.normalize(sanitized, java.text.Normalizer.Form.NFD);
        sanitized = sanitized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        sanitized = sanitized.replaceAll("[^a-z0-9]", "");

        if (sanitized.length() > 12) {
            sanitized = sanitized.substring(0, 12);
        }

        return sanitized;
    }

    private boolean isUsableName(String value) {
        if (value == null || value.trim().length() < 2) {
            return false;
        }

        if ("public".equals(value)
                || "private".equals(value)
                || "protected".equals(value)
                || "static".equals(value)
                || "final".equals(value)
                || "void".equals(value)
                || "if".equals(value)
                || "for".equals(value)
                || "while".equals(value)
                || "try".equals(value)
                || "catch".equals(value)
                || "return".equals(value)
                || "this".equals(value)
                || "super".equals(value)
                || "null".equals(value)
                || "true".equals(value)
                || "false".equals(value)
                || "get".equals(value)
                || "set".equals(value)
                || "list".equals(value)
                || "find".equals(value)
                || "load".equals(value)
                || "build".equals(value)
                || "create".equals(value)
                || "access".equals(value)
                || "denied".equals(value)
                || "accessdenied".equals(value)
                || "error".equals(value)
                || "erro".equals(value)
                || "falha".equals(value)
                || "failure".equals(value)
                || "forbidden".equals(value)
                || "unauthorized".equals(value)
                || "undefined".equals(value)
                || "unknown".equals(value)
                || "owner".equals(value)
                || "favourites".equals(value)
                || "favorite".equals(value)) {
            return false;
        }

        return true;
    }

    private String ensureUnique(String base, Set<String> existingNames) {
        if (existingNames == null || !existingNames.contains(base)) {
            return base;
        }

        for (int i = 2; i < 1000; i++) {
            String suffix = String.valueOf(i);
            int maxBase = 12 - suffix.length();
            String candidateBase = base;
            if (candidateBase.length() > maxBase) {
                candidateBase = candidateBase.substring(0, maxBase);
            }
            String candidate = candidateBase + suffix;

            if (!existingNames.contains(candidate)) {
                return candidate;
            }
        }

        if (base.length() > 11) {
            base = base.substring(0, 11);
        }

        return base + "x";
    }
}