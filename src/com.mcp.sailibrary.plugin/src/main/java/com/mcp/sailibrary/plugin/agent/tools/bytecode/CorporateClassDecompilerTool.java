package com.mcp.sailibrary.plugin.agent.tools.bytecode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.Signature;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolParameterMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadataProvider;
import com.mcp.sailibrary.plugin.agent.tools.support.ToolJsonSupport;

/** * Executa descompilacao cirurgica de classes corporativas resolvidas em jars ou * artefatos binarios do workspace. * * <p>Esta ferramenta foi desenhada para extrair codigo-fonte de classes * compiladas quando a implementacao real nao esta disponivel como fonte no * projeto atual. O comportamento e defensivo e inclui: * <ul> * <li>allowlist de dominios permitidos</li> * <li>limite maximo de descompilacoes por rodada</li> * <li>cache local de resultados</li> * <li>fallback para estrutura via JDT quando o decompilador falhar</li> * </ul> * </p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class CorporateClassDecompilerTool implements AgentTool, AgentToolPromptMetadataProvider {

    private static final int LIMITE_MAXIMO_CLASSES = 3;

    private static final List<String> DOMINIOS_PERMITIDOS = Arrays.asList(
            "br.gov.sp.prodesp",
            "com.legado.sistema"
    );

    private final File rootDirectory;
    private int classesDescompiladasNestaRodada;

    /** * Inicializa a ferramenta com a raiz segura do projeto. * * @param rootDirectory raiz segura do projeto atual * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public CorporateClassDecompilerTool(File rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.classesDescompiladasNestaRodada = 0;
    }

    @Override
    public String getName() {
        return "descompilar_classe_corporativa";
    }

    /** * Executa a descompilacao controlada da classe corporativa informada. * * <p>O parametro {@code classe} deve conter o nome completo da classe * (FQCN). Apenas classes pertencentes a dominios homologados podem ser * processadas por esta ferramenta.</p> * * @param jsonParameters parametros JSON da ferramenta * @return codigo fonte descompilado, fonte original ou fallback estrutural * * @author Renato Tomaz Nati * @since 2026-05-20 */
    @Override
    public String execute(String jsonParameters) {
        String classeCanonica = ToolJsonSupport.extractJsonStringValue(jsonParameters, "classe");

        if (classeCanonica == null || classeCanonica.trim().length() == 0) {
            return "Erro Operacional: O parametro 'classe' com o nome completo (FQCN) e obrigatorio.";
        }

        if (!isDominioPermitido(classeCanonica)) {
            return "Erro de Perimetro: A classe [" + classeCanonica
                    + "] nao pertence aos pacotes autorizados para descompilacao. Revise o FQCN.";
        }

        File pastaCache = new File(System.getProperty("user.home"), ".sai/decompiled_cache");
        File arquivoCache = new File(pastaCache, classeCanonica.replace(".", "_") + ".java");

        String conteudoCache = lerDoCacheLocal(arquivoCache);
        if (conteudoCache != null && conteudoCache.trim().length() > 0) {
            return "[CACHE HIT] Codigo recuperado da base local:\n\n" + conteudoCache;
        }

        if (classesDescompiladasNestaRodada >= LIMITE_MAXIMO_CLASSES) {
            return "Erro de Cota: Limite maximo de " + LIMITE_MAXIMO_CLASSES + " descompilacoes atingido.";
        }

        classesDescompiladasNestaRodada++;

        String codigoExtraido = localizarEDecompilar(classeCanonica);

        if (codigoExtraido.startsWith("Erro")) {
            return codigoExtraido;
        }

        salvarNoCacheLocal(arquivoCache, codigoExtraido);
        return "[CACHE MISS] Classe extraida com sucesso via Decompiler:\n\n" + codigoExtraido;
    }

    @Override
    public AgentToolPromptMetadata getPromptMetadata() {
        AgentToolPromptMetadata metadata = new AgentToolPromptMetadata();
        metadata.setToolName(getName());
        metadata.setOneLinePurpose("Extrair codigo-fonte real de classes compiladas ocultas em JARs corporativos.");
        metadata.setActivityDescription("Extrai codigo-fonte real de classes compiladas ocultas em JARs corporativos.");

        AgentToolParameterMetadata classe = new AgentToolParameterMetadata();
        classe.setName("classe");
        classe.setRequired(true);
        classe.setDescription("Nome completo da classe corporativa a descompilar.");
        classe.setExampleValue("br.gov.sp.prodesp.exemplo.servico.ServicoInterno");
        metadata.addParameter(classe);

        metadata.addRecommendedUseCase("Use quando a implementacao concreta nao estiver disponivel como fonte no workspace.");
        metadata.addRecommendedUseCase("Use quando houver classe corporativa em jar e a IA precisar do comportamento real.");
        metadata.addRecommendedUseCase("Use apenas quando o FQCN ja estiver claro e a classe estiver em dominio homologado.");

        metadata.addGuardrail("A ferramenta deve respeitar a allowlist de dominios permitidos para descompilacao.");
        metadata.addGuardrail("Nao use descompilacao como primeira escolha quando o fonte original estiver disponivel.");
        metadata.addGuardrail("A quantidade de descompilacoes por rodada deve permanecer limitada para proteger recursos.");

        metadata.addJsonExample(
                "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"descompilar_classe_corporativa\\\",\\\"parameters\\\":{\\\"classe\\\":\\\"br.gov.sp.prodesp.exemplo.servico.ServicoInterno\\\"},\\\"explanation\\\":\\\"Preciso extrair o comportamento real da classe corporativa compilada para fechar a analise.\\\"}"
        );

        return metadata;
    }

    /** * Localiza a classe nos projetos Java abertos e decide o melhor caminho de * extracao. * * @param classeCanonica nome completo da classe * @return fonte original, codigo descompilado ou fallback estrutural * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String localizarEDecompilar(String classeCanonica) {
        try {
            IProject[] projects = getWorkspaceProjects();
            IJavaProject[] projetosOrdenados = ordenarProjetosJavaPorRelevancia(projects);

            IType tipoEncontrado = localizarTipoNosProjetos(projetosOrdenados, classeCanonica);
            if (tipoEncontrado != null && tipoEncontrado.exists()) {
                return processarTipoEncontrado(classeCanonica, tipoEncontrado);
            }

            tipoEncontrado = localizarTipoNosBinariosResolvidos(projetosOrdenados, classeCanonica);
            if (tipoEncontrado != null && tipoEncontrado.exists()) {
                return processarTipoEncontrado(classeCanonica, tipoEncontrado);
            }

            return "Erro Tatico: A classe [" + classeCanonica
                    + "] nao foi localizada no workspace nem nos jars binarios resolvidos do projeto. "
                    + "INSTRUCAO IA: Trate a classe como externa ao fonte local atual e nao faca busca cega por diretorio.";
        } catch (Exception e) {
            System.out.println("localizarEDecompilar: Erro - Falha na varredura JDT da classe: " + e.getMessage());
            return localizarEDecompilarFallBack(classeCanonica);
        }
    }

    /** * Decide como extrair o conteudo de um tipo encontrado no workspace. * * @param classeCanonica nome completo da classe * @param tipoEncontrado tipo resolvido pelo JDT * @return fonte original, fonte descompilada ou fallback estrutural * * @throws Exception quando ocorrer falha grave de extracao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String processarTipoEncontrado(String classeCanonica, IType tipoEncontrado) throws Exception {
        IPackageFragmentRoot root = (IPackageFragmentRoot) tipoEncontrado.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);

        if (root != null && root.getKind() == IPackageFragmentRoot.K_SOURCE) {
            if (tipoEncontrado.getCompilationUnit() != null) {
                return "// [INFO] Arquivo fonte original localizado no Workspace.\n"
                        + tipoEncontrado.getCompilationUnit().getSource();
            }
        }

        if (root != null && root.getKind() == IPackageFragmentRoot.K_BINARY) {
            String resultado = acionarMotorDecompilacao(tipoEncontrado);
            if (resultado.startsWith("Erro")) {
                return "// [AVISO] Falha no motor decompilador. Usando fallback JDT (assinaturas):\n"
                        + extrairEstruturaViaJdt(classeCanonica);
            }
            return resultado;
        }

        return extrairEstruturaViaJdt(classeCanonica);
    }

    /** * Ordena projetos Java dando prioridade aos projetos mais relacionados a * raiz atual. * * @param projects projetos do workspace * @return projetos Java ordenados por relevancia * * @throws Exception quando houver falha ao inspecionar a natureza dos * projetos * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private IJavaProject[] ordenarProjetosJavaPorRelevancia(IProject[] projects) throws Exception {
        List<IJavaProject> projetosRelacionados = new java.util.ArrayList<IJavaProject>();
        List<IJavaProject> projetosRestantes = new java.util.ArrayList<IJavaProject>();

        for (int i = 0; i < projects.length; i++) {
            IProject project = projects[i];
            if (project.isOpen() && project.hasNature(JavaCore.NATURE_ID)) {
                IJavaProject javaProject = JavaCore.create(project);
                if (ehProjetoRelacionadoAoRoot(project)) {
                    projetosRelacionados.add(javaProject);
                } else {
                    projetosRestantes.add(javaProject);
                }
            }
        }

        List<IJavaProject> ordenados = new java.util.ArrayList<IJavaProject>();
        ordenados.addAll(projetosRelacionados);
        ordenados.addAll(projetosRestantes);

        return ordenados.toArray(new IJavaProject[ordenados.size()]);
    }

    /** * Retorna true quando o projeto estiver fisicamente relacionado a raiz * segura atual. * * @param project projeto candidato * @return true quando houver relacao fisica de perimetro * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean ehProjetoRelacionadoAoRoot(IProject project) {
        if (project == null || rootDirectory == null || project.getLocation() == null) {
            return false;
        }

        try {
            String raizCanonica = rootDirectory.getCanonicalPath();
            String projetoCanonico = project.getLocation().toFile().getCanonicalPath();
            return projetoCanonico.startsWith(raizCanonica) || raizCanonica.startsWith(projetoCanonico);
        } catch (Exception e) {
            return false;
        }
    }

    /** * Localiza um tipo por FQCN entre os projetos Java ordenados. * * @param projetosOrdenados projetos Java ordenados * @param classeCanonica nome completo da classe * @return tipo encontrado ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private IType localizarTipoNosProjetos(IJavaProject[] projetosOrdenados, String classeCanonica) {
        for (int i = 0; i < projetosOrdenados.length; i++) {
            try {
                IType tipoEncontrado = projetosOrdenados[i].findType(classeCanonica);
                if (tipoEncontrado != null && tipoEncontrado.exists()) {
                    return tipoEncontrado;
                }
            } catch (Exception e) {
            }
        }
        return null;
    }

    /** * Localiza um tipo diretamente entre roots binarias resolvidas. * * @param projetosOrdenados projetos Java ordenados * @param classeCanonica nome completo da classe * @return tipo encontrado ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private IType localizarTipoNosBinariosResolvidos(IJavaProject[] projetosOrdenados, String classeCanonica) {
        String nomePacote = extrairNomePacote(classeCanonica);
        String nomeSimples = extrairNomeSimples(classeCanonica);

        for (int i = 0; i < projetosOrdenados.length; i++) {
            try {
                IPackageFragmentRoot[] roots = projetosOrdenados[i].getPackageFragmentRoots();
                for (int j = 0; j < roots.length; j++) {
                    IPackageFragmentRoot root = roots[j];
                    if (root.getKind() != IPackageFragmentRoot.K_BINARY) {
                        continue;
                    }

                    IPackageFragment pkg = root.getPackageFragment(nomePacote);
                    if (pkg == null || !pkg.exists()) {
                        continue;
                    }

                    IClassFile classFile = pkg.getClassFile(nomeSimples + ".class");
                    if (classFile != null && classFile.exists()) {
                        IType tipo = classFile.getType();
                        if (tipo != null && tipo.exists()) {
                            return tipo;
                        }
                    }
                }
            } catch (Exception e) {
            }
        }

        return null;
    }

    /** * Extrai o nome do pacote a partir do FQCN. * * @param classeCanonica nome completo da classe * @return nome do pacote * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String extrairNomePacote(String classeCanonica) {
        if (classeCanonica == null) {
            return "";
        }

        int ultimoPonto = classeCanonica.lastIndexOf('.');
        if (ultimoPonto == -1) {
            return "";
        }

        return classeCanonica.substring(0, ultimoPonto);
    }

    /** * Extrai o nome simples da classe a partir do FQCN. * * @param classeCanonica nome completo da classe * @return nome simples * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String extrairNomeSimples(String classeCanonica) {
        if (classeCanonica == null) {
            return "";
        }

        int ultimoPonto = classeCanonica.lastIndexOf('.');
        if (ultimoPonto == -1) {
            return classeCanonica;
        }

        return classeCanonica.substring(ultimoPonto + 1);
    }

    /** * Executa fallback de localizacao e extracao via JDT. * * @param classeCanonica nome completo da classe * @return fonte original, fonte descompilada ou mensagem de erro * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String localizarEDecompilarFallBack(String classeCanonica) {
        try {
            IProject[] projects = getWorkspaceProjects();

            for (int i = 0; i < projects.length; i++) {
                IProject project = projects[i];
                if (project.isOpen() && project.hasNature(JavaCore.NATURE_ID)) {
                    IJavaProject javaProject = JavaCore.create(project);
                    IType tipoEncontrado = javaProject.findType(classeCanonica);

                    if (tipoEncontrado != null && tipoEncontrado.exists()) {
                        IPackageFragmentRoot root =
                                (IPackageFragmentRoot) tipoEncontrado.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);

                        if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
                            return "// [INFO] Arquivo fonte original localizado no Workspace.\n"
                                    + tipoEncontrado.getCompilationUnit().getSource();
                        } else if (root.getKind() == IPackageFragmentRoot.K_BINARY) {
                            String resultado = acionarMotorDecompilacao(tipoEncontrado);
                            if (resultado.startsWith("Erro")) {
                                return "// [AVISO] Falha no motor Fernflower. Usando Fallback JDT (Apenas Assinaturas):\n"
                                        + extrairEstruturaViaJdt(classeCanonica);
                            }
                            return resultado;
                        }
                    }
                }
            }

            return "Erro Tatico: A classe [" + classeCanonica
                    + "] nao foi localizada no Workspace. INSTRUCAO IA: Pare de buscar cegamente no diretorio. "
                    + "Verifique se o pacote informado (FQCN) esta correto.";
        } catch (Exception e) {
            return "Erro: Falha na varredura JDT da classe: " + e.getMessage();
        }
    }

    /** * Aciona o motor Vineflower/Fernflower para extrair o fonte de um tipo * binario. * * @param tipoBinario tipo binario alvo * @return codigo decompilado ou mensagem de erro * * @throws Exception quando ocorrer falha grave de extracao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String acionarMotorDecompilacao(IType tipoBinario) throws Exception {
        IClassFile classFile = tipoBinario.getClassFile();
        if (classFile == null) {
            return "Erro: O JDT nao conseguiu isolar o IClassFile associado a " + tipoBinario.getElementName();
        }

        byte[] bytecode = classFile.getBytes();

        File tempDir = new File(System.getProperty("java.io.tmpdir"), "sai_decompiler_tmp");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        File tempClassFile = new File(tempDir, tipoBinario.getElementName() + ".class");
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(tempClassFile);
            fos.write(bytecode);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception ex) {
                }
            }
        }

        String fqcn = tipoBinario.getFullyQualifiedName();
        MemoryResultSaver saver = new MemoryResultSaver();

        IFernflowerLogger logger = new IFernflowerLogger() {
            @Override
            public void writeMessage(String message, Severity severity) {
            }

            @Override
            public void writeMessage(String message, Severity severity, Throwable t) {
            }
        };

        Map<String, Object> options = new HashMap<String, Object>();
        Fernflower engine = new Fernflower(saver, options, logger);

        try {
            engine.addSource(tempClassFile);
            engine.decompileContext();
        } catch (Exception e) {
            return "Erro Critico no Motor Vineflower: " + e.getMessage();
        } finally {
            if (tempClassFile.exists()) {
                tempClassFile.delete();
            }
        }

        String codigoDecompilado = saver.getConteudoCapturado();
        if (codigoDecompilado != null && codigoDecompilado.trim().length() > 0) {
            return codigoDecompilado;
        }

        return "Erro de Extracao: Falha ao tentar transcrever os bytes da classe [" + fqcn + "].";
    }

    /** * Retorna true quando o FQCN pertence a um dominio homologado para * descompilacao. * * @param fqcn nome completo da classe * @return true quando o dominio for permitido * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean isDominioPermitido(String fqcn) {
        for (int i = 0; i < DOMINIOS_PERMITIDOS.size(); i++) {
            if (fqcn.startsWith(DOMINIOS_PERMITIDOS.get(i))) {
                return true;
            }
        }
        return false;
    }

    /** * Extrai uma estrutura textual via JDT quando o fonte integral nao puder * ser decompilado. * * @param classeCanonica nome completo da classe * @return estrutura textual minimamente util * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String extrairEstruturaViaJdt(String classeCanonica) {
        try {
            IProject[] projects = getWorkspaceProjects();
            IJavaProject[] projetosOrdenados = ordenarProjetosJavaPorRelevancia(projects);

            IType tipoEncontrado = localizarTipoNosProjetos(projetosOrdenados, classeCanonica);
            if (tipoEncontrado == null) {
                tipoEncontrado = localizarTipoNosBinariosResolvidos(projetosOrdenados, classeCanonica);
            }

            if (tipoEncontrado != null && tipoEncontrado.exists()) {
                StringBuilder sb = new StringBuilder();
                sb.append("// Estrutura extraida via Eclipse JDT de modulo binario compilado\n");
                sb.append("package ").append(tipoEncontrado.getPackageFragment().getElementName()).append(";\n\n");

                if (tipoEncontrado.isInterface()) {
                    sb.append("public interface ");
                } else {
                    sb.append("public class ");
                }
                sb.append(tipoEncontrado.getElementName()).append(" {\n\n");

                IMethod[] metodos = tipoEncontrado.getMethods();
                for (int j = 0; j < metodos.length; j++) {
                    IMethod m = metodos[j];
                    String fonteMetodo = m.getSource();

                    if (fonteMetodo != null && fonteMetodo.trim().length() > 0) {
                        sb.append(fonteMetodo).append("\n\n");
                    } else {
                        String nomeTipoRetorno = Signature.toString(m.getReturnType());
                        sb.append(" // Coordenada binaria detectada. Codigo interno oculto no bytecode.\n");
                        sb.append(" public ").append(nomeTipoRetorno).append(" ")
                                .append(m.getElementName()).append("() throws Exception;\n\n");
                    }
                }
                sb.append("}\n");
                return sb.toString();
            }

            return "Erro: A classe corporativa [" + classeCanonica
                    + "] nao foi localizada em nenhum build path do Workspace atual.";
        } catch (Exception e) {
            return "Erro: Falha na varredura JDT da classe: " + e.getMessage();
        }
    }

    /** * Retorna os projetos do workspace atual. * * @return projetos do workspace * * @throws Exception quando houver falha de acesso ao workspace * * @author Renato Tomaz Nati * @since 2026-05-20 */
    protected IProject[] getWorkspaceProjects() throws Exception {
        return ResourcesPlugin.getWorkspace().getRoot().getProjects();
    }

    /** * Le o conteudo do cache local da classe decompilada. * * @param arquivoCache arquivo de cache local * @return conteudo em cache ou string vazia * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String lerDoCacheLocal(File arquivoCache) {
        if (!arquivoCache.exists()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(arquivoCache));
            String linha;
            while ((linha = br.readLine()) != null) {
                sb.append(linha).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception e) {
                }
            }
        }
    }

    /** * Persiste o conteudo decompilado no cache local. * * @param arquivoCache arquivo de cache local * @param conteudo conteudo a ser gravado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void salvarNoCacheLocal(File arquivoCache, String conteudo) {
        FileWriter fw = null;
        try {
            File pai = arquivoCache.getParentFile();
            if (pai != null && !pai.exists()) {
                pai.mkdirs();
            }

            fw = new FileWriter(arquivoCache);
            fw.write(conteudo);
            fw.flush();
        } catch (Exception e) {
            // Falha silenciosa de cache.
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (Exception e) {
                }
            }
        }
    }

    /** * Saver em memoria para capturar o conteudo decompilado gerado pelo motor * Vineflower/Fernflower. * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private class MemoryResultSaver implements IResultSaver {

        private String conteudoCapturado = "";

        /** * Retorna o conteudo capturado em memoria. * * @return conteudo textual decompilado * * @author Renato Tomaz Nati * @since 2026-05-20 */
        public String getConteudoCapturado() {
            return conteudoCapturado;
        }

        @Override
        public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
            this.conteudoCapturado = content;
        }

        @Override
        public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
            this.conteudoCapturado = content;
        }

        @Override
        public void saveFolder(String path) {
        }

        @Override
        public void copyFile(String source, String path, String entryName) {
        }

        @Override
        public void createArchive(String path, String archiveName, Manifest manifest) {
        }

        @Override
        public void saveDirEntry(String path, String archiveName, String entryName) {
        }

        @Override
        public void copyEntry(String source, String path, String archiveName, String entryName) {
        }

        @Override
        public void closeArchive(String path, String archiveName) {
        }
    }
}