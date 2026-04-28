package pi.assistant;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

public class MicrophoneService {

    public interface AudioChunkConsumer {
        default void onMicrophoneReady(Mixer.Info mixerInfo, AudioFormat captureFormat, AudioFormat processingFormat) throws IOException {
        }

        void onAudioChunk(byte[] audioBytes, int bytesRead) throws IOException;
    }

    public static final String MICROPHONE_OPEN_FAILED_MESSAGE =
            "Microphone detected but could not be opened. Please close apps using the microphone and try again.";

    private static final AudioFormat[] CANDIDATE_AUDIO_FORMATS = new AudioFormat[]{
            new AudioFormat(16_000f, 16, 1, true, false),
            new AudioFormat(44_100f, 16, 1, true, false),
            new AudioFormat(48_000f, 16, 1, true, false),
            new AudioFormat(16_000f, 16, 1, true, true),
            new AudioFormat(44_100f, 16, 1, true, true)
    };
    private static final int BUFFER_SIZE = 4096;
    private static final int MIN_AUDIO_BYTES = 2048;

    public MicrophoneTestResult testMicrophoneAvailability() {
        return selectWorkingMicrophone(true);
    }

    public Path recordToWav(Path outputFile, Duration duration, BooleanSupplier stopSignal) throws IOException {
        MicrophoneTestResult testResult = selectWorkingMicrophone(true);
        if (!testResult.available() || testResult.selectedMixer() == null || testResult.selectedFormat() == null) {
            throw new IOException(testResult.userMessage());
        }
        return recordToWav(outputFile, duration, stopSignal, testResult.selectedMixer(), testResult.selectedFormat());
    }

    public Path runOneSecondTest() throws IOException {
        MicrophoneTestResult testResult = selectWorkingMicrophone(true);
        if (!testResult.available() || testResult.selectedMixer() == null || testResult.selectedFormat() == null) {
            throw new IOException(MICROPHONE_OPEN_FAILED_MESSAGE);
        }

        Path tempFile = Files.createTempFile("smart-voice-mic-test-", ".wav");
        try {
            recordToWav(tempFile, Duration.ofSeconds(1), () -> false, testResult.selectedMixer(), testResult.selectedFormat());
            return tempFile;
        } catch (IOException exception) {
            Files.deleteIfExists(tempFile);
            throw exception;
        }
    }

    public void streamAudio(BooleanSupplier stopSignal, AudioFormat processingFormat, AudioChunkConsumer consumer) throws IOException {
        MicrophoneTestResult testResult = selectWorkingMicrophone(true);
        if (!testResult.available() || testResult.selectedMixer() == null || testResult.selectedFormat() == null) {
            throw new IOException(MICROPHONE_OPEN_FAILED_MESSAGE);
        }
        streamAudio(stopSignal, processingFormat, consumer, testResult.selectedMixer(), testResult.selectedFormat());
    }

    public List<AudioFormat> candidateFormats() {
        return Arrays.asList(CANDIDATE_AUDIO_FORMATS.clone());
    }

    private MicrophoneTestResult selectWorkingMicrophone(boolean logMixers) {
        if (logMixers) {
            System.out.println("[MicrophoneService] Available mixers:");
        }
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        if (mixerInfos.length == 0) {
            if (logMixers) {
                System.out.println("[MicrophoneService] No mixers reported by AudioSystem.");
            }
            return new MicrophoneTestResult(false, MICROPHONE_OPEN_FAILED_MESSAGE, null, null);
        }

        boolean targetDataLineDetected = false;
        for (Mixer.Info mixerInfo : mixerInfos) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            boolean supportsTargetDataLine = supportsTargetDataLine(mixer);
            if (logMixers) {
                System.out.println("[MicrophoneService] - " + mixerInfo.getName()
                        + " (" + mixerInfo.getDescription() + ")"
                        + " targetDataLine=" + supportsTargetDataLine);
            }
            if (!supportsTargetDataLine) {
                continue;
            }

            targetDataLineDetected = true;
            for (AudioFormat candidate : CANDIDATE_AUDIO_FORMATS) {
                DataLine.Info lineInfo = new DataLine.Info(TargetDataLine.class, candidate);
                if (!mixer.isLineSupported(lineInfo)) {
                    if (logMixers) {
                        System.out.println("[MicrophoneService]   format failed (unsupported): " + formatLabel(candidate));
                    }
                    continue;
                }

                TargetDataLine line = null;
                try {
                    line = (TargetDataLine) mixer.getLine(lineInfo);
                    line.open(candidate);
                    if (logMixers) {
                        System.out.println("[MicrophoneService]   format succeeded: " + formatLabel(candidate));
                        System.out.println("[MicrophoneService] Selected mixer: " + mixerInfo.getName());
                    }
                    return new MicrophoneTestResult(true, "Microphone test successful", mixerInfo, candidate);
                } catch (LineUnavailableException | IllegalArgumentException exception) {
                    if (logMixers) {
                        System.out.println("[MicrophoneService]   format failed: " + formatLabel(candidate)
                                + " reason=" + exception.getMessage());
                    }
                } finally {
                    closeLine(line);
                }
            }
        }

