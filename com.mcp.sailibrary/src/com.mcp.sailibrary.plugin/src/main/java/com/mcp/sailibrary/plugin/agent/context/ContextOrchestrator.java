package com.mcp.sailibrary.plugin.agent.context;

import java.util.Set;

import org.eclipse.jdt.core.ICompilationUnit;

/** * Coordenador central que gerencia os agentes de escopo, analise e fallback * para gerar o breadcrumb estrutural do ponto atual. * * <p>Esta implementacao foi reforcada para trabalhar com um escopo resolvido de * projeto, diferenciando melhor: * <ul> * <li>raiz segura global</li> * <li>projeto Eclipse mais proximo</li> * <li>modulo Maven mais proximo</li> * <li>groupId mais relevante</li> * </ul> * </p> * * <p>O objetivo e reduzir ambiguidades em workspaces multimodulo sem quebrar o * fluxo atual de rastreamento baseado em AST e fallback fisico.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class ContextOrchestrator {

    private ProjectScopeResolver scopeAgent;
    private WorkspaceFallbackScanAgent scanAgent;
    private String groupIdRaiz;
    private ResolvedProjectScope resolvedProjectScope;

    /** * Executa o enraizamento de chamadas a partir de uma compilation unit e de * um offset inicial. * * <p>O metodo resolve primeiro o escopo estrutural do projeto para reduzir * risco de buscas em modulo ou projeto Eclipse errado. Em seguida aciona o * agente AST principal com suporte a fallback controlado.</p> * * @param unit compilation unit atual * @param offset offset de origem * @param maxDepth profundidade maxima de rastreamento * @return breadcrumb estrutural produzido * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String enraizarChamadas(ICompilationUnit unit, int offset, int maxDepth) {
        if (unit == null || maxDepth <= 0) {
            return "";
        }

        this.scopeAgent = new ProjectScopeResolver();
        this.scanAgent = new WorkspaceFallbackScanAgent();
        this.resolvedProjectScope = scopeAgent.resolveProjectScope(unit);
        this.groupIdRaiz = scopeAgent.obterGroupIdRaiz(unit);

        JdtCallTraceAgent astAgent = new JdtCallTraceAgent(
                scopeAgent,
                scanAgent,
                groupIdRaiz,
                resolvedProjectScope
        );

        Set<String> visitados = new java.util.HashSet<String>();
        StringBuilder builder = new StringBuilder();

        try {
            astAgent.rastrearRecursivo(unit, offset, 0, maxDepth, visitados, builder);
        } catch (Exception e) {
            System.err.println("Falha na orquestracao: " + e.getMessage());
        }

        return builder.toString();
    }

    /** * Retorna o escopo de projeto resolvido na ultima execucao. * * @return escopo resolvido ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public ResolvedProjectScope getResolvedProjectScope() {
        return resolvedProjectScope;
    }
}