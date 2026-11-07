package mcp.m2.movements.mcp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class SecurityConfig {

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http,
			@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer) {
		return http
				.authorizeHttpRequests(a -> a.anyRequest().authenticated())
				.oauth2ResourceServer(o -> o
						.jwt(Customizer.withDefaults())
						.protectedResourceMetadata(m -> m
								.protectedResourceMetadataCustomizer(b -> b.authorizationServer(issuer))))
				.csrf(c -> c.disable())
				.build();
	}
}
