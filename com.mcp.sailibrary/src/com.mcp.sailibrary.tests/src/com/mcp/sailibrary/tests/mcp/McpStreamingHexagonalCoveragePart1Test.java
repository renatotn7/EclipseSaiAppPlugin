package com.mcp.sailibrary.tests.mcp;

import static org.junit.Assert.*;

import java.lang.reflect.Method;

import org.junit.Test;

import com.mcp.sailibrary.plugin.mcp.McpPayloadBuilder;
import com.mcp.sailibrary.plugin.mcp.McpResponseExtractor;
import com.mcp.sailibrary.plugin.mcp.adapters.codec.request.LegacyMcpRequestCodec;
import com.mcp.sailibrary.plugin.mcp.adapters.codec.request.RawJsonStreamingRequestCodec;
import com.mcp.sailibrary.plugin.mcp.adapters.codec.request.StreamingPromptRequestCodec;
import com.mcp.sailibrary.plugin.mcp.adapters.codec.response.LegacyMcpResponseCodec;
import com.mcp.sailibrary.plugin.mcp.adapters.codec.response.PlainTextResponseCodec;
import com.mcp.sailibrary.plugin.mcp.adapters.codec.response.StreamingSseResponseCodec;
import com.mcp.sailibrary.plugin.mcp.core.McpAccessCredentials;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionRequest;
import com.mcp.sailibrary.plugin.mcp.core.RawModelResponse;

public class McpStreamingHexagonalCoveragePart1Test {

    @Test
    public void deveCobrirCredenciaisRequestRawResponseEProfile() throws Exception {
        McpAccessCredentials credentials = McpAccessCredentials.forApiKeyAndCookie(" api ", " cookie ");
        assertEquals("api", credentials.getApiKey());
        assertEquals("cookie", credentials.getCookieValue());
        assertTrue(credentials.hasApiKey());
        assertTrue(credentials.hasCookieValue());

        ModelExecutionRequest request = new ModelExecutionRequest();
        request.setPrompt(" prompt principal ");
        request.setRawJsonBody(" {\"raw\":true} ");
        request.setCredentials(credentials);

        assertEquals("prompt principal", request.getPrompt());
        assertEquals("{\"raw\":true}", request.getRawJsonBody());
        assertTrue(request.hasRawJsonBody());
        assertNotNull(request.getCredentials());

        RawModelResponse raw = new RawModelResponse(" corpo ", 200, " application/json ");
        assertEquals(" corpo ", raw.getRawBody());
        assertEquals(200, raw.getStatusCode());
        assertEquals(" application/json ", raw.getContentType());

        raw.setRawBody(null);
        raw.setContentType(null);
        raw.setStatusCode(204);

        assertEquals("", raw.getRawBody());
        assertEquals("", raw.getContentType());
        assertEquals(204, raw.getStatusCode());

        ModelExecutionProfile profile = criarProfileBasico();
        profile.setCreativity(Double.valueOf(2.0d));
        assertEquals(Double.valueOf(1.0d), profile.getCreativity());

        profile.setCreativity(Double.valueOf(-1.0d));
        assertEquals(Double.valueOf(0.0d), profile.getCreativity());

        profile.setMaxTokens(Integer.valueOf(-1));
        assertEquals(Integer.valueOf(16384), profile.getMaxTokens());

        profile.setLegacyModelAlias(" legacy-model ");
        assertEquals("legacy-model", profile.resolveEffectiveModelName());

        setTransportKind(profile, "STREAMING_SSE_HTTP");
        profile.setStreamingModelName(" streaming-model ");
        assertEquals("streaming-model", profile.resolveEffectiveModelName());

        profile.setEnableStreaming(false);
        profile.setFileSearch(false);
        profile.setCodeInterpreter(false);
        profile.setWebSearch(false);
        profile.setInstructions(" instrucoes ");

        assertFalse(profile.isEnableStreaming());
        assertFalse(profile.isFileSearch());
        assertFalse(profile.isCodeInterpreter());
        assertFalse(profile.isWebSearch());
        assertEquals("instrucoes", profile.getInstructions());
    }

