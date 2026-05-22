package com.mcp.sailibrary.plugin.agent.context.analise;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** * --- * yaml_header: * version: "1.0" * dependencies: * - java.io.File * - java.io.BufferedReader * - java.io.FileWriter * - com.google.gson * purpose: "Centralizar leitura e escrita defensiva dos arquivos JSON da memoria persistente." * design_pattern: "Utility / Gateway" * --- */
public class ProjectMemoryJsonSupport {

    private Gson gson;

    /**
 * Inicializa o serializador JSON com formato legivel.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
    public ProjectMemoryJsonSupport() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
 * Le um JSON de disco com fallback seguro para objeto vazio.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
    public JsonObject lerJson(File arquivo) {
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

            return JsonParser.parseString(conteudo).getAsJsonObject();
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

    /**
 * Persiste um JSON em disco com criacao previa da arvore de diretorios.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-16
 */
    public void gravarJson(File arquivo, JsonObject conteudo) {
        if (arquivo == null || conteudo == null) {
            return;
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
}