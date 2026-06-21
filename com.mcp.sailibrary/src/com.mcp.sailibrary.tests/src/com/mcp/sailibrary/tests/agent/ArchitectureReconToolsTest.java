package com.mcp.sailibrary.tests.agent;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.mcp.sailibrary.plugin.agent.tools.architecture.ArchitectureOrientationTool;
import com.mcp.sailibrary.plugin.agent.tools.architecture.ExistingProblemReconTool;
import com.mcp.sailibrary.plugin.agent.tools.architecture.NamingConventionDiscoveryTool;
import com.mcp.sailibrary.plugin.agent.tools.architecture.PersistenceStyleDetectionTool;
import com.mcp.sailibrary.plugin.agent.tools.architecture.ProjectStructureInventoryTool;

public class ArchitectureReconToolsTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void deveInventariarEDescobrirPadroes() throws Exception {
        File root = tempFolder.newFolder("proj");
        File src = new File(root, "src/main/java/com/acme");
        src.mkdirs();

        write(new File(root, "pom.xml"), "<project></project>");
        write(new File(src, "UsuarioController.java"), "class UsuarioController {}");
        write(new File(src, "UsuarioService.java"), "class UsuarioService {}");
        write(new File(src, "UsuarioRepository.java"), "interface UsuarioRepository extends JpaRepository<Usuario, Long> {}");
        write(new File(src, "UsuarioEntity.java"), "@Entity @Table(name=\"TB_USUARIO\") class UsuarioEntity {}");

        String inv = new ProjectStructureInventoryTool(root).execute("{\"path\":\"\",\"limite\":\"50\"}");
        assertTrue(inv.contains(".java"));
        assertTrue(inv.contains("pom.xml"));

        String naming = new NamingConventionDiscoveryTool(root).execute("{\"path\":\"\",\"min_ocorrencias\":\"1\"}");
        assertTrue(naming.contains("Controller"));
        assertTrue(naming.contains("Repository"));

        String persistence = new PersistenceStyleDetectionTool(root).execute("{\"path\":\"\"}");
        assertTrue(persistence.contains("Spring Data JPA"));
        assertTrue(persistence.contains("JPA annotations"));
    }

    @Test
    public void deveRastrearProblemasPreexistentesEMapaOperacional() throws Exception {
        File root = tempFolder.newFolder("legacy");
        File src = new File(root, "src/main/java");
        src.mkdirs();

        write(new File(src, "Dao.java"), "class Dao { void x(){ System.out.println(\"debug\"); String sql = \"select * from T where ID=\" + id; } }");

        String problemas = new ExistingProblemReconTool(root).execute("{\"path\":\"\",\"limite\":\"20\"}");
        assertTrue(problemas.contains("DEBUG_SYSTEM_OUT"));
        assertTrue(problemas.contains("SQL_CONCATENADO"));

        String mapa = new ArchitectureOrientationTool(root).execute("{\"path\":\"\",\"profundidade\":\"alta\"}");
        assertTrue(mapa.contains("Mapa Operacional da IA"));
        assertTrue(mapa.contains("Fase 6 - Problemas pre-existentes"));
    }

    private void write(File file, String text) throws Exception {
        Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
    }
}
