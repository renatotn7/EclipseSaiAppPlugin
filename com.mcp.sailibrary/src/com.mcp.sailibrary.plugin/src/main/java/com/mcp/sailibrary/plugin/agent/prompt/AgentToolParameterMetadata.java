package com.mcp.sailibrary.plugin.agent.prompt;

/** * Representa um parametro exposto por uma ferramenta para documentacao e * composicao automatica do prompt operacional da IA. * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class AgentToolParameterMetadata {

    private String name;
    private boolean required;
    private String description;
    private String exampleValue;

    /** * Retorna o nome do parametro. * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = safeTrim(name);
    }

    /** * Retorna true quando o parametro for obrigatorio. * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    /** * Retorna a descricao curta do parametro. * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = safeTrim(description);
    }

    /** * Retorna um valor de exemplo do parametro. * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String getExampleValue() {
        return exampleValue;
    }

    public void setExampleValue(String exampleValue) {
        this.exampleValue = safeTrim(exampleValue);
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}