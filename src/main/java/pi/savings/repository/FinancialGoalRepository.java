package pi.savings.repository;

import pi.entities.FinancialGoal;
import pi.tools.MyDatabase;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FinancialGoalRepository {

    private final Connection connection;

    public FinancialGoalRepository() {
        this.connection = MyDatabase.getInstance().getCnx();
    }

    public List<FinancialGoal> findBySavingAccountId(int savingAccountId) throws SQLException {
        String sql = """
                SELECT id, saving_account_id, nom, montant_cible, montant_actuel, date_limite, priorite
                FROM financial_goal
                WHERE saving_account_id = ?
                ORDER BY priorite DESC, date_limite ASC, id DESC
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, savingAccountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<FinancialGoal> goals = new ArrayList<>();
                while (resultSet.next()) {
                    FinancialGoal goal = new FinancialGoal();
                    goal.setId(resultSet.getInt("id"));
                    goal.setSavingAccountId(resultSet.getInt("saving_account_id"));
                    goal.setNom(resultSet.getString("nom"));
                    goal.setMontantCible(resultSet.getDouble("montant_cible"));
                    goal.setMontantActuel(resultSet.getDouble("montant_actuel"));
                    goal.setDateLimite(resultSet.getDate("date_limite"));
                    goal.setPriorite(resultSet.getInt("priorite"));
                    goals.add(goal);
                }
                return goals;
            }
        }
    }

    public Optional<FinancialGoal> findByIdAndSavingAccountId(int goalId, int savingAccountId) throws SQLException {
        String sql = """
                SELECT id, saving_account_id, nom, montant_cible, montant_actuel, date_limite, priorite
                FROM financial_goal
                WHERE id = ? AND saving_account_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, goalId);
            statement.setInt(2, savingAccountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    public FinancialGoal insert(FinancialGoal goal) throws SQLException {
        String sql = """
                INSERT INTO financial_goal (saving_account_id, nom, montant_cible, montant_actuel, date_limite, priorite)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, goal.getSavingAccountId());
            statement.setString(2, goal.getNom());
            statement.setDouble(3, goal.getMontantCible());
            statement.setDouble(4, goal.getMontantActuel());
            statement.setDate(5, goal.getDateLimite());
            statement.setInt(6, goal.getPriorite());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    goal.setId(keys.getInt(1));
                }
            }
        }
        return goal;
    }

    public void update(FinancialGoal goal) throws SQLException {
        String sql = """
                UPDATE financial_goal
                SET nom = ?, montant_cible = ?, montant_actuel = ?, date_limite = ?, priorite = ?
                WHERE id = ? AND saving_account_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, goal.getNom());
            statement.setDouble(2, goal.getMontantCible());
            statement.setDouble(3, goal.getMontantActuel());
            statement.setDate(4, goal.getDateLimite());
            statement.setInt(5, goal.getPriorite());
            statement.setInt(6, goal.getId());
            statement.setInt(7, goal.getSavingAccountId());
            statement.executeUpdate();
        }
    }

    public void delete(int goalId, int savingAccountId) throws SQLException {
        String sql = "DELETE FROM financial_goal WHERE id = ? AND saving_account_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, goalId);
            statement.setInt(2, savingAccountId);
            statement.executeUpdate();
        }
    }

    public void addContribution(int goalId, int savingAccountId, double amount, Connection transactionalConnection) throws SQLException {
        String sql = """
                UPDATE financial_goal
                SET montant_actuel = montant_actuel + ?
                WHERE id = ? AND saving_account_id = ?
                """;

        try (PreparedStatement statement = transactionalConnection.prepareStatement(sql)) {
            statement.setDouble(1, amount);
            statement.setInt(2, goalId);
            statement.setInt(3, savingAccountId);
            statement.executeUpdate();
        }
    }

    public List<FinancialGoalDetails> findDetailedByUserId(int userId) throws SQLException {
        String sql = """
                SELECT fg.id, fg.saving_account_id, fg.nom, fg.montant_cible, fg.montant_actuel, fg.date_limite, fg.priorite,
                       sa.user_id, sa.sold, sa.taux_interet,
                       u.nom AS user_name, u.email AS user_email
                FROM financial_goal fg
                INNER JOIN saving_account sa ON sa.id = fg.saving_account_id
                LEFT JOIN `user` u ON u.id = sa.user_id
                WHERE sa.user_id = ?
                ORDER BY fg.priorite DESC, fg.date_limite ASC, fg.id DESC
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<FinancialGoalDetails> goals = new ArrayList<>();
                while (resultSet.next()) {
                    goals.add(mapDetails(resultSet));
                }
                return goals;
            }
        }
    }

    public List<FinancialGoalDetails> findAllDetailedGoals() throws SQLException {
        String sql = """
                SELECT fg.id, fg.saving_account_id, fg.nom, fg.montant_cible, fg.montant_actuel, fg.date_limite, fg.priorite,
                       sa.user_id, sa.sold, sa.taux_interet,
                       u.nom AS user_name, u.email AS user_email
                FROM financial_goal fg
                INNER JOIN saving_account sa ON sa.id = fg.saving_account_id
                LEFT JOIN `user` u ON u.id = sa.user_id
                ORDER BY fg.priorite DESC, fg.date_limite ASC, fg.id DESC
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<FinancialGoalDetails> goals = new ArrayList<>();
            while (resultSet.next()) {
                goals.add(mapDetails(resultSet));
            }
            return goals;
        }
    }

    private FinancialGoal map(ResultSet resultSet) throws SQLException {
        FinancialGoal goal = new FinancialGoal();
        goal.setId(resultSet.getInt("id"));
        goal.setSavingAccountId(resultSet.getInt("saving_account_id"));
        goal.setNom(resultSet.getString("nom"));
        goal.setMontantCible(resultSet.getDouble("montant_cible"));
        goal.setMontantActuel(resultSet.getDouble("montant_actuel"));
        goal.setDateLimite(resultSet.getDate("date_limite"));
        goal.setPriorite(resultSet.getInt("priorite"));
        return goal;
    }

    private FinancialGoalDetails mapDetails(ResultSet resultSet) throws SQLException {
        Date deadline = resultSet.getDate("date_limite");
        return new FinancialGoalDetails(
                resultSet.getInt("id"),
                resultSet.getInt("saving_account_id"),
                resultSet.getInt("user_id"),
                resultSet.getString("nom"),
                resultSet.getBigDecimal("montant_cible").setScale(2, java.math.RoundingMode.HALF_UP),
                resultSet.getBigDecimal("montant_actuel").setScale(2, java.math.RoundingMode.HALF_UP),
                deadline == null ? null : deadline.toLocalDate(),
                resultSet.getInt("priorite"),
                resultSet.getString("user_name"),
                resultSet.getString("user_email"),
                resultSet.getBigDecimal("sold").setScale(2, java.math.RoundingMode.HALF_UP),
                resultSet.getBigDecimal("taux_interet").setScale(2, java.math.RoundingMode.HALF_UP)
        );
    }

    public record FinancialGoalDetails(
            int goalId,
            int savingAccountId,
            int userId,
            String goalName,
            java.math.BigDecimal targetAmount,
            java.math.BigDecimal currentAmount,
            LocalDate deadline,
            int priority,
            String userName,
            String userEmail,
            java.math.BigDecimal accountBalance,
            java.math.BigDecimal interestRate
    ) {
    }
}
