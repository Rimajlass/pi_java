package pi.services.ImprevusCasreelService;

public record CaseFundingAdvice(
        double emergencyFundBalance,
        double savingBalance,
        String suggestion,
        boolean suggestedRefusal
) {
}
