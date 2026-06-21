package com.mcp.sailibrary.plugin.mcp.adapters.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.mcp.sailibrary.plugin.mcp.core.ModelChannel;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.RequestFormatKind;
import com.mcp.sailibrary.plugin.mcp.core.ResponseFormatKind;
import com.mcp.sailibrary.plugin.mcp.core.TransportKind;
import com.mcp.sailibrary.plugin.mcp.multimodel.routing.DefaultMcpModelNameResolver;
import com.mcp.sailibrary.plugin.mcp.ports.ModelExecutionProfileResolver;

/* class_context: feature: model execution profile resolution java_version: 17 objective: carregar perfil de execucao com fallback seguro para classpath e filesystem libs: - java.io - java.util.Properties */
public class PropertiesBackedModelExecutionProfileResolver implements ModelExecutionProfileResolver {

    public static final String DEFAULT_RESOURCE_PATH = "/mcp-models.properties";
    public static final String DEFAULT_LEGACY_URL = "https://sai-library.saiapplications.com/api/mcp";
    public static final String DEFAULT_STREAMING_URL = "https://sai-library.saiapplications.com/api/streaming-responses/stream";
    public static final String DEFAULT_SAI_BASE_URL = "https://sai-library.saiapplications.com";
    public static final String DEFAULT_SAI_TEMPLATE_ID = "689529a9bc6f5e4a68e53eb3";

    private final Properties properties;
    private final DefaultMcpModelNameResolver defaultLegacyModelNameResolver;
    private final Class<?> resourceAnchor;

    /* * Feature: inicializa o resolver com ancora padrao. * Data: 2026-05-27 00:00 * Chamado por: construcao da classe em producao e testes * Chama: construtor principal * Objetivo: manter o caminho padrao de configuracao. */
    public PropertiesBackedModelExecutionProfileResolver() {
        this(DEFAULT_RESOURCE_PATH, PropertiesBackedModelExecutionProfileResolver.class);
    }

    /* * Feature: inicializa o resolver com resource customizado. * Data: 2026-05-27 00:00 * Chamado por: codigo de configuracao e testes * Chama: construtor principal * Objetivo: permitir override simples do arquivo de properties. */
    public PropertiesBackedModelExecutionProfileResolver(String resourcePath) {
        this(resourcePath, PropertiesBackedModelExecutionProfileResolver.class);
    }

    /* * Feature: inicializa dependencias e carrega configuracao. * Data: 2026-05-27 00:00 * Chamado por: demais construtores * Chama: loadProperties * Objetivo: preparar resolucao de perfis sem depender de estado externo oculto. */
    public PropertiesBackedModelExecutionProfileResolver(String resourcePath, Class<?> resourceAnchor) {
        this.properties = new Properties();
        this.defaultLegacyModelNameResolver = new DefaultMcpModelNameResolver();
        this.resourceAnchor = resourceAnchor != null
                ? resourceAnchor
                : PropertiesBackedModelExecutionProfileResolver.class;
        loadProperties(resourcePath);
    }

