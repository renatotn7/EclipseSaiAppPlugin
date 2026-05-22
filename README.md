# SAI App - Chat IA para Eclipse

Um poderoso agente de IA para projetos Java 🚀

Plugin de assistencia de engenharia de software para Eclipse, com foco em:

- analise de codigo orientada por contexto
- investigacao estrutural do projeto
- uso controlado de ferramentas pela IA
- mutacao segura de arquivos
- rastreabilidade de alteracoes
- historico, undo, redo e restauracao cirurgica

---

## 📋 Pré-requisitos

- Eclipse IDE (4.13+)
- Java Development Kit (JDK 11+)
- Eclipse Plugin Development Environment (PDE)
- Maven (opcional, para build)

---

## ⚙️ Instalação

### 1. Clonar o repositório

```bash
git clone https://github.com/renatotn7/EclipseSaiAppPlugin.git
cd EclipseSaiAppPlugin
```

### 2. Importar no Eclipse

- Abra Eclipse IDE
- File → Import → Existing Projects into Workspace
- Selecione a pasta do projeto
- Aguarde a compilação automática

### 3. Executar como Plugin Eclipse

- Clique com o botão direito no projeto
- Run As → Eclipse Application
- Uma nova instância do Eclipse será aberta com o plugin carregado

---

# Visão geral

O SAI App integra uma IA ao Eclipse para ajudar em tarefas como:

- explicar metodos e fluxos
- localizar chamadores e implementacoes
- inspecionar impacto de alteracao
- mapear queries, persistencia e efeitos colaterais
- criar arquivos em contextos permitidos
- alterar arquivos com backup
- restaurar alteracoes anteriores com seguranca

A proposta do plugin nao e "editar qualquer coisa automaticamente", e sim operar com contexto controlado, ferramentas homologadas e regras de seguranca explicitas.

---

# Principios de seguranca

A seguranca e parte central do projeto.

## O plugin foi desenhado para:

- evitar alteracoes cegas em arquivos do projeto
- limitar a IA ao contexto realmente fornecido
- impedir acesso fora da raiz segura do projeto
- proteger mutacoes com politica explicita
- registrar historico das alteracoes feitas
- permitir desfazer, refazer e restaurar estados anteriores
- reduzir risco de regressao por investigacao insuficiente

## O plugin nao deve:

- adivinhar implementacoes sem evidencia
- alterar indiscriminadamente qualquer arquivo do projeto
- sair do perimetro do projeto por path traversal
- apagar arquivos preexistentes sem politica e fluxo seguro
- tratar package editavel como permissao total de alteracao
- registrar memoria persistente com codigo grande ou dados sensiveis desnecessarios

---

# Modelo de contexto

O plugin trabalha com dois tipos principais de contexto.

## 1. Blocos nomeados textuais

Trechos selecionados no editor e registrados com nome.

Exemplos:

- validacao
- query
- retorno
- montagem

Esses blocos servem para:

- foco fino de analise
- edicao localizada
- referencia exata na conversa

## 2. Contextos estruturais nomeados

Arquivos, packages e pastas marcados na sessao.

Esses contextos servem para:

- delimitar escopo arquitetural
- indicar destino de criacao
- restringir area de mutacao
- fornecer referencia estrutural a IA

---

# Conceito de alvo principal

O sistema distingue entre:

- contexto principal
- contextos editaveis
- contextos referenciais

## Regra atual

- um unico `PRIMARY` por vez
- o `PRIMARY` representa o alvo principal da conversa
- o `PRIMARY` pode ser:
  - um bloco textual
  - um arquivo estrutural
  - package e pasta podem ser editaveis ou referenciais, mas nao sao alvo principal operacional de edicao direta

---

# Como usar

## Abrir o chat

Abra a view do plugin no Eclipse.

## Selecionar contexto textual

No editor:

- selecione um trecho de codigo
- use o menu rapido ou a aba de contexto para adicionar como:
  - Principal
  - Editavel
  - Referencial

## Selecionar contexto estrutural

No Project Explorer ou Package Explorer:

- selecione arquivo, package ou pasta
- use o menu contextual
- adicione como:
  - Principal
  - Editavel
  - Referencial

## Conversar com a IA

No campo de comando, voce pode pedir coisas como:

- explique este metodo
- descubra quem chama este metodo
- localize implementacao concreta
- mapeie queries relacionadas
- gere comentario tecnico
- proponha melhoria sem regressao

## Usar aliases de contexto

Quando um bloco ou contexto recebe nome, ele pode ser citado na conversa com `@nome`.

Exemplo:

- use `@service` como destino
- altere `@validacao`
- nao mexa em `@pedidoxml`, use apenas como referencia

---

# Regras importantes de mutacao

## Package ou pasta editavel nao significam permissao total

Marcar uma package ou pasta como editavel nao autoriza alterar qualquer arquivo preexistente dentro dela.

