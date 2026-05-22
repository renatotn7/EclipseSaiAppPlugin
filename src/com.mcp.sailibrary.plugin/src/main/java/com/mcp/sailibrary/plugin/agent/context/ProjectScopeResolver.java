package com.mcp.sailibrary.plugin.agent.context;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.core.resources.IProject;

/**
 * ---
 * yaml_header:
 * version: "1.1"
 * dependencies: 
 * - org.eclipse.jdt.core
 * - java.io.File
 * purpose: "Delimitar o perimetro de operacao da IA, identificando a raiz do projeto e o GroupId."
 * ---
 */
public class ProjectScopeResolver {

    /**
 * Descrição não fornecida.
 *
 * @author Renato Tomaz Nati
 */
    public String obterGroupIdRaiz(ICompilationUnit unit) {
        if (unit == null || unit.getJavaProject() == null) return null;

        try {
            IProject project = unit.getJavaProject().getProject();
            if (project == null || project.getLocation() == null) return null;

            File currentDir = project.getLocation().toFile();
            File pomFile = new File(currentDir, "pom.xml");

            while (!pomFile.exists() && currentDir.getParentFile() != null) {
                currentDir = currentDir.getParentFile();
                pomFile = new File(currentDir, "pom.xml");
            }

            if (!pomFile.exists()) return null;

            String conteudo = lerConteudo(pomFile);
            return extrairGroupId(conteudo);
        } catch (Exception e) {
            return null;
        }
    }

    /**
 * Descrição não fornecida.
 *
 * @author Renato Tomaz Nati
 */
    public boolean deveRastrearClasse(String qualifiedName, String groupIdRaiz) {
        if (qualifiedName == null || qualifiedName.trim().isEmpty()) return false;
        if (groupIdRaiz == null || groupIdRaiz.trim().isEmpty()) return true;
        return qualifiedName.startsWith(groupIdRaiz);
    }

    private String lerConteudo(File arquivo) {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(arquivo));
            String linha;
            while ((linha = br.readLine()) != null) {
                sb.append(linha).append("\n");
            }
            br.close();
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String extrairGroupId(String pomContent) {
        if (pomContent == null) return null;
        try {
            Pattern pattern = Pattern.compile("<groupId>\\s*([^<]+?)\\s*</groupId>");
            Matcher matcher = pattern.matcher(pomContent);
            if (matcher.find()) return matcher.group(1).trim();
        } catch (Exception e) {}
        return null;
    }
}