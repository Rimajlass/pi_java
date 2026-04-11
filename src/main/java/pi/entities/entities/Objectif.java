package pi.entities.entities;

import java.time.LocalDate;

public class Objectif {

    private int id;
    private String name;
    private double targetMultiplier;
    private double initialAmount;
    private double targetAmount;
    private boolean completed;
    private LocalDate createdAt;

    public Objectif(String name, double targetMultiplier, double initialAmount, double targetAmount, boolean completed, LocalDate createdAt) {
        this.name = name;
        this.targetMultiplier = targetMultiplier;
        this.initialAmount = initialAmount;
        this.targetAmount = targetAmount;
        this.completed = completed;
        this.createdAt = createdAt;
    }

    public Objectif(int id, String name, double targetMultiplier, double initialAmount, double targetAmount, boolean completed, LocalDate createdAt) {
        this.id = id;
        this.name = name;
        this.targetMultiplier = targetMultiplier;
        this.initialAmount = initialAmount;
        this.targetAmount = targetAmount;
        this.completed = completed;
        this.createdAt = createdAt;
    }

    public int getId() { return this.id; }

    public void setId(int id) { this.id = id; }

    public String getName() { return this.name; }

    public void setName(String name) { this.name = name; }

    public double getTargetMultiplier() { return this.targetMultiplier; }

    public void setTargetMultiplier(double targetMultiplier) { this.targetMultiplier = targetMultiplier; }

    public double getInitialAmount() { return this.initialAmount; }

    public void setInitialAmount(double initialAmount) { this.initialAmount = initialAmount; }

    public double getTargetAmount() { return this.targetAmount; }

    public void setTargetAmount(double targetAmount) { this.targetAmount = targetAmount; }

    public boolean isCompleted() { return this.completed; }

    public void setCompleted(boolean completed) { this.completed = completed; }

    public LocalDate getCreatedAt() { return this.createdAt; }

    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Objectif{" + "id=" + this.id + ", name='" + this.name + '\'' + ", targetAmount=" + this.targetAmount + '}';
    }
}
