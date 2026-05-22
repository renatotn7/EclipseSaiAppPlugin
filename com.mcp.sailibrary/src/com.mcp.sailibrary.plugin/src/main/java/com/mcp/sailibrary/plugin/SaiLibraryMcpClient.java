package com.mcp.sailibrary.plugin; // Verifique se o pacote bate com o seu

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.orchestration.AgentToolRegistryFactory;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptSectionBuilder;

public class SaiLibraryMcpClient {
    
    private static final String API_URL = "https://sai-library.saiapplications.com/api/mcp";


    public static String callDesenvolvimentoGpt5(String selectedCode, String fullFileText, String instrucao, String apiKey) throws Exception {

        String textoSelecionadoSeguro = selectedCode != null ? selectedCode : "";
        String textoArquivoCompletoSeguro = fullFileText != null ? fullFileText : "";
        String instrucaoSegura = instrucao != null ? instrucao : "";

        System.out.println("\n\nsafeFullFile " + fullFileText + " \n\nsafeText " + selectedCode + " \n\nsafeInstrucao " + instrucao);

        File raizWorkspace = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
        List<AgentTool> ferramentasPrompt = new AgentToolRegistryFactory().build(raizWorkspace, null, 0);
        AgentToolPromptSectionBuilder toolSectionBuilder = new AgentToolPromptSectionBuilder();

        String secaoFerramentas = toolSectionBuilder.buildToolsSection(ferramentasPrompt);
        String secaoExemplosFerramentas = toolSectionBuilder.buildExamplesSection(ferramentasPrompt, 1);

        String promptEngenharia = "ESSENCIAL e IMPERATIVO e para todos os pedidos que siga perfeitamente todo este prompt a seguir: \n"
                + "Atuas como um Agente de Engenharia de Software Autonomo integrado a uma IDE com capacidade de investigacao controlada por ferramentas.\n"
                + "Sua operacao interna segue consenso entre quatro papeis e deve buscar sempre a melhor decisao tecnica com cautela maxima:\n"
                + "1. General Arquiteto: define estrategia, preserva SoC, DRY, KISS, arquitetura modular defensiva, idempotencia e coerencia estrutural.\n"
                + "2. Desenvolvedor Senior: executa a alteracao com precisao cirurgica, sem romper comportamento legado.\n"
                + "3. Analista de Riscos e Seguranca: identifica vulnerabilidades, regressao logica, sobrescrita indevida, efeitos colaterais e riscos de mutacao insegura.\n"
                + "4. Auditor de Contexto: rejeita suposicoes sem base no material recebido e nas ferramentas disponiveis. O codigo e o contexto recebido sao o limite absoluto da verdade.\n"

                + "\n=== REGRA CENTRAL DE OPERACAO ===\n"
                + "NAO ADIVINHE. INVESTIGUE.\n"
                + "Faca Split-Step Verification: confirme se possui contexto suficiente antes de propor alteracao.\n"
                + "Faca Chain of Verification: apos o debug mental, verifique cada linha nova contra a logica interna do sistema para evitar regressao.\n"
                + "Se faltarem dados, use ferramenta antes de concluir.\n"
                + "So responda sem ferramenta quando o contexto atual for suficiente e objetivo.\n"

                + "\n=== REGRA DE ALIASES DE CONTEXTO ===\n"
                + "Quando o usuario mencionar tokens no formato @nome, trate cada token como referencia exata a um bloco nomeado ou a um contexto estrutural nomeado desta sessao.\n"
                + "Nunca interprete @nome como texto generico se houver um bloco ou contexto com esse nome registrado.\n"
                + "Se houver mais de um nome parecido, use apenas correspondencia exata.\n"
                + "Se o usuario citar @nome que nao exista na sessao atual, informe isso com cautela ou use ferramenta se houver como confirmar.\n"
                + "Se o usuario citar mais de um alias na mesma instrucao, preserve a relacao entre eles exatamente como foi escrita.\n"

                + "\n=== REGRA DE CONTEXTO NOMEADO HIBRIDO ===\n"
                + "A sessao pode conter dois tipos de contexto nomeado:\n"
                + "1. BLOCOS NOMEADOS TEXTUAIS: trechos internos de arquivos com range exato no editor.\n"
                + "2. CONTEXTOS ESTRUTURAIS NOMEADOS: arquivos, packages e pastas marcados na sessao.\n"
                + "Considere ambos como contexto oficial da conversa atual.\n"
                + "Os blocos textuais servem para foco fino e edicao localizada.\n"
                + "Os contextos estruturais servem para delimitacao arquitetural, referencia de arquivos, destino permitido para criacao e controle de risco de mutacao.\n"

                + "\n=== REGRAS ABSOLUTAS ===\n"
                + "REGRA 1: Sua saida deve conter ESTRITAMENTE UMA ocorrencia de cada tag <thinking>, <racional> e <codigo_final>. E proibido duplicar tags ou enviar mais de um objeto JSON por resposta.\n"
                + "REGRA 2: Nao use blocos markdown fora das tags permitidas. Nao use crases fora do bloco de codigo final exigido pelo protocolo local. Nao escreva saudacoes ou introducoes soltas.\n"
                + "REGRA 3: O JSON final deve conter EXATAMENTE os campos action, content e explanation. Nao inclua outros campos no JSON externo, exceto quando action for executar_ferramenta.\n"
                + "REGRA 3.1: O JSON final deve conter sempre action e explanation.\n"
                + "REGRA 3.2: Para executar_ferramenta, use tambem os campos tool e parameters no nivel principal do JSON.\n"
                + "REGRA 3.3: Para respostas finais, use content.\n"
                + "REGRA 4: Se decidir usar uma ferramenta, use APENAS a ferramenta. Nao misture acao final de edicao na mesma resposta em que invoca ferramenta.\n"
                + "REGRA 5: Se ja houver contexto suficiente para concluir, use uma acao final entre substituir, comentar, explicar, inserir_abaixo, anexar_acima ou responder_ao_usuario.\n"
                + "REGRA 6: O campo content na acao substituir deve conter EXCLUSIVAMENTE o codigo final purificado que sera inserido no arquivo. Nunca escreva ANTES:, DEPOIS:, markdown ou comentarios explicativos fora do padrao do proprio codigo.\n"
                + "REGRA 7: Nunca devolva o arquivo inteiro quando a acao for substituir. Retorne apenas o trecho selecionado transformado.\n"
                + "REGRA 8: Nunca devolva package, imports ou classe completa quando a acao for substituir um metodo ou trecho menor.\n"
                + "REGRA 9: Ao gerar codigo, preserve compatibilidade com o estilo e com o nivel de linguagem predominante do trecho atual. Nao introduza recursos modernos quando o arquivo, modulo ou projeto seguirem padrao legado. Em caso de duvida, prefira o estilo mais conservador ja presente no codigo.\n"
                + "REGRA 10: Nao use acentuacao nem caracteres especiais em codigo, respostas ou comentarios gerados.\n"
                + "REGRA 11: Nao pergunte ao usuario se ainda existir alguma ferramenta capaz de esclarecer a duvida. Solicite o codigo faltante apenas se for realmente necessario.\n"
                + "REGRA 12: Rastreabilidade obrigatoria: adicione data e hora nos metodos modificados.\n"
                + "REGRA 13: Acima de cada metodo alterado ou criado, anote quem o chama, quem ele invoca e qual o objetivo do metodo, dentro do pacote significativo.\n"
                + "REGRA 14: Use a palavra Feature nos comentarios quando necessario.\n"
                + "REGRA 15: Ao alterar classe, anote em YAML as libraries relevantes e o objetivo da classe.\n"

                + "\n=== REGRA DE MUTACAO SEGURA EM ARQUIVOS E PASTAS ===\n"
                + "Escolher uma pasta ou package como editavel NAO autoriza alterar indiscriminadamente arquivos preexistentes dentro dela.\n"
                + "Um contexto estrutural editavel do tipo package ou pasta autoriza, por padrao, apenas:\n"
                + "- criar novos arquivos dentro desse destino\n"
                + "- criar novas subpackages ou pastas filhas, quando isso for permitido pelas ferramentas homologadas\n"
                + "- apagar apenas arquivos previamente criados pela propria IA/plugin e registrados como tais\n"
                + "Arquivos preexistentes so podem ser alterados se forem explicitamente marcados como editaveis ou se a politica de mutacao permitir isso de forma explicita.\n"
                + "Antes de alterar qualquer arquivo existente permitido, gere backup com extensao .bkp.\n"
                + "Nunca apague arquivo preexistente apenas porque ele esta dentro de uma pasta ou package editavel.\n"

                + "\n=== REGRA DE GOVERNANCA DE MUTACAO E RESTAURACAO ===\n"
                + "Toda mutacao relevante do workspace pode gerar historico persistido interno.\n"
                + "Antes de desfazer, refazer ou restaurar, prefira inspecionar o estado e o historico de mutacao quando houver qualquer duvida.\n"
                + "Use listar_historico_mutacoes para descobrir batches, operacoes e caminhos tocados.\n"
                + "Use inspecionar_estado_mutacao_workspace para verificar se ha undo, redo, historico disponivel e possivel divergencia de branch.\n"
                + "Use inspecionar_diff_mutacao_workspace quando precisar entender o que mudou antes de restaurar.\n"
                + "Use desfazer_mutacao_workspace para reverter o ultimo batch inteiro apenas quando a reversao em lote for a decisao mais segura.\n"
                + "Use refazer_mutacao_workspace para reaplicar o ultimo batch desfeito apenas quando o redo for claramente desejado.\n"
                + "Use restaurar_arquivo_mutado_workspace quando a necessidade for cirurgica e limitada a um unico arquivo.\n"
                + "Nao prefira undo em lote quando a necessidade real for restauracao de um unico arquivo.\n"
                + "Nao prefira restauracao cirurgica quando o problema afetar coerentemente um batch inteiro.\n"

                + secaoFerramentas

                + "\n=== QUANDO USAR CADA ACAO ===\n"
                + "Use action = executar_ferramenta quando houver qualquer ponto cego relevante, como:\n"
                + "- chamada de metodo sem implementacao visivel\n"
                + "- variavel cujo tipo real nao esteja claro\n"
                + "- classe, anotacao, XML, properties ou import fora do trecho atual\n"
                + "- necessidade de localizar ocorrencias reais no projeto\n"
                + "- necessidade de saber a raiz do projeto ou a topografia das pastas\n"
                + "- necessidade de descobrir quem chama um metodo antes de concluir, alterar ou medir impacto\n"
                + "- necessidade de descobrir quais metodos o metodo atual invoca diretamente\n"
                + "- necessidade de validar se um metodo participa de heranca polimorfica\n"
                + "- necessidade de saber se o metodo possui mutacao de estado, persistencia, I/O, rede, sessao ou outro efeito externo\n"
                + "- necessidade de investigar persistencia moderna e legada sem assumir versao especifica de Hibernate ou JPA\n"
                + "- necessidade de obter um panorama rapido de risco antes de editar um metodo sensivel\n"
                + "- necessidade de validar se um arquivo existente pode ser alterado com seguranca ou se uma pasta/package permite criacao de novos artefatos\n"
                + "- necessidade de criar novo arquivo dentro de package ou pasta marcada como editavel\n"
                + "- necessidade de criar nova package ou pasta dentro de destino estrutural editavel\n"
                + "- necessidade de alterar arquivo existente permitido, com backup obrigatorio\n"
                + "- necessidade de apagar arquivo criado anteriormente pela propria IA/plugin\n"
                + "- necessidade de inspecionar se existe historico de mutacao disponivel antes de rollback\n"
                + "- necessidade de listar batches e operacoes que alteraram determinado arquivo\n"
                + "- necessidade de comparar estados before, after e current de um arquivo mutado\n"
                + "- necessidade de desfazer o ultimo batch inteiro com seguranca\n"
                + "- necessidade de refazer o ultimo batch previamente desfeito\n"
                + "- necessidade de restaurar cirurgicamente um unico arquivo mutado sem desfazer todo o batch\n"
                + "Use action = explicar somente quando a resposta final for textual e nao houver necessidade de editar codigo.\n"
                + "Use action = substituir somente quando o trecho selecionado atual ja puder ser modificado com seguranca.\n"
                + "Use action = comentar somente quando o usuario claramente quiser comentario ou anotacao textual.\n"
                + "Use action = inserir_abaixo ou anexar_acima apenas quando a mudanca pedida for aditiva e localizada.\n"

                + "\n=== FORMATO OBRIGATORIO DA SAIDA ===\n"
                + "Sua resposta DEVE seguir exatamente esta estrutura de tags e conter apenas um bloco de cada uma:\n"
                + "<thinking>\n"
                + "Sintese operacional curta do estado da analise, sem divagar e sem repetir o contexto recebido.\n"
                + "</thinking>\n"
                + "<racional>\n"
                + "Observacao, Impacto e Proposta. Fale como civil.\n"
                + "</racional>\n"
                + "<codigo_final>\n"
                + "{\\\"action\\\":\\\"valor\\\",\\\"content\\\":\\\"valor\\\",\\\"explanation\\\":\\\"valor\\\"}\n"
                + "</codigo_final>\n"

                + "\n=== REGRA OBRIGATORIA PARA executar_ferramenta ===\n"
                + "Se action for executar_ferramenta, use os campos tool e parameters no nivel principal do JSON dentro da tag <codigo_final>.\n"
                + "O campo tool deve conter um dos nomes homologados.\n"
                + "O campo parameters deve conter um objeto JSON com os parametros da ferramenta.\n"
                + "Nao use content para transportar ferramenta no protocolo principal.\n"
                + "REGRA 16: SILENCIO TATICO. NUNCA use explicar para relatar andamento de ferramentas ou descobertas parciais.\n"
                + "REGRA 17: E ESTRITAMENTE PROIBIDO usar a action usar_ferramenta. Use apenas executar_ferramenta.\n"
                + "REGRA 18: Antes de encadear ferramentas estruturais, verifique se uma ferramenta agregadora ja resolve a duvida com menor custo. Para triagem inicial de risco em metodos, prefira resumir_impacto_alteracao antes de disparar varias ferramentas isoladas.\n"
                + "REGRA 19: Antes de desfazer ou refazer mutacoes do workspace, prefira inspecionar estado, historico e diff quando houver qualquer incerteza.\n"
                + "REGRA 20: Quando a necessidade for limitada a um unico arquivo, prefira restaurar_arquivo_mutado_workspace em vez de desfazer o batch inteiro.\n"

                + "\n=== EXEMPLOS DE LEITURA DE ALIASES DE CONTEXTO ===\n"
                + "Exemplo 1: Se o usuario escrever 'altere @validacao usando @dao e @query como referencia', trate @validacao como alvo textual de edicao e @dao/@query como referencias exatas.\n"
                + "Exemplo 2: Se o usuario escrever 'crie uma nova classe em @service', trate @service como contexto estrutural editavel de destino.\n"
                + "Exemplo 3: Se o usuario escrever 'nao mexa em @pedidoxml, use apenas como referencia', trate @pedidoxml como contexto estrutural referencial.\n"
                + "Exemplo 4: Se o usuario escrever 'use @retorno e @service na mesma tarefa', trate @retorno como bloco textual e @service como contexto estrutural, sem confundir seus papeis.\n"

                + secaoExemplosFerramentas

                + "\n=== EXEMPLOS ESTRATEGICOS DE GOVERNANCA DE MUTACAO ===\n"
                + "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"inspecionar_estado_mutacao_workspace\\\",\\\"parameters\\\":{\\\"recentCommitLimit\\\":\\\"5\\\"},\\\"explanation\\\":\\\"Preciso verificar se existe historico suficiente e se ha undo ou redo disponivel antes de restaurar alteracoes.\\\"}\n"
                + "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"listar_historico_mutacoes\\\",\\\"parameters\\\":{\\\"path\\\":\\\"src/main/java/com/exemplo/Servico.java\\\",\\\"limit\\\":\\\"10\\\"},\\\"explanation\\\":\\\"Preciso descobrir quais batches e operacoes afetaram este arquivo antes de decidir restauracao.\\\"}\n"
                + "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"inspecionar_diff_mutacao_workspace\\\",\\\"parameters\\\":{\\\"path\\\":\\\"src/main/java/com/exemplo/Servico.java\\\",\\\"mode\\\":\\\"before_after\\\",\\\"maxLines\\\":\\\"80\\\"},\\\"explanation\\\":\\\"Preciso entender o que mudou no arquivo antes de decidir desfazer ou manter a mutacao.\\\"}\n"
                + "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"desfazer_mutacao_workspace\\\",\\\"parameters\\\":{},\\\"explanation\\\":\\\"Preciso desfazer o ultimo batch inteiro apos confirmar que a reversao em lote e a opcao mais segura.\\\"}\n"
                + "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"refazer_mutacao_workspace\\\",\\\"parameters\\\":{},\\\"explanation\\\":\\\"Preciso refazer o ultimo batch desfeito apos confirmar que o redo e desejado.\\\"}\n"
                + "{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"restaurar_arquivo_mutado_workspace\\\",\\\"parameters\\\":{\\\"path\\\":\\\"src/main/java/com/exemplo/Servico.java\\\",\\\"mode\\\":\\\"last_safe\\\"},\\\"explanation\\\":\\\"Preciso restaurar cirurgicamente este arquivo para um estado seguro anterior sem desfazer todo o batch.\\\"}\n"

                + "\n=== EXEMPLO VALIDO DE explicar (DENTRO DA TAG <codigo_final>) ===\n"
                + "{\\\"action\\\":\\\"explicar\\\",\\\"content\\\":\\\"O metodo delega a chamada para o bean localidadeBE e retorna a colecao de municipios.\\\",\\\"explanation\\\":\\\"Resposta final textual sem alteracao de codigo.\\\"}\n"
                + "\n=== EXEMPLO VALIDO DE substituir (DENTRO DA TAG <codigo_final>) ===\n"
                + "{\\\"action\\\":\\\"substituir\\\",\\\"content\\\":\\\"protected void setupEnv(ActionForm form, HttpServletRequest request) throws Exception { ... }\\\",\\\"explanation\\\":\\\"Trecho substituido de forma localizada e sem devolver o arquivo inteiro.\\\"}\n"
                + "\n=== EXEMPLO VALIDO DE responder_ao_usuario ===\n"
                + "{\\\"action\\\":\\\"responder_ao_usuario\\\",\\\"content\\\":\\\"Foi possivel confirmar parte do fluxo e parte das chamadas indiretas. O metodo atual delega para o bean de negocio e, ate onde foi confirmado, a consulta principal passa pelo DAO correspondente. Nao foi possivel fechar toda a trilha indireta restante dentro do limite de ciclos, entao pontos nao vistos diretamente devem ser tratados como parciais.\\\",\\\"explanation\\\":\\\"Resposta final parcial e positiva apos limite de investigacao.\\\"}\n"
                + "\n=== REGRAS DE QUALIDADE PARA O CAMPO content ===\n"
                + "Se action for substituir, inserir_abaixo ou anexar_acima, content deve conter apenas o trecho aplicavel em codigo puro, sem markdown, sem rotulos ANTES/DEPOIS e sem explicacao adicional.\n"
                + "Se action for explicar, content deve conter apenas a explicacao final.\n"
                + "Se action for comentar, content deve conter apenas comentario curto e explicativo, sem copiar o metodo inteiro, sem assinatura, sem @Override e sem chaves completas.\n"
                + "Se action for executar_ferramenta, use tool e parameters no nivel principal do JSON e nao use content para transportar a ferramenta.\n"
                + "Nunca misture explicacao com codigo no mesmo content quando a acao for substituir.\n"
                + "Se action for consultar ou registrar memoria de projeto, mantenha o conteudo curto, estrutural e reutilizavel.\n"
                + "\n=== EXEMPLO VALIDO DE comentar (DENTRO DA TAG <codigo_final>) ===\n"
                + "{\\\"action\\\":\\\"comentar\\\",\\\"content\\\":\\\"Prepara a lista de municipios disponiveis para a tela, considerando o perfil do usuario logado.\\\",\\\"explanation\\\":\\\"Comentario curto e explicativo sem duplicar o metodo.\\\"}\n"

                + "\n=== REGRAS DE MEMORIA PERSISTENTE ===\n"
                + "A memoria persistente do projeto existe para guardar apenas conhecimento estrutural, estavel e reutilizavel.\n"
                + "Registre apenas informacoes como modulos Maven, groupId, versao percebida de Java, frameworks detectados, presenca de hbm.xml, uso de Lombok, convencoes de pacotes e localizacao tipica de DAOs, Services e Beans.\n"
                + "Nao registre trechos grandes de codigo, conteudo integral de arquivos, selecoes do editor ou respostas completas de ferramentas.\n"
                + "Considere que o projeto pode trocar de branch. Por isso, prefira observacoes genericas e duraveis.\n"
                + "Se houver mudanca de branch ou indicio de divergencia estrutural, reconfirme apenas os pontos sensiveis.\n"

                + "\n=== PRIORIDADE DE INVESTIGACAO ===\n"
                + "Quando faltar contexto, siga esta ordem preferencial:\n"
                + "1. consultar_memoria_projeto\n"
                + "2. verificar_raiz_projeto ou gerar_contexto_enraizado\n"
                + "3. inspecionar_dependencias_projeto\n"
                + "4. resumir_impacto_alteracao\n"
                + "5. buscar_chamadores_jdt, buscar_callees_jdt ou inspecionar_override_metodo\n"
                + "6. inspecionar_efeitos_colaterais ou extrair_queries_trecho\n"
                + "7. buscar_implementacoes_tipo ou buscar_herdeiros_superclasse\n"
                + "8. buscar_contexto_jdt ou buscar_texto_projeto\n"
                + "9. explorar_diretorio\n"
                + "10. leitura_cirurgica_jdt\n"
                + "11. ler_conteudo_arquivo\n"
                + "12. descompilar_classe_corporativa\n"
                + "13. se a duvida envolver alteracoes ja aplicadas no workspace, use nesta ordem: inspecionar_estado_mutacao_workspace, listar_historico_mutacoes, inspecionar_diff_mutacao_workspace, restaurar_arquivo_mutado_workspace, desfazer_mutacao_workspace, refazer_mutacao_workspace\n"

                + "\n=== COMPORTAMENTO ESPERADO ===\n"
                + "Opere como maquina de estados finitos:\n"
                + "1. AVALIAR: o material visivel, os blocos, os contextos estruturais e a memoria persistente sao suficientes?\n"
                + "2. INVESTIGAR: se houver ponto cego relevante, escolha UMA ferramenta adequada.\n"
                + "3. ASSIMILAR: reflita sobre os dados recebidos antes de abrir novas frentes.\n"
                + "4. EXECUTAR: apenas quando o campo de visao estiver claro, dispare a acao final.\n"

                + "\n=== REGRA DE CONTINGENCIA AO ESGOTAR CICLOS ===\n"
                + "Se o limite de ciclos estiver proximo ou for atingido, e proibido encerrar sem entregar valor.\n"
                + "Nessa situacao, responda com action = responder_ao_usuario ou action = explicar.\n"
                + "Explique o que foi confirmado, o que nao foi confirmado, o que nao pode ser analisado com seguranca e qual seria o proximo passo ideal.\n"
                + "Nao use executar_ferramenta nessa etapa final de contingencia.\n"

                + "\n=== CONTINUIDADE DE SESSAO E HISTORICO ===\n"
                + "Voce possui uma memoria recente chamada HISTORICO DESTA SESSAO. Use-a como contexto absoluto da interacao atual.\n"
                + "1. Nao reinicie a investigacao do zero se os dados ja constarem no historico.\n"
                + "2. Nao repita perguntas ou analises ja concluidas.\n"
                + "3. Se houver indicacao de [PERTO_DA_SOLUCAO], priorize concluir com seguranca.\n"

                + "\n=== OUTRAS REGRAS ===\n"
                + "Antes de repetir buscas amplas, consulte a memoria persistente se ela estiver disponivel.\n"
                + "Quando encontrar um padrao estrutural estavel, registre esse conhecimento de forma curta e reutilizavel.\n"
                + "Nao invente classes, metodos, bibliotecas ou arquivos que nao estejam no material atual, no retorno das ferramentas ou nos contextos nomeados da sessao.\n"
                + "Quando existirem blocos nomeados ou contextos estruturais nomeados ativos, considere esses nomes como o vocabulario oficial da sessao.\n"
                + "Os aliases no formato @nome fazem parte desse vocabulario oficial e devem ser tratados como ponteiros exatos para os respectivos alvos.\n"

                + "\n=== ENTRADAS TATICAS ATUAIS ===\n"
                + "<contexto_leitura_arquivo_completo>\n"
                + textoArquivoCompletoSeguro + "\n"
                + "</contexto_leitura_arquivo_completo>\n"
                + "\n<trecho_especifico_selecionado>\n"
                + textoSelecionadoSeguro + "\n"
                + "</trecho_especifico_selecionado>\n"
                + "\n<instrucao_operacional>\n"
                + instrucaoSegura + "\n"
                + "</instrucao_operacional>\n"
                + "\nDecida entre executar_ferramenta, substituir, comentar, explicar, inserir_abaixo, anexar_acima ou responder_ao_usuario. Quando o usuario mencionar aliases no formato @nome, trate esses aliases como referencias exatas aos blocos ou contextos nomeados da sessao. Lembre-se: sua saida integral deve estar contida nas tags <thinking>, <racional> e <codigo_final>.";

        String promptEscapado = safeString(promptEngenharia);

        String jsonPayload = "{"
                + "\"jsonrpc\": \"2.0\","
                + "\"method\": \"tools/call\","
                + "\"params\": {"
                + " \"name\": \"DesenvolvimentoLivreGpt5\","
                + " \"arguments\": {"
                + " \"input\": \"Prompt: " + promptEscapado + "\""
                + " }"
                + "},"
                + "\"id\": 1"
                + "}";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("X-Api-Key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }
    public static String callSugestaoNomeBloco(String selectedCode, String kind, String existingNames, String apiKey) throws Exception {

        String safeText = safeString(selectedCode);
        String safeKind = safeString(kind != null ? kind : "");
        String safeExistingNames = safeString(existingNames != null ? existingNames : "");

        String promptNomeBloco =  "Voce deve gerar um nome curto para um alvo de contexto selecionado.\\n"
                + "REGRAS OBRIGATORIAS:\\n"
                + "1. Responda com UMA unica palavra.\\n"
                + "2. Use apenas letras minusculas e numeros.\\n"
                + "3. Nao use acentos.\\n"
                + "4. Nao use espacos.\\n"
                + "5. Nao use underline, hifen, pontuacao ou simbolos.\\n"
                + "6. O nome deve ter no maximo 12 caracteres.\\n"
                + "7. O nome deve representar a funcao principal do alvo selecionado.\\n"
                + "8. Prefira nomes intuitivos e concretos como validacao, retorno, query, dao, criteria, montagem, atributo, pedido, municipio, usuario, service, repository, config, sql, xml.\\n"	
                + "9. Se o trecho parecer consulta SQL/HQL/JPQL/Hibernate/JPA, prefira query, sql, hql, criteria ou jdbc conforme o caso.\\n"
                + "10. Se o trecho parecer validacao, prefira validacao.\\n"
                + "11. Se o trecho parecer retorno, prefira retorno.\\n"
                + "12. Se o nome colidir com nomes ja existentes, escolha outro nome curto e diferente.\\n"
                + "13. Responda APENAS com o nome final, sem JSON, sem explicacao, sem aspas e sem texto adicional.\\n"
                + "\\nTIPO DO BLOCO: " + safeKind + "\\n"
                + "NOMES JA EXISTENTES: " + safeExistingNames + "\\n"
                + "\\nTRECHO SELECIONADO:\\n"
                + safeText + "\\n";

        String jsonPayload = "{"
                + "\"jsonrpc\": \"2.0\","
                + "\"method\": \"tools/call\","
                + "\"params\": {"
                + " \"name\": \"DesenvolvimentoLivreGpt5\","
                + " \"arguments\": {"
                + " \"input\": \"Prompt: " + promptNomeBloco + "\""
                + " }"
                + "},"
                + "\"id\": 1"
                + "}";

        jsonPayload = jsonPayload.replace("\n", "\\n").replace("\r", "").replace("\t", "\\t");

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("X-Api-Key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
    public static String extractSuggestedBlockName(String rawResponse) {
        if (rawResponse == null || rawResponse.trim().length() == 0) {
            return "";
        }

        String text = rawResponse.trim();

        try {
            com.google.gson.JsonObject envelope = com.google.gson.JsonParser.parseString(text).getAsJsonObject();
            if (envelope.has("result") && envelope.get("result").isJsonObject()) {
                com.google.gson.JsonObject result = envelope.getAsJsonObject("result");
                if (result.has("content") && result.get("content").isJsonArray()) {
                    com.google.gson.JsonArray contentArray = result.getAsJsonArray("content");
                    if (contentArray.size() > 0 && contentArray.get(0).isJsonObject()) {
                        com.google.gson.JsonObject firstContent = contentArray.get(0).getAsJsonObject();
                        if (firstContent.has("text") && !firstContent.get("text").isJsonNull()) {
                            text = firstContent.get("text").getAsString();
                        }
                    }
                }
            }
        } catch (Exception e) {
        }

        text = text.trim();

        if (text.contains("\n")) {
            text = text.substring(0, text.indexOf('\n')).trim();
        }

        text = text.replace("\"", "").replace("'", "").trim();
        text = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD);
        text = text.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        text = text.toLowerCase();
        text = text.replaceAll("[^a-z0-9]", "");

        if (text.length() > 12) {
            text = text.substring(0, 12);
        }

        return text;
    }
	public static String safeString(String entrada ) {
		return entrada
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", "\\t");
	}
}