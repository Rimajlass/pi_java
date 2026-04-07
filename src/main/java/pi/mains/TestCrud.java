package pi.mains;

import pi.entities.Expense;
import pi.entities.Revenue;
import pi.entities.User;
import pi.services.ExpenseService;
import pi.services.RevenueService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TestCrud {
    public static void main(String[] args) {
        RevenueService revenueService = new RevenueService();
        ExpenseService expenseService = new ExpenseService();

        User user = new User();
        user.setId(1);

        try {
            Revenue revenue = new Revenue(
                    user,
                    1500.0,
                    "salary",
                    LocalDate.now(),
                    "CRUD test revenue",
                    LocalDateTime.now()
            );

            revenueService.add(revenue);
            System.out.println("Revenue added: " + revenue);

            Revenue savedRevenue = revenueService.getById(revenue.getId());
            System.out.println("Revenue fetched: " + savedRevenue);

            revenue.setAmount(1750.0);
            revenue.setDescription("Revenue updated");
            revenueService.update(revenue);
            System.out.println("Revenue updated: " + revenueService.getById(revenue.getId()));

            Expense expense = new Expense(
                    revenue,
                    user,
                    300.0,
                    "food",
                    LocalDate.now(),
                    "CRUD test expense"
            );

            expenseService.add(expense);
            System.out.println("Expense added: " + expense);

            Expense savedExpense = expenseService.getById(expense.getId());
            System.out.println("Expense fetched: " + savedExpense);

            expense.setAmount(420.0);
            expense.setCategory("transport");
            expense.setDescription("Expense updated");
            expenseService.update(expense);
            System.out.println("Expense updated: " + expenseService.getById(expense.getId()));

            System.out.println("All revenues count: " + revenueService.getAll().size());
            System.out.println("All expenses count: " + expenseService.getAll().size());

            expenseService.delete(expense.getId());
            System.out.println("Expense deleted, lookup result: " + expenseService.getById(expense.getId()));

            revenueService.delete(revenue.getId());
            System.out.println("Revenue deleted, lookup result: " + revenueService.getById(revenue.getId()));
        } catch (SQLException e) {
            System.out.println("SQL error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Validation error: " + e.getMessage());
        }
    }
}
