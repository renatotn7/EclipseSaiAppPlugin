package com.mcp.sailibrary.plugin.mcp.multimodel.policy;

import com.mcp.sailibrary.plugin.chat.settings.ChatRuntimeSettings;

/* --- version: "1.1" libraries: - ChatRuntimeSettings objetivo: "Definir cobertura minima obrigatoria de investigacao por perfil e por intencao do pedido, sem contar ferramenta que falhou como cobertura satisfeita." --- */

/** * Politica de cobertura minima de investigacao. * * @author Renato Tomaz Nati * @since 2026-05-24 */
public class InvestigationCoveragePolicy {

    public static class CoveragePlan {

        private boolean requerContextoProjeto;
        private boolean requerImpacto;
        private boolean requerImplementacaoConcreta;
        private boolean requerCallees;
        private boolean requerEfeitosColaterais;
        private boolean requerQueries;
        private boolean requerUmNivelAbaixo;

        private boolean contextoProjetoResolvido;
        private boolean impactoResolvido;
        private boolean implementacaoConcretaResolvida;
        private boolean calleesResolvidos;
        private boolean efeitosColateraisResolvidos;
        private boolean queriesResolvidas;
        private boolean umNivelAbaixoResolvido;

        public boolean isRequerContextoProjeto() {
            return requerContextoProjeto;
        }

        public void setRequerContextoProjeto(boolean requerContextoProjeto) {
            this.requerContextoProjeto = requerContextoProjeto;
        }

        public boolean isRequerImpacto() {
            return requerImpacto;
        }

        public void setRequerImpacto(boolean requerImpacto) {
            this.requerImpacto = requerImpacto;
        }

        public boolean isRequerImplementacaoConcreta() {
            return requerImplementacaoConcreta;
        }

        public void setRequerImplementacaoConcreta(boolean requerImplementacaoConcreta) {
            this.requerImplementacaoConcreta = requerImplementacaoConcreta;
        }

        public boolean isRequerCallees() {
            return requerCallees;
        }

        public void setRequerCallees(boolean requerCallees) {
            this.requerCallees = requerCallees;
        }

        public boolean isRequerEfeitosColaterais() {
            return requerEfeitosColaterais;
        }

        public void setRequerEfeitosColaterais(boolean requerEfeitosColaterais) {
            this.requerEfeitosColaterais = requerEfeitosColaterais;
        }

        public boolean isRequerQueries() {
            return requerQueries;
        }

        public void setRequerQueries(boolean requerQueries) {
            this.requerQueries = requerQueries;
        }

        public boolean isRequerUmNivelAbaixo() {
            return requerUmNivelAbaixo;
        }

        public void setRequerUmNivelAbaixo(boolean requerUmNivelAbaixo) {
            this.requerUmNivelAbaixo = requerUmNivelAbaixo;
        }

        public boolean isContextoProjetoResolvido() {
            return contextoProjetoResolvido;
        }

        public void setContextoProjetoResolvido(boolean contextoProjetoResolvido) {
            this.contextoProjetoResolvido = contextoProjetoResolvido;
        }

        public boolean isImpactoResolvido() {
            return impactoResolvido;
        }

        public void setImpactoResolvido(boolean impactoResolvido) {
            this.impactoResolvido = impactoResolvido;
        }

        public boolean isImplementacaoConcretaResolvida() {
            return implementacaoConcretaResolvida;
        }

        public void setImplementacaoConcretaResolvida(boolean implementacaoConcretaResolvida) {
            this.implementacaoConcretaResolvida = implementacaoConcretaResolvida;
        }

        public boolean isCalleesResolvidos() {
            return calleesResolvidos;
        }

        public void setCalleesResolvidos(boolean calleesResolvidos) {
            this.calleesResolvidos = calleesResolvidos;
        }

        public boolean isEfeitosColateraisResolvidos() {
            return efeitosColateraisResolvidos;
        }

        public void setEfeitosColateraisResolvidos(boolean efeitosColateraisResolvidos) {
            this.efeitosColateraisResolvidos = efeitosColateraisResolvidos;
        }

        public boolean isQueriesResolvidas() {
            return queriesResolvidas;
        }

        public void setQueriesResolvidas(boolean queriesResolvidas) {
            this.queriesResolvidas = queriesResolvidas;
        }

        public boolean isUmNivelAbaixoResolvido() {
            return umNivelAbaixoResolvido;
        }

        public void setUmNivelAbaixoResolvido(boolean umNivelAbaixoResolvido) {
            this.umNivelAbaixoResolvido = umNivelAbaixoResolvido;
        }

        public boolean isActive() {
            return requerContextoProjeto
                    || requerImpacto
                    || requerImplementacaoConcreta
                    || requerCallees
                    || requerEfeitosColaterais
                    || requerQueries
                    || requerUmNivelAbaixo;
        }

    }

