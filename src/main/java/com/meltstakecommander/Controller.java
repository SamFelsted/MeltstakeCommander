package com.meltstakecommander;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This is the back end of the view.fxml
 * There are 2 primary helper classes
 * SettingsLoader - loads settings data from files
 * Client - interfaces with the ROV/Meltstake networking
 */
public class Controller {
    SettingsLoader s = new SettingsLoader();
    Client c = new Client(s.getData());

    boolean settingsOpened = false;
    public boolean autoRelease = true;
    public boolean releasing = false;

    Timeline timerTimeline = new Timeline();
    Timeline pingTimeline = new Timeline();

    int REFRESH = 100; //MS

    @FXML
    VBox Main;
    @FXML
    TextField rightTurns;
    @FXML
    TextField leftTurns;
    @FXML
    TextArea custom;
    @FXML
    Button connectionButton;
    @FXML
    TextArea messages;
    @FXML
    Button configButton;
    @FXML
    Text timer;
    @FXML
    TextField rpdField;
    @FXML
    TextField tbdField;
    @FXML
    TextField dtField;
    @FXML
    Slider lightSlider;
    @FXML
    Slider speedSlider;
    @FXML
    ComboBox<String> dataSelector;
    @FXML
    Text dataDisplay1;
    @FXML
    Text dataDisplay2;
    @FXML
    TextField currentInput;
    @FXML
    TextField lsThreshInput;

    @FXML
    protected void onSendOffButtonClick() {
        c.sendData(Commands.getOffCommand(c.getAntennaID()));
    }
    @FXML
    protected void onARRestButtonClick() {
        c.sendData(Commands.getARReset(c.getAntennaID()));
    }
    @FXML
    protected void onSendReleaseButtonClick() {
        c.sendData(Commands.getReleaseCommand(c.getAntennaID()));
    }

    @FXML
    protected void onSendAutoButtonClick() {
        c.sendData(Commands.getAutoCommand(
                c.getAntennaID(), rpdField.getText(), tbdField.getText(), dtField.getText())
        );
    }

    @FXML
    protected void onSendDrillButtonClick() {
        c.sendData(Commands.getDrillCommand(c.getAntennaID(), rightTurns.getText(), leftTurns.getText()));
    }

    @FXML
    protected void onSendLightButtonClick() {
        c.sendData(Commands.getLightCommand(c.getAntennaID(), (int) lightSlider.getValue()));
    }

    @FXML
    protected void onSendSpeedButtonClick() {
        c.sendData(Commands.getSpeedCommand(c.getAntennaID(), speedSlider.getValue()));
    }

    @FXML
    protected void onSendCurrentLimitClick() {
        c.sendData(Commands.getCurrentLimitCommand(c.getAntennaID(), currentInput.getText()));
    }
    @FXML
    protected void onSendLSThreshClick() {
        c.sendData(Commands.getLSThreshCommand(c.getAntennaID(), lsThreshInput.getText()));
    }

    @FXML
    protected void onSendGetDataButtonClick() {
        c.sendData(Commands.getDataCommand(c.getAntennaID(), dataSelector.getValue()));
    }

    @FXML
    protected void onSendResetRotButtonClick() {
        c.sendData(Commands.getRotResetCommand(c.getAntennaID()));
    }

    @FXML
    protected void onSendTareButtonClick() {
        c.sendData(Commands.getTareCommand(c.getAntennaID()));
    }

    @FXML
    protected void onConnectButtonClick() {
        connectionButton.setText(c.testConnection() ? "Beacon Connected" : "Beacon NOT Connected (click to connect)");
    }

    @FXML
    protected void onCustomSendButtonClick(){
        c.sendData(custom.getText());
        custom.clear();
    }

    protected void onEnterPressed(KeyEvent e){
        if (e.getCode() == KeyCode.ENTER) {
            c.sendData(custom.getText());
            custom.clear();
        }
    }

    @FXML
    protected void addMessages(String message){
        messages.appendText("\n" + message);
    }

    private void startAutoPing() {
        pingTimeline.stop();
        pingTimeline.getKeyFrames().clear();


        pingTimeline = new Timeline(
                new KeyFrame(Duration.millis(Integer.parseInt(s.getData().get("pingRate").toString())),
                        e -> onConnectButtonClick()
                )
        );

        pingTimeline.setCycleCount(Animation.INDEFINITE); // loop forever
        pingTimeline.play();
    }

    private void stopTimerAnimation() {
        releasing = false;
        timerTimeline.getKeyFrames().clear();
        timerTimeline.stop();
        timer.applyCss();
    }

    private void startReleaseAnimation() {
        stopTimerAnimation();
        releasing = true;
        timer.setText("AUTO RELEASE IN PROGRESS\nSTOP MOTORS TO TERMINATE");
        timer.setFont(Font.font("ROBOTO MONO", FontWeight.BOLD, 15));
       //timer.setVisible(true);

        timerTimeline.getKeyFrames().add(
                new KeyFrame(Duration.millis(700),
                        e -> {
                            if (timer.getFill() == Color.BLACK) {
                                timer.setFill(Color.RED);
                            } else {
                                timer.setFill(Color.BLACK);
                            }
                        }
        ));

        timerTimeline.setCycleCount(Animation.INDEFINITE);
        timerTimeline.play();
    }

