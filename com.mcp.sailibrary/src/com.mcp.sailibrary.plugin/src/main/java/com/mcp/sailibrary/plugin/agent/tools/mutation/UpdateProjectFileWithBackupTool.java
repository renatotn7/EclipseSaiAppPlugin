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
import com.mcp.sailibrary.plugin.agent.tools.support.StructuralTargetResolver;
import com.mcp.sailibrary.plugin.agent.tools.support.ToolJsonSupport;

/** * Altera arquivo existente permitido, criando backup .bkp antes da gravacao e * registrando a mutacao na infraestrutura versionada interna do plugin. * * <p>Esta implementacao preserva o contrato funcional ja existente da tool, * incluindo validacao do parametro path e alteracao de arquivo dentro do * perimetro seguro do projeto. Alem disso, passa a aceitar resolucao por alias * estrutural via target + relativePath quando o path completo nao vier * explicitamente.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class UpdateProjectFileWithBackupTool implements AgentTool, AgentToolPromptMetadataProvider {

    private final File rootDirectory;
    private final WorkspaceMutationFacade workspaceMutationFacade;
    private final StructuralTargetResolver structuralTargetResolver;

    /** * Inicializa a tool de alteracao de arquivo com suporte a backup, journal * de mutacao e resolucao de aliases estruturais. * * @param rootDirectory raiz segura do projeto atual * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public UpdateProjectFileWithBackupTool(File rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.workspaceMutationFacade = new WorkspaceMutationFacade(
                rootDirectory,
                gerarProjectKey(rootDirectory)
        );
        this.structuralTargetResolver = new StructuralTargetResolver(rootDirectory);
    }

    @Override
    public String getName() {
        return "alterar_arquivo_com_backup";
    }

    @Override
    public String execute(String jsonParameters) {
        String path = ToolJsonSupport.extractJsonStringValue(jsonParameters, "path");
        String relativePath = ToolJsonSupport.extractJsonStringValue(jsonParameters, "relativePath");
        String content = ToolJsonSupport.extractJsonStringValue(jsonParameters, "content");
        String target = ToolJsonSupport.extractJsonStringValue(jsonParameters, "target");
        String instructionSummary = ToolJsonSupport.extractJsonStringValue(jsonParameters, "instructionSummary");

        String resolvedRelativePath = resolveEffectiveRelativePath(path, target, relativePath);
        if (isBlank(resolvedRelativePath)) {
            return "Erro Operacional: E necessario informar 'path' ou combinar 'target' com 'relativePath' para alterar o arquivo.";
        }

        if (rootDirectory == null || !rootDirectory.exists() || !rootDirectory.isDirectory()) {
            return "Erro Operacional: A raiz segura do projeto esta indisponivel para alteracao.";
        }

        MutationContext context = new MutationContext();
        context.setProjectRootDirectory(rootDirectory);
        context.setProjectKey(gerarProjectKey(rootDirectory));
        context.setBranchName(detectarBranchAtual(rootDirectory));
        context.setToolName(getName());
        context.setInstructionSummary(
                !isBlank(instructionSummary)
                        ? instructionSummary
                        : "Alteracao de arquivo existente com backup pela tool alterar_arquivo_com_backup."
        );
        context.setTargetName(!isBlank(target) ? target : "arquivo");
        context.setOrigin(MutationOrigin.AI);

        try {
            workspaceMutationFacade.initializeInfrastructure();
            return workspaceMutationFacade.applyUpdateFile(context, resolvedRelativePath, content);
        } catch (Exception e) {
            return "Falha ao alterar arquivo com backup: " + e.getMessage();
        }
    }

    @Override
    public AgentToolPromptMetadata getPromptMetadata() {
        AgentToolPromptMetadata metadata = new AgentToolPromptMetadata();
        metadata.setToolName(getName());
        metadata.setOneLinePurpose("Alterar arquivo existente permitido com backup previo.");
        metadata.setActivityDescription("Altera arquivo existente permitido gerando backup .bkp antes da gravacao.");

        AgentToolParameterMetadata path = new AgentToolParameterMetadata();
        path.setName("path");
        path.setRequired(false);
        path.setDescription("Caminho relativo do arquivo existente a ser alterado.");
        path.setExampleValue("src/main/resources/regras.xml");
        metadata.addParameter(path);

        AgentToolParameterMetadata target = new AgentToolParameterMetadata();
        target.setName("target");
        target.setRequired(false);
        target.setDescription("Nome logico opcional do alvo estrutural associado a alteracao.");
        target.setExampleValue("config");
        metadata.addParameter(target);

        AgentToolParameterMetadata relativePath = new AgentToolParameterMetadata();
        relativePath.setName("relativePath");
        relativePath.setRequired(false);
        relativePath.setDescription("Caminho relativo dentro do contexto estrutural quando path completo nao for informado.");
        relativePath.setExampleValue("regras.xml");
        metadata.addParameter(relativePath);

        AgentToolParameterMetadata content = new AgentToolParameterMetadata();
        content.setName("content");
        content.setRequired(true);
        content.setDescription("Novo conteudo textual completo a ser gravado no arquivo.");
        content.setExampleValue("<regras>...</regras>");
        metadata.addParameter(content);

        AgentToolParameterMetadata instructionSummary = new AgentToolParameterMetadata();
        instructionSummary.setName("instructionSummary");
        instructionSummary.setRequired(false);
        instructionSummary.setDescription("Resumo opcional da instrucao que motivou a alteracao.");
        instructionSummary.setExampleValue("Atualizacao controlada de arquivo existente com backup.");
        metadata.addParameter(instructionSummary);

        metadata.addRecommendedUseCase("Use quando a politica de mutacao permitir alterar um arquivo existente.");
        metadata.addRecommendedUseCase("Use quando a IA precisar modificar arquivo real sem perder rastreabilidade.");
        metadata.addRecommendedUseCase("Use quando backup .bkp for obrigatorio antes da gravacao.");

        metadata.addGuardrail("Nao use esta ferramenta para criar arquivo novo.");
        metadata.addGuardrail("Nao use esta ferramenta para apagar arquivo.");
        metadata.addGuardrail("A alteracao deve respeitar a politica de mutacao e o perimetro seguro do projeto.");
        metadata.addGuardrail("O conteudo enviado deve representar o estado final completo do arquivo alvo.");

        metadata.addJsonExample(
                "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"alterar_arquivo_com_backup\\\",\\\"parameters\\\":{\\\"path\\\":\\\"src/main/resources/regras.xml\\\",\\\"content\\\":\\\"<regras>...</regras>\\\"},\\\"explanation\\\":\\\"Preciso alterar arquivo existente permitido criando backup .bkp antes da gravacao.\\\"}"
        );

        return metadata;
    }

    /** * Resolve o caminho relativo efetivo do arquivo a ser alterado. * * <p>A prioridade e: * <ol> * <li>path explicito</li> * <li>target estrutural + relativePath</li> * </ol> * </p> * * @param path caminho relativo completo * @param target alias estrutural * @param relativePath caminho relativo dentro do alias estrutural * @return caminho relativo efetivo ou string vazia * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String resolveEffectiveRelativePath(String path, String target, String relativePath) {
        if (!isBlank(path)) {
            return normalizeRelativePath(path);
        }

        if (isBlank(target) || isBlank(relativePath)) {
            return "";
        }

        String baseRelativePath = structuralTargetResolver.resolveRelativePath(target);
        return joinRelativePath(baseRelativePath, relativePath);
    }

    /** * Une caminho relativo base e caminho relativo filho em um unico path * normalizado. * * @param baseRelativePath base relativa * @param childRelativePath filho relativo * @return caminho relativo final * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String joinRelativePath(String baseRelativePath, String childRelativePath) {
        String base = baseRelativePath != null ? baseRelativePath.trim().replace("\\", "/") : "";
        String child = childRelativePath != null ? childRelativePath.trim().replace("\\", "/") : "";

        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        while (child.startsWith("/")) {
            child = child.substring(1);
        }

        if (base.length() == 0) {
            return child;
        }
        if (child.length() == 0) {
            return base;
        }

        return base + "/" + child;
    }

    /** * Normaliza caminho relativo para formato com barras normais. * * @param value caminho original * @return caminho normalizado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String normalizeRelativePath(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim().replace("\\", "/");
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
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