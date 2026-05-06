package pi.services.AiQuizService;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public final class QuizGenerationModels {

    private QuizGenerationModels() {
    }

    public static final class GeneratedQuizBundle {
        public String title;
        public List<GeneratedQuestion> questions = new ArrayList<>();
    }

    public static final class GeneratedQuestion {
        public String question;
        public List<String> choices = new ArrayList<>();
        public String correct;
        public int points = 1;
        public String explanation;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class OpenAiChatCompletionResponse {
        public List<Choice> choices = new ArrayList<>();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Choice {
        public Message message;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Message {
        public String content;
        @JsonProperty("refusal")
        public String refusal;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class GeminiGenerateContentResponse {
        public List<GeminiCandidate> candidates = new ArrayList<>();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class GeminiCandidate {
        public GeminiContent content;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class GeminiContent {
        public List<GeminiPart> parts = new ArrayList<>();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class GeminiPart {
        public String text;
    }
}
