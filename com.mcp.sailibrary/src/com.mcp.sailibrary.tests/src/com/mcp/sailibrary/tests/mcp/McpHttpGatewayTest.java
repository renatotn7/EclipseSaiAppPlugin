package com.mcp.sailibrary.tests.mcp;

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

import com.mcp.sailibrary.plugin.mcp.McpHttpGateway;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/** * Testes do gateway HTTP MCP usando servidor local embutido. * * @author Renato Tomaz Nati */
public class McpHttpGatewayTest {

    private HttpServer server;
    private String baseUrl;

    private AtomicReference<String> lastBody;
    private AtomicReference<String> lastApiKey;
    private AtomicReference<String> lastCookie;
    private AtomicReference<String> lastAccept;

    @Before
    public void setUp() throws Exception {
        lastBody = new AtomicReference<String>("");
        lastApiKey = new AtomicReference<String>("");
        lastCookie = new AtomicReference<String>("");
        lastAccept = new AtomicReference<String>("");

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/test", this::handle);
        server.start();

        baseUrl = "http://localhost:" + server.getAddress().getPort() + "/test";
    }

    @After
    public void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void deveExecutarPostJsonRpc() throws Exception {
        McpHttpGateway gateway = new McpHttpGateway();

        String response = gateway.postJsonRpc(baseUrl, "api-key-123", "{\"x\":1}");

        assertEquals("OK", response);
        assertEquals("{\"x\":1}", lastBody.get());
        assertEquals("api-key-123", lastApiKey.get());
    }

    @Test
    public void deveExecutarPostJsonGenerico() throws Exception {
        McpHttpGateway gateway = new McpHttpGateway();

        McpHttpGateway.HttpGatewayResponse response = gateway.postJson(
                baseUrl,
                "{\"abc\":true}",
                null
        );

        assertEquals(200, response.getStatusCode());
        assertEquals("OK", response.getBody());
        assertEquals("{\"abc\":true}", lastBody.get());
    }

    @Test
    public void deveExecutarPostEventStreamComCookieEApiKey() throws Exception {
        McpHttpGateway gateway = new McpHttpGateway();

        McpHttpGateway.HttpGatewayResponse response = gateway.postEventStream(
                baseUrl,
                "{\"stream\":true}",
                "cookie-xyz",
                "key-789"
        );

        assertEquals(200, response.getStatusCode());
        assertEquals("OK", response.getBody());
        assertEquals("{\"stream\":true}", lastBody.get());
        assertEquals("key-789", lastApiKey.get());
        assertTrue(lastCookie.get().contains(".AspNetCore.Cookies=cookie-xyz"));
        assertEquals("text/event-stream", lastAccept.get());
    }

    private void handle(HttpExchange exchange) {
        try {
            lastApiKey.set(readHeader(exchange, "X-Api-Key"));
            lastCookie.set(readHeader(exchange, "Cookie"));
            lastAccept.set(readHeader(exchange, "Accept"));
            lastBody.set(readBody(exchange.getRequestBody()));

            byte[] responseBytes = "OK".getBytes(StandardCharsets.UTF_8);
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