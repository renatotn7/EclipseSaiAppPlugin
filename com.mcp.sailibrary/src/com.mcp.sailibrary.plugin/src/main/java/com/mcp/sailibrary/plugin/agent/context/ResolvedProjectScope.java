package com.mcp.sailibrary.plugin.agent.context;

import java.io.File;

/** * Representa o escopo estrutural resolvido de um projeto ou modulo para uso * seguro em investigacao e analise. * * <p>Este objeto separa explicitamente conceitos que antes se confundiam: * <ul> * <li>raiz segura global</li> * <li>projeto Eclipse mais proximo</li> * <li>nome do projeto Eclipse</li> * <li>modulo Maven mais proximo</li> * <li>pom agregador mais alto dentro do perimetro</li> * <li>groupId do modulo</li> * <li>groupId do agregador</li> * <li>raiz efetiva de busca</li> * </ul> * </p> * * <p>O objetivo principal e evitar que ferramentas de analise tratem “raiz * segura”, “modulo Maven” e “projeto Eclipse” como se fossem sempre o mesmo * diretorio.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class ResolvedProjectScope {

    private File safeRoot;
    private File nearestEclipseProjectRoot;
    private String nearestEclipseProjectName;
    private File nearestMavenModuleRoot;
    private File aggregatorPom;
    private String groupIdDoModulo;
    private String groupIdDoAgregador;
    private File effectiveSearchRoot;

    /** * Retorna a raiz segura global do perimetro analisado. * * @return raiz segura global * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File getSafeRoot() {
        return safeRoot;
    }

    public void setSafeRoot(File safeRoot) {
        this.safeRoot = safeRoot;
    }

    /** * Retorna o projeto Eclipse mais proximo do ponto analisado. * * @return diretorio contendo o `.project` mais proximo * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File getNearestEclipseProjectRoot() {
        return nearestEclipseProjectRoot;
    }

    public void setNearestEclipseProjectRoot(File nearestEclipseProjectRoot) {
        this.nearestEclipseProjectRoot = nearestEclipseProjectRoot;
    }

    /** * Retorna o nome do projeto Eclipse mais proximo. * * @return nome do projeto Eclipse * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String getNearestEclipseProjectName() {
        return nearestEclipseProjectName;
    }

    public void setNearestEclipseProjectName(String nearestEclipseProjectName) {
        this.nearestEclipseProjectName = safeTrim(nearestEclipseProjectName);
    }

    /** * Retorna o modulo Maven mais proximo do ponto analisado. * * @return diretorio contendo o `pom.xml` mais proximo * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File getNearestMavenModuleRoot() {
        return nearestMavenModuleRoot;
    }

    public void setNearestMavenModuleRoot(File nearestMavenModuleRoot) {
        this.nearestMavenModuleRoot = nearestMavenModuleRoot;
    }

    /** * Retorna o pom agregador mais alto dentro do perimetro seguro. * * @return arquivo pom agregador * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File getAggregatorPom() {
        return aggregatorPom;
    }

    public void setAggregatorPom(File aggregatorPom) {
        this.aggregatorPom = aggregatorPom;
    }

    /** * Retorna o groupId mais relevante do modulo Maven mais proximo. * * @return groupId do modulo * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String getGroupIdDoModulo() {
        return groupIdDoModulo;
    }

    public void setGroupIdDoModulo(String groupIdDoModulo) {
        this.groupIdDoModulo = safeTrim(groupIdDoModulo);
    }

    /** * Retorna o groupId do pom agregador, quando houver. * * @return groupId do agregador * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String getGroupIdDoAgregador() {
        return groupIdDoAgregador;
    }

    public void setGroupIdDoAgregador(String groupIdDoAgregador) {
        this.groupIdDoAgregador = safeTrim(groupIdDoAgregador);
    }

    /** * Retorna a raiz efetiva de busca a ser preferida pelas ferramentas. * * <p>Em geral, este campo deve apontar para o modulo Maven mais proximo e, * na ausencia dele, para o projeto Eclipse mais proximo ou para a raiz * segura.</p> * * @return raiz efetiva de busca * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File getEffectiveSearchRoot() {
        return effectiveSearchRoot;
    }

    public void setEffectiveSearchRoot(File effectiveSearchRoot) {
        this.effectiveSearchRoot = effectiveSearchRoot;
    }

    /** * Retorna true quando o escopo possui dados minimos para uso seguro. * * @return true quando houver pelo menos raiz segura e raiz efetiva de busca * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public boolean isUsable() {
        return safeRoot != null
                && effectiveSearchRoot != null
                && effectiveSearchRoot.exists();
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}