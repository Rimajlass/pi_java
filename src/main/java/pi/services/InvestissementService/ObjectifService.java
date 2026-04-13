package pi.services.InvestissementService;

import pi.entities.Crypto;
import pi.entities.Investissement;
import pi.entities.Objectif;
import pi.tools.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ObjectifService {

    private Connection cnx;

    public ObjectifService() {
        cnx = MyDatabase.getInstance().getCnx();
    }

    public void add(Objectif obj) throws Exception {
        String sql = "INSERT INTO objectif (name, target_multiplier, initial_amount, target_amount, is_completed, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement pst = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        pst.setString(1, obj.getName());
        pst.setDouble(2, obj.getTargetMultiplier());
        pst.setDouble(3, obj.getInitialAmount());
        pst.setDouble(4, obj.getTargetAmount());
        pst.setBoolean(5, obj.isCompleted());
        pst.setDate(6, Date.valueOf(obj.getCreatedAt()));
        pst.executeUpdate();

        ResultSet keys = pst.getGeneratedKeys();
        if (keys.next()) {
            obj.setId(keys.getInt(1));
        }
    }

    public void linkInvestissement(int objectifId, int investissementId) throws Exception {
        String sql = "UPDATE investissement SET objectif_id = ? WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(sql);
        pst.setInt(1, objectifId);
        pst.setInt(2, investissementId);
        pst.executeUpdate();
    }

    public List<Objectif> getAll() throws Exception {
        List<Objectif> list = new ArrayList<>();
        String sql = "SELECT * FROM objectif";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Objectif obj = new Objectif(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getDouble("target_multiplier"),
                    rs.getDouble("initial_amount"),
                    rs.getDouble("target_amount"),
                    rs.getBoolean("is_completed"),
                    rs.getDate("created_at").toLocalDate()
            );
            list.add(obj);
        }
        return list;
    }

    public double getCurrentValue(int objectifId) throws Exception {
        String sql = "SELECT SUM(c.currentprice * i.quantity) FROM investissement i JOIN crypto c ON i.crypto_id = c.id WHERE i.objectif_id = ?";
        PreparedStatement pst = cnx.prepareStatement(sql);
        pst.setInt(1, objectifId);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return rs.getDouble(1);
        }
        return 0;
    }

    public void checkAndMarkCompleted(int objectifId) throws Exception {
        String sqlGet = "SELECT target_amount FROM objectif WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(sqlGet);
        pst.setInt(1, objectifId);
        ResultSet rs = pst.executeQuery();

        if (rs.next()) {
            double targetAmount = rs.getDouble("target_amount");
            double currentValue = getCurrentValue(objectifId);

            if (currentValue >= targetAmount) {
                String sqlUpdate = "UPDATE objectif SET is_completed = 1 WHERE id = ?";
                PreparedStatement pst2 = cnx.prepareStatement(sqlUpdate);
                pst2.setInt(1, objectifId);
                pst2.executeUpdate();
            }
        }
    }
    public List<Investissement> getLinked(int objectifId) throws Exception {
        List<Investissement> list = new ArrayList<>();
        String sql = "SELECT i.id, i.amount_invested, i.buy_price, i.quantity, i.created_at, "
                + "c.id AS crypto_id, c.name, c.symbol, c.apiid, c.currentprice "
                + "FROM investissement i "
                + "JOIN crypto c ON i.crypto_id = c.id "
                + "WHERE i.objectif_id = ?";
        PreparedStatement pst = cnx.prepareStatement(sql);
        pst.setInt(1, objectifId);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            Crypto crypto = new Crypto(
                    rs.getInt("crypto_id"),
                    rs.getString("name"),
                    rs.getString("symbol"),
                    rs.getString("apiid"),
                    rs.getDouble("currentprice")
            );
            Investissement inv = new Investissement(
                    rs.getInt("id"),
                    crypto,
                    null,
                    null,
                    rs.getDouble("amount_invested"),
                    rs.getDouble("buy_price"),
                    rs.getDouble("quantity"),
                    rs.getDate("created_at").toLocalDate()
            );
            list.add(inv);
        }
        return list;
    }

    public void unlinkAll(int objectifId) throws Exception {
        String sql = "UPDATE investissement SET objectif_id = NULL WHERE objectif_id = ?";
        PreparedStatement pst = cnx.prepareStatement(sql);
        pst.setInt(1, objectifId);
        pst.executeUpdate();
    }

    public void update(Objectif obj) throws Exception {
        String sql = "UPDATE objectif SET name=?, target_multiplier=?, initial_amount=?, target_amount=? WHERE id=?";
        PreparedStatement pst = cnx.prepareStatement(sql);
        pst.setString(1, obj.getName());
        pst.setDouble(2, obj.getTargetMultiplier());
        pst.setDouble(3, obj.getInitialAmount());
        pst.setDouble(4, obj.getTargetAmount());
        pst.setInt(5, obj.getId());
        pst.executeUpdate();
    }

    public void delete(int id) throws Exception {
        String sqlUnlink = "UPDATE investissement SET objectif_id = NULL WHERE objectif_id = ?";
        PreparedStatement pst1 = cnx.prepareStatement(sqlUnlink);
        pst1.setInt(1, id);
        pst1.executeUpdate();

        String sqlDelete = "DELETE FROM objectif WHERE id = ?";
        PreparedStatement pst2 = cnx.prepareStatement(sqlDelete);
        pst2.setInt(1, id);
        pst2.executeUpdate();
    }

}
