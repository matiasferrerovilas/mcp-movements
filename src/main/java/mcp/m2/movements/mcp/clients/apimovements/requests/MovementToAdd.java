package mcp.m2.movements.mcp.clients.apimovements.requests;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MovementToAdd(
        BigDecimal amount,
        LocalDate date,
        String description,
        List<CategoryRef> categories,
        MovementType type,
        String currency,
        Integer cuotaActual,
        Integer cuotasTotales,
        String bank,
        List<MovementItem> items,
        String clientId
) {
}
