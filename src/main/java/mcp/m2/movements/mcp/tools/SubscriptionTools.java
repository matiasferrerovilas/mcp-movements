package mcp.m2.movements.mcp.tools;

import mcp.m2.movements.mcp.clients.apimovements.MovementsClient;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class SubscriptionTools {

	private final MovementsClient client;

	SubscriptionTools(MovementsClient client) {
		this.client = client;
	}

	@McpTool(name = "list_subscriptions",
			description = """
					Recurring subscriptions of the user's default workspace, not paginated. \
					Each item has description, amount, currency, lastPayment, isPaid (true only if lastPayment is in the current calendar month), \
					workspaceName and the owner. \
					The optional filters currencySymbol and lastPayment (exact date) are combined with AND. \
					If unpaidYear and unpaidMonth are both set, it returns instead the subscriptions with no payment recorded in that month \
					(never paid, or last paid in another month) and ignores the other filters.""",
			annotations = @McpTool.McpAnnotations(title = "List subscriptions", readOnlyHint = true,
					destructiveHint = false, idempotentHint = true))
	public Object listSubscriptions(
			@McpToolParam(description = "Currency symbols, e.g. [\"ARS\"]", required = false) List<String> currencySymbol,
			@McpToolParam(description = "Exact last payment date yyyy-MM-dd", required = false) String lastPayment,
			@McpToolParam(description = "Year of the month to check for unpaid subscriptions; use together with unpaidMonth", required = false) Integer unpaidYear,
			@McpToolParam(description = "Month 1-12 to check for unpaid subscriptions; use together with unpaidYear", required = false) Integer unpaidMonth) {
		if (unpaidYear != null && unpaidMonth != null) {
			return client.get("/v1/subscriptions/unpaid", QueryParams.of("year", unpaidYear, "month", unpaidMonth));
		}
		return client.get("/v1/subscriptions", QueryParams.of("currencySymbol", currencySymbol, "lastPayment", lastPayment));
	}
}
