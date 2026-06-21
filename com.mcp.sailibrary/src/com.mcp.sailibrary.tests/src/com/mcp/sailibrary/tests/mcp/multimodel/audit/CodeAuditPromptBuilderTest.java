package com.mcp.sailibrary.tests.mcp.multimodel.audit;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.mcp.sailibrary.plugin.mcp.multimodel.audit.CodeAuditPromptBuilder;

/** * Testes do builder de prompt de auditoria. * * @author Renato Tomaz Nati */
public class CodeAuditPromptBuilderTest {

    @Test
    public void deveMontarPromptCompletoDeAuditoria() {
        CodeAuditPromptBuilder builder = new CodeAuditPromptBuilder();

        String prompt = builder.build(
                "pedido original",
                "plano consolidado",
                "codigo candidato",
                "substituir"
        );

        assertTrue(prompt.contains("Voce atua como auditor tecnico e analista de riscos."));
        assertTrue(prompt.contains("=== ACAO FINAL ESPERADA ==="));
        assertTrue(prompt.contains("substituir"));
        assertTrue(prompt.contains("=== PEDIDO ORIGINAL DO USUARIO ==="));
        assertTrue(prompt.contains("pedido original"));
        assertTrue(prompt.contains("=== PLANO DE IMPLEMENTACAO ==="));
        assertTrue(prompt.contains("plano consolidado"));
        assertTrue(prompt.contains("=== CODIGO CANDIDATO ==="));
        assertTrue(prompt.contains("codigo candidato"));
        assertTrue(prompt.contains("Campos obrigatorios: approved, shouldRetry, riskLevel, feedback."));
    }
}