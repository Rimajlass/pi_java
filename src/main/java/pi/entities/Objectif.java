package pi.entities;

import java.time.LocalDate;

public class Objectif {

    public static final String P_BASSE = "BASSE";
    public static final String P_NORMALE = "NORMALE";
    public static final String P_HAUTE = "HAUTE";
    public static final String P_CRITIQUE = "CRITIQUE";

    private int id;
    private String name;
    private double targetMultiplier;
    private double initialAmount;
    private double targetAmount;
    private boolean completed;
    private LocalDate createdAt;
    /** Business priority for portfolio steering. */
    private String priorite;

    public Objectif(String name, double targetMultiplier, double initialAmount, double targetAmount,
                    boolean completed, LocalDate createdAt) {
        this(name, targetMultiplier, initialAmount, targetAmount, completed, createdAt, P_NORMALE);
    }

    public Objectif(String name, double targetMultiplier, double initialAmount, double targetAmount,
                    boolean completed, LocalDate createdAt, String priorite) {
        this.name = name;
        this.targetMultiplier = targetMultiplier;
        this.initialAmount = initialAmount;
        this.targetAmount = targetAmount;
        this.completed = completed;
        this.createdAt = createdAt;
        this.priorite = normalizePriorite(priorite);
    }

    public Objectif(int id, String name, double targetMultiplier, double initialAmount, double targetAmount,
                    boolean completed, LocalDate createdAt, String priorite) {
        this.id = id;
        this.name = name;
        this.targetMultiplier = targetMultiplier;
        this.initialAmount = initialAmount;
        this.targetAmount = targetAmount;
        this.completed = completed;
        this.createdAt = createdAt;
        this.priorite = normalizePriorite(priorite);
    }

    private static String normalizePriorite(String p) {
        if (p == null || p.isBlank()) {
            return P_NORMALE;
        }
        String u = p.trim().toUpperCase();
        return switch (u) {
            case P_BASSE, P_NORMALE, P_HAUTE, P_CRITIQUE -> u;
            default -> P_NORMALE;
        };
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getTargetMultiplier() {
        return this.targetMultiplier;
    }

    public void setTargetMultiplier(double targetMultiplier) {
        this.targetMultiplier = targetMultiplier;
    }

    public double getInitialAmount() {
        return this.initialAmount;
    }

    public void setInitialAmount(double initialAmount) {
        this.initialAmount = initialAmount;
    }

    public double getTargetAmount() {
        return this.targetAmount;
    }

    public void setTargetAmount(double targetAmount) {
        this.targetAmount = targetAmount;
    }

    public boolean isCompleted() {
        return this.completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public LocalDate getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    public String getPriorite() {
        return priorite != null ? priorite : P_NORMALE;
    }

    public void setPriorite(String priorite) {
        this.priorite = normalizePriorite(priorite);
    }

    @Override
    public String toString() {
        return "Objectif{" + "id=" + this.id + ", name='" + this.name + '\'' + ", targetAmount=" + this.targetAmount + '}';
    }
}
