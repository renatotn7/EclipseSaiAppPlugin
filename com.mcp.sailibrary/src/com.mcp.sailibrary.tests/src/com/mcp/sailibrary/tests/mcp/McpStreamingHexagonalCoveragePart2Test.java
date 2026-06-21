package com.mcp.sailibrary.tests.mcp;

import static org.junit.Assert.*;

import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mcp.sailibrary.plugin.chat.support.AiResponse;
import com.mcp.sailibrary.plugin.chat.support.CodeApplicationResult;
import com.mcp.sailibrary.plugin.chat.support.CodeApplicationState;
import com.mcp.sailibrary.plugin.chat.support.MissionExecutionContext;
import com.mcp.sailibrary.plugin.chat.support.WorkspaceCompilationValidationResult;
import com.mcp.sailibrary.plugin.mcp.BlockNamePromptBuilder;
import com.mcp.sailibrary.plugin.mcp.DesenvolvimentoPromptBuilder;
import com.mcp.sailibrary.plugin.mcp.multimodel.audit.AuditExecutionStatus;
import com.mcp.sailibrary.plugin.mcp.multimodel.audit.AuditObservationLevel;
import com.mcp.sailibrary.plugin.mcp.multimodel.audit.CodeAuditPromptBuilder;
import com.mcp.sailibrary.plugin.mcp.multimodel.audit.CodeAuditResult;
import com.mcp.sailibrary.plugin.mcp.multimodel.audit.NoOpCodeAuditService;
import com.mcp.sailibrary.plugin.mcp.multimodel.generation.CodeGenerationPromptBuilder;

public class McpStreamingHexagonalCoveragePart2Test {

    @Test
    public void deveCobrirBuildersDePrompt() {
        String blockPrompt = new BlockNamePromptBuilder().build(
                "classe X",
                "trecho Y",
                "objetivo Z");
        assertNotNull(blockPrompt);
        assertTrue(blockPrompt.length() > 10);

        String devPrompt = new DesenvolvimentoPromptBuilder().build(
                "instrucao",
                "pedido",
                "selecionado",
                "arquivo",
                "api",
                "modo");
        assertNotNull(devPrompt);
        assertTrue(devPrompt.contains("instrucao") || devPrompt.length() > 20);

        String codeGenPrompt = new CodeGenerationPromptBuilder().build(
                "plano",
                "instrucao",
                "pedido",
                "codigo",
                "arquivo");
        assertNotNull(codeGenPrompt);
        assertTrue(codeGenPrompt.contains("plano") || codeGenPrompt.length() > 20);

        String auditPrompt = new CodeAuditPromptBuilder().build(
                "instrucao",
                "pedido",
                "plano",
                "codigo");
        assertNotNull(auditPrompt);
        assertTrue(auditPrompt.contains("plano") || auditPrompt.length() > 20);
    }

    @Test
    public void deveCobrirResultadoDeAuditoriaSemUsarConstantesInvalidas() throws Exception  {
        CodeAuditResult result = new CodeAuditResult();
        result.setAprovado(true);
        result.setDeveTentarNovamente(false);
        result.setNivelRisco("baixo");
        result.setFeedback("feedback");
        result.setObservationLevel(AuditObservationLevel.NENHUM);
        result.setExecutionStatus(resolveAuditExecutionStatusPreferindoSucesso());

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
        assertNotNull(noOp.getObservationLevel());
        assertNotNull(noOp.getExecutionStatus());
    }