    public CoveragePlan createPlan(String perfilRaciocinio, String pedidoOriginal) {
        CoveragePlan plan = new CoveragePlan();
        String perfilNormalizado = normalizarPerfil(perfilRaciocinio);
        String pedido = pedidoOriginal != null ? pedidoOriginal.toLowerCase() : "";

        plan.setRequerContextoProjeto(true);

        
        boolean pedidoSomenteValidacaoSemMutacao = contemTermo(pedido, "validacao")
                && (contemTermo(pedido, "sem alterar")
                    || contemTermo(pedido, "nao altere")
                    || contemTermo(pedido, "nao alterar")
                    || contemTermo(pedido, "sem mutacao")
                    || contemTermo(pedido, "nao crie")
                    || contemTermo(pedido, "nao apague"));
        
        boolean pedidoMutacaoOuSeguranca = contemTermo(pedido, "seguranca")
                || contemTermo(pedido, "ajusta")
                || contemTermo(pedido, "altera")
                || contemTermo(pedido, "endure")
                || contemTermo(pedido, "proteger")
                || contemTermo(pedido, "implementar");

        boolean pedidoCallees = contemTermo(pedido, "o que ele chama")
                || contemTermo(pedido, "quem ele chama")
                || contemTermo(pedido, "proteger quem ele chama")
                || contemTermo(pedido, "fluxo")
                || contemTermo(pedido, "cadeia");

        boolean pedidoPersistencia = contemTermo(pedido, "query")
                || contemTermo(pedido, "jdbc")
                || contemTermo(pedido, "xml")
                || contemTermo(pedido, "persistencia")
                || contemTermo(pedido, "dao")
                || contemTermo(pedido, "banco");

        if (ChatRuntimeSettings.PERFIL_PADRAO.equals(perfilNormalizado)) {
        	 if (pedidoMutacaoOuSeguranca && !pedidoSomenteValidacaoSemMutacao) {
                plan.setRequerImpacto(true);
            }
            return plan;
        }

        if (ChatRuntimeSettings.PERFIL_COMPLEXO.equals(perfilNormalizado)) {
            plan.setRequerImpacto(true);
            plan.setRequerCallees(true);
            plan.setRequerImplementacaoConcreta(true);
            plan.setRequerEfeitosColaterais(true);

            if (pedidoPersistencia || pedidoMutacaoOuSeguranca) {
                plan.setRequerQueries(true);
            }

            return plan;
        }

        plan.setRequerImpacto(true);
        plan.setRequerImplementacaoConcreta(true);
        plan.setRequerCallees(true);
        plan.setRequerEfeitosColaterais(true);
        plan.setRequerQueries(true);
        plan.setRequerUmNivelAbaixo(true);

        return plan;
    }

    public void registrarFonteDeProjetoViaMemoria(CoveragePlan plan, String resumoMemoriaProjeto) {
        if (plan == null) {
            return;
        }

        if (resumoMemoriaProjeto != null && resumoMemoriaProjeto.trim().length() > 0) {
            plan.setContextoProjetoResolvido(true);
        }
    }

    public void registrarUsoFerramenta(CoveragePlan plan, String toolName, String toolResult) {
        if (plan == null || toolName == null) {
            return;
        }

        if (resultadoIndicaFalha(toolResult)) {
            System.out.println("[COVERAGE DEBUG] Ferramenta [" + toolName + "] falhou. Nao sera contabilizada como cobertura satisfeita.");
            return;
        }

        registrarCoberturaPorFerramenta(plan, toolName);
    }

    private void registrarCoberturaPorFerramenta(CoveragePlan plan, String toolName) {
        if ("consultar_memoria_projeto".equals(toolName)
                || "inspecionar_dependencias_projeto".equals(toolName)
                || "verificar_raiz_projeto".equals(toolName)
                || "registrar_memoria_projeto".equals(toolName)) {
            plan.setContextoProjetoResolvido(true);
        }

        if ("resumir_impacto_alteracao".equals(toolName)) {
            plan.setImpactoResolvido(true);
        }

        if ("buscar_implementacoes_tipo".equals(toolName) || "buscar_contexto_jdt".equals(toolName)) {
            plan.setImplementacaoConcretaResolvida(true);
        }

        if ("buscar_callees_jdt".equals(toolName)) {
            plan.setCalleesResolvidos(true);
        }

        if ("inspecionar_efeitos_colaterais".equals(toolName)) {
            plan.setEfeitosColateraisResolvidos(true);
        }

        if ("extrair_queries_trecho".equals(toolName)) {
            plan.setQueriesResolvidas(true);
        }

        if ("ler_conteudo_arquivo".equals(toolName)
                || "leitura_cirurgica_jdt".equals(toolName)
                || "buscar_contexto_jdt".equals(toolName)) {
            plan.setUmNivelAbaixoResolvido(true);
        }
    }

