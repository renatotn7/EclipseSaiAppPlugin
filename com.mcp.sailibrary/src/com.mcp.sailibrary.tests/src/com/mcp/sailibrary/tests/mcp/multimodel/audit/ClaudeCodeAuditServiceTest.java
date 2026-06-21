package com.mcp.sailibrary.tests.mcp.multimodel.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.mcp.sailibrary.plugin.mcp.McpResponseExtractor;
import com.mcp.sailibrary.plugin.mcp.core.ModelChannel;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionResponse;
import com.mcp.sailibrary.plugin.mcp.core.RequestFormatKind;
import com.mcp.sailibrary.plugin.mcp.core.ResponseFormatKind;
import com.mcp.sailibrary.plugin.mcp.core.TransportKind;
import com.mcp.sailibrary.plugin.mcp.multimodel.audit.AuditExecutionStatus;
import com.mcp.sailibrary.plugin.mcp.multimodel.audit.ClaudeCodeAuditService;
import com.mcp.sailibrary.plugin.mcp.multimodel.audit.CodeAuditPromptBuilder;
import com.mcp.sailibrary.plugin.mcp.multimodel.audit.CodeAuditResult;
import com.mcp.sailibrary.plugin.mcp.multimodel.gateway.UnifiedMcpModelGateway;
import com.mcp.sailibrary.plugin.mcp.multimodel.routing.ModelNameResolver;

/** * Testes do servico de auditoria de codigo. * * @author Renato Tomaz Nati */
public class ClaudeCodeAuditServiceTest {

    @Test
    public void deveAprovarQuandoRespostaDoAuditorForValida() throws Exception {
        FakeUnifiedMcpModelGateway gateway = new FakeUnifiedMcpModelGateway();
        gateway.auditResponseText = "{\"approved\":true,\"shouldRetry\":false,\"riskLevel\":\"BAIXO\",\"feedback\":\"codigo aprovado\"}";

        ClaudeCodeAuditService service = new ClaudeCodeAuditService(
                new DummyModelNameResolver(),
                gateway,
                new McpResponseExtractor(),
                new CodeAuditPromptBuilder()
        );

        CodeAuditResult result = service.auditarCodigo(
                "pedido original",
                "plano de implementacao",
                "codigo candidato",
                "substituir",
                "api-key-audit"
        );

        assertNotNull(result);
        assertEquals(AuditExecutionStatus.APROVADO, result.getExecutionStatus());
        assertEquals(true, result.isAprovado());
        assertEquals(false, result.isDeveTentarNovamente());
        assertEquals("BAIXO", result.getNivelRisco());
        assertEquals("codigo aprovado", result.getFeedback());
        assertEquals("api-key-audit", gateway.lastApiKey);
    }

    @Test
    public void deveClassificarComoFalhaInfraQuandoFaltarCampoApproved() throws Exception {
        FakeUnifiedMcpModelGateway gateway = new FakeUnifiedMcpModelGateway();
        gateway.auditResponseText = "{\"shouldRetry\":false,\"riskLevel\":\"MEDIO\",\"feedback\":\"faltou approved\"}";

        ClaudeCodeAuditService service = new ClaudeCodeAuditService(
                new DummyModelNameResolver(),
                gateway,
                new McpResponseExtractor(),
                new CodeAuditPromptBuilder()
        );

        CodeAuditResult result = service.auditarCodigo(
                "pedido original",
                "plano de implementacao",
                "codigo candidato",
                "substituir",
                "api-key-audit"
        );

        assertNotNull(result);
        assertEquals(AuditExecutionStatus.FALHA_INFRA, result.getExecutionStatus());
        assertEquals(false, result.isAprovado());
        assertEquals("MEDIO", result.getNivelRisco());
    }

    @Test
    public void deveClassificarComoFalhaInfraQuandoTextoIndicarTimeout() throws Exception {
        FakeUnifiedMcpModelGateway gateway = new FakeUnifiedMcpModelGateway();
        gateway.auditResponseText = "timeout while processing request";

        ClaudeCodeAuditService service = new ClaudeCodeAuditService(
                new DummyModelNameResolver(),
                gateway,
                new McpResponseExtractor(),
                new CodeAuditPromptBuilder()
        );

        CodeAuditResult result = service.auditarCodigo(
                "pedido original",
                "plano de implementacao",
                "codigo candidato",
                "substituir",
                "api-key-audit"
        );

        assertNotNull(result);
        assertEquals(AuditExecutionStatus.FALHA_INFRA, result.getExecutionStatus());
        assertEquals(false, result.isAprovado());
    }

    private static class FakeUnifiedMcpModelGateway extends UnifiedMcpModelGateway {

        private String auditResponseText;
        private String lastApiKey;

        public FakeUnifiedMcpModelGateway() {
            super("https://fake.test");
        }

        @Override
        public ModelExecutionProfile resolveProfile(ModelChannel channel) {
            ModelExecutionProfile profile = new ModelExecutionProfile();
            profile.setTransportKind(TransportKind.STREAMING_SSE_HTTP);
            profile.setRequestFormatKind(RequestFormatKind.STREAMING_PROMPT);
            profile.setResponseFormatKind(ResponseFormatKind.STREAMING_SSE_EVENTS);
            profile.setLegacyModelAlias("CLAUDESONNET46");
            profile.setStreamingModelName("claude-sonnet-4-6");
            profile.setCreativity(Double.valueOf(0.00d));
            profile.setMaxTokens(Integer.valueOf(16384));
            return profile;
        }

        @Override
        public ModelExecutionResponse executeCodeAuditorPrompt(String prompt, String apiKey) {
            this.lastApiKey = apiKey;

            ModelExecutionResponse response = new ModelExecutionResponse();
            response.setPrimaryText(auditResponseText);
            response.setRawResponseBody(auditResponseText);
            response.setHttpStatusCode(200);
            return response;
        }
    }

    private static class DummyModelNameResolver implements ModelNameResolver {
        @Override
        public String resolveInvestigatorModelName() {
            return "O3";
        }

        @Override
        public String resolvePlannerModelName() {
            return "GPT54";
        }

        @Override
        public String resolveCodeGeneratorModelName() {
            return "GPT52CODEX";
        }

        @Override
        public String resolveCodeAuditorModelName() {
            return "CLAUDESONNET46";
        }

        @Override
        public String resolveSummarizerModelName() {
            return "GPT54MINI";
        }
    }
}