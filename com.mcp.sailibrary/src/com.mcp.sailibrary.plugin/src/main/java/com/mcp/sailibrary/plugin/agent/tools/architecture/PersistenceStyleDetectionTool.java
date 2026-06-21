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

/** * Detecta estilos de persistencia por evidencias. */
public class PersistenceStyleDetectionTool implements AgentTool, AgentToolPromptMetadataProvider {

    private File rootDirectory;

    public PersistenceStyleDetectionTool(File rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public String getName() { return "detectar_estilos_persistencia"; }

    public AgentToolPromptMetadata getPromptMetadata() {
        AgentToolPromptMetadata metadata = new AgentToolPromptMetadata();
        metadata.setToolName(getName());
        metadata.setOneLinePurpose("Detectar estilos de persistencia: HBM, JPA, Spring Data, JDBC, MyBatis e afins.");
        metadata.setActivityDescription("Varre evidencias concretas antes de escolher ferramentas especificas de banco.");
        metadata.addParameter(param("path", false, "Caminho relativo inicial.", ""));
        metadata.addParameter(param("min_evidencias_para_proxima_fase", false, "Limiar minimo de evidencias de persistencia para avancar.", "1"));
        metadata.addRecommendedUseCase("Use quando a pergunta envolver banco, entidades, queries, DAO, repository ou Hibernate.");
        metadata.addGuardrail("Nao conclua ausencia de persistencia apenas por falta de HBM; verifique annotations e APIs.");
        metadata.addJsonExample("{\\\"action\\\":\\\"executar_ferramenta\\\",\\\"tool\\\":\\\"detectar_estilos_persistencia\\\",\\\"parameters\\\":{\\\"path\\\":\\\"\\\",\\\"min_evidencias_para_proxima_fase\\\":\\\"1\\\"},\\\"explanation\\\":\\\"Preciso descobrir o estilo de persistencia antes de levantar queries.\\\"}");
        return metadata;
    }

    public String execute(String jsonParameters) {
        String path = ToolJsonSupport.extractJsonStringValue(jsonParameters, "path");
        int minEvidence = ToolJsonSupport.extractJsonIntValue(jsonParameters, "min_evidencias_para_proxima_fase", 1, 0, 100000);
        File start = resolve(path);

        Evidence e = new Evidence();
        scan(start, e);

        int totalEvidence = e.hibernateHbm + e.hibernateCfg + e.hibernateApi + e.jpaAnnotation + e.springData + e.jdbc + e.mybatis + e.jooq;
        boolean proceed = totalEvidence >= minEvidence;

        System.out.println("[ARCH PERSISTENCE TOOL] ===========================================");
        System.out.println("[ARCH PERSISTENCE TOOL] start=" + describe(start));
        System.out.println("[ARCH PERSISTENCE TOOL] totalEvidence=" + totalEvidence);
        System.out.println("[ARCH PERSISTENCE TOOL] hbm=" + e.hibernateHbm + ", cfg=" + e.hibernateCfg + ", hibernateApi=" + e.hibernateApi);
        System.out.println("[ARCH PERSISTENCE TOOL] jpa=" + e.jpaAnnotation + ", springData=" + e.springData + ", jdbc=" + e.jdbc + ", mybatis=" + e.mybatis + ", jooq=" + e.jooq);
        System.out.println("[ARCH PERSISTENCE TOOL] threshold.minEvidence=" + minEvidence);
        System.out.println("[ARCH PERSISTENCE TOOL] phaseGate.next=" + proceed);
        System.out.println("[ARCH PERSISTENCE TOOL] ===========================================");

        StringBuilder sb = new StringBuilder();
        sb.append("Estilos de persistencia detectados\n");
        sb.append("start: ").append(describe(start)).append("\n");
        sb.append("totalEvidence: ").append(totalEvidence).append("\n");
        sb.append("PHASE_GATE PERSISTENCE_TO_PROBLEMS: ").append(proceed ? "PROCEED" : "STOP").append("\n\n");

        sb.append("- Hibernate HBM XML: ").append(e.hibernateHbm).append("\n");
        sb.append("- Hibernate cfg XML: ").append(e.hibernateCfg).append("\n");
        sb.append("- Hibernate Session/Criteria/Query API: ").append(e.hibernateApi).append("\n");
        sb.append("- JPA annotations/API: ").append(e.jpaAnnotation).append("\n");
        sb.append("- Spring Data/JPA Repository: ").append(e.springData).append("\n");
        sb.append("- JDBC/JdbcTemplate: ").append(e.jdbc).append("\n");
        sb.append("- MyBatis: ").append(e.mybatis).append("\n");
        sb.append("- jOOQ: ").append(e.jooq).append("\n\n");

        sb.append("Exemplos de evidencias:\n");
        appendList(sb, e.examples, 80);

        return sb.toString();
    }

    private void scan(File file, Evidence e) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            String name = file.getName();
            if ("target".equals(name) || "bin".equals(name) || ".git".equals(name) || ".settings".equals(name) || ".metadata".equals(name)) return;
            File[] children = file.listFiles();
            if (children != null) for (int i = 0; i < children.length; i++) scan(children[i], e);
            return;
        }

