package pi.savings.ui;

import java.util.List;

final class SavingsMockData {

    private SavingsMockData() {
    }

    static DashboardData create() {
        List<KpiData> kpis = List.of(
                new KpiData("Savings Balance", "Current account balance", "50000 TND", "\uD83D\uDC37"),
                new KpiData("Active Goals", "Goals in progress", "2", "\uD83C\uDFAF"),
                new KpiData("Goals Progress", "Average completion", "85%", "\uD83D\uDCCA"),
                new KpiData("Nearest Deadline", "Closest goal date", "2028-02-25", "\uD83D\uDCC5")
        );

        List<InsightData> insights = List.of(
                new InsightData("Smart Financial Insights", "Mini AI (rule-based)", List.of(
                        "Suggestion: Increase your monthly deposit",
                        "Interest tip: 30% is configured on the current account",
                        "Risk warning: prioritize the nearest deadline goal"
                ), "\uD83D\uDCA1"),
                new InsightData("Smart Alerts", "Deadlines • low balance • unusual activity", List.of(
                        "Upcoming deadline on the house goal",
                        "Consistency reminder: a small weekly deposit helps",
                        "Low savings buffer alert stays visible here"
                ), "\uD83D\uDD14"),
                new InsightData("Gamification", "Streak • XP • badges", List.of(
                        "Level 3",
                        "XP 650 / 1000",
                        "Badges: First Deposit, Goal Creator, Weekly Saver"
                ), "\uD83C\uDFC6")
        );

        List<HistoryRow> history = List.of(
                new HistoryRow("1", "2026-02-10", "3000 TND", "prime"),
                new HistoryRow("2", "2026-02-10", "50000 TND", "monthly"),
                new HistoryRow("3", "2026-02-10", "1000 TND", "Contribution to goal")
        );

        List<GoalCardData> goals = List.of(
                new GoalCardData("house", "50 TND", "30 TND", "2026-02-20", "P3", 60),
                new GoalCardData("pc", "4000 TND", "100 TND", "2026-02-20", "P2", 3)
        );

        return new DashboardData(kpis, insights, history, goals);
    }

    record DashboardData(
            List<KpiData> kpis,
            List<InsightData> insights,
            List<HistoryRow> history,
            List<GoalCardData> goals
    ) {
    }

    record KpiData(String title, String subtitle, String value, String icon) {
    }

    record InsightData(String title, String subtitle, List<String> lines, String icon) {
    }

    record HistoryRow(String index, String date, String amount, String description) {
    }

    record GoalCardData(
            String name,
            String target,
            String current,
            String deadline,
            String priority,
            double progress
    ) {
    }
}
