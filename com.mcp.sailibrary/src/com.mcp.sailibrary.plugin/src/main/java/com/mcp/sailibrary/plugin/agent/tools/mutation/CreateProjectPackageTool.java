package com.mcp.sailibrary.plugin.agent.tools.mutation;

import java.io.File;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolParameterMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadataProvider;
import com.mcp.sailibrary.plugin.agent.tools.support.StructuralTargetResolver;
import com.mcp.sailibrary.plugin.agent.tools.support.ToolJsonSupport;
import com.mcp.sailibrary.plugin.chat.context.service.SafeWorkspaceMutationPolicy;

/** * Cria nova package ou pasta dentro de um contexto estrutural editavel. * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class CreateProjectPackageTool implements AgentTool, AgentToolPromptMetadataProvider {

    private final File rootDirectory;
    private final SafeWorkspaceMutationPolicy mutationPolicy;
    private final StructuralTargetResolver structuralTargetResolver;

    /** * Inicializa a ferramenta com raiz segura, politica de mutacao e resolvedor * de aliases estruturais. * * @param rootDirectory raiz segura do projeto * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public CreateProjectPackageTool(File rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.mutationPolicy = new SafeWorkspaceMutationPolicy(rootDirectory);
        this.structuralTargetResolver = new StructuralTargetResolver(rootDirectory);
    }

    @Override
    public String getName() {
        return "criar_package_projeto";
    }

    @Override
    public String execute(String jsonParameters) {
        String target = ToolJsonSupport.extractJsonStringValue(jsonParameters, "target");
        String relativePath = ToolJsonSupport.extractJsonStringValue(jsonParameters, "relativePath");

        if (!mutationPolicy.canCreatePackage(target, relativePath)) {
            return "Erro Operacional: A politica de mutacao nao permite criar package/pasta neste alvo.";
        }

        File baseDirectory = structuralTargetResolver.resolveBaseDirectory(target);
        if (baseDirectory == null) {
            return "Erro Operacional: Nao foi possivel resolver o diretorio base real do alvo estrutural.";
        }

        File targetDirectory = new File(baseDirectory, relativePath);

        if (targetDirectory.exists()) {
            return "Erro Operacional: A package ou pasta alvo ja existe.";
        }

        boolean created = targetDirectory.mkdirs();
        if (!created) {
            return "Falha ao criar package/pasta no projeto.";
        }

        return "Package/pasta criada com sucesso em: " + normalizePath(targetDirectory);
    }

    @Override
    public AgentToolPromptMetadata getPromptMetadata() {
        AgentToolPromptMetadata metadata = new AgentToolPromptMetadata();
        metadata.setToolName(getName());
        metadata.setOneLinePurpose("Criar nova package ou pasta dentro de contexto estrutural editavel.");
        metadata.setActivityDescription("Cria nova package ou pasta dentro de um contexto estrutural editavel permitido.");

        AgentToolParameterMetadata target = new AgentToolParameterMetadata();
        target.setName("target");
        target.setRequired(true);
        target.setDescription("Nome do contexto estrutural editavel de destino.");
        target.setExampleValue("service");
        metadata.addParameter(target);

        AgentToolParameterMetadata relativePath = new AgentToolParameterMetadata();
        relativePath.setName("relativePath");
        relativePath.setRequired(true);
        relativePath.setDescription("Caminho relativo da package ou pasta a ser criada.");
        relativePath.setExampleValue("interno/auxiliar");
        metadata.addParameter(relativePath);

        metadata.addRecommendedUseCase("Use quando a IA precisar criar nova package ou pasta dentro de destino editavel.");
        metadata.addRecommendedUseCase("Use para preparar estrutura antes de criar novos arquivos.");
        metadata.addRecommendedUseCase("Use quando a politica permitir criacao estrutural no contexto alvo.");

        metadata.addGuardrail("O target deve apontar para contexto estrutural editavel valido.");
        metadata.addGuardrail("Nao use esta ferramenta para alterar arquivo existente.");
        metadata.addGuardrail("A criacao deve respeitar a politica de mutacao do contexto.");

        metadata.addJsonExample(
                "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"criar_package_projeto\\\",\\\"parameters\\\":{\\\"target\\\":\\\"service\\\",\\\"relativePath\\\":\\\"interno/auxiliar\\\"},\\\"explanation\\\":\\\"Preciso criar nova package dentro de contexto estrutural editavel permitido.\\\"}"
        );

        return metadata;
    }

    /** * Normaliza caminho fisico para formato com barras normais. * * @param file arquivo de origem * @return caminho normalizado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String normalizePath(File file) {
        try {
            return file.getCanonicalPath().replace("\\", "/");
        } catch (Exception e) {
            return file.getAbsolutePath().replace("\\", "/");
        }
    }
}