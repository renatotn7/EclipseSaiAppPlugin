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

/** * Inventaria estrutura fisica com gates de fase. */
public class ProjectStructureInventoryTool implements AgentTool, AgentToolPromptMetadataProvider {

    private File rootDirectory;

    public ProjectStructureInventoryTool(File rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public String getName() {
        return "inventariar_estrutura_projeto";
    }

    public AgentToolPromptMetadata getPromptMetadata() {
        AgentToolPromptMetadata metadata = new AgentToolPromptMetadata();
        metadata.setToolName(getName());
        metadata.setOneLinePurpose("Inventariar estrutura fisica, modulos, extensoes e arquivos canonicos do projeto.");
        metadata.setActivityDescription("Varre a raiz segura e resume estrutura fisica para orientar a primeira leitura arquitetural.");

        metadata.addParameter(param("path", false, "Caminho relativo inicial.", ""));
        metadata.addParameter(param("limite", false, "Limite de exemplos exibidos.", "200"));
        metadata.addParameter(param("min_arquivos_para_proxima_fase", false, "Limiar minimo de arquivos para considerar inventario suficiente.", "20"));
        metadata.addParameter(param("min_canonicos_para_proxima_fase", false, "Limiar minimo de arquivos canonicos para sinalizar arquitetura reconhecivel.", "1"));

        metadata.addRecommendedUseCase("Use no inicio de projeto desconhecido.");
        metadata.addRecommendedUseCase("Use antes de inferir camadas, frameworks ou persistencia.");
        metadata.addGuardrail("Nao confunda contagem fisica com confirmacao de framework.");
        metadata.addGuardrail("Use o gate de fase como sinal operacional, nao como verdade absoluta.");
        metadata.addJsonExample("{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"inventariar_estrutura_projeto\\\",\\\"parameters\\\":{\\\"path\\\":\\\"\\\",\\\"limite\\\":\\\"200\\\",\\\"min_arquivos_para_proxima_fase\\\":\\\"20\\\"},\\\"explanation\\\":\\\"Preciso inventariar o projeto antes de inferir arquitetura.\\\"}");
        return metadata;
    }

    public String execute(String jsonParameters) {
        String path = ToolJsonSupport.extractJsonStringValue(jsonParameters, "path");
        int limite = ToolJsonSupport.extractJsonIntValue(jsonParameters, "limite", 200, 10, 2000);
        int minArquivos = ToolJsonSupport.extractJsonIntValue(jsonParameters, "min_arquivos_para_proxima_fase", 20, 1, 1000000);
        int minCanonicos = ToolJsonSupport.extractJsonIntValue(jsonParameters, "min_canonicos_para_proxima_fase", 1, 0, 1000);

        File start = resolve(path);
        Inventory inventory = new Inventory();
        scan(start, inventory, limite);

        boolean proceed = inventory.totalFiles >= minArquivos && inventory.canonicalFiles.size() >= minCanonicos;

        System.out.println("[ARCH INVENTORY TOOL] =============================================");
        System.out.println("[ARCH INVENTORY TOOL] start=" + describe(start));
        System.out.println("[ARCH INVENTORY TOOL] totalFiles=" + inventory.totalFiles);
        System.out.println("[ARCH INVENTORY TOOL] totalDirs=" + inventory.totalDirs);
        System.out.println("[ARCH INVENTORY TOOL] modules=" + inventory.modules.size());
        System.out.println("[ARCH INVENTORY TOOL] canonicalFiles=" + inventory.canonicalFiles.size());
        System.out.println("[ARCH INVENTORY TOOL] threshold.minFiles=" + minArquivos);
        System.out.println("[ARCH INVENTORY TOOL] threshold.minCanonical=" + minCanonicos);
        System.out.println("[ARCH INVENTORY TOOL] phaseGate.next=" + proceed);
        System.out.println("[ARCH INVENTORY TOOL] =============================================");

        StringBuilder sb = new StringBuilder();
        sb.append("Inventario estrutural do projeto\n");
        sb.append("start: ").append(describe(start)).append("\n");
        sb.append("totalFiles: ").append(inventory.totalFiles).append("\n");
        sb.append("totalDirs: ").append(inventory.totalDirs).append("\n");
        sb.append("PHASE_GATE INVENTORY_TO_NAMING: ").append(proceed ? "PROCEED" : "STOP").append("\n");
        sb.append("thresholds: minArquivos=").append(minArquivos).append(", minCanonicos=").append(minCanonicos).append("\n\n");

        sb.append("Contagem por extensao:\n");
        appendMap(sb, inventory.extensionCounts, limite);

        sb.append("\nModulos candidatos:\n");
        appendList(sb, inventory.modules, limite);

        sb.append("\nArquivos canonicos:\n");
        appendList(sb, inventory.canonicalFiles, limite);

        return sb.toString();
    }

    private void scan(File file, Inventory inventory, int limite) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            String name = file.getName();
            if ("target".equals(name) || "bin".equals(name) || ".git".equals(name) || ".settings".equals(name) || ".metadata".equals(name)) return;
            inventory.totalDirs++;
            if (new File(file, "pom.xml").exists() || new File(file, "build.gradle").exists() || new File(file, "src/main/java").exists() || new File(file, "src/main/webapp").exists()) {
                addLimited(inventory.modules, describe(file), limite);
            }
            File[] children = file.listFiles();
            if (children != null) for (int i = 0; i < children.length; i++) scan(children[i], inventory, limite);
            return;
        }
        inventory.totalFiles++;
        String name = file.getName().toLowerCase();
        String ext = "(sem ext)";
        if (name.endsWith(".hbm.xml")) ext = ".hbm.xml";
        else {
            int idx = name.lastIndexOf('.');
            if (idx >= 0) ext = name.substring(idx);
        }
        Integer count = inventory.extensionCounts.get(ext);
        inventory.extensionCounts.put(ext, Integer.valueOf(count == null ? 1 : count.intValue() + 1));
        if (isCanonical(name)) addLimited(inventory.canonicalFiles, describe(file), limite);
    }

    private boolean isCanonical(String name) {
        return "pom.xml".equals(name) || "build.gradle".equals(name) || "web.xml".equals(name)
                || name.startsWith("struts-config") || name.startsWith("hibernate") || "persistence.xml".equals(name)
                || "applicationcontext.xml".equals(name) || "faces-config.xml".equals(name);
    }

    private File resolve(String path) {
        if (rootDirectory == null) return new File(".");
        if (path == null || path.trim().length() == 0) return rootDirectory;
        return new File(rootDirectory, path);
    }

    private void appendMap(StringBuilder sb, Map<String, Integer> map, int limite) {
        int count = 0;
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            if (count++ >= limite) break;
            sb.append("- ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
        }
    }

    private void appendList(StringBuilder sb, List<String> values, int limite) {
        if (values.isEmpty()) sb.append("- nenhum\n");
        for (int i = 0; i < values.size() && i < limite; i++) sb.append("- ").append(values.get(i)).append("\n");
    }

    private void addLimited(List<String> list, String value, int limite) {
        if (list.size() < limite) list.add(value);
    }

    private String describe(File file) {
        if (file == null) return "";
        try { return file.getCanonicalPath().replace("\\", "/"); } catch (Exception e) { return file.getAbsolutePath().replace("\\", "/"); }
    }

    private AgentToolParameterMetadata param(String name, boolean required, String description, String example) {
        AgentToolParameterMetadata p = new AgentToolParameterMetadata();
        p.setName(name); p.setRequired(required); p.setDescription(description); p.setExampleValue(example); return p;
    }

    private static class Inventory {
        int totalFiles;
        int totalDirs;
        Map<String, Integer> extensionCounts = new HashMap<String, Integer>();
        List<String> modules = new ArrayList<String>();
        List<String> canonicalFiles = new ArrayList<String>();
    }
}
