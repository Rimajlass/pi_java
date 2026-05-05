package pi.savings.service;

import pi.savings.dto.GoalRiskDTO;
import pi.savings.dto.WhatIfScenarioDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PdfReportHtmlBuilder {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public String build(SmartPdfReportService.SmartPdfReportData data) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        html.append("<title>Smart PDF Report</title>");
        html.append("<style>");
        html.append("""
                * { box-sizing: border-box; }
                body {
                    margin: 0;
                    font-family: 'Segoe UI', Arial, sans-serif;
                    color: #16324a;
                    background: linear-gradient(180deg, #f3fbff 0%, #ffffff 100%);
                }
                .page {
                    padding: 28px 32px 32px;
                }
                .hero {
                    border-radius: 22px;
                    padding: 28px 30px;
                    color: white;
                    background:
                        radial-gradient(circle at top right, rgba(120, 224, 255, 0.32), transparent 34%),
                        linear-gradient(135deg, #082542 0%, #0ea5c9 56%, #57d5f0 100%);
                    box-shadow: 0 20px 50px rgba(8, 37, 66, 0.18);
                }
                .eyebrow {
                    letter-spacing: 0.18em;
                    text-transform: uppercase;
                    font-size: 11px;
                    font-weight: 700;
                    opacity: 0.84;
                }
                .hero h1 {
                    margin: 10px 0 8px;
                    font-size: 30px;
                    line-height: 1.15;
                }
                .hero p {
                    margin: 0;
                    font-size: 14px;
                    max-width: 760px;
                    opacity: 0.94;
                }
                .meta {
                    margin-top: 18px;
                    display: flex;
                    gap: 18px;
                    flex-wrap: wrap;
                    font-size: 12px;
                    opacity: 0.92;
                }
                .section {
                    margin-top: 24px;
                    background: white;
                    border: 1px solid rgba(10, 60, 94, 0.08);
                    border-radius: 20px;
                    padding: 20px 22px;
                    box-shadow: 0 14px 34px rgba(15, 36, 61, 0.06);
                }
                .section h2 {
                    margin: 0 0 14px;
                    color: #0b2a46;
                    font-size: 20px;
                }
                .section h3 {
                    margin: 0 0 10px;
                    color: #0d3c63;
                    font-size: 15px;
                }
                .cards {
                    display: grid;
                    grid-template-columns: repeat(4, minmax(0, 1fr));
                    gap: 14px;
                }
                .card {
                    border-radius: 18px;
                    padding: 16px 16px 14px;
                    background: linear-gradient(180deg, #f7fdff 0%, #ecf8fd 100%);
                    border: 1px solid rgba(14, 165, 201, 0.14);
                }
                .card-label {
                    color: #4f6c80;
                    font-size: 11px;
                    text-transform: uppercase;
                    letter-spacing: 0.08em;
                    font-weight: 700;
                }
                .card-value {
                    margin-top: 8px;
                    color: #082542;
                    font-size: 22px;
                    font-weight: 800;
                }
                .summary-grid {
                    display: grid;
                    grid-template-columns: repeat(2, minmax(0, 1fr));
                    gap: 10px 16px;
                }
                .summary-item {
                    padding: 12px 14px;
                    border-radius: 16px;
                    background: #f9fcff;
                    border: 1px solid rgba(10, 60, 94, 0.08);
                }
                .summary-item strong {
                    display: block;
                    color: #46637a;
                    font-size: 11px;
                    text-transform: uppercase;
                    letter-spacing: 0.06em;
                    margin-bottom: 6px;
                }
                .summary-item span {
                    color: #0f2740;
                    font-size: 14px;
                    font-weight: 700;
                }
                table {
                    width: 100%;
                    border-collapse: collapse;
                    font-size: 11px;
                }
                thead th {
                    background: #0d3c63;
                    color: white;
                    padding: 10px 8px;
                    text-align: left;
                    vertical-align: top;
                }
                tbody td {
                    padding: 10px 8px;
                    border-bottom: 1px solid rgba(13, 60, 99, 0.10);
                    vertical-align: top;
                }
                tbody tr:nth-child(even) {
                    background: #f8fcff;
                }
                .pill {
                    display: inline-block;
                    padding: 4px 10px;
                    border-radius: 999px;
                    font-size: 10px;
                    font-weight: 700;
                    letter-spacing: 0.04em;
                }
                .pill.low { background: rgba(34, 197, 94, 0.16); color: #166534; }
                .pill.medium { background: rgba(251, 191, 36, 0.18); color: #92400e; }
                .pill.high { background: rgba(249, 115, 22, 0.18); color: #9a3412; }
                .pill.critical { background: rgba(239, 68, 68, 0.18); color: #991b1b; }
                .insight, .recommendations, .whatif {
                    font-size: 13px;
                    line-height: 1.6;
                    color: #21435f;
                }
                .recommendations ul, .whatif ul {
                    margin: 8px 0 0 18px;
                    padding: 0;
                }
                .recommendations li, .whatif li {
                    margin-bottom: 8px;
                }
                .footer {
                    margin-top: 20px;
                    text-align: center;
                    color: #617d92;
                    font-size: 11px;
                }
                """);
        html.append("</style></head><body><div class=\"page\">");

        html.append("<section class=\"hero\">");
        html.append("<div class=\"eyebrow\">Smart PDF Report API</div>");
        html.append("<h1>Savings &amp; Goals Professional Report</h1>");
        html.append("<p>API-generated PDF report built from live MySQL Savings &amp; Goals data, enriched with analytics, predictions, and AI insight.</p>");
        html.append("<div class=\"meta\">");
        html.append("<span>Generated: ").append(escape(LocalDateTime.now().format(DATE_TIME_FORMATTER))).append("</span>");
        html.append("<span>Currency: ").append(escape(data.currency())).append("</span>");
        html.append("<span>User: ").append(escape(data.userName())).append("</span>");
        html.append("</div></section>");

        html.append("<section class=\"section\"><h2>Executive Summary</h2><div class=\"cards\">");
        appendCard(html, "Savings Balance", money(data.savingsBalance()));
        appendCard(html, "Goals In Report", String.valueOf(data.goalRisks().size()));
        appendCard(html, "At Risk Goals", String.valueOf(data.analytics().atRiskGoals()));
        appendCard(html, "Required Monthly", money(data.analytics().requiredMonthlyContribution()));
        html.append("</div></section>");

        html.append("<section class=\"section\"><h2>Portfolio Snapshot</h2><div class=\"summary-grid\">");
        appendSummaryItem(html, "Completed Goals", String.valueOf(data.analytics().completedGoals()));
        appendSummaryItem(html, "Active Goals", String.valueOf(data.analytics().activeGoals()));
        appendSummaryItem(html, "Average Progress", percent(data.analytics().averageProgressPercentage()));
        appendSummaryItem(html, "Financial Health", data.analytics().financialHealthStatus() + " (" + data.analytics().financialHealthScore() + "/100)");
        appendSummaryItem(html, "Nearest Deadline", date(data.analytics().nearestDeadline()));
        appendSummaryItem(html, "Rate Provider", data.rateProvider());
        appendSummaryItem(html, "What-if Scenario", data.whatIfScenario() == null ? "Not available" : escape(data.whatIfScenario().scenarioName()));
        appendSummaryItem(html, "AI Insight", data.aiInsightAvailable() ? "Included" : "Fallback insight");
        html.append("</div></section>");

        html.append("<section class=\"section\"><h2>Goals Analytics Table</h2>");
        html.append("<table><thead><tr>");
        for (String header : List.of(
                "Goal Name",
                "Priority",
                "Target",
                "Current",
                "Remaining",
                "Progress %",
                "Deadline",
                "Risk Level",
                "Required Monthly",
                "Predicted Completion",
                "Status"
        )) {
            html.append("<th>").append(escape(header)).append("</th>");
        }
        html.append("</tr></thead><tbody>");
        for (GoalRiskDTO goal : data.goalRisks()) {
            html.append("<tr>");
            html.append("<td>").append(escape(goal.goalName())).append("</td>");
            html.append("<td>P").append(goal.priority()).append("</td>");
            html.append("<td>").append(money(goal.targetAmount())).append("</td>");
            html.append("<td>").append(money(goal.currentAmount())).append("</td>");
            html.append("<td>").append(money(goal.remainingAmount())).append("</td>");
            html.append("<td>").append(percent(goal.progressPercentage())).append("</td>");
            html.append("<td>").append(date(goal.deadline())).append("</td>");
            html.append("<td><span class=\"pill ").append(riskCss(goal.riskLevel())).append("\">")
                    .append(escape(goal.riskLevel())).append("</span></td>");
            html.append("<td>").append(money(goal.requiredMonthlyContribution())).append("</td>");
            html.append("<td>").append(date(goal.predictedCompletionDate())).append("</td>");
            html.append("<td>").append(escape(goal.status())).append("</td>");
            html.append("</tr>");
        }
        html.append("</tbody></table></section>");

        html.append("<section class=\"section\"><h2>AI Financial Insight</h2>");
        html.append("<div class=\"insight\">").append(escape(data.aiInsight())).append("</div>");
        html.append("</section>");

        html.append("<section class=\"section\"><h2>What-if Simulation Summary</h2><div class=\"whatif\">");
        if (data.whatIfScenario() == null || data.whatIfScenario().projections().isEmpty()) {
            html.append("No simulation summary available for the current portfolio.");
        } else {
            html.append("<strong>Scenario:</strong> ").append(escape(data.whatIfScenario().scenarioName()));
            html.append(" with monthly contribution ").append(money(data.whatIfScenario().monthlyContribution()));
            html.append("<ul>");
            int limit = Math.min(4, data.whatIfScenario().projections().size());
            for (int i = 0; i < limit; i++) {
                WhatIfScenarioDTO.GoalProjectionDTO projection = data.whatIfScenario().projections().get(i);
                html.append("<li><strong>").append(escape(projection.goalName())).append("</strong>: ");
                html.append("allocation ").append(money(projection.monthlyAllocation())).append(", ");
                html.append("predicted completion ").append(date(projection.predictedCompletionDate())).append(", ");
                html.append(escape(projection.statusMessage())).append(".</li>");
            }
            html.append("</ul>");
        }
        html.append("</div></section>");

        html.append("<section class=\"section\"><h2>Recommendations</h2><div class=\"recommendations\"><ul>");
        appendRecommendation(html, data.analytics().atRiskGoals() > 0,
                "Prioritize the " + data.analytics().atRiskGoals() + " at-risk goals and review their monthly contribution plans.");
        appendRecommendation(html, data.analytics().requiredMonthlyContribution().compareTo(BigDecimal.ZERO) > 0,
                "Set aside " + money(data.analytics().requiredMonthlyContribution()) + " per month to stay aligned with the portfolio target pace.");
        appendRecommendation(html, data.analytics().nearestDeadline() != null,
                "Review goals approaching " + date(data.analytics().nearestDeadline()) + " and rebalance contributions before the deadline slips.");
        appendRecommendation(html, true,
                "Generated by live MySQL data with Smart PDF Report API for structured reporting and easier stakeholder sharing.");
        html.append("</ul></div></section>");

        html.append("<div class=\"footer\">Generated by Smart PDF Report API</div>");
        html.append("</div></body></html>");
        return html.toString();
    }

    private void appendCard(StringBuilder html, String label, String value) {
        html.append("<div class=\"card\"><div class=\"card-label\">")
                .append(escape(label))
                .append("</div><div class=\"card-value\">")
                .append(escape(value))
                .append("</div></div>");
    }

    private void appendSummaryItem(StringBuilder html, String label, String value) {
        html.append("<div class=\"summary-item\"><strong>")
                .append(escape(label))
                .append("</strong><span>")
                .append(escape(value))
                .append("</span></div>");
    }

    private void appendRecommendation(StringBuilder html, boolean include, String text) {
        if (!include) {
            return;
        }
        html.append("<li>").append(escape(text)).append("</li>");
    }

    private String riskCss(String riskLevel) {
        if (riskLevel == null) {
            return "medium";
        }
        return switch (riskLevel.trim().toLowerCase(Locale.ROOT)) {
            case "low" -> "low";
            case "high" -> "high";
            case "critical" -> "critical";
            default -> "medium";
        };
    }

    private String money(BigDecimal value) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value;
        return safe.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + " TND";
    }

    private String percent(BigDecimal value) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value;
        return safe.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%";
    }

    private String date(LocalDate value) {
        return value == null ? "Not available" : value.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
