package pi.entities.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class RecurringTransactionRule {

    private int id;
    private User user;
    private Revenue expenseRevenue;
    private String kind;
    private String frequency;
    private double amount;
    private String label;
    private String signature;
    private LocalDate nextRunAt;
    private boolean active;
    private Double confidence;
    private String expenseCategory;
    private String revenueType;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RecurringTransactionRule(User user, Revenue expenseRevenue, String kind, String frequency, double amount, String label, String signature, LocalDate nextRunAt, boolean active, Double confidence, String expenseCategory, String revenueType, String description, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.user = user;
        this.expenseRevenue = expenseRevenue;
        this.kind = kind;
        this.frequency = frequency;
        this.amount = amount;
        this.label = label;
        this.signature = signature;
        this.nextRunAt = nextRunAt;
        this.active = active;
        this.confidence = confidence;
        this.expenseCategory = expenseCategory;
        this.revenueType = revenueType;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public RecurringTransactionRule(int id, User user, Revenue expenseRevenue, String kind, String frequency, double amount, String label, String signature, LocalDate nextRunAt, boolean active, Double confidence, String expenseCategory, String revenueType, String description, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.user = user;
        this.expenseRevenue = expenseRevenue;
        this.kind = kind;
        this.frequency = frequency;
        this.amount = amount;
        this.label = label;
        this.signature = signature;
        this.nextRunAt = nextRunAt;
        this.active = active;
        this.confidence = confidence;
        this.expenseCategory = expenseCategory;
        this.revenueType = revenueType;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getId() { return this.id; }

    public void setId(int id) { this.id = id; }

    public User getUser() { return this.user; }

    public void setUser(User user) { this.user = user; }

    public Revenue getExpenseRevenue() { return this.expenseRevenue; }

    public void setExpenseRevenue(Revenue expenseRevenue) { this.expenseRevenue = expenseRevenue; }

    public String getKind() { return this.kind; }

    public void setKind(String kind) { this.kind = kind; }

    public String getFrequency() { return this.frequency; }

    public void setFrequency(String frequency) { this.frequency = frequency; }

    public double getAmount() { return this.amount; }

    public void setAmount(double amount) { this.amount = amount; }

    public String getLabel() { return this.label; }

    public void setLabel(String label) { this.label = label; }

    public String getSignature() { return this.signature; }

    public void setSignature(String signature) { this.signature = signature; }

    public LocalDate getNextRunAt() { return this.nextRunAt; }

    public void setNextRunAt(LocalDate nextRunAt) { this.nextRunAt = nextRunAt; }

    public boolean isActive() { return this.active; }

    public void setActive(boolean active) { this.active = active; }

    public Double getConfidence() { return this.confidence; }

    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public String getExpenseCategory() { return this.expenseCategory; }

    public void setExpenseCategory(String expenseCategory) { this.expenseCategory = expenseCategory; }

    public String getRevenueType() { return this.revenueType; }

    public void setRevenueType(String revenueType) { this.revenueType = revenueType; }

    public String getDescription() { return this.description; }

    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return this.createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return this.updatedAt; }

    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "RecurringTransactionRule{" + "id=" + this.id + ", label='" + this.label + '\'' + ", amount=" + this.amount + '}';
    }
}
