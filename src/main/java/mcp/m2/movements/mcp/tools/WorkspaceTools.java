package mcp.m2.movements.mcp.tools;

import mcp.m2.movements.mcp.clients.apimovements.MovementsClient;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
class WorkspaceTools {

	private final MovementsClient client;

	WorkspaceTools(MovementsClient client) {
		this.client = client;
	}

	@McpTool(name = "list_workspaces",
			description = """
					Workspaces (shared ledgers) the authenticated user belongs to. \
					Most other tools act on the user's default workspace (isDefault = true); only get_workspace_monthly_summary and list_goals \
					need an explicit workspaceId, which is the workspaceId field of each item, not the membership id. \
					Each item has workspaceName, the user's role, the member list (userId, email, role) and isDefault.""",
			annotations = @McpTool.McpAnnotations(title = "List workspaces", readOnlyHint = true,
					destructiveHint = false, idempotentHint = true))
	public Object listWorkspaces() {
		return client.getWorkspaces();
	}

	@McpTool(name = "get_workspace_monthly_summary",
			description = """
					Income and spending summary of one month for a workspace the user is a member of, by cash date. \
					With byUser=false it returns USD totals (totalIncome, totalSpent, totalSpentDebit, totalSpentCredit, net, vsPreviousMonth), \
					plus one entry per workspace currency with income, spent, net, movementCount, topSpendingCategory, spendingByCategory \
					and deltas against the previous month. USD totals convert each movement by its exchange rate and leave out movements without one. \
					With byUser=true it returns instead, for each member with movements, the movementCount and totalSpent per currency; \
					automatic movements (recurring income, credit installments) are not attributed to anyone.""",
			annotations = @McpTool.McpAnnotations(title = "Workspace monthly summary", readOnlyHint = true,
					destructiveHint = false, idempotentHint = true))
	public Object getWorkspaceMonthlySummary(
			@McpToolParam(description = "workspaceId from list_workspaces") long workspaceId,
			@McpToolParam(description = "Year between 2000 and 2100") int year,
			@McpToolParam(description = "Month between 1 and 12") int month,
			@McpToolParam(description = "true for the per-member breakdown, default false", required = false) Boolean byUser) {
		return Boolean.TRUE.equals(byUser)
				? client.getWorkspaceMonthlySummaryByUser(workspaceId, year, month)
				: client.getWorkspaceMonthlySummary(workspaceId, year, month);
	}

	@McpTool(name = "list_categories",
			description = """
					Active categories of the user's default workspace: id, description, iconName and iconColor. \
					The description is the exact name to use in the categories filter of list_movements.""",
			annotations = @McpTool.McpAnnotations(title = "List categories", readOnlyHint = true,
					destructiveHint = false, idempotentHint = true))
	public Object listCategories() {
		return client.getCategories();
	}
}
