package pi.entities;

public class Cours {

    private int id;
    private User user;
    private String titre;
    private String contenuTexte;
    private String typeMedia;
    private String urlMedia;

    public Cours(User user, String titre, String contenuTexte, String typeMedia, String urlMedia) {
        this.user = user;
        this.titre = titre;
        this.contenuTexte = contenuTexte;
        this.typeMedia = typeMedia;
        this.urlMedia = urlMedia;
    }

    public Cours(int id, User user, String titre, String contenuTexte, String typeMedia, String urlMedia) {
        this.id = id;
        this.user = user;
        this.titre = titre;
        this.contenuTexte = contenuTexte;
        this.typeMedia = typeMedia;
        this.urlMedia = urlMedia;
    }

    public int getId() { return this.id; }

    public void setId(int id) { this.id = id; }

    public User getUser() { return this.user; }

    public void setUser(User user) { this.user = user; }

    public String getTitre() { return this.titre; }

    public void setTitre(String titre) { this.titre = titre; }

    public String getContenuTexte() { return this.contenuTexte; }

    public void setContenuTexte(String contenuTexte) { this.contenuTexte = contenuTexte; }

    public String getTypeMedia() { return this.typeMedia; }

    public void setTypeMedia(String typeMedia) { this.typeMedia = typeMedia; }

    public String getUrlMedia() { return this.urlMedia; }

    public void setUrlMedia(String urlMedia) { this.urlMedia = urlMedia; }

    @Override
    public String toString() {
        return "Cours{" + "id=" + this.id + ", titre='" + this.titre + '\'' + '}';
    }
}
