package com.mcp.sailibrary.plugin.agent.context;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.QualifiedName;

/**
 * ---
 * yaml_header:
 * version: "1.0"
 * dependencies: 
 * - org.eclipse.jdt.core: "3.38.0"
 * purpose: "Agente especialista em motor JDT. Analisa a arvore sintatica (AST) e resolve bindings de metodos e tipos."
 * ---
 */
public class JdtCallTraceAgent {
	private static final String AST_CACHE_SESSION_KEY = "compilationUnitCacheV4";
    private ProjectScopeResolver scopeAgent;
    private WorkspaceFallbackScanAgent scanAgent;
    private String groupIdRaiz;

    public JdtCallTraceAgent(ProjectScopeResolver scope, WorkspaceFallbackScanAgent scan, String groupId) {
        this.scopeAgent = scope;
        this.scanAgent = scan;
        this.groupIdRaiz = groupId;
    }

    /**
 * Descrição não fornecida.
 *
 * @author Renato Tomaz Nati
 */
    /**
 * Executa rastreamento recursivo ancorado no metodo exato do offset, com cache invalidavel e sem vazar para tipos internos.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-18
 */
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
                    processarChamadaComBinding(binding, profundidadeAtual, profundidadeMaxima, visitados, builder);
                } else {
                    processarChamadaSemBinding(node, astNodeFinal, unit, profundidadeAtual, profundidadeMaxima, visitados, builder);
                }

                return true;
            }
        });
    }
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

    private void processarChamadaComBinding(IMethodBinding binding, int nivel, int max, Set<String> visitados, StringBuilder builder) {
        ITypeBinding typeBinding = binding.getDeclaringClass();
        if (typeBinding == null) return;

        if (scopeAgent.deveRastrearClasse(typeBinding.getQualifiedName(), groupIdRaiz)) {
            IJavaElement element = binding.getJavaElement();
            if (element instanceof IMethod) {
                IMethod method = (IMethod) element;
                try {
                    if (typeBinding.isInterface()) {
                        scanAgent.buscarImplementacoesTipo(typeBinding.getJavaElement().getAdapter(IType.class), method.getElementName(), nivel, max, visitados, builder, this);
                    } else {
                        rastrearRecursivo(method.getCompilationUnit(), method.getNameRange().getOffset(), nivel + 1, max, visitados, builder);
                    }
                } catch (Exception e) {}
            }
        }
    }

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
            } else {
                
                if (tipoObjeto != null) {
                    buscarMetodoEmTipoProjeto(unit, tipoObjeto, nomeMetodo, nivel, max, visitados, builder);
                }
            }
        } catch (Exception e) {}
    }

    public void buscarEmHierarquiaLocal(ICompilationUnit unit, String nomeMetodo, int nivel, int max, Set<String> visitados, StringBuilder builder) throws Exception {
        IType type = unit.findPrimaryType();
        if (type == null) return;
        if (scanAgent.buscarMetodoEmTipo(type, nomeMetodo, nivel, max, visitados, builder, this)) return;
        
        ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
        IType[] superTypes = hierarchy.getAllSupertypes(type);
        for (int i = 0; i < superTypes.length; i++) {
            if (superTypes[i].getElementName().equals("Object")) continue;
            if (scanAgent.buscarMetodoEmTipo(superTypes[i], nomeMetodo, nivel, max, visitados, builder, this)) return;
        }
    }

    public void buscarMetodoEmTipoProjeto(ICompilationUnit unit, String nomeTipo, String nomeMetodo, int nivel, int max, Set<String> visitados, StringBuilder builder) {
        try {
        	String tipoNormalizado = scanAgent.normalizarNomeTipo(nomeTipo);

        	if (scanAgent.deveIgnorarTipoNaoProjeto(tipoNormalizado)) {
        	    scanAgent.registrarIgnoradoUmaVez(tipoNormalizado, nomeMetodo);
        	    return;
        	}
            IType type = unit.getJavaProject().findType(nomeTipo);
            if (type == null) type = encontrarTipoNoProjeto(unit, nomeTipo);
            
            if (type != null) {
                if (type.isInterface()) {
                    scanAgent.buscarImplementacoesTipo(type, nomeMetodo, nivel, max, visitados, builder, this);
                } else {
                    scanAgent.buscarMetodoEmTipo(type, nomeMetodo, nivel, max, visitados, builder, this);
                }
            } else {
                scanAgent.buscarForcaBrutaMaven(unit, nomeTipo, nomeMetodo, nivel, max, visitados, builder, this);
            }
        } catch (Exception e) {}
    }

    public String resolverTipoReceptor(Expression receiver, CompilationUnit ast, ICompilationUnit unit) {
        if (receiver == null) return null;
        ITypeBinding typeBinding = receiver.resolveTypeBinding();
        if (typeBinding != null) {
            return scanAgent.normalizarNomeTipo(typeBinding.getQualifiedName());
        }

        return scanAgent.normalizarNomeTipo(resolverTipoCampoNaClasse(ast, unit, receiver.toString()));
    }

    private String resolverTipoCampoNaClasse(CompilationUnit ast, final ICompilationUnit unit, final String fieldName) {
        final List<String> result = new ArrayList<String>();
        ast.accept(new ASTVisitor() {
            @Override
            public boolean visit(VariableDeclarationFragment node) {
                if (node.getName().getIdentifier().equals(fieldName)) {
                    IVariableBinding b = node.resolveBinding();
                    if (b != null && b.getType() != null) result.add(b.getType().getQualifiedName());
                }
                return true;
            }
        });
        return result.isEmpty() ? null : result.get(0);
    }

    public IType encontrarTipoNoProjeto(ICompilationUnit unit, String nomeSimples) {
        try {
            IPackageFragment[] packages = unit.getJavaProject().getPackageFragments();
            for (int i = 0; i < packages.length; i++) {
                if (packages[i].getKind() == IPackageFragmentRoot.K_SOURCE) {
                    for (ICompilationUnit cu : packages[i].getCompilationUnits()) {
                        for (IType type : cu.getAllTypes()) {
                            if (type.getElementName().equals(nomeSimples)) return type;
                        }
                    }
                }
            }
        } catch (Exception e) {}
        return null;
    }
    private static class AstCacheEntry {
        private CompilationUnit astNode;
        private long modificationStamp;
        private int sourceLength;
    }
    /**
 * Retorna AST do arquivo usando cache invalidado por modificationStamp e tamanho do fonte.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-18
 */
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
    /**
 * Localiza o metodo mais interno e exato que contem o offset selecionado.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-18
 */
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
    /**
 * Le modificationStamp do recurso sem quebrar o fluxo se o recurso nao existir.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-18
 */
    private long obterModificationStampSeguro(ICompilationUnit unit) {
        try {
            if (unit != null && unit.getResource() != null) {
                return unit.getResource().getModificationStamp();
            }
        } catch (Exception e) {
        }
        return -1L;
    }
    /**
 * Usa tamanho do fonte como segunda linha de defesa para invalidacao do cache.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-18
 */
    private int obterSourceLengthSeguro(ICompilationUnit unit) {
        try {
            if (unit != null && unit.getSource() != null) {
                return unit.getSource().length();
            }
        } catch (Exception e) {
        }
        return -1;
    }
}