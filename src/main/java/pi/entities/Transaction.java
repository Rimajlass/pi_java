package pi.entities;

import java.time.LocalDate;

public class Transaction {

    private int id;
    private User user;
    private Expense expense;
    private String type;
    private double montant;
    private LocalDate date;
    private String description;
    private String moduleSource;

    public Transaction(User user, Expense expense, String type, double montant, LocalDate date, String description, String moduleSource) {
        this.user = user;
        this.expense = expense;
        this.type = type;
        this.montant = montant;
        this.date = date;
        this.description = description;
        this.moduleSource = moduleSource;
    }

    public Transaction(int id, User user, Expense expense, String type, double montant, LocalDate date, String description, String moduleSource) {
        this.id = id;
        this.user = user;
        this.expense = expense;
        this.type = type;
        this.montant = montant;
        this.date = date;
        this.description = description;
        this.moduleSource = moduleSource;
    }

    public int getId() { return this.id; }

    public void setId(int id) { this.id = id; }

    public User getUser() { return this.user; }

    public void setUser(User user) { this.user = user; }

    public Expense getExpense() { return this.expense; }

    public void setExpense(Expense expense) { this.expense = expense; }

    public String getType() { return this.type; }

    public void setType(String type) { this.type = type; }

    public double getMontant() { return this.montant; }

    public void setMontant(double montant) { this.montant = montant; }

    public LocalDate getDate() { return this.date; }

    public void setDate(LocalDate date) { this.date = date; }

    public String getDescription() { return this.description; }

    public void setDescription(String description) { this.description = description; }

    public String getModuleSource() { return this.moduleSource; }

    public void setModuleSource(String moduleSource) { this.moduleSource = moduleSource; }

    @Override
    public String toString() {
        return "Transaction{" + "id=" + this.id + ", type='" + this.type + '\'' + ", montant=" + this.montant + '}';
    }
}
