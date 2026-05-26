package com.mcp.sailibrary.plugin.chat.context.service;

import java.util.List;

import com.mcp.sailibrary.plugin.chat.context.model.NamedContextTargetRole;
import com.mcp.sailibrary.plugin.chat.context.model.NamedStructuralContext;
import com.mcp.sailibrary.plugin.chat.context.model.NamedStructuralContextType;

/* --- version: "1.1" libraries: - List - NamedContextTargetRole - NamedStructuralContext - NamedStructuralContextType objetivo: "Formatar o contexto estrutural nomeado da sessao para o prompt da IA, deixando explicita a semantica de foco e mutabilidade." --- */

/** * Formata o contexto estrutural nomeado da sessao para o prompt da IA. * * <p>Esta classe deixa explicito no texto do prompt quais contextos sao: * <ul> * <li>PRIMARY: foco principal e tambem mutavel, conforme a policy permitir</li> * <li>EDITABLE: mutavel, conforme a policy permitir</li> * <li>REFERENCE: somente leitura</li> * </ul> * </p> * * @author Renato Tomaz Nati * @since 2026-05-24 */
public class NamedStructuralContextPromptFormatter {

    /** * Caller: ChatAiController.construirInstrucaoFinal * Callee: appendRole * Objetivo: Montar o contexto estrutural nomeado da sessao para a IA. * Feature: Contexto PRIMARY passa a ser tratado como foco principal e * tambem como alvo mutavel. Contexto EDITABLE continua mutavel. Contexto * REFERENCE continua somente leitura. * Data modificacao: 2026-05-24 00:00 * * @param contexts contextos estruturais nomeados da sessao * @return texto formatado para o prompt * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public String format(List<NamedStructuralContext> contexts) {
        if (contexts == null || contexts.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== CONTEXTO ESTRUTURAL DA SESSAO ===").append("\n");

        appendRole(sb, "PRIMARY", contexts, NamedContextTargetRole.PRIMARY);
        appendRole(sb, "EDITABLE", contexts, NamedContextTargetRole.EDITABLE);
        appendRole(sb, "REFERENCE", contexts, NamedContextTargetRole.REFERENCE);

        sb.append("REGRAS:").append("\n");
        sb.append("1. Itens estruturais PRIMARY sao o foco principal da tarefa e tambem podem ser usados como alvo de criacao ou alteracao quando a policy permitir.").append("\n");
        sb.append("2. Itens estruturais EDITABLE tambem podem ser usados como alvo de criacao ou alteracao quando a policy permitir.").append("\n");
        sb.append("3. Itens estruturais REFERENCE nao devem ser alterados.").append("\n");
        sb.append("4. Quando existir contexto estrutural PRIMARY, trate-o como o destino estrutural principal da tarefa atual, salvo instrucao explicita em contrario.").append("\n");

        return sb.toString();
    }

    /** * Caller: format * Callee: N/A * Objetivo: Listar contextos estruturais por role no prompt. * Feature: Marca explicitamente se cada contexto e mutavel ou nao para * evitar interpretacao errada do modelo. * Data modificacao: 2026-05-24 00:00 * * @param sb acumulador textual * @param title titulo do bloco * @param contexts lista de contextos * @param role role filtrada * * @author Renato Tomaz Nati * @since 2026-05-24 */
    private void appendRole(StringBuilder sb, String title, List<NamedStructuralContext> contexts, NamedContextTargetRole role) {
        sb.append(title).append(":").append("\n");

        boolean found = false;
        for (int i = 0; i < contexts.size(); i++) {
            NamedStructuralContext context = contexts.get(i);
            if (context == null || context.getRole() != role || !context.isUsable()) {
                continue;
            }

            found = true;

            boolean mutavel = role == NamedContextTargetRole.PRIMARY
                    || role == NamedContextTargetRole.EDITABLE;

            sb.append("- ").append(context.getName())
              .append(" -> tipo=").append(context.getType() != null ? context.getType().name() : "")
              .append(" | mutavel=").append(mutavel ? "true" : "false");

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
            sb.append("- nenhum").append("\n");
        }
    }
}