package pi.savings.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;

public final class SavingsValidation {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000");

    private SavingsValidation() {
    }

    public static BigDecimal parseRequiredMoney(String rawValue, String emptyMessage) {
        String safeValue = rawValue == null ? "" : rawValue.trim().replace(',', '.');
        if (safeValue.isEmpty()) {
            throw new SavingsValidationException(emptyMessage);
        }

        try {
            return new BigDecimal(safeValue).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException exception) {
            throw new SavingsValidationException("Valeur numerique invalide.");
        }
    }

    public static void validateDeposit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new SavingsValidationException("Le montant du depot doit etre > 0.");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new SavingsValidationException("Montant trop grand. Maximum: 1000000.");
        }
    }

    public static void validateInterestRate(BigDecimal rate) {
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(new BigDecimal("100")) > 0) {
            throw new SavingsValidationException("Le taux d'interet doit etre entre 0 et 100.");
        }
    }

    public static int parseRequiredInt(String rawValue, String emptyMessage, String invalidMessage) {
        String safeValue = rawValue == null ? "" : rawValue.trim();
        if (safeValue.isEmpty()) {
            throw new SavingsValidationException(emptyMessage);
        }

        try {
            return Integer.parseInt(safeValue);
        } catch (NumberFormatException exception) {
            throw new SavingsValidationException(invalidMessage);
        }
    }

    public static Date parseRequiredDate(String rawValue, String emptyMessage) {
        String safeValue = rawValue == null ? "" : rawValue.trim();
        if (safeValue.isEmpty()) {
            throw new SavingsValidationException(emptyMessage);
        }

        try {
            return Date.valueOf(LocalDate.parse(safeValue));
        } catch (RuntimeException exception) {
            throw new SavingsValidationException("Date invalide. Format attendu: yyyy-mm-dd.");
        }
    }

    public static void validateGoal(String name, BigDecimal target, BigDecimal current, Date deadline, int priority) {
        String safeName = name == null ? "" : name.trim();
        if (safeName.isEmpty()) {
            throw new SavingsValidationException("Le nom du goal est obligatoire.");
        }
        if (safeName.length() < 3 || safeName.length() > 60) {
            throw new SavingsValidationException("Le nom du goal doit contenir entre 3 et 60 caracteres.");
        }
        if (target.compareTo(BigDecimal.ZERO) <= 0) {
            throw new SavingsValidationException("Le montant cible doit etre > 0.");
        }
        if (target.compareTo(MAX_AMOUNT) > 0) {
            throw new SavingsValidationException("Le montant cible ne doit pas depasser 1000000.");
        }
        if (current.compareTo(BigDecimal.ZERO) < 0) {
            throw new SavingsValidationException("Le montant actuel ne peut pas etre negatif.");
        }
        if (current.compareTo(target) > 0) {
            throw new SavingsValidationException("Le montant actuel ne peut pas depasser le montant cible.");
        }
        if (deadline == null) {
            throw new SavingsValidationException("La date limite est obligatoire.");
        }
        if (deadline.toLocalDate().isBefore(LocalDate.now())) {
            throw new SavingsValidationException("La date limite doit etre aujourd'hui ou dans le futur.");
        }
        if (priority < 1 || priority > 5) {
            throw new SavingsValidationException("La priorite doit etre comprise entre 1 et 5.");
        }
    }

    public static void validateContribution(BigDecimal amount, BigDecimal availableBalance, BigDecimal remainingTarget) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new SavingsValidationException("Le montant de contribution doit etre > 0.");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new SavingsValidationException("Le montant de contribution ne doit pas depasser 1000000.");
        }
        if (amount.compareTo(availableBalance) > 0) {
            throw new SavingsValidationException("La contribution depasse le solde disponible.");
        }
        if (remainingTarget.compareTo(BigDecimal.ZERO) <= 0) {
            throw new SavingsValidationException("Ce goal est deja atteint.");
        }
        if (amount.compareTo(remainingTarget) > 0) {
            throw new SavingsValidationException("La contribution depasse le montant restant a atteindre.");
        }
    }

    public static final class SavingsValidationException extends RuntimeException {
        public SavingsValidationException(String message) {
            super(message);
        }
    }
}
