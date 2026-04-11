package pi.services;

import pi.entities.Cours;
import pi.entities.User;
import pi.interfaces.ICoursService;
import pi.tools.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CoursService implements ICoursService {

    Connection cnx;

    public CoursService() {
        cnx = MyDatabase.getInstance().getCnx();
    }

    @Override
    public void ajouter(Cours cours) {
        String sql = "INSERT INTO cours (user_id, titre, contenu_texte, type_media, url_media) VALUES (?, ?, ?, ?, ?)";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, cours.getUser().getId());
            ps.setString(2, cours.getTitre());
            ps.setString(3, cours.getContenuTexte());
            ps.setString(4, cours.getTypeMedia());
            ps.setString(5, cours.getUrlMedia());

            ps.executeUpdate();
            System.out.println("Cours ajouté avec succès !");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void modifier(Cours cours) {
        String sql = "UPDATE cours SET user_id=?, titre=?, contenu_texte=?, type_media=?, url_media=? WHERE id=?";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, cours.getUser().getId());
            ps.setString(2, cours.getTitre());
            ps.setString(3, cours.getContenuTexte());
            ps.setString(4, cours.getTypeMedia());
            ps.setString(5, cours.getUrlMedia());
            ps.setInt(6, cours.getId());

            ps.executeUpdate();
            System.out.println("Cours modifié avec succès !");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void supprimer(int id) {
        String sql = "DELETE FROM cours WHERE id=?";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);

            ps.executeUpdate();
            System.out.println("Cours supprimé avec succès !");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public List<Cours> afficher() {
        List<Cours> list = new ArrayList<>();
        String sql = "SELECT * FROM cours";

        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("user_id"));

                Cours c = new Cours(
                        rs.getInt("id"),
                        user,
                        rs.getString("titre"),
                        rs.getString("contenu_texte"),
                        rs.getString("type_media"),
                        rs.getString("url_media")
                );

                list.add(c);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return list;
    }

    @Override
    public Cours recupererParId(int id) {
        String sql = "SELECT * FROM cours WHERE id=?";
        Cours cours = null;

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("user_id"));

                cours = new Cours(
                        rs.getInt("id"),
                        user,
                        rs.getString("titre"),
                        rs.getString("contenu_texte"),
                        rs.getString("type_media"),
                        rs.getString("url_media")
                );
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return cours;
    }
}