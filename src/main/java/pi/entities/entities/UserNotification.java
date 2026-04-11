package pi.entities.entities;

import java.time.LocalDateTime;

public class UserNotification {

    private int id;
    private User user;
    private String title;
    private String message;
    private String status;
    private boolean read;
    private LocalDateTime createdAt;

    public UserNotification(User user, String title, String message, String status, boolean read, LocalDateTime createdAt) {
        this.user = user;
        this.title = title;
        this.message = message;
        this.status = status;
        this.read = read;
        this.createdAt = createdAt;
    }

    public UserNotification(int id, User user, String title, String message, String status, boolean read, LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.title = title;
        this.message = message;
        this.status = status;
        this.read = read;
        this.createdAt = createdAt;
    }

    public int getId() { return this.id; }

    public void setId(int id) { this.id = id; }

    public User getUser() { return this.user; }

    public void setUser(User user) { this.user = user; }

    public String getTitle() { return this.title; }

    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return this.message; }

    public void setMessage(String message) { this.message = message; }

    public String getStatus() { return this.status; }

    public void setStatus(String status) { this.status = status; }

    public boolean isRead() { return this.read; }

    public void setRead(boolean read) { this.read = read; }

    public LocalDateTime getCreatedAt() { return this.createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "UserNotification{" + "id=" + this.id + ", title='" + this.title + '\'' + ", read=" + this.read + '}';
    }
}
