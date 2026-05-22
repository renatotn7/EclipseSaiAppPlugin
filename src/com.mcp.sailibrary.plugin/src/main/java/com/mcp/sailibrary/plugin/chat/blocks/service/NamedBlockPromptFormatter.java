package com.mcp.sailibrary.plugin.chat.blocks.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.mcp.sailibrary.plugin.chat.blocks.model.NamedBlockKind;
import com.mcp.sailibrary.plugin.chat.blocks.model.NamedCodeBlock;

/* yaml_header: version: "1.1" purpose: "Formatar blocos textuais nomeados para a IA preservando foco principal, escopo editavel e escopo referencial." libraries: - java.util.List: runtime - java.util.Collections: runtime */
public class NamedBlockPromptFormatter {

    private static final int MAX_BLOCKS_PER_KIND = 8;
    private static final int MAX_PREVIEW_LENGTH = 160;

    public String format(List<NamedCodeBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return "";
        }

        List<NamedCodeBlock> validBlocks = filterValidBlocks(blocks);
        if (validBlocks.isEmpty()) {
            return "";
        }

        sortBlocks(validBlocks);

        StringBuilder sb = new StringBuilder();
        sb.append("=== BLOCOS NOMEADOS DA SESSAO ===\n");

        sb.append("FOCO_PRINCIPAL_DA_ANALISE:\n");
        appendKind(sb, validBlocks, NamedBlockKind.PRIMARY);

        sb.append("BLOCOS_EDITAVEIS:\n");
        appendKind(sb, validBlocks, NamedBlockKind.EDITABLE);

        sb.append("BLOCOS_REFERENCIA:\n");
        appendKind(sb, validBlocks, NamedBlockKind.REFERENCE);

        sb.append("REGRAS:\n");
        sb.append("1. O bloco PRIMARY e o sujeito central da analise e da alteracao atual.\n");
        sb.append("2. Quando existir bloco PRIMARY ativo, trate esse bloco como alvo principal da operacao.\n");
        sb.append("3. Quando o usuario mencionar um nome de bloco, trate esse nome como referencia exata ao trecho correspondente.\n");
        sb.append("4. BLOCOS_REFERENCIA existem apenas para consulta, comparacao e alinhamento semantico.\n");
        sb.append("5. BLOCOS_EDITAVEIS existem como alvos permitidos de alteracao quando a instrucao mencionar explicitamente seus nomes.\n");
        sb.append("6. Nunca altere BLOCOS_REFERENCIA.\n");
        sb.append("7. Se houver conflito entre descricoes vagas, selecao atual e nomes de blocos, priorize primeiro o bloco PRIMARY, depois os nomes de blocos explicitamente citados pelo usuario.\n");
        sb.append("8. Se existirem blocos nomeados ativos, considere-os parte do contexto oficial desta sessao.\n");
        sb.append("9. A selecao atual do editor continua relevante, mas passa a ser contexto auxiliar quando houver bloco PRIMARY ativo, salvo instrucao explicita em sentido contrario.\n");
        return sb.toString();
    }

    private void appendKind(StringBuilder sb, List<NamedCodeBlock> blocks, NamedBlockKind kind) {
        int count = 0;

        for (int i = 0; i < blocks.size(); i++) {
            NamedCodeBlock block = blocks.get(i);
            if (block.getKind() != kind) {
                continue;
            }

            if (count >= MAX_BLOCKS_PER_KIND) {
                sb.append("- [RESUMO] limite de blocos deste tipo atingido no prompt.\n");
                return;
            }

            sb.append("- ")
              .append(safe(block.getName()))
              .append(" -> arquivo ")
              .append(safe(block.getFileName()))
              .append(" linhas ")
              .append(formatLineRange(block.getStartLine(), block.getEndLine()))
              .append(" | preview: ")
              .append(sanitizePreview(block.getPreview()))
              .append("\n");

            count++;
        }

        if (count == 0) {
            sb.append("- nenhum\n");
        }
    }

    private List<NamedCodeBlock> filterValidBlocks(List<NamedCodeBlock> blocks) {
        List<NamedCodeBlock> result = new ArrayList<NamedCodeBlock>();

        for (int i = 0; i < blocks.size(); i++) {
            NamedCodeBlock block = blocks.get(i);
            if (!isValidBlock(block)) {
                continue;
            }
            result.add(block);
        }

        return result;
    }

    private boolean isValidBlock(NamedCodeBlock block) {
        if (block == null) {
            return false;
        }

        if (block.getKind() == null) {
            return false;
        }

        if (isBlank(block.getName())) {
            return false;
        }

        if (isBlank(block.getFileName())) {
            return false;
        }

        if (block.getStartLine() <= 0 || block.getEndLine() <= 0) {
            return false;
        }

        if (block.getEndLine() < block.getStartLine()) {
            return false;
        }

        return true;
    }

    private void sortBlocks(List<NamedCodeBlock> blocks) {
        Collections.sort(blocks, new Comparator<NamedCodeBlock>() {
            @Override
            public int compare(NamedCodeBlock a, NamedCodeBlock b) {
                int kindCompare = Integer.compare(kindWeight(a.getKind()), kindWeight(b.getKind()));
                if (kindCompare != 0) {
                    return kindCompare;
                }

                int fileCompare = safe(a.getFileName()).compareToIgnoreCase(safe(b.getFileName()));
                if (fileCompare != 0) {
                    return fileCompare;
                }

                int lineCompare = Integer.compare(a.getStartLine(), b.getStartLine());
                if (lineCompare != 0) {
                    return lineCompare;
                }

                return safe(a.getName()).compareToIgnoreCase(safe(b.getName()));
            }
        });
    }

    private int kindWeight(NamedBlockKind kind) {
        if (kind == NamedBlockKind.PRIMARY) {
            return 0;
        }
        if (kind == NamedBlockKind.EDITABLE) {
            return 1;
        }
        return 2;
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
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
    
}