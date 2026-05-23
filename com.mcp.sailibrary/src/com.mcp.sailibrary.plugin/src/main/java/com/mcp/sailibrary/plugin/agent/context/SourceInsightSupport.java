package com.mcp.sailibrary.plugin.agent.context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** * Centraliza busca textual segura em Java e XML, priorizando modulo Maven * atual, modulos declarados no pom agregador e respeitando o perimetro do * projeto. * * <p>Esta classe foi reforcada para diferenciar melhor: * <ul> * <li>raiz segura global</li> * <li>projeto Eclipse mais proximo</li> * <li>modulo Maven mais proximo</li> * <li>pom agregador</li> * <li>escopo efetivo de busca</li> * </ul> * </p> * * <p>As assinaturas antigas foram preservadas para evitar regressao. Novos * overloads com {@link ResolvedProjectScope} foram adicionados para permitir * uso mais preciso pelas ferramentas e agentes.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class SourceInsightSupport {

    /** * Extrai valor simples de uma chave em JSON plano. * * @param json texto JSON * @param chave chave procurada * @return valor extraido ou string vazia * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String extrairValorVariavel(String json, String chave) {
        String padrao = "\"" + chave + "\":\"";
        if (json != null && json.contains(padrao)) {
            int inicio = json.indexOf(padrao) + padrao.length();
            int fim = json.indexOf("\"", inicio);
            if (fim > inicio) {
                return json.substring(inicio, fim);
            }
        }
        return "";
    }

    /** * Converte inteiro com fallback seguro. * * @param texto texto de origem * @param valorPadrao fallback * @param valorMaximo teto maximo * @return inteiro convertido * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public int extrairInteiro(String texto, int valorPadrao, int valorMaximo) {
        int valor = valorPadrao;
        try {
            if (texto != null && texto.trim().length() > 0) {
                valor = Integer.parseInt(texto);
            }
        } catch (Exception e) {
            valor = valorPadrao;
        }

        if (valor <= 0) {
            valor = valorPadrao;
        }

        if (valor > valorMaximo) {
            valor = valorMaximo;
        }

        return valor;
    }

    /** * Resolve o ponto inicial da busca a partir da raiz e de um path relativo. * * <p>Por compatibilidade, quando o path solicitado nao existir este metodo * ainda faz fallback para a raiz. Para fluxos que precisem maior rigidez, * prefira os overloads com {@link ResolvedProjectScope}.</p> * * @param rootDirectory raiz segura conhecida * @param requestedPath caminho relativo solicitado * @return ponto inicial resolvido * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File resolverPontoInicial(File rootDirectory, String requestedPath) {
        if (rootDirectory == null) {
            return null;
        }

        if (requestedPath == null || requestedPath.trim().length() == 0) {
            return rootDirectory;
        }

        File pontoInicial = new File(rootDirectory, requestedPath);
        if (!pontoInicial.exists()) {
            return rootDirectory;
        }

        return pontoInicial;
    }

    /** * Resolve o ponto inicial da busca a partir de um escopo ja enriquecido. * * @param scope escopo resolvido * @param requestedPath caminho relativo solicitado * @return ponto inicial resolvido * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File resolverPontoInicial(ResolvedProjectScope scope, String requestedPath) {
        if (scope == null) {
            return null;
        }

        File base = scope.getEffectiveSearchRoot() != null
                ? scope.getEffectiveSearchRoot()
                : scope.getNearestMavenModuleRoot() != null
                ? scope.getNearestMavenModuleRoot()
                : scope.getSafeRoot();

        return resolverPontoInicial(base, requestedPath);
    }

    /** * Resolve um escopo estruturado de projeto a partir de um ponto inicial e * de uma raiz segura conhecida. * * @param pontoInicial ponto inicial de analise * @param rootDirectory raiz segura conhecida * @return escopo resolvido * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public ResolvedProjectScope resolverEscopoProjeto(File pontoInicial, File rootDirectory) {
        ResolvedProjectScope scope = new ResolvedProjectScope();

        File safeRoot = localizarRaizSeguraProjeto(pontoInicial, rootDirectory);
        File nearestProjectRoot = localizarProjectRootMaisProximo(pontoInicial, safeRoot);
        File nearestModuleRoot = localizarModuloMavenMaisProximo(pontoInicial, safeRoot);
        File aggregatorPom = localizarPomAgregador(nearestModuleRoot, safeRoot);

        scope.setSafeRoot(safeRoot);
        scope.setNearestEclipseProjectRoot(nearestProjectRoot);
        scope.setNearestEclipseProjectName(lerNomeProject(nearestProjectRoot));
        scope.setNearestMavenModuleRoot(nearestModuleRoot);
        scope.setAggregatorPom(aggregatorPom);

        String groupIdModulo = "";
        if (nearestModuleRoot != null) {
            File pomModulo = new File(nearestModuleRoot, "pom.xml");
            if (pomModulo.exists()) {
                groupIdModulo = extrairPrimeiraTagNoEscopoProjeto(lerConteudoArquivo(pomModulo), "groupId");
                if (groupIdModulo.length() == 0) {
                    groupIdModulo = extrairPrimeiraTag(lerConteudoArquivo(pomModulo), "groupId");
                }
            }
        }

        String groupIdAgregador = "";
        if (aggregatorPom != null && aggregatorPom.exists()) {
            String conteudoPomAgregador = lerConteudoArquivo(aggregatorPom);
            groupIdAgregador = extrairPrimeiraTagNoEscopoProjeto(conteudoPomAgregador, "groupId");
            if (groupIdAgregador.length() == 0) {
                groupIdAgregador = extrairPrimeiraTag(conteudoPomAgregador, "groupId");
            }
        }

        scope.setGroupIdDoModulo(groupIdModulo);
        scope.setGroupIdDoAgregador(groupIdAgregador);

        File effectiveSearchRoot = nearestModuleRoot != null
                ? nearestModuleRoot
                : nearestProjectRoot != null
                ? nearestProjectRoot
                : safeRoot;

        scope.setEffectiveSearchRoot(effectiveSearchRoot);
        return scope;
    }

    /** * Localiza a raiz segura do projeto. O limite valido e diretorio com .git * ou .project. * * @param pontoInicial ponto inicial * @param rootDirectory raiz conhecida * @return melhor raiz segura encontrada * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File localizarRaizSeguraProjeto(File pontoInicial, File rootDirectory) {
        if (pontoInicial == null && rootDirectory == null) {
            return null;
        }

        File cursor = pontoInicial != null ? pontoInicial : rootDirectory;
        if (cursor != null && cursor.isFile()) {
            cursor = cursor.getParentFile();
        }

        File ultimaPastaValida = null;

        while (cursor != null) {
            if (possuiMarcadorRaizProjeto(cursor)) {
                ultimaPastaValida = cursor;
            }

            if (rootDirectory != null) {
                try {
                    String raizCanonica = rootDirectory.getCanonicalPath();
                    String cursorCanonico = cursor.getCanonicalPath();
                    if (cursorCanonico.equals(raizCanonica)) {
                        break;
                    }
                } catch (Exception e) {
                    break;
                }
            }

            cursor = cursor.getParentFile();
        }

        if (ultimaPastaValida != null) {
            return ultimaPastaValida;
        }

        return rootDirectory;
    }

    /** * Localiza o `.project` mais proximo subindo a partir do ponto inicial, * respeitando o perimetro da raiz segura. * * @param pontoInicial ponto inicial * @param raizSeguraProjeto limite superior de seguranca * @return projeto Eclipse mais proximo ou null * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File localizarProjectRootMaisProximo(File pontoInicial, File raizSeguraProjeto) {
        if (pontoInicial == null) {
            return null;
        }

        File cursor = pontoInicial.isFile() ? pontoInicial.getParentFile() : pontoInicial;

        while (cursor != null) {
            File projectFile = new File(cursor, ".project");
            if (projectFile.exists() && projectFile.isFile()) {
                return cursor;
            }

            if (raizSeguraProjeto != null) {
                try {
                    String raizCanonica = raizSeguraProjeto.getCanonicalPath();
                    String cursorCanonico = cursor.getCanonicalPath();
                    if (cursorCanonico.equals(raizCanonica)) {
                        break;
                    }
                } catch (Exception e) {
                    break;
                }
            }

            cursor = cursor.getParentFile();
        }

        return null;
    }

    /** * Le o nome do projeto Eclipse a partir do conteudo do `.project`. * * @param projectRoot diretorio do projeto Eclipse * @return nome do projeto ou string vazia * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String lerNomeProject(File projectRoot) {
        if (projectRoot == null) {
            return "";
        }

        File projectFile = new File(projectRoot, ".project");
        if (!projectFile.exists() || !projectFile.isFile()) {
            return "";
        }

        String conteudo = lerConteudoArquivo(projectFile);
        if (conteudo == null || conteudo.trim().length() == 0) {
            return "";
        }

        return extrairPrimeiraTag(conteudo, "name");
    }

    /** * Localiza o modulo Maven mais proximo subindo a arvore ate encontrar * `pom.xml`. * * @param pontoInicial ponto inicial * @param rootDirectory limite superior * @return modulo Maven mais proximo * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File localizarModuloMavenMaisProximo(File pontoInicial, File rootDirectory) {
        if (pontoInicial == null) {
            return rootDirectory;
        }

        File cursor = pontoInicial;
        if (cursor.isFile()) {
            cursor = cursor.getParentFile();
        }

        while (cursor != null) {
            File pom = new File(cursor, "pom.xml");
            if (pom.exists() && pom.isFile()) {
                return cursor;
            }

            if (rootDirectory != null) {
                try {
                    String raizCanonica = rootDirectory.getCanonicalPath();
                    String cursorCanonico = cursor.getCanonicalPath();
                    if (cursorCanonico.equals(raizCanonica)) {
                        break;
                    }
                } catch (Exception e) {
                    break;
                }
            }

            cursor = cursor.getParentFile();
        }

        return rootDirectory;
    }

    /** * Localiza o pom agregador mais alto dentro do perimetro seguro. * * @param moduloAtual modulo atual * @param raizSeguraProjeto raiz segura * @return pom agregador mais alto * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public File localizarPomAgregador(File moduloAtual, File raizSeguraProjeto) {
        if (moduloAtual == null) {
            return null;
        }

        File melhorPom = null;
        File cursor = moduloAtual;

        while (cursor != null) {
            File pomAtual = new File(cursor, "pom.xml");
            if (pomAtual.exists() && pomAtual.isFile()) {
                melhorPom = pomAtual;
            }

            if (raizSeguraProjeto != null) {
                try {
                    String raizCanonica = raizSeguraProjeto.getCanonicalPath();
                    String cursorCanonico = cursor.getCanonicalPath();
                    if (cursorCanonico.equals(raizCanonica)) {
                        break;
                    }
                } catch (Exception e) {
                    break;
                }
            }

            cursor = cursor.getParentFile();
        }

        return melhorPom;
    }

    /** * Le os modulos declarados no pom agregador. * * @param pomAgregador pom agregador * @param raizSeguraProjeto raiz segura * @return lista de diretorios de modulos validos * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public List<File> localizarModulosDeclarados(File pomAgregador, File raizSeguraProjeto) {
        List<File> modulos = new ArrayList<File>();

        if (pomAgregador == null || !pomAgregador.exists()) {
            return modulos;
        }

        String conteudoPom = lerConteudoArquivo(pomAgregador);
        if (conteudoPom == null || conteudoPom.trim().length() == 0) {
            return modulos;
        }

        Pattern pattern = Pattern.compile("<module>\\s*([^<]+?)\\s*</module>");
        Matcher matcher = pattern.matcher(conteudoPom);

        while (matcher.find()) {
            String moduloRelativo = matcher.group(1).trim();
            if (moduloRelativo.length() == 0) {
                continue;
            }

            File diretorioModulo = new File(pomAgregador.getParentFile(), moduloRelativo);
            if (!diretorioModulo.exists() || !diretorioModulo.isDirectory()) {
                continue;
            }

            if (!estaDentroDaRaizSegura(diretorioModulo, raizSeguraProjeto)) {
                continue;
            }

            adicionarSeAusente(modulos, diretorioModulo);
        }

        return modulos;
    }

    /** * Coleta arquivos Java e XML priorizando modulo atual, depois modulos do * pom pai e por fim o restante da raiz segura. * * @param rootDirectory raiz segura global * @param moduleDirectory modulo atual * @return lista de arquivos priorizados * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public List<File> coletarArquivosModuloPrimeiro(File rootDirectory, File moduleDirectory) {
        ResolvedProjectScope scope = resolverEscopoProjeto(moduleDirectory, rootDirectory);
        return coletarArquivosModuloPrimeiro(scope);
    }

    /** * Coleta arquivos Java e XML a partir de um escopo resolvido. * * @param scope escopo resolvido * @return lista de arquivos priorizados * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public List<File> coletarArquivosModuloPrimeiro(ResolvedProjectScope scope) {
        List<File> arquivos = new ArrayList<File>();
        List<String> caminhosVisitados = new ArrayList<String>();

        if (scope == null) {
            return arquivos;
        }

        File moduloAtual = scope.getNearestMavenModuleRoot();
        File raizSegura = scope.getSafeRoot();
        File pomAgregador = scope.getAggregatorPom();

        if (moduloAtual != null && moduloAtual.exists() && estaDentroDaRaizSegura(moduloAtual, raizSegura)) {
            coletarArquivosRecursivos(moduloAtual, arquivos, caminhosVisitados, raizSegura);
        }

        List<File> modulosDeclarados = localizarModulosDeclarados(pomAgregador, raizSegura);
        for (int i = 0; i < modulosDeclarados.size(); i++) {
            File moduloDeclarado = modulosDeclarados.get(i);
            if (moduloAtual != null && saoMesmoDiretorio(moduloDeclarado, moduloAtual)) {
                continue;
            }
            coletarArquivosRecursivos(moduloDeclarado, arquivos, caminhosVisitados, raizSegura);
        }

        if (raizSegura != null && raizSegura.exists()) {
            coletarArquivosRecursivos(raizSegura, arquivos, caminhosVisitados, raizSegura);
        }

        return arquivos;
    }

    /** * Faz varredura recursiva ignorando zonas de build e respeitando o limite * fisico do projeto. * * @param pasta pasta atual * @param arquivos lista acumuladora * @param caminhosVisitados deduplicacao de caminhos * @param raizSeguraProjeto raiz segura * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void coletarArquivosRecursivos(File pasta, List<File> arquivos, List<String> caminhosVisitados, File raizSeguraProjeto) {
        if (pasta == null || !pasta.exists()) {
            return;
        }

        if (!estaDentroDaRaizSegura(pasta, raizSeguraProjeto)) {
            return;
        }

        if (pasta.isDirectory()) {
            if (eDiretorioIgnorado(pasta.getName())) {
                return;
            }

            File[] filhos = pasta.listFiles();
            if (filhos == null) {
                return;
            }

            for (int i = 0; i < filhos.length; i++) {
                coletarArquivosRecursivos(filhos[i], arquivos, caminhosVisitados, raizSeguraProjeto);
            }
            return;
        }

        String nome = pasta.getName().toLowerCase();
        if (!nome.endsWith(".java") && !nome.endsWith(".xml")) {
            return;
        }

        try {
            String caminhoCanonico = pasta.getCanonicalPath();
            if (!caminhosVisitados.contains(caminhoCanonico)) {
                caminhosVisitados.add(caminhoCanonico);
                arquivos.add(pasta);
            }
        } catch (Exception e) {
        }
    }

    /** * Define zonas fora do perimetro util de analise. * * @param nomePasta nome da pasta * @return true quando a pasta for ruido tecnico * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean eDiretorioIgnorado(String nomePasta) {
        if (nomePasta == null) {
            return false;
        }

        return "target".equals(nomePasta)
                || ".git".equals(nomePasta)
                || "bin".equals(nomePasta)
                || ".settings".equals(nomePasta)
                || ".metadata".equals(nomePasta);
    }

    /** * Verifica se um diretorio esta dentro da raiz segura do projeto. * * @param alvo diretorio ou arquivo alvo * @param raizSeguraProjeto raiz segura * @return true quando o alvo estiver dentro do perimetro * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean estaDentroDaRaizSegura(File alvo, File raizSeguraProjeto) {
        if (alvo == null) {
            return false;
        }

        if (raizSeguraProjeto == null) {
            return true;
        }

        try {
            String caminhoRaiz = raizSeguraProjeto.getCanonicalPath();
            String caminhoAlvo = alvo.getCanonicalPath();
            return caminhoAlvo.startsWith(caminhoRaiz);
        } catch (Exception e) {
            return false;
        }
    }

    /** * Detecta se o diretorio atual representa uma raiz valida de projeto. * * @param diretorio diretorio candidato * @return true quando houver `.git` ou `.project` * * @author Renato Tomaz Nati * @since 2026-05-20 */
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

    /** * Evita entradas duplicadas de diretorio na lista de modulos. * * @param arquivos lista acumuladora * @param candidato diretorio candidato * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void adicionarSeAusente(List<File> arquivos, File candidato) {
        for (int i = 0; i < arquivos.size(); i++) {
            if (saoMesmoDiretorio(arquivos.get(i), candidato)) {
                return;
            }
        }
        arquivos.add(candidato);
    }

    /** * Compara diretorios de forma segura. * * @param primeiro primeiro diretorio * @param segundo segundo diretorio * @return true quando representarem o mesmo caminho fisico * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private boolean saoMesmoDiretorio(File primeiro, File segundo) {
        if (primeiro == null || segundo == null) {
            return false;
        }

        try {
            return primeiro.getCanonicalPath().equals(segundo.getCanonicalPath());
        } catch (Exception e) {
            return primeiro.getAbsolutePath().equals(segundo.getAbsolutePath());
        }
    }

    /** * Le o conteudo de arquivo texto de forma simples. * * @param arquivo arquivo de origem * @return conteudo textual ou string vazia * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String lerConteudoArquivo(File arquivo) {
        StringBuilder builder = new StringBuilder();
        BufferedReader bufferedReader = null;

        try {
            bufferedReader = new BufferedReader(new FileReader(arquivo));
            String linha;
            while ((linha = bufferedReader.readLine()) != null) {
                builder.append(linha).append("\n");
            }
        } catch (Exception e) {
            return "";
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (Exception e) {
                }
            }
        }

        return builder.toString();
    }

    /** * Detecta anotacoes comuns de Hibernate e JPA. * * @param conteudo conteudo textual * @return lista de marcadores encontrados * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public List<String> detectarMarcadoresHibernate(String conteudo) {
        List<String> marcadores = new ArrayList<String>();

        adicionarSeContiver(conteudo, "@Entity", marcadores);
        adicionarSeContiver(conteudo, "@MappedSuperclass", marcadores);
        adicionarSeContiver(conteudo, "@Embeddable", marcadores);
        adicionarSeContiver(conteudo, "@Inheritance", marcadores);
        adicionarSeContiver(conteudo, "@Table", marcadores);
        adicionarSeContiver(conteudo, "@DiscriminatorColumn", marcadores);
        adicionarSeContiver(conteudo, "@DiscriminatorValue", marcadores);
        adicionarSeContiver(conteudo, "@OneToMany", marcadores);
        adicionarSeContiver(conteudo, "@ManyToOne", marcadores);
        adicionarSeContiver(conteudo, "@OneToOne", marcadores);
        adicionarSeContiver(conteudo, "@ManyToMany", marcadores);

        return marcadores;
    }

    /** * Detecta anotacoes comuns de Lombok. * * @param conteudo conteudo textual * @return lista de marcadores encontrados * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public List<String> detectarMarcadoresLombok(String conteudo) {
        List<String> marcadores = new ArrayList<String>();

        adicionarSeContiver(conteudo, "@Data", marcadores);
        adicionarSeContiver(conteudo, "@Getter", marcadores);
        adicionarSeContiver(conteudo, "@Setter", marcadores);
        adicionarSeContiver(conteudo, "@Builder", marcadores);
        adicionarSeContiver(conteudo, "@Value", marcadores);
        adicionarSeContiver(conteudo, "@AllArgsConstructor", marcadores);
        adicionarSeContiver(conteudo, "@NoArgsConstructor", marcadores);
        adicionarSeContiver(conteudo, "@RequiredArgsConstructor", marcadores);

        return marcadores;
    }

    /** * Verifica se o arquivo XML aparenta ser mapeamento Hibernate. * * @param arquivo arquivo XML * @param conteudo conteudo textual * @return true quando aparentar mapeamento Hibernate * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public boolean eArquivoHibernateXml(File arquivo, String conteudo) {
        if (arquivo == null) {
            return false;
        }

        String nome = arquivo.getName().toLowerCase();
        if (nome.endsWith(".hbm.xml")) {
            return true;
        }

        if (conteudo == null) {
            return false;
        }

        return conteudo.contains("<hibernate-mapping")
                || conteudo.contains("<class ")
                || conteudo.contains("<subclass ")
                || conteudo.contains("<joined-subclass ")
                || conteudo.contains("<union-subclass ");
    }

    /** * Faz match por regex de forma segura no conteudo completo. * * @param conteudo conteudo textual * @param regex regex procurada * @return true quando houver match * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public boolean contemPadrao(String conteudo, String regex) {
        if (conteudo == null || conteudo.trim().length() == 0) {
            return false;
        }

        try {
            return Pattern.compile(regex, Pattern.MULTILINE | Pattern.DOTALL).matcher(conteudo).find();
        } catch (Exception e) {
            return false;
        }
    }

    /** * Monta descricao padrao de arquivo encontrado. * * @param arquivo arquivo encontrado * @return descricao textual * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String descreverArquivo(File arquivo) {
        if (arquivo == null) {
            return "Arquivo desconhecido";
        }

        try {
            return arquivo.getCanonicalPath();
        } catch (Exception e) {
            return arquivo.getAbsolutePath();
        }
    }

    /** * Extrai a primeira tag simples do XML informado. * * @param conteudo texto XML * @param tag nome da tag * @return valor da tag ou string vazia * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String extrairPrimeiraTag(String conteudo, String tag) {
        if (conteudo == null || tag == null || tag.trim().length() == 0) {
            return "";
        }

        Pattern pattern = Pattern.compile("<" + tag + ">\\s*([^<]+?)\\s*</" + tag + ">");
        Matcher matcher = pattern.matcher(conteudo);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return "";
    }

    /** * Extrai a primeira tag em escopo de projeto, ignorando bloco parent * quando necessario. * * @param conteudo texto XML * @param tag nome da tag * @return valor da tag ou string vazia * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String extrairPrimeiraTagNoEscopoProjeto(String conteudo, String tag) {
        if (conteudo == null || tag == null || tag.trim().length() == 0) {
            return "";
        }

        Pattern projectPattern = Pattern.compile("<project[\\s\\S]*?</project>", Pattern.DOTALL);
        Matcher projectMatcher = projectPattern.matcher(conteudo);
        if (!projectMatcher.find()) {
            return "";
        }

        String blocoProjeto = projectMatcher.group(0);
        Pattern parentPattern = Pattern.compile("<parent[\\s\\S]*?</parent>", Pattern.DOTALL);
        blocoProjeto = parentPattern.matcher(blocoProjeto).replaceFirst("");

        Pattern tagPattern = Pattern.compile("<" + tag + ">\\s*([^<]+?)\\s*</" + tag + ">");
        Matcher tagMatcher = tagPattern.matcher(blocoProjeto);
        if (tagMatcher.find()) {
            return tagMatcher.group(1).trim();
        }

        return "";
    }

    /** * Extrai a primeira tag em escopo de parent. * * @param conteudo texto XML * @param tag nome da tag * @return valor da tag ou string vazia * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String extrairPrimeiraTagNoEscopoParent(String conteudo, String tag) {
        if (conteudo == null || tag == null || tag.trim().length() == 0) {
            return "";
        }

        Pattern parentPattern = Pattern.compile("<parent>([\\s\\S]*?)</parent>", Pattern.DOTALL);
        Matcher parentMatcher = parentPattern.matcher(conteudo);
        if (parentMatcher.find()) {
            String blocoParent = parentMatcher.group(1);
            Pattern tagPattern = Pattern.compile("<" + tag + ">\\s*([^<]+?)\\s*</" + tag + ">");
            Matcher tagMatcher = tagPattern.matcher(blocoParent);
            if (tagMatcher.find()) {
                return tagMatcher.group(1).trim();
            }
        }

        return "";
    }

    /** * Extrai valor de propriedade XML simples. * * @param conteudo texto XML * @param nomePropriedade nome da propriedade * @return valor da propriedade ou string vazia * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public String extrairValorPropriedade(String conteudo, String nomePropriedade) {
        if (conteudo == null || nomePropriedade == null || nomePropriedade.trim().length() == 0) {
            return "";
        }

        Pattern pattern = Pattern.compile("<" + Pattern.quote(nomePropriedade) + ">\\s*([^<]+?)\\s*</" + Pattern.quote(nomePropriedade) + ">");
        Matcher matcher = pattern.matcher(conteudo);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return "";
    }

    /** * Adiciona marcador a lista quando o conteudo contiver o texto desejado. * * @param conteudo conteudo base * @param marcador marcador procurado * @param marcadores lista acumuladora * * @author Renato Tomaz Nati * @since 2026-05-20 */
    private void adicionarSeContiver(String conteudo, String marcador, List<String> marcadores) {
        if (conteudo != null && conteudo.contains(marcador)) {
            marcadores.add(marcador);
        }
    }
}