package pi.savings.ui;

import org.junit.jupiter.api.Test;
import pi.savings.repository.SavingsTransactionRepository;
import pi.savings.service.SavingsModuleService;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SavingsUiControllerTest {

    @Test
    void shouldInitializeAndApplySavingsCrudOperations() {
        SavingsModuleService.DashboardSnapshot initialSnapshot = dashboardSnapshot(
                new BigDecimal("1200.00"),
                new BigDecimal("4.50"),
                List.of(goalSnapshot(10, "Car", "3000", "900", LocalDate.of(2026, 6, 1), 4, 30)),
                List.of(transactionRow(1, "EPARGNE", "2026-04-15T08:00:00", "120.00", "monthly savings"))
        );
        SavingsModuleService.DashboardSnapshot afterDeposit = dashboardSnapshot(
                new BigDecimal("1500.00"),
                new BigDecimal("4.50"),
                initialSnapshot.goals(),
                List.of(
                        transactionRow(2, "EPARGNE", "2026-04-16T09:00:00", "300.00", "bonus"),
                        transactionRow(1, "EPARGNE", "2026-04-15T08:00:00", "120.00", "monthly savings")
                )
        );
        SavingsModuleService.DashboardSnapshot afterRateUpdate = dashboardSnapshot(
                new BigDecimal("1500.00"),
                new BigDecimal("5.25"),
                initialSnapshot.goals(),
                afterDeposit.transactions()
        );

        FakeSavingsModuleService service = new FakeSavingsModuleService(
                initialSnapshot,
                afterDeposit,
                afterRateUpdate,
                initialSnapshot,
                initialSnapshot,
                initialSnapshot,
                initialSnapshot
        );
        SavingsUiController controller = new SavingsUiController(service);

        SavingsUiController.OperationResult init = controller.initialize();
        SavingsUiController.OperationResult deposit = controller.safeDeposit("300", "bonus");
        SavingsUiController.OperationResult updateRate = controller.safeUpdateInterestRate("5.25");

        assertTrue(init.success());
        assertTrue(deposit.success());
        assertTrue(updateRate.success());
        assertEquals(new BigDecimal("1500.00"), controller.getSnapshot().balance());
        assertEquals(new BigDecimal("5.25"), controller.getSnapshot().interestRate());
        assertEquals(1, service.initializeCalls);
        assertEquals(1, service.depositCalls);
        assertEquals(1, service.updateRateCalls);
    }

    @Test
    void shouldApplyGoalsCrudOperations() {
        SavingsModuleService.DashboardSnapshot initialSnapshot = dashboardSnapshot(
                new BigDecimal("2000.00"),
                new BigDecimal("3.50"),
                List.of(
                        goalSnapshot(1, "Bike", "1200", "200", LocalDate.of(2026, 7, 1), 2, 17),
                        goalSnapshot(2, "Laptop", "2500", "600", LocalDate.of(2026, 5, 20), 5, 24)
                ),
                List.of()
        );
        SavingsModuleService.DashboardSnapshot afterCreate = dashboardSnapshot(
                initialSnapshot.balance(),
                initialSnapshot.interestRate(),
                List.of(
                        goalSnapshot(1, "Bike", "1200", "200", LocalDate.of(2026, 7, 1), 2, 17),
                        goalSnapshot(2, "Laptop", "2500", "600", LocalDate.of(2026, 5, 20), 5, 24),
                        goalSnapshot(3, "Trip", "1800", "0", LocalDate.of(2026, 8, 15), 3, 0)
                ),
                List.of()
        );
        SavingsModuleService.DashboardSnapshot afterUpdate = dashboardSnapshot(
                initialSnapshot.balance(),
                initialSnapshot.interestRate(),
                List.of(
                        goalSnapshot(1, "Bike", "1200", "200", LocalDate.of(2026, 7, 1), 2, 17),
                        goalSnapshot(2, "Laptop Pro", "2600", "900", LocalDate.of(2026, 5, 25), 4, 35),
                        goalSnapshot(3, "Trip", "1800", "0", LocalDate.of(2026, 8, 15), 3, 0)
                ),
                List.of()
        );
        SavingsModuleService.DashboardSnapshot afterContribution = dashboardSnapshot(
                new BigDecimal("1700.00"),
                initialSnapshot.interestRate(),
                List.of(
                        goalSnapshot(1, "Bike", "1200", "200", LocalDate.of(2026, 7, 1), 2, 17),
                        goalSnapshot(2, "Laptop Pro", "2600", "1200", LocalDate.of(2026, 5, 25), 4, 46),
                        goalSnapshot(3, "Trip", "1800", "0", LocalDate.of(2026, 8, 15), 3, 0)
                ),
                List.of()
        );
        SavingsModuleService.DashboardSnapshot afterDelete = dashboardSnapshot(
                new BigDecimal("1700.00"),
                initialSnapshot.interestRate(),
                List.of(
                        goalSnapshot(1, "Bike", "1200", "200", LocalDate.of(2026, 7, 1), 2, 17),
                        goalSnapshot(2, "Laptop Pro", "2600", "1200", LocalDate.of(2026, 5, 25), 4, 46)
                ),
                List.of()
        );

        FakeSavingsModuleService service = new FakeSavingsModuleService(
                initialSnapshot,
                initialSnapshot,
                initialSnapshot,
                afterCreate,
                afterUpdate,
                afterDelete,
                afterContribution
        );
        SavingsUiController controller = new SavingsUiController(service);
        controller.initialize();

        SavingsUiController.OperationResult create = controller.safeCreateGoal("Trip", "1800", "0", "2026-08-15", "3");
        SavingsUiController.OperationResult update = controller.safeUpdateGoal(2, "Laptop Pro", "2600", "900", "2026-05-25", "4");
        SavingsUiController.OperationResult contribute = controller.safeContributeToGoal(2, "300");
        SavingsUiController.OperationResult delete = controller.safeDeleteGoal(3);

        assertTrue(create.success());
        assertTrue(update.success());
        assertTrue(contribute.success());
        assertTrue(delete.success());
        assertEquals(2, controller.getSnapshot().goals().size());
        assertEquals("Laptop Pro", controller.getSnapshot().goals().get(1).name());
        assertEquals(1, service.createGoalCalls);
        assertEquals(1, service.updateGoalCalls);
        assertEquals(1, service.contributeCalls);
        assertEquals(1, service.deleteGoalCalls);
    }

    @Test
    void shouldFilterAndSortGoalsConsistently() {
        SavingsUiController controller = new SavingsUiController(new FakeSavingsModuleService(
                dashboardSnapshot(
                        new BigDecimal("1000.00"),
                        new BigDecimal("2.50"),
                        List.of(
                                goalSnapshot(1, "Bike", "1000", "100", LocalDate.of(2026, 7, 1), 3, 10),
                                goalSnapshot(2, "Bike Pro", "2000", "300", LocalDate.of(2026, 6, 1), 3, 15),
                                goalSnapshot(3, "Trip", "1500", "500", LocalDate.of(2026, 5, 1), 5, 33)
                        ),
                        List.of()
                ),
                null, null, null, null, null, null
        ));
        controller.initialize();

        List<SavingsModuleService.GoalSnapshot> result = controller.filterAndSortGoals("bike", "Priority", "Descending");

        assertEquals(List.of(2, 1), result.stream().map(SavingsModuleService.GoalSnapshot::id).toList());
    }

    @Test
    void shouldFilterAndSortHistoryConsistently() {
        SavingsUiController controller = new SavingsUiController(new FakeSavingsModuleService(
                dashboardSnapshot(
                        new BigDecimal("1000.00"),
                        new BigDecimal("2.50"),
                        List.of(),
                        List.of(
                                transactionRow(1, "EPARGNE", "2026-04-15T08:00:00", "120.00", "monthly savings"),
                                transactionRow(2, "EPARGNE", "2026-04-15T08:00:00", "120.00", "salary transfer"),
                                transactionRow(3, "GOAL_CONTRIBUTION", "2026-04-16T08:00:00", "70.00", "bike goal")
                        )
                ),
                null, null, null, null, null, null
        ));
        controller.initialize();

        List<SavingsTransactionRepository.TransactionRow> result = controller.filterAndSortHistory("", "Amount", "Ascending");

        assertEquals(List.of(3, 1, 2), result.stream().map(SavingsTransactionRepository.TransactionRow::id).toList());
        assertEquals(
                List.of(2),
                controller.filterAndSortHistory("salary", "Amount", "Ascending")
                        .stream()
                        .map(SavingsTransactionRepository.TransactionRow::id)
                        .toList()
        );
    }

    @Test
    void shouldExportFilteredHistory() throws Exception {
        SavingsUiController controller = new SavingsUiController(new FakeSavingsModuleService(
                dashboardSnapshot(
                        new BigDecimal("1000.00"),
                        new BigDecimal("2.50"),
                        List.of(),
                        List.of(
                                transactionRow(1, "EPARGNE", "2026-04-15T08:00:00", "120.00", "monthly savings"),
                                transactionRow(2, "GOAL_CONTRIBUTION", "2026-04-16T08:00:00", "70.00", "bike goal")
                        )
                ),
                null, null, null, null, null, null
        ));
        controller.initialize();
        Path exportDir = Files.createTempDirectory("savings-ui-export");

        SavingsUiController.OperationResult csvResult =
                controller.safeExportHistoryCsv("bike", "Amount", "Ascending", exportDir);
        SavingsUiController.OperationResult pdfResult =
                controller.safeExportHistoryPdf("bike", "Amount", "Ascending", exportDir);

        assertTrue(csvResult.success());
        assertTrue(pdfResult.success());
        assertEquals(2, Files.list(exportDir).count());
    }

    @Test
    void shouldExportFilteredGoals() throws Exception {
        SavingsUiController controller = new SavingsUiController(new FakeSavingsModuleService(
                dashboardSnapshot(
                        new BigDecimal("1000.00"),
                        new BigDecimal("2.50"),
                        List.of(
                                goalSnapshot(1, "Bike", "1000", "100", LocalDate.of(2026, 7, 1), 3, 10),
                                goalSnapshot(2, "Trip", "2000", "500", LocalDate.of(2026, 5, 1), 5, 25)
                        ),
                        List.of()
                ),
                null, null, null, null, null, null
        ));
        controller.initialize();
        Path exportDir = Files.createTempDirectory("goals-ui-export");

        SavingsUiController.OperationResult csvResult =
                controller.safeExportGoalsCsv("trip", "Priority", "Descending", exportDir);
        SavingsUiController.OperationResult pdfResult =
                controller.safeExportGoalsPdf("trip", "Priority", "Descending", exportDir);

        assertTrue(csvResult.success());
        assertTrue(pdfResult.success());
        assertEquals(2, Files.list(exportDir).count());
    }

    @Test
    void shouldReturnErrorWhenInitializationFails() {
        SavingsUiController controller = new SavingsUiController(new FailingSavingsModuleService());

        SavingsUiController.OperationResult init = controller.initialize();

        assertFalse(init.success());
        assertEquals("Impossible de charger le module Savings & Goals.", init.message());
        assertEquals(BigDecimal.ZERO, controller.getSnapshot().balance());
        assertTrue(controller.getSnapshot().goals().isEmpty());
    }

    private static SavingsModuleService.DashboardSnapshot dashboardSnapshot(
            BigDecimal balance,
            BigDecimal rate,
            List<SavingsModuleService.GoalSnapshot> goals,
            List<SavingsTransactionRepository.TransactionRow> transactions
    ) {
        return new SavingsModuleService.DashboardSnapshot(
                1,
                1,
                balance,
                rate,
                LocalDate.of(2026, 4, 1),
                goals.size(),
                0,
                goals.isEmpty() ? "--/--/----" : goals.get(0).deadline().toString(),
                goals,
                transactions
        );
    }

    private static SavingsModuleService.GoalSnapshot goalSnapshot(
            int id,
            String name,
            String target,
            String current,
            LocalDate deadline,
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

    private static final class FakeSavingsModuleService extends SavingsModuleService {
        private final SavingsModuleService.DashboardSnapshot initializeSnapshot;
        private final SavingsModuleService.DashboardSnapshot depositSnapshot;
        private final SavingsModuleService.DashboardSnapshot updateRateSnapshot;
        private final SavingsModuleService.DashboardSnapshot createGoalSnapshot;
        private final SavingsModuleService.DashboardSnapshot updateGoalSnapshot;
        private final SavingsModuleService.DashboardSnapshot deleteGoalSnapshot;
        private final SavingsModuleService.DashboardSnapshot contributeSnapshot;
        private int initializeCalls;
        private int depositCalls;
        private int updateRateCalls;
        private int createGoalCalls;
        private int updateGoalCalls;
        private int deleteGoalCalls;
        private int contributeCalls;

        private FakeSavingsModuleService(
                SavingsModuleService.DashboardSnapshot initializeSnapshot,
                SavingsModuleService.DashboardSnapshot depositSnapshot,
                SavingsModuleService.DashboardSnapshot updateRateSnapshot,
                SavingsModuleService.DashboardSnapshot createGoalSnapshot,
                SavingsModuleService.DashboardSnapshot updateGoalSnapshot,
                SavingsModuleService.DashboardSnapshot deleteGoalSnapshot,
                SavingsModuleService.DashboardSnapshot contributeSnapshot
        ) {
            super(false);
            this.initializeSnapshot = initializeSnapshot;
            this.depositSnapshot = depositSnapshot;
            this.updateRateSnapshot = updateRateSnapshot;
            this.createGoalSnapshot = createGoalSnapshot;
            this.updateGoalSnapshot = updateGoalSnapshot;
            this.deleteGoalSnapshot = deleteGoalSnapshot;
            this.contributeSnapshot = contributeSnapshot;
        }

        @Override
        public SavingsModuleService.DashboardSnapshot loadDashboard(int userId) {
            initializeCalls++;
            return initializeSnapshot;
        }

        @Override
        public SavingsModuleService.DashboardSnapshot saveDeposit(int userId, String amountText, String descriptionText) {
            depositCalls++;
            return depositSnapshot;
        }

        @Override
        public SavingsModuleService.DashboardSnapshot updateInterestRate(int userId, String rateText) {
            updateRateCalls++;
            return updateRateSnapshot;
        }

        @Override
        public SavingsModuleService.DashboardSnapshot createGoal(
                int userId,
                String nameText,
                String targetText,
                String currentText,
                String deadlineText,
                String priorityText
        ) {
            createGoalCalls++;
            return createGoalSnapshot;
        }

        @Override
        public SavingsModuleService.DashboardSnapshot updateGoal(
                int userId,
                int goalId,
                String nameText,
                String targetText,
                String currentText,
                String deadlineText,
                String priorityText
        ) {
            updateGoalCalls++;
            return updateGoalSnapshot;
        }

        @Override
        public SavingsModuleService.DashboardSnapshot deleteGoal(int userId, int goalId) {
            deleteGoalCalls++;
            return deleteGoalSnapshot;
        }

        @Override
        public SavingsModuleService.DashboardSnapshot contributeToGoal(int userId, int goalId, String amountText) {
            contributeCalls++;
            return contributeSnapshot;
        }
    }

    private static final class FailingSavingsModuleService extends SavingsModuleService {
        private FailingSavingsModuleService() {
            super(false);
        }

        @Override
        public SavingsModuleService.DashboardSnapshot loadDashboard(int userId) {
            throw new SavingsModuleService.SavingsModuleException(
                    "Impossible de charger le module Savings & Goals.",
                    null
            );
        }
    }
}
