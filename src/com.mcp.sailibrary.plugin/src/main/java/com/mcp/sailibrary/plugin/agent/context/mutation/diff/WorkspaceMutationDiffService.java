package com.mcp.sailibrary.plugin.agent.context.mutation.diff;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.mcp.sailibrary.plugin.agent.context.mutation.JGitWorkspaceRepository;
import com.mcp.sailibrary.plugin.agent.context.mutation.ProjectMutationStore;
import com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationBatch;
import com.mcp.sailibrary.plugin.agent.context.mutation.model.MutationOperation;

/** * Gera relatorios de diff textual e semantico para arquivos mutados no * workspace, comparando estados before, after e current. * * <p>Esta classe foi desenhada para apoiar a IA em tarefas como: * <ul> * <li>explicar o que mudou</li> * <li>avaliar se vale desfazer ou refazer</li> * <li>inspecionar impacto local de mutacoes</li> * <li>comparar estado atual com snapshots versionados</li> * </ul> * </p> * * <p>O servico nao altera o workspace, nao grava no journal e nao executa undo * ou redo. Ele apenas resolve os estados corretos e produz um relatorio * comparativo de leitura.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class WorkspaceMutationDiffService {

    private final File rootDirectory;
    private final ProjectMutationStore mutationStore;
    private final JGitWorkspaceRepository gitRepository;

    /** * Inicializa o servico de diff da camada de mutacao. * * @param rootDirectory raiz fisica do projeto real * @param mutationStore store semantico de mutacao * @param gitRepository backend Git interno da camada de mutacao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public WorkspaceMutationDiffService(File rootDirectory, ProjectMutationStore mutationStore, JGitWorkspaceRepository gitRepository) {
        this.rootDirectory = rootDirectory;
        this.mutationStore = mutationStore;
        this.gitRepository = gitRepository;
    }

    /** * Inspeciona o diff de um arquivo mutado usando operacao, batch e modo de * comparacao. * * <p>Modos suportados: * <ul> * <li>before_after</li> * <li>current_before</li> * <li>current_after</li> * </ul> * </p> * * <p>A prioridade de resolucao da operacao e: * <ol> * <li>operationId explicito</li> * <li>batchId + path</li> * <li>path no historico global</li> * </ol> * </p> * * @param relativePath caminho relativo do arquivo alvo * @param batchId batch opcional * @param operationId operacao opcional * @param mode modo de comparacao * @param maxLines quantidade maxima de linhas de diff exibidas no trecho * @return relatorio textual de diff * * @throws Exception quando ocorrer falha de leitura ou resolucao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String inspectDiff(String relativePath, String batchId, String operationId, String mode, int maxLines) throws Exception {

        if (rootDirectory == null || !rootDirectory.exists() || !rootDirectory.isDirectory()) {
            return "Erro Operacional: A raiz segura do projeto esta indisponivel para inspecao de diff.";
        }

        String normalizedPath = normalizeRelativePath(relativePath);
        if (isBlank(normalizedPath)) {
            return "Erro Operacional: O parametro path e obrigatorio para inspecao de diff.";
        }

        String normalizedMode = normalizeMode(mode);
        int safeMaxLines = maxLines > 0 ? maxLines : 80;

        mutationStore.inicializarEstrutura();
        gitRepository.ensureRepositoryInitialized();

        MutationOperation targetOperation = resolveTargetOperation(normalizedPath, batchId, operationId);
        if (targetOperation == null) {
            return "Erro Operacional: Nenhuma operacao compativel foi encontrada para o path informado.";
        }

        String leftLabel;
        String rightLabel;
        String leftContent;
        String rightContent;

        if ("before_after".equals(normalizedMode)) {
            leftLabel = "before";
            rightLabel = "after";
            leftContent = loadContentFromCommit(targetOperation.getBeforeCommitId(), normalizedPath);
            rightContent = loadContentFromCommit(targetOperation.getAfterCommitId(), normalizedPath);
        } else if ("current_before".equals(normalizedMode)) {
            leftLabel = "current";
            rightLabel = "before";
            leftContent = loadCurrentWorkspaceContent(normalizedPath);
            rightContent = loadContentFromCommit(targetOperation.getBeforeCommitId(), normalizedPath);
        } else {
            leftLabel = "current";
            rightLabel = "after";
            leftContent = loadCurrentWorkspaceContent(normalizedPath);
            rightContent = loadContentFromCommit(targetOperation.getAfterCommitId(), normalizedPath);
        }

        if (leftContent == null) {
            leftContent = "";
        }
        if (rightContent == null) {
            rightContent = "";
        }

        DiffSummary summary = buildDiffSummary(normalizedPath, targetOperation, normalizedMode, leftLabel, rightLabel, leftContent, rightContent, safeMaxLines);
        return formatSummary(summary);
    }

    /** * Resolve a operacao alvo para a comparacao de diff. * * @param relativePath caminho relativo do arquivo * @param batchId batch opcional * @param operationId operacao opcional * @return operacao resolvida ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private MutationOperation resolveTargetOperation(String relativePath, String batchId, String operationId) {
        if (!isBlank(operationId)) {
            MutationOperation byOperationId = findOperationById(operationId);
            if (byOperationId != null && operationMatchesPath(byOperationId, relativePath)) {
                return byOperationId;
            }
        }

        if (!isBlank(batchId)) {
            MutationBatch batch = mutationStore.buscarBatchPorId(batchId);
            if (batch != null && batch.getOperations() != null) {
                List<MutationOperation> operations = new ArrayList<MutationOperation>(batch.getOperations());
                ordenarOperacoesMaisRecentesPrimeiro(operations);

                for (int i = 0; i < operations.size(); i++) {
                    MutationOperation current = operations.get(i);
                    if (operationMatchesPath(current, relativePath)) {
                        return current;
                    }
                }
            }
        }

        List<MutationBatch> batches = mutationStore.listarBatches();
        ordenarBatchesMaisRecentesPrimeiro(batches);

        for (int i = 0; i < batches.size(); i++) {
            MutationBatch batch = batches.get(i);
            if (batch == null || batch.getOperations() == null) {
                continue;
            }

            List<MutationOperation> operations = new ArrayList<MutationOperation>(batch.getOperations());
            ordenarOperacoesMaisRecentesPrimeiro(operations);

            for (int j = 0; j < operations.size(); j++) {
                MutationOperation current = operations.get(j);
                if (operationMatchesPath(current, relativePath)) {
                    return current;
                }
            }
        }

        return null;
    }

    /** * Localiza uma operacao por identificador global no historico de mutacao. * * @param operationId identificador da operacao * @return operacao encontrada ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private MutationOperation findOperationById(String operationId) {
        if (isBlank(operationId)) {
            return null;
        }

        List<MutationBatch> batches = mutationStore.listarBatches();
        for (int i = 0; i < batches.size(); i++) {
            MutationBatch batch = batches.get(i);
            if (batch == null || batch.getOperations() == null) {
                continue;
            }

            for (int j = 0; j < batch.getOperations().size(); j++) {
                MutationOperation operation = batch.getOperations().get(j);
                if (operation != null && safe(operationId).equals(safe(operation.getOperationId()))) {
                    return operation;
                }
            }
        }

        return null;
    }

    /** * Retorna true quando a operacao corresponde ao path informado. * * @param operation operacao a validar * @param relativePath caminho relativo alvo * @return true quando houver correspondencia exata de caminho * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean operationMatchesPath(MutationOperation operation, String relativePath) {
        if (operation == null) {
            return false;
        }

        String operationRelativePath = normalizeRelativePath(operation.getRelativePath());
        return safe(operationRelativePath).equals(safe(relativePath));
    }

    /** * Carrega o conteudo atual do arquivo real no workspace. * * @param relativePath caminho relativo do arquivo * @return conteudo textual atual ou string vazia * * @throws Exception quando ocorrer falha de leitura * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String loadCurrentWorkspaceContent(String relativePath) throws Exception {
        File workspaceFile = new File(rootDirectory, normalizeRelativePath(relativePath));
        if (!workspaceFile.exists() || !workspaceFile.isFile()) {
            return "";
        }

        return Files.readString(workspaceFile.toPath(), StandardCharsets.UTF_8);
    }

    /** * Carrega o conteudo textual versionado de um arquivo a partir de um commit * especifico. * * @param commitId commit alvo * @param relativePath caminho relativo do arquivo no espelho interno * @return conteudo textual versionado ou string vazia * * @throws Exception quando ocorrer falha de leitura * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String loadContentFromCommit(String commitId, String relativePath) throws Exception {
        if (isBlank(commitId)) {
            return "";
        }

        return gitRepository.readFileContentAtCommit(commitId, normalizeRelativePath(relativePath));
    }

    /** * Monta o resumo de diff com heuristicas semanticas e diff textual curto. * * @param relativePath caminho relativo do arquivo * @param operation operacao selecionada * @param mode modo de comparacao * @param leftLabel nome do estado esquerdo * @param rightLabel nome do estado direito * @param leftContent conteudo esquerdo * @param rightContent conteudo direito * @param maxLines limite de linhas no trecho diff * @return resumo consolidado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private DiffSummary buildDiffSummary(String relativePath, MutationOperation operation, String mode, String leftLabel, String rightLabel, String leftContent, String rightContent, int maxLines) {

        String[] leftLines = splitLines(leftContent);
        String[] rightLines = splitLines(rightContent);

        int common = longestCommonPrefixLength(leftLines, rightLines)
                   + longestCommonSuffixLength(leftLines, rightLines, longestCommonPrefixLength(leftLines, rightLines));

        int linesAddedEstimate = Math.max(0, rightLines.length - common);
        int linesRemovedEstimate = Math.max(0, leftLines.length - common);

        boolean packageChanged = detectLinePresenceChange(leftLines, rightLines, "package ");
        boolean importsChanged = detectLineGroupChange(leftLines, rightLines, "import ");
        boolean classSignatureChanged = detectClassSignatureChange(leftLines, rightLines);
        boolean methodSignatureChanged = detectMethodSignatureChange(leftLines, rightLines);
        boolean annotationsChanged = detectAnnotationsChange(leftLines, rightLines);
        boolean xmlStructureChanged = detectXmlStructureChange(leftContent, rightContent);
        boolean queryEvidenceChanged = detectQueryEvidenceChange(leftContent, rightContent);

        String changeMagnitude = classifyMagnitude(linesAddedEstimate, linesRemovedEstimate,
                classSignatureChanged, methodSignatureChanged, xmlStructureChanged);

        String tacticalSummary = buildTacticalSummary(
                packageChanged,
                importsChanged,
                classSignatureChanged,
                methodSignatureChanged,
                annotationsChanged,
                xmlStructureChanged,
                queryEvidenceChanged,
                changeMagnitude
        );

        String diffSnippet = buildDiffSnippet(leftLines, rightLines, maxLines);

        DiffSummary summary = new DiffSummary();
        summary.relativePath = relativePath;
        summary.mode = mode;
        summary.leftLabel = leftLabel;
        summary.rightLabel = rightLabel;
        summary.operationId = operation != null ? safe(operation.getOperationId()) : "";
        summary.batchId = operation != null ? safe(operation.getBatchId()) : "";
        summary.changeMagnitude = changeMagnitude;
        summary.linesAddedEstimate = linesAddedEstimate;
        summary.linesRemovedEstimate = linesRemovedEstimate;
        summary.packageChanged = packageChanged;
        summary.importsChanged = importsChanged;
        summary.classSignatureChanged = classSignatureChanged;
        summary.methodSignatureChanged = methodSignatureChanged;
        summary.annotationsChanged = annotationsChanged;
        summary.xmlStructureChanged = xmlStructureChanged;
        summary.queryEvidenceChanged = queryEvidenceChanged;
        summary.tacticalSummary = tacticalSummary;
        summary.diffSnippet = diffSnippet;
        return summary;
    }

    /** * Gera relatorio textual final do resumo de diff. * * @param summary resumo consolidado * @return texto final do relatorio * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String formatSummary(DiffSummary summary) {
        StringBuilder sb = new StringBuilder();
        sb.append("Relatorio de Diff de Mutacao").append("\n");
        sb.append("path: ").append(safe(summary.relativePath)).append("\n");
        sb.append("mode: ").append(safe(summary.mode)).append("\n");
        sb.append("leftState: ").append(safe(summary.leftLabel)).append("\n");
        sb.append("rightState: ").append(safe(summary.rightLabel)).append("\n");
        sb.append("operationId: ").append(safe(summary.operationId)).append("\n");
        sb.append("batchId: ").append(safe(summary.batchId)).append("\n");
        sb.append("changeMagnitude: ").append(safe(summary.changeMagnitude)).append("\n");
        sb.append("linesAddedEstimate: ").append(summary.linesAddedEstimate).append("\n");
        sb.append("linesRemovedEstimate: ").append(summary.linesRemovedEstimate).append("\n");
        sb.append("packageChanged: ").append(summary.packageChanged ? "true" : "false").append("\n");
        sb.append("importsChanged: ").append(summary.importsChanged ? "true" : "false").append("\n");
        sb.append("classSignatureChanged: ").append(summary.classSignatureChanged ? "true" : "false").append("\n");
        sb.append("methodSignatureChanged: ").append(summary.methodSignatureChanged ? "true" : "false").append("\n");
        sb.append("annotationsChanged: ").append(summary.annotationsChanged ? "true" : "false").append("\n");
        sb.append("xmlStructureChanged: ").append(summary.xmlStructureChanged ? "true" : "false").append("\n");
        sb.append("queryEvidenceChanged: ").append(summary.queryEvidenceChanged ? "true" : "false").append("\n");
        sb.append("\n");
        sb.append("Resumo tatico:").append("\n");
        sb.append(summary.tacticalSummary).append("\n");
        sb.append("\n");
        sb.append("TrechoDiff:").append("\n");
        sb.append(summary.diffSnippet);
        return sb.toString();
    }

    /** * Construi um resumo tatico curto com base nas heuristicas detectadas. * * @param packageChanged alteracao de package * @param importsChanged alteracao de imports * @param classSignatureChanged alteracao de assinatura de classe * @param methodSignatureChanged alteracao de assinatura de metodo * @param annotationsChanged alteracao de anotacoes * @param xmlStructureChanged alteracao de estrutura XML * @param queryEvidenceChanged alteracao de evidencias de query * @param changeMagnitude magnitude agregada * @return texto resumido * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String buildTacticalSummary(boolean packageChanged, boolean importsChanged, boolean classSignatureChanged, boolean methodSignatureChanged, boolean annotationsChanged, boolean xmlStructureChanged, boolean queryEvidenceChanged, String changeMagnitude) {

        List<String> bullets = new ArrayList<String>();

        if (packageChanged) {
            bullets.add("- Houve alteracao de package.");
        }
        if (importsChanged) {
            bullets.add("- Houve ajuste de imports.");
        }
        if (classSignatureChanged) {
            bullets.add("- Houve alteracao de assinatura de classe.");
        }
        if (methodSignatureChanged) {
            bullets.add("- Houve alteracao de assinatura de metodo.");
        }
        if (annotationsChanged) {
            bullets.add("- Houve alteracao em anotacoes.");
        }
        if (xmlStructureChanged) {
            bullets.add("- Houve alteracao estrutural em XML ou marcacao semelhante.");
        }
        if (queryEvidenceChanged) {
            bullets.add("- Houve alteracao em evidencias de query, SQL, JPQL, HQL ou Criteria.");
        }

        if (bullets.isEmpty()) {
            bullets.add("- O diff parece concentrado em corpo textual e ajustes localizados.");
        }

        bullets.add("- Magnitude aparente da mudanca: " + changeMagnitude + ".");

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bullets.size(); i++) {
            sb.append(bullets.get(i)).append("\n");
        }
        return sb.toString().trim();
    }

    /** * Gera um trecho reduzido de diff textual usando comparacao simples por * linhas. * * @param leftLines linhas do estado esquerdo * @param rightLines linhas do estado direito * @param maxLines limite maximo de linhas exibidas * @return diff textual reduzido * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String buildDiffSnippet(String[] leftLines, String[] rightLines, int maxLines) {
        StringBuilder sb = new StringBuilder();
        int max = Math.max(leftLines.length, rightLines.length);
        int emitted = 0;

        for (int i = 0; i < max && emitted < maxLines; i++) {
            String left = i < leftLines.length ? leftLines[i] : null;
            String right = i < rightLines.length ? rightLines[i] : null;

            if (safe(left).equals(safe(right))) {
                continue;
            }

            if (left != null) {
                sb.append("- ").append(left).append("\n");
                emitted++;
                if (emitted >= maxLines) {
                    break;
                }
            }

            if (right != null) {
                sb.append("+ ").append(right).append("\n");
                emitted++;
                if (emitted >= maxLines) {
                    break;
                }
            }
        }

        if (sb.length() == 0) {
            sb.append("Nenhuma diferenca textual relevante foi detectada no limite de analise.");
        }

        return sb.toString().trim();
    }

    /** * Classifica a magnitude aparente da mudanca. * * @param linesAddedEstimate linhas adicionadas estimadas * @param linesRemovedEstimate linhas removidas estimadas * @param classSignatureChanged alteracao de assinatura de classe * @param methodSignatureChanged alteracao de assinatura de metodo * @param xmlStructureChanged alteracao estrutural XML * @return classificacao textual da magnitude * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String classifyMagnitude(int linesAddedEstimate, int linesRemovedEstimate, boolean classSignatureChanged, boolean methodSignatureChanged, boolean xmlStructureChanged) {

        int total = linesAddedEstimate + linesRemovedEstimate;

        if (classSignatureChanged || methodSignatureChanged || xmlStructureChanged) {
            if (total > 40) {
                return "ALTA";
            }
            return "MEDIA";
        }

        if (total <= 8) {
            return "BAIXA";
        }

        if (total <= 30) {
            return "MEDIA";
        }

        return "ALTA";
    }

    /** * Detecta mudanca de presenca de linhas iniciadas por um prefixo. * * @param leftLines linhas do estado esquerdo * @param rightLines linhas do estado direito * @param prefix prefixo a analisar * @return true quando houver mudanca relevante * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean detectLinePresenceChange(String[] leftLines, String[] rightLines, String prefix) {
        String left = collectLinesByPrefix(leftLines, prefix);
        String right = collectLinesByPrefix(rightLines, prefix);
        return !left.equals(right);
    }

    /** * Detecta mudanca em grupos de imports. * * @param leftLines linhas do estado esquerdo * @param rightLines linhas do estado direito * @param prefix prefixo do grupo * @return true quando houver mudanca relevante * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean detectLineGroupChange(String[] leftLines, String[] rightLines, String prefix) {
        return detectLinePresenceChange(leftLines, rightLines, prefix);
    }

    /** * Detecta alteracao de assinatura de classe por heuristica textual. * * @param leftLines linhas do estado esquerdo * @param rightLines linhas do estado direito * @return true quando houver alteracao aparente * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean detectClassSignatureChange(String[] leftLines, String[] rightLines) {
        String left = collectFirstSignatureLine(leftLines, " class ", " interface ", " enum ", " record ");
        String right = collectFirstSignatureLine(rightLines, " class ", " interface ", " enum ", " record ");
        return !safe(left).equals(safe(right));
    }

    /** * Detecta alteracao de assinatura de metodo por heuristica textual. * * @param leftLines linhas do estado esquerdo * @param rightLines linhas do estado direito * @return true quando houver alteracao aparente * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean detectMethodSignatureChange(String[] leftLines, String[] rightLines) {
        String left = collectMethodSignatureLines(leftLines);
        String right = collectMethodSignatureLines(rightLines);
        return !safe(left).equals(safe(right));
    }

    /** * Detecta alteracao em anotacoes por heuristica textual. * * @param leftLines linhas do estado esquerdo * @param rightLines linhas do estado direito * @return true quando houver alteracao em anotacoes * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean detectAnnotationsChange(String[] leftLines, String[] rightLines) {
        String left = collectAnnotationLines(leftLines);
        String right = collectAnnotationLines(rightLines);
        return !safe(left).equals(safe(right));
    }

    /** * Detecta alteracao estrutural de XML por heuristica simples. * * @param leftContent conteudo esquerdo * @param rightContent conteudo direito * @return true quando houver alteracao aparente em tags XML * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean detectXmlStructureChange(String leftContent, String rightContent) {
        boolean leftHasXml = leftContent != null && leftContent.contains("<") && leftContent.contains(">");
        boolean rightHasXml = rightContent != null && rightContent.contains("<") && rightContent.contains(">");

        if (leftHasXml != rightHasXml) {
            return true;
        }

        String leftTags = extractXmlLikeTokens(leftContent);
        String rightTags = extractXmlLikeTokens(rightContent);
        return !safe(leftTags).equals(safe(rightTags));
    }

    /** * Detecta alteracao em evidencias de query, SQL, JPQL, HQL ou Criteria. * * @param leftContent conteudo esquerdo * @param rightContent conteudo direito * @return true quando houver mudanca aparente * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean detectQueryEvidenceChange(String leftContent, String rightContent) {
        String left = extractQueryEvidence(leftContent);
        String right = extractQueryEvidence(rightContent);
        return !safe(left).equals(safe(right));
    }

    /** * Divide um texto em linhas preservando comportamento defensivo para valor * nulo. * * @param content conteudo textual * @return vetor de linhas * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String[] splitLines(String content) {
        if (content == null || content.length() == 0) {
            return new String[0];
        }

        String normalized = content.replace("\r\n", "\n").replace("\r", "\n");
        return normalized.split("\n", -1);
    }

    /** * Calcula o tamanho do prefixo comum entre dois vetores de linhas. * * @param left linhas do estado esquerdo * @param right linhas do estado direito * @return tamanho do prefixo comum * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private int longestCommonPrefixLength(String[] left, String[] right) {
        int max = Math.min(left.length, right.length);
        int count = 0;

        for (int i = 0; i < max; i++) {
            if (safe(left[i]).equals(safe(right[i]))) {
                count++;
            } else {
                break;
            }
        }

        return count;
    }

    /** * Calcula o tamanho do sufixo comum entre dois vetores de linhas, * respeitando o prefixo ja considerado. * * @param left linhas do estado esquerdo * @param right linhas do estado direito * @param prefix tamanho do prefixo comum ja consumido * @return tamanho do sufixo comum * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private int longestCommonSuffixLength(String[] left, String[] right, int prefix) {
        int max = Math.min(left.length, right.length);
        int count = 0;

        while (count < (max - prefix)) {
            String leftValue = left[left.length - 1 - count];
            String rightValue = right[right.length - 1 - count];

            if (safe(leftValue).equals(safe(rightValue))) {
                count++;
            } else {
                break;
            }
        }

        return count;
    }

    /** * Coleta todas as linhas iniciadas por um prefixo especifico. * * @param lines vetor de linhas * @param prefix prefixo procurado * @return texto agregado das linhas encontradas * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String collectLinesByPrefix(String[] lines, String prefix) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i] != null ? lines[i].trim() : "";
            if (trimmed.startsWith(prefix)) {
                sb.append(trimmed).append("\n");
            }
        }
        return sb.toString().trim();
    }

    /** * Coleta a primeira linha que aparenta ser assinatura de tipo. * * @param lines vetor de linhas * @param markers marcadores de assinatura * @return primeira linha compativel ou string vazia * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String collectFirstSignatureLine(String[] lines, String... markers) {
        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i] != null ? lines[i].trim() : "";
            for (int j = 0; j < markers.length; j++) {
                if (trimmed.contains(markers[j])) {
                    return trimmed;
                }
            }
        }
        return "";
    }

    /** * Coleta linhas que aparentam ser assinaturas de metodo. * * @param lines vetor de linhas * @return texto agregado das assinaturas provaveis * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String collectMethodSignatureLines(String[] lines) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i] != null ? lines[i].trim() : "";
            if (looksLikeMethodSignature(trimmed)) {
                sb.append(trimmed).append("\n");
            }
        }

        return sb.toString().trim();
    }

    /** * Retorna true quando a linha aparenta ser assinatura de metodo Java. * * @param line linha a verificar * @return true quando a heuristica considerar assinatura de metodo * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean looksLikeMethodSignature(String line) {
        if (isBlank(line)) {
            return false;
        }

        if (!line.contains("(") || !line.contains(")")) {
            return false;
        }

        if (line.startsWith("if ") || line.startsWith("for ") || line.startsWith("while ")
                || line.startsWith("switch ") || line.startsWith("catch ")) {
            return false;
        }

        return line.contains("public ")
                || line.contains("private ")
                || line.contains("protected ")
                || line.startsWith("void ")
                || line.contains(" throws ")
                || line.endsWith("{");
    }

    /** * Coleta linhas de anotacao. * * @param lines vetor de linhas * @return texto agregado das anotacoes * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String collectAnnotationLines(String[] lines) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i] != null ? lines[i].trim() : "";
            if (trimmed.startsWith("@")) {
                sb.append(trimmed).append("\n");
            }
        }

        return sb.toString().trim();
    }

    /** * Extrai tokens aparentes de estrutura XML. * * @param content conteudo textual * @return texto agregado de tags encontradas * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String extractXmlLikeTokens(String content) {
        if (isBlank(content)) {
            return "";
        }

        String[] lines = splitLines(content);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i] != null ? lines[i].trim() : "";
            if (trimmed.startsWith("<") && trimmed.contains(">")) {
                sb.append(trimmed).append("\n");
            }
        }

        return sb.toString().trim();
    }

    /** * Extrai evidencias de query por heuristica textual simples. * * @param content conteudo textual * @return texto agregado das evidencias * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String extractQueryEvidence(String content) {
        if (isBlank(content)) {
            return "";
        }

        String lower = content.toLowerCase();
        StringBuilder sb = new StringBuilder();

        appendIfContains(sb, lower, "select ");
        appendIfContains(sb, lower, "insert ");
        appendIfContains(sb, lower, "update ");
        appendIfContains(sb, lower, "delete ");
        appendIfContains(sb, lower, "from ");
        appendIfContains(sb, lower, "where ");
        appendIfContains(sb, lower, "createquery");
        appendIfContains(sb, lower, "createcriteria");
        appendIfContains(sb, lower, "criteria");
        appendIfContains(sb, lower, "jpql");
        appendIfContains(sb, lower, "hql");
        appendIfContains(sb, lower, "preparedstatement");
        appendIfContains(sb, lower, "resultset");

        return sb.toString().trim();
    }

    /** * Adiciona marcador textual ao buffer quando o conteudo contiver o trecho * procurado. * * @param sb buffer de destino * @param content conteudo base * @param marker marcador procurado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void appendIfContains(StringBuilder sb, String content, String marker) {
        if (content.contains(marker)) {
            sb.append(marker).append("\n");
        }
    }

    /** * Ordena batches do mais recente para o mais antigo. * * @param batches lista de batches * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void ordenarBatchesMaisRecentesPrimeiro(List<MutationBatch> batches) {
        Collections.sort(batches, new Comparator<MutationBatch>() {
            @Override
            public int compare(MutationBatch a, MutationBatch b) {
                return Long.compare(b.getStartedAt(), a.getStartedAt());
            }
        });
    }

    /** * Ordena operacoes do mais recente para o mais antigo. * * @param operations lista de operacoes * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void ordenarOperacoesMaisRecentesPrimeiro(List<MutationOperation> operations) {
        Collections.sort(operations, new Comparator<MutationOperation>() {
            @Override
            public int compare(MutationOperation a, MutationOperation b) {
                return Long.compare(b.getCreatedAt(), a.getCreatedAt());
            }
        });
    }

    /** * Normaliza o modo de comparacao com fallback seguro. * * @param mode modo original * @return modo normalizado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String normalizeMode(String mode) {
        if (isBlank(mode)) {
            return "before_after";
        }

        String normalized = mode.trim().toLowerCase();
        if ("before_after".equals(normalized)
                || "current_before".equals(normalized)
                || "current_after".equals(normalized)) {
            return normalized;
        }

        return "before_after";
    }

    /** * Normaliza um caminho relativo para o formato com barras normais. * * @param relativePath caminho relativo original * @return caminho normalizado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String normalizeRelativePath(String relativePath) {
        if (relativePath == null) {
            return "";
        }

        String normalized = relativePath.trim().replace("\\", "/");
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    /** * Retorna string segura nao nula. * * @param value valor original * @return string segura * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    /** * Retorna true quando o valor informado for nulo ou vazio. * * @param value valor a validar * @return true quando o valor estiver em branco * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    /** * DTO interno de resumo de diff. * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private static class DiffSummary {
        private String relativePath;
        private String mode;
        private String leftLabel;
        private String rightLabel;
        private String operationId;
        private String batchId;
        private String changeMagnitude;
        private int linesAddedEstimate;
        private int linesRemovedEstimate;
        private boolean packageChanged;
        private boolean importsChanged;
        private boolean classSignatureChanged;
        private boolean methodSignatureChanged;
        private boolean annotationsChanged;
        private boolean xmlStructureChanged;
        private boolean queryEvidenceChanged;
        private String tacticalSummary;
        private String diffSnippet;
    }
}