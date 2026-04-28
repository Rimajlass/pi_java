package pi.assistant;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class SmartVoiceAgentPane extends StackPane {

    private final SmartVoiceAgentController controller;
    private final Consumer<CommandResult> resultListener;
    private final AnimatedRobotPane robotPane;
    private final VBox floatingPanel;
    private final VBox responseCardsBox;
    private final TextField commandField;
    private final Button voiceButton;
    private final Label stateLabel;
    private final Label heardLabel;
    private final Label finalTranscriptLabel;
    private final Label diagnosticsLabel;
    private final Label voiceNoteLabel;
    private final CheckBox autoExecuteCheckBox;
    private final AtomicBoolean voiceStopRequested = new AtomicBoolean(false);
    private boolean voiceRunning;

    public SmartVoiceAgentPane(SmartVoiceAgentController controller, Consumer<CommandResult> resultListener) {
        this.controller = controller;
        this.resultListener = resultListener;

        getStyleClass().add("smart-agent-root");
        setPickOnBounds(false);

        robotPane = new AnimatedRobotPane();
        StackPane.setAlignment(robotPane, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(robotPane, new Insets(0, 6, 6, 0));

        floatingPanel = new VBox(14);
        floatingPanel.getStyleClass().add("smart-agent-panel");
        floatingPanel.setVisible(false);
        floatingPanel.setManaged(false);
        floatingPanel.setMaxWidth(560);
        floatingPanel.setPrefWidth(560);
        floatingPanel.setMinWidth(560);
        floatingPanel.setMaxHeight(720);
        floatingPanel.setPrefHeight(720);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("smart-agent-header");
        Label avatar = new Label("🤖");
        avatar.getStyleClass().add("smart-agent-avatar");
        Label title = new Label("Smart Voice Command Agent");
        title.getStyleClass().add("smart-agent-title");
        Label sub = new Label("Control your Savings & Goals by typing or voice");
        sub.getStyleClass().add("smart-agent-subtitle");
        VBox titleBox = new VBox(2, title, sub);
        Button minimizeBtn = new Button("×");
        minimizeBtn.getStyleClass().add("agent-mini-btn");
        minimizeBtn.setOnAction(e -> togglePanel(false));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(avatar, titleBox, spacer, minimizeBtn);

        commandField = new TextField();
        commandField.getStyleClass().add("field");
        commandField.setPromptText("Try: contribute 100 TND to goal PCC");
        commandField.setOnMouseClicked(e -> togglePanel(true));
        commandField.setOnAction(e -> runTyped());
        HBox.setHgrow(commandField, Priority.ALWAYS);
        commandField.setPrefHeight(46);

        Button sendButton = new Button("Send");
        sendButton.getStyleClass().add("primary-btn");
        sendButton.setOnAction(e -> runTyped());
        sendButton.setMinWidth(92);
        sendButton.setPrefHeight(46);

        voiceButton = new Button("Voice");
        voiceButton.getStyleClass().add("ghost-btn");
        voiceButton.setOnAction(e -> runVoice());
        voiceButton.setMinWidth(104);
        voiceButton.setPrefHeight(46);

        HBox commandRow = new HBox(10, commandField, sendButton, voiceButton);
        commandRow.setAlignment(Pos.CENTER_LEFT);
        commandRow.getStyleClass().add("smart-agent-input-row");

        FlowPane chipsRow = new FlowPane();
        chipsRow.setHgap(8);
        chipsRow.setVgap(8);
        chipsRow.getStyleClass().add("smart-agent-chip-row");
        chipsRow.getChildren().addAll(
                quickChip("Show balance", "show my savings balance", false, false),
                quickChip("List goals", "list my goals", false, false),
                quickChip("Contribute to goal", "contribute 100 TND to goal PCC", false, false),
                quickChip("Create goal", "create goal Emergency target 1000 deadline 2026-12-31", false, false),
                quickChip("/help", "/help", false, true),
                quickChip("Delete goal", "delete goal", true, false)
        );

        stateLabel = new Label("Status: Idle");
        stateLabel.getStyleClass().add("smart-agent-state");
        stateLabel.setWrapText(true);

        heardLabel = new Label("Heard live: ");
        heardLabel.getStyleClass().add("mini-row-label");
        heardLabel.setWrapText(true);

        finalTranscriptLabel = new Label("Final recognized text: ");
        finalTranscriptLabel.getStyleClass().add("mini-row-label");
        finalTranscriptLabel.setWrapText(true);

        autoExecuteCheckBox = new CheckBox("Auto execute after voice recognition");
        autoExecuteCheckBox.setSelected(false);
        autoExecuteCheckBox.getStyleClass().add("smart-agent-checkbox");

        Label modelNote = new Label("Voice recognition works best in English with the current model.");
        modelNote.getStyleClass().add("mini-row-label");
        modelNote.setWrapText(true);
        voiceNoteLabel = new Label("You can edit the recognized command before sending.");
        voiceNoteLabel.getStyleClass().add("mini-row-label");
        voiceNoteLabel.setWrapText(true);

        Label voiceCardTitle = new Label("Voice Recognition");
        voiceCardTitle.getStyleClass().add("smart-agent-section-title");
        VBox transcriptBox = new VBox(8, voiceCardTitle, stateLabel, heardLabel, finalTranscriptLabel, autoExecuteCheckBox, voiceNoteLabel, modelNote);
        transcriptBox.getStyleClass().add("smart-agent-voice-card");

        Button testMicButton = new Button("Test Microphone");
        testMicButton.getStyleClass().add("ghost-btn");
        testMicButton.setOnAction(e -> runMicrophoneTest());
        testMicButton.setMaxWidth(Double.MAX_VALUE);

        Button testVoiceRecognitionButton = new Button("Test Voice Recognition");
        testVoiceRecognitionButton.getStyleClass().add("ghost-btn");
        testVoiceRecognitionButton.setOnAction(e -> runVoiceRecognitionTest());
        testVoiceRecognitionButton.setMaxWidth(Double.MAX_VALUE);

        diagnosticsLabel = new Label("Diagnostics are available if you need to verify hardware or recognition.");
        diagnosticsLabel.getStyleClass().add("mini-row-label");
        diagnosticsLabel.setWrapText(true);

        VBox diagnosticsContent = new VBox(8, diagnosticsLabel, testMicButton, testVoiceRecognitionButton);
        diagnosticsContent.getStyleClass().add("smart-agent-diagnostics-content");
        TitledPane diagnosticsPane = new TitledPane("Settings / Diagnostics", diagnosticsContent);
        diagnosticsPane.getStyleClass().add("smart-agent-diagnostics");
        diagnosticsPane.setExpanded(false);
        diagnosticsPane.setCollapsible(true);

        responseCardsBox = new VBox(8);
        responseCardsBox.getStyleClass().add("smart-agent-responses");
        responseCardsBox.setPadding(new Insets(0, 0, 18, 0));
        Label empty = new Label("Ready for commands. Click voice or type to start.");
        empty.getStyleClass().add("analytics-placeholder");
        responseCardsBox.getChildren().add(empty);

        ScrollPane cardsScroll = new ScrollPane(responseCardsBox);
        cardsScroll.getStyleClass().add("smart-agent-scroll");
        cardsScroll.setFitToWidth(true);
        VBox.setVgrow(cardsScroll, Priority.ALWAYS);
        cardsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        cardsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        Label resultsTitle = new Label("Assistant Results");
        resultsTitle.getStyleClass().add("smart-agent-section-title");
        Label resultsSubTitle = new Label("Recent assistant responses appear here.");
        resultsSubTitle.getStyleClass().add("smart-agent-subtitle");
        VBox resultsHeader = new VBox(2, resultsTitle, resultsSubTitle);
        VBox resultsPanel = new VBox(10, resultsHeader, cardsScroll);
        resultsPanel.getStyleClass().add("smart-agent-results-panel");
        VBox.setVgrow(resultsPanel, Priority.ALWAYS);

        Label footer = new Label(controller.footerLabel());
        footer.getStyleClass().add("assistant-powered-label");

        VBox panelContent = new VBox(14, commandRow, chipsRow, transcriptBox, diagnosticsPane, resultsPanel);
        VBox.setVgrow(resultsPanel, Priority.ALWAYS);

        ScrollPane panelScroll = new ScrollPane(panelContent);
        panelScroll.getStyleClass().add("smart-agent-panel-scroll");
        panelScroll.setFitToWidth(true);
        panelScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        panelScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(panelScroll, Priority.ALWAYS);

        floatingPanel.getChildren().addAll(header, panelScroll, footer);
        StackPane.setAlignment(floatingPanel, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(floatingPanel, new Insets(18, 18, 92, 0));

        robotPane.setOnMouseClicked(e -> togglePanel(!floatingPanel.isVisible()));
        getChildren().addAll(floatingPanel, robotPane);
    }

    private Button quickChip(String text, String command, boolean destructive, boolean executeImmediately) {
        Button chip = new Button(text);
        chip.getStyleClass().add("agent-chip-btn");
        if (destructive) {
            chip.getStyleClass().add("agent-chip-btn-danger");
        }
        chip.setOnAction(e -> {
            togglePanel(true);
            commandField.setText(command);
            if (executeImmediately) {
                runTyped();
            }
        });
        return chip;
    }

    private void runTyped() {
        togglePanel(true);
        String command = commandField.getText() == null ? "" : commandField.getText().trim();
        if (command.isBlank()) {
            updateState(AgentUiState.ERROR, null);
            return;
        }
        SmartVoiceAgentController.AgentExecutionResult result = controller.executeTyped(command);
        renderCard(result.responseCard());
        updateState(result.commandResult().isSuccess() ? AgentUiState.EXECUTING : AgentUiState.ERROR, null);
        if (resultListener != null) {
            resultListener.accept(result.commandResult());
        }
    }

    private void runVoice() {
        togglePanel(true);
        if (voiceRunning) {
            voiceStopRequested.set(true);
            updateState(AgentUiState.TRANSCRIBING, null);
            return;
        }

        voiceRunning = true;
        voiceStopRequested.set(false);
        voiceButton.setText("Stop");
        voiceButton.setDisable(false);
        voiceButton.getStyleClass().add("agent-voice-live");
        heardLabel.setText("Heard live: ");
        finalTranscriptLabel.setText("Final recognized text: ");
        updateState(AgentUiState.LISTENING, null);

        Task<VoiceRecognitionResult> task = new Task<>() {
            @Override
            protected VoiceRecognitionResult call() {
                return controller.captureVoice(
                        () -> voiceStopRequested.get(),
                        (state, message) -> Platform.runLater(() -> updateState(state, message)),
                        partialText -> Platform.runLater(() -> heardLabel.setText("Heard live: " + (partialText == null ? "" : partialText)))
                );
            }
        };

        task.setOnSucceeded(e -> {
            VoiceRecognitionResult result = task.getValue();
            if (result != null && result.success() && result.recognizedText() != null && !result.recognizedText().isBlank()) {
                commandField.setText(result.recognizedText());
                heardLabel.setText("Heard live: " + result.recognizedText());
                finalTranscriptLabel.setText("Final recognized text: " + result.recognizedText());
                updateState(AgentUiState.DONE, "recognized");
                updateState(AgentUiState.IDLE, "ready");
                if (autoExecuteCheckBox.isSelected()) {
                    SmartVoiceAgentController.AgentExecutionResult executionResult = controller.executeTyped(result.recognizedText());
                    renderCard(executionResult.responseCard());
                    updateState(executionResult.commandResult().isSuccess() ? AgentUiState.EXECUTING : AgentUiState.ERROR, null);
                    if (resultListener != null) {
                        resultListener.accept(executionResult.commandResult());
                    }
                }
            } else {
                heardLabel.setText("Heard live: ");
                finalTranscriptLabel.setText("Final recognized text: ");
                updateState(AgentUiState.ERROR, null);
            }
            resetVoiceButton();
        });

        task.setOnFailed(e -> {
            resetVoiceButton();
            updateState(AgentUiState.ERROR, "Unable to recognize voice right now. Please try again or type your command.");
        });

        Thread thread = new Thread(task, "smart-voice-agent-thread");
        thread.setDaemon(true);
        thread.start();
    }

    private void runMicrophoneTest() {
        togglePanel(true);
        diagnosticsLabel.setText("Running microphone availability test...");
        updateState(AgentUiState.LISTENING, null);

        Task<MicrophoneTestResult> task = new Task<>() {
            @Override
            protected MicrophoneTestResult call() {
                return controller.testMicrophone();
            }
        };

        task.setOnSucceeded(e -> {
            MicrophoneTestResult result = task.getValue();
            if (result != null && result.available()) {
                diagnosticsLabel.setText("Microphone test successful.");
                updateState(AgentUiState.IDLE, "ready");
            } else {
                diagnosticsLabel.setText("Microphone detected but could not be opened.");
                updateState(AgentUiState.ERROR, null);
            }
        });

        task.setOnFailed(e -> {
            diagnosticsLabel.setText("Microphone detected but could not be opened.");
            updateState(AgentUiState.ERROR, null);
        });

        Thread thread = new Thread(task, "smart-voice-microphone-test-thread");
        thread.setDaemon(true);
        thread.start();
    }

    private void runVoiceRecognitionTest() {
        togglePanel(true);
        diagnosticsLabel.setText("Running voice recognition test...");
        heardLabel.setText("Heard live: ");
        finalTranscriptLabel.setText("Final recognized text: ");
        updateState(AgentUiState.LISTENING, null);

        Task<VoiceRecognitionResult> task = new Task<>() {
            @Override
            protected VoiceRecognitionResult call() {
                return controller.testVoiceRecognition();
            }
        };

        task.setOnSucceeded(e -> {
            VoiceRecognitionResult result = task.getValue();
            if (result != null && result.success()) {
                diagnosticsLabel.setText("Voice recognition test successful.");
                heardLabel.setText("Heard live: " + result.recognizedText());
                finalTranscriptLabel.setText("Final recognized text: " + result.recognizedText());
                commandField.setText(result.recognizedText());
                updateState(AgentUiState.IDLE, "ready");
            } else {
                diagnosticsLabel.setText(result == null ? "Voice recognition test failed." : result.userMessage());
                updateState(AgentUiState.ERROR, null);
            }
        });

        task.setOnFailed(e -> {
            diagnosticsLabel.setText("Voice recognition test failed.");
            updateState(AgentUiState.ERROR, null);
        });

        Thread thread = new Thread(task, "smart-voice-recognition-test-thread");
        thread.setDaemon(true);
        thread.start();
    }

    private void resetVoiceButton() {
        voiceRunning = false;
        voiceStopRequested.set(false);
        voiceButton.setDisable(false);
        voiceButton.setText("Voice");
        voiceButton.getStyleClass().remove("agent-voice-live");
    }

    private void togglePanel(boolean expanded) {
        floatingPanel.setManaged(expanded);
        floatingPanel.setVisible(expanded);
    }

    private void updateState(AgentUiState state, String text) {
        robotPane.setState(state);
        stateLabel.setText("Status: " + statusLabelFor(state, text));
        stateLabel.getStyleClass().removeAll("agent-state-error", "agent-state-active");
        if (state == AgentUiState.ERROR) {
            stateLabel.getStyleClass().add("agent-state-error");
        } else if (state != AgentUiState.IDLE || (text != null && !text.isBlank())) {
            stateLabel.getStyleClass().add("agent-state-active");
        }
    }

    private String statusLabelFor(AgentUiState state, String text) {
        if ("ready".equalsIgnoreCase(text)) {
            return "Ready";
        }
        if ("recognized".equalsIgnoreCase(text)) {
            return "Recognized";
        }
        return switch (state) {
            case LISTENING, RECORDING -> "Listening";
            case TRANSCRIBING, UNDERSTANDING -> "Transcribing";
            case EXECUTING, RESPONDING -> "Executed";
            case DONE -> "Recognized";
            case ERROR -> "Error";
            case IDLE -> "Idle";
        };
    }

    private void renderCard(AgentResponseCard card) {
        if (card == null) {
            return;
        }
        if (responseCardsBox.getChildren().size() == 1 && responseCardsBox.getChildren().get(0) instanceof Label) {
            responseCardsBox.getChildren().clear();
        }

        VBox wrapper = new VBox(6);
        wrapper.getStyleClass().add("agent-response-card");
        wrapper.setFillWidth(true);

        String titleText = card.title() != null && !card.title().isBlank()
                ? card.title()
                : (card.success() ? "Command completed" : "Assistant needs clarification");
        Label title = new Label(titleText);
        title.getStyleClass().add("risk-item-title");

        Label subtitle = new Label(card.resultMessage());
        subtitle.getStyleClass().add("mini-row-label");
        subtitle.setWrapText(true);

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label status = new Label(card.success() ? "Success" : "Error");
        if ("voice_setup_required".equalsIgnoreCase(card.intentLabel())) {
            status.setText("Voice setup required");
        } else if ("voice_model_missing".equalsIgnoreCase(card.intentLabel())) {
            status.setText("Voice model required");
        } else if ("help".equalsIgnoreCase(card.intentLabel())) {
            status.setText("Info");
        }
        status.getStyleClass().addAll("risk-level-pill", card.success() ? "risk-level-low" : "risk-level-critical");
        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        topRow.getChildren().addAll(title, topSpacer, status);

        Label said = new Label("Command recognized: \"" + (card.commandRecognized() == null || card.commandRecognized().isBlank() ? "-" : card.commandRecognized()) + "\"");
        said.getStyleClass().add("mini-row-label");
        said.setWrapText(true);
        Label intent = new Label("Intent: " + card.intentLabel());
        intent.getStyleClass().add("mini-row-label");
        intent.setWrapText(true);
        Label action = new Label("Action: " + card.action());
        action.getStyleClass().add("mini-row-label");
        action.setWrapText(true);

        wrapper.getChildren().addAll(topRow, subtitle, said, intent);
        for (Map.Entry<String, String> entry : card.parameters().entrySet()) {
            Label p = new Label(entry.getKey() + ": " + entry.getValue());
            p.getStyleClass().add("mini-row-label");
            p.setWrapText(true);
            wrapper.getChildren().add(p);
        }
        if (card.subtitle() != null && !card.subtitle().isBlank()) {
            Label secondarySubtitle = new Label(card.subtitle());
            secondarySubtitle.getStyleClass().add("mini-row-label");
            secondarySubtitle.setWrapText(true);
            wrapper.getChildren().add(2, secondarySubtitle);
        }

        for (Map.Entry<String, java.util.List<String>> section : card.sections().entrySet()) {
            Label sectionTitle = new Label(section.getKey());
            sectionTitle.getStyleClass().add("analytics-chip-value");
            wrapper.getChildren().add(sectionTitle);
            for (String line : section.getValue()) {
                Label item = new Label("- " + line);
                item.getStyleClass().add("mini-row-label");
                item.setWrapText(true);
                wrapper.getChildren().add(item);
            }
        }
        if (!card.quickActions().isEmpty()) {
            FlowPane actions = new FlowPane();
            actions.setHgap(6);
            actions.setVgap(6);
            for (String quickAction : card.quickActions()) {
                Button chip = new Button(quickAction);
                chip.getStyleClass().add("agent-chip-btn");
                chip.setOnAction(e -> commandField.setText(quickAction));
                actions.getChildren().add(chip);
            }
            wrapper.getChildren().add(actions);
        }
        wrapper.getChildren().add(action);

        responseCardsBox.getChildren().add(0, wrapper);
        if (responseCardsBox.getChildren().size() > 10) {
            responseCardsBox.getChildren().setAll(FXCollections.observableArrayList(responseCardsBox.getChildren().subList(0, 10)));
        }
    }
}
