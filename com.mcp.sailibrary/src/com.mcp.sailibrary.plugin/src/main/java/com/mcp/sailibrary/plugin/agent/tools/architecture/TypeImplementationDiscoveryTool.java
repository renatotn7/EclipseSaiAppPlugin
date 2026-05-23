package com.mcp.sailibrary.plugin.agent.tools.architecture;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.context.ResolvedProjectScope;
import com.mcp.sailibrary.plugin.agent.context.SourceInsightSupport;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolParameterMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadataProvider;

/** * Localiza implementacoes de interface ou contrato por busca textual, * priorizando o mesmo modulo Maven e sinalizando framework, Hibernate, hbm.xml * e Lombok. * * <p>Esta implementacao foi reforcada para trabalhar com escopo resolvido de * projeto, reduzindo o risco de trazer implementacoes de modulos ou projetos * errados em workspaces complexos.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class TypeImplementationDiscoveryTool implements AgentTool, AgentToolPromptMetadataProvider {

    private File rootDirectory;
    private SourceInsightSupport support;

    /** * Inicializa a ferramenta de descoberta de implementacoes. * * @param rootDirectory raiz segura do projeto * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public TypeImplementationDiscoveryTool(File rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.support = new SourceInsightSupport();
    }

    @Override
    public String getName() {
        return "buscar_implementacoes_tipo";
    }

    @Override
    public AgentToolPromptMetadata getPromptMetadata() {
        AgentToolPromptMetadata metadata = new AgentToolPromptMetadata();
        metadata.setToolName(getName());
        metadata.setOneLinePurpose("Localizar implementacoes de interface ou contrato e sinais de framework relacionados.");
        metadata.setActivityDescription("Localiza implementacoes de interface ou contrato e sinais de framework.");

        AgentToolParameterMetadata classe = new AgentToolParameterMetadata();
        classe.setName("classe");
        classe.setRequired(true);
        classe.setDescription("Nome da interface ou contrato alvo.");
        classe.setExampleValue("AgendaService");
        metadata.addParameter(classe);

        AgentToolParameterMetadata path = new AgentToolParameterMetadata();
        path.setName("path");
        path.setRequired(false);
        path.setDescription("Caminho inicial preferencial para a busca.");
        path.setExampleValue("src/main/java");
        metadata.addParameter(path);

        AgentToolParameterMetadata limite = new AgentToolParameterMetadata();
        limite.setName("limite");
        limite.setRequired(false);
        limite.setDescription("Quantidade maxima de resultados retornados.");
        limite.setExampleValue("20");
        metadata.addParameter(limite);

        metadata.addRecommendedUseCase("Use quando precisar localizar classes que implementam uma interface ou contrato.");
        metadata.addRecommendedUseCase("Use quando a implementacao concreta pode estar mediada por framework, XML ou codigo gerado.");
        metadata.addRecommendedUseCase("Use para localizar candidatos concretos antes de ler arquivos maiores.");

        metadata.addGuardrail("A busca deve priorizar o mesmo modulo Maven e respeitar o perimetro seguro.");
        metadata.addGuardrail("Ausencia de implements direto nao elimina runtime mediado por framework.");
        metadata.addGuardrail("Sinais de Lombok e Hibernate devem ser tratados como pistas, nao como prova unica de fluxo.");

        metadata.addJsonExample(
                "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"buscar_implementacoes_tipo\\\",\\\"parameters\\\":{\\\"classe\\\":\\\"AgendaService\\\",\\\"path\\\":\\\"src/main/java\\\",\\\"limite\\\":\\\"20\\\"},\\\"explanation\\\":\\\"Preciso localizar implementacoes concretas do contrato antes de continuar a investigacao.\\\"}"
        );

        return metadata;
    }

    /** * Executa a busca textual de implementacoes e sinais complementares. * * @param jsonParameters parametros JSON da ferramenta * @return relatorio textual das implementacoes e sinais encontrados * * @author Renato Tomaz Nati * @since 2026-05-20 */
    @Override
    public String execute(String jsonParameters) {
        String nomeClasse = support.extrairValorVariavel(jsonParameters, "classe");
        String requestedPath = support.extrairValorVariavel(jsonParameters, "path");
        String limiteTexto = support.extrairValorVariavel(jsonParameters, "limite");

        if (nomeClasse == null || nomeClasse.trim().length() == 0) {
            return "Erro Operacional: O parametro 'classe' e obrigatorio para localizar implementacoes.";
        }

        int limiteResultados = support.extrairInteiro(limiteTexto, 20, 100);

        File pontoInicial = support.resolverPontoInicial(rootDirectory, requestedPath);
        ResolvedProjectScope scope = support.resolverEscopoProjeto(pontoInicial, rootDirectory);

        if (scope == null || !scope.isUsable()) {
            return "Erro Operacional: Nao foi possivel resolver escopo seguro para localizar implementacoes.";
        }

        File raizSeguraProjeto = scope.getSafeRoot();
        File moduloPreferencial = scope.getNearestMavenModuleRoot() != null
                ? scope.getNearestMavenModuleRoot()
                : scope.getEffectiveSearchRoot();

        List<File> arquivos = support.coletarArquivosModuloPrimeiro(scope);

        List<String> implementacoes = new ArrayList<String>();
        List<String> sinaisFramework = new ArrayList<String>();

        String regexImplements = "\\bimplements\\b[^\\n\\r\\{]*\\b" + PatternEscape.escape(nomeClasse) + "\\b";

        for (int i = 0; i < arquivos.size(); i++) {
            if (implementacoes.size() >= limiteResultados) {
                break;
            }

            File arquivoAtual = arquivos.get(i);
            String conteudo = support.lerConteudoArquivo(arquivoAtual);
            if (conteudo.trim().length() == 0) {
                continue;
            }

            if (arquivoAtual.getName().toLowerCase().endsWith(".java")) {
                if (support.contemPadrao(conteudo, regexImplements)) {
                    implementacoes.add("Implementacao localizada em: " + support.descreverArquivo(arquivoAtual));
                    continue;
                }

                if (conteudo.contains(nomeClasse)) {
                    List<String> marcadoresHibernate = support.detectarMarcadoresHibernate(conteudo);
                    List<String> marcadoresLombok = support.detectarMarcadoresLombok(conteudo);

                    if (!marcadoresHibernate.isEmpty()) {
                        sinaisFramework.add("Sinal de framework em " + support.descreverArquivo(arquivoAtual)
                                + " | Hibernate/JPA: " + juntarLista(marcadoresHibernate));
                    }

                    if (!marcadoresLombok.isEmpty()) {
                        sinaisFramework.add("Sinal de codigo gerado em " + support.descreverArquivo(arquivoAtual)
                                + " | Lombok: " + juntarLista(marcadoresLombok));
                    }
                }
            } else if (arquivoAtual.getName().toLowerCase().endsWith(".xml")) {
                if (support.eArquivoHibernateXml(arquivoAtual, conteudo) && conteudo.contains(nomeClasse)) {
                    sinaisFramework.add("Mapeamento Hibernate localizado em: " + support.descreverArquivo(arquivoAtual));
                }
            }
        }

        StringBuilder relatorio = new StringBuilder();
        relatorio.append("Relatorio de implementacoes para [").append(nomeClasse).append("]").append("\n");
        relatorio.append("safeRoot: ").append(support.descreverArquivo(raizSeguraProjeto)).append("\n");
        relatorio.append("moduloPreferencial: ").append(support.descreverArquivo(moduloPreferencial)).append("\n");
        relatorio.append("nearestEclipseProject: ").append(support.descreverArquivo(scope.getNearestEclipseProjectRoot())).append("\n\n");

        if (!implementacoes.isEmpty()) {
            relatorio.append("Implementacoes diretas encontradas:").append("\n");
            for (int i = 0; i < implementacoes.size(); i++) {
                relatorio.append("- ").append(implementacoes.get(i)).append("\n");
            }
        } else {
            relatorio.append("Nenhuma implementacao direta por 'implements' foi encontrada.").append("\n");
        }

        if (!sinaisFramework.isEmpty()) {
            relatorio.append("\n");
            relatorio.append("Sinais de framework ou geracao detectados:").append("\n");
            for (int i = 0; i < sinaisFramework.size(); i++) {
                relatorio.append("- ").append(sinaisFramework.get(i)).append("\n");
            }
            relatorio.append("\n");
            relatorio.append("Observacao: ausencia de implementacao direta pode indicar runtime de framework, mapeamento XML ou metodos gerados.");
        }

        if (implementacoes.isEmpty() && sinaisFramework.isEmpty()) {
            relatorio.append("\nNenhum indicio adicional foi localizado.");
        }

        return relatorio.toString();
    }

    /** * Junta lista de marcadores em uma string compacta. * * @param valores lista de valores * @return texto unido por virgula * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String juntarLista(List<String> valores) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < valores.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(valores.get(i));
        }
        return builder.toString();
    }

    /** * Utilitario de escape para regex textual simples. * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private static class PatternEscape {
        private static String escape(String texto) {
            String valor = texto;
            valor = valor.replace("\\", "\\\\");
            valor = valor.replace(".", "\\.");
            valor = valor.replace("$", "\\$");
            valor = valor.replace("[", "\\[");
            valor = valor.replace("]", "\\]");
            valor = valor.replace("(", "\\(");
            valor = valor.replace(")", "\\)");
            valor = valor.replace("{", "\\{");
            valor = valor.replace("}", "\\}");
            valor = valor.replace("*", "\\*");
            valor = valor.replace("+", "\\+");
            valor = valor.replace("?", "\\?");
            valor = valor.replace("|", "\\|");
            valor = valor.replace("^", "\\^");
            return valor;
        }
    }
}