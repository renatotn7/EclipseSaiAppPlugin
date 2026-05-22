package com.mcp.sailibrary.plugin.agent.context;

import java.util.Set;
import org.eclipse.jdt.core.ICompilationUnit;



/**
 * ---
 * yaml_header:
 * version: "1.2"
 * dependencies: 
 * - org.eclipse.jdt.core
 * purpose: "Coordenador central (General) que gerencia os agentes de escopo, analise e resgate para gerar o breadcrumb."
 * ---
 */
public class ContextOrchestrator {

    private ProjectScopeResolver scopeAgent;
    private WorkspaceFallbackScanAgent scanAgent;
    private String groupIdRaiz;

    /**
 * Descrição não fornecida.
 *
 * @author Renato Tomaz Nati
 */
    /**
 * Descrição não fornecida.
 *
 * @author Renato Tomaz Nati
 */
    public String enraizarChamadas(ICompilationUnit unit, int offset, int maxDepth) {
        this.scopeAgent = new ProjectScopeResolver();
        this.scanAgent = new WorkspaceFallbackScanAgent();
        this.groupIdRaiz = scopeAgent.obterGroupIdRaiz(unit);

        if (unit == null || maxDepth <= 0) return "";
        
        // Feature: Instanciacao do Agente Especialista em AST
        JdtCallTraceAgent astAgent = new JdtCallTraceAgent(scopeAgent, scanAgent, groupIdRaiz);
        
        Set<String> visitados = new java.util.HashSet<String>();
        StringBuilder builder = new StringBuilder();
        
        try {
            // Delega a missao principal para o motor JDT
            astAgent.rastrearRecursivo(unit, offset, 0, maxDepth, visitados, builder);
        } catch (Exception e) {
            System.err.println("Falha na orquestracao: " + e.getMessage());
        }
        
        return builder.toString();
    }

 
    
}