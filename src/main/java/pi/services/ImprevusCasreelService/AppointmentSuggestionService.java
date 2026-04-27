package pi.services.ImprevusCasreelService;

import pi.entities.User;
import pi.tools.AppEnv;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AppointmentSuggestionService {

    public record AppointmentSuggestion(
            String title,
            String description,
            String reason,
            String caseTitle,
            String city,
            LocalDateTime startAt,
            LocalDateTime endAt,
            boolean recurringMonthly,
            List<String> placeTypesNeeded,
            String calendarUrl
    ) {
    }

    private static final DateTimeFormatter CALENDAR_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter UI_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a' HH:mm");

    public AppointmentSuggestion suggest(String dominantRisk, User user) {
        return suggest(dominantRisk, null, null, user, false, null);
    }

    public AppointmentSuggestion suggest(String dominantRisk, String caseTitle, String caseDescription, User user) {
        return suggest(dominantRisk, caseTitle, caseDescription, user, false, null);
    }

    public AppointmentSuggestion suggest(String dominantRisk, String caseTitle, String caseDescription, User user, boolean recurringMonthly) {
        return suggest(dominantRisk, caseTitle, caseDescription, user, recurringMonthly, null);
    }

    public AppointmentSuggestion suggest(String dominantRisk, String caseTitle, String caseDescription, User user, boolean recurringMonthly, String cityOverride) {
        String normalizedRisk = dominantRisk == null || dominantRisk.isBlank() ? "Autres" : dominantRisk;
        String city = resolveCity(user, cityOverride);
        LocalDateTime startAt = resolveStart(normalizedRisk);
        LocalDateTime endAt = startAt.plusMinutes(45);
        String title = switch (normalizedRisk) {
            case "Sante" -> "Bilan de prevention";
            case "Voiture" -> "Entretien preventif";
            case "Maison" -> "Controle maintenance maison";
            default -> "Rendez-vous de suivi";
        };
        String description = switch (normalizedRisk) {
            case "Sante" -> "Un rendez-vous de prevention est recommande pour limiter les risques sante recurrents.";
            case "Voiture" -> "Un rendez-vous garage est recommande pour limiter les pannes recurrentes.";
            case "Maison" -> "Un rendez-vous de maintenance est recommande pour limiter les imprevus maison.";
            default -> "Un rendez-vous de suivi est recommande pour anticiper les prochains imprevus.";
        };
        String reason = buildReason(normalizedRisk, caseTitle, caseDescription);
        List<String> placeTypes = buildPlaceTypes(normalizedRisk, caseTitle, caseDescription);
        String safeCaseTitle = caseTitle == null || caseTitle.isBlank() ? "risque general" : caseTitle.trim();

        return new AppointmentSuggestion(
                title,
                description,
                reason,
                safeCaseTitle,
                city,
                startAt,
                endAt,
                recurringMonthly,
                placeTypes,
                buildCalendarUrl(title, description + " Motif: " + reason, city, startAt, endAt, recurringMonthly)
        );
    }

    public String formatForUi(AppointmentSuggestion suggestion) {
        if (suggestion == null) {
            return "Rendez-vous suggere: aucun rendez-vous prioritaire pour le moment.";
        }
        if (suggestion.recurringMonthly()) {
            return "Rappel conseille pour \"" + suggestion.caseTitle() + "\": " + suggestion.title()
                    + " le " + suggestion.startAt().format(UI_DATE_FORMAT)
                    + " a " + suggestion.city()
                    + ". Pourquoi ce rappel mensuel: " + suggestion.reason()
                    + ". L'objectif est d'agir avant que ce probleme revienne encore.";
        }
        return "Rendez-vous suggere pour \"" + suggestion.caseTitle() + "\": " + suggestion.title()
                + " le " + suggestion.startAt().format(UI_DATE_FORMAT)
                + " a " + suggestion.city()
                + ". Pourquoi: " + suggestion.reason();
    }

    public String formatPlaceTypesForUi(AppointmentSuggestion suggestion) {
        if (suggestion == null || suggestion.placeTypesNeeded() == null || suggestion.placeTypesNeeded().isEmpty()) {
            return "Types de lieux utiles: suivi preventif general.";
        }
        return "Types de lieux utiles pour eviter ce cas: " + String.join(", ", suggestion.placeTypesNeeded()) + ".";
    }

    private String resolveCity(User user, String cityOverride) {
        if (cityOverride != null && !cityOverride.isBlank()) {
            return cityOverride.trim();
        }
        if (user != null && user.getGeoCityName() != null && !user.getGeoCityName().isBlank()) {
            return user.getGeoCityName();
        }
        String configured = AppEnv.get("DEFAULT_SUGGESTION_CITY");
        return configured == null || configured.isBlank() ? "Tunis" : configured.trim();
    }

    private LocalDateTime resolveStart(String dominantRisk) {
        int plusDays = switch (dominantRisk) {
            case "Sante" -> 2;
            case "Voiture" -> 4;
            case "Maison" -> 5;
            default -> 7;
        };
        LocalDate date = LocalDate.now().plusDays(plusDays);
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        LocalTime time = "Sante".equals(dominantRisk) ? LocalTime.of(9, 30) : LocalTime.of(10, 0);
        return LocalDateTime.of(date, time);
    }

    private String buildCalendarUrl(String title, String description, String city, LocalDateTime startAt, LocalDateTime endAt, boolean recurringMonthly) {
        String url = "https://calendar.google.com/calendar/render?action=TEMPLATE"
                + "&text=" + encode(title)
                + "&details=" + encode(description)
                + "&location=" + encode(city)
                + "&dates=" + startAt.format(CALENDAR_FORMAT) + "/" + endAt.format(CALENDAR_FORMAT);
        if (recurringMonthly) {
            url += "&recur=" + encode("RRULE:FREQ=MONTHLY;COUNT=12");
        }
        return url;
    }

    private String buildReason(String dominantRisk, String caseTitle, String caseDescription) {
        String context = (caseTitle == null ? "" : caseTitle) + " " + (caseDescription == null ? "" : caseDescription);
        String normalized = context.toLowerCase();
        if ("Voiture".equals(dominantRisk)) {
            if (normalized.contains("pneu")) return "des signaux autour des pneus ou du roulage reviennent";
            if (normalized.contains("frein")) return "des signaux autour du freinage reviennent";
            return "ce cas ressemble a un risque voiture recurrent a prevenir";
        }
        if ("Sante".equals(dominantRisk)) {
            if (normalized.contains("analyse") || normalized.contains("labo")) return "un suivi biologique semble utile pour anticiper une rechute";
            if (normalized.contains("denta")) return "un controle specialise peut eviter une aggravation";
            return "ce cas ressemble a un risque sante recurrent a surveiller";
        }
        if ("Maison".equals(dominantRisk)) {
            if (normalized.contains("fuite")) return "une verification preventive peut eviter une nouvelle fuite";
            if (normalized.contains("electric")) return "un controle electrique preventif peut eviter une nouvelle panne";
            return "ce cas ressemble a un risque maison recurrent a prevenir";
        }
        return "un suivi preventif peut reduire le risque de repetition";
    }

    private List<String> buildPlaceTypes(String dominantRisk, String caseTitle, String caseDescription) {
        String context = ((caseTitle == null ? "" : caseTitle) + " " + (caseDescription == null ? "" : caseDescription)).toLowerCase();
        if ("Voiture".equals(dominantRisk)) {
            if (context.contains("pneu")) return List.of("garage d'entretien", "centre pneu", "diagnostic auto");
            if (context.contains("frein")) return List.of("garage freinage", "diagnostic auto", "centre entretien");
            return List.of("garage d'entretien", "diagnostic auto", "centre vidange");
        }
        if ("Sante".equals(dominantRisk)) {
            if (context.contains("analyse") || context.contains("labo")) return List.of("laboratoire", "cabinet medical", "centre d'imagerie");
            if (context.contains("denta")) return List.of("cabinet dentaire", "centre radio dentaire", "cabinet medical");
            return List.of("cabinet medical", "laboratoire", "clinique");
        }
        if ("Maison".equals(dominantRisk)) {
            if (context.contains("fuite")) return List.of("plombier", "maintenance maison", "magasin de reparation");
            if (context.contains("electric")) return List.of("electricien", "maintenance maison", "diagnostic technique");
            return List.of("maintenance maison", "plombier", "electricien");
        }
        return List.of("cabinet de conseil", "maintenance preventive", "service specialise");
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
