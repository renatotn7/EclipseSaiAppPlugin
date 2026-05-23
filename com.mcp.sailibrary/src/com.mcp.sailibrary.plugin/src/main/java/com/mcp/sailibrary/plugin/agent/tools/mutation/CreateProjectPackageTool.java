package com.mcp.sailibrary.plugin.agent.tools.mutation;

import java.io.File;
import java.security.MessageDigest;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.context.mutation.WorkspaceMutationFacade;
import com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationContext;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolParameterMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadataProvider;
import com.mcp.sailibrary.plugin.agent.tools.support.ResolvedStructuralTarget;
import com.mcp.sailibrary.plugin.agent.tools.support.StructuralTargetResolver;
import com.mcp.sailibrary.plugin.agent.tools.support.ToolJsonSupport;
import com.mcp.sailibrary.plugin.chat.context.service.SafeWorkspaceMutationPolicy;

/** * Cria nova package ou pasta dentro de um contexto estrutural editavel. * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class CreateProjectPackageTool implements AgentTool, AgentToolPromptMetadataProvider {

    private final File rootDirectory;
    private final SafeWorkspaceMutationPolicy mutationPolicy;
    private final WorkspaceMutationFacade workspaceMutationFacade;
    private final StructuralTargetResolver structuralTargetResolver;

    /** * Inicializa a ferramenta com raiz segura, politica de mutacao e resolvedor * de aliases estruturais. * * @param rootDirectory raiz segura do projeto * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public CreateProjectPackageTool(File rootDirectory) {
    	  this.rootDirectory = rootDirectory;
    	    this.mutationPolicy = new SafeWorkspaceMutationPolicy(rootDirectory);
    	    this.workspaceMutationFacade = new WorkspaceMutationFacade(
    	            rootDirectory,
    	            gerarProjectKey(rootDirectory)
    	    );
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
        String instructionSummary = ToolJsonSupport.extractJsonStringValue(jsonParameters, "instructionSummary");

        if (isBlank(target)) {
            return "Erro Operacional: O parametro target e obrigatorio.";
        }

        if (isBlank(relativePath)) {
            return "Erro Operacional: O parametro relativePath e obrigatorio.";
        }

        if (rootDirectory == null || !rootDirectory.exists() || !rootDirectory.isDirectory()) {
            return "Erro Operacional: A raiz segura do projeto esta indisponivel para criacao de package/pasta.";
        }

        ResolvedStructuralTarget resolvedTarget = structuralTargetResolver.resolveTarget(target);
        if (resolvedTarget == null || !resolvedTarget.isUsable()) {
            return "Erro Operacional: O alvo estrutural informado nao foi encontrado ou nao pode ser resolvido para um destino real do workspace.";
        }

        MutationContext context = new MutationContext();
        context.setProjectRootDirectory(rootDirectory);
        context.setProjectKey(gerarProjectKey(rootDirectory));
        context.setBranchName(detectarBranchAtual(rootDirectory));
        context.setToolName(getName());
        context.setInstructionSummary(
                !isBlank(instructionSummary)
                        ? instructionSummary
                        : "Criacao de package/pasta pela tool criar_package_projeto."
        );
        context.setTargetName(resolvedTarget.getContext().getName());
        context.setTargetAbsoluteBasePath(resolvedTarget.getBaseDirectory().getAbsolutePath().replace("\\", "/"));
        context.setTargetRelativeBasePath(resolvedTarget.getRelativeBasePathFromOwningProject());
        context.setTargetOwningProjectRootPath(resolvedTarget.getOwningEclipseProjectRoot().getAbsolutePath().replace("\\", "/"));
        context.setTargetOwningProjectName(resolvedTarget.getOwningEclipseProjectName());
        context.setTargetMirrorBaseRelativePath(resolvedTarget.getMirrorRelativeBasePath());
        context.setOrigin(com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationOrigin.AI);

        try {
            workspaceMutationFacade.initializeInfrastructure();
            return workspaceMutationFacade.applyCreatePackage(context, relativePath);
        } catch (Exception e) {
            return "Falha ao criar package/pasta no projeto: " + e.getMessage();
        }
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
    private String gerarProjectKey(File rootDirectory) {
        if (rootDirectory == null) {
            return "unknown_project";
        }

        try {
            String canonicalPath = rootDirectory.getCanonicalPath();
            String baseName = rootDirectory.getName();
            String hash = gerarHashCurto(canonicalPath);
            return normalizarNome(baseName) + "_" + hash;
        } catch (Exception e) {
            return normalizarNome(rootDirectory.getName()) + "_fallback";
        }
    }
    private String gerarHashCurto(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] digest = messageDigest.digest(value.getBytes("UTF-8"));

            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < digest.length && builder.length() < 8; i++) {
                String hex = Integer.toHexString(digest[i] & 0xff);
                if (hex.length() == 1) {
                    builder.append("0");
                }
                builder.append(hex);
            }

            return builder.toString();
        } catch (Exception e) {
            return "hashfail";
        }
    }
    private String normalizarNome(String name) {
        if (name == null || name.trim().length() == 0) {
            return "project";
        }

        String normalized = name.toLowerCase();
        normalized = normalized.replaceAll("[^a-z0-9_\\-]", "_");
        return normalized;
    }
    private String detectarBranchAtual(File projectRoot) {
        if (projectRoot == null) {
            return "";
        }

        try {
            File gitHead = new File(projectRoot, ".git/HEAD");
            if (!gitHead.exists() || !gitHead.isFile()) {
                return "";
            }

            java.util.List<String> lines = java.nio.file.Files.readAllLines(
                    gitHead.toPath(),
                    java.nio.charset.StandardCharsets.UTF_8
            );

            if (lines.isEmpty()) {
                return "";
            }

            String line = lines.get(0) != null ? lines.get(0).trim() : "";
            String prefix = "ref: refs/heads/";
            if (line.startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }

            return line;
        } catch (Exception e) {
            return "";
        }
    }
    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}