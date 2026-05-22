package com.mcp.sailibrary.plugin.agent.tools.jdt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.tools.support.ToolJsonSupport;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolParameterMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadataProvider;
/** * --- * yaml_header: * version: "1.0" * dependencies: * - org.eclipse.jdt.core: "3.38.0" * - org.eclipse.jdt.core.search * purpose: "Localizar chamadores de um metodo usando busca reversa por referencias no workspace JDT." * design_pattern: "Command / Adapter" * --- */
public class MethodCallersSearchTool implements AgentTool, AgentToolPromptMetadataProvider {

    private ICompilationUnit compilationUnitAtual;
    private int offsetAtual;

    public MethodCallersSearchTool(ICompilationUnit compilationUnitAtual, int offsetAtual) {
        this.compilationUnitAtual = compilationUnitAtual;
        this.offsetAtual = offsetAtual;
    }

    @Override
    public String getName() {
        return "buscar_chamadores_jdt";
    }

    /**
 * Localiza chamadores do metodo alvo por nome ou pelo metodo atual do editor.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
    @Override
    public String execute(String jsonParameters) {
        String modo = ToolJsonSupport.extractJsonStringValue(jsonParameters, "modo");
        String nomeClasse = ToolJsonSupport.extractJsonStringValue(jsonParameters, "classe");
        String nomeMetodo =  ToolJsonSupport.extractJsonStringValue(jsonParameters, "metodo");
        int limiteResultados = ToolJsonSupport.extractJsonIntValue(jsonParameters, "limite", 20, 1, 100);

        if (limiteResultados <= 0) {
            limiteResultados = 20;
        }

        if (limiteResultados > 100) {
            limiteResultados = 100;
        }

        IMethod metodoAlvo = resolveTargetMethod(modo, nomeClasse, nomeMetodo);
        if (metodoAlvo == null) {
            return "Erro Operacional: Nao foi possivel localizar o metodo alvo para busca de chamadores.";
        }

        return findCallers(metodoAlvo, limiteResultados);
    }
    @Override
    public AgentToolPromptMetadata getPromptMetadata() {
        AgentToolPromptMetadata metadata = new AgentToolPromptMetadata();
        metadata.setToolName(getName());
        metadata.setOneLinePurpose("Localizar chamadores reais de um metodo por referencias JDT.");
        metadata.setActivityDescription("Encontra com precisao os locais que invocam um metodo ou classe especifica.");

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

        AgentToolParameterMetadata limite = new AgentToolParameterMetadata();
        limite.setName("limite");
        limite.setRequired(false);
        limite.setDescription("Limite maximo de chamadores retornados.");
        limite.setExampleValue("20");
        metadata.addParameter(limite);

        metadata.addRecommendedUseCase("Use quando precisar medir impacto transversal de uma alteracao.");
        metadata.addRecommendedUseCase("Use quando quiser saber quem invoca o metodo atual.");
        metadata.addRecommendedUseCase("Use antes de editar metodo com suspeita de alto reaproveitamento.");

        metadata.addGuardrail("Nao confunda chamadores com callees.");
        metadata.addGuardrail("Resultados locais AST e busca global JDT devem ser interpretados em conjunto.");
        metadata.addGuardrail("Mantenha o limite prudente para evitar excesso de ruido.");

        metadata.addJsonExample(
                "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"buscar_chamadores_jdt\\\",\\\"parameters\\\":{\\\"classe\\\":\\\"RelatorioAcompanhamentoDivisaoAction\\\",\\\"metodo\\\":\\\"setupEnv\\\",\\\"limite\\\":\\\"20\\\"},\\\"explanation\\\":\\\"Preciso localizar os chamadores reais deste metodo antes de alterar seu comportamento.\\\"}"
        );

        return metadata;
    }
    /**
 * Resolve o metodo alvo por contexto do editor ou por classe e metodo informados.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
    private IMethod resolveTargetMethod(String modo, String nomeClasse, String nomeMetodo) {
        // Feature: Se a IA pedir explicitamente o editor ativo, OU se esquecer de passar a classe/metodo, usa o cursor atual.
        if ("editor_ativo".equalsIgnoreCase(modo) || (nomeClasse.isEmpty() && nomeMetodo.isEmpty())) {
            return resolverMetodoPorEditor();
        }

        return findMethodByName(nomeClasse, nomeMetodo);
    }
    /**
 * Resolve o metodo alvo a partir do offset atual do editor.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
    private IMethod resolverMetodoPorEditor() {
        if (compilationUnitAtual == null) {
            return null;
        }

        try {
            ASTParser parser = ASTParser.newParser(AST.JLS21);
            parser.setSource(compilationUnitAtual);
            parser.setKind(ASTParser.K_COMPILATION_UNIT);
            parser.setResolveBindings(true);
            parser.setBindingsRecovery(true);
            parser.setProject(compilationUnitAtual.getJavaProject());

            final CompilationUnit astNode = (CompilationUnit) parser.createAST(null);
            final IMethod[] metodoEncontrado = new IMethod[1];

            astNode.accept(new ASTVisitor() {
                @Override
                public boolean visit(MethodDeclaration node) {
                    int inicio = node.getStartPosition();
                    int fim = inicio + node.getLength();

                    if (offsetAtual >= inicio && offsetAtual <= fim) {
                        if (node.resolveBinding() != null && node.resolveBinding().getJavaElement() instanceof IMethod) {
                            metodoEncontrado[0] = (IMethod) node.resolveBinding().getJavaElement();
                            return false;
                        }
                    }

                    return true;
                }
            });

            if (metodoEncontrado[0] != null) {
                return metodoEncontrado[0];
            }

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

    /**
 * Localiza o metodo alvo priorizando drasticamente o arquivo atual antes de varrer o projeto.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
    private IMethod findMethodByName(String nomeClasse, String nomeMetodo) {
        if (compilationUnitAtual == null || compilationUnitAtual.getJavaProject() == null) {
            return null;
        }

        try {
            IType tipo = null;

            // 1. OTIMIZAÇÃO TÁTICA (Foco Local): Verifica se a classe solicitada é a que já está aberta no editor.
            // Isso impede que o Eclipse faça varreduras globais desnecessárias quando o alvo está na cara dele.
            IType tipoPrimario = compilationUnitAtual.findPrimaryType();
            if (tipoPrimario != null) {
                String nomeSimplesAtual = tipoPrimario.getElementName();
                String fqcnAtual = tipoPrimario.getFullyQualifiedName();
                
                if (nomeSimplesAtual.equals(nomeClasse) || fqcnAtual.equals(nomeClasse)) {
                    tipo = tipoPrimario;
                }
            }

            // 2. Busca exata por FQCN no projeto (caso seja uma classe externa)
            if (tipo == null) {
                tipo = compilationUnitAtual.getJavaProject().findType(nomeClasse);
            }

            // 3. Fallback de alto custo: varrendo pacotes procurando o nome simples
            if (tipo == null) {
                tipo = findTypeBySimpleName(nomeClasse);
            }

            // Se falhou em todas as frentes, aborta.
            if (tipo == null) {
                return null;
            }

            // Encontrou o tipo (Classe). Agora extrai o método exato.
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

    /**
 * Faz busca por tipo usando nome simples no projeto atual.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
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

    /**
 * Descrição não fornecida.
 *
 * @author Renato Tomaz Nati
 */
    private String findCallers(IMethod metodoAlvo, final int limiteResultados) {
        if (metodoAlvo == null) {
            return "Erro Operacional: Metodo alvo nulo para busca de chamadores.";
        }

        try {
            final List<String> resultados = new ArrayList<String>();
            final Set<Integer> offsetsEncontrados = new HashSet<Integer>();

            // Feature: Passo 1 - Varredura AST Local (1a Linha de Defesa). 
            // Resolve a cegueira de heranca/static binding do JDT Indexer garantindo a leitura do proprio arquivo.
            buscarChamadoresLocaisAST(metodoAlvo, resultados, offsetsEncontrados, limiteResultados);

            // Se o limite ja foi atingido apenas com chamadores locais, poupamos a varredura global
            if (resultados.size() >= limiteResultados) {
                return formatarRelatorio(metodoAlvo, limiteResultados, resultados);
            }

            // Passo 2: Varredura Global (JDT Workspace) para achar chamadores em outras classes
            SearchPattern padrao = SearchPattern.createPattern(metodoAlvo, IJavaSearchConstants.REFERENCES);
            if (padrao != null) {
                IJavaSearchScope escopo = SearchEngine.createWorkspaceScope();
                SearchEngine motorBusca = new SearchEngine();
                SearchParticipant[] participantes = new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() };

                SearchRequestor requestor = new SearchRequestor() {
                    @Override
                    public void acceptSearchMatch(SearchMatch match) {
                        if (resultados.size() >= limiteResultados) {
                            return;
                        }

                        // Bloqueio defensivo: Nao duplica as chamadas locais ja extraidas pelo AST
                        if (offsetsEncontrados.contains(match.getOffset())) {
                            return; 
                        }
                        offsetsEncontrados.add(match.getOffset());

                        String descricao = descreverChamador(match);
                        if (descricao != null && descricao.trim().length() > 0) {
                            resultados.add(descricao);
                        }
                    }
                };

                motorBusca.search(padrao, participantes, escopo, requestor, new NullProgressMonitor());
            }

            if (resultados.isEmpty()) {
                return "Nenhum chamador foi localizado para o metodo alvo [" + metodoAlvo.getElementName() + "].";
            }

            return formatarRelatorio(metodoAlvo, limiteResultados, resultados);

        } catch (Exception e) {
            return "Falha critica durante busca de chamadores: " + e.getMessage();
        }
    }
    /**
 * Descrição não fornecida.
 *
 * @author Renato Tomaz Nati
 */
    private void buscarChamadoresLocaisAST(final IMethod metodoAlvo, final List<String> resultados, final Set<Integer> offsets, final int limite) {
        if (compilationUnitAtual == null) return;
        
        try {
            ASTParser parser = ASTParser.newParser(AST.JLS21);
            parser.setSource(compilationUnitAtual);
            parser.setKind(ASTParser.K_COMPILATION_UNIT);
            
            final CompilationUnit astNode = (CompilationUnit) parser.createAST(null);
            
            astNode.accept(new ASTVisitor() {
                @Override
                public boolean visit(MethodInvocation node) {
                    if (resultados.size() >= limite) return false;
                    
                    if (node.getName().getIdentifier().equals(metodoAlvo.getElementName())) {
                        // Verifica se a chamada e local (ex: metodo() ou this.metodo())
                        boolean isLocalCall = (node.getExpression() == null || node.getExpression() instanceof ThisExpression);
                        
                        if (isLocalCall) {
                            int offset = node.getStartPosition();
                            if (!offsets.contains(offset)) {
                                offsets.add(offset);
                                int linha = astNode.getLineNumber(offset);
                                
                                org.eclipse.jdt.core.dom.ASTNode parent = node.getParent();
                                while (parent != null && !(parent instanceof MethodDeclaration)) {
                                    parent = parent.getParent();
                                }
                                
                                String nomeChamador = "EscopoGlobal";
                                if (parent instanceof MethodDeclaration) {
                                    nomeChamador = ((MethodDeclaration) parent).getName().getIdentifier();
                                }
                                
                                String classeNome = "ClasseDesconhecida";
                                try {
                                    if (compilationUnitAtual.findPrimaryType() != null) {
                                        classeNome = compilationUnitAtual.findPrimaryType().getElementName();
                                    }
                                } catch (Exception e) {}
                                
                                String descricao = "Posivel chamador " + classeNome+"."+nomeChamador +"("+linha+"')" + " Encontrado por:  "+ "[AST Local]";
                                resultados.add(descricao);
                            }
                        }
                    }
                    return true;
                }
            });
        } catch (Exception e) {
            // Falha silenciosa permitida. Cai nativamente no fallback do JDT Workspace
        }
    }
    /**
 * Descrição não fornecida.
 *
 * @author Renato Tomaz Nati
 */
    private String formatarRelatorio(IMethod metodoAlvo, int limiteResultados, List<String> resultados) {
        StringBuilder relatorio = new StringBuilder();
        relatorio.append("Relatorio de Chamadores JDT").append("\n");
        relatorio.append("Metodo alvo: ").append(metodoAlvo.getDeclaringType().getFullyQualifiedName()).append(".").append(metodoAlvo.getElementName()).append("\n");
        relatorio.append("Limite aplicado: ").append(limiteResultados).append("\n\n");

        for (int i = 0; i < resultados.size(); i++) {
            relatorio.append(i + 1).append(". ").append(resultados.get(i)).append("\n");
        }

        if (resultados.size() >= limiteResultados) {
            relatorio.append("\n");
            relatorio.append("[AVISO]: A busca foi interrompida no limite configurado de resultados.");
        }

        return relatorio.toString();
    }
    /**
 * Converte um SearchMatch em descricao civil e legivel do chamador.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
    private String descreverChamador(SearchMatch match) {
        if (match == null) {
            return null;
        }

        Object elementoBruto = match.getElement();
        if (!(elementoBruto instanceof IJavaElement)) {
            return null;
        }

        IJavaElement elemento = (IJavaElement) elementoBruto;
        ICompilationUnit unidade = (ICompilationUnit) elemento.getAncestor(IJavaElement.COMPILATION_UNIT);

        if (unidade == null) {
            if (elemento instanceof ICompilationUnit) {
                unidade = (ICompilationUnit) elemento;
            } else {
                return null;
            }
        }

        IMethod metodoChamador = resolverMetodoChamadorPorOffset(unidade, match.getOffset());
        
        String nomeClasse = "ClasseDesconhecida";
        String nomeMetodo = "MetodoDesconhecido";

        try {
            IType tipoPrimario = unidade.findPrimaryType();
            if (tipoPrimario != null) {
                nomeClasse = tipoPrimario.getElementName();
            }
        } catch (Exception e) {
        }

        if (metodoChamador != null) {
            try {
                if (metodoChamador.getDeclaringType() != null) {
                    nomeClasse = metodoChamador.getDeclaringType().getElementName();
                }
                nomeMetodo = metodoChamador.getElementName();
            } catch (Exception e) {
            }
        }

        int linha = obterLinhaDaReferencia(unidade, match.getOffset());
        String descricao = "Busca por chamadores de  " + nomeClasse+"."+nomeMetodo +"("+linha+"')" + " Encontrado por:  "+ "[AST Local]";
        return descricao.toString();
    }

    /**
 * Resolve o metodo chamador mais proximo do offset da referencia encontrada.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
    private IMethod resolverMetodoChamadorPorOffset(ICompilationUnit unidade, final int offsetReferencia) {
        if (unidade == null || offsetReferencia < 0) {
            return null;
        }

        try {
            ASTParser parser = ASTParser.newParser(AST.JLS21);
            parser.setSource(unidade);
            parser.setKind(ASTParser.K_COMPILATION_UNIT);
            parser.setResolveBindings(true);
            parser.setBindingsRecovery(true);
            parser.setProject(unidade.getJavaProject());

            final CompilationUnit astNode = (CompilationUnit) parser.createAST(null);
            final IMethod[] metodoEncontrado = new IMethod[1];

            astNode.accept(new ASTVisitor() {
                @Override
                public boolean visit(MethodDeclaration node) {
                    int inicio = node.getStartPosition();
                    int fim = inicio + node.getLength();

                    if (offsetReferencia >= inicio && offsetReferencia <= fim) {
                        if (node.resolveBinding() != null && node.resolveBinding().getJavaElement() instanceof IMethod) {
                            metodoEncontrado[0] = (IMethod) node.resolveBinding().getJavaElement();
                            return false;
                        }
                    }

                    return true;
                }
            });

            return metodoEncontrado[0];
        } catch (Exception e) {
            return null;
        }
    }

    /**
 * Calcula a linha aproximada da referencia no arquivo.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
    private int obterLinhaDaReferencia(ICompilationUnit unidade, int offsetReferencia) {
        if (unidade == null || offsetReferencia < 0) {
            return -1;
        }

        try {
            ASTParser parser = ASTParser.newParser(AST.JLS21);
            parser.setSource(unidade);
            parser.setKind(ASTParser.K_COMPILATION_UNIT);

            CompilationUnit astNode = (CompilationUnit) parser.createAST(null);
            return astNode.getLineNumber(offsetReferencia);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
 * Extrai valor simples de uma chave em JSON plano.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */

}