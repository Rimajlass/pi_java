package pi.assistant;

import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VoskSpeechRecognitionService {

    private static final Pattern TEXT_JSON_PATTERN = Pattern.compile("\"text\"\\s*:\\s*\"(.*?)\"");
    private static final Pattern PARTIAL_JSON_PATTERN = Pattern.compile("\"partial\"\\s*:\\s*\"(.*?)\"");
    private static final AudioFormat VOSK_AUDIO_FORMAT = new AudioFormat(16_000f, 16, 1, true, false);

    private final MicrophoneService microphoneService;

    public VoskSpeechRecognitionService() {
        this(new MicrophoneService());
    }

    VoskSpeechRecognitionService(MicrophoneService microphoneService) {
        this.microphoneService = microphoneService;
    }

    public boolean isModelAvailable() {
        return resolveModelPath() != null;
    }

    public String transcribe(Path wavFile) throws IOException {
        Path modelPath = resolveModelPath();
        if (modelPath == null) {
            throw new IOException("Voice model missing.");
        }

        try (AudioInputStream sourceStream = AudioSystem.getAudioInputStream(wavFile.toFile());
             AudioInputStream stream = toVoskCompatibleStream(sourceStream)) {
            VoiceRecognitionResult result = transcribeStream(stream, modelPath, null);
            return result.recognizedText();
        } catch (IOException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IOException("Voice recognition failed.", exception);
        }
    }

    public VoiceRecognitionResult transcribeFromMicrophone(BooleanSupplier stopSignal, Consumer<String> partialTranscriptListener) throws IOException {
        Path modelPath = resolveModelPath();
        if (modelPath == null) {
            throw new IOException("Voice model missing.");
        }

        try (Model model = new Model(modelPath.toAbsolutePath().toString());
             Recognizer recognizer = new Recognizer(model, 16_000f)) {
            StringBuilder committedTranscript = new StringBuilder();
            microphoneService.streamAudio(stopSignal, VOSK_AUDIO_FORMAT, new MicrophoneService.AudioChunkConsumer() {
                @Override
                public void onMicrophoneReady(Mixer.Info mixerInfo, AudioFormat captureFormat, AudioFormat processingFormat) {
                    System.out.println("[Vosk] Selected mixer: " + mixerInfo.getName());
                    System.out.println("[Vosk] Selected audio format: " + captureFormat);
                    System.out.println("[Vosk] Processing audio format: " + processingFormat);
                }

                @Override
                public void onAudioChunk(byte[] audioBytes, int bytesRead) {
                    System.out.println("[Vosk] bytes read: " + bytesRead);
                    if (recognizer.acceptWaveForm(audioBytes, bytesRead)) {
                        String json = recognizer.getResult();
                        System.out.println("[Vosk] result: " + json);
                        appendTranscript(committedTranscript, extractText(json));
                    } else {
                        String partial = extractPartialText(recognizer.getPartialResult());
                        System.out.println("[Vosk] partial: " + partial);
                        if (partialTranscriptListener != null) {
                            partialTranscriptListener.accept(combineTranscript(committedTranscript.toString(), partial));
                        }
                    }
                }
            });

            String finalJson = recognizer.getFinalResult();
            System.out.println("[Vosk] final result: " + finalJson);
            appendTranscript(committedTranscript, extractText(finalJson));

            String transcript = committedTranscript.toString().trim();
            if (transcript.isBlank()) {
                return new VoiceRecognitionResult(false, "", "I could not hear a clear command. Please try again or type it.");
            }
            return new VoiceRecognitionResult(true, transcript, "Transcription ready. You can edit then press Send.");
        } catch (IOException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IOException("Voice recognition failed.", exception);
        }
    }

    private VoiceRecognitionResult transcribeStream(AudioInputStream stream, Path modelPath, Consumer<String> partialTranscriptListener) throws IOException {
        try (Model model = new Model(modelPath.toAbsolutePath().toString());
             Recognizer recognizer = new Recognizer(model, 16_000f)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            StringBuilder transcript = new StringBuilder();

            while ((bytesRead = stream.read(buffer)) >= 0) {
                if (bytesRead <= 0) {
                    continue;
                }
                System.out.println("[Vosk] bytes read: " + bytesRead);
                if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                    String json = recognizer.getResult();
                    System.out.println("[Vosk] result: " + json);
                    appendTranscript(transcript, extractText(json));
                } else if (partialTranscriptListener != null) {
                    String partial = extractPartialText(recognizer.getPartialResult());
                    System.out.println("[Vosk] partial: " + partial);
                    partialTranscriptListener.accept(combineTranscript(transcript.toString(), partial));
                }
            }

            String finalJson = recognizer.getFinalResult();
            System.out.println("[Vosk] final result: " + finalJson);
            appendTranscript(transcript, extractText(finalJson));

            String recognizedText = transcript.toString().trim();
            if (recognizedText.isBlank()) {
                throw new IOException("No speech detected.");
            }
            return new VoiceRecognitionResult(true, recognizedText, "Transcription ready. You can edit then press Send.");
        } catch (IOException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IOException("Voice recognition failed.", exception);
        }
    }

    private AudioInputStream toVoskCompatibleStream(AudioInputStream sourceStream) throws IOException {
        AudioFormat sourceFormat = sourceStream.getFormat();
        if (matchesVoskFormat(sourceFormat)) {
            return sourceStream;
        }
        if (!AudioSystem.isConversionSupported(VOSK_AUDIO_FORMAT, sourceFormat)) {
            throw new IOException("Recorded audio format is not supported for speech recognition.");
        }
        return AudioSystem.getAudioInputStream(VOSK_AUDIO_FORMAT, sourceStream);
    }

    private boolean matchesVoskFormat(AudioFormat format) {
        return format.getSampleRate() == VOSK_AUDIO_FORMAT.getSampleRate()
                && format.getSampleSizeInBits() == VOSK_AUDIO_FORMAT.getSampleSizeInBits()
                && format.getChannels() == VOSK_AUDIO_FORMAT.getChannels()
                && format.getEncoding().equals(VOSK_AUDIO_FORMAT.getEncoding())
                && format.isBigEndian() == VOSK_AUDIO_FORMAT.isBigEndian();
    }

    private Path resolveModelPath() {
        List<Path> candidates = List.of(
                Paths.get("models", "vosk-model-small-en-us"),
                Paths.get("src", "main", "resources", "models", "vosk-model-small-en-us")
        );
        for (Path candidate : candidates) {
            Path resolved = resolveModelDirectory(candidate);
            if (resolved != null) {
                System.out.println("[Vosk] Using model path: " + resolved.toAbsolutePath());
                return resolved;
            }
        }

        try (InputStream ignored = getClass().getResourceAsStream("/models/vosk-model-small-en-us/README")) {
            return null;
        } catch (IOException exception) {
            return null;
        }
    }

    private Path resolveModelDirectory(Path candidate) {
        if (!Files.exists(candidate) || !Files.isDirectory(candidate)) {
            return null;
        }
        if (isValidModelDirectory(candidate)) {
            return candidate;
        }

        try (var children = Files.list(candidate)) {
            return children
                    .filter(Files::isDirectory)
                    .filter(this::isValidModelDirectory)
                    .findFirst()
                    .orElse(null);
        } catch (IOException exception) {
            System.err.println("[Vosk] Unable to inspect model directory " + candidate.toAbsolutePath() + ": " + exception.getMessage());
            return null;
        }
    }

    private boolean isValidModelDirectory(Path directory) {
        return Files.isRegularFile(directory.resolve("am").resolve("final.mdl"))
                && Files.isRegularFile(directory.resolve("conf").resolve("model.conf"));
    }

    private String extractText(String json) {
        return extractField(json, TEXT_JSON_PATTERN);
    }

    private String extractPartialText(String json) {
        return extractField(json, PARTIAL_JSON_PATTERN);
    }

    private String extractField(String json, Pattern pattern) {
        if (json == null || json.isBlank()) {
            return "";
        }
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1).replace("\\\"", "\"").trim();
    }

    private void appendTranscript(StringBuilder target, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        if (!target.isEmpty()) {
            target.append(' ');
        }
        target.append(text.trim());
    }

    private String combineTranscript(String committedText, String partialText) {
        String committed = committedText == null ? "" : committedText.trim();
        String partial = partialText == null ? "" : partialText.trim();
        if (committed.isBlank()) {
            return partial;
        }
        if (partial.isBlank()) {
            return committed;
        }
        return committed + " " + partial;
    }
}
