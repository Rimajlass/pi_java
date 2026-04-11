package pi.interfaces;

import pi.entities.Quiz;
import java.util.List;

public interface IQuizService {
    void ajouter(Quiz quiz);
    void modifier(Quiz quiz);
    void supprimer(int id);
    List<Quiz> afficher();
    Quiz recupererParId(int id);
}