package com.mcp.sailibrary.plugin.agent.tools.exploration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolParameterMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadataProvider;
import com.mcp.sailibrary.plugin.agent.tools.support.ToolJsonSupport;

/** * Realiza busca textual profunda em arquivos legiveis dentro da raiz segura do * projeto. * * <p>Esta ferramenta foi desenhada para localizar rapidamente anotacoes, * variaveis, configuracoes, nomes de metodos, trechos SQL, referencias XML e * outros indícios textuais relevantes sem abrir todo o conteudo do projeto no * contexto da IA.</p> * * <p>O comportamento e defensivo: * <ul> * <li>impede path traversal</li> * <li>restringe a busca a raiz segura</li> * <li>ignora diretorios de ruido tecnico</li> * <li>limita a quantidade maxima de ocorrencias retornadas</li> * <li>faz falha isolada silenciosa por arquivo para preservar a varredura</li> * </ul> * </p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class ProjectTextSearchTool implements AgentTool, AgentToolPromptMetadataProvider {

    private static final int DEFAULT_MAX_RESULTS = 20;
    private static final int MAX_ALLOWED_RESULTS = 200;

    private final File rootDirectory;

    /** * Inicializa a ferramenta com a raiz segura do projeto. * * @param rootDirectory raiz segura do projeto atual * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public ProjectTextSearchTool(File rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public String getName() {
        return "buscar_texto_projeto";
    }

    /** * Executa busca textual recursiva dentro do perimetro seguro do projeto. * * <p>O parametro {@code termo} e obrigatorio. O parametro {@code path} e * opcional e permite restringir a regiao inicial da busca. O retorno e * textual e limitado por quantidade maxima de ocorrencias.</p> * * @param jsonParameters parametros JSON da ferramenta * @return relatorio textual de ocorrencias encontradas * * @author Renato Tomaz Nati * @since 2026-05-20 */
    @Override
    public String execute(String jsonParameters) {
        if (rootDirectory == null || !rootDirectory.exists() || !rootDirectory.isDirectory()) {
            return "Erro Operacional: A raiz segura do projeto esta indisponivel para busca textual.";
        }

        String searchTerm = ToolJsonSupport.extractJsonStringValue(jsonParameters, "termo");
        String requestedPath = ToolJsonSupport.extractJsonStringValue(jsonParameters, "path");
        int maxResults = ToolJsonSupport.extractJsonIntValue(
                jsonParameters,
                "limite",
                DEFAULT_MAX_RESULTS,
                1,
                MAX_ALLOWED_RESULTS
        );

        if (isBlank(searchTerm)) {
            return "Erro Operacional: O parametro 'termo' de busca e obrigatorio.";
        }

        try {
            File startPoint = resolveStartPoint(requestedPath);
            if (startPoint == null || !startPoint.exists()) {
                return "Erro Operacional: O diretorio base para busca nao existe ou nao pode ser acessado com seguranca.";
            }

            List<String> results = new ArrayList<String>();
            searchRecursively(startPoint, searchTerm, results, maxResults);

            StringBuilder report = new StringBuilder();
            report.append("Relatorio de Busca Tatica").append("\n");
            report.append("root: ").append(normalizePath(rootDirectory)).append("\n");
            report.append("pathSolicitado: ").append(safe(requestedPath)).append("\n");
            report.append("pathResolvido: ").append(normalizePath(startPoint)).append("\n");
            report.append("termo: ").append(searchTerm).append("\n");
            report.append("limiteAplicado: ").append(maxResults).append("\n");
            report.append("ocorrenciasEncontradas: ").append(results.size()).append("\n");

            if (results.isEmpty()) {
                report.append("\n");
                report.append("Nenhuma ocorrencia do termo [").append(searchTerm)
                      .append("] foi encontrada nos arquivos legiveis da regiao demarcada.");
                return report.toString();
            }

            report.append("\n");
            report.append("Ocorrencias Encontradas").append("\n");
            for (int i = 0; i < results.size(); i++) {
                report.append(results.get(i)).append("\n");
            }

            if (results.size() >= maxResults) {
                report.append("\n");
                report.append("[AVISO]: A busca foi interrompida pois atingiu o limite de seguranca. Podem existir mais ocorrencias.");
            }

            return report.toString().trim();
        } catch (Exception e) {
            return "Falha ao realizar busca textual no projeto: " + e.getMessage();
        }
    }

    @Override
    public AgentToolPromptMetadata getPromptMetadata() {
        AgentToolPromptMetadata metadata = new AgentToolPromptMetadata();
        metadata.setToolName(getName());
        metadata.setOneLinePurpose("Localizar ocorrencias textuais relevantes em arquivos legiveis do projeto.");
        metadata.setActivityDescription("Varredura profunda linha a linha em extensoes seguras.");

        AgentToolParameterMetadata termo = new AgentToolParameterMetadata();
        termo.setName("termo");
        termo.setRequired(true);
        termo.setDescription("Texto exato ou fragmento textual a ser buscado.");
        termo.setExampleValue("@Transactional");
        metadata.addParameter(termo);

        AgentToolParameterMetadata path = new AgentToolParameterMetadata();
        path.setName("path");
        path.setRequired(false);
        path.setDescription("Diretorio relativo inicial da busca dentro da raiz segura.");
        path.setExampleValue("src/main/java");
        metadata.addParameter(path);

        AgentToolParameterMetadata limite = new AgentToolParameterMetadata();
        limite.setName("limite");
        limite.setRequired(false);
        limite.setDescription("Quantidade maxima de ocorrencias retornadas.");
        limite.setExampleValue("20");
        metadata.addParameter(limite);

        metadata.addRecommendedUseCase("Use quando precisar localizar rapidamente uma anotacao, variavel, metodo, string ou configuracao no projeto.");
        metadata.addRecommendedUseCase("Use quando houver suspeita de XML, SQL, properties, annotations ou referencias indiretas fora do trecho atual.");
        metadata.addRecommendedUseCase("Use quando o caminho exato do arquivo ainda nao estiver claro, mas o termo de busca ja for conhecido.");

        metadata.addGuardrail("A busca deve permanecer dentro da raiz segura do projeto.");
        metadata.addGuardrail("Diretorios de build e ruido tecnico nao devem ser tratados como fonte principal de verdade.");
        metadata.addGuardrail("Resultados devem ser limitados para proteger o contexto da IA.");

        metadata.addJsonExample(
                "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"buscar_texto_projeto\\\",\\\"parameters\\\":{\\\"termo\\\":\\\"@Transactional\\\",\\\"path\\\":\\\"src/main/java\\\",\\\"limite\\\":\\\"20\\\"},\\\"explanation\\\":\\\"Preciso localizar ocorrencias reais do termo no projeto antes de concluir a analise.\\\"}"
        );

        return metadata;
    }

    /** * Resolve o ponto inicial da busca com base no path informado, respeitando * a raiz segura do projeto. * * @param requestedPath caminho relativo solicitado * @return ponto inicial resolvido ou null quando o path for inseguro * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private File resolveStartPoint(String requestedPath) {
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

    /** * Realiza a varredura recursiva a partir da pasta informada. * * @param folder pasta atual da recursao * @param term termo de busca * @param results lista acumuladora de resultados * @param maxResults quantidade maxima de ocorrencias * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void searchRecursively(File folder, String term, List<String> results, int maxResults) {
        if (results.size() >= maxResults) {
            return;
        }

        if (folder == null || !folder.exists()) {
            return;
        }

        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }

        for (int i = 0; i < files.length; i++) {
            if (results.size() >= maxResults) {
                return;
            }

            File current = files[i];
            if (current.isDirectory()) {
                if (shouldIgnoreDirectory(current.getName())) {
                    continue;
                }
                searchRecursively(current, term, results, maxResults);
            } else {
                if (isReadableFile(current.getName())) {
                    inspectFile(current, term, results, maxResults);
                }
            }
        }
    }

    /** * Retorna true quando a pasta deve ser ignorada na varredura. * * @param directoryName nome da pasta * @return true quando a pasta for zona de exclusao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean shouldIgnoreDirectory(String directoryName) {
        if (directoryName == null) {
            return true;
        }

        return "target".equals(directoryName)
                || ".git".equals(directoryName)
                || "bin".equals(directoryName)
                || ".settings".equals(directoryName)
                || ".metadata".equals(directoryName);
    }

    /** * Retorna true quando o arquivo possui extensao legivel pela busca textual. * * @param fileName nome do arquivo * @return true quando a extensao for suportada * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean isReadableFile(String fileName) {
        String lower = fileName != null ? fileName.toLowerCase() : "";
        return lower.endsWith(".java")
                || lower.endsWith(".xml")
                || lower.endsWith(".properties")
                || lower.endsWith(".json")
                || lower.endsWith(".yaml")
                || lower.endsWith(".yml");
    }

    /** * Analisa um arquivo legivel linha a linha em busca do termo informado. * * @param file arquivo atual * @param term termo de busca * @param results lista acumuladora de resultados * @param maxResults quantidade maxima de ocorrencias * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void inspectFile(File file, String term, List<String> results, int maxResults) {
        BufferedReader bufferedReader = null;

        try {
            bufferedReader = new BufferedReader(new FileReader(file));
            String line;
            int lineNumber = 1;
            String relativePath = getRelativePath(file);

            while ((line = bufferedReader.readLine()) != null && results.size() < maxResults) {
                if (line.contains(term)) {
                    String occurrence = "Arquivo: "
                            + relativePath
                            + " | Linha "
                            + lineNumber
                            + ": "
                            + line.trim();

                    results.add(occurrence);
                }

                lineNumber++;
            }
        } catch (Exception e) {
            // Falha isolada em arquivo individual nao deve interromper a varredura.
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (Exception e) {
                }
            }
        }
    }

    /** * Retorna o caminho relativo do arquivo em relacao a raiz segura. * * @param file arquivo de origem * @return caminho relativo normalizado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String getRelativePath(File file) {
        if (file == null || rootDirectory == null) {
            return file != null ? file.getName() : "";
        }

        try {
            String rootPath = rootDirectory.getCanonicalPath().replace("\\", "/");
            String filePath = file.getCanonicalPath().replace("\\", "/");

            if (filePath.startsWith(rootPath)) {
                String relative = filePath.substring(rootPath.length());
                while (relative.startsWith("/")) {
                    relative = relative.substring(1);
                }
                return relative;
            }

            return file.getName();
        } catch (Exception e) {
            return file.getName();
        }
    }

    /** * Retorna true quando o arquivo candidato estiver fisicamente dentro da * raiz segura do projeto. * * @param candidate arquivo ou pasta candidata * @return true quando o candidato estiver dentro do perimetro seguro * * @author Renato Tomaz Nati * @since 2026-05-20 */
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