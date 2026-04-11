package pi.services.UserTransactionService;

import pi.entities.User;
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
import java.util.Locale;

public class UserService {

    private final Connection cnx;
    private final PasswordService passwordService;

    public UserService() {
        this.cnx = MyDatabase.getInstance().getCnx();
        this.passwordService = new PasswordService();
    }

    public List<User> findForAdminIndex(String search, String role, String sortBy, String order) {
        List<User> users = new ArrayList<>();
        String normalizedRole = normalizeRoleFilter(role);
        String sortExpression = resolveSort(sortBy);
        String sortOrder = "DESC".equalsIgnoreCase(order) ? "DESC" : "ASC";

        StringBuilder sql = new StringBuilder("SELECT * FROM `user` WHERE 1=1");
        List<Object> parameters = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND (LOWER(nom) LIKE ? OR LOWER(email) LIKE ?)");
            String searchPattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            parameters.add(searchPattern);
            parameters.add(searchPattern);
        }

        if (!normalizedRole.isBlank()) {
            if ("ROLE_USER_ONLY".equals(normalizedRole)) {
                sql.append(" AND roles NOT LIKE ? AND roles NOT LIKE ? AND roles NOT LIKE ?");
                parameters.add("%ROLE_ADMIN%");
                parameters.add("%ROLE_SALARY%");
                parameters.add("%ROLE_ETUDIANT%");
            } else {
                sql.append(" AND roles LIKE ?");
                parameters.add("%" + normalizedRole + "%");
            }
        }

        sql.append(" ORDER BY ").append(sortExpression).append(" ").append(sortOrder).append(", id DESC");

        try (PreparedStatement ps = cnx.prepareStatement(sql.toString())) {
            bindParameters(ps, parameters);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du chargement des utilisateurs : " + e.getMessage(), e);
        }

