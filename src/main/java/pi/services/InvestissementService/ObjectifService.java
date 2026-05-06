package pi.services.InvestissementService;

import pi.entities.Crypto;
import pi.entities.Investissement;
import pi.entities.Objectif;
import pi.tools.MyDatabase;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjectifService {

    public ObjectifService() {
        ensurePrioriteColumn();
    }

    private Connection cnx() {
        return MyDatabase.getInstance().getCnx();
    }

    private void ensurePrioriteColumn() {
        try {
            Connection c = cnx();
            if (c == null) {
                return;
            }
            DatabaseMetaData md = c.getMetaData();
            try (ResultSet cols = md.getColumns(null, null, "objectif", "priorite")) {
                if (!cols.next()) {
                    try (Statement st = c.createStatement()) {
                        st.executeUpdate(
                                "ALTER TABLE objectif ADD COLUMN priorite VARCHAR(20) NOT NULL DEFAULT 'NORMALE'");
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("ensurePrioriteColumn: " + e.getMessage());
        }
    }

    private static String readPriorite(ResultSet rs) {
        try {
            String p = rs.getString("priorite");
            return p != null && !p.isBlank() ? p : Objectif.P_NORMALE;
        } catch (SQLException e) {
            return Objectif.P_NORMALE;
        }
    }

    public void add(Objectif obj) throws Exception {
        String sql = "INSERT INTO objectif (name, target_multiplier, initial_amount, target_amount, is_completed, created_at, priorite) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement pst = cnx().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        pst.setString(1, obj.getName());
        pst.setDouble(2, obj.getTargetMultiplier());
        pst.setDouble(3, obj.getInitialAmount());
        pst.setDouble(4, obj.getTargetAmount());
        pst.setBoolean(5, obj.isCompleted());
        pst.setDate(6, Date.valueOf(obj.getCreatedAt()));
        pst.setString(7, obj.getPriorite());
        pst.executeUpdate();

        ResultSet keys = pst.getGeneratedKeys();
        if (keys.next()) {
            obj.setId(keys.getInt(1));
        }
    }

    public void linkInvestissement(int objectifId, int investissementId) throws Exception {
        String sql = "UPDATE investissement SET objectif_id = ? WHERE id = ?";
        PreparedStatement pst = cnx().prepareStatement(sql);
        pst.setInt(1, objectifId);
        pst.setInt(2, investissementId);
        pst.executeUpdate();
    }

    public void linkInvestissementForUser(int objectifId, int investissementId, int userId) throws Exception {
        String sql = "UPDATE investissement SET objectif_id = ? WHERE id = ? AND user_id = ?";
        PreparedStatement pst = cnx().prepareStatement(sql);
        pst.setInt(1, objectifId);
        pst.setInt(2, investissementId);
        pst.setInt(3, userId);
        pst.executeUpdate();
    }

    public List<Objectif> getAll() throws Exception {
        List<Objectif> list = new ArrayList<>();
        String sql = "SELECT * FROM objectif";
        Statement st = cnx().createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    public List<Objectif> getAllByUser(int userId) throws Exception {
        List<Objectif> list = new ArrayList<>();
        String sql = "SELECT DISTINCT o.* "
                + "FROM objectif o "
                + "JOIN investissement i ON i.objectif_id = o.id "
                + "WHERE i.user_id = ? "
                + "ORDER BY o.created_at DESC, o.id DESC";
        PreparedStatement pst = cnx().prepareStatement(sql);
        pst.setInt(1, userId);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    private Objectif mapRow(ResultSet rs) throws SQLException {
        LocalDate created = rs.getDate("created_at").toLocalDate();
        String priorite = readPriorite(rs);
        return new Objectif(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getDouble("target_multiplier"),
                rs.getDouble("initial_amount"),
                rs.getDouble("target_amount"),
                rs.getBoolean("is_completed"),
                created,
                priorite
        );
    }

    public Map<Integer, Double> getCurrentValuesAll() throws Exception {
        Map<Integer, Double> map = new HashMap<>();
        String sql = "SELECT o.id, COALESCE(SUM(c.currentprice * i.quantity), 0) AS v "
                + "FROM objectif o "
                + "LEFT JOIN investissement i ON i.objectif_id = o.id "
                + "LEFT JOIN crypto c ON i.crypto_id = c.id "
                + "GROUP BY o.id";
        Statement st = cnx().createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            map.put(rs.getInt("id"), rs.getDouble("v"));
        }
        return map;
    }

    public Map<Integer, Double> getCurrentValuesAllByUser(int userId) throws Exception {
        Map<Integer, Double> map = new HashMap<>();
        String sql = "SELECT o.id, COALESCE(SUM(c.currentprice * i.quantity), 0) AS v "
                + "FROM objectif o "
                + "JOIN investissement i ON i.objectif_id = o.id "
                + "LEFT JOIN crypto c ON i.crypto_id = c.id "
                + "WHERE i.user_id = ? "
                + "GROUP BY o.id";
        PreparedStatement pst = cnx().prepareStatement(sql);
        pst.setInt(1, userId);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            map.put(rs.getInt("id"), rs.getDouble("v"));
        }
        return map;
    }

    public double getCurrentValue(int objectifId) throws Exception {
        String sql = "SELECT SUM(c.currentprice * i.quantity) FROM investissement i JOIN crypto c ON i.crypto_id = c.id WHERE i.objectif_id = ?";
        PreparedStatement pst = cnx().prepareStatement(sql);
        pst.setInt(1, objectifId);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return rs.getDouble(1);
        }
        return 0;
    }

    public double getCurrentValueByUser(int objectifId, int userId) throws Exception {
        String sql = "SELECT SUM(c.currentprice * i.quantity) "
                + "FROM investissement i "
                + "JOIN crypto c ON i.crypto_id = c.id "
                + "WHERE i.objectif_id = ? AND i.user_id = ?";
        PreparedStatement pst = cnx().prepareStatement(sql);
        pst.setInt(1, objectifId);
        pst.setInt(2, userId);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return rs.getDouble(1);
        }
        return 0;
    }

    public void checkAndMarkCompleted(int objectifId) throws Exception {
        String sqlGet = "SELECT target_amount FROM objectif WHERE id = ?";
        PreparedStatement pst = cnx().prepareStatement(sqlGet);
        pst.setInt(1, objectifId);
        ResultSet rs = pst.executeQuery();

        if (rs.next()) {
            double targetAmount = rs.getDouble("target_amount");
            double currentValue = getCurrentValue(objectifId);

            if (currentValue >= targetAmount) {
                String sqlUpdate = "UPDATE objectif SET is_completed = 1 WHERE id = ?";
                PreparedStatement pst2 = cnx().prepareStatement(sqlUpdate);
                pst2.setInt(1, objectifId);
                pst2.executeUpdate();
            }
        }
    }

    public void checkAndMarkCompletedForUser(int objectifId, int userId) throws Exception {
        if (!isOwnedByUser(objectifId, userId)) {
            return;
        }
        String sqlGet = "SELECT target_amount FROM objectif WHERE id = ?";
        PreparedStatement pst = cnx().prepareStatement(sqlGet);
        pst.setInt(1, objectifId);
        ResultSet rs = pst.executeQuery();

        if (rs.next()) {
            double targetAmount = rs.getDouble("target_amount");
            double currentValue = getCurrentValueByUser(objectifId, userId);

            if (currentValue >= targetAmount) {
                String sqlUpdate = "UPDATE objectif SET is_completed = 1 WHERE id = ?";
                PreparedStatement pst2 = cnx().prepareStatement(sqlUpdate);
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
        PreparedStatement pst = cnx().prepareStatement(sql);
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

    public List<Investissement> getLinkedByUser(int objectifId, int userId) throws Exception {
        List<Investissement> list = new ArrayList<>();
        String sql = "SELECT i.id, i.amount_invested, i.buy_price, i.quantity, i.created_at, "
                + "c.id AS crypto_id, c.name, c.symbol, c.apiid, c.currentprice "
                + "FROM investissement i "
                + "JOIN crypto c ON i.crypto_id = c.id "
                + "WHERE i.objectif_id = ? AND i.user_id = ?";
        PreparedStatement pst = cnx().prepareStatement(sql);
        pst.setInt(1, objectifId);
        pst.setInt(2, userId);
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
        PreparedStatement pst = cnx().prepareStatement(sql);
        pst.setInt(1, objectifId);
        pst.executeUpdate();
    }

    public void unlinkAllForUser(int objectifId, int userId) throws Exception {
        String sql = "UPDATE investissement SET objectif_id = NULL WHERE objectif_id = ? AND user_id = ?";
        PreparedStatement pst = cnx().prepareStatement(sql);
        pst.setInt(1, objectifId);
        pst.setInt(2, userId);
        pst.executeUpdate();
    }

    public void update(Objectif obj) throws Exception {
        String sql = "UPDATE objectif SET name=?, target_multiplier=?, initial_amount=?, target_amount=?, priorite=? WHERE id=?";
        PreparedStatement pst = cnx().prepareStatement(sql);
        pst.setString(1, obj.getName());
        pst.setDouble(2, obj.getTargetMultiplier());
        pst.setDouble(3, obj.getInitialAmount());
        pst.setDouble(4, obj.getTargetAmount());
        pst.setString(5, obj.getPriorite());
        pst.setInt(6, obj.getId());
        pst.executeUpdate();
    }

    public void updateForUser(Objectif obj, int userId) throws Exception {
        if (!isOwnedByUser(obj.getId(), userId)) {
            throw new SQLException("Vous ne pouvez pas modifier cet objectif.");
        }
        update(obj);
    }

    public void delete(int id) throws Exception {
        String sqlUnlink = "UPDATE investissement SET objectif_id = NULL WHERE objectif_id = ?";
        PreparedStatement pst1 = cnx().prepareStatement(sqlUnlink);
        pst1.setInt(1, id);
        pst1.executeUpdate();

        String sqlDelete = "DELETE FROM objectif WHERE id = ?";
        PreparedStatement pst2 = cnx().prepareStatement(sqlDelete);
        pst2.setInt(1, id);
        pst2.executeUpdate();
    }

    public void deleteForUser(int id, int userId) throws Exception {
        if (!isOwnedByUser(id, userId)) {
            throw new SQLException("Vous ne pouvez pas supprimer cet objectif.");
        }

        unlinkAllForUser(id, userId);

        String sqlCount = "SELECT COUNT(*) FROM investissement WHERE objectif_id = ?";
        PreparedStatement countPst = cnx().prepareStatement(sqlCount);
        countPst.setInt(1, id);
        ResultSet rs = countPst.executeQuery();
        int remaining = rs.next() ? rs.getInt(1) : 0;
        if (remaining == 0) {
            String sqlDelete = "DELETE FROM objectif WHERE id = ?";
            PreparedStatement pst2 = cnx().prepareStatement(sqlDelete);
            pst2.setInt(1, id);
            pst2.executeUpdate();
        }
    }

    private boolean isOwnedByUser(int objectifId, int userId) throws SQLException {
        String sql = "SELECT 1 FROM investissement WHERE objectif_id = ? AND user_id = ? LIMIT 1";
        PreparedStatement pst = cnx().prepareStatement(sql);
        pst.setInt(1, objectifId);
        pst.setInt(2, userId);
        ResultSet rs = pst.executeQuery();
        return rs.next();
    }
}
