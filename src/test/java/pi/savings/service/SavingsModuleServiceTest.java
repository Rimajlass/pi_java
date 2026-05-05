package pi.savings.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pi.savings.repository.SavingsTransactionRepository;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SavingsModuleServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCalculateHistoryStatsForDepositsAndContributions() {
        SavingsModuleService service = new SavingsModuleService(false);

        SavingsModuleService.HistoryStats stats = service.calculateHistoryStats(List.of(
                transactionRow(1, "EPARGNE", "2026-04-15T08:00:00", "120.00", "monthly savings"),
                transactionRow(2, "GOAL_CONTRIBUTION", "2026-04-16T09:30:00", "70.00", "bike goal"),
                transactionRow(3, "EPARGNE", "2026-04-17T10:15:00", "310.00", "bonus")
        ));

        assertEquals(3, stats.transactionCount());
        assertEquals(2, stats.depositCount());
        assertEquals(1, stats.goalContributionCount());
        assertEquals(new BigDecimal("430.00"), stats.totalDeposited());
        assertEquals(new BigDecimal("70.00"), stats.totalContributedToGoals());
        assertEquals(new BigDecimal("166.67"), stats.averageAmount());
        assertEquals("2026-04-17", stats.latestTransactionDate());
    }

    @Test
    void shouldReturnZeroHistoryStatsWhenThereAreNoTransactions() {
        SavingsModuleService service = new SavingsModuleService(false);

        SavingsModuleService.HistoryStats stats = service.calculateHistoryStats(List.of());

        assertEquals(0, stats.transactionCount());
        assertEquals(0, stats.depositCount());
        assertEquals(0, stats.goalContributionCount());
        assertEquals(new BigDecimal("0.00"), stats.totalDeposited());
        assertEquals(new BigDecimal("0.00"), stats.totalContributedToGoals());
        assertEquals(new BigDecimal("0.00"), stats.averageAmount());
        assertEquals("--/--/----", stats.latestTransactionDate());
    }

    @Test
    void shouldExportHistoryToCsvAndPdf() throws Exception {
        SavingsModuleService service = new SavingsModuleService(false);
        List<SavingsTransactionRepository.TransactionRow> rows = List.of(
                transactionRow(1, "EPARGNE", "2026-04-15T08:00:00", "120.00", "monthly savings"),
                transactionRow(2, "GOAL_CONTRIBUTION", "2026-04-16T09:30:00", "70.00", "bike goal")
        );

        Path csv = service.exportHistoryCsv(rows, tempDir);
        Path pdf = service.exportHistoryPdf(rows, tempDir);

        assertTrue(Files.exists(csv));
        assertTrue(Files.exists(pdf));
        assertTrue(Files.readString(csv, StandardCharsets.UTF_8).contains("monthly savings"));
        assertTrue(Files.readString(csv, StandardCharsets.UTF_8).contains("Goal Contribution"));
        assertTrue(Files.size(pdf) > 0);
        assertTrue(new String(Files.readAllBytes(pdf), StandardCharsets.ISO_8859_1).startsWith("%PDF-1.4"));
    }

    @Test
    void shouldCalculateGoalStatsAndExportGoals() throws Exception {
        SavingsModuleService service = new SavingsModuleService(false);
        List<SavingsModuleService.GoalSnapshot> goals = List.of(
                goalSnapshot(1, "Bike", "1000.00", "1000.00",  LocalDateTime.parse("2026-06-01T00:00:00").toLocalDate(), 3, 100),
                goalSnapshot(2, "Trip", "2000.00", "500.00", LocalDateTime.parse("2026-05-15T00:00:00").toLocalDate(), 4, 25)
        );

        SavingsModuleService.GoalStats stats = service.calculateGoalStats(goals);
        Path csv = service.exportGoalsCsv(goals, tempDir);
        Path pdf = service.exportGoalsPdf(goals, tempDir);

        assertEquals(2, stats.goalCount());
        assertEquals(1, stats.completedGoalCount());
        assertEquals(50, stats.completionRate());
        assertEquals(new BigDecimal("3000.00"), stats.totalTarget());
        assertEquals(new BigDecimal("1500.00"), stats.totalCurrent());
        assertEquals(new BigDecimal("1500.00"), stats.remainingAmount());
        assertEquals("2026-05-15", stats.nearestDeadline());
        assertTrue(Files.readString(csv, StandardCharsets.UTF_8).contains("Bike"));
        assertTrue(Files.readString(csv, StandardCharsets.UTF_8).contains("Trip"));
        String pdfContent = new String(Files.readAllBytes(pdf), StandardCharsets.ISO_8859_1);
        assertTrue(pdfContent.contains("Goals Report"));
        assertTrue(pdfContent.contains("Bike"));
        assertTrue(pdfContent.contains("Trip"));
    }

    @Test
    void shouldCalculateGoalStatsForEmptyGoalList() {
        SavingsModuleService service = new SavingsModuleService(false);

        SavingsModuleService.GoalStats stats = service.calculateGoalStats(List.of());

        assertEquals(0, stats.goalCount());
        assertEquals(0, stats.completedGoalCount());
        assertEquals(0, stats.completionRate());
        assertEquals(new BigDecimal("0.00"), stats.totalTarget());
        assertEquals(new BigDecimal("0.00"), stats.totalCurrent());
        assertEquals(new BigDecimal("0.00"), stats.remainingAmount());
        assertEquals("--/--/----", stats.nearestDeadline());
    }

    private static SavingsModuleService.GoalSnapshot goalSnapshot(
            int id,
            String name,
            String target,
            String current,
            java.time.LocalDate deadline,
            int priority,
            double progress
    ) {
        return new SavingsModuleService.GoalSnapshot(
                id,
                name,
                new BigDecimal(target),
                new BigDecimal(current),
                deadline,
                priority,
                progress
        );
    }

    private static SavingsTransactionRepository.TransactionRow transactionRow(
            int id,
            String type,
            String dateTime,
            String amount,
            String description
    ) {
        return new SavingsTransactionRepository.TransactionRow(
                id,
                type,
                LocalDateTime.parse(dateTime),
                new BigDecimal(amount),
                description,
                "SAVINGS",
                1
        );
    }
}