        String name = file.getName().toLowerCase();
        if (name.endsWith(".hbm.xml")) { e.hibernateHbm++; add(e, "HBM " + describe(file)); }
        if (name.startsWith("hibernate") && name.endsWith(".xml")) { e.hibernateCfg++; add(e, "Hibernate cfg " + describe(file)); }
        if ("persistence.xml".equals(name) || "orm.xml".equals(name)) { e.jpaAnnotation++; add(e, "JPA XML " + describe(file)); }

        if (name.endsWith(".java") || name.endsWith(".xml")) {
            String content = read(file);
            if (content.contains("org.hibernate.Session") || content.contains("createCriteria") || content.contains("Restrictions.") || content.contains("createSQLQuery")) { e.hibernateApi++; add(e, "Hibernate API " + describe(file)); }
            if (content.contains("@Entity") || content.contains("@Table") || content.contains("javax.persistence") || content.contains("jakarta.persistence")) { e.jpaAnnotation++; add(e, "JPA annotation/API " + describe(file)); }
            if (content.contains("JpaRepository") || content.contains("CrudRepository") || content.contains("PagingAndSortingRepository")) { e.springData++; add(e, "Spring Data " + describe(file)); }
            if (content.contains("PreparedStatement") || content.contains("ResultSet") || content.contains("JdbcTemplate") || content.contains("NamedParameterJdbcTemplate")) { e.jdbc++; add(e, "JDBC " + describe(file)); }
            if (content.contains("SqlSession") || content.contains("@Mapper") || content.contains("<mapper")) { e.mybatis++; add(e, "MyBatis " + describe(file)); }
            if (content.contains("DSLContext") || content.contains("org.jooq")) { e.jooq++; add(e, "jOOQ " + describe(file)); }
        }
    }

    private String read(File file) {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            int count = 0;
            while ((line = br.readLine()) != null && count++ < 400) sb.append(line).append("\n");
            br.close();
        } catch (Exception ex) {
            return "";
        }
        return sb.toString();
    }

    private void add(Evidence e, String value) { if (e.examples.size() < 120) e.examples.add(value); }

    private void appendList(StringBuilder sb, List<String> values, int limite) {
        if (values.isEmpty()) sb.append("- nenhuma evidencia exemplar\n");
        for (int i = 0; i < values.size() && i < limite; i++) sb.append("- ").append(values.get(i)).append("\n");
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

    private static class Evidence {
        int hibernateHbm; int hibernateCfg; int hibernateApi; int jpaAnnotation; int springData; int jdbc; int mybatis; int jooq;
        List<String> examples = new ArrayList<String>();
    }
}