    @Override
    public ModelExecutionProfile resolve(ModelChannel channel) {
        if (channel == null) {
            throw new IllegalArgumentException("Erro Operacional: ModelChannel nao pode ser nulo.");
        }

        TransportKind transportKind = resolveTransportKind(channel);
        RequestFormatKind requestFormatKind = resolveRequestFormatKind(channel, transportKind);
        ResponseFormatKind responseFormatKind = resolveResponseFormatKind(channel, transportKind);

        ModelExecutionProfile profile = new ModelExecutionProfile();
        profile.setChannel(channel);
        profile.setTransportKind(transportKind);
        profile.setRequestFormatKind(requestFormatKind);
        profile.setResponseFormatKind(responseFormatKind);

        profile.setEndpointUrl(resolveEndpointUrl(channel, transportKind));

        profile.setLegacyModelAlias(resolveLegacyModelAlias(channel));
        profile.setStreamingModelName(resolveStreamingModelName(channel));

        profile.setCreativity(resolveCreativity(channel));
        profile.setMaxTokens(resolveMaxTokens(channel));

        profile.setEnableStreaming(resolveBoolean("mcp.streaming.enableStreaming", true));
        profile.setFileSearch(resolveBoolean("mcp.streaming.fileSearch", true));
        profile.setCodeInterpreter(resolveBoolean("mcp.streaming.codeInterpreter", true));
        profile.setWebSearch(resolveBoolean("mcp.streaming.webSearch", true));

        profile.setConversationId(resolveString("mcp.streaming.conversationId", ""));
        profile.setWorkspaceId(resolveString("mcp.streaming.workspaceId", ""));
        profile.setInstructions(resolveString("mcp.streaming.instructions", ""));

        profile.setIndexerEnabled(resolveBoolean("mcp.streaming.indexerEnabled", false));
        profile.setIndexerHash(resolveString("mcp.streaming.indexerHash", ""));
        profile.setIndexerDescription(resolveString("mcp.streaming.indexerDescription", ""));
        profile.setIncludeIndexerMetadata(resolveBoolean("mcp.streaming.includeIndexerMetadata", false));
        profile.setCreateScheduledTask(resolveBoolean("mcp.streaming.createScheduledTask", false));
        profile.setIndexerNumberOfDocuments(resolveInteger("mcp.streaming.indexerNumberOfDocuments", Integer.valueOf(3)));

        String defaultApiKey = resolveString(
                "mcp.legacy.apiKey",
                resolveString("mcp.credentials.apiKey", "")
        );
        profile.setDefaultApiKey(defaultApiKey);

        String defaultCookieValue = resolveString(
                "sai.cookie",
                resolveString(
                        "mcp.saiChat.cookieValue",
                        resolveString(
                                "mcp.streaming.cookieValue",
                                resolveString(
                                        "mcp.streaming.cookie",
                                        resolveString(
                                                "mcp.credentials.cookieValue",
                                                resolveString("mcp.credentials.cookie", "")
                                        )
                                )
                        )
                )
        );
        profile.setDefaultCookieValue(defaultCookieValue);

        System.out.println("[MCP PROFILE DEBUG] ==================================================");
        System.out.println("[MCP PROFILE DEBUG] channel=" + channel.name());
        System.out.println("[MCP PROFILE DEBUG] transportKind=" + profile.getTransportKind());
        System.out.println("[MCP PROFILE DEBUG] requestFormatKind=" + profile.getRequestFormatKind());
        System.out.println("[MCP PROFILE DEBUG] responseFormatKind=" + profile.getResponseFormatKind());
        System.out.println("[MCP PROFILE DEBUG] endpointUrl=" + profile.getEndpointUrl());
        System.out.println("[MCP PROFILE DEBUG] legacyModelAlias=" + profile.getLegacyModelAlias());
        System.out.println("[MCP PROFILE DEBUG] streamingModelName=" + profile.getStreamingModelName());
        System.out.println("[MCP PROFILE DEBUG] creativity=" + profile.getCreativity());
        System.out.println("[MCP PROFILE DEBUG] maxTokens=" + profile.getMaxTokens());
        System.out.println("[MCP PROFILE DEBUG] enableStreaming=" + profile.isEnableStreaming());
        System.out.println("[MCP PROFILE DEBUG] fileSearch=" + profile.isFileSearch());
        System.out.println("[MCP PROFILE DEBUG] codeInterpreter=" + profile.isCodeInterpreter());
        System.out.println("[MCP PROFILE DEBUG] webSearch=" + profile.isWebSearch());
        System.out.println("[MCP PROFILE DEBUG] conversationId=" + profile.getConversationId());
        System.out.println("[MCP PROFILE DEBUG] workspaceId=" + profile.getWorkspaceId());
        System.out.println("[MCP PROFILE DEBUG] defaultApiKeyConfigured=" + (!isBlank(profile.getDefaultApiKey()) ? "true" : "false"));
        System.out.println("[MCP PROFILE DEBUG] defaultCookieConfigured=" + (!isBlank(profile.getDefaultCookieValue()) ? "true" : "false"));
        System.out.println("[MCP PROFILE DEBUG] ==================================================");

        return profile;
    }

