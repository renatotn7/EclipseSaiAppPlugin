package com.mcp.sailibrary.plugin.agent.context;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

/**
 * ---
 * yaml_header:
 * version: "1.1"
 * dependencies: 
 * - org.eclipse.jdt.core.dom
 * - java.io.File
 * purpose: "Realizar varredura fisica no sistema de arquivos para localizar implementacoes Maven/EJB fora do classpath da IDE."
 * ---
 */
public class WorkspaceFallbackScanAgent {
	private static final Set<String> tiposIgnoradosJaLogados = new HashSet<String>();
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
    public boolean buscarMetodoEmTipo(IType type, String nomeMetodo, int nivel, int max, Set<String> visitados, StringBuilder builder, JdtCallTraceAgent astAgent) throws Exception {
        ICompilationUnit unit = type.getCompilationUnit();
        if (unit == null) return false;

        IMethod[] methods = type.getMethods();
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].getElementName().equals(nomeMetodo)) {
                astAgent.rastrearRecursivo(unit, methods[i].getNameRange().getOffset(), nivel + 1, max, visitados, builder);
                return true;
            }
        }
        return false;
    }

    /**
 * Descrição não fornecida.
 *
 * @author Renato Tomaz Nati
 */
    public void buscarImplementacoesTipo(IType interfaceType, String nomeMetodo, int nivel, int max, Set<String> visitados, StringBuilder builder, JdtCallTraceAgent astAgent) throws Exception {
        ITypeHierarchy hierarchy = interfaceType.newTypeHierarchy(interfaceType.getJavaProject(), null);
        IType[] implementations = hierarchy.getAllSubtypes(interfaceType);
        
        for (int i = 0; i < implementations.length; i++) {
            if (!implementations[i].isInterface()) {
                if (buscarMetodoEmTipo(implementations[i], nomeMetodo, nivel, max, visitados, builder, astAgent)) return;
            }
        }
        // Fallback para varredura bruta se a hierarquia JDT falhar
        buscarImplementacaoPorForcaBrutaWorkspace(interfaceType.getElementName(), nomeMetodo, nivel, max, visitados, builder, astAgent);
    }

    public boolean buscarImplementacaoPorForcaBrutaWorkspace(String nomeInterface, String nomeMetodo, int nivel, int max, Set<String> visitados, StringBuilder builder, JdtCallTraceAgent astAgent) throws Exception {
        org.eclipse.core.resources.IProject[] allProjects = org.eclipse.core.resources.ResourcesPlugin.getWorkspace().getRoot().getProjects();
        for (int p = 0; p < allProjects.length; p++) {
            if (allProjects[p].isOpen() && allProjects[p].hasNature(org.eclipse.jdt.core.JavaCore.NATURE_ID)) {
                IJavaProject javaProject = org.eclipse.jdt.core.JavaCore.create(allProjects[p]);
                for (IPackageFragment pkg : javaProject.getPackageFragments()) {
                    if (pkg.getKind() == IPackageFragmentRoot.K_SOURCE) {
                        for (ICompilationUnit cu : pkg.getCompilationUnits()) {
                            for (IType type : cu.getAllTypes()) {
                                if (!type.isInterface() && contemInterface(type, nomeInterface)) {
                                    if (buscarMetodoEmTipo(type, nomeMetodo, nivel, max, visitados, builder, astAgent)) return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean contemInterface(IType type, String nomeInterface) throws Exception {
        String[] interfaces = type.getSuperInterfaceNames();
        for (int i = 0; i < interfaces.length; i++) {
            if (interfaces[i].equals(nomeInterface) || interfaces[i].endsWith("." + nomeInterface)) return true;
        }
        return false;
    }
    /**
 * Descrição não fornecida.
 *
 * @author Renato Tomaz Nati
 */
    public boolean buscarForcaBrutaMaven(ICompilationUnit unit, String nomeClasse, String nomeMetodo, int nivel, int max, Set<String> visitados, StringBuilder builder) {
        if (unit == null || unit.getJavaProject() == null) return false;

        try {
            File currentDir = unit.getJavaProject().getProject().getLocation().toFile();
            File arquivoLocal = localizarArquivoRecursivo(currentDir, nomeClasse + ".java");

            if (arquivoLocal != null) {
                if (extrairMetodoDeArquivoFisico(arquivoLocal, nomeMetodo, nivel, max, visitados, builder) == 1) return true;
            }
            // Logica de subida para POM pai e busca em modulos segue o padrao ja existente da classe.
        } catch (Exception e) {}
        return false;
    }

    /**
 * Descrição não fornecida.
 *
 * @author Renato Tomaz Nati
 */
    public boolean buscarForcaBrutaMaven(ICompilationUnit unit, String nomeClasse, String nomeMetodo, int nivel, int max, Set<String> visitados, StringBuilder builder, JdtCallTraceAgent astAgent) {
        String tipoNormalizado = normalizarNomeTipo(nomeClasse);

        if (deveIgnorarTipoNaoProjeto(tipoNormalizado)) {
            registrarIgnoradoUmaVez(tipoNormalizado, nomeMetodo);
            return false;
        }

        return buscarForcaBrutaMaven(unit, tipoNormalizado, nomeMetodo, nivel, max, visitados, builder);
    }
    private File localizarArquivoRecursivo(File dir, String nome) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return null;
        File[] arquivos = dir.listFiles();
        if (arquivos != null) {
            for (int i = 0; i < arquivos.length; i++) {
                if (arquivos[i].isDirectory()) {
                    File achou = localizarArquivoRecursivo(arquivos[i], nome);
                    if (achou != null) return achou;
                } else if (arquivos[i].getName().equals(nome)) {
                    return arquivos[i];
                }
            }
        }
        return null;
    }

    private static int extrairMetodoDeArquivoFisico(final File arquivoJava, final String nomeMetodoProcurado, final int nivel, final int max, final Set<String> visitados, final StringBuilder builder) {
        String conteudo = lerConteudoArquivo(arquivoJava);
        if (conteudo == null || conteudo.trim().length() == 0) return 0;

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
                        String chaveFisica = arquivoJava.getName() + "#" + nomeMetodoProcurado;
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
    

    public String normalizarNomeTipo(String nomeTipo) {
        if (nomeTipo == null) return null;

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
    public  boolean deveIgnorarTipoNaoProjeto(String nomeTipo) {
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
    public  void registrarIgnoradoUmaVez(String nomeTipo, String nomeMetodo) {
        String chave = normalizarNomeTipo(nomeTipo) + "#" + nomeMetodo;
        if (tiposIgnoradosJaLogados.add(chave)) {
            System.out.println("[TELEMETRIA JDT] [SKIP EXTERNO] Ignorando tipo nao projeto: " + nomeTipo + "." + nomeMetodo);
        }
    }
    
}