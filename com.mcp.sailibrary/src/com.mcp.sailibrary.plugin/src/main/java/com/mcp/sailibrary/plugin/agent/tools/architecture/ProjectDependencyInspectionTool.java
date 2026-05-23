package com.mcp.sailibrary.plugin.agent.tools.architecture;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.context.ResolvedProjectScope;
import com.mcp.sailibrary.plugin.agent.context.SourceInsightSupport;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolParameterMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadataProvider;

/** * Inspeciona a estrutura de dependencias do projeto resolvendo o POM efetivo * via M2E com fallback defensivo por varredura fisica. * * <p>Esta implementacao foi ajustada para respeitar melhor cenarios Maven * multimodulo e multiplos `.project`, diferenciando: * <ul> * <li>raiz segura global</li> * <li>modulo Maven mais proximo</li> * <li>pom agregador</li> * <li>projeto Eclipse mais proximo</li> * </ul> * </p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class ProjectDependencyInspectionTool implements AgentTool, AgentToolPromptMetadataProvider {

    private File rootDirectory;
    private SourceInsightSupport support;

    /** * Inicializa a ferramenta de inspecao de dependencias. * * @param rootDirectory raiz segura do projeto * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public ProjectDependencyInspectionTool(File rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.support = new SourceInsightSupport();
    }

    @Override
    public String getName() {
        return "inspecionar_dependencias_projeto";
    }

    @Override
    public AgentToolPromptMetadata getPromptMetadata() {
        AgentToolPromptMetadata metadata = new AgentToolPromptMetadata();
        metadata.setToolName(getName());
        metadata.setOneLinePurpose("Inspecionar estrutura de build, dependencias, modulos e frameworks do projeto.");
        metadata.setActivityDescription("Inspeciona estrutura de build extraindo groupId, artifactId, javaVersion e dependencias.");

        AgentToolParameterMetadata path = new AgentToolParameterMetadata();
        path.setName("path");
        path.setRequired(false);
        path.setDescription("Caminho relativo inicial para descobrir modulo preferencial e pom agregador.");
        path.setExampleValue("src/main/java");
        metadata.addParameter(path);

        metadata.addRecommendedUseCase("Use quando a IA precisar confirmar build tool, groupId, javaVersion ou dependencias.");
        metadata.addRecommendedUseCase("Use antes de inferir frameworks, modulos Maven ou convencoes de persistencia.");
        metadata.addRecommendedUseCase("Use para reduzir suposicoes sobre stack tecnologica do projeto.");

        metadata.addGuardrail("Sempre trate a resolucao M2E como opcional e preserve fallback defensivo.");
        metadata.addGuardrail("Nao assuma Maven resolvido quando a IDE nao expuser o modelo efetivo.");
        metadata.addGuardrail("Limite a exibicao de dependencias para preservar legibilidade do contexto.");

        metadata.addJsonExample(
                "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"inspecionar_dependencias_projeto\\\",\\\"parameters\\\":{\\\"path\\\":\\\"src/main/java\\\"},\\\"explanation\\\":\\\"Preciso confirmar a estrutura de build, dependencias e frameworks antes de continuar a analise.\\\"}"
        );

        return metadata;
    }

    /** * Executa a inspecao estrutural de dependencias. * * @param jsonParameters parametros JSON da ferramenta * @return relatorio textual da estrutura de build e dependencias * * @author Renato Tomaz Nati * @since 2026-05-20 */
    @Override
    public String execute(String jsonParameters) {
        String requestedPath = support.extrairValorVariavel(jsonParameters, "path");

        File pontoInicial = support.resolverPontoInicial(rootDirectory, requestedPath);
        ResolvedProjectScope scope = support.resolverEscopoProjeto(pontoInicial, rootDirectory);

        if (scope == null || !scope.isUsable()) {
            return "Erro Operacional: Nao foi possivel resolver escopo seguro do projeto para inspecao de dependencias.";
        }

        File raizSeguraProjeto = scope.getSafeRoot();
        File moduloPreferencial = scope.getNearestMavenModuleRoot() != null
                ? scope.getNearestMavenModuleRoot()
                : scope.getEffectiveSearchRoot();
        File pomAgregador = scope.getAggregatorPom();

        String buildTool = detectarBuildTool(raizSeguraProjeto, moduloPreferencial);
        String branchAtual = detectarBranchAtual(raizSeguraProjeto);

        StringBuilder relatorio = new StringBuilder();
        relatorio.append("Relatorio estrutural do projeto").append("\n");
        relatorio.append("safeRoot: ").append(descreverArquivo(raizSeguraProjeto)).append("\n");
        relatorio.append("nearestEclipseProject: ").append(descreverArquivo(scope.getNearestEclipseProjectRoot())).append("\n");
        relatorio.append("nearestEclipseProjectName: ").append(valorSeguro(scope.getNearestEclipseProjectName())).append("\n");
        relatorio.append("moduloPreferencial: ").append(descreverArquivo(moduloPreferencial)).append("\n");
        relatorio.append("groupIdModulo: ").append(valorSeguro(scope.getGroupIdDoModulo())).append("\n");
        relatorio.append("groupIdAgregador: ").append(valorSeguro(scope.getGroupIdDoAgregador())).append("\n");
        relatorio.append("buildTool: ").append(buildTool).append("\n");
        relatorio.append("branchAtual: ").append(branchAtual).append("\n");

        if (!"maven".equals(buildTool) || pomAgregador == null) {
            if ("gradle".equals(buildTool)) {
                relatorio.append("Observacao: build Gradle detectado. Esta versao prioriza leitura de pom Maven.").append("\n");
                return relatorio.toString();
            }
            relatorio.append("Observacao: nenhum build tool suportado foi detectado com seguranca.").append("\n");
            return relatorio.toString();
        }

        relatorio.append("pomAgregador: ").append(descreverArquivo(pomAgregador)).append("\n");

        boolean resolvidoM2E = false;
        try {
            resolvidoM2E = tentarResolverViaM2E(moduloPreferencial, pomAgregador, relatorio);
        } catch (Throwable t) {
            resolvidoM2E = false;
        }

        if (!resolvidoM2E) {
            executarFallbackFisico(moduloPreferencial, pomAgregador, relatorio, raizSeguraProjeto);
        }

        return relatorio.toString();
    }

    /** * Executa fallback fisico e estatico de analise de build quando o M2E nao * estiver disponivel ou falhar. * * @param moduloPreferencial modulo atual mais proximo * @param pomAgregador pom agregador mais alto * @param relatorio acumulador textual * @param raizSegura raiz segura do perimetro * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void executarFallbackFisico(File moduloPreferencial, File pomAgregador, StringBuilder relatorio, File raizSegura) {

        File pomPreferencial = moduloPreferencial != null ? new File(moduloPreferencial, "pom.xml") : null;
        File pomBase = (pomPreferencial != null && pomPreferencial.exists()) ? pomPreferencial : pomAgregador;

        String conteudoPom = support.lerConteudoArquivo(pomBase);

        relatorio.append("groupId: ").append(extrairGroupIdEfetivo(conteudoPom)).append("\n");
        relatorio.append("artifactId: ").append(extrairPrimeiraTag(conteudoPom, "artifactId")).append("\n");
        relatorio.append("version: ").append(extrairVersionEfetiva(conteudoPom)).append("\n");
        relatorio.append("javaVersion: ").append(extrairJavaVersion(conteudoPom)).append("\n");

        List<File> modulos = support.localizarModulosDeclarados(pomAgregador, raizSegura);
        relatorio.append("modulos: ").append("\n");
        if (modulos.isEmpty()) {
            relatorio.append("- nenhum modulo declarado encontrado").append("\n");
        } else {
            for (int i = 0; i < modulos.size(); i++) {
                relatorio.append("- ").append(modulos.get(i).getName()).append("\n");
            }
        }

        List<String> dependencias = extrairDependenciasDeclaradas(conteudoPom);

        String parentGroupId = extrairPrimeiraTagNoEscopoParent(conteudoPom, "groupId");
        String parentArtifactId = extrairPrimeiraTagNoEscopoParent(conteudoPom, "artifactId");
        String parentVersion = extrairPrimeiraTagNoEscopoParent(conteudoPom, "version");

        if (parentGroupId.length() > 0 && parentArtifactId.length() > 0 && parentVersion.length() > 0) {
            String conteudoParentM2 = carregarParentPomDoM2(parentGroupId, parentArtifactId, parentVersion);
            if (conteudoParentM2.length() > 0) {
                List<String> depsParent = extrairDependenciasDeclaradas(conteudoParentM2);
                for (int i = 0; i < depsParent.size(); i++) {
                    if (!dependencias.contains(depsParent.get(i))) {
                        dependencias.add(depsParent.get(i));
                    }
                }
            }
        }

        relatorio.append("dependenciasDeclaradas: ").append("\n");
        if (dependencias.isEmpty()) {
            relatorio.append("- nenhuma dependencia encontrada no fluxo estatico").append("\n");
        } else {
            for (int i = 0; i < dependencias.size() && i < 40; i++) {
                relatorio.append("- ").append(dependencias.get(i)).append("\n");
            }
            if (dependencias.size() > 40) {
                relatorio.append("- [RESUMO] limite de visualizacao estatica atingido").append("\n");
            }
        }

        List<String> frameworks = detectarFrameworksPorDependencia(dependencias);
        relatorio.append("frameworkHints: ").append("\n");
        if (frameworks.isEmpty()) {
            relatorio.append("- nenhum framework evidente pelas dependencias locais").append("\n");
        } else {
            for (int i = 0; i < frameworks.size(); i++) {
                relatorio.append("- ").append(frameworks.get(i)).append("\n");
            }
        }

        relatorio.append("[RESOLUCAO]: Processado via Varredura Estatica Defensiva (M2E Ausente).\n");
    }

    /** * Tenta resolver o modelo efetivo via M2E usando o projeto Eclipse mais * relacionado ao pom/modulo analisado. * * @param moduloPreferencial modulo Maven mais proximo * @param pomFile pom agregador ou pom de referencia * @param relatorio acumulador textual * @return true quando a resolucao via M2E for concluida com sucesso * * @throws Exception quando houver falha grave de acesso ao M2E * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean tentarResolverViaM2E(File moduloPreferencial, File pomFile, StringBuilder relatorio) throws Exception {
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IProject[] projects = workspaceRoot.getProjects();
        IProject projetoAlvo = null;

        File referencia = moduloPreferencial != null ? moduloPreferencial : pomFile != null ? pomFile.getParentFile() : null;
        if (referencia == null) {
            return false;
        }

        for (int i = 0; i < projects.length; i++) {
            IProject p = projects[i];
            if (p.isOpen() && p.getLocation() != null) {
                File pFile = p.getLocation().toFile();
                if (mesmoOuRelacionado(referencia, pFile)) {
                    projetoAlvo = p;
                    break;
                }
            }
        }

        if (projetoAlvo == null) {
            return false;
        }

        IMavenProjectRegistry registry = org.eclipse.m2e.core.MavenPlugin.getMavenProjectRegistry();
        IMavenProjectFacade facade = registry.create(projetoAlvo, new NullProgressMonitor());

        if (facade == null) {
            return false;
        }

        MavenProject mavenProject = facade.getMavenProject(new NullProgressMonitor());
        if (mavenProject == null) {
            return false;
        }

        relatorio.append("groupId: ").append(valorSeguro(mavenProject.getGroupId())).append("\n");
        relatorio.append("artifactId: ").append(valorSeguro(mavenProject.getArtifactId())).append("\n");
        relatorio.append("version: ").append(valorSeguro(mavenProject.getVersion())).append("\n");

        Properties props = mavenProject.getProperties();
        String jVersion = props.getProperty("maven.compiler.source");
        if (jVersion == null) jVersion = props.getProperty("java.version");
        if (jVersion == null) jVersion = props.getProperty("maven.compiler.target");
        if (jVersion == null) jVersion = "1.8";

        relatorio.append("javaVersion: ").append(jVersion).append("\n");

        List<String> modules = mavenProject.getModules();
        relatorio.append("modulos: ").append("\n");
        if (modules == null || modules.isEmpty()) {
            relatorio.append("- nenhum modulo declarado encontrado").append("\n");
        } else {
            for (int i = 0; i < modules.size(); i++) {
                relatorio.append("- ").append(modules.get(i)).append("\n");
            }
        }

        List<org.apache.maven.model.Dependency> deps = mavenProject.getDependencies();
        List<String> dependenciasLista = new ArrayList<String>();
        relatorio.append("dependenciasDeclaradas: ").append("\n");

        if (deps == null || deps.isEmpty()) {
            relatorio.append("- nenhuma dependencia resolvida").append("\n");
        } else {
            for (int i = 0; i < deps.size(); i++) {
                org.apache.maven.model.Dependency d = deps.get(i);
                String linhaDep = d.getGroupId() + ":" + d.getArtifactId() + ":" + d.getVersion();
                dependenciasLista.add(linhaDep);
                if (i < 40) {
                    relatorio.append("- ").append(linhaDep).append("\n");
                }
            }
            if (deps.size() > 40) {
                relatorio.append("- [RESUMO] limite visual de dependencias atingido").append("\n");
            }
        }

        List<String> frameworks = detectarFrameworksPorDependencia(dependenciasLista);
        relatorio.append("frameworkHints: ").append("\n");
        if (frameworks.isEmpty()) {
            relatorio.append("- nenhum framework evidente").append("\n");
        } else {
            for (int i = 0; i < frameworks.size(); i++) {
                relatorio.append("- ").append(frameworks.get(i)).append("\n");
            }
        }

        relatorio.append("[RESOLUCAO]: Processado com sucesso via motor nativo Eclipse M2E.\n");
        return true;
    }

    /** * Carrega o pom do parent a partir do repositorio local `.m2`. * * @param groupId groupId do parent * @param artifactId artifactId do parent * @param version version do parent * @return conteudo do pom ou string vazia * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String carregarParentPomDoM2(String groupId, String artifactId, String version) {
        String userHome = System.getProperty("user.home");
        File m2Repo = new File(userHome, ".m2/repository");
        if (!m2Repo.exists()) {
            return "";
        }

        String groupPath = groupId.replace(".", "/");
        File pastaParent = new File(m2Repo, groupPath + "/" + artifactId + "/" + version);
        File pomParent = new File(pastaParent, artifactId + "-" + version + ".pom");

        if (!pomParent.exists()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(pomParent));
            String linha;
            while ((linha = br.readLine()) != null) {
                sb.append(linha).append("\n");
            }
            br.close();
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /** * Detecta a ferramenta de build mais provavel. * * @param raizSeguraProjeto raiz segura * @param moduloPreferencial modulo preferencial * @return nome da ferramenta de build * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String detectarBuildTool(File raizSeguraProjeto, File moduloPreferencial) {
        if (moduloPreferencial != null) {
            if (new File(moduloPreferencial, "pom.xml").exists()) return "maven";
            if (new File(moduloPreferencial, "build.gradle").exists()) return "gradle";
            if (new File(moduloPreferencial, "build.gradle.kts").exists()) return "gradle";
        }

        if (raizSeguraProjeto != null) {
            if (new File(raizSeguraProjeto, "pom.xml").exists()) return "maven";
            if (new File(raizSeguraProjeto, "build.gradle").exists()) return "gradle";
            if (new File(raizSeguraProjeto, "build.gradle.kts").exists()) return "gradle";
        }

        return "desconhecido";
    }

    /** * Detecta branch atual a partir da raiz segura. * * @param raizSeguraProjeto raiz segura * @return branch atual ou string vazia * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String detectarBranchAtual(File raizSeguraProjeto) {
        if (raizSeguraProjeto == null) return "";
        File gitHead = new File(raizSeguraProjeto, ".git/HEAD");
        if (!gitHead.exists()) return "";
        String conteudo = support.lerConteudoArquivo(gitHead).trim();
        String prefixo = "ref: refs/heads/";
        if (conteudo.startsWith(prefixo)) {
            return conteudo.substring(prefixo.length()).trim();
        }
        return conteudo;
    }

    private String extrairGroupIdEfetivo(String conteudoPom) {
        String groupIdProjeto = support.extrairPrimeiraTagNoEscopoProjeto(conteudoPom, "groupId");
        if (groupIdProjeto != null && groupIdProjeto.length() > 0) return groupIdProjeto;
        return support.extrairPrimeiraTag(conteudoPom, "groupId");
    }

    private String extrairVersionEfetiva(String conteudoPom) {
        String versionProjeto = support.extrairPrimeiraTagNoEscopoProjeto(conteudoPom, "version");
        if (versionProjeto != null && versionProjeto.length() > 0) return versionProjeto;
        return support.extrairPrimeiraTag(conteudoPom, "version");
    }

    private String extrairJavaVersion(String conteudoPom) {
        String valor = support.extrairValorPropriedade(conteudoPom, "maven.compiler.source");
        if (valor != null && valor.length() > 0) return valor;
        valor = support.extrairValorPropriedade(conteudoPom, "java.version");
        if (valor != null && valor.length() > 0) return valor;
        valor = support.extrairValorPropriedade(conteudoPom, "maven.compiler.target");
        if (valor != null && valor.length() > 0) return valor;
        return "1.7";
    }

    private List<String> extrairDependenciasDeclaradas(String conteudoPom) {
        List<String> dependencias = new ArrayList<String>();
        if (conteudoPom == null || conteudoPom.trim().length() == 0) return dependencias;

        Pattern dependencyPattern = Pattern.compile(
                "<dependency>\\s*.*?<groupId>\\s*([^<]+?)\\s*</groupId>\\s*.*?<artifactId>\\s*([^<]+?)\\s*</artifactId>\\s*(?:.*?<version>\\s*([^<]+?)\\s*</version>)?\\s*.*?</dependency>",
                Pattern.DOTALL
        );

        Matcher matcher = dependencyPattern.matcher(conteudoPom);
        while (matcher.find()) {
            String groupId = valorSeguro(matcher.group(1));
            String artifactId = valorSeguro(matcher.group(2));
            String version = "";
            if (matcher.groupCount() >= 3 && matcher.group(3) != null) {
                version = matcher.group(3).trim();
            }

            StringBuilder linha = new StringBuilder();
            linha.append(groupId).append(":").append(artifactId);
            if (version.length() > 0) {
                linha.append(":").append(version);
            }
            dependencias.add(linha.toString());
        }

        return dependencias;
    }

    private List<String> detectarFrameworksPorDependencia(List<String> dependencias) {
        List<String> frameworks = new ArrayList<String>();
        for (int i = 0; i < dependencias.size(); i++) {
            String dependencia = dependencias.get(i).toLowerCase();
            adicionarSeAusente(frameworks, dependencia, "hibernate", "hibernate");
            adicionarSeAusente(frameworks, dependencia, "jakarta.persistence", "jpa");
            adicionarSeAusente(frameworks, dependencia, "javax.persistence", "jpa");
            adicionarSeAusente(frameworks, dependencia, "lombok", "lombok");
            adicionarSeAusente(frameworks, dependencia, "spring", "spring");
            adicionarSeAusente(frameworks, dependencia, "struts", "struts");
            adicionarSeAusente(frameworks, dependencia, "ejb", "ejb");
            adicionarSeAusente(frameworks, dependencia, "ehcache", "ehcache");
        }
        return frameworks;
    }

    private void adicionarSeAusente(List<String> frameworks, String dependencia, String trecho, String nomeFramework) {
        if (dependencia.contains(trecho) && !frameworks.contains(nomeFramework)) {
            frameworks.add(nomeFramework);
        }
    }

    private boolean mesmoOuRelacionado(File a, File b) {
        if (a == null || b == null) {
            return false;
        }

        try {
            String ca = a.getCanonicalPath();
            String cb = b.getCanonicalPath();
            return ca.startsWith(cb) || cb.startsWith(ca);
        } catch (Exception e) {
            return false;
        }
    }

    private String descreverArquivo(File arquivo) {
        if (arquivo == null) return "";
        try {
            return streetPath(arquivo.getCanonicalPath());
        } catch (Exception e) {
            return streetPath(arquivo.getAbsolutePath());
        }
    }

    private String streetPath(String path) {
        if (path == null) return "";
        return path.replace("\\\\", "/").replace("\\", "/");
    }

    private String valorSeguro(String valor) {
        if (valor == null) return "";
        return valor.trim();
    }
    /** * Extrai a primeira ocorrencia simples de uma tag XML textual. * * @param conteudo conteudo XML * @param tag nome da tag * @return valor encontrado ou string vazia * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String extrairPrimeiraTag(String conteudo, String tag) {
        return support.extrairPrimeiraTag(conteudo, tag);
    }

    /** * Extrai a primeira ocorrencia de uma tag dentro do escopo do bloco parent. * * @param conteudo conteudo XML * @param tag nome da tag * @return valor encontrado ou string vazia * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String extrairPrimeiraTagNoEscopoParent(String conteudo, String tag) {
        return support.extrairPrimeiraTagNoEscopoParent(conteudo, tag);
    }
    
}