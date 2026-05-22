package com.mcp.sailibrary.plugin.chat.context.service;

import java.util.List;

import com.mcp.sailibrary.plugin.chat.context.model.NamedContextTarget;
import com.mcp.sailibrary.plugin.chat.context.model.NamedContextTargetRole;
import com.mcp.sailibrary.plugin.chat.context.model.NamedContextTargetType;

/* yaml_header: version: "1.0" purpose: "Formatar alvos contextuais nomeados para a IA preservando foco principal, escopo editavel e escopo referencial." libraries: - java.util.List: runtime */
public class NamedContextPromptFormatter {

    private static final int MAX_TARGETS_PER_ROLE = 10;
    private static final int MAX_PREVIEW_LENGTH = 160;

    public String format(List<NamedContextTarget> targets) {
        if (targets == null || targets.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== ALVOS CONTEXTUAIS DA SESSAO ===\n");

        appendPrimary(sb, targets);
        appendRole(sb, targets, NamedContextTargetRole.EDITABLE, "ESCOPO_EDITAVEL");
        appendRole(sb, targets, NamedContextTargetRole.REFERENCE, "ESCOPO_REFERENCIAL");

        sb.append("REGRAS:\n");
        sb.append("1. O alvo PRIMARY e o sujeito central da analise.\n");
        sb.append("2. O ESCOPO_EDITAVEL pode ser alterado quando a instrucao justificar isso.\n");
        sb.append("3. O ESCOPO_REFERENCIAL existe apenas para consulta e alinhamento semantico.\n");
        sb.append("4. Nunca altere alvos do ESCOPO_REFERENCIAL.\n");
        sb.append("5. Quando o usuario mencionar um nome de alvo, trate esse nome como referencia exata ao alvo correspondente.\n");
        sb.append("6. Packages no escopo editavel ou referencial podem servir como destino para criacao de novas classes ou subpackages se a tarefa exigir isso.\n");
        sb.append("7. Package vazio nao deve ser tratado como PRIMARY por padrao.\n");
        sb.append("8. Contexto estrutural EDITABLE do tipo PACKAGE ou FOLDER permite criacao de novos arquivos, mas nao permite alterar arquivos preexistentes nao marcados explicitamente.\n");
        return sb.toString();
    }

    private void appendPrimary(StringBuilder sb, List<NamedContextTarget> targets) {
        sb.append("FOCO_PRINCIPAL_DA_ANALISE:\n");

        NamedContextTarget primary = null;
        for (int i = 0; i < targets.size(); i++) {
            if (targets.get(i).getRole() == NamedContextTargetRole.PRIMARY && targets.get(i).isUsable()) {
                primary = targets.get(i);
                break;
            }
        }

        if (primary == null) {
            sb.append("- nenhum\n");
            return;
        }

        sb.append("- ").append(primary.getName())
          .append(" -> ")
          .append(describeTarget(primary))
          .append("\n");
    }

    private void appendRole(StringBuilder sb, List<NamedContextTarget> targets, NamedContextTargetRole role, String title) {
        sb.append(title).append(":\n");

        int count = 0;
        for (int i = 0; i < targets.size(); i++) {
            NamedContextTarget target = targets.get(i);
            if (target.getRole() != role || !target.isUsable()) {
                continue;
            }

            if (count >= MAX_TARGETS_PER_ROLE) {
                sb.append("- [RESUMO] limite de alvos deste tipo atingido no prompt.\n");
                return;
            }

            sb.append("- ").append(target.getName())
              .append(" -> ")
              .append(describeTarget(target))
              .append("\n");

            count++;
        }

        if (count == 0) {
            sb.append("- nenhum\n");
        }
    }

    private String describeTarget(NamedContextTarget target) {
        StringBuilder sb = new StringBuilder();

        if (target.getType() == NamedContextTargetType.CODE_BLOCK) {
            sb.append("tipo=CODE_BLOCK");
            sb.append(" | arquivo=").append(safe(target.getFileName()));
            sb.append(" | linhas=").append(formatLineRange(target.getStartLine(), target.getEndLine()));
            sb.append(" | preview=").append(sanitizePreview(target.getPreview()));
            return sb.toString();
        }

        if (target.getType() == NamedContextTargetType.FILE) {
            sb.append("tipo=FILE");
            sb.append(" | arquivo=").append(safe(target.getFileName()));
            if (!isBlank(target.getRelativeFilePath())) {
                sb.append(" | caminho=").append(target.getRelativeFilePath());
            }
            return sb.toString();
        }

        sb.append("tipo=PACKAGE");
        if (!isBlank(target.getPackageName())) {
            sb.append(" | package=").append(target.getPackageName());
        }
        if (!isBlank(target.getRelativeFilePath())) {
            sb.append(" | caminho=").append(target.getRelativeFilePath());
        }
        return sb.toString();
    }

    private String sanitizePreview(String preview) {
        if (isBlank(preview)) {
            return "(sem preview)";
        }

        String value = preview.trim();
        value = value.replace("\r", " ");
        value = value.replace("\n", " ");
        value = value.replace("\t", " ");
        value = value.replaceAll("\\s+", " ");
        value = value.replace("\"", "'");
        value = value.trim();

        if (value.length() > MAX_PREVIEW_LENGTH) {
            value = value.substring(0, MAX_PREVIEW_LENGTH) + "...";
        }

        return value;
    }

    private String formatLineRange(int startLine, int endLine) {
        if (startLine <= 0 || endLine <= 0) {
            return "indefinido";
        }
        if (startLine == endLine) {
            return String.valueOf(startLine);
        }
        return startLine + "-" + endLine;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}