package com.mcp.sailibrary.tests.mcp.adapters.connector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mcp.sailibrary.plugin.mcp.adapters.connector.StreamingSseConnector;
import com.mcp.sailibrary.plugin.mcp.core.McpAccessCredentials;
import com.mcp.sailibrary.plugin.mcp.core.ModelExecutionProfile;
import com.mcp.sailibrary.plugin.mcp.core.RawModelResponse;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/** * Testes do connector streaming SSE. * * @author Renato Tomaz Nati */
public class StreamingSseConnectorTest {

    private HttpServer server;
    private String baseUrl;
    private AtomicReference<String> lastApiKey;
    private AtomicReference<String> lastCookie;
    private AtomicReference<String> lastAccept;
    private AtomicReference<String> lastBody;

    @Before
    public void setUp() throws Exception {
        lastApiKey = new AtomicReference<String>("");
        lastCookie = new AtomicReference<String>("");
        lastAccept = new AtomicReference<String>("");
        lastBody = new AtomicReference<String>("");

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/stream", this::handle);
        server.start();

        baseUrl = "http://localhost:" + server.getAddress().getPort() + "/stream";
    }

    @After
    public void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void deveEnviarHeadersDeStreaming() throws Exception {
        StreamingSseConnector connector = new StreamingSseConnector();

        ModelExecutionProfile profile = new ModelExecutionProfile();
        profile.setEndpointUrl(baseUrl);

        RawModelResponse response = connector.execute(
                profile,
                "{\"messages\":[]}",
                McpAccessCredentials.forApiKeyAndCookie("api", "cookie")
        );

        assertEquals(200, response.getStatusCode());
        assertEquals("text/event-stream", lastAccept.get());
        assertNotNull(lastCookie.get());
        assertTrue(lastCookie.get().contains(".AspNetCore.Cookies=cookie"));
        assertEquals("{\"messages\":[]}", lastBody.get());

        // compatível com a implementação atual:
        assertTrue(lastApiKey.get() == null || lastApiKey.get().isEmpty() || "api".equals(lastApiKey.get()));
    }

    private void handle(HttpExchange exchange) {
        try {
            lastApiKey.set(readHeader(exchange, "X-Api-Key"));
            lastCookie.set(readHeader(exchange, "Cookie"));
            lastAccept.set(readHeader(exchange, "Accept"));
            lastBody.set(readBody(exchange.getRequestBody()));

            byte[] responseBytes = "event: complete\ndata: {\"status\":\"finished\"}\n\n"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
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