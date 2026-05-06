package pi.assistant;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CommandHelpService {

    public HelpContent buildHelp() {
        Map<String, List<String>> sections = new LinkedHashMap<>();
        sections.put("Savings", List.of(
                "show my savings balance",
                "show balance",
                "list savings accounts",
                "deposit 100 TND to savings",
                "add 100 TND to savings"
        ));
        sections.put("Goals", List.of(
                "list my goals",
                "show my goals",
                "create goal Laptop target 3000 deadline 2026-06-01",
                "update goal Laptop target 4000",
                "delete goal Laptop"
        ));
        sections.put("Contributions", List.of(
                "contribute 100 TND to goal Laptop",
                "contribute 100 TND in Laptop",
                "contribute 100 to Laptop",
                "add 100 TND to goal Laptop",
                "put 100 TND into Laptop"
        ));
        sections.put("Search & Status", List.of(
                "show completed goals",
                "show overdue goals",
                "show at risk goals",
                "show goal Laptop",
                "show progress of Laptop"
        ));
        sections.put("Confirmation", List.of(
                "yes",
                "no",
                "cancel",
                "confirm"
        ));
        sections.put("Voice Examples", List.of(
                "contribute 100 TND to goal Laptop",
                "show my savings balance",
                "list my goals"
        ));

        List<String> examples = List.of(
                "contribute 100 TND to goal PCC",
                "delete goal Travel",
                "create goal Phone target 2000 deadline 2026-08-01",
                "show my savings balance"
        );

        List<String> quickActions = List.of(
                "show my savings balance",
                "list my goals",
                "contribute 100 TND to goal PCC",
                "create goal Laptop target 3000 deadline 2026-06-01",
                "delete goal Laptop"
        );

        return new HelpContent(
                "Available Commands",
                "Type or say one of these commands to control Savings & Goals.",
                sections,
                examples,
                quickActions
        );
    }

    public record HelpContent(
            String title,
            String subtitle,
            Map<String, List<String>> sections,
            List<String> examples,
            List<String> quickActions
    ) {
    }
}
