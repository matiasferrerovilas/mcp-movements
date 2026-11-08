package mcp.m2.movements.mcp.tools;

import mcp.m2.movements.mcp.clients.apimovements.MovementsClient;
import mcp.m2.movements.mcp.clients.apimovements.requests.CategoryRef;
import mcp.m2.movements.mcp.clients.apimovements.requests.MovementItem;
import mcp.m2.movements.mcp.clients.apimovements.requests.MovementToAdd;
import mcp.m2.movements.mcp.clients.apimovements.requests.MovementType;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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
		return client.getMovements(type, currency, bank, categories, description, dateFrom, dateTo, isLive,
				page == null ? 0 : Math.max(page, 0),
				size == null ? 20 : Math.clamp(size, 1, 100));
	}

	@McpTool(name = "create_movement",
			description = """
					Creates ONE movement in the user's default workspace, owned by the authenticated user. \
					This writes data, so confirm amount, date, type, currency and description with the user before calling. \
					amount is always positive and type sets the direction: DEBITO and CREDITO are spending, \
					REINTEGRO is a refund that reduces spending, INGRESO is income. \
					CREDITO (credit card) requires cuotaActual and cuotasTotales with 1 <= cuotaActual <= cuotasTotales. \
					categories must be existing names from list_categories; an unknown name is rejected and nothing is created, \
					and when omitted the default category is used. bank must be an exact existing bank name. \
					Returns the created movement. Sending the same clientId again returns the movement already created instead of duplicating it.""",
			annotations = @McpTool.McpAnnotations(title = "Create movement", readOnlyHint = false,
					destructiveHint = false, idempotentHint = false))
	public Object createMovement(
			@McpToolParam(description = "Amount greater than 0, e.g. 12.50") BigDecimal amount,
			@McpToolParam(description = "Value date yyyy-MM-dd, the date the bank applied the movement") LocalDate date,
			@McpToolParam(description = "What the movement is, e.g. Mercadona") String description,
			@McpToolParam(description = "DEBITO, CREDITO, INGRESO or REINTEGRO") MovementType type,
			@McpToolParam(description = "Currency symbol, e.g. EUR") String currency,
			@McpToolParam(description = "Exact category names from list_categories", required = false) List<String> categories,
			@McpToolParam(description = "Current installment, only for CREDITO", required = false) Integer cuotaActual,
			@McpToolParam(description = "Total installments, only for CREDITO", required = false) Integer cuotasTotales,
			@McpToolParam(description = "Exact bank name, e.g. SANTANDER RIO", required = false) String bank,
			@McpToolParam(description = "Optional breakdown of the movement into lines", required = false) List<MovementItem> items,
			@McpToolParam(description = "Idempotency key, up to 36 characters; reuse the same one when retrying the same movement. A random one is used when omitted", required = false) String clientId) {
		return client.createMovement(new MovementToAdd(amount, date, description, resolveCategories(categories), type,
				currency, cuotaActual, cuotasTotales, bank, items,
				clientId == null || clientId.isBlank() ? UUID.randomUUID().toString() : clientId));
	}

	private List<CategoryRef> resolveCategories(List<String> requested) {
		if (requested == null || requested.isEmpty()) {
			return null;
		}
		var known = client.getCategories().stream().map(category -> (String) category.get("description")).toList();
		return requested.stream()
				.map(name -> known.stream()
						.filter(name::equalsIgnoreCase)
						.findFirst()
						.map(CategoryRef::new)
						.orElseThrow(() -> new IllegalArgumentException(
								"Unknown category '%s'. Valid categories: %s".formatted(name, known))))
				.toList();
	}
}
