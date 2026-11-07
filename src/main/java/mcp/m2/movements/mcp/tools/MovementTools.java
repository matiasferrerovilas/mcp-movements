package mcp.m2.movements.mcp.tools;

import mcp.m2.movements.mcp.clients.apimovements.MovementsClient;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class MovementTools {

	private final MovementsClient client;

	MovementTools(MovementsClient client) {
		this.client = client;
	}

	@McpTool(name = "list_movements",
			description = """
					List the movements of the user's default workspace (including those of other members), newest first, paginated. \
					Each item has id, amount, description, date, categories, currency, bank, type, installments \
					(cuotaActual/cuotasTotales, only for CREDITO), metadata.owner, metadata.exchangeRate and items. \
					metadata.amountUsd is always null: compute USD as amount / exchangeRate (exchangeRate can be null on old rows). \
					Types: DEBITO and CREDITO are spending, REINTEGRO is a refund that reduces spending, INGRESO is income. \
					All filters are optional and combined with AND.""",
			annotations = @McpTool.McpAnnotations(title = "List movements", readOnlyHint = true,
					destructiveHint = false, idempotentHint = true))
	public Object listMovements(
			@McpToolParam(description = "Exact types to include: DEBITO, CREDITO, INGRESO, REINTEGRO", required = false) List<String> type,
			@McpToolParam(description = "Exact currency symbols, e.g. ARS, USD", required = false) List<String> currency,
			@McpToolParam(description = "Exact bank names", required = false) List<String> bank,
			@McpToolParam(description = "Exact category names (see list_categories); a movement matches if it has any of them", required = false) List<String> categories,
			@McpToolParam(description = "Case-insensitive text contained in the description", required = false) String description,
			@McpToolParam(description = "Inclusive start date yyyy-MM-dd, applied to the movement date", required = false) String dateFrom,
			@McpToolParam(description = "Inclusive end date yyyy-MM-dd, applied to the movement date", required = false) String dateTo,
			@McpToolParam(description = "true to return only the current calendar month, ignoring dateFrom and dateTo. Default false", required = false) Boolean currentMonthOnly,
			@McpToolParam(description = "Zero-based page index, default 0", required = false) Integer page,
			@McpToolParam(description = "Page size, default 20, max 100", required = false) Integer size) {
		Boolean isLive = Boolean.TRUE.equals(currentMonthOnly) ? Boolean.TRUE
				: dateFrom != null || dateTo != null ? Boolean.FALSE : null;
		return client.get("/v1/expenses", QueryParams.of(
				"type", type, "currency", currency, "bank", bank, "categories", categories,
				"description", description, "dateFrom", dateFrom,
				"dateTO", dateTo,
				"isLive", isLive,
				"page", page == null ? 0 : Math.max(page, 0),
				"size", size == null ? 20 : Math.clamp(size, 1, 100)));
	}
}
