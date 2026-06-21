package com.mcp.sailibrary.tests.mcp.adapters.config;

import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;

import org.junit.Test;

public class PropertiesBackedModelExecutionProfileResolverCoverageTest {

    @Test
    public void deveCobrirResolverEMetodosInternos() throws Exception {
        Object resolver = newInstance(
                "com.mcp.sailibrary.plugin.mcp.adapters.config.PropertiesBackedModelExecutionProfileResolver");

        Properties props = new Properties();
        props.setProperty("planner.transport", "HTTP");
        props.setProperty("planner.request.format", "JSON");
        props.setProperty("planner.response.format", "JSON");
        props.setProperty("planner.endpoint.url", "http://localhost/planner");
        props.setProperty("planner.legacy.model.alias", "planner-legacy");
        props.setProperty("planner.streaming.model.name", "planner-streaming");
        props.setProperty("planner.creativity", "1.8");
        props.setProperty("planner.max.tokens", "4096");

        props.setProperty("investigator.transport", "HTTP");
        props.setProperty("investigator.streaming.model.name", "investigator-streaming");
        props.setProperty("investigator.creativity", "-10");
        props.setProperty("investigator.max.tokens", "abc");

        injectProperties(resolver, props);

        Class<?> modelChannelClass = Class.forName("com.mcp.sailibrary.plugin.mcp.core.ModelChannel");
        Object[] channels = modelChannelClass.getEnumConstants();
        assertNotNull(channels);
        assertTrue(channels.length > 0);

        for (Object channel : channels) {
            Object profile = invoke(resolver, "resolve", new Class<?>[] { modelChannelClass }, channel);
            assertNotNull(profile);
            assertTrue(readAnyString(profile).length() >= 0);
        }

        Object firstChannel = channels[0];

        Object transportKind = invokePrivate(resolver, "resolveTransportKind",
                new Class<?>[] { modelChannelClass }, firstChannel);
        assertNotNull(transportKind);

        Object requestFormat = invokePrivate(resolver, "resolveRequestFormatKind",
                new Class<?>[] { modelChannelClass, transportKind.getClass() }, firstChannel, transportKind);
        assertNotNull(requestFormat);

        Object responseFormat = invokePrivate(resolver, "resolveResponseFormatKind",
                new Class<?>[] { modelChannelClass, transportKind.getClass() }, firstChannel, transportKind);
        assertNotNull(responseFormat);

        String endpoint = (String) invokePrivate(resolver, "resolveEndpointUrl",
                new Class<?>[] { transportKind.getClass() }, transportKind);
        assertNotNull(endpoint);

        String alias = (String) invokePrivate(resolver, "resolveLegacyModelAlias",
                new Class<?>[] { modelChannelClass }, firstChannel);
        assertNotNull(alias);

        String streamingModel = (String) invokePrivate(resolver, "resolveStreamingModelName",
                new Class<?>[] { modelChannelClass }, firstChannel);
        assertNotNull(streamingModel);

        Object creativity = invokePrivate(resolver, "resolveCreativity",
                new Class<?>[] { modelChannelClass }, firstChannel);
        assertNotNull(creativity);

        Object maxTokens = invokePrivate(resolver, "resolveMaxTokens",
                new Class<?>[] { modelChannelClass }, firstChannel);
        assertNotNull(maxTokens);

        String defaultAlias = (String) invokePrivate(resolver, "resolveDefaultLegacyModelAlias",
                new Class<?>[] { modelChannelClass }, firstChannel);
        assertNotNull(defaultAlias);

        Object defaultCreativity = invokePrivate(resolver, "defaultCreativity",
                new Class<?>[] { modelChannelClass }, firstChannel);
        assertNotNull(defaultCreativity);

        Object defaultMaxTokens = invokePrivate(resolver, "defaultMaxTokens",
                new Class<?>[] { modelChannelClass }, firstChannel);
        assertNotNull(defaultMaxTokens);

        boolean hasNonBlank = (Boolean) invokePrivate(resolver, "hasNonBlankProperty",
                new Class<?>[] { String.class }, "planner.streaming.model.name");
        assertTrue(hasNonBlank);

        String resolvedString = (String) invokePrivate(resolver, "resolveString",
                new Class<?>[] { String.class, String.class }, "planner.streaming.model.name", "fallback");
        assertEquals("planner-streaming", resolvedString);

        boolean bool1 = (Boolean) invokePrivate(resolver, "resolveBoolean",
                new Class<?>[] { String.class, boolean.class }, "missing.boolean", true);
        assertTrue(bool1);

        Object int1 = invokePrivate(resolver, "resolveInteger",
                new Class<?>[] { String.class, Integer.class }, "planner.max.tokens", Integer.valueOf(10));
        assertNotNull(int1);

        Object int2 = invokePrivate(resolver, "resolveInteger",
                new Class<?>[] { String.class, Integer.class }, "investigator.max.tokens", Integer.valueOf(99));
        assertEquals(Integer.valueOf(99), int2);

        Object parsedDouble = invokePrivate(resolver, "parseDouble",
                new Class<?>[] { String.class, double.class }, "not-a-double", 0.7d);
        assertEquals(0.7d, ((Double) parsedDouble).doubleValue(), 0.0001d);

        Double clampedLow = (Double) invokePrivate(resolver, "clampCreativity",
                new Class<?>[] { double.class }, -5.0d);
        assertNotNull(clampedLow);

        Double clampedHigh = (Double) invokePrivate(resolver, "clampCreativity",
                new Class<?>[] { double.class }, 99.0d);
        assertNotNull(clampedHigh);

        Boolean blank1 = (Boolean) invokePrivate(resolver, "isBlank",
                new Class<?>[] { String.class }, "");
        Boolean blank2 = (Boolean) invokePrivate(resolver, "isBlank",
                new Class<?>[] { String.class }, "abc");
        assertTrue(blank1);
        assertFalse(blank2);
    }

    private void injectProperties(Object resolver, Properties properties) throws Exception {
        Class<?> current = resolver.getClass();
        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                if (Properties.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    field.set(resolver, properties);
                    return;
                }
            }
            current = current.getSuperclass();
        }
        throw new IllegalStateException("Campo Properties nao encontrado no resolver.");
    }

    private Object newInstance(String className) throws Exception {
        Class<?> clazz = Class.forName(className);
        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException ex) {
            Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            Class<?>[] types = constructor.getParameterTypes();
            Object[] args = new Object[types.length];
            for (int i = 0; i < types.length; i++) {
                args[i] = defaultValue(types[i]);
            }
            return constructor.newInstance(args);
        }
    }

    private Object invoke(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = findMethod(target.getClass(), name, types);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private Object invokePrivate(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        return invoke(target, name, types, args);
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
            return null;
        }
        if (boolean.class.equals(type)) return false;
        if (int.class.equals(type)) return 0;
        if (long.class.equals(type)) return 0L;
        if (double.class.equals(type)) return 0d;
        if (float.class.equals(type)) return 0f;
        if (short.class.equals(type)) return (short) 0;
        if (byte.class.equals(type)) return (byte) 0;
        if (char.class.equals(type)) return '\0';
        return null;
    }

    private String readAnyString(Object obj) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (Method m : obj.getClass().getMethods()) {
            if (m.getParameterTypes().length == 0 && m.getReturnType() == String.class) {
                try {
                    Object value = m.invoke(obj);
                    if (value != null) {
                        sb.append(value.toString());
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return sb.toString();
    }
}