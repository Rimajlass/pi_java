package pi.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Revenue {

    private int id;
    private User user;
    private double amount;
    private String type;
    private LocalDate receivedAt;
    private String description;
    private LocalDateTime createdAt;

    public Revenue(User user, double amount, String type, LocalDate receivedAt, String description, LocalDateTime createdAt) {
        this.user = user;
        this.amount = amount;
        this.type = type;
        this.receivedAt = receivedAt;
        this.description = description;
        this.createdAt = createdAt;
    }

    public Revenue() {
    }

    public Revenue(int id, User user, double amount, String type, LocalDate receivedAt, String description, LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.amount = amount;
        this.type = type;
        this.receivedAt = receivedAt;
        this.description = description;
        this.createdAt = createdAt;
    }

    public int getId() { return this.id; }

    public void setId(int id) { this.id = id; }

    public User getUser() { return this.user; }

    public void setUser(User user) { this.user = user; }

    public double getAmount() { return this.amount; }

    public void setAmount(double amount) { this.amount = amount; }

    public String getType() { return this.type; }

    public void setType(String type) { this.type = type; }

    public LocalDate getReceivedAt() { return this.receivedAt; }

    public void setReceivedAt(LocalDate receivedAt) { this.receivedAt = receivedAt; }

    public String getDescription() { return this.description; }

    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return this.createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Revenue{" + "id=" + this.id + ", amount=" + this.amount + ", type='" + this.type + '\'' + '}';
    }
}
