package com.mcp.sailibrary.tests.mcp.adapters.connector;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mcp.sailibrary.plugin.mcp.adapters.connector.LegacyJsonRpcConnector;
import com.mcp.sailibrary.plugin.mcp.core.McpAccessCredentials;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.RawModelResponse;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/** * Testes do connector legado JSON-RPC. * * @author Renato Tomaz Nati */
public class LegacyJsonRpcConnectorTest {

    private HttpServer server;
    private String baseUrl;
    private AtomicReference<String> lastApiKey;
    private AtomicReference<String> lastBody;

    @Before
    public void setUp() throws Exception {
        lastApiKey = new AtomicReference<String>("");
        lastBody = new AtomicReference<String>("");

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/legacy", this::handle);
        server.start();

        baseUrl = "http://localhost:" + server.getAddress().getPort() + "/legacy";
    }

    @After
    public void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void deveEnviarPostLegadoComApiKey() throws Exception {
        LegacyJsonRpcConnector connector = new LegacyJsonRpcConnector();

        ModelExecutionProfile profile = new ModelExecutionProfile();
        profile.setEndpointUrl(baseUrl);

        RawModelResponse response = connector.execute(
                profile,
                "{\"jsonrpc\":\"2.0\"}",
                McpAccessCredentials.forApiKey("key-123")
        );

        assertEquals(200, response.getStatusCode());
        assertEquals("{\"jsonrpc\":\"2.0\"}", lastBody.get());
        assertEquals("key-123", lastApiKey.get());
    }

    private void handle(HttpExchange exchange) {
        try {
            lastApiKey.set(readHeader(exchange, "X-Api-Key"));
            lastBody.set(readBody(exchange.getRequestBody()));

            byte[] responseBytes = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
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