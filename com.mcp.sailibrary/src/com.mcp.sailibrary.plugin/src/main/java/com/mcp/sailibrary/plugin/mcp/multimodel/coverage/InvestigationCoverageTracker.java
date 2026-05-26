package com.mcp.sailibrary.plugin.mcp.multimodel.coverage;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/* --- version: "1.0" libraries: - ArrayList - LinkedHashSet - List - Set objetivo: "Rastrear a cobertura investigativa real de uma missao com base nas ferramentas efetivamente executadas." --- */

/** * Rastreia a cobertura investigativa efetivamente atingida durante uma missao. * * <p>Esta classe observa quais ferramentas foram executadas e registra se a * investigacao desceu para implementacoes concretas e metodos concretos.</p> * * @author Renato Tomaz Nati * @since 2026-05-24 */
public class InvestigationCoverageTracker {

    private Set<String> ferramentasExecutadas;
    private boolean implementacaoConcretaLocalizada;
    private boolean metodoConcretoLido;
    private boolean arquivoConcretoLido;

    /** * Caller: ChatAiController * Callee: N/A * Objetivo: Inicializar o rastreador de cobertura. * Data modificacao: 2026-05-24 00:00 * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public InvestigationCoverageTracker() {
        this.ferramentasExecutadas = new LinkedHashSet<String>();
        this.implementacaoConcretaLocalizada = false;
        this.metodoConcretoLido = false;
        this.arquivoConcretoLido = false;
    }

    /** * Caller: ChatAiController * Callee: N/A * Objetivo: Registrar uma ferramenta executada. * Data modificacao: 2026-05-24 00:00 * * @param nomeFerramenta nome da ferramenta * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public void registrarFerramenta(String nomeFerramenta) {
        if (nomeFerramenta == null || nomeFerramenta.trim().length() == 0) {
            return;
        }

        ferramentasExecutadas.add(nomeFerramenta.trim());
    }

    /** * Caller: ChatAiController * Callee: registrarFerramenta * Objetivo: Registrar a ferramenta e inferir marcos de cobertura concreta. * Data modificacao: 2026-05-24 00:00 * * @param nomeFerramenta nome da ferramenta * @param parametrosFerramenta parametros usados * @param resultadoFerramenta resultado bruto da ferramenta * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public void registrarFerramentaComEvidencia(String nomeFerramenta, String parametrosFerramenta, String resultadoFerramenta) {
        registrarFerramenta(nomeFerramenta);

        String parametros = parametrosFerramenta != null ? parametrosFerramenta : "";
        String resultado = resultadoFerramenta != null ? resultadoFerramenta : "";

        if ("buscar_implementacoes_tipo".equals(nomeFerramenta) && resultado.contains("Implementacao localizada em:")) {
            implementacaoConcretaLocalizada = true;
        }

        if ("leitura_cirurgica_jdt".equals(nomeFerramenta) && parametros.contains("\"modo\":\"metodo\"")) {
            metodoConcretoLido = true;
        }

        if ("ler_conteudo_arquivo".equals(nomeFerramenta) && resultado.trim().length() > 0) {
            arquivoConcretoLido = true;
        }
    }

    /** * Caller: DefaultInvestigationCoveragePolicy * Callee: N/A * Objetivo: Informar se a ferramenta ja foi executada nesta missao. * Data modificacao: 2026-05-24 00:00 * * @param nomeFerramenta nome da ferramenta * @return true quando a ferramenta ja tiver sido usada * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public boolean foiExecutada(String nomeFerramenta) {
        if (nomeFerramenta == null || nomeFerramenta.trim().length() == 0) {
            return false;
        }

        return ferramentasExecutadas.contains(nomeFerramenta.trim());
    }

    /** * Caller: DefaultInvestigationCoveragePolicy * Callee: N/A * Objetivo: Informar se a investigacao ja desceu para implementacao concreta. * Data modificacao: 2026-05-24 00:00 * * @return true quando houver evidencia de descida concreta * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public boolean isDescidaConcretaConfirmada() {
        return implementacaoConcretaLocalizada && (metodoConcretoLido || arquivoConcretoLido);
    }

    /** * Caller: ChatAiController, DefaultInvestigationCoveragePolicy * Callee: N/A * Objetivo: Devolver as ferramentas executadas em ordem de registro. * Data modificacao: 2026-05-24 00:00 * * @return lista de ferramentas executadas * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public List<String> getFerramentasExecutadas() {
        return new ArrayList<String>(ferramentasExecutadas);
    }

    /** * Caller: ChatAiController * Callee: N/A * Objetivo: Montar resumo textual curto da cobertura atual. * Data modificacao: 2026-05-24 00:00 * * @return resumo textual * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public String toCoverageSummary() {
        StringBuilder builder = new StringBuilder();
        builder.append("ferramentas=").append(getFerramentasExecutadas());
        builder.append(" | implementacaoConcretaLocalizada=").append(implementacaoConcretaLocalizada);
        builder.append(" | metodoConcretoLido=").append(metodoConcretoLido);
        builder.append(" | arquivoConcretoLido=").append(arquivoConcretoLido);
        return builder.toString();
    }
}