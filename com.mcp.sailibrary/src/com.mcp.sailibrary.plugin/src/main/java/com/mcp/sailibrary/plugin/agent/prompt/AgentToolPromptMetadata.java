package com.mcp.sailibrary.plugin.agent.prompt;

import java.util.ArrayList;
import java.util.List;

/** * Representa os metadados de prompt de uma ferramenta homologada. * * <p>Esta estrutura serve como fonte unica da verdade para descricao, * parametros, atividade e usos recomendados da ferramenta dentro do prompt * operacional da IA.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class AgentToolPromptMetadata {

    private String toolName;
    private String oneLinePurpose;
    private String activityDescription;
    private List<AgentToolParameterMetadata> parameters = new ArrayList<AgentToolParameterMetadata>();
    private List<String> recommendedUseCases = new ArrayList<String>();
    private List<String> guardrails = new ArrayList<String>();
    private List<String> jsonExamples = new ArrayList<String>();

    /** * Retorna o nome homologado da ferramenta. * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = safeTrim(toolName);
    }

    /** * Retorna o resumo curto da finalidade da ferramenta. * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String getOneLinePurpose() {
        return oneLinePurpose;
    }

    public void setOneLinePurpose(String oneLinePurpose) {
        this.oneLinePurpose = safeTrim(oneLinePurpose);
    }

    /** * Retorna a descricao detalhada da atividade da ferramenta. * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String getActivityDescription() {
        return activityDescription;
    }

    public void setActivityDescription(String activityDescription) {
        this.activityDescription = safeTrim(activityDescription);
    }

    /** * Retorna os parametros estruturados da ferramenta. * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public List<AgentToolParameterMetadata> getParameters() {
        return parameters;
    }

    public void setParameters(List<AgentToolParameterMetadata> parameters) {
        this.parameters = parameters != null ? parameters : new ArrayList<AgentToolParameterMetadata>();
    }

    /** * Retorna os casos de uso recomendados da ferramenta. * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public List<String> getRecommendedUseCases() {
        return recommendedUseCases;
    }

    public void setRecommendedUseCases(List<String> recommendedUseCases) {
        this.recommendedUseCases = recommendedUseCases != null ? recommendedUseCases : new ArrayList<String>();
    }

    /** * Retorna guardrails e cuidados de uso da ferramenta. * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public List<String> getGuardrails() {
        return guardrails;
    }

    public void setGuardrails(List<String> guardrails) {
        this.guardrails = guardrails != null ? guardrails : new ArrayList<String>();
    }

    /** * Retorna exemplos JSON de uso da ferramenta. * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public List<String> getJsonExamples() {
        return jsonExamples;
    }

    public void setJsonExamples(List<String> jsonExamples) {
        this.jsonExamples = jsonExamples != null ? jsonExamples : new ArrayList<String>();
    }

    /** * Retorna true quando os metadados estiverem minimamente consistentes. * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public boolean isUsable() {
        return toolName != null && toolName.trim().length() > 0
                && activityDescription != null && activityDescription.trim().length() > 0;
    }

    public void addParameter(AgentToolParameterMetadata parameter) {
        if (parameter != null) {
            this.parameters.add(parameter);
        }
    }

    public void addRecommendedUseCase(String useCase) {
        if (useCase != null && useCase.trim().length() > 0) {
            this.recommendedUseCases.add(useCase.trim());
        }
    }

    public void addGuardrail(String guardrail) {
        if (guardrail != null && guardrail.trim().length() > 0) {
            this.guardrails.add(guardrail.trim());
        }
    }

    public void addJsonExample(String jsonExample) {
        if (jsonExample != null && jsonExample.trim().length() > 0) {
            this.jsonExamples.add(jsonExample.trim());
        }
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}