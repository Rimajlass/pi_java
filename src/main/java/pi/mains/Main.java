package pi.mains;

import pi.entities.FinancialGoal;
import pi.entities.SavingAccount;
import pi.services.FinancialGoalService;
import pi.services.SavingAccountService;

import java.sql.Date;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        SavingAccountService sas = new SavingAccountService();
        FinancialGoalService fgs = new FinancialGoalService();

        System.out.println("=========== TEST CRUD SAVING_ACCOUNT ===========");

        // 1) AJOUTER SavingAccount
        SavingAccount saving = new SavingAccount(1, 1200.0, Date.valueOf("2026-04-08"), 4.5);
        sas.ajouter(saving);

        // 2) AFFICHER SavingAccount
        System.out.println("----- Liste des saving accounts après ajout -----");
        List<SavingAccount> savingAccounts = sas.afficher();
        for (SavingAccount sa : savingAccounts) {
            System.out.println(sa);
        }

        // 3) MODIFIER SavingAccount
        if (!savingAccounts.isEmpty()) {
            int savingId = savingAccounts.get(savingAccounts.size() - 1).getId();

            SavingAccount savingModifie = new SavingAccount(
                    savingId,
                    1,
                    2000.0,
                    Date.valueOf("2026-04-10"),
                    6.0
            );
            sas.modifier(savingModifie);

            System.out.println("----- Liste des saving accounts après modification -----");
            for (SavingAccount sa : sas.afficher()) {
                System.out.println(sa);
            }

            System.out.println("=========== TEST CRUD FINANCIAL_GOAL ===========");

            // 4) AJOUTER FinancialGoal lié au saving account modifié
            FinancialGoal goal = new FinancialGoal(
                    savingId,
                    "Acheter telephone",
                    3000.0,
                    500.0,
                    Date.valueOf("2026-12-31"),
                    3
            );
            fgs.ajouter(goal);

            // 5) AFFICHER FinancialGoal
            System.out.println("----- Liste des financial goals après ajout -----");
            List<FinancialGoal> goals = fgs.afficher();
            for (FinancialGoal fg : goals) {
                System.out.println(fg);
            }

            // 6) MODIFIER FinancialGoal
            if (!goals.isEmpty()) {
                int goalId = goals.get(goals.size() - 1).getId();

                FinancialGoal goalModifie = new FinancialGoal(
                        goalId,
                        savingId,
                        "Acheter PC",
                        4500.0,
                        1200.0,
                        Date.valueOf("2026-11-30"),
                        2
                );
                fgs.modifier(goalModifie);

                System.out.println("----- Liste des financial goals après modification -----");
                for (FinancialGoal fg : fgs.afficher()) {
                    System.out.println(fg);
                }

                // 7) SUPPRIMER FinancialGoal
                fgs.supprimer(goalId);

                System.out.println("----- Liste des financial goals après suppression -----");
                for (FinancialGoal fg : fgs.afficher()) {
                    System.out.println(fg);
                }
            } else {
                System.out.println("Aucun financial goal trouvé pour le test de modification/suppression.");
            }

            // 8) SUPPRIMER SavingAccount
            sas.supprimer(savingId);

            System.out.println("----- Liste des saving accounts après suppression -----");
            for (SavingAccount sa : sas.afficher()) {
                System.out.println(sa);
            }

        } else {
            System.out.println("Aucun saving account trouvé pour le test de modification/suppression.");
        }

        System.out.println("=========== FIN DES TESTS ===========");
    }
}