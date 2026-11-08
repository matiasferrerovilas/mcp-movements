package mcp.m2.movements.mcp.clients.apimovements;

import mcp.m2.movements.mcp.clients.apimovements.requests.MovementToAdd;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;
import java.util.Map;

@HttpExchange
public interface MovementsClient {

    @GetExchange("/v1/expenses")
    Object getMovements(@RequestParam(required = false) List<String> type,
                        @RequestParam(required = false) List<String> currency,
                        @RequestParam(required = false) List<String> bank,
                        @RequestParam(required = false) List<String> categories,
                        @RequestParam(required = false) String description,
                        @RequestParam(required = false) String dateFrom,
                        @RequestParam(name = "dateTO", required = false) String dateTo,
                        @RequestParam(required = false) Boolean isLive,
                        @RequestParam int page,
                        @RequestParam int size);

    @GetExchange("/v1/balance")
    Object getBalance(@RequestParam String startDate,
                      @RequestParam String endDate,
                      @RequestParam List<String> currencies);

    @GetExchange("/v1/balance/category")
    Object getBalanceByCategory(@RequestParam String startDate,
                                @RequestParam List<String> currencies);

    @GetExchange("/v1/balance/monthly-evolution")
    Object getMonthlyEvolution(@RequestParam int year);

    @GetExchange("/v1/balance/recovery-time")
    Object getRecoveryTime(@RequestParam String amount,
                           @RequestParam String currency,
                           @RequestParam(required = false) Integer months);

    @GetExchange("/v1/workspace")
    Object getWorkspaces();

    @GetExchange("/v1/workspaces/{workspaceId}/summary/monthly")
    Object getWorkspaceMonthlySummary(@PathVariable long workspaceId,
                                      @RequestParam int year,
                                      @RequestParam int month);

    @GetExchange("/v1/workspaces/{workspaceId}/summary/monthly/by-user")
    Object getWorkspaceMonthlySummaryByUser(@PathVariable long workspaceId,
                                            @RequestParam int year,
                                            @RequestParam int month);

    @GetExchange("/v1/workspace/categories")
    List<Map<String, Object>> getCategories();

    @PostExchange("/v1/expenses")
    Object createMovement(@RequestBody MovementToAdd movement);

    @GetExchange("/v1/subscriptions")
    Object getSubscriptions(@RequestParam(required = false) List<String> currencySymbol,
                            @RequestParam(required = false) String lastPayment);

    @GetExchange("/v1/subscriptions/unpaid")
    Object getUnpaidSubscriptions(@RequestParam int year,
                                  @RequestParam int month);

    @GetExchange("/v1/budgets")
    Object getBudgets(@RequestParam int year,
                      @RequestParam int month,
                      @RequestParam(required = false) String currency);

    @GetExchange("/v1/goals")
    Object getGoals(@RequestParam long workspaceId);

    @GetExchange("/v1/income")
    Object getIncome(@RequestParam int page,
                     @RequestParam int size);
}
