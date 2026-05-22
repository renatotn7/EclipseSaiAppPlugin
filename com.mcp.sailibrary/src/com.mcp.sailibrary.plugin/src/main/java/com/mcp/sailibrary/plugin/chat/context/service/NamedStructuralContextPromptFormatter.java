package com.mcp.sailibrary.plugin.chat.context.service;

import java.util.List;

import com.mcp.sailibrary.plugin.chat.context.model.NamedContextTargetRole;
import com.mcp.sailibrary.plugin.chat.context.model.NamedStructuralContext;
import com.mcp.sailibrary.plugin.chat.context.model.NamedStructuralContextType;

public class NamedStructuralContextPromptFormatter {

    public String format(List<NamedStructuralContext> contexts) {
        if (contexts == null || contexts.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== CONTEXTO ESTRUTURAL DA SESSAO ===\n");

        appendRole(sb, "PRIMARY", contexts, NamedContextTargetRole.PRIMARY);
        appendRole(sb, "EDITABLE", contexts, NamedContextTargetRole.EDITABLE);
        appendRole(sb, "REFERENCE", contexts, NamedContextTargetRole.REFERENCE);

        sb.append("REGRAS:\n");
        sb.append("1. Itens estruturais PRIMARY sao o foco estrutural principal da tarefa.\n");
        sb.append("2. Itens estruturais EDITABLE podem ser alterados ou usados como destino para criacao.\n");
        sb.append("3. Itens estruturais REFERENCE nao devem ser alterados.\n");
        sb.append("4. Packages vazias e pastas nao devem ser tratadas como foco principal por padrao.\n");

        return sb.toString();
    }

    private void appendRole(StringBuilder sb, String title, List<NamedStructuralContext> contexts, NamedContextTargetRole role) {
        sb.append(title).append(":\n");

        boolean found = false;
        for (int i = 0; i < contexts.size(); i++) {
            NamedStructuralContext context = contexts.get(i);
            if (context == null || context.getRole() != role || !context.isUsable()) {
                continue;
            }

            found = true;
            sb.append("- ").append(context.getName())
              .append(" -> tipo=").append(context.getType() != null ? context.getType().name() : "");

            if (context.getType() == NamedStructuralContextType.FILE && context.getFileName() != null) {
                sb.append(" | arquivo=").append(context.getFileName());
            } else if (context.getType() == NamedStructuralContextType.PACKAGE && context.getPackageName() != null) {
                sb.append(" | package=").append(context.getPackageName());
            } else if (context.getType() == NamedStructuralContextType.FOLDER) {
                if (context.getRelativePath() != null && context.getRelativePath().trim().length() > 0) {
                    sb.append(" | caminho=").append(context.getRelativePath());
                } else if (context.getFileName() != null) {
                    sb.append(" | pasta=").append(context.getFileName());
                }
            }

            sb.append("\n");
        }

        if (!found) {
            sb.append("- nenhum\n");
        }
    }
}