package mcp.m2.movements.mcp.tools;

import mcp.m2.movements.mcp.clients.apimovements.MovementsClient;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class BalanceTools {

	private final MovementsClient client;

	BalanceTools(MovementsClient client) {
		this.client = client;
	}

	@McpTool(name = "get_balance",
			description = """
					Income and spending totals for a period, counting only the authenticated user's own movements \
					in the default workspace, by cash date. \
					Returns {INGRESO, GASTO}: INGRESO is the sum of income, GASTO is DEBITO + CREDITO minus REINTEGRO. \
					Amounts in different currencies are added without conversion, so pass a single currency to get a meaningful number. \
					startDate, endDate and currencies are all required.""",
			annotations = @McpTool.McpAnnotations(title = "Get balance", readOnlyHint = true,
					destructiveHint = false, idempotentHint = true))
	public Object getBalance(
			@McpToolParam(description = "Inclusive start date yyyy-MM-dd") String startDate,
			@McpToolParam(description = "Inclusive end date yyyy-MM-dd") String endDate,
			@McpToolParam(description = "Currency symbols to include, e.g. [\"ARS\"]") List<String> currencies) {
		return client.getBalance(startDate, endDate, currencies);
	}

	@McpTool(name = "get_balance_by_category",
			description = """
					Spending per category for ONE month of the default workspace (all members), by cash date. \
					Only the year and month of startDate are used. \
					Returns an unordered list of {category, year, month, currencySymbol, total}. INGRESO is excluded and REINTEGRO counts as negative. \
					A movement with several categories is counted fully in each one, so the totals can add up to more than the real spending, \
					and movements without category do not appear. \
					startDate and currencies are required.""",
			annotations = @McpTool.McpAnnotations(title = "Balance by category", readOnlyHint = true,
					destructiveHint = false, idempotentHint = true))
	public Object getBalanceByCategory(
			@McpToolParam(description = "Any date inside the month to analyze, yyyy-MM-dd; only its year and month are used") String startDate,
			@McpToolParam(description = "Currency symbols to include, e.g. [\"ARS\"]") List<String> currencies) {
		return client.getBalanceByCategory(startDate, currencies);
	}

	@McpTool(name = "get_monthly_evolution",
			description = """
					Month-by-month figures for a whole year of the default workspace, by cash date, in each currency's own amounts (no conversion). \
					Returns 12 rows per currency that has data: {month, currencySymbol, spent, savings}. \
					spent = DEBITO + CREDITO minus REINTEGRO and savings = INGRESO minus spent, which can be negative. Months without data are zero.""",
			annotations = @McpTool.McpAnnotations(title = "Monthly evolution", readOnlyHint = true,
					destructiveHint = false, idempotentHint = true))
	public Object getMonthlyEvolution(
			@McpToolParam(description = "Year between 2000 and 2100") int year) {
		return client.getMonthlyEvolution(year);
	}

	@McpTool(name = "get_recovery_time",
			description = """
					Estimates how many months it would take to recover an amount of money. \
					Uses the average monthly net savings (income minus spending) of the last N closed months, excluding the current one, \
					in the given currency for the default workspace. \
					Returns {monto, moneda, mesesConsiderados, ahorroPromedioMensual, mesesParaRecuperar, recuperable}. \
					If the average savings is zero or negative, mesesParaRecuperar is null and recuperable is false.""",
			annotations = @McpTool.McpAnnotations(title = "Recovery time", readOnlyHint = true,
					destructiveHint = false, idempotentHint = true))
	public Object getRecoveryTime(
			@McpToolParam(description = "Amount to recover, greater than 0, e.g. 150000") String amount,
			@McpToolParam(description = "Currency symbol, e.g. ARS (case-insensitive)") String currency,
			@McpToolParam(description = "Closed months of history to average, 1-24, default 3", required = false) Integer months) {
		return client.getRecoveryTime(amount, currency, months);
	}
}
