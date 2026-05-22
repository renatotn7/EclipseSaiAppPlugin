package com.mcp.sailibrary.plugin.agent.tools.jdt;

/* version: "1.0"
 * dependencies:
 * - org.eclipse.jdt.core
 * - java.io.File
 * - java.io.BufferedReader
 * purpose:
 *  "Fatiar arquivos gigantes via AST, retornando apenas o esqueleto estrutural ou um metodo cirurgico especifico, blindando o contexto da IA."
 *
 *  design_pattern: "Command / Visitor / Facade"
 * --- */

import org.eclipse.jdt.core.dom.*;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.tools.support.ToolJsonSupport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolParameterMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadataProvider;

public class JdtSourceSliceTool implements AgentTool, AgentToolPromptMetadataProvider {
    
    private File raizProjetoWorkspace;

    /**
 * Descrição não fornecida.
 *
 * @author Renato Tomaz Nati
 */
    public JdtSourceSliceTool(File raizProjetoWorkspace) {
        this.raizProjetoWorkspace = raizProjetoWorkspace;
    }

    @Override
    public String getName() {
        return "leitura_cirurgica_jdt";
    }
    @Override
    public AgentToolPromptMetadata getPromptMetadata() {
        AgentToolPromptMetadata metadata = new AgentToolPromptMetadata();
        metadata.setToolName(getName());
        metadata.setOneLinePurpose("Fatiar arquivo via AST para retornar esqueleto ou metodo especifico.");
        metadata.setActivityDescription("Fatiador cirurgico de codigo via AST JDT.");

        AgentToolParameterMetadata path = new AgentToolParameterMetadata();
        path.setName("path");
        path.setRequired(true);
        path.setDescription("Caminho do arquivo a ser fatiado.");
        path.setExampleValue("src/main/java/com/exemplo/ClasseGigante.java");
        metadata.addParameter(path);

        AgentToolParameterMetadata modo = new AgentToolParameterMetadata();
        modo.setName("modo");
        modo.setRequired(true);
        modo.setDescription("Modo de leitura, como esqueleto ou metodo.");
        modo.setExampleValue("esqueleto");
        metadata.addParameter(modo);

        AgentToolParameterMetadata alvo = new AgentToolParameterMetadata();
        alvo.setName("alvo");
        alvo.setRequired(false);
        alvo.setDescription("Nome do metodo quando o modo for metodo.");
        alvo.setExampleValue("setupEnv");
        metadata.addParameter(alvo);

        metadata.addRecommendedUseCase("Use quando a classe for grande demais para leitura integral.");
        metadata.addRecommendedUseCase("Use para obter somente o esqueleto estrutural de uma classe.");
        metadata.addRecommendedUseCase("Use para extrair cirurgicamente um metodo especifico sem carregar o arquivo inteiro.");

        metadata.addGuardrail("O path deve apontar para arquivo real dentro do perimetro seguro.");
        metadata.addGuardrail("Use modo metodo apenas quando o nome do metodo estiver claro.");
        metadata.addGuardrail("Nao use esta ferramenta para busca textual ampla.");

        metadata.addJsonExample(
                "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"leitura_cirurgica_jdt\\\",\\\"parameters\\\":{\\\"path\\\":\\\"src/main/java/com/exemplo/ClasseGigante.java\\\",\\\"modo\\\":\\\"esqueleto\\\",\\\"alvo\\\":\\\"\\\"},\\\"explanation\\\":\\\"Preciso mapear a estrutura da classe sem carregar todo o conteudo no contexto.\\\"}"
        );

        return metadata;
    }
    /**
 * Descrição não fornecida.
 *
 * @author Renato Tomaz Nati
 */
    @Override
    public String execute(String jsonParameters) {
    	String path = ToolJsonSupport.extractJsonStringValue(jsonParameters, "path");
    	String modo = ToolJsonSupport.extractJsonStringValue(jsonParameters, "modo");
    	String alvo = ToolJsonSupport.extractJsonStringValue(jsonParameters, "alvo");
        if (path == null || path.trim().length() == 0) {
            return "Erro Tatico: Parametro 'path' nao fornecido.";
        }

        if (modo == null || modo.trim().length() == 0) {
            modo = "esqueleto"; // Resguardo passivo padrao
        }

        File alvoArquivo = new File(raizProjetoWorkspace, path);
        
        if (!alvoArquivo.exists() || !alvoArquivo.isFile()) {
            alvoArquivo = new File(path);
            if (!alvoArquivo.exists() || !alvoArquivo.isFile()) {
                return "Erro Tatico: Arquivo nao encontrado no perimetro -> " + path;
            }
        }

        String conteudo = lerConteudoArquivo(alvoArquivo);
        if (conteudo == null || conteudo.trim().length() == 0) {
            return "Erro Tatico: Arquivo vazio ou ilegivel.";
        }

        try {
            ASTParser parser = ASTParser.newParser(AST.JLS21);
            parser.setSource(conteudo.toCharArray());
            parser.setKind(ASTParser.K_COMPILATION_UNIT);
            CompilationUnit astNode = (CompilationUnit) parser.createAST(null);

            if ("esqueleto".equalsIgnoreCase(modo)) {
                return extrairEsqueleto(astNode, path);
            } else if ("metodo".equalsIgnoreCase(modo)) {
                if (alvo == null || alvo.trim().length() == 0) {
                    return "Erro Tatico: Parametro 'alvo' (nome do metodo) exigido para o modo 'metodo'.";
                }
                return extrairMetodo(astNode, alvo, path);
            } else {
                return "Erro Tatico: Modo invalido. Use estritamente 'esqueleto' ou 'metodo'.";
            }
        } catch (Exception e) {
            return "Falha critica durante varredura AST JDT: " + e.getMessage();
        }
    }

