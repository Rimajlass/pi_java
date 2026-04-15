package pi.savings.service;

import org.junit.jupiter.api.Test;
import pi.savings.service.SavingsValidation.SavingsValidationException;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SavingsValidationTest {

    @Test
    void shouldRejectInvalidDepositAmount() {
        SavingsValidationException exception = assertThrows(
                SavingsValidationException.class,
                () -> SavingsValidation.validateDeposit(new BigDecimal("0.00"))
        );

        assertEquals("Le montant du depot doit etre > 0.", exception.getMessage());
    }

    @Test
    void shouldRejectPastGoalDeadline() {
        SavingsValidationException exception = assertThrows(
                SavingsValidationException.class,
                () -> SavingsValidation.validateGoal(
                        "Trip",
                        new BigDecimal("1000.00"),
                        new BigDecimal("100.00"),
                        Date.valueOf(LocalDate.now().minusDays(1)),
                        3
                )
        );

        assertEquals("La date limite doit etre aujourd'hui ou dans le futur.", exception.getMessage());
    }

    @Test
    void shouldRejectContributionAboveRemainingTarget() {
        SavingsValidationException exception = assertThrows(
                SavingsValidationException.class,
                () -> SavingsValidation.validateContribution(
                        new BigDecimal("500.00"),
                        new BigDecimal("1000.00"),
                        new BigDecimal("200.00")
                )
        );

        assertEquals("La contribution depasse le montant restant a atteindre.", exception.getMessage());
    }

    @Test
    void shouldParseMoneyWithCommaSeparator() {
        assertEquals(new BigDecimal("25.50"), SavingsValidation.parseRequiredMoney("25,5", "required"));
    }

    @Test
    void shouldRejectTooLargeDepositAmount() {
        SavingsValidationException exception = assertThrows(
                SavingsValidationException.class,
                () -> SavingsValidation.validateDeposit(new BigDecimal("1000000.01"))
        );

        assertEquals("Montant trop grand. Maximum: 1000000.", exception.getMessage());
    }

    @Test
    void shouldRejectNegativeInterestRate() {
        SavingsValidationException exception = assertThrows(
                SavingsValidationException.class,
                () -> SavingsValidation.validateInterestRate(new BigDecimal("-1"))
        );

        assertEquals("Le taux d'interet doit etre entre 0 et 100.", exception.getMessage());
    }

    @Test
    void shouldRejectGoalNameShorterThanThreeCharacters() {
        SavingsValidationException exception = assertThrows(
                SavingsValidationException.class,
                () -> SavingsValidation.validateGoal(
                        "ab",
                        new BigDecimal("1000.00"),
                        new BigDecimal("100.00"),
                        Date.valueOf(LocalDate.now().plusDays(10)),
                        3
                )
        );

        assertEquals("Le nom du goal doit contenir entre 3 et 60 caracteres.", exception.getMessage());
    }

    @Test
    void shouldRejectAlreadyCompletedGoalContribution() {
        SavingsValidationException exception = assertThrows(
                SavingsValidationException.class,
                () -> SavingsValidation.validateContribution(
                        new BigDecimal("100.00"),
                        new BigDecimal("500.00"),
                        BigDecimal.ZERO
                )
        );

        assertEquals("Ce goal est deja atteint.", exception.getMessage());
    }

    @Test
    void shouldRejectInvalidDateFormat() {
        SavingsValidationException exception = assertThrows(
                SavingsValidationException.class,
                () -> SavingsValidation.parseRequiredDate("15/04/2026", "required")
        );

        assertEquals("Date invalide. Format attendu: yyyy-mm-dd.", exception.getMessage());
    }

    @Test
    void shouldAcceptValidGoalAndContributionInputs() {
        assertDoesNotThrow(() -> SavingsValidation.validateGoal(
                "Emergency Fund",
                new BigDecimal("5000.00"),
                new BigDecimal("1200.00"),
                Date.valueOf(LocalDate.now().plusDays(30)),
                4
        ));
        assertDoesNotThrow(() -> SavingsValidation.validateContribution(
                new BigDecimal("150.00"),
                new BigDecimal("900.00"),
                new BigDecimal("400.00")
        ));
    }

    @Test
    void shouldRejectEmptyMoneyInput() {
        SavingsValidationException exception = assertThrows(
                SavingsValidationException.class,
                () -> SavingsValidation.parseRequiredMoney(" ", "amount required")
        );

        assertEquals("amount required", exception.getMessage());
    }

    @Test
    void shouldRejectCurrentAmountGreaterThanTarget() {
        SavingsValidationException exception = assertThrows(
                SavingsValidationException.class,
                () -> SavingsValidation.validateGoal(
                        "Trip",
                        new BigDecimal("1000.00"),
                        new BigDecimal("1200.00"),
                        Date.valueOf(LocalDate.now().plusDays(10)),
                        3
                )
        );

        assertEquals("Le montant actuel ne peut pas depasser le montant cible.", exception.getMessage());
    }

    @Test
    void shouldRejectPriorityOutsideAllowedRange() {
        SavingsValidationException exception = assertThrows(
                SavingsValidationException.class,
                () -> SavingsValidation.validateGoal(
                        "Trip",
                        new BigDecimal("1000.00"),
                        new BigDecimal("200.00"),
                        Date.valueOf(LocalDate.now().plusDays(10)),
                        6
                )
        );

        assertEquals("La priorite doit etre comprise entre 1 et 5.", exception.getMessage());
    }
}
