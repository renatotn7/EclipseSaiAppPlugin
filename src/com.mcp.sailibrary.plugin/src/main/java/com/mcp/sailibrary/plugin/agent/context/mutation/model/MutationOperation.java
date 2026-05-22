package com.mcp.sailibrary.plugin.agent.context.mutation.model;

/** * Representa uma operacao individual de mutacao registrada na linha do tempo * interna do plugin. * * <p>Uma operacao descreve um ato unitario de criacao, alteracao, remocao, * restauracao, undo ou redo sobre o workspace, mantendo rastreabilidade de * commits before/after, caminho atingido e metadados de origem.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class MutationOperation {

    private String operationId;
    private String batchId;
    private MutationActionType actionType;
    private MutationOperationStatus status;
    private MutationOrigin origin;
    private MutationTargetScope scope;

    private String toolName;
    private String targetName;
    private String relativePath;
    private String absolutePath;
    private String branchAtOperation;
    private String summary;

    private String beforeCommitId;
    private String afterCommitId;

    private long createdAt;

    /** * Retorna o identificador unico da operacao. * * @return operationId * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = safeTrim(operationId);
    }

    /** * Retorna o identificador do lote ao qual a operacao pertence. * * @return batchId * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = safeTrim(batchId);
    }

    /** * Retorna o tipo semantico da operacao. * * @return tipo da mutacao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public MutationActionType getActionType() {
        return actionType;
    }

    public void setActionType(MutationActionType actionType) {
        this.actionType = actionType;
    }

    /** * Retorna o status operacional atual da mutacao. * * @return status da operacao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public MutationOperationStatus getStatus() {
        return status;
    }

    public void setStatus(MutationOperationStatus status) {
        this.status = status;
    }

    /** * Retorna a origem funcional da mutacao. * * @return origem da operacao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public MutationOrigin getOrigin() {
        return origin;
    }

    public void setOrigin(MutationOrigin origin) {
        this.origin = origin;
    }

    /** * Retorna o escopo principal do alvo atingido. * * @return escopo do alvo * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public MutationTargetScope getScope() {
        return scope;
    }

    public void setScope(MutationTargetScope scope) {
        this.scope = scope;
    }

    /** * Retorna o nome da tool de origem. * * @return nome da ferramenta * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = safeTrim(toolName);
    }

    /** * Retorna o nome logico do alvo semantico da mutacao. * * @return nome do alvo * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = safeTrim(targetName);
    }

    /** * Retorna o caminho relativo do artefato atingido. * * @return caminho relativo * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = normalizePath(relativePath);
    }

    /** * Retorna o caminho absoluto do artefato atingido. * * @return caminho absoluto * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String getAbsolutePath() {
        return absolutePath;
    }

    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = normalizePath(absolutePath);
    }

    /** * Retorna a branch observada no momento da operacao. * * @return nome da branch observada * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String getBranchAtOperation() {
        return branchAtOperation;
    }

    public void setBranchAtOperation(String branchAtOperation) {
        this.branchAtOperation = safeTrim(branchAtOperation);
    }

    /** * Retorna um resumo curto da operacao. * * @return resumo semantico da mutacao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = safeTrim(summary);
    }

    /** * Retorna o commit que representa o estado anterior da operacao. * * @return hash ou id logico do commit before * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String getBeforeCommitId() {
        return beforeCommitId;
    }

    public void setBeforeCommitId(String beforeCommitId) {
        this.beforeCommitId = safeTrim(beforeCommitId);
    }

    /** * Retorna o commit que representa o estado posterior da operacao. * * @return hash ou id logico do commit after * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String getAfterCommitId() {
        return afterCommitId;
    }

    public void setAfterCommitId(String afterCommitId) {
        this.afterCommitId = safeTrim(afterCommitId);
    }

    /** * Retorna o timestamp de criacao da operacao. * * @return instante em milessegundos * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    /** * Retorna true quando a operacao possui os campos minimos esperados para * persistencia e rastreabilidade. * * @return true quando a operacao estiver minimamente consistente * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public boolean isUsable() {
        return !isBlank(operationId)
                && !isBlank(batchId)
                && actionType != null
                && status != null
                && origin != null
                && scope != null;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizePath(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replace("\\", "/");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}