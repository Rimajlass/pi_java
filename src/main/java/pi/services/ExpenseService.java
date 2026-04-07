package pi.services;

import pi.entities.Expense;
import pi.entities.Revenue;
import pi.entities.User;
import pi.interfaces.IExpenseService;
import pi.tools.MyDatabase;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ExpenseService implements IExpenseService {
    private static final String INSERT_SQL =
            "INSERT INTO expense (revenue_id, user_id, amount, category, expense_date, description) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_SQL =
            "UPDATE expense SET revenue_id = ?, user_id = ?, amount = ?, category = ?, expense_date = ?, description = ? WHERE id = ?";
    private static final String DELETE_SQL = "DELETE FROM expense WHERE id = ?";
    private static final String SELECT_BY_ID_SQL = "SELECT * FROM expense WHERE id = ?";
    private static final String SELECT_ALL_SQL = "SELECT * FROM expense";

    private final Connection connection;

    public ExpenseService() {
        this.connection = MyDatabase.getInstance().getCnx();
    }

    @Override
    public void add(Expense expense) throws SQLException {
        validateExpense(expense);

        try (PreparedStatement ps = this.connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            fillStatement(ps, expense);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    expense.setId(rs.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(Expense expense) throws SQLException {
        if (expense == null || expense.getId() <= 0) {
            throw new IllegalArgumentException("Expense id is required for update");
        }
        validateExpense(expense);

        try (PreparedStatement ps = this.connection.prepareStatement(UPDATE_SQL)) {
            fillStatement(ps, expense);
            ps.setInt(7, expense.getId());
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
    public Expense getById(int id) throws SQLException {
        try (PreparedStatement ps = this.connection.prepareStatement(SELECT_BY_ID_SQL)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapExpense(rs);
                }
            }
        }

        return null;
    }

    @Override
    public List<Expense> getAll() throws SQLException {
        List<Expense> expenses = new ArrayList<>();

        try (Statement st = this.connection.createStatement();
             ResultSet rs = st.executeQuery(SELECT_ALL_SQL)) {
            while (rs.next()) {
                expenses.add(mapExpense(rs));
            }
        }

        return expenses;
    }

    private void fillStatement(PreparedStatement ps, Expense expense) throws SQLException {
        ps.setInt(1, expense.getRevenue().getId());
        ps.setInt(2, expense.getUser().getId());
        ps.setDouble(3, expense.getAmount());
        ps.setString(4, expense.getCategory());
        ps.setDate(5, expense.getExpenseDate() != null ? Date.valueOf(expense.getExpenseDate()) : null);
        ps.setString(6, expense.getDescription());
    }

    private Expense mapExpense(ResultSet rs) throws SQLException {
        Revenue revenue = new Revenue();
        revenue.setId(rs.getInt("revenue_id"));

        User user = new User();
        user.setId(rs.getInt("user_id"));

        Date expenseDate = rs.getDate("expense_date");

        return new Expense(
                rs.getInt("id"),
                revenue,
                user,
                rs.getDouble("amount"),
                rs.getString("category"),
                expenseDate != null ? expenseDate.toLocalDate() : null,
                rs.getString("description")
        );
    }

    private void validateExpense(Expense expense) {
        if (expense == null) {
            throw new IllegalArgumentException("Expense cannot be null");
        }
        if (expense.getRevenue() == null || expense.getRevenue().getId() <= 0) {
            throw new IllegalArgumentException("Expense must reference a valid revenue id");
        }
        if (expense.getUser() == null || expense.getUser().getId() <= 0) {
            throw new IllegalArgumentException("Expense must reference a valid user id");
        }
    }
}
