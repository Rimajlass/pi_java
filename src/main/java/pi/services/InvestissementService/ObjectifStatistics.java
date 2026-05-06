package pi.services.InvestissementService;

import pi.entities.Objectif;

import java.util.List;
import java.util.Map;

/**
 * Agrégats statistiques sur un ensemble d'objectifs et les valeurs de marché courantes.
 */
public final class ObjectifStatistics {

    private ObjectifStatistics() {
    }

    public static final class Snapshot {
        public final int total;
        public final long completedCount;
        /** Part des objectifs marqués complétés (0–100). */
        public final double completionRatePercent;
        /** Progression moyenne (%) sur les objectifs encore en cours (vers la cible). */
        public final double avgProgressOpenPercent;
        /** ROI moyen pondéré par le montant initial investi (USD). */
        public final double weightedAvgRoiPercent;
        /** Somme des montants initiaux déclarés (USD). */
        public final double sumInitialUsd;
        /** Somme des valeurs de marché courantes (USD). */
        public final double sumMarketUsd;
        /** Somme des cibles sur objectifs non complétés (USD). */
        public final double sumTargetOpenUsd;
        /** Somme des restes à gagner sur objectifs non complétés (USD). */
        public final double sumRemainingOpenUsd;
        /** Objectifs critiques avec progression &lt; 35 % de la cible. */
        public final int critiquesRetard;
        public final int countBasse;
        public final int countNormale;
        public final int countHaute;
        public final int countCritique;

        Snapshot(int total, long completedCount, double completionRatePercent,
                double avgProgressOpenPercent, double weightedAvgRoiPercent,
                double sumInitialUsd, double sumMarketUsd, double sumTargetOpenUsd,
                double sumRemainingOpenUsd, int critiquesRetard,
                int countBasse, int countNormale, int countHaute, int countCritique) {
            this.total = total;
            this.completedCount = completedCount;
            this.completionRatePercent = completionRatePercent;
            this.avgProgressOpenPercent = avgProgressOpenPercent;
            this.weightedAvgRoiPercent = weightedAvgRoiPercent;
            this.sumInitialUsd = sumInitialUsd;
            this.sumMarketUsd = sumMarketUsd;
            this.sumTargetOpenUsd = sumTargetOpenUsd;
            this.sumRemainingOpenUsd = sumRemainingOpenUsd;
            this.critiquesRetard = critiquesRetard;
            this.countBasse = countBasse;
            this.countNormale = countNormale;
            this.countHaute = countHaute;
            this.countCritique = countCritique;
        }
    }

    public static Snapshot compute(List<Objectif> objectifs, Map<Integer, Double> valeurActuelleParId) {
        int total = objectifs.size();
        long completed = objectifs.stream().filter(Objectif::isCompleted).count();

        double completionRate = total > 0 ? (completed * 100.0 / total) : 0;

        double sumInitial = 0;
        double sumMarket = 0;
        double sumTargetOpen = 0;
        double weightedRoiNumerator = 0;
        double sumRemainingOpen = 0;
        double sumProgressOpen = 0;
        int countOpenWithTarget = 0;
        int critiquesRetard = 0;

        int cb = 0;
        int cn = 0;
        int ch = 0;
        int cc = 0;

        for (Objectif o : objectifs) {
            sumInitial += o.getInitialAmount();
            double cur = valeurActuelleParId.getOrDefault(o.getId(), 0.0);
            sumMarket += cur;
            if (!o.isCompleted()) {
                sumTargetOpen += o.getTargetAmount();
            }

            String p = o.getPriorite();
            if (Objectif.P_BASSE.equals(p)) {
                cb++;
            } else if (Objectif.P_HAUTE.equals(p)) {
                ch++;
            } else if (Objectif.P_CRITIQUE.equals(p)) {
                cc++;
            } else {
                cn++;
            }

            if (o.getInitialAmount() > 0) {
                weightedRoiNumerator += ObjectifMetrics.roiPercent(o, cur) * o.getInitialAmount();
            }

            if (!o.isCompleted()) {
                sumRemainingOpen += Math.max(0, o.getTargetAmount() - cur);
                double tgt = o.getTargetAmount();
                if (tgt > 0) {
                    double progPct = Math.min(100.0, cur / tgt * 100.0);
                    sumProgressOpen += progPct;
                    countOpenWithTarget++;
                }
                double ratio = tgt > 0 ? cur / tgt : 0;
                if (Objectif.P_CRITIQUE.equals(o.getPriorite()) && ratio < 0.35) {
                    critiquesRetard++;
                }
            }
        }

        double avgProgressOpen = countOpenWithTarget > 0 ? sumProgressOpen / countOpenWithTarget : 0;
        double weightedRoi = sumInitial > 0 ? weightedRoiNumerator / sumInitial : 0;

        return new Snapshot(
                total,
                completed,
                completionRate,
                avgProgressOpen,
                weightedRoi,
                sumInitial,
                sumMarket,
                sumTargetOpen,
                sumRemainingOpen,
                critiquesRetard,
                cb,
                cn,
                ch,
                cc
        );
    }
}
