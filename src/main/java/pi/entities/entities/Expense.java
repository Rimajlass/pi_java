package pi.entities.entities;

import java.time.LocalDate;

public class Expense {

    private int id;
    private Revenue revenue;
    private User user;
    private double amount;
    private String category;
    private LocalDate expenseDate;
    private String description;

    public Expense(Revenue revenue, User user, double amount, String category, LocalDate expenseDate, String description) {
        this.revenue = revenue;
        this.user = user;
        this.amount = amount;
        this.category = category;
        this.expenseDate = expenseDate;
        this.description = description;
    }

    public Expense(int id, Revenue revenue, User user, double amount, String category, LocalDate expenseDate, String description) {
        this.id = id;
        this.revenue = revenue;
        this.user = user;
        this.amount = amount;
        this.category = category;
        this.expenseDate = expenseDate;
        this.description = description;
    }

    public int getId() { return this.id; }

    public void setId(int id) { this.id = id; }

    public Revenue getRevenue() { return this.revenue; }

    public void setRevenue(Revenue revenue) { this.revenue = revenue; }

    public User getUser() { return this.user; }

    public void setUser(User user) { this.user = user; }

    public double getAmount() { return this.amount; }

    public void setAmount(double amount) { this.amount = amount; }

    public String getCategory() { return this.category; }

    public void setCategory(String category) { this.category = category; }

    public LocalDate getExpenseDate() { return this.expenseDate; }

    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }

    public String getDescription() { return this.description; }

    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return "Expense{" + "id=" + this.id + ", amount=" + this.amount + ", category='" + this.category + '\'' + '}';
    }
}
