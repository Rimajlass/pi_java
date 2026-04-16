package pi.entities;

import java.sql.Date;

public class FinancialGoal {
    private int id;
    private int savingAccountId;
    private String nom;
    private double montantCible;
    private double montantActuel;
    private Date dateLimite;
    private int priorite;

    public FinancialGoal() {
    }

    public FinancialGoal(int savingAccountId, String nom, double montantCible, double montantActuel, Date dateLimite, int priorite) {
        this.savingAccountId = savingAccountId;
        this.nom = nom;
        this.montantCible = montantCible;
        this.montantActuel = montantActuel;
        this.dateLimite = dateLimite;
        this.priorite = priorite;
    }

    public FinancialGoal(int id, int savingAccountId, String nom, double montantCible, double montantActuel, Date dateLimite, int priorite) {
        this.id = id;
        this.savingAccountId = savingAccountId;
        this.nom = nom;
        this.montantCible = montantCible;
        this.montantActuel = montantActuel;
        this.dateLimite = dateLimite;
        this.priorite = priorite;
    }

    public int getId() {
        return id;
    }

    public int getSavingAccountId() {
        return savingAccountId;
    }

    public String getNom() {
        return nom;
    }

    public double getMontantCible() {
        return montantCible;
    }

    public double getMontantActuel() {
        return montantActuel;
    }

    public Date getDateLimite() {
        return dateLimite;
    }

    public int getPriorite() {
        return priorite;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setSavingAccountId(int savingAccountId) {
        this.savingAccountId = savingAccountId;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setMontantCible(double montantCible) {
        this.montantCible = montantCible;
    }

    public void setMontantActuel(double montantActuel) {
        this.montantActuel = montantActuel;
    }

    public void setDateLimite(Date dateLimite) {
        this.dateLimite = dateLimite;
    }

    public void setPriorite(int priorite) {
        this.priorite = priorite;
    }

    @Override
    public String toString() {
        return "FinancialGoal{" +
                "id=" + id +
                ", savingAccountId=" + savingAccountId +
                ", nom='" + nom + '\'' +
                ", montantCible=" + montantCible +
                ", montantActuel=" + montantActuel +
                ", dateLimite=" + dateLimite +
                ", priorite=" + priorite +
                '}';
    }
}