        return users;
    }

    public User findById(int id) {
        String sql = "SELECT * FROM `user` WHERE id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche utilisateur : " + e.getMessage(), e);
        }

        return null;
    }

    public User findByEmail(String email) {
        String sql = "SELECT * FROM `user` WHERE LOWER(email) = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, normalizeEmail(email));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche par email : " + e.getMessage(), e);
        }

        return null;
    }

    public User create(User user, String plainPassword) {
        validateUserForWrite(user, true);
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("Le mot de passe est obligatoire.");
        }
        if (findByEmail(user.getEmail()) != null) {
            throw new IllegalArgumentException("Cet email existe deja.");
        }

        String sql = """
                INSERT INTO `user` (
                    nom, email, password, roles, date_inscription, solde_total, image,
                    face_id_credential_id, face_id_enabled, face_plus_token, face_plus_enabled,
                    email_verified, email_verification_token, email_verified_at,
                    is_blocked, blocked_reason, blocked_at,
                    geo_country_code, geo_country_name, geo_region_name, geo_city_name,
                    geo_detected_ip, geo_vpn_suspected, geo_last_checked_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        prepareUserForInsert(user, plainPassword);

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindUserStatement(ps, user);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout utilisateur : " + e.getMessage(), e);
        }

        return user;
    }

    public User update(User user, String plainPassword) {
        if (user == null || user.getId() <= 0) {
            throw new IllegalArgumentException("Utilisateur invalide.");
        }

        User existing = findById(user.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Utilisateur introuvable.");
        }

        validateUserForWrite(user, false);

        User emailOwner = findByEmail(user.getEmail());
        if (emailOwner != null && emailOwner.getId() != user.getId()) {
            throw new IllegalArgumentException("Cet email existe deja.");
        }

        String sql = """
                UPDATE `user`
                SET nom = ?, email = ?, password = ?, roles = ?, date_inscription = ?, solde_total = ?, image = ?,
                    face_id_credential_id = ?, face_id_enabled = ?, face_plus_token = ?, face_plus_enabled = ?,
                    email_verified = ?, email_verification_token = ?, email_verified_at = ?,
                    is_blocked = ?, blocked_reason = ?, blocked_at = ?,
                    geo_country_code = ?, geo_country_name = ?, geo_region_name = ?, geo_city_name = ?,
                    geo_detected_ip = ?, geo_vpn_suspected = ?, geo_last_checked_at = ?
                WHERE id = ?
                """;

        user.setEmail(normalizeEmail(user.getEmail()));
        if (user.getDateInscription() == null) {
            user.setDateInscription(existing.getDateInscription());
        }
        if (user.getRoles() == null || user.getRoles().isBlank()) {
            user.setRoles(existing.getRoles());
        }

        String passwordToStore = existing.getPassword();
        if (plainPassword != null && !plainPassword.isBlank()) {
            passwordToStore = hashPassword(plainPassword);
        }
        user.setPassword(passwordToStore);

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            bindUserStatement(ps, user);
            ps.setInt(25, user.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la modification utilisateur : " + e.getMessage(), e);
        }

        return user;
    }

    public void delete(int id) {
        String sql = "DELETE FROM `user` WHERE id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression utilisateur : " + e.getMessage(), e);
        }
    }

    public User register(String nom, String email, String password, String confirmPassword, String role, double solde,
                         String faceIdCredentialId, String facePlusToken) {
        if (password == null || !password.equals(confirmPassword)) {
            throw new IllegalArgumentException("La confirmation du mot de passe ne correspond pas.");
        }

        User user = new User();
        user.setNom(nom);
        user.setEmail(normalizeEmail(email));
        user.setRoles(resolveRegistrationRoles(role));
        user.setDateInscription(LocalDate.now());
        user.setSoldeTotal(solde);
        user.setFaceIdCredentialId(blankToNull(faceIdCredentialId));
        user.setFaceIdEnabled(faceIdCredentialId != null && !faceIdCredentialId.isBlank());
        user.setFacePlusToken(blankToNull(facePlusToken));
        user.setFacePlusEnabled(facePlusToken != null && !facePlusToken.isBlank());
        user.setEmailVerified(true);
        user.markEmailVerifiedAt(LocalDateTime.now());
        user.setEmailVerificationToken(null);
        user.setBlocked(false);
        user.setGeoVpnSuspected(false);

        return create(user, password);
    }

    public User authenticate(String email, String plainPassword) {
        User user = findByEmail(email);
        if (user == null) {
            return null;
        }
        if (user.isBlocked()) {
            throw new IllegalStateException("Ce compte est bloque.");
        }
        if (plainPassword == null || !passwordService.verifyPassword(plainPassword, user.getPassword())) {
            return null;
        }
        return user;
    }

    public boolean isEmailAvailable(String email) {
        return normalizeEmail(email) != null && findByEmail(email) == null;
    }

    private void prepareUserForInsert(User user, String plainPassword) {
        user.setEmail(normalizeEmail(user.getEmail()));
        user.setPassword(hashPassword(plainPassword));
        if (user.getDateInscription() == null) {
            user.setDateInscription(LocalDate.now());
        }
        if (user.getRoles() == null || user.getRoles().isBlank()) {
            user.setRoles("[\"ROLE_USER\"]");
        }
    }

    private void validateUserForWrite(User user, boolean creating) {
        if (user == null) {
            throw new IllegalArgumentException("Utilisateur invalide.");
        }
        if (user.getNom() == null || user.getNom().trim().length() < 2) {
            throw new IllegalArgumentException("Le nom doit contenir au moins 2 caracteres.");
        }
        String email = normalizeEmail(user.getEmail());
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new IllegalArgumentException("Email invalide.");
        }
        if (email.length() > 180) {
            throw new IllegalArgumentException("Email trop long.");
        }
        if (user.getSoldeTotal() < 0) {
            throw new IllegalArgumentException("Le solde doit etre positif ou nul.");
        }
        if (!creating && user.getId() <= 0) {
            throw new IllegalArgumentException("Identifiant utilisateur invalide.");
        }
    }

    private void bindUserStatement(PreparedStatement ps, User user) throws SQLException {
        ps.setString(1, user.getNom());
        ps.setString(2, user.getEmail());
        ps.setString(3, user.getPassword());
        ps.setString(4, user.getRoles());
        ps.setDate(5, user.getDateInscription() != null ? Date.valueOf(user.getDateInscription()) : null);
        ps.setDouble(6, user.getSoldeTotal());
        ps.setString(7, blankToNull(user.getImage()));
        ps.setString(8, blankToNull(user.getFaceIdCredentialId()));
        ps.setBoolean(9, user.isFaceIdEnabled());
        ps.setString(10, blankToNull(user.getFacePlusToken()));
        ps.setBoolean(11, user.isFacePlusEnabled());
        ps.setBoolean(12, user.isEmailVerified());
        ps.setString(13, blankToNull(user.getEmailVerificationToken()));
        ps.setTimestamp(14, toTimestamp(user.getEmailVerifiedAt()));
        ps.setBoolean(15, user.isBlocked());
        ps.setString(16, blankToNull(user.getBlockedReason()));
        ps.setTimestamp(17, toTimestamp(user.getBlockedAt()));
        ps.setString(18, blankToNull(user.getGeoCountryCode()));
        ps.setString(19, blankToNull(user.getGeoCountryName()));
        ps.setString(20, blankToNull(user.getGeoRegionName()));
        ps.setString(21, blankToNull(user.getGeoCityName()));
        ps.setString(22, blankToNull(user.getGeoDetectedIp()));
        ps.setBoolean(23, user.isGeoVpnSuspected());
        ps.setTimestamp(24, toTimestamp(user.getGeoLastCheckedAt()));
    }

    private User mapUser(ResultSet rs) throws SQLException {
        Date dateInscription = rs.getDate("date_inscription");
        Timestamp emailVerifiedAt = rs.getTimestamp("email_verified_at");
        Timestamp blockedAt = rs.getTimestamp("blocked_at");
        Timestamp geoLastCheckedAt = rs.getTimestamp("geo_last_checked_at");

        return new User(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getString("roles"),
                dateInscription != null ? dateInscription.toLocalDate() : null,
                rs.getDouble("solde_total"),
                rs.getString("image"),
                rs.getString("face_id_credential_id"),
                rs.getBoolean("face_id_enabled"),
                rs.getString("face_plus_token"),
                rs.getBoolean("face_plus_enabled"),
                rs.getBoolean("email_verified"),
                rs.getString("email_verification_token"),
                emailVerifiedAt != null ? emailVerifiedAt.toLocalDateTime() : null,
                rs.getBoolean("is_blocked"),
                rs.getString("blocked_reason"),
                blockedAt != null ? blockedAt.toLocalDateTime() : null,
                rs.getString("geo_country_code"),
                rs.getString("geo_country_name"),
                rs.getString("geo_region_name"),
                rs.getString("geo_city_name"),
                rs.getString("geo_detected_ip"),
                rs.getBoolean("geo_vpn_suspected"),
                geoLastCheckedAt != null ? geoLastCheckedAt.toLocalDateTime() : null
        );
    }

    private void bindParameters(PreparedStatement ps, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            ps.setObject(i + 1, parameters.get(i));
        }
    }

    private String normalizeRoleFilter(String role) {
        if (role == null) {
            return "";
        }
        String value = role.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "ROLE_ADMIN", "ROLE_SALARY", "ROLE_ETUDIANT", "ROLE_USER_ONLY" -> value;
            default -> "";
        };
    }

    private String resolveRegistrationRoles(String role) {
        if (role == null) {
            throw new IllegalArgumentException("Role invalide.");
        }
        return switch (role.trim().toUpperCase(Locale.ROOT)) {
            case "ETUDIANT", "ROLE_ETUDIANT" -> "[\"ROLE_ETUDIANT\"]";
            case "SALARIE", "SALARY", "ROLE_SALARY" -> "[\"ROLE_SALARY\"]";
            case "ADMIN", "ROLE_ADMIN" -> "[\"ROLE_ADMIN\"]";
            default -> throw new IllegalArgumentException("Role invalide.");
        };
    }

    private String resolveSort(String sortBy) {
        if (sortBy == null) {
            return "nom";
        }
        return switch (sortBy.trim().toLowerCase(Locale.ROOT)) {
            case "id" -> "id";
            case "email" -> "email";
            case "solde" -> "solde_total";
            case "date" -> "date_inscription";
            default -> "nom";
        };
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Timestamp toTimestamp(LocalDateTime value) {
        return value != null ? Timestamp.valueOf(value) : null;
    }

    private String hashPassword(String plainPassword) {
        return passwordService.hashPassword(plainPassword);
    }
}
