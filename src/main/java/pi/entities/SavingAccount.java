package pi.entities;

import java.time.LocalDate;

public class SavingAccount {

    private int id;
    private User user;
    private Double sold;
    private LocalDate dateCreation;
    private Double tauxInteret;

    public SavingAccount(User user, Double sold, LocalDate dateCreation, Double tauxInteret) {
        this.user = user;
        this.sold = sold;
        this.dateCreation = dateCreation;
        this.tauxInteret = tauxInteret;
    }

    public SavingAccount(int id, User user, Double sold, LocalDate dateCreation, Double tauxInteret) {
        this.id = id;
        this.user = user;
        this.sold = sold;
        this.dateCreation = dateCreation;
        this.tauxInteret = tauxInteret;
    }

    public int getId() { return this.id; }

    public void setId(int id) { this.id = id; }

    public User getUser() { return this.user; }

    public void setUser(User user) { this.user = user; }

    public Double getSold() { return this.sold; }

    public void setSold(Double sold) { this.sold = sold; }

    public LocalDate getDateCreation() { return this.dateCreation; }

    public void setDateCreation(LocalDate dateCreation) { this.dateCreation = dateCreation; }

    public Double getTauxInteret() { return this.tauxInteret; }

    public void setTauxInteret(Double tauxInteret) { this.tauxInteret = tauxInteret; }

    @Override
    public String toString() {
        return "SavingAccount{" + "id=" + this.id + ", sold=" + this.sold + '}';
    }
}
