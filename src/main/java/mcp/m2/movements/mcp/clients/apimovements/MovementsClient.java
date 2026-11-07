package mcp.m2.movements.mcp.clients.apimovements;

import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

public class MovementsClient {

	private final RestClient http;

	public MovementsClient(RestClient http) {
		this.http = http;
	}

	public Object get(String path, Map<String, List<String>> params) {
		return http.get()
				.uri(u -> {
					u.path(path);
					params.forEach(u::queryParam);
					return u.build();
				})
				.retrieve()
				.body(Object.class);
	}
}
