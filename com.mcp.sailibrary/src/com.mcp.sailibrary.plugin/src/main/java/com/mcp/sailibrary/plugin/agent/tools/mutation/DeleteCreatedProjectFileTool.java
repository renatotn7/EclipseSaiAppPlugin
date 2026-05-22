package com.mcp.sailibrary.plugin.agent.tools.mutation;

import java.io.File;
import java.security.MessageDigest;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.context.mutation.WorkspaceMutationFacade;
import com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationContext;
import com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationOrigin;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolParameterMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadataProvider;
import com.mcp.sailibrary.plugin.agent.tools.support.ToolJsonSupport;

/** * Apaga arquivo previamente permitido pela politica de mutacao, registrando a * operacao na infraestrutura versionada interna do plugin. * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class DeleteCreatedProjectFileTool implements AgentTool, AgentToolPromptMetadataProvider {

    private final File rootDirectory;
    private final WorkspaceMutationFacade workspaceMutationFacade;

    /** * Inicializa a tool de remocao de arquivo com suporte a journal e * versionamento interno. * * @param rootDirectory raiz segura do projeto atual * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public DeleteCreatedProjectFileTool(File rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.workspaceMutationFacade = new WorkspaceMutationFacade(
                rootDirectory,
                gerarProjectKey(rootDirectory)
        );
    }

    @Override
    public String getName() {
        return "apagar_arquivo_criado";
    }

    @Override
    public String execute(String jsonParameters) {
        String relativePath = ToolJsonSupport.extractJsonStringValue(jsonParameters, "path");
        String target = ToolJsonSupport.extractJsonStringValue(jsonParameters, "target");
        String instructionSummary = ToolJsonSupport.extractJsonStringValue(jsonParameters, "instructionSummary");

        if (isBlank(relativePath)) {
            return "Erro Operacional: O parametro path e obrigatorio.";
        }

        if (rootDirectory == null || !rootDirectory.exists() || !rootDirectory.isDirectory()) {
            return "Erro Operacional: A raiz segura do projeto esta indisponivel para remocao de arquivo.";
        }

        MutationContext context = new MutationContext();
        context.setProjectRootDirectory(rootDirectory);
        context.setProjectKey(gerarProjectKey(rootDirectory));
        context.setBranchName(detectarBranchAtual(rootDirectory));
        context.setToolName(getName());
        context.setInstructionSummary(
                !isBlank(instructionSummary)
                        ? instructionSummary
                        : "Remocao de arquivo pela tool apagar_arquivo_criado."
        );
        context.setTargetName(!isBlank(target) ? target : "arquivo");
        context.setOrigin(MutationOrigin.AI);

        try {
            workspaceMutationFacade.initializeInfrastructure();
            return workspaceMutationFacade.applyDeleteCreatedFile(context, relativePath);
        } catch (Exception e) {
            return "Falha ao apagar arquivo no projeto: " + e.getMessage();
        }
    }

    @Override
    public AgentToolPromptMetadata getPromptMetadata() {
        AgentToolPromptMetadata metadata = new AgentToolPromptMetadata();
        metadata.setToolName(getName());
        metadata.setOneLinePurpose("Apagar arquivo permitido pela politica de mutacao do workspace.");
        metadata.setActivityDescription("Apaga somente arquivos previamente criados pela propria IA/plugin e registrados como tais.");

        AgentToolParameterMetadata path = new AgentToolParameterMetadata();
        path.setName("path");
        path.setRequired(true);
        path.setDescription("Caminho relativo do arquivo a ser apagado.");
        path.setExampleValue("src/main/java/com/exemplo/service/NovaRegraService.java");
        metadata.addParameter(path);

        AgentToolParameterMetadata target = new AgentToolParameterMetadata();
        target.setName("target");
        target.setRequired(false);
        target.setDescription("Nome logico opcional do alvo estrutural associado.");
        target.setExampleValue("service");
        metadata.addParameter(target);

        AgentToolParameterMetadata instructionSummary = new AgentToolParameterMetadata();
        instructionSummary.setName("instructionSummary");
        instructionSummary.setRequired(false);
        instructionSummary.setDescription("Resumo opcional da instrucao de origem.");
        instructionSummary.setExampleValue("Remocao de arquivo criado anteriormente.");
        metadata.addParameter(instructionSummary);

        metadata.addRecommendedUseCase("Use quando o arquivo foi criado pela propria IA/plugin e precisa ser removido.");
        metadata.addRecommendedUseCase("Use para rollback cirurgico de artefato novo.");
        metadata.addRecommendedUseCase("Use quando a politica permitir remocao segura do arquivo.");

        metadata.addGuardrail("Nao use esta ferramenta para apagar arquivo preexistente do projeto.");
        metadata.addGuardrail("O path deve ser resolvido com seguranca dentro da raiz do projeto.");
        metadata.addGuardrail("A remocao depende da politica de mutacao do workspace.");

        metadata.addJsonExample(
                "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"apagar_arquivo_criado\\\",\\\"parameters\\\":{\\\"path\\\":\\\"src/main/java/com/exemplo/service/NovaRegraService.java\\\"},\\\"explanation\\\":\\\"Preciso apagar apenas um arquivo previamente criado pela propria IA/plugin.\\\"}"
        );

        return metadata;
    }

    /** * Gera a chave estavel do projeto com base na raiz fisica informada. * * @param rootDirectory raiz fisica do projeto * @return chave estavel do projeto * * @author Renato Tomaz Nati * @since 2026-05-20 */
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

    /** * Gera hash curto deterministico para a raiz canonica do projeto. * * @param value valor base para hash * @return hash curto em hexadecimal * * @author Renato Tomaz Nati * @since 2026-05-20 */
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

    /** * Normaliza nome de projeto para uso seguro em identificadores internos. * * @param name nome base do projeto * @return nome normalizado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String normalizarNome(String name) {
        if (name == null || name.trim().length() == 0) {
            return "project";
        }

        String normalized = name.toLowerCase();
        normalized = normalized.replaceAll("[^a-z0-9_\\-]", "_");
        return normalized;
    }

    /** * Detecta a branch atual do projeto a partir do arquivo .git/HEAD. * * @param projectRoot raiz fisica do projeto * @return nome da branch atual ou string vazia * * @author Renato Tomaz Nati * @since 2026-05-20 */
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

    /** * Retorna true quando o valor informado for nulo ou vazio. * * @param value valor a validar * @return true quando o valor estiver em branco * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}