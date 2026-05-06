package pi.services.CoursQuizService;

import pi.entities.Cours;
import pi.entities.User;
import pi.tools.MyDatabase;

import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class LearningCertificationService {

    public static final int PASS_THRESHOLD_PERCENT = 80;

    private final Connection cnx;

    public LearningCertificationService() {
        this.cnx = MyDatabase.getInstance().getCnx();
        creerTablesSiAbsentes();
    }

    public CertificationOutcome recordAttemptAndAward(User user, Cours cours, int score, int total, int percentage) {
        if (user == null || user.getId() <= 0) {
            throw new IllegalArgumentException("Utilisateur non connecté.");
        }
        if (cours == null || cours.getId() <= 0) {
            throw new IllegalArgumentException("Cours invalide.");
        }
        int safeTotal = Math.max(0, total);
        int safeScore = Math.max(0, score);
        int safePercent = Math.max(0, Math.min(100, percentage));
        boolean passed = safePercent >= PASS_THRESHOLD_PERCENT;

        long attemptId = insertAttempt(user.getId(), cours.getId(), safeScore, safeTotal, safePercent, passed);

        List<UserBadge> newlyAwarded = new ArrayList<>();
        CertificateInfo certificate = null;

        if (passed) {
            certificate = ensureCertificate(user, cours, safePercent);
            newlyAwarded.addAll(awardBadges(user, cours));
        }

        return new CertificationOutcome(attemptId, passed, newlyAwarded, certificate);
    }

    public List<UserBadge> listBadges(int userId) {
        String sql = """
                SELECT badge_code, badge_title, level, cours_id, awarded_at
                FROM learning_user_badges
                WHERE user_id = ?
                ORDER BY awarded_at DESC, id DESC
                """;
        List<UserBadge> badges = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    badges.add(new UserBadge(
                            rs.getString("badge_code"),
                            rs.getString("badge_title"),
                            rs.getString("level"),
                            (Integer) rs.getObject("cours_id"),
                            rs.getTimestamp("awarded_at").toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur chargement badges: " + e.getMessage(), e);
        }
        return badges;
    }

    public CertificateInfo getCertificate(int userId, int coursId) {
        String sql = """
                SELECT certificate_code, percentage, issued_at, pdf_path
                FROM learning_certificates
                WHERE user_id = ? AND cours_id = ?
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, coursId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new CertificateInfo(
                            rs.getString("certificate_code"),
                            rs.getInt("percentage"),
                            rs.getTimestamp("issued_at").toLocalDateTime(),
                            rs.getString("pdf_path")
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur chargement certificat: " + e.getMessage(), e);
        }
        return null;
    }

    public List<AttemptInfo> listAttempts(int userId, int limit) {
        int safeLimit = Math.max(1, Math.min(200, limit));
        String sql = """
                SELECT cours_id, score, total, percentage, passed, attempted_at
                FROM learning_quiz_attempts
                WHERE user_id = ?
                ORDER BY attempted_at DESC, id DESC
                LIMIT ?
                """;
        List<AttemptInfo> attempts = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, safeLimit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    attempts.add(new AttemptInfo(
                            rs.getInt("cours_id"),
                            rs.getInt("score"),
                            rs.getInt("total"),
                            rs.getInt("percentage"),
                            rs.getBoolean("passed"),
                            rs.getTimestamp("attempted_at").toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur chargement tentatives: " + e.getMessage(), e);
        }
        return attempts;
    }

    public List<CertificateInfoWithCourse> listCertificates(int userId, int limit) {
        int safeLimit = Math.max(1, Math.min(200, limit));
        String sql = """
                SELECT cours_id, certificate_code, percentage, issued_at, pdf_path
                FROM learning_certificates
                WHERE user_id = ?
                ORDER BY issued_at DESC, id DESC
                LIMIT ?
                """;
        List<CertificateInfoWithCourse> certs = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, safeLimit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    certs.add(new CertificateInfoWithCourse(
                            rs.getInt("cours_id"),
                            rs.getString("certificate_code"),
                            rs.getInt("percentage"),
                            rs.getTimestamp("issued_at").toLocalDateTime(),
                            rs.getString("pdf_path")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur chargement certificats: " + e.getMessage(), e);
        }
        return certs;
    }

    private long insertAttempt(int userId, int coursId, int score, int total, int percentage, boolean passed) {
        String sql = """
                INSERT INTO learning_quiz_attempts(user_id, cours_id, score, total, percentage, passed, attempted_at)
                VALUES(?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setInt(2, coursId);
            ps.setInt(3, score);
            ps.setInt(4, total);
            ps.setInt(5, percentage);
            ps.setBoolean(6, passed);
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            return -1;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur insertion tentative quiz: " + e.getMessage(), e);
        }
    }

    private CertificateInfo ensureCertificate(User user, Cours cours, int percentage) {
        CertificateInfo existing = getCertificate(user.getId(), cours.getId());
        if (existing != null) {
            return existing;
        }

        String code = buildCertificateCode();
        String pdfPath = null;

        String sql = """
                INSERT INTO learning_certificates(user_id, cours_id, certificate_code, percentage, issued_at, pdf_path)
                VALUES(?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, user.getId());
            ps.setInt(2, cours.getId());
            ps.setString(3, code);
            ps.setInt(4, percentage);
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            if (pdfPath == null || pdfPath.isBlank()) {
                ps.setNull(6, Types.VARCHAR);
            } else {
                ps.setString(6, pdfPath);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur création certificat: " + e.getMessage(), e);
        }

        return new CertificateInfo(code, percentage, LocalDateTime.now(), "");
    }

    private List<UserBadge> awardBadges(User user, Cours cours) {
        List<UserBadge> awarded = new ArrayList<>();

        // Badge "course passed"
        awarded.addAll(insertBadgeIfAbsent(user.getId(), "COURSE_PASSED", "Cours validé", "BRONZE", cours.getId()));

        // Badge "first certification"
        int certCount = countCertificates(user.getId());
        if (certCount == 1) {
            awarded.addAll(insertBadgeIfAbsent(user.getId(), "FIRST_CERT", "Première certification", "BRONZE", null));
        }

        // Level badges based on number of certificates
        if (certCount >= 1) {
            awarded.addAll(insertBadgeIfAbsent(user.getId(), "LEVEL_BRONZE", "Niveau Bronze", "BRONZE", null));
        }
        if (certCount >= 3) {
            awarded.addAll(insertBadgeIfAbsent(user.getId(), "LEVEL_SILVER", "Niveau Silver", "SILVER", null));
        }
        if (certCount >= 5) {
            awarded.addAll(insertBadgeIfAbsent(user.getId(), "LEVEL_GOLD", "Niveau Gold", "GOLD", null));
        }

        return awarded;
    }

    private int countCertificates(int userId) {
        String sql = "SELECT COUNT(*) FROM learning_certificates WHERE user_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur count certificats: " + e.getMessage(), e);
        }
    }

    private List<UserBadge> insertBadgeIfAbsent(int userId, String code, String title, String level, Integer coursId) {
        if (badgeExists(userId, code, coursId)) {
            return List.of();
        }
        String sql = """
                INSERT INTO learning_user_badges(user_id, badge_code, badge_title, level, cours_id, awarded_at)
                VALUES(?, ?, ?, ?, ?, ?)
                """;
        LocalDateTime now = LocalDateTime.now();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, code);
            ps.setString(3, title);
            ps.setString(4, level);
            if (coursId == null) {
                ps.setNull(5, Types.INTEGER);
            } else {
                ps.setInt(5, coursId);
            }
            ps.setTimestamp(6, Timestamp.valueOf(now));
            ps.executeUpdate();
        } catch (SQLException e) {
            // ignore duplicates in case of race
            if (e.getMessage() == null || !e.getMessage().toLowerCase().contains("duplicate")) {
                throw new RuntimeException("Erreur attribution badge: " + e.getMessage(), e);
            }
        }
        return List.of(new UserBadge(code, title, level, coursId, now));
    }

    private boolean badgeExists(int userId, String badgeCode, Integer coursId) {
        String sql = """
                SELECT 1 FROM learning_user_badges
                WHERE user_id = ? AND badge_code = ? AND (cours_id <=> ?)
                LIMIT 1
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, badgeCode);
            if (coursId == null) {
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(3, coursId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur check badge: " + e.getMessage(), e);
        }
    }

    private String buildCertificateCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private void creerTablesSiAbsentes() {
        createTableAttempts();
        createTableCertificates();
        createTableBadges();
    }

    private void createTableAttempts() {
        String sql = """
                CREATE TABLE IF NOT EXISTS learning_quiz_attempts (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    user_id INT NOT NULL,
                    cours_id INT NOT NULL,
                    score INT NOT NULL,
                    total INT NOT NULL,
                    percentage INT NOT NULL,
                    passed TINYINT(1) NOT NULL DEFAULT 0,
                    attempted_at DATETIME NOT NULL,
                    INDEX idx_learning_attempt_user (user_id),
                    INDEX idx_learning_attempt_course (cours_id)
                )
                """;
        executeDdl(sql, "learning_quiz_attempts");
    }

    private void createTableCertificates() {
        String sql = """
                CREATE TABLE IF NOT EXISTS learning_certificates (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    user_id INT NOT NULL,
                    cours_id INT NOT NULL,
                    certificate_code VARCHAR(40) NOT NULL,
                    percentage INT NOT NULL,
                    issued_at DATETIME NOT NULL,
                    pdf_path VARCHAR(500) NULL,
                    UNIQUE KEY uq_learning_cert_user_course (user_id, cours_id),
                    INDEX idx_learning_cert_user (user_id)
                )
                """;
        executeDdl(sql, "learning_certificates");
    }

    private void createTableBadges() {
        String sql = """
                CREATE TABLE IF NOT EXISTS learning_user_badges (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    user_id INT NOT NULL,
                    badge_code VARCHAR(60) NOT NULL,
                    badge_title VARCHAR(140) NOT NULL,
                    level VARCHAR(20) NOT NULL,
                    cours_id INT NULL,
                    awarded_at DATETIME NOT NULL,
                    UNIQUE KEY uq_learning_badge (user_id, badge_code, cours_id),
                    INDEX idx_learning_badge_user (user_id)
                )
                """;
        executeDdl(sql, "learning_user_badges");
    }

    private void executeDdl(String ddl, String tableName) {
        try (Statement st = cnx.createStatement()) {
            st.executeUpdate(ddl);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur creation table " + tableName + " : " + e.getMessage(), e);
        }
    }

    public record UserBadge(String code, String title, String level, Integer coursId, LocalDateTime awardedAt) {
    }

    public record CertificateInfo(String code, int percentage, LocalDateTime issuedAt, String pdfPath) {
    }

    public record CertificationOutcome(long attemptId, boolean passed, List<UserBadge> badgesAwarded, CertificateInfo certificate) {
    }

    public record AttemptInfo(int coursId, int score, int total, int percentage, boolean passed, LocalDateTime attemptedAt) {
    }

    public record CertificateInfoWithCourse(int coursId, String code, int percentage, LocalDateTime issuedAt, String pdfPath) {
    }
}
