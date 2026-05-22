package com.mcp.sailibrary.plugin.agent.tools.mutation;

import java.io.File;
import java.security.MessageDigest;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.context.mutation.WorkspaceMutationFacade;
import com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationContext;
import com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationOrigin;
import com.mcp.sailibrary.plugin.agent.tools.support.ToolJsonSupport;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolParameterMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadataProvider;
/** * Altera arquivo existente permitido, criando backup .bkp antes da gravacao e * registrando a mutacao na infraestrutura versionada interna do plugin. * * <p>Esta implementacao preserva o contrato funcional ja existente da tool, * incluindo validacao do parametro path e alteracao de arquivo dentro do * perimetro seguro do projeto. A diferenca principal e que agora a escrita * passa pela {@link WorkspaceMutationFacade}, evitando perda de rastreabilidade * e preparando o caminho para undo e redo sem regressao do fluxo atual.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class UpdateProjectFileWithBackupTool implements AgentTool, AgentToolPromptMetadataProvider {

    private final File rootDirectory;
    private final WorkspaceMutationFacade workspaceMutationFacade;

    /** * Inicializa a tool de alteracao de arquivo com suporte a backup e journal * de mutacao. * * @param rootDirectory raiz segura do projeto atual * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public UpdateProjectFileWithBackupTool(File rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.workspaceMutationFacade = new WorkspaceMutationFacade(
                rootDirectory,
                gerarProjectKey(rootDirectory)
        );
    }

    @Override
    public String getName() {
        return "alterar_arquivo_com_backup";
    }

    @Override
    public String execute(String jsonParameters) {
        String relativePath = ToolJsonSupport.extractJsonStringValue(jsonParameters, "path");
        String content = ToolJsonSupport.extractJsonStringValue(jsonParameters, "content");
        String target = ToolJsonSupport.extractJsonStringValue(jsonParameters, "target");
        String instructionSummary = ToolJsonSupport.extractJsonStringValue(jsonParameters, "instructionSummary");

        if (relativePath == null || relativePath.trim().length() == 0) {
            return "Erro Operacional: O parametro path e obrigatorio.";
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
            return workspaceMutationFacade.applyUpdateFile(context, relativePath, content);
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
        path.setRequired(true);
        path.setDescription("Caminho relativo do arquivo existente a ser alterado.");
        path.setExampleValue("src/main/resources/regras.xml");
        metadata.addParameter(path);

        AgentToolParameterMetadata content = new AgentToolParameterMetadata();
        content.setName("content");
        content.setRequired(true);
        content.setDescription("Novo conteudo textual completo a ser gravado no arquivo.");
        content.setExampleValue("<regras>...</regras>");
        metadata.addParameter(content);

        AgentToolParameterMetadata target = new AgentToolParameterMetadata();
        target.setName("target");
        target.setRequired(false);
        target.setDescription("Nome logico opcional do alvo estrutural associado a alteracao.");
        target.setExampleValue("config");
        metadata.addParameter(target);

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
    /** * Gera a chave estavel do projeto com base na raiz fisica informada. * * <p>O formato segue a mesma estrategia defensiva usada pela memoria * persistente do projeto, preservando nome base normalizado e hash curto * da raiz canonica.</p> * * @param rootDirectory raiz fisica do projeto * @return chave estavel do projeto * * @author Renato Tomaz Nati * @since 2026-05-20 */
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

    /** * Detecta a branch atual do projeto a partir do arquivo .git/HEAD, quando * disponivel. * * @param projectRoot raiz fisica do projeto * @return nome da branch atual ou string vazia * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String detectarBranchAtual(File projectRoot) {
        if (projectRoot == null) {
            return "";
        }

        try {
            File gitHead = new File(projectRoot, ".git/HEAD");
            if (!gitHead.exists() || !gitHead.isFile()) {
                return "";
            }

            java.util.List<String> lines = java.nio.file.Files.readAllLines(gitHead.toPath(), java.nio.charset.StandardCharsets.UTF_8);
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

    /** * Retorna true quando o valor informado for nulo ou vazio. * * @param value valor a ser validado * @return true quando o valor estiver em branco * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}