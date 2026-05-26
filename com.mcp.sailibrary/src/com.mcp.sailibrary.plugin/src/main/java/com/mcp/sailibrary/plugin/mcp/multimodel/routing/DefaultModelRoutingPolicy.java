package com.mcp.sailibrary.plugin.mcp.multimodel.routing;

/** * --- * yaml_header: * version: "1.0" * dependencies: * - N/A * purpose: "Fornecer a politica padrao de roteamento de modelos para o agente de engenharia." * design_pattern: "Concrete Policy" * --- */
public class DefaultModelRoutingPolicy implements ModelRoutingPolicy {

    private String reasonerModel;
    private String codeGeneratorModel;
    private String codeAuditorModel;
    private int maxRetryCount;

    /** * Caller: Bootstrapping da camada MCP * Callee: N/A * Objetivo: Inicializar a politica padrao com os modelos definidos como mais adequados para raciocinio, codigo e auditoria. * Feature: Mantem centralizada a decisao de nomes de modelos e limite de tentativas. * Data modificacao: 2026-05-24 00:00 * * @author Renato Tomaz Nati * @since 2026-05-24 */
    /** * Caller: bootstrapping do circuito multi-modelo * Callee: N/A * Objetivo: Inicializar a policy de fluxo com regras de tentativa e * reaplicacao, sem carregar responsabilidade de nomes de modelos. * Feature: Os nomes reais dos modelos passam a ser responsabilidade exclusiva * do ModelNameResolver. * Data modificacao: 2026-05-24 00:00 * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public DefaultModelRoutingPolicy() {
        this.maxRetryCount = 2;
    }
    @Override
    public String getReasonerModel() {
        return reasonerModel;
    }

    @Override
    public String getCodeGeneratorModel() {
        return codeGeneratorModel;
    }

    @Override
    public String getCodeAuditorModel() {
        return codeAuditorModel;
    }

    @Override
    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    /** * Caller: Futuras configuracoes administrativas * Callee: N/A * Objetivo: Permitir sobreposicao do modelo principal de raciocinio sem alterar a interface. * Data modificacao: 2026-05-24 00:00 * * @param reasonerModel nome do modelo principal * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public void setReasonerModel(String reasonerModel) {
        this.reasonerModel = safeTrim(reasonerModel);
    }

    /** * Caller: Futuras configuracoes administrativas * Callee: N/A * Objetivo: Permitir sobreposicao do modelo de codigo sem alterar a interface. * Data modificacao: 2026-05-24 00:00 * * @param codeGeneratorModel nome do modelo de codigo * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public void setCodeGeneratorModel(String codeGeneratorModel) {
        this.codeGeneratorModel = safeTrim(codeGeneratorModel);
    }

    /** * Caller: Futuras configuracoes administrativas * Callee: N/A * Objetivo: Permitir sobreposicao do modelo auditor sem alterar a interface. * Data modificacao: 2026-05-24 00:00 * * @param codeAuditorModel nome do modelo auditor * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public void setCodeAuditorModel(String codeAuditorModel) {
        this.codeAuditorModel = safeTrim(codeAuditorModel);
    }

    /** * Caller: Futuras configuracoes administrativas * Callee: N/A * Objetivo: Permitir ajuste do limite de tentativas de correcao. * Data modificacao: 2026-05-24 00:00 * * @param maxRetryCount quantidade maxima de tentativas * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public void setMaxRetryCount(int maxRetryCount) {
        if (maxRetryCount <= 0) {
            return;
        }
        this.maxRetryCount = maxRetryCount;
    }

    /** * Caller: Setters locais * Callee: N/A * Objetivo: Normalizar entradas de configuracao sem aceitar nulos. * Data modificacao: 2026-05-24 00:00 * * @param value valor de entrada * @return valor seguro * * @author Renato Tomaz Nati * @since 2026-05-24 */
    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}