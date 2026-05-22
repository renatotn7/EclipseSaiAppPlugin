package com.mcp.sailibrary.plugin.agent.tools.exploration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolParameterMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadataProvider;
import com.mcp.sailibrary.plugin.agent.tools.support.ToolJsonSupport;

/** * Explora diretorios do projeto dentro da raiz segura e retorna uma visao * textual compacta da topografia encontrada. * * <p>Esta ferramenta foi desenhada para dar a IA uma leitura rapida da * estrutura de pastas e arquivos sem expor todo o conteudo do projeto. O foco * e navegacao tatica, delimitacao de perimetro e descoberta inicial de * caminhos relevantes para investigacao posterior.</p> * * <p>O comportamento e defensivo: * <ul> * <li>impede path traversal</li> * <li>restringe a exploracao a raiz segura</li> * <li>ignora diretorios de ruido tecnico</li> * <li>limita profundidade e quantidade de resultados</li> * </ul> * </p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class DirectoryExplorerTool implements AgentTool, AgentToolPromptMetadataProvider {

    private static final int DEFAULT_MAX_DEPTH = 3;
    private static final int MAX_ALLOWED_DEPTH = 6;
    private static final int DEFAULT_MAX_ENTRIES = 120;
    private static final int MAX_ALLOWED_ENTRIES = 300;

    private final File rootDirectory;

    /** * Inicializa a ferramenta com a raiz segura do projeto. * * @param rootDirectory raiz segura do projeto atual * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public DirectoryExplorerTool(File rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public String getName() {
        return "explorar_diretorio";
    }

    /** * Executa a exploracao controlada do diretorio solicitado. * * <p>O caminho informado deve ser relativo a raiz segura do projeto. O * retorno e textual e limitado por profundidade e quantidade maxima de * entradas para proteger o contexto da IA.</p> * * @param jsonParameters parametros JSON da ferramenta * @return relatorio textual de exploracao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    @Override
    public String execute(String jsonParameters) {
        if (rootDirectory == null || !rootDirectory.exists() || !rootDirectory.isDirectory()) {
            return "Erro Operacional: A raiz segura do projeto esta indisponivel para exploracao.";
        }

        String requestedPath = ToolJsonSupport.extractJsonStringValue(jsonParameters, "path");
        int maxDepth = ToolJsonSupport.extractJsonIntValue(
                jsonParameters,
                "maxDepth",
                DEFAULT_MAX_DEPTH,
                1,
                MAX_ALLOWED_DEPTH
        );
        int maxEntries = ToolJsonSupport.extractJsonIntValue(
                jsonParameters,
                "maxEntries",
                DEFAULT_MAX_ENTRIES,
                10,
                MAX_ALLOWED_ENTRIES
        );

        try {
            File startDirectory = resolveStartDirectory(requestedPath);
            if (startDirectory == null || !startDirectory.exists() || !startDirectory.isDirectory()) {
                return "Erro Operacional: O diretorio solicitado nao existe ou nao pode ser explorado com seguranca.";
            }

            List<String> entries = new ArrayList<String>();
            collectEntries(startDirectory, startDirectory, 0, maxDepth, maxEntries, entries);

            StringBuilder sb = new StringBuilder();
            sb.append("Relatorio de Exploracao de Diretorio").append("\n");
            sb.append("root: ").append(normalizePath(rootDirectory)).append("\n");
            sb.append("pathSolicitado: ").append(safe(requestedPath)).append("\n");
            sb.append("pathResolvido: ").append(normalizePath(startDirectory)).append("\n");
            sb.append("profundidadeAplicada: ").append(maxDepth).append("\n");
            sb.append("limiteEntradas: ").append(maxEntries).append("\n");
            sb.append("entradasEncontradas: ").append(entries.size()).append("\n");

            if (entries.isEmpty()) {
                sb.append("\n");
                sb.append("Nenhuma entrada relevante foi encontrada dentro dos filtros aplicados.");
                return sb.toString();
            }

            sb.append("\n");
            sb.append("Topografia Encontrada").append("\n");
            for (int i = 0; i < entries.size(); i++) {
                sb.append(entries.get(i)).append("\n");
            }

            return sb.toString().trim();
        } catch (Exception e) {
            return "Falha ao explorar diretorio: " + e.getMessage();
        }
    }

    @Override
    public AgentToolPromptMetadata getPromptMetadata() {
        AgentToolPromptMetadata metadata = new AgentToolPromptMetadata();
        metadata.setToolName(getName());
        metadata.setOneLinePurpose("Mapear a topografia de pastas dentro da raiz segura do projeto.");
        metadata.setActivityDescription("Mapeia topografia de pastas e arquivos dentro da raiz segura. Ignora ruidos de compilacao como target, bin, .git, .settings e .metadata. Previne path traversal e limita profundidade e volume de retorno.");

        AgentToolParameterMetadata path = new AgentToolParameterMetadata();
        path.setName("path");
        path.setRequired(false);
        path.setDescription("Caminho relativo inicial para exploracao a partir da raiz segura.");
        path.setExampleValue("src/main/java");
        metadata.addParameter(path);

        AgentToolParameterMetadata maxDepth = new AgentToolParameterMetadata();
        maxDepth.setName("maxDepth");
        maxDepth.setRequired(false);
        maxDepth.setDescription("Profundidade maxima da exploracao.");
        maxDepth.setExampleValue("3");
        metadata.addParameter(maxDepth);

        AgentToolParameterMetadata maxEntries = new AgentToolParameterMetadata();
        maxEntries.setName("maxEntries");
        maxEntries.setRequired(false);
        maxEntries.setDescription("Quantidade maxima de entradas retornadas.");
        maxEntries.setExampleValue("120");
        metadata.addParameter(maxEntries);

        metadata.addRecommendedUseCase("Use quando precisar mapear rapidamente a estrutura de pastas e arquivos antes de escolher outra ferramenta.");
        metadata.addRecommendedUseCase("Use quando houver duvida sobre a localizacao de arquivos, packages, modulos ou pastas relevantes.");
        metadata.addRecommendedUseCase("Use antes de leitura de arquivo quando o caminho real ainda nao estiver claro.");
        metadata.addRecommendedUseCase("Use para triagem inicial de topografia sem abrir arquivos desnecessariamente.");

        metadata.addGuardrail("A exploracao deve permanecer dentro da raiz segura do projeto.");
        metadata.addGuardrail("Diretorios de build e ruido tecnico nao devem ser tratados como fonte principal de verdade.");
        metadata.addGuardrail("Nao use esta ferramenta para ler conteudo de arquivo; use-a apenas para topografia.");

        metadata.addJsonExample(
                "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"explorar_diretorio\\\",\\\"parameters\\\":{\\\"path\\\":\\\"src/main/java\\\",\\\"maxDepth\\\":\\\"3\\\",\\\"maxEntries\\\":\\\"80\\\"},\\\"explanation\\\":\\\"Preciso mapear a topografia de diretorios e arquivos antes de continuar a investigacao.\\\"}"
        );

        return metadata;
    }

    /** * Resolve o diretorio inicial da exploracao a partir do path informado, * respeitando o perimetro da raiz segura. * * @param requestedPath caminho relativo solicitado * @return diretorio inicial resolvido ou null quando o path for invalido * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private File resolveStartDirectory(String requestedPath) {
        if (isBlank(requestedPath)) {
            return rootDirectory;
        }

        String normalizedRequestedPath = requestedPath.trim().replace("\\", "/");

        if (normalizedRequestedPath.contains("..")) {
            return null;
        }

        File candidate = new File(rootDirectory, normalizedRequestedPath);
        if (!isInsideRoot(candidate)) {
            return null;
        }

        return candidate;
    }

    /** * Coleta recursivamente entradas textuais da estrutura de diretorios. * * @param current diretorio atual * @param base diretorio base usado para gerar path relativo * @param currentDepth profundidade atual * @param maxDepth profundidade maxima permitida * @param maxEntries quantidade maxima de entradas acumuladas * @param entries lista acumuladora de saida * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void collectEntries(File current, File base, int currentDepth, int maxDepth, int maxEntries, List<String> entries) {

        if (current == null || !current.exists() || !current.isDirectory()) {
            return;
        }

        if (entries.size() >= maxEntries) {
            return;
        }

        File[] children = current.listFiles();
        if (children == null || children.length == 0) {
            return;
        }

        List<File> sortedChildren = new ArrayList<File>();
        for (int i = 0; i < children.length; i++) {
            sortedChildren.add(children[i]);
        }

        Collections.sort(sortedChildren, new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                if (a.isDirectory() && !b.isDirectory()) {
                    return -1;
                }
                if (!a.isDirectory() && b.isDirectory()) {
                    return 1;
                }
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });

        for (int i = 0; i < sortedChildren.size(); i++) {
            if (entries.size() >= maxEntries) {
                return;
            }

            File child = sortedChildren.get(i);
            if (shouldIgnore(child)) {
                continue;
            }

            String relativePath = buildRelativePath(base, child);
            String indent = buildIndent(currentDepth);

            if (child.isDirectory()) {
                entries.add(indent + "[DIR] " + relativePath);

                if (currentDepth < maxDepth) {
                    collectEntries(child, base, currentDepth + 1, maxDepth, maxEntries, entries);
                }
            } else {
                entries.add(indent + "[FILE] " + relativePath);
            }
        }
    }

    /** * Retorna true quando o arquivo ou diretorio deve ser ignorado na * exploracao. * * @param file candidato atual * @return true quando o item deve ser ignorado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean shouldIgnore(File file) {
        if (file == null) {
            return true;
        }

        String name = file.getName();
        if (name == null) {
            return true;
        }

        if (".git".equals(name)) {
            return true;
        }
        if ("target".equals(name)) {
            return true;
        }
        if ("bin".equals(name)) {
            return true;
        }
        if (".settings".equals(name)) {
            return true;
        }
        if (".metadata".equals(name)) {
            return true;
        }

        return false;
    }

    /** * Constroi o path relativo do item atual em relacao ao diretorio base. * * @param base diretorio base * @param child item atual * @return caminho relativo normalizado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String buildRelativePath(File base, File child) {
        try {
            String basePath = base.getCanonicalPath().replace("\\", "/");
            String childPath = child.getCanonicalPath().replace("\\", "/");

            if (childPath.startsWith(basePath)) {
                String relative = childPath.substring(basePath.length());
                while (relative.startsWith("/")) {
                    relative = relative.substring(1);
                }
                return relative.length() > 0 ? relative : child.getName();
            }
        } catch (Exception e) {
        }

        return child.getName();
    }

    /** * Gera recuo visual para representar profundidade na saida textual. * * @param depth profundidade atual * @return string de indentacao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String buildIndent(int depth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    /** * Retorna true quando o arquivo candidato estiver fisicamente dentro da * raiz segura do projeto. * * @param candidate arquivo ou diretorio candidato * @return true quando o item estiver dentro da raiz segura * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean isInsideRoot(File candidate) {
        if (candidate == null || rootDirectory == null) {
            return false;
        }

        try {
            String rootPath = rootDirectory.getCanonicalPath().replace("\\", "/");
            String candidatePath = candidate.getCanonicalPath().replace("\\", "/");
            return candidatePath.startsWith(rootPath);
        } catch (Exception e) {
            return false;
        }
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