    /* * Feature: carrega properties com fallback para ambiente de teste PDE. * Data: 2026-05-27 00:00 * Chamado por: construtor principal * Chama: openResourceStream, locateResourceFile, closeQuietly * Objetivo: evitar queda para defaults quando o resource nao entra no bundle. */
    /** * Caller: * - construtores da classe * * Chama: * - openPropertiesStream * * Objetivo: * - Carregar properties do classpath ou do filesystem para nao cair em defaults indevidos * * Alterado em: 2026-05-27 03:45 */
    private void loadProperties(String resourcePath) {
        if (isBlank(resourcePath)) {
            System.out.println("[MCP PROFILE DEBUG] resourcePath vazio. Usando apenas defaults.");
            return;
        }

        InputStream inputStream = null;
        try {
            System.out.println("[MCP PROFILE DEBUG] Tentando carregar resource: " + resourcePath);

            inputStream = openPropertiesStream(resourcePath);
            if (inputStream == null) {
                System.out.println("[MCP PROFILE DEBUG] Resource nao encontrado. Usando defaults.");
                return;
            }

            properties.load(inputStream);

            System.out.println("[MCP PROFILE DEBUG] Resource carregado com sucesso.");
            System.out.println("[MCP PROFILE DEBUG] quantidadeDeProperties=" + properties.size());
            System.out.println("[MCP PROFILE DEBUG] debug.config.marker=" + properties.getProperty("debug.config.marker", ""));
            System.out.println("[MCP PROFILE DEBUG] prop.mcp.transport.planner=" + properties.getProperty("mcp.transport.planner", ""));
            System.out.println("[MCP PROFILE DEBUG] prop.mcp.transport.context.naming=" + properties.getProperty("mcp.transport.context.naming", ""));
            System.out.println("[MCP PROFILE DEBUG] prop.sai.baseUrl=" + properties.getProperty("sai.baseUrl", ""));
            System.out.println("[MCP PROFILE DEBUG] prop.sai.templateId=" + properties.getProperty("sai.templateId", ""));
            System.out.println("[MCP PROFILE DEBUG] prop.sai.cookie.configured=" + (!isBlank(properties.getProperty("sai.cookie", "")) ? "true" : "false"));
        } catch (Exception exception) {
            System.out.println("[MCP PROFILE DEBUG] Falha ao carregar properties: " + exception.getMessage());
            exception.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException exception) {
                }
            }
        }
    }

    /** * Caller: * - loadProperties * * Chama: * - locateResourceOnFileSystem * * Objetivo: * - Tentar classpath primeiro e filesystem depois * * Alterado em: 2026-05-27 03:45 */
    private InputStream openPropertiesStream(String resourcePath) {
        String normalizedResourcePath = resourcePath != null ? resourcePath.trim() : "";
        String classLoaderResourcePath = normalizedResourcePath.startsWith("/")
                ? normalizedResourcePath.substring(1)
                : normalizedResourcePath;

        InputStream inputStream = resourceAnchor.getResourceAsStream(normalizedResourcePath);
        if (inputStream != null) {
            System.out.println("[MCP PROFILE DEBUG] Resource encontrado via resourceAnchor.getResourceAsStream.");
            return inputStream;
        }

        ClassLoader anchorClassLoader = resourceAnchor.getClassLoader();
        if (anchorClassLoader != null) {
            inputStream = anchorClassLoader.getResourceAsStream(classLoaderResourcePath);
            if (inputStream != null) {
                System.out.println("[MCP PROFILE DEBUG] Resource encontrado via resourceAnchor.getClassLoader.");
                return inputStream;
            }
        }

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            inputStream = contextClassLoader.getResourceAsStream(classLoaderResourcePath);
            if (inputStream != null) {
                System.out.println("[MCP PROFILE DEBUG] Resource encontrado via contextClassLoader.");
                return inputStream;
            }
        }

        File filesystemResource = locateResourceOnFileSystem(normalizedResourcePath, classLoaderResourcePath);
        if (filesystemResource != null && filesystemResource.isFile()) {
            try {
                System.out.println("[MCP PROFILE DEBUG] Resource encontrado via filesystem: " + filesystemResource.getAbsolutePath());
                return new FileInputStream(filesystemResource);
            } catch (Exception exception) {
                System.out.println("[MCP PROFILE DEBUG] Falha ao abrir arquivo no filesystem: " + exception.getMessage());
            }
        }

        return null;
    }

    /** * Caller: * - openPropertiesStream * * Objetivo: * - Procurar o properties em locais comuns do plugin de testes e do workspace * * Alterado em: 2026-05-27 03:45 */
    private File locateResourceOnFileSystem(String normalizedResourcePath, String classLoaderResourcePath) {
        File directFile = new File(normalizedResourcePath);
        if (directFile.isFile()) {
            return directFile.getAbsoluteFile();
        }

        File relativeFile = new File(classLoaderResourcePath);
        if (relativeFile.isFile()) {
            return relativeFile.getAbsoluteFile();
        }

        File currentDirectory = new File(System.getProperty("user.dir"));
        for (int depth = 0; depth < 8 && currentDirectory != null; depth++) {
            File[] candidates = new File[] {
                    new File(currentDirectory, classLoaderResourcePath),
                    new File(currentDirectory, normalizedResourcePath),
                    new File(currentDirectory, "src/" + classLoaderResourcePath),
                    new File(currentDirectory, "resources/" + classLoaderResourcePath),
                    new File(currentDirectory, "src/main/resources/" + classLoaderResourcePath),
                    new File(currentDirectory, "src/test/resources/" + classLoaderResourcePath),
                    new File(currentDirectory, "com.mcp.sailibrary.tests/" + classLoaderResourcePath),
                    new File(currentDirectory, "com.mcp.sailibrary.tests/src/" + classLoaderResourcePath),
                    new File(currentDirectory, "../com.mcp.sailibrary.tests/" + classLoaderResourcePath),
                    new File(currentDirectory, "../com.mcp.sailibrary.tests/src/" + classLoaderResourcePath)
            };

            for (int index = 0; index < candidates.length; index++) {
                File currentCandidate = candidates[index];
                if (currentCandidate.isFile()) {
                    return currentCandidate.getAbsoluteFile();
                }
            }

            currentDirectory = currentDirectory.getParentFile();
        }

        return null;
    }

    /* * Feature: tenta abrir o resource pelas estrategias mais previsiveis do bundle. * Data: 2026-05-27 00:00 * Chamado por: loadProperties * Chama: nenhuma * Objetivo: cobrir classpath por ancora e por classloader. */
    private InputStream openResourceStream(String normalizedResourcePath) {
        InputStream inputStream = resourceAnchor.getResourceAsStream(normalizedResourcePath);
        if (inputStream != null) {
            return inputStream;
        }

        String classLoaderPath = normalizedResourcePath.startsWith("/")
                ? normalizedResourcePath.substring(1)
                : normalizedResourcePath;

        ClassLoader classLoader = resourceAnchor.getClassLoader();
        if (classLoader != null) {
            inputStream = classLoader.getResourceAsStream(classLoaderPath);
            if (inputStream != null) {
                return inputStream;
            }
        }

        inputStream = PropertiesBackedModelExecutionProfileResolver.class.getResourceAsStream(normalizedResourcePath);
        if (inputStream != null) {
            return inputStream;
        }

        ClassLoader fallbackClassLoader = PropertiesBackedModelExecutionProfileResolver.class.getClassLoader();
        if (fallbackClassLoader != null) {
            return fallbackClassLoader.getResourceAsStream(classLoaderPath);
        }

        return null;
    }

    /* * Feature: localiza arquivo no filesystem do projeto de testes ou do workspace. * Data: 2026-05-27 00:00 * Chamado por: loadProperties * Chama: nenhuma * Objetivo: permitir execucao em PDE quando o build nao incluiu o resource. */
    private File locateResourceFile(String normalizedResourcePath) {
        String fileName = normalizedResourcePath.startsWith("/")
                ? normalizedResourcePath.substring(1)
                : normalizedResourcePath;

        File currentDirectory = new File(System.getProperty("user.dir"));

        for (int depth = 0; depth < 8 && currentDirectory != null; depth++) {
            File[] candidates = new File[] {
                    new File(currentDirectory, fileName),
                    new File(currentDirectory, "src/" + fileName),
                    new File(currentDirectory, "resources/" + fileName),
                    new File(currentDirectory, "com.mcp.sailibrary.tests/" + fileName),
                    new File(currentDirectory, "com.mcp.sailibrary.tests/src/" + fileName),
                    new File(currentDirectory, "../com.mcp.sailibrary.tests/" + fileName),
                    new File(currentDirectory, "../com.mcp.sailibrary.tests/src/" + fileName)
            };

            for (int index = 0; index < candidates.length; index++) {
                File candidateFile = candidates[index];
                if (candidateFile.isFile()) {
                    return candidateFile.getAbsoluteFile();
                }
            }

            currentDirectory = currentDirectory.getParentFile();
        }

        return null;
    }

    /* * Feature: resolve o transporte do canal. * Data: 2026-05-27 00:00 * Chamado por: resolve * Chama: hasNonBlankProperty * Objetivo: priorizar configuracao explicita e manter fallback seguro. */
    private TransportKind resolveTransportKind(ModelChannel channel) {
        String key = "mcp.transport." + channel.getPropertySuffix();

        if (hasNonBlankProperty(key)) {
            TransportKind resolved = TransportKind.fromProperty(properties.getProperty(key));
            System.out.println("[MCP PROFILE DEBUG] resolveTransportKind channel=" + channel.name()
                    + " key=" + key
                    + " raw=" + properties.getProperty(key)
                    + " resolved=" + resolved);
            return resolved;
        }

        System.out.println("[MCP PROFILE DEBUG] resolveTransportKind channel=" + channel.name()
                + " key=" + key
                + " raw="
                + " resolved=" + TransportKind.SAI_CHAT_EXECUTE_HTTP);
        return TransportKind.SAI_CHAT_EXECUTE_HTTP;
    }

    /* * Feature: resolve o formato de request conforme canal e transporte. * Data: 2026-05-27 00:00 * Chamado por: resolve * Chama: hasNonBlankProperty * Objetivo: evitar payload incorreto entre legado e streaming. */
    private RequestFormatKind resolveRequestFormatKind(ModelChannel channel, TransportKind transportKind) {
        String key = "mcp.requestFormat." + channel.getPropertySuffix();

        if (hasNonBlankProperty(key)) {
            RequestFormatKind resolved = RequestFormatKind.fromProperty(properties.getProperty(key));
            System.out.println("[MCP PROFILE DEBUG] resolveRequestFormatKind channel=" + channel.name()
                    + " key=" + key
                    + " raw=" + properties.getProperty(key)
                    + " resolved=" + resolved);
            return resolved;
        }

        if (TransportKind.STREAMING_SSE_HTTP.equals(transportKind)) {
            return RequestFormatKind.STREAMING_PROMPT;
        }

        if (TransportKind.LEGACY_JSON_RPC_HTTP.equals(transportKind)) {
            return RequestFormatKind.LEGACY_MCP_TOOLS_CALL;
        }

        return RequestFormatKind.SAI_CHAT_EXECUTE;
    }

    /* * Feature: resolve o formato de response conforme canal e transporte. * Data: 2026-05-27 00:00 * Chamado por: resolve * Chama: hasNonBlankProperty * Objetivo: garantir codec compativel com o protocolo recebido. */
    private ResponseFormatKind resolveResponseFormatKind(ModelChannel channel, TransportKind transportKind) {
        String key = "mcp.responseFormat." + channel.getPropertySuffix();

        if (hasNonBlankProperty(key)) {
            ResponseFormatKind resolved = ResponseFormatKind.fromProperty(properties.getProperty(key));
            System.out.println("[MCP PROFILE DEBUG] resolveResponseFormatKind channel=" + channel.name()
                    + " key=" + key
                    + " raw=" + properties.getProperty(key)
                    + " resolved=" + resolved);
            return resolved;
        }

        if (TransportKind.STREAMING_SSE_HTTP.equals(transportKind)) {
            return ResponseFormatKind.STREAMING_SSE_EVENTS;
        }

        if (TransportKind.LEGACY_JSON_RPC_HTTP.equals(transportKind)) {
            return ResponseFormatKind.LEGACY_MCP_ENVELOPE;
        }

        return ResponseFormatKind.SAI_CHAT_EXECUTE_JSON;
    }

    /* * Feature: resolve a url do endpoint conforme transporte. * Data: 2026-05-27 00:00 * Chamado por: resolve * Chama: resolveString * Objetivo: separar endpoint legado do endpoint SSE. */
    private String resolveEndpointUrl(ModelChannel channel, TransportKind transportKind) {
        if (TransportKind.STREAMING_SSE_HTTP.equals(transportKind)) {
            return resolveString("mcp.streaming.url", DEFAULT_STREAMING_URL);
        }

        if (TransportKind.SAI_CHAT_EXECUTE_HTTP.equals(transportKind)) {
            String explicitUrl = resolveString("mcp.saiChat.url." + channel.getPropertySuffix(), "");
            if (!isBlank(explicitUrl)) {
                return explicitUrl;
            }

            explicitUrl = resolveString("mcp.saiChat.url", "");
            if (!isBlank(explicitUrl)) {
                return explicitUrl;
            }

            String baseUrl = resolveString("sai.baseUrl", "https://sai-library.saiapplications.com");
            String templateId = resolveSaiTemplateId(channel);
            System.out.println("[MCP PROFILE DEBUG] saiTemplateId=" + templateId);
            return stripRightSlash(baseUrl) + "/api/templates/" + templateId + "/chatexecute";
        }

        return resolveString("mcp.legacy.url", DEFAULT_LEGACY_URL);
    }
    private String resolveSaiTemplateId(ModelChannel channel) {
        String channelKey = "sai.templateId." + channel.getPropertySuffix();

        if (hasNonBlankProperty(channelKey)) {
            return properties.getProperty(channelKey).trim();
        }

        String alternativeChannelKey = "mcp.saiChat.templateId." + channel.getPropertySuffix();
        if (hasNonBlankProperty(alternativeChannelKey)) {
            return properties.getProperty(alternativeChannelKey).trim();
        }

        return resolveString("sai.templateId", "689529a9bc6f5e4a68e53eb3");
    }

    private String stripRightSlash(String value) {
        String safeValue = value != null ? value.trim() : "";

        while (safeValue.endsWith("/")) {
            safeValue = safeValue.substring(0, safeValue.length() - 1);
        }

        return safeValue;
    }

    /* * Feature: resolve o alias legado por canal. * Data: 2026-05-27 00:00 * Chamado por: resolve * Chama: hasNonBlankProperty, resolveDefaultLegacyModelAlias * Objetivo: manter configuracao explicita e fallback coerente. */
    private String resolveLegacyModelAlias(ModelChannel channel) {
        String key = "mcp.model." + channel.getPropertySuffix();

        if (hasNonBlankProperty(key)) {
            return properties.getProperty(key).trim();
        }

        return resolveDefaultLegacyModelAlias(channel);
    }

    /* * Feature: resolve o modelo de streaming por canal. * Data: 2026-05-27 00:00 * Chamado por: resolve * Chama: hasNonBlankProperty * Objetivo: usar modelo SSE correto por papel cognitivo. */
    private String resolveStreamingModelName(ModelChannel channel) {
        String key = "mcp.streaming.model." + channel.getPropertySuffix();

        if (hasNonBlankProperty(key)) {
            return properties.getProperty(key).trim();
        }

        if (ModelChannel.INVESTIGATOR.equals(channel)) {
            return "gpt-5.4-2026-03-05";
        }

        if (ModelChannel.PLANNER.equals(channel)) {
            return "gpt-5.4-2026-03-05";
        }

        if (ModelChannel.CODE_GENERATOR.equals(channel)) {
            return "gpt-5.2-codex";
        }

        if (ModelChannel.CODE_AUDITOR.equals(channel)) {
            return "claude-sonnet-4-6";
        }

        if (ModelChannel.SUMMARIZER.equals(channel)) {
            return "gpt-5.4-mini-2026-03-17";
        }

        return "gpt-5.4-2026-03-05";
    }

    /* * Feature: resolve criatividade por canal. * Data: 2026-05-27 00:00 * Chamado por: resolve * Chama: hasNonBlankProperty, parseDouble, defaultCreativity, clampCreativity * Objetivo: evitar valores fora da faixa aceita. */
    private Double resolveCreativity(ModelChannel channel) {
        String key = "mcp.creativity." + channel.getPropertySuffix();

        if (hasNonBlankProperty(key)) {
            return clampCreativity(parseDouble(properties.getProperty(key), defaultCreativity(channel)));
        }

        return clampCreativity(defaultCreativity(channel));
    }

    /* * Feature: resolve max tokens por canal. * Data: 2026-05-27 00:00 * Chamado por: resolve * Chama: hasNonBlankProperty, resolveInteger, defaultMaxTokens * Objetivo: manter teto seguro de resposta. */
    private Integer resolveMaxTokens(ModelChannel channel) {
        String key = "mcp.maxTokens." + channel.getPropertySuffix();

        if (hasNonBlankProperty(key)) {
            return resolveInteger(key, Integer.valueOf(defaultMaxTokens(channel)));
        }

        return Integer.valueOf(defaultMaxTokens(channel));
    }

    /* * Feature: resolve fallback do alias legado pelo resolver padrao. * Data: 2026-05-27 00:00 * Chamado por: resolveLegacyModelAlias * Chama: DefaultMcpModelNameResolver * Objetivo: manter compatibilidade quando properties nao definem o alias. */
    private String resolveDefaultLegacyModelAlias(ModelChannel channel) {
        if (ModelChannel.INVESTIGATOR.equals(channel)) {
            return defaultLegacyModelNameResolver.resolveInvestigatorModelName();
        }

        if (ModelChannel.PLANNER.equals(channel)) {
            return defaultLegacyModelNameResolver.resolvePlannerModelName();
        }

        if (ModelChannel.CODE_GENERATOR.equals(channel)) {
            return defaultLegacyModelNameResolver.resolveCodeGeneratorModelName();
        }

        if (ModelChannel.CODE_AUDITOR.equals(channel)) {
            return defaultLegacyModelNameResolver.resolveCodeAuditorModelName();
        }

        if (ModelChannel.SUMMARIZER.equals(channel)) {
            return defaultLegacyModelNameResolver.resolveSummarizerModelName();
        }

        return defaultLegacyModelNameResolver.resolvePlannerModelName();
    }

    /* * Feature: default de criatividade por canal. * Data: 2026-05-27 00:00 * Chamado por: resolveCreativity * Chama: nenhuma * Objetivo: manter defaults previsiveis. */
    private double defaultCreativity(ModelChannel channel) {
        if (ModelChannel.INVESTIGATOR.equals(channel)) {
            return 0.10d;
        }

        if (ModelChannel.PLANNER.equals(channel)) {
            return 0.10d;
        }

        if (ModelChannel.CODE_GENERATOR.equals(channel)) {
            return 0.00d;
        }

        if (ModelChannel.CODE_AUDITOR.equals(channel)) {
            return 0.00d;
        }

        if (ModelChannel.SUMMARIZER.equals(channel)) {
            return 0.00d;
        }

        return 0.00d;
    }

    /* * Feature: default de tokens por canal. * Data: 2026-05-27 00:00 * Chamado por: resolveMaxTokens * Chama: nenhuma * Objetivo: manter limite consistente por papel. */
    private int defaultMaxTokens(ModelChannel channel) {
        if (ModelChannel.SUMMARIZER.equals(channel)) {
            return 8192;
        }

        return 16384;
    }

    /* * Feature: verifica se existe property util. * Data: 2026-05-27 00:00 * Chamado por: metodos de resolucao * Chama: isBlank * Objetivo: reduzir leitura de valor vazio como configuracao valida. */
    private boolean hasNonBlankProperty(String key) {
        if (isBlank(key)) {
            return false;
        }

        String value = properties.getProperty(key);
        return value != null && value.trim().length() > 0;
    }

    /* * Feature: resolve string com fallback. * Data: 2026-05-27 00:00 * Chamado por: metodos de resolucao * Chama: nenhuma * Objetivo: evitar nulos na montagem do perfil. */
    private String resolveString(String key, String defaultValue) {
        String value = properties.getProperty(key);

        if (value == null || value.trim().length() == 0) {
            return defaultValue;
        }

        return value.trim();
    }

    /* * Feature: resolve boolean tolerante. * Data: 2026-05-27 00:00 * Chamado por: resolve * Chama: nenhuma * Objetivo: aceitar formatos textuais comuns sem quebrar configuracao. */
    private boolean resolveBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);

        if (value == null || value.trim().length() == 0) {
            return defaultValue;
        }

        String normalizedValue = value.trim();

        return "true".equalsIgnoreCase(normalizedValue)
                || "1".equals(normalizedValue)
                || "yes".equalsIgnoreCase(normalizedValue)
                || "sim".equalsIgnoreCase(normalizedValue);
    }

    /* * Feature: resolve inteiro com fallback. * Data: 2026-05-27 00:00 * Chamado por: resolveMaxTokens e resolve * Chama: nenhuma * Objetivo: evitar valores invalidos no perfil. */
    private Integer resolveInteger(String key, Integer defaultValue) {
        String value = properties.getProperty(key);

        if (value == null || value.trim().length() == 0) {
            return defaultValue;
        }

        try {
            int parsedValue = Integer.parseInt(value.trim());
            if (parsedValue <= 0) {
                return defaultValue;
            }
            return Integer.valueOf(parsedValue);
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    /* * Feature: parse de double tolerante. * Data: 2026-05-27 00:00 * Chamado por: resolveCreativity * Chama: nenhuma * Objetivo: evitar quebra por valor numerico mal formatado. */
    private double parseDouble(String value, double defaultValue) {
        if (value == null || value.trim().length() == 0) {
            return defaultValue;
        }

        try {
            return Double.parseDouble(value.trim());
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    /* * Feature: limita criatividade na faixa valida. * Data: 2026-05-27 00:00 * Chamado por: resolveCreativity * Chama: nenhuma * Objetivo: manter contrato do profile. */
    private Double clampCreativity(double value) {
        double safeValue = value;

        if (safeValue < 0.0d) {
            safeValue = 0.0d;
        }

        if (safeValue > 1.0d) {
            safeValue = 1.0d;
        }

        return Double.valueOf(safeValue);
    }

    /* * Feature: normaliza o nome do resource. * Data: 2026-05-27 00:00 * Chamado por: loadProperties * Chama: isBlank * Objetivo: manter lookup estavel entre classpath e filesystem. */
    private String normalizeResourcePath(String resourcePath) {
        if (isBlank(resourcePath)) {
            return DEFAULT_RESOURCE_PATH;
        }

        String trimmedPath = resourcePath.trim();
        if (trimmedPath.startsWith("/")) {
            return trimmedPath;
        }

        return "/" + trimmedPath;
    }

    /* * Feature: fecha stream sem ruido operacional. * Data: 2026-05-27 00:00 * Chamado por: loadProperties * Chama: nenhuma * Objetivo: liberar recurso com baixo acoplamento. */
    private void closeQuietly(InputStream inputStream) {
        if (inputStream == null) {
            return;
        }

        try {
            inputStream.close();
        } catch (IOException exception) {
        }
    }

    /* * Feature: valida texto vazio. * Data: 2026-05-27 00:00 * Chamado por: helpers internos * Chama: nenhuma * Objetivo: evitar duplicacao de regra de branco. */
    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}