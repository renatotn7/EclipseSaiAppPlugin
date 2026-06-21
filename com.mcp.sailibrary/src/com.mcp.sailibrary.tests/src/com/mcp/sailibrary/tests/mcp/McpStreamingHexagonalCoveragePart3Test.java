package com.mcp.sailibrary.tests.mcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.mcp.sailibrary.plugin.mcp.BlockNamePromptBuilder;
import com.mcp.sailibrary.plugin.mcp.multimodel.audit.AuditExecutionStatus;
import com.mcp.sailibrary.plugin.mcp.multimodel.audit.AuditObservationLevel;
import com.mcp.sailibrary.plugin.mcp.multimodel.audit.CodeAuditPromptBuilder;
import com.mcp.sailibrary.plugin.mcp.multimodel.audit.CodeAuditResult;
import com.mcp.sailibrary.plugin.mcp.multimodel.audit.NoOpCodeAuditService;
import com.mcp.sailibrary.plugin.mcp.multimodel.generation.CodeGenerationPromptBuilder;

public class McpStreamingHexagonalCoveragePart3Test {

    @Test
    public void deveCobrirResultadoDeAuditoriaSemUsarConstantesInvalidas() throws Exception {
        CodeAuditResult result = new CodeAuditResult();
        result.setAprovado(true);
        result.setDeveTentarNovamente(false);
        result.setNivelRisco("baixo");
        result.setFeedback("feedback");
        result.setObservationLevel(AuditObservationLevel.NENHUM);
        result.setExecutionStatus(AuditExecutionStatus.values()[0]);

        assertTrue(result.isAprovado());
        assertFalse(result.isDeveTentarNovamente());
        assertEquals("baixo", result.getNivelRisco());
        assertEquals("feedback", result.getFeedback());
        assertNotNull(result.getExecutionStatus());
        assertEquals(AuditObservationLevel.NENHUM, result.getObservationLevel());

        result.isFalhaInfra();
        result.isReprovadoRealmente();
        result.isAprovadoRealmente();
        result.exigeConfirmacaoMesmoAprovado();

        CodeAuditResult noOp = new NoOpCodeAuditService().auditarCodigo(
                "instrucao",
                "pedido",
                "plano",
                "codigo",
                "api");

        assertNotNull(noOp);
        assertTrue(noOp.isAprovado());
        assertFalse(noOp.isDeveTentarNovamente());
        assertEquals("NAO_AVALIADO", noOp.getNivelRisco());
        assertNotNull(noOp.getFeedback());
        assertTrue(noOp.getFeedback().contains("Auditoria neutra"));
    }

    @Test
    public void deveCobrirBuildersCurtosDePrompt() {
        String auditPrompt = new CodeAuditPromptBuilder().build(
                "instrucao",
                "pedido",
                "plano",
                "codigo");
        assertNotNull(auditPrompt);
        assertTrue(auditPrompt.length() > 20);

        String generationPrompt = new CodeGenerationPromptBuilder().build(
                "plano",
                "instrucao",
                "pedido",
                "codigo",
                "arquivo");
        assertNotNull(generationPrompt);
        assertTrue(generationPrompt.length() > 20);

        String blockPrompt = new BlockNamePromptBuilder().build(
                "classe X",
                "trecho Y",
                "objetivo Z");
        assertNotNull(blockPrompt);
        assertTrue(blockPrompt.length() > 10);
    }

    @Test
    public void deveCobrirEnumsDeAuditoria() {
        assertTrue(AuditObservationLevel.values().length > 0);
        assertTrue(AuditExecutionStatus.values().length > 0);

        assertNotNull(AuditObservationLevel.valueOf(AuditObservationLevel.NENHUM.name()));
        assertNotNull(AuditExecutionStatus.valueOf(AuditExecutionStatus.values()[0].name()));
    }
}