package pi.assistant;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Mixer;

public record MicrophoneTestResult(
        boolean available,
        String userMessage,
        Mixer.Info selectedMixer,
        AudioFormat selectedFormat
) {
}
