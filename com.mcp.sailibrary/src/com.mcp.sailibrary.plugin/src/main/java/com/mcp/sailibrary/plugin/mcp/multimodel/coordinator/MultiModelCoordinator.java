package com.mcp.sailibrary.plugin.mcp.multimodel.coordinator;

import com.mcp.sailibrary.plugin.chat.support.AiResponse;
import com.mcp.sailibrary.plugin.mcp.multimodel.audit.AuditExecutionStatus;
import com.mcp.sailibrary.plugin.mcp.multimodel.audit.ClaudeCodeAuditService;
import com.mcp.sailibrary.plugin.mcp.multimodel.audit.CodeAuditResult;
import com.mcp.sailibrary.plugin.mcp.multimodel.audit.CodeAuditService;
import com.mcp.sailibrary.plugin.mcp.multimodel.audit.NoOpCodeAuditService;
import com.mcp.sailibrary.plugin.mcp.multimodel.generation.CodeGenerationService;
import com.mcp.sailibrary.plugin.mcp.multimodel.generation.CodexCodeGenerationService;
import com.mcp.sailibrary.plugin.mcp.multimodel.policy.DefaultFinalApplicationDecisionPolicy;
import com.mcp.sailibrary.plugin.mcp.multimodel.policy.FinalApplicationDecision;
import com.mcp.sailibrary.plugin.mcp.multimodel.routing.DefaultModelRoutingPolicy;
import com.mcp.sailibrary.plugin.mcp.multimodel.routing.ModelRoutingPolicy;

/* --- version: "1.5" libraries: - AiResponse - CodeAuditResult - CodeAuditService - NoOpCodeAuditService - ClaudeCodeAuditService - CodeGenerationService - CodexCodeGenerationService - DefaultModelRoutingPolicy - ModelRoutingPolicy - DefaultFinalApplicationDecisionPolicy - FinalApplicationDecision objetivo: "Coordenar multi-modelo real separando reprovacao de codigo de falha tecnica do auditor e decidindo corretamente quando confirmar com o usuario." --- */

/** * Coordenador multi-modelo do agente. * * @author Renato Tomaz Nati * @since 2026-05-24 */
public class MultiModelCoordinator implements AgentModelCoordinator {

    private ModelRoutingPolicy politicaRoteamentoModelos;
    private AgentModelCoordinator coordenadorSingleModel;
    private CodeGenerationService servicoGeracaoCodigo;
    private CodeAuditService servicoAuditoriaCodigo;
    private DefaultFinalApplicationDecisionPolicy finalApplicationDecisionPolicy;

    public MultiModelCoordinator() {
        this(
                new DefaultModelRoutingPolicy(),
                new SingleModelCoordinator(),
                new CodexCodeGenerationService(),
                new ClaudeCodeAuditService(),
                new DefaultFinalApplicationDecisionPolicy()
        );
    }

    public MultiModelCoordinator(ModelRoutingPolicy politicaRoteamentoModelos, AgentModelCoordinator coordenadorSingleModel, CodeGenerationService servicoGeracaoCodigo, CodeAuditService servicoAuditoriaCodigo, DefaultFinalApplicationDecisionPolicy finalApplicationDecisionPolicy) {
        this.politicaRoteamentoModelos = politicaRoteamentoModelos;
        this.coordenadorSingleModel = coordenadorSingleModel;
        this.servicoGeracaoCodigo = servicoGeracaoCodigo;
        this.servicoAuditoriaCodigo = servicoAuditoriaCodigo != null ? servicoAuditoriaCodigo : new NoOpCodeAuditService();
        this.finalApplicationDecisionPolicy = finalApplicationDecisionPolicy != null ? finalApplicationDecisionPolicy : new DefaultFinalApplicationDecisionPolicy();
    }

    @Override
    public AiResponse executarMissao(String selectedCode, String fullFileText, String instrucao, String apiKey) throws Exception {
        AiResponse respostaInicial = executarFluxoSingleModel(selectedCode, fullFileText, instrucao, apiKey);

        if (!deveUsarFluxoMultiModelo(respostaInicial)) {
            return respostaInicial;
        }

        if (servicoGeracaoCodigo == null || servicoAuditoriaCodigo == null) {
            return respostaInicial;
        }

        return executarFluxoCodigoAuditado(
                respostaInicial,
                selectedCode,
                fullFileText,
                instrucao,
                apiKey
        );
    }

