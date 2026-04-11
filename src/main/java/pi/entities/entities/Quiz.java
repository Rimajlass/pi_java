package pi.entities.entities;

public class Quiz {

    private int id;
    private Cours cours;
    private User user;
    private String question;
    private String choixReponses;
    private String reponseCorrecte;
    private int pointsValeur;

    public Quiz(Cours cours, User user, String question, String choixReponses, String reponseCorrecte, int pointsValeur) {
        this.cours = cours;
        this.user = user;
        this.question = question;
        this.choixReponses = choixReponses;
        this.reponseCorrecte = reponseCorrecte;
        this.pointsValeur = pointsValeur;
    }

    public Quiz(int id, Cours cours, User user, String question, String choixReponses, String reponseCorrecte, int pointsValeur) {
        this.id = id;
        this.cours = cours;
        this.user = user;
        this.question = question;
        this.choixReponses = choixReponses;
        this.reponseCorrecte = reponseCorrecte;
        this.pointsValeur = pointsValeur;
    }

    public int getId() { return this.id; }

    public void setId(int id) { this.id = id; }

    public Cours getCours() { return this.cours; }

    public void setCours(Cours cours) { this.cours = cours; }

    public User getUser() { return this.user; }

    public void setUser(User user) { this.user = user; }

    public String getQuestion() { return this.question; }

    public void setQuestion(String question) { this.question = question; }

    public String getChoixReponses() { return this.choixReponses; }

    public void setChoixReponses(String choixReponses) { this.choixReponses = choixReponses; }

    public String getReponseCorrecte() { return this.reponseCorrecte; }

    public void setReponseCorrecte(String reponseCorrecte) { this.reponseCorrecte = reponseCorrecte; }

    public int getPointsValeur() { return this.pointsValeur; }

    public void setPointsValeur(int pointsValeur) { this.pointsValeur = pointsValeur; }

    @Override
    public String toString() {
        return "Quiz{" + "id=" + this.id + ", pointsValeur=" + this.pointsValeur + '}';
    }
}
