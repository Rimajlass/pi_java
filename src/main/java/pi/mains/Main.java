package pi.mains;

import pi.tools.MyDatabase;
import pi.entities.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import pi.entities.Imprevus;
import pi.services.ImprevusService;


public class Main {
    public static void main(String[] args) {

        ImprevusService service = new ImprevusService();

        // ajout
        Imprevus i1 = new Imprevus("Panne voiture", "Transport",500.50, "Prévoir une épargne d'urgence");
        service.ajouter(i1);

        // affichage
        System.out.println("=== Liste des imprévus ===");
        for (Imprevus i : service.afficher()) {
            System.out.println(i);
        }

        // modification
        Imprevus imp = service.getById(1);

        if (imp != null) {
            System.out.println("ID récupéré = " + imp.getId());
            System.out.println("Titre avant modif = " + imp.getTitre());

            imp.setTitre("ok");
            imp.setBudget(650);
            service.modifier(imp);
        } else {
            System.out.println("Aucun imprévu avec id = 1");
        }

        // suppression
        service.supprimer(7);
    }
}