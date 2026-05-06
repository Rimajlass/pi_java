package pi.services.InvestissementService;

import pi.entities.Objectif;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

/**
 * Simulation Monte Carlo (mouvement brownien géométrique, une étape à l'horizon T)
 * pour estimer la distribution de la valeur de portefeuille d'un objectif.
 * <p>
 * Modèle : {@code V_T = V_0 * exp((μ - σ²/2) T + σ √T Z)} avec {@code Z ~ N(0,1)}.
 * Hypothèses pédagogiques : pas de flux, volatilité et tendance constants — adapté à une démo crypto.
 */
public final class ObjectifMonteCarloService {

    public static final double DEFAULT_ANNUAL_VOLATILITY = 0.55;
    public static final double DEFAULT_ANNUAL_DRIFT = 0.06;
    public static final double DEFAULT_HORIZON_YEARS = 1.0;
    public static final int DEFAULT_SIMULATIONS = 25_000;

    private ObjectifMonteCarloService() {
    }

    public static final class SimulationResult {
        public final double v0;
        public final double targetAmount;
        public final double horizonYears;
        public final double muAnnual;
        public final double sigmaAnnual;
        public final int simulations;
        /** Proportion de trajectoires avec {@code V_T >= cible}. */
        public final double probabilityReachTarget;
        public final double meanFinalValue;
        public final double medianFinalValue;
        public final double percentile5;
        public final double percentile25;
        public final double percentile75;
        public final double percentile95;
        public final String[] histogramCategories;
        public final double[] histogramCounts;
        public final boolean objectiveAlreadyCompleted;

        SimulationResult(double v0, double targetAmount, double horizonYears, double muAnnual, double sigmaAnnual,
                int simulations, double probabilityReachTarget, double meanFinalValue, double medianFinalValue,
                double percentile5, double percentile25, double percentile75, double percentile95,
                String[] histogramCategories, double[] histogramCounts, boolean objectiveAlreadyCompleted) {
            this.v0 = v0;
            this.targetAmount = targetAmount;
            this.horizonYears = horizonYears;
            this.muAnnual = muAnnual;
            this.sigmaAnnual = sigmaAnnual;
            this.simulations = simulations;
            this.probabilityReachTarget = probabilityReachTarget;
            this.meanFinalValue = meanFinalValue;
            this.medianFinalValue = medianFinalValue;
            this.percentile5 = percentile5;
            this.percentile25 = percentile25;
            this.percentile75 = percentile75;
            this.percentile95 = percentile95;
            this.histogramCategories = histogramCategories;
            this.histogramCounts = histogramCounts;
            this.objectiveAlreadyCompleted = objectiveAlreadyCompleted;
        }
    }

    /**
     * Dérive annualisée implicite depuis la valeur initiale déclarée et la valeur courante (log-return / temps).
     * Bornée pour éviter des entrées extrêmes si l'historique est très court ou bruité.
     */
    public static double impliedAnnualDriftCapped(Objectif objectif, double currentValue) {
        if (objectif.getInitialAmount() <= 0 || currentValue <= 0) {
            return DEFAULT_ANNUAL_DRIFT;
        }
        LocalDate start = objectif.getCreatedAt() != null ? objectif.getCreatedAt() : LocalDate.now();
        long days = Math.max(1L, ChronoUnit.DAYS.between(start, LocalDate.now()));
        double years = days / 365.25;
        double mu = Math.log(currentValue / objectif.getInitialAmount()) / years;
        return Math.max(-0.6, Math.min(0.6, mu));
    }

