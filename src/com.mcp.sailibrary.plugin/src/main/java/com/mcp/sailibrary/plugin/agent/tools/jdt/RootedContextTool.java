package com.mcp.sailibrary.plugin.agent.tools.jdt;

import org.eclipse.jdt.core.ICompilationUnit;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.context.ContextOrchestrator;
import com.mcp.sailibrary.plugin.agent.tools.support.ToolJsonSupport;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolParameterMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadataProvider;

public class RootedContextTool implements AgentTool, AgentToolPromptMetadataProvider {
    private ContextOrchestrator contextOrchestrator;
    private ICompilationUnit compilationUnitAtual;
    private int offsetAtual;

    public RootedContextTool(ICompilationUnit compilationUnitAtual, int offsetAtual) {
        this.contextOrchestrator = new ContextOrchestrator();
        this.compilationUnitAtual = compilationUnitAtual;
        this.offsetAtual = offsetAtual;
    }

    @Override
    public String getName() {
        return "gerar_contexto_enraizado";
    }

    /**
 * Descrição não fornecida.
 *
 * @author Renato Tomaz Nati
 */
    @Override
    public String execute(String jsonParameters) {
        int profundidade = ToolJsonSupport.extractJsonIntValue(jsonParameters, "profundidade", 1, 0, 5);

        if (compilationUnitAtual == null) {
            return "Erro Operacional: Nao ha unidade JDT ativa para gerar o contexto enraizado.";
        }

        return contextOrchestrator.enraizarChamadas(compilationUnitAtual, offsetAtual, profundidade);
    }
    @Override
    public AgentToolPromptMetadata getPromptMetadata() {
        AgentToolPromptMetadata metadata = new AgentToolPromptMetadata();
        metadata.setToolName(getName());
        metadata.setOneLinePurpose("Gerar breadcrumb estrutural do ponto atual do editor.");
        metadata.setActivityDescription("Exibe a trilha estrutural do ponto atual na IDE.");

        AgentToolParameterMetadata profundidade = new AgentToolParameterMetadata();
        profundidade.setName("profundidade");
        profundidade.setRequired(false);
        profundidade.setDescription("Profundidade maxima do breadcrumb estrutural.");
        profundidade.setExampleValue("2");
        metadata.addParameter(profundidade);

        metadata.addRecommendedUseCase("Use quando o alvo atual ja estiver no editor e a IA precisar do breadcrumb estrutural.");
        metadata.addRecommendedUseCase("Use para contextualizar rapidamente o ponto atual sem informar classe e metodo manualmente.");
        metadata.addRecommendedUseCase("Use antes de outras ferramentas que dependem do contexto do editor.");

        metadata.addGuardrail("Depende de compilation unit ativa no editor.");
        metadata.addGuardrail("Nao substitui busca por nome quando o alvo nao estiver aberto.");
        metadata.addGuardrail("Mantenha a profundidade controlada para evitar ruido.");

        metadata.addJsonExample(
                "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"gerar_contexto_enraizado\\\",\\\"parameters\\\":{\\\"profundidade\\\":\\\"2\\\"},\\\"explanation\\\":\\\"Preciso obter a trilha estrutural do ponto atual do editor antes de continuar.\\\"}"
        );

        return metadata;
    }
}