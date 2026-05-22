package com.mcp.sailibrary.plugin.agent.tools.mutation.history;

import java.io.File;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.context.mutation.ProjectMutationStore;
import com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationBatch;
import com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationOperation;
import com.mcp.sailibrary.plugin.agent.tools.support.ToolJsonSupport;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolParameterMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadataProvider;
/** * Lista o historico persistido de mutacoes do workspace para o projeto atual. * * <p>Esta tool permite inspecionar batches e operacoes registradas pela camada * de mutacao, com filtro opcional por path e limite de resultados. O objetivo * e oferecer visibilidade tatica para a IA antes de executar undo, redo ou * restauracoes seletivas.</p> * * <p>Esta implementacao e somente de leitura. Nenhuma alteracao e aplicada no * workspace real, no espelho interno ou no journal de mutacao.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class ListWorkspaceMutationHistoryTool implements AgentTool, AgentToolPromptMetadataProvider {

    private final File rootDirectory;
    private final ProjectMutationStore mutationStore;

    /** * Inicializa a tool de listagem de historico de mutacoes do projeto atual. * * @param rootDirectory raiz segura do projeto atual * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public ListWorkspaceMutationHistoryTool(File rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.mutationStore = new ProjectMutationStore(
                rootDirectory,
                gerarProjectKey(rootDirectory)
        );
    }

    @Override
    public String getName() {
        return "listar_historico_mutacoes";
    }

    @Override
    public String execute(String jsonParameters) {
        String path = ToolJsonSupport.extractJsonStringValue(jsonParameters, "path");
        String batchId = ToolJsonSupport.extractJsonStringValue(jsonParameters, "batchId");
        int limit = ToolJsonSupport.extractJsonIntValue(jsonParameters, "limit", 10, 1, 200);

        if (rootDirectory == null || !rootDirectory.exists() || !rootDirectory.isDirectory()) {
            return "Erro Operacional: A raiz segura do projeto esta indisponivel para consulta de historico de mutacoes.";
        }

        try {
            mutationStore.inicializarEstrutura();

            if (!isBlank(batchId)) {
                return listarBatchEspecifico(batchId);
            }

            return listarHistoricoGeral(path, limit);
        } catch (Exception e) {
            return "Falha ao consultar historico de mutacoes: " + e.getMessage();
        }
    }
    @Override
    public AgentToolPromptMetadata getPromptMetadata() {
        AgentToolPromptMetadata metadata = new AgentToolPromptMetadata();
        metadata.setToolName(getName());
        metadata.setOneLinePurpose("Listar o historico persistido de mutacoes do workspace.");
        metadata.setActivityDescription("Lista o historico persistido de mutacoes do workspace.");

        AgentToolParameterMetadata path = new AgentToolParameterMetadata();
        path.setName("path");
        path.setRequired(false);
        path.setDescription("Filtro opcional por caminho relativo ou trecho de caminho.");
        path.setExampleValue("src/main/java/com/exemplo/Servico.java");
        metadata.addParameter(path);

        AgentToolParameterMetadata batchId = new AgentToolParameterMetadata();
        batchId.setName("batchId");
        batchId.setRequired(false);
        batchId.setDescription("Batch especifico a ser inspecionado.");
        batchId.setExampleValue("batch_1716400000000_123");
        metadata.addParameter(batchId);

        AgentToolParameterMetadata limit = new AgentToolParameterMetadata();
        limit.setName("limit");
        limit.setRequired(false);
        limit.setDescription("Quantidade maxima de batches retornados.");
        limit.setExampleValue("10");
        metadata.addParameter(limit);

        metadata.addRecommendedUseCase("Use antes de desfazer ou refazer mutacoes.");
        metadata.addRecommendedUseCase("Use para descobrir qual batch ou operacao afetou determinado arquivo.");
        metadata.addRecommendedUseCase("Use para explicar o historico recente de alteracoes do workspace.");

        metadata.addGuardrail("Prefira batchId quando o historico estiver muito grande.");
        metadata.addGuardrail("Use filtro por path para reduzir ruido.");
        metadata.addGuardrail("Esta ferramenta nao altera nada no workspace.");

        metadata.addJsonExample(
                "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"listar_historico_mutacoes\\\",\\\"parameters\\\":{\\\"path\\\":\\\"src/main/java/com/exemplo/Servico.java\\\",\\\"limit\\\":\\\"10\\\"},\\\"explanation\\\":\\\"Preciso consultar o historico persistido de mutacoes antes de decidir o proximo passo.\\\"}"
        );

        return metadata;
    }
    /** * Lista um batch especifico do historico persistido. * * @param batchId identificador do lote desejado * @return relatorio textual do batch ou mensagem de ausencia * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String listarBatchEspecifico(String batchId) {
        MutationBatch batch = mutationStore.buscarBatchPorId(batchId);
        if (batch == null) {
            return "Nenhum batch encontrado para o identificador informado.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Historico de Mutacao - Batch Especifico").append("\n");
        sb.append("batchId: ").append(safe(batch.getBatchId())).append("\n");
        sb.append("origin: ").append(batch.getOrigin() != null ? batch.getOrigin().name() : "").append("\n");
        sb.append("status: ").append(batch.getStatus() != null ? batch.getStatus().name() : "").append("\n");
        sb.append("branchAtOperation: ").append(safe(batch.getBranchAtOperation())).append("\n");
        sb.append("instructionSummary: ").append(safe(batch.getInstructionSummary())).append("\n");
        sb.append("startedAt: ").append(batch.getStartedAt()).append("\n");
        sb.append("finishedAt: ").append(batch.getFinishedAt()).append("\n");
        sb.append("operationCount: ").append(batch.getOperationCount()).append("\n");

        List<MutationOperation> operations = batch.getOperations() != null
                ? batch.getOperations()
                : new ArrayList<MutationOperation>();

        ordenarOperacoesMaisRecentesPrimeiro(operations);

        for (int i = 0; i < operations.size(); i++) {
            sb.append("\n");
            sb.append(formatarOperacao(operations.get(i), i + 1));
        }

        return sb.toString();
    }

    /** * Lista o historico geral de mutacoes com filtro opcional por caminho. * * @param pathFilter filtro opcional por caminho relativo ou trecho do path * @param limit quantidade maxima de batches retornados * @return relatorio textual do historico * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String listarHistoricoGeral(String pathFilter, int limit) {
        List<MutationBatch> batches = mutationStore.listarBatches();
        ordenarBatchesMaisRecentesPrimeiro(batches);

        String normalizedFilter = normalizePathFragment(pathFilter);
        List<MutationBatch> filtrados = new ArrayList<MutationBatch>();

        for (int i = 0; i < batches.size(); i++) {
            MutationBatch batch = batches.get(i);
            if (batch == null) {
                continue;
            }

            if (isBlank(normalizedFilter) || batchContemPath(batch, normalizedFilter)) {
                filtrados.add(batch);
            }

            if (filtrados.size() >= limit) {
                break;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Historico de Mutacoes do Workspace").append("\n");
        sb.append("projectRoot: ").append(normalizePath(rootDirectory)).append("\n");
        sb.append("pathFilter: ").append(safe(pathFilter)).append("\n");
        sb.append("limit: ").append(limit).append("\n");
        sb.append("totalBatchesEncontrados: ").append(filtrados.size()).append("\n");

        if (filtrados.isEmpty()) {
            sb.append("\n");
            sb.append("Nenhum batch de mutacao encontrado para os filtros informados.");
            return sb.toString();
        }

        for (int i = 0; i < filtrados.size(); i++) {
            MutationBatch batch = filtrados.get(i);
            sb.append("\n");
            sb.append("--------------------------------------------------").append("\n");
            sb.append("Batch ").append(i + 1).append("\n");
            sb.append("batchId: ").append(safe(batch.getBatchId())).append("\n");
            sb.append("origin: ").append(batch.getOrigin() != null ? batch.getOrigin().name() : "").append("\n");
            sb.append("status: ").append(batch.getStatus() != null ? batch.getStatus().name() : "").append("\n");
            sb.append("branchAtOperation: ").append(safe(batch.getBranchAtOperation())).append("\n");
            sb.append("instructionSummary: ").append(safe(batch.getInstructionSummary())).append("\n");
            sb.append("startedAt: ").append(batch.getStartedAt()).append("\n");
            sb.append("finishedAt: ").append(batch.getFinishedAt()).append("\n");
            sb.append("operationCount: ").append(batch.getOperationCount()).append("\n");

            List<MutationOperation> operations = batch.getOperations() != null
                    ? batch.getOperations()
                    : new ArrayList<MutationOperation>();

            ordenarOperacoesMaisRecentesPrimeiro(operations);

            for (int j = 0; j < operations.size(); j++) {
                MutationOperation operation = operations.get(j);

                if (!isBlank(normalizedFilter) && !operacaoCombinaComPath(operation, normalizedFilter)) {
                    continue;
                }

                sb.append("\n");
                sb.append(formatarOperacao(operation, j + 1));
            }
        }

        return sb.toString();
    }

    /** * Formata uma operacao individual do historico em texto compacto e legivel. * * @param operation operacao a formatar * @param index indice exibido ao usuario * @return bloco textual da operacao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String formatarOperacao(MutationOperation operation, int index) {
        StringBuilder sb = new StringBuilder();
        sb.append("Operacao ").append(index).append("\n");
        sb.append("operationId: ").append(safe(operation.getOperationId())).append("\n");
        sb.append("actionType: ").append(operation.getActionType() != null ? operation.getActionType().name() : "").append("\n");
        sb.append("status: ").append(operation.getStatus() != null ? operation.getStatus().name() : "").append("\n");
        sb.append("origin: ").append(operation.getOrigin() != null ? operation.getOrigin().name() : "").append("\n");
        sb.append("scope: ").append(operation.getScope() != null ? operation.getScope().name() : "").append("\n");
        sb.append("toolName: ").append(safe(operation.getToolName())).append("\n");
        sb.append("targetName: ").append(safe(operation.getTargetName())).append("\n");
        sb.append("relativePath: ").append(safe(operation.getRelativePath())).append("\n");
        sb.append("absolutePath: ").append(safe(operation.getAbsolutePath())).append("\n");
        sb.append("branchAtOperation: ").append(safe(operation.getBranchAtOperation())).append("\n");
        sb.append("summary: ").append(safe(operation.getSummary())).append("\n");
        sb.append("beforeCommitId: ").append(safe(operation.getBeforeCommitId())).append("\n");
        sb.append("afterCommitId: ").append(safe(operation.getAfterCommitId())).append("\n");
        sb.append("createdAt: ").append(operation.getCreatedAt()).append("\n");
        return sb.toString();
    }

    /** * Retorna true quando o batch contem ao menos uma operacao cujo path seja * compatível com o filtro informado. * * @param batch batch a inspecionar * @param normalizedFilter filtro de caminho normalizado * @return true quando houver compatibilidade de path * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean batchContemPath(MutationBatch batch, String normalizedFilter) {
        if (batch == null || batch.getOperations() == null) {
            return false;
        }

        for (int i = 0; i < batch.getOperations().size(); i++) {
            MutationOperation operation = batch.getOperations().get(i);
            if (operacaoCombinaComPath(operation, normalizedFilter)) {
                return true;
            }
        }

        return false;
    }

    /** * Retorna true quando a operacao combina com o filtro de caminho * informado. * * @param operation operacao a verificar * @param normalizedFilter filtro normalizado * @return true quando o filtro estiver contido no path relativo ou absoluto * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean operacaoCombinaComPath(MutationOperation operation, String normalizedFilter) {
        if (operation == null || isBlank(normalizedFilter)) {
            return false;
        }

        String relativePath = normalizePathFragment(operation.getRelativePath());
        String absolutePath = normalizePathFragment(operation.getAbsolutePath());

        return relativePath.contains(normalizedFilter) || absolutePath.contains(normalizedFilter);
    }

    /** * Ordena batches do mais recente para o mais antigo. * * @param batches lista de batches a ordenar * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void ordenarBatchesMaisRecentesPrimeiro(List<MutationBatch> batches) {
        Collections.sort(batches, new Comparator<MutationBatch>() {
            @Override
            public int compare(MutationBatch a, MutationBatch b) {
                return Long.compare(b.getStartedAt(), a.getStartedAt());
            }
        });
    }

    /** * Ordena operacoes do mais recente para o mais antigo. * * @param operations lista de operacoes a ordenar * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void ordenarOperacoesMaisRecentesPrimeiro(List<MutationOperation> operations) {
        Collections.sort(operations, new Comparator<MutationOperation>() {
            @Override
            public int compare(MutationOperation a, MutationOperation b) {
                return Long.compare(b.getCreatedAt(), a.getCreatedAt());
            }
        });
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

    /** * Normaliza um fragmento de caminho para comparacao textual segura. * * @param path valor original * @return caminho normalizado em minusculo com barras normais * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String normalizePathFragment(String path) {
        if (path == null) {
            return "";
        }

        return path.trim().replace("\\", "/").toLowerCase();
    }

    /** * Normaliza caminho fisico para formato com barras normais. * * @param file arquivo de origem * @return caminho normalizado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String normalizePath(File file) {
        if (file == null) {
            return "";
        }

        try {
            return file.getCanonicalPath().replace("\\", "/");
        } catch (Exception e) {
            return file.getAbsolutePath().replace("\\", "/");
        }
    }

    /** * Retorna string segura nao nula. * * @param value valor original * @return string segura * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    /** * Retorna true quando o valor informado for nulo ou vazio. * * @param value valor a validar * @return true quando o valor for branco * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}