package pi.assistant;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RobotAssistantController {

    private static final String VOICE_UNAVAILABLE_MESSAGE =
            "Voice service is currently unavailable. Please type your command.";

    private final CommandInterpreterService commandInterpreterService;
    private final VoiceRecognitionApiService voiceRecognitionApiService;
    private final AudioRecorderService audioRecorderService;

    public RobotAssistantController(CommandInterpreterService.CommandGateway commandGateway) {
        this.commandInterpreterService = new CommandInterpreterService(commandGateway);
        this.voiceRecognitionApiService = new VoiceRecognitionApiService();
        this.audioRecorderService = new AudioRecorderService();
    }

    public CommandResult executeTypedCommand(String command) {
        return commandInterpreterService.interpret(command);
    }

    public VoiceCommandOutcome executeVoiceCommand() {
        Path audioFile = null;
        try {
            audioFile = audioRecorderService.recordCommand();
            String recognizedText = voiceRecognitionApiService.transcribeAudio(audioFile);
            CommandResult result = executeTypedCommand(recognizedText);
            return new VoiceCommandOutcome(recognizedText, result);
        } catch (IOException | VoiceRecognitionApiService.VoiceRecognitionException exception) {
            String details = exception.getMessage() == null ? "" : exception.getMessage().trim();
            String message = VOICE_UNAVAILABLE_MESSAGE;
            if (!details.isBlank()) {
                message = message + " (" + details + ")";
            }
            return new VoiceCommandOutcome(
                    "",
                    CommandResult.error(message, CommandIntent.UNKNOWN)
            );
        } finally {
            if (audioFile != null) {
                try {
                    Files.deleteIfExists(audioFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    public record VoiceCommandOutcome(String recognizedText, CommandResult commandResult) {
    }
}
