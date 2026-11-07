package mcp.m2.movements.mcp.tools;

import mcp.m2.movements.mcp.clients.apimovements.MovementsClient;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
class BudgetTools {

	private final MovementsClient client;

	BudgetTools(MovementsClient client) {
		this.client = client;
	}

	@McpTool(name = "list_budgets",
			description = """
					Budgets that apply to a given month in the user's default workspace, with their progress. \
					A budget applies if it is recurring (no year), or set for that year and either that month or the whole year. \
					Each item has category, currency, amount, year, month, spent and percentage. \
					spent is the category's spending in that period and currency across all members, with REINTEGRO subtracting, \
					and percentage = spent / amount * 100, which can exceed 100.""",
			annotations = @McpTool.McpAnnotations(title = "List budgets", readOnlyHint = true,
					destructiveHint = false, idempotentHint = true))
	public Object listBudgets(
			@McpToolParam(description = "Year, e.g. 2026") int year,
			@McpToolParam(description = "Month between 1 and 12") int month,
			@McpToolParam(description = "Only budgets in this currency symbol, e.g. ARS; all currencies when omitted", required = false) String currency) {
		return client.get("/v1/budgets", QueryParams.of("year", year, "month", month, "currency", currency));
	}
}
