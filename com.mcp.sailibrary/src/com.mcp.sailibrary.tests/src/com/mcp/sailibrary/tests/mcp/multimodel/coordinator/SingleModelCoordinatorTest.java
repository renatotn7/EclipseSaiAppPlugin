package com.mcp.sailibrary.tests.mcp.multimodel.coordinator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptSectionBuilder;
import com.mcp.sailibrary.plugin.chat.service.McpAgentResponseService;
import com.mcp.sailibrary.plugin.chat.support.AiResponse;
import com.mcp.sailibrary.plugin.mcp.DesenvolvimentoPromptBuilder;
import com.mcp.sailibrary.plugin.mcp.McpResponseExtractor;
import com.mcp.sailibrary.plugin.mcp.core.ModelChannel;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionResponse;
import com.mcp.sailibrary.plugin.mcp.core.RequestFormatKind;
import com.mcp.sailibrary.plugin.mcp.core.ResponseFormatKind;
import com.mcp.sailibrary.plugin.mcp.core.StructuralContextDetector;
import com.mcp.sailibrary.plugin.mcp.core.TransportKind;
import com.mcp.sailibrary.plugin.mcp.multimodel.coordinator.SingleModelCoordinator;
import com.mcp.sailibrary.plugin.mcp.multimodel.gateway.UnifiedMcpModelGateway;
import com.mcp.sailibrary.plugin.mcp.multimodel.routing.ModelNameResolver;

/** * Testes do SingleModelCoordinator em estilo plugin Eclipse. * * <p>Observacao: * este teste e "quase unitario". Ele ainda passa pelo workspace Eclipse, * porque a classe concreta usa ResourcesPlugin e AgentToolRegistryFactory * internamente.</p> * * @author Renato Tomaz Nati */
public class SingleModelCoordinatorTest {

    @Test(expected = IllegalStateException.class)
    public void deveFalharQuandoNaoHaContextoMinimo() throws Exception {
        SingleModelCoordinator coordinator = new SingleModelCoordinator(
                new DummyModelNameResolver(),
                new FakeUnifiedMcpModelGateway(),
                new McpResponseExtractor(),
                new FakeMcpAgentResponseService(),
                new DesenvolvimentoPromptBuilder(),
                new FakeAgentToolPromptSectionBuilder(),
                new StructuralContextDetector()
        );

        coordinator.executarMissao("", "", "", "api-key");
    }

    @Test
    public void deveExecutarPlannerEInterpretarResposta() throws Exception {
        FakeUnifiedMcpModelGateway gateway = new FakeUnifiedMcpModelGateway();
        FakeMcpAgentResponseService responseService = new FakeMcpAgentResponseService();

        SingleModelCoordinator coordinator = new SingleModelCoordinator(
                new DummyModelNameResolver(),
                gateway,
                new McpResponseExtractor(),
                responseService,
                new DesenvolvimentoPromptBuilder(),
                new FakeAgentToolPromptSectionBuilder(),
                new StructuralContextDetector()
        );

        AiResponse response = coordinator.executarMissao(
                "public void x() {}",
                "class A { public void x() {} }",
                "me de o significado deste contexto principal",
                "api-key-123"
        );

        assertNotNull(response);
        assertEquals("explicar", response.getAction());
        assertEquals("significado do contexto", response.getContent());
        assertEquals("resposta interpretada com sucesso", response.getExplanation());

        assertEquals("api-key-123", gateway.lastApiKey);
        assertNotNull(gateway.lastPlannerPrompt);
        assertTrue(gateway.lastPlannerPrompt.contains("=== MODO OPERACIONAL DETECTADO ==="));
        assertTrue(gateway.lastPlannerPrompt.contains("MODO_TEXTUAL"));
    }

    private static class FakeUnifiedMcpModelGateway extends UnifiedMcpModelGateway {

        private String lastPlannerPrompt;
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
            profile.setLegacyModelAlias("GPT54");
            profile.setStreamingModelName("gpt-5.4-2026-03-05");
            profile.setCreativity(Double.valueOf(0.10d));
            profile.setMaxTokens(Integer.valueOf(16384));
            return profile;
        }

        @Override
        public ModelExecutionResponse executePlannerPrompt(String prompt, String apiKey) {
            this.lastPlannerPrompt = prompt;
            this.lastApiKey = apiKey;

            ModelExecutionResponse response = new ModelExecutionResponse();
            response.setPrimaryText("{\"action\":\"explicar\",\"content\":\"significado do contexto\",\"explanation\":\"resposta interpretada com sucesso\"}");
            response.setRawResponseBody("{\"action\":\"explicar\",\"content\":\"significado do contexto\",\"explanation\":\"resposta interpretada com sucesso\"}");
            response.setHttpStatusCode(200);
            return response;
        }
    }

    private static class FakeMcpAgentResponseService extends McpAgentResponseService {

        @Override
        public boolean isModelInfrastructureFailureText(String text) {
            return false;
        }

        @Override
        public AiResponse interpretarRespostaIA(String textoResposta) {
            AiResponse response = new AiResponse();
            response.setAction("explicar");
            response.setContent("significado do contexto");
            response.setExplanation("resposta interpretada com sucesso");
            return response;
        }

        @Override
        public AiResponse normalizarProtocoloFerramentaLegado(AiResponse resposta) {
            return resposta;
        }

        @Override
        public AiResponse buildInfrastructureFailureResponse(String detalheTecnico, boolean podeTentarNovamente) {
            AiResponse response = new AiResponse();
            response.setAction("responder_ao_usuario");
            response.setContent("falha");
            response.setExplanation(detalheTecnico);
            return response;
        }
    }

    private static class FakeAgentToolPromptSectionBuilder extends AgentToolPromptSectionBuilder {
        @Override
        public String buildToolsSection(java.util.List tools) {
            return "\n=== FERRAMENTAS DE TESTE ===\n";
        }

        @Override
        public String buildExamplesSection(java.util.List tools, int maxExamplesPerTool) {
            return "\n=== EXEMPLOS DE TESTE ===\n";
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