package com.mcp.sailibrary.tests.mcp.adapters.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import com.mcp.sailibrary.plugin.mcp.adapters.config.PropertiesBackedModelExecutionProfileResolver;
import com.mcp.sailibrary.plugin.mcp.core.ModelChannel;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.RequestFormatKind;
import com.mcp.sailibrary.plugin.mcp.core.ResponseFormatKind;
import com.mcp.sailibrary.plugin.mcp.core.TransportKind;

public class PropertiesBackedModelExecutionProfileResolverTest {

    @Test
    public void deveResolverPlannerStreamingComValoresDoProperties() {
    	PropertiesBackedModelExecutionProfileResolver resolver =
    	        new PropertiesBackedModelExecutionProfileResolver(
    	                "mcp-models-test.properties",
    	                PropertiesBackedModelExecutionProfileResolverTest.class
    	        );

        ModelExecutionProfile profile = resolver.resolve(ModelChannel.PLANNER);

        assertEquals(TransportKind.STREAMING_SSE_HTTP, profile.getTransportKind());
        assertEquals(RequestFormatKind.STREAMING_PROMPT, profile.getRequestFormatKind());
        assertEquals(ResponseFormatKind.STREAMING_SSE_EVENTS, profile.getResponseFormatKind());
        assertEquals("https://stream.test/api/stream", profile.getEndpointUrl());
        assertEquals("GPT54", profile.getLegacyModelAlias());
        assertEquals("gpt-5.4-2026-03-05", profile.getStreamingModelName());
        assertEquals(0.20d, profile.getCreativity().doubleValue(), 0.0001d);
        assertEquals(2000, profile.getMaxTokens().intValue());
        assertEquals("conv-test", profile.getConversationId());
        assertTrue(profile.isFileSearch());
        assertTrue(profile.isWebSearch());
    }

    @Test
    public void deveResolverCanalLegadoQuandoConfiguradoAssim() {
        File propsFile = localizarArquivo("mcp-models-test.properties");
        assertNotNull("Nao encontrei o arquivo mcp-models-test.properties no projeto de testes.", propsFile);

        PropertiesBackedModelExecutionProfileResolver resolver =
                new PropertiesBackedModelExecutionProfileResolver(
                        propsFile.getAbsolutePath(),
                        PropertiesBackedModelExecutionProfileResolverTest.class
                );

        ModelExecutionProfile profile = resolver.resolve(ModelChannel.CODE_AUDITOR);

        assertEquals(TransportKind.LEGACY_JSON_RPC_HTTP, profile.getTransportKind());
        assertEquals(RequestFormatKind.LEGACY_MCP_TOOLS_CALL, profile.getRequestFormatKind());
        assertEquals(ResponseFormatKind.LEGACY_MCP_ENVELOPE, profile.getResponseFormatKind());
        assertEquals("https://legacy.test/api/mcp", profile.getEndpointUrl());
        assertEquals("CLAUDESONNET46", profile.getLegacyModelAlias());
    }

    private File localizarArquivo(String nome) {
        File dir = new File(System.getProperty("user.dir"));

        for (int i = 0; i < 8 && dir != null; i++) {
            File[] candidatos = new File[] {
                    new File(dir, nome),
                    new File(dir, "src/" + nome),
                    new File(dir, "resources/" + nome),
                    new File(dir, "com.mcp.sailibrary.tests/" + nome),
                    new File(dir, "com.mcp.sailibrary.tests/src/" + nome),
                    new File(dir, "../com.mcp.sailibrary.tests/" + nome),
                    new File(dir, "../com.mcp.sailibrary.tests/src/" + nome)
            };

            for (File candidato : candidatos) {
                if (candidato.isFile()) {
                    return candidato.getAbsoluteFile();
                }
            }

            dir = dir.getParentFile();
        }

        return null;
    }
}