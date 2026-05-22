package com.mcp.sailibrary.plugin.agent.tools.exploration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolParameterMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadataProvider;
import com.mcp.sailibrary.plugin.agent.tools.support.ToolJsonSupport;

/** * Le o conteudo textual integral de um arquivo dentro da raiz segura do * projeto, com limite defensivo de linhas. * * <p>Esta ferramenta foi desenhada para fornecer a IA uma leitura textual * controlada do arquivo alvo, permitindo analise de imports, anotacoes, * estrutura de classe, configuracoes XML e evidencias de frameworks sem abrir * acesso irrestrito ao filesystem.</p> * * <p>O comportamento e defensivo: * <ul> * <li>impede path traversal</li> * <li>restringe a leitura a raiz segura</li> * <li>bloqueia caminhos inexistentes ou invalidos</li> * <li>interrompe a leitura em limite maximo de linhas para proteger o * contexto da IA</li> * </ul> * </p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class FileContentReadTool implements AgentTool, AgentToolPromptMetadataProvider {

    private static final int DEFAULT_MAX_LINES = 1000;
    private static final int MAX_ALLOWED_LINES = 4000;

    private final File rootDirectory;

    /** * Inicializa a ferramenta com a raiz segura do projeto. * * @param rootDirectory raiz segura do projeto atual * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public FileContentReadTool(File rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public String getName() {
        return "ler_conteudo_arquivo";
    }

    /** * Executa a leitura segura do arquivo solicitado. * * <p>O caminho informado deve ser relativo a raiz segura do projeto. A * leitura e interrompida quando atingir o limite maximo configurado para * evitar explosao de contexto no prompt da IA.</p> * * @param jsonParameters parametros JSON da ferramenta * @return conteudo textual do arquivo ou mensagem de erro operacional * * @author Renato Tomaz Nati * @since 2026-05-20 */
    @Override
    public String execute(String jsonParameters) {
        if (rootDirectory == null || !rootDirectory.exists() || !rootDirectory.isDirectory()) {
            return "Erro Operacional: A raiz segura do projeto esta indisponivel para leitura de arquivo.";
        }

        String requestedPath = ToolJsonSupport.extractJsonStringValue(jsonParameters, "path");
        int maxLines = ToolJsonSupport.extractJsonIntValue(
                jsonParameters,
                "maxLines",
                DEFAULT_MAX_LINES,
                50,
                MAX_ALLOWED_LINES
        );

        if (isBlank(requestedPath)) {
            return "Erro Operacional: E necessario especificar o caminho ('path') do arquivo a ser lido.";
        }

        try {
            File targetFile = resolveTargetFile(requestedPath);
            if (targetFile == null || !targetFile.exists() || !targetFile.isFile()) {
                return "Erro Operacional: O arquivo solicitado nao foi encontrado dentro do perimetro seguro.";
            }

            StringBuilder content = new StringBuilder();
            BufferedReader bufferedReader = null;

            try {
                bufferedReader = new BufferedReader(new FileReader(targetFile));
                String line;
                int lineCounter = 0;

                while ((line = bufferedReader.readLine()) != null) {
                    content.append(line).append("\n");
                    lineCounter++;

                    if (lineCounter >= maxLines) {
                        content.append("\n[AVISO TATICO]: Leitura interrompida no limite de seguranca de ")
                               .append(maxLines)
                               .append(" linhas.");
                        break;
                    }
                }
            } finally {
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (Exception e) {
                    }
                }
            }

            return content.toString();
        } catch (Exception e) {
            return "Falha ao extrair o conteudo do arquivo: " + e.getMessage();
        }
    }

    @Override
    public AgentToolPromptMetadata getPromptMetadata() {
        AgentToolPromptMetadata metadata = new AgentToolPromptMetadata();
        metadata.setToolName(getName());
        metadata.setOneLinePurpose("Ler o conteudo textual de um arquivo dentro da raiz segura do projeto.");
        metadata.setActivityDescription("Extrai o texto bruto do arquivo. Interrompe no limite de seguranca para proteger o contexto.");

        AgentToolParameterMetadata path = new AgentToolParameterMetadata();
        path.setName("path");
        path.setRequired(true);
        path.setDescription("Caminho relativo do arquivo a ser lido dentro da raiz segura.");
        path.setExampleValue("src/main/java/com/exemplo/Servico.java");
        metadata.addParameter(path);

        AgentToolParameterMetadata maxLines = new AgentToolParameterMetadata();
        maxLines.setName("maxLines");
        maxLines.setRequired(false);
        maxLines.setDescription("Quantidade maxima de linhas a serem lidas antes de interromper.");
        maxLines.setExampleValue("1000");
        metadata.addParameter(maxLines);

        metadata.addRecommendedUseCase("Use quando precisar inspecionar imports, anotacoes, metodos, XML ou configuracoes reais de um arquivo especifico.");
        metadata.addRecommendedUseCase("Use quando o caminho do arquivo ja estiver claro e a IA precisar de leitura textual direta.");
        metadata.addRecommendedUseCase("Use depois de explorar diretorios ou localizar ocorrencias para abrir apenas o arquivo relevante.");

        metadata.addGuardrail("A leitura deve permanecer dentro da raiz segura do projeto.");
        metadata.addGuardrail("Nao use esta ferramenta como substituta de exploracao de diretorio.");
        metadata.addGuardrail("Arquivos muito grandes devem respeitar limite de linhas para proteger o contexto da IA.");

        metadata.addJsonExample(
                "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"ler_conteudo_arquivo\\\",\\\"parameters\\\":{\\\"path\\\":\\\"src/main/java/com/exemplo/Servico.java\\\",\\\"maxLines\\\":\\\"1000\\\"},\\\"explanation\\\":\\\"Preciso ler o conteudo real do arquivo para validar imports, estrutura e regras aplicadas.\\\"}"
        );

        return metadata;
    }

    /** * Resolve o arquivo alvo a partir do caminho relativo informado, * respeitando a raiz segura do projeto. * * @param requestedPath caminho relativo solicitado * @return arquivo resolvido ou null quando o path for inseguro * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private File resolveTargetFile(String requestedPath) {
        String normalizedRequestedPath = requestedPath != null
                ? requestedPath.trim().replace("\\", "/")
                : "";

        if (isBlank(normalizedRequestedPath)) {
            return null;
        }

        if (normalizedRequestedPath.contains("..")) {
            return null;
        }

        File candidate = new File(rootDirectory, normalizedRequestedPath);
        if (!isInsideRoot(candidate)) {
            return null;
        }

        return candidate;
    }

    /** * Retorna true quando o arquivo candidato estiver fisicamente dentro da * raiz segura do projeto. * * @param candidate arquivo candidato * @return true quando o arquivo estiver dentro do perimetro seguro * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean isInsideRoot(File candidate) {
        if (candidate == null || rootDirectory == null) {
            return false;
        }

        try {
            String rootCanonical = rootDirectory.getCanonicalPath().replace("\\", "/");
            String candidateCanonical = candidate.getCanonicalPath().replace("\\", "/");
            return candidateCanonical.startsWith(rootCanonical);
        } catch (Exception e) {
            return false;
        }
    }

    /** * Retorna true quando o valor informado for nulo ou vazio. * * @param value valor a validar * @return true quando o valor for branco * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}