    /**
 * Descrição não fornecida.
 *
 * @author Renato Tomaz Nati
 */
    private String extrairEsqueleto(CompilationUnit astNode, String path) {
        final StringBuilder relatorio = new StringBuilder();
        relatorio.append("=== ESQUELETO AST (Raio-X) ===\n");
        relatorio.append("Arquivo: ").append(path).append("\n");

        astNode.accept(new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {
                relatorio.append("Classe/Interface: ").append(node.getName().getIdentifier()).append("\n");
                return true;
            }

            @Override
            public boolean visit(FieldDeclaration node) {
                List<?> fragmentos = node.fragments();
                for (int i = 0; i < fragmentos.size(); i++) {
                    VariableDeclarationFragment frag = (VariableDeclarationFragment) fragmentos.get(i);
                    relatorio.append("  [Atributo] ").append(node.getType().toString()).append(" ").append(frag.getName().getIdentifier()).append("\n");
                }
                return false;
            }

            @Override
            public boolean visit(MethodDeclaration node) {
                relatorio.append("  [Metodo] ");
                if (node.getReturnType2() != null) {
                    relatorio.append(node.getReturnType2().toString()).append(" ");
                }
                relatorio.append(node.getName().getIdentifier()).append("(");

                List<?> params = node.parameters();
                for (int i = 0; i < params.size(); i++) {
                    SingleVariableDeclaration param = (SingleVariableDeclaration) params.get(i);
                    relatorio.append(param.getType().toString()).append(" ").append(param.getName().getIdentifier());
                    if (i < params.size() - 1) {
                        relatorio.append(", ");
                    }
                }
                relatorio.append(")\n");

                // Bloqueio tatico: Retornar false impede o parser de ler o conteudo do corpo (bloco {}) do metodo
                return false;
            }
        });

        relatorio.append("\n[INSTRUCAO]: Para ver o corpo isolado de algum metodo acima, chame esta ferramenta com modo='metodo' e alvo='nomeDoMetodo'.\n");
        return relatorio.toString();
    }

    /**
 * Descrição não fornecida.
 *
 * @author Renato Tomaz Nati
 */
    private String extrairMetodo(CompilationUnit astNode, final String alvo, String path) {
        final StringBuilder relatorio = new StringBuilder();
        final boolean[] encontrado = new boolean[1];

        astNode.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                if (node.getName().getIdentifier().equals(alvo)) {
                    relatorio.append("=== EXTRACAO CIRURGICA DE METODO ===\n");
                    relatorio.append("Metodo alvo: ").append(alvo).append("\n");

                    // A magica do AST: converte a sub-arvore perfeita de volta para texto
                    relatorio.append(node.toString());

                    relatorio.append("\n====================================\n");
                    encontrado[0] = true;
                }
                return true; // Continua verificando em caso de sobrecarga (overload) do mesmo nome
            }
        });

        if (!encontrado[0]) {
            return "Erro Tatico: Metodo [" + alvo + "] nao localizado no arquivo " + path + ". Verifique o nome exato no Esqueleto.";
        }
        return relatorio.toString();
    }

    /**
 * Descrição não fornecida.
 *
 * @author Renato Tomaz Nati
 */
    private String lerConteudoArquivo(File arquivo) {
        StringBuilder conteudo = new StringBuilder();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(arquivo));
            String linha;
            while ((linha = reader.readLine()) != null) {
                conteudo.append(linha).append(System.lineSeparator());
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ex) {
                    // ignorado de forma segura
                }
            }
        }
        return conteudo.toString();
    }

   
}