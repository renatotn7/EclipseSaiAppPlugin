package com.mcp.sailibrary.tests.mcp.adapters.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.mcp.sailibrary.plugin.mcp.adapters.config.PropertiesBackedModelExecutionProfileResolver;
import com.mcp.sailibrary.plugin.mcp.core.ModelChannel;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.RequestFormatKind;
import com.mcp.sailibrary.plugin.mcp.core.ResponseFormatKind;
import com.mcp.sailibrary.plugin.mcp.core.TransportKind;

/** * Testes adicionais do resolver por properties para cobrir fallbacks. * * @author Renato Tomaz Nati */
public class PropertiesBackedModelExecutionProfileResolverAdditionalTest {

    @Test
    public void deveUsarDefaultsQuandoResourceNaoExistir() {
        PropertiesBackedModelExecutionProfileResolver resolver =
                new PropertiesBackedModelExecutionProfileResolver(
                        "/arquivo-que-nao-existe.properties",
                        PropertiesBackedModelExecutionProfileResolverAdditionalTest.class
                );

        ModelExecutionProfile profile = resolver.resolve(ModelChannel.PLANNER);

        assertNotNull(profile);
        assertEquals(TransportKind.LEGACY_JSON_RPC_HTTP, profile.getTransportKind());
        assertEquals(RequestFormatKind.LEGACY_MCP_TOOLS_CALL, profile.getRequestFormatKind());
        assertEquals(ResponseFormatKind.LEGACY_MCP_ENVELOPE, profile.getResponseFormatKind());
        assertEquals("https://sai-library.saiapplications.com/api/mcp", profile.getEndpointUrl());
        assertEquals("GPT54", profile.getLegacyModelAlias());
    }

    @Test
    public void deveResolverStreamingModelDefaultDoSummarizer() {
        PropertiesBackedModelExecutionProfileResolver resolver =
                new PropertiesBackedModelExecutionProfileResolver(
                        "/arquivo-que-nao-existe.properties",
                        PropertiesBackedModelExecutionProfileResolverAdditionalTest.class
                );

        ModelExecutionProfile profile = resolver.resolve(ModelChannel.SUMMARIZER);

        assertEquals("gpt-5.4-mini-2026-03-17", profile.getStreamingModelName());
        assertEquals(8192, profile.getMaxTokens().intValue());
        assertEquals(0.0d, profile.getCreativity().doubleValue(), 0.0001d);
    }
}