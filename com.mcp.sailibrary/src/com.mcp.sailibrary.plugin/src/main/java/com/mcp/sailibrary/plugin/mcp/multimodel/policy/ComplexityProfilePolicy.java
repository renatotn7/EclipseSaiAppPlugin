package com.mcp.sailibrary.plugin.mcp.multimodel.policy;

import com.mcp.sailibrary.plugin.chat.settings.ChatRuntimeSettings;

/* --- version: "1.0" libraries: - ChatRuntimeSettings objetivo: "Definir o comportamento esperado por perfil de raciocinio, traduzindo os perfis da UI em diretrizes operacionais para a IA." --- */

/** * Politica de comportamento por perfil de raciocinio. * * <p>Esta classe transforma os perfis da configuracao da UI em texto * operacional forte para o prompt, reduzindo a dependencia de boa vontade * espontanea do modelo.</p> * * @author Renato Tomaz Nati * @since 2026-05-24 */
public class ComplexityProfilePolicy {

    /** * Caller: ChatAiController.construirInstrucaoFinal * Callee: N/A * Objetivo: Montar a diretiva textual do perfil de raciocinio atual. * Feature: Mesmo o perfil padrao passa a exigir confirmacao minima do * contexto do projeto antes de conclusao. * Data modificacao: 2026-05-24 00:00 * * @param perfilRaciocinio perfil atual vindo da UI * @param pedidoOriginal pedido atual do usuario * @return bloco textual para anexar ao prompt * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public String buildProfileDirective(String perfilRaciocinio, String pedidoOriginal) {
        String perfilNormalizado = normalizarPerfil(perfilRaciocinio);

        StringBuilder builder = new StringBuilder();
        builder.append("=== PERFIL DE RACIOCINIO DA SESSAO ===").append("\n");
        builder.append("perfil=").append(perfilNormalizado).append("\n");

        if (ChatRuntimeSettings.PERFIL_ULTRA.equals(perfilNormalizado)) {
            builder.append("REGRA: Esta sessao esta em perfil ULTRA_COMPLEXO. ").append("\n");
            builder.append("Voce deve investigar com profundidade acima do normal e nao pode concluir sem fechar a cadeia minima de execucao concreta.").append("\n");
            builder.append("Voce deve obrigatoriamente buscar o contexto do projeto e descer pelo menos um nivel abaixo do primeiro service concreto relevante.").append("\n");
            builder.append("Se houver persistencia, efeitos colaterais ou risco funcional, isso deve ser confirmado por ferramentas antes da resposta final.").append("\n");
            return builder.toString();
        }

        if (ChatRuntimeSettings.PERFIL_COMPLEXO.equals(perfilNormalizado)) {
            builder.append("REGRA: Esta sessao esta em perfil COMPLEXO. ").append("\n");
            builder.append("Voce deve obrigatoriamente confirmar contexto do projeto, impacto, chamadas diretas e efeitos colaterais antes de concluir pedidos de analise ou endurecimento.").append("\n");
            builder.append("Se houver service, contrato ou delegacao relevante, localize a implementacao concreta principal antes de concluir.").append("\n");
            return builder.toString();
        }

        builder.append("REGRA: Esta sessao esta em perfil NORMAL. ").append("\n");
        builder.append("Mesmo no perfil normal, voce deve confirmar pelo menos o contexto do projeto antes de concluir, usando memoria persistente ou inspecao de dependencias quando necessario.").append("\n");
        builder.append("Se houver pedido de alteracao, risco, impacto ou seguranca, confirme pelo menos o panorama de impacto antes de mutar.").append("\n");
        return builder.toString();
    }

    /** * Caller: buildProfileDirective * Callee: N/A * Objetivo: Garantir que o perfil usado pelo sistema seja sempre um dos * perfis conhecidos. * Data modificacao: 2026-05-24 00:00 * * @param perfilRaciocinio valor original * @return perfil normalizado * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public String normalizarPerfil(String perfilRaciocinio) {
        if (ChatRuntimeSettings.PERFIL_ULTRA.equals(perfilRaciocinio)) {
            return ChatRuntimeSettings.PERFIL_ULTRA;
        }

        if (ChatRuntimeSettings.PERFIL_COMPLEXO.equals(perfilRaciocinio)) {
            return ChatRuntimeSettings.PERFIL_COMPLEXO;
        }

        return ChatRuntimeSettings.PERFIL_PADRAO;
    }
}