    private boolean resultadoIndicaFalha(String toolResult) {
        String texto = toolResult != null ? toolResult.trim().toLowerCase() : "";

        if (texto.length() == 0) {
            return true;
        }

        if (texto.startsWith("erro operacional:")) {
            return true;
        }

        if (texto.startsWith("erro:")) {
            return true;
        }

        if (texto.startsWith("[erro]")) {
            return true;
        }

        if (texto.startsWith("falha operacional:")) {
            return true;
        }

        if (texto.startsWith("falha tecnica:")) {
            return true;
        }

        if (texto.contains("erro operacional:")) {
            return true;
        }

        if (texto.contains("erro ao executar ferramenta")) {
            return true;
        }

        if (texto.contains("falha ao executar ferramenta")) {
            return true;
        }

        if (texto.contains("tool execution failed")) {
            return true;
        }

        if (texto.contains("access denied")) {
            return true;
        }

        if (texto.contains("permission denied")) {
            return true;
        }

        if (texto.contains("connection refused")) {
            return true;
        }

        if (texto.contains("connection reset")) {
            return true;
        }

        if (texto.contains("read timed out")) {
            return true;
        }

        if (texto.contains("socket timeout")) {
            return true;
        }

        return false;
    }

    public boolean podeConcluir(CoveragePlan plan) {
        if (plan == null) {
            return true;
        }

        if (plan.isRequerContextoProjeto() && !plan.isContextoProjetoResolvido()) {
            return false;
        }

        if (plan.isRequerImpacto() && !plan.isImpactoResolvido()) {
            return false;
        }

        if (plan.isRequerImplementacaoConcreta() && !plan.isImplementacaoConcretaResolvida()) {
            return false;
        }

        if (plan.isRequerCallees() && !plan.isCalleesResolvidos()) {
            return false;
        }

        if (plan.isRequerEfeitosColaterais() && !plan.isEfeitosColateraisResolvidos()) {
            return false;
        }

        if (plan.isRequerQueries() && !plan.isQueriesResolvidas()) {
            return false;
        }

        if (plan.isRequerUmNivelAbaixo() && !plan.isUmNivelAbaixoResolvido()) {
            return false;
        }

        return true;
    }

    public String buildPendenciasMensagem(CoveragePlan plan) {
        if (plan == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        if (plan.isRequerContextoProjeto() && !plan.isContextoProjetoResolvido()) {
            builder.append("\n- contexto do projeto");
        }
        if (plan.isRequerImpacto() && !plan.isImpactoResolvido()) {
            builder.append("\n- panorama de impacto");
        }
        if (plan.isRequerImplementacaoConcreta() && !plan.isImplementacaoConcretaResolvida()) {
            builder.append("\n- implementacao concreta principal");
        }
        if (plan.isRequerCallees() && !plan.isCalleesResolvidos()) {
            builder.append("\n- chamadas diretas confirmadas");
        }
        if (plan.isRequerEfeitosColaterais() && !plan.isEfeitosColateraisResolvidos()) {
            builder.append("\n- efeitos colaterais confirmados");
        }
        if (plan.isRequerQueries() && !plan.isQueriesResolvidas()) {
            builder.append("\n- persistencia ou queries confirmadas");
        }
        if (plan.isRequerUmNivelAbaixo() && !plan.isUmNivelAbaixoResolvido()) {
            builder.append("\n- descida concreta de um nivel abaixo do primeiro service relevante");
        }

        if (builder.length() == 0) {
            return "";
        }

        return "Ainda nao pode concluir. Faltam evidencias obrigatorias:" + builder.toString();
    }

    public String buildPromptDirective(CoveragePlan plan) {
        if (plan == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("=== COBERTURA MINIMA DE INVESTIGACAO ===").append("\n");

        if (plan.isRequerContextoProjeto()) {
            builder.append("- confirmar contexto do projeto por memoria persistente ou inspecao de dependencias").append("\n");
        }
        if (plan.isRequerImpacto()) {
            builder.append("- obter panorama de impacto antes de concluir").append("\n");
        }
        if (plan.isRequerImplementacaoConcreta()) {
            builder.append("- localizar a implementacao concreta principal do contrato ou service relevante").append("\n");
        }
        if (plan.isRequerCallees()) {
            builder.append("- confirmar o que o metodo chama diretamente").append("\n");
        }
        if (plan.isRequerEfeitosColaterais()) {
            builder.append("- confirmar efeitos colaterais reais").append("\n");
        }
        if (plan.isRequerQueries()) {
            builder.append("- confirmar evidencias de persistencia, queries, JDBC ou XML quando houver risco ou delegacao para camada de dados").append("\n");
        }
        if (plan.isRequerUmNivelAbaixo()) {
            builder.append("- descer pelo menos um nivel abaixo do primeiro service concreto relevante antes de concluir").append("\n");
        }

        builder.append("Nao conclua a analise enquanto essas exigencias minimas nao forem satisfeitas.").append("\n");
        return builder.toString();
    }


    private boolean contemTermo(String texto, String termo) {
        return texto != null && texto.contains(termo);
    }

    private String normalizarPerfil(String perfilRaciocinio) {
        if (ChatRuntimeSettings.PERFIL_ULTRA.equals(perfilRaciocinio)) {
            return ChatRuntimeSettings.PERFIL_ULTRA;
        }

        if (ChatRuntimeSettings.PERFIL_COMPLEXO.equals(perfilRaciocinio)) {
            return ChatRuntimeSettings.PERFIL_COMPLEXO;
        }

        return ChatRuntimeSettings.PERFIL_PADRAO;
    }
}