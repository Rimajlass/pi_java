package pi.entities.entities;

import java.time.LocalDateTime;

public class QuizResult {

    private int id;
    private Cours cours;
    private Quiz quiz;
    private String userName;
    private String userEmail;
    private int score;
    private int total;
    private int percentage;
    private boolean passed;
    private LocalDateTime date;

    public QuizResult(Cours cours, Quiz quiz, String userName, String userEmail, int score, int total, int percentage, boolean passed, LocalDateTime date) {
        this.cours = cours;
        this.quiz = quiz;
        this.userName = userName;
        this.userEmail = userEmail;
        this.score = score;
        this.total = total;
        this.percentage = percentage;
        this.passed = passed;
        this.date = date;
    }

    public QuizResult(int id, Cours cours, Quiz quiz, String userName, String userEmail, int score, int total, int percentage, boolean passed, LocalDateTime date) {
        this.id = id;
        this.cours = cours;
        this.quiz = quiz;
        this.userName = userName;
        this.userEmail = userEmail;
        this.score = score;
        this.total = total;
        this.percentage = percentage;
        this.passed = passed;
        this.date = date;
    }

    public int getId() { return this.id; }

    public void setId(int id) { this.id = id; }

    public Cours getCours() { return this.cours; }

    public void setCours(Cours cours) { this.cours = cours; }

    public Quiz getQuiz() { return this.quiz; }

    public void setQuiz(Quiz quiz) { this.quiz = quiz; }

    public String getUserName() { return this.userName; }

    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return this.userEmail; }

    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

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
        return "QuizResult{" + "id=" + this.id + ", score=" + this.score + '/' + this.total + '}';
    }
}
