package com.mcp.sailibrary.tests.mcp;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.mcp.sailibrary.plugin.mcp.DesenvolvimentoPromptBuilder;

public class DesenvolvimentoPromptBuilderTest {

    @Test
    public void deveMontarPromptTextualCompleto() {
        DesenvolvimentoPromptBuilder builder = new DesenvolvimentoPromptBuilder();

        String prompt = builder.build(
                "MODO_TEXTUAL",
                "trecho selecionado",
                "arquivo completo",
                "instrucao enriquecida",
                "\n=== FERRAMENTAS ===\nfoo\n",
                "\n=== EXEMPLOS ===\nbar\n"
        );

        assertTrue(prompt.contains("=== MODO OPERACIONAL DETECTADO ==="));
        assertTrue(prompt.contains("modo: MODO_TEXTUAL"));
        assertTrue(prompt.contains("trecho selecionado"));
        assertTrue(prompt.contains("arquivo completo"));
        assertTrue(prompt.contains("instrucao enriquecida"));
        assertTrue(prompt.contains("=== FERRAMENTAS ==="));
        assertTrue(prompt.contains("=== EXEMPLOS ==="));
        assertTrue(prompt.contains("<thinking>"));
        assertTrue(prompt.contains("<racional>"));
        assertTrue(prompt.contains("<codigo_final>"));
    }

    @Test
    public void deveMontarPromptEstruturalCompleto() {
        DesenvolvimentoPromptBuilder builder = new DesenvolvimentoPromptBuilder();

        String prompt = builder.build(
                "MODO_ESTRUTURAL",
                null,
                null,
                null,
                null,
                null
        );

        assertTrue(prompt.contains("modo: MODO_ESTRUTURAL"));
        assertTrue(prompt.contains("Nao ha trecho textual selecionado"));
        assertTrue(prompt.contains("=== ENTRADAS TATICAS ATUAIS ==="));
    }

    @Test
    public void deveAceitarNulosSemLancarExcecao() {
        DesenvolvimentoPromptBuilder builder = new DesenvolvimentoPromptBuilder();

        String prompt = builder.build(null, null, null, null, null, null);

        assertTrue(prompt.contains("modo: MODO_TEXTUAL"));
        assertTrue(prompt.contains("=== ENTRADAS TATICAS ATUAIS ==="));
    }
}