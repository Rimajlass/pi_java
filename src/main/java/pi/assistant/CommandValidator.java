package pi.assistant;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class CommandValidator {

    private CommandValidator() {
    }

    public static BigDecimal parseAmount(String rawAmount) {
        if (rawAmount == null || rawAmount.isBlank()) {
            return null;
        }

        try {
            BigDecimal amount = new BigDecimal(rawAmount.trim().replace(',', '.'));
            return amount.signum() > 0 ? amount : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public static LocalDate parseDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(rawDate.trim());
        } catch (DateTimeParseException exception) {
            return null;
        }
    }
}
