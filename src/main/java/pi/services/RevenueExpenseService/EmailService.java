package pi.services.RevenueExpenseService;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import pi.entities.Expense;
import pi.entities.Revenue;
import pi.entities.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EmailService {

    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Path DOTENV_PATH = Path.of(".env");
    private static final Map<String, String> DOTENV_VALUES = loadDotenv();

    private final OkHttpClient httpClient;
    private final String apiKey;
    private final String senderEmail;
    private final String senderName;
    private final String fallbackRecipientEmail;

    public EmailService() {
        this(new OkHttpClient());
    }

    public EmailService(OkHttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
        this.apiKey = requireEnv("BREVO_API_KEY");
        this.senderEmail = requireEnv("BREVO_SENDER_EMAIL");
        this.senderName = getEnvOrDefault("BREVO_SENDER_NAME", "Finance Tracker");
        this.fallbackRecipientEmail = getEnvOrDefault("BREVO_RECIPIENT_EMAIL", "");
    }

    public void sendRevenueNotification(User recipient, Revenue revenue) throws IOException {
        if (revenue == null) {
            throw new IllegalArgumentException("Revenue cannot be null");
        }

        String subject = "Revenue added successfully";
        String htmlContent = buildRevenueHtml(revenue);
        String textContent = buildRevenueText(revenue);
        sendEmail(resolveRecipientEmail(recipient), safeRecipientName(recipient), subject, htmlContent, textContent);
    }

    public void sendExpenseNotification(User recipient, Expense expense) throws IOException {
        if (expense == null) {
            throw new IllegalArgumentException("Expense cannot be null");
        }

        String subject = "Expense added successfully";
        String htmlContent = buildExpenseHtml(expense);
        String textContent = buildExpenseText(expense);
        sendEmail(resolveRecipientEmail(recipient), safeRecipientName(recipient), subject, htmlContent, textContent);
    }

    public void sendExpenseLimitAlert(User recipient, double expenseAmount, Revenue revenue, String category, java.time.LocalDate expenseDate, String description) throws IOException {
        if (revenue == null) {
            throw new IllegalArgumentException("Revenue cannot be null");
        }

        String subject = "Alert: expense exceeds selected revenue";
        String htmlContent = buildExpenseLimitAlertHtml(expenseAmount, revenue, category, expenseDate, description);
        String textContent = buildExpenseLimitAlertText(expenseAmount, revenue, category, expenseDate, description);
        sendEmail(resolveRecipientEmail(recipient), safeRecipientName(recipient), subject, htmlContent, textContent);
    }

    public void sendMonthlyReport(User recipient, String subject, String htmlContent, String textContent) throws IOException {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Email subject cannot be empty");
        }
        if (htmlContent == null || htmlContent.isBlank()) {
            throw new IllegalArgumentException("HTML content cannot be empty");
        }
        if (textContent == null || textContent.isBlank()) {
            throw new IllegalArgumentException("Text content cannot be empty");
        }

        sendEmail(resolveRecipientEmail(recipient), safeRecipientName(recipient), subject, htmlContent, textContent);
    }

    public String buildRevenueEmailJson(String toEmail, String toName, Revenue revenue) {
        if (revenue == null) {
            throw new IllegalArgumentException("Revenue cannot be null");
        }
        return buildEmailJson(
                toEmail,
                toName,
                "Revenue added successfully",
                buildRevenueHtml(revenue),
                buildRevenueText(revenue)
        );
    }

    public String buildExpenseEmailJson(String toEmail, String toName, Expense expense) {
        if (expense == null) {
            throw new IllegalArgumentException("Expense cannot be null");
        }
        return buildEmailJson(
                toEmail,
                toName,
                "Expense added successfully",
                buildExpenseHtml(expense),
                buildExpenseText(expense)
        );
    }

    private void sendEmail(String toEmail, String toName, String subject, String htmlContent, String textContent) throws IOException {
        String payload = buildEmailJson(toEmail, toName, subject, htmlContent, textContent);
        RequestBody body = RequestBody.create(payload, JSON_MEDIA_TYPE);

        Request request = new Request.Builder()
                .url(BREVO_URL)
                .addHeader("accept", "application/json")
                .addHeader("content-type", "application/json")
                .addHeader("api-key", apiKey)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException("Brevo API error: HTTP " + response.code() + " - " + errorBody);
            }
        }
    }

    private String buildEmailJson(String toEmail, String toName, String subject, String htmlContent, String textContent) {
        return """
                {
                  "sender": {
                    "name": "%s",
                    "email": "%s"
                  },
                  "to": [
                    {
                      "email": "%s",
                      "name": "%s"
                    }
                  ],
                  "subject": "%s",
                  "htmlContent": "%s",
                  "textContent": "%s"
                }
                """.formatted(
                escapeJson(senderName),
                escapeJson(senderEmail),
                escapeJson(toEmail),
                escapeJson(toName),
                escapeJson(subject),
                escapeJson(htmlContent),
                escapeJson(textContent)
        );
    }

    private String buildRevenueHtml(Revenue revenue) {
        return """
                <html>
                  <body>
                    <h2>Revenue Added</h2>
                    <p>A new revenue has been added successfully.</p>
                    <ul>
                      <li><strong>Amount:</strong> %s TND</li>
                      <li><strong>Type:</strong> %s</li>
                      <li><strong>Date:</strong> %s</li>
                      <li><strong>Description:</strong> %s</li>
                    </ul>
                  </body>
                </html>
                """.formatted(
                formatAmount(revenue.getAmount()),
                safe(revenue.getType()),
                formatDate(revenue.getReceivedAt()),
                safe(revenue.getDescription())
        );
    }

    private String buildRevenueText(Revenue revenue) {
        return """
                Revenue Added

                A new revenue has been added successfully.
                Amount: %s TND
                Type: %s
                Date: %s
                Description: %s
                """.formatted(
                formatAmount(revenue.getAmount()),
                safe(revenue.getType()),
                formatDate(revenue.getReceivedAt()),
                safe(revenue.getDescription())
        );
    }

    private String buildExpenseHtml(Expense expense) {
        return """
                <html>
                  <body>
                    <h2>Expense Added</h2>
                    <p>A new expense has been added successfully.</p>
                    <ul>
                      <li><strong>Amount:</strong> %s TND</li>
                      <li><strong>Category:</strong> %s</li>
                      <li><strong>Date:</strong> %s</li>
                      <li><strong>Description:</strong> %s</li>
                    </ul>
                  </body>
                </html>
                """.formatted(
                formatAmount(expense.getAmount()),
                safe(expense.getCategory()),
                formatDate(expense.getExpenseDate()),
                safe(expense.getDescription())
        );
    }

    private String buildExpenseText(Expense expense) {
        return """
                Expense Added

                A new expense has been added successfully.
                Amount: %s TND
                Category: %s
                Date: %s
                Description: %s
                """.formatted(
                formatAmount(expense.getAmount()),
                safe(expense.getCategory()),
                formatDate(expense.getExpenseDate()),
                safe(expense.getDescription())
        );
    }

    private String buildExpenseLimitAlertHtml(double expenseAmount, Revenue revenue, String category, java.time.LocalDate expenseDate, String description) {
        return """
                <html>
                  <body>
                    <h2>Expense Limit Alert</h2>
                    <p>An expense entry exceeded the selected revenue and was rejected.</p>
                    <ul>
                      <li><strong>Expense Amount:</strong> %s TND</li>
                      <li><strong>Expense Category:</strong> %s</li>
                      <li><strong>Expense Date:</strong> %s</li>
                      <li><strong>Expense Description:</strong> %s</li>
                      <li><strong>Selected Revenue Amount:</strong> %s TND</li>
                      <li><strong>Selected Revenue Type:</strong> %s</li>
                      <li><strong>Selected Revenue Date:</strong> %s</li>
                    </ul>
                  </body>
                </html>
                """.formatted(
                formatAmount(expenseAmount),
                safe(category),
                formatDate(expenseDate),
                safe(description),
                formatAmount(revenue.getAmount()),
                safe(revenue.getType()),
                formatDate(revenue.getReceivedAt())
        );
    }

    private String buildExpenseLimitAlertText(double expenseAmount, Revenue revenue, String category, java.time.LocalDate expenseDate, String description) {
        return """
                Expense Limit Alert

                An expense entry exceeded the selected revenue and was rejected.
                Expense Amount: %s TND
                Expense Category: %s
                Expense Date: %s
                Expense Description: %s
                Selected Revenue Amount: %s TND
                Selected Revenue Type: %s
                Selected Revenue Date: %s
                """.formatted(
                formatAmount(expenseAmount),
                safe(category),
                formatDate(expenseDate),
                safe(description),
                formatAmount(revenue.getAmount()),
                safe(revenue.getType()),
                formatDate(revenue.getReceivedAt())
        );
    }

    private String resolveRecipientEmail(User recipient) {
        if (fallbackRecipientEmail != null && !fallbackRecipientEmail.isBlank()) {
            return fallbackRecipientEmail.trim();
        }
        if (recipient != null && recipient.getEmail() != null && !recipient.getEmail().isBlank()) {
            return recipient.getEmail().trim();
        }
        throw new IllegalArgumentException(
                "A valid recipient email is required. Set the logged-in user email or BREVO_RECIPIENT_EMAIL in .env."
        );
    }

    private String safeRecipientName(User recipient) {
        if (recipient == null || recipient.getNom() == null || recipient.getNom().isBlank()) {
            return "User";
        }
        return recipient.getNom().trim();
    }

    private String formatAmount(double amount) {
        return String.format(java.util.Locale.US, "%.2f", amount);
    }

    private String formatDate(java.time.LocalDate date) {
        return date == null ? "--/--/----" : DATE_FORMATTER.format(date);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private String requireEnv(String key) {
        String value = resolveConfigValue(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + key);
        }
        return value.trim();
    }

    private String getEnvOrDefault(String key, String defaultValue) {
        String value = resolveConfigValue(key);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String resolveConfigValue(String key) {
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return DOTENV_VALUES.get(key);
    }

    private static Map<String, String> loadDotenv() {
        Map<String, String> values = new HashMap<>();
        if (!Files.exists(DOTENV_PATH)) {
            return values;
        }

        try {
            List<String> lines = Files.readAllLines(DOTENV_PATH);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                int separatorIndex = trimmed.indexOf('=');
                if (separatorIndex <= 0) {
                    continue;
                }

                String key = trimmed.substring(0, separatorIndex).trim();
                String value = trimmed.substring(separatorIndex + 1).trim();
                values.put(key, stripOptionalQuotes(value));
            }
        } catch (IOException ignored) {
            return values;
        }

        return values;
    }

    private static String stripOptionalQuotes(String value) {
        if (value == null || value.length() < 2) {
            return value;
        }
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length() + 16);
        for (char ch : value.toCharArray()) {
            switch (ch) {
                case '\\' -> builder.append("\\\\");
                case '"' -> builder.append("\\\"");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        builder.append(String.format("\\u%04x", (int) ch));
                    } else {
                        builder.append(ch);
                    }
                }
            }
        }
        return builder.toString();
    }
}
