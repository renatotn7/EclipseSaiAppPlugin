package com.mcp.sailibrary.plugin.agent.tools.architecture;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolParameterMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadataProvider;
import com.mcp.sailibrary.plugin.agent.tools.support.ToolJsonSupport;

/** * Extrai evidencias concretas de HQL, SQL, JPQL, Criteria, JDBC, annotations * e XML relacionados ao trecho alvo. * * <p>Esta ferramenta foi desenhada para localizar sinais reais de persistencia * e consulta sem assumir versao especifica de framework. O objetivo e devolver * evidencias concretas e nao suposicoes abstratas sobre o comportamento do * trecho analisado.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class QueryExtractionTool implements AgentTool, AgentToolPromptMetadataProvider {

    private File rootDirectory;
    private ICompilationUnit compilationUnitAtual;
    private int offsetAtual;

    /** * Inicializa a ferramenta de extracao de queries para o contexto atual. * * @param rootDirectory raiz segura do projeto * @param compilationUnitAtual unidade de compilacao atual do editor * @param offsetAtual offset atual no editor * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public QueryExtractionTool(File rootDirectory, ICompilationUnit compilationUnitAtual, int offsetAtual) {
        this.rootDirectory = rootDirectory;
        this.compilationUnitAtual = compilationUnitAtual;
        this.offsetAtual = offsetAtual;
    }

    @Override
    public String getName() {
        return "extrair_queries_trecho";
    }

    @Override
    public AgentToolPromptMetadata getPromptMetadata() {
        AgentToolPromptMetadata metadata = new AgentToolPromptMetadata();
        metadata.setToolName(getName());
        metadata.setOneLinePurpose("Extrair evidencias concretas de query, Criteria, JDBC e XML relacionados ao alvo.");
        metadata.setActivityDescription("Extrai evidencias de HQL, SQL, JPQL, Criteria, JDBC, annotations e XML Hibernate.");

        AgentToolParameterMetadata modo = new AgentToolParameterMetadata();
        modo.setName("modo");
        modo.setRequired(false);
        modo.setDescription("Modo de resolucao do alvo, como editor_ativo.");
        modo.setExampleValue("editor_ativo");
        metadata.addParameter(modo);

        AgentToolParameterMetadata classe = new AgentToolParameterMetadata();
        classe.setName("classe");
        classe.setRequired(false);
        classe.setDescription("Nome da classe alvo quando o modo nao for editor_ativo.");
        classe.setExampleValue("RelatorioAcompanhamentoDivisaoAction");
        metadata.addParameter(classe);

        AgentToolParameterMetadata metodo = new AgentToolParameterMetadata();
        metodo.setName("metodo");
        metodo.setRequired(false);
        metodo.setDescription("Nome do metodo alvo quando o modo nao for editor_ativo.");
        metodo.setExampleValue("setupEnv");
        metadata.addParameter(metodo);

        AgentToolParameterMetadata path = new AgentToolParameterMetadata();
        path.setName("path");
        path.setRequired(false);
        path.setDescription("Caminho relativo de arquivo para extracao textual direta.");
        path.setExampleValue("src/main/java/com/exemplo/RelatorioAction.java");
        metadata.addParameter(path);

        AgentToolParameterMetadata limite = new AgentToolParameterMetadata();
        limite.setName("limite");
        limite.setRequired(false);
        limite.setDescription("Quantidade maxima de evidencias retornadas.");
        limite.setExampleValue("30");
        metadata.addParameter(limite);

        AgentToolParameterMetadata incluirXml = new AgentToolParameterMetadata();
        incluirXml.setName("incluir_xml");
        incluirXml.setRequired(false);
        incluirXml.setDescription("Define se XMLs relacionados devem ser inspecionados.");
        incluirXml.setExampleValue("true");
        metadata.addParameter(incluirXml);

        metadata.addRecommendedUseCase("Use quando precisar localizar queries concretas relacionadas ao metodo ou trecho atual.");
        metadata.addRecommendedUseCase("Use quando houver suspeita de HQL, SQL, JPQL, Criteria, JDBC ou XML Hibernate.");
        metadata.addRecommendedUseCase("Use antes de alterar um metodo que possa tocar persistencia ou consulta.");

        metadata.addGuardrail("Nao assuma versao especifica de Hibernate ou JPA sem evidencia concreta.");
        metadata.addGuardrail("Devolva evidencias, nao interpretacoes inventadas.");
        metadata.addGuardrail("Limite o volume de resultados para preservar legibilidade do contexto.");

        metadata.addJsonExample(
                "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"extrair_queries_trecho\\\",\\\"parameters\\\":{\\\"modo\\\":\\\"editor_ativo\\\",\\\"limite\\\":\\\"30\\\",\\\"incluir_xml\\\":\\\"true\\\"},\\\"explanation\\\":\\\"Preciso localizar evidencias concretas de queries, Criteria, JDBC ou XML Hibernate relacionadas ao trecho atual.\\\"}"
        );

        return metadata;
    }

    /** * Executa a extracao de evidencias de queries no alvo resolvido. * * @param jsonParameters parametros JSON da ferramenta * @return relatorio textual de evidencias encontradas * * @author Renato Tomaz Nati * @since 2026-05-20 */
    @Override
    public String execute(String jsonParameters) {
        String modo = ToolJsonSupport.extractJsonStringValue(jsonParameters, "modo");
        String nomeClasse = ToolJsonSupport.extractJsonStringValue(jsonParameters, "classe");
        String nomeMetodo = ToolJsonSupport.extractJsonStringValue(jsonParameters, "metodo");
        String path = ToolJsonSupport.extractJsonStringValue(jsonParameters, "path");
        int limiteResultados = ToolJsonSupport.extractJsonIntValue(jsonParameters, "limite", 40, 1, 300);

        String incluirXmlTexto = ToolJsonSupport.extractJsonStringValue(jsonParameters, "incluir_xml");
        boolean incluirXml = "true".equalsIgnoreCase(incluirXmlTexto)
                || "sim".equalsIgnoreCase(incluirXmlTexto)
                || "1".equals(incluirXmlTexto);

        ExtractionTarget alvo = resolveTarget(modo, nomeClasse, nomeMetodo, path);
        if (alvo == null || isBlank(alvo.source)) {
            return "Erro Operacional: Nao foi possivel resolver o alvo para extracao de queries.";
        }

        List<String> evidencias = new ArrayList<String>();
        Set<String> vistos = new HashSet<String>();

        extractFromSource(alvo.source, evidencias, vistos, limiteResultados);

        if (incluirXml && evidencias.size() < limiteResultados) {
            extractXmlEvidence(alvo, evidencias, vistos, limiteResultados);
        }

        if (evidencias.isEmpty()) {
            return "Nenhuma evidencia relevante de query, criteria, HBM ou persistencia foi localizada no alvo inspecionado.";
        }

        return formatReport(alvo.originDescription, limiteResultados, incluirXml, evidencias);
    }

    /** * Resolve o alvo da extracao com base em modo, classe, metodo ou path. * * @param modo modo de resolucao * @param nomeClasse classe alvo * @param nomeMetodo metodo alvo * @param path caminho relativo opcional * @return alvo resolvido ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private ExtractionTarget resolveTarget(String modo, String nomeClasse, String nomeMetodo, String path) {
        if (!isBlank(path)) {
            File arquivo = new File(rootDirectory, path);
            if (!arquivo.exists() || !arquivo.isFile()) {
                return null;
            }

            String conteudo = readFile(arquivo);
            if (isBlank(conteudo)) {
                return null;
            }

            ExtractionTarget target = new ExtractionTarget();
            target.source = conteudo;
            target.originDescription = "Arquivo: " + normalizePath(arquivo);
            target.primaryTypeName = stripExtension(arquivo.getName());
            target.relatedJavaFile = arquivo;
            return target;
        }

        IMethod metodoAlvo;
        if ("editor_ativo".equalsIgnoreCase(modo) || (isBlank(nomeClasse) && isBlank(nomeMetodo))) {
            metodoAlvo = resolveMethodFromEditor();
        } else {
            metodoAlvo = findMethodByName(nomeClasse, nomeMetodo);
        }

        if (metodoAlvo == null) {
            return null;
        }

        String source = extractMethodSource(metodoAlvo);
        if (isBlank(source)) {
            return null;
        }

        ExtractionTarget target = new ExtractionTarget();
        target.source = source;
        target.originDescription = "Metodo: " + safeQualifiedMethodName(metodoAlvo);
        try {
            if (metodoAlvo.getDeclaringType() != null) {
                target.primaryTypeName = metodoAlvo.getDeclaringType().getElementName();
            }
            if (metodoAlvo.getCompilationUnit() != null && metodoAlvo.getCompilationUnit().getResource() != null) {
                target.relatedJavaFile = metodoAlvo.getCompilationUnit().getResource().getLocation().toFile();
            }
        } catch (Exception e) {
        }

        return target;
    }

    /** * Resolve o metodo alvo a partir do editor atual. * * @return metodo localizado ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private IMethod resolveMethodFromEditor() {
        if (compilationUnitAtual == null) {
            return null;
        }

        try {
            IType tipoPrimario = compilationUnitAtual.findPrimaryType();
            if (tipoPrimario == null) {
                return null;
            }

            IMethod[] metodos = tipoPrimario.getMethods();
            for (int i = 0; i < metodos.length; i++) {
                int inicioMetodo = metodos[i].getSourceRange().getOffset();
                int fimMetodo = inicioMetodo + metodos[i].getSourceRange().getLength();
                if (offsetAtual >= inicioMetodo && offsetAtual <= fimMetodo) {
                    return metodos[i];
                }
            }
        } catch (Exception e) {
            return null;
        }

        return null;
    }

    /** * Localiza metodo por nome em classe resolvida. * * @param nomeClasse classe alvo * @param nomeMetodo metodo alvo * @return metodo localizado ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private IMethod findMethodByName(String nomeClasse, String nomeMetodo) {
        if (compilationUnitAtual == null || compilationUnitAtual.getJavaProject() == null) {
            return null;
        }

        try {
            IType tipo = null;

            IType tipoPrimario = compilationUnitAtual.findPrimaryType();
            if (tipoPrimario != null) {
                String nomeSimplesAtual = tipoPrimario.getElementName();
                String fqcnAtual = tipoPrimario.getFullyQualifiedName();

                if (nomeSimplesAtual.equals(nomeClasse) || fqcnAtual.equals(nomeClasse)) {
                    tipo = tipoPrimario;
                }
            }

            if (tipo == null) {
                tipo = compilationUnitAtual.getJavaProject().findType(nomeClasse);
            }

            if (tipo == null) {
                tipo = findTypeBySimpleName(nomeClasse);
            }

            if (tipo == null) {
                return null;
            }

            IMethod[] metodos = tipo.getMethods();
            for (int i = 0; i < metodos.length; i++) {
                if (metodos[i].getElementName().equals(nomeMetodo)) {
                    return metodos[i];
                }
            }
        } catch (Exception e) {
            return null;
        }

        return null;
    }

    /** * Localiza tipo por nome simples nas sources do projeto atual. * * @param nomeClasse nome simples * @return tipo encontrado ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private IType findTypeBySimpleName(String nomeClasse) {
        if (compilationUnitAtual == null || compilationUnitAtual.getJavaProject() == null) {
            return null;
        }

        try {
            IPackageFragment[] pacotes = compilationUnitAtual.getJavaProject().getPackageFragments();
            for (int i = 0; i < pacotes.length; i++) {
                if (pacotes[i].getKind() == IPackageFragmentRoot.K_SOURCE) {
                    ICompilationUnit[] unidades = pacotes[i].getCompilationUnits();
                    for (int j = 0; j < unidades.length; j++) {
                        IType[] tipos = unidades[j].getAllTypes();
                        for (int k = 0; k < tipos.length; k++) {
                            if (tipos[k].getElementName().equals(nomeClasse)) {
                                return tipos[k];
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }

        return null;
    }

    /** * Extrai o codigo fonte do metodo alvo via AST. * * @param metodoAlvo metodo alvo * @return codigo fonte do metodo ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String extractMethodSource(final IMethod metodoAlvo) {
        try {
            ICompilationUnit unidade = metodoAlvo.getCompilationUnit();
            if (unidade == null) {
                return null;
            }

            ASTParser parser = ASTParser.newParser(AST.JLS21);
            parser.setSource(unidade);
            parser.setKind(ASTParser.K_COMPILATION_UNIT);
            parser.setResolveBindings(true);
            parser.setBindingsRecovery(true);
            parser.setProject(unidade.getJavaProject());

            final CompilationUnit astNode = (CompilationUnit) parser.createAST(null);
            final String[] metodoSource = new String[1];

            astNode.accept(new ASTVisitor() {
                @Override
                public boolean visit(MethodDeclaration node) {
                    try {
                        if (node.resolveBinding() != null && node.resolveBinding().getJavaElement() instanceof IMethod) {
                            IMethod metodoNode = (IMethod) node.resolveBinding().getJavaElement();
                            if (sameSignature(metodoAlvo, metodoNode)) {
                                metodoSource[0] = node.toString();
                                return false;
                            }
                        }
                    } catch (Exception e) {
                    }
                    return true;
                }
            });

            return metodoSource[0];
        } catch (Exception e) {
            return null;
        }
    }

    /** * Orquestra a extracao das evidencias encontradas no fonte alvo. * * @param source fonte textual alvo * @param evidencias lista acumuladora * @param vistos conjunto de deduplicacao * @param limite limite maximo * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void extractFromSource(String source, List<String> evidencias, Set<String> vistos, int limite) {
        extractApiCalls(source, evidencias, vistos, limite);
        extractCriteriaEvidence(source, evidencias, vistos, limite);
        extractQueryLiterals(source, evidencias, vistos, limite);
        extractNamedQueryReferences(source, evidencias, vistos, limite);
        extractPreparedStatementUsage(source, evidencias, vistos, limite);
        extractDynamicAssemblyHints(source, evidencias, vistos, limite);
        extractAnnotationQueries(source, evidencias, vistos, limite);
    }

    /** * Extrai chamadas de APIs de persistencia e acesso a dados. * * @param source fonte textual * @param evidencias lista acumuladora * @param vistos conjunto de deduplicacao * @param limite limite maximo * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void extractApiCalls(String source, List<String> evidencias, Set<String> vistos, int limite) {
        String[] padroes = new String[] {
            "\\bcreateQuery\\s*\\(",
            "\\bcreateSQLQuery\\s*\\(",
            "\\bcreateNativeQuery\\s*\\(",
            "\\bprepareStatement\\s*\\(",
            "\\bgetNamedQuery\\s*\\(",
            "\\bcreateNamedQuery\\s*\\(",
            "\\bsetParameter\\s*\\(",
            "\\bsetString\\s*\\(",
            "\\bsetLong\\s*\\(",
            "\\bsetInteger\\s*\\(",
            "\\bsetMaxResults\\s*\\(",
            "\\blist\\s*\\(",
            "\\buniqueResult\\s*\\(",
            "\\bgetResultList\\s*\\(",
            "\\bgetSingleResult\\s*\\("
        };

        for (int i = 0; i < padroes.length && evidencias.size() < limite; i++) {
            Matcher matcher = Pattern.compile(padroes[i]).matcher(source);
            while (matcher.find() && evidencias.size() < limite) {
                String chamada = matcher.group();
                String tipo = classifyApiCall(chamada);
                String chave = tipo + "|" + chamada + "|" + matcher.start();
                if (vistos.add(chave)) {
                    evidencias.add(tipo + " | linha aprox " + estimateLine(source, matcher.start()) + " | " + chamada.trim());
                }
            }
        }
    }

    /** * Extrai evidencias de Criteria Hibernate e JPA. * * @param source fonte textual * @param evidencias lista acumuladora * @param vistos conjunto de deduplicacao * @param limite limite maximo * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void extractCriteriaEvidence(String source, List<String> evidencias, Set<String> vistos, int limite) {
        String[] hibernateCriteriaApi = new String[] {
            "\\bcreateCriteria\\s*\\(",
            "\\bDetachedCriteria\\b",
            "\\bsetProjection\\s*\\(",
            "\\bcreateAlias\\s*\\(",
            "\\bsetFetchMode\\s*\\(",
            "\\bExample\\.create\\s*\\(",
            "\\bRestrictions\\.",
            "\\bOrder\\.",
            "\\bProjections\\."
        };

        for (int i = 0; i < hibernateCriteriaApi.length && evidencias.size() < limite; i++) {
            Matcher matcher = Pattern.compile(hibernateCriteriaApi[i]).matcher(source);
            while (matcher.find() && evidencias.size() < limite) {
                String achado = matcher.group().trim();
                String tipo = achado.startsWith("Restrictions.")
                        ? "HIBERNATE_CRITERIA_RESTRICTION"
                        : achado.startsWith("Order.")
                        ? "HIBERNATE_CRITERIA_ORDER"
                        : achado.startsWith("Projections.")
                        ? "HIBERNATE_CRITERIA_PROJECTION"
                        : "HIBERNATE_CRITERIA_API";
                String chave = tipo + "|" + achado + "|" + matcher.start();
                if (vistos.add(chave)) {
                    evidencias.add(tipo + " | linha aprox " + estimateLine(source, matcher.start()) + " | " + achado);
                }
            }
        }

        String[] jpaCriteriaApi = new String[] {
            "\\bCriteriaBuilder\\b",
            "\\bCriteriaQuery\\b",
            "\\bRoot\\s*<",
            "\\bPredicate\\b",
            "\\bgetCriteriaBuilder\\s*\\(",
            "\\bbuilder\\.equal\\s*\\(",
            "\\bbuilder\\.like\\s*\\(",
            "\\bbuilder\\.and\\s*\\(",
            "\\bbuilder\\.or\\s*\\(",
            "\\bbuilder\\.between\\s*\\(",
            "\\bbuilder\\.in\\s*\\("
        };

        for (int i = 0; i < jpaCriteriaApi.length && evidencias.size() < limite; i++) {
            Matcher matcher = Pattern.compile(jpaCriteriaApi[i]).matcher(source);
            while (matcher.find() && evidencias.size() < limite) {
                String achado = matcher.group().trim();
                String tipo = achado.startsWith("builder.")
                        ? "JPA_CRITERIA_PREDICATE"
                        : "JPA_CRITERIA_API";
                String chave = tipo + "|" + achado + "|" + matcher.start();
                if (vistos.add(chave)) {
                    evidencias.add(tipo + " | linha aprox " + estimateLine(source, matcher.start()) + " | " + achado);
                }
            }
        }
    }

    /** * Extrai literais que aparentam representar SQL, HQL ou JPQL. * * @param source fonte textual * @param evidencias lista acumuladora * @param vistos conjunto de deduplicacao * @param limite limite maximo * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void extractQueryLiterals(String source, List<String> evidencias, Set<String> vistos, int limite) {
        Pattern p = Pattern.compile("\"([^\"]{0,600})\"");
        Matcher matcher = p.matcher(source);

        while (matcher.find() && evidencias.size() < limite) {
            String literal = matcher.group(1);
            String normalizado = literal.trim().replaceAll("\\s+", " ");

            if (looksLikeSql(normalizado)) {
                String chave = "SQL_LITERAL|" + normalizado;
                if (vistos.add(chave)) {
                    evidencias.add("SQL_LITERAL | linha aprox " + estimateLine(source, matcher.start())
                            + " | \"" + truncate(normalizado, 240) + "\"");
                }
            } else if (looksLikeHqlOrJpql(normalizado)) {
                String chave = "HQL_LITERAL|" + normalizado;
                if (vistos.add(chave)) {
                    evidencias.add("HQL_LITERAL | linha aprox " + estimateLine(source, matcher.start())
                            + " | \"" + truncate(normalizado, 240) + "\"");
                }
            }
        }
    }

    /** * Extrai referencias a named queries. * * @param source fonte textual * @param evidencias lista acumuladora * @param vistos conjunto de deduplicacao * @param limite limite maximo * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void extractNamedQueryReferences(String source, List<String> evidencias, Set<String> vistos, int limite) {
        Pattern p = Pattern.compile("\\b(getNamedQuery|createNamedQuery)\\s*\\(\\s*\"([^\"]+)\"\\s*\\)");
        Matcher matcher = p.matcher(source);

        while (matcher.find() && evidencias.size() < limite) {
            String nome = matcher.group(2);
            String chave = "NAMED_QUERY_USAGE|" + nome;
            if (vistos.add(chave)) {
                evidencias.add("NAMED_QUERY_USAGE | linha aprox " + estimateLine(source, matcher.start()) + " | " + nome);
            }
        }
    }

    /** * Extrai uso de PreparedStatement e JDBC direto. * * @param source fonte textual * @param evidencias lista acumuladora * @param vistos conjunto de deduplicacao * @param limite limite maximo * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void extractPreparedStatementUsage(String source, List<String> evidencias, Set<String> vistos, int limite) {
        Pattern p = Pattern.compile("\\bprepareStatement\\s*\\(");
        Matcher matcher = p.matcher(source);

        while (matcher.find() && evidencias.size() < limite) {
            String chave = "JDBC_API_CALL|" + matcher.start();
            if (vistos.add(chave)) {
                evidencias.add("JDBC_API_CALL | linha aprox " + estimateLine(source, matcher.start()) + " | uso de JDBC PreparedStatement");
            }
        }
    }

    /** * Extrai indicios de montagem dinamica de query. * * @param source fonte textual * @param evidencias lista acumuladora * @param vistos conjunto de deduplicacao * @param limite limite maximo * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void extractDynamicAssemblyHints(String source, List<String> evidencias, Set<String> vistos, int limite) {
        String[] padroes = new String[] {
            "\\bStringBuilder\\b",
            "\\bStringBuffer\\b",
            "\\.append\\s*\\(",
            "\"\\s*\\+\\s*[a-zA-Z_]",
            "[a-zA-Z_]\\w*\\s*\\+\\s*\"",
            "\\bconcat\\s*\\("
        };

        for (int i = 0; i < padroes.length && evidencias.size() < limite; i++) {
            Matcher matcher = Pattern.compile(padroes[i]).matcher(source);
            while (matcher.find() && evidencias.size() < limite) {
                String chave = "DYNAMIC_QUERY_ASSEMBLY|" + matcher.start() + "|" + padroes[i];
                if (vistos.add(chave)) {
                    evidencias.add("DYNAMIC_QUERY_ASSEMBLY | linha aprox " + estimateLine(source, matcher.start())
                            + " | indicio de montagem dinamica de string/query");
                }
            }
        }
    }

    /** * Extrai evidencias de queries declaradas em annotations. * * @param source fonte textual * @param evidencias lista acumuladora * @param vistos conjunto de deduplicacao * @param limite limite maximo * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void extractAnnotationQueries(String source, List<String> evidencias, Set<String> vistos, int limite) {
        String[] padroes = new String[] {
            "@Query\\s*\\(",
            "@NamedQuery\\s*\\(",
            "@NamedNativeQuery\\s*\\(",
            "@Queries\\s*\\(",
            "@NamedQueries\\s*\\("
        };

        for (int i = 0; i < padroes.length && evidencias.size() < limite; i++) {
            Matcher matcher = Pattern.compile(padroes[i]).matcher(source);
            while (matcher.find() && evidencias.size() < limite) {
                String nome = matcher.group().trim();
                String tipo = nome.startsWith("@Query") ? "SPRING_QUERY_ANNOTATION" : "NAMED_QUERY_DECLARATION";
                String chave = tipo + "|" + nome + "|" + matcher.start();
                if (vistos.add(chave)) {
                    evidencias.add(tipo + " | linha aprox " + estimateLine(source, matcher.start()) + " | " + nome);
                }
            }
        }
    }

    /** * Extrai evidencias relacionadas em XMLs de persistencia. * * @param alvo alvo resolvido * @param evidencias lista acumuladora * @param vistos conjunto de deduplicacao * @param limite limite maximo * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void extractXmlEvidence(ExtractionTarget alvo, List<String> evidencias, Set<String> vistos, int limite) {
        if (rootDirectory == null || !rootDirectory.exists()) {
            return;
        }

        List<File> arquivosXml = new ArrayList<File>();
        collectXmlFiles(rootDirectory, arquivosXml, 400);

        for (int i = 0; i < arquivosXml.size() && evidencias.size() < limite; i++) {
            File xml = arquivosXml.get(i);
            String conteudo = readFile(xml);
            if (isBlank(conteudo)) {
                continue;
            }

            boolean parecePersistenciaXml = xml.getName().toLowerCase().endsWith(".hbm.xml")
                    || conteudo.contains("<hibernate-mapping")
                    || conteudo.contains("<sql-query")
                    || conteudo.contains("<query")
                    || conteudo.contains("<named-query")
                    || conteudo.contains("<named-native-query")
                    || conteudo.contains("query-ref=");

            if (!parecePersistenciaXml) {
                continue;
            }

            extractNamedQueryDeclarationsFromXml(xml, conteudo, evidencias, vistos, limite);
            extractQueryBlocksFromXml(xml, conteudo, evidencias, vistos, limite);
            extractSqlQueryBlocksFromXml(xml, conteudo, evidencias, vistos, limite);
            extractQueryRefFromXml(xml, conteudo, evidencias, vistos, limite);

            if (!isBlank(alvo.primaryTypeName) && conteudo.contains(alvo.primaryTypeName) && evidencias.size() < limite) {
                String chave = "HBM_ENTITY_MAPPING|" + xml.getAbsolutePath() + "|" + alvo.primaryTypeName;
                if (vistos.add(chave)) {
                    evidencias.add("HBM_ENTITY_MAPPING | arquivo " + normalizePath(xml) + " | referencia relacionada a " + alvo.primaryTypeName);
                }
            }
        }
    }

    /** * Extrai named queries declaradas em XML. * * @param xml arquivo XML * @param conteudo conteudo textual do XML * @param evidencias lista acumuladora * @param vistos conjunto de deduplicacao * @param limite limite maximo * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void extractNamedQueryDeclarationsFromXml(File xml, String conteudo, List<String> evidencias, Set<String> vistos, int limite) {
        Pattern p = Pattern.compile("<(query|named-query|named-native-query|sql-query)\\b[^>]*\\bname\\s*=\\s*\"([^\"]+)\"[^>]*>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = p.matcher(conteudo);

        while (matcher.find() && evidencias.size() < limite) {
            String tipoTag = matcher.group(1);
            String nome = matcher.group(2);
            String tipo = "NAMED_QUERY_DECLARATION";
            if ("sql-query".equalsIgnoreCase(tipoTag)) {
                tipo = "HBM_SQL_QUERY_EVIDENCE";
            } else if ("query".equalsIgnoreCase(tipoTag)) {
                tipo = "HBM_QUERY_EVIDENCE";
            }

            String chave = tipo + "|" + normalizePath(xml) + "|" + nome;
            if (vistos.add(chave)) {
                evidencias.add(tipo + " | arquivo " + normalizePath(xml) + " | name=" + nome);
            }
        }
    }

    /** * Extrai blocos de query declarados em XML. * * @param xml arquivo XML * @param conteudo conteudo textual * @param evidencias lista acumuladora * @param vistos conjunto de deduplicacao * @param limite limite maximo * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void extractQueryBlocksFromXml(File xml, String conteudo, List<String> evidencias, Set<String> vistos, int limite) {
        Pattern p = Pattern.compile("<query\\b[^>]*>([\\s\\S]*?)</query>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = p.matcher(conteudo);

        while (matcher.find() && evidencias.size() < limite) {
            String corpo = cleanXmlText(matcher.group(1));
            if (looksLikeHqlOrJpql(corpo) || looksLikeSql(corpo)) {
                String chave = "HBM_QUERY_BLOCK|" + normalizePath(xml) + "|" + corpo;
                if (vistos.add(chave)) {
                    evidencias.add("HBM_QUERY_EVIDENCE | arquivo " + normalizePath(xml) + " | \"" + truncate(corpo, 240) + "\"");
                }
            }
        }
    }

    /** * Extrai blocos de sql-query declarados em XML. * * @param xml arquivo XML * @param conteudo conteudo textual * @param evidencias lista acumuladora * @param vistos conjunto de deduplicacao * @param limite limite maximo * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void extractSqlQueryBlocksFromXml(File xml, String conteudo, List<String> evidencias, Set<String> vistos, int limite) {
        Pattern p = Pattern.compile("<sql-query\\b[^>]*>([\\s\\S]*?)</sql-query>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = p.matcher(conteudo);

        while (matcher.find() && evidencias.size() < limite) {
            String corpo = cleanXmlText(matcher.group(1));
            String chave = "HBM_SQL_QUERY_BLOCK|" + normalizePath(xml) + "|" + corpo;
            if (vistos.add(chave)) {
                evidencias.add("HBM_SQL_QUERY_EVIDENCE | arquivo " + normalizePath(xml) + " | \"" + truncate(corpo, 240) + "\"");
            }
        }
    }

    /** * Extrai referencias query-ref de XML. * * @param xml arquivo XML * @param conteudo conteudo textual * @param evidencias lista acumuladora * @param vistos conjunto de deduplicacao * @param limite limite maximo * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void extractQueryRefFromXml(File xml, String conteudo, List<String> evidencias, Set<String> vistos, int limite) {
        Pattern p = Pattern.compile("\\bquery-ref\\s*=\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
        Matcher matcher = p.matcher(conteudo);

        while (matcher.find() && evidencias.size() < limite) {
            String ref = matcher.group(1);
            String chave = "QUERY_REF_XML|" + normalizePath(xml) + "|" + ref;
            if (vistos.add(chave)) {
                evidencias.add("NAMED_QUERY_USAGE | arquivo " + normalizePath(xml) + " | query-ref=" + ref);
            }
        }
    }

    /** * Coleta arquivos XML dentro da raiz segura. * * @param pasta pasta atual * @param arquivos lista acumuladora * @param limiteArquivos limite maximo de arquivos * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void collectXmlFiles(File pasta, List<File> arquivos, int limiteArquivos) {
        if (pasta == null || !pasta.exists() || arquivos.size() >= limiteArquivos) {
            return;
        }

        File[] filhos = pasta.listFiles();
        if (filhos == null) {
            return;
        }

        for (int i = 0; i < filhos.length && arquivos.size() < limiteArquivos; i++) {
            File atual = filhos[i];
            if (atual.isDirectory()) {
                String nome = atual.getName();
                if ("target".equals(nome) || ".git".equals(nome) || "bin".equals(nome) || ".settings".equals(nome)) {
                    continue;
                }
                collectXmlFiles(atual, arquivos, limiteArquivos);
            } else if (atual.getName().toLowerCase().endsWith(".xml")) {
                arquivos.add(atual);
            }
        }
    }

    /** * Classifica chamada de API de persistencia em categoria tatica. * * @param chamada trecho da chamada * @return tipo tatico classificado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String classifyApiCall(String chamada) {
        if (chamada == null) {
            return "PERSISTENCE_API_CALL";
        }

        String valor = chamada.toLowerCase();

        if (valor.contains("createsqlquery")) {
            return "HIBERNATE_API_CALL";
        }
        if (valor.contains("createnativequery")) {
            return "JPA_API_CALL";
        }
        if (valor.contains("preparestatement")) {
            return "JDBC_API_CALL";
        }
        if (valor.contains("namedquery")) {
            return "NAMED_QUERY_USAGE";
        }
        if (valor.contains("createquery")) {
            return "PERSISTENCE_API_CALL";
        }
        return "PERSISTENCE_API_CALL";
    }

    /** * Detecta se um literal aparenta ser SQL. * * @param texto texto a verificar * @return true quando a heuristica indicar SQL * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean looksLikeSql(String texto) {
        if (isBlank(texto)) {
            return false;
        }

        String t = texto.toLowerCase();
        return t.startsWith("select ")
                || t.startsWith("insert ")
                || t.startsWith("update ")
                || t.startsWith("delete ")
                || t.startsWith("merge ")
                || t.startsWith("with ")
                || t.contains(" from ")
                || t.contains(" where ")
                || t.contains(" join ")
                || t.contains(" order by ")
                || t.contains(" group by ")
                || t.contains(" into ")
                || t.contains(" values ")
                || t.contains(" set ");
    }

    /** * Detecta se um literal aparenta ser HQL ou JPQL. * * @param texto texto a verificar * @return true quando a heuristica indicar HQL ou JPQL * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean looksLikeHqlOrJpql(String texto) {
        if (isBlank(texto)) {
            return false;
        }

        String t = texto.toLowerCase();
        return t.startsWith("from ")
                || t.startsWith("select ")
                || t.startsWith("delete from ")
                || t.startsWith("update ")
                || t.contains(" join fetch ")
                || t.contains(" from ")
                || t.contains(" where ")
                || t.contains(" order by ")
                || t.contains(" group by ");
    }

    /** * Estima a linha aproximada de um offset dentro do fonte. * * @param source fonte textual * @param offset offset de origem * @return numero de linha aproximado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private int estimateLine(String source, int offset) {
        if (source == null || offset < 0) {
            return -1;
        }

        int line = 1;
        for (int i = 0; i < source.length() && i < offset; i++) {
            if (source.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    /** * Formata o relatorio final de evidencias. * * @param origem descricao da origem analisada * @param limite limite maximo aplicado * @param incluirXml flag de inclusao de XML * @param evidencias evidencias encontradas * @return relatorio textual final * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String formatReport(String origem, int limite, boolean incluirXml, List<String> evidencias) {
        StringBuilder relatorio = new StringBuilder();
        relatorio.append("Relatorio de Extracao de Queries").append("\n");
        relatorio.append("Origem: ").append(origem).append("\n");
        relatorio.append("Limite aplicado: ").append(limite).append("\n");
        relatorio.append("Incluir XML: ").append(incluirXml ? "sim" : "nao").append("\n\n");

        for (int i = 0; i < evidencias.size(); i++) {
            relatorio.append(i + 1).append(". ").append(evidencias.get(i)).append("\n");
        }

        relatorio.append("\nObservacao: este relatorio devolve evidencias concretas de HQL, SQL, Criteria, APIs de persistencia, annotations e XML. ");
        relatorio.append("Nao assume versao especifica de Hibernate/JPA e nao reconstrui consultas dinamicas sem evidencia suficiente.");

        return relatorio.toString();
    }

    /** * Gera o nome qualificado seguro do metodo alvo. * * @param metodo metodo de origem * @return nome qualificado seguro * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String safeQualifiedMethodName(IMethod metodo) {
        try {
            if (metodo != null && metodo.getDeclaringType() != null) {
                return metodo.getDeclaringType().getFullyQualifiedName() + "." + metodo.getElementName();
            }
        } catch (Exception e) {
        }
        return "MetodoDesconhecido";
    }

    /** * Compara assinatura de dois metodos JDT. * * @param a primeiro metodo * @param b segundo metodo * @return true quando as assinaturas forem equivalentes * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean sameSignature(IMethod a, IMethod b) {
        if (a == null || b == null) {
            return false;
        }

        try {
            if (!a.getElementName().equals(b.getElementName())) {
                return false;
            }

            String[] paramsA = a.getParameterTypes();
            String[] paramsB = b.getParameterTypes();

            if (paramsA == null && paramsB == null) {
                return true;
            }
            if (paramsA == null || paramsB == null) {
                return false;
            }
            if (paramsA.length != paramsB.length) {
                return false;
            }

            for (int i = 0; i < paramsA.length; i++) {
                if (!paramsA[i].equals(paramsB[i])) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** * Le o conteudo textual de um arquivo. * * @param arquivo arquivo a ser lido * @return conteudo textual ou string vazia * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String readFile(File arquivo) {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(arquivo));
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

    /** * Normaliza caminho fisico para formato com barras normais. * * @param arquivo arquivo de origem * @return caminho normalizado * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String normalizePath(File arquivo) {
        if (arquivo == null) {
            return "";
        }

        try {
            return arquivo.getCanonicalPath().replace("\\", "/");
        } catch (Exception e) {
            return arquivo.getAbsolutePath().replace("\\", "/");
        }
    }

    /** * Remove extensao do nome de arquivo quando presente. * * @param nomeArquivo nome original * @return nome sem extensao * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String stripExtension(String nomeArquivo) {
        if (nomeArquivo == null) {
            return "";
        }
        int idx = nomeArquivo.lastIndexOf('.');
        if (idx <= 0) {
            return nomeArquivo;
        }
        return nomeArquivo.substring(0, idx);
    }

    /** * Trunca texto grande para preservar legibilidade. * * @param valor texto original * @param max tamanho maximo * @return texto truncado quando necessario * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String truncate(String valor, int max) {
        if (valor == null) {
            return "";
        }
        if (valor.length() <= max) {
            return valor;
        }
        return valor.substring(0, max) + "...";
    }

    /** * Limpa texto XML removendo CDATA e excesso de espacos. * * @param valor texto XML bruto * @return texto limpo * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private String cleanXmlText(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replaceAll("<!\\[CDATA\\[", "")
                .replaceAll("\\]\\]>", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /** * Retorna true quando o valor informado for nulo ou vazio. * * @param valor valor a validar * @return true quando o valor estiver em branco * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean isBlank(String valor) {
        return valor == null || valor.trim().length() == 0;
    }

    /** * Estrutura interna de resolucao do alvo de extracao. * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private static class ExtractionTarget {
        private String source;
        private String originDescription;
        private String primaryTypeName;
        private File relatedJavaFile;
    }
}