package com.mcp.sailibrary.plugin.mcp.multimodel.routing;

/* --- version: "1.1" libraries: - N/A objetivo: "Fornecer o mapeamento padrao entre papeis cognitivos e nomes de modelos MCP, preservando suporte a monomodelo e separando investigador de planejador." --- */

/** * Resolver padrao de nomes de modelos MCP. * * <p>Esta implementacao centraliza o nome dos modelos usados pelo plugin e * permite alternar entre: * <ul> * <li>modo monomodelo, totalmente compativel com o fluxo atual</li> * <li>modo multimodelo, com investigador, planejador, gerador e auditor</li> * </ul> * </p> * * @author Renato Tomaz Nati * @since 2026-05-24 */
public class DefaultMcpModelNameResolver implements ModelNameResolver {

    private String investigatorModelName;
    private String plannerModelName;
    private String codeGeneratorModelName;
    private String codeAuditorModelName;
    private String summarizerModelName;
   

    /** * Caller: Bootstrapping da camada MCP * Callee: N/A * Objetivo: Inicializar o resolver padrao com nomes de modelos seguros e * suporte a monomodelo desligado por padrao. * Data modificacao: 2026-05-24 00:00 * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public DefaultMcpModelNameResolver() {
        this.investigatorModelName = "O3";
        this.plannerModelName = "GPT54";
        this.codeGeneratorModelName = "GPT52CODEX";
        this.codeAuditorModelName = "CLAUDESONNET46";
        this.summarizerModelName = "GPT54MINI";
    }
    @Override
    public String resolveInvestigatorModelName() {
        return investigatorModelName;
    }

    @Override
    public String resolvePlannerModelName() {
        return plannerModelName;
    }

    @Override
    public String resolveCodeGeneratorModelName() {
     
        return codeGeneratorModelName;
    }

    @Override
    public String resolveCodeAuditorModelName() {
        
        return codeAuditorModelName;
    }

    @Override
    public String resolveSummarizerModelName() {
       
        return summarizerModelName;
    }



    public void setInvestigatorModelName(String investigatorModelName) {
        if (isBlank(investigatorModelName)) {
            return;
        }
        this.investigatorModelName = investigatorModelName.trim();
    }

    public void setPlannerModelName(String plannerModelName) {
        if (isBlank(plannerModelName)) {
            return;
        }
        this.plannerModelName = plannerModelName.trim();
    }

    public void setCodeGeneratorModelName(String codeGeneratorModelName) {
        if (isBlank(codeGeneratorModelName)) {
            return;
        }
        this.codeGeneratorModelName = codeGeneratorModelName.trim();
    }

    public void setCodeAuditorModelName(String codeAuditorModelName) {
        if (isBlank(codeAuditorModelName)) {
            return;
        }
        this.codeAuditorModelName = codeAuditorModelName.trim();
    }

    public void setSummarizerModelName(String summarizerModelName) {
        if (isBlank(summarizerModelName)) {
            return;
        }
        this.summarizerModelName = summarizerModelName.trim();
    }

 

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}