    @Test
    public void deveCobrirMcpResponseExtractorEPayloadBuilder() throws Exception {
        McpResponseExtractor extractor = new McpResponseExtractor();

        String legacyEnvelope = "{"
                + "\"result\":{"
                + "\"content\":[{\"type\":\"text\",\"text\":\"LEGACY_TEXT\"}]"
                + "}"
                + "}";

        String directJson = "{"
                + "\"output\":[{"
                + "\"content\":[{\"type\":\"output_text\",\"text\":\"DIRECT_TEXT\"}]"
                + "}]"
                + "}";

        String sse = ""
                + "event: response.output_text.delta\n"
                + "data: {\"delta\":\"Hello \"}\n\n"
                + "event: response.output_text.delta\n"
                + "data: {\"delta\":\"streaming\"}\n\n"
                + "event: response.output_item.done\n"
                + "data: {\"item\":{\"content\":[{\"type\":\"output_text\",\"text\":\" item\"}]}}\n\n"
                + "event: response.content_part.done\n"
                + "data: {\"part\":{\"type\":\"output_text\",\"text\":\" part\"}}\n\n"
                + "event: response.completed\n"
                + "data: {\"response\":{\"output\":[{\"content\":[{\"type\":\"output_text\",\"text\":\" done\"}]}]}}\n\n";

        String extractedLegacy = extractor.extractPrimaryText(legacyEnvelope);
        assertNotNull(extractedLegacy);
        assertTrue(extractedLegacy.contains("LEGACY_TEXT"));

        String extractedDirect = (String) invokePrivate(
                extractor,
                "extractPrimaryTextFromDirectJson",
                new Class<?>[] { String.class },
                new Object[] { directJson });
        assertNotNull(extractedDirect);
        assertTrue(extractedDirect.contains("DIRECT_TEXT"));

        String extractedSse = (String) invokePrivate(
                extractor,
                "extractPrimaryTextFromStreamingSse",
                new Class<?>[] { String.class },
                new Object[] { sse });
        assertNotNull(extractedSse);
        assertTrue(extractedSse.contains("Hello"));
        assertTrue(extractedSse.contains("streaming"));

        String suggestedBlock = extractor.extractSuggestedBlockName(
                "{\"suggestedBlockName\":\"Meu Bloco*1\"}");
        assertNotNull(suggestedBlock);
        assertFalse(suggestedBlock.trim().isEmpty());

        String sanitized = (String) invokePrivate(
                extractor,
                "sanitizeBlockName",
                new Class<?>[] { String.class },
                new Object[] { " bloco::inválido<> " });
        assertNotNull(sanitized);
        assertFalse(sanitized.trim().isEmpty());

        McpPayloadBuilder payloadBuilder = new McpPayloadBuilder();
        ModelExecutionProfile profile = criarProfileBasico();

        String payload = payloadBuilder.buildStreamingPromptPayload(
                "prompt com \"aspas\"\nlinha 2",
                profile);
        assertNotNull(payload);
        assertTrue(payload.contains("prompt") || payload.contains("input"));

        String normalizedRaw = payloadBuilder.normalizeRawJsonPayload("{\"a\":1}");
        assertNotNull(normalizedRaw);
        assertTrue(normalizedRaw.contains("\"a\""));

        String escaped = payloadBuilder.escapeForJsonTransport("a\"b\\c\nd");
        assertNotNull(escaped);
        assertTrue(escaped.contains("\\\"") || escaped.contains("\\n"));

        String toolsPayload = payloadBuilder.buildToolsCallPayload("model-x", "conteudo");
        assertNotNull(toolsPayload);
        assertTrue(toolsPayload.contains("conteudo"));
    }

