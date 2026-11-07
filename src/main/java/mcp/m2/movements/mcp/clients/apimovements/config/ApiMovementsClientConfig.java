package mcp.m2.movements.mcp.clients.apimovements.config;

import mcp.m2.movements.mcp.clients.apimovements.MovementsClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.web.client.RestClient;

@Configuration
class ApiMovementsClientConfig {

	@Bean
	MovementsClient movementsClient(ApiMovementsProperties properties) {
		var http = RestClient.builder()
				.baseUrl(properties.baseUrl())
				.requestInterceptor((req, body, exec) -> {
					var auth = SecurityContextHolder.getContext().getAuthentication();
					if (!(auth instanceof AbstractOAuth2TokenAuthenticationToken<?> t)) {
						throw new IllegalStateException("No authenticated user in context");
					}
					req.getHeaders().setBearerAuth(t.getToken().getTokenValue());
					return exec.execute(req, body);
				})
				.build();
		return new MovementsClient(http);
	}
}
