package pi.interfaces;

import pi.entities.Cours;
import java.util.List;

public interface ICoursService {
    void ajouter(Cours cours);
    void modifier(Cours cours);
    void supprimer(int id);
    List<Cours> afficher();
    Cours recupererParId(int id);
}