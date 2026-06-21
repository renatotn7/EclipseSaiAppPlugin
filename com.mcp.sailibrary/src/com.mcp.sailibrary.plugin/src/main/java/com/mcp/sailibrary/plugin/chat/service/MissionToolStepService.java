package com.mcp.sailibrary.plugin.chat.service;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mcp.sailibrary.plugin.agent.context.analise.ProjectMemoryStore;
import com.mcp.sailibrary.plugin.agent.orchestration.AgentOrchestrator;
import com.mcp.sailibrary.plugin.chat.support.MissionExecutionContext;
import com.mcp.sailibrary.plugin.chat.support.ToolResultSummarizer;
import com.mcp.sailibrary.plugin.mcp.multimodel.policy.InvestigationCoveragePolicy;
import com.mcp.sailibrary.plugin.mcp.multimodel.policy.InvestigationCoveragePolicy.CoveragePlan;

/* class_context: feature: "tool execution step" java_version: "21" objective: "Executar ferramentas do agente com fallback seguro e rastreabilidade curta." libs: - "Gson" - "JDK reflection" */
public class MissionToolStepService {

    private static final int MAX_RAW_RESULT_FOR_PROMPT = 12000;
    private static final int MAX_RAW_RESULT_FOR_CHAT = 4000;

    private final Gson gson;

    public MissionToolStepService() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /* * Feature: executa uma ferramenta solicitada pela IA. * Data: 2026-05-27 14:20 * Chamado por: * - ChatAiController.executarMissaoIA * Chama: * - invocarFerramenta * - resumirResultadoParaChat * - registrarUltimoResultadoNoContexto * Objetivo: * - executar a ferramenta pelo AgentOrchestrator * - registrar cobertura * - devolver resultado bruto e resumido */
    public ToolStepResult executarFerramenta( String nomeFerramenta, String parametrosFerramenta, AgentOrchestrator orquestrador, ToolResultSummarizer toolResultPresenter, ProjectMemoryStore projectMemoryStore, SessionHistoryService sessionHistoryService, InvestigationCoveragePolicy investigationCoveragePolicy, CoveragePlan coveragePlan, MissionExecutionContext contextoMissao) {

        String toolName = safeTrim(nomeFerramenta);
        String toolParameters = safeTrim(parametrosFerramenta);

        System.out.println("[TOOL STEP DEBUG] ===============================================");
        System.out.println("[TOOL STEP DEBUG] toolName=" + toolName);
        System.out.println("[TOOL STEP DEBUG] parameters=" + toolParameters);

        if (toolName.length() == 0) {
            return ToolStepResult.falha("Falha operacional: nome da ferramenta vazio.");
        }

        if (orquestrador == null) {
            return ToolStepResult.falha("Falha operacional: orquestrador de ferramentas indisponivel.");
        }

        try {
            Object retornoBruto = invocarFerramenta(orquestrador, toolName, toolParameters);
            String resultadoBruto = sanitizarResultadoBruto(converterResultadoParaTexto(retornoBruto));
            String resultadoParaChat = resumirResultadoParaChat(toolName, resultadoBruto, toolResultPresenter);

            if (investigationCoveragePolicy != null && coveragePlan != null) {
                investigationCoveragePolicy.registrarUsoFerramenta(coveragePlan, toolName, resultadoBruto);
            }

            if (sessionHistoryService != null) {
                sessionHistoryService.adicionar("[Ferramenta - " + toolName + "]: " + truncate(resultadoParaChat, 2000));
            }

            registrarUltimoResultadoNoContexto(contextoMissao, toolName, resultadoParaChat, resultadoBruto);

            System.out.println("[TOOL STEP DEBUG] resultadoBrutoLength=" + resultadoBruto.length());
            System.out.println("[TOOL STEP DEBUG] resultadoParaChatLength=" + resultadoParaChat.length());
            System.out.println("[TOOL STEP DEBUG] status=sucesso");
            System.out.println("[TOOL STEP DEBUG] ===============================================");

            return ToolStepResult.sucesso(toolName, resultadoParaChat, resultadoBruto);
        } catch (Exception e) {
            String mensagemErro = "Falha ao executar ferramenta [" + toolName + "]: "
                    + e.getClass().getSimpleName()
                    + " - "
                    + safeTrim(e.getMessage());

            System.out.println("[TOOL STEP DEBUG] status=falha");
            System.out.println("[TOOL STEP DEBUG] erro=" + mensagemErro);
            System.out.println("[TOOL STEP DEBUG] ===============================================");

            return ToolStepResult.falha(mensagemErro);
        }
    }