        if (!targetDataLineDetected && logMixers) {
            System.out.println("[MicrophoneService] No mixers expose TargetDataLine support.");
        }
        return new MicrophoneTestResult(false, MICROPHONE_OPEN_FAILED_MESSAGE, null, null);
    }

    private Path recordToWav(
            Path outputFile,
            Duration duration,
            BooleanSupplier stopSignal,
            Mixer.Info mixerInfo,
            AudioFormat selectedFormat
    ) throws IOException {
        Mixer mixer = AudioSystem.getMixer(mixerInfo);
        DataLine.Info lineInfo = new DataLine.Info(TargetDataLine.class, selectedFormat);
        TargetDataLine line = null;
        try {
            line = (TargetDataLine) mixer.getLine(lineInfo);
            line.open(selectedFormat);
            line.start();

            long recordingMillis = Math.max(1_000L, duration.toMillis());
            long deadline = System.currentTimeMillis() + recordingMillis;
            byte[] buffer = new byte[BUFFER_SIZE];
            ByteArrayOutputStream recordedBytes = new ByteArrayOutputStream((int) (selectedFormat.getSampleRate() * 2));

            while (System.currentTimeMillis() < deadline) {
                if (stopSignal != null && stopSignal.getAsBoolean()) {
                    break;
                }
                int read = line.read(buffer, 0, buffer.length);
                if (read > 0) {
                    recordedBytes.write(buffer, 0, read);
                }
            }

            byte[] audioData = recordedBytes.toByteArray();
            if (audioData.length < MIN_AUDIO_BYTES) {
                throw new IOException("No usable microphone input was captured.");
            }

            try (ByteArrayInputStream input = new ByteArrayInputStream(audioData);
                 AudioInputStream audioInputStream = new AudioInputStream(
                         input,
                         selectedFormat,
                         audioData.length / selectedFormat.getFrameSize()
                 )) {
                AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFile.toFile());
            }
            return outputFile;
        } catch (LineUnavailableException | IllegalArgumentException exception) {
            throw new IOException(MICROPHONE_OPEN_FAILED_MESSAGE, exception);
        } finally {
            closeLine(line);
        }
    }

    private void streamAudio(
            BooleanSupplier stopSignal,
            AudioFormat processingFormat,
            AudioChunkConsumer consumer,
            Mixer.Info mixerInfo,
            AudioFormat selectedFormat
    ) throws IOException {
        Mixer mixer = AudioSystem.getMixer(mixerInfo);
        DataLine.Info lineInfo = new DataLine.Info(TargetDataLine.class, selectedFormat);
        TargetDataLine line = null;
        try {
            line = (TargetDataLine) mixer.getLine(lineInfo);
            line.open(selectedFormat);
            line.start();
            if (consumer != null) {
                consumer.onMicrophoneReady(mixerInfo, selectedFormat, processingFormat);
            }

            byte[] buffer = new byte[BUFFER_SIZE];
            while (stopSignal == null || !stopSignal.getAsBoolean()) {
                int read = line.read(buffer, 0, buffer.length);
                if (read <= 0) {
                    continue;
                }
                byte[] processedBytes = convertAudioChunk(buffer, read, selectedFormat, processingFormat);
                if (consumer != null && processedBytes.length > 0) {
                    consumer.onAudioChunk(processedBytes, processedBytes.length);
                }
            }
        } catch (LineUnavailableException | IllegalArgumentException exception) {
            throw new IOException(MICROPHONE_OPEN_FAILED_MESSAGE, exception);
        } finally {
            closeLine(line);
        }
    }

    private byte[] convertAudioChunk(byte[] buffer, int bytesRead, AudioFormat sourceFormat, AudioFormat processingFormat) throws IOException {
        byte[] exactBuffer = Arrays.copyOf(buffer, bytesRead);
        if (sameFormat(sourceFormat, processingFormat)) {
            return exactBuffer;
        }
        if (!AudioSystem.isConversionSupported(processingFormat, sourceFormat)) {
            throw new IOException("Microphone audio format could not be converted for speech recognition.");
        }

        try (ByteArrayInputStream input = new ByteArrayInputStream(exactBuffer);
             AudioInputStream sourceStream = new AudioInputStream(input, sourceFormat, bytesRead / sourceFormat.getFrameSize());
             AudioInputStream convertedStream = AudioSystem.getAudioInputStream(processingFormat, sourceStream);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] convertedBuffer = new byte[BUFFER_SIZE];
            int convertedRead;
            while ((convertedRead = convertedStream.read(convertedBuffer)) != -1) {
                if (convertedRead > 0) {
                    output.write(convertedBuffer, 0, convertedRead);
                }
            }
            return output.toByteArray();
        } catch (IllegalArgumentException exception) {
            throw new IOException("Microphone audio format could not be converted for speech recognition.", exception);
        }
    }

    private boolean supportsTargetDataLine(Mixer mixer) {
        List<Line.Info> candidates = new ArrayList<>();
        candidates.addAll(Arrays.asList(mixer.getTargetLineInfo()));
        candidates.add(new DataLine.Info(TargetDataLine.class, null));
        for (Line.Info info : candidates) {
            if (info instanceof DataLine.Info dataLineInfo
                    && TargetDataLine.class.isAssignableFrom(dataLineInfo.getLineClass())) {
                return true;
            }
        }
        return false;
    }

    private String formatLabel(AudioFormat format) {
        return (int) format.getSampleRate() + "Hz/"
                + format.getSampleSizeInBits() + "bit/"
                + format.getChannels() + "ch/"
                + (format.isBigEndian() ? "big-endian" : "little-endian");
    }

    private boolean sameFormat(AudioFormat left, AudioFormat right) {
        return left.getSampleRate() == right.getSampleRate()
                && left.getSampleSizeInBits() == right.getSampleSizeInBits()
                && left.getChannels() == right.getChannels()
                && left.isBigEndian() == right.isBigEndian()
                && left.getEncoding().equals(right.getEncoding());
    }

    private void closeLine(TargetDataLine line) {
        if (line == null) {
            return;
        }
        try {
            if (line.isRunning()) {
                line.stop();
            }
        } catch (Exception ignored) {
        }
        try {
            line.flush();
        } catch (Exception ignored) {
        }
        try {
            line.close();
        } catch (Exception ignored) {
        }
    }
}
