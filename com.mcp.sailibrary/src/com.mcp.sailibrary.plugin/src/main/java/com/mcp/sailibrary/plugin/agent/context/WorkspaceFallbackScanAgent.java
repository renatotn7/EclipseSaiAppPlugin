package com.mcp.sailibrary.plugin.agent.context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

/** * Realiza varredura fisica e fallback estrutural no workspace para localizar * implementacoes fora do classpath ideal da IDE. * * <p>Esta classe foi reforcada para reduzir falsos positivos em projetos Maven * multimodulo e em workspaces com varios projetos Eclipse abertos. O fallback * agora prioriza: * <ul> * <li>o projeto Eclipse atual</li> * <li>projetos relacionados ao mesmo perimetro fisico</li> * <li>o modulo Maven mais proximo</li> * <li>resolucao por package quando disponivel</li> * </ul> * </p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class WorkspaceFallbackScanAgent {

    private static final Set<String> tiposIgnoradosJaLogados = new HashSet<String>();

    /** * Tenta localizar um metodo pelo nome dentro de um tipo e, se encontrado, * delega o rastreamento recursivo ao agente AST. * * @param type tipo alvo * @param nomeMetodo nome do metodo desejado * @param nivel nivel atual do rastreamento * @param max profundidade maxima * @param visitados chaves de deduplicacao * @param builder acumulador textual do breadcrumb * @param astAgent agente AST principal * @return true quando o metodo for encontrado e processado * * @throws Exception quando houver falha grave de leitura JDT * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public boolean buscarMetodoEmTipo(IType type, String nomeMetodo, int nivel, int max, Set<String> visitados, StringBuilder builder, JdtCallTraceAgent astAgent) throws Exception {
        ICompilationUnit unit = type.getCompilationUnit();
        if (unit == null) {
            return false;
        }

        IMethod[] methods = type.getMethods();
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].getElementName().equals(nomeMetodo)) {
                astAgent.rastrearRecursivo(unit, methods[i].getNameRange().getOffset(), nivel + 1, max, visitados, builder);
                return true;
            }
        }
        return false;
    }

    /** * Busca implementacoes concretas de uma interface usando primeiro a * hierarquia JDT e depois fallback controlado por varredura do workspace. * * @param interfaceType interface alvo * @param nomeMetodo nome do metodo procurado * @param nivel nivel atual do rastreamento * @param max profundidade maxima * @param visitados chaves de deduplicacao * @param builder acumulador textual * @param astAgent agente AST principal * * @throws Exception quando houver falha grave de analise * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void buscarImplementacoesTipo(IType interfaceType, String nomeMetodo, int nivel, int max, Set<String> visitados, StringBuilder builder, JdtCallTraceAgent astAgent) throws Exception {

        ITypeHierarchy hierarchy = interfaceType.newTypeHierarchy(interfaceType.getJavaProject(), null);
        IType[] implementations = hierarchy.getAllSubtypes(interfaceType);

        for (int i = 0; i < implementations.length; i++) {
            if (!implementations[i].isInterface()) {
                if (buscarMetodoEmTipo(implementations[i], nomeMetodo, nivel, max, visitados, builder, astAgent)) {
                    return;
                }
            }
        }

        buscarImplementacaoPorForcaBrutaWorkspace(interfaceType, nomeMetodo, nivel, max, visitados, builder, astAgent);
    }

    /** * Fallback de implementacao por varredura controlada de projetos Java do * workspace, priorizando o projeto atual e projetos fisicamente * relacionados. * * @param interfaceType interface alvo * @param nomeMetodo metodo desejado * @param nivel nivel atual do rastreamento * @param max profundidade maxima * @param visitados chaves de deduplicacao * @param builder acumulador textual * @param astAgent agente AST principal * @return true quando encontrar implementacao compativel * * @throws Exception quando houver falha grave de leitura JDT * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public boolean buscarImplementacaoPorForcaBrutaWorkspace(IType interfaceType, String nomeMetodo, int nivel, int max, Set<String> visitados, StringBuilder builder, JdtCallTraceAgent astAgent) throws Exception {

        if (interfaceType == null) {
            return false;
        }

        IProject[] orderedProjects = ordenarProjetosJavaPorRelevancia(interfaceType.getJavaProject());

        String nomeInterface = interfaceType.getElementName();
        String fqcnInterface = interfaceType.getFullyQualifiedName();

        for (int p = 0; p < orderedProjects.length; p++) {
            IProject projetoAtual = orderedProjects[p];

            if (!projetoAtual.isOpen() || !projetoAtual.hasNature(JavaCore.NATURE_ID)) {
                continue;
            }

            IJavaProject javaProject = JavaCore.create(projetoAtual);
            for (IPackageFragment pkg : javaProject.getPackageFragments()) {
                if (pkg.getKind() != IPackageFragmentRoot.K_SOURCE) {
                    continue;
                }

                for (ICompilationUnit cu : pkg.getCompilationUnits()) {
                    for (IType type : cu.getAllTypes()) {
                        if (!type.isInterface() && contemInterface(type, nomeInterface, fqcnInterface)) {
                            if (buscarMetodoEmTipo(type, nomeMetodo, nivel, max, visitados, builder, astAgent)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    /** * Verifica se um tipo implementa a interface alvo, priorizando FQCN quando * disponivel e usando nome simples apenas como fallback. * * @param type tipo candidato * @param nomeInterface nome simples da interface * @param fqcnInterface nome completo da interface * @return true quando houver indicio forte de implementacao * * @throws Exception quando houver falha de leitura JDT * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean contemInterface(IType type, String nomeInterface, String fqcnInterface) throws Exception {
        String[] interfaces = type.getSuperInterfaceNames();
        for (int i = 0; i < interfaces.length; i++) {
            String atual = interfaces[i];
            if (!isBlank(fqcnInterface) && (atual.equals(fqcnInterface) || atual.endsWith("." + fqcnInterface))) {
                return true;
            }
            if (atual.equals(nomeInterface) || atual.endsWith("." + nomeInterface)) {
                return true;
            }
        }
        return false;
    }

    /** * Fallback fisico de busca de classe Java por nome de tipo no projeto * relacionado ao compilation unit atual. * * @param unit compilation unit atual * @param nomeClasse nome da classe * @param nomeMetodo metodo a localizar * @param nivel nivel atual * @param max profundidade maxima * @param visitados chaves de deduplicacao * @param builder acumulador textual * @return true quando a extracao do metodo concreto for concluida * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public boolean buscarForcaBrutaMaven(ICompilationUnit unit, String nomeClasse, String nomeMetodo, int nivel, int max, Set<String> visitados, StringBuilder builder) {
        if (unit == null || unit.getJavaProject() == null) {
            return false;
        }

        try {
            File currentProjectDir = unit.getJavaProject().getProject().getLocation().toFile();
            File moduleRoot = localizarModuloMavenMaisProximo(currentProjectDir);
            File searchBase = moduleRoot != null ? moduleRoot : currentProjectDir;

            File arquivoLocal = localizarArquivoJavaPorTipo(searchBase, nomeClasse);

            if (arquivoLocal != null) {
                if (extrairMetodoDeArquivoFisico(arquivoLocal, nomeMetodo, nivel, max, visitados, builder) == 1) {
                    return true;
                }
            }
        } catch (Exception e) {
        }

        return false;
    }

    /** * Fallback fisico de busca Maven com normalizacao previa do nome do tipo e * filtro de tipos externos irrelevantes. * * @param unit compilation unit atual * @param nomeClasse nome da classe * @param nomeMetodo nome do metodo * @param nivel nivel atual * @param max profundidade maxima * @param visitados chaves de deduplicacao * @param builder acumulador textual * @param astAgent agente AST principal * @return true quando encontrar implementacao concreta * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public boolean buscarForcaBrutaMaven(ICompilationUnit unit, String nomeClasse, String nomeMetodo, int nivel, int max, Set<String> visitados, StringBuilder builder, JdtCallTraceAgent astAgent) {

        String tipoNormalizado = normalizarNomeTipo(nomeClasse);

        if (deveIgnorarTipoNaoProjeto(tipoNormalizado)) {
            registrarIgnoradoUmaVez(tipoNormalizado, nomeMetodo);
            return false;
        }

        return buscarForcaBrutaMaven(unit, tipoNormalizado, nomeMetodo, nivel, max, visitados, builder);
    }

    /** * Tenta localizar um arquivo Java a partir de nome de tipo, primeiro por * package/FQCN e depois por busca recursiva de nome simples. * * @param baseDir diretorio base da busca * @param nomeClasse nome simples ou FQCN do tipo * @return arquivo Java encontrado ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private File localizarArquivoJavaPorTipo(File baseDir, String nomeClasse) {
        if (baseDir == null || !baseDir.exists()) {
            return null;
        }

        String tipoNormalizado = normalizarNomeTipo(nomeClasse);
        if (isBlank(tipoNormalizado)) {
            return null;
        }

        if (tipoNormalizado.contains(".")) {
            File candidato = new File(baseDir, tipoNormalizado.replace(".", "/") + ".java");
            if (candidato.exists() && candidato.isFile()) {
                return candidato;
            }
        }

        String nomeSimples = extrairNomeSimples(tipoNormalizado);
        return localizarArquivoRecursivo(baseDir, nomeSimples + ".java");
    }

    /** * Localiza recursivamente um arquivo por nome simples dentro de um * diretorio base. * * @param dir diretorio base * @param nome nome do arquivo * @return arquivo encontrado ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private File localizarArquivoRecursivo(File dir, String nome) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return null;
        }

        File[] arquivos = dir.listFiles();
        if (arquivos != null) {
            for (int i = 0; i < arquivos.length; i++) {
                if (arquivos[i].isDirectory()) {
                    if (shouldIgnoreDirectory(arquivos[i].getName())) {
                        continue;
                    }

                    File achou = localizarArquivoRecursivo(arquivos[i], nome);
                    if (achou != null) {
                        return achou;
                    }
                } else if (arquivos[i].getName().equals(nome)) {
                    return arquivos[i];
                }
            }
        }
        return null;
    }

    /** * Extrai o metodo concreto de um arquivo Java fisico via AST textual. * * @param arquivoJava arquivo Java * @param nomeMetodoProcurado nome do metodo * @param nivel nivel atual * @param max profundidade maxima * @param visitados chaves de deduplicacao * @param builder acumulador textual * @return 0 quando nao localizar, 1 quando localizar metodo com corpo, 2 * quando localizar assinatura sem corpo * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private static int extrairMetodoDeArquivoFisico(final File arquivoJava, final String nomeMetodoProcurado, final int nivel, final int max, final Set<String> visitados, final StringBuilder builder) {

        String conteudo = lerConteudoArquivo(arquivoJava);
        if (conteudo == null || conteudo.trim().length() == 0) {
            return 0;
        }

        ASTParser parser = ASTParser.newParser(AST.JLS21);
        parser.setSource(conteudo.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        final CompilationUnit astNode = (CompilationUnit) parser.createAST(null);
        final int[] status = {0};

        astNode.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                if (node.getName().getIdentifier().equals(nomeMetodoProcurado)) {
                    if (node.getBody() == null) {
                        status[0] = 2;
                    } else {
                        String chaveFisica = arquivoJava.getAbsolutePath() + "#" + nomeMetodoProcurado;
                        if (visitados.add(chaveFisica)) {
                            builder.append("\n// ==========================================\n");
                            builder.append("// Feature: Contexto Enraizado (Forca Bruta Maven) - Nivel ").append(nivel).append("\n");
                            builder.append("// Arquivo Fisico: ").append(arquivoJava.getAbsolutePath()).append("\n");
                            builder.append("// Metodo: ").append(nomeMetodoProcurado).append("\n");
                            builder.append("// ==========================================\n");
                            builder.append(node.toString()).append("\n");
                            System.out.println("[TELEMETRIA JDT] [SUCESSO] Metodo concreto injetado via Maven: " + chaveFisica);
                            status[0] = 1;
                        }
                    }
                }
                return super.visit(node);
            }
        });

        return status[0];
    }

    /** * Le o conteudo textual de um arquivo com fallback seguro. * * @param arquivo arquivo de origem * @return conteudo textual ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private static String lerConteudoArquivo(File arquivo) {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(arquivo));
            String linha;
            while ((linha = br.readLine()) != null) {
                sb.append(linha).append("\n");
            }
            br.close();
        } catch (Exception e) {
            return null;
        }
        return sb.toString();
    }

    /** * Normaliza nome de tipo removendo generics, arrays e wildcard bounds. * * @param nomeTipo nome do tipo original * @return nome do tipo normalizado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String normalizarNomeTipo(String nomeTipo) {
        if (nomeTipo == null) {
            return null;
        }

        String tipoNormalizado = nomeTipo.trim();

        int indiceGenerics = tipoNormalizado.indexOf('<');
        if (indiceGenerics != -1) {
            tipoNormalizado = tipoNormalizado.substring(0, indiceGenerics).trim();
        }

        while (tipoNormalizado.endsWith("[]")) {
            tipoNormalizado = tipoNormalizado.substring(0, tipoNormalizado.length() - 2).trim();
        }

        if (tipoNormalizado.startsWith("? extends ")) {
            tipoNormalizado = tipoNormalizado.substring("? extends ".length()).trim();
        }

        if (tipoNormalizado.startsWith("? super ")) {
            tipoNormalizado = tipoNormalizado.substring("? super ".length()).trim();
        }

        return tipoNormalizado;
    }

    /** * Decide se um tipo deve ser ignorado por pertencer claramente ao mundo * externo/infraestrutural e nao ao projeto relevante. * * @param nomeTipo tipo a validar * @return true quando o tipo deve ser ignorado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public boolean deveIgnorarTipoNaoProjeto(String nomeTipo) {
        if (nomeTipo == null || nomeTipo.trim().length() == 0) {
            return true;
        }

        String tipo = normalizarNomeTipo(nomeTipo);

        if ("String".equals(tipo)) return true;
        if ("StringBuffer".equals(tipo)) return true;
        if ("StringBuilder".equals(tipo)) return true;
        if ("Boolean".equals(tipo)) return true;
        if ("Integer".equals(tipo)) return true;
        if ("Long".equals(tipo)) return true;
        if ("Double".equals(tipo)) return true;
        if ("Float".equals(tipo)) return true;
        if ("Short".equals(tipo)) return true;
        if ("Byte".equals(tipo)) return true;
        if ("Character".equals(tipo)) return true;
        if ("Object".equals(tipo)) return true;

        if ("Collection".equals(tipo)) return true;
        if ("List".equals(tipo)) return true;
        if ("Set".equals(tipo)) return true;
        if ("Map".equals(tipo)) return true;
        if ("Iterator".equals(tipo)) return true;
        if ("Iterable".equals(tipo)) return true;

        if ("HttpServletRequest".equals(tipo)) return true;
        if ("HttpServletResponse".equals(tipo)) return true;
        if ("HttpSession".equals(tipo)) return true;
        if ("ServletContext".equals(tipo)) return true;

        if ("ActionMessages".equals(tipo)) return true;
        if ("ActionMessage".equals(tipo)) return true;
        if ("DispatchAction".equals(tipo)) return true;

        if (tipo.startsWith("java.")) return true;
        if (tipo.startsWith("javax.")) return true;
        if (tipo.startsWith("jakarta.")) return true;
        if (tipo.startsWith("org.apache.struts.")) return true;
        if (tipo.startsWith("org.apache.")) return true;

        return false;
    }

    /** * Registra uma unica vez a decisao de ignorar um tipo externo. * * @param nomeTipo nome do tipo * @param nomeMetodo metodo associado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void registrarIgnoradoUmaVez(String nomeTipo, String nomeMetodo) {
        String chave = normalizarNomeTipo(nomeTipo) + "#" + nomeMetodo;
        if (tiposIgnoradosJaLogados.add(chave)) {
            System.out.println("[TELEMETRIA JDT] [SKIP EXTERNO] Ignorando tipo nao projeto: " + nomeTipo + "." + nomeMetodo);
        }
    }

    /** * Ordena projetos Java por relevancia fisica em relacao ao projeto atual. * * @param projetoAtual projeto Java atual * @return vetor ordenado de projetos Eclipse * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private IProject[] ordenarProjetosJavaPorRelevancia(IJavaProject projetoAtual) {
        IProject[] todos = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        java.util.List<IProject> relacionados = new java.util.ArrayList<IProject>();
        java.util.List<IProject> restantes = new java.util.ArrayList<IProject>();

        File raizAtual = descobrirRaizSeguraProjeto(projetoAtual);

        for (int i = 0; i < todos.length; i++) {
            IProject projeto = todos[i];
            try {
                if (!projeto.isOpen() || !projeto.hasNature(JavaCore.NATURE_ID)) {
                    continue;
                }

                if (projetoAtual != null && projeto.equals(projetoAtual.getProject())) {
                    relacionados.add(projeto);
                    continue;
                }

                File raizProjeto = projeto.getLocation() != null ? projeto.getLocation().toFile() : null;
                if (raizAtual != null && raizProjeto != null && saoRelacionados(raizAtual, raizProjeto)) {
                    relacionados.add(projeto);
                } else {
                    restantes.add(projeto);
                }
            } catch (Exception e) {
            }
        }

        java.util.List<IProject> ordenados = new java.util.ArrayList<IProject>();
        ordenados.addAll(relacionados);
        ordenados.addAll(restantes);
        return ordenados.toArray(new IProject[ordenados.size()]);
    }

    /** * Descobre a raiz segura de um projeto Java atual. * * @param projetoAtual projeto Java atual * @return raiz segura detectada ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private File descobrirRaizSeguraProjeto(IJavaProject projetoAtual) {
        try {
            if (projetoAtual == null || projetoAtual.getProject() == null || projetoAtual.getProject().getLocation() == null) {
                return null;
            }

            File cursor = projetoAtual.getProject().getLocation().toFile();
            File melhor = cursor;

            while (cursor != null && cursor.exists()) {
                if (possuiMarcadorRaizProjeto(cursor)) {
                    melhor = cursor;
                }
                cursor = cursor.getParentFile();
            }

            return melhor;
        } catch (Exception e) {
            return null;
        }
    }

    /** * Retorna true quando dois caminhos fisicos pertencem ao mesmo perimetro * geral. * * @param a primeiro caminho * @param b segundo caminho * @return true quando houver relacao de ancestralidade ou igualdade * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean saoRelacionados(File a, File b) {
        try {
            String ca = a.getCanonicalPath();
            String cb = b.getCanonicalPath();
            return ca.startsWith(cb) || cb.startsWith(ca);
        } catch (Exception e) {
            return false;
        }
    }

    /** * Localiza o modulo Maven mais proximo subindo a arvore. * * @param start ponto inicial * @return diretorio do modulo mais proximo ou o proprio start * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private File localizarModuloMavenMaisProximo(File start) {
        File cursor = start;
        if (cursor != null && cursor.isFile()) {
            cursor = cursor.getParentFile();
        }

        while (cursor != null && cursor.exists()) {
            File pom = new File(cursor, "pom.xml");
            if (pom.exists() && pom.isFile()) {
                return cursor;
            }
            cursor = cursor.getParentFile();
        }

        return start;
    }

    /** * Retorna true quando o diretorio aparenta ser uma raiz valida de projeto. * * @param diretorio diretorio candidato * @return true quando houver .git ou .project * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean possuiMarcadorRaizProjeto(File diretorio) {
        if (diretorio == null || !diretorio.exists() || !diretorio.isDirectory()) {
            return false;
        }

        File gitDir = new File(diretorio, ".git");
        if (gitDir.exists()) {
            return true;
        }

        File eclipseProject = new File(diretorio, ".project");
        return eclipseProject.exists();
    }

    /** * Extrai nome simples de um tipo Java ou FQCN. * * @param nomeTipo nome do tipo * @return nome simples * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String extrairNomeSimples(String nomeTipo) {
        if (isBlank(nomeTipo)) {
            return "";
        }

        int idx = nomeTipo.lastIndexOf('.');
        if (idx < 0) {
            return nomeTipo;
        }

        return nomeTipo.substring(idx + 1);
    }

    /** * Define se um diretorio deve ser ignorado na busca recursiva fisica. * * @param nome nome da pasta * @return true quando a pasta for ruido tecnico * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean shouldIgnoreDirectory(String nome) {
        if (nome == null) {
            return false;
        }

        return "target".equals(nome)
                || ".git".equals(nome)
                || "bin".equals(nome)
                || ".settings".equals(nome)
                || ".metadata".equals(nome);
    }

    /** * Retorna true quando o valor informado for nulo ou vazio. * * @param value valor a validar * @return true quando o valor estiver em branco * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}