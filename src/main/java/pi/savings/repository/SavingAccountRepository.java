package pi.savings.repository;

import pi.entities.SavingAccount;
import pi.tools.MyDatabase;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Optional;

public class SavingAccountRepository {

    private final Connection connection;

    public SavingAccountRepository() {
        this.connection = MyDatabase.getInstance().getCnx();
    }

    public Optional<SavingAccount> findLatestByUserId(int userId) throws SQLException {
        String sql = """
                SELECT id, user_id, sold, date_creation, taux_interet
                FROM saving_account
                WHERE user_id = ?
                ORDER BY id DESC
                LIMIT 1
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    public SavingAccount createDefaultAccount(int userId) throws SQLException {
        String sql = """
                INSERT INTO saving_account (user_id, sold, date_creation, taux_interet)
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, userId);
            statement.setBigDecimal(2, BigDecimal.ZERO);
            statement.setDate(3, Date.valueOf(LocalDate.now()));
            statement.setBigDecimal(4, BigDecimal.ZERO);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    return new SavingAccount(id, userId, 0.0, Date.valueOf(LocalDate.now()), 0.0);
                }
            }
        }

        return findLatestByUserId(userId)
                .orElseThrow(() -> new SQLException("Compte epargne cree mais introuvable apres insertion."));
    }

    public void addToBalance(int accountId, BigDecimal amount) throws SQLException {
        String sql = "UPDATE saving_account SET sold = sold + ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, amount);
            statement.setInt(2, accountId);
            statement.executeUpdate();
        }
    }

    public void subtractFromBalance(int accountId, BigDecimal amount) throws SQLException {
        String sql = "UPDATE saving_account SET sold = sold - ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, amount);
            statement.setInt(2, accountId);
            statement.executeUpdate();
        }
    }

    public void updateInterestRate(int accountId, BigDecimal rate) throws SQLException {
        String sql = "UPDATE saving_account SET taux_interet = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, rate);
            statement.setInt(2, accountId);
            statement.executeUpdate();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    private SavingAccount map(ResultSet resultSet) throws SQLException {
        SavingAccount savingAccount = new SavingAccount();
        savingAccount.setId(resultSet.getInt("id"));
        savingAccount.setUserId(resultSet.getInt("user_id"));
        savingAccount.setSold(resultSet.getDouble("sold"));
        savingAccount.setDateCreation(resultSet.getDate("date_creation"));
        savingAccount.setTauxInteret(resultSet.getDouble("taux_interet"));
        return savingAccount;
    }
}
