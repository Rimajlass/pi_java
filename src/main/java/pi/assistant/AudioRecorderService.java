package pi.assistant;

import pi.tools.ConfigLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.function.BooleanSupplier;

public class AudioRecorderService {

    private static final int DEFAULT_RECORD_SECONDS = 7;
    private final MicrophoneService microphoneService;

    public AudioRecorderService() {
        this(new MicrophoneService());
    }

    AudioRecorderService(MicrophoneService microphoneService) {
        this.microphoneService = microphoneService;
    }

    public Path recordCommand() throws IOException {
        int durationSeconds = Math.max(2, ConfigLoader.getInt("VOICE_RECORD_SECONDS", DEFAULT_RECORD_SECONDS));
        return microphoneService.recordToWav(
                java.nio.file.Files.createTempFile("smart-voice-command-", ".wav"),
                Duration.ofSeconds(durationSeconds),
                () -> false
        );
    }

    public void recordToWav(Path outputFile, Duration duration) throws IOException {
        recordToWav(outputFile, duration, () -> false);
    }

    public void recordToWav(Path outputFile, Duration duration, BooleanSupplier stopSignal) throws IOException {
        microphoneService.recordToWav(outputFile, duration, stopSignal);
    }

    public MicrophoneTestResult testMicrophoneAvailability() {
        return microphoneService.testMicrophoneAvailability();
    }

    public void testMicrophone() throws IOException {
        Path tempFile = microphoneService.runOneSecondTest();
        java.nio.file.Files.deleteIfExists(tempFile);
    }
}
