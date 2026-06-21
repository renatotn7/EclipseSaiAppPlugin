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

/** * Rastreia problemas pre-existentes antes de alteracoes. */
public class ExistingProblemReconTool implements AgentTool, AgentToolPromptMetadataProvider {

    private File rootDirectory;

    public ExistingProblemReconTool(File rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public String getName() { return "rastrear_problemas_preexistentes"; }

    public AgentToolPromptMetadata getPromptMetadata() {
        AgentToolPromptMetadata metadata = new AgentToolPromptMetadata();
        metadata.setToolName(getName());
        metadata.setOneLinePurpose("Rastrear problemas pre-existentes para separar risco antigo de regressao nova.");
        metadata.setActivityDescription("Procura SQL concatenado, credenciais, TODO/FIXME, catch silencioso, System.out e configuracoes sensiveis.");
        metadata.addParameter(param("path", false, "Caminho relativo inicial.", ""));
        metadata.addParameter(param("limite", false, "Limite de evidencias exibidas.", "100"));
        metadata.addParameter(param("max_severos_para_proxima_fase", false, "Maximo de achados severos para prosseguir sem alerta forte.", "0"));
        metadata.addRecommendedUseCase("Use antes de alteracoes, refatoracoes e investigacoes de risco.");
        metadata.addRecommendedUseCase("Use para separar defeitos antigos de regressao criada pela alteracao.");
        metadata.addGuardrail("Achados sao candidatos; confirme manualmente antes de classificar como vulnerabilidade.");
        metadata.addGuardrail("Nao altere codigo apenas por um achado; use como orientacao de risco.");
        metadata.addJsonExample("{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"rastrear_problemas_preexistentes\\\",\\\"parameters\\\":{\\\"path\\\":\\\"\\\",\\\"limite\\\":\\\"100\\\"},\\\"explanation\\\":\\\"Preciso levantar riscos pre-existentes antes de propor alteracoes.\\\"}");
        return metadata;
    }

    public String execute(String jsonParameters) {
        String path = ToolJsonSupport.extractJsonStringValue(jsonParameters, "path");
        int limite = ToolJsonSupport.extractJsonIntValue(jsonParameters, "limite", 100, 1, 1000);
        int maxSevere = ToolJsonSupport.extractJsonIntValue(jsonParameters, "max_severos_para_proxima_fase", 0, 0, 100000);
        File start = resolve(path);

        Issues issues = new Issues();
        scan(start, issues, limite);

        int severe = issues.credentials + issues.sqlConcat;
        boolean proceed = severe <= maxSevere;

        System.out.println("[ARCH PROBLEM RECON TOOL] =========================================");
        System.out.println("[ARCH PROBLEM RECON TOOL] start=" + describe(start));
        System.out.println("[ARCH PROBLEM RECON TOOL] totalIssues=" + issues.total());
        System.out.println("[ARCH PROBLEM RECON TOOL] severeIssues=" + severe);
        System.out.println("[ARCH PROBLEM RECON TOOL] credentials=" + issues.credentials + ", sqlConcat=" + issues.sqlConcat);
        System.out.println("[ARCH PROBLEM RECON TOOL] todos=" + issues.todos + ", systemOut=" + issues.systemOut + ", catchSwallow=" + issues.catchSwallow);
        System.out.println("[ARCH PROBLEM RECON TOOL] threshold.maxSevere=" + maxSevere);
        System.out.println("[ARCH PROBLEM RECON TOOL] phaseGate.safeToChange=" + proceed);
        System.out.println("[ARCH PROBLEM RECON TOOL] =========================================");

        StringBuilder sb = new StringBuilder();
        sb.append("Rastreio de problemas pre-existentes\n");
        sb.append("start: ").append(describe(start)).append("\n");
        sb.append("totalIssues: ").append(issues.total()).append("\n");
        sb.append("severeIssues: ").append(severe).append("\n");
        sb.append("PHASE_GATE PROBLEMS_TO_CHANGE: ").append(proceed ? "PROCEED" : "STOP_REVIEW").append("\n");
        sb.append("thresholds: maxSeveros=").append(maxSevere).append("\n\n");

        sb.append("- Credenciais/configuracoes sensiveis candidatas: ").append(issues.credentials).append("\n");
        sb.append("- SQL concatenado candidato: ").append(issues.sqlConcat).append("\n");
        sb.append("- TODO/FIXME: ").append(issues.todos).append("\n");
        sb.append("- System.out/printStackTrace: ").append(issues.systemOut).append("\n");
        sb.append("- Catch vazio/silencioso candidato: ").append(issues.catchSwallow).append("\n\n");

        sb.append("Evidencias:\n");
        appendList(sb, issues.examples, limite);

        return sb.toString();
    }

    private void scan(File file, Issues issues, int limite) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            String name = file.getName();
            if ("target".equals(name) || "bin".equals(name) || ".git".equals(name) || ".settings".equals(name) || ".metadata".equals(name)) return;
            File[] children = file.listFiles();
            if (children != null) for (int i = 0; i < children.length; i++) scan(children[i], issues, limite);
            return;
        }

