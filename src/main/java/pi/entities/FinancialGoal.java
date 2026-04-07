package pi.entities;

import java.time.LocalDate;

public class FinancialGoal {

    private int id;
    private SavingAccount savingAccount;
    private String nom;
    private double montantCible;
    private double montantActuel;
    private LocalDate dateLimite;
    private Integer priorite;

    public FinancialGoal(SavingAccount savingAccount, String nom, double montantCible, double montantActuel, LocalDate dateLimite, Integer priorite) {
        this.savingAccount = savingAccount;
        this.nom = nom;
        this.montantCible = montantCible;
        this.montantActuel = montantActuel;
        this.dateLimite = dateLimite;
        this.priorite = priorite;
    }

    public FinancialGoal(int id, SavingAccount savingAccount, String nom, double montantCible, double montantActuel, LocalDate dateLimite, Integer priorite) {
        this.id = id;
        this.savingAccount = savingAccount;
        this.nom = nom;
        this.montantCible = montantCible;
        this.montantActuel = montantActuel;
        this.dateLimite = dateLimite;
        this.priorite = priorite;
    }

    public int getId() { return this.id; }

    public void setId(int id) { this.id = id; }

    public SavingAccount getSavingAccount() { return this.savingAccount; }

    public void setSavingAccount(SavingAccount savingAccount) { this.savingAccount = savingAccount; }

    public String getNom() { return this.nom; }

    public void setNom(String nom) { this.nom = nom; }

    public double getMontantCible() { return this.montantCible; }

    public void setMontantCible(double montantCible) { this.montantCible = montantCible; }

    public double getMontantActuel() { return this.montantActuel; }

    public void setMontantActuel(double montantActuel) { this.montantActuel = montantActuel; }

    public LocalDate getDateLimite() { return this.dateLimite; }

    public void setDateLimite(LocalDate dateLimite) { this.dateLimite = dateLimite; }

    public Integer getPriorite() { return this.priorite; }

    public void setPriorite(Integer priorite) { this.priorite = priorite; }

    @Override
    public String toString() {
        return "FinancialGoal{" + "id=" + this.id + ", nom='" + this.nom + '\'' + ", montantCible=" + this.montantCible + '}';
    }
}
