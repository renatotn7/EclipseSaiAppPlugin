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

    /** * Caller: ChatAiController.construirInstrucaoFinal * Callee: N/A * Objetivo: Montar o contexto textual nomeado da sessao para a IA. * Feature: Bloco PRIMARY passa a ser tratado como alvo principal e tambem como * alvo mutavel. Bloco EDITABLE continua mutavel. Bloco REFERENCE continua * somente leitura. * Data modificacao: 2026-05-24 00:00 * * @param blocks blocos textuais nomeados da sessao * @return texto formatado para o prompt * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public String format(List<NamedCodeBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== CONTEXTO TEXTUAL NOMEADO DA SESSAO ===").append("\n");
        sb.append("FOCO_PRINCIPAL_TEXTUAL:").append("\n");

        boolean encontrouPrimary = false;
        for (int i = 0; i < blocks.size(); i++) {
            NamedCodeBlock block = blocks.get(i);
            if (block == null || block.getKind() == null) {
                continue;
            }

            if (block.getKind().name().equals("PRIMARY")) {
                encontrouPrimary = true;
                sb.append("- ").append(block.getName())
                  .append(" -> arquivo=").append(block.getFileName())
                  .append(" | linhas=").append(block.getStartLine()).append("-").append(block.getEndLine())
                  .append(" | papel=PRIMARY")
                  .append(" | mutavel=true")
                  .append("\n");
            }
        }

        if (!encontrouPrimary) {
            sb.append("- nenhum").append("\n");
        }

        sb.append("BLOCOS_EDITAVEIS:").append("\n");
        boolean encontrouEditable = false;
        for (int i = 0; i < blocks.size(); i++) {
            NamedCodeBlock block = blocks.get(i);
            if (block == null || block.getKind() == null) {
                continue;
            }

            if (block.getKind().name().equals("EDITABLE")) {
                encontrouEditable = true;
                sb.append("- ").append(block.getName())
                  .append(" -> arquivo=").append(block.getFileName())
                  .append(" | linhas=").append(block.getStartLine()).append("-").append(block.getEndLine())
                  .append(" | papel=EDITABLE")
                  .append(" | mutavel=true")
                  .append("\n");
            }
        }

        if (!encontrouEditable) {
            sb.append("- nenhum").append("\n");
        }

        sb.append("BLOCOS_REFERENCIAIS:").append("\n");
        boolean encontrouReference = false;
        for (int i = 0; i < blocks.size(); i++) {
            NamedCodeBlock block = blocks.get(i);
            if (block == null || block.getKind() == null) {
                continue;
            }

            if (block.getKind().name().equals("REFERENCE")) {
                encontrouReference = true;
                sb.append("- ").append(block.getName())
                  .append(" -> arquivo=").append(block.getFileName())
                  .append(" | linhas=").append(block.getStartLine()).append("-").append(block.getEndLine())
                  .append(" | papel=REFERENCE")
                  .append(" | mutavel=false")
                  .append("\n");
            }
        }

        if (!encontrouReference) {
            sb.append("- nenhum").append("\n");
        }

        sb.append("REGRAS:").append("\n");
        sb.append("1. Bloco PRIMARY textual e o foco principal da tarefa e tambem pode ser alterado quando a acao do usuario exigir mutacao.").append("\n");
        sb.append("2. Bloco EDITABLE textual tambem pode ser alterado.").append("\n");
        sb.append("3. Bloco REFERENCE textual deve ser usado apenas como referencia e nao deve ser alterado.").append("\n");
        sb.append("4. Se houver PRIMARY textual ativo, trate-o como o principal alvo mutavel da analise atual, salvo instrucao explicita em contrario.").append("\n");

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