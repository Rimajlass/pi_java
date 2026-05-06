package pi.entities;

import java.time.LocalDateTime;

public class CryptoPricePoint {

    private LocalDateTime dateTime;
    private double price;

    public CryptoPricePoint(LocalDateTime dateTime, double price) {
        this.dateTime = dateTime;
        this.price = price;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public double getPrice() {
        return price;
    }
}