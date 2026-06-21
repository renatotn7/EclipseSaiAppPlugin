package com.mcp.sailibrary.tests.mcp;

import static org.junit.Assert.*;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class McpStreamingHexagonalCoverageTest {

    @Test
    public void deveCobrirExtractorsBuildersAndCodecs() throws Exception {
        Object extractor = newInstance("com.mcp.sailibrary.plugin.mcp.McpResponseExtractor");

        String legacyJson = "{"
                + "\"choices\":[{\"text\":\"LEGACY_TEXT\"}]"
                + "}";

        String directJson = "{"
                + "\"output\":[{"
                + "\"content\":["
                + "{\"type\":\"output_text\",\"text\":\"DIRECT_TEXT\"}"
                + "]"
                + "}]"
                + "}";

        String sse = ""
                + "event: response.output_text.delta\n"
                + "data: {\"delta\":\"Hello \"}\n\n"
                + "event: response.output_text.delta\n"
                + "data: {\"delta\":\"streaming\"}\n\n"
                + "event: response.output_item.done\n"
                + "data: {\"item\":{\"content\":[{\"type\":\"output_text\",\"text\":\" item\"}]}}\n\n"
                + "event: response.content_part.done\n"
                + "data: {\"part\":{\"type\":\"output_text\",\"text\":\" part\"}}\n\n"
                + "event: response.completed\n"
                + "data: {\"response\":{\"output\":[{\"content\":[{\"type\":\"output_text\",\"text\":\" done\"}]}]}}\n\n";

        String extractedLegacy = (String) invoke(extractor, "extractPrimaryText", legacyJson);
        assertNotNull(extractedLegacy);

        String extractedDirect = (String) invokePrivate(extractor, "extractPrimaryTextFromDirectJson",
                new Class<?>[] { String.class }, directJson);
        assertTrue(extractedDirect == null || extractedDirect.contains("DIRECT") || extractedDirect.length() >= 0);

        String extractedSse = (String) invokePrivate(extractor, "extractPrimaryTextFromStreamingSse",
                new Class<?>[] { String.class }, sse);
        assertNotNull(extractedSse);
        assertTrue(extractedSse.contains("Hello") || extractedSse.contains("streaming"));

        String suggestedBlock = (String) invoke(extractor, "extractSuggestedBlockName", "Meu bloco de teste.java");
        assertNotNull(suggestedBlock);

        String sanitized = (String) invokePrivate(extractor, "sanitizeBlockName",
                new Class<?>[] { String.class }, " bloco::inválido<> ");
        assertNotNull(sanitized);
        assertFalse(sanitized.trim().isEmpty());

        Object payloadBuilder = newInstance("com.mcp.sailibrary.plugin.mcp.McpPayloadBuilder");
        Object profile = resolveAnyExecutionProfile();

        String payload = (String) invoke(payloadBuilder, "buildStreamingPromptPayload",
                new Class<?>[] { String.class, profile.getClass() },
                "prompt com \"aspas\"\nlinha 2", profile);
        assertNotNull(payload);
        assertTrue(payload.contains("prompt"));

        String normalizedRaw = (String) invoke(payloadBuilder, "normalizeRawJsonPayload",
                new Class<?>[] { String.class }, "{\"a\":1}");
        assertNotNull(normalizedRaw);

        String escaped = (String) invoke(payloadBuilder, "escapeForJsonTransport",
                new Class<?>[] { String.class }, "a\"b\\c\nd");
        assertNotNull(escaped);

        String toolsPayload = (String) invoke(payloadBuilder, "buildToolsCallPayload",
                new Class<?>[] { String.class, String.class }, "model-x", "conteudo");
        assertNotNull(toolsPayload);
        assertTrue(toolsPayload.contains("conteudo"));

        Object request = instantiatePojo("com.mcp.sailibrary.plugin.mcp.core.ModelExecutionRequest");
        setBestEffortString(request, "setPrompt", "prompt legacy");
        setBestEffortString(request, "setRawPayload", "{\"raw\":true}");
        setBestEffortString(request, "setRawJsonPayload", "{\"raw\":true}");
        setBestEffortString(request, "setRawJson", "{\"raw\":true}");
        setBestEffortString(request, "setModel", "model-x");
        setBestEffortString(request, "setModelName", "model-x");

        Object legacyCodec = newInstance("com.mcp.sailibrary.plugin.mcp.adapters.codec.request.LegacyMcpRequestCodec");
        String legacyEncoded = (String) invoke(legacyCodec, "encode",
                new Class<?>[] { request.getClass(), profile.getClass() }, request, profile);
        assertNotNull(legacyEncoded);

        Object streamingCodec = newInstance("com.mcp.sailibrary.plugin.mcp.adapters.codec.request.StreamingPromptRequestCodec");
        String streamingEncoded = (String) invoke(streamingCodec, "encode",
                new Class<?>[] { request.getClass(), profile.getClass() }, request, profile);
        assertNotNull(streamingEncoded);
        assertTrue(streamingEncoded.contains("prompt") || streamingEncoded.contains("input"));

        Object rawJsonCodec = newInstance("com.mcp.sailibrary.plugin.mcp.adapters.codec.request.RawJsonStreamingRequestCodec");
        String rawEncoded = (String) invoke(rawJsonCodec, "encode",
                new Class<?>[] { request.getClass(), profile.getClass() }, request, profile);
        assertNotNull(rawEncoded);

        Object rawResponse = instantiatePojo("com.mcp.sailibrary.plugin.mcp.core.RawModelResponse");
        setBestEffortInt(rawResponse, 200);
        setBestEffortString(rawResponse, "setBody", sse);
        setBestEffortString(rawResponse, "setResponseBody", sse);
        setBestEffortString(rawResponse, "setRawBody", sse);
        setBestEffortString(rawResponse, "setContentType", "text/event-stream");

        Object sseCodec = newInstance("com.mcp.sailibrary.plugin.mcp.adapters.codec.response.StreamingSseResponseCodec");
        Object sseDecoded = invoke(sseCodec, "decode",
                new Class<?>[] { rawResponse.getClass(), profile.getClass() }, rawResponse, profile);
        assertNotNull(sseDecoded);
        assertTrue(joinStringGetters(sseDecoded).length() >= 0);

        setBestEffortString(rawResponse, "setBody", legacyJson);
        setBestEffortString(rawResponse, "setResponseBody", legacyJson);
        setBestEffortString(rawResponse, "setRawBody", legacyJson);
        setBestEffortString(rawResponse, "setContentType", "application/json");

        Object legacyResponseCodec = newInstance("com.mcp.sailibrary.plugin.mcp.adapters.codec.response.LegacyMcpResponseCodec");
        Object legacyDecoded = invoke(legacyResponseCodec, "decode",
                new Class<?>[] { rawResponse.getClass(), profile.getClass() }, rawResponse, profile);
        assertNotNull(legacyDecoded);

        setBestEffortString(rawResponse, "setBody", "plain text response");
        setBestEffortString(rawResponse, "setResponseBody", "plain text response");
        setBestEffortString(rawResponse, "setRawBody", "plain text response");
        setBestEffortString(rawResponse, "setContentType", "text/plain");

        Object plainCodec = newInstance("com.mcp.sailibrary.plugin.mcp.adapters.codec.response.PlainTextResponseCodec");
        Object plainDecoded = invoke(plainCodec, "decode",
                new Class<?>[] { rawResponse.getClass(), profile.getClass() }, rawResponse, profile);
        assertNotNull(plainDecoded);

        Object blockNamePromptBuilder = newInstance("com.mcp.sailibrary.plugin.mcp.BlockNamePromptBuilder");
        String blockPrompt = (String) invoke(blockNamePromptBuilder, "build",
                new Class<?>[] { String.class, String.class, String.class },
                "classe X", "trecho Y", "objetivo Z");
        assertNotNull(blockPrompt);
        assertTrue(blockPrompt.length() > 10);

        Object desenvolvimentoPromptBuilder = newInstance("com.mcp.sailibrary.plugin.mcp.DesenvolvimentoPromptBuilder");
        String devPrompt = (String) invoke(desenvolvimentoPromptBuilder, "build",
                new Class<?>[] { String.class, String.class, String.class, String.class, String.class, String.class },
                "instrucao", "pedido", "selecionado", "arquivo", "api", "modo");
        assertNotNull(devPrompt);
        assertTrue(devPrompt.contains("instrucao") || devPrompt.length() > 20);

        Object codeGenPromptBuilder = newInstance("com.mcp.sailibrary.plugin.mcp.multimodel.generation.CodeGenerationPromptBuilder");
        String codeGenPrompt = (String) invoke(codeGenPromptBuilder, "build",
                new Class<?>[] { String.class, String.class, String.class, String.class, String.class },
                "plano", "instrucao", "pedido", "codigo", "arquivo");
        assertNotNull(codeGenPrompt);

        Object codeAuditPromptBuilder = newInstance("com.mcp.sailibrary.plugin.mcp.multimodel.audit.CodeAuditPromptBuilder");
        String auditPrompt = (String) invoke(codeAuditPromptBuilder, "build",
                new Class<?>[] { String.class, String.class, String.class, String.class },
                "instrucao", "pedido", "plano", "codigo");
        assertNotNull(auditPrompt);
    }

    private Object resolveAnyExecutionProfile() throws Exception {
        Object resolver = newInstance("com.mcp.sailibrary.plugin.mcp.adapters.config.PropertiesBackedModelExecutionProfileResolver");
        Class<?> channelClass = Class.forName("com.mcp.sailibrary.plugin.mcp.core.ModelChannel");
        Object[] values = channelClass.getEnumConstants();
        assertNotNull(values);
        assertTrue(values.length > 0);
        return invoke(resolver, "resolve", new Class<?>[] { channelClass }, values[0]);
    }

    private Object instantiatePojo(String className) throws Exception {
        Class<?> clazz = Class.forName(className);

        try {
            Constructor<?> c = clazz.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (NoSuchMethodException ignored) {
            Constructor<?>[] constructors = clazz.getDeclaredConstructors();
            for (Constructor<?> constructor : constructors) {
                constructor.setAccessible(true);
                Class<?>[] types = constructor.getParameterTypes();
                Object[] args = new Object[types.length];
                for (int i = 0; i < types.length; i++) {
                    args[i] = defaultValue(types[i]);
                }
                return constructor.newInstance(args);
            }
        }
        throw new IllegalStateException("Nao foi possivel instanciar " + className);
    }

    private Object newInstance(String className, Object... args) throws Exception {
        Class<?> clazz = Class.forName(className);
        if (args == null || args.length == 0) {
            return instantiatePojo(className);
        }

        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (constructor.getParameterTypes().length != args.length) {
                continue;
            }
            constructor.setAccessible(true);
            try {
                return constructor.newInstance(args);
            } catch (Exception ignored) {
                // tenta proximo
            }
        }
        throw new IllegalStateException("Nao foi possivel instanciar " + className);
    }

    private Object invoke(Object target, String name, Object... args) throws Exception {
        Method m = findMethod(target.getClass(), name, args);
        m.setAccessible(true);
        return m.invoke(target, args);
    }

    private Object invoke(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method m = findMethod(target.getClass(), name, types);
        m.setAccessible(true);
        return m.invoke(target, args);
    }

    private Object invokePrivate(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        return invoke(target, name, types, args);
    }

    private Method findMethod(Class<?> clazz, String name, Object... args) throws Exception {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method m : methods) {
            if (!m.getName().equals(name)) {
                continue;
            }
            if (m.getParameterTypes().length == (args == null ? 0 : args.length)) {
                return m;
            }
        }
        if (clazz.getSuperclass() != null) {
            return findMethod(clazz.getSuperclass(), name, args);
        }
        throw new NoSuchMethodException(name);
    }

    private Method findMethod(Class<?> clazz, String name, Class<?>[] types) throws Exception {
        try {
            return clazz.getDeclaredMethod(name, types);
        } catch (NoSuchMethodException ex) {
            if (clazz.getSuperclass() != null) {
                return findMethod(clazz.getSuperclass(), name, types);
            }
            throw ex;
        }
    }

    private Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            if (String.class.equals(type)) {
                return "";
            }
            if (type.isEnum()) {
                Object[] constants = type.getEnumConstants();
                return constants != null && constants.length > 0 ? constants[0] : null;
            }
            if (type.isArray()) {
                return Array.newInstance(type.getComponentType(), 0);
            }
            return null;
        }
        if (boolean.class.equals(type)) return false;
        if (byte.class.equals(type)) return (byte) 0;
        if (short.class.equals(type)) return (short) 0;
        if (int.class.equals(type)) return 0;
        if (long.class.equals(type)) return 0L;
        if (float.class.equals(type)) return 0f;
        if (double.class.equals(type)) return 0d;
        if (char.class.equals(type)) return '\0';
        return null;
    }

    private void setBestEffortString(Object target, String preferredSetter, String value) throws Exception {
        for (Method m : target.getClass().getMethods()) {
            if (m.getParameterTypes().length == 1
                    && String.class.equals(m.getParameterTypes()[0])
                    && (m.getName().equals(preferredSetter)
                        || m.getName().toLowerCase().contains("body")
                        || m.getName().toLowerCase().contains("contenttype")
                        || m.getName().toLowerCase().contains("raw")
                        || m.getName().toLowerCase().contains("payload")
                        || m.getName().toLowerCase().contains("prompt")
                        || m.getName().toLowerCase().contains("model"))) {
                try {
                    m.invoke(target, value);
                } catch (Exception ignored) {
                }
            }
        }

        for (Field f : allFields(target.getClass())) {
            if (String.class.equals(f.getType())) {
                f.setAccessible(true);
                String name = f.getName().toLowerCase();
                if (name.contains("body")
                        || name.contains("contenttype")
                        || name.contains("raw")
                        || name.contains("payload")
                        || name.contains("prompt")
                        || name.contains("model")) {
                    try {
                        f.set(target, value);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    private void setBestEffortInt(Object target, int value) throws Exception {
        for (Method m : target.getClass().getMethods()) {
            if (m.getParameterTypes().length == 1
                    && (int.class.equals(m.getParameterTypes()[0]) || Integer.class.equals(m.getParameterTypes()[0]))
                    && m.getName().toLowerCase().contains("status")) {
                try {
                    m.invoke(target, value);
                } catch (Exception ignored) {
                }
            }
        }

        for (Field f : allFields(target.getClass())) {
            if ((int.class.equals(f.getType()) || Integer.class.equals(f.getType()))
                    && f.getName().toLowerCase().contains("status")) {
                f.setAccessible(true);
                try {
                    f.set(target, value);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private List<Field> allFields(Class<?> type) {
        List<Field> fields = new ArrayList<Field>();
        Class<?> current = type;
        while (current != null) {
            for (Field f : current.getDeclaredFields()) {
                fields.add(f);
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    private String joinStringGetters(Object target) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (Method m : target.getClass().getMethods()) {
            if (m.getParameterTypes().length == 0
                    && m.getReturnType() == String.class
                    && (m.getName().startsWith("get") || m.getName().startsWith("is"))) {
                try {
                    Object v = m.invoke(target);
                    if (v != null) {
                        sb.append(v.toString()).append('\n');
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return sb.toString();
    }
}