package mcp.m2.movements.mcp.clients.apimovements.config;

import mcp.m2.movements.mcp.clients.apimovements.MovementsClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
class ApiMovementsClientConfig {

	private static final String BEARER_PREFIX = "Bearer ";

	@Bean
	MovementsClient movementsClient(ApiMovementsProperties properties) {
		var restClient = RestClient.builder()
				.baseUrl(properties.baseUrl())
				.requestInterceptor((request, body, execution) -> {
					request.getHeaders().add(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + getCurrentToken());
					return execution.execute(request, body);
				})
				.build();
		var factory = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build();
		return factory.createClient(MovementsClient.class);
	}

	private String getCurrentToken() {
		if (SecurityContextHolder.getContext().getAuthentication() instanceof AbstractOAuth2TokenAuthenticationToken<?> auth) {
			return auth.getToken().getTokenValue();
		}
		throw new IllegalStateException("No authenticated user in context");
	}
}
