package com.mcp.sailibrary.tests.mcp.multimodel.generation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.mcp.sailibrary.plugin.chat.service.McpAgentResponseService;
import com.mcp.sailibrary.plugin.chat.support.AiResponse;
import com.mcp.sailibrary.plugin.mcp.McpResponseExtractor;
import com.mcp.sailibrary.plugin.mcp.core.ModelChannel;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionResponse;
import com.mcp.sailibrary.plugin.mcp.core.RequestFormatKind;
import com.mcp.sailibrary.plugin.mcp.core.ResponseFormatKind;
import com.mcp.sailibrary.plugin.mcp.core.TransportKind;
import com.mcp.sailibrary.plugin.mcp.multimodel.generation.CodeGenerationPromptBuilder;
import com.mcp.sailibrary.plugin.mcp.multimodel.generation.CodexCodeGenerationService;
import com.mcp.sailibrary.plugin.mcp.multimodel.gateway.UnifiedMcpModelGateway;
import com.mcp.sailibrary.plugin.mcp.multimodel.routing.ModelNameResolver;

/** * Testes do servico de geracao de codigo. * * @author Renato Tomaz Nati */
public class CodexCodeGenerationServiceTest {

    @Test
    public void deveGerarCodigoUsandoCanalCodeGenerator() throws Exception {
        FakeUnifiedMcpModelGateway gateway = new FakeUnifiedMcpModelGateway();
        FakeMcpAgentResponseService responseService = new FakeMcpAgentResponseService();

        CodexCodeGenerationService service = new CodexCodeGenerationService(
                new DummyModelNameResolver(),
                gateway,
                new McpResponseExtractor(),
                responseService,
                new CodeGenerationPromptBuilder()
        );

        AiResponse response = service.gerarCodigo(
                "implemente a alteracao",
                "plano tecnico consolidado",
                "trecho selecionado",
                "arquivo completo",
                "substituir",
                "api-key-xyz"
        );

        assertNotNull(response);
        assertEquals("substituir", response.getAction());
        assertEquals("codigo final gerado", response.getContent());
        assertEquals("geracao concluida", response.getExplanation());

        assertEquals("api-key-xyz", gateway.lastApiKey);
        assertNotNull(gateway.lastCodeGeneratorPrompt);
        assertTrue(gateway.lastCodeGeneratorPrompt.contains("=== PEDIDO ORIGINAL DO USUARIO ==="));
        assertTrue(gateway.lastCodeGeneratorPrompt.contains("=== PLANO DE IMPLEMENTACAO RECEBIDO ==="));
        assertTrue(gateway.lastCodeGeneratorPrompt.contains("=== ACAO FINAL ESPERADA ==="));
    }

    @Test
    public void deveRetornarNuloQuandoGatewayNaoRetornarResposta() throws Exception {
        FakeUnifiedMcpModelGateway gateway = new FakeUnifiedMcpModelGateway();
        gateway.returnNullResponse = true;

        CodexCodeGenerationService service = new CodexCodeGenerationService(
                new DummyModelNameResolver(),
                gateway,
                new McpResponseExtractor(),
                new FakeMcpAgentResponseService(),
                new CodeGenerationPromptBuilder()
        );

        AiResponse response = service.gerarCodigo(
                "pedido",
                "plano",
                "trecho",
                "arquivo",
                "substituir",
                "api-key"
        );

        assertEquals(null, response);
    }

    private static class FakeUnifiedMcpModelGateway extends UnifiedMcpModelGateway {

        private String lastCodeGeneratorPrompt;
        private String lastApiKey;
        private boolean returnNullResponse;

        public FakeUnifiedMcpModelGateway() {
            super("https://fake.test");
        }

        @Override
        public ModelExecutionProfile resolveProfile(ModelChannel channel) {
            ModelExecutionProfile profile = new ModelExecutionProfile();
            profile.setTransportKind(TransportKind.STREAMING_SSE_HTTP);
            profile.setRequestFormatKind(RequestFormatKind.STREAMING_PROMPT);
            profile.setResponseFormatKind(ResponseFormatKind.STREAMING_SSE_EVENTS);
            profile.setLegacyModelAlias("GPT52CODEX");
            profile.setStreamingModelName("gpt-5.2-codex");
            profile.setCreativity(Double.valueOf(0.00d));
            profile.setMaxTokens(Integer.valueOf(16384));
            return profile;
        }

        @Override
        public ModelExecutionResponse executeCodeGeneratorPrompt(String prompt, String apiKey) {
            this.lastCodeGeneratorPrompt = prompt;
            this.lastApiKey = apiKey;

            if (returnNullResponse) {
                return null;
            }

            ModelExecutionResponse response = new ModelExecutionResponse();
            response.setPrimaryText("{\"action\":\"substituir\",\"content\":\"codigo final gerado\",\"explanation\":\"geracao concluida\"}");
            response.setRawResponseBody("{\"action\":\"substituir\",\"content\":\"codigo final gerado\",\"explanation\":\"geracao concluida\"}");
            response.setHttpStatusCode(200);
            return response;
        }
    }

    private static class FakeMcpAgentResponseService extends McpAgentResponseService {

        @Override
        public AiResponse interpretarRespostaIA(String textoResposta) {
            AiResponse response = new AiResponse();
            response.setAction("substituir");
            response.setContent("codigo final gerado");
            response.setExplanation("geracao concluida");
            return response;
        }

        @Override
        public AiResponse normalizarProtocoloFerramentaLegado(AiResponse resposta) {
            return resposta;
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