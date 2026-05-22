package com.mcp.sailibrary.plugin.agent.tools.architecture;

import java.io.File;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolParameterMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadataProvider;
import com.mcp.sailibrary.plugin.agent.tools.jdt.MethodCalleesDiscoveryTool;
import com.mcp.sailibrary.plugin.agent.tools.jdt.MethodCallersSearchTool;
import com.mcp.sailibrary.plugin.agent.tools.jdt.MethodOverrideInspectionTool;
import com.mcp.sailibrary.plugin.agent.tools.jdt.SideEffectInspectionTool;
import com.mcp.sailibrary.plugin.agent.tools.support.ToolJsonSupport;

/** * Consolida um resumo de impacto de alteracao a partir de chamadores, callees, * override, efeitos colaterais e persistencia. * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class ChangeImpactSummaryTool implements AgentTool, AgentToolPromptMetadataProvider {

    private File rootDirectory;
    private ICompilationUnit compilationUnitAtual;
    private int offsetAtual;

    public ChangeImpactSummaryTool(File rootDirectory, ICompilationUnit compilationUnitAtual, int offsetAtual) {
        this.rootDirectory = rootDirectory;
        this.compilationUnitAtual = compilationUnitAtual;
        this.offsetAtual = offsetAtual;
    }

    @Override
    public String getName() {
        return "resumir_impacto_alteracao";
    }

    @Override
    public AgentToolPromptMetadata getPromptMetadata() {
        AgentToolPromptMetadata metadata = new AgentToolPromptMetadata();
        metadata.setToolName(getName());
        metadata.setOneLinePurpose("Consolidar um panorama tatico de risco e impacto antes de alterar um metodo.");
        metadata.setActivityDescription("Consolida um resumo tatico de impacto de alteracao.");

        AgentToolParameterMetadata modo = new AgentToolParameterMetadata();
        modo.setName("modo");
        modo.setRequired(false);
        modo.setDescription("Modo de resolucao do alvo, como editor_ativo.");
        modo.setExampleValue("editor_ativo");
        metadata.addParameter(modo);

        AgentToolParameterMetadata classe = new AgentToolParameterMetadata();
        classe.setName("classe");
        classe.setRequired(false);
        classe.setDescription("Nome da classe alvo quando o modo nao for editor_ativo.");
        classe.setExampleValue("RelatorioAcompanhamentoDivisaoAction");
        metadata.addParameter(classe);

        AgentToolParameterMetadata metodo = new AgentToolParameterMetadata();
        metodo.setName("metodo");
        metodo.setRequired(false);
        metodo.setDescription("Nome do metodo alvo quando o modo nao for editor_ativo.");
        metodo.setExampleValue("setupEnv");
        metadata.addParameter(metodo);

        AgentToolParameterMetadata limiteChamadores = new AgentToolParameterMetadata();
        limiteChamadores.setName("limite_chamadores");
        limiteChamadores.setRequired(false);
        limiteChamadores.setDescription("Limite maximo de chamadores considerados no resumo.");
        limiteChamadores.setExampleValue("10");
        metadata.addParameter(limiteChamadores);

        AgentToolParameterMetadata limiteCallees = new AgentToolParameterMetadata();
        limiteCallees.setName("limite_callees");
        limiteCallees.setRequired(false);
        limiteCallees.setDescription("Limite maximo de callees considerados no resumo.");
        limiteCallees.setExampleValue("15");
        metadata.addParameter(limiteCallees);

        AgentToolParameterMetadata limiteEfeitos = new AgentToolParameterMetadata();
        limiteEfeitos.setName("limite_efeitos");
        limiteEfeitos.setRequired(false);
        limiteEfeitos.setDescription("Limite maximo de efeitos colaterais considerados no resumo.");
        limiteEfeitos.setExampleValue("15");
        metadata.addParameter(limiteEfeitos);

        AgentToolParameterMetadata limiteQueries = new AgentToolParameterMetadata();
        limiteQueries.setName("limite_queries");
        limiteQueries.setRequired(false);
        limiteQueries.setDescription("Limite maximo de evidencias de query e persistencia.");
        limiteQueries.setExampleValue("15");
        metadata.addParameter(limiteQueries);

        AgentToolParameterMetadata limiteOverride = new AgentToolParameterMetadata();
        limiteOverride.setName("limite_override");
        limiteOverride.setRequired(false);
        limiteOverride.setDescription("Limite maximo de evidencias de override.");
        limiteOverride.setExampleValue("15");
        metadata.addParameter(limiteOverride);

        metadata.addRecommendedUseCase("Use antes de propor alteracao em metodo sensivel.");
        metadata.addRecommendedUseCase("Use quando a IA precisar estimar risco sem disparar varias ferramentas isoladas manualmente.");
        metadata.addRecommendedUseCase("Use para triagem inicial de impacto antes de editar comportamento legado.");

        metadata.addGuardrail("Nao substitui leitura detalhada quando o metodo for altamente sensivel.");
        metadata.addGuardrail("O resumo depende das ferramentas auxiliares e do contexto real resolvido.");
        metadata.addGuardrail("Use limites prudentes para evitar excesso de ruido.");

        metadata.addJsonExample(
                "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"resumir_impacto_alteracao\\\",\\\"parameters\\\":{\\\"modo\\\":\\\"editor_ativo\\\",\\\"limite_chamadores\\\":\\\"10\\\",\\\"limite_callees\\\":\\\"15\\\",\\\"limite_efeitos\\\":\\\"15\\\",\\\"limite_queries\\\":\\\"15\\\",\\\"limite_override\\\":\\\"15\\\"},\\\"explanation\\\":\\\"Preciso de um panorama tatico de impacto antes de propor alteracao segura no metodo atual.\\\"}"
        );

        return metadata;
    }

    @Override
    public String execute(String jsonParameters) {
        String modo = ToolJsonSupport.extractJsonStringValue(jsonParameters, "modo");
        String nomeClasse = ToolJsonSupport.extractJsonStringValue(jsonParameters, "classe");
        String nomeMetodo = ToolJsonSupport.extractJsonStringValue(jsonParameters, "metodo");

        int limiteChamadores = ToolJsonSupport.extractJsonIntValue(jsonParameters, "limite_chamadores", 15, 1, 100);
        int limiteCallees = ToolJsonSupport.extractJsonIntValue(jsonParameters, "limite_callees", 20, 1, 100);
        int limiteEfeitos = ToolJsonSupport.extractJsonIntValue(jsonParameters, "limite_efeitos", 20, 1, 100);
        int limiteQueries = ToolJsonSupport.extractJsonIntValue(jsonParameters, "limite_queries", 20, 1, 100);
        int limiteOverride = ToolJsonSupport.extractJsonIntValue(jsonParameters, "limite_override", 20, 1, 100);

        IMethod metodoAlvo = resolveTargetMethod(modo, nomeClasse, nomeMetodo);
        if (metodoAlvo == null) {
            return "Erro Operacional: Nao foi possivel localizar o metodo alvo para resumir impacto de alteracao.";
        }

        String classeAlvo = safeDeclaringTypeName(metodoAlvo);
        String metodoNome = metodoAlvo.getElementName();

        String jsonBase = buildNamedMethodJson(classeAlvo, metodoNome);

        String jsonChamadores = appendField(jsonBase, "limite", String.valueOf(limiteChamadores));
        String jsonCallees = appendField(appendField(jsonBase, "limite", String.valueOf(limiteCallees)), "incluir_externos", "false");
        String jsonOverride = appendField(jsonBase, "limite", String.valueOf(limiteOverride));
        String jsonEfeitos = appendField(jsonBase, "limite", String.valueOf(limiteEfeitos));
        String jsonQueries = appendField(appendField(jsonBase, "limite", String.valueOf(limiteQueries)), "incluir_xml", "true");

        MethodCallersSearchTool callersTool = new MethodCallersSearchTool(compilationUnitAtual, offsetAtual);
        MethodCalleesDiscoveryTool calleesTool = new MethodCalleesDiscoveryTool(compilationUnitAtual, offsetAtual);
        MethodOverrideInspectionTool overrideTool = new MethodOverrideInspectionTool(compilationUnitAtual, offsetAtual);
        SideEffectInspectionTool sideEffectTool = new SideEffectInspectionTool(compilationUnitAtual, offsetAtual);
        QueryExtractionTool queryTool = new QueryExtractionTool(rootDirectory, compilationUnitAtual, offsetAtual);

        String relatorioChamadores = callersTool.execute(jsonChamadores);
        String relatorioCallees = calleesTool.execute(jsonCallees);
        String relatorioOverride = overrideTool.execute(jsonOverride);
        String relatorioEfeitos = sideEffectTool.execute(jsonEfeitos);
        String relatorioQueries = queryTool.execute(jsonQueries);

        ImpactSummary resumo = summarize(
                relatorioChamadores,
                relatorioCallees,
                relatorioOverride,
                relatorioEfeitos,
                relatorioQueries
        );

        return formatFinalReport(
                metodoAlvo,
                resumo,
                relatorioChamadores,
                relatorioCallees,
                relatorioOverride,
                relatorioEfeitos,
                relatorioQueries
        );
    }

    private IMethod resolveTargetMethod(String modo, String nomeClasse, String nomeMetodo) {
        if ("editor_ativo".equalsIgnoreCase(modo) || (isBlank(nomeClasse) && isBlank(nomeMetodo))) {
            return resolveMethodFromEditor();
        }

        return findMethodByName(nomeClasse, nomeMetodo);
    }

    private IMethod resolveMethodFromEditor() {
        if (compilationUnitAtual == null) {
            return null;
        }

        try {
            IType tipoPrimario = compilationUnitAtual.findPrimaryType();
            if (tipoPrimario == null) {
                return null;
            }

            IMethod[] metodos = tipoPrimario.getMethods();
            for (int i = 0; i < metodos.length; i++) {
                int inicioMetodo = metodos[i].getSourceRange().getOffset();
                int fimMetodo = inicioMetodo + metodos[i].getSourceRange().getLength();
                if (offsetAtual >= inicioMetodo && offsetAtual <= fimMetodo) {
                    return metodos[i];
                }
            }
        } catch (Exception e) {
            return null;
        }

        return null;
    }

    private IMethod findMethodByName(String nomeClasse, String nomeMetodo) {
        if (compilationUnitAtual == null || compilationUnitAtual.getJavaProject() == null) {
            return null;
        }

        try {
            IType tipo = null;

            IType tipoPrimario = compilationUnitAtual.findPrimaryType();
            if (tipoPrimario != null) {
                String nomeSimplesAtual = tipoPrimario.getElementName();
                String fqcnAtual = tipoPrimario.getFullyQualifiedName();

                if (nomeSimplesAtual.equals(nomeClasse) || fqcnAtual.equals(nomeClasse)) {
                    tipo = tipoPrimario;
                }
            }

            if (tipo == null) {
                tipo = compilationUnitAtual.getJavaProject().findType(nomeClasse);
            }

            if (tipo == null) {
                tipo = findTypeBySimpleName(nomeClasse);
            }

            if (tipo == null) {
                return null;
            }

            IMethod[] metodos = tipo.getMethods();
            for (int i = 0; i < metodos.length; i++) {
                if (metodos[i].getElementName().equals(nomeMetodo)) {
                    return metodos[i];
                }
            }
        } catch (Exception e) {
            return null;
        }

        return null;
    }

    private IType findTypeBySimpleName(String nomeClasse) {
        if (compilationUnitAtual == null || compilationUnitAtual.getJavaProject() == null) {
            return null;
        }

        try {
            IPackageFragment[] pacotes = compilationUnitAtual.getJavaProject().getPackageFragments();
            for (int i = 0; i < pacotes.length; i++) {
                if (pacotes[i].getKind() == IPackageFragmentRoot.K_SOURCE) {
                    ICompilationUnit[] unidades = pacotes[i].getCompilationUnits();
                    for (int j = 0; j < unidades.length; j++) {
                        IType[] tipos = unidades[j].getAllTypes();
                        for (int k = 0; k < tipos.length; k++) {
                            if (tipos[k].getElementName().equals(nomeClasse)) {
                                return tipos[k];
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }

        return null;
    }

    private ImpactSummary summarize(String relatorioChamadores, String relatorioCallees, String relatorioOverride, String relatorioEfeitos, String relatorioQueries) {

        ImpactSummary resumo = new ImpactSummary();

        resumo.totalChamadores = countEnumeratedItems(relatorioChamadores);
        resumo.totalCallees = countEnumeratedItems(relatorioCallees);
        resumo.totalOverrideSuper = countOccurrences(relatorioOverride, "IMPLEMENTA_CONTRATO")
                + countOccurrences(relatorioOverride, "SOBRESCREVE_SUPERCLASSE");
        resumo.totalOverrideSub = countOccurrences(relatorioOverride, "SOBRESCRITO_POR");
        resumo.totalEfeitos = countEnumeratedItems(relatorioEfeitos);
        resumo.totalPersistencia = countPersistenceEvidence(relatorioEfeitos, relatorioQueries);

        resumo.temPolimorfismo = resumo.totalOverrideSuper > 0 || resumo.totalOverrideSub > 0;
        resumo.temPersistencia = resumo.totalPersistencia > 0;
        resumo.temEfeitoColateral = resumo.totalEfeitos > 0;
        resumo.temMuitosChamadores = resumo.totalChamadores >= 5;
        resumo.temMuitosCallees = resumo.totalCallees >= 8;

        resumo.nivelRisco = inferRisk(resumo);

        return resumo;
    }

    private String formatFinalReport(IMethod metodoAlvo, ImpactSummary resumo, String relatorioChamadores, String relatorioCallees, String relatorioOverride, String relatorioEfeitos, String relatorioQueries) {

        StringBuilder sb = new StringBuilder();
        sb.append("Resumo de Impacto de Alteracao").append("\n");
        sb.append("Metodo alvo: ").append(safeQualifiedMethodName(metodoAlvo)).append("\n");
        sb.append("Nivel de risco estimado: ").append(resumo.nivelRisco).append("\n\n");

        sb.append("Sinais consolidados").append("\n");
        sb.append("- Chamadores identificados: ").append(resumo.totalChamadores).append("\n");
        sb.append("- Callees identificados: ").append(resumo.totalCallees).append("\n");
        sb.append("- Relacoes de override com supertipos: ").append(resumo.totalOverrideSuper).append("\n");
        sb.append("- Sobrescritas em subtipos: ").append(resumo.totalOverrideSub).append("\n");
        sb.append("- Evidencias de efeito colateral: ").append(resumo.totalEfeitos).append("\n");
        sb.append("- Evidencias de persistencia/query: ").append(resumo.totalPersistencia).append("\n\n");

        sb.append("Leitura tatica").append("\n");
        if (resumo.temPolimorfismo) {
            sb.append("- O metodo participa de cadeia polimorfica. Alteracoes exigem cautela com heranca, interfaces e comportamento final em runtime.").append("\n");
        }
        if (resumo.temMuitosChamadores) {
            sb.append("- O metodo possui varios chamadores mapeados. Ha potencial de impacto transversal na aplicacao.").append("\n");
        }
        if (resumo.temMuitosCallees) {
            sb.append("- O metodo delega para muitos pontos. Ha risco de acoplamento funcional e efeitos indiretos.").append("\n");
        }
        if (resumo.temEfeitoColateral) {
            sb.append("- Foram localizadas evidencias de mutacao de estado ou efeito externo. Nao trate este metodo como puramente consultivo.").append("\n");
        }
        if (resumo.temPersistencia) {
            sb.append("- Foram localizadas evidencias de persistencia, queries, criteria, JDBC, XML Hibernate ou APIs correlatas.").append("\n");
        }
        if (!resumo.temPolimorfismo && !resumo.temMuitosChamadores && !resumo.temMuitosCallees
                && !resumo.temEfeitoColateral && !resumo.temPersistencia) {
            sb.append("- O metodo parece de impacto relativamente contido no perimetro inspecionado.").append("\n");
        }

        sb.append("\nEvidencias resumidas").append("\n");
        sb.append("\n[Chamadores]\n").append(compact(relatorioChamadores, 1200)).append("\n");
        sb.append("\n[Callees]\n").append(compact(relatorioCallees, 1200)).append("\n");
        sb.append("\n[Override]\n").append(compact(relatorioOverride, 1200)).append("\n");
        sb.append("\n[Efeitos colaterais]\n").append(compact(relatorioEfeitos, 1200)).append("\n");
        sb.append("\n[Persistencia e queries]\n").append(compact(relatorioQueries, 1200)).append("\n");

        sb.append("\nConclusao tatica final").append("\n");
        if ("ALTO".equals(resumo.nivelRisco)) {
            sb.append("Alteracao com risco alto. Antes de editar, valide chamadores, cadeia de override e pontos de persistencia/efeito externo.");
        } else if ("MEDIO".equals(resumo.nivelRisco)) {
            sb.append("Alteracao com risco medio. Ha sinais suficientes para exigir validacao dirigida, mas o impacto ainda parece controlavel.");
        } else {
            sb.append("Alteracao com risco baixo no perimetro observado. Ainda assim, preserve comportamento e valide rapidamente os pontos indiretos.");
        }

        return sb.toString();
    }

    private String inferRisk(ImpactSummary resumo) {
        int score = 0;

        if (resumo.temPolimorfismo) score += 3;
        if (resumo.temMuitosChamadores) score += 3;
        if (resumo.temMuitosCallees) score += 2;
        if (resumo.temEfeitoColateral) score += 3;
        if (resumo.temPersistencia) score += 3;

        if (score >= 8) {
            return "ALTO";
        }
        if (score >= 4) {
            return "MEDIO";
        }
        return "BAIXO";
    }

    private int countEnumeratedItems(String texto) {
        if (isBlank(texto)) {
            return 0;
        }

        int total = 0;
        String[] linhas = texto.split("\\r?\\n");
        for (int i = 0; i < linhas.length; i++) {
            String linha = linhas[i].trim();
            if (linha.matches("^\\d+\\..*")) {
                total++;
            }
        }
        return total;
    }

    private int countOccurrences(String texto, String trecho) {
        if (isBlank(texto) || isBlank(trecho)) {
            return 0;
        }

        int count = 0;
        int idx = 0;
        while ((idx = texto.indexOf(trecho, idx)) >= 0) {
            count++;
            idx += trecho.length();
        }
        return count;
    }

    private int countPersistenceEvidence(String relatorioEfeitos, String relatorioQueries) {
        int total = 0;
        total += countOccurrences(relatorioEfeitos, "PERSISTENCE_WRITE");
        total += countOccurrences(relatorioQueries, "HIBERNATE_API_CALL");
        total += countOccurrences(relatorioQueries, "JPA_API_CALL");
        total += countOccurrences(relatorioQueries, "JDBC_API_CALL");
        total += countOccurrences(relatorioQueries, "SQL_LITERAL");
        total += countOccurrences(relatorioQueries, "HQL_LITERAL");
        total += countOccurrences(relatorioQueries, "HIBERNATE_CRITERIA_API");
        total += countOccurrences(relatorioQueries, "HIBERNATE_CRITERIA_RESTRICTION");
        total += countOccurrences(relatorioQueries, "HIBERNATE_CRITERIA_ORDER");
        total += countOccurrences(relatorioQueries, "HIBERNATE_CRITERIA_PROJECTION");
        total += countOccurrences(relatorioQueries, "JPA_CRITERIA_API");
        total += countOccurrences(relatorioQueries, "JPA_CRITERIA_PREDICATE");
        total += countOccurrences(relatorioQueries, "HBM_QUERY_EVIDENCE");
        total += countOccurrences(relatorioQueries, "HBM_SQL_QUERY_EVIDENCE");
        total += countOccurrences(relatorioQueries, "NAMED_QUERY_USAGE");
        total += countOccurrences(relatorioQueries, "NAMED_QUERY_DECLARATION");
        return total;
    }

    private String buildNamedMethodJson(String classe, String metodo) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"classe\":\"").append(escapeJson(classe)).append("\",");
        sb.append("\"metodo\":\"").append(escapeJson(metodo)).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private String appendField(String jsonBase, String chave, String valor) {
        String semFecho = jsonBase.substring(0, jsonBase.length() - 1);
        return semFecho + ",\"" + chave + "\":\"" + escapeJson(valor) + "\"}";
    }

    private String compact(String texto, int max) {
        if (texto == null) {
            return "";
        }
        String valor = texto.trim();
        if (valor.length() <= max) {
            return valor;
        }
        return valor.substring(0, max) + "\n[RESUMO]: Conteudo truncado para preservar legibilidade.";
    }

    private String safeDeclaringTypeName(IMethod metodo) {
        try {
            if (metodo != null && metodo.getDeclaringType() != null) {
                return metodo.getDeclaringType().getFullyQualifiedName();
            }
        } catch (Exception e) {
        }
        return "";
    }

    private String safeQualifiedMethodName(IMethod metodo) {
        try {
            if (metodo != null && metodo.getDeclaringType() != null) {
                return metodo.getDeclaringType().getFullyQualifiedName() + "." + metodo.getElementName();
            }
        } catch (Exception e) {
        }
        return "MetodoDesconhecido";
    }

    private String escapeJson(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private boolean isBlank(String valor) {
        return valor == null || valor.trim().length() == 0;
    }

    private static class ImpactSummary {
        private int totalChamadores;
        private int totalCallees;
        private int totalOverrideSuper;
        private int totalOverrideSub;
        private int totalEfeitos;
        private int totalPersistencia;

        private boolean temPolimorfismo;
        private boolean temPersistencia;
        private boolean temEfeitoColateral;
        private boolean temMuitosChamadores;
        private boolean temMuitosCallees;

        private String nivelRisco;
    }
}