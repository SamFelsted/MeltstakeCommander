package com.meltstakecommander;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.util.Duration;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class HelloController {
    Client c = new Client("192.168.2.2", 3000);

    @FXML
    TextField rightTurns;
    @FXML
    TextField leftTurns;

    @FXML
    protected void onSendDrillButtonClick() throws IOException {
        c.sendData(Commands.getDrillCommand(rightTurns.getText(), leftTurns.getText()));
    }

    @FXML
    protected void onSendLightButtonClick() throws IOException {
        c.sendData(Commands.getLightCommand());
    }

    @FXML
    protected void onConnectButtonClick() throws IOException {
        c.connect();
    }

    @FXML
    Button connectionButton;

    public void initialize() {
        // Create a timeline that updates the label every second
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(1000),
                e -> connectionButton.setText(c.testConnection() ? "Connected" : "Not Connected (click to connect)")
        ));
        timeline.setCycleCount(Animation.INDEFINITE); // loop forever
        timeline.play();
    }
}