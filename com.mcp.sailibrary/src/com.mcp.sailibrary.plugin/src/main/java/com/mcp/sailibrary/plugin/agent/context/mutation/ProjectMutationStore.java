package com.mcp.sailibrary.plugin.agent.context.mutation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationBatch;
import com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationOperation;
import com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationOperationStatus;
import com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationOrigin;

/** * Centraliza a persistencia semantica das mutacoes do workspace em nivel de * projeto. * * <p>Esta classe atua como repository/facade da camada de journal de mutacao, * mantendo batches, operacoes, pilhas de undo/redo e metadados auxiliares em * arquivos JSON persistidos dentro da estrutura .sai do usuario.</p> * * <p>Esta classe nao executa operacoes Git, nao altera arquivos do workspace e * nao aplica politica de mutacao. Seu papel e registrar e consultar o estado * semantico da linha do tempo de mutacoes.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class ProjectMutationStore {

    private final File projectRootDirectory;
    private final ProjectMutationPaths mutationPaths;
    private final ProjectMutationJsonSupport jsonSupport;

    /** * Inicializa o store semantico de mutacoes para o projeto informado. * * @param projectRootDirectory raiz fisica do projeto atual * @param projectKey chave estavel do projeto dentro da pasta .sai * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public ProjectMutationStore(File projectRootDirectory, String projectKey) {
        this.projectRootDirectory = projectRootDirectory;
        this.mutationPaths = new ProjectMutationPaths(projectKey);
        this.jsonSupport = new ProjectMutationJsonSupport();
    }

    /** * Inicializa a estrutura minima da camada de mutacao em disco. * * <p>Se os arquivos ainda nao existirem, o metodo cria: * <ul> * <li>mutation_journal.json</li> * <li>mutation_state.json</li> * <li>mutation_repo_meta.json</li> * </ul> * sem sobrescrever conteudo preexistente.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void inicializarEstrutura() {
        jsonSupport.criarJsonObjectSeAusente(
                mutationPaths.getMutationJournalFile(),
                criarMutationJournalInicial()
        );

        jsonSupport.criarJsonObjectSeAusente(
                mutationPaths.getMutationStateFile(),
                criarMutationStateInicial()
        );

        jsonSupport.criarJsonObjectSeAusente(
                mutationPaths.getMutationRepoMetaFile(),
                criarMutationRepoMetaInicial()
        );

        File projectDirectory = mutationPaths.getProjectDirectory();
        if (projectDirectory != null && !projectDirectory.exists()) {
            projectDirectory.mkdirs();
        }

        File workspaceGitDirectory = mutationPaths.getWorkspaceGitDirectory();
        if (workspaceGitDirectory != null && !workspaceGitDirectory.exists()) {
            workspaceGitDirectory.mkdirs();
        }
    }

    /** * Registra um novo batch no journal de mutacao. * * <p>Se o batch for nulo ou inconsistente, a chamada sera ignorada. Se ja * existir um batch com o mesmo id, ele sera substituido pela nova versao * informada.</p> * * @param batch lote semantico de mutacoes * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void salvarBatch(MutationBatch batch) {
        if (batch == null || !batch.isUsable()) {
            return;
        }

        JsonObject journal = carregarMutationJournal();
        JsonArray batches = obterOuCriarArray(journal, "batches");

        removerBatchPorId(batches, batch.getBatchId());
        batches.add(toJson(batch));

        persistirMutationJournal(journal);
    }

    /** * Registra ou atualiza uma operacao dentro do batch informado. * * <p>Se o batch ainda nao existir no journal, ele sera criado com metadados * minimos e com a operacao associada. Se a operacao ja existir dentro do * batch, ela sera substituida.</p> * * @param batchId identificador do lote * @param operation operacao a ser persistida * @param instructionSummary resumo da instrucao de origem do batch * @param origin origem funcional do batch * @param branchAtOperation branch observada no momento da mutacao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void salvarOuAtualizarOperacao(String batchId, MutationOperation operation, String instructionSummary, MutationOrigin origin, String branchAtOperation) {

        if (isBlank(batchId) || operation == null || !operation.isUsable()) {
            return;
        }

        JsonObject journal = carregarMutationJournal();
        JsonArray batches = obterOuCriarArray(journal, "batches");

        JsonObject batchObject = localizarBatchJsonPorId(batches, batchId);
        if (batchObject == null) {
            batchObject = criarBatchMinimo(batchId, instructionSummary, origin, branchAtOperation);
            batches.add(batchObject);
        }

        JsonArray operations = obterOuCriarArray(batchObject, "operations");
        removerOperacaoPorId(operations, operation.getOperationId());
        operations.add(toJson(operation));

        batchObject.addProperty("finishedAt", String.valueOf(System.currentTimeMillis()));

        persistirMutationJournal(journal);
    }

    /** * Atualiza apenas o status de um batch existente. * * @param batchId identificador do lote * @param status novo status agregado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void atualizarStatusBatch(String batchId, MutationOperationStatus status) {
        if (isBlank(batchId) || status == null) {
            return;
        }

        JsonObject journal = carregarMutationJournal();
        JsonArray batches = obterOuCriarArray(journal, "batches");
        JsonObject batchObject = localizarBatchJsonPorId(batches, batchId);

        if (batchObject == null) {
            return;
        }

        batchObject.addProperty("status", status.name());
        batchObject.addProperty("finishedAt", String.valueOf(System.currentTimeMillis()));

        persistirMutationJournal(journal);
    }

    /** * Atualiza apenas o status de uma operacao existente. * * @param batchId identificador do lote * @param operationId identificador da operacao * @param status novo status da operacao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void atualizarStatusOperacao(String batchId, String operationId, MutationOperationStatus status) {
        if (isBlank(batchId) || isBlank(operationId) || status == null) {
            return;
        }

        JsonObject journal = carregarMutationJournal();
        JsonArray batches = obterOuCriarArray(journal, "batches");
        JsonObject batchObject = localizarBatchJsonPorId(batches, batchId);

        if (batchObject == null) {
            return;
        }

        JsonArray operations = obterOuCriarArray(batchObject, "operations");
        JsonObject operationObject = localizarOperacaoJsonPorId(operations, operationId);

        if (operationObject == null) {
            return;
        }

        operationObject.addProperty("status", status.name());
        persistirMutationJournal(journal);
    }

    /** * Retorna um batch especifico do journal, quando existir. * * @param batchId identificador do lote * @return batch carregado ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public MutationBatch buscarBatchPorId(String batchId) {
        if (isBlank(batchId)) {
            return null;
        }

        JsonObject journal = carregarMutationJournal();
        JsonArray batches = obterOuCriarArray(journal, "batches");
        JsonObject batchObject = localizarBatchJsonPorId(batches, batchId);

        if (batchObject == null) {
            return null;
        }

        return fromJsonBatch(batchObject);
    }

    /** * Retorna todos os batches persistidos no journal. * * @return lista de batches conhecidos * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public List<MutationBatch> listarBatches() {
        List<MutationBatch> result = new ArrayList<MutationBatch>();

        JsonObject journal = carregarMutationJournal();
        JsonArray batches = obterOuCriarArray(journal, "batches");

        for (int i = 0; i < batches.size(); i++) {
            JsonElement current = batches.get(i);
            if (current != null && current.isJsonObject()) {
                MutationBatch batch = fromJsonBatch(current.getAsJsonObject());
                if (batch != null) {
                    result.add(batch);
                }
            }
        }

        return result;
    }

    /** * Retorna as operacoes pertencentes a um batch especifico. * * @param batchId identificador do lote * @return lista de operacoes do lote ou lista vazia * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public List<MutationOperation> listarOperacoesDoBatch(String batchId) {
        List<MutationOperation> result = new ArrayList<MutationOperation>();

        MutationBatch batch = buscarBatchPorId(batchId);
        if (batch == null || batch.getOperations() == null) {
            return result;
        }

        result.addAll(batch.getOperations());
        return result;
    }

    /** * Empilha um batch na pilha de undo e limpa a pilha de redo. * * <p>Este comportamento segue o padrao classico de historico transacional: * ao aplicar uma nova mutacao, o redo anterior perde validade.</p> * * @param batchId identificador do lote aplicado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void pushUndoBatch(String batchId) {
        if (isBlank(batchId)) {
            return;
        }

        JsonObject state = carregarMutationState();
        JsonArray undoStack = obterOuCriarArray(state, "undoStack");
        JsonArray redoStack = obterOuCriarArray(state, "redoStack");

        removerStringDaLista(undoStack, batchId);
        undoStack.add(batchId);

        redoStack = new JsonArray();
        state.add("redoStack", redoStack);
        state.addProperty("lastUpdatedAt", String.valueOf(System.currentTimeMillis()));

        persistirMutationState(state);
    }

    /** * Move o batch do topo de undo para redo. * * @return batchId movido ou string vazia se nao houver lote em undo * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String moverUltimoUndoParaRedo() {
        JsonObject state = carregarMutationState();
        JsonArray undoStack = obterOuCriarArray(state, "undoStack");
        JsonArray redoStack = obterOuCriarArray(state, "redoStack");

        if (undoStack.size() == 0) {
            return "";
        }

        String batchId = undoStack.get(undoStack.size() - 1).getAsString();
        undoStack.remove(undoStack.size() - 1);
        redoStack.add(batchId);

        state.addProperty("lastUpdatedAt", String.valueOf(System.currentTimeMillis()));
        persistirMutationState(state);

        return batchId;
    }

    /** * Move o batch do topo de redo para undo. * * @return batchId movido ou string vazia se nao houver lote em redo * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String moverUltimoRedoParaUndo() {
        JsonObject state = carregarMutationState();
        JsonArray undoStack = obterOuCriarArray(state, "undoStack");
        JsonArray redoStack = obterOuCriarArray(state, "redoStack");

        if (redoStack.size() == 0) {
            return "";
        }

        String batchId = redoStack.get(redoStack.size() - 1).getAsString();
        redoStack.remove(redoStack.size() - 1);
        undoStack.add(batchId);

        state.addProperty("lastUpdatedAt", String.valueOf(System.currentTimeMillis()));
        persistirMutationState(state);

        return batchId;
    }

    /** * Retorna o ultimo batch disponivel na pilha de undo sem remove-lo. * * @return batchId do topo ou string vazia * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String peekUndoBatchId() {
        JsonObject state = carregarMutationState();
        JsonArray undoStack = obterOuCriarArray(state, "undoStack");

        if (undoStack.size() == 0) {
            return "";
        }

        return undoStack.get(undoStack.size() - 1).getAsString();
    }

    /** * Retorna o ultimo batch disponivel na pilha de redo sem remove-lo. * * @return batchId do topo ou string vazia * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String peekRedoBatchId() {
        JsonObject state = carregarMutationState();
        JsonArray redoStack = obterOuCriarArray(state, "redoStack");

        if (redoStack.size() == 0) {
            return "";
        }

        return redoStack.get(redoStack.size() - 1).getAsString();
    }

    /** * Remove um batch especifico das pilhas de undo e redo, quando existir. * * @param batchId identificador do lote a ser expurgado das pilhas * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void removerBatchDasPilhas(String batchId) {
        if (isBlank(batchId)) {
            return;
        }

        JsonObject state = carregarMutationState();
        JsonArray undoStack = obterOuCriarArray(state, "undoStack");
        JsonArray redoStack = obterOuCriarArray(state, "redoStack");

        removerStringDaLista(undoStack, batchId);
        removerStringDaLista(redoStack, batchId);

        state.addProperty("lastUpdatedAt", String.valueOf(System.currentTimeMillis()));
        persistirMutationState(state);
    }

    /** * Retorna o arquivo de journal de mutacao. * * @return arquivo mutation_journal.json * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File getMutationJournalFile() {
        return mutationPaths.getMutationJournalFile();
    }

    /** * Retorna o arquivo de estado da camada de mutacao. * * @return arquivo mutation_state.json * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File getMutationStateFile() {
        return mutationPaths.getMutationStateFile();
    }

    /** * Retorna o arquivo de metadados do repositorio interno de mutacao. * * @return arquivo mutation_repo_meta.json * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File getMutationRepoMetaFile() {
        return mutationPaths.getMutationRepoMetaFile();
    }

    /** * Retorna os caminhos da infraestrutura de mutacao do projeto atual. * * @return resolvedor de caminhos fisicos * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public ProjectMutationPaths getMutationPaths() {
        return mutationPaths;
    }

    /** * Carrega o journal atual de mutacao em forma de JsonObject seguro. * * @return journal carregado ou objeto vazio inicializado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public JsonObject carregarMutationJournal() {
        JsonObject journal = jsonSupport.lerJsonObject(mutationPaths.getMutationJournalFile());
        if (!journal.has("projectRoot")) {
            journal.addProperty("projectRoot", projectRootDirectory != null ? normalizePath(projectRootDirectory) : "");
        }
        if (!journal.has("batches") || !journal.get("batches").isJsonArray()) {
            journal.add("batches", new JsonArray());
        }
        return journal;
    }

    /** * Carrega o estado atual de undo/redo em forma de JsonObject seguro. * * @return state carregado ou objeto vazio inicializado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public JsonObject carregarMutationState() {
        JsonObject state = jsonSupport.lerJsonObject(mutationPaths.getMutationStateFile());

        if (!state.has("undoStack") || !state.get("undoStack").isJsonArray()) {
            state.add("undoStack", new JsonArray());
        }

        if (!state.has("redoStack") || !state.get("redoStack").isJsonArray()) {
            state.add("redoStack", new JsonArray());
        }

        return state;
    }

    /** * Persiste o journal informado no arquivo oficial da camada de mutacao. * * @param journal conteudo do journal * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void persistirMutationJournal(JsonObject journal) {
        jsonSupport.gravarJsonObject(mutationPaths.getMutationJournalFile(), journal);
    }

    /** * Persiste o estado informado no arquivo oficial da camada de mutacao. * * @param state conteudo do state * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void persistirMutationState(JsonObject state) {
        jsonSupport.gravarJsonObject(mutationPaths.getMutationStateFile(), state);
    }

    /** * Cria a estrutura inicial do arquivo mutation_journal.json. * * @return objeto inicial do journal * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private JsonObject criarMutationJournalInicial() {
        JsonObject root = new JsonObject();
        root.addProperty("projectRoot", projectRootDirectory != null ? normalizePath(projectRootDirectory) : "");
        root.add("batches", new JsonArray());
        return root;
    }

    /** * Cria a estrutura inicial do arquivo mutation_state.json. * * @return objeto inicial do state * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private JsonObject criarMutationStateInicial() {
        JsonObject root = new JsonObject();
        root.add("undoStack", new JsonArray());
        root.add("redoStack", new JsonArray());
        root.addProperty("lastUpdatedAt", "");
        return root;
    }

    /** * Cria a estrutura inicial do arquivo mutation_repo_meta.json. * * @return objeto inicial do metadata do repositorio interno * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private JsonObject criarMutationRepoMetaInicial() {
        JsonObject root = new JsonObject();
        root.addProperty("projectRoot", projectRootDirectory != null ? normalizePath(projectRootDirectory) : "");
        root.addProperty("repoInitialized", "false");
        root.addProperty("repoDirectory", normalizePath(mutationPaths.getWorkspaceGitDirectory()));
        root.addProperty("schemaVersion", "1.0");
        root.addProperty("lastUpdatedAt", "");
        return root;
    }

    /** * Converte um batch de dominio para JsonObject persistivel. * * @param batch lote de mutacao * @return representacao JSON do batch * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private JsonObject toJson(MutationBatch batch) {
        JsonObject json = new JsonObject();
        json.addProperty("batchId", safe(batch.getBatchId()));
        json.addProperty("instructionSummary", safe(batch.getInstructionSummary()));
        json.addProperty("origin", batch.getOrigin() != null ? batch.getOrigin().name() : "");
        json.addProperty("status", batch.getStatus() != null ? batch.getStatus().name() : "");
        json.addProperty("branchAtOperation", safe(batch.getBranchAtOperation()));
        json.addProperty("startedAt", String.valueOf(batch.getStartedAt()));
        json.addProperty("finishedAt", String.valueOf(batch.getFinishedAt()));

        JsonArray operations = new JsonArray();
        if (batch.getOperations() != null) {
            for (int i = 0; i < batch.getOperations().size(); i++) {
                MutationOperation operation = batch.getOperations().get(i);
                if (operation != null) {
                    operations.add(toJson(operation));
                }
            }
        }

        json.add("operations", operations);
        return json;
    }

    /** * Converte uma operacao de dominio para JsonObject persistivel. * * @param operation operacao de mutacao * @return representacao JSON da operacao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private JsonObject toJson(MutationOperation operation) {
        JsonObject json = new JsonObject();
        json.addProperty("operationId", safe(operation.getOperationId()));
        json.addProperty("batchId", safe(operation.getBatchId()));
        json.addProperty("actionType", operation.getActionType() != null ? operation.getActionType().name() : "");
        json.addProperty("status", operation.getStatus() != null ? operation.getStatus().name() : "");
        json.addProperty("origin", operation.getOrigin() != null ? operation.getOrigin().name() : "");
        json.addProperty("scope", operation.getScope() != null ? operation.getScope().name() : "");
        json.addProperty("toolName", safe(operation.getToolName()));
        json.addProperty("targetName", safe(operation.getTargetName()));
        json.addProperty("relativePath", safe(operation.getRelativePath()));
        json.addProperty("absolutePath", safe(operation.getAbsolutePath()));
        json.addProperty("branchAtOperation", safe(operation.getBranchAtOperation()));
        json.addProperty("summary", safe(operation.getSummary()));
        json.addProperty("beforeCommitId", safe(operation.getBeforeCommitId()));
        json.addProperty("afterCommitId", safe(operation.getAfterCommitId()));
        json.addProperty("createdAt", String.valueOf(operation.getCreatedAt()));
        return json;
    }

    /** * Converte um JsonObject persistido em um batch de dominio. * * @param json objeto JSON do batch * @return batch convertido * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private MutationBatch fromJsonBatch(JsonObject json) {
        if (json == null) {
            return null;
        }

        MutationBatch batch = new MutationBatch();
        batch.setBatchId(getString(json, "batchId"));
        batch.setInstructionSummary(getString(json, "instructionSummary"));
        batch.setOrigin(parseOrigin(getString(json, "origin")));
        batch.setStatus(parseStatus(getString(json, "status")));
        batch.setBranchAtOperation(getString(json, "branchAtOperation"));
        batch.setStartedAt(getLong(json, "startedAt"));
        batch.setFinishedAt(getLong(json, "finishedAt"));

        JsonArray operations = obterOuCriarArray(json, "operations");
        List<MutationOperation> operationList = new ArrayList<MutationOperation>();

        for (int i = 0; i < operations.size(); i++) {
            JsonElement current = operations.get(i);
            if (current != null && current.isJsonObject()) {
                MutationOperation operation = fromJsonOperation(current.getAsJsonObject());
                if (operation != null) {
                    operationList.add(operation);
                }
            }
        }

        batch.setOperations(operationList);
        return batch;
    }

    /** * Converte um JsonObject persistido em uma operacao de dominio. * * @param json objeto JSON da operacao * @return operacao convertida * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private MutationOperation fromJsonOperation(JsonObject json) {
        if (json == null) {
            return null;
        }

        MutationOperation operation = new MutationOperation();
        operation.setOperationId(getString(json, "operationId"));
        operation.setBatchId(getString(json, "batchId"));
        operation.setActionType(parseActionType(getString(json, "actionType")));
        operation.setStatus(parseStatus(getString(json, "status")));
        operation.setOrigin(parseOrigin(getString(json, "origin")));
        operation.setScope(parseScope(getString(json, "scope")));
        operation.setToolName(getString(json, "toolName"));
        operation.setTargetName(getString(json, "targetName"));
        operation.setRelativePath(getString(json, "relativePath"));
        operation.setAbsolutePath(getString(json, "absolutePath"));
        operation.setBranchAtOperation(getString(json, "branchAtOperation"));
        operation.setSummary(getString(json, "summary"));
        operation.setBeforeCommitId(getString(json, "beforeCommitId"));
        operation.setAfterCommitId(getString(json, "afterCommitId"));
        operation.setCreatedAt(getLong(json, "createdAt"));

        return operation;
    }

    /** * Cria um batch minimo quando uma operacao precisa ser registrada antes que * o chamador tenha persistido explicitamente o lote completo. * * @param batchId identificador do batch * @param instructionSummary resumo da instrucao * @param origin origem do lote * @param branchAtOperation branch observada * @return batch minimo em formato JSON * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private JsonObject criarBatchMinimo(String batchId, String instructionSummary, MutationOrigin origin, String branchAtOperation) {

        JsonObject batch = new JsonObject();
        batch.addProperty("batchId", safe(batchId));
        batch.addProperty("instructionSummary", safe(instructionSummary));
        batch.addProperty("origin", origin != null ? origin.name() : "");
        batch.addProperty("status", MutationOperationStatus.STARTED.name());
        batch.addProperty("branchAtOperation", safe(branchAtOperation));
        batch.addProperty("startedAt", String.valueOf(System.currentTimeMillis()));
        batch.addProperty("finishedAt", "");
        batch.add("operations", new JsonArray());
        return batch;
    }

    /** * Retorna um array JSON existente ou cria um novo array no campo indicado. * * @param object objeto raiz * @param fieldName nome do campo * @return array JSON associado ao campo * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private JsonArray obterOuCriarArray(JsonObject object, String fieldName) {
        if (object.has(fieldName) && object.get(fieldName).isJsonArray()) {
            return object.getAsJsonArray(fieldName);
        }

        JsonArray array = new JsonArray();
        object.add(fieldName, array);
        return array;
    }

    /** * Localiza um batch JSON pelo identificador. * * @param batches array de batches * @param batchId identificador procurado * @return objeto do batch ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private JsonObject localizarBatchJsonPorId(JsonArray batches, String batchId) {
        for (int i = 0; i < batches.size(); i++) {
            JsonElement current = batches.get(i);
            if (current != null && current.isJsonObject()) {
                JsonObject batchObject = current.getAsJsonObject();
                if (safe(batchId).equals(getString(batchObject, "batchId"))) {
                    return batchObject;
                }
            }
        }
        return null;
    }

    /** * Localiza uma operacao JSON pelo identificador. * * @param operations array de operacoes * @param operationId identificador procurado * @return objeto da operacao ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private JsonObject localizarOperacaoJsonPorId(JsonArray operations, String operationId) {
        for (int i = 0; i < operations.size(); i++) {
            JsonElement current = operations.get(i);
            if (current != null && current.isJsonObject()) {
                JsonObject operationObject = current.getAsJsonObject();
                if (safe(operationId).equals(getString(operationObject, "operationId"))) {
                    return operationObject;
                }
            }
        }
        return null;
    }

    /** * Remove um batch de um array JSON pelo identificador informado. * * @param batches array de batches * @param batchId identificador a remover * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void removerBatchPorId(JsonArray batches, String batchId) {
        for (int i = 0; i < batches.size(); i++) {
            JsonElement current = batches.get(i);
            if (current != null && current.isJsonObject()) {
                JsonObject batchObject = current.getAsJsonObject();
                if (safe(batchId).equals(getString(batchObject, "batchId"))) {
                    batches.remove(i);
                    return;
                }
            }
        }
    }

    /** * Remove uma operacao de um array JSON pelo identificador informado. * * @param operations array de operacoes * @param operationId identificador a remover * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void removerOperacaoPorId(JsonArray operations, String operationId) {
        for (int i = 0; i < operations.size(); i++) {
            JsonElement current = operations.get(i);
            if (current != null && current.isJsonObject()) {
                JsonObject operationObject = current.getAsJsonObject();
                if (safe(operationId).equals(getString(operationObject, "operationId"))) {
                    operations.remove(i);
                    return;
                }
            }
        }
    }

    /** * Remove uma string de uma pilha JSON quando ela existir. * * @param array pilha JSON * @param value valor a remover * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void removerStringDaLista(JsonArray array, String value) {
        for (int i = 0; i < array.size(); i++) {
            JsonElement current = array.get(i);
            if (current != null && current.isJsonPrimitive() && safe(value).equals(current.getAsString())) {
                array.remove(i);
                i--;
            }
        }
    }

    /** * Extrai string segura de um campo JSON. * * @param json objeto origem * @param fieldName nome do campo * @return string segura * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String getString(JsonObject json, String fieldName) {
        if (json != null && json.has(fieldName) && !json.get(fieldName).isJsonNull()) {
            try {
                return json.get(fieldName).getAsString();
            } catch (Exception e) {
                return "";
            }
        }
        return "";
    }

    /** * Extrai long seguro de um campo JSON. * * @param json objeto origem * @param fieldName nome do campo * @return long convertido ou zero em fallback seguro * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private long getLong(JsonObject json, String fieldName) {
        String value = getString(json, fieldName);
        if (isBlank(value)) {
            return 0L;
        }

        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return 0L;
        }
    }

    /** * Converte string em enum de origem com fallback nulo. * * @param value valor textual * @return enum correspondente ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private MutationOrigin parseOrigin(String value) {
        try {
            if (!isBlank(value)) {
                return MutationOrigin.valueOf(value.trim());
            }
        } catch (Exception e) {
        }
        return null;
    }

    /** * Converte string em enum de status com fallback nulo. * * @param value valor textual * @return enum correspondente ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private MutationOperationStatus parseStatus(String value) {
        try {
            if (!isBlank(value)) {
                return MutationOperationStatus.valueOf(value.trim());
            }
        } catch (Exception e) {
        }
        return null;
    }

    /** * Converte string em enum de actionType com fallback nulo. * * @param value valor textual * @return enum correspondente ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationActionType parseActionType(String value) {
        try {
            if (!isBlank(value)) {
                return com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationActionType.valueOf(value.trim());
            }
        } catch (Exception e) {
        }
        return null;
    }

    /** * Converte string em enum de scope com fallback nulo. * * @param value valor textual * @return enum correspondente ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationTargetScope parseScope(String value) {
        try {
            if (!isBlank(value)) {
                return com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationTargetScope.valueOf(value.trim());
            }
        } catch (Exception e) {
        }
        return null;
    }

    /** * Retorna string segura nao nula. * * @param value valor original * @return string segura * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String safe(String value) {
        return value == null ? "" : value.trim();
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

    /** * Retorna true quando o valor informado for nulo ou vazio. * * @param value valor a validar * @return true quando o valor for branco * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}