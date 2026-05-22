package com.mcp.sailibrary.plugin.agent.tools.jdt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.tools.support.ToolJsonSupport;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolParameterMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadataProvider;

public class MethodOverrideInspectionTool implements AgentTool, AgentToolPromptMetadataProvider {

    private ICompilationUnit compilationUnitAtual;
    private int offsetAtual;

    public MethodOverrideInspectionTool(ICompilationUnit compilationUnitAtual, int offsetAtual) {
        this.compilationUnitAtual = compilationUnitAtual;
        this.offsetAtual = offsetAtual;
    }

    @Override
    public String getName() {
        return "inspecionar_override_metodo";
    }

    @Override
    public String execute(String jsonParameters) {
        String modo = ToolJsonSupport.extractJsonStringValue(jsonParameters, "modo");
        String nomeClasse = ToolJsonSupport.extractJsonStringValue(jsonParameters, "classe");
        String nomeMetodo = ToolJsonSupport.extractJsonStringValue(jsonParameters, "metodo");
        int limiteResultados = ToolJsonSupport.extractJsonIntValue(jsonParameters, "limite", 30, 1, 100);

        IMethod metodoAlvo = resolveTargetMethod(modo, nomeClasse, nomeMetodo);
        if (metodoAlvo == null) {
            return "Erro Operacional: Nao foi possivel localizar o metodo alvo para inspecao de override.";
        }

        return inspectOverrides(metodoAlvo, limiteResultados);
    }
    @Override
    public AgentToolPromptMetadata getPromptMetadata() {
        AgentToolPromptMetadata metadata = new AgentToolPromptMetadata();
        metadata.setToolName(getName());
        metadata.setOneLinePurpose("Inspecionar override, implementacao de contrato e polimorfismo de um metodo.");
        metadata.setActivityDescription("Inspeciona override, implementacao de contrato e sobrescrita por subclasses.");

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

        AgentToolParameterMetadata limite = new AgentToolParameterMetadata();
        limite.setName("limite");
        limite.setRequired(false);
        limite.setDescription("Limite maximo de evidencias retornadas.");
        limite.setExampleValue("20");
        metadata.addParameter(limite);

        metadata.addRecommendedUseCase("Use quando precisar validar participacao polimorfica do metodo.");
        metadata.addRecommendedUseCase("Use quando houver suspeita de implementacao de contrato ou sobrescrita por subclasses.");
        metadata.addRecommendedUseCase("Use antes de alterar metodos que podem ter comportamento diferente em runtime.");

        metadata.addGuardrail("Nao assuma comportamento final apenas pelo metodo base.");
        metadata.addGuardrail("Considere supertipos e subtipos na interpretacao do resultado.");
        metadata.addGuardrail("Use limites prudentes para evitar excesso de evidencias repetitivas.");

        metadata.addJsonExample(
                "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"inspecionar_override_metodo\\\",\\\"parameters\\\":{\\\"classe\\\":\\\"RelatorioAcompanhamentoDivisaoAction\\\",\\\"metodo\\\":\\\"setupEnv\\\",\\\"limite\\\":\\\"20\\\"},\\\"explanation\\\":\\\"Preciso verificar se este metodo implementa contrato, sobrescreve superclasse ou e sobrescrito por subclasses.\\\"}"
        );

        return metadata;
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

    private String inspectOverrides(IMethod metodoAlvo, int limiteResultados) {
        try {
            IType tipoDeclarante = metodoAlvo.getDeclaringType();
            if (tipoDeclarante == null) {
                return "Erro Tatico: O metodo alvo nao possui tipo declarante acessivel.";
            }

            ITypeHierarchy hierarquia = tipoDeclarante.newTypeHierarchy(null);

            List<String> contratosSuperiores = new ArrayList<String>();
            List<String> sobrescritasInferiores = new ArrayList<String>();
            Set<String> vistos = new HashSet<String>();

            inspecionarSupertypes(metodoAlvo, tipoDeclarante, hierarquia, contratosSuperiores, vistos, limiteResultados);
            inspecionarSubtypes(metodoAlvo, tipoDeclarante, hierarquia, sobrescritasInferiores, vistos, limiteResultados);

            return formatReport(metodoAlvo, contratosSuperiores, sobrescritasInferiores, limiteResultados);
        } catch (Exception e) {
            return "Falha critica durante inspecao de override: " + e.getMessage();
        }
    }

    private void inspecionarSupertypes( IMethod metodoAlvo, IType tipoDeclarante, ITypeHierarchy hierarquia, List<String> contratosSuperiores, Set<String> vistos, int limiteResultados) throws Exception {

        IType[] supertypes = hierarquia.getAllSupertypes(tipoDeclarante);
        for (int i = 0; i < supertypes.length; i++) {
            if (contratosSuperiores.size() >= limiteResultados) {
                return;
            }

            IType superTipo = supertypes[i];
            IMethod metodoCorrespondente = findCompatibleMethod(superTipo, metodoAlvo);
            if (metodoCorrespondente != null) {
                String natureza = superTipo.isInterface() ? "IMPLEMENTA_CONTRATO" : "SOBRESCREVE_SUPERCLASSE";
                String chave = natureza + "|" + safeQualifiedMethodName(metodoCorrespondente);
                if (vistos.add(chave)) {
                    contratosSuperiores.add(natureza + " | " + safeQualifiedMethodName(metodoCorrespondente));
                }
            }
        }
    }

    private void inspecionarSubtypes( IMethod metodoAlvo, IType tipoDeclarante, ITypeHierarchy hierarquia, List<String> sobrescritasInferiores, Set<String> vistos, int limiteResultados) throws Exception {

        IType[] subtypes = hierarquia.getAllSubtypes(tipoDeclarante);
        for (int i = 0; i < subtypes.length; i++) {
            if (sobrescritasInferiores.size() >= limiteResultados) {
                return;
            }

            IType subtipo = subtypes[i];
            IMethod metodoCorrespondente = findCompatibleMethod(subtipo, metodoAlvo);
            if (metodoCorrespondente != null) {
                String chave = "SOBRESCRITO_POR|" + safeQualifiedMethodName(metodoCorrespondente);
                if (vistos.add(chave)) {
                    sobrescritasInferiores.add("SOBRESCRITO_POR | " + safeQualifiedMethodName(metodoCorrespondente));
                }
            }
        }
    }

    private IMethod findCompatibleMethod(IType tipo, IMethod metodoBase) {
        if (tipo == null || metodoBase == null) {
            return null;
        }

        try {
            IMethod[] metodos = tipo.getMethods();
            for (int i = 0; i < metodos.length; i++) {
                IMethod candidato = metodos[i];
                if (sameSignature(metodoBase, candidato)) {
                    return candidato;
                }
            }
        } catch (Exception e) {
            return null;
        }

        return null;
    }

    private boolean sameSignature(IMethod a, IMethod b) {
        if (a == null || b == null) {
            return false;
        }

        try {
            if (!a.getElementName().equals(b.getElementName())) {
                return false;
            }

            String[] paramsA = a.getParameterTypes();
            String[] paramsB = b.getParameterTypes();

            if (paramsA == null && paramsB == null) {
                return true;
            }
            if (paramsA == null || paramsB == null) {
                return false;
            }
            if (paramsA.length != paramsB.length) {
                return false;
            }

            for (int i = 0; i < paramsA.length; i++) {
                if (!paramsA[i].equals(paramsB[i])) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String formatReport( IMethod metodoAlvo, List<String> contratosSuperiores, List<String> sobrescritasInferiores, int limiteResultados) {

        StringBuilder relatorio = new StringBuilder();
        relatorio.append("Relatorio de Override JDT").append("\n");
        relatorio.append("Metodo alvo: ").append(safeQualifiedMethodName(metodoAlvo)).append("\n");
        relatorio.append("Limite aplicado: ").append(limiteResultados).append("\n\n");

        if (contratosSuperiores.isEmpty()) {
            relatorio.append("Relacao com supertipos: nenhuma evidenca de override ou implementacao de contrato encontrada.").append("\n");
        } else {
            relatorio.append("Relacao com supertipos:").append("\n");
            for (int i = 0; i < contratosSuperiores.size(); i++) {
                relatorio.append("- ").append(contratosSuperiores.get(i)).append("\n");
            }
        }

        relatorio.append("\n");

        if (sobrescritasInferiores.isEmpty()) {
            relatorio.append("Relacao com subtipos: nenhuma sobrescrita por subclasses foi localizada.").append("\n");
        } else {
            relatorio.append("Relacao com subtipos:").append("\n");
            for (int i = 0; i < sobrescritasInferiores.size(); i++) {
                relatorio.append("- ").append(sobrescritasInferiores.get(i)).append("\n");
            }
        }

        relatorio.append("\nConclusao tática: ");
        if (!contratosSuperiores.isEmpty() && !sobrescritasInferiores.isEmpty()) {
            relatorio.append("o metodo participa de cadeia polimorfica bidirecional. Alteracoes exigem cautela ampliada.");
        } else if (!contratosSuperiores.isEmpty()) {
            relatorio.append("o metodo implementa ou sobrescreve contrato superior. Alteracoes podem impactar coerencia arquitetural.");
        } else if (!sobrescritasInferiores.isEmpty()) {
            relatorio.append("o metodo e sobrescrito por subclasses. Alteracoes na base podem nao refletir o comportamento final em runtime.");
        } else {
            relatorio.append("nao ha indicios fortes de override relevante no perimetro inspecionado.");
        }

        return relatorio.toString();
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

    private boolean isBlank(String valor) {
        return valor == null || valor.trim().length() == 0;
    }
}