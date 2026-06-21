package com.mcp.sailibrary.tests.mcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.mcp.sailibrary.plugin.mcp.McpResponseExtractor;

public class McpResponseExtractorTest {

    @Test
    public void deveExtrairTextoDoEnvelopeLegado() {
        McpResponseExtractor extractor = new McpResponseExtractor();

        String raw = "{"
                + "\"result\":{"
                + "\"content\":[{\"text\":\"texto legado\"}]"
                + "}"
                + "}";

        assertEquals("texto legado", extractor.extractPrimaryText(raw));
    }

    @Test
    public void deveExtrairTextoDoResponseCompletedNoStreaming() {
        McpResponseExtractor extractor = new McpResponseExtractor();

        String raw = ""
                + "event: responsecreated\n"
                + "data: {\"type\":\"ResponseCreated\"}\n"
                + "\n"
                + "event: responsecompleted\n"
                + "data: {\"type\":\"ResponseCompleted\",\"items\":[{\"role\":\"assistant\",\"content\":[{\"type\":\"output_text\",\"text\":\"Recebi. Pode continuar com o teste.\"}]}]}\n"
                + "\n"
                + "event: complete\n"
                + "data: {\"status\":\"finished\"}\n";

        assertEquals("Recebi. Pode continuar com o teste.", extractor.extractPrimaryText(raw));
    }

    @Test
    public void devePriorizarResponseCompletedEmVezDeDelta() {
        McpResponseExtractor extractor = new McpResponseExtractor();

        String raw = ""
                + "event: outputtextdelta\n"
                + "data: {\"type\":\"OutputTextDelta\",\"delta\":\"texto parcial\"}\n"
                + "\n"
                + "event: responsecompleted\n"
                + "data: {\"type\":\"ResponseCompleted\",\"items\":[{\"role\":\"assistant\",\"content\":[{\"type\":\"output_text\",\"text\":\"texto final consolidado\"}]}]}\n"
                + "\n";

        assertEquals("texto final consolidado", extractor.extractPrimaryText(raw));
    }

    @Test
    public void deveSanitizarNomeDeBloco() {
        McpResponseExtractor extractor = new McpResponseExtractor();

        assertEquals("validacao", extractor.extractSuggestedBlockName("\"Validacao!!!\""));
    }

    @Test
    public void deveRetornarTextoPuroQuandoNaoConsegueParsear() {
        McpResponseExtractor extractor = new McpResponseExtractor();

        assertEquals("texto puro", extractor.extractPrimaryText("texto puro"));
    }

    @Test
    public void deveRetornarVazioQuandoEntradaForVazia() {
        McpResponseExtractor extractor = new McpResponseExtractor();

        assertTrue(extractor.extractPrimaryText("").isEmpty());
    }
}