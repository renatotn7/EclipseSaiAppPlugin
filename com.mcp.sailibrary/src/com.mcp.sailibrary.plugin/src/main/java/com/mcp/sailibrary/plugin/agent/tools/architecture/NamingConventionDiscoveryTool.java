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

/** * Descobre convencoes por frequencia de nomes sem hardcode de stack. */
public class NamingConventionDiscoveryTool implements AgentTool, AgentToolPromptMetadataProvider {

    private File rootDirectory;

    public NamingConventionDiscoveryTool(File rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public String getName() { return "descobrir_padroes_nomenclatura_projeto"; }

    public AgentToolPromptMetadata getPromptMetadata() {
        AgentToolPromptMetadata metadata = new AgentToolPromptMetadata();
        metadata.setToolName(getName());
        metadata.setOneLinePurpose("Descobrir sufixos, prefixos e familias de nomes reais do projeto.");
        metadata.setActivityDescription("Quebra nomes por CamelCase e agrupa padroes frequentes para inferir convencoes sem assumir Struts, Spring ou Hibernate.");
        metadata.addParameter(param("path", false, "Caminho relativo inicial.", "src/main/java"));
        metadata.addParameter(param("min_ocorrencias", false, "Ocorrencias minimas para considerar padrao relevante.", "3"));
        metadata.addParameter(param("min_padroes_para_proxima_fase", false, "Quantidade minima de padroes relevantes para avancar de fase.", "3"));
        metadata.addRecommendedUseCase("Use antes de assumir sufixos como Action, Controller, Repository, DAO ou Entity.");
        metadata.addRecommendedUseCase("Use para orientar a IA sobre convencoes reais do projeto.");
        metadata.addGuardrail("Sufixo frequente e pista; confirme papel com imports, annotations e XMLs.");
        metadata.addGuardrail("Projetos podem misturar padroes. Nao force uma arquitetura unica.");
        metadata.addJsonExample("{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"descobrir_padroes_nomenclatura_projeto\\\",\\\"parameters\\\":{\\\"path\\\":\\\"src/main/java\\\",\\\"min_ocorrencias\\\":\\\"3\\\"},\\\"explanation\\\":\\\"Preciso descobrir convencoes reais de nomenclatura.\\\"}");
        return metadata;
    }

    public String execute(String jsonParameters) {
        String path = ToolJsonSupport.extractJsonStringValue(jsonParameters, "path");
        int min = ToolJsonSupport.extractJsonIntValue(jsonParameters, "min_ocorrencias", 3, 1, 1000);
        int minPatterns = ToolJsonSupport.extractJsonIntValue(jsonParameters, "min_padroes_para_proxima_fase", 3, 1, 1000);
        File start = resolve(path);

        Map<String, Integer> suffixes = new HashMap<String, Integer>();
        Map<String, Integer> prefixes = new HashMap<String, Integer>();
        Map<String, Integer> families = new HashMap<String, Integer>();
        List<String> examples = new ArrayList<String>();
        int[] totalJava = new int[] {0};

        scan(start, suffixes, prefixes, families, examples, totalJava);

        int relevant = countRelevant(suffixes, min);
        boolean proceed = relevant >= minPatterns;

        System.out.println("[ARCH NAMING TOOL] ================================================");
        System.out.println("[ARCH NAMING TOOL] start=" + describe(start));
        System.out.println("[ARCH NAMING TOOL] totalJava=" + totalJava[0]);
        System.out.println("[ARCH NAMING TOOL] relevantSuffixes=" + relevant);
        System.out.println("[ARCH NAMING TOOL] threshold.minOccurrences=" + min);
        System.out.println("[ARCH NAMING TOOL] threshold.minPatterns=" + minPatterns);
        System.out.println("[ARCH NAMING TOOL] phaseGate.next=" + proceed);
        System.out.println("[ARCH NAMING TOOL] ================================================");

        StringBuilder sb = new StringBuilder();
        sb.append("Padroes de nomenclatura descobertos\n");
        sb.append("start: ").append(describe(start)).append("\n");
        sb.append("totalJava: ").append(totalJava[0]).append("\n");
        sb.append("relevantSuffixes: ").append(relevant).append("\n");
        sb.append("PHASE_GATE NAMING_TO_PERSISTENCE: ").append(proceed ? "PROCEED" : "STOP").append("\n");
        sb.append("thresholds: minOcorrencias=").append(min).append(", minPadroes=").append(minPatterns).append("\n\n");

        sb.append("Sufixos relevantes:\n");
        appendRelevant(sb, suffixes, min, 80);
        sb.append("\nPrefixos relevantes:\n");
        appendRelevant(sb, prefixes, min, 50);
        sb.append("\nFamilias/radicais relevantes:\n");
        appendRelevant(sb, families, min, 50);
        sb.append("\nExemplos de classes:\n");
        appendList(sb, examples, 60);

        return sb.toString();
    }

    private void scan(File file, Map<String, Integer> suffixes, Map<String, Integer> prefixes, Map<String, Integer> families, List<String> examples, int[] totalJava) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            String name = file.getName();
            if ("target".equals(name) || "bin".equals(name) || ".git".equals(name) || ".settings".equals(name) || ".metadata".equals(name)) return;
            File[] children = file.listFiles();
            if (children != null) for (int i = 0; i < children.length; i++) scan(children[i], suffixes, prefixes, families, examples, totalJava);
            return;
        }
        if (!file.getName().endsWith(".java")) return;
        totalJava[0]++;
        String className = file.getName().substring(0, file.getName().length() - 5);
        if (examples.size() < 80) examples.add(className);
        List<String> tokens = splitCamel(className);
        if (tokens.isEmpty()) return;
        increment(prefixes, tokens.get(0));
        increment(families, tokens.get(0));
        for (int size = 1; size <= 3; size++) {
            if (tokens.size() >= size) {
                StringBuilder sb = new StringBuilder();
                for (int i = tokens.size() - size; i < tokens.size(); i++) sb.append(tokens.get(i));
                increment(suffixes, sb.toString());
            }
        }
    }

    private List<String> splitCamel(String name) {
        List<String> tokens = new ArrayList<String>();
        String[] parts = name.split("(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])|_|-");
        for (int i = 0; i < parts.length; i++) if (parts[i] != null && parts[i].trim().length() > 0) tokens.add(parts[i].trim());
        return tokens;
    }

    private void increment(Map<String, Integer> map, String key) {
        Integer count = map.get(key);
        map.put(key, Integer.valueOf(count == null ? 1 : count.intValue() + 1));
    }

    private int countRelevant(Map<String, Integer> map, int min) {
        int count = 0;
        for (Integer v : map.values()) if (v.intValue() >= min) count++;
        return count;
    }

    private void appendRelevant(StringBuilder sb, Map<String, Integer> map, int min, int limit) {
        int count = 0;
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            if (e.getValue().intValue() >= min) {
                if (count++ >= limit) break;
                sb.append("- ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
            }
        }
        if (count == 0) sb.append("- nenhum acima do limiar\n");
    }

    private void appendList(StringBuilder sb, List<String> values, int limite) {
        for (int i = 0; i < values.size() && i < limite; i++) sb.append("- ").append(values.get(i)).append("\n");
        if (values.isEmpty()) sb.append("- nenhum\n");
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
}
