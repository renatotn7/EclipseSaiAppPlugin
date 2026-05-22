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
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.tools.support.ToolJsonSupport;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolParameterMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadataProvider;

public class SideEffectInspectionTool implements AgentTool, AgentToolPromptMetadataProvider {
    private ICompilationUnit compilationUnitAtual;
    private int offsetAtual;

    public SideEffectInspectionTool(ICompilationUnit compilationUnitAtual, int offsetAtual) {
        this.compilationUnitAtual = compilationUnitAtual;
        this.offsetAtual = offsetAtual;
    }

    @Override
    public String getName() {
        return "inspecionar_efeitos_colaterais";
    }
    @Override
    public AgentToolPromptMetadata getPromptMetadata() {
        AgentToolPromptMetadata metadata = new AgentToolPromptMetadata();
        metadata.setToolName(getName());
        metadata.setOneLinePurpose("Detectar evidencias de mutacao de estado e efeitos externos no metodo alvo.");
        metadata.setActivityDescription("Detecta mutacao de estado, persistencia, I/O, sessao, rede, logging, auditoria e efeitos externos.");

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
        limite.setDescription("Quantidade maxima de evidencias retornadas.");
        limite.setExampleValue("25");
        metadata.addParameter(limite);

        metadata.addRecommendedUseCase("Use quando precisar saber se o metodo e puramente consultivo ou se produz efeitos externos.");
        metadata.addRecommendedUseCase("Use antes de editar metodos com suspeita de mutacao de estado, I/O ou persistencia.");
        metadata.addRecommendedUseCase("Use para diferenciar leitura de dado de operacao com impacto funcional real.");

        metadata.addGuardrail("Nao trate ausencia de evidencia como prova absoluta de pureza.");
        metadata.addGuardrail("Resultados devem ser lidos como indicios tecnicos concretos.");
        metadata.addGuardrail("Combine com query extraction e impact summary quando o metodo for sensivel.");

        metadata.addJsonExample(
                "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"inspecionar_efeitos_colaterais\\\",\\\"parameters\\\":{\\\"modo\\\":\\\"editor_ativo\\\",\\\"limite\\\":\\\"25\\\"},\\\"explanation\\\":\\\"Preciso validar se o metodo atual apenas consulta dados ou se possui mutacao de estado e efeitos externos.\\\"}"
        );

        return metadata;
    }
    @Override
    public String execute(String jsonParameters) {
        String modo = ToolJsonSupport.extractJsonStringValue(jsonParameters, "modo");
        String nomeClasse = ToolJsonSupport.extractJsonStringValue(jsonParameters, "classe");
        String nomeMetodo = ToolJsonSupport.extractJsonStringValue(jsonParameters, "metodo");
        int limiteResultados = ToolJsonSupport.extractJsonIntValue(jsonParameters, "limite", 30, 1, 200);

        IMethod metodoAlvo = resolveTargetMethod(modo, nomeClasse, nomeMetodo);
        if (metodoAlvo == null) {
            return "Erro Operacional: Nao foi possivel localizar o metodo alvo para inspecao de efeitos colaterais.";
        }

        return inspectSideEffects(metodoAlvo, limiteResultados);
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

    private String inspectSideEffects(IMethod metodoAlvo, final int limiteResultados) {
        try {
            ICompilationUnit unidade = metodoAlvo.getCompilationUnit();
            if (unidade == null) {
                return "Erro Operacional: O metodo alvo nao possui CompilationUnit acessivel para inspecao AST.";
            }

            ASTParser parser = ASTParser.newParser(AST.JLS21);
            parser.setSource(unidade);
            parser.setKind(ASTParser.K_COMPILATION_UNIT);
            parser.setResolveBindings(true);
            parser.setBindingsRecovery(true);
            parser.setProject(unidade.getJavaProject());

            final CompilationUnit astNode = (CompilationUnit) parser.createAST(null);
            final MethodDeclaration[] metodoDeclaracao = new MethodDeclaration[1];

            astNode.accept(new org.eclipse.jdt.core.dom.ASTVisitor() {
                @Override
                public boolean visit(MethodDeclaration node) {
                    try {
                        if (node.resolveBinding() != null && node.resolveBinding().getJavaElement() instanceof IMethod) {
                            IMethod metodoNode = (IMethod) node.resolveBinding().getJavaElement();
                            if (sameSignature(metodoAlvo, metodoNode)) {
                                metodoDeclaracao[0] = node;
                                return false;
                            }
                        }
                    } catch (Exception e) {
                    }
                    return true;
                }
            });

            if (metodoDeclaracao[0] == null) {
                return "Erro Tatico: Nao foi possivel localizar o corpo AST do metodo alvo para inspecao de efeitos colaterais.";
            }

            final List<String> evidencias = new ArrayList<String>();
            final Set<String> vistos = new HashSet<String>();

            // Anotacoes do metodo
            extractMethodAnnotations(metodoDeclaracao[0], evidencias, vistos, limiteResultados);

            metodoDeclaracao[0].accept(new org.eclipse.jdt.core.dom.ASTVisitor() {
                @Override
                public boolean visit(Assignment node) {
                    if (evidencias.size() >= limiteResultados) {
                        return false;
                    }

                    String ladoEsquerdo = node.getLeftHandSide() != null ? node.getLeftHandSide().toString() : "";
                    String tipo = classifyAssignmentTarget(node.getLeftHandSide());
                    String chave = tipo + "|" + ladoEsquerdo + "|" + node.getStartPosition();

                    if (vistos.add(chave)) {
                        evidencias.add(tipo + " | linha " + astNode.getLineNumber(node.getStartPosition()) + " | " + ladoEsquerdo + " = ...");
                    }

                    return true;
                }

                @Override
                public boolean visit(VariableDeclarationFragment node) {
                    if (evidencias.size() >= limiteResultados) {
                        return false;
                    }

                    if (node.getInitializer() != null && node.getParent() != null && node.getParent().getParent() instanceof MethodDeclaration) {
                        String texto = node.toString();
                        if (looksLikeResourceAllocation(texto)) {
                            String chave = "RESOURCE_ALLOCATION|" + texto + "|" + node.getStartPosition();
                            if (vistos.add(chave)) {
                                evidencias.add("RESOURCE_ALLOCATION | linha " + astNode.getLineNumber(node.getStartPosition()) + " | " + truncate(texto, 180));
                            }
                        }
                    }
                    return true;
                }

                @Override
                public boolean visit(MethodInvocation node) {
                    if (evidencias.size() >= limiteResultados) {
                        return false;
                    }

                    String nomeMetodoInvocado = node.getName() != null ? node.getName().getIdentifier() : "metodoDesconhecido";
                    String receptor = node.getExpression() != null ? node.getExpression().toString() : "this";
                    String tipo = classifyMethodInvocation(node, nomeMetodoInvocado);

                    if (tipo != null) {
                        String chave = tipo + "|" + receptor + "|" + nomeMetodoInvocado + "|" + node.getStartPosition();
                        if (vistos.add(chave)) {
                            evidencias.add(tipo + " | linha " + astNode.getLineNumber(node.getStartPosition())
                                    + " | " + receptor + "." + nomeMetodoInvocado + "(...)");
                        }
                    }

                    return true;
                }

                @Override
                public boolean visit(ExpressionStatement node) {
                    return true;
                }
            });

            return formatReport(metodoAlvo, evidencias, limiteResultados);
        } catch (Exception e) {
            return "Falha critica durante inspecao de efeitos colaterais: " + e.getMessage();
        }
    }

    private void extractMethodAnnotations(MethodDeclaration metodo, List<String> evidencias, Set<String> vistos, int limite) {
        if (metodo == null || evidencias.size() >= limite) {
            return;
        }

        List<?> modifiers = metodo.modifiers();
        for (int i = 0; i < modifiers.size() && evidencias.size() < limite; i++) {
            Object atual = modifiers.get(i);
            String texto = String.valueOf(atual);

            if ("@Transactional".equals(texto) || texto.startsWith("@Transactional(")) {
                String chave = "TRANSACTION_ANNOTATION|" + texto;
                if (vistos.add(chave)) {
                    evidencias.add("TRANSACTION_ANNOTATION | metodo anotado com @Transactional");
                }
            }
        }
    }

    private String classifyAssignmentTarget(org.eclipse.jdt.core.dom.Expression left) {
        if (left == null) {
            return "STATE_MUTATION";
        }

        if (left instanceof FieldAccess || left instanceof SuperFieldAccess) {
            return "FIELD_STATE_MUTATION";
        }

        if (left instanceof QualifiedName) {
            return "OBJECT_STATE_MUTATION";
        }

        String texto = left.toString();
        if (texto.startsWith("this.")) {
            return "FIELD_STATE_MUTATION";
        }

        return "LOCAL_ASSIGNMENT";
    }

    private String classifyMethodInvocation(MethodInvocation node, String nomeMetodo) {
        if (nomeMetodo == null) {
            return null;
        }

        String metodoLower = nomeMetodo.toLowerCase();
        String receptor = node.getExpression() != null ? node.getExpression().toString().toLowerCase() : "";
        String fullText = node.toString().toLowerCase();

        if (isPersistenceWriteMethod(metodoLower)) {
            return "PERSISTENCE_WRITE";
        }

        if (isCollectionMutationMethod(metodoLower)) {
            return "COLLECTION_MUTATION";
        }

        if (isSessionMutationMethod(metodoLower)) {
            return "SESSION_OR_CONTEXT_MUTATION";
        }

        if (isIoMethod(metodoLower, receptor, fullText)) {
            return "IO_SIDE_EFFECT";
        }

        if (isNetworkMethod(metodoLower, receptor, fullText)) {
            return "NETWORK_SIDE_EFFECT";
        }

        if (isLoggingMethod(metodoLower, receptor, fullText)) {
            return "LOGGING_OR_AUDIT";
        }

        if (isEventMethod(metodoLower, receptor, fullText)) {
            return "EVENT_OR_MESSAGE_EMISSION";
        }

        if (isStateSetterMethod(metodoLower)) {
            return "OBJECT_STATE_MUTATION";
        }

        return null;
    }

    private boolean isPersistenceWriteMethod(String metodoLower) {
        return "save".equals(metodoLower)
                || "update".equals(metodoLower)
                || "merge".equals(metodoLower)
                || "persist".equals(metodoLower)
                || "delete".equals(metodoLower)
                || "remove".equals(metodoLower)
                || "flush".equals(metodoLower)
                || "saveorupdate".equals(metodoLower)
                || "executeupdate".equals(metodoLower)
                || "batchupdate".equals(metodoLower);
    }

    private boolean isCollectionMutationMethod(String metodoLower) {
        return "add".equals(metodoLower)
                || "addall".equals(metodoLower)
                || "remove".equals(metodoLower)
                || "removeall".equals(metodoLower)
                || "put".equals(metodoLower)
                || "putall".equals(metodoLower)
                || "clear".equals(metodoLower)
                || "replace".equals(metodoLower);
    }

    private boolean isSessionMutationMethod(String metodoLower) {
        return "setattribute".equals(metodoLower)
                || "removeattribute".equals(metodoLower)
                || "invalidate".equals(metodoLower)
                || "setsessionattribute".equals(metodoLower);
    }

    private boolean isIoMethod(String metodoLower, String receptor, String fullText) {
        return "write".equals(metodoLower)
                || "append".equals(metodoLower)
                || "print".equals(metodoLower)
                || "println".equals(metodoLower)
                || "mkdir".equals(metodoLower)
                || "mkdirs".equals(metodoLower)
                || "createNewFile".toLowerCase().equals(metodoLower)
                || receptor.contains("file")
                || receptor.contains("writer")
                || receptor.contains("outputstream")
                || receptor.contains("files")
                || fullText.contains("files.write")
                || fullText.contains("new filewriter")
                || fullText.contains("new bufferedwriter");
    }

    private boolean isNetworkMethod(String metodoLower, String receptor, String fullText) {
        return "send".equals(metodoLower)
                || "post".equals(metodoLower)
                || "put".equals(metodoLower)
                || "exchange".equals(metodoLower)
                || receptor.contains("httpclient")
                || receptor.contains("resttemplate")
                || receptor.contains("webclient")
                || fullText.contains("openconnection")
                || fullText.contains("execute(");
    }

    private boolean isLoggingMethod(String metodoLower, String receptor, String fullText) {
        return "info".equals(metodoLower)
                || "warn".equals(metodoLower)
                || "error".equals(metodoLower)
                || "debug".equals(metodoLower)
                || "trace".equals(metodoLower)
                || receptor.contains("logger")
                || receptor.contains("log")
                || fullText.contains("audit");
    }

    private boolean isEventMethod(String metodoLower, String receptor, String fullText) {
        return "publishevent".equals(metodoLower)
                || "notify".equals(metodoLower)
                || "dispatch".equals(metodoLower)
                || "enqueue".equals(metodoLower)
                || receptor.contains("event")
                || receptor.contains("publisher")
                || receptor.contains("producer")
                || receptor.contains("queue")
                || receptor.contains("kafka")
                || receptor.contains("rabbit");
    }

    private boolean isStateSetterMethod(String metodoLower) {
        return metodoLower.startsWith("set") && metodoLower.length() > 3;
    }

    private boolean looksLikeResourceAllocation(String texto) {
        if (texto == null) {
            return false;
        }

        String lower = texto.toLowerCase();
        return lower.contains("new filewriter")
                || lower.contains("new bufferedwriter")
                || lower.contains("new printwriter")
                || lower.contains("new fileoutputstream")
                || lower.contains("new socket")
                || lower.contains("new url(")
                || lower.contains("new resttemplate")
                || lower.contains("new httpclient");
    }

    private String formatReport(IMethod metodoAlvo, List<String> evidencias, int limiteResultados) {
        StringBuilder relatorio = new StringBuilder();
        relatorio.append("Relatorio de Efeitos Colaterais").append("\n");
        relatorio.append("Metodo alvo: ").append(safeQualifiedMethodName(metodoAlvo)).append("\n");
        relatorio.append("Limite aplicado: ").append(limiteResultados).append("\n\n");

        if (evidencias.isEmpty()) {
            relatorio.append("Nenhuma evidencia forte de efeito colateral foi localizada no perimetro inspecionado.").append("\n");
            relatorio.append("Conclusao tatica: o metodo parece predominantemente consultivo ou de baixo impacto externo.");
            return relatorio.toString();
        }

        for (int i = 0; i < evidencias.size(); i++) {
            relatorio.append(i + 1).append(". ").append(evidencias.get(i)).append("\n");
        }

        relatorio.append("\nConclusao tatica: ha evidencias de mutacao de estado, persistencia ou efeito externo. ");
        relatorio.append("Antes de alterar este metodo, valide impacto funcional e risco de regressao.");

        if (evidencias.size() >= limiteResultados) {
            relatorio.append("\n[AVISO]: O limite de resultados foi atingido. Podem existir mais efeitos colaterais nao listados.");
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

    private String truncate(String valor, int max) {
        if (valor == null) {
            return "";
        }
        if (valor.length() <= max) {
            return valor;
        }
        return valor.substring(0, max) + "...";
    }

    private boolean isBlank(String valor) {
        return valor == null || valor.trim().length() == 0;
    }
}