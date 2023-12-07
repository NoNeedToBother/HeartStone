package ru.kpfu.itis.paramonov.heartstone.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;
import ru.kpfu.itis.paramonov.heartstone.net.ServerMessage;
import ru.kpfu.itis.paramonov.heartstone.net.client.GameClient;

public class MainMenuController {

    @FXML
    private Button btnPlay;

    private GameApplication application = null;

    @FXML
    private void initialize() {
        application = GameApplication.getApplication();
        setOnClickListeners();
    }

    private void setOnClickListeners() {
        btnPlay.setOnMouseClicked(mouseEvent -> onPlayClicked());
    }

    private void onPlayClicked() {
        GameClient client = application.getClient();
        String msg = ServerMessage.builder()
                .setEntityToConnect(ServerMessage.Entity.SERVER)
                .setServerAction(ServerMessage.ServerAction.CONNECT)
                .build();
        client.sendMessage(msg);
    }
}