    /* * Feature: chama o AgentOrchestrator com compatibilidade para nomes legados. * Data: 2026-05-27 14:20 * Chamado por: * - executarFerramenta * Chama: * - tryInvoke * Objetivo: * - priorizar dispatch(String, String) * - manter fallback para assinaturas antigas */
    private Object invocarFerramenta(AgentOrchestrator orquestrador, String toolName, String toolParameters) throws Exception {
        Object retorno = tryInvoke(orquestrador, "dispatch",
                new Class<?>[] { String.class, String.class },
                new Object[] { toolName, toolParameters });

        if (retorno != null) {
            System.out.println("[TOOL STEP DEBUG] executor=AgentOrchestrator.dispatch");
            return retorno;
        }

        retorno = tryInvoke(orquestrador, "executarFerramenta",
                new Class<?>[] { String.class, String.class },
                new Object[] { toolName, toolParameters });

        if (retorno != null) {
            System.out.println("[TOOL STEP DEBUG] executor=AgentOrchestrator.executarFerramenta");
            return retorno;
        }

        retorno = tryInvoke(orquestrador, "executeTool",
                new Class<?>[] { String.class, String.class },
                new Object[] { toolName, toolParameters });

        if (retorno != null) {
            System.out.println("[TOOL STEP DEBUG] executor=AgentOrchestrator.executeTool");
            return retorno;
        }

        retorno = tryInvoke(orquestrador, "runTool",
                new Class<?>[] { String.class, String.class },
                new Object[] { toolName, toolParameters });

        if (retorno != null) {
            System.out.println("[TOOL STEP DEBUG] executor=AgentOrchestrator.runTool");
            return retorno;
        }

        throw new IllegalStateException("Nenhum metodo compativel de execucao de ferramenta foi encontrado no AgentOrchestrator. Esperado: dispatch(String,String).");
    }

