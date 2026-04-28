package pi.savings.service;

import pi.entities.CalendarEvent;
import pi.savings.repository.SavingAccountRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CsvExportService {

    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public Path exportSavingAccounts(
            List<SavingAccountRepository.SavingAccountDetails> accounts,
            CurrencyRateService.RateSnapshot rateSnapshot,
            Path exportDirectory
    ) {
        try {
            Files.createDirectories(exportDirectory);
            Path file = exportDirectory.resolve("saving-accounts-" + LocalDateTime.now().format(FILE_STAMP) + ".csv");
            List<String> lines = new ArrayList<>();
            lines.add("balance,interest_rate,creation_date,user_name,user_email,selected_currency,converted_balance_tnd,rate_to_tnd,rate_date");
            for (SavingAccountRepository.SavingAccountDetails account : accounts) {
                lines.add(String.join(",",
                        csv(money(account.balance())),
                        csv(percent(account.interestRate())),
                        csv(account.createdOn()),
                        csv(account.userName()),
                        csv(account.userEmail()),
                        csv(rateSnapshot.currency()),
                        csv(convert(account.balance(), rateSnapshot)),
                        csv(rateSnapshot.rateToTnd()),
                        csv(rateSnapshot.rateDate())
                ));
            }
            Files.write(file, lines, StandardCharsets.UTF_8);
            return file;
        } catch (IOException exception) {
            throw new ExportException("Impossible d'exporter les saving accounts en CSV.", exception);
        }
    }

    public Path exportGoals(
            List<SavingsStatsService.GoalDetails> goals,
            CurrencyRateService.RateSnapshot rateSnapshot,
            List<CalendarEvent> holidays,
            Path exportDirectory
    ) {
        try {
            Files.createDirectories(exportDirectory);
            Path file = exportDirectory.resolve("financial-goals-" + LocalDateTime.now().format(FILE_STAMP) + ".csv");
            List<String> lines = new ArrayList<>();
            lines.add("goal_name,target_amount,current_amount,remaining_amount,deadline,priority,status,progress_percentage,user_name,user_email,selected_currency,converted_target_amount_tnd,converted_current_amount_tnd,holiday_deadline_warning,rate_to_tnd,rate_date");
            for (SavingsStatsService.GoalDetails goal : goals) {
                BigDecimal remaining = goal.targetAmount().subtract(goal.currentAmount()).max(BigDecimal.ZERO);
                lines.add(String.join(",",
                        csv(goal.name()),
                        csv(money(goal.targetAmount())),
                        csv(money(goal.currentAmount())),
                        csv(money(remaining)),
                        csv(goal.deadline()),
                        csv(goal.priority()),
                        csv(SavingsStatsService.goalStatus(goal, LocalDate.now())),
                        csv(SavingsStatsService.progress(goal)),
                        csv(goal.userName()),
                        csv(goal.userEmail()),
                        csv(rateSnapshot.currency()),
                        csv(convert(goal.targetAmount(), rateSnapshot)),
                        csv(convert(goal.currentAmount(), rateSnapshot)),
                        csv(describeHoliday(goal.deadline(), holidays)),
                        csv(rateSnapshot.rateToTnd()),
                        csv(rateSnapshot.rateDate())
                ));
            }
            Files.write(file, lines, StandardCharsets.UTF_8);
            return file;
        } catch (IOException exception) {
            throw new ExportException("Impossible d'exporter les financial goals en CSV.", exception);
        }
    }

    private String describeHoliday(java.time.LocalDate deadline, List<CalendarEvent> holidays) {
        return holidays.stream()
                .anyMatch(event -> deadline != null && Math.abs(java.time.temporal.ChronoUnit.DAYS.between(deadline, event.getDate())) <= 1)
                ? "deadline_near_holiday"
                : "clear";
    }

    private BigDecimal convert(BigDecimal amount, CurrencyRateService.RateSnapshot rateSnapshot) {
        return BigDecimal.valueOf(amount.doubleValue() * rateSnapshot.rateToTnd()).setScale(2, RoundingMode.HALF_UP);
    }

    private String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String percent(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String csv(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    public static final class ExportException extends RuntimeException {
        public ExportException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
