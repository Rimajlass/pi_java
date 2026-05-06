package pi.services.AiQuizService;

public final class AiSettings {
    private final String apiKey;
    private final String model;

    public AiSettings(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
    }
}

