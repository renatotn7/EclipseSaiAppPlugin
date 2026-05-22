package com.mcp.sailibrary.plugin.agent.prompt;

/** * Contrato para ferramentas que desejam expor metadados estruturados usados na * composicao automatica do prompt da IA. * * <p>Este contrato desacopla a implementacao real da ferramenta do formato * textual do prompt, permitindo que um builder central gere a secao de * ferramentas homologadas por loop.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public interface AgentToolPromptMetadataProvider {

    /** * Retorna os metadados estruturados da ferramenta para composicao do * prompt operacional. * * @return metadados de prompt da ferramenta * * @author Renato Tomaz Nati * @since 2026-05-20 */
    AgentToolPromptMetadata getPromptMetadata();
}