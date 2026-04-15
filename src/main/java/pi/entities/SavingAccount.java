package pi.entities;

import java.sql.Date;

public class SavingAccount {
    private int id;
    private int userId;
    private double sold;
    private Date dateCreation;
    private double tauxInteret;

    public SavingAccount() {
    }

    public SavingAccount(int userId, double sold, Date dateCreation, double tauxInteret) {
        this.userId = userId;
        this.sold = sold;
        this.dateCreation = dateCreation;
        this.tauxInteret = tauxInteret;
    }

    public SavingAccount(int id, int userId, double sold, Date dateCreation, double tauxInteret) {
        this.id = id;
        this.userId = userId;
        this.sold = sold;
        this.dateCreation = dateCreation;
        this.tauxInteret = tauxInteret;
    }

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public double getSold() {
        return sold;
    }

    public Date getDateCreation() {
        return dateCreation;
    }

    public double getTauxInteret() {
        return tauxInteret;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setSold(double sold) {
        this.sold = sold;
    }

    public void setDateCreation(Date dateCreation) {
        this.dateCreation = dateCreation;
    }

    public void setTauxInteret(double tauxInteret) {
        this.tauxInteret = tauxInteret;
    }

    @Override
    public String toString() {
        return "SavingAccount{" +
                "id=" + id +
                ", userId=" + userId +
                ", sold=" + sold +
                ", dateCreation=" + dateCreation +
                ", tauxInteret=" + tauxInteret +
                '}';
    }
}