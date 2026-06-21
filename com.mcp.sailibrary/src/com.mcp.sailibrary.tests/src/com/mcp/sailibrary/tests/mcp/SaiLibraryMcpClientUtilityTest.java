package com.mcp.sailibrary.tests.mcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.mcp.sailibrary.plugin.mcp.SaiLibraryMcpClient;
import com.mcp.sailibrary.plugin.mcp.core.ModelChannel;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;

/** * Testes utilitarios da fachada publica SaiLibraryMcpClient. * * <p>Estes testes evitam chamadas remotas e exercitam apenas metodos locais, * puros ou quase puros.</p> * * @author Renato Tomaz Nati */
public class SaiLibraryMcpClientUtilityTest {

    @Test
    public void deveExtrairNomeSugeridoDoEnvelopeLegado() {
        String result = SaiLibraryMcpClient.extractSuggestedBlockName(
                "{\"result\":{\"content\":[{\"text\":\"Validacao\"}]}}"
        );

        assertEquals("validacao", result);
    }

    @Test
    public void deveEscaparTextoComSafeString() {
        String result = SaiLibraryMcpClient.safeString("abc\n\"x\"");

        assertEquals("abc\\n\\\"x\\\"", result);
    }

    @Test
    public void deveExtrairTextoPrincipalDeEnvelopeLegado() {
        String result = SaiLibraryMcpClient.extractPrimaryText(
                "{\"result\":{\"content\":[{\"text\":\"texto principal\"}]}}"
        );

        assertEquals("texto principal", result);
    }

    @Test
    public void deveExtrairTextoPrincipalDeStreamingResponseCompleted() {
        String raw = ""
                + "event: responsecompleted\n"
                + "data: {\"type\":\"ResponseCompleted\",\"items\":[{\"role\":\"assistant\",\"content\":[{\"type\":\"output_text\",\"text\":\"texto final do stream\"}]}]}\n"
                + "\n"
                + "event: complete\n"
                + "data: {\"status\":\"finished\"}\n";

        String result = SaiLibraryMcpClient.extractPrimaryText(raw);

        assertEquals("texto final do stream", result);
    }

    @Test
    public void deveResolverExecutionProfileDoCanal() {
        ModelExecutionProfile profile = SaiLibraryMcpClient.resolveExecutionProfile(ModelChannel.SUMMARIZER);

        assertNotNull(profile);
        assertNotNull(profile.getTransportKind());
        assertNotNull(profile.getRequestFormatKind());
        assertNotNull(profile.getResponseFormatKind());
    }
}