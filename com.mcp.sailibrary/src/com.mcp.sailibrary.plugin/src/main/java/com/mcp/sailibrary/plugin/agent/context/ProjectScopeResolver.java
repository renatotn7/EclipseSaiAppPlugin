package com.mcp.sailibrary.plugin.agent.context;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.ICompilationUnit;

/** * Delimita o perimetro de operacao da IA, identificando escopo estrutural do * projeto, groupId e criterios mais seguros para rastreamento. * * <p>Esta classe foi reforcada para reduzir erros de escopo em projetos Maven * multimodulo e em workspaces com mais de um `.project`. Agora ela consegue * resolver um {@link ResolvedProjectScope} mais rico, sem quebrar os metodos * legados ja usados por outras partes do sistema.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class ProjectScopeResolver {

    private final SourceInsightSupport support;

    /** * Inicializa o resolvedor de escopo do projeto. * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public ProjectScopeResolver() {
        this.support = new SourceInsightSupport();
    }

    /** * Resolve um escopo completo do projeto a partir de uma compilation unit. * * <p>O escopo resolvido distingue: * <ul> * <li>raiz segura global</li> * <li>projeto Eclipse mais proximo</li> * <li>modulo Maven mais proximo</li> * <li>pom agregador</li> * <li>groupId do modulo</li> * <li>groupId do agregador</li> * </ul> * </p> * * @param unit compilation unit atual * @return escopo resolvido ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public ResolvedProjectScope resolveProjectScope(ICompilationUnit unit) {
        if (unit == null || unit.getJavaProject() == null) {
            return null;
        }

        try {
            IProject project = unit.getJavaProject().getProject();
            if (project == null || project.getLocation() == null) {
                return null;
            }

            File startPoint = project.getLocation().toFile();
            return support.resolverEscopoProjeto(startPoint, startPoint);
        } catch (Exception e) {
            return null;
        }
    }

    /** * Resolve um escopo completo do projeto a partir de um diretorio inicial. * * @param startPoint ponto inicial de analise * @return escopo resolvido ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public ResolvedProjectScope resolveProjectScope(File startPoint) {
        if (startPoint == null) {
            return null;
        }

        return support.resolverEscopoProjeto(startPoint, startPoint);
    }

    /** * Retorna o groupId mais relevante do escopo atual. * * <p>Por compatibilidade com a API legada, este metodo continua existindo. * Internamente ele usa o escopo enriquecido e prioriza: * <ol> * <li>groupId do modulo Maven mais proximo</li> * <li>groupId do agregador Maven</li> * </ol> * </p> * * @param unit compilation unit atual * @return groupId mais relevante ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String obterGroupIdRaiz(ICompilationUnit unit) {
        ResolvedProjectScope scope = resolveProjectScope(unit);
        if (scope == null) {
            return null;
        }

        if (!isBlank(scope.getGroupIdDoModulo())) {
            return scope.getGroupIdDoModulo();
        }

        if (!isBlank(scope.getGroupIdDoAgregador())) {
            return scope.getGroupIdDoAgregador();
        }

        return null;
    }

    /** * Decide se uma classe deve ser rastreada com base no escopo resolvido. * * <p>Esta versao e mais precisa do que a heuristica simples por prefixo de * groupId. Ela ainda preserva a assinatura antiga por compatibilidade com * o restante do sistema.</p> * * @param qualifiedName nome qualificado completo da classe * @param groupIdRaiz groupId legado informado * @return true quando a classe aparenta pertencer ao perimetro do projeto * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public boolean deveRastrearClasse(String qualifiedName, String groupIdRaiz) {
        if (qualifiedName == null || qualifiedName.trim().length() == 0) {
            return false;
        }

        if (groupIdRaiz == null || groupIdRaiz.trim().length() == 0) {
            return true;
        }

        if (qualifiedName.startsWith(groupIdRaiz)) {
            return true;
        }

        String prefixoModulo = extrairPrefixoEstrutural(groupIdRaiz);
        if (prefixoModulo.length() > 0 && qualifiedName.startsWith(prefixoModulo)) {
            return true;
        }

        return false;
    }

    /** * Decide se uma classe deve ser rastreada com base no escopo completo. * * @param qualifiedName nome qualificado completo da classe * @param scope escopo resolvido do projeto * @return true quando a classe aparenta pertencer ao perimetro do projeto * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public boolean deveRastrearClasse(String qualifiedName, ResolvedProjectScope scope) {
        if (qualifiedName == null || qualifiedName.trim().length() == 0) {
            return false;
        }

        if (scope == null) {
            return true;
        }

        if (!isBlank(scope.getGroupIdDoModulo()) && qualifiedName.startsWith(scope.getGroupIdDoModulo())) {
            return true;
        }

        if (!isBlank(scope.getGroupIdDoAgregador()) && qualifiedName.startsWith(scope.getGroupIdDoAgregador())) {
            return true;
        }

        if (!isBlank(scope.getNearestEclipseProjectName())) {
            String prefixoProjeto = extrairPrefixoEstrutural(scope.getNearestEclipseProjectName());
            if (prefixoProjeto.length() > 0 && qualifiedName.toLowerCase().contains(prefixoProjeto.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /** * Extrai prefixo estrutural simples a partir de um identificador de * projeto/groupId. * * @param valor valor de origem * @return prefixo simplificado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String extrairPrefixoEstrutural(String valor) {
        if (valor == null) {
            return "";
        }

        String normalizado = valor.trim();
        if (normalizado.length() == 0) {
            return "";
        }

        int ultimoPonto = normalizado.lastIndexOf('.');
        if (ultimoPonto > 0) {
            return normalizado.substring(0, ultimoPonto);
        }

        return normalizado;
    }

    /** * Retorna true quando o valor informado for nulo ou vazio. * * @param valor valor a validar * @return true quando o valor for branco * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean isBlank(String valor) {
        return valor == null || valor.trim().length() == 0;
    }
}