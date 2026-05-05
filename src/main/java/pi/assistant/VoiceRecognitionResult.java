package pi.assistant;

public record VoiceRecognitionResult(
        boolean success,
        String recognizedText,
        String userMessage
) {
}
