package pi.entities;

import java.time.LocalDateTime;

public class CertificationResult {

    private int id;
    private String userName;
    private String userEmail;
    private String type;
    private String certificationName;
    private int score;
    private int total;
    private int percentage;
    private boolean passed;
    private LocalDateTime date;

    public CertificationResult(String userName, String userEmail, String type, String certificationName, int score, int total, int percentage, boolean passed, LocalDateTime date) {
        this.userName = userName;
        this.userEmail = userEmail;
        this.type = type;
        this.certificationName = certificationName;
        this.score = score;
        this.total = total;
        this.percentage = percentage;
        this.passed = passed;
        this.date = date;
    }

    public CertificationResult(int id, String userName, String userEmail, String type, String certificationName, int score, int total, int percentage, boolean passed, LocalDateTime date) {
        this.id = id;
        this.userName = userName;
        this.userEmail = userEmail;
        this.type = type;
        this.certificationName = certificationName;
        this.score = score;
        this.total = total;
        this.percentage = percentage;
        this.passed = passed;
        this.date = date;
    }

    public int getId() { return this.id; }

    public void setId(int id) { this.id = id; }

    public String getUserName() { return this.userName; }

    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return this.userEmail; }

    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getType() { return this.type; }

    public void setType(String type) { this.type = type; }

    public String getCertificationName() { return this.certificationName; }

    public void setCertificationName(String certificationName) { this.certificationName = certificationName; }

    public int getScore() { return this.score; }

    public void setScore(int score) { this.score = score; }

    public int getTotal() { return this.total; }

    public void setTotal(int total) { this.total = total; }

    public int getPercentage() { return this.percentage; }

    public void setPercentage(int percentage) { this.percentage = percentage; }

    public boolean isPassed() { return this.passed; }

    public void setPassed(boolean passed) { this.passed = passed; }

    public LocalDateTime getDate() { return this.date; }

    public void setDate(LocalDateTime date) { this.date = date; }

    @Override
    public String toString() {
        return "CertificationResult{" + "id=" + this.id + ", certificationName='" + this.certificationName + '\'' + ", passed=" + this.passed + '}';
    }
}
