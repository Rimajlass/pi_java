package pi.entities;

import java.time.LocalDateTime;

public class AiObjectiveReport {

    private int id;
    private Objectif objectif;
    private String content;
    private Integer riskScore;
    private LocalDateTime createdAt;

    public AiObjectiveReport(Objectif objectif, String content, Integer riskScore, LocalDateTime createdAt) {
        this.objectif = objectif;
        this.content = content;
        this.riskScore = riskScore;
        this.createdAt = createdAt;
    }

    public AiObjectiveReport(int id, Objectif objectif, String content, Integer riskScore, LocalDateTime createdAt) {
        this.id = id;
        this.objectif = objectif;
        this.content = content;
        this.riskScore = riskScore;
        this.createdAt = createdAt;
    }

    public int getId() { return this.id; }

    public void setId(int id) { this.id = id; }

    public Objectif getObjectif() { return this.objectif; }

    public void setObjectif(Objectif objectif) { this.objectif = objectif; }

    public String getContent() { return this.content; }

    public void setContent(String content) { this.content = content; }

    public Integer getRiskScore() { return this.riskScore; }

    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }

    public LocalDateTime getCreatedAt() { return this.createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "AiObjectiveReport{" + "id=" + this.id + ", riskScore=" + this.riskScore + '}';
    }
}
