package pi.services.ImprevusCasreelService;

import pi.entities.User;
import pi.entities.UserNotification;
import pi.tools.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserNotificationService {

    private final Connection connection;

    public UserNotificationService() {
        this.connection = MyDatabase.getInstance().getCnx();
        createTableIfMissing();
    }

    public void create(UserNotification notification) {
        String sql = """
                INSERT INTO user_notification (user_id, title, message, status, is_read, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, notification.getUser().getId());
            statement.setString(2, notification.getTitle());
            statement.setString(3, notification.getMessage());
            statement.setString(4, notification.getStatus());
            statement.setBoolean(5, notification.isRead());
            statement.setTimestamp(6, Timestamp.valueOf(notification.getCreatedAt() == null ? LocalDateTime.now() : notification.getCreatedAt()));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Impossible d'enregistrer la notification utilisateur : " + exception.getMessage(), exception);
        }
    }

    public Optional<UserNotification> findLatestByUserId(int userId) {
        String sql = """
                SELECT id, user_id, title, message, status, is_read, created_at
                FROM user_notification
                WHERE user_id = ?
                ORDER BY created_at DESC, id DESC
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Impossible de charger la notification utilisateur : " + exception.getMessage(), exception);
        }
        return Optional.empty();
    }

    public List<UserNotification> findByUserId(int userId) {
        String sql = """
                SELECT id, user_id, title, message, status, is_read, created_at
                FROM user_notification
                WHERE user_id = ?
                ORDER BY created_at DESC, id DESC
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<UserNotification> notifications = new ArrayList<>();
                while (resultSet.next()) {
                    notifications.add(map(resultSet));
                }
                return notifications;
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Impossible de lister les notifications utilisateur : " + exception.getMessage(), exception);
        }
    }

    public void markAsRead(int notificationId) {
        String sql = "UPDATE user_notification SET is_read = 1 WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, notificationId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Impossible de marquer la notification comme lue : " + exception.getMessage(), exception);
        }
    }

    private UserNotification map(ResultSet resultSet) throws SQLException {
        User user = new User();
        user.setId(resultSet.getInt("user_id"));
        Timestamp createdAt = resultSet.getTimestamp("created_at");
        return new UserNotification(
                resultSet.getInt("id"),
                user,
                resultSet.getString("title"),
                resultSet.getString("message"),
                resultSet.getString("status"),
                resultSet.getBoolean("is_read"),
                createdAt == null ? null : createdAt.toLocalDateTime()
        );
    }

    private void createTableIfMissing() {
        String sql = """
                CREATE TABLE IF NOT EXISTS user_notification (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    user_id INT NOT NULL,
                    title VARCHAR(180) NOT NULL,
                    message LONGTEXT NOT NULL,
                    status VARCHAR(40) NOT NULL,
                    is_read TINYINT(1) NOT NULL DEFAULT 0,
                    created_at DATETIME NOT NULL
                )
                """;
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException exception) {
            throw new RuntimeException("Impossible de creer la table user_notification : " + exception.getMessage(), exception);
        }
    }
}