    protected void startTimer() {
        if (!autoRelease) {
            return;
        }

        LocalTime end = LocalTime.now().plusMinutes(5);

        stopTimerAnimation();

        timer.setFill(Color.BLACK);

        timerTimeline.getKeyFrames().add(
            new KeyFrame(Duration.millis((double) REFRESH / 2),
                    e -> {
                        java.time.Duration remaining = java.time.Duration.between(LocalTime.now(), end);
                        if (!remaining.isNegative()) {
                            if (remaining.toMinutesPart() == 0) {
                                timer.setFill(Color.RED);
                            }
                            timer.setText(String.format("%02d:%02d", remaining.toMinutesPart(), remaining.toSecondsPart()));
                        } else {
                            startReleaseAnimation();
                        }
                    })
        );
        timerTimeline.setCycleCount(Animation.INDEFINITE);
        timerTimeline.play();
    }


    @FXML
    protected void refreshClient() {
        String data = c.getReceivedData();

        if (data != null) {
            connectionButton.setText("Beacon Connected");
            addMessages(data);
            refreshClient();
        }
        if (c.getRestartTimer()) {
            startTimer();
            c.setRestartTimer(false);
        }
        if (c.getConfirmedARovrdStatus()) {
            releasing = false;
            if (Objects.equals(timer.getText(), "AUTO RELEASE DISABLED")) {
                timer.setText("00:00");
            }
        }
        if (!c.getConfirmedARovrdStatus()) {
            timer.setText("AUTO RELEASE DISABLED");
        }

        dataDisplay1.setText(c.getDataDisplay1());
        dataDisplay2.setText(c.getDataDisplay2());
    }


    void addSettingParam(VBox parent, TextArea input, String name) {
        HBox box = new HBox();
        box.setAlignment(Pos.BASELINE_LEFT);
        box.getChildren().add(new Text(name));

        input.setMaxHeight(30);
        input.setMinHeight(20);
        input.setMaxWidth(150);
        box.getChildren().add(input);

        parent.getChildren().add(box);
    }

    @FXML
    protected void openSettings() {

        ToggleButton autoReleaseButton = new ToggleButton("A");
        autoReleaseButton.setAlignment(Pos.CENTER);
        autoReleaseButton.setMinWidth(200);
        autoReleaseButton.setMinHeight(40);

        autoReleaseButton.setOnAction(event -> {
            if (releasing) {
                autoReleaseButton.setSelected(true);
                return;
            }
            stopTimerAnimation();
            autoRelease = autoReleaseButton.isSelected();
            c.sendData(Commands.getDisengageToggle(c.getAntennaID(), autoReleaseButton.isSelected()));
        });

        Timeline autoEngageRefresh = new Timeline(
                new KeyFrame(Duration.millis(REFRESH),
                        e -> {
                            if (c.getConfirmedARovrdStatus()) {
                                autoReleaseButton.setText("Auto Release ON");
                                autoReleaseButton.setSelected(true);
                            }
                            if (!c.getConfirmedARovrdStatus()) {
                                autoReleaseButton.setText("Auto Release OFF");
                                autoReleaseButton.setSelected(false);
                            }
                        }
                )
        );


        if (!settingsOpened) {
            Map<String, TextArea> inputs = new HashMap<>();
            settingsOpened = true;

            Stage settingsStage = new Stage();
            settingsStage.setTitle("Settings");
            settingsStage.setWidth(275);
            settingsStage.setHeight(500);

            VBox settingsBox = new VBox();
            settingsBox.setAlignment(Pos.TOP_CENTER);
            settingsBox.setSpacing(10);
            settingsBox.setPadding(new Insets(10, 10, 10, 10));


            settingsBox.getChildren().add(autoReleaseButton);

            Map<String, Object> data = s.getData();

            for (String key : data.keySet()) {
                TextArea input = new TextArea(data.get(key).toString());
                addSettingParam(settingsBox, input, key + ": ");
                inputs.put(key, input);
            }


            Button saveButton = new Button("Save");
            saveButton.setAlignment(Pos.CENTER);
            saveButton.setMinWidth(250);
            saveButton.setMinHeight(40);
            saveButton.setOnAction(event -> {
                for (String key : data.keySet()) {
                    String param = inputs.get(key).getText();
                    data.put(key, param);
                }
                s.setDataAndSave(data);
                c.setNewSettings(data);
                startAutoPing();
                settingsStage.close();
                settingsOpened = false;
            });


            settingsBox.getChildren().add(saveButton);

            Scene settingsScene = new Scene(settingsBox);
            settingsStage.setScene(settingsScene);
            settingsStage.show();

            autoEngageRefresh.setCycleCount(Animation.INDEFINITE);
            autoEngageRefresh.play();

            settingsStage.setOnCloseRequest(t -> {
                settingsOpened = false;
                autoEngageRefresh.stop();
            });
        }

    }


    @FXML
    public void initialize() {
        // Create a timeline that updates the label every second
        startAutoPing();

        Timeline textRefresh = new Timeline(
                new KeyFrame(Duration.millis(REFRESH),
                        e -> refreshClient()
                )
        );

        custom.setOnKeyPressed(this::onEnterPressed);

        for (String data : Commands.getDataStrings()) {
            dataSelector.getItems().add(data);
        }

        textRefresh.setCycleCount(Animation.INDEFINITE);
        textRefresh.play();

    }
}