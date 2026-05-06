package pi.assistant;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class SmartVoiceAgentController {
    private static final String MICROPHONE_FRIENDLY_ERROR = MicrophoneService.MICROPHONE_OPEN_FAILED_MESSAGE;

    private final CommandInterpreterService interpreterService;
    private final AiIntentExtractionService extractionService;
    private final CommandHelpService helpService;
    private final AudioRecorderService recorderService;
    private final VoskSpeechRecognitionService voskService;

    public SmartVoiceAgentController(CommandInterpreterService.CommandGateway gateway) {
        this.interpreterService = new CommandInterpreterService(gateway);
        this.extractionService = new AiIntentExtractionService();
        this.helpService = new CommandHelpService();
        MicrophoneService microphoneService = new MicrophoneService();
        this.recorderService = new AudioRecorderService(microphoneService);
        this.voskService = new VoskSpeechRecognitionService(microphoneService);
    }

    public AgentExecutionResult executeTyped(String commandText) {
        AiIntentExtractionService.ExtractionResult extraction = extractionService.extract(commandText);
        if (extraction.intent() == CommandIntent.HELP) {
            return buildHelpResult(commandText);
        }
        CommandResult result = interpreterService.interpret(commandText);
        if (!extractionService.isGeminiConfigured() && !result.isSuccess() && result.getIntent() == CommandIntent.UNKNOWN) {
            result = CommandResult.error(
                    "AI understanding is not configured. Local command parsing is active. Type /help to see supported commands.",
                    CommandIntent.UNKNOWN
            );
        }
        AgentResponseCard card = AgentResponseFormatter.from(commandText, result);
        return new AgentExecutionResult(commandText == null ? "" : commandText, card, result);
    }

    public VoiceRecognitionResult captureVoice(
            BooleanSupplier stopSignal,
            BiConsumer<AgentUiState, String> stateListener,
            Consumer<String> partialTranscriptListener
    ) {
        try {
            if (!voskService.isModelAvailable()) {
                emit(stateListener, AgentUiState.ERROR, "Voice model not found");
                return new VoiceRecognitionResult(false, "", "Voice model not found. Please install the Vosk model or type your command.");
            }

            emit(stateListener, AgentUiState.LISTENING, "Listening... Speak now.");
            VoiceRecognitionResult result = voskService.transcribeFromMicrophone(
                    stopSignal == null ? () -> false : stopSignal,
                    partialTranscriptListener
            );
            emit(stateListener, result.success() ? AgentUiState.DONE : AgentUiState.ERROR, result.userMessage());
            return result;
        } catch (IOException exception) {
            if (MICROPHONE_FRIENDLY_ERROR.equals(exception.getMessage())) {
                System.err.println("[SmartVoiceAgent] Microphone error: " + exception.getMessage());
                emit(stateListener, AgentUiState.ERROR, MICROPHONE_FRIENDLY_ERROR);
                return new VoiceRecognitionResult(false, "", MICROPHONE_FRIENDLY_ERROR);
            }
            System.err.println("[SmartVoiceAgent] Voice recognition error: " + exception.getMessage());
            String message = "Unable to recognize voice right now. Please try again or type your command.";
            emit(stateListener, AgentUiState.ERROR, message);
            return new VoiceRecognitionResult(false, "", message);
        } catch (Exception exception) {
            System.err.println("[SmartVoiceAgent] Voice recognition error: " + exception.getMessage());
            String message = "Unable to recognize voice right now. Please try again or type your command.";
            emit(stateListener, AgentUiState.ERROR, message);
            return new VoiceRecognitionResult(false, "", message);
        }
    }

    public MicrophoneTestResult testMicrophone() {
        try {
            recorderService.testMicrophone();
            MicrophoneTestResult result = recorderService.testMicrophoneAvailability();
            return new MicrophoneTestResult(
                    true,
                    "Microphone test successful",
                    result.selectedMixer(),
                    result.selectedFormat()
            );
        } catch (IOException exception) {
            System.err.println("[SmartVoiceAgent] Microphone test error: " + exception.getMessage());
            return new MicrophoneTestResult(false, "Microphone detected but could not be opened.", null, null);
        }
    }

    public VoiceRecognitionResult testVoiceRecognition() {
        try {
            if (!voskService.isModelAvailable()) {
                return new VoiceRecognitionResult(false, "", "Voice model not found. Please install the Vosk model or type your command.");
            }
            return voskService.transcribeFromMicrophone(new TimedStopSignal(Duration.ofSeconds(4)), null);
        } catch (IOException exception) {
            if (MICROPHONE_FRIENDLY_ERROR.equals(exception.getMessage())) {
                System.err.println("[SmartVoiceAgent] Voice recognition test microphone error: " + exception.getMessage());
                return new VoiceRecognitionResult(false, "", MICROPHONE_FRIENDLY_ERROR);
            }
            System.err.println("[SmartVoiceAgent] Voice recognition test error: " + exception.getMessage());
            return new VoiceRecognitionResult(false, "", "Unable to recognize voice right now. Please try again or type your command.");
        }
    }

    private void emit(BiConsumer<AgentUiState, String> listener, AgentUiState state, String message) {
        if (listener != null) {
            listener.accept(state, message);
        }
    }

    public String footerLabel() {
        if (extractionService.isGeminiConfigured()) {
            return "Voice powered by Vosk • Understanding powered by Gemini API";
        }
        return "Voice powered by Vosk • Local understanding active";
    }

    private AgentExecutionResult buildHelpResult(String commandText) {
        CommandHelpService.HelpContent help = helpService.buildHelp();
        Map<String, String> status = new LinkedHashMap<>();
        status.put("Status", "Info");
        AgentResponseCard card = new AgentResponseCard(
                help.title(),
                help.subtitle(),
                commandText == null ? "help" : commandText.trim(),
                "help",
                status,
                help.sections(),
                help.quickActions(),
                "Browse supported commands",
                "Choose a command from the sections below.",
                true
        );
        return new AgentExecutionResult(commandText == null ? "" : commandText, card, CommandResult.success("Help displayed.", CommandIntent.HELP));
    }

    public record AgentExecutionResult(
            String recognizedText,
            AgentResponseCard responseCard,
            CommandResult commandResult
    ) {
    }

    private static final class TimedStopSignal implements BooleanSupplier {
        private final long deadline;

        private TimedStopSignal(Duration duration) {
            this.deadline = System.currentTimeMillis() + Math.max(1_000L, duration.toMillis());
        }

        @Override
        public boolean getAsBoolean() {
            return System.currentTimeMillis() >= deadline;
        }
    }
}
