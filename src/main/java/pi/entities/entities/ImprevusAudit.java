package pi.entities.entities;

import java.time.LocalDateTime;

public class ImprevusAudit {

    private int id;
    private String type;
    private String objectId;
    private String discriminator;
    private String transactionHash;
    private String diffs;
    private String blameId;
    private String blameUser;
    private String blameUserFqdn;
    private String blameUserFirewall;
    private String ip;
    private LocalDateTime createdAt;

    public ImprevusAudit(String type, String objectId, String discriminator, String transactionHash, String diffs, String blameId, String blameUser, String blameUserFqdn, String blameUserFirewall, String ip, LocalDateTime createdAt) {
        this.type = type;
        this.objectId = objectId;
        this.discriminator = discriminator;
        this.transactionHash = transactionHash;
        this.diffs = diffs;
        this.blameId = blameId;
        this.blameUser = blameUser;
        this.blameUserFqdn = blameUserFqdn;
        this.blameUserFirewall = blameUserFirewall;
        this.ip = ip;
        this.createdAt = createdAt;
    }

    public ImprevusAudit(int id, String type, String objectId, String discriminator, String transactionHash, String diffs, String blameId, String blameUser, String blameUserFqdn, String blameUserFirewall, String ip, LocalDateTime createdAt) {
        this.id = id;
        this.type = type;
        this.objectId = objectId;
        this.discriminator = discriminator;
        this.transactionHash = transactionHash;
        this.diffs = diffs;
        this.blameId = blameId;
        this.blameUser = blameUser;
        this.blameUserFqdn = blameUserFqdn;
        this.blameUserFirewall = blameUserFirewall;
        this.ip = ip;
        this.createdAt = createdAt;
    }

    public int getId() { return this.id; }

    public void setId(int id) { this.id = id; }

    public String getType() { return this.type; }

    public void setType(String type) { this.type = type; }

    public String getObjectId() { return this.objectId; }

    public void setObjectId(String objectId) { this.objectId = objectId; }

    public String getDiscriminator() { return this.discriminator; }

    public void setDiscriminator(String discriminator) { this.discriminator = discriminator; }

    public String getTransactionHash() { return this.transactionHash; }

    public void setTransactionHash(String transactionHash) { this.transactionHash = transactionHash; }

    public String getDiffs() { return this.diffs; }

    public void setDiffs(String diffs) { this.diffs = diffs; }

    public String getBlameId() { return this.blameId; }

    public void setBlameId(String blameId) { this.blameId = blameId; }

    public String getBlameUser() { return this.blameUser; }

    public void setBlameUser(String blameUser) { this.blameUser = blameUser; }

    public String getBlameUserFqdn() { return this.blameUserFqdn; }

    public void setBlameUserFqdn(String blameUserFqdn) { this.blameUserFqdn = blameUserFqdn; }

    public String getBlameUserFirewall() { return this.blameUserFirewall; }

    public void setBlameUserFirewall(String blameUserFirewall) { this.blameUserFirewall = blameUserFirewall; }

    public String getIp() { return this.ip; }

    public void setIp(String ip) { this.ip = ip; }

    public LocalDateTime getCreatedAt() { return this.createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "ImprevusAudit{" + "id=" + this.id + ", type='" + this.type + '\'' + ", objectId='" + this.objectId + '\'' + '}';
    }
}
