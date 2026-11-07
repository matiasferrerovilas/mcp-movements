package mcp.m2.movements.mcp.tools;

import mcp.m2.movements.mcp.clients.apimovements.MovementsClient;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
class GoalTools {

	private final MovementsClient client;

	GoalTools(MovementsClient client) {
		this.client = client;
	}

	@McpTool(name = "list_goals",
			description = """
					Savings goals of a workspace the user is a member of, newest first, not paginated. \
					Each item has name, targetAmount, currentAmount, currency, targetDate and progressPercent (0-100, capped at 100).""",
			annotations = @McpTool.McpAnnotations(title = "List goals", readOnlyHint = true,
					destructiveHint = false, idempotentHint = true))
	public Object listGoals(
			@McpToolParam(description = "workspaceId from list_workspaces; unlike other tools it does not default to the user's default workspace") long workspaceId) {
		return client.get("/v1/goals", QueryParams.of("workspaceId", workspaceId));
	}
}
