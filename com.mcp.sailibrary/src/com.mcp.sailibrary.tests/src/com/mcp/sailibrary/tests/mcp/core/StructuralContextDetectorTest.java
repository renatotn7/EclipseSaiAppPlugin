package com.mcp.sailibrary.tests.mcp.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.mcp.sailibrary.plugin.mcp.core.StructuralContextDetector;

public class StructuralContextDetectorTest {

    @Test
    public void deveDetectarAlias() {
        StructuralContextDetector detector = new StructuralContextDetector();

        assertTrue(detector.hasStructuralContext("use @service como referencia"));
    }

    @Test
    public void deveDetectarMarcadoresEstruturais() {
        StructuralContextDetector detector = new StructuralContextDetector();

        assertTrue(detector.hasStructuralContext("=== CONTEXTO ESTRUTURAL DA SESSAO ==="));
        assertTrue(detector.hasStructuralContext("FOCO_PRINCIPAL_ESTRUTURAL: x"));
        assertTrue(detector.hasStructuralContext("ESCOPO_EDITAVEL_ESTRUTURAL: y"));
        assertTrue(detector.hasStructuralContext("ESCOPO_REFERENCIAL_ESTRUTURAL: z"));
        assertTrue(detector.hasStructuralContext("Contexto estrutural importante"));
        assertTrue(detector.hasStructuralContext("ALVO PRINCIPAL: abc arquivo=Teste.java"));
    }

    @Test
    public void naoDeveDetectarQuandoNaoHaSinais() {
        StructuralContextDetector detector = new StructuralContextDetector();

        assertFalse(detector.hasStructuralContext("texto comum sem nada"));
        assertFalse(detector.hasStructuralContext(null));
        assertFalse(detector.hasStructuralContext(""));
    }
}