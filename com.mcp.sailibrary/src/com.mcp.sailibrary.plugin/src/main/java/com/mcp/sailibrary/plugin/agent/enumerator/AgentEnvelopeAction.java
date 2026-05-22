package com.mcp.sailibrary.plugin.agent.enumerator;

/**
 * Representar de forma tipada as actions aceitas no protocolo da IA.
 *
 * @author Renato Tomaz Nati
 * @since 2026-05-18
 */
public enum AgentEnvelopeAction {

    EXECUTAR_FERRAMENTA("executar_ferramenta"),
    //USAR_FERRAMENTA("usar_ferramenta"),
    PERGUNTAR_AO_USUARIO("perguntar_ao_usuario"),
    RESPONDER_AO_USUARIO("responder_ao_usuario"),
    EXPLICAR("explicar"),
    SUBSTITUIR("substituir"),
    COMENTAR("comentar"),
    INSERIR_ABAIXO("inserir_abaixo"),
    ANEXAR_ACIMA("anexar_acima");

    private final String value;

    private AgentEnvelopeAction(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AgentEnvelopeAction fromValue(String value) {
        if (value == null) {
            return null;
        }

        for (AgentEnvelopeAction action : values()) {
            if (action.value.equalsIgnoreCase(value.trim())) {
                return action;
            }
        }

        return null;
    }

    public boolean isAcaoDeEdicao() {
        return this == SUBSTITUIR
            || this == COMENTAR
            || this == INSERIR_ABAIXO
            || this == ANEXAR_ACIMA;
    }

    public boolean isRespostaFinalNaoDestrutiva() {
        return this == RESPONDER_AO_USUARIO
            || this == EXPLICAR
            || this == PERGUNTAR_AO_USUARIO;
    }
}