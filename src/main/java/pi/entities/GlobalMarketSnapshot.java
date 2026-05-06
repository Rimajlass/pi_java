package pi.entities;

/**
 * CoinGecko {@code /global} summary (USD-focused fields).
 */
public class GlobalMarketSnapshot {

    private final double totalMarketCapUsd;
    private final double totalVolumeUsd;
    private final double btcDominancePercent;
    private final int activeCryptocurrencies;
    private final int markets;

    public GlobalMarketSnapshot(
            double totalMarketCapUsd,
            double totalVolumeUsd,
            double btcDominancePercent,
            int activeCryptocurrencies,
            int markets) {
        this.totalMarketCapUsd = totalMarketCapUsd;
        this.totalVolumeUsd = totalVolumeUsd;
        this.btcDominancePercent = btcDominancePercent;
        this.activeCryptocurrencies = activeCryptocurrencies;
        this.markets = markets;
    }

    public double getTotalMarketCapUsd() {
        return totalMarketCapUsd;
    }

    public double getTotalVolumeUsd() {
        return totalVolumeUsd;
    }

    public double getBtcDominancePercent() {
        return btcDominancePercent;
    }

    public int getActiveCryptocurrencies() {
        return activeCryptocurrencies;
    }

    public int getMarkets() {
        return markets;
    }
}
