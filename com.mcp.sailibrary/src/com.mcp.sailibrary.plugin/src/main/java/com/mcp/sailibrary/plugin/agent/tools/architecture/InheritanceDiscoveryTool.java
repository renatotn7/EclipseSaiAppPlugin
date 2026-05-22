package com.mcp.sailibrary.plugin.agent.tools.architecture;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolParameterMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadataProvider;
import com.mcp.sailibrary.plugin.agent.context.SourceInsightSupport;

/** * Localiza classes que herdam de uma superclasse especifica, priorizando o * mesmo modulo Maven e considerando sinais de heranca em XML Hibernate. * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class InheritanceDiscoveryTool implements AgentTool, AgentToolPromptMetadataProvider {

    private File rootDirectory;
    private SourceInsightSupport support;

    public InheritanceDiscoveryTool(File rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.support = new SourceInsightSupport();
    }

    @Override
    public String getName() {
        return "buscar_herdeiros_superclasse";
    }

    @Override
    public AgentToolPromptMetadata getPromptMetadata() {
        AgentToolPromptMetadata metadata = new AgentToolPromptMetadata();
        metadata.setToolName(getName());
        metadata.setOneLinePurpose("Localizar subclasses e sinais de heranca relacionados a uma superclasse.");
        metadata.setActivityDescription("Descobre subclasses declaradas e heranca mapeada em XML.");

        AgentToolParameterMetadata superclasse = new AgentToolParameterMetadata();
        superclasse.setName("superclasse");
        superclasse.setRequired(true);
        superclasse.setDescription("Nome da superclasse base usada como referencia de heranca.");
        superclasse.setExampleValue("BaseRelatorioAction");
        metadata.addParameter(superclasse);

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

        metadata.addRecommendedUseCase("Use quando precisar descobrir subclasses concretas de uma superclasse.");
        metadata.addRecommendedUseCase("Use quando houver suspeita de heranca declarada em XML Hibernate.");
        metadata.addRecommendedUseCase("Use para avaliar impacto de alteracao em superclasse ou contrato base.");

        metadata.addGuardrail("A busca deve priorizar o mesmo modulo Maven e respeitar a raiz segura.");
        metadata.addGuardrail("Ausencia de extends direto nao elimina heranca mediada por framework ou XML.");
        metadata.addGuardrail("Resultados devem ser tratados como evidencias, nao como prova unica de comportamento final.");

        metadata.addJsonExample(
                "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"buscar_herdeiros_superclasse\\\",\\\"parameters\\\":{\\\"superclasse\\\":\\\"BaseRelatorioAction\\\",\\\"path\\\":\\\"src/main/java\\\",\\\"limite\\\":\\\"20\\\"},\\\"explanation\\\":\\\"Preciso localizar subclasses e sinais de heranca relacionados a esta superclasse.\\\"}"
        );

        return metadata;
    }

    @Override
    public String execute(String jsonParameters) {
        String nomeSuperclasse = support.extrairValorVariavel(jsonParameters, "superclasse");
        String requestedPath = support.extrairValorVariavel(jsonParameters, "path");
        String limiteTexto = support.extrairValorVariavel(jsonParameters, "limite");

        if (nomeSuperclasse == null || nomeSuperclasse.trim().length() == 0) {
            return "Erro Operacional: O parametro 'superclasse' e obrigatorio para localizar herdeiros.";
        }

        int limiteResultados = support.extrairInteiro(limiteTexto, 20, 100);

        File pontoInicial = support.resolverPontoInicial(rootDirectory, requestedPath);
        File raizSeguraProjeto = support.localizarRaizSeguraProjeto(pontoInicial, rootDirectory);
        File moduloPreferencial = support.localizarModuloMavenMaisProximo(pontoInicial, raizSeguraProjeto);
        List<File> arquivos = support.coletarArquivosModuloPrimeiro(raizSeguraProjeto, moduloPreferencial);

        List<String> herdeiros = new ArrayList<String>();
        List<String> sinaisXml = new ArrayList<String>();

        String regexExtends = "\\bextends\\b[^\\n\\r\\{]*\\b" + PatternEscape.escape(nomeSuperclasse) + "\\b";

        for (int i = 0; i < arquivos.size(); i++) {
            if (herdeiros.size() >= limiteResultados) {
                break;
            }

            File arquivoAtual = arquivos.get(i);
            String conteudo = support.lerConteudoArquivo(arquivoAtual);
            if (conteudo.trim().length() == 0) {
                continue;
            }

            String nomeArquivo = arquivoAtual.getName().toLowerCase();

            if (nomeArquivo.endsWith(".java")) {
                if (support.contemPadrao(conteudo, regexExtends)) {
                    String descricao = "Heranca localizada em: " + support.descreverArquivo(arquivoAtual);
                    List<String> marcadoresLombok = support.detectarMarcadoresLombok(conteudo);
                    if (!marcadoresLombok.isEmpty()) {
                        descricao += " | Lombok: " + juntarLista(marcadoresLombok);
                    }
                    herdeiros.add(descricao);
                }
            } else if (nomeArquivo.endsWith(".xml")) {
                if (support.eArquivoHibernateXml(arquivoAtual, conteudo) && conteudo.contains(nomeSuperclasse)) {
                    if (conteudo.contains("<subclass ")
                            || conteudo.contains("<joined-subclass ")
                            || conteudo.contains("<union-subclass ")) {
                        sinaisXml.add("Heranca ou especializacao em XML localizada em: " + support.descreverArquivo(arquivoAtual));
                    }
                }
            }
        }

        StringBuilder relatorio = new StringBuilder();
        relatorio.append("Relatorio de herdeiros para [").append(nomeSuperclasse).append("]").append("\n");
        relatorio.append("Modulo preferencial: ").append(support.descreverArquivo(moduloPreferencial)).append("\n\n");

        if (!herdeiros.isEmpty()) {
            relatorio.append("Herdeiros diretos encontrados:").append("\n");
            for (int i = 0; i < herdeiros.size(); i++) {
                relatorio.append("- ").append(herdeiros.get(i)).append("\n");
            }
        } else {
            relatorio.append("Nenhum herdeiro direto por 'extends' foi encontrado.").append("\n");
        }

        if (!sinaisXml.isEmpty()) {
            relatorio.append("\n");
            relatorio.append("Sinais de heranca em XML Hibernate:").append("\n");
            for (int i = 0; i < sinaisXml.size(); i++) {
                relatorio.append("- ").append(sinaisXml.get(i)).append("\n");
            }
        }

        if (herdeiros.isEmpty() && sinaisXml.isEmpty()) {
            relatorio.append("\nNenhum indicio adicional foi localizado.");
        }

        return relatorio.toString();
    }

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