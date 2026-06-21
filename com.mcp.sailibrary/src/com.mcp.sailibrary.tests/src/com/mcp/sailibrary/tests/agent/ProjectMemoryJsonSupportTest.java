package com.mcp.sailibrary.tests.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mcp.sailibrary.plugin.agent.context.analise.ProjectMemoryJsonSupport;

/** * Testes do suporte JSON da memoria persistente. * * @author Renato Tomaz Nati */
public class ProjectMemoryJsonSupportTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void deveGravarELerJsonObject() throws Exception {
        ProjectMemoryJsonSupport support = new ProjectMemoryJsonSupport();
        File file = tempFolder.newFile("memory.json");

        JsonObject object = new JsonObject();
        object.addProperty("x", "1");

        support.gravarJson(file, object);

        JsonObject read = support.lerJson(file);
        assertEquals("1", read.get("x").getAsString());
    }

    @Test
    public void deveRetornarObjetoVazioQuandoArquivoNaoExiste() throws Exception {
        ProjectMemoryJsonSupport support = new ProjectMemoryJsonSupport();
        File file = new File(tempFolder.getRoot(), "nao_existe.json");

        JsonObject read = support.lerJson(file);

        assertTrue(read.entrySet().isEmpty());
    }
}