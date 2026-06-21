package com.mcp.sailibrary.plugin.mcp.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** * Detecta sinais fortes de contexto estrutural utilizavel no prompt. * * @author Renato Tomaz Nati * @since 2026-05-26 */
public class StructuralContextDetector {

    private static final Pattern ALIAS_PATTERN = Pattern.compile("@[a-zA-Z0-9_]+");

    public boolean hasStructuralContext(String textoInstrucao) {
        if (textoInstrucao == null || textoInstrucao.trim().length() == 0) {
            return false;
        }

        String texto = textoInstrucao;

        if (texto.contains("@")) {
            Matcher matcher = ALIAS_PATTERN.matcher(texto);
            if (matcher.find()) {
                return true;
            }
        }

        if (texto.contains("=== CONTEXTO ESTRUTURAL DA SESSAO ===")) {
            return true;
        }

        if (texto.contains("FOCO_PRINCIPAL_ESTRUTURAL:")) {
            return true;
        }

        if (texto.contains("ESCOPO_EDITAVEL_ESTRUTURAL:")) {
            return true;
        }

        if (texto.contains("ESCOPO_REFERENCIAL_ESTRUTURAL:")) {
            return true;
        }

        if (texto.contains("Contexto estrutural")) {
            return true;
        }

        if (texto.contains("ALVO PRINCIPAL:") && texto.contains("arquivo=")) {
            return true;
        }

        return false;
    }
}