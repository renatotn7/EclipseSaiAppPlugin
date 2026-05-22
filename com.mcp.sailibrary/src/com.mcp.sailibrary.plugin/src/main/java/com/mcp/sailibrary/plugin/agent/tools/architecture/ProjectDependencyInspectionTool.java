package com.mcp.sailibrary.plugin.agent.tools.architecture;

import java.io.File;
import java.io.BufferedReader;
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
import com.mcp.sailibrary.plugin.agent.context.SourceInsightSupport;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolParameterMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadataProvider;

/** * Inspeciona a estrutura de dependencias do projeto resolvendo o POM efetivo * via M2E com fallback defensivo por varredura fisica. * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class ProjectDependencyInspectionTool implements AgentTool, AgentToolPromptMetadataProvider {

    private File rootDirectory;
    private SourceInsightSupport support;

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

    @Override
    public String execute(String jsonParameters) {
        String requestedPath = support.extrairValorVariavel(jsonParameters, "path");

        File pontoInicial = support.resolverPontoInicial(rootDirectory, requestedPath);
        File raizSeguraProjeto = support.localizarRaizSeguraProjeto(pontoInicial, rootDirectory);
        File moduloPreferencial = support.localizarModuloMavenMaisProximo(pontoInicial, raizSeguraProjeto);
        File pomAgregador = support.localizarPomAgregador(moduloPreferencial, raizSeguraProjeto);

        String buildTool = detectarBuildTool(raizSeguraProjeto);
        String branchAtual = detectarBranchAtual(raizSeguraProjeto);

        StringBuilder relatorio = new StringBuilder();
        relatorio.append("Relatorio estrutural do projeto").append("\n");
        relatorio.append("safeRoot: ").append(descreverArquivo(raizSeguraProjeto)).append("\n");
        relatorio.append("moduloPreferencial: ").append(descreverArquivo(moduloPreferencial)).append("\n");
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
            resolvidoM2E = tentarResolverViaM2E(pomAgregador, relatorio, raizSeguraProjeto);
        } catch (Throwable t) {
            resolvidoM2E = false;
        }

        if (!resolvidoM2E) {
            executarFallbackFisico(pomAgregador, relatorio, raizSeguraProjeto);
        }

        return relatorio.toString();
    }

    private void executarFallbackFisico(File pomAgregador, StringBuilder relatorio, File raizSegura) {
        String conteudoPom = support.lerConteudoArquivo(pomAgregador);

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

    private boolean tentarResolverViaM2E(File pomFile, StringBuilder relatorio, File raizSegura) throws Exception {
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IProject[] projects = workspaceRoot.getProjects();
        IProject projetoAlvo = null;

        for (int i = 0; i < projects.length; i++) {
            IProject p = projects[i];
            if (p.isOpen() && p.getLocation() != null) {
                File pFile = p.getLocation().toFile();
                if (pomFile.getAbsolutePath().startsWith(pFile.getAbsolutePath())) {
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

    private String detectarBuildTool(File raizSeguraProjeto) {
        if (raizSeguraProjeto == null) return "desconhecido";
        if (new File(raizSeguraProjeto, "pom.xml").exists()) return "maven";
        if (new File(raizSeguraProjeto, "build.gradle").exists()) return "gradle";
        if (new File(raizSeguraProjeto, "build.gradle.kts").exists()) return "gradle";
        return "desconhecido";
    }

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
        String groupIdProjeto = extrairPrimeiraTagNoEscopoProjeto(conteudoPom, "groupId");
        if (groupIdProjeto != null && groupIdProjeto.length() > 0) return groupIdProjeto;
        return extrairPrimeiraTag(conteudoPom, "groupId");
    }

    private String extrairVersionEfetiva(String conteudoPom) {
        String versionProjeto = extrairPrimeiraTagNoEscopoProjeto(conteudoPom, "version");
        if (versionProjeto != null && versionProjeto.length() > 0) return versionProjeto;
        return extrairPrimeiraTag(conteudoPom, "version");
    }

    private String extrairJavaVersion(String conteudoPom) {
        String valor = extrairValorPropriedade(conteudoPom, "maven.compiler.source");
        if (valor != null && valor.length() > 0) return valor;
        valor = extrairValorPropriedade(conteudoPom, "java.version");
        if (valor != null && valor.length() > 0) return valor;
        valor = extrairValorPropriedade(conteudoPom, "maven.compiler.target");
        if (valor != null && valor.length() > 0) return valor;
        return "1.7";
    }

    private List<String> extrairDependenciasDeclaradas(String conteudoPom) {
        List<String> dependencias = new ArrayList<String>();
        if (conteudoPom == null || conteudoPom.trim().length() == 0) return dependencias;
        Pattern dependencyPattern = Pattern.compile("<dependency>\\s*.*?<groupId>\\s*([^<]+?)\\s*</groupId>\\s*.*?<artifactId>\\s*([^<]+?)\\s*</artifactId>\\s*(?:.*?<version>\\s*([^<]+?)\\s*</version>)?\\s*.*?</dependency>", Pattern.DOTALL);
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

    private String extrairPrimeiraTag(String conteudo, String tag) {
        if (conteudo == null || tag == null || tag.trim().length() == 0) return "";
        Pattern pattern = Pattern.compile("<" + tag + ">\\s*([^<]+?)\\s*</" + tag + ">");
        Matcher matcher = pattern.matcher(conteudo);
        if (matcher.find()) return matcher.group(1).trim();
        return "";
    }

    private String extrairPrimeiraTagNoEscopoParent(String conteudo, String tag) {
        if (conteudo == null || tag == null || tag.trim().length() == 0) return "";
        Pattern parentPattern = Pattern.compile("<parent>([\\s\\S]*?)</parent>", Pattern.DOTALL);
        Matcher parentMatcher = parentPattern.matcher(conteudo);
        if (parentMatcher.find()) {
            String blocoParent = parentMatcher.group(1);
            Pattern tagPattern = Pattern.compile("<" + tag + ">\\s*([^<]+?)\\s*</" + tag + ">");
            Matcher tagMatcher = tagPattern.matcher(blocoParent);
            if (tagMatcher.find()) return tagMatcher.group(1).trim();
        }
        return "";
    }

    private String extrairPrimeiraTagNoEscopoProjeto(String conteudo, String tag) {
        if (conteudo == null || tag == null || tag.trim().length() == 0) return "";
        Pattern projectPattern = Pattern.compile("<project[\\s\\S]*?</project>", Pattern.DOTALL);
        Matcher projectMatcher = projectPattern.matcher(conteudo);
        if (!projectMatcher.find()) return "";
        String blocoProjeto = projectMatcher.group(0);
        Pattern parentPattern = Pattern.compile("<parent[\\s\\S]*?</parent>", Pattern.DOTALL);
        blocoProjeto = parentPattern.matcher(blocoProjeto).replaceFirst("");
        Pattern tagPattern = Pattern.compile("<" + tag + ">\\s*([^<]+?)\\s*</" + tag + ">");
        Matcher tagMatcher = tagPattern.matcher(blocoProjeto);
        if (tagMatcher.find()) return tagMatcher.group(1).trim();
        return "";
    }

    private String extrairValorPropriedade(String conteudo, String nomePropriedade) {
        if (conteudo == null || nomePropriedade == null || nomePropriedade.trim().length() == 0) return "";
        Pattern pattern = Pattern.compile("<" + Pattern.quote(nomePropriedade) + ">\\s*([^<]+?)\\s*</" + Pattern.quote(nomePropriedade) + ">");
        Matcher matcher = pattern.matcher(conteudo);
        if (matcher.find()) return matcher.group(1).trim();
        return "";
    }

    private String streetPath(String path) {
        if (path == null) return "";
        return path.replace("\\\\", "/").replace("\\", "/");
    }

    private String descreverArquivo(File arquivo) {
        if (arquivo == null) return "";
        try {
            return streetPath(arquivo.getCanonicalPath());
        } catch (Exception e) {
            return streetPath(arquivo.getAbsolutePath());
        }
    }

    private String valorSeguro(String valor) {
        if (valor == null) return "";
        return valor.trim();
    }
}