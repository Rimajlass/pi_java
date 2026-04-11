package pi.mains;

import pi.entities.Cours;
import pi.entities.User;
import pi.services.CoursService;

public class MainCours {
    public static void main(String[] args) {

        CoursService cs = new CoursService();

        User u = new User();
        u.setId(1); // id d'un utilisateur existant en BD

        Cours c = new Cours(u, "Java JDBC", "Introduction au JDBC", "video", "https://youtube.com/test");
        cs.ajouter(c);


        System.out.println("Liste des cours :");
        for (Cours cours : cs.afficher()) {
            System.out.println(cours);
        }

        // 🔹 1. Récupérer un cours existant
        Cours cours = cs.recupererParId(1);

        if (cours != null) {

            // 🔹 2. Modifier le titre
            cours.setTitre("Nouveau titre");

            // 🔹 3. Appliquer la modification dans la BD
            cs.modifier(cours);

            System.out.println("Cours modifié !");
        } else {
            System.out.println("Cours introuvable !");
        }
        cs.supprimer(1);
    }
}