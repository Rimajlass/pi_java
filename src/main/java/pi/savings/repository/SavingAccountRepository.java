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
import java.util.ArrayList;
import java.util.List;
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

    public Optional<SavingAccountDetails> findLatestDetailsByUserId(int userId) throws SQLException {
        String sql = """
                SELECT sa.id, sa.user_id, sa.sold, sa.date_creation, sa.taux_interet,
                       u.nom AS user_name, u.email AS user_email
                FROM saving_account sa
                LEFT JOIN `user` u ON u.id = sa.user_id
                WHERE sa.user_id = ?
                ORDER BY sa.id DESC
                LIMIT 1
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapDetails(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    public List<SavingAccountDetails> findAllDetailedAccounts() throws SQLException {
        String sql = """
                SELECT sa.id, sa.user_id, sa.sold, sa.date_creation, sa.taux_interet,
                       u.nom AS user_name, u.email AS user_email
                FROM saving_account sa
                LEFT JOIN `user` u ON u.id = sa.user_id
                ORDER BY sa.sold DESC, sa.id DESC
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<SavingAccountDetails> rows = new ArrayList<>();
            while (resultSet.next()) {
                rows.add(mapDetails(resultSet));
            }
            return rows;
        }
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

    private SavingAccountDetails mapDetails(ResultSet resultSet) throws SQLException {
        return new SavingAccountDetails(
                resultSet.getInt("id"),
                resultSet.getInt("user_id"),
                resultSet.getString("user_name"),
                resultSet.getString("user_email"),
                resultSet.getBigDecimal("sold").setScale(2, java.math.RoundingMode.HALF_UP),
                resultSet.getDate("date_creation").toLocalDate(),
                resultSet.getBigDecimal("taux_interet").setScale(2, java.math.RoundingMode.HALF_UP)
        );
    }

    public record SavingAccountDetails(
            int accountId,
            int userId,
            String userName,
            String userEmail,
            BigDecimal balance,
            LocalDate createdOn,
            BigDecimal interestRate
    ) {
    }
}
