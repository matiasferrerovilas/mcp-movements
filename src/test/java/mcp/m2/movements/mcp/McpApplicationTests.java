package mcp.m2.movements.mcp;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost/realm")
class McpApplicationTests {

	static final AtomicReference<String> seenAuth = new AtomicReference<>();
	static final AtomicReference<String> seenQuery = new AtomicReference<>();
	static final AtomicReference<String> seenPath = new AtomicReference<>();
	static final AtomicReference<String> seenMethod = new AtomicReference<>();
	static final AtomicReference<String> seenBody = new AtomicReference<>();
	static HttpServer api;

	@DynamicPropertySource
	static void props(DynamicPropertyRegistry r) throws IOException {
		api = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		api.createContext("/v1", ex -> {
			seenAuth.set(ex.getRequestHeaders().getFirst("Authorization"));
			seenPath.set(ex.getRequestURI().getPath());
			seenQuery.set(ex.getRequestURI().getRawQuery());
			seenMethod.set(ex.getRequestMethod());
			seenBody.set(new String(ex.getRequestBody().readAllBytes()));
			byte[] body = (ex.getRequestURI().getPath().endsWith("/categories")
					? "[{\"id\":2,\"description\":\"Hogar\"},{\"id\":6,\"description\":\"Servicios\"}]"
					: "{\"content\":[{\"id\":1,\"description\":\"coffee\"}]}").getBytes();
			ex.getResponseHeaders().add("Content-Type", "application/json");
			ex.sendResponseHeaders(200, body.length);
			ex.getResponseBody().write(body);
			ex.close();
		});
		api.start();
		r.add("api-movements.base-url", () -> "http://localhost:" + api.getAddress().getPort());
	}

	@AfterAll
	static void stop() {
		api.stop(0);
	}

	@TestConfiguration
	static class Config {
		@Bean
		JwtDecoder jwtDecoder() {
			return token -> Jwt.withTokenValue(token).header("alg", "none").subject("u1")
					.issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
		}
	}

	@LocalServerPort
	int port;

	HttpResponse<String> post(String session, String token, String json) throws Exception {
		var b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/mcp"))
				.header("Content-Type", "application/json")
				.header("Accept", "application/json, text/event-stream")
				.POST(HttpRequest.BodyPublishers.ofString(json));
		if (token != null) b.header("Authorization", "Bearer " + token);
		if (session != null) b.header("Mcp-Session-Id", session);
		return HttpClient.newHttpClient().send(b.build(), HttpResponse.BodyHandlers.ofString());
	}

	String openSession() throws Exception {
		var init = post(null, "user-token", """
				{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18",
				"capabilities":{},"clientInfo":{"name":"t","version":"1"}}}""");
		String session = init.headers().firstValue("Mcp-Session-Id").orElse(null);
		post(session, "user-token", "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}");
		return session;
	}

	@Test
	void createsMovementWithIdempotencyKeyAndCanonicalCategory() throws Exception {
		String session = openSession();
		seenMethod.set(null);
		seenBody.set(null);

		var call = post(session, "user-token", """
				{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"create_movement",
				"arguments":{"amount":12.5,"date":"2026-11-06","description":"Mercadona","type":"DEBITO",
				"currency":"EUR","categories":["hogar"],"bank":"SANTANDER RIO"}}}""");

		assertEquals("POST", seenMethod.get(), call.body());
		assertEquals("/v1/expenses", seenPath.get());
		assertEquals("Bearer user-token", seenAuth.get());
		var body = seenBody.get();
		assertTrue(body.contains("\"amount\":12.5") && body.contains("\"date\":\"2026-11-06\"")
				&& body.contains("\"type\":\"DEBITO\"") && body.contains("\"currency\":\"EUR\"")
				&& body.contains("\"categories\":[{\"description\":\"Hogar\"}]")
				&& body.matches(".*\"clientId\":\"[0-9a-f-]{36}\".*"), body);
	}

