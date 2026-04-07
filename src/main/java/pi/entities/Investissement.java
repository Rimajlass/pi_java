package pi.entities;

import java.time.LocalDate;

public class Investissement {

    private int id;
    private Crypto crypto;
    private Objectif objectif;
    private User user;
    private double amountInvested;
    private double buyPrice;
    private double quantity;
    private LocalDate createdAt;

    public Investissement(Crypto crypto, Objectif objectif, User user, double amountInvested, double buyPrice, double quantity, LocalDate createdAt) {
        this.crypto = crypto;
        this.objectif = objectif;
        this.user = user;
        this.amountInvested = amountInvested;
        this.buyPrice = buyPrice;
        this.quantity = quantity;
        this.createdAt = createdAt;
    }

    public Investissement(int id, Crypto crypto, Objectif objectif, User user, double amountInvested, double buyPrice, double quantity, LocalDate createdAt) {
        this.id = id;
        this.crypto = crypto;
        this.objectif = objectif;
        this.user = user;
        this.amountInvested = amountInvested;
        this.buyPrice = buyPrice;
        this.quantity = quantity;
        this.createdAt = createdAt;
    }

    public int getId() { return this.id; }

    public void setId(int id) { this.id = id; }

    public Crypto getCrypto() { return this.crypto; }

    public void setCrypto(Crypto crypto) { this.crypto = crypto; }

    public Objectif getObjectif() { return this.objectif; }

    public void setObjectif(Objectif objectif) { this.objectif = objectif; }

    public User getUser() { return this.user; }

    public void setUser(User user) { this.user = user; }

    public double getAmountInvested() { return this.amountInvested; }

    public void setAmountInvested(double amountInvested) { this.amountInvested = amountInvested; }

    public double getBuyPrice() { return this.buyPrice; }

    public void setBuyPrice(double buyPrice) { this.buyPrice = buyPrice; }

    public double getQuantity() { return this.quantity; }

    public void setQuantity(double quantity) { this.quantity = quantity; }

    public LocalDate getCreatedAt() { return this.createdAt; }

    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Investissement{" + "id=" + this.id + ", amountInvested=" + this.amountInvested + '}';
    }
}
