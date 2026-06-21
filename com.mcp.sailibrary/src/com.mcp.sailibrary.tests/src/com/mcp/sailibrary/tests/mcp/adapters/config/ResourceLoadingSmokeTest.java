package com.mcp.sailibrary.tests.mcp.adapters.config;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.junit.Test;

public class ResourceLoadingSmokeTest {

    @Test
    public void deveCarregarPropertiesDeTeste() throws Exception {
        InputStream in = ResourceLoadingSmokeTest.class.getResourceAsStream("/mcp-models-test.properties");

        if (in == null) {
            File arquivo = localizarArquivo("mcp-models-test.properties");
            assertNotNull(
                    "O arquivo mcp-models-test.properties nao foi encontrado nem no classpath nem no filesystem do projeto de testes.",
                    arquivo);
            in = new FileInputStream(arquivo);
        }

        try (InputStream use = in) {
            Properties props = new Properties();
            props.load(use);
            assertFalse(props.isEmpty());
        }
    }
    
    @Test
    public void deveCarregarPropertiesDeTestev2() {
        InputStream in = ResourceLoadingSmokeTest.class.getResourceAsStream("mcp-models-test.properties");
        assertNotNull("O arquivo mcp-models-test.properties nao foi encontrado no classpath do bundle de testes.", in);
    }

    private File localizarArquivo(String nome) {
        File dir = new File(System.getProperty("user.dir"));

        for (int i = 0; i < 8 && dir != null; i++) {
            File[] candidatos = new File[] {
                    new File(dir, nome),
                    new File(dir, "src/" + nome),
                    new File(dir, "resources/" + nome),
                    new File(dir, "com.mcp.sailibrary.tests/" + nome),
                    new File(dir, "com.mcp.sailibrary.tests/src/" + nome),
                    new File(dir, "../com.mcp.sailibrary.tests/" + nome),
                    new File(dir, "../com.mcp.sailibrary.tests/src/" + nome)
            };

            for (File candidato : candidatos) {
                if (candidato.isFile()) {
                    return candidato.getAbsoluteFile();
                }
            }

            dir = dir.getParentFile();
        }

        return null;
    }
}