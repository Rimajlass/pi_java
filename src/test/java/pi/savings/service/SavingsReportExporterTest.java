package pi.savings.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pi.savings.repository.SavingsTransactionRepository;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SavingsReportExporterTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldWriteHistoryCsvWithEscapedValues() throws Exception {
        Path csv = SavingsReportExporter.writeHistoryCsv(List.of(
                transactionRow(1, "EPARGNE", "2026-04-15T08:00:00", "120.00", "salary bonus, april", "SAVINGS", 1),
                transactionRow(2, "GOAL_CONTRIBUTION", "2026-04-16T09:30:00", "70.00", "goal \"bike\"", "GOALS", 1)
        ), tempDir);

        String content = Files.readString(csv, StandardCharsets.UTF_8);

        assertTrue(content.contains("date,type,amount,description,module_source"));
        assertTrue(!content.contains("user_id"));
        assertTrue(content.contains("\"salary bonus, april\""));
        assertTrue(content.contains("\"goal \"\"bike\"\"\""));
    }

    @Test
    void shouldWriteHistoryPdfForEmptyRows() throws Exception {
        Path pdf = SavingsReportExporter.writeHistoryPdf(
                List.of(),
                new SavingsModuleService.HistoryStats(
                        0,
                        0,
                        0,
                        new BigDecimal("0.00"),
                        new BigDecimal("0.00"),
                        new BigDecimal("0.00"),
                        "--/--/----"
                ),
                tempDir
        );

        String content = new String(Files.readAllBytes(pdf), StandardCharsets.ISO_8859_1);

        assertTrue(content.startsWith("%PDF-1.4"));
        assertTrue(content.contains("Savings History Report"));
        assertTrue(content.contains("No rows available for the current filters."));
    }

    @Test
    void shouldWriteGoalsCsvAndPdfWithNullDeadline() throws Exception {
        List<SavingsModuleService.GoalSnapshot> goals = List.of(
                new SavingsModuleService.GoalSnapshot(
                        3,
                        "Laptop Fund",
                        new BigDecimal("3000.00"),
                        new BigDecimal("800.00"),
                        null,
                        2,
                        27
                )
        );

        Path csv = SavingsReportExporter.writeGoalsCsv(goals, tempDir);
        Path pdf = SavingsReportExporter.writeGoalsPdf(
                goals,
                new SavingsModuleService.GoalStats(
                        1,
                        0,
                        0,
                        new BigDecimal("3000.00"),
                        new BigDecimal("800.00"),
                        new BigDecimal("2200.00"),
                        "--/--/----"
                ),
                tempDir
        );

        String csvContent = Files.readString(csv, StandardCharsets.UTF_8);
        String pdfContent = new String(Files.readAllBytes(pdf), StandardCharsets.ISO_8859_1);

        assertTrue(csvContent.contains("progress_percent"));
        assertTrue(!csvContent.contains("id,name"));
        assertTrue(csvContent.contains("\"null\""));
        assertTrue(pdfContent.contains("Goals Report"));
        assertTrue(pdfContent.contains("Laptop Fund"));
        assertTrue(pdfContent.contains("--/--/----"));
    }

    @Test
    void shouldWriteGoalsPdfWithRemainingAmountColumn() throws Exception {
        List<SavingsModuleService.GoalSnapshot> goals = List.of(
                new SavingsModuleService.GoalSnapshot(
                        1,
                        "Trip",
                        new BigDecimal("2000.00"),
                        new BigDecimal("500.00"),
                        LocalDate.of(2026, 8, 20),
                        4,
                        25
                )
        );

        Path pdf = SavingsReportExporter.writeGoalsPdf(
                goals,
                new SavingsModuleService.GoalStats(
                        1,
                        0,
                        0,
                        new BigDecimal("2000.00"),
                        new BigDecimal("500.00"),
                        new BigDecimal("1500.00"),
                        "2026-08-20"
                ),
                tempDir
        );

        String pdfContent = new String(Files.readAllBytes(pdf), StandardCharsets.ISO_8859_1);

        assertTrue(pdfContent.contains("Remaining"));
        assertTrue(pdfContent.contains("1500 TND"));
        assertTrue(pdfContent.contains("25%"));
    }

    private static SavingsTransactionRepository.TransactionRow transactionRow(
            int id,
            String type,
            String dateTime,
            String amount,
            String description,
            String moduleSource,
            int userId
    ) {
        return new SavingsTransactionRepository.TransactionRow(
                id,
                type,
                LocalDateTime.parse(dateTime),
                new BigDecimal(amount),
                description,
                moduleSource,
                userId
        );
    }
}
