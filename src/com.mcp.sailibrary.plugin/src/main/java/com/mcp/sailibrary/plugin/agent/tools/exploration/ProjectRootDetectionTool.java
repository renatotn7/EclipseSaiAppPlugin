package com.mcp.sailibrary.plugin.agent.tools.exploration;

import java.io.File;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolParameterMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadataProvider;
import com.mcp.sailibrary.plugin.agent.tools.support.ToolJsonSupport;

/** * Detecta a raiz segura do projeto a partir de um caminho informado ou da raiz * atual configurada para o workspace da IA. * * <p>Esta ferramenta foi desenhada para confirmar o perimetro seguro de * operacao antes de exploracao, busca textual, leitura de arquivo ou mutacao * de workspace. A deteccao sobe a arvore de diretorios procurando marcadores * como .git e .project.</p> * * <p>O comportamento e defensivo: * <ul> * <li>impede path traversal para fora da raiz segura</li> * <li>nao sai do perimetro conhecido do projeto</li> * <li>reconhece marcadores de raiz de projeto comuns ao seu ecossistema</li> * <li>usa fallback seguro para a raiz atual quando nao encontrar marcador * superior valido</li> * </ul> * </p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class ProjectRootDetectionTool implements AgentTool, AgentToolPromptMetadataProvider {

    private final File rootDirectory;

    /** * Inicializa a ferramenta com a raiz segura atual do projeto. * * @param rootDirectory raiz segura do projeto atual * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public ProjectRootDetectionTool(File rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public String getName() {
        return "verificar_raiz_projeto";
    }

    /** * Executa a deteccao da raiz segura do projeto a partir do caminho * informado. * * <p>Se o parametro {@code path} nao for informado, a busca parte da raiz * segura ja conhecida. O retorno textual informa o ponto solicitado, o * ponto efetivo de partida e a raiz segura detectada.</p> * * @param jsonParameters parametros JSON da ferramenta * @return relatorio textual da raiz detectada * * @author Renato Tomaz Nati * @since 2026-05-20 */
    @Override
    public String execute(String jsonParameters) {
        if (rootDirectory == null || !rootDirectory.exists() || !rootDirectory.isDirectory()) {
            return "Erro Operacional: A raiz segura do projeto esta indisponivel para verificacao.";
        }

        String requestedPath = ToolJsonSupport.extractJsonStringValue(jsonParameters, "path");

        try {
            File startPoint = resolveStartPoint(requestedPath);
            if (startPoint == null || !startPoint.exists()) {
                return "Erro Operacional: O caminho solicitado nao existe ou nao pode ser analisado com seguranca.";
            }

            File safeRoot = detectSafeProjectRoot(startPoint);

            StringBuilder sb = new StringBuilder();
            sb.append("Relatorio de Verificacao de Raiz do Projeto").append("\n");
            sb.append("rootAtual: ").append(normalizePath(rootDirectory)).append("\n");
            sb.append("pathSolicitado: ").append(safe(requestedPath)).append("\n");
            sb.append("pontoPartidaResolvido: ").append(normalizePath(startPoint)).append("\n");
            sb.append("raizSeguraDetectada: ").append(normalizePath(safeRoot)).append("\n");

            if (sameDirectory(rootDirectory, safeRoot)) {
                sb.append("status: A raiz segura atual ja coincide com a melhor raiz detectada.");
            } else {
                sb.append("status: Foi detectado um diretorio raiz superior dentro do perimetro seguro.");
            }

            return sb.toString();
        } catch (Exception e) {
            return "Falha ao verificar raiz do projeto: " + e.getMessage();
        }
    }

    @Override
    public AgentToolPromptMetadata getPromptMetadata() {
        AgentToolPromptMetadata metadata = new AgentToolPromptMetadata();
        metadata.setToolName(getName());
        metadata.setOneLinePurpose("Confirmar a raiz segura do projeto antes de exploracao, leitura ou mutacao.");
        metadata.setActivityDescription("Sobe a arvore para localizar o perimetro seguro do projeto.");

        AgentToolParameterMetadata path = new AgentToolParameterMetadata();
        path.setName("path");
        path.setRequired(false);
        path.setDescription("Caminho relativo inicial a partir do qual a deteccao deve subir a arvore.");
        path.setExampleValue("src/main/java");
        metadata.addParameter(path);

        metadata.addRecommendedUseCase("Use quando houver duvida sobre a base real do projeto antes de explorar diretorios ou buscar texto.");
        metadata.addRecommendedUseCase("Use antes de cadeias de ferramentas que dependem fortemente do perimetro seguro.");
        metadata.addRecommendedUseCase("Use quando a IA precisar confirmar se esta operando na raiz correta do modulo ou agregador.");

        metadata.addGuardrail("A verificacao deve respeitar o perimetro seguro ja conhecido do projeto.");
        metadata.addGuardrail("Nao use esta ferramenta para ler arquivos ou explorar conteudo interno.");
        metadata.addGuardrail("Marcadores como .git e .project devem guiar a deteccao da raiz.");

        metadata.addJsonExample(
                "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"verificar_raiz_projeto\\\",\\\"parameters\\\":{\\\"path\\\":\\\"src/main/java\\\"},\\\"explanation\\\":\\\"Preciso confirmar a raiz segura do projeto antes de continuar a investigacao.\\\"}"
        );

        return metadata;
    }

    /** * Resolve o ponto inicial da verificacao a partir do caminho solicitado, * respeitando o perimetro seguro. * * @param requestedPath caminho relativo solicitado * @return arquivo ou diretorio inicial resolvido * * @author Renato Tomaz Nati * @since 2026-05-20 */
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

    /** * Detecta a melhor raiz segura subindo a arvore de diretorios a partir do * ponto inicial informado. * * @param startPoint ponto inicial de analise * @return raiz segura detectada * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private File detectSafeProjectRoot(File startPoint) {
        File cursor = startPoint;

        if (cursor.isFile()) {
            cursor = cursor.getParentFile();
        }

        File bestRoot = rootDirectory;

        while (cursor != null && isInsideRoot(cursor)) {
            if (hasProjectRootMarker(cursor)) {
                bestRoot = cursor;
            }

            if (sameDirectory(cursor, rootDirectory)) {
                break;
            }

            cursor = cursor.getParentFile();
        }

        return bestRoot != null ? bestRoot : rootDirectory;
    }

    /** * Retorna true quando o diretorio possui marcador tipico de raiz de * projeto. * * @param directory diretorio candidato * @return true quando o diretorio aparenta ser raiz de projeto * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean hasProjectRootMarker(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return false;
        }

        File gitDir = new File(directory, ".git");
        if (gitDir.exists()) {
            return true;
        }

        File eclipseProject = new File(directory, ".project");
        return eclipseProject.exists();
    }

    /** * Retorna true quando o candidato estiver fisicamente dentro da raiz segura * atual. * * @param candidate arquivo ou diretorio candidato * @return true quando o candidato estiver dentro do perimetro seguro * * @author Renato Tomaz Nati * @since 2026-05-20 */
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

    /** * Compara dois diretorios ou arquivos de forma segura. * * @param first primeiro candidato * @param second segundo candidato * @return true quando representarem o mesmo caminho fisico * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean sameDirectory(File first, File second) {
        if (first == null || second == null) {
            return false;
        }

        try {
            return first.getCanonicalPath().equals(second.getCanonicalPath());
        } catch (Exception e) {
            return first.getAbsolutePath().equals(second.getAbsolutePath());
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