    @Test
    public void deveCobrirCodecsDeRequisicaoEResposta() throws Exception {
        ModelExecutionProfile profile = criarProfileBasico();

        ModelExecutionRequest legacyRequest = new ModelExecutionRequest();
        legacyRequest.setPrompt("prompt legacy");
        legacyRequest.setCredentials(McpAccessCredentials.forApiKey("api-key"));

        String legacyEncoded = new LegacyMcpRequestCodec().encode(legacyRequest, profile);
        assertNotNull(legacyEncoded);
        assertFalse(legacyEncoded.isEmpty());

        ModelExecutionRequest streamingRequest = new ModelExecutionRequest();
        streamingRequest.setPrompt("prompt streaming");
        streamingRequest.setCredentials(McpAccessCredentials.forCookie("cookie-x"));

        String streamingEncoded = new StreamingPromptRequestCodec().encode(streamingRequest, profile);
        assertNotNull(streamingEncoded);
        assertTrue(streamingEncoded.contains("prompt") || streamingEncoded.contains("input"));

        ModelExecutionRequest rawRequest = new ModelExecutionRequest();
        rawRequest.setRawJsonBody("{\"raw\":true}");
        rawRequest.setCredentials(McpAccessCredentials.forApiKeyAndCookie("api", "cookie"));

        String rawEncoded = new RawJsonStreamingRequestCodec().encode(rawRequest, profile);
        assertNotNull(rawEncoded);
        assertTrue(rawEncoded.contains("\"raw\":true"));

        String legacyEnvelope = "{"
                + "\"result\":{"
                + "\"content\":[{\"type\":\"text\",\"text\":\"LEGACY_CODEC\"}]"
                + "}"
                + "}";

        Object legacyDecoded = new LegacyMcpResponseCodec().decode(
                new RawModelResponse(legacyEnvelope, 200, "application/json"),
                profile);
        assertNotNull(legacyDecoded);
        assertTrue(primaryTextOf(legacyDecoded).contains("LEGACY_CODEC"));

        Object plainDecoded = new PlainTextResponseCodec().decode(
                new RawModelResponse("plain text response", 200, "text/plain"),
                profile);
        assertNotNull(plainDecoded);
        assertEquals("plain text response", primaryTextOf(plainDecoded));

        String sse = ""
                + "event: response.output_text.delta\n"
                + "data: {\"delta\":\"Hello \"}\n\n"
                + "event: response.output_text.delta\n"
                + "data: {\"delta\":\"codec\"}\n\n"
                + "event: response.completed\n"
                + "data: {\"response\":{\"output\":[{\"content\":[{\"type\":\"output_text\",\"text\":\" done\"}]}]}}\n\n";

        Object sseDecoded = new StreamingSseResponseCodec().decode(
                new RawModelResponse(sse, 200, "text/event-stream"),
                profile);
        assertNotNull(sseDecoded);

        String primaryText = primaryTextOf(sseDecoded);
        assertNotNull(primaryText);
        assertTrue(primaryText.contains("Hello") || primaryText.contains("done"));
    }

    private ModelExecutionProfile criarProfileBasico() {
        ModelExecutionProfile profile = new ModelExecutionProfile();
        profile.setEndpointUrl("https://unit.test/api/mcp");
        profile.setLegacyModelAlias("LEGACY_MODEL");
        profile.setStreamingModelName("STREAMING_MODEL");
        profile.setEnableStreaming(true);
        profile.setFileSearch(true);
        profile.setCodeInterpreter(false);
        profile.setWebSearch(false);
        profile.setInstructions("instrucao-base");
        return profile;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void setTransportKind(ModelExecutionProfile profile, String enumName) throws Exception {
        Class enumClass = Class.forName("com.mcp.sailibrary.plugin.mcp.core.TransportKind");
        Object constant = Enum.valueOf(enumClass, enumName);
        Method setter = ModelExecutionProfile.class.getMethod("setTransportKind", enumClass);
        setter.invoke(profile, constant);
    }

    private Object invokePrivate(Object target, String methodName, Class<?>[] types, Object[] args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, types);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private String primaryTextOf(Object response) throws Exception {
        Method method = response.getClass().getMethod("getPrimaryText");
        Object value = method.invoke(response);
        return value == null ? "" : String.valueOf(value);
    }
}