Por padrao, package ou pasta editavel permitem:

- criar novos arquivos
- criar subpackages ou subpastas
- apagar apenas arquivos criados pela propria IA/plugin, quando permitido

## Arquivo preexistente

Um arquivo existente so deve ser alterado quando:

- estiver explicitamente autorizado pelo fluxo de mutacao
- ou a politica de mutacao permitir isso de forma segura

## Backup obrigatorio

Alteracoes em arquivo existente usam backup `.bkp`.

---

# Ferramentas da IA

A IA nao atua livremente no filesystem. Ela usa um conjunto de ferramentas homologadas.

Essas ferramentas cobrem areas como:

- exploracao de diretorio
- leitura de arquivo
- busca textual
- contexto JDT
- chamadores e callees
- override e polimorfismo
- efeitos colaterais
- queries e persistencia
- impacto de alteracao
- criacao e alteracao de arquivos
- politica de mutacao
- historico de mutacoes
- undo, redo e restore

A documentacao detalhada das ferramentas e montada a partir do proprio metadata das tools registradas no plugin.

---

# Seguranca de mutacao e historico

O plugin possui uma infraestrutura de mutacao com rastreabilidade.

## Objetivos

- registrar o que foi alterado
- manter historico interno
- permitir desfazer e refazer
- restaurar arquivo especifico
- inspecionar diff e estado da mutacao

## Operacoes suportadas

- criar arquivo
- alterar arquivo com backup
- apagar arquivo permitido
- criar package/pasta
- desfazer ultimo batch
- refazer ultimo batch
- listar historico
- inspecionar estado da mutacao
- inspecionar diff
- restaurar arquivo especifico

---

# Como a restauracao funciona

A restauracao nao depende do `.git` real do usuario.

O plugin usa uma infraestrutura interna em:

`~/.sai/projects/<projectKey>/`

Ali ficam:

- memoria persistente do projeto
- journal de mutacoes
- estado de undo/redo
- repositorio Git interno do plugin
- espelho incremental dos arquivos mutados

Isso permite:

- desfazer ultimas alteracoes
- refazer alteracoes desfeitas
- restaurar arquivo especifico
- comparar estados before, after e current

sem interferir diretamente no repositorio Git oficial do projeto do usuario.

---

# Estrutura persistente

Exemplo simplificado:

```
~/.sai/
└── projects/
    └── <projectKey>/
        ├── branch_context.json
        ├── dependency_snapshot.json
        ├── discovered_patterns.json
        ├── project_memory.json
        ├── tool_history.json
        ├── mutation_journal.json
        ├── mutation_state.json
        ├── mutation_repo_meta.json
        └── workspace_git/
```

---

# Memoria persistente do projeto

A memoria persistente existe para guardar conhecimento estrutural reutilizavel.

## Exemplos do que pode ser registrado

- groupId
- javaVersion
- buildTool
- frameworks detectados
- modulos Maven
- padroes arquiteturais
- indicios estruturais confirmados

## O que nao deve ser registrado

- arquivos inteiros
- respostas completas gigantes
- selecoes grandes do editor
- dados sensiveis desnecessarios
- codigo detalhado que nao seja conhecimento estrutural estavel

---

# Undo, redo e restore

## Undo

Desfaz o ultimo batch de mutacao.

## Redo

Refaz o ultimo batch desfeito.

## Restore de arquivo

Restaura um arquivo especifico usando historico persistido.

## Diff de mutacao

Permite comparar estados como:

- before x after
- current x before
- current x after

---

# Limites operacionais

Algumas ferramentas trabalham com limites defensivos, por exemplo:

- numero maximo de ocorrencias em busca textual
- numero maximo de linhas em leitura de arquivo
- profundidade maxima de exploracao
- limite de evidencias em analises
- limite de descompilacoes por rodada

Esses limites existem para:

- proteger o contexto da IA
- reduzir custo computacional
- evitar respostas gigantes e pouco uteis
- diminuir risco de comportamento descontrolado

---

# Para usuario final

## Boas praticas

- selecione bem o contexto antes de pedir alteracoes
- prefira usar blocos nomeados e contextos estruturais
- valide impacto antes de editar codigo sensivel
- use historico e diff antes de desfazer em lote
- prefira restore cirurgico quando o problema for localizado

## Quando desconfiar

Se a tarefa envolver:

- muitos arquivos
- heranca complexa
- persistencia
- XML antigo
- chamadas indiretas
- frameworks legados

prefira primeiro pedir:

- impacto de alteracao
- chamadores
- callees
- efeitos colaterais
- queries
- diff e historico, se ja houve mutacao

---

# Para desenvolvedores

Abaixo esta um resumo tecnico da arquitetura.

---

# Arquitetura tecnica

## Camadas principais

### 1. UI

Responsavel por:

- chat
- contexto
- blocos nomeados
- integracao com editor e explorers

