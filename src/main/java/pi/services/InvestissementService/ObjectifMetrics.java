package pi.services.InvestissementService;

import pi.entities.Objectif;

/**
 * Rule-based KPIs for investment objectives (no external API).
 */
public final class ObjectifMetrics {

    private ObjectifMetrics() {
    }

    public static double roiPercent(Objectif o, double valeurActuelleUsd) {
        if (o.getInitialAmount() <= 0) {
            return 0.0;
        }
        return (valeurActuelleUsd - o.getInitialAmount()) / o.getInitialAmount() * 100.0;
    }

    /**
     * Short business alert label for steering committees / pilots.
     */
    public static String alerteMetier(Objectif o, double valeurActuelleUsd) {
        if (o.isCompleted()) {
            return "Atteint";
        }
        double target = o.getTargetAmount();
        if (target <= 0) {
            return "—";
        }
        double ratio = valeurActuelleUsd / target;
        boolean critique = Objectif.P_CRITIQUE.equals(o.getPriorite());
        if (critique && ratio < 0.35) {
            return "Critique — écart fort";
        }
        if (ratio < 0.20) {
            return "À surveiller";
        }
        if (ratio < 0.50) {
            return "Prioriser";
        }
        if (ratio < 1.0) {
            return "En bonne voie";
        }
        return "Sous tension";
    }

    public static String prioriteLabel(String code) {
        if (code == null) {
            return Objectif.P_NORMALE;
        }
        return switch (code) {
            case Objectif.P_BASSE -> "Basse";
            case Objectif.P_HAUTE -> "Haute";
            case Objectif.P_CRITIQUE -> "Critique";
            default -> "Normale";
        };
    }
}
