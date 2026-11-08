package mcp.m2.movements.mcp.tools;

import mcp.m2.movements.mcp.clients.apimovements.MovementsClient;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
class IncomeTools {

	private final MovementsClient client;

	IncomeTools(MovementsClient client) {
		this.client = client;
	}

	@McpTool(name = "list_income",
			description = """
					Income templates (for example a salary) of the user's default workspace, paginated: id, amount, currency and bank. \
					They are not transactions and do not count in balances by themselves. \
					Real income shows up as INGRESO movements (see list_movements), created when a template is reloaded or by the monthly recurring-income job.""",
			annotations = @McpTool.McpAnnotations(title = "List income templates", readOnlyHint = true,
					destructiveHint = false, idempotentHint = true))
	public Object listIncome(
			@McpToolParam(description = "Zero-based page index, default 0", required = false) Integer page,
			@McpToolParam(description = "Page size, default 20, max 100", required = false) Integer size) {
		return client.getIncome(
				page == null ? 0 : Math.max(page, 0),
				size == null ? 20 : Math.clamp(size, 1, 100));
	}
}
