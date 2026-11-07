package mcp.m2.movements.mcp.clients.apimovements.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "api-movements")
public record ApiMovementsProperties(String baseUrl) {
}
