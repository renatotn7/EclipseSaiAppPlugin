package com.mcp.sailibrary.plugin.agent.context.analise;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.security.MessageDigest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/** * --- * yaml_header: * version: "1.0" * dependencies: * - java.io.File * - java.security.MessageDigest * - com.google.gson * purpose: "Gerenciar a memoria persistente por projeto em diretorio local do usuario, com contexto de branch e dados estruturais reutilizaveis." * design_pattern: "Facade / Repository" * --- */
public class ProjectMemoryStore {

    private File projectRootDirectory;
    private ProjectMemoryPaths memoryPaths;
    private ProjectMemoryJsonSupport jsonSupport;

    /**
 * Inicializa a estrutura de memoria persistente para o projeto informado.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
    public ProjectMemoryStore(File projectRootDirectory) {
        this.projectRootDirectory = projectRootDirectory;
        this.jsonSupport = new ProjectMemoryJsonSupport();
        this.memoryPaths = new ProjectMemoryPaths(gerarProjectKey(projectRootDirectory));
    }
    public void registrarProjectMemoryBasica(String safeRoot) {
        JsonObject root = jsonSupport.lerJson(memoryPaths.getProjectMemoryFile());

        root.addProperty("projectKey", gerarProjectKey(projectRootDirectory));
        root.addProperty("projectRoot", projectRootDirectory != null ? projectRootDirectory.getAbsolutePath() : "");
        root.addProperty("safeRoot", valorSeguro(safeRoot));
        root.addProperty("lastUpdatedAt", String.valueOf(System.currentTimeMillis()));

        jsonSupport.gravarJson(memoryPaths.getProjectMemoryFile(), root);
    }
    /**
 * Retorna um resumo curto da memoria persistente para enriquecer a instrucao da IA.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-18
 */
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
        if (projectMemory.has("javaVersion")) {
            resumo.append("javaVersion: ").append(projectMemory.get("javaVersion").getAsString()).append("\n");
        }
        if (projectMemory.has("groupId")) {
            resumo.append("groupId: ").append(projectMemory.get("groupId").getAsString()).append("\n");
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

        return resumo.toString();
    }
    /**
 * Garante que os arquivos basicos existam em disco com estrutura inicial.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
    public void inicializarEstrutura() {
        criarSeAusente(memoryPaths.getProjectMemoryFile(), criarProjectMemoryInicial());
        criarSeAusente(memoryPaths.getToolHistoryFile(), criarToolHistoryInicial());
        criarSeAusente(memoryPaths.getDependencySnapshotFile(), criarDependencySnapshotInicial());
        criarSeAusente(memoryPaths.getDiscoveredPatternsFile(), criarDiscoveredPatternsInicial());
        criarSeAusente(memoryPaths.getBranchContextFile(), criarBranchContextInicial());
    }

    /**
 * Atualiza o contexto da branch atual percebida no projeto.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
    public void atualizarBranchContexto() {
        JsonObject branchContext = jsonSupport.lerJson(memoryPaths.getBranchContextFile());
        String branchAtual = detectarBranchAtual();

        String ultimaBranch = "";
        if (branchContext.has("currentBranch")) {
            ultimaBranch = branchContext.get("currentBranch").getAsString();
        }

        branchContext.addProperty("projectKey", gerarProjectKey(projectRootDirectory));
        branchContext.addProperty("projectRoot", projectRootDirectory != null ? projectRootDirectory.getAbsolutePath() : "");
        branchContext.addProperty("lastSeenAt", String.valueOf(System.currentTimeMillis()));
        branchContext.addProperty("currentBranch", branchAtual);

        if (ultimaBranch != null && ultimaBranch.length() > 0 && !ultimaBranch.equals(branchAtual)) {
            branchContext.addProperty("previousBranch", ultimaBranch);
            branchContext.addProperty("reconfirmSensitiveHints", "true");
        } else if (!branchContext.has("reconfirmSensitiveHints")) {
            branchContext.addProperty("reconfirmSensitiveHints", "false");
        }

        jsonSupport.gravarJson(memoryPaths.getBranchContextFile(), branchContext);
    }

 

    /**
 * Registra informacoes estruturais mais estaveis do projeto.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
    public void registrarProjectMemory(String safeRoot, String buildTool, String javaVersion, String groupId) {
        JsonObject root = jsonSupport.lerJson(memoryPaths.getProjectMemoryFile());

        root.addProperty("projectKey", gerarProjectKey(projectRootDirectory));
        root.addProperty("projectRoot", projectRootDirectory != null ? projectRootDirectory.getAbsolutePath() : "");
        root.addProperty("safeRoot", valorSeguro(safeRoot));
        root.addProperty("buildTool", valorSeguro(buildTool));
        root.addProperty("javaVersion", valorSeguro(javaVersion));
        root.addProperty("groupId", valorSeguro(groupId));
        root.addProperty("lastUpdatedAt", String.valueOf(System.currentTimeMillis()));

        jsonSupport.gravarJson(memoryPaths.getProjectMemoryFile(), root);
    }

    /**
 * Registra um snapshot de dependencias e frameworks percebidos.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
    public void registrarDependencySnapshot(JsonArray dependencies, JsonArray frameworks, JsonArray modules) {
        JsonObject root = jsonSupport.lerJson(memoryPaths.getDependencySnapshotFile());

        root.addProperty("projectKey", gerarProjectKey(projectRootDirectory));
        root.addProperty("projectRoot", projectRootDirectory != null ? projectRootDirectory.getAbsolutePath() : "");
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

    /**
 * Registra historico compacto da ultima execucao de ferramenta.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
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

    public ProjectMemoryPaths getMemoryPaths() {
        return memoryPaths;
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
        root.addProperty("projectRoot", projectRootDirectory != null ? projectRootDirectory.getAbsolutePath() : "");
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
        root.add("dependencies", new JsonArray());
        root.add("frameworkHints", new JsonArray());
        root.add("modules", new JsonArray());
        root.addProperty("lastUpdatedAt", "");
        return root;
    }

    /**
     * ---
     * yaml_header:
     * version: "1.2"
     * dependencies:
     * - java.io.File
     * - java.io.BufferedReader
     * - java.io.FileReader
     * - java.security.MessageDigest
     * - com.google.gson.JsonObject
     * - com.google.gson.JsonArray
     * purpose: "Gerenciar a memoria persistente por projeto adicionando suporte a segregacao tática entre padroes efetivos e padroes aparentes suspeitos com rastreabilidade de origem."
     * design_pattern: "Repository / Facade"
     * ---
     */

