package pi.savings.repository;

import pi.entities.FinancialGoal;
import pi.tools.MyDatabase;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
}
