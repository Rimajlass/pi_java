package pi.entities;

import java.time.LocalDateTime;

public class Reclamation {

    private int id;
    private User user;
    private User adminResponder;
    private String subject;
    private String message;
    private String adminResponse;
    private String status;
    private boolean containsBadWords;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;

    public Reclamation(User user, User adminResponder, String subject, String message, String adminResponse, String status, boolean containsBadWords, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime resolvedAt) {
        this.user = user;
        this.adminResponder = adminResponder;
        this.subject = subject;
        this.message = message;
        this.adminResponse = adminResponse;
        this.status = status;
        this.containsBadWords = containsBadWords;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.resolvedAt = resolvedAt;
    }

    public Reclamation(int id, User user, User adminResponder, String subject, String message, String adminResponse, String status, boolean containsBadWords, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime resolvedAt) {
        this.id = id;
        this.user = user;
        this.adminResponder = adminResponder;
        this.subject = subject;
        this.message = message;
        this.adminResponse = adminResponse;
        this.status = status;
        this.containsBadWords = containsBadWords;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.resolvedAt = resolvedAt;
    }

    public int getId() { return this.id; }

    public void setId(int id) { this.id = id; }

    public User getUser() { return this.user; }

    public void setUser(User user) { this.user = user; }

    public User getAdminResponder() { return this.adminResponder; }

    public void setAdminResponder(User adminResponder) { this.adminResponder = adminResponder; }

    public String getSubject() { return this.subject; }

    public void setSubject(String subject) { this.subject = subject; }

    public String getMessage() { return this.message; }

    public void setMessage(String message) { this.message = message; }

    public String getAdminResponse() { return this.adminResponse; }

    public void setAdminResponse(String adminResponse) { this.adminResponse = adminResponse; }

    public String getStatus() { return this.status; }

    public void setStatus(String status) { this.status = status; }

    public boolean isContainsBadWords() { return this.containsBadWords; }

    public void setContainsBadWords(boolean containsBadWords) { this.containsBadWords = containsBadWords; }

    public LocalDateTime getCreatedAt() { return this.createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return this.updatedAt; }

    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getResolvedAt() { return this.resolvedAt; }

    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    @Override
    public String toString() {
        return "Reclamation{" + "id=" + this.id + ", subject='" + this.subject + '\'' + ", status='" + this.status + '\'' + '}';
    }
}
