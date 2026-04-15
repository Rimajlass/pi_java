package pi.savings.service;

import pi.entities.FinancialGoal;
import pi.entities.SavingAccount;
import pi.savings.repository.FinancialGoalRepository;
import pi.savings.repository.SavingAccountRepository;
import pi.savings.repository.SavingsTransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SavingsModuleService {

    private final SavingAccountRepository savingAccountRepository;
    private final FinancialGoalRepository financialGoalRepository;
    private final SavingsTransactionRepository savingsTransactionRepository;

    public SavingsModuleService() {
        this(true);
    }

    protected SavingsModuleService(boolean initializeRepositories) {
        if (initializeRepositories) {
            this.savingAccountRepository = new SavingAccountRepository();
            this.financialGoalRepository = new FinancialGoalRepository();
            this.savingsTransactionRepository = new SavingsTransactionRepository();
        } else {
            this.savingAccountRepository = null;
            this.financialGoalRepository = null;
            this.savingsTransactionRepository = null;
        }
    }

    SavingsModuleService(
            SavingAccountRepository savingAccountRepository,
            FinancialGoalRepository financialGoalRepository,
            SavingsTransactionRepository savingsTransactionRepository
    ) {
        this.savingAccountRepository = savingAccountRepository;
        this.financialGoalRepository = financialGoalRepository;
        this.savingsTransactionRepository = savingsTransactionRepository;
    }

    public DashboardSnapshot loadDashboard(int userId) {
        try {
            SavingAccount account = getOrCreateAccount(userId);
            List<FinancialGoal> goals = financialGoalRepository.findBySavingAccountId(account.getId());
            List<SavingsTransactionRepository.TransactionRow> transactions =
                    savingsTransactionRepository.findSavingsHistoryByUserId(userId);
            return buildSnapshot(account, goals, transactions);
        } catch (SQLException exception) {
            throw new SavingsModuleException("Impossible de charger le module Savings & Goals.", exception);
        }
    }

    public DashboardSnapshot saveDeposit(int userId, String amountText, String descriptionText) {
        BigDecimal amount = SavingsValidation.parseRequiredMoney(amountText, "Le montant du depot est obligatoire.");
        SavingsValidation.validateDeposit(amount);

        String description = descriptionText == null || descriptionText.trim().isEmpty()
                ? "Deposit"
                : descriptionText.trim();

        try {
            SavingAccount account = getOrCreateAccount(userId);
            Connection connection = savingAccountRepository.getConnection();
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try {
                savingAccountRepository.addToBalance(account.getId(), amount);
                savingsTransactionRepository.insertDeposit(userId, amount, description, connection);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }

            return loadDashboard(userId);
        } catch (SQLException exception) {
            throw new SavingsModuleException("Impossible d'enregistrer le depot.", exception);
        }
    }

    public DashboardSnapshot updateInterestRate(int userId, String rateText) {
        BigDecimal rate = SavingsValidation.parseRequiredMoney(rateText, "Le taux d'interet est obligatoire.");
        SavingsValidation.validateInterestRate(rate);

        try {
            SavingAccount account = getOrCreateAccount(userId);
            savingAccountRepository.updateInterestRate(account.getId(), rate);
            return loadDashboard(userId);
        } catch (SQLException exception) {
            throw new SavingsModuleException("Impossible de mettre a jour le taux d'interet.", exception);
        }
    }

    public DashboardSnapshot createGoal(
            int userId,
            String nameText,
            String targetText,
            String currentText,
            String deadlineText,
            String priorityText
    ) {
        BigDecimal target = SavingsValidation.parseRequiredMoney(targetText, "Le montant cible est obligatoire.");
        BigDecimal current = SavingsValidation.parseRequiredMoney(currentText, "Le montant actuel est obligatoire.");
        java.sql.Date deadline = SavingsValidation.parseRequiredDate(deadlineText, "La date limite est obligatoire.");
        int priority = SavingsValidation.parseRequiredInt(priorityText, "La priorite est obligatoire.", "Priorite invalide.");
        SavingsValidation.validateGoal(nameText, target, current, deadline, priority);

        try {
            SavingAccount account = getOrCreateAccount(userId);
            FinancialGoal goal = new FinancialGoal(
                    account.getId(),
                    nameText.trim(),
                    target.doubleValue(),
                    current.doubleValue(),
                    deadline,
                    priority
            );
            financialGoalRepository.insert(goal);
            return loadDashboard(userId);
        } catch (SQLException exception) {
            throw new SavingsModuleException("Impossible de creer le goal.", exception);
        }
    }

    public DashboardSnapshot updateGoal(
            int userId,
            int goalId,
            String nameText,
            String targetText,
            String currentText,
            String deadlineText,
            String priorityText
    ) {
        BigDecimal target = SavingsValidation.parseRequiredMoney(targetText, "Le montant cible est obligatoire.");
        BigDecimal current = SavingsValidation.parseRequiredMoney(currentText, "Le montant actuel est obligatoire.");
        java.sql.Date deadline = SavingsValidation.parseRequiredDate(deadlineText, "La date limite est obligatoire.");
        int priority = SavingsValidation.parseRequiredInt(priorityText, "La priorite est obligatoire.", "Priorite invalide.");
        SavingsValidation.validateGoal(nameText, target, current, deadline, priority);

        try {
            SavingAccount account = getOrCreateAccount(userId);
            FinancialGoal existingGoal = financialGoalRepository.findByIdAndSavingAccountId(goalId, account.getId())
                    .orElseThrow(() -> new SavingsModuleException("Goal introuvable.", null));

            existingGoal.setNom(nameText.trim());
            existingGoal.setMontantCible(target.doubleValue());
            existingGoal.setMontantActuel(current.doubleValue());
            existingGoal.setDateLimite(deadline);
            existingGoal.setPriorite(priority);
            financialGoalRepository.update(existingGoal);
            return loadDashboard(userId);
        } catch (SQLException exception) {
            throw new SavingsModuleException("Impossible de modifier le goal.", exception);
        }
    }

    public DashboardSnapshot deleteGoal(int userId, int goalId) {
        try {
            SavingAccount account = getOrCreateAccount(userId);
            financialGoalRepository.delete(goalId, account.getId());
            return loadDashboard(userId);
        } catch (SQLException exception) {
            throw new SavingsModuleException("Impossible de supprimer le goal.", exception);
        }
    }

    public DashboardSnapshot contributeToGoal(int userId, int goalId, String amountText) {
        BigDecimal amount = SavingsValidation.parseRequiredMoney(amountText, "Le montant de contribution est obligatoire.");

        try {
            SavingAccount account = getOrCreateAccount(userId);
            FinancialGoal goal = financialGoalRepository.findByIdAndSavingAccountId(goalId, account.getId())
                    .orElseThrow(() -> new SavingsModuleException("Goal introuvable.", null));

            BigDecimal balance = BigDecimal.valueOf(account.getSold()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal remainingTarget = BigDecimal.valueOf(goal.getMontantCible())
                    .subtract(BigDecimal.valueOf(goal.getMontantActuel()))
                    .setScale(2, RoundingMode.HALF_UP);
            SavingsValidation.validateContribution(amount, balance, remainingTarget);

            Connection connection = savingAccountRepository.getConnection();
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try {
                savingAccountRepository.subtractFromBalance(account.getId(), amount);
                financialGoalRepository.addContribution(goalId, account.getId(), amount.doubleValue(), connection);
                savingsTransactionRepository.insertGoalContribution(
                        userId,
                        amount,
                        "Contribution to goal: " + goal.getNom(),
                        connection
                );
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }

            return loadDashboard(userId);
        } catch (SQLException exception) {
            throw new SavingsModuleException("Impossible d'enregistrer la contribution.", exception);
        }
    }

    public HistoryStats calculateHistoryStats(List<SavingsTransactionRepository.TransactionRow> transactions) {
        BigDecimal totalDeposited = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalContributed = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        int depositCount = 0;
        int goalContributionCount = 0;

        for (SavingsTransactionRepository.TransactionRow transaction : transactions) {
            BigDecimal amount = transaction.amount().setScale(2, RoundingMode.HALF_UP);
            if ("GOAL_CONTRIBUTION".equalsIgnoreCase(transaction.type())) {
                totalContributed = totalContributed.add(amount);
                goalContributionCount++;
            } else {
                totalDeposited = totalDeposited.add(amount);
                depositCount++;
            }
        }

        BigDecimal averageAmount = transactions.isEmpty()
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : totalDeposited.add(totalContributed)
                .divide(BigDecimal.valueOf(transactions.size()), 2, RoundingMode.HALF_UP);

        String latestTransactionDate = transactions.stream()
                .map(SavingsTransactionRepository.TransactionRow::date)
                .max(Comparator.naturalOrder())
                .map(LocalDate::from)
                .map(LocalDate::toString)
                .orElse("--/--/----");

        return new HistoryStats(
                transactions.size(),
                depositCount,
                goalContributionCount,
                totalDeposited,
                totalContributed,
                averageAmount,
                latestTransactionDate
        );
    }

    public Path exportHistoryCsv(List<SavingsTransactionRepository.TransactionRow> transactions, Path exportDirectory) {
        try {
            return SavingsReportExporter.writeHistoryCsv(transactions, exportDirectory);
        } catch (Exception exception) {
            throw new SavingsModuleException("Impossible d'exporter l'historique en CSV.", exception);
        }
    }

    public Path exportHistoryPdf(List<SavingsTransactionRepository.TransactionRow> transactions, Path exportDirectory) {
        try {
            return SavingsReportExporter.writeHistoryPdf(
                    transactions,
                    calculateHistoryStats(transactions),
                    exportDirectory
            );
        } catch (Exception exception) {
            throw new SavingsModuleException("Impossible d'exporter l'historique en PDF.", exception);
        }
    }

    public GoalStats calculateGoalStats(List<GoalSnapshot> goals) {
        BigDecimal totalTarget = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCurrent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        int completedGoalCount = 0;

        for (GoalSnapshot goal : goals) {
            BigDecimal target = goal.target().setScale(2, RoundingMode.HALF_UP);
            BigDecimal current = goal.current().setScale(2, RoundingMode.HALF_UP);
            totalTarget = totalTarget.add(target);
            totalCurrent = totalCurrent.add(current);
            if (current.compareTo(target) >= 0 && target.compareTo(BigDecimal.ZERO) > 0) {
                completedGoalCount++;
            }
        }

        BigDecimal remainingAmount = totalTarget.subtract(totalCurrent).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        int completionRate = goals.isEmpty()
                ? 0
                : (int) Math.round((completedGoalCount * 100.0) / goals.size());

        String nearestDeadline = goals.stream()
                .map(GoalSnapshot::deadline)
                .filter(deadline -> deadline != null)
                .min(Comparator.naturalOrder())
                .map(LocalDate::toString)
                .orElse("--/--/----");

        return new GoalStats(
                goals.size(),
                completedGoalCount,
                completionRate,
                totalTarget,
                totalCurrent,
                remainingAmount,
                nearestDeadline
        );
    }

    public Path exportGoalsCsv(List<GoalSnapshot> goals, Path exportDirectory) {
        try {
            return SavingsReportExporter.writeGoalsCsv(goals, exportDirectory);
        } catch (Exception exception) {
            throw new SavingsModuleException("Impossible d'exporter les goals en CSV.", exception);
        }
    }

    public Path exportGoalsPdf(List<GoalSnapshot> goals, Path exportDirectory) {
        try {
            return SavingsReportExporter.writeGoalsPdf(
                    goals,
                    calculateGoalStats(goals),
                    exportDirectory
            );
        } catch (Exception exception) {
            throw new SavingsModuleException("Impossible d'exporter les goals en PDF.", exception);
        }
    }

    private SavingAccount getOrCreateAccount(int userId) throws SQLException {
        return savingAccountRepository.findLatestByUserId(userId)
                .orElseGet(() -> {
                    try {
                        return savingAccountRepository.createDefaultAccount(userId);
                    } catch (SQLException exception) {
                        throw new SavingsModuleException("Impossible d'initialiser le compte epargne.", exception);
                    }
                });
    }

    private DashboardSnapshot buildSnapshot(
            SavingAccount account,
            List<FinancialGoal> goals,
            List<SavingsTransactionRepository.TransactionRow> transactions
    ) {
        BigDecimal balance = BigDecimal.valueOf(account.getSold()).setScale(2, RoundingMode.HALF_UP);
        int activeGoals = (int) goals.stream()
                .filter(goal -> BigDecimal.valueOf(goal.getMontantActuel()).compareTo(BigDecimal.valueOf(goal.getMontantCible())) < 0)
                .count();

        int averageProgress = 0;
        if (!goals.isEmpty()) {
            double totalProgress = goals.stream().mapToDouble(this::progressPercent).sum();
            averageProgress = (int) Math.round(totalProgress / goals.size());
        }

        String nearestDeadline = goals.stream()
                .map(FinancialGoal::getDateLimite)
                .filter(date -> date != null)
                .map(java.sql.Date::toLocalDate)
                .min(Comparator.naturalOrder())
                .map(LocalDate::toString)
                .orElse("--/--/----");

        List<GoalSnapshot> goalSnapshots = new ArrayList<>();
        for (FinancialGoal goal : goals) {
            goalSnapshots.add(new GoalSnapshot(
                    goal.getId(),
                    goal.getNom(),
                    BigDecimal.valueOf(goal.getMontantCible()).setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(goal.getMontantActuel()).setScale(2, RoundingMode.HALF_UP),
                    goal.getDateLimite() != null ? goal.getDateLimite().toLocalDate() : null,
                    goal.getPriorite(),
                    progressPercent(goal)
            ));
        }

        return new DashboardSnapshot(
                account.getId(),
                account.getUserId(),
                balance,
                BigDecimal.valueOf(account.getTauxInteret()).setScale(2, RoundingMode.HALF_UP),
                account.getDateCreation() != null ? account.getDateCreation().toLocalDate() : LocalDate.now(),
                activeGoals,
                averageProgress,
                nearestDeadline,
                goalSnapshots,
                transactions
        );
    }

    private double progressPercent(FinancialGoal goal) {
        BigDecimal target = BigDecimal.valueOf(goal.getMontantCible());
        if (target.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        return BigDecimal.valueOf(goal.getMontantActuel())
                .divide(target, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .min(new BigDecimal("100"))
                .doubleValue();
    }

    public record DashboardSnapshot(
            int accountId,
            int userId,
            BigDecimal balance,
            BigDecimal interestRate,
            LocalDate createdOn,
            int activeGoals,
            int averageProgress,
            String nearestDeadline,
            List<GoalSnapshot> goals,
            List<SavingsTransactionRepository.TransactionRow> transactions
    ) {
    }

    public record GoalSnapshot(
            int id,
            String name,
            BigDecimal target,
            BigDecimal current,
            LocalDate deadline,
            int priority,
            double progressPercent
    ) {
    }

    public record HistoryStats(
            int transactionCount,
            int depositCount,
            int goalContributionCount,
            BigDecimal totalDeposited,
            BigDecimal totalContributedToGoals,
            BigDecimal averageAmount,
            String latestTransactionDate
    ) {
    }

    public record GoalStats(
            int goalCount,
            int completedGoalCount,
            int completionRate,
            BigDecimal totalTarget,
            BigDecimal totalCurrent,
            BigDecimal remainingAmount,
            String nearestDeadline
    ) {
    }

    public static final class SavingsModuleException extends RuntimeException {
        public SavingsModuleException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
