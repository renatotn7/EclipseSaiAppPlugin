package com.mcp.sailibrary.tests.mcp.multimodel.generation;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.mcp.sailibrary.plugin.mcp.multimodel.generation.CodeGenerationPromptBuilder;

/** * Testes do builder de prompt de geracao de codigo. * * @author Renato Tomaz Nati */
public class CodeGenerationPromptBuilderTest {

    @Test
    public void deveMontarPromptCompleto() {
        CodeGenerationPromptBuilder builder = new CodeGenerationPromptBuilder();

        String prompt = builder.build(
                "pedido original",
                "plano tecnico",
                "trecho selecionado",
                "arquivo completo",
                "substituir"
        );

        assertTrue(prompt.contains("=== ACAO FINAL ESPERADA ==="));
        assertTrue(prompt.contains("substituir"));
        assertTrue(prompt.contains("=== PEDIDO ORIGINAL DO USUARIO ==="));
        assertTrue(prompt.contains("pedido original"));
        assertTrue(prompt.contains("=== PLANO DE IMPLEMENTACAO RECEBIDO ==="));
        assertTrue(prompt.contains("plano tecnico"));
        assertTrue(prompt.contains("=== TRECHO ATUAL SELECIONADO ==="));
        assertTrue(prompt.contains("trecho selecionado"));
        assertTrue(prompt.contains("=== ARQUIVO ATUAL COMPLETO ==="));
        assertTrue(prompt.contains("arquivo completo"));
        assertTrue(prompt.contains("Responda somente em JSON valido."));
    }

    @Test
    public void deveUsarMarcadoresQuandoNaoHaTrechoOuArquivo() {
        CodeGenerationPromptBuilder builder = new CodeGenerationPromptBuilder();

        String prompt = builder.build(
                null,
                null,
                "",
                "",
                null
        );

        assertTrue(prompt.contains("[SEM_TRECHO_SELECIONADO]"));
        assertTrue(prompt.contains("[SEM_ARQUIVO_TEXTUAL_ATIVO]"));
    }
}