    private AiResponse executarFluxoSingleModel(String selectedCode, String fullFileText, String instrucao, String apiKey) throws Exception {
        return coordenadorSingleModel.executarMissao(selectedCode, fullFileText, instrucao, apiKey);
    }

    private AiResponse executarFluxoCodigoAuditado(AiResponse respostaInicial, String selectedCode, String fullFileText, String instrucao, String apiKey) throws Exception {

        String pedidoOriginal = instrucao != null ? instrucao : "";
        String planoImplementacao = montarPlanoImplementacao(respostaInicial);
        String acaoEsperada = safeTrim(respostaInicial.getAction());

        AiResponse respostaGerada = servicoGeracaoCodigo.gerarCodigo(
                pedidoOriginal,
                planoImplementacao,
                selectedCode,
                fullFileText,
                acaoEsperada,
                apiKey
        );

        if (respostaGerada == null) {
            return respostaInicial;
        }

        if (!pareceAcaoDeMutacao(respostaGerada.getAction())) {
            return respostaGerada;
        }

        AiResponse respostaCandidata = respostaGerada;
        int maximoTentativas = politicaRoteamentoModelos != null ? politicaRoteamentoModelos.getMaxRetryCount() : 1;
        int tentativaAtual = 0;

        while (tentativaAtual < maximoTentativas) {
            CodeAuditResult resultadoAuditoria = servicoAuditoriaCodigo.auditarCodigo(
                    pedidoOriginal,
                    planoImplementacao,
                    respostaCandidata.getContent(),
                    acaoEsperada,
                    apiKey
            );

            System.out.println("[MULTI MODEL DEBUG] resultadoAuditoria.status="
                    + (resultadoAuditoria != null ? resultadoAuditoria.getExecutionStatus() : "null"));

            if (resultadoAuditoria == null) {
                FinalApplicationDecision decisaoSemResultado = finalApplicationDecisionPolicy.decide(null, pedidoOriginal);

                if (FinalApplicationDecision.APLICAR_COM_CONFIRMACAO.equals(decisaoSemResultado)) {
                    return montarPerguntaConfirmacaoSemAuditoria(
                            "A auditoria final nao retornou resultado utilizavel. Deseja aplicar mesmo assim e validar pelo workspace Eclipse?"
                    );
                }

                return montarRespostaBloqueioAuditoria(
                        "A implementacao foi bloqueada porque a auditoria final nao retornou resultado utilizavel e nao houve autorizacao explicita para seguir sem auditoria."
                );
            }

            FinalApplicationDecision decisaoFinal = finalApplicationDecisionPolicy.decide(resultadoAuditoria, pedidoOriginal);

            if (AuditExecutionStatus.APROVADO.equals(resultadoAuditoria.getExecutionStatus())
                    && resultadoAuditoria.isAprovadoRealmente()) {

                if (FinalApplicationDecision.APLICAR_COM_CONFIRMACAO.equals(decisaoFinal)) {
                    return montarPerguntaConfirmacaoSemAuditoria(
                            finalApplicationDecisionPolicy.buildUserConfirmationQuestion(resultadoAuditoria)
                    );
                }

                anexarFeedbackDeAuditoriaNaExplicacao(respostaCandidata, resultadoAuditoria);
                return respostaCandidata;
            }

            if (AuditExecutionStatus.FALHA_INFRA.equals(resultadoAuditoria.getExecutionStatus())) {
                if (FinalApplicationDecision.APLICAR_COM_CONFIRMACAO.equals(decisaoFinal)) {
                    return montarPerguntaConfirmacaoSemAuditoria(
                            finalApplicationDecisionPolicy.buildUserConfirmationQuestion(resultadoAuditoria)
                    );
                }

                return montarRespostaBloqueioAuditoria(
                        finalApplicationDecisionPolicy.buildBlockingMessage(resultadoAuditoria)
                );
            }

            if (AuditExecutionStatus.REPROVADO.equals(resultadoAuditoria.getExecutionStatus())) {
                if (!resultadoAuditoria.isDeveTentarNovamente()) {
                    return montarRespostaBloqueioAuditoria(
                            finalApplicationDecisionPolicy.buildBlockingMessage(resultadoAuditoria)
                    );
                }

                String planoCorrigido = planoImplementacao
                        + "\n\n=== FEEDBACK DE AUDITORIA ===\n"
                        + resultadoAuditoria.getFeedback()
                        + "\n=== FIM DO FEEDBACK DE AUDITORIA ===\n";

                respostaCandidata = servicoGeracaoCodigo.gerarCodigo(
                        pedidoOriginal,
                        planoCorrigido,
                        selectedCode,
                        fullFileText,
                        acaoEsperada,
                        apiKey
                );

                if (respostaCandidata == null) {
                    return respostaInicial;
                }

                tentativaAtual++;
                continue;
            }

            return montarPerguntaConfirmacaoSemAuditoria(
                    "A auditoria ficou em estado indefinido. Deseja aplicar mesmo assim e validar pelo workspace Eclipse?"
            );
        }

        return montarPerguntaConfirmacaoSemAuditoria(
                "A auditoria nao convergiu dentro do limite de tentativas. Deseja aplicar mesmo assim e validar pelo workspace Eclipse?"
        );
    }