	@Test
	void rejectsUnknownCategoryWithoutCreatingAnything() throws Exception {
		String session = openSession();
		seenMethod.set(null);

		var call = post(session, "user-token", """
				{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"create_movement",
				"arguments":{"amount":5,"date":"2026-11-06","description":"X","type":"DEBITO",
				"currency":"EUR","categories":["Inexistente"]}}}""");

		assertTrue(call.body().contains("Unknown category") && call.body().contains("Hogar"), call.body());
		assertEquals("GET", seenMethod.get());
	}

	@Test
	void doesNotSendEmptyFilters() throws Exception {
		String session = openSession();
		seenPath.set(null);
		seenQuery.set(null);
		post(session, "user-token", """
				{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"list_movements",
				"arguments":{"type":[],"currency":[],"bank":[],"categories":[]}}}""");

		assertEquals("/v1/expenses", seenPath.get());
		var query = seenQuery.get();
		assertTrue(query.contains("page=0") && !query.contains("type") && !query.contains("currency")
				&& !query.contains("bank") && !query.contains("categories"), query);
	}

	@Test
	void rejectsAnonymousAndPointsToMetadata() throws Exception {
		var res = post(null, null, "{}");
		assertEquals(401, res.statusCode());
		assertTrue(res.headers().firstValue("WWW-Authenticate").orElse("").contains("resource_metadata="));
	}

	@Test
	void publishesAuthorizationServerWithoutAuth() throws Exception {
		var req = HttpRequest.newBuilder(
				URI.create("http://localhost:" + port + "/.well-known/oauth-protected-resource/mcp")).GET().build();
		var res = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
		assertEquals(200, res.statusCode(), res.body());
		assertTrue(res.body().contains("http://localhost/realm"), res.body());
		assertTrue(res.body().contains("/mcp"), res.body());
		assertTrue(res.body().contains("\"tls_client_certificate_bound_access_tokens\":false"), res.body());
	}

	@Test
	void forwardsCallerJwtToApi() throws Exception {
		var init = post(null, "user-token", """
				{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18",
				"capabilities":{},"clientInfo":{"name":"t","version":"1"}}}""");
		assertEquals(200, init.statusCode(), init.body());
		String session = init.headers().firstValue("Mcp-Session-Id").orElse(null);
		post(session, "user-token", "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}");

		var call = post(session, "user-token", """
				{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"list_movements",
				"arguments":{"type":["DEBITO"],"dateTo":"2026-01-31","size":5}}}""");

		assertTrue(call.body().contains("coffee"), call.body());
		assertEquals("Bearer user-token", seenAuth.get());
		assertTrue(seenQuery.get().contains("type=DEBITO") && seenQuery.get().contains("dateTO=2026-01-31")
				&& seenQuery.get().contains("size=5") && seenQuery.get().contains("isLive=false"), seenQuery.get());
		assertEquals("/v1/expenses", seenPath.get());

		var list = post(session, "user-token", "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/list\"}");
		for (String name : new String[] {"list_movements", "get_balance", "get_balance_by_category",
				"get_monthly_evolution", "get_recovery_time", "list_workspaces", "get_workspace_monthly_summary",
				"list_categories", "list_subscriptions", "list_budgets", "list_goals", "list_income", "create_movement"}) {
			assertTrue(list.body().contains("\"" + name + "\""), name);
		}

		post(session, "user-token", """
				{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"get_workspace_monthly_summary",
				"arguments":{"workspaceId":7,"year":2026,"month":3,"byUser":true}}}""");
		assertEquals("/v1/workspaces/7/summary/monthly/by-user", seenPath.get());
		assertEquals("year=2026&month=3", seenQuery.get());

		post(session, "user-token", """
				{"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"list_subscriptions",
				"arguments":{"unpaidYear":2026,"unpaidMonth":3}}}""");
		assertEquals("/v1/subscriptions/unpaid", seenPath.get());
	}
}
