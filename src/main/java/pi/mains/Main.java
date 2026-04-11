package pi.mains;

import pi.entities.Imprevus;
import pi.services.ImprevusService;

public class Main {
    public static void main(String[] args) {
        ImprevusService service = new ImprevusService();

        Imprevus i1 = new Imprevus("Panne voiture", "Transport", 500.50);
        service.ajouter(i1);

        System.out.println("=== Liste des imprevus ===");
        for (Imprevus i : service.afficher()) {
            System.out.println(i);
        }

        Imprevus imp = service.getById(1);
        if (imp != null) {
            imp.setTitre("ok");
            imp.setBudget(650);
            service.modifier(imp);
        }

        service.supprimer(7);
    }
}
