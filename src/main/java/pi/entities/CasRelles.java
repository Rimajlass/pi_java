package pi.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CasRelles {

    private int id;
    private User user;
    private Imprevus imprevus;
    private User confirmedBy;
    private FinancialGoal financialGoal;
    private String titre;
    private String description;
    private String type;
    private String categorie;
    private double montant;
    private String solution;
    private LocalDate dateEffet;
    private String resultat;
    private String raisonRefus;
    private LocalDateTime confirmedAt;
    private String justificatifFileName;
    private LocalDateTime updatedAt;

    public CasRelles() {
    }

    public CasRelles(Imprevus imprevus, String titre, String description, String type, String categorie, double montant, String solution, LocalDate dateEffet, String justificatifFileName) {
        this.imprevus = imprevus;
        this.titre = titre;
        this.description = description;
        this.type = type;
        this.categorie = categorie;
        this.montant = montant;
        this.solution = solution;
        this.dateEffet = dateEffet;
        this.justificatifFileName = justificatifFileName;
    }

    public CasRelles(int id, Imprevus imprevus, String titre, String description, String type, String categorie, double montant, String solution, LocalDate dateEffet, String justificatifFileName) {
        this.id = id;
        this.imprevus = imprevus;
        this.titre = titre;
        this.description = description;
        this.type = type;
        this.categorie = categorie;
        this.montant = montant;
        this.solution = solution;
        this.dateEffet = dateEffet;
        this.justificatifFileName = justificatifFileName;
    }

    public CasRelles(User user, Imprevus imprevus, User confirmedBy, FinancialGoal financialGoal, String titre, String description, String type, String categorie, double montant, String solution, LocalDate dateEffet, String resultat, String raisonRefus, LocalDateTime confirmedAt, String justificatifFileName, LocalDateTime updatedAt) {
        this.user = user;
        this.imprevus = imprevus;
        this.confirmedBy = confirmedBy;
        this.financialGoal = financialGoal;
        this.titre = titre;
        this.description = description;
        this.type = type;
        this.categorie = categorie;
        this.montant = montant;
        this.solution = solution;
        this.dateEffet = dateEffet;
        this.resultat = resultat;
        this.raisonRefus = raisonRefus;
        this.confirmedAt = confirmedAt;
        this.justificatifFileName = justificatifFileName;
        this.updatedAt = updatedAt;
    }

    public CasRelles(int id, User user, Imprevus imprevus, User confirmedBy, FinancialGoal financialGoal, String titre, String description, String type, String categorie, double montant, String solution, LocalDate dateEffet, String resultat, String raisonRefus, LocalDateTime confirmedAt, String justificatifFileName, LocalDateTime updatedAt) {
        this.id = id;
        this.user = user;
        this.imprevus = imprevus;
        this.confirmedBy = confirmedBy;
        this.financialGoal = financialGoal;
        this.titre = titre;
        this.description = description;
        this.type = type;
        this.categorie = categorie;
        this.montant = montant;
        this.solution = solution;
        this.dateEffet = dateEffet;
        this.resultat = resultat;
        this.raisonRefus = raisonRefus;
        this.confirmedAt = confirmedAt;
        this.justificatifFileName = justificatifFileName;
        this.updatedAt = updatedAt;
    }

    public int getId() { return this.id; }

    public void setId(int id) { this.id = id; }

    public User getUser() { return this.user; }

    public void setUser(User user) { this.user = user; }

    public Imprevus getImprevus() { return this.imprevus; }

    public void setImprevus(Imprevus imprevus) { this.imprevus = imprevus; }

    public User getConfirmedBy() { return this.confirmedBy; }

    public void setConfirmedBy(User confirmedBy) { this.confirmedBy = confirmedBy; }

    public FinancialGoal getFinancialGoal() { return this.financialGoal; }

    public void setFinancialGoal(FinancialGoal financialGoal) { this.financialGoal = financialGoal; }

    public String getTitre() { return this.titre; }

    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return this.description; }

    public void setDescription(String description) { this.description = description; }

    public String getType() { return this.type; }

    public void setType(String type) { this.type = type; }

    public String getCategorie() { return this.categorie; }

    public void setCategorie(String categorie) { this.categorie = categorie; }

    public double getMontant() { return this.montant; }

    public void setMontant(double montant) { this.montant = montant; }

    public String getSolution() { return this.solution; }

    public void setSolution(String solution) { this.solution = solution; }

    public LocalDate getDateEffet() { return this.dateEffet; }

    public void setDateEffet(LocalDate dateEffet) { this.dateEffet = dateEffet; }

    public String getResultat() { return this.resultat; }

    public void setResultat(String resultat) { this.resultat = resultat; }

    public String getRaisonRefus() { return this.raisonRefus; }

    public void setRaisonRefus(String raisonRefus) { this.raisonRefus = raisonRefus; }

    public LocalDateTime getConfirmedAt() { return this.confirmedAt; }

    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }

    public String getJustificatifFileName() { return this.justificatifFileName; }

    public void setJustificatifFileName(String justificatifFileName) { this.justificatifFileName = justificatifFileName; }

    public LocalDateTime getUpdatedAt() { return this.updatedAt; }

    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "CasRelles{" + "id=" + this.id + ", titre='" + this.titre + '\'' + ", montant=" + this.montant + '}';
    }
}
