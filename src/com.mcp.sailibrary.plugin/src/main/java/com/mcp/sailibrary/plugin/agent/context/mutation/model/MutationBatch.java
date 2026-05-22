package com.mcp.sailibrary.plugin.agent.context.mutation.model;

import java.util.ArrayList;
import java.util.List;

/** * Representa um lote logico de mutacoes agrupadas sob a mesma missao, * instrucao ou unidade coerente de trabalho. * * <p>O batch permite undo e redo em nivel superior ao da operacao individual, * preservando a coerencia entre multiplos arquivos, packages e artefatos * alterados pela mesma acao da IA ou do usuario.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class MutationBatch {

    private String batchId;
    private String instructionSummary;
    private MutationOrigin origin;
    private MutationOperationStatus status;
    private String branchAtOperation;
    private long startedAt;
    private long finishedAt;
    private List<MutationOperation> operations = new ArrayList<MutationOperation>();

    /** * Retorna o identificador do lote. * * @return batchId * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = safeTrim(batchId);
    }

    /** * Retorna um resumo curto da instrucao que originou o lote. * * @return resumo da instrucao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String getInstructionSummary() {
        return instructionSummary;
    }

    public void setInstructionSummary(String instructionSummary) {
        this.instructionSummary = safeTrim(instructionSummary);
    }

    /** * Retorna a origem funcional do lote. * * @return origem do batch * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public MutationOrigin getOrigin() {
        return origin;
    }

    public void setOrigin(MutationOrigin origin) {
        this.origin = origin;
    }

    /** * Retorna o status agregado do lote. * * @return status do batch * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public MutationOperationStatus getStatus() {
        return status;
    }

    public void setStatus(MutationOperationStatus status) {
        this.status = status;
    }

    /** * Retorna a branch observada no momento da execucao do lote. * * @return branch observada * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String getBranchAtOperation() {
        return branchAtOperation;
    }

    public void setBranchAtOperation(String branchAtOperation) {
        this.branchAtOperation = safeTrim(branchAtOperation);
    }

    /** * Retorna o instante de inicio do lote. * * @return timestamp inicial * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    /** * Retorna o instante de encerramento do lote. * * @return timestamp final * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public long getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(long finishedAt) {
        this.finishedAt = finishedAt;
    }

    /** * Retorna a lista de operacoes pertencentes ao lote. * * @return lista de operacoes * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public List<MutationOperation> getOperations() {
        return operations;
    }

    public void setOperations(List<MutationOperation> operations) {
        if (operations == null) {
            this.operations = new ArrayList<MutationOperation>();
        } else {
            this.operations = operations;
        }
    }

    /** * Adiciona uma operacao ao lote, ignorando valores nulos. * * @param operation operacao a ser associada ao batch * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void addOperation(MutationOperation operation) {
        if (operation == null) {
            return;
        }

        if (this.operations == null) {
            this.operations = new ArrayList<MutationOperation>();
        }

        this.operations.add(operation);
    }

    /** * Retorna true quando o lote possui dados minimos para uso seguro. * * @return true quando o batch estiver minimamente consistente * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public boolean isUsable() {
        return !isBlank(batchId)
                && origin != null
                && status != null
                && operations != null;
    }

    /** * Retorna a quantidade atual de operacoes no lote. * * @return total de operacoes registradas * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public int getOperationCount() {
        return operations == null ? 0 : operations.size();
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}