package pi.entities;

public class Crypto {

    private int id;
    private String name;
    private String symbol;
    private String apiid;
    private double currentprice;
    private String imageUrl;

    public Crypto() {
    }

    public Crypto(String name, String symbol, String apiid, double currentprice) {
        this.name = name;
        this.symbol = symbol;
        this.apiid = apiid;
        this.currentprice = currentprice;
    }

    public Crypto(int id, String name, String symbol, String apiid, double currentprice) {
        this.id = id;
        this.name = name;
        this.symbol = symbol;
        this.apiid = apiid;
        this.currentprice = currentprice;
    }

    public Crypto(String name, String symbol, String apiid, double currentprice, String imageUrl) {
        this.name = name;
        this.symbol = symbol;
        this.apiid = apiid;
        this.currentprice = currentprice;
        this.imageUrl = imageUrl;
    }

    public Crypto(int id, String name, String symbol, String apiid, double currentprice, String imageUrl) {
        this.id = id;
        this.name = name;
        this.symbol = symbol;
        this.apiid = apiid;
        this.currentprice = currentprice;
        this.imageUrl = imageUrl;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getSymbolUpper() {
        return symbol != null ? symbol.toUpperCase() : "";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getApiid() {
        return apiid;
    }

    public void setApiid(String apiid) {
        this.apiid = apiid;
    }

    public double getCurrentprice() {
        return currentprice;
    }

    public void setCurrentprice(double currentprice) {
        this.currentprice = currentprice;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    @Override
    public String toString() {
        return name; // important pour ComboBox
    }
}