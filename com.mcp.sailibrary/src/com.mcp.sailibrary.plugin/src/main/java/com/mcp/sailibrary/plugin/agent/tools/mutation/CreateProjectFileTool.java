package com.mcp.sailibrary.plugin.agent.tools.mutation;

import java.io.File;
import java.security.MessageDigest;
import java.util.List;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.context.mutation.WorkspaceMutationFacade;
import com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationContext;
import com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationOrigin;
import com.mcp.sailibrary.plugin.agent.tools.support.ToolJsonSupport;
import com.mcp.sailibrary.plugin.chat.context.model.NamedStructuralContext;
import com.mcp.sailibrary.plugin.chat.context.service.NamedStructuralContextSessionService;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolParameterMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadataProvider;
/** * Cria novo arquivo dentro de um contexto estrutural editavel permitido, * registrando a mutacao na infraestrutura versionada interna do plugin. * * <p>Esta implementacao preserva o contrato funcional da tool, incluindo o uso * de target, relativePath e content como parametros principais. A criacao * passa agora pela {@link WorkspaceMutationFacade}, reduzindo duplicacao de * logica e preparando o caminho para undo e redo sem regressao do * comportamento existente.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class CreateProjectFileTool implements AgentTool, AgentToolPromptMetadataProvider {

    private final File rootDirectory;
    private final WorkspaceMutationFacade workspaceMutationFacade;

    /** * Inicializa a tool de criacao de arquivo com suporte a journal e * versionamento interno. * * @param rootDirectory raiz segura do projeto atual * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public CreateProjectFileTool(File rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.workspaceMutationFacade = new WorkspaceMutationFacade(
                rootDirectory,
                gerarProjectKey(rootDirectory)
        );
    }

    @Override
    public String getName() {
        return "criar_arquivo_projeto";
    }

    @Override
    public String execute(String jsonParameters) {
        String target = ToolJsonSupport.extractJsonStringValue(jsonParameters, "target");
        String relativePath = ToolJsonSupport.extractJsonStringValue(jsonParameters, "relativePath");
        String content = ToolJsonSupport.extractJsonStringValue(jsonParameters, "content");
        String instructionSummary = ToolJsonSupport.extractJsonStringValue(jsonParameters, "instructionSummary");

        if (isBlank(target)) {
            return "Erro Operacional: O parametro target e obrigatorio.";
        }

        if (isBlank(relativePath)) {
            return "Erro Operacional: O parametro relativePath e obrigatorio.";
        }

        if (rootDirectory == null || !rootDirectory.exists() || !rootDirectory.isDirectory()) {
            return "Erro Operacional: A raiz segura do projeto esta indisponivel para criacao de arquivo.";
        }

        NamedStructuralContext targetContext = resolveTargetContext(target);
        if (targetContext == null) {
            return "Erro Operacional: O alvo estrutural informado nao foi encontrado na sessao atual.";
        }

        MutationContext context = new MutationContext();
        context.setProjectRootDirectory(rootDirectory);
        context.setProjectKey(gerarProjectKey(rootDirectory));
        context.setBranchName(detectarBranchAtual(rootDirectory));
        context.setToolName(getName());
        context.setInstructionSummary(
                !isBlank(instructionSummary)
                        ? instructionSummary
                        : "Criacao de novo arquivo pela tool criar_arquivo_projeto."
        );
        context.setTargetName(targetContext.getName());
        context.setOrigin(MutationOrigin.AI);

        try {
            workspaceMutationFacade.initializeInfrastructure();
            return workspaceMutationFacade.applyCreateFile(context, relativePath, content);
        } catch (Exception e) {
            return "Falha ao criar arquivo no projeto: " + e.getMessage();
        }
    }
    @Override
    public AgentToolPromptMetadata getPromptMetadata() {
        AgentToolPromptMetadata metadata = new AgentToolPromptMetadata();
        metadata.setToolName(getName());
        metadata.setOneLinePurpose("Criar novo arquivo dentro de contexto estrutural editavel permitido.");
        metadata.setActivityDescription("Cria novo arquivo dentro de package ou pasta estrutural marcada como editavel, respeitando a politica de mutacao.");

        AgentToolParameterMetadata target = new AgentToolParameterMetadata();
        target.setName("target");
        target.setRequired(true);
        target.setDescription("Nome do contexto estrutural editavel de destino.");
        target.setExampleValue("service");
        metadata.addParameter(target);

        AgentToolParameterMetadata relativePath = new AgentToolParameterMetadata();
        relativePath.setName("relativePath");
        relativePath.setRequired(true);
        relativePath.setDescription("Caminho relativo do novo arquivo dentro do destino.");
        relativePath.setExampleValue("NovaRegraService.java");
        metadata.addParameter(relativePath);

        AgentToolParameterMetadata content = new AgentToolParameterMetadata();
        content.setName("content");
        content.setRequired(true);
        content.setDescription("Conteudo textual inicial do novo arquivo.");
        content.setExampleValue("public class NovaRegraService { }");
        metadata.addParameter(content);

        AgentToolParameterMetadata instructionSummary = new AgentToolParameterMetadata();
        instructionSummary.setName("instructionSummary");
        instructionSummary.setRequired(false);
        instructionSummary.setDescription("Resumo opcional da instrucao de origem.");
        instructionSummary.setExampleValue("Criacao de novo arquivo em contexto editavel.");
        metadata.addParameter(instructionSummary);

        metadata.addRecommendedUseCase("Use quando o contexto estrutural permitir criacao de novo arquivo.");
        metadata.addRecommendedUseCase("Use quando a IA precisar criar nova classe sem alterar arquivo preexistente.");
        metadata.addRecommendedUseCase("Use para criacao controlada dentro de package ou pasta editavel.");

        metadata.addGuardrail("O target deve apontar para contexto estrutural editavel valido.");
        metadata.addGuardrail("Nao use para alterar arquivo existente.");
        metadata.addGuardrail("A criacao deve respeitar a politica de mutacao do contexto.");

        metadata.addJsonExample(
                "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"criar_arquivo_projeto\\\",\\\"parameters\\\":{\\\"target\\\":\\\"service\\\",\\\"relativePath\\\":\\\"NovaRegraService.java\\\",\\\"content\\\":\\\"public class NovaRegraService { }\\\"},\\\"explanation\\\":\\\"Preciso criar novo arquivo dentro de contexto estrutural editavel autorizado.\\\"}"
        );

        return metadata;
    }
    /** * Resolve o contexto estrutural alvo pelo nome registrado na sessao atual. * * <p>O metodo busca o alvo entre os contextos estruturais nomeados ativos, * preservando compatibilidade com o comportamento anterior da tool.</p> * * @param targetName nome logico do alvo estrutural * @return contexto estrutural encontrado ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private NamedStructuralContext resolveTargetContext(String targetName) {
        if (isBlank(targetName)) {
            return null;
        }

        List<NamedStructuralContext> contexts =
                NamedStructuralContextSessionService.getInstance().getAll();

        for (int i = 0; i < contexts.size(); i++) {
            NamedStructuralContext context = contexts.get(i);
            if (context != null && targetName.equals(context.getName())) {
                return context;
            }
        }

        return null;
    }

    /** * Gera a chave estavel do projeto com base na raiz fisica informada. * * <p>O formato segue a mesma estrategia defensiva usada na camada de * memoria persistente e mutacao, preservando nome base normalizado e hash * curto da raiz canonica.</p> * * @param rootDirectory raiz fisica do projeto * @return chave estavel do projeto * * @author Renato Tomaz Nati * @since 2026-05-20 */
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

    /** * Retorna true quando o valor informado for nulo ou vazio. * * @param value valor a ser validado * @return true quando o valor estiver em branco * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}