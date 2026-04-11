package pi.entities.entities;

import java.time.LocalDateTime;

public class UserBehaviorProfile {

    private int id;
    private User user;
    private int score;
    private String profileType;
    private String strengths;
    private String weaknesses;
    private String nextActions;
    private LocalDateTime updatedAt;

    public UserBehaviorProfile(User user, int score, String profileType, String strengths, String weaknesses, String nextActions, LocalDateTime updatedAt) {
        this.user = user;
        this.score = score;
        this.profileType = profileType;
        this.strengths = strengths;
        this.weaknesses = weaknesses;
        this.nextActions = nextActions;
        this.updatedAt = updatedAt;
    }

    public UserBehaviorProfile(int id, User user, int score, String profileType, String strengths, String weaknesses, String nextActions, LocalDateTime updatedAt) {
        this.id = id;
        this.user = user;
        this.score = score;
        this.profileType = profileType;
        this.strengths = strengths;
        this.weaknesses = weaknesses;
        this.nextActions = nextActions;
        this.updatedAt = updatedAt;
    }

    public int getId() { return this.id; }

    public void setId(int id) { this.id = id; }

    public User getUser() { return this.user; }

    public void setUser(User user) { this.user = user; }

    public int getScore() { return this.score; }

    public void setScore(int score) { this.score = score; }

    public String getProfileType() { return this.profileType; }

    public void setProfileType(String profileType) { this.profileType = profileType; }

    public String getStrengths() { return this.strengths; }

    public void setStrengths(String strengths) { this.strengths = strengths; }

    public String getWeaknesses() { return this.weaknesses; }

    public void setWeaknesses(String weaknesses) { this.weaknesses = weaknesses; }

    public String getNextActions() { return this.nextActions; }

    public void setNextActions(String nextActions) { this.nextActions = nextActions; }

    public LocalDateTime getUpdatedAt() { return this.updatedAt; }

    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "UserBehaviorProfile{" + "id=" + this.id + ", profileType='" + this.profileType + '\'' + ", score=" + this.score + '}';
    }
}