    public static SimulationResult simulate(
            double v0,
            double targetAmount,
            boolean completed,
            double horizonYears,
            double annualDriftMu,
            double annualVolSigma,
            int numSimulations,
            long seed) {

        if (completed) {
            return new SimulationResult(
                    v0,
                    targetAmount,
                    horizonYears,
                    annualDriftMu,
                    annualVolSigma,
                    0,
                    1.0,
                    v0,
                    v0,
                    v0,
                    v0,
                    v0,
                    v0,
                    new String[0],
                    new double[0],
                    true
            );
        }

        double vStart = v0;
        if (vStart <= 0) {
            throw new IllegalArgumentException(
                    "La valeur actuelle de l'objectif est nulle ou négative : liez des investissements ou rafraîchissez.");
        }

        int n = Math.max(1_000, Math.min(numSimulations, 500_000));
        double T = horizonYears > 0 ? horizonYears : 0.25;
        double mu = annualDriftMu;
        double sigma = Math.max(annualVolSigma, 1e-6);

        Random rnd = new Random(seed);
        double driftTerm = (mu - 0.5 * sigma * sigma) * T;
        double volCoeff = sigma * Math.sqrt(T);

        double[] finals = new double[n];
        int reach = 0;
        double sum = 0;
        for (int i = 0; i < n; i++) {
            double z = rnd.nextGaussian();
            double vT = vStart * Math.exp(driftTerm + volCoeff * z);
            finals[i] = vT;
            sum += vT;
            if (vT >= targetAmount) {
                reach++;
            }
        }

        double[] sorted = finals.clone();
        Arrays.sort(sorted);

        double p = (double) reach / n;
        double mean = sum / n;
        double median = percentile(sorted, 0.50);
        double p5 = percentile(sorted, 0.05);
        double p25 = percentile(sorted, 0.25);
        double p75 = percentile(sorted, 0.75);
        double p95 = percentile(sorted, 0.95);

        Histogram h = buildHistogram(finals, 16);

        return new SimulationResult(
                vStart,
                targetAmount,
                T,
                mu,
                sigma,
                n,
                p,
                mean,
                median,
                p5,
                p25,
                p75,
                p95,
                h.labels,
                h.counts,
                false
        );
    }

    private static double percentile(double[] sorted, double p) {
        if (sorted.length == 0) {
            return Double.NaN;
        }
        double idx = p * (sorted.length - 1);
        int lo = (int) Math.floor(idx);
        int hi = (int) Math.ceil(idx);
        if (lo == hi) {
            return sorted[lo];
        }
        double w = idx - lo;
        return sorted[lo] * (1 - w) + sorted[hi] * w;
    }

    private static final class Histogram {
        final String[] labels;
        final double[] counts;

        Histogram(String[] labels, double[] counts) {
            this.labels = labels;
            this.counts = counts;
        }
    }

    private static Histogram buildHistogram(double[] values, int bins) {
        if (values.length == 0 || bins < 2) {
            return new Histogram(new String[0], new double[0]);
        }
        double min = Arrays.stream(values).min().orElse(0);
        double max = Arrays.stream(values).max().orElse(1);
        if (max <= min) {
            max = min * 1.0001 + 1;
        }
        double width = (max - min) / bins;
        double[] counts = new double[bins];
        for (double v : values) {
            int b = (int) Math.floor((v - min) / width);
            if (b < 0) {
                b = 0;
            }
            if (b >= bins) {
                b = bins - 1;
            }
            counts[b]++;
        }
        String[] labels = new String[bins];
        for (int i = 0; i < bins; i++) {
            double a = min + i * width;
            double bnd = min + (i + 1) * width;
            labels[i] = String.format(Locale.FRENCH, "%.0f–%.0f", a, bnd);
        }
        return new Histogram(labels, counts);
    }

    public static String formatSummary(SimulationResult r, Locale locale) {
        if (r.objectiveAlreadyCompleted) {
            return String.format(locale,
                    "Objectif déjà marqué comme atteint.\nValeur actuelle : %.2f USD — cible : %.2f USD.\n"
                            + "Probabilité d'atteindre la cible (trivial) : 100 %%.",
                    r.v0, r.targetAmount);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(locale, "Valeur de départ V₀ : %.2f USD\n", r.v0));
        sb.append(String.format(locale, "Cible : %.2f USD\n", r.targetAmount));
        sb.append(String.format(locale, "Horizon : %.2f an(s)\n", r.horizonYears));
        sb.append(String.format(locale, "Paramètres GBM — tendance annuelle μ : %.2f %%, volatilité annuelle σ : %.2f %%\n",
                r.muAnnual * 100, r.sigmaAnnual * 100));
        sb.append(String.format(locale, "Simulations : %d\n\n", r.simulations));
        sb.append(String.format(locale, "P(V_T ≥ cible) ≈ %.2f %%\n", r.probabilityReachTarget * 100));
        sb.append(String.format(locale, "E[V_T] ≈ %.2f USD\n", r.meanFinalValue));
        sb.append(String.format(locale, "Médiane : %.2f USD\n", r.medianFinalValue));
        sb.append(String.format(locale, "Percentiles — 5 %% : %.2f | 25 %% : %.2f | 75 %% : %.2f | 95 %% : %.2f USD\n",
                r.percentile5, r.percentile25, r.percentile75, r.percentile95));
        sb.append("\n— Modèle indicatif (GBM, pas de flux). Ne constitue pas un conseil en investissement.");
        return sb.toString();
    }
}
