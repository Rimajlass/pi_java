package pi.services;

import pi.entities.Expense;
import pi.entities.Transaction;
import pi.entities.User;
import pi.interfaces.ICrud;
import pi.tools.MyDatabase;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TransactionService implements ICrud<Transaction> {
    private static final String INSERT_SQL =
            "INSERT INTO `transaction` (user_id, expense_id, type, montant, date, description, module_source) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_SQL =
            "UPDATE `transaction` SET user_id = ?, expense_id = ?, type = ?, montant = ?, date = ?, description = ?, module_source = ? WHERE id = ?";
    private static final String DELETE_SQL = "DELETE FROM `transaction` WHERE id = ?";
    private static final String SELECT_BY_ID_SQL = "SELECT * FROM `transaction` WHERE id = ?";
    private static final String SELECT_ALL_SQL = "SELECT * FROM `transaction`";

    private final Connection connection;

    public TransactionService() {
        this.connection = MyDatabase.getInstance().getCnx();
    }

    @Override
    public void add(Transaction transaction) throws SQLException {
        validateTransaction(transaction);

        try (PreparedStatement ps = this.connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            fillStatement(ps, transaction);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    transaction.setId(rs.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(Transaction transaction) throws SQLException {
        if (transaction == null || transaction.getId() <= 0) {
            throw new IllegalArgumentException("Transaction id is required for update");
        }
        validateTransaction(transaction);

        try (PreparedStatement ps = this.connection.prepareStatement(UPDATE_SQL)) {
            fillStatement(ps, transaction);
            ps.setInt(8, transaction.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = this.connection.prepareStatement(DELETE_SQL)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public Transaction getById(int id) throws SQLException {
        try (PreparedStatement ps = this.connection.prepareStatement(SELECT_BY_ID_SQL)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapTransaction(rs);
                }
            }
        }

        return null;
    }

    @Override
    public List<Transaction> getAll() throws SQLException {
        List<Transaction> transactions = new ArrayList<>();

        try (Statement st = this.connection.createStatement();
             ResultSet rs = st.executeQuery(SELECT_ALL_SQL)) {
            while (rs.next()) {
                transactions.add(mapTransaction(rs));
            }
        }

        return transactions;
    }

    private void fillStatement(PreparedStatement ps, Transaction transaction) throws SQLException {
        ps.setInt(1, transaction.getUser().getId());
        if (transaction.getExpense() != null && transaction.getExpense().getId() > 0) {
            ps.setInt(2, transaction.getExpense().getId());
        } else {
            ps.setNull(2, Types.INTEGER);
        }
        ps.setString(3, transaction.getType());
        ps.setDouble(4, transaction.getMontant());
        ps.setDate(5, toSqlDate(transaction.getDate()));
        ps.setString(6, transaction.getDescription());
        ps.setString(7, transaction.getModuleSource());
    }

    private Transaction mapTransaction(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("user_id"));

        int expenseId = rs.getInt("expense_id");
        Expense expense = null;
        if (!rs.wasNull()) {
            expense = new Expense(null, null, 0, null, null, null);
            expense.setId(expenseId);
        }

        Date transactionDate = rs.getDate("date");

        return new Transaction(
                rs.getInt("id"),
                user,
                expense,
                rs.getString("type"),
                rs.getDouble("montant"),
                transactionDate != null ? transactionDate.toLocalDate() : null,
                rs.getString("description"),
                rs.getString("module_source")
        );
    }

    private void validateTransaction(Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (transaction.getUser() == null || transaction.getUser().getId() <= 0) {
            throw new IllegalArgumentException("Transaction must reference a valid user id");
        }
        if (transaction.getType() == null || transaction.getType().isBlank()) {
            throw new IllegalArgumentException("Transaction type is required");
        }
    }

    private Date toSqlDate(LocalDate value) {
        return value != null ? Date.valueOf(value) : null;
    }
}