### 2. Controller

Responsavel por:

- orquestrar alvo
- montar instrucao
- gerenciar sessao
- acionar IA
- aplicar resposta
- sincronizar highlights e contexto

### 3. Agent tools

Ferramentas homologadas usadas pela IA para investigar e mutar com seguranca.

### 4. Memoria persistente

Infraestrutura para registrar conhecimento estrutural do projeto.

### 5. Mutacao versionada

Infraestrutura para:

- registrar mutacoes
- versionar espelho interno
- undo/redo
- restore de arquivo
- diff de mutacao

---

# Organizacao de packages

Exemplo de organizacao sugerida:

```
com.mcp.sailibrary.plugin.agent
├── AgentTool.java
├── orchestration
├── context
│   ├── analise
│   └── mutation
│       ├── model
│       └── diff
└── tools
    ├── exploration
    ├── jdt
    ├── architecture
    ├── memory
    ├── mutation
    ├── mutation.history
    ├── bytecode
    └── support
```

---

# Contrato de tool

As tools seguem o contrato basico:

```java
public interface AgentTool {
    String getName();
    String execute(String jsonParameters);
}
```

Ferramentas que tambem documentam seu comportamento para o prompt implementam:

`AgentToolPromptMetadataProvider`

Isso permite que a secao de ferramentas do prompt seja montada dinamicamente por loop, reduzindo duplicacao e drift entre implementacao e documentacao.

---

# Metadata de prompt das tools

Cada tool pode expor metadados estruturados como:

- nome homologado
- finalidade resumida
- descricao de atividade
- parametros
- casos de uso recomendados
- guardrails
- exemplo JSON

Esses metadados sao usados para montar:

- ferramentas homologadas
- exemplos validos de `executar_ferramenta`
- recomendacoes de uso

---

# Orquestracao de tools

O `AgentOrchestrator` nao deve crescer indefinidamente com registro manual confuso.

A recomendacao e usar uma factory de registry, por exemplo:

`AgentToolRegistryFactory`

Ela concentra a montagem do arsenal homologado conforme:

- raiz do projeto
- compilation unit atual
- offset atual

---

# Prompt operacional

O prompt principal combina:

- regras globais de engenharia
- regras de seguranca
- regras de mutacao
- aliases de contexto
- formato de saida
- lista de ferramentas homologadas
- exemplos validos
- entradas atuais do editor e da conversa

A secao de ferramentas deve preferencialmente ser montada dinamicamente a partir do metadata das tools.

---

# Diretrizes de desenvolvimento

## Ao criar nova tool

- mantenha responsabilidade unica
- use nome homologado claro
- valide parametros cedo
- preserve perimetro seguro
- limite volume de retorno
- implemente metadata de prompt quando aplicavel

## Ao criar nova mutacao

- nao escreva direto em disco se puder passar pela facade de mutacao
- preserve journal
- preserve integracao com historico
- pense em undo/redo e restore desde o desenho

## Ao evoluir prompt

- evite duplicar descricao de ferramentas em string fixa
- prefira metadata por tool + builder central

---

# Troubleshooting

## O chat nao encontra contexto

- confira se ha bloco principal ou arquivo principal valido
- confira se a sessao foi sincronizada
- confira se o editor/selection atual e valido

## Tool nao retorna nada

- valide parametros obrigatorios
- confira path relativo
- confirme se a raiz segura do projeto esta correta

## Mutacao nao permitida

- consulte a politica de mutacao do contexto
- verifique se o arquivo e preexistente
- confirme se o destino e realmente editavel

## Undo/redo indisponivel

- inspecione o estado da mutacao
- liste o historico
- confirme se ha batches persistidos

---

# Filosofia final

Este plugin nao foi desenhado para dar liberdade irrestrita a uma IA.

Ele foi desenhado para oferecer:

- autonomia controlada
- investigacao verificavel
- mutacao segura
- rastreabilidade
- contexto estruturado
- possibilidade real de recuperacao

A ideia central e simples:

**nao adivinhar, investigar.**
**nao alterar cegamente, alterar com contexto.**
**nao perder controle, manter historico e restauracao.**

---

## 🤝 Contribuindo

Contribuições são bem-vindas! Por favor:

1. Faça um Fork do projeto
2. Crie uma branch para sua feature (`git checkout -b feature/AmazingFeature`)
3. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
4. Push para a branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request

---

## 📄 Licença

Este projeto está disponível sob licença MIT. Consulte o arquivo LICENSE para mais detalhes.

---

## 👤 Autor

**renatotn7** - [@renatotn7](https://github.com/renatotn7)

---

## 💬 Suporte

Para dúvidas, problemas ou sugestões, abra uma issue no repositório:
[Issues do Projeto](https://github.com/renatotn7/EclipseSaiAppPlugin/issues)

---

⭐ Se você achou este projeto útil, considere deixar uma estrela!
