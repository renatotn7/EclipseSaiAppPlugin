package com.mcp.sailibrary.plugin.agent.context.analise;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Date;
import java.security.MessageDigest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/** * Gerencia a memoria persistente por projeto em diretorio local do usuario, * com contexto de branch e dados estruturais reutilizaveis. * * <p>Esta implementacao foi reforcada para reduzir fragmentacao de memoria em * projetos Maven multimodulo e em workspaces com mais de um `.project`. A * chave do projeto e a raiz semantica da memoria agora sao baseadas em um * diretorio de identidade mais estavel.</p> * * <p>Tambem foi ajustada para: * - suportar registro explicito de branch_context * - persistir javaVersion/groupId em dependency_snapshot * - resumir javaVersion/groupId com fallback entre project_memory e dependency_snapshot * - nao sobrescrever branch registrada manualmente quando a deteccao automatica * nao encontrar `.git/HEAD` no ambiente atual</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class ProjectMemoryStore {

    private File projectRootDirectory;
    private ProjectMemoryPaths memoryPaths;
    private ProjectMemoryJsonSupport jsonSupport;

    /** * Inicializa a estrutura de memoria persistente para o projeto informado. * * @param projectRootDirectory raiz original informada */
    public ProjectMemoryStore(File projectRootDirectory) {
        this.projectRootDirectory = resolverDiretorioIdentidade(projectRootDirectory);
        this.jsonSupport = new ProjectMemoryJsonSupport();
        this.memoryPaths = new ProjectMemoryPaths(gerarProjectKey(this.projectRootDirectory));
    }

    /**
     * Inicializa a memoria persistente a partir de caminho textual.
     *
     * @param projectRootDirectory caminho da raiz do projeto
     */
    public ProjectMemoryStore(String projectRootDirectory) {
        this(projectRootDirectory != null && projectRootDirectory.trim().length() > 0
                ? new File(projectRootDirectory)
                : null);
    }


    /** * Registra uma memoria basica do projeto com raiz segura e metadados * minimos. * * @param safeRoot raiz segura percebida */
    public void registrarProjectMemoryBasica(String safeRoot) {
        JsonObject root = jsonSupport.lerJson(memoryPaths.getProjectMemoryFile());

        root.addProperty("projectKey", gerarProjectKey(projectRootDirectory));
        root.addProperty("projectRoot", normalizarCaminho(projectRootDirectory));
        root.addProperty("safeRoot", valorSeguro(safeRoot));
        root.addProperty("lastUpdatedAt", String.valueOf(System.currentTimeMillis()));

        jsonSupport.gravarJson(memoryPaths.getProjectMemoryFile(), root);
    }

    /** * Retorna um resumo curto da memoria persistente para enriquecer a * instrucao da IA. * * @return resumo textual da memoria do projeto */
    public String consultarResumoMemoria() {
        JsonObject projectMemory = jsonSupport.lerJson(memoryPaths.getProjectMemoryFile());
        JsonObject branchContext = jsonSupport.lerJson(memoryPaths.getBranchContextFile());
        JsonObject dependencySnapshot = jsonSupport.lerJson(memoryPaths.getDependencySnapshotFile());
        JsonObject discoveredPatterns = jsonSupport.lerJson(memoryPaths.getDiscoveredPatternsFile());

        StringBuilder resumo = new StringBuilder();

        if (projectMemory.has("projectRoot")) {
            resumo.append("projectRoot: ").append(projectMemory.get("projectRoot").getAsString()).append("\n");
        }
        if (projectMemory.has("safeRoot")) {
            resumo.append("safeRoot: ").append(projectMemory.get("safeRoot").getAsString()).append("\n");
        }
        if (projectMemory.has("buildTool")) {
            resumo.append("buildTool: ").append(projectMemory.get("buildTool").getAsString()).append("\n");
        }

        String javaVersion = "";
        if (projectMemory.has("javaVersion")) {
            javaVersion = projectMemory.get("javaVersion").getAsString();
        }
        if ((javaVersion == null || javaVersion.length() == 0) && dependencySnapshot.has("javaVersion")) {
            javaVersion = dependencySnapshot.get("javaVersion").getAsString();
        }
        if (javaVersion != null && javaVersion.length() > 0) {
            resumo.append("javaVersion: ").append(javaVersion).append("\n");
        }

        String groupId = "";
        if (projectMemory.has("groupId")) {
            groupId = projectMemory.get("groupId").getAsString();
        }
        if ((groupId == null || groupId.length() == 0) && dependencySnapshot.has("groupId")) {
            groupId = dependencySnapshot.get("groupId").getAsString();
        }
        if (groupId != null && groupId.length() > 0) {
            resumo.append("groupId: ").append(groupId).append("\n");
        }

        if (branchContext.has("currentBranch")) {
            resumo.append("currentBranch: ").append(branchContext.get("currentBranch").getAsString()).append("\n");
        }
        if (branchContext.has("reconfirmSensitiveHints")) {
            resumo.append("reconfirmSensitiveHints: ").append(branchContext.get("reconfirmSensitiveHints").getAsString()).append("\n");
        }
        if (dependencySnapshot.has("frameworkHints")) {
            resumo.append("frameworkHints: ").append(dependencySnapshot.get("frameworkHints").toString()).append("\n");
        }
        if (dependencySnapshot.has("modules")) {
            resumo.append("modules: ").append(dependencySnapshot.get("modules").toString()).append("\n");
        }
        if (discoveredPatterns.has("patterns")) {
            resumo.append("patterns: ").append(discoveredPatterns.get("patterns").toString()).append("\n");
        }
        if (discoveredPatterns.has("patternsAparentes")) {
            resumo.append("patternsAparentes: ").append(discoveredPatterns.get("patternsAparentes").toString()).append("\n");
        }

        return resumo.toString();
    }

    /** * Garante que os arquivos basicos existam em disco com estrutura inicial. */
    public void inicializarEstrutura() {
        criarSeAusente(memoryPaths.getProjectMemoryFile(), criarProjectMemoryInicial());
        criarSeAusente(memoryPaths.getToolHistoryFile(), criarToolHistoryInicial());
        criarSeAusente(memoryPaths.getDependencySnapshotFile(), criarDependencySnapshotInicial());
        criarSeAusente(memoryPaths.getDiscoveredPatternsFile(), criarDiscoveredPatternsInicial());
        criarSeAusente(memoryPaths.getBranchContextFile(), criarBranchContextInicial());
    }

    /** * Atualiza o contexto da branch atual percebida no projeto. * * <p>Regra importante: * se a deteccao automatica nao encontrar branch alguma, o metodo preserva a * branch ja registrada anteriormente, evitando apagar contexto valido * informado manualmente.</p> */
    public void atualizarBranchContexto() {
        JsonObject branchContext = jsonSupport.lerJson(memoryPaths.getBranchContextFile());
        String branchAtualDetectada = detectarBranchAtual();

        String branchRegistrada = "";
        if (branchContext.has("currentBranch")) {
            branchRegistrada = branchContext.get("currentBranch").getAsString();
        }

        branchContext.addProperty("projectKey", gerarProjectKey(projectRootDirectory));
        branchContext.addProperty("projectRoot", normalizarCaminho(projectRootDirectory));
        branchContext.addProperty("lastSeenAt", String.valueOf(System.currentTimeMillis()));

        if (branchAtualDetectada == null || branchAtualDetectada.trim().length() == 0) {
            if (branchRegistrada == null || branchRegistrada.trim().length() == 0) {
                branchContext.addProperty("currentBranch", "");
                if (!branchContext.has("reconfirmSensitiveHints")) {
                    branchContext.addProperty("reconfirmSensitiveHints", "false");
                }
            } else {
                branchContext.addProperty("currentBranch", branchRegistrada);
                if (!branchContext.has("reconfirmSensitiveHints")) {
                    branchContext.addProperty("reconfirmSensitiveHints", "false");
                }
            }

            jsonSupport.gravarJson(memoryPaths.getBranchContextFile(), branchContext);
            return;
        }

        branchContext.addProperty("currentBranch", branchAtualDetectada);

        if (branchRegistrada != null
                && branchRegistrada.length() > 0
                && !branchRegistrada.equals(branchAtualDetectada)) {
            branchContext.addProperty("previousBranch", branchRegistrada);
            branchContext.addProperty("reconfirmSensitiveHints", "true");
        } else if (!branchContext.has("reconfirmSensitiveHints")) {
            branchContext.addProperty("reconfirmSensitiveHints", "false");
        }

        jsonSupport.gravarJson(memoryPaths.getBranchContextFile(), branchContext);
    }

    /** * Registra contexto explicito de branch. * * @param currentBranch branch atual * @param reconfirmSensitiveHints indicador textual */
    public void registrarBranchContext(String currentBranch, String reconfirmSensitiveHints) {
        JsonObject branchContext = jsonSupport.lerJson(memoryPaths.getBranchContextFile());

        String ultimaBranch = "";
        if (branchContext.has("currentBranch")) {
            ultimaBranch = branchContext.get("currentBranch").getAsString();
        }

        String branchSegura = valorSeguro(currentBranch);
        String reconfirmSeguro = valorSeguro(reconfirmSensitiveHints);

        branchContext.addProperty("projectKey", gerarProjectKey(projectRootDirectory));
        branchContext.addProperty("projectRoot", normalizarCaminho(projectRootDirectory));
        branchContext.addProperty("lastSeenAt", String.valueOf(System.currentTimeMillis()));
        branchContext.addProperty("currentBranch", branchSegura);

        if (ultimaBranch != null && ultimaBranch.length() > 0 && !ultimaBranch.equals(branchSegura)) {
            branchContext.addProperty("previousBranch", ultimaBranch);
            if (reconfirmSeguro.length() == 0) {
                branchContext.addProperty("reconfirmSensitiveHints", "true");
            }
        }

        if (reconfirmSeguro.length() > 0) {
            branchContext.addProperty("reconfirmSensitiveHints", reconfirmSeguro);
        } else if (!branchContext.has("reconfirmSensitiveHints")) {
            branchContext.addProperty("reconfirmSensitiveHints", "false");
        }

        jsonSupport.gravarJson(memoryPaths.getBranchContextFile(), branchContext);
    }

    /** * Registra informacoes estruturais mais estaveis do projeto. * * @param safeRoot raiz segura percebida * @param buildTool ferramenta de build * @param javaVersion versao Java percebida * @param groupId groupId percebido */
    public void registrarProjectMemory(String safeRoot, String buildTool, String javaVersion, String groupId) {
        JsonObject root = jsonSupport.lerJson(memoryPaths.getProjectMemoryFile());

        root.addProperty("projectKey", gerarProjectKey(projectRootDirectory));
        root.addProperty("projectRoot", normalizarCaminho(projectRootDirectory));
        root.addProperty("safeRoot", valorSeguro(safeRoot));
        root.addProperty("buildTool", valorSeguro(buildTool));
        root.addProperty("javaVersion", valorSeguro(javaVersion));
        root.addProperty("groupId", valorSeguro(groupId));
        root.addProperty("lastUpdatedAt", String.valueOf(System.currentTimeMillis()));

        jsonSupport.gravarJson(memoryPaths.getProjectMemoryFile(), root);
    }

    /** * Registra um snapshot de dependencias e frameworks percebidos. * * <p>Metodo mantido por compatibilidade com chamadas antigas.</p> * * @param dependencies dependencias detectadas * @param frameworks frameworks detectados * @param modules modulos detectados */
    public void registrarDependencySnapshot(JsonArray dependencies, JsonArray frameworks, JsonArray modules) {
        registrarDependencySnapshot(dependencies, frameworks, modules, "", "");
    }

    /** * Registra um snapshot de dependencias, frameworks e metadados estruturais. * * @param dependencies dependencias detectadas * @param frameworks frameworks detectados * @param modules modulos detectados * @param javaVersion versao Java percebida * @param groupId groupId percebido */
    public void registrarDependencySnapshot(JsonArray dependencies, JsonArray frameworks, JsonArray modules, String javaVersion, String groupId) {
        JsonObject root = jsonSupport.lerJson(memoryPaths.getDependencySnapshotFile());

        root.addProperty("projectKey", gerarProjectKey(projectRootDirectory));
        root.addProperty("projectRoot", normalizarCaminho(projectRootDirectory));
        root.addProperty("javaVersion", valorSeguro(javaVersion));
        root.addProperty("groupId", valorSeguro(groupId));
        root.addProperty("lastUpdatedAt", String.valueOf(System.currentTimeMillis()));

        if (dependencies != null) {
            root.add("dependencies", dependencies);
        }

        if (frameworks != null) {
            root.add("frameworkHints", frameworks);
        }

        if (modules != null) {
            root.add("modules", modules);
        }

        jsonSupport.gravarJson(memoryPaths.getDependencySnapshotFile(), root);
    }

    /** * Registra historico compacto da ultima execucao de ferramenta. * * @param toolName nome da ferramenta * @param parametersSummary resumo dos parametros * @param resultSummary resumo do resultado */
    public void registrarToolHistory(String toolName, String parametersSummary, String resultSummary) {
        JsonObject root = jsonSupport.lerJson(memoryPaths.getToolHistoryFile());
        JsonArray history;

        if (root.has("history") && root.get("history").isJsonArray()) {
            history = root.getAsJsonArray("history");
        } else {
            history = new JsonArray();
            root.add("history", history);
        }

        JsonObject registro = new JsonObject();
        registro.addProperty("tool", valorSeguro(toolName));
        registro.addProperty("parametersSummary", valorSeguro(parametersSummary));
        registro.addProperty("resultSummary", valorSeguro(resultSummary));
        registro.addProperty("timestamp", String.valueOf(System.currentTimeMillis()));

        history.add(registro);

        while (history.size() > 30) {
            history.remove(0);
        }

        jsonSupport.gravarJson(memoryPaths.getToolHistoryFile(), root);
    }

    /** * Registra ou atualiza um pattern estrutural na memoria persistente. * * @param kind categoria do pattern * @param key chave do pattern * @param value valor do pattern * @param evidence evidencia resumida * @param confidence nivel de confianca */
    public void registrarPattern(String kind, String key, String value, String evidence, String confidence) {
        if (key == null || key.trim().length() == 0) {
            return;
        }

        JsonObject root = jsonSupport.lerJson(memoryPaths.getDiscoveredPatternsFile());

        if (!root.has("patterns") || !root.get("patterns").isJsonArray()) {
            root.add("patterns", new JsonArray());
        }
        if (!root.has("patternsAparentes") || !root.get("patternsAparentes").isJsonArray()) {
            root.add("patternsAparentes", new JsonArray());
        }

        JsonArray patternsEfetivos = root.getAsJsonArray("patterns");
        JsonArray patternsAparentes = root.getAsJsonArray("patternsAparentes");

        if ("alta".equalsIgnoreCase(confidence) || "confirmado".equalsIgnoreCase(confidence)) {
            removerPadraoDaLista(patternsAparentes, key);
            atualizarOuInserirPadrao(patternsEfetivos, kind, key, value, evidence, confidence);
        } else {
            removerPadraoDaLista(patternsEfetivos, key);
            atualizarOuInserirPadrao(patternsAparentes, kind, key, value, evidence, confidence);
        }

        jsonSupport.gravarJson(memoryPaths.getDiscoveredPatternsFile(), root);
    }

    /** * Retorna o resolver de caminhos da memoria persistente. * * @return paths da memoria persistente */
    public ProjectMemoryPaths getMemoryPaths() {
        return memoryPaths;
    }

    /** * Retorna o diretorio de identidade efetivo usado por esta memoria. * * @return diretorio identidade do projeto */
    public File getProjectRootDirectory() {
        return projectRootDirectory;
    }

    private void criarSeAusente(File arquivo, JsonObject conteudoInicial) {
        if (arquivo == null) {
            return;
        }

        if (!arquivo.exists()) {
            jsonSupport.gravarJson(arquivo, conteudoInicial);
        }
    }

    private JsonObject criarProjectMemoryInicial() {
        JsonObject root = new JsonObject();
        root.addProperty("projectKey", gerarProjectKey(projectRootDirectory));
        root.addProperty("projectRoot", normalizarCaminho(projectRootDirectory));
        root.addProperty("safeRoot", "");
        root.addProperty("buildTool", "");
        root.addProperty("javaVersion", "");
        root.addProperty("groupId", "");
        root.add("modules", new JsonArray());
        root.add("frameworkHints", new JsonArray());
        root.add("conventions", new JsonArray());
        root.addProperty("lastUpdatedAt", "");
        return root;
    }

    private JsonObject criarToolHistoryInicial() {
        JsonObject root = new JsonObject();
        root.addProperty("projectKey", gerarProjectKey(projectRootDirectory));
        root.add("history", new JsonArray());
        return root;
    }

    private JsonObject criarDependencySnapshotInicial() {
        JsonObject root = new JsonObject();
        root.addProperty("projectKey", gerarProjectKey(projectRootDirectory));
        root.addProperty("javaVersion", "");
        root.addProperty("groupId", "");
        root.add("dependencies", new JsonArray());
        root.add("frameworkHints", new JsonArray());
        root.add("modules", new JsonArray());
        root.addProperty("lastUpdatedAt", "");
        return root;
    }

    private JsonObject criarDiscoveredPatternsInicial() {
        JsonObject root = new JsonObject();
        root.addProperty("projectKey", gerarProjectKey(projectRootDirectory));
        root.add("patterns", new JsonArray());
        root.add("patternsAparentes", new JsonArray());
        return root;
    }

    private JsonObject criarBranchContextInicial() {
        JsonObject root = new JsonObject();
        root.addProperty("projectKey", gerarProjectKey(projectRootDirectory));
        root.addProperty("projectRoot", normalizarCaminho(projectRootDirectory));
        root.addProperty("currentBranch", "");
        root.addProperty("previousBranch", "");
        root.addProperty("reconfirmSensitiveHints", "false");
        root.addProperty("lastSeenAt", "");
        return root;
    }

    private void removerPadraoDaLista(JsonArray lista, String chave) {
        if (lista == null || chave == null) {
            return;
        }

        for (int i = 0; i < lista.size(); i++) {
            JsonObject atual = lista.get(i).getAsJsonObject();
            if (atual.has("key") && chave.equals(atual.get("key").getAsString())) {
                lista.remove(i);
                break;
            }
        }
    }

    private void atualizarOuInserirPadrao(JsonArray lista, String kind, String key, String value, String evidence, String confidence) {
        boolean atualizado = false;

        for (int i = 0; i < lista.size(); i++) {
            JsonObject atual = lista.get(i).getAsJsonObject();
            if (atual.has("key") && key.equals(atual.get("key").getAsString())) {
                atual.addProperty("kind", valorSeguro(kind));
                atual.addProperty("value", valorSeguro(value));
                atual.addProperty("evidence", valorSeguro(evidence));
                atual.addProperty("confidence", valorSeguro(confidence));
                atual.addProperty("lastSeenAt", String.valueOf(System.currentTimeMillis()));

                if (evidence != null && evidence.contains("/")) {
                    atual.addProperty("caminhoComumRaiz", valorSeguro(evidence));
                }

                atualizado = true;
                break;
            }
        }

        if (!atualizado) {
            JsonObject novo = new JsonObject();
            novo.addProperty("kind", valorSeguro(kind));
            novo.addProperty("key", valorSeguro(key));
            novo.addProperty("value", valorSeguro(value));
            novo.addProperty("evidence", valorSeguro(evidence));
            novo.addProperty("confidence", valorSeguro(confidence));
            novo.addProperty("lastSeenAt", String.valueOf(System.currentTimeMillis()));

            if (evidence != null && evidence.contains("/")) {
                novo.addProperty("caminhoComumRaiz", valorSeguro(evidence));
            }

            lista.add(novo);
        }
    }

    private String detectarBranchAtual() {
        if (projectRootDirectory == null) {
            return "";
        }

        File cursor = projectRootDirectory;
        while (cursor != null && cursor.exists()) {
            File gitHead = new File(cursor, ".git/HEAD");
            if (gitHead.exists() && gitHead.isFile()) {
                BufferedReader bufferedReader = null;
                try {
                    bufferedReader = new BufferedReader(new FileReader(gitHead));
                    String linha = bufferedReader.readLine();
                    if (linha == null) {
                        return "";
                    }

                    linha = linha.trim();
                    String prefixo = "ref: refs/heads/";
                    if (linha.startsWith(prefixo)) {
                        return linha.substring(prefixo.length());
                    }

                    return linha;
                } catch (Exception e) {
                    return "";
                } finally {
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (Exception e) {
                        }
                    }
                }
            }

            cursor = cursor.getParentFile();
        }

        return "";
    }

    private File resolverDiretorioIdentidade(File originalRoot) {
        if (originalRoot == null) {
            return null;
        }

        File start = originalRoot;
        if (start.isFile()) {
            start = start.getParentFile();
        }

        File projectRootMaisProximo = localizarProjectRootMaisProximo(start);
        if (projectRootMaisProximo != null) {
            return projectRootMaisProximo;
        }

        File moduloMavenMaisProximo = localizarModuloMavenMaisProximo(start);
        if (moduloMavenMaisProximo != null) {
            return moduloMavenMaisProximo;
        }

        return originalRoot;
    }

    private File localizarProjectRootMaisProximo(File start) {
        File cursor = start;

        while (cursor != null && cursor.exists()) {
            File projectFile = new File(cursor, ".project");
            if (projectFile.exists() && projectFile.isFile()) {
                return cursor;
            }

            cursor = cursor.getParentFile();
        }

        return null;
    }

    private File localizarModuloMavenMaisProximo(File start) {
        File cursor = start;

        while (cursor != null && cursor.exists()) {
            File pom = new File(cursor, "pom.xml");
            if (pom.exists() && pom.isFile()) {
                return cursor;
            }

            cursor = cursor.getParentFile();
        }

        return null;
    }

    private String gerarProjectKey(File rootDirectory) {
        if (rootDirectory == null) {
            return "unknown_project";
        }

        try {
            String canonicalPath = rootDirectory.getCanonicalPath();
            String nomeBase = rootDirectory.getName();
            String hash = gerarHashCurto(canonicalPath);
            return normalizarNome(nomeBase) + "_" + hash;
        } catch (Exception e) {
            return normalizarNome(rootDirectory.getName()) + "_fallback";
        }
    }

    private String gerarHashCurto(String valor) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] digest = messageDigest.digest(valor.getBytes("UTF-8"));

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


    /**
     * Registra um padrao estrutural estavel descoberto automaticamente.
     *
     * @param path caminho relativo usado pela ferramenta
     * @param kind categoria do padrao
     * @param key chave do padrao
     * @param value valor detectado
     * @param evidence evidencia resumida
     * @param confidence confianca numerica
     */
    public void recordStablePattern(String path, String kind, String key, String value, String evidence, double confidence) {
        String confidenceText = confidence >= 0.80d ? "alta" : "media";
        StringBuilder evidenceBuilder = new StringBuilder();

        if (evidence != null && evidence.trim().length() > 0) {
            evidenceBuilder.append(evidence.trim());
        }
        if (path != null && path.trim().length() > 0) {
            if (evidenceBuilder.length() > 0) {
                evidenceBuilder.append(" | ");
            }
            evidenceBuilder.append("path=").append(path.trim());
        }

        registrarPattern(kind, key, value, evidenceBuilder.toString(), confidenceText);
    }

    /**
     * Persiste o resultado resumido de agent tools de analise em JSONL.
     *
     * @param toolName nome da ferramenta
     * @param parametersJson parametros em JSON
     * @param resultText resultado bruto ou resumido
     */
    public void registrarAgentToolAnalise(String toolName, String parametersJson, String resultText) {
        if (toolName == null || toolName.trim().length() == 0) {
            return;
        }

        try {
            File memoryDir = memoryPaths.getProjectDirectory();
            if (!memoryDir.exists()) {
                memoryDir.mkdirs();
            }

            File history = new File(memoryDir, "agent-tools-analysis.jsonl");
            String resumo = resumirTexto(resultText, 1200);
            String registro = "{"
                    + "\"timestamp\":\"" + escapeJson(new Date().toString()) + "\","
                    + "\"projectRoot\":\"" + escapeJson(normalizarCaminho(projectRootDirectory)) + "\","
                    + "\"tool\":\"" + escapeJson(toolName) + "\","
                    + "\"parameters\":" + normalizarJsonObjeto(parametersJson) + ","
                    + "\"resultSummary\":\"" + escapeJson(resumo) + "\""
                    + "}" + System.lineSeparator();

            FileWriter writer = new FileWriter(history, true);
            try {
                writer.write(registro);
            } finally {
                writer.close();
            }

            System.out.println("[MCP MEMORY DEBUG] agentToolAnalysis.persisted=true | file="
                    + history.getAbsolutePath() + " | tool=" + toolName);
        } catch (Exception e) {
            System.out.println("[MCP MEMORY DEBUG] agentToolAnalysis.persisted=false | tool="
                    + toolName + " | error=" + e.getMessage());
        }
    }

    private static String normalizarJsonObjeto(String json) {
        if (json == null) {
            return "{}";
        }
        String trimmed = json.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }
        return "{\"raw\":\"" + escapeJson(trimmed) + "\"}";
    }

    private static String resumirTexto(String texto, int limite) {
        if (texto == null) {
            return "";
        }
        String normalizado = texto.replace('\r', ' ').replace('\n', ' ').trim();
        if (normalizado.length() <= limite) {
            return normalizado;
        }
        return normalizado.substring(0, limite) + "...";
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", " ")
                .replace("\n", " ");
    }

    private String normalizarNome(String nome) {
        if (nome == null || nome.trim().length() == 0) {
            return "project";
        }

        String normalizado = nome.toLowerCase();
        normalizado = normalizado.replaceAll("[^a-z0-9_\\-]", "_");
        return normalizado;
    }

    private String normalizarCaminho(File arquivo) {
        if (arquivo == null) {
            return "";
        }

        try {
            return arquivo.getCanonicalPath().replace("\\", "/");
        } catch (Exception e) {
            return arquivo.getAbsolutePath().replace("\\", "/");
        }
    }

    private String valorSeguro(String valor) {
        if (valor == null) {
            return "";
        }
        return valor;
    }
}