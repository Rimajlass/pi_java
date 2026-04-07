package pi.entities;

public class Crypto {

    private int id;
    private String name;
    private String symbol;
    private String apiid;
    private double currentprice;

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

    public int getId() { return this.id; }

    public void setId(int id) { this.id = id; }

    public String getName() { return this.name; }

    public void setName(String name) { this.name = name; }

    public String getSymbol() { return this.symbol; }

    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getApiid() { return this.apiid; }

    public void setApiid(String apiid) { this.apiid = apiid; }

    public double getCurrentprice() { return this.currentprice; }

    public void setCurrentprice(double currentprice) { this.currentprice = currentprice; }

    @Override
    public String toString() {
        return "Crypto{" + "id=" + this.id + ", symbol='" + this.symbol + '\'' + ", currentprice=" + this.currentprice + '}';
    }
}
