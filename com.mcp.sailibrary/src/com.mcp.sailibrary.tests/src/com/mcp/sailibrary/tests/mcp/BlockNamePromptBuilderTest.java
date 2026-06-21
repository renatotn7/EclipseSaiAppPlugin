package com.mcp.sailibrary.tests.mcp;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.mcp.sailibrary.plugin.mcp.BlockNamePromptBuilder;

public class BlockNamePromptBuilderTest {

    @Test
    public void deveMontarPromptComCamposInformados() {
        BlockNamePromptBuilder builder = new BlockNamePromptBuilder();

        String prompt = builder.build("select * from tabela", "SQL", "query,dao");

        assertTrue(prompt.contains("TIPO DO BLOCO: SQL"));
        assertTrue(prompt.contains("NOMES JA EXISTENTES: query,dao"));
        assertTrue(prompt.contains("TRECHO SELECIONADO:"));
        assertTrue(prompt.contains("select * from tabela"));
        assertTrue(prompt.contains("Responda APENAS com o nome final"));
    }

    @Test
    public void deveAceitarNulosSemQuebrar() {
        BlockNamePromptBuilder builder = new BlockNamePromptBuilder();

        String prompt = builder.build(null, null, null);

        assertTrue(prompt.contains("TIPO DO BLOCO: "));
        assertTrue(prompt.contains("NOMES JA EXISTENTES: "));
        assertTrue(prompt.contains("TRECHO SELECIONADO:"));
    }
}