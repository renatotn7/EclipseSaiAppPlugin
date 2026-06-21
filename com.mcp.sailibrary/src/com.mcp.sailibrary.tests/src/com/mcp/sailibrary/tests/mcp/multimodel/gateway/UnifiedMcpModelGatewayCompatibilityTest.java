package com.mcp.sailibrary.tests.mcp.multimodel.gateway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcp.sailibrary.plugin.mcp.multimodel.gateway.UnifiedMcpModelGateway;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/** * Teste do caminho legado de compatibilidade callModel(...). * * @author Renato Tomaz Nati */
public class UnifiedMcpModelGatewayCompatibilityTest {

    private HttpServer server;
    private String baseUrl;
    private AtomicReference<String> lastApiKey;
    private AtomicReference<String> lastBody;

    @Before
    public void setUp() throws Exception {
        lastApiKey = new AtomicReference<String>("");
        lastBody = new AtomicReference<String>("");

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/mcp", this::handle);
        server.start();

        baseUrl = "http://localhost:" + server.getAddress().getPort() + "/mcp";
    }

    @After
    public void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void deveEnviarPayloadLegadoViaCallModel() throws Exception {
        UnifiedMcpModelGateway gateway = new UnifiedMcpModelGateway(baseUrl);

        String response = gateway.callModel("GPT54", "prompt de teste", "key-abc");

        assertEquals("{\"ok\":true}", response);
        assertEquals("key-abc", lastApiKey.get());

        JsonObject root = JsonParser.parseString(lastBody.get()).getAsJsonObject();
        assertEquals("tools/call", root.get("method").getAsString());
        assertEquals("GPT54", root.getAsJsonObject("params").get("name").getAsString());
        assertTrue(root.getAsJsonObject("params")
                .getAsJsonObject("arguments")
                .get("input")
                .getAsString()
                .contains("Prompt: prompt de teste"));
    }

    private void handle(HttpExchange exchange) {
        try {
            lastApiKey.set(readHeader(exchange, "X-Api-Key"));
            lastBody.set(readBody(exchange.getRequestBody()));

            byte[] responseBytes = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            exchange.getResponseBody().write(responseBytes);
            exchange.getResponseBody().flush();
        } catch (Exception e) {
        } finally {
            exchange.close();
        }
    }

    private String readHeader(HttpExchange exchange, String name) {
        String value = exchange.getRequestHeaders().getFirst(name);
        return value != null ? value : "";
    }

    private String readBody(InputStream inputStream) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read = 0;

        while ((read = inputStream.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }

        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }
}