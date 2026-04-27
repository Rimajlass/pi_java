package pi.services.ImprevusCasreelService;

import pi.entities.CasRelles;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PreventionPlanService {

    public record PreventionPlan(
            boolean eligible,
            String dominantRisk,
            int caseCount,
            int percentage,
            String triggerReason,
            String immediateAction,
            String monthlyReminderTitle,
            String suggestedBudget,
            String suggestedService,
            String preventionAdvice
    ) {
    }

    public PreventionPlan buildPlan(List<CasRelles> allCases,
                                    Function<CasRelles, String> riskResolver,
                                    String activeCity) {
        if (allCases == null || allCases.isEmpty() || riskResolver == null) {
            return emptyPlan();
        }

        LocalDate limit = LocalDate.now().minusDays(30);
        List<CasRelles> recentExpenses = allCases.stream()
                .filter(cas -> "Depense".equalsIgnoreCase(cas.getType()))
                .filter(cas -> cas.getDateEffet() != null && !cas.getDateEffet().isBefore(limit))
                .toList();

        if (recentExpenses.isEmpty()) {
            return emptyPlan();
        }

        Map<String, Long> counts = recentExpenses.stream()
                .collect(Collectors.groupingBy(riskResolver, Collectors.counting()));

        Map.Entry<String, Long> dominantEntry = counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);
        if (dominantEntry == null || dominantEntry.getValue() <= 0) {
            return emptyPlan();
        }

        String dominantRisk = dominantEntry.getKey();
        int caseCount = dominantEntry.getValue().intValue();
        int percentage = (int) Math.round((caseCount * 100.0) / recentExpenses.size());
        boolean eligible = caseCount >= 2 || percentage > 40;
        if (!eligible) {
            return emptyPlan();
        }

        double averageAmount = recentExpenses.stream()
                .filter(cas -> dominantRisk.equals(riskResolver.apply(cas)))
                .mapToDouble(CasRelles::getMontant)
                .average()
                .orElse(0.0);

        String city = activeCity == null || activeCity.isBlank() ? "votre zone" : activeCity;
        return switch (dominantRisk) {
            case "Voiture" -> new PreventionPlan(
                    true,
                    dominantRisk,
                    caseCount,
                    percentage,
                    "Ce risque devient prioritaire car " + caseCount + " cas voiture ont ete detectes sur 30 jours (" + percentage + "% des depenses imprevues).",
                    "Verifier aujourd'hui pneus, freins et niveau d'huile pour eviter une panne plus lourde.",
                    "Entretien preventif voiture",
                    formatBudget(Math.max(120, averageAmount * 0.35)),
                    "Garage d'entretien ou diagnostic auto proche de " + city,
                    "Programmer un controle mensuel et garder une reserve dediee a l'entretien auto."
            );
            case "Sante" -> new PreventionPlan(
                    true,
                    dominantRisk,
                    caseCount,
                    percentage,
                    "Ce risque devient prioritaire car " + caseCount + " cas sante ont ete detectes sur 30 jours (" + percentage + "% des depenses imprevues).",
                    "Prendre rapidement un rendez-vous de bilan ou de controle selon le symptome recurrent.",
                    "Bilan prevention sante",
                    formatBudget(Math.max(80, averageAmount * 0.30)),
                    "Medecin, laboratoire ou clinique proche de " + city,
                    "Faire un suivi mensuel et ne pas attendre que le meme probleme revienne en urgence."
            );
            case "Maison" -> new PreventionPlan(
                    true,
                    dominantRisk,
                    caseCount,
                    percentage,
                    "Ce risque devient prioritaire car " + caseCount + " incidents maison ont ete detectes sur 30 jours (" + percentage + "% des depenses imprevues).",
                    "Faire une verification immediate de la zone touchee pour stopper la repetition du probleme.",
                    "Controle maintenance maison",
                    formatBudget(Math.max(100, averageAmount * 0.30)),
                    "Plombier, electricien ou service maintenance proche de " + city,
                    "Planifier un controle mensuel et traiter les petits signes avant qu'ils deviennent couteux."
            );
            default -> new PreventionPlan(
                    true,
                    dominantRisk,
                    caseCount,
                    percentage,
                    "Ce risque devient prioritaire car " + caseCount + " cas ont ete detectes sur 30 jours (" + percentage + "% des depenses imprevues).",
                    "Traiter rapidement la cause principale pour limiter une nouvelle depense.",
                    "Rappel mensuel de prevention",
                    formatBudget(Math.max(70, averageAmount * 0.25)),
                    "Service specialise proche de " + city,
                    "Suivre ce risque chaque mois et reserver un petit budget preventif."
            );
        };
    }

    private PreventionPlan emptyPlan() {
        return new PreventionPlan(false, null, 0, 0, null, null, null, null, null, null);
    }

    private String formatBudget(double amount) {
        return String.format("%.2f DT", amount);
    }
}
