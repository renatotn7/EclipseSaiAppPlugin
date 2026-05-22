package com.mcp.sailibrary.plugin.agent.tools.mutation;

import java.io.File;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.tools.support.ToolJsonSupport;
import com.mcp.sailibrary.plugin.chat.context.service.SafeWorkspaceMutationPolicy;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolParameterMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadataProvider;
/* yaml_header: version: "1.0" purpose: "Criar nova package ou pasta dentro de um contexto estrutural editavel." libraries: - java.io.File: runtime */
public class CreateProjectPackageTool implements AgentTool, AgentToolPromptMetadataProvider {

    private final File rootDirectory;
    private final SafeWorkspaceMutationPolicy mutationPolicy;

    public CreateProjectPackageTool(File rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.mutationPolicy = new SafeWorkspaceMutationPolicy(rootDirectory);
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

        File baseDirectory = resolveBaseDirectory(target);
        if (baseDirectory == null) {
            return "Erro Operacional: Nao foi possivel resolver o diretorio base do alvo estrutural.";
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

    private File resolveBaseDirectory(String targetName) {
        java.util.List<com.mcp.sailibrary.plugin.chat.context.model.NamedStructuralContext> contexts =
                com.mcp.sailibrary.plugin.chat.context.service.NamedStructuralContextSessionService.getInstance().getAll();

        for (int i = 0; i < contexts.size(); i++) {
            com.mcp.sailibrary.plugin.chat.context.model.NamedStructuralContext context = contexts.get(i);
            if (context != null && targetName != null && targetName.equals(context.getName())) {
                if (context.getFilePath() != null && context.getFilePath().trim().length() > 0) {
                    File file = new File(context.getFilePath());
                    if (file.exists() && file.isDirectory()) {
                        return file;
                    }
                }
                if (context.getRelativePath() != null && context.getRelativePath().trim().length() > 0) {
                    File file = new File(rootDirectory, context.getRelativePath());
                    if (file.exists() && file.isDirectory()) {
                        return file;
                    }
                }
            }
        }

        return null;
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
    private String normalizePath(File file) {
        try {
            return file.getCanonicalPath().replace("\\", "/");
        } catch (Exception e) {
            return file.getAbsolutePath().replace("\\", "/");
        }
    }
}