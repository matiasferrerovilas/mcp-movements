package mcp.m2.movements.mcp.clients.apimovements.requests;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.math.BigDecimal;

public record MovementItem(
        @JsonPropertyDescription("Quantity, greater than 0")
        BigDecimal quantity,
        @JsonPropertyDescription("Unit, e.g. UNIDAD")
        String unit,
        @JsonPropertyDescription("What the line is, e.g. Dona nube")
        String description,
        @JsonPropertyDescription("Total price of the whole line, not per unit; 0 or more")
        BigDecimal price
) {
}
