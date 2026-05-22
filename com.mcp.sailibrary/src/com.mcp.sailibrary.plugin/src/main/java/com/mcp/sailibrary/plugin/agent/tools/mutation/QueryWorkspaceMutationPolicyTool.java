package com.mcp.sailibrary.plugin.agent.tools.mutation;

import java.io.File;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolParameterMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadataProvider;
import com.mcp.sailibrary.plugin.agent.tools.support.StructuralTargetResolver;
import com.mcp.sailibrary.plugin.agent.tools.support.ToolJsonSupport;
import com.mcp.sailibrary.plugin.chat.context.service.SafeWorkspaceMutationPolicy;

/** * Consulta a politica de mutacao segura para arquivos, packages e pastas antes * de criar, alterar ou apagar. * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class QueryWorkspaceMutationPolicyTool implements AgentTool, AgentToolPromptMetadataProvider {

    private final File rootDirectory;
    private final SafeWorkspaceMutationPolicy mutationPolicy;
    private final StructuralTargetResolver structuralTargetResolver;

    /** * Inicializa a ferramenta com a raiz segura, politica de mutacao e * resolvedor de aliases estruturais. * * @param rootDirectory raiz segura do projeto * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public QueryWorkspaceMutationPolicyTool(File rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.mutationPolicy = new SafeWorkspaceMutationPolicy(rootDirectory);
        this.structuralTargetResolver = new StructuralTargetResolver(rootDirectory);
    }

    @Override
    public String getName() {
        return "consultar_politica_mutacao_contexto";
    }

    @Override
    public String execute(String jsonParameters) {
        String target = ToolJsonSupport.extractJsonStringValue(jsonParameters, "target");
        String operation = ToolJsonSupport.extractJsonStringValue(jsonParameters, "operation");
        String path = ToolJsonSupport.extractJsonStringValue(jsonParameters, "path");
        String relativePath = ToolJsonSupport.extractJsonStringValue(jsonParameters, "relativePath");

        if (isBlank(operation)) {
            return "Erro Operacional: O parametro operation e obrigatorio.";
        }

        String normalizedOperation = operation.trim().toLowerCase();

        if ("create_file".equals(normalizedOperation)) {
            File baseDirectory = structuralTargetResolver.resolveBaseDirectory(target);
            boolean allowed = mutationPolicy.canCreateFile(target, relativePath) && baseDirectory != null;
            return buildResult("create_file", allowed, target, relativePath, baseDirectory != null ? normalizePath(baseDirectory) : "");
        }

        if ("create_package".equals(normalizedOperation)) {
            File baseDirectory = structuralTargetResolver.resolveBaseDirectory(target);
            boolean allowed = mutationPolicy.canCreatePackage(target, relativePath) && baseDirectory != null;
            return buildResult("create_package", allowed, target, relativePath, baseDirectory != null ? normalizePath(baseDirectory) : "");
        }

        if ("update_file".equals(normalizedOperation)) {
            File targetFile = resolveTargetFile(path);
            boolean allowed = mutationPolicy.canUpdateFile(targetFile);
            String backupPath = "";
            if (allowed) {
                File backupFile = mutationPolicy.createBackupFile(targetFile);
                if (backupFile != null) {
                    backupPath = normalizePath(backupFile);
                }
            }
            return buildResult("update_file", allowed, target, path, backupPath);
        }

        if ("delete_file".equals(normalizedOperation)) {
            File targetFile = resolveTargetFile(path);
            boolean allowed = mutationPolicy.canDeleteFile(targetFile);
            return buildResult("delete_file", allowed, target, path, "");
        }

        return "Erro Operacional: Operation nao suportada. Valores aceitos: create_file, create_package, update_file, delete_file.";
    }

    @Override
    public AgentToolPromptMetadata getPromptMetadata() {
        AgentToolPromptMetadata metadata = new AgentToolPromptMetadata();
        metadata.setToolName(getName());
        metadata.setOneLinePurpose("Consultar a politica de mutacao segura para arquivos, packages e pastas.");
        metadata.setActivityDescription("Verifica se a politica de mutacao permite criar, alterar ou apagar arquivos com seguranca dentro do contexto atual.");

        AgentToolParameterMetadata target = new AgentToolParameterMetadata();
        target.setName("target");
        target.setRequired(false);
        target.setDescription("Nome do contexto estrutural alvo.");
        target.setExampleValue("service");
        metadata.addParameter(target);

        AgentToolParameterMetadata operation = new AgentToolParameterMetadata();
        operation.setName("operation");
        operation.setRequired(true);
        operation.setDescription("Operacao desejada: create_file, create_package, update_file ou delete_file.");
        operation.setExampleValue("create_file");
        metadata.addParameter(operation);

        AgentToolParameterMetadata path = new AgentToolParameterMetadata();
        path.setName("path");
        path.setRequired(false);
        path.setDescription("Caminho de arquivo existente usado para update_file ou delete_file.");
        path.setExampleValue("src/main/resources/regras.xml");
        metadata.addParameter(path);

        AgentToolParameterMetadata relativePath = new AgentToolParameterMetadata();
        relativePath.setName("relativePath");
        relativePath.setRequired(false);
        relativePath.setDescription("Caminho relativo usado para create_file ou create_package.");
        relativePath.setExampleValue("NovaRegraService.java");
        metadata.addParameter(relativePath);

        metadata.addRecommendedUseCase("Use antes de criar, alterar ou apagar artefatos no workspace.");
        metadata.addRecommendedUseCase("Use quando houver qualquer duvida sobre permissao de mutacao no contexto atual.");
        metadata.addRecommendedUseCase("Use para decidir entre criacao segura e bloqueio de alteracao indevida.");

        metadata.addGuardrail("Nao assuma permissao de mutacao apenas porque a pasta ou package esta marcada.");
        metadata.addGuardrail("Prefira consultar esta politica antes de mutar arquivo preexistente.");
        metadata.addGuardrail("Use operation correta para cada tipo de mutacao.");

        metadata.addJsonExample(
                "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"consultar_politica_mutacao_contexto\\\",\\\"parameters\\\":{\\\"target\\\":\\\"service\\\",\\\"operation\\\":\\\"create_file\\\",\\\"relativePath\\\":\\\"NovaRegraService.java\\\"},\\\"explanation\\\":\\\"Preciso validar se o contexto estrutural @service permite criar novo arquivo com seguranca.\\\"}"
        );

        return metadata;
    }

    /** * Resolve o arquivo alvo para update/delete. * * @param path caminho informado * @return arquivo resolvido * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private File resolveTargetFile(String path) {
        if (isBlank(path)) {
            return null;
        }

        File file = new File(path);
        if (file.exists()) {
            return file;
        }

        return new File(rootDirectory, path);
    }

    /** * Monta relatorio textual da politica de mutacao consultada. * * @param operation operacao consultada * @param allowed flag de permissao * @param target alvo logico * @param path caminho consultado * @param detail detalhe adicional como backup path ou base path * @return relatorio textual * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String buildResult(String operation, boolean allowed, String target, String path, String detail) {
        StringBuilder sb = new StringBuilder();
        sb.append("Relatorio de Politica de Mutacao\n");
        sb.append("operation: ").append(operation).append("\n");
        sb.append("target: ").append(target != null ? target : "").append("\n");
        sb.append("path: ").append(path != null ? path : "").append("\n");
        sb.append("allowed: ").append(allowed ? "true" : "false").append("\n");

        if (detail != null && detail.trim().length() > 0) {
            if ("update_file".equals(operation)) {
                sb.append("backupPath: ").append(detail).append("\n");
            } else {
                sb.append("resolvedBasePath: ").append(detail).append("\n");
            }
        }

        if (allowed) {
            sb.append("result: Operacao permitida pela politica atual.");
        } else {
            sb.append("result: Operacao negada pela politica atual.");
        }

        return sb.toString();
    }

    /** * Normaliza caminho fisico para formato com barras normais. * * @param file arquivo de origem * @return caminho normalizado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String normalizePath(File file) {
        try {
            return file.getCanonicalPath().replace("\\", "/");
        } catch (Exception e) {
            return file.getAbsolutePath().replace("\\", "/");
        }
    }

    /** * Retorna true quando o valor informado for nulo ou vazio. * * @param value valor a validar * @return true quando o valor estiver em branco * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}