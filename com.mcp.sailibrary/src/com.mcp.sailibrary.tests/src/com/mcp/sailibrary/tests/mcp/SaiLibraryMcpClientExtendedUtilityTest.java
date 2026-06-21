package com.mcp.sailibrary.tests.mcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.mcp.sailibrary.plugin.mcp.SaiLibraryMcpClient;
import com.mcp.sailibrary.plugin.mcp.core.ModelChannel;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;

/** * Testes adicionais utilitarios da fachada SaiLibraryMcpClient. * * @author Renato Tomaz Nati */
public class SaiLibraryMcpClientExtendedUtilityTest {

    @Test
    public void deveExtrairTextoDiretoDoStreamingResponseCompleted() {
        String raw = ""
                + "event: responsecompleted\n"
                + "data: {\"type\":\"ResponseCompleted\",\"items\":[{\"role\":\"assistant\",\"content\":[{\"type\":\"output_text\",\"text\":\"texto final via facade\"}]}]}\n"
                + "\n";

        String result = SaiLibraryMcpClient.extractPrimaryText(raw);

        assertEquals("texto final via facade", result);
    }

    @Test
    public void deveResolverProfileDoPlanner() {
        ModelExecutionProfile profile = SaiLibraryMcpClient.resolveExecutionProfile(ModelChannel.PLANNER);

        assertNotNull(profile);
        assertNotNull(profile.getTransportKind());
        assertNotNull(profile.getRequestFormatKind());
        assertNotNull(profile.getResponseFormatKind());
    }
}