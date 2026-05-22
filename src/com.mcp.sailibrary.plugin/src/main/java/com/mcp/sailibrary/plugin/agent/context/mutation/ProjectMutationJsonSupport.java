package com.mcp.sailibrary.plugin.agent.context.mutation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** * Centraliza leitura e escrita defensiva dos arquivos JSON da camada de * mutacao persistente. * * <p>Esta classe deve ser usada como apoio tecnico para repositories e stores * da infraestrutura de mutacao. Sua responsabilidade e apenas persistencia * segura de JSON, sem carregar regras de negocio de undo, redo, batch ou * versionamento.</p> * * <p>Quando um arquivo nao existir, estiver vazio ou apresentar falha de * leitura, a classe devolve estruturas vazias seguras para evitar propagacao * de falhas desnecessarias no fluxo da aplicacao.</p> * * @author Renato Tomaz Nati * @since 2026-05-20 */
public class ProjectMutationJsonSupport {

    private final Gson gson;

    /** * Inicializa o serializador JSON com formatacao legivel. * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public ProjectMutationJsonSupport() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /** * Le um arquivo JSON e devolve um objeto JSON valido. * * <p>Se o arquivo nao existir, estiver vazio, nao for arquivo regular ou * ocorrer qualquer falha de leitura ou parse, o retorno sera um * {@link JsonObject} vazio.</p> * * @param arquivo arquivo JSON a ser lido * @return objeto JSON lido ou objeto vazio em fallback seguro * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public JsonObject lerJsonObject(File arquivo) {
        JsonElement element = lerJsonElement(arquivo);
        if (element != null && element.isJsonObject()) {
            return element.getAsJsonObject();
        }
        return new JsonObject();
    }

    /** * Le um arquivo JSON e devolve um array JSON valido. * * <p>Se o arquivo nao existir, estiver vazio, nao for arquivo regular ou * ocorrer qualquer falha de leitura ou parse, o retorno sera um * {@link JsonArray} vazio.</p> * * @param arquivo arquivo JSON a ser lido * @return array JSON lido ou array vazio em fallback seguro * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public JsonArray lerJsonArray(File arquivo) {
        JsonElement element = lerJsonElement(arquivo);
        if (element != null && element.isJsonArray()) {
            return element.getAsJsonArray();
        }
        return new JsonArray();
    }

    /** * Le um arquivo JSON e devolve o elemento raiz correspondente. * * <p>Este metodo e util quando o chamador ainda nao sabe se o conteudo * esperado e objeto ou array. Em caso de falha, o retorno sera um * {@link JsonObject} vazio como fallback defensivo.</p> * * @param arquivo arquivo JSON a ser lido * @return elemento JSON lido ou objeto vazio em fallback seguro * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public JsonElement lerJsonElement(File arquivo) {
        if (arquivo == null || !arquivo.exists() || !arquivo.isFile()) {
            return new JsonObject();
        }

        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(arquivo));
            StringBuilder builder = new StringBuilder();
            String linha;

            while ((linha = bufferedReader.readLine()) != null) {
                builder.append(linha).append("\n");
            }

            String conteudo = builder.toString().trim();
            if (conteudo.length() == 0) {
                return new JsonObject();
            }

            return JsonParser.parseString(conteudo);
        } catch (Exception e) {
            return new JsonObject();
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (Exception e) {
                }
            }
        }
    }

    /** * Persiste um objeto JSON em disco, criando previamente a arvore de * diretorios quando necessario. * * @param arquivo arquivo de destino * @param conteudo conteudo a ser persistido * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void gravarJsonObject(File arquivo, JsonObject conteudo) {
        if (conteudo == null) {
            conteudo = new JsonObject();
        }
        gravarJsonElement(arquivo, conteudo);
    }

    /** * Persiste um array JSON em disco, criando previamente a arvore de * diretorios quando necessario. * * @param arquivo arquivo de destino * @param conteudo conteudo a ser persistido * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void gravarJsonArray(File arquivo, JsonArray conteudo) {
        if (conteudo == null) {
            conteudo = new JsonArray();
        }
        gravarJsonElement(arquivo, conteudo);
    }

    /** * Persiste um elemento JSON generico em disco, criando previamente a arvore * de diretorios quando necessario. * * <p>O metodo ignora chamadas com arquivo nulo. Quando o conteudo for nulo, * sera persistido um objeto JSON vazio para manter consistencia de leitura * posterior.</p> * * @param arquivo arquivo de destino * @param conteudo elemento JSON a ser persistido * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void gravarJsonElement(File arquivo, JsonElement conteudo) {
        if (arquivo == null) {
            return;
        }

        if (conteudo == null) {
            conteudo = new JsonObject();
        }

        FileWriter fileWriter = null;
        try {
            File diretorioPai = arquivo.getParentFile();
            if (diretorioPai != null && !diretorioPai.exists()) {
                diretorioPai.mkdirs();
            }

            fileWriter = new FileWriter(arquivo);
            fileWriter.write(gson.toJson(conteudo));
            fileWriter.flush();
        } catch (Exception e) {
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (Exception e) {
                }
            }
        }
    }

    /** * Garante a existencia de um objeto JSON raiz em disco quando o arquivo * ainda nao existir. * * <p>Se o arquivo ja existir, nenhum conteudo sera sobrescrito.</p> * * @param arquivo arquivo alvo * @param conteudoInicial conteudo inicial a ser gravado se necessario * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void criarJsonObjectSeAusente(File arquivo, JsonObject conteudoInicial) {
        if (arquivo == null || arquivo.exists()) {
            return;
        }

        if (conteudoInicial == null) {
            conteudoInicial = new JsonObject();
        }

        gravarJsonObject(arquivo, conteudoInicial);
    }

    /** * Garante a existencia de um array JSON raiz em disco quando o arquivo * ainda nao existir. * * <p>Se o arquivo ja existir, nenhum conteudo sera sobrescrito.</p> * * @param arquivo arquivo alvo * @param conteudoInicial conteudo inicial a ser gravado se necessario * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public void criarJsonArraySeAusente(File arquivo, JsonArray conteudoInicial) {
        if (arquivo == null || arquivo.exists()) {
            return;
        }

        if (conteudoInicial == null) {
            conteudoInicial = new JsonArray();
        }

        gravarJsonArray(arquivo, conteudoInicial);
    }

    /** * Retorna true quando o arquivo informado existe, e um arquivo regular e * possui conteudo textual nao vazio. * * @param arquivo arquivo a ser validado * @return true quando o arquivo possui conteudo JSON textual nao vazio * * @author Renato Tomaz Nati * @since 2026-05-20 */
    public boolean possuiConteudo(File arquivo) {
        if (arquivo == null || !arquivo.exists() || !arquivo.isFile()) {
            return false;
        }

        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(arquivo));
            String linha;
            while ((linha = bufferedReader.readLine()) != null) {
                if (linha.trim().length() > 0) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (Exception e) {
                }
            }
        }

        return false;
    }
}