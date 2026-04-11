package pi.mains;

import pi.entities.Cours;
import pi.entities.Quiz;
import pi.entities.User;
import pi.services.QuizService;

public class MainQuiz {
    public static void main(String[] args) {

        QuizService qs = new QuizService();

        User u = new User();
        u.setId(1); // user existant

        Cours c = new Cours();
        c.setId(2); // cours existant

        Quiz q = new Quiz(
                c,
                u,
                "Qu'est-ce que JDBC ?",
                "API Java,Langage,Framework",
                "API Java",
                10
        );

        qs.ajouter(q);

        System.out.println("Liste des quiz :");
        for (Quiz quiz : qs.afficher()) {
            System.out.println(quiz);
        }
        Quiz quiz = qs.recupererParId(2);
        quiz.setPointsValeur(20);
        qs.modifier(quiz);
        qs.supprimer(2);
    }
}