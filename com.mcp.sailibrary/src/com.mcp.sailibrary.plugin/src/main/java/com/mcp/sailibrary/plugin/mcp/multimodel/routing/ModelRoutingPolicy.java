package com.mcp.sailibrary.plugin.mcp.multimodel.routing;

/** * --- * yaml_header: * version: "1.0" * dependencies: * - N/A * purpose: "Definir a politica de roteamento entre modelos por papel cognitivo." * design_pattern: "Strategy / Policy" * --- */
public interface ModelRoutingPolicy {

    /** * Caller: MultiModelCoordinator * Callee: Implementacao concreta da politica * Objetivo: Informar qual modelo sera usado como cerebro principal de raciocinio e investigacao. * Data modificacao: 2026-05-24 00:00 * * @return nome do modelo principal de raciocinio * * @author Renato Tomaz Nati * @since 2026-05-24 */
    String getReasonerModel();

    /** * Caller: MultiModelCoordinator * Callee: Implementacao concreta da politica * Objetivo: Informar qual modelo sera usado para escrita e refatoracao de codigo. * Data modificacao: 2026-05-24 00:00 * * @return nome do modelo de codigo * * @author Renato Tomaz Nati * @since 2026-05-24 */
    String getCodeGeneratorModel();

    /** * Caller: MultiModelCoordinator * Callee: Implementacao concreta da politica * Objetivo: Informar qual modelo sera usado para auditoria independente do codigo gerado. * Data modificacao: 2026-05-24 00:00 * * @return nome do modelo auditor * * @author Renato Tomaz Nati * @since 2026-05-24 */
    String getCodeAuditorModel();

    /** * Caller: MultiModelCoordinator * Callee: Implementacao concreta da politica * Objetivo: Limitar o numero de novas tentativas de correcao quando a auditoria reprovar o codigo gerado. * Data modificacao: 2026-05-24 00:00 * * @return quantidade maxima de tentativas de correcao * * @author Renato Tomaz Nati * @since 2026-05-24 */
    int getMaxRetryCount();
}