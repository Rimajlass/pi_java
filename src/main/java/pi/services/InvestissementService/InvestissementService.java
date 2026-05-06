package pi.services.InvestissementService;

import pi.entities.Crypto;
import pi.entities.Investissement;
import pi.entities.User;
import pi.tools.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InvestissementService {

    private Connection cnx() {
        return MyDatabase.getInstance().getCnx();
    }
    public void update(int id, double newAmount, double newQuantity) throws Exception {
        String sql = "UPDATE investissement SET amount_invested=?, quantity=? WHERE id=?";
        PreparedStatement pst = cnx().prepareStatement(sql);
        pst.setDouble(1, newAmount);
        pst.setDouble(2, newQuantity);
        pst.setInt(3, id);
        pst.executeUpdate();
    }

    public void delete(int id) throws Exception {
     String sql = "DELETE FROM investissement WHERE id = ?";
     PreparedStatement pst = cnx().prepareStatement(sql);
     pst.setInt(1,id);
     pst.executeUpdate();

    }

    public void add(Investissement inv) throws Exception {
        String sql = "INSERT INTO investissement (crypto_id, objectif_id, user_id, amount_invested, buy_price, quantity, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement pst = cnx().prepareStatement(sql);

        pst.setInt(1, inv.getCrypto().getId());

        if (inv.getObjectif() != null) {
            pst.setInt(2, inv.getObjectif().getId());
        } else {
            pst.setNull(2, java.sql.Types.INTEGER);
        }

        if (inv.getUser() != null) {
            pst.setInt(3, inv.getUser().getId());
        } else {
            pst.setNull(3, java.sql.Types.INTEGER);
        }

        pst.setDouble(4, inv.getAmountInvested());
        pst.setDouble(5, inv.getBuyPrice());
        pst.setDouble(6, inv.getQuantity());
        pst.setDate(7, Date.valueOf(inv.getCreatedAt()));

        pst.executeUpdate();
    }

    public List<Investissement> getAll() throws Exception {
        List<Investissement> list = new ArrayList<>();

        String sql = "SELECT i.id, i.amount_invested, i.buy_price, i.quantity, i.created_at, "
                + "c.id AS crypto_id, c.name, c.symbol, c.apiid, c.currentprice "
                + "FROM investissement i "
                + "JOIN crypto c ON i.crypto_id = c.id";

        Statement st = cnx().createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            list.add(mapRow(rs));
        }

        return list;
    }
    public List<Investissement> getUnlinked() throws Exception {
        List<Investissement> list = new ArrayList<>();

        String sql = "SELECT i.id, i.amount_invested, i.buy_price, i.quantity, i.created_at, "
                + "c.id AS crypto_id, c.name, c.symbol, c.apiid, c.currentprice "
                + "FROM investissement i "
                + "JOIN crypto c ON i.crypto_id = c.id "
                + "WHERE i.objectif_id IS NULL";

        Statement st = cnx().createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            list.add(mapRow(rs));
        }

        return list;
    }

    public List<Investissement> getAllByUser(int userId) throws Exception {
        List<Investissement> list = new ArrayList<>();
        String sql = "SELECT i.id, i.amount_invested, i.buy_price, i.quantity, i.created_at, i.user_id, "
                + "c.id AS crypto_id, c.name, c.symbol, c.apiid, c.currentprice "
                + "FROM investissement i "
                + "JOIN crypto c ON i.crypto_id = c.id "
                + "WHERE i.user_id = ?";

        PreparedStatement pst = cnx().prepareStatement(sql);
        pst.setInt(1, userId);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    public List<Investissement> getUnlinkedByUser(int userId) throws Exception {
        List<Investissement> list = new ArrayList<>();
        String sql = "SELECT i.id, i.amount_invested, i.buy_price, i.quantity, i.created_at, i.user_id, "
                + "c.id AS crypto_id, c.name, c.symbol, c.apiid, c.currentprice "
                + "FROM investissement i "
                + "JOIN crypto c ON i.crypto_id = c.id "
                + "WHERE i.objectif_id IS NULL AND i.user_id = ?";

        PreparedStatement pst = cnx().prepareStatement(sql);
        pst.setInt(1, userId);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    private Investissement mapRow(ResultSet rs) throws SQLException {
        Crypto crypto = new Crypto(
                rs.getInt("crypto_id"),
                rs.getString("name"),
                rs.getString("symbol"),
                rs.getString("apiid"),
                rs.getDouble("currentprice")
        );
        Integer uid = null;
        try {
            int parsed = rs.getInt("user_id");
            if (!rs.wasNull()) {
                uid = parsed;
            }
        } catch (SQLException ignored) {
            // Legacy queries might not project user_id.
        }
        User user = null;
        if (uid != null) {
            user = new User();
            user.setId(uid);
        }
        return new Investissement(
                rs.getInt("id"),
                crypto,
                null,
                user,
                rs.getDouble("amount_invested"),
                rs.getDouble("buy_price"),
                rs.getDouble("quantity"),
                rs.getDate("created_at").toLocalDate()
        );
    }



}