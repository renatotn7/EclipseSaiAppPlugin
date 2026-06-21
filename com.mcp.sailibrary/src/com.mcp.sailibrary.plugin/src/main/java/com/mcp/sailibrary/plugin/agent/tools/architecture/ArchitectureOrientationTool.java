package com.mcp.sailibrary.plugin.agent.tools.architecture;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mcp.sailibrary.plugin.agent.AgentTool;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolParameterMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadata;
import com.mcp.sailibrary.plugin.agent.prompt.AgentToolPromptMetadataProvider;
import com.mcp.sailibrary.plugin.agent.tools.support.ToolJsonSupport;

/** * Orquestra fases de reconhecimento arquitetural com gates diagnosticos. */
public class ArchitectureOrientationTool implements AgentTool, AgentToolPromptMetadataProvider {

    private File rootDirectory;

    public ArchitectureOrientationTool(File rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public String getName() { return "gerar_mapa_operacional_ia"; }

    public AgentToolPromptMetadata getPromptMetadata() {
        AgentToolPromptMetadata metadata = new AgentToolPromptMetadata();
        metadata.setToolName(getName());
        metadata.setOneLinePurpose("Gerar mapa operacional consolidado para orientar a IA no projeto.");
        metadata.setActivityDescription("Executa inventario, nomenclatura, dependencias, persistencia e problemas pre-existentes em fases.");
        metadata.addParameter(param("path", false, "Caminho relativo inicial.", ""));
        metadata.addParameter(param("profundidade", false, "Profundidade: baixa, media ou alta.", "alta"));
        metadata.addParameter(param("min_arquivos", false, "Limiar minimo de arquivos para sair da fase de inventario.", "20"));
        metadata.addParameter(param("min_padroes", false, "Limiar minimo de padroes de nomes para sair da fase de nomenclatura.", "3"));
        metadata.addParameter(param("min_persistencia", false, "Limiar minimo de evidencias de persistencia.", "1"));
        metadata.addParameter(param("max_problemas_severos", false, "Maximo de achados severos antes de exigir revisao.", "0"));
        metadata.addRecommendedUseCase("Use no inicio de um projeto desconhecido.");
        metadata.addRecommendedUseCase("Use quando o usuario pedir arquitetura, camadas, padroes ou fluxo geral.");
        metadata.addRecommendedUseCase("Use antes de varias ferramentas estruturais isoladas.");
        metadata.addGuardrail("Distingua evidencias de inferencias e pare quando gates minimos falharem.");
        metadata.addGuardrail("Nao registre memoria automaticamente; apenas sugira registros estaveis.");
        metadata.addJsonExample("{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"gerar_mapa_operacional_ia\\\",\\\"parameters\\\":{\\\"path\\\":\\\"\\\",\\\"profundidade\\\":\\\"alta\\\"},\\\"explanation\\\":\\\"Preciso de um mapa operacional completo para navegar no projeto.\\\"}");
        return metadata;
    }

    public String execute(String jsonParameters) {
        String path = ToolJsonSupport.extractJsonStringValue(jsonParameters, "path");
        String profundidade = ToolJsonSupport.extractJsonStringValue(jsonParameters, "profundidade");
        int minArquivos = ToolJsonSupport.extractJsonIntValue(jsonParameters, "min_arquivos", 20, 1, 1000000);
        int minPadroes = ToolJsonSupport.extractJsonIntValue(jsonParameters, "min_padroes", 3, 1, 1000);
        int minPersistencia = ToolJsonSupport.extractJsonIntValue(jsonParameters, "min_persistencia", 1, 0, 1000000);
        int maxSeveros = ToolJsonSupport.extractJsonIntValue(jsonParameters, "max_problemas_severos", 0, 0, 1000000);

        if (profundidade == null || profundidade.trim().length() == 0) {
            profundidade = "alta";
        }

        System.out.println("[ARCH ORIENTATION TOOL] ===========================================");
        System.out.println("[ARCH ORIENTATION TOOL] phase=START");
        System.out.println("[ARCH ORIENTATION TOOL] root=" + describe(rootDirectory));
        System.out.println("[ARCH ORIENTATION TOOL] path=" + path);
        System.out.println("[ARCH ORIENTATION TOOL] profundidade=" + profundidade);
        System.out.println("[ARCH ORIENTATION TOOL] thresholds minArquivos=" + minArquivos + ", minPadroes=" + minPadroes + ", minPersistencia=" + minPersistencia + ", maxSeveros=" + maxSeveros);
        System.out.println("[ARCH ORIENTATION TOOL] ===========================================");

        PhaseReport inventory = runInventory(path, minArquivos);
        PhaseReport naming = inventory.proceed ? runNaming(path, minPadroes) : skipped("NAMING", "Inventario abaixo do limiar.");
        PhaseReport dependency = naming.proceed ? runDependency(path) : skipped("DEPENDENCY", "Nomenclatura abaixo do limiar.");
        PhaseReport persistence = naming.proceed ? runPersistence(path, minPersistencia) : skipped("PERSISTENCE", "Nomenclatura abaixo do limiar.");
        PhaseReport problems = runProblems(path, maxSeveros);

        boolean ready = inventory.proceed && naming.proceed && problems.proceed;

        System.out.println("[ARCH ORIENTATION TOOL] phase=FINAL");
        System.out.println("[ARCH ORIENTATION TOOL] inventoryProceed=" + inventory.proceed);
        System.out.println("[ARCH ORIENTATION TOOL] namingProceed=" + naming.proceed);
        System.out.println("[ARCH ORIENTATION TOOL] persistenceProceed=" + persistence.proceed);
        System.out.println("[ARCH ORIENTATION TOOL] problemsProceed=" + problems.proceed);
        System.out.println("[ARCH ORIENTATION TOOL] readyForDeepAnalysis=" + ready);
        System.out.println("[ARCH ORIENTATION TOOL] ===========================================");

        StringBuilder sb = new StringBuilder();
        sb.append("Mapa operacional da IA\n");
        sb.append("path: ").append(path == null ? "" : path).append("\n");
        sb.append("profundidade: ").append(profundidade).append("\n");
        sb.append("FINAL_GATE READY_FOR_DEEP_ANALYSIS: ").append(ready ? "PROCEED" : "STOP_REVIEW").append("\n\n");

        appendPhase(sb, "1. Inventario", inventory);
        appendPhase(sb, "2. Nomenclatura", naming);
        appendPhase(sb, "3. Dependencias", dependency);
        appendPhase(sb, "4. Persistencia", persistence);
        appendPhase(sb, "5. Problemas pre-existentes", problems);

        sb.append("\nGuia operacional para a IA\n");
        sb.append("- Para perguntas de arquitetura geral, use este mapa antes de ferramentas especificas.\n");
        sb.append("- Para tela ou fluxo web, comece por texto de tela/JSP/controller/Action conforme padroes encontrados.\n");
        sb.append("- Para banco, identifique o estilo de persistencia antes de mapear tabela, entidade ou query.\n");
        sb.append("- Para alteracoes, trate achados de problemas pre-existentes como risco anterior e nao como regressao nova automaticamente.\n");

        sb.append("\nSugestoes de memoria persistente\n");
        sb.append("- Registrar kind=architecture key=orientation_gate value=").append(ready ? "ready" : "review_required").append("\n");
        sb.append("- Registrar kind=naming key=discovered_conventions com base na fase de nomenclatura.\n");
        sb.append("- Registrar kind=persistence key=detected_styles com base na fase de persistencia.\n");
        sb.append("- Nao registrar cookies, secrets, conteudo integral de arquivos ou respostas completas.\n");

        return sb.toString();
    }

    private PhaseReport runInventory(String path, int minArquivos) {
        String json = "{\"path\":\"" + escape(path) + "\",\"limite\":\"200\",\"min_arquivos_para_proxima_fase\":\"" + minArquivos + "\"}";
        String report = new ProjectStructureInventoryTool(rootDirectory).execute(json);
        return new PhaseReport("INVENTORY", report, report.indexOf("PHASE_GATE INVENTORY_TO_NAMING: PROCEED") >= 0);
    }

    private PhaseReport runNaming(String path, int minPadroes) {
        String json = "{\"path\":\"" + escape(path) + "\",\"min_ocorrencias\":\"3\",\"min_padroes_para_proxima_fase\":\"" + minPadroes + "\"}";
        String report = new NamingConventionDiscoveryTool(rootDirectory).execute(json);
        return new PhaseReport("NAMING", report, report.indexOf("PHASE_GATE NAMING_TO_PERSISTENCE: PROCEED") >= 0);
    }

    private PhaseReport runDependency(String path) {
        try {
            String json = "{\"path\":\"" + escape(path) + "\"}";
            String report = new ProjectDependencyInspectionTool(rootDirectory).execute(json);
            return new PhaseReport("DEPENDENCY", report, true);
        } catch (Exception e) {
            return new PhaseReport("DEPENDENCY", "Falha ao inspecionar dependencias: " + e.getMessage(), false);
        }
    }

    private PhaseReport runPersistence(String path, int minPersistencia) {
        String json = "{\"path\":\"" + escape(path) + "\",\"min_evidencias_para_proxima_fase\":\"" + minPersistencia + "\"}";
        String report = new PersistenceStyleDetectionTool(rootDirectory).execute(json);
        return new PhaseReport("PERSISTENCE", report, report.indexOf("PHASE_GATE PERSISTENCE_TO_PROBLEMS: PROCEED") >= 0);
    }

    private PhaseReport runProblems(String path, int maxSeveros) {
        String json = "{\"path\":\"" + escape(path) + "\",\"limite\":\"120\",\"max_severos_para_proxima_fase\":\"" + maxSeveros + "\"}";
        String report = new ExistingProblemReconTool(rootDirectory).execute(json);
        return new PhaseReport("PROBLEMS", report, report.indexOf("PHASE_GATE PROBLEMS_TO_CHANGE: PROCEED") >= 0);
    }

    private PhaseReport skipped(String name, String reason) {
        return new PhaseReport(name, "Fase ignorada: " + reason, false);
    }

    private void appendPhase(StringBuilder sb, String title, PhaseReport phase) {
        sb.append("\n").append(title).append("\n");
        sb.append("phase: ").append(phase.name).append("\n");
        sb.append("proceed: ").append(phase.proceed).append("\n");
        sb.append(compact(phase.report, 2500)).append("\n");
    }

    private String compact(String text, int max) {
        if (text == null) return "";
        if (text.length() <= max) return text;
        return text.substring(0, max) + "\n[RESUMO]: Conteudo truncado para preservar contexto.";
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String describe(File file) {
        if (file == null) return "";
        try { return file.getCanonicalPath().replace("\\", "/"); } catch (Exception e) { return file.getAbsolutePath().replace("\\", "/"); }
    }

    private AgentToolParameterMetadata param(String name, boolean required, String description, String example) {
        AgentToolParameterMetadata p = new AgentToolParameterMetadata();
        p.setName(name); p.setRequired(required); p.setDescription(description); p.setExampleValue(example); return p;
    }

    private static class PhaseReport {
        String name;
        String report;
        boolean proceed;

        PhaseReport(String name, String report, boolean proceed) {
            this.name = name;
            this.report = report;
            this.proceed = proceed;
        }
    }
}
