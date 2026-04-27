package pi.services.ImprevusCasreelService;

import pi.entities.CasRelles;
import pi.entities.FinancialGoal;
import pi.entities.Imprevus;
import pi.entities.SavingAccount;
import pi.entities.User;
import pi.savings.repository.FinancialGoalRepository;
import pi.savings.repository.SavingAccountRepository;
import pi.tools.AppEnv;
import pi.tools.MyDatabase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class CasReelService {

    public static final String STATUT_EN_ATTENTE = "EN_ATTENTE";
    /** Admin must arbitrate emergency fund vs savings (goal almost reached). */
    public static final String STATUT_EN_ATTENTE_ALLOCATION = "EN_ATTENTE_ALLOCATION";
    public static final String STATUT_ACCEPTE = "ACCEPTE";
    public static final String STATUT_REFUSE = "REFUSE";

    private static final int RECURRENCE_WINDOW_DAYS = 180;
    private static final int RECURRENCE_MIN_EVENTS = 3;
    public static final String PAYMENT_EMERGENCY_FUND = "EMERGENCY_FUND";
    public static final String PAYMENT_SAVING_ACCOUNT = "SAVING_ACCOUNT";

    private final Connection cnx;
    private final SavingAccountRepository savingAccountRepository;
    private final FinancialGoalRepository financialGoalRepository;
    private final CaseNotificationService caseNotificationService;
    private final UserNotificationService userNotificationService;
    private final LocationSuggestionService locationSuggestionService;

    public record CaseWorkflowOutcome(
            boolean notificationCreated,
            String notificationError,
            boolean emailSent,
            String emailError,
            boolean decisionEmailSkippedByPolicy
    ) {
    }

    public record CaseInsertOutcome(
            int caseId,
            boolean allocationPending,
            boolean autoAccepted,
            boolean recurrenceRiskEmailSent,
            String userMessage
    ) {
    }

    public record GainAllocationDecision(
            String recommendedPaymentMethod,
            FinancialGoal targetGoal,
            String suggestion,
            double emergencyFundBalance,
            double savingBalance
    ) {
    }

    private record StatusChangeDecision(
            String paymentMethod,
            FinancialGoal financialGoal,
            String solution,
            String aiSuggestion,
            String adminNote,
            boolean suppressDecisionEmail
    ) {
    }

    public CasReelService() {
        cnx = MyDatabase.getInstance().getCnx();
        savingAccountRepository = new SavingAccountRepository();
        financialGoalRepository = new FinancialGoalRepository();
        caseNotificationService = new CaseNotificationService();
        userNotificationService = new UserNotificationService();
        locationSuggestionService = new LocationSuggestionService();
        creerTableSiAbsente();
        ajouterColonnesWorkflowSiNecessaire();
    }

    public CaseInsertOutcome ajouter(CasRelles casReel) {
        CasRelles prepared = prepareCaseBeforePersistence(casReel);
        CaseFundingAdvice advice = analyzeFundingChoice(
                prepared.getUser().getId(),
                prepared.getPaymentMethod(),
                prepared.getMontant()
        );

        boolean needsAdminAllocation = needsAdminAllocationReview(prepared, advice);
        boolean isGain = "Gain".equalsIgnoreCase(prepared.getType());
        prepared.setSuppressDecisionEmail(false);

        LocalDateTime now = LocalDateTime.now();
        if (isGain) {
            prepared.setResultat(STATUT_EN_ATTENTE);
            prepared.setConfirmedAt(null);
            prepared.setConfirmedBy(null);
        } else if (needsAdminAllocation) {
            prepared.setResultat(STATUT_EN_ATTENTE_ALLOCATION);
            prepared.setConfirmedAt(null);
            prepared.setConfirmedBy(null);
        } else {
            prepared.setResultat(STATUT_ACCEPTE);
            prepared.setConfirmedAt(now);
            User self = new User();
            self.setId(prepared.getUser().getId());
            self.setNom(prepared.getUser().getNom());
            self.setEmail(prepared.getUser().getEmail());
            prepared.setConfirmedBy(self);
        }

        String req = """
                INSERT INTO cas_relles
                (user_id, imprevus_id, confirmed_by_id, financial_goal_id, titre, description, type, categorie, montant, solution,
                 date_effet, resultat, raison_refus, confirmed_at, justificatif_file_name, updated_at, payment_method, admin_note,
                 ai_refusal_suggestion, notification_sent_at, suppress_decision_email)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        int newId;
        try (PreparedStatement ps = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            bindCase(ps, prepared, false);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new RuntimeException("Impossible de recuperer l'id du cas cree.");
                }
                newId = keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur ajout cas reel : " + e.getMessage(), e);
        }

        boolean recurrenceSent = false;
        if (isGain) {
            notifyAdminsPendingReview(prepared, newId);
            notifyCaseOwnerInApp(prepared.getUser(),
                    "Gain en attente de traitement",
                    "Votre gain \"" + safeShort(prepared.getTitre(), 80) + "\" est en attente. "
                            + "L'admin choisira l'affectation finale entre epargne et fonds d'urgence.",
                    STATUT_EN_ATTENTE);
        } else if (needsAdminAllocation) {
            notifyAdminsPendingReview(prepared, newId);
            notifyCaseOwnerInApp(prepared.getUser(),
                    "Validation administrateur requise",
                    "Votre cas \"" + safeShort(prepared.getTitre(), 80) + "\" necessite un arbitrage epargne / fonds d'urgence. "
                            + "L'admin vous notifiera apres decision.",
                    STATUT_EN_ATTENTE_ALLOCATION);
        } else {
            notifyCaseOwnerInApp(prepared.getUser(),
                    "Cas enregistre",
                    "Votre cas \"" + safeShort(prepared.getTitre(), 80) + "\" est accepte automatiquement (date, preuve et montant enregistres).",
                    STATUT_ACCEPTE);
            recurrenceSent = maybeSendRecurrenceInsight(prepared, newId);
        }

        String userMessage = isGain
                ? "Gain soumis : statut EN_ATTENTE. L'admin decidera l'affectation epargne / fonds d'urgence."
                : needsAdminAllocation
                ? "Cas soumis : en attente de validation admin pour le choix epargne / fonds de securite (notification in-app, pas d'email)."
                : "Cas enregistre et valide automatiquement." + (recurrenceSent ? " Un email de prevention vous a ete envoye (repetition du risque)." : "");

        return new CaseInsertOutcome(newId, isGain || needsAdminAllocation, !isGain && !needsAdminAllocation, recurrenceSent, userMessage);
    }

    public void modifier(CasRelles casReel) {
        CasRelles prepared = prepareCaseBeforePersistence(casReel);
        String req = """
                UPDATE cas_relles
                SET user_id = ?, imprevus_id = ?, confirmed_by_id = ?, financial_goal_id = ?, titre = ?, description = ?, type = ?, categorie = ?,
                    montant = ?, solution = ?, date_effet = ?, resultat = ?, raison_refus = ?, confirmed_at = ?, justificatif_file_name = ?,
                    updated_at = ?, payment_method = ?, admin_note = ?, ai_refusal_suggestion = ?, notification_sent_at = ?, suppress_decision_email = ?
                WHERE id = ?
                """;

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            bindCase(ps, prepared, true);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur modification cas reel : " + e.getMessage(), e);
        }
    }

    public List<CasRelles> afficher() {
        return afficherAvecFiltre(null);
    }

    public List<CasRelles> afficherParStatut(String statut) {
        return afficherAvecFiltre(statut);
    }

    public Optional<CasRelles> findById(int id) {
        String req = baseSelect() + " WHERE cr.id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapCasReel(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur recherche cas reel : " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public void changerStatut(int id, String statut, String raisonRefus, String adminNote, Integer confirmedById) {
        changerStatutWithOutcome(id, statut, raisonRefus, adminNote, confirmedById);
    }

    public CaseWorkflowOutcome changerStatutWithOutcome(int id, String statut, String raisonRefus, String adminNote, Integer confirmedById) {
        Optional<CasRelles> prior = findById(id);

        String req = """
                UPDATE cas_relles
                SET resultat = ?, raison_refus = ?, admin_note = ?, confirmed_at = ?, confirmed_by_id = ?, updated_at = ?,
                    payment_method = ?, financial_goal_id = ?, solution = ?, ai_refusal_suggestion = ?, suppress_decision_email = ?
                WHERE id = ?
                """;

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            LocalDateTime now = LocalDateTime.now();
            StatusChangeDecision decision = resolveStatusChangeDecision(prior.orElse(null), statut, adminNote);
            ps.setString(1, statut);
            ps.setString(2, emptyToNull(raisonRefus));
            ps.setString(3, emptyToNull(decision.adminNote()));
            ps.setTimestamp(4, Timestamp.valueOf(now));
            if (confirmedById == null || confirmedById <= 0) {
                ps.setNull(5, Types.INTEGER);
            } else {
                ps.setInt(5, confirmedById);
            }
            ps.setTimestamp(6, Timestamp.valueOf(now));
            ps.setString(7, emptyToNull(decision.paymentMethod()));
            setFinancialGoal(ps, 8, decision.financialGoal());
            ps.setString(9, emptyToNull(decision.solution()));
            ps.setString(10, emptyToNull(decision.aiSuggestion()));
            ps.setBoolean(11, decision.suppressDecisionEmail());
            ps.setInt(12, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur confirmation cas reel : " + e.getMessage(), e);
        }

        return afterStatusChangeSideEffects(id, prior.orElse(null));
    }

    private CaseWorkflowOutcome afterStatusChangeSideEffects(int id, CasRelles prior) {
        LocalDateTime now = LocalDateTime.now();
        Optional<CasRelles> refreshed = findById(id);
        if (refreshed.isEmpty()) {
            return new CaseWorkflowOutcome(false, "case not found after update", false, null, false);
        }
        CasRelles cas = refreshed.get();
        User targetUser = cas.getUser();

        boolean notificationCreated = false;
        String notificationError = null;
        try {
            if (targetUser == null || targetUser.getId() <= 0) {
                notificationError = "no case owner (user_id missing)";
            } else {
                createUserDecisionNotification(cas, targetUser);
                notificationCreated = true;
            }
        } catch (Exception e) {
            notificationError = e.getMessage();
        }

        boolean skipDecisionEmail = prior != null && prior.isSuppressDecisionEmail();
        boolean emailSent = false;
        String emailError = null;
        if (!skipDecisionEmail) {
            try {
                CaseNotificationService.EmailSendResult emailResult = caseNotificationService.sendDecisionEmailDetailed(cas, targetUser);
                emailSent = emailResult.sent();
                emailError = emailResult.failureReason();
                if (emailSent) {
                    markNotificationSent(id, now);
                }
            } catch (Exception e) {
                emailError = e.getMessage();
            }
        }

        return new CaseWorkflowOutcome(notificationCreated, notificationError, emailSent, emailError, skipDecisionEmail);
    }

    public void supprimer(int id) {
        String req = "DELETE FROM cas_relles WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur suppression cas reel : " + e.getMessage(), e);
        }
    }

    public double calculateEmergencyFundBalance(int userId) {
        if (userId <= 0) {
            return 0;
        }
        String req = """
                SELECT COALESCE(SUM(
                    CASE
                        WHEN LOWER(type) = 'gain' AND resultat = ? AND payment_method = ? THEN montant
                        WHEN LOWER(type) = 'depense' AND resultat = ? AND payment_method = ? THEN -montant
                        ELSE 0
                    END
                ), 0) AS emergency_balance
                FROM cas_relles
                WHERE user_id = ?
                """;
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, STATUT_ACCEPTE);
            ps.setString(2, PAYMENT_EMERGENCY_FUND);
            ps.setString(3, STATUT_ACCEPTE);
            ps.setString(4, PAYMENT_EMERGENCY_FUND);
            ps.setInt(5, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double balance = rs.getDouble("emergency_balance");
                    if (Double.isNaN(balance) || Double.isInfinite(balance)) {
                        return 0;
                    }
                    return Math.max(balance, 0);
                }
            }
        } catch (SQLException e) {
            return 0;
        }
        return 0;
    }

    public double getSavingBalance(int userId) {
        if (userId <= 0) {
            return 0;
        }
        try {
            return savingAccountRepository.findLatestByUserId(userId)
                    .map(SavingAccount::getSold)
                    .orElse(0.0);
        } catch (SQLException e) {
            return 0;
        }
    }

    public CaseFundingAdvice analyzeFundingChoice(int userId, String paymentMethod, double amount) {
        double emergencyFundBalance = calculateEmergencyFundBalance(userId);
        double savingBalance = getSavingBalance(userId);
        String normalizedPaymentMethod = paymentMethod == null ? "" : paymentMethod.trim().toUpperCase(Locale.ROOT);
        String suggestion = null;
        boolean suggestedRefusal = false;

        if (PAYMENT_EMERGENCY_FUND.equals(normalizedPaymentMethod) && emergencyFundBalance < amount) {
            suggestion = String.format(Locale.US,
                    "Emergency Fund insuffisant: disponible %.2f DT pour %.2f DT. L'epargne est plus adaptee ici.",
                    emergencyFundBalance, amount);
            suggestedRefusal = true;
        } else if (PAYMENT_SAVING_ACCOUNT.equals(normalizedPaymentMethod) && emergencyFundBalance >= amount) {
            suggestion = String.format(Locale.US,
                    "Choix peu optimal: l'Emergency Fund couvre deja %.2f DT, donc l'epargne n'est pas prioritaire pour ce cas.",
                    emergencyFundBalance);
            suggestedRefusal = true;
        } else if (PAYMENT_SAVING_ACCOUNT.equals(normalizedPaymentMethod)) {
            Optional<FinancialGoal> closestGoal = findClosestGoalToCompletion(userId);
            if (closestGoal.isPresent()) {
                FinancialGoal goal = closestGoal.get();
                double remaining = Math.max(goal.getMontantCible() - goal.getMontantActuel(), 0);
                double progress = goal.getMontantCible() <= 0 ? 0 : (goal.getMontantActuel() / goal.getMontantCible()) * 100.0;
                if (progress >= 80.0 && savingBalance - amount < remaining) {
                    suggestion = String.format(Locale.US,
                            "Choix risqué: l'epargne est proche de l'objectif \"%s\" (%.0f%% atteint, %.2f DT restants).",
                            goal.getNom(), progress, remaining);
                    suggestedRefusal = true;
                }
            }
            if (suggestion == null && savingBalance < amount) {
                suggestion = String.format(Locale.US,
                        "Epargne insuffisante: disponible %.2f DT pour %.2f DT.", savingBalance, amount);
                suggestedRefusal = true;
            }
        }

        if (suggestion == null) {
            suggestion = String.format(Locale.US,
                    "Choix coherent. Emergency Fund: %.2f DT, Epargne: %.2f DT.",
                    emergencyFundBalance, savingBalance);
        }

        return new CaseFundingAdvice(emergencyFundBalance, savingBalance, suggestion, suggestedRefusal);
    }

    public GainAllocationDecision analyzeGainAllocation(int userId, double amount) {
        double emergencyFundBalance = calculateEmergencyFundBalance(userId);
        double savingBalance = getSavingBalance(userId);
        Optional<FinancialGoal> closestGoal = findClosestGoalToCompletion(userId);
        if (closestGoal.isPresent()) {
            FinancialGoal goal = closestGoal.get();
            double remaining = Math.max(goal.getMontantCible() - goal.getMontantActuel(), 0);
            double progress = goal.getMontantCible() <= 0 ? 0 : (goal.getMontantActuel() / goal.getMontantCible()) * 100.0;
            if (progress >= 80.0 || amount >= remaining) {
                String suggestion = String.format(Locale.US,
                        "Gain en attente de validation admin. Suggestion: affecter a l'epargne pour accelerer l'objectif \"%s\" (%.0f%% atteint, %.2f DT restants).",
                        goal.getNom(), progress, remaining);
                return new GainAllocationDecision(
                        PAYMENT_SAVING_ACCOUNT,
                        goal,
                        suggestion,
                        emergencyFundBalance,
                        savingBalance
                );
            }
        }

        String suggestion = String.format(Locale.US,
                "Gain en attente de validation admin. Suggestion: affecter au fonds d'urgence. Solde projete: %.2f DT.",
                emergencyFundBalance + amount);
        return new GainAllocationDecision(
                PAYMENT_EMERGENCY_FUND,
                null,
                suggestion,
                emergencyFundBalance,
                savingBalance
        );
    }

    public String inferRiskCategory(String titre, String description, Imprevus imprevu) {
        String source = (titre == null ? "" : titre) + " "
                + (description == null ? "" : description) + " "
                + (imprevu == null || imprevu.getTitre() == null ? "" : imprevu.getTitre());
        String normalized = source.toLowerCase(Locale.ROOT);

        if (containsAny(normalized, "maladie", "sante", "medecin", "hopital", "clinique", "pharmacie", "fievre", "consultation")) return "Sante";
        if (containsAny(normalized, "voiture", "auto", "garage", "panne", "mecanique", "essence", "carburant", "entretien")) return "Voiture";
        if (containsAny(normalized, "maison", "loyer", "plombier", "fuite", "reparation", "mobilier", "electricite maison")) return "Maison";
        if (containsAny(normalized, "telephone", "pc", "ordinateur", "laptop", "ecran", "electronique", "imprimante")) return "Electronique";
        if (containsAny(normalized, "ecole", "education", "formation", "cours", "universite", "frais scolaire")) return "Education";
        if (containsAny(normalized, "facture", "eau", "gaz", "internet", "electricite", "abonnement")) return "Factures";
        return "Autres";
    }

    private List<CasRelles> afficherAvecFiltre(String statut) {
        List<CasRelles> liste = new ArrayList<>();
        String req = baseSelect() + (statut == null ? "" : " WHERE cr.resultat = ?") + " ORDER BY cr.id DESC";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            if (statut != null) {
                ps.setString(1, statut);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    liste.add(mapCasReel(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur affichage cas reels : " + e.getMessage(), e);
        }

        return liste;
    }

    private String baseSelect() {
        return """
                SELECT cr.id, cr.user_id, cr.imprevus_id, cr.confirmed_by_id, cr.financial_goal_id, cr.titre, cr.description, cr.type, cr.categorie,
                       cr.montant, cr.solution, cr.date_effet, cr.justificatif_file_name, cr.resultat, cr.raison_refus, cr.confirmed_at,
                       cr.updated_at, cr.payment_method, cr.admin_note, cr.ai_refusal_suggestion, cr.notification_sent_at, cr.suppress_decision_email,
                       i.titre AS imprevu_titre, i.type AS imprevu_type, i.budget AS imprevu_budget,
                       u.nom AS user_nom, u.email AS user_email,
                       cb.nom AS confirmed_by_nom, cb.email AS confirmed_by_email,
                       fg.nom AS financial_goal_nom, fg.montant_cible AS financial_goal_target, fg.montant_actuel AS financial_goal_current
                FROM cas_relles cr
                LEFT JOIN imprevus i ON cr.imprevus_id = i.id
                LEFT JOIN `user` u ON cr.user_id = u.id
                LEFT JOIN `user` cb ON cr.confirmed_by_id = cb.id
                LEFT JOIN financial_goal fg ON cr.financial_goal_id = fg.id
                """;
    }

    private CasRelles mapCasReel(ResultSet rs) throws SQLException {
        User user = null;
        int userId = rs.getInt("user_id");
        if (!rs.wasNull()) {
            user = new User();
            user.setId(userId);
            user.setNom(rs.getString("user_nom"));
            user.setEmail(rs.getString("user_email"));
        }

        User confirmedBy = null;
        int confirmedById = rs.getInt("confirmed_by_id");
        if (!rs.wasNull()) {
            confirmedBy = new User();
            confirmedBy.setId(confirmedById);
            confirmedBy.setNom(rs.getString("confirmed_by_nom"));
            confirmedBy.setEmail(rs.getString("confirmed_by_email"));
        }

        Imprevus imprevu = null;
        int imprevuId = rs.getInt("imprevus_id");
        if (!rs.wasNull()) {
            imprevu = new Imprevus(
                    imprevuId,
                    rs.getString("imprevu_titre"),
                    rs.getString("imprevu_type"),
                    rs.getDouble("imprevu_budget")
            );
        }

        FinancialGoal goal = null;
        int goalId = rs.getInt("financial_goal_id");
        if (!rs.wasNull()) {
            goal = new FinancialGoal();
            goal.setId(goalId);
            goal.setNom(rs.getString("financial_goal_nom"));
            goal.setMontantCible(rs.getDouble("financial_goal_target"));
            goal.setMontantActuel(rs.getDouble("financial_goal_current"));
        }

        Date dateEffet = rs.getDate("date_effet");
        Timestamp confirmedAt = rs.getTimestamp("confirmed_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        Timestamp notificationSentAt = rs.getTimestamp("notification_sent_at");

        CasRelles cas = new CasRelles(
                rs.getInt("id"),
                user,
                imprevu,
                confirmedBy,
                goal,
                rs.getString("titre"),
                rs.getString("description"),
                rs.getString("type"),
                rs.getString("categorie"),
                rs.getDouble("montant"),
                rs.getString("solution"),
                dateEffet == null ? null : dateEffet.toLocalDate(),
                emptyToDefault(rs.getString("resultat"), STATUT_EN_ATTENTE),
                rs.getString("raison_refus"),
                confirmedAt == null ? null : confirmedAt.toLocalDateTime(),
                rs.getString("justificatif_file_name"),
                updatedAt == null ? null : updatedAt.toLocalDateTime()
        );
        cas.setPaymentMethod(rs.getString("payment_method"));
        cas.setAdminNote(rs.getString("admin_note"));
        cas.setAiRefusalSuggestion(rs.getString("ai_refusal_suggestion"));
        cas.setNotificationSentAt(notificationSentAt == null ? null : notificationSentAt.toLocalDateTime());
        try {
            cas.setSuppressDecisionEmail(rs.getBoolean("suppress_decision_email"));
        } catch (SQLException ignored) {
            cas.setSuppressDecisionEmail(false);
        }
        return cas;
    }

    private void bindCase(PreparedStatement ps, CasRelles cas, boolean withIdAtEnd) throws SQLException {
        setUser(ps, 1, cas.getUser());
        setImprevu(ps, 2, cas.getImprevus());
        setUser(ps, 3, cas.getConfirmedBy());
        setFinancialGoal(ps, 4, cas.getFinancialGoal());
        ps.setString(5, cas.getTitre());
        ps.setString(6, emptyToNull(cas.getDescription()));
        ps.setString(7, cas.getType());
        ps.setString(8, emptyToNull(cas.getCategorie()));
        ps.setDouble(9, cas.getMontant());
        ps.setString(10, emptyToNull(cas.getSolution()));
        ps.setDate(11, cas.getDateEffet() == null ? null : Date.valueOf(cas.getDateEffet()));
        ps.setString(12, emptyToDefault(cas.getResultat(), STATUT_EN_ATTENTE));
        ps.setString(13, emptyToNull(cas.getRaisonRefus()));
        ps.setTimestamp(14, cas.getConfirmedAt() == null ? null : Timestamp.valueOf(cas.getConfirmedAt()));
        ps.setString(15, emptyToNull(cas.getJustificatifFileName()));
        ps.setTimestamp(16, cas.getUpdatedAt() == null ? Timestamp.valueOf(LocalDateTime.now()) : Timestamp.valueOf(cas.getUpdatedAt()));
        ps.setString(17, emptyToNull(cas.getPaymentMethod()));
        ps.setString(18, emptyToNull(cas.getAdminNote()));
        ps.setString(19, emptyToNull(cas.getAiRefusalSuggestion()));
        ps.setTimestamp(20, cas.getNotificationSentAt() == null ? null : Timestamp.valueOf(cas.getNotificationSentAt()));
        ps.setBoolean(21, cas.isSuppressDecisionEmail());
        if (withIdAtEnd) {
            ps.setInt(22, cas.getId());
        }
    }

    private CasRelles prepareCaseBeforePersistence(CasRelles casReel) {
        if (casReel == null) {
            throw new RuntimeException("Cas reel invalide.");
        }
        if (casReel.getUser() == null || casReel.getUser().getId() <= 0) {
            throw new RuntimeException("Aucun utilisateur connecte pour ce cas reel.");
        }

        String normalizedPaymentMethod = normalizePaymentMethod(casReel.getPaymentMethod());
        casReel.setPaymentMethod(normalizedPaymentMethod);

        if (casReel.getImprevus() == null) {
            casReel.setImprevus(resolveOrCreateSmartUnexpectedEvent(casReel.getTitre(), casReel.getDescription(), casReel.getType()));
            if (casReel.getCategorie() == null || casReel.getCategorie().isBlank()) {
                casReel.setCategorie("Creation intelligente");
            }
        }

        CaseFundingAdvice advice = analyzeFundingChoice(casReel.getUser().getId(), normalizedPaymentMethod, casReel.getMontant());
        if ("Gain".equalsIgnoreCase(casReel.getType())) {
            GainAllocationDecision gainDecision = analyzeGainAllocation(casReel.getUser().getId(), casReel.getMontant());
            casReel.setPaymentMethod(gainDecision.recommendedPaymentMethod());
            casReel.setFinancialGoal(gainDecision.targetGoal());
            casReel.setAiRefusalSuggestion(gainDecision.suggestion());
        } else {
            casReel.setAiRefusalSuggestion(advice.suggestion());
        }
        casReel.setUpdatedAt(LocalDateTime.now());

        if ("Depense".equalsIgnoreCase(casReel.getType())) {
            validateFundingChoice(casReel, advice);
        }
        return casReel;
    }

    private void validateFundingChoice(CasRelles casReel, CaseFundingAdvice advice) {
        if (PAYMENT_EMERGENCY_FUND.equals(casReel.getPaymentMethod()) && advice.emergencyFundBalance() < casReel.getMontant()) {
            throw new RuntimeException(advice.suggestion());
        }
        if (PAYMENT_SAVING_ACCOUNT.equals(casReel.getPaymentMethod()) && advice.savingBalance() < casReel.getMontant()) {
            throw new RuntimeException(advice.suggestion());
        }
    }

    private Optional<FinancialGoal> findClosestGoalToCompletion(int userId) {
        try {
            Optional<SavingAccount> account = savingAccountRepository.findLatestByUserId(userId);
            if (account.isEmpty()) {
                return Optional.empty();
            }
            return financialGoalRepository.findBySavingAccountId(account.get().getId()).stream()
                    .filter(goal -> goal.getMontantCible() > 0)
                    .min(Comparator.comparingDouble(goal -> Math.max(goal.getMontantCible() - goal.getMontantActuel(), 0)));
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

    private Imprevus resolveOrCreateSmartUnexpectedEvent(String titre, String description, String type) {
        String safeTitle = titre == null || titre.isBlank() ? "Unexpected event" : titre.trim();
        String safeType = type == null || type.isBlank() ? "Depense" : type.trim();
        String inferredCategory = inferRiskCategory(titre, description, null);

        String semanticLookup = """
                SELECT id, titre, type, budget
                FROM imprevus
                WHERE LOWER(type) = ?
                  AND (
                    LOWER(titre) LIKE ?
                    OR LOWER(titre) LIKE ?
                    OR LOWER(titre) LIKE ?
                  )
                ORDER BY id DESC
                LIMIT 1
                """;
        try (PreparedStatement ps = cnx.prepareStatement(semanticLookup)) {
            ps.setString(1, safeType.toLowerCase(Locale.ROOT));
            ps.setString(2, "%" + safeTitle.toLowerCase(Locale.ROOT) + "%");
            ps.setString(3, "%" + inferredCategory.toLowerCase(Locale.ROOT) + "%");
            ps.setString(4, "%" + firstKeywordForCategory(inferredCategory) + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Imprevus(rs.getInt("id"), rs.getString("titre"), rs.getString("type"), rs.getDouble("budget"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur recherche imprevu intelligent : " + e.getMessage(), e);
        }

        String lookup = """
                SELECT id, titre, type, budget
                FROM imprevus
                WHERE LOWER(titre) = ? AND LOWER(type) = ?
                ORDER BY id DESC
                LIMIT 1
                """;
        try (PreparedStatement ps = cnx.prepareStatement(lookup)) {
            ps.setString(1, safeTitle.toLowerCase(Locale.ROOT));
            ps.setString(2, safeType.toLowerCase(Locale.ROOT));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Imprevus(rs.getInt("id"), rs.getString("titre"), rs.getString("type"), rs.getDouble("budget"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur recherche imprevu intelligent : " + e.getMessage(), e);
        }

        String generatedTitle = inferredCategory.equals("Autres") ? safeTitle : inferredCategory + " - " + safeTitle;
        String insert = "INSERT INTO imprevus (titre, type, budget) VALUES (?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, generatedTitle);
            ps.setString(2, safeType);
            ps.setDouble(3, 0.0);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Imprevus(keys.getInt(1), generatedTitle, safeType, 0.0);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur creation imprevu intelligent : " + e.getMessage(), e);
        }

        throw new RuntimeException("Impossible de creer un imprevu intelligent.");
    }

    private String normalizePaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            return PAYMENT_EMERGENCY_FUND;
        }
        String normalized = paymentMethod.trim().toUpperCase(Locale.ROOT);
        if (!PAYMENT_EMERGENCY_FUND.equals(normalized) && !PAYMENT_SAVING_ACCOUNT.equals(normalized)) {
            throw new RuntimeException("Methode de paiement invalide.");
        }
        return normalized;
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String firstKeywordForCategory(String category) {
        return switch (category) {
            case "Sante" -> "maladie";
            case "Voiture" -> "voiture";
            case "Maison" -> "maison";
            case "Electronique" -> "telephone";
            case "Education" -> "education";
            case "Factures" -> "facture";
            default -> "autres";
        };
    }

    private void markNotificationSent(int id, LocalDateTime sentAt) {
        String req = "UPDATE cas_relles SET notification_sent_at = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setTimestamp(1, Timestamp.valueOf(sentAt));
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    private void createUserDecisionNotification(CasRelles cas, User targetUser) {
        if (cas == null || targetUser == null || targetUser.getId() <= 0) {
            return;
        }
        String status = cas.getResultat() == null ? STATUT_EN_ATTENTE : cas.getResultat();
        String title = "Real case processed: " + cas.getTitre();
        String message = "Status: " + status
                + " | Payment: " + (cas.getPaymentMethod() == null ? "-" : cas.getPaymentMethod())
                + " | Reason: " + (cas.getRaisonRefus() == null || cas.getRaisonRefus().isBlank() ? "-" : cas.getRaisonRefus());
        userNotificationService.create(new pi.entities.UserNotification(
                targetUser,
                title,
                message,
                status,
                false,
                LocalDateTime.now()
        ));
    }

    private void setUser(PreparedStatement ps, int index, User user) throws SQLException {
        if (user == null || user.getId() <= 0) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, user.getId());
        }
    }

    private void setImprevu(PreparedStatement ps, int index, Imprevus imprevu) throws SQLException {
        if (imprevu == null || imprevu.getId() <= 0) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, imprevu.getId());
        }
    }

    private void setFinancialGoal(PreparedStatement ps, int index, FinancialGoal goal) throws SQLException {
        if (goal == null || goal.getId() <= 0) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, goal.getId());
        }
    }

    private boolean needsAdminAllocationReview(CasRelles prepared, CaseFundingAdvice advice) {
        if ("Gain".equalsIgnoreCase(prepared.getType())) {
            return true;
        }
        return "Depense".equalsIgnoreCase(prepared.getType()) && advice.suggestedRefusal();
    }

    private void notifyAdminsAllocationPending(CasRelles prepared, int caseId) {
        String title = "Arbitrage epargne / fonds — cas #" + caseId;
        String message = "Utilisateur #" + prepared.getUser().getId()
                + " — \"" + safeShort(prepared.getTitre(), 120) + "\" — "
                + String.format(Locale.US, "%.2f DT", prepared.getMontant())
                + " — methode demandee: " + prepared.getPaymentMethod()
                + ". Suggestion moteur: " + safeShort(prepared.getAiRefusalSuggestion(), 400);
        userNotificationService.notifyAdminUsers(title, message, STATUT_EN_ATTENTE_ALLOCATION);
    }

    private void notifyAdminsPendingReview(CasRelles prepared, int caseId) {
        boolean isGain = "Gain".equalsIgnoreCase(prepared.getType());
        String title = (isGain ? "Gain a affecter" : "Arbitrage epargne / fonds") + " - cas #" + caseId;
        String message = "Utilisateur #" + prepared.getUser().getId()
                + " - \"" + safeShort(prepared.getTitre(), 120) + "\" - "
                + String.format(Locale.US, "%.2f DT", prepared.getMontant())
                + " - methode suggeree: " + prepared.getPaymentMethod()
                + ". Suggestion moteur: " + safeShort(prepared.getAiRefusalSuggestion(), 400);
        userNotificationService.notifyAdminUsers(title, message, isGain ? STATUT_EN_ATTENTE : STATUT_EN_ATTENTE_ALLOCATION);
    }

    private StatusChangeDecision resolveStatusChangeDecision(CasRelles prior, String statut, String requestedAdminNote) {
        if (prior == null) {
            return new StatusChangeDecision(null, null, null, null, emptyToNull(requestedAdminNote), false);
        }

        String paymentMethod = prior.getPaymentMethod();
        FinancialGoal financialGoal = prior.getFinancialGoal();
        String solution = prior.getSolution();
        String aiSuggestion = prior.getAiRefusalSuggestion();
        String finalAdminNote = emptyToNull(requestedAdminNote);

        if (STATUT_ACCEPTE.equalsIgnoreCase(statut) && "Gain".equalsIgnoreCase(prior.getType()) && prior.getUser() != null) {
            GainAllocationDecision gainDecision = analyzeGainAllocation(prior.getUser().getId(), prior.getMontant());
            paymentMethod = gainDecision.recommendedPaymentMethod();
            financialGoal = gainDecision.targetGoal();
            aiSuggestion = gainDecision.suggestion();
            solution = PAYMENT_SAVING_ACCOUNT.equals(paymentMethod) ? "Allocation admin -> Epargne" : "Allocation admin -> Fonds d'urgence";
            String autoNote = PAYMENT_SAVING_ACCOUNT.equals(paymentMethod)
                    ? "Affectation admin: epargne" + (financialGoal == null ? "" : " pour l'objectif " + financialGoal.getNom())
                    : "Affectation admin: fonds d'urgence";
            finalAdminNote = finalAdminNote == null || finalAdminNote.isBlank()
                    ? autoNote
                    : finalAdminNote + " | " + autoNote;
        }

        return new StatusChangeDecision(
                paymentMethod,
                financialGoal,
                solution,
                aiSuggestion,
                finalAdminNote,
                false
        );
    }

    private void notifyCaseOwnerInApp(User user, String title, String message, String statusToken) {
        if (user == null || user.getId() <= 0) {
            return;
        }
        userNotificationService.create(new pi.entities.UserNotification(
                user,
                truncateNotifTitle(title),
                message,
                statusToken,
                false,
                LocalDateTime.now()
        ));
    }

    private String truncateNotifTitle(String title) {
        if (title == null) {
            return "";
        }
        return title.length() <= 180 ? title : title.substring(0, 177) + "...";
    }

    private static String safeShort(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max - 3) + "...";
    }

    private boolean maybeSendRecurrenceInsight(CasRelles inserted, int caseId) {
        User user = inserted.getUser();
        if (user == null || user.getId() <= 0) {
            return false;
        }
        String riskKey = inferRiskCategory(inserted.getTitre(), inserted.getDescription(), inserted.getImprevus());
        if (!isTrackedRecurrenceCluster(riskKey)) {
            return false;
        }
        LocalDate since = LocalDate.now().minusDays(RECURRENCE_WINDOW_DAYS);
        int n = countSimilarRiskEvents(user.getId(), riskKey, since);
        if (n < RECURRENCE_MIN_EVENTS) {
            return false;
        }
        String city = resolveCityForSuggestions(user);
        List<String> places = locationSuggestionService.suggestNearbyPlaces(riskKey, city);
        String monthly = buildMonthlySuggestionFr(riskKey);
        CaseNotificationService.EmailSendResult r = caseNotificationService.sendRecurrenceRiskEmail(
                user, riskKey, n, monthly, places);
        return r.sent();
    }

    private boolean isTrackedRecurrenceCluster(String riskKey) {
        return "Sante".equals(riskKey) || "Voiture".equals(riskKey) || "Maison".equals(riskKey);
    }

    private String resolveCityForSuggestions(User user) {
        if (user != null && user.getGeoCityName() != null && !user.getGeoCityName().isBlank()) {
            return user.getGeoCityName();
        }
        String fromEnv = AppEnv.get("DEFAULT_SUGGESTION_CITY");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        return "Tunis";
    }

    private String buildMonthlySuggestionFr(String riskKey) {
        return switch (riskKey) {
            case "Sante" -> "Planifier un rendez-vous medical de suivi chaque mois (medecin traitant, prevention, bilan).";
            case "Voiture" -> "Planifier un entretien preventive mensuel (vidange, pneus, diagnostic garage).";
            case "Maison" -> "Planifier une inspection / maintenance mensuelle (plomberie, electricite, toiture).";
            default -> "Planifier un suivi mensuel adapte a ce type de depense.";
        };
    }

    private int countSimilarRiskEvents(int userId, String riskKey, LocalDate since) {
        String req = """
                SELECT cr.titre, cr.description, i.titre AS imprevu_titre, i.type AS imprevu_type, i.budget AS imprevu_budget
                FROM cas_relles cr
                LEFT JOIN imprevus i ON cr.imprevus_id = i.id
                WHERE cr.user_id = ? AND cr.date_effet >= ?
                """;
        int count = 0;
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(since));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Imprevus im = null;
                    String imTitre = rs.getString("imprevu_titre");
                    if (imTitre != null) {
                        im = new Imprevus(
                                0,
                                imTitre,
                                rs.getString("imprevu_type"),
                                rs.getDouble("imprevu_budget")
                        );
                    }
                    String cat = inferRiskCategory(rs.getString("titre"), rs.getString("description"), im);
                    if (riskKey.equals(cat)) {
                        count++;
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur analyse recurrence risque : " + e.getMessage(), e);
        }
        return count;
    }

    private void creerTableSiAbsente() {
        String req = """
                CREATE TABLE IF NOT EXISTS cas_relles (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    user_id INT NULL,
                    imprevus_id INT NULL,
                    confirmed_by_id INT NULL,
                    financial_goal_id INT NULL,
                    titre VARCHAR(255) NOT NULL,
                    description TEXT NULL,
                    type VARCHAR(50) NOT NULL,
                    categorie VARCHAR(100) NULL,
                    montant DOUBLE NOT NULL,
                    solution VARCHAR(100) NULL,
                    date_effet DATE NOT NULL,
                    resultat VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE',
                    raison_refus LONGTEXT NULL,
                    confirmed_at DATETIME NULL,
                    justificatif_file_name VARCHAR(255) NULL,
                    updated_at DATETIME NULL,
                    payment_method VARCHAR(30) NULL,
                    admin_note LONGTEXT NULL,
                    ai_refusal_suggestion LONGTEXT NULL,
                    notification_sent_at DATETIME NULL,
                    suppress_decision_email TINYINT(1) NOT NULL DEFAULT 0
                )
                """;

        try (Statement st = cnx.createStatement()) {
            st.executeUpdate(req);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur creation table cas_relles : " + e.getMessage(), e);
        }
    }

    private void ajouterColonnesWorkflowSiNecessaire() {
        rendreColonneNullableSiPossible("user_id", "INT NULL");
        rendreColonneNullableSiPossible("confirmed_by_id", "INT NULL");
        rendreColonneNullableSiPossible("financial_goal_id", "INT NULL");
        ajouterColonneSiAbsente("ALTER TABLE cas_relles ADD COLUMN resultat VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE'");
        ajouterColonneSiAbsente("ALTER TABLE cas_relles ADD COLUMN raison_refus LONGTEXT NULL");
        ajouterColonneSiAbsente("ALTER TABLE cas_relles ADD COLUMN confirmed_at DATETIME NULL");
        ajouterColonneSiAbsente("ALTER TABLE cas_relles ADD COLUMN updated_at DATETIME NULL");
        ajouterColonneSiAbsente("ALTER TABLE cas_relles ADD COLUMN payment_method VARCHAR(30) NULL");
        ajouterColonneSiAbsente("ALTER TABLE cas_relles ADD COLUMN admin_note LONGTEXT NULL");
        ajouterColonneSiAbsente("ALTER TABLE cas_relles ADD COLUMN ai_refusal_suggestion LONGTEXT NULL");
        ajouterColonneSiAbsente("ALTER TABLE cas_relles ADD COLUMN notification_sent_at DATETIME NULL");
        ajouterColonneSiAbsente("ALTER TABLE cas_relles ADD COLUMN suppress_decision_email TINYINT(1) NOT NULL DEFAULT 0");
    }

    private void ajouterColonneSiAbsente(String sql) {
        try (Statement st = cnx.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase(Locale.ROOT);
            if (!message.contains("duplicate column")) {
                throw new RuntimeException("Erreur mise a jour table cas_relles : " + e.getMessage(), e);
            }
        }
    }

    private void rendreColonneNullableSiPossible(String columnName, String definition) {
        try {
            if (!colonneExiste("cas_relles", columnName)) {
                return;
            }
            try (Statement st = cnx.createStatement()) {
                st.executeUpdate("ALTER TABLE cas_relles MODIFY COLUMN " + columnName + " " + definition);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur mise a jour colonne " + columnName + " : " + e.getMessage(), e);
        }
    }

    private boolean colonneExiste(String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = cnx.getMetaData();
        try (ResultSet rs = metaData.getColumns(null, null, tableName, columnName)) {
            return rs.next();
        }
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String emptyToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
