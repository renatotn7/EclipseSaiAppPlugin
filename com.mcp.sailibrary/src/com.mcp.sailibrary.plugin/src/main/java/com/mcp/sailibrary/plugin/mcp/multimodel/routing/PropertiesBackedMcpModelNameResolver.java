package com.mcp.sailibrary.plugin.mcp.multimodel.routing;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/* --- version: "1.0" libraries: - InputStream - Properties objetivo: "Resolver nomes de modelos MCP a partir de arquivo properties, preservando fallback seguro para os valores padrao." --- */

/** * Resolver de nomes de modelos MCP baseado em arquivo properties. * * <p>Esta implementacao permite externalizar a configuracao dos modelos sem * espalhar nomes hardcoded pelo codigo. Em caso de falha de leitura ou ausencia * de propriedade, os valores padrao continuam sendo usados.</p> * * @author Renato Tomaz Nati * @since 2026-05-24 */
public class PropertiesBackedMcpModelNameResolver implements ModelNameResolver {

    private static final String DEFAULT_RESOURCE_PATH = "/mcp-models.properties";

    private DefaultMcpModelNameResolver defaultResolver;
    private Properties properties;

    /** * Caller: Bootstrapping da camada MCP * Callee: PropertiesBackedMcpModelNameResolver(String) * Objetivo: Inicializar o resolver com o caminho padrao do arquivo de propriedades. * Data modificacao: 2026-05-24 00:00 * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public PropertiesBackedMcpModelNameResolver() {
        this(DEFAULT_RESOURCE_PATH);
    }

    /** * Caller: Bootstrapping da camada MCP * Callee: carregarProperties * Objetivo: Inicializar o resolver usando um resource path explicito. * Feature: Mantem fallback seguro para os modelos padrao em caso de falha de leitura. * Data modificacao: 2026-05-24 00:00 * * @param resourcePath caminho do arquivo properties no classpath * * @author Renato Tomaz Nati * @since 2026-05-24 */
    public PropertiesBackedMcpModelNameResolver(String resourcePath) {
        this.defaultResolver = new DefaultMcpModelNameResolver();
        this.properties = new Properties();
        carregarProperties(resourcePath);
    }

    @Override
    public String resolveInvestigatorModelName() {
        return resolveString("mcp.model.investigator", defaultResolver.resolveInvestigatorModelName());
    }

    @Override
    public String resolvePlannerModelName() {
        return resolveString("mcp.model.planner", defaultResolver.resolvePlannerModelName());
    }

    @Override
    public String resolveCodeGeneratorModelName() {
        return resolveString("mcp.model.code.generator", defaultResolver.resolveCodeGeneratorModelName());
    }

    @Override
    public String resolveCodeAuditorModelName() {
        return resolveString("mcp.model.code.auditor", defaultResolver.resolveCodeAuditorModelName());
    }

    @Override
    public String resolveSummarizerModelName() {
          return resolveString("mcp.model.summarizer", defaultResolver.resolveSummarizerModelName());
    }

   
    private void carregarProperties(String resourcePath) {
        if (isBlank(resourcePath)) {
            System.out.println("[MCP CONFIG DEBUG] resourcePath vazio. Usando apenas defaults.");
            return;
        }

        InputStream inputStream = null;
        try {
            System.out.println("[MCP CONFIG DEBUG] Tentando carregar resource: " + resourcePath);

            inputStream = PropertiesBackedMcpModelNameResolver.class.getResourceAsStream(resourcePath);
            if (inputStream == null) {
                System.out.println("[MCP CONFIG DEBUG] Resource nao encontrado no classpath. Usando defaults.");
                return;
            }

            properties.load(inputStream);

            System.out.println("[MCP CONFIG DEBUG] Resource carregado com sucesso.");
            System.out.println("[MCP CONFIG DEBUG] mcp.model.investigator=" + properties.getProperty("mcp.model.investigator"));
            System.out.println("[MCP CONFIG DEBUG] mcp.model.planner=" + properties.getProperty("mcp.model.planner"));
            System.out.println("[MCP CONFIG DEBUG] mcp.model.code.generator=" + properties.getProperty("mcp.model.code.generator"));
            System.out.println("[MCP CONFIG DEBUG] mcp.model.code.auditor=" + properties.getProperty("mcp.model.code.auditor"));
            System.out.println("[MCP CONFIG DEBUG] mcp.model.summarizer=" + properties.getProperty("mcp.model.summarizer"));
        } catch (Exception e) {
            System.out.println("[MCP CONFIG DEBUG] Falha ao carregar resource de configuracao: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /** * Caller: metodos de resolucao publica * Callee: N/A * Objetivo: Resolver string com fallback para valor padrao. * Data modificacao: 2026-05-24 00:00 * * @param key chave da propriedade * @param defaultValue valor padrao * @return valor resolvido * * @author Renato Tomaz Nati * @since 2026-05-24 */
    private String resolveString(String key, String defaultValue) {
        String value = properties.getProperty(key);

        if (value == null || value.trim().length() == 0) {
            System.out.println("[MCP CONFIG DEBUG] Chave [" + key + "] ausente ou vazia. Fallback=" + defaultValue);
            return defaultValue;
        }

        String resolved = value.trim();
        System.out.println("[MCP CONFIG DEBUG] Chave [" + key + "] resolvida para [" + resolved + "]");
        return resolved;
    }

   

    /** * Caller: metodos internos * Callee: N/A * Objetivo: Verificar se a string recebida esta em branco. * Data modificacao: 2026-05-24 00:00 * * @param value valor de entrada * @return true quando a string estiver vazia * * @author Renato Tomaz Nati * @since 2026-05-24 */
    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}