package pi.assistant;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record AgentResponseCard(
        String title,
        String subtitle,
        String commandRecognized,
        String intentLabel,
        Map<String, String> parameters,
        Map<String, List<String>> sections,
        List<String> quickActions,
        String action,
        String resultMessage,
        boolean success
) {
    public AgentResponseCard {
        parameters = parameters == null ? new LinkedHashMap<>() : new LinkedHashMap<>(parameters);
        sections = sections == null ? new LinkedHashMap<>() : new LinkedHashMap<>(sections);
        quickActions = quickActions == null ? List.of() : List.copyOf(quickActions);
    }
}
