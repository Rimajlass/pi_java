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

    public Cours() {

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

    public void validate() {
        if (titre == null || titre.trim().isEmpty()) {
            throw new IllegalArgumentException("Le titre ne peut pas être vide.");
        }
        String trimmedTitre = titre.trim();
        if (!trimmedTitre.matches("^[a-zA-Z0-9\\sàâäéèêëîïôöùûüçÀÂÇÉÈÊÏÎÔÙÛÜÿ.-]+$")) {
            throw new IllegalArgumentException("Le titre ne doit contenir que des lettres, chiffres, espaces et certains caractères autorisés (àâäéèêëîïôöùûüç.-).");
        }
        if (contenuTexte == null || contenuTexte.trim().length() < 30) {
            throw new IllegalArgumentException("Le contenu texte doit contenir au minimum 30 caractères.");
        }
        if (typeMedia == null || !(typeMedia.equals("video") || typeMedia.equals("pdf") || typeMedia.equals("image"))) {
            throw new IllegalArgumentException("Le type media doit être 'video', 'pdf' ou 'image' uniquement.");
        }
    }

    @Override
    public String toString() {
        return "Cours{" + "id=" + this.id + ", titre='" + this.titre + '\'' + '}';
    }
}
