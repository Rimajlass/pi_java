package pi.entities;

public class Imprevus {

    private int id;
    private String titre;
    private String type;
    private double budget;
    private String messageEducatif;

    public Imprevus(String titre, String type, double budget, String messageEducatif) {
        this.titre = titre;
        this.type = type;
        this.budget = budget;
        this.messageEducatif = messageEducatif;
    }

    public Imprevus(int id, String titre, String type, double budget, String messageEducatif) {
        this.id = id;
        this.titre = titre;
        this.type = type;
        this.budget = budget;
        this.messageEducatif = messageEducatif;
    }

    public int getId() { return this.id; }

    public void setId(int id) { this.id = id; }

    public String getTitre() { return this.titre; }

    public void setTitre(String titre) { this.titre = titre; }

    public String getType() { return this.type; }

    public void setType(String type) { this.type = type; }

    public double getBudget() { return this.budget; }

    public void setBudget(double budget) { this.budget = budget; }

    public String getMessageEducatif() { return this.messageEducatif; }

    public void setMessageEducatif(String messageEducatif) { this.messageEducatif = messageEducatif; }

    @Override
    public String toString() {
        return "Imprevus{" + "id=" + this.id + ", titre='" + this.titre + '\'' + ", budget=" + this.budget + '}';
    }
}
