package pi.entities.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class User {

    private int id;
    private String nom;
    private String email;
    private String password;
    private String roles;
    private LocalDate dateInscription;
    private double soldeTotal;
    private String image;
    private String faceIdCredentialId;
    private boolean faceIdEnabled;
    private String facePlusToken;
    private boolean facePlusEnabled;
    private boolean emailVerified;
    private String emailVerificationToken;
    private LocalDateTime emailVerifiedAt;
    private boolean blocked;
    private String blockedReason;
    private LocalDateTime blockedAt;
    private String geoCountryCode;
    private String geoCountryName;
    private String geoRegionName;
    private String geoCityName;
    private String geoDetectedIp;
    private boolean geoVpnSuspected;
    private LocalDateTime geoLastCheckedAt;

    public User(String nom, String email, String password, String roles, LocalDate dateInscription, double soldeTotal, String image, String faceIdCredentialId, boolean faceIdEnabled, String facePlusToken, boolean facePlusEnabled, boolean emailVerified, String emailVerificationToken, LocalDateTime emailVerifiedAt, boolean blocked, String blockedReason, LocalDateTime blockedAt, String geoCountryCode, String geoCountryName, String geoRegionName, String geoCityName, String geoDetectedIp, boolean geoVpnSuspected, LocalDateTime geoLastCheckedAt) {
        this.nom = nom;
        this.email = email;
        this.password = password;
        this.roles = roles;
        this.dateInscription = dateInscription;
        this.soldeTotal = soldeTotal;
        this.image = image;
        this.faceIdCredentialId = faceIdCredentialId;
        this.faceIdEnabled = faceIdEnabled;
        this.facePlusToken = facePlusToken;
        this.facePlusEnabled = facePlusEnabled;
        this.emailVerified = emailVerified;
        this.emailVerificationToken = emailVerificationToken;
        this.emailVerifiedAt = emailVerifiedAt;
        this.blocked = blocked;
        this.blockedReason = blockedReason;
        this.blockedAt = blockedAt;
        this.geoCountryCode = geoCountryCode;
        this.geoCountryName = geoCountryName;
        this.geoRegionName = geoRegionName;
        this.geoCityName = geoCityName;
        this.geoDetectedIp = geoDetectedIp;
        this.geoVpnSuspected = geoVpnSuspected;
        this.geoLastCheckedAt = geoLastCheckedAt;
    }

    public User(int id, String nom, String email, String password, String roles, LocalDate dateInscription, double soldeTotal, String image, String faceIdCredentialId, boolean faceIdEnabled, String facePlusToken, boolean facePlusEnabled, boolean emailVerified, String emailVerificationToken, LocalDateTime emailVerifiedAt, boolean blocked, String blockedReason, LocalDateTime blockedAt, String geoCountryCode, String geoCountryName, String geoRegionName, String geoCityName, String geoDetectedIp, boolean geoVpnSuspected, LocalDateTime geoLastCheckedAt) {
        this.id = id;
        this.nom = nom;
        this.email = email;
        this.password = password;
        this.roles = roles;
        this.dateInscription = dateInscription;
        this.soldeTotal = soldeTotal;
        this.image = image;
        this.faceIdCredentialId = faceIdCredentialId;
        this.faceIdEnabled = faceIdEnabled;
        this.facePlusToken = facePlusToken;
        this.facePlusEnabled = facePlusEnabled;
        this.emailVerified = emailVerified;
        this.emailVerificationToken = emailVerificationToken;
        this.emailVerifiedAt = emailVerifiedAt;
        this.blocked = blocked;
        this.blockedReason = blockedReason;
        this.blockedAt = blockedAt;
        this.geoCountryCode = geoCountryCode;
        this.geoCountryName = geoCountryName;
        this.geoRegionName = geoRegionName;
        this.geoCityName = geoCityName;
        this.geoDetectedIp = geoDetectedIp;
        this.geoVpnSuspected = geoVpnSuspected;
        this.geoLastCheckedAt = geoLastCheckedAt;
    }
    public User() {
    }
    public int getId() { return this.id; }

    public void setId(int id) { this.id = id; }

    public String getNom() { return this.nom; }

    public void setNom(String nom) { this.nom = nom; }

    public String getEmail() { return this.email; }

    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return this.password; }

    public void setPassword(String password) { this.password = password; }

    public String getRoles() { return this.roles; }

    public void setRoles(String roles) { this.roles = roles; }

    public LocalDate getDateInscription() { return this.dateInscription; }

    public void setDateInscription(LocalDate dateInscription) { this.dateInscription = dateInscription; }

    public double getSoldeTotal() { return this.soldeTotal; }

    public void setSoldeTotal(double soldeTotal) { this.soldeTotal = soldeTotal; }

    public String getImage() { return this.image; }

    public void setImage(String image) { this.image = image; }

    public String getFaceIdCredentialId() { return this.faceIdCredentialId; }

    public void setFaceIdCredentialId(String faceIdCredentialId) { this.faceIdCredentialId = faceIdCredentialId; }

    public boolean isFaceIdEnabled() { return this.faceIdEnabled; }

    public void setFaceIdEnabled(boolean faceIdEnabled) { this.faceIdEnabled = faceIdEnabled; }

    public String getFacePlusToken() { return this.facePlusToken; }

    public void setFacePlusToken(String facePlusToken) { this.facePlusToken = facePlusToken; }

    public boolean isFacePlusEnabled() { return this.facePlusEnabled; }

    public void setFacePlusEnabled(boolean facePlusEnabled) { this.facePlusEnabled = facePlusEnabled; }

    public boolean isEmailVerified() { return this.emailVerified; }

    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public String getEmailVerificationToken() { return this.emailVerificationToken; }

    public void setEmailVerificationToken(String emailVerificationToken) { this.emailVerificationToken = emailVerificationToken; }

    public LocalDateTime getEmailVerifiedAt() { return this.emailVerifiedAt; }

    public void setEmailVerifiedAt(LocalDateTime emailVerifiedAt) { this.emailVerifiedAt = emailVerifiedAt; }

    public boolean isBlocked() { return this.blocked; }

    public void setBlocked(boolean blocked) { this.blocked = blocked; }

    public String getBlockedReason() { return this.blockedReason; }

    public void setBlockedReason(String blockedReason) { this.blockedReason = blockedReason; }

    public LocalDateTime getBlockedAt() { return this.blockedAt; }

    public void setBlockedAt(LocalDateTime blockedAt) { this.blockedAt = blockedAt; }

    public String getGeoCountryCode() { return this.geoCountryCode; }

    public void setGeoCountryCode(String geoCountryCode) { this.geoCountryCode = geoCountryCode; }

    public String getGeoCountryName() { return this.geoCountryName; }

    public void setGeoCountryName(String geoCountryName) { this.geoCountryName = geoCountryName; }

    public String getGeoRegionName() { return this.geoRegionName; }

    public void setGeoRegionName(String geoRegionName) { this.geoRegionName = geoRegionName; }

    public String getGeoCityName() { return this.geoCityName; }

    public void setGeoCityName(String geoCityName) { this.geoCityName = geoCityName; }

    public String getGeoDetectedIp() { return this.geoDetectedIp; }

    public void setGeoDetectedIp(String geoDetectedIp) { this.geoDetectedIp = geoDetectedIp; }

    public boolean isGeoVpnSuspected() { return this.geoVpnSuspected; }

    public void setGeoVpnSuspected(boolean geoVpnSuspected) { this.geoVpnSuspected = geoVpnSuspected; }

    public LocalDateTime getGeoLastCheckedAt() { return this.geoLastCheckedAt; }

    public void setGeoLastCheckedAt(LocalDateTime geoLastCheckedAt) { this.geoLastCheckedAt = geoLastCheckedAt; }

    @Override
    public String toString() {
        return "User{" + "id=" + this.id + ", nom='" + this.nom + '\'' + ", email='" + this.email + '\'' + ", soldeTotal=" + this.soldeTotal + '}';
    }
}
