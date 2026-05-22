package com.mcp.sailibrary.plugin.agent.tools.jdt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.tools.support.ToolJsonSupport;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolParameterMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadataProvider;

public class MethodCalleesDiscoveryTool implements AgentTool, AgentToolPromptMetadataProvider {

    private ICompilationUnit compilationUnitAtual;
    private int offsetAtual;

    public MethodCalleesDiscoveryTool(ICompilationUnit compilationUnitAtual, int offsetAtual) {
        this.compilationUnitAtual = compilationUnitAtual;
        this.offsetAtual = offsetAtual;
    }

    @Override
    public String getName() {
        return "buscar_callees_jdt";
    }

    @Override
    public String execute(String jsonParameters) {
        String modo = ToolJsonSupport.extractJsonStringValue(jsonParameters, "modo");
        String nomeClasse = ToolJsonSupport.extractJsonStringValue(jsonParameters, "classe");
        String nomeMetodo = ToolJsonSupport.extractJsonStringValue(jsonParameters, "metodo");
        int limiteResultados = ToolJsonSupport.extractJsonIntValue(jsonParameters, "limite", 30, 1, 200);

        String incluirExternosTexto = ToolJsonSupport.extractJsonStringValue(jsonParameters, "incluir_externos");
        boolean incluirExternos = "true".equalsIgnoreCase(incluirExternosTexto)
                || "sim".equalsIgnoreCase(incluirExternosTexto)
                || "1".equals(incluirExternosTexto);

        IMethod metodoAlvo = resolveTargetMethod(modo, nomeClasse, nomeMetodo);
        if (metodoAlvo == null) {
            return "Erro Operacional: Nao foi possivel localizar o metodo alvo para busca de callees.";
        }

        return findCallees(metodoAlvo, limiteResultados, incluirExternos);
    }
    @Override
    public AgentToolPromptMetadata getPromptMetadata() {
        AgentToolPromptMetadata metadata = new AgentToolPromptMetadata();
        metadata.setToolName(getName());
        metadata.setOneLinePurpose("Mapear os metodos invocados diretamente pelo metodo alvo.");
        metadata.setActivityDescription("Mapeia os metodos invocados diretamente pelo metodo alvo.");

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
        limite.setDescription("Limite maximo de callees retornados.");
        limite.setExampleValue("20");
        metadata.addParameter(limite);

        AgentToolParameterMetadata incluirExternos = new AgentToolParameterMetadata();
        incluirExternos.setName("incluir_externos");
        incluirExternos.setRequired(false);
        incluirExternos.setDescription("Define se chamadas externas ao projeto devem ser retornadas.");
        incluirExternos.setExampleValue("false");
        metadata.addParameter(incluirExternos);

        metadata.addRecommendedUseCase("Use quando precisar mapear delegacoes diretas do metodo atual.");
        metadata.addRecommendedUseCase("Use antes de avaliar impacto funcional de alteracao.");
        metadata.addRecommendedUseCase("Use para descobrir rapidamente para onde o metodo atual encaminha a execucao.");

        metadata.addGuardrail("Nao confunda callees com chamadores.");
        metadata.addGuardrail("Use incluir_externos com cautela para evitar ruido.");
        metadata.addGuardrail("Mantenha o limite sob controle para preservar legibilidade.");

        metadata.addJsonExample(
                "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"buscar_callees_jdt\\\",\\\"parameters\\\":{\\\"modo\\\":\\\"editor_ativo\\\",\\\"limite\\\":\\\"20\\\",\\\"incluir_externos\\\":\\\"false\\\"},\\\"explanation\\\":\\\"Preciso mapear os metodos invocados diretamente pelo metodo atual antes de concluir o impacto.\\\"}"
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
            ASTParser parser = ASTParser.newParser(AST.JLS21);
            parser.setSource(compilationUnitAtual);
            parser.setKind(ASTParser.K_COMPILATION_UNIT);
            parser.setResolveBindings(true);
            parser.setBindingsRecovery(true);
            parser.setProject(compilationUnitAtual.getJavaProject());

            final CompilationUnit astNode = (CompilationUnit) parser.createAST(null);
            final IMethod[] metodoEncontrado = new IMethod[1];

            astNode.accept(new ASTVisitor() {
                @Override
                public boolean visit(MethodDeclaration node) {
                    int inicio = node.getStartPosition();
                    int fim = inicio + node.getLength();

                    if (offsetAtual >= inicio && offsetAtual <= fim) {
                        if (node.resolveBinding() != null && node.resolveBinding().getJavaElement() instanceof IMethod) {
                            metodoEncontrado[0] = (IMethod) node.resolveBinding().getJavaElement();
                            return false;
                        }
                    }

                    return true;
                }
            });

            if (metodoEncontrado[0] != null) {
                return metodoEncontrado[0];
            }

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

    private String findCallees(IMethod metodoAlvo, final int limiteResultados, final boolean incluirExternos) {
        try {
            ICompilationUnit unidade = metodoAlvo.getCompilationUnit();
            if (unidade == null) {
                return "Erro Operacional: O metodo alvo nao possui CompilationUnit acessivel para analise AST.";
            }

            ASTParser parser = ASTParser.newParser(AST.JLS21);
            parser.setSource(unidade);
            parser.setKind(ASTParser.K_COMPILATION_UNIT);
            parser.setResolveBindings(true);
            parser.setBindingsRecovery(true);
            parser.setProject(unidade.getJavaProject());

            final CompilationUnit astNode = (CompilationUnit) parser.createAST(null);
            final MethodDeclaration[] metodoDeclaracao = new MethodDeclaration[1];

            astNode.accept(new ASTVisitor() {
                @Override
                public boolean visit(MethodDeclaration node) {
                    int inicio = node.getStartPosition();
                    int fim = inicio + node.getLength();

                    try {
                        if (metodoAlvo.getSourceRange() != null) {
                            int inicioMetodo = metodoAlvo.getSourceRange().getOffset();
                            int fimMetodo = inicioMetodo + metodoAlvo.getSourceRange().getLength();
                            if (inicio == inicioMetodo && fim == fimMetodo) {
                                metodoDeclaracao[0] = node;
                                return false;
                            }
                        }
                    } catch (Exception e) {
                    }

                    if (node.resolveBinding() != null && node.resolveBinding().getJavaElement() instanceof IMethod) {
                        IMethod metodoNode = (IMethod) node.resolveBinding().getJavaElement();
                        if (mesmaAssinatura(metodoAlvo, metodoNode)) {
                            metodoDeclaracao[0] = node;
                            return false;
                        }
                    }

                    return true;
                }
            });

            if (metodoDeclaracao[0] == null) {
                return "Erro Tatico: Nao foi possivel localizar o corpo AST do metodo alvo para extrair callees.";
            }

            final List<String> resultados = new ArrayList<String>();
            final Set<String> chavesVistas = new HashSet<String>();
            final String fqcnMetodoAlvo = safeQualifiedMethodName(metodoAlvo);

            metodoDeclaracao[0].accept(new ASTVisitor() {
                @Override
                public boolean visit(MethodInvocation node) {
                    if (resultados.size() >= limiteResultados) {
                        return false;
                    }

                    registrarMethodInvocation(astNode, node, fqcnMetodoAlvo, resultados, chavesVistas, incluirExternos);
                    return true;
                }

                @Override
                public boolean visit(SuperMethodInvocation node) {
                    if (resultados.size() >= limiteResultados) {
                        return false;
                    }

                    registrarSuperMethodInvocation(astNode, node, fqcnMetodoAlvo, resultados, chavesVistas, incluirExternos);
                    return true;
                }
            });

            if (resultados.isEmpty()) {
                return "Nenhum callee foi localizado para o metodo alvo [" + metodoAlvo.getElementName() + "].";
            }

            return formatReport(metodoAlvo, limiteResultados, incluirExternos, resultados);
        } catch (Exception e) {
            return "Falha critica durante busca de callees: " + e.getMessage();
        }
    }

    private void registrarMethodInvocation( CompilationUnit astNode, MethodInvocation node, String fqcnMetodoAlvo, List<String> resultados, Set<String> chavesVistas, boolean incluirExternos) {

        String nomeMetodoInvocado = node.getName() != null ? node.getName().getIdentifier() : "MetodoDesconhecido";
        int linha = astNode.getLineNumber(node.getStartPosition());

        IMethodBinding binding = node.resolveMethodBinding();
        if (binding != null && binding.getJavaElement() instanceof IMethod) {
            IMethod metodoInvocado = (IMethod) binding.getJavaElement();

            String fqcnInvocado = safeQualifiedMethodName(metodoInvocado);
            String classificacao = classifyMethod(metodoInvocado, fqcnMetodoAlvo);

            if (!incluirExternos && "EXTERNO".equals(classificacao)) {
                return;
            }

            String chave = classificacao + "|" + fqcnInvocado + "|" + linha;
            if (!chavesVistas.add(chave)) {
                return;
            }

            StringBuilder descricao = new StringBuilder();
            descricao.append(classificacao)
                     .append(" | ")
                     .append(fqcnInvocado)
                     .append(" | linha ")
                     .append(linha);

            Expression expression = node.getExpression();
            if (expression != null) {
                descricao.append(" | receptor=").append(expression.toString());
            }

            resultados.add(descricao.toString());
            return;
        }

        String receptor = node.getExpression() != null ? node.getExpression().toString() : "this";
        String chave = "NAO_RESOLVIDO|" + receptor + "|" + nomeMetodoInvocado + "|" + linha;
        if (!chavesVistas.add(chave)) {
            return;
        }

        resultados.add("NAO_RESOLVIDO | " + receptor + "." + nomeMetodoInvocado + "(...) | linha " + linha);
    }

    private void registrarSuperMethodInvocation( CompilationUnit astNode, SuperMethodInvocation node, String fqcnMetodoAlvo, List<String> resultados, Set<String> chavesVistas, boolean incluirExternos) {

        String nomeMetodoInvocado = node.getName() != null ? node.getName().getIdentifier() : "MetodoDesconhecido";
        int linha = astNode.getLineNumber(node.getStartPosition());

        IMethodBinding binding = node.resolveMethodBinding();
        if (binding != null && binding.getJavaElement() instanceof IMethod) {
            IMethod metodoInvocado = (IMethod) binding.getJavaElement();
            String fqcnInvocado = safeQualifiedMethodName(metodoInvocado);
            String classificacao = classifyMethod(metodoInvocado, fqcnMetodoAlvo);

            if (!incluirExternos && "EXTERNO".equals(classificacao)) {
                return;
            }

            String chave = "SUPER|" + classificacao + "|" + fqcnInvocado + "|" + linha;
            if (!chavesVistas.add(chave)) {
                return;
            }

            resultados.add(classificacao + " | super." + nomeMetodoInvocado + "(...) -> " + fqcnInvocado + " | linha " + linha);
            return;
        }

        String chave = "SUPER|NAO_RESOLVIDO|" + nomeMetodoInvocado + "|" + linha;
        if (!chavesVistas.add(chave)) {
            return;
        }

        resultados.add("NAO_RESOLVIDO | super." + nomeMetodoInvocado + "(...) | linha " + linha);
    }

    private String classifyMethod(IMethod metodoInvocado, String fqcnMetodoAlvo) {
        try {
            String fqcnInvocado = safeQualifiedMethodName(metodoInvocado);

            if (fqcnInvocado.equals(fqcnMetodoAlvo)) {
                return "LOCAL";
            }

            if (metodoInvocado.getCompilationUnit() != null) {
                return "PROJETO";
            }

            IType tipo = metodoInvocado.getDeclaringType();
            if (tipo != null) {
                IJavaElement raiz = tipo.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
                if (raiz instanceof IPackageFragmentRoot) {
                    IPackageFragmentRoot root = (IPackageFragmentRoot) raiz;
                    if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
                        return "PROJETO";
                    }
                }
            }

            return "EXTERNO";
        } catch (Exception e) {
            return "NAO_RESOLVIDO";
        }
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

    private boolean mesmaAssinatura(IMethod a, IMethod b) {
        if (a == null || b == null) {
            return false;
        }

        try {
            if (!a.getElementName().equals(b.getElementName())) {
                return false;
            }

            IType tipoA = a.getDeclaringType();
            IType tipoB = b.getDeclaringType();
            if (tipoA == null || tipoB == null) {
                return false;
            }

            if (!tipoA.getFullyQualifiedName().equals(tipoB.getFullyQualifiedName())) {
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

    private String formatReport(IMethod metodoAlvo, int limiteResultados, boolean incluirExternos, List<String> resultados) {
        StringBuilder relatorio = new StringBuilder();
        relatorio.append("Relatorio de Callees JDT").append("\n");
        relatorio.append("Metodo alvo: ")
                 .append(safeQualifiedMethodName(metodoAlvo))
                 .append("\n");
        relatorio.append("Limite aplicado: ").append(limiteResultados).append("\n");
        relatorio.append("Incluir externos: ").append(incluirExternos ? "sim" : "nao").append("\n\n");

        for (int i = 0; i < resultados.size(); i++) {
            relatorio.append(i + 1).append(". ").append(resultados.get(i)).append("\n");
        }

        if (resultados.size() >= limiteResultados) {
            relatorio.append("\n");
            relatorio.append("[AVISO]: A busca foi interrompida no limite configurado de resultados.");
        }

        return relatorio.toString();
    }

    private boolean isBlank(String valor) {
        return valor == null || valor.trim().length() == 0;
    }
}