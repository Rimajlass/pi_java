package pi.services;

import pi.entities.User;
import pi.interfaces.ICrud;
import pi.tools.MyDatabase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserService implements ICrud<User> {
    private static final String DELETE_SQL = "DELETE FROM `user` WHERE id = ?";
    private static final String SELECT_BY_ID_SQL = "SELECT * FROM `user` WHERE id = ?";
    private static final String SELECT_ALL_SQL = "SELECT * FROM `user`";

    private final Connection connection;
    private final Set<String> userColumns;
    private final List<String> allowedRoles;

    public UserService() {
        this.connection = MyDatabase.getInstance().getCnx();
        this.userColumns = loadUserColumns();
        this.allowedRoles = loadAllowedRoles();
    }

    @Override
    public void add(User user) throws SQLException {
        validateUser(user);
        List<String> columns = getWritableColumns();
        String insertSql = buildInsertSql(columns);

        try (PreparedStatement ps = this.connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            fillStatement(ps, user, columns);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setId(rs.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(User user) throws SQLException {
        if (user == null || user.getId() <= 0) {
            throw new IllegalArgumentException("User id is required for update");
        }
        validateUser(user);
        List<String> columns = getWritableColumns();
        String updateSql = buildUpdateSql(columns);

        try (PreparedStatement ps = this.connection.prepareStatement(updateSql)) {
            int lastIndex = fillStatement(ps, user, columns);
            ps.setInt(lastIndex + 1, user.getId());
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
    public User getById(int id) throws SQLException {
        try (PreparedStatement ps = this.connection.prepareStatement(SELECT_BY_ID_SQL)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        }

        return null;
    }

    @Override
    public List<User> getAll() throws SQLException {
        List<User> users = new ArrayList<>();

        try (Statement st = this.connection.createStatement();
             ResultSet rs = st.executeQuery(SELECT_ALL_SQL)) {
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        }

        return users;
    }

    private int fillStatement(PreparedStatement ps, User user, List<String> columns) throws SQLException {
        int index = 1;
        for (String column : columns) {
            switch (column) {
                case "nom" -> ps.setString(index++, user.getNom());
                case "email" -> ps.setString(index++, user.getEmail());
                case "password" -> ps.setString(index++, user.getPassword());
                case "roles" -> ps.setString(index++, normalizeRole(user.getRoles()));
                case "date_inscription" -> ps.setDate(index++, toSqlDate(user.getDateInscription()));
                case "solde_total" -> ps.setDouble(index++, user.getSoldeTotal());
                case "image" -> ps.setString(index++, user.getImage());
                case "face_id_credential_id" -> ps.setString(index++, user.getFaceIdCredentialId());
                case "face_id_enabled" -> ps.setBoolean(index++, user.isFaceIdEnabled());
                case "face_plus_token" -> ps.setString(index++, user.getFacePlusToken());
                case "face_plus_enabled" -> ps.setBoolean(index++, user.isFacePlusEnabled());
                case "email_verified" -> ps.setBoolean(index++, user.isEmailVerified());
                case "email_verification_token" -> ps.setString(index++, user.getEmailVerificationToken());
                case "email_verified_at" -> ps.setTimestamp(index++, toSqlTimestamp(user.getEmailVerifiedAt()));
                case "blocked" -> ps.setBoolean(index++, user.isBlocked());
                case "blocked_reason" -> ps.setString(index++, user.getBlockedReason());
                case "blocked_at" -> ps.setTimestamp(index++, toSqlTimestamp(user.getBlockedAt()));
                case "geo_country_code" -> ps.setString(index++, user.getGeoCountryCode());
                case "geo_country_name" -> ps.setString(index++, user.getGeoCountryName());
                case "geo_region_name" -> ps.setString(index++, user.getGeoRegionName());
                case "geo_city_name" -> ps.setString(index++, user.getGeoCityName());
                case "geo_detected_ip" -> ps.setString(index++, user.getGeoDetectedIp());
                case "geo_vpn_suspected" -> ps.setBoolean(index++, user.isGeoVpnSuspected());
                case "geo_last_checked_at" -> ps.setTimestamp(index++, toSqlTimestamp(user.getGeoLastCheckedAt()));
                default -> throw new IllegalStateException("Unhandled user column: " + column);
            }
        }
        return index - 1;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        Date dateInscription = rs.getDate("date_inscription");

        return new User(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getString("roles"),
                dateInscription != null ? dateInscription.toLocalDate() : null,
                rs.getDouble("solde_total"),
                rs.getString("image"),
                null,
                false,
                null,
                false,
                false,
                null,
                null,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null
        );
    }

    private void validateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (user.getNom() == null || user.getNom().isBlank()) {
            throw new IllegalArgumentException("User name is required");
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("User email is required");
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new IllegalArgumentException("User password is required");
        }
    }

    private Date toSqlDate(LocalDate value) {
        return value != null ? Date.valueOf(value) : null;
    }

    private Timestamp toSqlTimestamp(LocalDateTime value) {
        return value != null ? Timestamp.valueOf(value) : null;
    }

    private Set<String> loadUserColumns() {
        Set<String> columns = new HashSet<>();
        try {
            DatabaseMetaData metaData = this.connection.getMetaData();
            try (ResultSet rs = metaData.getColumns(this.connection.getCatalog(), null, "user", null)) {
                while (rs.next()) {
                    columns.add(rs.getString("COLUMN_NAME"));
                }
            }
        } catch (SQLException ignored) {
            // Keep empty: SQL statements will fail clearly if table is not reachable.
        }
        return columns;
    }

    private List<String> loadAllowedRoles() {
        List<String> roles = new ArrayList<>();
        String sql = "SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'roles'";
        try (Statement st = this.connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                String columnType = rs.getString("COLUMN_TYPE");
                if (columnType != null && columnType.toLowerCase().startsWith("enum(")) {
                    Matcher matcher = Pattern.compile("'([^']*)'").matcher(columnType);
                    while (matcher.find()) {
                        roles.add(matcher.group(1));
                    }
                }
            }
        } catch (SQLException ignored) {
            // Keep empty: when unavailable, role is used as provided.
        }
        if (!roles.isEmpty()) {
            return roles;
        }

        String checkSql = "SELECT cc.CHECK_CLAUSE FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS cc " +
                "JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc " +
                "ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME " +
                "WHERE tc.TABLE_SCHEMA = DATABASE() AND tc.TABLE_NAME = 'user' AND tc.CONSTRAINT_TYPE = 'CHECK'";
        try (Statement st = this.connection.createStatement();
             ResultSet rs = st.executeQuery(checkSql)) {
            while (rs.next()) {
                String clause = rs.getString("CHECK_CLAUSE");
                if (clause != null && clause.toLowerCase().contains("roles")) {
                    Matcher matcher = Pattern.compile("'([^']*)'").matcher(clause);
                    while (matcher.find()) {
                        String value = matcher.group(1);
                        if (!value.isBlank() && !roles.contains(value)) {
                            roles.add(value);
                        }
                    }
                }
            }
        } catch (SQLException ignored) {
            // Keep empty: next fallback is existing data.
        }
        if (!roles.isEmpty()) {
            return roles;
        }

        String sampleSql = "SELECT roles FROM `user` WHERE roles IS NOT NULL AND roles <> '' LIMIT 1";
        try (Statement st = this.connection.createStatement();
             ResultSet rs = st.executeQuery(sampleSql)) {
            if (rs.next()) {
                String role = rs.getString("roles");
                if (role != null && !role.isBlank()) {
                    roles.add(role);
                }
            }
        } catch (SQLException ignored) {
            // Keep empty.
        }
        return roles;
    }

    private String normalizeRole(String role) {
        if (this.allowedRoles.isEmpty()) {
            return role;
        }
        if (role == null || role.isBlank()) {
            return this.allowedRoles.get(0);
        }
        String normalizedInput = role.trim();
        for (String allowed : this.allowedRoles) {
            if (allowed.equalsIgnoreCase(normalizedInput)) {
                return allowed;
            }
        }
        if (normalizedInput.toUpperCase().startsWith("ROLE_")) {
            String withoutPrefix = normalizedInput.substring(5);
            for (String allowed : this.allowedRoles) {
                if (allowed.equalsIgnoreCase(withoutPrefix)) {
                    return allowed;
                }
            }
        }
        for (String allowed : this.allowedRoles) {
            if (("ROLE_" + allowed).equalsIgnoreCase(normalizedInput)) {
                return allowed;
            }
        }
        return this.allowedRoles.get(0);
    }

    private List<String> getWritableColumns() {
        List<String> columns = new ArrayList<>();
        addIfPresent(columns, "nom");
        addIfPresent(columns, "email");
        addIfPresent(columns, "password");
        addIfPresent(columns, "roles");
        addIfPresent(columns, "date_inscription");
        addIfPresent(columns, "solde_total");
        addIfPresent(columns, "image");
        addIfPresent(columns, "face_id_credential_id");
        addIfPresent(columns, "face_id_enabled");
        addIfPresent(columns, "face_plus_token");
        addIfPresent(columns, "face_plus_enabled");
        addIfPresent(columns, "email_verified");
        addIfPresent(columns, "email_verification_token");
        addIfPresent(columns, "email_verified_at");
        addIfPresent(columns, "blocked");
        addIfPresent(columns, "blocked_reason");
        addIfPresent(columns, "blocked_at");
        addIfPresent(columns, "geo_country_code");
        addIfPresent(columns, "geo_country_name");
        addIfPresent(columns, "geo_region_name");
        addIfPresent(columns, "geo_city_name");
        addIfPresent(columns, "geo_detected_ip");
        addIfPresent(columns, "geo_vpn_suspected");
        addIfPresent(columns, "geo_last_checked_at");
        if (columns.isEmpty()) {
            columns.add("nom");
            columns.add("email");
            columns.add("password");
            columns.add("roles");
            columns.add("date_inscription");
            columns.add("solde_total");
            columns.add("image");
        }
        return columns;
    }

    private void addIfPresent(List<String> list, String column) {
        if (this.userColumns.contains(column)) {
            list.add(column);
        }
    }

    private String buildInsertSql(List<String> columns) {
        StringJoiner cols = new StringJoiner(", ");
        StringJoiner vals = new StringJoiner(", ");
        for (String column : columns) {
            cols.add(column);
            vals.add("?");
        }
        return "INSERT INTO `user` (" + cols + ") VALUES (" + vals + ")";
    }

    private String buildUpdateSql(List<String> columns) {
        StringJoiner sets = new StringJoiner(", ");
        for (String column : columns) {
            sets.add(column + " = ?");
        }
        return "UPDATE `user` SET " + sets + " WHERE id = ?";
    }

}
