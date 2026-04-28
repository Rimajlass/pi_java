package pi.savings.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CalendarEventDTO(
        LocalDate date,
        String title,
        String type,
        String source,
        BigDecimal amount,
        String goalName,
        String description
) {
}
