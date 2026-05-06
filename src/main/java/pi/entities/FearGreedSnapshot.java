package pi.entities;

/**
 * Latest Crypto Fear and Greed Index reading (Alternative.me API).
 */
public class FearGreedSnapshot {

    private final int value;
    private final String classification;
    private final long timestampSeconds;

    public FearGreedSnapshot(int value, String classification, long timestampSeconds) {
        this.value = value;
        this.classification = classification != null ? classification : "";
        this.timestampSeconds = timestampSeconds;
    }

    public int getValue() {
        return value;
    }

    public String getClassification() {
        return classification;
    }

    public long getTimestampSeconds() {
        return timestampSeconds;
    }
}
