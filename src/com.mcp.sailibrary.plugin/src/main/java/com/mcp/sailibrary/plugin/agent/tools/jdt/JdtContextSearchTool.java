package com.mcp.sailibrary.plugin.agent.tools.jdt;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.context.JdtContextByNameResolver;
import com.mcp.sailibrary.plugin.agent.tools.support.ToolJsonSupport;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolParameterMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadataProvider;
/**
 * ---
 * yaml_header:
 * version: "1.0"
 * dependencies: 
 * - com.mcp.sailibrary.plugin.handlers.ContextualizadorJDT
 * purpose: "Expor a capacidade de geracao de AST e arvore de chamadas do JDT para a LLM."
 * design_pattern: "Adapter / Tool"
 * ---
 */
public class JdtContextSearchTool implements AgentTool, AgentToolPromptMetadataProvider  {

    @Override
    public String getName() {
        return "buscar_contexto_jdt";
    }

    /**
 * Descrição não fornecida.
 *
 * @author Renato Tomaz Nati
 */
    @Override
    public String execute(String jsonParameters) {
        String nomeClasse = ToolJsonSupport.extractJsonStringValue(jsonParameters, "classe");
        String nomeMetodo = ToolJsonSupport.extractJsonStringValue(jsonParameters, "metodo");
        int maxDepth = ToolJsonSupport.extractJsonIntValue(jsonParameters, "profundidade", 1, 1, 3);
        
        if (nomeClasse.isEmpty() || nomeMetodo.isEmpty()) {
            return "Erro Operacional: Os parametros 'classe' e 'metodo' sao obrigatorios na carga JSON.";
        }
        JdtContextByNameResolver resolver = new JdtContextByNameResolver();
        return resolver.enraizarChamadasPorNome(nomeClasse, nomeMetodo, maxDepth);
    }
    @Override
    public AgentToolPromptMetadata getPromptMetadata() {
        AgentToolPromptMetadata metadata = new AgentToolPromptMetadata();
        metadata.setToolName(getName());
        metadata.setOneLinePurpose("Gerar breadcrumb estrutural e contexto JDT a partir de classe e metodo.");
        metadata.setActivityDescription("Processa AST e devolve breadcrumb estrutural.");

        AgentToolParameterMetadata classe = new AgentToolParameterMetadata();
        classe.setName("classe");
        classe.setRequired(true);
        classe.setDescription("Nome da classe alvo para enraizamento.");
        classe.setExampleValue("RelatorioAcompanhamentoDivisaoAction");
        metadata.addParameter(classe);

        AgentToolParameterMetadata metodo = new AgentToolParameterMetadata();
        metodo.setName("metodo");
        metodo.setRequired(true);
        metodo.setDescription("Nome do metodo alvo para enraizamento.");
        metodo.setExampleValue("setupEnv");
        metadata.addParameter(metodo);

        AgentToolParameterMetadata profundidade = new AgentToolParameterMetadata();
        profundidade.setName("profundidade");
        profundidade.setRequired(false);
        profundidade.setDescription("Profundidade maxima do breadcrumb estrutural.");
        profundidade.setExampleValue("2");
        metadata.addParameter(profundidade);

        metadata.addRecommendedUseCase("Use quando a IA precisar reconstruir o contexto estrutural de um metodo conhecido por nome.");
        metadata.addRecommendedUseCase("Use quando classe e metodo ja estiverem claros e o objetivo for obter breadcrumb JDT.");
        metadata.addRecommendedUseCase("Use quando a analise depender da trilha de chamadas de um metodo especifico.");

        metadata.addGuardrail("A ferramenta exige classe e metodo validos.");
        metadata.addGuardrail("Nao use esta ferramenta como substituta de busca textual ampla.");
        metadata.addGuardrail("Mantenha a profundidade em nivel prudente para evitar excesso de ruido.");

        metadata.addJsonExample(
                "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"buscar_contexto_jdt\\\",\\\"parameters\\\":{\\\"classe\\\":\\\"RelatorioAcompanhamentoDivisaoAction\\\",\\\"metodo\\\":\\\"setupEnv\\\",\\\"profundidade\\\":\\\"2\\\"},\\\"explanation\\\":\\\"Preciso gerar o breadcrumb estrutural do metodo alvo.\\\"}"
        );

        return metadata;
    }
   
}