    /* * Feature: tenta invocar metodo publico por reflection. * Data: 2026-05-27 14:20 * Chamado por: * - invocarFerramenta * Chama: * - Method.invoke * Objetivo: * - permitir compatibilidade sem acoplamento rigido */
    private Object tryInvoke(Object target, String methodName, Class<?>[] parameterTypes, Object[] args) {
        if (target == null || methodName == null) {
            return null;
        }

        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            return method.invoke(target, args);
        } catch (Exception e) {
            return null;
        }
    }

    /* * Feature: resume resultado para exibicao no chat. * Data: 2026-05-27 14:20 * Chamado por: * - executarFerramenta * Chama: * - tentarResumidorExterno * - truncate * Objetivo: * - reduzir ruido no chat sem perder resultado bruto no contexto */
    private String resumirResultadoParaChat(String toolName, String resultadoBruto, ToolResultSummarizer toolResultPresenter) {
        String resumo = tentarResumidorExterno(toolResultPresenter, toolName, resultadoBruto);
        if (resumo != null && resumo.trim().length() > 0) {
            return truncate(resumo, MAX_RAW_RESULT_FOR_CHAT);
        }

        return truncate(resultadoBruto, MAX_RAW_RESULT_FOR_CHAT);
    }

    /* * Feature: tenta usar o resumidor existente se houver assinatura compativel. * Data: 2026-05-27 14:20 * Chamado por: * - resumirResultadoParaChat * Chama: * - tryInvoke * Objetivo: * - preservar compatibilidade com ToolResultSummarizer sem exigir refactor */
    private String tentarResumidorExterno(ToolResultSummarizer toolResultPresenter, String toolName, String resultadoBruto) {
        if (toolResultPresenter == null) {
            return "";
        }

        Object retorno = tryInvoke(toolResultPresenter, "resumirResultadoFerramenta",
                new Class<?>[] { String.class, String.class },
                new Object[] { toolName, resultadoBruto });

        if (retorno instanceof String) {
            return (String) retorno;
        }

        retorno = tryInvoke(toolResultPresenter, "resumir",
                new Class<?>[] { String.class, String.class },
                new Object[] { toolName, resultadoBruto });

        if (retorno instanceof String) {
            return (String) retorno;
        }

        retorno = tryInvoke(toolResultPresenter, "buildSummary",
                new Class<?>[] { String.class, String.class },
                new Object[] { toolName, resultadoBruto });

        if (retorno instanceof String) {
            return (String) retorno;
        }

        return "";
    }

    /* * Feature: registra ultimo resultado no contexto da missao. * Data: 2026-05-27 14:20 * Chamado por: * - executarFerramenta * Chama: * - tryInvokeVoid * Objetivo: * - permitir resposta de contingencia caso o limite de ciclos seja atingido */
    private void registrarUltimoResultadoNoContexto(MissionExecutionContext contextoMissao, String toolName, String resultadoParaChat, String resultadoBruto) {
        if (contextoMissao == null) {
            return;
        }

        tryInvokeVoid(contextoMissao, "setUltimoNomeFerramenta", new Class<?>[] { String.class }, new Object[] { toolName });
        tryInvokeVoid(contextoMissao, "setUltimoResumoFerramenta", new Class<?>[] { String.class }, new Object[] { resultadoParaChat });
        tryInvokeVoid(contextoMissao, "setUltimoResultadoFerramentaBruto", new Class<?>[] { String.class }, new Object[] { resultadoBruto });
    }

    /* * Feature: invocacao defensiva sem retorno. * Data: 2026-05-27 14:20 * Chamado por: * - registrarUltimoResultadoNoContexto * Chama: * - Method.invoke * Objetivo: * - evitar quebra se a versao do contexto nao tiver algum setter */
    private void tryInvokeVoid(Object target, String methodName, Class<?>[] parameterTypes, Object[] args) {
        if (target == null || methodName == null) {
            return;
        }

        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            method.invoke(target, args);
        } catch (Exception e) {
        }
    }

    /* * Feature: converte retorno de ferramenta para texto. * Data: 2026-05-27 14:20 * Chamado por: * - executarFerramenta * Chama: * - Gson.toJson * Objetivo: * - suportar String, Map, Collection e arrays */
    private String converterResultadoParaTexto(Object retornoBruto) {
        if (retornoBruto == null) {
            return "";
        }

        if (retornoBruto instanceof String) {
            return (String) retornoBruto;
        }

        if (retornoBruto instanceof Map<?, ?>) {
            return gson.toJson(retornoBruto);
        }

        if (retornoBruto instanceof Collection<?>) {
            return gson.toJson(retornoBruto);
        }

        if (retornoBruto.getClass().isArray()) {
            return gson.toJson(retornoBruto);
        }

        return String.valueOf(retornoBruto);
    }

    /* * Feature: sanitiza resultado bruto antes de recolocar no prompt. * Data: 2026-05-27 14:20 * Chamado por: * - executarFerramenta * Chama: * - truncate * Objetivo: * - evitar prompt infinito e estouro de contexto */
    private String sanitizarResultadoBruto(String resultadoBruto) {
        String texto = resultadoBruto != null ? resultadoBruto : "";
        texto = texto.replace("\r\n", "\n").replace('\r', '\n').trim();

        if (texto.length() == 0) {
            return "";
        }

        return truncate(texto, MAX_RAW_RESULT_FOR_PROMPT);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }


    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }

        if (max <= 0 || value.length() <= max) {
            return value;
        }

        return value.substring(0, max) + "\n[RESUMO]: Conteudo truncado para preservar legibilidade.";
    }

    public static class ToolStepResult {

        private boolean sucesso;
        private String nomeFerramenta;
        private String resultadoParaChat;
        private String resultadoBruto;
        private String mensagemErro;

        public static ToolStepResult sucesso(String nomeFerramenta, String resultadoParaChat, String resultadoBruto) {
            ToolStepResult result = new ToolStepResult();
            result.sucesso = true;
            result.nomeFerramenta = nomeFerramenta;
            result.resultadoParaChat = resultadoParaChat != null ? resultadoParaChat : "";
            result.resultadoBruto = resultadoBruto != null ? resultadoBruto : "";
            result.mensagemErro = "";
            return result;
        }

        public static ToolStepResult falha(String mensagemErro) {
            ToolStepResult result = new ToolStepResult();
            result.sucesso = false;
            result.nomeFerramenta = "";
            result.resultadoParaChat = "";
            result.resultadoBruto = "";
            result.mensagemErro = mensagemErro != null ? mensagemErro : "Falha desconhecida.";
            return result;
        }

        public boolean isSucesso() {
            return sucesso;
        }

        public String getNomeFerramenta() {
            return nomeFerramenta;
        }

        public String getResultadoParaChat() {
            return resultadoParaChat;
        }

        public String getResultadoBruto() {
            return resultadoBruto;
        }

        public String getMensagemErro() {
            return mensagemErro;
        }
    }
}