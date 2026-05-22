package com.mcp.sailibrary.plugin.agent.prompt;

import java.util.ArrayList;
import java.util.List;

import com.mcp.sailibrary.plugin.agent.AgentTool;

/** * Monta os blocos textuais de ferramentas homologadas, exemplos validos e * recomendacoes de uso a partir dos metadados estruturados expostos pelas * tools. * * <p>O objetivo desta classe e preservar o estilo textual do prompt atual, * reduzindo duplicacao manual e mantendo uma fonte unica da verdade por * ferramenta.</p> * * <p>Esta implementacao foi desenhada para ser eficiente: * <ul> * <li>ignora tools sem metadado valido</li> * <li>compacta parametros em uma unica linha</li> * <li>limita exemplos por ferramenta</li> * <li>permite montar apenas os blocos realmente desejados</li> * </ul> * </p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class AgentToolPromptSectionBuilder {

    private static final int DEFAULT_MAX_EXAMPLES_PER_TOOL = 1;

    /** * Monta o bloco principal de ferramentas homologadas no formato textual * usado pelo prompt atual. * * @param tools lista de ferramentas registradas no arsenal atual * @return bloco textual de ferramentas homologadas * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String buildToolsSection(List<AgentTool> tools) {
        StringBuilder sb = new StringBuilder();
        sb.append("\\n=== FERRAMENTAS HOMOLOGADAS E SUAS CAPACIDADES ===\\n");
        sb.append("Voce pode solicitar APENAS estas ferramentas:\\n");

        List<AgentToolPromptMetadata> metadatas = collectPromptMetadatas(tools);
        int index = 1;

        for (int i = 0; i < metadatas.size(); i++) {
            AgentToolPromptMetadata metadata = metadatas.get(i);

            sb.append(index)
              .append(". ")
              .append(metadata.getToolName())
              .append(" -> parametros: ")
              .append(formatParameters(metadata))
              .append(" \\n");

            sb.append(index)
              .append(".1 -> ")
              .append(metadata.getToolName())
              .append(" - Atividade: ")
              .append(safe(metadata.getActivityDescription()))
              .append(" \\n");

            index++;
        }

        return sb.toString();
    }

    /** * Monta o bloco de exemplos validos de executar_ferramenta. * * <p>Por padrao, esta implementacao usa no maximo um exemplo por * ferramenta para preservar o contexto do prompt.</p> * * @param tools lista de ferramentas registradas no arsenal atual * @return bloco textual de exemplos JSON validos * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String buildExamplesSection(List<AgentTool> tools) {
        return buildExamplesSection(tools, DEFAULT_MAX_EXAMPLES_PER_TOOL);
    }

    /** * Monta o bloco de exemplos validos de executar_ferramenta, permitindo * controlar quantos exemplos cada ferramenta pode contribuir. * * @param tools lista de ferramentas registradas no arsenal atual * @param maxExamplesPerTool limite de exemplos por ferramenta * @return bloco textual de exemplos JSON validos * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String buildExamplesSection(List<AgentTool> tools, int maxExamplesPerTool) {
        int safeMaxExamples = maxExamplesPerTool > 0 ? maxExamplesPerTool : DEFAULT_MAX_EXAMPLES_PER_TOOL;

        StringBuilder sb = new StringBuilder();
        sb.append("\\n=== EXEMPLOS VALIDOS DE executar_ferramenta (DENTRO DA TAG <codigo_final>) ===\\n");

        List<AgentToolPromptMetadata> metadatas = collectPromptMetadatas(tools);
        for (int i = 0; i < metadatas.size(); i++) {
            AgentToolPromptMetadata metadata = metadatas.get(i);
            List<String> examples = metadata.getJsonExamples();

            int emitted = 0;
            for (int j = 0; j < examples.size() && emitted < safeMaxExamples; j++) {
                String example = examples.get(j);
                if (isBlank(example)) {
                    continue;
                }

                sb.append(example).append("\\n");
                emitted++;
            }
        }

        return sb.toString();
    }

    /** * Monta um bloco resumido de recomendacoes de uso das ferramentas. * * <p>Este bloco e opcional e deve ser usado apenas quando houver beneficio * real de orientar a IA sobre o melhor encaixe de cada ferramenta sem * inflar demais o prompt.</p> * * @param tools lista de ferramentas registradas no arsenal atual * @return bloco textual resumido de recomendacoes * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String buildRecommendedUseCasesSection(List<AgentTool> tools) {
        StringBuilder sb = new StringBuilder();
        sb.append("\\n=== RECOMENDACOES DE USO DAS FERRAMENTAS ===\\n");

        List<AgentToolPromptMetadata> metadatas = collectPromptMetadatas(tools);
        boolean appendedAtLeastOne = false;

        for (int i = 0; i < metadatas.size(); i++) {
            AgentToolPromptMetadata metadata = metadatas.get(i);
            List<String> useCases = metadata.getRecommendedUseCases();

            if (useCases == null || useCases.isEmpty()) {
                continue;
            }

            sb.append("- ")
              .append(metadata.getToolName())
              .append(": ")
              .append(compactUseCases(useCases))
              .append("\\n");

            appendedAtLeastOne = true;
        }

        if (!appendedAtLeastOne) {
            sb.append("- Nenhuma recomendacao adicional foi declarada pelas ferramentas registradas.\\n");
        }

        return sb.toString();
    }

    /** * Monta um bloco completo e eficiente contendo ferramentas homologadas, * exemplos validos e recomendacoes resumidas. * * <p>Use este metodo quando quiser gerar a secao operacional de ferramentas * do prompt de uma vez so, preservando ordem e consistencia textual.</p> * * @param tools lista de ferramentas registradas no arsenal atual * @param includeExamples true quando exemplos devem ser incluidos * @param includeUseCases true quando recomendacoes devem ser incluidas * @param maxExamplesPerTool limite de exemplos por ferramenta * @return bloco textual consolidado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String buildCompleteSections(List<AgentTool> tools, boolean includeExamples, boolean includeUseCases, int maxExamplesPerTool) {

        StringBuilder sb = new StringBuilder();
        sb.append(buildToolsSection(tools));

        if (includeUseCases) {
            sb.append(buildRecommendedUseCasesSection(tools));
        }

        if (includeExamples) {
            sb.append(buildExamplesSection(tools, maxExamplesPerTool));
        }

        return sb.toString();
    }

    /** * Coleta apenas os metadados validos das tools que implementam o contrato * de prompt metadata. * * @param tools lista total de ferramentas registradas * @return lista de metadados utilizaveis no prompt * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private List<AgentToolPromptMetadata> collectPromptMetadatas(List<AgentTool> tools) {
        List<AgentToolPromptMetadata> result = new ArrayList<AgentToolPromptMetadata>();

        if (tools == null) {
            return result;
        }

        for (int i = 0; i < tools.size(); i++) {
            AgentTool tool = tools.get(i);

            if (!(tool instanceof AgentToolPromptMetadataProvider)) {
                continue;
            }

            AgentToolPromptMetadata metadata =
                    ((AgentToolPromptMetadataProvider) tool).getPromptMetadata();

            if (metadata != null && metadata.isUsable()) {
                result.add(metadata);
            }
        }

        return result;
    }

    /** * Formata a lista de parametros da ferramenta no mesmo estilo textual do * prompt atual. * * @param metadata metadados da ferramenta * @return texto textual de parametros em linha unica * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String formatParameters(AgentToolPromptMetadata metadata) {
        if (metadata == null || metadata.getParameters() == null || metadata.getParameters().isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < metadata.getParameters().size(); i++) {
            AgentToolParameterMetadata parameter = metadata.getParameters().get(i);
            if (parameter == null || isBlank(parameter.getName())) {
                continue;
            }

            if (sb.length() > 0) {
                sb.append(", ");
            }

            sb.append(parameter.getName());
        }

        return sb.toString();
    }

    /** * Compacta os casos de uso recomendados em uma unica linha curta. * * @param useCases lista de casos de uso recomendados * @return texto compacto com separador visual leve * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String compactUseCases(List<String> useCases) {
        if (useCases == null || useCases.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < useCases.size(); i++) {
            String useCase = useCases.get(i);
            if (isBlank(useCase)) {
                continue;
            }

            if (sb.length() > 0) {
                sb.append(" | ");
            }

            sb.append(useCase.trim());
        }

        return sb.toString();
    }

    /** * Retorna string segura nao nula. * * @param value valor original * @return string segura * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    /** * Retorna true quando o valor informado for nulo ou vazio. * * @param value valor a validar * @return true quando o valor for branco * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}