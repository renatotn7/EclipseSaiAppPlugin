package com.mcp.sailibrary.plugin.agent.context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** * --- * yaml_header: * version: "1.1" * dependencies: * - java.io.File * - java.io.BufferedReader * - java.util.List * - java.util.regex.Pattern * purpose: "Centralizar busca textual segura em Java e XML, priorizando modulo Maven atual, modulos declarados no pom agregador e respeitando o perimetro do projeto." * design_pattern: "Helper / Defensive Utility" * --- */
public class SourceInsightSupport {

    /**
 * Extrai valor simples de uma chave em JSON plano.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
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

    /**
 * Converte inteiro com fallback seguro.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
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

    /**
 * Resolve o ponto inicial da busca a partir da raiz e de um path relativo.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
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

    /**
 * Localiza a raiz segura do projeto. O limite valido e diretorio com .git ou .project.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
    public File localizarRaizSeguraProjeto(File pontoInicial, File rootDirectory) {
        if (pontoInicial == null && rootDirectory == null) {
            return null;
        }

        File cursor = pontoInicial != null ? pontoInicial : rootDirectory;
        if (cursor != null && cursor.isFile()) {
            cursor = cursor.getParentFile();
        }

        File ultimaPastаValida = null;

        while (cursor != null) {
            if (possuiMarcadorRaizProjeto(cursor)) {
                ultimaPastаValida = cursor;
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

        if (ultimaPastаValida != null) {
            return ultimaPastаValida;
        }

        return rootDirectory;
    }

    /**
 * Localiza o modulo Maven mais proximo subindo a arvore ate encontrar pom.xml.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
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

    /**
 * Localiza o pom agregador mais alto dentro do perimetro seguro.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
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

    /**
 * Le os modulos declarados no pom agregador.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
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

    /**
 * Coleta arquivos Java e XML priorizando modulo atual, depois modulos do pom pai e por fim o restante da raiz segura.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
    public List<File> coletarArquivosModuloPrimeiro(File rootDirectory, File moduleDirectory) {
        List<File> arquivos = new ArrayList<File>();
        List<String> caminhosVisitados = new ArrayList<String>();

        File raizSeguraProjeto = localizarRaizSeguraProjeto(moduleDirectory, rootDirectory);

        if (moduleDirectory != null && moduleDirectory.exists() && estaDentroDaRaizSegura(moduleDirectory, raizSeguraProjeto)) {
            coletarArquivosRecursivos(moduleDirectory, arquivos, caminhosVisitados, raizSeguraProjeto);
        }

        File pomAgregador = localizarPomAgregador(moduleDirectory, raizSeguraProjeto);
        List<File> modulosDeclarados = localizarModulosDeclarados(pomAgregador, raizSeguraProjeto);

        for (int i = 0; i < modulosDeclarados.size(); i++) {
            File moduloDeclarado = modulosDeclarados.get(i);
            if (moduleDirectory != null && saoMesmoDiretorio(moduloDeclarado, moduleDirectory)) {
                continue;
            }
            coletarArquivosRecursivos(moduloDeclarado, arquivos, caminhosVisitados, raizSeguraProjeto);
        }

        if (raizSeguraProjeto != null && raizSeguraProjeto.exists()) {
            coletarArquivosRecursivos(raizSeguraProjeto, arquivos, caminhosVisitados, raizSeguraProjeto);
        }

        return arquivos;
    }

    /**
 * Faz varredura recursiva ignorando zonas de build e respeitando o limite fisico do projeto.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
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

    /**
 * Define zonas fora do perimetro util de analise.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
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

    /**
 * Verifica se um diretorio esta dentro da raiz segura do projeto.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
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

    /**
 * Detecta se o diretorio atual representa a raiz do projeto.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
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

    /**
 * Evita entradas duplicadas de diretorio na lista de modulos.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
    private void adicionarSeAusente(List<File> arquivos, File candidato) {
        for (int i = 0; i < arquivos.size(); i++) {
            if (saoMesmoDiretorio(arquivos.get(i), candidato)) {
                return;
            }
        }
        arquivos.add(candidato);
    }

    /**
 * Compara diretorios de forma segura.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
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

    /**
 * Le o conteudo de arquivo texto de forma simples.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
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

    /**
 * Detecta anotacoes comuns de Hibernate e JPA.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
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

    /**
 * Detecta anotacoes comuns de Lombok para alertar sobre metodos gerados.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
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

    /**
 * Verifica se o arquivo XML aparenta ser mapeamento Hibernate.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
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

    /**
 * Faz match por regex de forma segura no conteudo completo.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
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

    /**
 * Monta descricao padrao de arquivo encontrado.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
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

    private void adicionarSeContiver(String conteudo, String marcador, List<String> marcadores) {
        if (conteudo != null && conteudo.contains(marcador)) {
            marcadores.add(marcador);
        }
    }
}