    private AiResponse montarPerguntaConfirmacaoSemAuditoria(String pergunta) {
        AiResponse resposta = new AiResponse();
        resposta.setAction("perguntar_ao_usuario");
        resposta.setQuestion(pergunta);
        resposta.setExplanation("Auditoria final indisponivel, inconclusiva ou dependente de confirmacao do usuario antes de aplicar.");
        return resposta;
    }

    private AiResponse montarRespostaBloqueioAuditoria(String mensagem) {
        AiResponse resposta = new AiResponse();
        resposta.setAction("responder_ao_usuario");
        resposta.setContent(mensagem);
        resposta.setExplanation("Implementacao bloqueada por reprovacao real da auditoria ou por ausencia de autorizacao para seguir sem auditoria valida.");
        return resposta;
    }

    private boolean deveUsarFluxoMultiModelo(AiResponse respostaInicial) {
        if (respostaInicial == null) {
            return false;
        }

        return pareceAcaoDeMutacao(respostaInicial.getAction());
    }

    private String montarPlanoImplementacao(AiResponse respostaInicial) {
        if (respostaInicial == null) {
            return "";
        }

        StringBuilder plano = new StringBuilder();

        if (!isBlank(respostaInicial.getExplanation())) {
            plano.append("EXPLICACAO DO REASONER: ").append(respostaInicial.getExplanation()).append("\n");
        }

        if (!isBlank(respostaInicial.getContent())) {
            plano.append("RASCUNHO OU CONTEUDO BASE: ").append("\n");
            plano.append(respostaInicial.getContent()).append("\n");
        }

        return plano.toString().trim();
    }

    private void anexarFeedbackDeAuditoriaNaExplicacao(AiResponse respostaCandidata, CodeAuditResult resultadoAuditoria) {
        if (respostaCandidata == null || resultadoAuditoria == null) {
            return;
        }

        String explicacaoAtual = safeTrim(respostaCandidata.getExplanation());
        StringBuilder novaExplicacao = new StringBuilder();

        if (explicacaoAtual.length() > 0) {
            novaExplicacao.append(explicacaoAtual).append("\n");
        }

        novaExplicacao.append("AUDITORIA: codigo aprovado");

        if (!isBlank(resultadoAuditoria.getNivelRisco())) {
            novaExplicacao.append(" | risco=").append(resultadoAuditoria.getNivelRisco());
        }

        if (resultadoAuditoria.getObservationLevel() != null) {
            novaExplicacao.append(" | observacao=").append(resultadoAuditoria.getObservationLevel().name());
        }

        if (!isBlank(resultadoAuditoria.getFeedback())) {
            novaExplicacao.append(" | feedback=").append(resultadoAuditoria.getFeedback());
        }

        respostaCandidata.setExplanation(novaExplicacao.toString().trim());
    }

    private boolean pareceAcaoDeMutacao(String action) {
        if (action == null) {
            return false;
        }

        return "substituir".equalsIgnoreCase(action)
                || "inserir_abaixo".equalsIgnoreCase(action)
                || "anexar_acima".equalsIgnoreCase(action)
                || "comentar".equalsIgnoreCase(action);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    public ModelRoutingPolicy getModelRoutingPolicy() {
        return politicaRoteamentoModelos;
    }
}