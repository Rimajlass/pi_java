package pi.savings.repository;

import pi.tools.MyDatabase;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SavingsTransactionRepository {

    private final Connection connection;

    public SavingsTransactionRepository() {
        this.connection = MyDatabase.getInstance().getCnx();
    }

    public void insertDeposit(
            int userId,
            BigDecimal amount,
            String description,
            Connection transactionalConnection
    ) throws SQLException {
        String sql = """
                INSERT INTO `transaction` (type, montant, `date`, description, module_source, user_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = transactionalConnection.prepareStatement(sql)) {
            statement.setString(1, "EPARGNE");
            statement.setBigDecimal(2, amount);
            statement.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(4, description);
            statement.setString(5, "SAVINGS");
            statement.setInt(6, userId);
            statement.executeUpdate();
        }
    }

    public void insertGoalContribution(
            int userId,
            BigDecimal amount,
            String description,
            Connection transactionalConnection
    ) throws SQLException {
        String sql = """
                INSERT INTO `transaction` (type, montant, `date`, description, module_source, user_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = transactionalConnection.prepareStatement(sql)) {
            statement.setString(1, "GOAL_CONTRIBUTION");
            statement.setBigDecimal(2, amount);
            statement.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(4, description);
            statement.setString(5, "SAVINGS");
            statement.setInt(6, userId);
            statement.executeUpdate();
        }
    }

    public List<TransactionRow> findSavingsHistoryByUserId(int userId) throws SQLException {
        String sql = """
                SELECT id, type, montant, `date`, description, module_source, user_id
                FROM `transaction`
                WHERE user_id = ? AND module_source = ?
                ORDER BY `date` DESC, id DESC
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setString(2, "SAVINGS");
            try (ResultSet resultSet = statement.executeQuery()) {
                List<TransactionRow> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(new TransactionRow(
                            resultSet.getInt("id"),
                            resultSet.getString("type"),
                            resultSet.getTimestamp("date").toLocalDateTime(),
                            resultSet.getBigDecimal("montant"),
                            resultSet.getString("description"),
                            resultSet.getString("module_source"),
                            resultSet.getInt("user_id")
                    ));
                }
                return rows;
            }
        }
    }

    public record TransactionRow(
            int id,
            String type,
            LocalDateTime date,
            BigDecimal amount,
            String description,
            String moduleSource,
            int userId
    ) {
    }
}
