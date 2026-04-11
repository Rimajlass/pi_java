package pi.entities.entities;

import java.time.LocalDateTime;

public class MessengerMessage {

    private long id;
    private String body;
    private String headers;
    private String queueName;
    private LocalDateTime createdAt;
    private LocalDateTime availableAt;
    private LocalDateTime deliveredAt;

    public MessengerMessage(String body, String headers, String queueName, LocalDateTime createdAt, LocalDateTime availableAt, LocalDateTime deliveredAt) {
        this.body = body;
        this.headers = headers;
        this.queueName = queueName;
        this.createdAt = createdAt;
        this.availableAt = availableAt;
        this.deliveredAt = deliveredAt;
    }

    public MessengerMessage(long id, String body, String headers, String queueName, LocalDateTime createdAt, LocalDateTime availableAt, LocalDateTime deliveredAt) {
        this.id = id;
        this.body = body;
        this.headers = headers;
        this.queueName = queueName;
        this.createdAt = createdAt;
        this.availableAt = availableAt;
        this.deliveredAt = deliveredAt;
    }

    public long getId() { return this.id; }

    public void setId(long id) { this.id = id; }

    public String getBody() { return this.body; }

    public void setBody(String body) { this.body = body; }

    public String getHeaders() { return this.headers; }

    public void setHeaders(String headers) { this.headers = headers; }

    public String getQueueName() { return this.queueName; }

    public void setQueueName(String queueName) { this.queueName = queueName; }

    public LocalDateTime getCreatedAt() { return this.createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getAvailableAt() { return this.availableAt; }

    public void setAvailableAt(LocalDateTime availableAt) { this.availableAt = availableAt; }

    public LocalDateTime getDeliveredAt() { return this.deliveredAt; }

    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }

    @Override
    public String toString() {
        return "MessengerMessage{" + "id=" + this.id + ", queueName='" + this.queueName + '\'' + '}';
    }
}