        String lower = file.getName().toLowerCase();
        if (!(lower.endsWith(".java") || lower.endsWith(".xml") || lower.endsWith(".properties") || lower.endsWith(".jsp") || lower.endsWith(".js"))) return;

        String content = read(file);
        String[] lines = content.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String normalized = line.toLowerCase();

            if (normalized.contains("todo") || normalized.contains("fixme")) {
                issues.todos++;
                add(issues, limite, "TODO_FIXME | " + describe(file) + ":" + (i + 1) + " | " + trim(line));
            }

            if (line.contains("System.out") || line.contains("printStackTrace")) {
                issues.systemOut++;
                add(issues, limite, "CONSOLE_OR_STACKTRACE | " + describe(file) + ":" + (i + 1) + " | " + trim(line));
            }

            if (looksLikeCredential(normalized)) {
                issues.credentials++;
                add(issues, limite, "SENSITIVE_CONFIG_CANDIDATE | " + describe(file) + ":" + (i + 1) + " | " + trim(mask(line)));
            }

            if (looksLikeSqlConcat(line)) {
                issues.sqlConcat++;
                add(issues, limite, "SQL_CONCAT_CANDIDATE | " + describe(file) + ":" + (i + 1) + " | " + trim(line));
            }

            if (normalized.contains("catch") && normalized.contains("exception")) {
                String next = i + 1 < lines.length ? lines[i + 1].trim() : "";
                if (next.length() == 0 || next.equals("// FAZ NADA") || next.equals("// faz nada")) {
                    issues.catchSwallow++;
                    add(issues, limite, "CATCH_SWALLOW_CANDIDATE | " + describe(file) + ":" + (i + 1) + " | " + trim(line));
                }
            }
        }
    }

    private boolean looksLikeCredential(String line) {
        if (!(line.contains("password") || line.contains("passwd") || line.contains("senha") || line.contains("secret")
                || line.contains("token") || line.contains("apikey") || line.contains("api_key") || line.contains("cookie"))) {
            return false;
        }
        return line.contains("=") || line.contains(":") || line.contains("\"");
    }

    private boolean looksLikeSqlConcat(String line) {
        String normalized = line.toLowerCase();
        boolean hasSql = normalized.contains("select ") || normalized.contains("update ") || normalized.contains("delete ") || normalized.contains("insert ");
        return hasSql && line.contains("+");
    }

    private String read(File file) {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            int count = 0;
            while ((line = br.readLine()) != null && count++ < 1200) sb.append(line).append("\n");
            br.close();
        } catch (Exception ex) {
            return "";
        }
        return sb.toString();
    }

    private void add(Issues issues, int limite, String value) {
        if (issues.examples.size() < limite) issues.examples.add(value);
    }

    private String mask(String value) {
        if (value == null) return "";
        int idx = value.indexOf('=');
        if (idx < 0) return value;
        return value.substring(0, idx + 1) + "REDACTED";
    }

    private String trim(String value) {
        if (value == null) return "";
        String safe = value.trim();
        return safe.length() > 220 ? safe.substring(0, 220) + "..." : safe;
    }

    private void appendList(StringBuilder sb, List<String> values, int limite) {
        if (values.isEmpty()) sb.append("- nenhum candidato encontrado\n");
        for (int i = 0; i < values.size() && i < limite; i++) sb.append(i + 1).append(". ").append(values.get(i)).append("\n");
    }

    private File resolve(String path) {
        if (rootDirectory == null) return new File(".");
        if (path == null || path.trim().length() == 0) return rootDirectory;
        return new File(rootDirectory, path);
    }

    private String describe(File file) {
        if (file == null) return "";
        try { return file.getCanonicalPath().replace("\\", "/"); } catch (Exception e) { return file.getAbsolutePath().replace("\\", "/"); }
    }

    private AgentToolParameterMetadata param(String name, boolean required, String description, String example) {
        AgentToolParameterMetadata p = new AgentToolParameterMetadata();
        p.setName(name); p.setRequired(required); p.setDescription(description); p.setExampleValue(example); return p;
    }

    private static class Issues {
        int credentials; int sqlConcat; int todos; int systemOut; int catchSwallow;
        List<String> examples = new ArrayList<String>();
        int total() { return credentials + sqlConcat + todos + systemOut + catchSwallow; }
    }
}
