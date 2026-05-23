package com.mcp.sailibrary.plugin.agent.context.analise;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.security.MessageDigest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/** * Gerencia a memoria persistente por projeto em diretorio local do usuario, * com contexto de branch e dados estruturais reutilizaveis. * * <p>Esta implementacao foi reforcada para reduzir fragmentacao de memoria em * projetos Maven multimodulo e em workspaces com mais de um `.project`. A * chave do projeto e a raiz semantica da memoria agora sao baseadas em um * diretorio de identidade mais estavel, priorizando: * <ul> * <li>o `.project` mais proximo</li> * <li>ou o `pom.xml` mais proximo</li> * <li>ou, em ultimo caso, o diretorio originalmente informado</li> * </ul> * </p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class ProjectMemoryStore {

    private File projectRootDirectory;
    private ProjectMemoryPaths memoryPaths;
    private ProjectMemoryJsonSupport jsonSupport;

    /** * Inicializa a estrutura de memoria persistente para o projeto informado. * * @param projectRootDirectory raiz original informada * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public ProjectMemoryStore(File projectRootDirectory) {
        this.projectRootDirectory = resolverDiretorioIdentidade(projectRootDirectory);
        this.jsonSupport = new ProjectMemoryJsonSupport();
        this.memoryPaths = new ProjectMemoryPaths(gerarProjectKey(this.projectRootDirectory));
    }

    /** * Registra uma memoria basica do projeto com raiz segura e metadados * minimos. * * @param safeRoot raiz segura percebida * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void registrarProjectMemoryBasica(String safeRoot) {
        JsonObject root = jsonSupport.lerJson(memoryPaths.getProjectMemoryFile());

        root.addProperty("projectKey", gerarProjectKey(projectRootDirectory));
        root.addProperty("projectRoot", normalizarCaminho(projectRootDirectory));
        root.addProperty("safeRoot", valorSeguro(safeRoot));
        root.addProperty("lastUpdatedAt", String.valueOf(System.currentTimeMillis()));

        jsonSupport.gravarJson(memoryPaths.getProjectMemoryFile(), root);
    }

    /** * Retorna um resumo curto da memoria persistente para enriquecer a * instrucao da IA. * * @return resumo textual da memoria do projeto * * @author Renato Tomaz Nati * @since 2026-05-20 */
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
        if (discoveredPatterns.has("patternsAparentes")) {
            resumo.append("patternsAparentes: ").append(discoveredPatterns.get("patternsAparentes").toString()).append("\n");
        }

        return resumo.toString();
    }

    /** * Garante que os arquivos basicos existam em disco com estrutura inicial. * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void inicializarEstrutura() {
        criarSeAusente(memoryPaths.getProjectMemoryFile(), criarProjectMemoryInicial());
        criarSeAusente(memoryPaths.getToolHistoryFile(), criarToolHistoryInicial());
        criarSeAusente(memoryPaths.getDependencySnapshotFile(), criarDependencySnapshotInicial());
        criarSeAusente(memoryPaths.getDiscoveredPatternsFile(), criarDiscoveredPatternsInicial());
        criarSeAusente(memoryPaths.getBranchContextFile(), criarBranchContextInicial());
    }

    /** * Atualiza o contexto da branch atual percebida no projeto. * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void atualizarBranchContexto() {
        JsonObject branchContext = jsonSupport.lerJson(memoryPaths.getBranchContextFile());
        String branchAtual = detectarBranchAtual();

        String ultimaBranch = "";
        if (branchContext.has("currentBranch")) {
            ultimaBranch = branchContext.get("currentBranch").getAsString();
        }

        branchContext.addProperty("projectKey", gerarProjectKey(projectRootDirectory));
        branchContext.addProperty("projectRoot", normalizarCaminho(projectRootDirectory));
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

    /** * Registra informacoes estruturais mais estaveis do projeto. * * @param safeRoot raiz segura percebida * @param buildTool ferramenta de build * @param javaVersion versao Java percebida * @param groupId groupId percebido * * @author Renato Tomaz Nati * @since 2026-05-20 */
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

    /** * Registra um snapshot de dependencias e frameworks percebidos. * * @param dependencies dependencias detectadas * @param frameworks frameworks detectados * @param modules modulos detectados * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void registrarDependencySnapshot(JsonArray dependencies, JsonArray frameworks, JsonArray modules) {
        JsonObject root = jsonSupport.lerJson(memoryPaths.getDependencySnapshotFile());

        root.addProperty("projectKey", gerarProjectKey(projectRootDirectory));
        root.addProperty("projectRoot", normalizarCaminho(projectRootDirectory));
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

    /** * Registra historico compacto da ultima execucao de ferramenta. * * @param toolName nome da ferramenta * @param parametersSummary resumo dos parametros * @param resultSummary resumo do resultado * * @author Renato Tomaz Nati * @since 2026-05-20 */
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

    /** * Registra ou atualiza um pattern estrutural na memoria persistente. * * @param kind categoria do pattern * @param key chave do pattern * @param value valor do pattern * @param evidence evidencia resumida * @param confidence nivel de confianca * * @author Renato Tomaz Nati * @since 2026-05-20 */
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

    /** * Retorna o resolver de caminhos da memoria persistente. * * @return paths da memoria persistente * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public ProjectMemoryPaths getMemoryPaths() {
        return memoryPaths;
    }

    /** * Retorna o diretorio de identidade efetivo usado por esta memoria. * * @return diretorio identidade do projeto * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File getProjectRootDirectory() {
        return projectRootDirectory;
    }

    /** * Garante a criacao inicial de um arquivo JSON quando ele ainda nao existir. * * @param arquivo arquivo alvo * @param conteudoInicial conteudo inicial * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void criarSeAusente(File arquivo, JsonObject conteudoInicial) {
        if (arquivo == null) {
            return;
        }

        if (!arquivo.exists()) {
            jsonSupport.gravarJson(arquivo, conteudoInicial);
        }
    }

    /** * Cria o JSON inicial de memoria estrutural do projeto. * * @return objeto inicial * * @author Renato Tomaz Nati * @since 2026-05-20 */
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

    /** * Cria o JSON inicial de historico de ferramentas. * * @return objeto inicial * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private JsonObject criarToolHistoryInicial() {
        JsonObject root = new JsonObject();
        root.addProperty("projectKey", gerarProjectKey(projectRootDirectory));
        root.add("history", new JsonArray());
        return root;
    }

    /** * Cria o JSON inicial de snapshot de dependencias. * * @return objeto inicial * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private JsonObject criarDependencySnapshotInicial() {
        JsonObject root = new JsonObject();
        root.addProperty("projectKey", gerarProjectKey(projectRootDirectory));
        root.add("dependencies", new JsonArray());
        root.add("frameworkHints", new JsonArray());
        root.add("modules", new JsonArray());
        root.addProperty("lastUpdatedAt", "");
        return root;
    }

    /** * Cria o JSON inicial de patterns descobertos. * * @return objeto inicial * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private JsonObject criarDiscoveredPatternsInicial() {
        JsonObject root = new JsonObject();
        root.addProperty("projectKey", gerarProjectKey(projectRootDirectory));
        root.add("patterns", new JsonArray());
        root.add("patternsAparentes", new JsonArray());
        return root;
    }

    /** * Cria o JSON inicial de branch context. * * @return objeto inicial * * @author Renato Tomaz Nati * @since 2026-05-20 */
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

    /** * Remove um pattern de uma lista JSON pelo valor da chave. * * @param lista lista JSON * @param chave chave do pattern * * @author Renato Tomaz Nati * @since 2026-05-20 */
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

    /** * Atualiza ou insere um pattern em uma lista JSON. * * @param lista lista alvo * @param kind categoria * @param key chave * @param value valor * @param evidence evidencia * @param confidence nivel de confianca * * @author Renato Tomaz Nati * @since 2026-05-20 */
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

    /** * Detecta a branch atual subindo a arvore a partir do diretorio identidade * ate encontrar um `.git/HEAD`. * * @return branch atual ou string vazia * * @author Renato Tomaz Nati * @since 2026-05-20 */
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

    /** * Resolve o diretorio de identidade do projeto, priorizando o `.project` * mais proximo e depois o `pom.xml` mais proximo. * * @param originalRoot raiz original recebida * @return diretorio identidade mais estavel para memoria persistente * * @author Renato Tomaz Nati * @since 2026-05-20 */
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

    /** * Localiza o `.project` mais proximo subindo a arvore. * * @param start ponto inicial * @return diretorio contendo `.project` ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
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

    /** * Localiza o modulo Maven mais proximo subindo a arvore. * * @param start ponto inicial * @return diretorio contendo `pom.xml` ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
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

    /** * Gera a chave estavel do projeto a partir do diretorio identidade. * * @param rootDirectory diretorio identidade * @return chave estavel do projeto * * @author Renato Tomaz Nati * @since 2026-05-20 */
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

    /** * Gera hash curto deterministico. * * @param valor valor base * @return hash curto * * @author Renato Tomaz Nati * @since 2026-05-20 */
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

    /** * Normaliza nome para uso em chave persistente. * * @param nome nome original * @return nome normalizado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String normalizarNome(String nome) {
        if (nome == null || nome.trim().length() == 0) {
            return "project";
        }

        String normalizado = nome.toLowerCase();
        normalizado = normalizado.replaceAll("[^a-z0-9_\\-]", "_");
        return normalizado;
    }

    /** * Converte arquivo em caminho normalizado. * * @param arquivo arquivo alvo * @return caminho normalizado * * @author Renato Tomaz Nati * @since 2026-05-20 */
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

    /** * Retorna valor seguro nao nulo. * * @param valor valor original * @return valor seguro * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String valorSeguro(String valor) {
        if (valor == null) {
            return "";
        }
        return valor;
    }
}