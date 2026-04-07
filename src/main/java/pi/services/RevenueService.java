package pi.services;

import pi.entities.Revenue;
import pi.entities.User;
import pi.interfaces.IRevenueService;
import pi.tools.MyDatabase;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RevenueService implements IRevenueService {
    private static final String INSERT_SQL =
            "INSERT INTO revenue (user_id, amount, type, received_at, description, created_at) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_SQL =
            "UPDATE revenue SET user_id = ?, amount = ?, type = ?, received_at = ?, description = ?, created_at = ? WHERE id = ?";
    private static final String DELETE_SQL = "DELETE FROM revenue WHERE id = ?";
    private static final String SELECT_BY_ID_SQL = "SELECT * FROM revenue WHERE id = ?";
    private static final String SELECT_ALL_SQL = "SELECT * FROM revenue";

    private final Connection connection;

    public RevenueService() {
        this.connection = MyDatabase.getInstance().getCnx();
    }

    @Override
    public void add(Revenue revenue) throws SQLException {
        validateRevenue(revenue);

        try (PreparedStatement ps = this.connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            fillStatement(ps, revenue);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    revenue.setId(rs.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(Revenue revenue) throws SQLException {
        if (revenue == null || revenue.getId() <= 0) {
            throw new IllegalArgumentException("Revenue id is required for update");
        }
        validateRevenue(revenue);

        try (PreparedStatement ps = this.connection.prepareStatement(UPDATE_SQL)) {
            fillStatement(ps, revenue);
            ps.setInt(7, revenue.getId());
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
    public Revenue getById(int id) throws SQLException {
        try (PreparedStatement ps = this.connection.prepareStatement(SELECT_BY_ID_SQL)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRevenue(rs);
                }
            }
        }

        return null;
    }

    @Override
    public List<Revenue> getAll() throws SQLException {
        List<Revenue> revenues = new ArrayList<>();

        try (Statement st = this.connection.createStatement();
             ResultSet rs = st.executeQuery(SELECT_ALL_SQL)) {
            while (rs.next()) {
                revenues.add(mapRevenue(rs));
            }
        }

        return revenues;
    }

    private void fillStatement(PreparedStatement ps, Revenue revenue) throws SQLException {
        ps.setInt(1, revenue.getUser().getId());
        ps.setDouble(2, revenue.getAmount());
        ps.setString(3, revenue.getType());
        ps.setDate(4, toSqlDate(revenue.getReceivedAt()));
        ps.setString(5, revenue.getDescription());
        ps.setTimestamp(6, toSqlTimestamp(revenue.getCreatedAt()));
    }

    private Revenue mapRevenue(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("user_id"));

        Date receivedAt = rs.getDate("received_at");
        Timestamp createdAt = rs.getTimestamp("created_at");

        return new Revenue(
                rs.getInt("id"),
                user,
                rs.getDouble("amount"),
                rs.getString("type"),
                receivedAt != null ? receivedAt.toLocalDate() : null,
                rs.getString("description"),
                createdAt != null ? createdAt.toLocalDateTime() : null
        );
    }

    private void validateRevenue(Revenue revenue) {
        if (revenue == null) {
            throw new IllegalArgumentException("Revenue cannot be null");
        }
        if (revenue.getUser() == null || revenue.getUser().getId() <= 0) {
            throw new IllegalArgumentException("Revenue must reference a valid user id");
        }
    }

    private Date toSqlDate(LocalDate value) {
        return value != null ? Date.valueOf(value) : null;
    }

    private Timestamp toSqlTimestamp(LocalDateTime value) {
        return value != null ? Timestamp.valueOf(value) : null;
    }
}