    // [METODO MODIFICADO]
    // Data: 2026-05-16 20:30:00
    // Caller: camada de controle do plugin ou ferramentas da IA
    // Callee: ProjectMemoryJsonSupport.lerJson, ProjectMemoryJsonSupport.gravarJson
    // Objetivo: Feature para registrar ou atualizar padroes estruturais, suportando a promocao de padroes aparentes para efetivos com mapeamento de caminhos comuns e evidencias.
    public void registrarPattern(String kind, String key, String value, String evidence, String confidence) {
        if (key == null || key.trim().length() == 0) {
            return;
        }

        JsonObject root = jsonSupport.lerJson(memoryPaths.getDiscoveredPatternsFile());
        
        // Garante a existencia das duas matrizes de conhecimento tatico
        if (!root.has("patterns") || !root.get("patterns").isJsonArray()) {
            root.add("patterns", new JsonArray());
        }
        if (!root.has("patternsAparentes") || !root.get("patternsAparentes").isJsonArray()) {
            root.add("patternsAparentes", new JsonArray());
        }

        JsonArray patternsEfetivos = root.getAsJsonArray("patterns");
        JsonArray patternsAparentes = root.getAsJsonArray("patternsAparentes");

        // Se a confianca for alta, removemos das suspeitas (se existia) e promovemos para efetivo
     // Se a confianca for alta, removemos das suspeitas (se existia) e promovemos para efetivo
        if ("alta".equalsIgnoreCase(confidence) || "confirmado".equalsIgnoreCase(confidence)) {
            removerPadraoDaLista(patternsAparentes, key);
            atualizarOuInserirPadrao(patternsEfetivos, kind, key, value, evidence, confidence);
        } else {
            // Feature: Exclusao mutua militar. Se a confianca for baixa/aparente, expurga da base de efetivos (caso tenha sido rebaixada) e mantem nas suspeitas.
            removerPadraoDaLista(patternsEfetivos, key);
            atualizarOuInserirPadrao(patternsAparentes, kind, key, value, evidence, confidence);
        }

        jsonSupport.gravarJson(memoryPaths.getDiscoveredPatternsFile(), root);
    }

    /**
 * Descrição não fornecida.
 *
 * @author Renato Tomaz Nati
 */
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

    /**
 * Descrição não fornecida.
 *
 * @author Renato Tomaz Nati
 */
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
                
                // Calcula e insere caminhos relativos provaveis a partir da raiz se houver evidencia de arquivo
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

    // [METODO MODIFICADO]
    // Data: 2026-05-16 20:40:00
    // Caller: ProjectMemoryStore.inicializarEstrutura
    // Callee: nenhum
    // Objetivo: Atualizar o construtor do JSON inicial de padroes para incluir a estrutura de suspeitas.
    private JsonObject criarDiscoveredPatternsInicial() {
        JsonObject root = new JsonObject();
        root.addProperty("projectKey", gerarProjectKey(projectRootDirectory));
        root.add("patterns", new JsonArray());
        root.add("patternsAparentes", new JsonArray()); // Matriz de suspeitas iniciais da IA
        return root;
    }

    private JsonObject criarBranchContextInicial() {
        JsonObject root = new JsonObject();
        root.addProperty("projectKey", gerarProjectKey(projectRootDirectory));
        root.addProperty("projectRoot", projectRootDirectory != null ? projectRootDirectory.getAbsolutePath() : "");
        root.addProperty("currentBranch", "");
        root.addProperty("previousBranch", "");
        root.addProperty("reconfirmSensitiveHints", "false");
        root.addProperty("lastSeenAt", "");
        return root;
    }

    private String detectarBranchAtual() {
        if (projectRootDirectory == null) {
            return "";
        }

        File gitHead = new File(projectRootDirectory, ".git/HEAD");
        if (!gitHead.exists()) {
            return "";
        }

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

    private String normalizarNome(String nome) {
        if (nome == null || nome.trim().length() == 0) {
            return "project";
        }

        String normalizado = nome.toLowerCase();
        normalizado = normalizado.replaceAll("[^a-z0-9_\\-]", "_");
        return normalizado;
    }

    private String valorSeguro(String valor) {
        if (valor == null) {
            return "";
        }
        return valor;
    }
}