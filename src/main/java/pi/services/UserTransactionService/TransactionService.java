package pi.services.UserTransactionService;

import pi.entities.Transaction;
import pi.entities.User;
import pi.tools.MyDatabase;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TransactionService {

    private static final String TX_SELECT_BASE = """
            SELECT t.*, u.id AS u_id, u.nom AS u_nom, u.email AS u_email, u.password AS u_password,
                   u.roles AS u_roles, u.date_inscription AS u_date_inscription, u.solde_total AS u_solde_total,
                   u.image AS u_image, u.face_id_credential_id AS u_face_id_credential_id,
                   u.face_id_enabled AS u_face_id_enabled, u.face_plus_token AS u_face_plus_token,
                   u.face_plus_enabled AS u_face_plus_enabled, u.email_verified AS u_email_verified,
                   u.email_verification_token AS u_email_verification_token,
                   u.email_verified_at AS u_email_verified_at, u.is_blocked AS u_is_blocked,
                   u.blocked_reason AS u_blocked_reason, u.blocked_at AS u_blocked_at,
                   u.geo_country_code AS u_geo_country_code, u.geo_country_name AS u_geo_country_name,
                   u.geo_region_name AS u_geo_region_name, u.geo_city_name AS u_geo_city_name,
                   u.geo_detected_ip AS u_geo_detected_ip, u.geo_vpn_suspected AS u_geo_vpn_suspected,
                   u.geo_last_checked_at AS u_geo_last_checked_at
            FROM transaction t
            JOIN `user` u ON u.id = t.user_id
            """;

    private final Connection cnx;
    private final UserService userService;

    public TransactionService() {
        this.cnx = MyDatabase.getInstance().getCnx();
        this.userService = new UserService();
    }

    public List<Transaction> findByUserId(int userId) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = TX_SELECT_BASE + """
                WHERE t.user_id = ?
                ORDER BY t.date DESC, t.id DESC
                """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapTransaction(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du chargement des transactions : " + e.getMessage(), e);
        }

        return transactions;
    }

    /**
     * Liste admin avec filtres (même logique que l’écran Symfony).
     */
    public List<Transaction> findAllForAdmin(
            String typeFilter,
            LocalDate from,
            LocalDate to,
            String userNomLike
    ) {
        StringBuilder sql = new StringBuilder(TX_SELECT_BASE).append(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        appendAdminFilters(sql, params, typeFilter, from, to, userNomLike);
        sql.append(" ORDER BY t.date DESC, t.id DESC ");

        List<Transaction> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapTransaction(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du chargement des transactions : " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Comptages par type sur le périmètre filtré.
     */
    public Map<String, Integer> countGroupedByType(
            String typeFilter,
            LocalDate from,
            LocalDate to,
            String userNomLike
    ) {
        StringBuilder sql = new StringBuilder(
                "SELECT t.type AS txtype, COUNT(t.id) AS cnt FROM transaction t JOIN `user` u ON u.id = t.user_id WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        appendAdminFilters(sql, params, typeFilter, from, to, userNomLike);
        sql.append(" GROUP BY t.type ");

        Map<String, Integer> out = new HashMap<>();
        out.put("EXPENSE", 0);
        out.put("SAVING", 0);
        out.put("INVESTMENT", 0);

        try (PreparedStatement ps = cnx.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String t = rs.getString("txtype");
                    if (t != null && out.containsKey(t)) {
                        out.put(t, rs.getInt("cnt"));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur comptage transactions : " + e.getMessage(), e);
        }
        return out;
    }

    public void insertTransactionForUser(
            int userId,
            String type,
            double montant,
            LocalDate date,
            String description,
            String moduleSource
    ) {
        if (montant <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif.");
        }
        String t = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        if (!List.of("EXPENSE", "SAVING", "INVESTMENT").contains(t)) {
            throw new IllegalArgumentException("Type de transaction invalide.");
        }

        User user = userService.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Utilisateur introuvable.");
        }

        double newSolde = user.getSoldeTotal() + deltaForBalance(t, montant);

        String sql = """
                INSERT INTO transaction (user_id, expense_id, type, montant, date, description, module_source)
                VALUES (?, NULL, ?, ?, ?, ?, ?)
                """;

        boolean prevAuto = true;
        try {
            prevAuto = cnx.getAutoCommit();
            cnx.setAutoCommit(false);
            try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, userId);
                ps.setString(2, t);
                ps.setDouble(3, montant);
                ps.setDate(4, Date.valueOf(date));
                ps.setString(5, description == null || description.isBlank() ? null : description.trim());
                ps.setString(6, moduleSource == null || moduleSource.isBlank() ? null : moduleSource.trim());
                ps.executeUpdate();
            }
            userService.updateSoldeTotal(userId, newSolde);
            cnx.commit();
        } catch (Exception e) {
            try {
                cnx.rollback();
            } catch (SQLException ex) {
                e.addSuppressed(ex);
            }
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(e);
        } finally {
            try {
                cnx.setAutoCommit(prevAuto);
            } catch (SQLException ignored) {
            }
        }
    }

    private static double deltaForBalance(String typeUpper, double montant) {
        return switch (typeUpper) {
            case "SAVING" -> montant;
            case "EXPENSE", "INVESTMENT" -> -montant;
            default -> 0;
        };
    }

    private static void appendAdminFilters(
            StringBuilder sql,
            List<Object> params,
            String typeFilter,
            LocalDate from,
            LocalDate to,
            String userNomLike
    ) {
        if (typeFilter != null && !typeFilter.isBlank()) {
            sql.append(" AND t.type = ? ");
            params.add(typeFilter.trim().toUpperCase(Locale.ROOT));
        }
        if (from != null) {
            sql.append(" AND t.date >= ? ");
            params.add(Date.valueOf(from));
        }
        if (to != null) {
            sql.append(" AND t.date <= ? ");
            params.add(Date.valueOf(to));
        }
        if (userNomLike != null && !userNomLike.isBlank()) {
            sql.append(" AND LOWER(u.nom) LIKE ? ");
            params.add("%" + userNomLike.trim().toLowerCase(Locale.ROOT) + "%");
        }
    }

    private static void bindParams(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    public UserTransactionSummary buildHistorySummary(int userId) {
        List<Transaction> transactions = findByUserId(userId);
        double totalSavings = 0;
        double totalExpenses = 0;

        for (Transaction transaction : transactions) {
            if ("SAVING".equalsIgnoreCase(transaction.getType())) {
                totalSavings += transaction.getMontant();
            } else if ("EXPENSE".equalsIgnoreCase(transaction.getType())) {
                totalExpenses += transaction.getMontant();
            }
        }

        return new UserTransactionSummary(
                transactions.size(),
                totalSavings,
                totalExpenses,
                totalSavings - totalExpenses,
                transactions
        );
    }

    public UserDashboardStats buildUserDashboard(int userId) {
        User user = userService.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Utilisateur introuvable.");
        }

        List<Transaction> transactions = findByUserId(userId);
        int savingCount = 0;
        int expenseCount = 0;
        int investmentCount = 0;
        double totalRevenues = 0;
        double totalExpenses = 0;
        LocalDate lastActivityDate = transactions.isEmpty() ? null : transactions.get(0).getDate();

        Map<LocalDate, double[]> monthlyMap = initMonthlyMap();

        for (Transaction transaction : transactions) {
            String type = transaction.getType() == null ? "" : transaction.getType().toUpperCase(Locale.ROOT);

            switch (type) {
                case "SAVING" -> {
                    savingCount++;
                    totalRevenues += transaction.getMontant();
                }
                case "EXPENSE" -> {
                    expenseCount++;
                    totalExpenses += transaction.getMontant();
                }
                case "INVESTMENT" -> investmentCount++;
                default -> {
                }
            }

            LocalDate transactionMonth = transaction.getDate() != null ? transaction.getDate().withDayOfMonth(1) : null;
            if (transactionMonth != null && monthlyMap.containsKey(transactionMonth)) {
                double[] values = monthlyMap.get(transactionMonth);
                if ("SAVING".equals(type)) {
                    values[0] += transaction.getMontant();
                } else if ("EXPENSE".equals(type)) {
                    values[1] += transaction.getMontant();
                }
            }
        }

        double netCashFlow = totalRevenues - totalExpenses;
        double expenseRatio = totalRevenues > 0 ? (totalExpenses / totalRevenues) * 100 : 0;
        double savingsRate = totalRevenues > 0 ? ((totalRevenues - totalExpenses) / totalRevenues) * 100 : 0;

        String financialHealth = "Good";
        if (expenseRatio > 85) {
            financialHealth = "Critical";
        } else if (expenseRatio > 65) {
            financialHealth = "Moderate";
        }

        List<String> insights = new ArrayList<>();
        insights.add(String.format(Locale.US, "Expense ratio: %.1f%% of revenues.", expenseRatio));
        insights.add(String.format(Locale.US, "Savings rate: %.1f%%.", savingsRate));
        insights.add(netCashFlow >= 0
                ? "Positive net cash flow. Keep the current rhythm."
                : "Negative net cash flow. Consider reducing non-essential expenses.");
        insights.add(transactions.size() >= 20
                ? "High activity user profile (20+ transactions)."
                : "Low/medium activity user profile.");

        List<String> monthLabels = new ArrayList<>();
        List<Double> monthlyRevenue = new ArrayList<>();
        List<Double> monthlyExpense = new ArrayList<>();

        for (Map.Entry<LocalDate, double[]> entry : monthlyMap.entrySet()) {
            LocalDate month = entry.getKey();
            monthLabels.add(month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + month.getYear());
            monthlyRevenue.add(entry.getValue()[0]);
            monthlyExpense.add(entry.getValue()[1]);
        }

        return new UserDashboardStats(
                user,
                transactions,
                transactions.size(),
                totalRevenues,
                totalExpenses,
                savingCount,
                expenseCount,
                investmentCount,
                lastActivityDate,
                netCashFlow,
                expenseRatio,
                savingsRate,
                financialHealth,
                monthLabels,
                monthlyRevenue,
                monthlyExpense,
                List.of(savingCount, expenseCount, investmentCount),
                insights
        );
    }

    private Map<LocalDate, double[]> initMonthlyMap() {
        Map<LocalDate, double[]> monthlyMap = new LinkedHashMap<>();
        LocalDate cursor = LocalDate.now().withDayOfMonth(1).minusMonths(5);

        for (int i = 0; i < 6; i++) {
            monthlyMap.put(cursor, new double[]{0, 0});
            cursor = cursor.plusMonths(1);
        }

        return monthlyMap;
    }

    private Transaction mapTransaction(ResultSet rs) throws SQLException {
        Date transactionDate = rs.getDate("date");
        User user = mapEmbeddedUser(rs);

        return new Transaction(
                rs.getInt("id"),
                user,
                null,
                rs.getString("type"),
                rs.getDouble("montant"),
                transactionDate != null ? transactionDate.toLocalDate() : null,
                rs.getString("description"),
                rs.getString("module_source")
        );
    }

    private User mapEmbeddedUser(ResultSet rs) throws SQLException {
        Date inscriptionDate = rs.getDate("u_date_inscription");

        return new User(
                rs.getInt("u_id"),
                rs.getString("u_nom"),
                rs.getString("u_email"),
                rs.getString("u_password"),
                rs.getString("u_roles"),
                inscriptionDate != null ? inscriptionDate.toLocalDate() : null,
                rs.getDouble("u_solde_total"),
                rs.getString("u_image"),
                rs.getString("u_face_id_credential_id"),
                rs.getBoolean("u_face_id_enabled"),
                rs.getString("u_face_plus_token"),
                rs.getBoolean("u_face_plus_enabled"),
                rs.getBoolean("u_email_verified"),
                rs.getString("u_email_verification_token"),
                rs.getTimestamp("u_email_verified_at") != null ? rs.getTimestamp("u_email_verified_at").toLocalDateTime() : null,
                rs.getBoolean("u_is_blocked"),
                rs.getString("u_blocked_reason"),
                rs.getTimestamp("u_blocked_at") != null ? rs.getTimestamp("u_blocked_at").toLocalDateTime() : null,
                rs.getString("u_geo_country_code"),
                rs.getString("u_geo_country_name"),
                rs.getString("u_geo_region_name"),
                rs.getString("u_geo_city_name"),
                rs.getString("u_geo_detected_ip"),
                rs.getBoolean("u_geo_vpn_suspected"),
                rs.getTimestamp("u_geo_last_checked_at") != null ? rs.getTimestamp("u_geo_last_checked_at").toLocalDateTime() : null
        );
    }

    public record UserTransactionSummary(
            int count,
            double totalSavings,
            double totalExpenses,
            double net,
            List<Transaction> transactions
    ) {
    }

    public record UserDashboardStats(
            User user,
            List<Transaction> transactions,
            int totalTransactions,
            double totalRevenues,
            double totalExpenses,
            int savingCount,
            int expenseTxCount,
            int investmentCount,
            LocalDate lastActivityDate,
            double netCashFlow,
            double expenseRatio,
            double savingsRate,
            String financialHealth,
            List<String> monthLabels,
            List<Double> monthlyRevenue,
            List<Double> monthlyExpense,
            List<Integer> distribution,
            List<String> insights
    ) {
    }
}
