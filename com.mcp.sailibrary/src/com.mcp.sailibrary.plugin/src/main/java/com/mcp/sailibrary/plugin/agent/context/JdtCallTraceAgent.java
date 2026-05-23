package com.mcp.sailibrary.plugin.agent.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

/** * Agente especialista em motor JDT. Analisa a arvore sintatica (AST) e resolve * bindings de metodos e tipos. * * <p>Esta implementacao foi reforcada para reduzir ambiguidades em projetos com * varios modulos e tipos homonimos, priorizando o projeto Java atual e apenas * depois abrindo fallback controlado. O agente tambem recebe um escopo * resolvido do projeto para ajudar a restringir buscas e evitar contaminacao * por modulos ou projetos Eclipse errados.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class JdtCallTraceAgent {

    private static final String AST_CACHE_SESSION_KEY = "compilationUnitCacheV4";

    private final ProjectScopeResolver scopeAgent;
    private final WorkspaceFallbackScanAgent scanAgent;
    private final String groupIdRaiz;
    private final ResolvedProjectScope resolvedProjectScope;

    /** * Inicializa o agente JDT com suporte a escopo e fallback. * * @param scope agente de escopo * @param scan agente de fallback * @param groupId groupId raiz percebido * @param resolvedProjectScope escopo resolvido do projeto atual * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public JdtCallTraceAgent(ProjectScopeResolver scope, WorkspaceFallbackScanAgent scan, String groupId, ResolvedProjectScope resolvedProjectScope) {
        this.scopeAgent = scope;
        this.scanAgent = scan;
        this.groupIdRaiz = groupId;
        this.resolvedProjectScope = resolvedProjectScope;
    }

    /** * Executa rastreamento recursivo ancorado no metodo exato do offset, com * cache invalidavel e sem vazar para tipos internos. * * @param unit compilation unit atual * @param offset offset de origem * @param profundidadeAtual profundidade atual * @param profundidadeMaxima profundidade maxima * @param visitados chaves de deduplicacao * @param builder acumulador textual * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void rastrearRecursivo(final ICompilationUnit unit, final int offset, final int profundidadeAtual, final int profundidadeMaxima, final Set<String> visitados, final StringBuilder builder) {
        if (profundidadeAtual > profundidadeMaxima || unit == null) {
            return;
        }

        final CompilationUnit astNodeFinal = obterOuCriarAstComCache(unit);
        if (astNodeFinal == null) {
            return;
        }

        final MethodDeclaration metodoAlvo = localizarMetodoAlvoMaisInterno(astNodeFinal, offset);
        if (metodoAlvo == null) {
            return;
        }

        registrarMetodoNoBuilder(metodoAlvo, unit, profundidadeAtual, visitados, builder);

        metodoAlvo.accept(new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {
                return false;
            }

            @Override
            public boolean visit(AnonymousClassDeclaration node) {
                return false;
            }

            @Override
            public boolean visit(LambdaExpression node) {
                return false;
            }

            @Override
            public boolean visit(EnumDeclaration node) {
                return false;
            }

            @Override
            public boolean visit(AnnotationTypeDeclaration node) {
                return false;
            }

            @Override
            public boolean visit(MethodInvocation node) {
                if (profundidadeAtual >= profundidadeMaxima) {
                    return false;
                }

                IMethodBinding binding = node.resolveMethodBinding();
                if (binding != null) {
                    processarChamadaComBinding(binding, unit, profundidadeAtual, profundidadeMaxima, visitados, builder);
                } else {
                    processarChamadaSemBinding(node, astNodeFinal, unit, profundidadeAtual, profundidadeMaxima, visitados, builder);
                }

                return true;
            }
        });
    }

    /** * Registra o metodo atual no builder textual do breadcrumb. * * @param node declaracao de metodo * @param unit compilation unit corrente * @param nivel nivel atual * @param visitados chaves de deduplicacao * @param builder acumulador textual * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void registrarMetodoNoBuilder(MethodDeclaration node, ICompilationUnit unit, int nivel, Set<String> visitados, StringBuilder builder) {
        String nomeMetodo = node.getName().getIdentifier();
        String chave = unit.getElementName() + "#" + nomeMetodo;
        if (visitados.add(chave)) {
            builder.append("\n// Feature: Contexto Enraizado - Nivel ").append(nivel).append("\n");
            builder.append("// Arquivo: ").append(unit.getElementName()).append("\n");
            builder.append("// Metodo: ").append(nomeMetodo).append("\n");
            builder.append(node.toString()).append("\n");
        }
    }

    /** * Processa chamada com binding resolvido, priorizando contexto do projeto * atual. * * @param binding binding do metodo invocado * @param unidadeAtual unidade atual de referencia * @param nivel nivel atual * @param max profundidade maxima * @param visitados chaves de deduplicacao * @param builder acumulador textual * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void processarChamadaComBinding(IMethodBinding binding, ICompilationUnit unidadeAtual, int nivel, int max, Set<String> visitados, StringBuilder builder) {
        ITypeBinding typeBinding = binding.getDeclaringClass();
        if (typeBinding == null) {
            return;
        }

        boolean deveRastrear;
        if (resolvedProjectScope != null) {
            deveRastrear = scopeAgent.deveRastrearClasse(typeBinding.getQualifiedName(), resolvedProjectScope);
        } else {
            deveRastrear = scopeAgent.deveRastrearClasse(typeBinding.getQualifiedName(), groupIdRaiz);
        }

        if (!deveRastrear) {
            return;
        }

        IJavaElement element = binding.getJavaElement();
        if (!(element instanceof IMethod)) {
            return;
        }

        IMethod method = (IMethod) element;
        try {
            if (typeBinding.isInterface()) {
                IType interfaceType = null;
                if (typeBinding.getJavaElement() instanceof IType) {
                    interfaceType = (IType) typeBinding.getJavaElement();
                }
                if (interfaceType != null) {
                    scanAgent.buscarImplementacoesTipo(interfaceType, method.getElementName(), nivel, max, visitados, builder, this);
                }
            } else {
                ICompilationUnit targetUnit = method.getCompilationUnit();
                if (targetUnit != null) {
                    rastrearRecursivo(targetUnit, method.getNameRange().getOffset(), nivel + 1, max, visitados, builder);
                }
            }
        } catch (Exception e) {
        }
    }

    /** * Processa chamada sem binding resolvido, aplicando heuristicas locais e * fallback controlado. * * @param node invocacao atual * @param ast AST corrente * @param unit unidade de compilacao atual * @param nivel nivel atual * @param max profundidade maxima * @param visitados chaves de deduplicacao * @param builder acumulador textual * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void processarChamadaSemBinding(MethodInvocation node, CompilationUnit ast, ICompilationUnit unit, int nivel, int max, Set<String> visitados, StringBuilder builder) {
        String nomeMetodo = node.getName().getIdentifier();
        Expression receiver = node.getExpression();
        String tipoObjeto = resolverTipoReceptor(receiver, ast, unit);
        tipoObjeto = scanAgent.normalizarNomeTipo(tipoObjeto);

        if (scanAgent.deveIgnorarTipoNaoProjeto(tipoObjeto)) {
            scanAgent.registrarIgnoradoUmaVez(tipoObjeto, nomeMetodo);
            return;
        }

        try {
            if (receiver == null) {
                buscarEmHierarquiaLocal(unit, nomeMetodo, nivel, max, visitados, builder);
            } else if (tipoObjeto != null) {
                buscarMetodoEmTipoProjeto(unit, tipoObjeto, nomeMetodo, nivel, max, visitados, builder);
            }
        } catch (Exception e) {
        }
    }

    /** * Busca metodo na hierarquia local da classe atual. * * @param unit unidade atual * @param nomeMetodo nome do metodo * @param nivel nivel atual * @param max profundidade maxima * @param visitados chaves de deduplicacao * @param builder acumulador textual * * @throws Exception quando houver falha grave de leitura JDT * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void buscarEmHierarquiaLocal(ICompilationUnit unit, String nomeMetodo, int nivel, int max, Set<String> visitados, StringBuilder builder) throws Exception {
        IType type = unit.findPrimaryType();
        if (type == null) {
            return;
        }

        if (scanAgent.buscarMetodoEmTipo(type, nomeMetodo, nivel, max, visitados, builder, this)) {
            return;
        }

        ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
        IType[] superTypes = hierarchy.getAllSupertypes(type);
        for (int i = 0; i < superTypes.length; i++) {
            if (superTypes[i].getElementName().equals("Object")) {
                continue;
            }
            if (scanAgent.buscarMetodoEmTipo(superTypes[i], nomeMetodo, nivel, max, visitados, builder, this)) {
                return;
            }
        }
    }

    /** * Busca metodo em tipo do projeto, priorizando projeto atual e fallback * Maven controlado. * * @param unit unidade atual * @param nomeTipo nome do tipo * @param nomeMetodo nome do metodo * @param nivel nivel atual * @param max profundidade maxima * @param visitados chaves de deduplicacao * @param builder acumulador textual * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void buscarMetodoEmTipoProjeto(ICompilationUnit unit, String nomeTipo, String nomeMetodo, int nivel, int max, Set<String> visitados, StringBuilder builder) {
        try {
            String tipoNormalizado = scanAgent.normalizarNomeTipo(nomeTipo);

            if (scanAgent.deveIgnorarTipoNaoProjeto(tipoNormalizado)) {
                scanAgent.registrarIgnoradoUmaVez(tipoNormalizado, nomeMetodo);
                return;
            }

            IType type = null;

            if (unit != null && unit.getJavaProject() != null) {
                type = unit.getJavaProject().findType(tipoNormalizado);
            }

            if (type == null) {
                type = encontrarTipoNoProjeto(unit, tipoNormalizado);
            }

            if (type != null) {
                if (type.isInterface()) {
                    scanAgent.buscarImplementacoesTipo(type, nomeMetodo, nivel, max, visitados, builder, this);
                } else {
                    scanAgent.buscarMetodoEmTipo(type, nomeMetodo, nivel, max, visitados, builder, this);
                }
            } else {
                scanAgent.buscarForcaBrutaMaven(unit, tipoNormalizado, nomeMetodo, nivel, max, visitados, builder, this);
            }
        } catch (Exception e) {
        }
    }

    /** * Resolve o tipo do receptor com binding direto ou fallback por campo * localmente resolvido. * * @param receiver expressao receptora * @param ast AST corrente * @param unit unidade atual * @return nome qualificado do tipo receptor ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String resolverTipoReceptor(Expression receiver, CompilationUnit ast, ICompilationUnit unit) {
        if (receiver == null) {
            return null;
        }

        ITypeBinding typeBinding = receiver.resolveTypeBinding();
        if (typeBinding != null) {
            return scanAgent.normalizarNomeTipo(typeBinding.getQualifiedName());
        }

        return scanAgent.normalizarNomeTipo(resolverTipoCampoNaClasse(ast, unit, receiver.toString()));
    }

    /** * Resolve tipo de campo dentro da classe atual por binding AST. * * @param ast AST atual * @param unit compilation unit atual * @param fieldName nome do campo * @return nome qualificado do tipo ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String resolverTipoCampoNaClasse(CompilationUnit ast, final ICompilationUnit unit, final String fieldName) {
        final List<String> result = new ArrayList<String>();
        ast.accept(new ASTVisitor() {
            @Override
            public boolean visit(VariableDeclarationFragment node) {
                if (node.getName().getIdentifier().equals(fieldName)) {
                    IVariableBinding b = node.resolveBinding();
                    if (b != null && b.getType() != null) {
                        result.add(b.getType().getQualifiedName());
                    }
                }
                return true;
            }
        });
        return result.isEmpty() ? null : result.get(0);
    }

    /** * Busca tipo por nome simples dentro do projeto Java atual. * * @param unit compilation unit atual * @param nomeSimples nome simples do tipo * @return tipo encontrado ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public IType encontrarTipoNoProjeto(ICompilationUnit unit, String nomeSimples) {
        try {
            if (unit == null || unit.getJavaProject() == null) {
                return null;
            }

            IJavaProject javaProject = unit.getJavaProject();
            IPackageFragment[] packages = javaProject.getPackageFragments();
            for (int i = 0; i < packages.length; i++) {
                if (packages[i].getKind() == IPackageFragmentRoot.K_SOURCE) {
                    for (ICompilationUnit cu : packages[i].getCompilationUnits()) {
                        for (IType type : cu.getAllTypes()) {
                            if (type.getElementName().equals(nomeSimples)) {
                                return type;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    /** * Retorna AST do arquivo usando cache invalidado por modificationStamp e * tamanho do fonte. * * @param unit compilation unit atual * @return AST compilada ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private CompilationUnit obterOuCriarAstComCache(ICompilationUnit unit) {
        java.util.Map<ICompilationUnit, AstCacheEntry> compilationUnitCache = null;

        try {
            IProject projetoAtual = unit.getJavaProject() != null ? unit.getJavaProject().getProject() : null;
            if (projetoAtual != null) {
                QualifiedName chaveCache = new QualifiedName(JdtCallTraceAgent.class.getName(), AST_CACHE_SESSION_KEY);
                Object valorArmazenado = projetoAtual.getSessionProperty(chaveCache);

                if (valorArmazenado instanceof java.util.Map) {
                    compilationUnitCache = (java.util.Map<ICompilationUnit, AstCacheEntry>) valorArmazenado;
                } else {
                    compilationUnitCache = java.util.Collections.synchronizedMap(new java.util.HashMap<ICompilationUnit, AstCacheEntry>());
                    projetoAtual.setSessionProperty(chaveCache, compilationUnitCache);
                }
            }
        } catch (Exception e) {
            compilationUnitCache = null;
        }

        long modificationStampAtual = obterModificationStampSeguro(unit);
        int sourceLengthAtual = obterSourceLengthSeguro(unit);

        if (compilationUnitCache != null) {
            AstCacheEntry cacheEntry = compilationUnitCache.get(unit);
            if (cacheEntry != null
                    && cacheEntry.astNode != null
                    && cacheEntry.modificationStamp == modificationStampAtual
                    && cacheEntry.sourceLength == sourceLengthAtual) {
                return cacheEntry.astNode;
            }
        }

        ASTParser parser = ASTParser.newParser(AST.JLS21);
        parser.setSource(unit);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);
        parser.setProject(unit.getJavaProject());

        CompilationUnit novoAst = (CompilationUnit) parser.createAST(null);

        if (compilationUnitCache != null && novoAst != null) {
            AstCacheEntry novoEntry = new AstCacheEntry();
            novoEntry.astNode = novoAst;
            novoEntry.modificationStamp = modificationStampAtual;
            novoEntry.sourceLength = sourceLengthAtual;
            compilationUnitCache.put(unit, novoEntry);
        }

        return novoAst;
    }

    /** * Localiza o metodo mais interno e exato que contem o offset selecionado. * * @param astNode AST da compilation unit * @param offset offset selecionado * @return metodo mais interno encontrado ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private MethodDeclaration localizarMetodoAlvoMaisInterno(CompilationUnit astNode, final int offset) {
        final MethodDeclaration[] metodoEncontrado = new MethodDeclaration[1];
        final int[] menorComprimento = new int[] { Integer.MAX_VALUE };

        astNode.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                int start = node.getStartPosition();
                int end = start + node.getLength();

                if (offset >= start && offset <= end) {
                    int comprimentoAtual = node.getLength();
                    if (comprimentoAtual < menorComprimento[0]) {
                        menorComprimento[0] = comprimentoAtual;
                        metodoEncontrado[0] = node;
                    }
                }

                return true;
            }
        });

        return metodoEncontrado[0];
    }

    /** * Le modificationStamp do recurso sem quebrar o fluxo se o recurso nao * existir. * * @param unit compilation unit atual * @return modification stamp ou -1 * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private long obterModificationStampSeguro(ICompilationUnit unit) {
        try {
            if (unit != null && unit.getResource() != null) {
                return unit.getResource().getModificationStamp();
            }
        } catch (Exception e) {
        }
        return -1L;
    }

    /** * Usa tamanho do fonte como segunda linha de defesa para invalidacao do * cache. * * @param unit compilation unit atual * @return tamanho do fonte ou -1 * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private int obterSourceLengthSeguro(ICompilationUnit unit) {
        try {
            if (unit != null && unit.getSource() != null) {
                return unit.getSource().length();
            }
        } catch (Exception e) {
        }
        return -1;
    }

    /** * Estrutura interna de cache AST por compilation unit. * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private static class AstCacheEntry {
        private CompilationUnit astNode;
        private long modificationStamp;
        private int sourceLength;
    }
}