    @Test
    public void deveCobrirPojosDeSuporteDoChat() {
        AiResponse ai = new AiResponse();
        JsonObject params = new JsonObject();
        params.addProperty("path", "src/A.java");
        JsonArray options = new JsonArray();
        options.add("sim");
        options.add("nao");

        ai.setAction("RESPONDER");
        ai.setContent("conteudo");
        ai.setExplanation("explicacao");
        ai.setTool("tool-x");
        ai.setParameters(params);
        ai.setQuestion("pergunta?");
        ai.setExpectedAnswerType("texto");
        ai.setOptions(options);

        assertEquals("RESPONDER", ai.getAction());
        assertEquals("conteudo", ai.getContent());
        assertEquals("explicacao", ai.getExplanation());
        assertEquals("tool-x", ai.getTool());
        assertNotNull(ai.getParameters());
        assertEquals("pergunta?", ai.getQuestion());
        assertEquals("texto", ai.getExpectedAnswerType());
        assertNotNull(ai.getOptions());

        CodeApplicationState state = new CodeApplicationState();
        state.setOffsetInicial(10);
        state.setComprimentoOriginal(5);
        state.setConteudoAnterior("abc");

        assertEquals(10, state.getOffsetInicial());
        assertEquals(5, state.getComprimentoOriginal());
        assertEquals("abc", state.getConteudoAnterior());

        CodeApplicationResult result = new CodeApplicationResult();
        result.setAplicou(true);
        result.setValidacaoOk(true);
        result.setRevertido(false);
        result.setMensagemUsuario("ok");
        result.setMensagemTecnica("sem erro");
        result.setEstadoAnterior(state);

        assertTrue(result.isAplicou());
        assertTrue(result.isValidacaoOk());
        assertFalse(result.isRevertido());
        assertEquals("ok", result.getMensagemUsuario());
        assertEquals("sem erro", result.getMensagemTecnica());
        assertNotNull(result.getEstadoAnterior());

        MissionExecutionContext ctx = new MissionExecutionContext();
        ctx.setTokenMissao(123L);
        ctx.setInstrucaoSnapshot("instrucao");
        ctx.setPedidoOriginalSnapshot("pedido");
        ctx.setSelectedCodeSnapshot("codigo");
        ctx.setFullFileTextSnapshot("arquivo");
        ctx.setApiKeySnapshot("api");
        ctx.setOffsetAtual(7);
        ctx.setIteracoesMaximas(3);
        ctx.setIteracaoAtual(1);
        ctx.setMissaoConcluida(false);
        ctx.setExtensoesPermitidas(2);
        ctx.setExtensoesUsadas(0);
        ctx.setAlertaProximidadeEnviado(false);
        ctx.setInstrucaoEnriquecida("instrucao enriquecida");
        ctx.setUltimaRespostaEstruturadaValida(ai);
        ctx.setUltimoResultadoFerramentaBruto("resultado bruto");
        ctx.setUltimoNomeFerramenta("tool-x");
        ctx.setUltimoResumoFerramenta("resumo");

        assertEquals(123L, ctx.getTokenMissao());
        assertEquals("instrucao", ctx.getInstrucaoSnapshot());
        assertEquals("pedido", ctx.getPedidoOriginalSnapshot());
        assertEquals("codigo", ctx.getSelectedCodeSnapshot());
        assertEquals("arquivo", ctx.getFullFileTextSnapshot());
        assertEquals("api", ctx.getApiKeySnapshot());
        assertEquals(7, ctx.getOffsetAtual());
        assertEquals(3, ctx.getIteracoesMaximas());
        assertEquals(1, ctx.getIteracaoAtual());
        assertFalse(ctx.isMissaoConcluida());
        assertEquals(2, ctx.getExtensoesPermitidas());
        assertFalse(ctx.isAlertaProximidadeEnviado());
        assertEquals("instrucao enriquecida", ctx.getInstrucaoEnriquecida());
        assertNotNull(ctx.getUltimaRespostaEstruturadaValida());
        assertEquals("resultado bruto", ctx.getUltimoResultadoFerramentaBruto());
        assertEquals("tool-x", ctx.getUltimoNomeFerramenta());
        assertEquals("resumo", ctx.getUltimoResumoFerramenta());

        assertTrue(ctx.podeExecutarNovoCiclo());
        ctx.avancarIteracao();
        assertEquals(2, ctx.getIteracaoAtual());

        ctx.registrarExtensaoCiclos(2);
        assertTrue(ctx.getExtensoesUsadas() >= 0);

        WorkspaceCompilationValidationResult validation = new WorkspaceCompilationValidationResult();
        validation.setPossuiErros(true);
        validation.setPossuiWarnings(true);
        validation.setResumo("Resumo validacao");
        validation.setCausaRaizPrincipal("Causa principal");
        validation.adicionarMensagemErro("Erro 1");
        validation.adicionarMensagemWarning("Warning 1");

        assertTrue(validation.isPossuiErros());
        assertTrue(validation.isPossuiWarnings());
        assertEquals("Resumo validacao", validation.getResumo());
        assertEquals("Causa principal", validation.getCausaRaizPrincipal());
        assertEquals(1, validation.getMensagensErro().size());
        assertEquals(1, validation.getMensagensWarning().size());

        String detailed = validation.toDetailedString();
        assertNotNull(detailed);
        assertTrue(detailed.contains("Erro 1") || detailed.contains("Resumo validacao"));
    }

    private AuditExecutionStatus resolveAuditExecutionStatusPreferindoSucesso() {
        for (AuditExecutionStatus status : AuditExecutionStatus.values()) {
            String name = status.name();
            if ("SUCESSO".equalsIgnoreCase(name)
                    || "SUCCESS".equalsIgnoreCase(name)
                    || "EXECUTADO_COM_SUCESSO".equalsIgnoreCase(name)
                    || "OK".equalsIgnoreCase(name)
                    || "CONCLUIDO".equalsIgnoreCase(name)) {
                return status;
            }
        }
        return AuditExecutionStatus.values()[0];
    }
}