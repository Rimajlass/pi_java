package pi.savings.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pi.entities.FinancialGoal;
import pi.entities.SavingAccount;
import pi.savings.repository.FinancialGoalRepository;
import pi.savings.repository.SavingAccountRepository;
import pi.savings.repository.SavingsTransactionRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavingsModuleServiceMockitoTest {

    @Mock
    private SavingAccountRepository savingAccountRepository;

    @Mock
    private FinancialGoalRepository financialGoalRepository;

    @Mock
    private SavingsTransactionRepository savingsTransactionRepository;

    @Mock
    private Connection connection;

    private SavingsModuleService service;

    @BeforeEach
    void setUp() {
        service = new SavingsModuleService(
                savingAccountRepository,
                financialGoalRepository,
                savingsTransactionRepository
        );
    }

    @Test
    void shouldLoadDashboardUsingExistingAccountAndComputeIndicators() throws Exception {
        SavingAccount account = savingAccount(5, 1, "1500.00", "3.50", LocalDate.of(2026, 4, 1));
        FinancialGoal completedGoal = goal(10, 5, "Trip", "1000.00", "1000.00", LocalDate.of(2026, 6, 10), 3);
        FinancialGoal activeGoal = goal(11, 5, "Car", "4000.00", "1000.00", LocalDate.of(2026, 5, 20), 5);

        when(savingAccountRepository.findLatestByUserId(1)).thenReturn(Optional.of(account));
        when(financialGoalRepository.findBySavingAccountId(5)).thenReturn(List.of(completedGoal, activeGoal));
        when(savingsTransactionRepository.findSavingsHistoryByUserId(1)).thenReturn(List.of(
                transactionRow(1, "EPARGNE", "2026-04-15T08:00:00", "200.00", "salary"),
                transactionRow(2, "GOAL_CONTRIBUTION", "2026-04-16T08:30:00", "50.00", "goal contribution")
        ));

        SavingsModuleService.DashboardSnapshot snapshot = service.loadDashboard(1);

        assertEquals(5, snapshot.accountId());
        assertEquals(new BigDecimal("1500.00"), snapshot.balance());
        assertEquals(new BigDecimal("3.50"), snapshot.interestRate());
        assertEquals(1, snapshot.activeGoals());
        assertEquals(63, snapshot.averageProgress());
        assertEquals("2026-05-20", snapshot.nearestDeadline());
        assertEquals(2, snapshot.goals().size());
        assertEquals(2, snapshot.transactions().size());
        verify(savingAccountRepository, never()).createDefaultAccount(anyInt());
    }

    @Test
    void shouldCreateDefaultAccountWhenNoAccountExists() throws Exception {
        SavingAccount createdAccount = savingAccount(9, 7, "0.00", "0.00", LocalDate.of(2026, 4, 15));

        when(savingAccountRepository.findLatestByUserId(7)).thenReturn(Optional.empty());
        when(savingAccountRepository.createDefaultAccount(7)).thenReturn(createdAccount);
        when(financialGoalRepository.findBySavingAccountId(9)).thenReturn(List.of());
        when(savingsTransactionRepository.findSavingsHistoryByUserId(7)).thenReturn(List.of());

        SavingsModuleService.DashboardSnapshot snapshot = service.loadDashboard(7);

        assertEquals(9, snapshot.accountId());
        assertEquals(0, snapshot.activeGoals());
        assertTrue(snapshot.goals().isEmpty());
        verify(savingAccountRepository).createDefaultAccount(7);
    }

    @Test
    void shouldSaveDepositCommitTransactionAndUseDefaultDescription() throws Exception {
        SavingAccount account = savingAccount(5, 1, "1500.00", "3.50", LocalDate.of(2026, 4, 1));

        when(savingAccountRepository.findLatestByUserId(1)).thenReturn(Optional.of(account));
        when(savingAccountRepository.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        when(financialGoalRepository.findBySavingAccountId(5)).thenReturn(List.of());
        when(savingsTransactionRepository.findSavingsHistoryByUserId(1)).thenReturn(List.of());

        SavingsModuleService.DashboardSnapshot snapshot = service.saveDeposit(1, "250", " ");

        assertEquals(new BigDecimal("1500.00"), snapshot.balance());
        verify(connection, atLeastOnce()).setAutoCommit(false);
        verify(savingAccountRepository).addToBalance(5, new BigDecimal("250.00"));
        verify(savingsTransactionRepository).insertDeposit(1, new BigDecimal("250.00"), "Deposit", connection);
        verify(connection).commit();
        verify(connection).setAutoCommit(true);
    }

    @Test
    void shouldRollbackDepositWhenTransactionInsertFails() throws Exception {
        SavingAccount account = savingAccount(5, 1, "1500.00", "3.50", LocalDate.of(2026, 4, 1));

        when(savingAccountRepository.findLatestByUserId(1)).thenReturn(Optional.of(account));
        when(savingAccountRepository.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(false);
        doThrow(new SQLException("insert failed")).when(savingsTransactionRepository)
                .insertDeposit(anyInt(), any(BigDecimal.class), anyString(), any(Connection.class));

        SavingsModuleService.SavingsModuleException exception = assertThrows(
                SavingsModuleService.SavingsModuleException.class,
                () -> service.saveDeposit(1, "250", "bonus")
        );

        assertEquals("Impossible d'enregistrer le depot.", exception.getMessage());
        verify(connection).rollback();
        verify(connection, atLeastOnce()).setAutoCommit(false);
    }

    @Test
    void shouldUpdateInterestRateForExistingAccount() throws Exception {
        SavingAccount account = savingAccount(8, 1, "900.00", "2.00", LocalDate.of(2026, 4, 3));

        when(savingAccountRepository.findLatestByUserId(1)).thenReturn(Optional.of(account));
        when(financialGoalRepository.findBySavingAccountId(8)).thenReturn(List.of());
        when(savingsTransactionRepository.findSavingsHistoryByUserId(1)).thenReturn(List.of());

        service.updateInterestRate(1, "4.25");

        verify(savingAccountRepository).updateInterestRate(8, new BigDecimal("4.25"));
    }

    @Test
    void shouldCreateGoalForSavingAccount() throws Exception {
        SavingAccount account = savingAccount(5, 1, "1500.00", "3.50", LocalDate.of(2026, 4, 1));

        when(savingAccountRepository.findLatestByUserId(1)).thenReturn(Optional.of(account));
        when(financialGoalRepository.findBySavingAccountId(5)).thenReturn(List.of());
        when(savingsTransactionRepository.findSavingsHistoryByUserId(1)).thenReturn(List.of());

        service.createGoal(1, "Emergency Fund", "3000", "500", "2026-12-01", "4");

        verify(financialGoalRepository).insert(any(FinancialGoal.class));
    }

    @Test
    void shouldRejectGoalUpdateWhenGoalDoesNotExist() throws Exception {
        SavingAccount account = savingAccount(5, 1, "1500.00", "3.50", LocalDate.of(2026, 4, 1));

        when(savingAccountRepository.findLatestByUserId(1)).thenReturn(Optional.of(account));
        when(financialGoalRepository.findByIdAndSavingAccountId(99, 5)).thenReturn(Optional.empty());

        SavingsModuleService.SavingsModuleException exception = assertThrows(
                SavingsModuleService.SavingsModuleException.class,
                () -> service.updateGoal(1, 99, "Car", "5000", "1000", "2026-12-01", "3")
        );

        assertEquals("Goal introuvable.", exception.getMessage());
    }

    @Test
    void shouldContributeToGoalAndCommitTransaction() throws Exception {
        SavingAccount account = savingAccount(5, 1, "2000.00", "3.50", LocalDate.of(2026, 4, 1));
        FinancialGoal goal = goal(22, 5, "Car", "3000.00", "1000.00", LocalDate.of(2026, 12, 1), 5);

        when(savingAccountRepository.findLatestByUserId(1)).thenReturn(Optional.of(account));
        when(financialGoalRepository.findByIdAndSavingAccountId(22, 5)).thenReturn(Optional.of(goal));
        when(savingAccountRepository.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        when(financialGoalRepository.findBySavingAccountId(5)).thenReturn(List.of(goal));
        when(savingsTransactionRepository.findSavingsHistoryByUserId(1)).thenReturn(List.of());

        service.contributeToGoal(1, 22, "300");

        verify(savingAccountRepository).subtractFromBalance(5, new BigDecimal("300.00"));
        verify(financialGoalRepository).addContribution(22, 5, 300.0, connection);
        verify(savingsTransactionRepository).insertGoalContribution(1, new BigDecimal("300.00"), "Contribution to goal: Car", connection);
        verify(connection).commit();
    }

    @Test
    void shouldRejectContributionWhenBalanceIsInsufficient() throws Exception {
        SavingAccount account = savingAccount(5, 1, "100.00", "3.50", LocalDate.of(2026, 4, 1));
        FinancialGoal goal = goal(22, 5, "Car", "3000.00", "1000.00", LocalDate.of(2026, 12, 1), 5);

        when(savingAccountRepository.findLatestByUserId(1)).thenReturn(Optional.of(account));
        when(financialGoalRepository.findByIdAndSavingAccountId(22, 5)).thenReturn(Optional.of(goal));

        SavingsValidation.SavingsValidationException exception = assertThrows(
                SavingsValidation.SavingsValidationException.class,
                () -> service.contributeToGoal(1, 22, "300")
        );

        assertEquals("La contribution depasse le solde disponible.", exception.getMessage());
        verify(savingAccountRepository, never()).subtractFromBalance(anyInt(), any(BigDecimal.class));
    }

    @Test
    void shouldDeleteGoalForAccount() throws Exception {
        SavingAccount account = savingAccount(5, 1, "100.00", "3.50", LocalDate.of(2026, 4, 1));

        when(savingAccountRepository.findLatestByUserId(1)).thenReturn(Optional.of(account));
        when(financialGoalRepository.findBySavingAccountId(5)).thenReturn(List.of());
        when(savingsTransactionRepository.findSavingsHistoryByUserId(1)).thenReturn(List.of());

        service.deleteGoal(1, 77);

        verify(financialGoalRepository).delete(77, 5);
    }

    private static SavingAccount savingAccount(int id, int userId, String balance, String rate, LocalDate createdOn) {
        SavingAccount account = new SavingAccount();
        account.setId(id);
        account.setUserId(userId);
        account.setSold(new BigDecimal(balance).doubleValue());
        account.setTauxInteret(new BigDecimal(rate).doubleValue());
        account.setDateCreation(Date.valueOf(createdOn));
        return account;
    }

    private static FinancialGoal goal(
            int id,
            int savingAccountId,
            String name,
            String target,
            String current,
            LocalDate deadline,
            int priority
    ) {
        FinancialGoal goal = new FinancialGoal();
        goal.setId(id);
        goal.setSavingAccountId(savingAccountId);
        goal.setNom(name);
        goal.setMontantCible(new BigDecimal(target).doubleValue());
        goal.setMontantActuel(new BigDecimal(current).doubleValue());
        goal.setDateLimite(Date.valueOf(deadline));
        goal.setPriorite(priority);
        return goal;
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
