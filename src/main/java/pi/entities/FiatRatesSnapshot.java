package pi.entities;

/**
 * ECB-backed FX rates from Frankfurter (base USD).
 */
public class FiatRatesSnapshot {

    private final double eurPerUsd;
    private final double gbpPerUsd;
    private final String rateDateIso;

    public FiatRatesSnapshot(double eurPerUsd, double gbpPerUsd, String rateDateIso) {
        this.eurPerUsd = eurPerUsd;
        this.gbpPerUsd = gbpPerUsd;
        this.rateDateIso = rateDateIso != null ? rateDateIso : "";
    }

    public double getEurPerUsd() {
        return eurPerUsd;
    }

    public double getGbpPerUsd() {
        return gbpPerUsd;
    }

    public String getRateDateIso() {
        return rateDateIso;
    }
}
