package com.mcp.sailibrary.plugin.mcp.core;

/** * Container simples para as secoes de ferramentas do prompt. * * @author Renato Tomaz Nati * @since 2026-05-26 */
public class ToolPromptSections {

    private String toolsSection;
    private String examplesSection;

    public ToolPromptSections() {
        this.toolsSection = "";
        this.examplesSection = "";
    }

    public ToolPromptSections(String toolsSection, String examplesSection) {
        this.toolsSection = toolsSection != null ? toolsSection : "";
        this.examplesSection = examplesSection != null ? examplesSection : "";
    }

    public String getToolsSection() {
        return toolsSection;
    }

    public void setToolsSection(String toolsSection) {
        this.toolsSection = toolsSection != null ? toolsSection : "";
    }

    public String getExamplesSection() {
        return examplesSection;
    }

    public void setExamplesSection(String examplesSection) {
        this.examplesSection = examplesSection != null ? examplesSection : "";
    }
}