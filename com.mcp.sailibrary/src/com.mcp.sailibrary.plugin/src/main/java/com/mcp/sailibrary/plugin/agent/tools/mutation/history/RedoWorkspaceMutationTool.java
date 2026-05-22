package com.mcp.sailibrary.plugin.agent.tools.mutation.history;

import java.io.File;
import java.security.MessageDigest;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.context.mutation.WorkspaceMutationFacade;
import com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationContext;
import com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationOrigin;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadataProvider;
import com.mcp.sailibrary.plugin.agent.tools.support.ToolJsonSupport;

/** * Refaz o ultimo batch de mutacao registrado na infraestrutura versionada * interna do plugin. * * <p>Esta implementacao preserva o contrato simples de tool autonoma e delega * a logica efetiva de reaplicacao para a {@link WorkspaceMutationFacade}. A * primeira versao opera sobre o ultimo batch disponivel na pilha de redo, * evitando comportamento ambiguo enquanto o suporte a redo seletivo por path * ou batch especifico ainda nao foi expandido na fachada.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class RedoWorkspaceMutationTool implements AgentTool, AgentToolPromptMetadataProvider {

    private final File rootDirectory;
    private final WorkspaceMutationFacade workspaceMutationFacade;

    /** * Inicializa a tool de redo de mutacao do workspace. * * @param rootDirectory raiz segura do projeto atual * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public RedoWorkspaceMutationTool(File rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.workspaceMutationFacade = new WorkspaceMutationFacade(
                rootDirectory,
                gerarProjectKey(rootDirectory)
        );
    }

    @Override
    public String getName() {
        return "refazer_mutacao_workspace";
    }

    @Override
    public String execute(String jsonParameters) {
        String instructionSummary = ToolJsonSupport.extractJsonStringValue(jsonParameters, "instructionSummary");

        if (rootDirectory == null || !rootDirectory.exists() || !rootDirectory.isDirectory()) {
            return "Erro Operacional: A raiz segura do projeto esta indisponivel para redo de mutacao.";
        }

        MutationContext context = new MutationContext();
        context.setProjectRootDirectory(rootDirectory);
        context.setProjectKey(gerarProjectKey(rootDirectory));
        context.setBranchName(detectarBranchAtual(rootDirectory));
        context.setToolName(getName());
        context.setInstructionSummary(
                !isBlank(instructionSummary)
                        ? instructionSummary
                        : "Redo do ultimo batch pela tool refazer_mutacao_workspace."
        );
        context.setTargetName("workspace");
        context.setOrigin(MutationOrigin.AI);

        try {
            workspaceMutationFacade.initializeInfrastructure();
            return workspaceMutationFacade.redoLastBatch(context);
        } catch (Exception e) {
            return "Falha ao refazer mutacao do workspace: " + e.getMessage();
        }
    }

    @Override
    public AgentToolPromptMetadata getPromptMetadata() {
        AgentToolPromptMetadata metadata = new AgentToolPromptMetadata();
        metadata.setToolName(getName());
        metadata.setOneLinePurpose("Refazer o ultimo batch de mutacao disponivel na pilha de redo.");
        metadata.setActivityDescription("Refaz o ultimo batch de mutacao disponivel na pilha de redo do workspace.");

        metadata.addRecommendedUseCase("Use quando houver redo disponivel e a alteracao desfeita precisar ser reaplicada.");
        metadata.addRecommendedUseCase("Use apos validar o estado da mutacao e o historico recente.");
        metadata.addRecommendedUseCase("Use quando a IA concluir que o undo anterior nao deveria ter sido mantido.");

        metadata.addGuardrail("Nao use se a pilha de redo estiver vazia.");
        metadata.addGuardrail("A ferramenta atua sobre o ultimo batch, nao sobre arquivo arbitrario.");
        metadata.addGuardrail("Consulte estado e historico antes de refazer mudancas sensiveis.");

        metadata.addJsonExample(
                "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"refazer_mutacao_workspace\\\",\\\"parameters\\\":{},\\\"explanation\\\":\\\"Preciso refazer o ultimo batch desfeito apos validar que o redo e desejado.\\\"}"
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